package com.laborbook.base

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

fun EditText.toggleKeyboard(activity: Activity) {
    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = activity.currentFocus

    if (view != null && imm.isAcceptingText) {
        // Keyboard is open, so hide it
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    } else {
        // Keyboard is closed, so open it and focus on the EditText
        this.requestFocus()
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun EditText.hideKeyboard(activity: Activity) {
    try {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus

        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }catch (e: Exception){}
}

fun TextInputEditText.toggleKeyboard(activity: Activity) {
    val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val view = activity.currentFocus

    if (view != null && imm.isAcceptingText) {
        // Keyboard is open, so hide it
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    } else {
        // Keyboard is closed, so open it and focus on the EditText
        this.requestFocus()
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }
}

// Extension function to convert date format
fun String.toFormattedDate(): String {
    // Define the input and output date formats
    val inputFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    val outputFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // Parse the input date string to a Date object
    val date = inputFormatter.parse(this)

    // Format the Date object to the desired output format
    return date?.let { outputFormatter.format(it) } ?: ""
}

fun AppCompatTextView.setRandomLightCircleBackground() {
    // List of predefined colors
    val colors = listOf(
        0xFFFFE8D3.toInt(),
        0xFFFFE6EB.toInt(),
        0xFFEEFFFA.toInt(),
        0xFFF2F2F2.toInt(),
        0xFFF4F2FF.toInt(),
        0xFFE8EFF8.toInt(),
        0xFFF1F7FF.toInt()
    )

    // Select a random color from the list
    val color = colors[Random.nextInt(colors.size)]

    // Create a GradientDrawable
    val drawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    // Set the drawable as background
    this.background = drawable
}

fun Fragment.captureAndShareFullContent(
    toolbarView: View,
    llStatsView: View,
    llTableHeaderView: View,
    recyclerView: RecyclerView
) {
    try {
        // Capture toolbar, stats, and table header
        val toolbarBitmap = toolbarView.getBitmapWithoutChanges()
        val llStatsBitmap = llStatsView.getBitmapWithoutChanges()
        val llTableHeaderBitmap = llTableHeaderView.getBitmapWithoutChanges()

        // Capture RecyclerView's full content
        val recyclerViewBitmap = recyclerView.getBitmapFromRecyclerView()

        if (toolbarBitmap != null && llStatsBitmap != null && llTableHeaderBitmap != null && recyclerViewBitmap != null) {
            // Combine all bitmaps with background_secondary as the background
            val combinedBitmap = combineBitmapsWithBackground(
                com.boilerplate.uikit.R.color.background_secondary, toolbarBitmap, llStatsBitmap, llTableHeaderBitmap, recyclerViewBitmap
            )

            // Save and share the combined bitmap
            val uri = saveBitmapToMediaStore(combinedBitmap)
            if (uri != null) {
                shareScreenshot(uri)
            } else {
                Toast.makeText(requireContext(), "Failed to save screenshot.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Failed to capture all components.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun View.getBitmapWithoutChanges(): Bitmap? {
    return try {
        val bitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        this.draw(canvas)
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun RecyclerView.getBitmapFromRecyclerView(): Bitmap? {
    return try {
        val adapter = this.adapter ?: return null
        val itemCount = adapter.itemCount
        val paint = Paint()
        var height = 0
        val width = this.width

        // Get RecyclerView's background color
        val backgroundColor = (this.background as? ColorDrawable)?.color ?: Color.WHITE

        // Measure and combine bitmaps for all items
        val bitmaps = mutableListOf<Bitmap>()
        for (i in 0 until itemCount) {
            val holder = adapter.createViewHolder(this, adapter.getItemViewType(i))
            adapter.onBindViewHolder(holder, i)
            holder.itemView.measure(
                View.MeasureSpec.makeMeasureSpec(this.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            holder.itemView.layout(0, 0, holder.itemView.measuredWidth, holder.itemView.measuredHeight)

            val itemBitmap = Bitmap.createBitmap(holder.itemView.width, holder.itemView.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(itemBitmap)

            // Draw the background color
            canvas.drawColor(backgroundColor)

            // Draw the item view
            holder.itemView.draw(canvas)
            bitmaps.add(itemBitmap)
            height += holder.itemView.height
        }

        // Combine all item bitmaps into a single bitmap
        val combinedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val combinedCanvas = Canvas(combinedBitmap)

        var top = 0
        for (bitmap in bitmaps) {
            combinedCanvas.drawBitmap(bitmap, 0f, top.toFloat(), paint)
            top += bitmap.height
        }
        combinedBitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun Fragment.combineBitmapsWithBackground(backgroundColorRes: Int, vararg bitmaps: Bitmap): Bitmap {
    // Get the background color from resources
    val backgroundColor = ContextCompat.getColor(requireContext(), backgroundColorRes)

    // Calculate the total width and height of the combined bitmap
    val width = bitmaps[0].width
    val totalHeight = bitmaps.sumOf { it.height }

    // Create the combined bitmap with the total width and height
    val combinedBitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(combinedBitmap)

    // Draw the background color on the entire canvas
    canvas.drawColor(backgroundColor)

    // Draw each bitmap one below the other on the canvas
    var top = 0
    for (bitmap in bitmaps) {
        canvas.drawBitmap(bitmap, 0f, top.toFloat(), null)
        top += bitmap.height
    }
    return combinedBitmap
}

private fun Fragment.saveBitmapToMediaStore(bitmap: Bitmap): Uri? {
    return try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "full_page_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        uri?.let {
            requireContext().contentResolver.openOutputStream(it).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                requireContext().contentResolver.update(it, contentValues, null, null)
            }
        }
        uri
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun Fragment.shareScreenshot(uri: Uri) {
    // Use consistent refer friend message for all shares
    val shareMessage = getString(com.laborbook.base.R.string.refer_friend_whatsapp_message)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareMessage)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        startActivity(Intent.createChooser(intent, "Share Screenshot"))
    } catch (e: Exception) {
        Toast.makeText(requireContext(), "Error sharing screenshot.", Toast.LENGTH_SHORT).show()
    }
}

fun Fragment.shareLaborbookContentOnWhatsApp() {
    // Use consistent refer friend message for all WhatsApp shares
    val shareContent = getString(com.laborbook.base.R.string.refer_friend_whatsapp_message)
    // Read the image from R.raw.share_image
    val inputStream = resources.openRawResource(R.raw.share_image)
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
        putExtra(Intent.EXTRA_TEXT, shareContent)
        putExtra(Intent.EXTRA_STREAM, imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setPackage("com.whatsapp")  // Set WhatsApp package name
    }

    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // WhatsApp is not installed
        Toast.makeText(requireContext(), "WhatsApp is not installed on your device.", Toast.LENGTH_SHORT).show()
    } catch (e: IOException) {
        // Error handling for file I/O
        Toast.makeText(requireContext(), "Failed to prepare image for sharing.", Toast.LENGTH_SHORT).show()
    }
}

fun String.appendDotsAfterFirstTwelve(): String {
    return if (this.length > 12) {
        this.substring(0, 12) + "..."
    } else {
        this
    }
}

fun String.toReadableDate(): String {
    return try {
        // Parse the ISO date string to a Date object
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val date = isoFormat.parse(this)

        // Format the Date object to the desired "MMM dd, yyyy" format
        val readableFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        readableFormat.format(date ?: Date()) // Use the parsed date or the current date as fallback
    } catch (e: Exception) {
        // If parsing fails, return the original string
        this
    }
}