package com.dailybook.keep.screen.addstaff.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.boilerplate.uikit.views.hide
import com.boilerplate.uikit.views.show
import com.boilerplate.uikit.views.textviews.text10.TextViewRegular10
import com.dailybook.base.AdUnitConstants
import com.dailybook.base.BaseFragment
import com.dailybook.base.analytics.ConstantEventAttributes
import com.dailybook.base.analytics.ConstantEventNames
import com.dailybook.base.analytics.ConstantEventSources
import com.dailybook.base.datastore.DataStoreManager
import com.dailybook.base.hideKeyboard
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.dailybook.base.navigator.FragmentNavigator
import com.dailybook.keep.R
import com.dailybook.keep.databinding.FragmentAddStaffContactsBinding
import com.dailybook.keep.model.AddStaffUserRequestBody
import com.dailybook.keep.screen.addstaff.adapter.ContactItemAdapter
import com.dailybook.keep.screen.addstaff.uistate.AddStaffUiState
import com.dailybook.keep.screen.addstaff.viewmodel.ContactsViewModel
import com.dailybook.keep.screen.calendar.fragment.LaborMonthlyCalendarFragment
import com.dailybook.keep.screen.calendar.utils.ObserverUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class AddStaffContactsFragment : BaseFragment<FragmentAddStaffContactsBinding>() {

    private val viewModel: ContactsViewModel by viewModel()
    private val observerUtil: ObserverUtil by inject()
    private lateinit var adapter: ContactItemAdapter
    private var isAddStaffOpen = false
    private var selectedCategory: String? = null

    private val adUnitId = AdUnitConstants.NativeAds.CONTACTS_LIST

    private val categories = listOf("Mason", "Carpenter", "Painter", "Plumber", "Electrician", "Driver", "Office Staff", "Other")

    override val screenName: String
        get() = ConstantEventNames.CONTACTS

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.loadContacts(requireContext(), false)
            binding?.etNoResultFound?.hide()
            binding?.llAddStaffToolTip?.hide()
            binding?.etSearchContacts?.show()
            binding?.llContacts?.show()
            recordClickEvent(
                ConstantEventNames.PERMIT_CONTACTS, hashMapOf(
                    Pair(ConstantEventAttributes.PERMISSION_GRANTED, ConstantEventSources.YES)
                )
            )
        } else {
            binding?.etNoResultFound?.show()
            binding?.llAddStaffToolTip?.show()
            binding?.etSearchContacts?.hide()
            binding?.llContacts?.hide()
            viewModel.openNonContactStaff()
            recordClickEvent(
                ConstantEventNames.PERMIT_CONTACTS, hashMapOf(
                    Pair(ConstantEventAttributes.PERMISSION_GRANTED, ConstantEventSources.NO)
                )
            )
            Toast.makeText(context, "Permission denied to read contacts", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentAddStaffContactsBinding? {
        return FragmentAddStaffContactsBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        requestContactsPermission(false)
        viewModelObserver()
        registerOnClickListeners()
        setObserver()
        
        // Observe Pro status changes to remove ads immediately when user upgrades
        observeProStatusChanges()
    }
    
    /**
     * Observe Pro status changes and remove ads from adapter when user becomes Pro
     */
    private fun observeProStatusChanges() {
        dataStoreManager.read(DataStoreManager.PRO_STATUS, false)
            .onEach { isPro ->
                if (isPro && ::adapter.isInitialized) {
                    // User became Pro - remove all ads from adapter immediately
                    adapter.removeAllAds()
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun setObserver() {
        observerUtil.onStaffUserAddedListener = { staff ->
            lifecycleScope.launch(Dispatchers.IO) {
                val cleanedPhoneNumber = staff.mobileNumber
                    .trim()
                    .replace("[\\s()-]+".toRegex(), "")
                    .takeLast(10)
                val addStaffRequestBody = AddStaffUserRequestBody(
                    staff.name,
                    cleanedPhoneNumber,
                    dataStoreManager.read(DataStoreManager.USER_ID, "").first()
                )
                viewModel.addStaffUser(addStaffRequestBody)
                recordClickEvent(ConstantEventNames.ADD_LABOR_FROM_CONTACTS)
            }
        }
    }

    private fun setupViews() {
        adapter = ContactItemAdapter(adUnitId = adUnitId)
        binding?.apply {
            hideAddStaffLayout()
            btnAddStaff.isEnabled = false
            etStaffName.let { viewModel.attachNameTextWatcher(it) }
            etStaffMobileNumber.let { viewModel.attachMobileNumberTextWatcher(it) }
            rvContacts.adapter = adapter
            tvToolbarText.text = getString(R.string.add_staff)
            setupCategoryChips()
            etSearchContacts.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    s?.let {
                        adapter.filter(it.toString())
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        }
    }

    private fun registerOnClickListeners() {
        binding?.apply {
            btnAddStaff.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    val cleanedPhoneNumber = etStaffMobileNumber.text.toString()
                        .trim()
                        .replace("[\\s()-]+".toRegex(), "")
                        .takeLast(10)
                    val addStaffRequestBody = AddStaffUserRequestBody(
                        etStaffName.text.toString().trim(),
                        cleanedPhoneNumber,
                        dataStoreManager.read(DataStoreManager.USER_ID, "").first(),
                        selectedCategory
                    )
                    viewModel.addStaffUser(addStaffRequestBody)
                    withContext(Dispatchers.Main) {
                        etStaffName.text.clear()
                        etStaffMobileNumber.text.clear()
                        etStaffName.requestFocus()
                    }
                    recordClickEvent(ConstantEventNames.ADD_LABOR_MANUAL)
                }
            }

            ivBack.setOnClickListener {
                fragmentNavigator.goBack()
            }

            ivRefreshContacts.setOnClickListener {
                requestContactsPermission(true)
                recordClickEvent(ConstantEventNames.REFRESH_CONTACTS)
            }

            llAddStaff.setOnClickListener {
                openOrCloseAddStaffManualLayout()
            }

            ivAddStaff.setOnClickListener {
                openOrCloseAddStaffManualLayout()
            }

            tvAddStaffManually.setOnClickListener {
                openOrCloseAddStaffManualLayout()
            }

            ivChevron.setOnClickListener {
                openOrCloseAddStaffManualLayout()
            }

            etSearchContacts.setOnClickListener {
                recordClickEvent(ConstantEventNames.SEARCH_CONTACTS)
            }
        }
    }

    private fun FragmentAddStaffContactsBinding.openOrCloseAddStaffManualLayout() {
        if (isAddStaffOpen) {
            isAddStaffOpen = false
            hideAddStaffLayout()
        } else {
            isAddStaffOpen = true
            showAddStaffLayout()
            recordClickEvent(ConstantEventNames.SHOW_MANUAL_ADD_LABOR_FORM)
        }
    }

    private fun setupCategoryChips() {
        val container = binding?.llCategoryChips ?: return
        container.removeAllViews()
        categories.forEach { cat ->
            val chip = TextView(requireContext()).apply {
                text = cat
                textSize = 11f
                setPadding(
                    resources.getDimensionPixelSize(com.boilerplate.uikit.R.dimen.margin_10),
                    resources.getDimensionPixelSize(com.boilerplate.uikit.R.dimen.margin_4),
                    resources.getDimensionPixelSize(com.boilerplate.uikit.R.dimen.margin_10),
                    resources.getDimensionPixelSize(com.boilerplate.uikit.R.dimen.margin_4),
                )
                val lp = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = resources.getDimensionPixelSize(com.boilerplate.uikit.R.dimen.margin_6) }
                layoutParams = lp
                background = androidx.core.content.ContextCompat.getDrawable(
                    requireContext(), com.dailybook.keep.R.drawable.custom_radio_button_bg
                )
                setTextColor(androidx.core.content.ContextCompat.getColor(
                    requireContext(), com.dailybook.keep.R.color.custom_radio_text_color
                ))
                isClickable = true
                isFocusable = true
                setOnClickListener { selectCategory(cat, this) }
            }
            container.addView(chip)
        }
    }

    private fun selectCategory(cat: String, selectedView: TextView) {
        selectedCategory = if (selectedCategory == cat) {
            // Deselect if already selected
            null
        } else {
            cat
        }
        // Refresh chip visuals
        val container = binding?.llCategoryChips ?: return
        for (i in 0 until container.childCount) {
            val chip = container.getChildAt(i) as? TextView ?: continue
            val isSelected = chip.text == selectedCategory
            chip.background = androidx.core.content.ContextCompat.getDrawable(
                requireContext(),
                if (isSelected) com.dailybook.keep.R.drawable.custom_radio_button_bg_selected
                else com.dailybook.keep.R.drawable.custom_radio_button_bg
            )
            chip.setTextColor(androidx.core.content.ContextCompat.getColor(
                requireContext(),
                if (isSelected) android.R.color.white
                else com.dailybook.keep.R.color.custom_radio_text_color
            ))
        }
    }

    private fun FragmentAddStaffContactsBinding.showAddStaffLayout() {
        etStaffName.show()
        etStaffMobileNumber.show()
        hsvCategory.show()
        btnAddStaff.show()
        ivChevron.setImageResource(R.drawable.ic_chevron_up)
    }

    private fun FragmentAddStaffContactsBinding.hideAddStaffLayout() {
        etStaffName.hide()
        etStaffMobileNumber.hide()
        hsvCategory.hide()
        btnAddStaff.hide()
        ivChevron.setImageResource(R.drawable.ic_chevron_down)
    }

    private fun viewModelObserver() {
        viewModel.uiState().observe(viewLifecycleOwner) { uiState ->
            when (uiState) {
                is AddStaffUiState.Loading -> binding?.pb?.show()
                is AddStaffUiState.Success -> {
                    binding?.pb?.hide()
                    binding?.etNoResultFound?.hide()
                    binding?.llAddStaffToolTip?.hide()
                    binding?.etSearchContacts?.show()
                    binding?.llContacts?.show()
                    adapter.submitOriginalList(uiState.contacts)
                }

                is AddStaffUiState.Error -> {
                    binding?.etNoResultFound?.show()
                    binding?.llAddStaffToolTip?.show()
                    binding?.etSearchContacts?.hide()
                    binding?.llContacts?.hide()
                    Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                }

                is AddStaffUiState.EnableAddStaffButton -> {
                    binding?.btnAddStaff?.isEnabled = uiState.isDetailsEntered
                }

                is AddStaffUiState.StaffUserAddedSuccess -> {
                    binding?.pb?.hide()
                    binding?.etSearchContacts?.hideKeyboard(requireActivity())
                    observerUtil.addedStaff?.invoke(true)
                    fragmentNavigator.start(LaborMonthlyCalendarFragment.newInstance(uiState.id, uiState.mobileNumber))
                }

                is AddStaffUiState.OpenNonContactStaff -> {
                    binding?.showAddStaffLayout()
                }

                is AddStaffUiState.StaffAddError -> {
                    binding?.pb?.hide()
                    Toast.makeText(context, uiState.message, Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
        }
    }

    private fun requestContactsPermission(shouldRefresh: Boolean) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.loadContacts(requireContext(), shouldRefresh)
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::adapter.isInitialized) {
            adapter.releaseAds()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = AddStaffContactsFragment()
    }
}