package com.dailybook.base.languagemanager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.dailybook.base.BaseBottomsheetFragment
import com.dailybook.base.R
import com.dailybook.base.analytics.ConstantEventAttributes
import com.dailybook.base.analytics.ConstantEventNames
import com.dailybook.base.databinding.FragmentLanguageBottomSheetBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class LanguageBottomSheetFragment : BaseBottomsheetFragment<FragmentLanguageBottomSheetBinding>() {

    override val screenName: String
        get() = ConstantEventNames.LANGUAGE_BS

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentLanguageBottomSheetBinding? {
        return FragmentLanguageBottomSheetBinding.inflate(inflater, container, false)
    }

    private val languageManager: LanguageManager by inject() // Inject LanguageManager

    companion object {
        // You can pass arguments here if needed, for now, we'll use it without arguments
        @JvmStatic
        fun newInstance(): LanguageBottomSheetFragment {
            return LanguageBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    // Add any additional arguments here if needed
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as BottomSheetDialog
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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
        registerOnClickListeners()
        loadSavedLanguage()
    }

    private fun loadSavedLanguage() {
        lifecycleScope.launch {
            val savedLanguage = languageManager.getSavedLanguage().first()
            markSelected(savedLanguage)
        }
    }

    private fun registerOnClickListeners() {
        // Set click listeners for language buttons
        binding?.btnEnglish?.setOnClickListener {
            changeLanguage("en")
        }

        binding?.btnHindi?.setOnClickListener {
            changeLanguage("hi")
        }

        binding?.btnTamil?.setOnClickListener {
            changeLanguage("ta")
        }

        binding?.btnBengali?.setOnClickListener {
            changeLanguage("bn")
        }

        binding?.btnKannada?.setOnClickListener {
            changeLanguage("kn")
        }

        binding?.btnMarathi?.setOnClickListener {
            changeLanguage("mr")
        }

        binding?.btnTelugu?.setOnClickListener {
            changeLanguage("te")
        }

        binding?.ivClose?.setOnClickListener {
            dismiss()
        }
    }

    private fun changeLanguage(languageCode: String) {
        lifecycleScope.launch {
            recordClickEvent(ConstantEventNames.SET_LANGUAGE, hashMapOf(Pair(ConstantEventAttributes.LANGUAGE, languageCode)))
            context?.let { languageManager.setLocale(it, languageCode) }
            activity?.recreate()
            dismiss()
        }
    }

    private fun markSelected(languageCode: String) {
        // Define the selected drawable and unselected background
        val selectedDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_language_selected)
        val selectedBackground = R.drawable.rounded_border_edittext_background_selected_100
        val unselectedBackground = R.drawable.rounded_border_edittext_background_100

        // Function to set the drawable and background for selected and unselected states
        fun setDrawableAndBackground(textView: TextView?, leftDrawableId: Int, show: Boolean) {
            textView?.let {
                // Set the left drawable dynamically based on language
                val leftDrawable = ContextCompat.getDrawable(requireContext(), leftDrawableId)
                val drawableTop = it.compoundDrawables[1] // Preserve top drawable
                val drawableBottom = it.compoundDrawables[3] // Preserve bottom drawable

                // Set the right drawable only if 'show' is true, otherwise remove it
                val drawableRight = if (show) selectedDrawable else null
                it.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, drawableTop, drawableRight, drawableBottom)

                // Set the background depending on the 'show' flag (selected or not)
                it.setBackgroundResource(if (show) selectedBackground else unselectedBackground)
            }
        }

        // Apply logic based on selected language using ViewBinding references and specific drawable IDs
        when (languageCode) {
            "en" -> {
                setDrawableAndBackground(binding?.btnEnglish, R.drawable.ic_en, true)
                setDrawableAndBackground(binding?.btnHindi, R.drawable.ic_hi, false)
                setDrawableAndBackground(binding?.btnTamil, R.drawable.ic_ta, false)
                setDrawableAndBackground(binding?.btnKannada, R.drawable.ic_kn, false)
                setDrawableAndBackground(binding?.btnTelugu, R.drawable.ic_te, false)
                setDrawableAndBackground(binding?.btnMarathi, R.drawable.ic_mr, false)
                setDrawableAndBackground(binding?.btnBengali, R.drawable.ic_bn, false)
            }
            "hi" -> {
                setDrawableAndBackground(binding?.btnEnglish, R.drawable.ic_en, false)
                setDrawableAndBackground(binding?.btnHindi, R.drawable.ic_hi, true)
                setDrawableAndBackground(binding?.btnTamil, R.drawable.ic_ta, false)
                setDrawableAndBackground(binding?.btnKannada, R.drawable.ic_kn, false)
                setDrawableAndBackground(binding?.btnTelugu, R.drawable.ic_te, false)
                setDrawableAndBackground(binding?.btnMarathi, R.drawable.ic_mr, false)
                setDrawableAndBackground(binding?.btnBengali, R.drawable.ic_bn, false)
            }
            "ta" -> {
                setDrawableAndBackground(binding?.btnEnglish, R.drawable.ic_en, false)
                setDrawableAndBackground(binding?.btnHindi, R.drawable.ic_hi, false)
                setDrawableAndBackground(binding?.btnTamil, R.drawable.ic_ta, true)
                setDrawableAndBackground(binding?.btnKannada, R.drawable.ic_kn, false)
                setDrawableAndBackground(binding?.btnTelugu, R.drawable.ic_te, false)
                setDrawableAndBackground(binding?.btnMarathi, R.drawable.ic_mr, false)
                setDrawableAndBackground(binding?.btnBengali, R.drawable.ic_bn, false)
            }
            "kn" -> {
                setDrawableAndBackground(binding?.btnEnglish, R.drawable.ic_en, false)
                setDrawableAndBackground(binding?.btnHindi, R.drawable.ic_hi, false)
                setDrawableAndBackground(binding?.btnTamil, R.drawable.ic_ta, false)
                setDrawableAndBackground(binding?.btnKannada, R.drawable.ic_kn, true)
                setDrawableAndBackground(binding?.btnTelugu, R.drawable.ic_te, false)
                setDrawableAndBackground(binding?.btnMarathi, R.drawable.ic_mr, false)
                setDrawableAndBackground(binding?.btnBengali, R.drawable.ic_bn, false)
            }
            "te" -> {
                setDrawableAndBackground(binding?.btnEnglish, R.drawable.ic_en, false)
                setDrawableAndBackground(binding?.btnHindi, R.drawable.ic_hi, false)
                setDrawableAndBackground(binding?.btnTamil, R.drawable.ic_ta, false)
                setDrawableAndBackground(binding?.btnKannada, R.drawable.ic_kn, false)
                setDrawableAndBackground(binding?.btnTelugu, R.drawable.ic_te, true)
                setDrawableAndBackground(binding?.btnMarathi, R.drawable.ic_mr, false)
                setDrawableAndBackground(binding?.btnBengali, R.drawable.ic_bn, false)
            }
            "mr" -> {
                setDrawableAndBackground(binding?.btnEnglish, R.drawable.ic_en, false)
                setDrawableAndBackground(binding?.btnHindi, R.drawable.ic_hi, false)
                setDrawableAndBackground(binding?.btnTamil, R.drawable.ic_ta, false)
                setDrawableAndBackground(binding?.btnKannada, R.drawable.ic_kn, false)
                setDrawableAndBackground(binding?.btnTelugu, R.drawable.ic_te, false)
                setDrawableAndBackground(binding?.btnMarathi, R.drawable.ic_mr, true)
                setDrawableAndBackground(binding?.btnBengali, R.drawable.ic_bn, false)
            }
            "bn" -> {
                setDrawableAndBackground(binding?.btnEnglish, R.drawable.ic_en, false)
                setDrawableAndBackground(binding?.btnHindi, R.drawable.ic_hi, false)
                setDrawableAndBackground(binding?.btnTamil, R.drawable.ic_ta, false)
                setDrawableAndBackground(binding?.btnKannada, R.drawable.ic_kn, false)
                setDrawableAndBackground(binding?.btnTelugu, R.drawable.ic_te, false)
                setDrawableAndBackground(binding?.btnMarathi, R.drawable.ic_mr, false)
                setDrawableAndBackground(binding?.btnBengali, R.drawable.ic_bn, true)
            }
            else -> {
                // Default selection for English if the provided language code is invalid
                setDrawableAndBackground(binding?.btnEnglish, R.drawable.ic_en, true)
                setDrawableAndBackground(binding?.btnHindi, R.drawable.ic_hi, false)
                setDrawableAndBackground(binding?.btnTamil, R.drawable.ic_ta, false)
                setDrawableAndBackground(binding?.btnKannada, R.drawable.ic_kn, false)
                setDrawableAndBackground(binding?.btnTelugu, R.drawable.ic_te, false)
                setDrawableAndBackground(binding?.btnMarathi, R.drawable.ic_mr, false)
                setDrawableAndBackground(binding?.btnBengali, R.drawable.ic_bn, false)
            }
        }
    }
}