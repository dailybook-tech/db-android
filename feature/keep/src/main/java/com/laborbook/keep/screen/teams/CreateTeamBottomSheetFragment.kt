package com.laborbook.keep.screen.teams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.laborbook.base.BaseBottomsheetFragment
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.keep.databinding.FragmentCreateTeamBottomsheetBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class CreateTeamBottomSheetFragment : BaseBottomsheetFragment<FragmentCreateTeamBottomsheetBinding>() {

    override val screenName: String get() = ConstantEventNames.SETTINGS

    private val viewModel: TeamViewModel by sharedViewModel()
    private var editingTeamId: String? = null
    private var editingTeamName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingTeamId = arguments?.getString(ARG_TEAM_ID)
        editingTeamName = arguments?.getString(ARG_TEAM_NAME)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentCreateTeamBottomsheetBinding? {
        return FragmentCreateTeamBottomsheetBinding.inflate(inflater, container, false)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as BottomSheetDialog
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        BottomSheetBehavior.from(bottomSheet).state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editingTeamName?.let { binding?.etTeamName?.setText(it) }
        editingTeamId?.let { binding?.tvTitle?.setText(com.laborbook.keep.R.string.edit_profile) }

        binding?.etTeamName?.addTextChangedListener {
            binding?.btnSave?.isEnabled = it?.trim()?.isNotEmpty() == true
        }

        binding?.ivClose?.setOnClickListener { dismiss() }

        binding?.btnSave?.setOnClickListener {
            val name = binding?.etTeamName?.text?.toString()?.trim() ?: return@setOnClickListener
            if (editingTeamId != null) {
                viewModel.updateTeam(editingTeamId!!, name)
            } else {
                viewModel.createTeam(name)
            }
            dismiss()
        }
    }

    companion object {
        private const val ARG_TEAM_ID = "team_id"
        private const val ARG_TEAM_NAME = "team_name"

        fun newInstance(): CreateTeamBottomSheetFragment = CreateTeamBottomSheetFragment()

        fun newInstanceForEdit(teamId: String, teamName: String): CreateTeamBottomSheetFragment {
            return CreateTeamBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEAM_ID, teamId)
                    putString(ARG_TEAM_NAME, teamName)
                }
            }
        }
    }
}
