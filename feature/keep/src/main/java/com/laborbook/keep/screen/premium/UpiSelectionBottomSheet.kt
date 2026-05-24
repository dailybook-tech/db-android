package com.laborbook.keep.screen.premium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.laborbook.keep.databinding.BottomsheetUpiSelectionBinding

class UpiSelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetUpiSelectionBinding? = null
    private val binding get() = _binding
    private var selectedPackageName: String = ""
    private var onUpiSelectedListener: ((InstalledUpiApp) -> Unit)? = null
    private var installedUpiAppsList: List<InstalledUpiApp> = emptyList()
    private lateinit var adapter: UpiAppAdapter

    /** Set the list of UPI apps (from Razorpay.getAppsWhichSupportAutopayIntent) before showing. */
    fun setInstalledUpiApps(apps: List<InstalledUpiApp>) {
        installedUpiAppsList = apps
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = BottomsheetUpiSelectionBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        selectedPackageName = arguments?.getString(ARG_SELECTED_PACKAGE) ?: ""
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        if (installedUpiAppsList.isEmpty()) {
            dismiss()
            return
        }

        adapter = UpiAppAdapter(
            installedUpiAppsList,
            selectedPackageName
        ) { upiApp ->
            selectedPackageName = upiApp.packageName
            onUpiSelectedListener?.invoke(upiApp)
            dismiss()
        }

        binding?.rvUpiApps?.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@UpiSelectionBottomSheet.adapter
        }
    }

    fun setOnUpiSelectedListener(listener: (InstalledUpiApp) -> Unit) {
        onUpiSelectedListener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "UpiSelectionBottomSheet"
        private const val ARG_SELECTED_PACKAGE = "selected_package"

        @JvmStatic
        fun newInstance(selectedPackage: String = "") =
            UpiSelectionBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_PACKAGE, selectedPackage)
                }
            }
    }
}

