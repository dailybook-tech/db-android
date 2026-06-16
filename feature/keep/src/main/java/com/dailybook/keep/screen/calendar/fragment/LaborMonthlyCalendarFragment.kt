package com.dailybook.keep.screen.calendar.fragment

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.boilerplate.uikit.views.hide
import com.boilerplate.uikit.views.show
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.dailybook.base.BaseFragment
import com.dailybook.base.analytics.ConstantEventAttributes
import com.dailybook.base.analytics.ConstantEventNames
import com.dailybook.base.appendDotsAfterFirstTwelve
import com.dailybook.base.captureAndShareFullContent
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.income.screen.monthchooser.MonthYearChooserFragment
import com.dailybook.keep.R
import com.dailybook.keep.databinding.FragmentLaborMonthlyCalendarBinding
import com.dailybook.keep.model.AttendanceUser
import com.dailybook.keep.model.CurrentSalaryResponse
import com.dailybook.keep.screen.calendar.adapter.AttendanceCalendarItemAdapter
import com.dailybook.keep.screen.calendar.uistate.CalendarUiState
import com.dailybook.keep.screen.calendar.utils.Constants
import com.dailybook.keep.screen.calendar.utils.ObserverUtil
import com.dailybook.keep.screen.calendar.viewmodel.CalendarViewModel
import com.dailybook.keep.screen.deletestaff.DeleteStaffBottomsheetFragment
import com.dailybook.keep.screen.status.TransactionStatusFragment
import com.dailybook.keep.utils.CoachMarkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.content.Intent
import androidx.core.content.FileProvider
import com.dailybook.base.utils.PdfGenerator
import java.util.Calendar
import java.util.Locale

private const val STAFF_ID = "staff_id"
private const val STAFF_PHONE = "staff_phone"

class LaborMonthlyCalendarFragment : BaseFragment<FragmentLaborMonthlyCalendarBinding>() {

    override val screenName: String
        get() = ConstantEventNames.CALENDAR
    private var currentYear: Int = 2024
    private var currentDate: Int = 1
    private var monthName: String? = "Jan"
    private var monthNumber: Int = 1
    private var staffName: String = ""
    private var staffFullName: String = ""
    private var staffId: String? = ""
    private var staffPhone: String = ""
    private var isStatsExpanded = false
    private lateinit var adapter: AttendanceCalendarItemAdapter
    private val viewModel: CalendarViewModel by viewModel()
    private val observerUtil: ObserverUtil by inject()
    private val coachMarkManager: CoachMarkManager by inject()
    private var isSalaryLoading = false
    private var isReportLoading = false
    private var refreshAnimator: ObjectAnimator? = null
    private var currentAttendanceUser: AttendanceUser? = null
    private var currentSalary: Double? = null
    private var currentSalaryData: CurrentSalaryResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            staffId = it.getString(STAFF_ID)
            staffPhone = it.getString(STAFF_PHONE) ?: ""
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentLaborMonthlyCalendarBinding? {
        return FragmentLaborMonthlyCalendarBinding.inflate(inflater, container, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        isFirstTime = true
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getCurrentDateInfo()
        viewModelObserver()
        registerOnClickListeners()
        setupView()
        getCalendarData(false)
        setObserver()
        // Delay coach mark check to ensure view is fully laid out
        binding?.root?.postDelayed({
            // Check if fragment is still attached and view exists before calling
            if (isAdded && view != null && !isDetached) {
                checkAndShowCoachMark()
            }
        }, 1000)

        parentFragmentManager.setFragmentResultListener("edit_profile_result", viewLifecycleOwner) { _, bundle ->
            val updatedName = bundle.getString("updated_staff_name")
            if (!updatedName.isNullOrBlank()) {
                staffName = updatedName
                binding?.tvToolbarText?.text = updatedName
            }
        }
        binding?.ivRefreshAmount?.setOnClickListener {
            if (!isSalaryLoading) {
                val userId = staffId ?: return@setOnClickListener
                viewModel.fetchUserSalary(userId, monthNumber, currentYear)
            }
        }
    }

    private fun getCalendarData(isRefresh: Boolean) {
        // Check if view exists before accessing viewLifecycleOwner
        if (view == null) {
            return
        }
        try {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                if (!isRefresh) {
                    delay(500)
                }
                // Check if Fragment is still attached and view exists before making API calls
                if (isAdded && view != null) {
                    staffId?.let {
                        viewModel.getStaffAttendances(
                            it,
                            monthNumber.toString(),
                            currentYear.toString()
                        )
                    }
                }
            }
        } catch (e: IllegalStateException) {
            // Fragment view is destroyed, ignore the call
            android.util.Log.d("LaborMonthlyCalendarFragment", "View destroyed, ignoring getCalendarData call")
        }
    }

    private fun setObserver() {
        observerUtil.onDailyAttendanceMarkListener = { attendance ->
            // Check if view exists before accessing viewLifecycleOwner
            if (view != null) {
                try {
                    viewLifecycleOwner.lifecycleScope.launch {
                        // Check if Fragment is still attached before making API calls
                        if (isAdded && view != null) {
                            val markAttendanceBody = viewModel.createMarkSingleAttendanceRequestBody(attendance)
                            viewModel.markSingleAttendance(staffId ?: "", markAttendanceBody)
                        }
                    }
                } catch (e: IllegalStateException) {
                    // Fragment view is destroyed, ignore the callback
                    android.util.Log.d("LaborMonthlyCalendarFragment", "View destroyed, ignoring attendance mark callback")
                }
            }
        }

        observerUtil.refreshCalendar =
            { shouldRefresh: Boolean, isAdvanceTransactionSuccess: Boolean, advance: String, iAccessedDate: Int ->
                // Check if view exists before processing callback
                if (view != null && isAdded) {
                    if (shouldRefresh) {
                        isRefresh = true
                        getCalendarData(true)
                    }
                    if (isAdvanceTransactionSuccess) {
                        fragmentNavigator.start(
                            TransactionStatusFragment.newInstance(
                                staffName,
                                Constants.TYPE_ADVANCE,
                                advance
                            )
                        )
                    }
                }
            }

        observerUtil.goBackFromCalendar = {
            // Check if view exists before processing callback
            if (view != null && isAdded) {
                fragmentNavigator.goBack()
            }
        }
    }

    private fun setupView() {
        adapter = AttendanceCalendarItemAdapter()
        binding?.apply {
            try {
                tvMonthYear.text = monthName?.take(3).plus(" ").plus(currentYear.toString())
                rvAttendance.adapter = adapter
                btnMarkAttendance.isEnabled = viewModel.isAttendancesMarked()
            } catch (e: Exception) {
            }
        }
    }

    private fun registerOnClickListeners() {
        binding?.apply {
            btnMarkAttendance.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    // Check if Fragment is still attached before making API calls
                    if (isAdded) {
                        val markAttendanceBody = viewModel.createMarkAttendanceRequestBody()
                        viewModel.markBulkAttendance(staffId ?: "", markAttendanceBody)
                    }
                }
            }

            ivBack.setOnClickListener {
                fragmentNavigator.goBack()
            }

            tvMonthYear.setOnClickListener {
                openMonthYearChooser()
                recordClickEvent(ConstantEventNames.CHANGE_MONTH, hashMapOf(Pair(ConstantEventAttributes.CURRENT_MONTH, monthName?:"")))
            }

            btnShare.setOnClickListener {
                checkPermissionsAndCapture()
                recordClickEvent(ConstantEventNames.SHARE_ATTENDANCE_TO_LABOR, hashMapOf(Pair(ConstantEventAttributes.LABOR_NAME, staffFullName)))
            }

            ivDeleteStaff.setOnClickListener {
                staffId?.let { sId ->
                    DeleteStaffBottomsheetFragment.newInstance(
                        sId, staffFullName
                    )
                }?.let { deleteStaffBs ->
                    recordClickEvent(ConstantEventNames.DELETE_LABOR_BS, hashMapOf(Pair(ConstantEventAttributes.LABOR_NAME, staffFullName)))
                    fragmentNavigator.start(deleteStaffBs)
                }
            }

            tvToolbarText.setOnClickListener {
                staffId?.let { sId ->
                    DeleteStaffBottomsheetFragment.newInstance(
                        sId, staffFullName
                    )
                }?.let { deleteStaffBs ->
                    recordClickEvent(ConstantEventNames.DELETE_LABOR_BS, hashMapOf(Pair(ConstantEventAttributes.LABOR_NAME, staffFullName)))
                    fragmentNavigator.start(deleteStaffBs)
                }
            }
            tvEdit.setOnClickListener {
                // Fetch current salary data first
                staffId?.let { userId ->
                    viewModel.getCurrentSalary(userId)
                } ?: run {
                    // If no staffId, open without salary data
                    val editProfileFragment = com.dailybook.keep.screen.profile.fragment.EditProfileBottomsheetFragment.newInstance(
                        staffId = staffId ?: "",
                        staffName = staffFullName,
                        staffMobile = staffPhone.takeIf { it.isNotEmpty() }
                    )
                    editProfileFragment.show(parentFragmentManager, "EditProfileBottomsheetFragment")
                }
            }

            // Long press on edit button to reset coach mark for testing (only in debug builds)
            if (com.dailybook.base.BaseConstants.DEBUG) {
                tvEdit.setOnLongClickListener {
                    viewLifecycleOwner.lifecycleScope.launch {
                        // Check if Fragment is still attached before accessing context
                        if (!isAdded || context == null) {
                            return@launch
                        }
                        
                        try {
                            coachMarkManager.resetCoachMark(requireContext())
                            Toast.makeText(requireContext(), "Coach mark reset for testing", Toast.LENGTH_SHORT).show()
                            // Force show coach mark after reset
                            binding?.tvEdit?.let { editButton ->
                                coachMarkManager.showEditButtonCoachMark(
                                    requireActivity(),
                                    editButton
                                ) {
                                    viewLifecycleOwner.lifecycleScope.launch {
                                        // Check if Fragment is still attached before marking as shown
                                        if (isAdded && context != null) {
                                            try {
                                                coachMarkManager.markCoachMarkAsShown(requireContext())
                                            } catch (e: Exception) {
                                                android.util.Log.e("LaborMonthlyCalendarFragment", "Long press: Error marking coach mark as shown", e)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("LaborMonthlyCalendarFragment", "Long press: Error resetting coach mark", e)
                        }
                    }
                    true
                }
            }
            btnOpenReport.setOnClickListener {
                openReport()
            }
            llOpenReport.setOnClickListener {
                openReport()
            }
            btnGeneratePayslip.setOnClickListener {
                generateAndSharePayslip()
                recordClickEvent(ConstantEventNames.LABOR_REPORTS_TAP)
            }
        }
    }

    private fun openReport() {
        recordClickEvent(ConstantEventNames.LABOR_REPORTS_TAP)
        if (currentAttendanceUser == null) {
            Toast.makeText(
                requireContext(),
                "Please wait for attendance data to load",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Start report loading
        viewModel.startReportLoading()

        // Get current month and year for report title
        val monthYear = "${monthName} $currentYear"

        // Check if salary is available, if not fetch it first
        if (currentSalary == null) {
            // Show loading and fetch salary
            val userId = staffId ?: return
            viewModel.fetchUserSalary(userId, monthNumber, currentYear)

            // Wait for salary fetch to complete
            viewLifecycleOwner.lifecycleScope.launch {
                // Wait a bit for the salary fetch to complete
                delay(1000)
                // Check if Fragment is still attached before navigating
                if (isAdded && context != null) {
                    navigateToReport(monthYear)
                }
            }
        } else {
            // Salary is already available, navigate directly
            navigateToReport(monthYear)
        }
    }

    private fun viewModelObserver() {
        viewModel.uiState().observe(viewLifecycleOwner) {
            when (it) {
                is CalendarUiState.Loading -> {
                    if (!isRefresh) {
                        binding?.pb?.show()
                    }
                }
                is CalendarUiState.SalaryLoading -> {
                    if (!isSalaryLoading) {
                        isSalaryLoading = true
                        startRefreshAnimation()
                    }
                }
                is CalendarUiState.GetUserSalarySuccess -> {
                    isSalaryLoading = false
                    stopRefreshAnimation()
                    currentSalary = it.salary
                    binding?.tvAddAmount?.text = getString(R.string.rupee) + " " + it.salary.toString()
                }
                is CalendarUiState.GetUserSalaryError -> {
                    isSalaryLoading = false
                    stopRefreshAnimation()
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
                is CalendarUiState.ReportLoading -> {
                    isReportLoading = it.isLoading
                    if (it.isLoading) {
                        binding?.pb?.show()
                    } else {
                        binding?.pb?.hide()
                    }
                }
                is CalendarUiState.GetCurrentSalarySuccess -> {
                    binding?.pb?.hide()
                    currentSalaryData = it.currentSalary
                    // Open edit profile bottom sheet with salary data
                    val editProfileFragment = com.dailybook.keep.screen.profile.fragment.EditProfileBottomsheetFragment.newInstance(
                        staffId = staffId ?: "",
                        staffName = staffFullName,
                        staffMobile = staffPhone.takeIf { it.isNotEmpty() },
                        initialSalary = it.currentSalary.salary_per_day,
                        initialSalaryType = it.currentSalary.salary_type
                    )
                    editProfileFragment.show(parentFragmentManager, "EditProfileBottomsheetFragment")
                }
                is CalendarUiState.GetCurrentSalaryError -> {
                    binding?.pb?.hide()
                    // Open edit profile bottom sheet without salary data
                    val editProfileFragment = com.dailybook.keep.screen.profile.fragment.EditProfileBottomsheetFragment.newInstance(
                        staffId = staffId ?: "",
                        staffName = staffFullName,
                        staffMobile = staffPhone.takeIf { it.isNotEmpty() }
                    )
                    editProfileFragment.show(parentFragmentManager, "EditProfileBottomsheetFragment")
                }
                is CalendarUiState.CurrentSalaryLoading -> {
                    // Show loading while fetching current salary
                    binding?.pb?.show()
                }
                is CalendarUiState.GetStaffAttendanceSuccess -> {
                    binding?.pb?.hide()
                    it.staff.user?.let { staff ->
                        setUserDetails(staff)
                        currentAttendanceUser = staff
                    }
                    it.staff.user?.name?.let { name -> adapter.setStaffName(name) }
                    adapter.setStaffId(staffId ?: "")
                    adapter.submitList(viewModel.withSundaysMarked(it.staff.attendance))
                    try {
                        if(isFirstTime) {
                            binding?.rvAttendance?.scrollToPosition(currentDate - 1)
                            isFirstTime = false
                        }
                    } catch (e: Exception) {
                    }
                    isRefresh = false
                }
                is CalendarUiState.MarkBulkAttendanceSuccess -> {
                    isRefresh = false
                    binding?.pb?.hide()
                    observerUtil.refreshCalendar?.invoke(true, false, "", 0)
                    triggerInAppReview()
                    viewLifecycleOwner.lifecycleScope.launch {
                        dataStoreManager.write(DataStoreManager.INTERACTED_WITH_APP_FEATURES, true)
                    }
                }
                is CalendarUiState.Error -> {
                    isRefresh = false
                    binding?.pb?.hide()
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    private fun triggerInAppReview() {
        try {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // Check if Fragment is still attached before accessing context
                    if (!isAdded || context == null || activity == null) {
                        return@launch
                    }
                    
                    val appOpenCount =
                        dataStoreManager.read(DataStoreManager.APP_OPEN_COUNT, 0).first()
                    if (appOpenCount != 0 && appOpenCount % 3 == 0) {
                        val reviewManager: ReviewManager =
                            ReviewManagerFactory.create(requireContext())
                        val request: Task<ReviewInfo> = reviewManager.requestReviewFlow()
                        request.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                try {
                                    // We got the ReviewInfo object
                                    val reviewInfo = task.result
                                    val flow: Task<Void> =
                                        reviewManager.launchReviewFlow(
                                            requireActivity(),
                                            reviewInfo
                                        )
                                    triggerImpressionEvent(ConstantEventNames.IN_APP_REVIEW)
                                    flow.addOnCompleteListener { _ ->
                                        recordClickEvent(ConstantEventNames.IN_APP_REVIEW_DONE)
                                    }
                                }catch (e: Exception){}
                            } else {
                            }
                        }
                    }
                }catch (e: Exception){}
            }
        }catch (e: Exception){}
    }

    private fun setUserDetails(staff: AttendanceUser) {
        binding?.apply {
            staffFullName = staff.name // Store the full name
            staffName = staff.name // Store the truncated name for display
            tvToolbarText.text = staffName
            
            // First row stats
            tvTotalPresent.text = staff.totalPresent.toString()
            tvTotalAbsent.text = staff.totalAbsent.toString()
            
            // Convert overtime to hours and display with cleaner format
            val overtimeHours = staff.totalOt?.let { totalOt ->
                // Debug: Log the actual value received
                android.util.Log.d("OvertimeDebug", "Total OT received: $totalOt")
                
                if (totalOt > 0) {
                    // If the value is already in hours (e.g., 9.0 for 9 hours)
                    if (totalOt >= 1) {
                        val hours = totalOt.toInt()
                        val remainingMinutes = ((totalOt - hours) * 60).toInt()
                        if (remainingMinutes > 0) {
                            "${hours}h${remainingMinutes}m"
                        } else {
                            "${hours}h"
                        }
                    } else {
                        // If the value is less than 1, it might be in hours (e.g., 0.5 for 30 minutes)
                        val totalMinutes = (totalOt * 60).toInt()
                        val hours = totalMinutes / 60
                        val minutes = totalMinutes % 60
                        if (hours > 0) {
                            if (minutes > 0) {
                                "${hours}h${minutes}m"
                            } else {
                                "${hours}h"
                            }
                        } else {
                            "${minutes}m"
                        }
                    }
                } else {
                    "0h"
                }
            } ?: "0h"
            tvTotalOt.text = overtimeHours
            
            tvTotalAdvance.text = getString(R.string.rupee).plus(staff.totalAdvance.toString())
            
            // Second row stats
            tvTotalHalfDay.text = staff.totalH?.toString() ?: "0"
            tvTotalPp.text = staff.totalPp?.toString() ?: "0"
            tvTotalPh.text = staff.totalPh?.toString() ?: "0"
            ivRefreshAmount.setOnClickListener {
                fetchAndDisplaySalary()
            }
            
            btnShare.text = getString(R.string.share_to).plus(" ").plus(staff.name)
            // Get the drawable and set it to the start of the button text
            val drawable: Drawable? =
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_whatsapp)
            btnShare.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            val paddingDp = 4
            val density = resources.displayMetrics.density
            val paddingPixel = (paddingDp * density).toInt()
            btnShare.compoundDrawablePadding = paddingPixel
            
            // Set up stats dropdown functionality
            setupStatsDropdown()
        }
    }

    private fun fetchAndDisplaySalary() {
        binding?.apply {
            viewLifecycleOwner.lifecycleScope.launch {
                val userId = staffId ?: return@launch
                viewModel.fetchUserSalary(userId, monthNumber, currentYear)
            }
        }
    }

    private fun setupStatsDropdown() {
        // Make the entire first row clickable for better UX
        binding?.llStatsFirstRow?.setOnClickListener {
            toggleStatsDropdown()
        }
        
        // Also make the dropdown arrow clickable
        binding?.ivStatsDropdown?.setOnClickListener {
            toggleStatsDropdown()
        }
        
        // Ensure initial state is correct
        binding?.ivStatsDropdown?.rotation = 0f
        // binding?.llStatsSecondRow?.visibility = View.INVISIBLE // Temporarily commented for testing
        isStatsExpanded = false
        
        // Ensure initial state is correct
        binding?.llStatsSecondRow?.visibility = View.GONE
        binding?.llStatsSecondRow?.alpha = 0f
    }
    
    private fun toggleStatsDropdown() {
        isStatsExpanded = !isStatsExpanded

        if (isStatsExpanded) {
            // Show second row and report button, change arrow to up with animation
            binding?.llStatsSecondRow?.visibility = View.VISIBLE
            binding?.llStatsSecondRow?.alpha = 0f
            binding?.llStatsSecondRow?.animate()
                ?.alpha(1f)
                ?.setDuration(100)
                ?.start()
            binding?.ivStatsDropdown?.animate()
                ?.rotation(180f)
                ?.setDuration(100)
                ?.start()
        } else {
            // Hide second row and report button, change arrow to down with animation
            binding?.llStatsSecondRow?.animate()
                ?.alpha(0f)
                ?.setDuration(100)
                ?.withEndAction {
                    binding?.llStatsSecondRow?.visibility = View.GONE
                }
                ?.start()
            binding?.ivStatsDropdown?.animate()
                ?.rotation(0f)
                ?.setDuration(100)
                ?.start()
        }
    }

    private fun getCurrentDateInfo() {
        val calendar = Calendar.getInstance()

        monthNumber = calendar.get(Calendar.MONTH) + 1
        monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        currentYear = calendar.get(Calendar.YEAR)
        currentDate = calendar.get(Calendar.DATE)
    }

    private fun openMonthYearChooser() {
        val monthChooserFragment = MonthYearChooserFragment.newInstance(monthNumber - 1, currentYear)
        monthChooserFragment.setOnSelectionCallback { selectedMonth, selectedYear ->
            monthNumber = selectedMonth + 1
            currentYear = selectedYear
            monthName = Calendar.getInstance().apply {
                clear() // Clear all fields to avoid unexpected carry-over from previous state
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.YEAR, selectedYear) // Explicitly set the year
            }.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())

            binding?.tvMonthYear?.text = getFormattedMonthYear()
            
            // Clear the salary when month changes
            currentSalary = null
            binding?.tvAddAmount?.text = getString(R.string.add_amount)
            
            getCalendarData(true)
        }
        monthChooserFragment.show(parentFragmentManager, "MonthYearChooserFragment")
    }

    private fun getFormattedMonthYear(): String {
        return "${monthName?.take(3)} $currentYear"
    }

    private fun checkPermissionsAndCapture() {
        // For Android 10+ (API 29+), no permission is needed to save images to MediaStore
        // using IS_PENDING flag. For Android 9 and below, we need READ_EXTERNAL_STORAGE.
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Android 9 and below - need READ_EXTERNAL_STORAGE for MediaStore access
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        // Android 10+ (API 29+) - no permission needed for MediaStore writes with IS_PENDING

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            try {
                binding?.toolbar?.let { binding?.llStats?.let { it1 ->
                    binding?.llTableHeader?.let { it2 ->
                        binding?.rvAttendance?.let { it3 ->
                            captureAndShareFullContent(it,
                                it1, it2, it3
                            )
                        }
                    }
                } }
            }catch (e: Exception){}
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            try {
                binding?.toolbar?.let { binding?.llStats?.let { it1 ->
                    binding?.llTableHeader?.let { it2 ->
                        binding?.rvAttendance?.let { it3 ->
                            captureAndShareFullContent(it,
                                it1, it2, it3
                            )
                        }
                    }
                } }
            }catch (e: Exception){}
        } else {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startRefreshAnimation() {
        binding?.ivRefreshAmount?.let { iv ->
            refreshAnimator?.cancel()
            refreshAnimator = ObjectAnimator.ofFloat(iv, "rotation", iv.rotation, iv.rotation + 360f).apply {
                duration = 800
                repeatCount = ObjectAnimator.INFINITE
                interpolator = null
                start()
            }
        }
    }

    private fun stopRefreshAnimation() {
        refreshAnimator?.let { animator ->
            animator.cancel()
            binding?.ivRefreshAmount?.rotation = 0f
        }
        refreshAnimator = null
    }

    private fun navigateToReport(monthYear: String) {
        val attendanceUser = currentAttendanceUser ?: return
        
        // Stop report loading before navigation
        viewModel.stopReportLoading()
        
        fragmentNavigator.start(
            com.dailybook.keep.screen.report.ReportFragment.newInstance(
                staffName = staffName,
                staffPhone = staffPhone,
                monthYear = monthYear,
                presentCount = attendanceUser.totalPresent.toInt(),
                absentCount = attendanceUser.totalAbsent.toInt(),
                overtimeCount = attendanceUser.totalOt?.toDouble() ?: 0.0,
                halfdayCount = attendanceUser.totalH?.toInt() ?: 0,
                pPlusHalf = attendanceUser.totalPh?.toString() ?: "-",
                pPlusP = attendanceUser.totalPp?.toString() ?: "-",
                advanceAmount = attendanceUser.totalAdvance,
                totalEarnings = currentSalary ?: 0.0
            )
        )
    }

    private fun generateAndSharePayslip() {
        val attendanceUser = currentAttendanceUser
        if (attendanceUser == null) {
            Toast.makeText(requireContext(), "Please wait for attendance data to load", Toast.LENGTH_SHORT).show()
            return
        }
        val monthYear = "${monthName} $currentYear"
        val overtimeHours = attendanceUser.totalOt?.let { ot ->
            if (ot > 0) {
                val hours = ot.toInt()
                val mins = ((ot - hours) * 60).toInt()
                if (mins > 0) "${hours}h${mins}m" else "${hours}h"
            } else "0h"
        } ?: "0h"

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val file = PdfGenerator.generatePayslip(
                context = requireContext(),
                staffName = staffFullName,
                staffPhone = staffPhone,
                category = "",
                monthYear = monthYear,
                presentCount = attendanceUser.totalPresent.toInt(),
                absentCount = attendanceUser.totalAbsent.toInt(),
                halfdayCount = attendanceUser.totalH?.toInt() ?: 0,
                pPlusHalf = attendanceUser.totalPh?.toString() ?: "0",
                pPlusP = attendanceUser.totalPp?.toString() ?: "0",
                overtimeHours = overtimeHours,
                dailyRate = currentSalaryData?.salary_per_day ?: 0.0,
                overtimeAmount = 0.0,
                bonusAmount = 0.0,
                totalAdvance = attendanceUser.totalAdvance,
                netPayable = currentSalary ?: 0.0
            )
            withContext(Dispatchers.Main) {
                if (file != null && isAdded) {
                    try {
                        val uri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider",
                            file
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share Payslip"))
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Failed to share payslip", Toast.LENGTH_SHORT).show()
                    }
                } else if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to generate payslip", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkAndShowCoachMark() {
        try {
            // Check if fragment is in a valid state before proceeding
            if (!isAdded || view == null || isDetached || activity == null) {
                return
            }
            
            viewLifecycleOwner.lifecycleScope.launch {
                // Check if Fragment is still attached and view exists before proceeding
                if (!isAdded || context == null || view == null || isDetached) {
                    return@launch
                }
                
                if (coachMarkManager.shouldShowCoachMark(requireContext())) {
                    // Wait for the view to be fully laid out
                    binding?.tvEdit?.post {
                        // Check again if Fragment is still attached before showing coach mark
                        if (!isAdded || context == null || activity == null) {
                            return@post
                        }
                        
                        binding?.tvEdit?.let { editButton ->
                            coachMarkManager.showEditButtonCoachMark(
                                requireActivity(),
                                editButton
                            ) {
                                // Mark coach mark as shown when dismissed
                                viewLifecycleOwner.lifecycleScope.launch {
                                    // Check if Fragment is still attached before marking as shown
                                    if (isAdded && context != null) {
                                        try {
                                            coachMarkManager.markCoachMarkAsShown(requireContext())
                                        } catch (e: Exception) {
                                            // Silently handle exception
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Silently handle any exception - don't show coach mark if there's an error
        }
    }

    override fun onDestroyView() {
        // Clear observers to prevent callbacks after view is destroyed
        observerUtil.onDailyAttendanceMarkListener = null
        observerUtil.refreshCalendar = null
        observerUtil.goBackFromCalendar = null
        // Cancel refresh animation if running
        stopRefreshAnimation()
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance(staffId: String, staffPhone: String) = LaborMonthlyCalendarFragment().apply {
            arguments = Bundle().apply {
                putString(STAFF_ID, staffId)
                putString(STAFF_PHONE, staffPhone)
            }
        }
    }
}