package com.laborbook.keep.screen.home.fragment

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.laborbook.base.BaseBottomsheetFragment
import com.laborbook.base.analytics.ConstantEventNames
import com.laborbook.keep.R
import com.laborbook.keep.databinding.FragmentReferFriendBottomsheetBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ReferFriendBottomSheetFragment : BaseBottomsheetFragment<FragmentReferFriendBottomsheetBinding>() {

    override val screenName: String
        get() = ConstantEventNames.REFER_A_FRIEND

    override fun onStart() {
        super.onStart()
        val dialog = dialog as BottomSheetDialog
        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as View
        val behavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        
        // Allow dismissing by tapping outside (no close icon)
        dialog.setCanceledOnTouchOutside(true)
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): FragmentReferFriendBottomsheetBinding? {
        return FragmentReferFriendBottomsheetBinding.inflate(inflater, container, false)
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
        setupClickListeners()
        startConfettiAnimation()
    }

    private fun startConfettiAnimation() {
        binding?.confettiView?.postDelayed({
            binding?.confettiView?.startConfettiAnimation()
        }, 100) // Small delay to ensure view is measured
    }

    private fun setupClickListeners() {
        binding?.btnShareWhatsapp?.setOnClickListener {
            shareOnWhatsApp()
            dismiss()
        }
    }

    private fun shareOnWhatsApp() {
        try {
            // Pre-filled message
            val shareMessage = getString(R.string.refer_friend_whatsapp_message)
            
            // Read the image from R.raw.share_image
            val inputStream = resources.openRawResource(com.laborbook.base.R.raw.share_image)
            val file = File(requireContext().cacheDir, "share_image.jpeg")
            val outputStream = FileOutputStream(file)

            // Copy the image data to the cache file
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Get the URI for the cached image file
            val imageUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_TEXT, shareMessage)
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.whatsapp")  // Set WhatsApp package name
            }

            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // WhatsApp is not installed
            android.widget.Toast.makeText(requireContext(), "WhatsApp is not installed on your device.", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            // Error handling for file I/O
            android.widget.Toast.makeText(requireContext(), "Failed to prepare image for sharing.", android.widget.Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(requireContext(), "Failed to share: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val TAG = "ReferFriendBottomSheetFragment"
        
        fun newInstance(): ReferFriendBottomSheetFragment {
            return ReferFriendBottomSheetFragment()
        }
    }
}

