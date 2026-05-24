package com.laborbook.keep.screen.home.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.boilerplate.analytics.AnalyticsPlatforms
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.laborbook.base.BaseBottomsheetFragment
import com.laborbook.base.analytics.Analytics
import com.laborbook.base.analytics.ConstantEventAttributes
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.base.toggleKeyboard
import com.laborbook.keep.R
import com.laborbook.keep.databinding.FragmentRequestFeatureBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RequestFeatureBottomSheetFragment: BaseBottomsheetFragment<FragmentRequestFeatureBinding>() {
    override val screenName: String
        get() = ConstantEventNames.REQUEST_FEATURE

    override fun onStart() {
        super.onStart()
        val dialog = dialog as BottomSheetDialog
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentRequestFeatureBinding? {
        return FragmentRequestFeatureBinding.inflate(inflater, container, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setOnClickListeners()
        lifecycleScope.launch {
            delay(500)
            binding?.etDescription?.toggleKeyboard(requireActivity())
        }
    }

    private fun setOnClickListeners() {
        binding?.apply {
            btnSubmit.setOnClickListener {
                if(binding?.etDescription?.text.toString().isNotEmpty()) {
                    analytics.logEvent(
                        ConstantEventNames.REQUEST_FEATURE_SUBMIT,
                        Analytics.CLICK,
                        listOf(AnalyticsPlatforms.MIXPANEL, AnalyticsPlatforms.FIREBASE),
                        hashMapOf(Pair(ConstantEventAttributes.MESSAGE, binding?.etDescription?.text.toString())))
                    Toast.makeText(requireContext(), getString(R.string.request_sent_successfully), Toast.LENGTH_SHORT).show()
                    recordClickEvent(ConstantEventNames.FEATURE_REQUEST_SENT)
                    dismiss()
                }
            }

            ivClose.setOnClickListener {
                dismiss()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RequestFeatureBottomSheetFragment()
    }
}