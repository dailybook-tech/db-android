package co.dailybook.keep.screen.premium

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import co.dailybook.keep.databinding.ItemPaywallReviewBinding

data class PaywallReviewItem(
    val name: String,
    val reviewText: String,
    val ratingStars: Int = 5,
    val avatarResId: Int? = null
)

class PaywallReviewAdapter(
    private val items: List<PaywallReviewItem>
) : RecyclerView.Adapter<PaywallReviewAdapter.ReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemPaywallReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ReviewViewHolder(
        private val binding: ItemPaywallReviewBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PaywallReviewItem) {
            binding.tvReviewName.text = item.name
            binding.tvReviewText.text = item.reviewText
            binding.tvReviewStars.text = "★".repeat(item.ratingStars.coerceIn(1, 5))
            
            // Set avatar image if provided
            item.avatarResId?.let {
                binding.ivReviewAvatar.setImageResource(it)
            }
        }
    }
}
