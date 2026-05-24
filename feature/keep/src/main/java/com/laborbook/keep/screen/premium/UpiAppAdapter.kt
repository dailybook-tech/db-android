package com.laborbook.keep.screen.premium

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.boilerplate.uikit.views.hide
import com.boilerplate.uikit.views.show
import com.laborbook.keep.databinding.ItemUpiAppBinding

class UpiAppAdapter(
    private val installedApps: List<InstalledUpiApp>,
    private var selectedPackageName: String,
    private val onUpiSelected: (InstalledUpiApp) -> Unit
) : RecyclerView.Adapter<UpiAppAdapter.UpiAppViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpiAppViewHolder {
        val binding = ItemUpiAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UpiAppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UpiAppViewHolder, position: Int) {
        holder.bind(installedApps[position])
    }

    override fun getItemCount(): Int = installedApps.size

    inner class UpiAppViewHolder(
        private val binding: ItemUpiAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(upiApp: InstalledUpiApp) {
            val ctx = binding.root.context
            binding.apply {
                tvUpiName.text = upiApp.displayName
                if (upiApp.isInstalled && upiApp.icon != null) {
                    ivUpiIcon.visibility = android.view.View.VISIBLE
                    tvUpiLetter.visibility = android.view.View.GONE
                    ivUpiIcon.setImageDrawable(upiApp.icon)
                } else {
                    ivUpiIcon.visibility = android.view.View.GONE
                    tvUpiLetter.visibility = android.view.View.VISIBLE
                    val firstChar = upiApp.displayName.firstOrNull()?.uppercaseChar() ?: '?'
                    tvUpiLetter.text = firstChar.toString()
                }
                if (upiApp.packageName == selectedPackageName && upiApp.isInstalled) {
                    ivSelected.show()
                } else {
                    ivSelected.hide()
                }
                root.isEnabled = upiApp.isInstalled
                root.alpha = if (upiApp.isInstalled) 1f else 0.5f
                root.setOnClickListener {
                    if (!upiApp.isInstalled) return@setOnClickListener
                    val previousPackage = selectedPackageName
                    selectedPackageName = upiApp.packageName
                    notifyItemChanged(installedApps.indexOfFirst { it.packageName == previousPackage })
                    notifyItemChanged(installedApps.indexOfFirst { it.packageName == selectedPackageName })
                    onUpiSelected(upiApp)
                }
            }
        }
    }
}

