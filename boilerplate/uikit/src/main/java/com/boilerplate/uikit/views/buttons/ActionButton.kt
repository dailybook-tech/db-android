package com.boilerplate.uikit.views.buttons

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.boilerplate.uikit.R
import com.google.android.material.button.MaterialButton

open class ActionButton : MaterialButton {

     var enabledBackgroundTintColor : Int? = null
     var disabledBackgroundTintColor : Int? = null
     var enabledTextColor : Int? = null
     var disabledTextColor : Int? = null
     var progressBarColor : Int? = null

    private var textBeforeLoading : String? = null

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ActionButton,defStyle,0)
        kotlin.runCatching {
            enabledBackgroundTintColor = typedArray.getColor(R.styleable.ActionButton_buttonEnabledBgColor,ContextCompat.getColor(context,R.color.button_enabled_bg_color))
            backgroundTintList = enabledTextColor?.let { ColorStateList.valueOf(it) }
            disabledBackgroundTintColor = typedArray.getColor(R.styleable.ActionButton_buttonDisabledBgColor,ContextCompat.getColor(context,R.color.button_disabled_bg_color))
            enabledTextColor = typedArray.getColor(R.styleable.ActionButton_buttonEnabledTextColor,ContextCompat.getColor(context,R.color.button_enabled_text_color))
            disabledTextColor = typedArray.getColor(R.styleable.ActionButton_buttonDisabledTextColor,ContextCompat.getColor(context,R.color.button_disabled_text_color))
            progressBarColor = typedArray.getColor(R.styleable.ActionButton_buttonProgressColor,ContextCompat.getColor(context,R.color.progress_bar_color))
        }
        typedArray.recycle()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ActionButton)
        kotlin.runCatching {
            enabledBackgroundTintColor = typedArray.getColor(R.styleable.ActionButton_buttonEnabledBgColor,ContextCompat.getColor(context,R.color.button_enabled_bg_color))
            disabledBackgroundTintColor = typedArray.getColor(R.styleable.ActionButton_buttonDisabledBgColor,ContextCompat.getColor(context,R.color.button_disabled_bg_color))
            enabledTextColor = typedArray.getColor(R.styleable.ActionButton_buttonEnabledTextColor,ContextCompat.getColor(context,R.color.button_enabled_text_color))
            disabledTextColor = typedArray.getColor(R.styleable.ActionButton_buttonDisabledTextColor,ContextCompat.getColor(context,R.color.button_disabled_text_color))
            progressBarColor = typedArray.getColor(R.styleable.ActionButton_buttonProgressColor,ContextCompat.getColor(context,R.color.progress_bar_color))
        }
        typedArray.recycle()
    }

    constructor(context: Context) : super(context) {}

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
         if(isEnabled) {
             backgroundTintList = enabledBackgroundTintColor?.let { ColorStateList.valueOf(it) }
             enabledTextColor?.let { setTextColor(ColorStateList.valueOf(it)) }
             strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context,R.color.present))
        }else{
             backgroundTintList =disabledBackgroundTintColor?.let { ColorStateList.valueOf(it) }
             disabledTextColor?.let { setTextColor(ColorStateList.valueOf(it)) }
             strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context,R.color.absent))
        }
    }

    fun setShowProgress(showProgress: Boolean?) {
        icon = if (showProgress == true) {
            maxLines = 1;
            ellipsize = TextUtils.TruncateAt.END
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            textBeforeLoading = text.toString()
            text = ""
            CircularProgressDrawable(context).apply {
                setStyle(CircularProgressDrawable.DEFAULT)
                progressBarColor?.let { setColorSchemeColors(it) }
                start()
            }
        } else {
            textBeforeLoading?.let {
                text = it
                textBeforeLoading = null
            }
            null
        }
        if (icon != null) { // callback to redraw button icon
            icon.callback = object : Drawable.Callback {
                override fun unscheduleDrawable(who: Drawable, what: Runnable) {
                }

                override fun invalidateDrawable(who: Drawable) {
                    invalidate()
                }

                override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
                }
            }
        }
    }
}