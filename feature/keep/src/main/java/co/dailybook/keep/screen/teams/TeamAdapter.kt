package co.dailybook.keep.screen.teams

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.dailybook.keep.databinding.ItemTeamBinding
import co.dailybook.keep.model.TeamResponse

class TeamAdapter(
    private val onEdit: (TeamResponse) -> Unit,
    private val onDelete: (TeamResponse) -> Unit
) : ListAdapter<TeamResponse, TeamAdapter.TeamViewHolder>(DiffCallback()) {

    inner class TeamViewHolder(private val binding: ItemTeamBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(team: TeamResponse) {
            binding.tvTeamName.text = team.name
            binding.ivEdit.setOnClickListener { onEdit(team) }
            binding.ivDelete.setOnClickListener { onDelete(team) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        return TeamViewHolder(
            ItemTeamBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<TeamResponse>() {
        override fun areItemsTheSame(oldItem: TeamResponse, newItem: TeamResponse) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TeamResponse, newItem: TeamResponse) =
            oldItem == newItem
    }
}
