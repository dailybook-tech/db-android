package com.laborbook.keep.utils

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.laborbook.base.BaseConstants
import com.laborbook.base.datastore.DataStoreManager
import com.laborbook.keep.R
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class CoachMarkManager : KoinComponent {
    
    private val dataStore: DataStoreManager by inject()
    
    companion object {
        private val COACH_MARK_SHOWN_KEY = booleanPreferencesKey("edit_button_coach_mark_shown")
    }
    
    suspend fun shouldShowCoachMark(context: Context): Boolean {
        val isShown = dataStore.read(COACH_MARK_SHOWN_KEY, false).first()
        android.util.Log.d("CoachMarkManager", "shouldShowCoachMark: isShown = $isShown")
        return !isShown
    }
    
    suspend fun markCoachMarkAsShown(context: Context) {
        dataStore.write(COACH_MARK_SHOWN_KEY, true)
    }
    
    // Method to reset coach mark for testing (only works in debug builds)
    suspend fun resetCoachMark(context: Context) {
        if (BaseConstants.DEBUG) {
            dataStore.write(COACH_MARK_SHOWN_KEY, false)
            android.util.Log.d("CoachMarkManager", "resetCoachMark: Coach mark reset in debug mode")
        } else {
            android.util.Log.w("CoachMarkManager", "resetCoachMark: Attempted to reset coach mark in release mode - ignored")
        }
    }
    
    // Check if debug features are enabled
    fun isDebugMode(): Boolean {
        return BaseConstants.DEBUG
    }
    
    fun showEditButtonCoachMark(activity: Activity, targetView: View, onDismiss: () -> Unit) {
        android.util.Log.d("CoachMarkManager", "showEditButtonCoachMark: Starting coach mark display")
        // Create a simple popup window for the coach mark
        val popupView = LayoutInflater.from(activity).inflate(R.layout.coach_mark_simple, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Configure popup
        popupWindow.isFocusable = true
        popupWindow.isOutsideTouchable = true
        popupWindow.setBackgroundDrawable(null)
        
        // Set up dismiss listener
        popupWindow.setOnDismissListener {
            onDismiss()
        }
        
        // Set text
        val titleText = popupView.findViewById<TextView>(R.id.tv_title)
        val descriptionText = popupView.findViewById<TextView>(R.id.tv_description)
        val gotItButton = popupView.findViewById<TextView>(R.id.btn_got_it)
        
        titleText.text = activity.getString(R.string.edit_staff_details)
        descriptionText.text = activity.getString(R.string.edit_button_coach_mark_description)
        gotItButton.text = activity.getString(R.string.got_it)
        
        // Set up button click
        gotItButton.setOnClickListener {
            popupWindow.dismiss()
        }
        
        // Calculate position
        val location = IntArray(2)
        targetView.getLocationInWindow(location)
        
        // Show popup
        popupWindow.showAsDropDown(
            targetView,
            -popupView.width / 2 + targetView.width / 2,
            20
        )
    }
} 