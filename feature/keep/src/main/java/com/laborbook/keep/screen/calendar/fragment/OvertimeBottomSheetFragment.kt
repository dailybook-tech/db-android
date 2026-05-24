package com.laborbook.keep.screen.calendar.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.EditText
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.laborbook.base.BaseBottomsheetFragment
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.base.toFormattedDate
import com.laborbook.keep.R
import com.laborbook.keep.databinding.FragmentOvertimeBottomsheetBinding
import com.laborbook.keep.screen.calendar.utils.ObserverUtil
import com.laborbook.keep.screen.calendar.viewmodel.OvertimeViewModel
import com.laborbook.keep.screen.attendance.AttendanceMarkBottomsheetFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.laborbook.base.datastore.DataStoreManager
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.laborbook.base.analytics.ConstantEventAttributes

// Extension function for consistent NumberPicker styling
private fun NumberPicker.applyConsistentStyling(context: android.content.Context) {
    // Disable focus to prevent keyboard from showing
    isFocusable = false
    isFocusableInTouchMode = false
    
    // Set formatter for consistent display
    setFormatter { String.format("%02d", it) }
    
    // Apply styling to all child views
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is EditText) {
            child.apply {
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, com.boilerplate.uikit.R.color.text_20_color))
                isFocusable = false
                isCursorVisible = false
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                setPadding(0, 0, 0, 0)
            }
        }
    }
    
    // Use reflection to style the selector wheel paint
    try {
        val pickerClass = NumberPicker::class.java
        
        // Style the selector wheel paint
        val paintField = pickerClass.getDeclaredField("mSelectorWheelPaint")
        paintField.isAccessible = true
        val paint = paintField.get(this) as android.graphics.Paint
        paint.textSize = 20f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = ContextCompat.getColor(context, com.boilerplate.uikit.R.color.text_20_color)
        paint.isAntiAlias = true
        
        // Style the input text
        val inputTextField = pickerClass.getDeclaredField("mInputText")
        inputTextField.isAccessible = true
        val inputText = inputTextField.get(this) as EditText
        inputText.apply {
            textSize = 20f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, com.boilerplate.uikit.R.color.text_20_color))
            isFocusable = false
            isCursorVisible = false
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
        }
        
        // Also style the selector element paint for better consistency
        try {
            val selectorElementPaintField = pickerClass.getDeclaredField("mSelectorElementPaint")
            selectorElementPaintField.isAccessible = true
            val selectorPaint = selectorElementPaintField.get(this) as android.graphics.Paint
            selectorPaint.textSize = 20f
            selectorPaint.typeface = Typeface.DEFAULT_BOLD
            selectorPaint.color = ContextCompat.getColor(context, com.boilerplate.uikit.R.color.text_20_color)
            selectorPaint.isAntiAlias = true
        } catch (e: Exception) {
            // Ignore if this field doesn't exist
        }
        
        invalidate()
    } catch (e: Exception) {
        // Fallback styling - if reflection fails, just ignore
        // The main styling should work in most cases
    }
}

class OvertimeBottomSheetFragment : BaseBottomsheetFragment<FragmentOvertimeBottomsheetBinding>(), KoinComponent {

    override val screenName: String = ConstantEventNames.OVERTIME_BOTTOM_SHEET

    private val viewModel: OvertimeViewModel by viewModel()
    private val dataStore: DataStoreManager by inject()
    private val observerUtil: ObserverUtil by inject()

    private var _binding: FragmentOvertimeBottomsheetBinding? = null
    private var date: String? = null
    private var selectedTimeRaw: String = ""
    private var userId: String? = null
    private var existingOtMinutes: Double = 0.0
    private var existingOtPerHour: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        date = arguments?.getString(DATE)
        userId = arguments?.getString(USER_ID)
        existingOtMinutes = arguments?.getDouble(OT_MINUTES, 0.0) ?: 0.0
        existingOtPerHour = arguments?.getDouble(OT_PER_HOUR, 0.0) ?: 0.0
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentOvertimeBottomsheetBinding {
        _binding = FragmentOvertimeBottomsheetBinding.inflate(inflater, container, false)
        return _binding!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        setupTextWatchers()
        binding?.tvDate?.text = date?.toFormattedDate()
        
        // Pre-fill fields if existing OT data is available
        if (existingOtMinutes > 0 && existingOtPerHour > 0) {
            val hours = (existingOtMinutes / 60).toInt()
            val minutes = (existingOtMinutes % 60).toInt()
            selectedTimeRaw = String.format("%02d:%02d", hours, minutes)
            binding?.etHours?.setText(selectedTimeRaw.plus(" hrs"))
            binding?.etRate?.setText(existingOtPerHour.toString())
            calculateTotal()
            // Show remove button when existing OT data is available
            binding?.btnRemoveOt?.visibility = View.VISIBLE
        } else {
            binding?.btnOk?.isEnabled = false
            updateButtonStyle(false)
            // Hide remove button when no existing OT data
            binding?.btnRemoveOt?.visibility = View.GONE
        }
        
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding?.ivClose?.setOnClickListener { dismiss() }

        binding?.etHours?.setOnClickListener {
            showTimePicker()
        }
        
        binding?.etHours?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showTimePicker()
            }
        }
        
        binding?.btnOk?.setOnClickListener {
            saveOvertime()
        }

        binding?.btnRemoveOt?.setOnClickListener {
            deleteOvertime()
        }
    }

    private fun setupTextWatchers() {
        val watcher: (CharSequence?, Int, Int, Int) -> Unit = { _, _, _, _ ->
            calculateTotal()
            val filled = !binding?.etHours?.text.isNullOrBlank() && !binding?.etRate?.text.isNullOrBlank()
            binding?.btnOk?.isEnabled = filled
            updateButtonStyle(filled)
        }

        binding?.etHours?.doOnTextChanged(watcher)
        binding?.etRate?.doOnTextChanged(watcher)
    }

    private fun showTimePicker() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_overtime_timepicker, null)
        dialog.setContentView(view)

        val ivClose = view.findViewById<ImageView>(R.id.iv_close_picker)
        val npHours = view.findViewById<NumberPicker>(R.id.np_hours)
        val npMinutes = view.findViewById<NumberPicker>(R.id.np_minutes)
        val btnOk = view.findViewById<com.boilerplate.uikit.views.buttons.PrimaryButton>(R.id.btn_picker_ok)

        npHours.minValue = 0
        npHours.maxValue = 23

        npMinutes.minValue = 0
        npMinutes.maxValue = 59

        // Set initial values based on existing data or current selection
        val currentHours = if (selectedTimeRaw.isNotEmpty()) {
            selectedTimeRaw.split(":").getOrNull(0)?.toIntOrNull() ?: 0
        } else {
            0
        }
        val currentMinutes = if (selectedTimeRaw.isNotEmpty()) {
            selectedTimeRaw.split(":").getOrNull(1)?.toIntOrNull() ?: 0
        } else {
            0
        }

        npHours.value = currentHours
        npMinutes.value = currentMinutes

        // Apply consistent styling using the extension function
        npHours.applyConsistentStyling(requireContext())
        npMinutes.applyConsistentStyling(requireContext())
        
        // Apply styling again after a short delay to ensure it's applied after layout
        npHours.postDelayed({
            if (isAdded && context != null) {
                npHours.applyConsistentStyling(requireContext())
            }
        }, 100)
        npMinutes.postDelayed({
            if (isAdded && context != null) {
                npMinutes.applyConsistentStyling(requireContext())
            }
        }, 100)

        fun updateOkState() {
            btnOk.isEnabled = !(npHours.value == 0 && npMinutes.value == 0)
        }

        npHours.setOnValueChangedListener { _, _, _ ->
            updateOkState()
            // Re-apply styling after value change to ensure consistency
            npHours.applyConsistentStyling(requireContext())
        }
        npMinutes.setOnValueChangedListener { _, _, _ ->
            updateOkState()
            // Re-apply styling after value change to ensure consistency
            npMinutes.applyConsistentStyling(requireContext())
        }

        updateOkState()

        ivClose.setOnClickListener { dialog.dismiss() }

        btnOk.setOnClickListener {
            val h = npHours.value
            val m = npMinutes.value
            val formattedTime = String.format("%02d:%02d", h, m)
            selectedTimeRaw = formattedTime
            binding?.etHours?.setText("$formattedTime hrs")
            calculateTotal()
            dialog.dismiss()
        }

        dialog.show()
        
        // Apply styling again after dialog is shown to ensure consistency
        dialog.window?.decorView?.post {
            if (isAdded && context != null) {
                npHours.applyConsistentStyling(requireContext())
                npMinutes.applyConsistentStyling(requireContext())
            }
        }
    }



    private fun calculateTotal() {
        try {
            val hoursText = binding?.etHours?.text?.toString()?.replace(" hrs", "") ?: ""
            val rateText = binding?.etRate?.text?.toString() ?: ""
            
            if (hoursText.isNotEmpty() && rateText.isNotEmpty()) {
                val hours = hoursText.split(":").let {
                    it[0].toDouble() + (it.getOrNull(1)?.toDoubleOrNull() ?: 0.0) / 60
                }
                val rate = rateText.toDouble()
                val total = hours * rate
                
                val formattedAmount = String.format("%.2f", total)
                binding?.tvTotalAmount?.text = "₹$formattedAmount"
            }
        } catch (e: Exception) {
            // Handle error
        }
        val filled = !binding?.etHours?.text.isNullOrBlank() && !binding?.etRate?.text.isNullOrBlank()
        binding?.btnOk?.isEnabled = filled
        updateButtonStyle(filled)
    }

    private fun saveOvertime() {
        val hoursText = binding?.etHours?.text?.toString()?.replace(" hrs", "") ?: ""
        val rateText = binding?.etRate?.text?.toString() ?: ""
        val dateStr = date ?: ""
        val staffUserId = userId ?: ""
        if (hoursText.isEmpty() || rateText.isEmpty() || dateStr.isEmpty() || staffUserId.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }
        val otMinutes = hoursText.split(":").let {
            (it[0].toDoubleOrNull() ?: 0.0) * 60 + (it.getOrNull(1)?.toDoubleOrNull() ?: 0.0)
        }
        val otPerHour = rateText.toDoubleOrNull() ?: 0.0
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val managerId = dataStore.read(DataStoreManager.USER_ID, "").first()
                viewModel.addOvertime(staffUserId, dateStr, otMinutes, otPerHour, managerId)
            }
        }
    }

    private fun deleteOvertime() {
        val dateStr = date ?: ""
        val staffUserId = userId ?: ""
        if (dateStr.isEmpty() || staffUserId.isEmpty()) {
            Toast.makeText(requireContext(), "Invalid data", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Record analytics event
        recordClickEvent(ConstantEventNames.REMOVE_OVERTIME_FROM_BS, hashMapOf(
            Pair(ConstantEventAttributes.DATE, dateStr),
            Pair(ConstantEventAttributes.USER_ID, staffUserId)
        ))
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                val managerId = dataStore.read(DataStoreManager.USER_ID, "").first()
                viewModel.deleteOvertime(staffUserId, dateStr, managerId)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.otResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                // Refresh calendar after successful OT addition/deletion
                observerUtil.refreshCalendar?.invoke(true, false, "", 0)
                dismiss()
                // Close the parent attendance marking bottom sheet
                parentFragmentManager.fragments.find { it is AttendanceMarkBottomsheetFragment }?.let { parentFragment ->
                    (parentFragment as? BaseBottomsheetFragment<*>)?.dismiss()
                }
            }
            result.onFailure {
                Toast.makeText(requireContext(), it.message ?: "Failed to process OT", Toast.LENGTH_SHORT).show()
                // Do not dismiss on failure
            }
        }
    }

    private fun updateButtonStyle(enabled: Boolean) {
        binding?.btnOk?.apply {
            alpha = if (enabled) 1f else 0.5f
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "OvertimeBottomSheetFragment"
        private const val DATE = "DATE"
        private const val USER_ID = "user_id"
        private const val OT_MINUTES = "ot_minutes"
        private const val OT_PER_HOUR = "ot_per_hour"

        @JvmStatic
        fun newInstance(userId: String, date: String, otMinutes: Double = 0.0, otPerHour: Double = 0.0) = OvertimeBottomSheetFragment().apply {
            arguments = Bundle().apply {
                putString(DATE, date)
                putString(USER_ID, userId)
                putDouble(OT_MINUTES, otMinutes)
                putDouble(OT_PER_HOUR, otPerHour)
            }
        }
    }
}
