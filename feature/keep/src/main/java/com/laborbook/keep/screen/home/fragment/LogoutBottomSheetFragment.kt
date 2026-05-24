package com.laborbook.keep.screen.home.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.laborbook.base.BaseBottomsheetFragment
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.keep.database.AppDatabase
import com.laborbook.keep.databinding.FragmentLogoutBottomsheetBinding
import com.laborbook.keep.screen.addstaff.model.ContactDatabase
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class LogoutBottomSheetFragment : BaseBottomsheetFragment<FragmentLogoutBottomsheetBinding>() {

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
    ): FragmentLogoutBottomsheetBinding? {
        return FragmentLogoutBottomsheetBinding.inflate(inflater, container, false)
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
        setOnClickListeners()
    }

    private fun setOnClickListeners() {
        binding?.apply {
            ivClose.setOnClickListener {
                dismiss()
            }

            btnNo.setOnClickListener {
                dismiss()
            }

            btnYes.setOnClickListener {
                handleLogout()
            }
        }
    }

    private fun handleLogout() {
        lifecycleScope.launch {
            // Clear all local storage (DataStore)
            dataStoreManager.clearAllPreferences()
            
            // Clear Room databases
            val database = AppDatabase.getDatabase(requireContext())
            database.staffUserDao().deleteAllStaffs()
            database.attendanceUserDao().deleteAll()
            database.calendarItemDao().deleteAll()
            
            val contactDatabase = ContactDatabase.getDatabase(requireContext())
            contactDatabase.contactDao().deleteAllContacts()
            
            // Navigate to LoginActivity and clear back stack
            val intent = Intent(requireContext(), Class.forName("com.laborbook.auth.screen.login.view.LoginActivity")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            requireActivity().finishAffinity()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = LogoutBottomSheetFragment()
    }
}

