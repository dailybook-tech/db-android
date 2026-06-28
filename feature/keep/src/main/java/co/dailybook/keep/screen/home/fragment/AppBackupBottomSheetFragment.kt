package co.dailybook.keep.screen.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import co.dailybook.base.BaseBottomsheetFragment
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.keep.databinding.FragmentAppBackupBottomsheetBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppBackupBottomSheetFragment : BaseBottomsheetFragment<FragmentAppBackupBottomsheetBinding>() {

    override val screenName: String
        get() = ConstantEventNames.SETTINGS

    override fun onStart() {
        super.onStart()
        val dialog = dialog as BottomSheetDialog
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentAppBackupBottomsheetBinding? {
        return FragmentAppBackupBottomsheetBinding.inflate(inflater, container, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setOnClickListeners()
    }

    private fun setupViews() {
        binding?.apply {
            // Set current time as last updated time
            val currentTime = Date()
            val timeFormat = SimpleDateFormat("h:mma, d MMM yyyy", Locale.getDefault())
            val formattedTime = timeFormat.format(currentTime)
            // Replace space between time and AM/PM if present
            val finalTime = formattedTime.replace(" AM", "AM").replace(" PM", "PM")
            tvLastUpdated.text = getString(co.dailybook.keep.R.string.last_updated, finalTime)
        }
    }

    private fun setOnClickListeners() {
        binding?.apply {
            ivClose.setOnClickListener {
                dismiss()
            }

            btnOk.setOnClickListener {
                dismiss()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = AppBackupBottomSheetFragment()
    }
}

