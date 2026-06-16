package com.dailybook.expense.screen.reports.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.dailybook.expense.databinding.ItemTransactionReportBinding
import com.dailybook.expense.model.Transaction
import com.dailybook.expense.util.Constants
import com.dailybook.base.toReadableDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransactionReportsAdapter : ListAdapter<Transaction, TransactionReportViewHolder>(TransactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionReportViewHolder {
        val binding = ItemTransactionReportBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionReportViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TransactionReportViewHolder(
    private val binding: ItemTransactionReportBinding
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

    fun bind(transaction: Transaction) {
        // Extract day number and day of week separately (vertical format)
        val dayNumber = extractDayNumber(transaction.date)
        val dayOfWeek = extractDayOfWeek(transaction.date)
        
        binding.tvDate.text = dayNumber
        binding.tvDay.text = dayOfWeek

        // Notes
        binding.tvNotes.text = transaction.reason

        // Amount with color
        val amountText = "₹ ${transaction.amount}"
        binding.tvAmount.text = amountText

        // Set color based on transaction type
        val amountColor = if (transaction.type == Constants.DEBIT) {
            binding.root.context.getColor(com.dailybook.expense.R.color.error_state_color)
        } else {
            binding.root.context.getColor(com.dailybook.expense.R.color.button_green_color)
        }
        binding.tvAmount.setTextColor(amountColor)
    }

    private fun extractDayNumber(dateString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val date = sdf.parse(dateString)
            val calendar = Calendar.getInstance()
            date?.let { calendar.time = it }
            calendar.get(Calendar.DAY_OF_MONTH).toString()
        } catch (e: Exception) {
            // Fallback: try to extract from dateStr if available, otherwise use readable date
            dateString.toReadableDate()
        }
    }
    
    private fun extractDayOfWeek(dateString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            val date = sdf.parse(dateString)
            val calendar = Calendar.getInstance()
            date?.let { calendar.time = it }
            val dayName = calendar.getDisplayName(
                Calendar.DAY_OF_WEEK,
                Calendar.SHORT,
                Locale.getDefault()
            )
            dayName?.take(3) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}

class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }
}

