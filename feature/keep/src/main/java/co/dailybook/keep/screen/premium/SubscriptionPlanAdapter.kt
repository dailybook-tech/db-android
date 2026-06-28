package co.dailybook.keep.screen.premium

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import co.dailybook.keep.R
import co.dailybook.keep.databinding.ItemSubscriptionPlanBinding
import co.dailybook.keep.model.subscription.SubscriptionPlan

class SubscriptionPlanAdapter(
    private val plans: List<SubscriptionPlan>,
    private var selectedPosition: Int = 0,
    private val onPlanSelected: (SubscriptionPlan, Int) -> Unit
) : RecyclerView.Adapter<SubscriptionPlanAdapter.PlanViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val binding = ItemSubscriptionPlanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        holder.bind(plans[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = plans.size

    fun updateSelection(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    inner class PlanViewHolder(
        private val binding: ItemSubscriptionPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(plan: SubscriptionPlan, isSelected: Boolean) {
            binding.apply {
                val context = root.context
                
                // Set plan name (e.g., "Monthly", "Yearly")
                tvPlanName.text = plan.interval.replaceFirstChar { it.uppercase() }
                
                // Show discount badge if there's a discount
                if (plan.hasDiscount) {
                    tvDiscountBadge.visibility = android.view.View.VISIBLE
                    tvDiscountBadge.text = "${plan.discountPercent}% OFF"
                } else {
                    tvDiscountBadge.visibility = android.view.View.GONE
                }
                
                // Calculate weekly breakdown
                val finalPrice = if (plan.hasDiscount) plan.discountedPrice else plan.price
                val weeksInPeriod = when (plan.interval.lowercase()) {
                    "yearly", "year" -> 52
                    "monthly", "month" -> 4
                    "quarterly", "quarter" -> 13
                    "weekly", "week" -> 1
                    else -> 4 // default to monthly
                }
                val weeklyPrice = finalPrice / weeksInPeriod
                
                // Format breakdown text: "₹X/weekly, billed [interval] at ₹Y"
                val intervalText = when (plan.interval.lowercase()) {
                    "yearly", "year" -> "yearly"
                    "monthly", "month" -> "monthly"
                    "quarterly", "quarter" -> "quarterly"
                    "weekly", "week" -> "weekly"
                    else -> plan.interval.lowercase()
                }
                tvPlanBreakdown.text = "₹$weeklyPrice/weekly, billed $intervalText at ₹$finalPrice"
                
                // Show/hide selection checkmark
                ivSelectionCheck.visibility = if (isSelected) android.view.View.VISIBLE else android.view.View.GONE
                
                // Update selection state: selected = white border, unselected = gray border
                if (isSelected) {
                    clPlanRoot.setBackgroundResource(R.drawable.plan_selected_border)
                } else {
                    clPlanRoot.setBackgroundResource(R.drawable.plan_unselected_border)
                }
                
                // Click listener
                root.setOnClickListener {
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onPlanSelected(plan, adapterPosition)
                        updateSelection(adapterPosition)
                    }
                }
            }
        }
    }
}
