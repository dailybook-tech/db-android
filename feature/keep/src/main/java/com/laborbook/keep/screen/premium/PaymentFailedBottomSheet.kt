package com.laborbook.keep.screen.premium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.laborbook.keep.R
import com.laborbook.keep.databinding.BottomsheetPaymentFailedBinding

class PaymentFailedBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetPaymentFailedBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomsheetPaymentFailedBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val message = arguments?.getString(ARG_MESSAGE) ?: getString(R.string.payment_failed_try_again)
        val title = arguments?.getString(ARG_TITLE) ?: getString(R.string.payment_failed_title)

        binding?.apply {
            tvTitle.text = title
            tvMessage.text = message
            ivClose.setOnClickListener { dismiss() }
            btnTryAgain.setOnClickListener { dismiss() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PaymentFailedBottomSheet"
        private const val ARG_MESSAGE = "message"
        private const val ARG_TITLE = "title"

        @JvmStatic
        fun newInstance(
            message: String,
            title: String? = null
        ) = PaymentFailedBottomSheet().apply {
            arguments = Bundle().apply {
                putString(ARG_MESSAGE, message)
                title?.let { putString(ARG_TITLE, it) }
            }
        }
    }
}
