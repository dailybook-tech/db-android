package com.boilerplate.uikit.views.util

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import androidx.core.content.res.ResourcesCompat


object Utils {
    fun dpToPx(dp: Float, context: Context): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics).toInt()
    }

    fun spToPx(sp: Float, context: Context): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics).toInt()
    }

    fun getColor(context: Context, colorResId: Int): Int {

        //return ContextCompat.getColor(context, colorResId); // Doesn't seem to work for R.attr.colorPrimary
        val typedValue = TypedValue()
        val typedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(colorResId))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }

    fun getThemeAttrData(context: Context, attr : Int) : Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr,typedValue,true)
        return typedValue.data
    }

    fun getFont(context: Context,font : Int) : Typeface? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.resources.getFont(font)
        }else{
            ResourcesCompat.getFont(context, font)
        }
    }
}