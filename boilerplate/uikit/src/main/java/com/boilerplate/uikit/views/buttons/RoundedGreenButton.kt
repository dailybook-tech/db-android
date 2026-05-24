package com.boilerplate.uikit.views.buttons

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.boilerplate.uikit.R
import com.boilerplate.uikit.views.util.Utils

class RoundedGreenButton : ActionButton {

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

    init {
        isAllCaps = false
        elevation = 0f
        stateListAnimator = null
        cornerRadius = Utils.dpToPx(100f,context)
        insetBottom = 0
        insetTop = 0
        typeface = Utils.getFont(context,R.font.inter_bold)

        backgroundTintList = ColorStateList.valueOf(Utils.getThemeAttrData(context,R.attr.buttonGreenColor))
        setTextColor(Utils.getThemeAttrData(context, R.attr.buttonEnabledTextColor))
        enabledBackgroundTintColor =  Utils.getThemeAttrData(context,R.attr.buttonGreenColor)
        disabledBackgroundTintColor = Utils.getThemeAttrData(context,R.attr.buttonDisabledBgColor)
        enabledTextColor = Utils.getThemeAttrData(context,R.attr.buttonEnabledTextColor)
        disabledTextColor =  Utils.getThemeAttrData(context,R.attr.buttonDisabledTextColor)

        rippleColor = ColorStateList.valueOf(ContextCompat.getColor(context,R.color.ripple))

    }
}