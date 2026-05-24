package com.laborbook.keep.screen.premium

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.keep.R
import com.laborbook.keep.databinding.DialogSubscriptionSuccessBinding
import com.laborbook.keep.screen.premium.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class SubscriptionSuccessDialogFragment : DialogFragment() {

    private var _binding: DialogSubscriptionSuccessBinding? = null
    private val binding get() = _binding
    private val viewModel: SubscriptionViewModel by viewModel()
    private val dataStoreManager: DataStoreManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogSubscriptionSuccessBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews()
        refreshSubscriptionStatus()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.MATCH_PARENT
            )
            
            // Set status bar color
            statusBarColor = resources.getColor(R.color.background, null)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
        return dialog
    }

    private fun setupViews() {
        binding?.apply {
            ivClose.setOnClickListener {
                dismiss()
            }
            
            btnContinue.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun refreshSubscriptionStatus() {
        lifecycleScope.launch {
            try {
                // Add a small delay to allow backend to process subscription
                kotlinx.coroutines.delay(2000) // 2 seconds delay
                
                // Get user ID
                val userId = dataStoreManager.read(DataStoreManager.USER_ID, "").first()
                
                if (userId.isNotEmpty()) {
                    // Call API to get latest subscription status
                    viewModel.checkUserSubscriptionStatus(userId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Observe subscription state
        viewModel.subscriptionState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SubscriptionViewModel.SubscriptionState.UserSubscriptionLoaded -> {
                    // Subscription data refreshed successfully
                    // Pro status already updated in ViewModel
                }
                else -> {
                    // Handle other states if needed
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SubscriptionSuccessDialogFragment"

        @JvmStatic
        fun newInstance() = SubscriptionSuccessDialogFragment()
    }
}
