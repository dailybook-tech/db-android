package co.dailybook.keep.screen.teams

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import co.dailybook.boilerplate.uikit.views.hide
import co.dailybook.boilerplate.uikit.views.show
import co.dailybook.base.BaseFragment
import co.dailybook.base.analytics.ConstantEventNames
import co.dailybook.keep.databinding.FragmentTeamListBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class TeamListFragment : BaseFragment<FragmentTeamListBinding>() {

    override val screenName: String get() = ConstantEventNames.SETTINGS

    private val viewModel: TeamViewModel by viewModel()
    private lateinit var adapter: TeamAdapter

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): FragmentTeamListBinding? {
        return FragmentTeamListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        registerClickListeners()
        viewModel.loadTeams()
    }

    private fun setupRecyclerView() {
        adapter = TeamAdapter(
            onEdit = { team ->
                CreateTeamBottomSheetFragment.newInstanceForEdit(team.id, team.name)
                    .show(parentFragmentManager, "EditTeam")
            },
            onDelete = { team ->
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Team")
                    .setMessage("Delete \"${team.name}\"? Workers assigned to this team will be unassigned.")
                    .setPositiveButton("Delete") { _, _ -> viewModel.deleteTeam(team.id) }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        binding?.rvTeams?.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is TeamUiState.Loading -> {
                    binding?.pb?.show()
                    binding?.tvEmpty?.hide()
                }
                is TeamUiState.ListSuccess -> {
                    binding?.pb?.hide()
                    if (state.teams.isEmpty()) {
                        binding?.tvEmpty?.show()
                        binding?.rvTeams?.hide()
                    } else {
                        binding?.tvEmpty?.hide()
                        binding?.rvTeams?.show()
                        adapter.submitList(state.teams)
                    }
                }
                is TeamUiState.CreateSuccess, is TeamUiState.UpdateSuccess, is TeamUiState.DeleteSuccess -> {
                    binding?.pb?.hide()
                    viewModel.loadTeams()
                }
                is TeamUiState.Error -> {
                    binding?.pb?.hide()
                    Toast.makeText(requireContext(), state.message ?: "Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun registerClickListeners() {
        binding?.ivBack?.setOnClickListener { fragmentNavigator.goBack() }
        binding?.btnAddTeam?.setOnClickListener {
            CreateTeamBottomSheetFragment.newInstance()
                .show(parentFragmentManager, "CreateTeam")
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TeamListFragment()
    }
}
