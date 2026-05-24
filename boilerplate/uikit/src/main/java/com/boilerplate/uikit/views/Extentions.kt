package com.boilerplate.uikit.views

import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import com.boilerplate.uikit.views.util.AnimationType


// Extension function to zoom out and hide the view
fun View.invisible(duration: Long = 300, animationType: String = AnimationType.NONE) {
    if (this.visibility == View.VISIBLE) {
        if(animationType == AnimationType.ZOOM) {
            val scaleAnimation = ScaleAnimation(
                1f, 0f, // Start and end values for the X axis scaling
                1f, 0f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f // Pivot point of Y scaling
            ).apply {
                fillAfter = true // Needed to keep the result of the animation
                this.duration = duration
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}

                    override fun onAnimationEnd(animation: Animation) {
                        this@invisible.visibility = View.INVISIBLE
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
            }
            this.startAnimation(scaleAnimation)
        } else if(animationType == AnimationType.NONE) {
            this.visibility = View.INVISIBLE
        }
    }
}

// Extension function to zoom out and hide the view
fun View.hide(duration: Long = 300, animationType: String = AnimationType.NONE) {
    if (this.visibility == View.VISIBLE) {
        if(animationType == AnimationType.ZOOM) {
            val scaleAnimation = ScaleAnimation(
                1f, 0f, // Start and end values for the X axis scaling
                1f, 0f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f // Pivot point of Y scaling
            ).apply {
                fillAfter = true // Needed to keep the result of the animation
                this.duration = duration
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}

                    override fun onAnimationEnd(animation: Animation) {
                        this@hide.visibility = View.GONE
                    }

                    override fun onAnimationRepeat(animation: Animation) {}
                })
            }
            this.startAnimation(scaleAnimation)
        } else if(animationType == AnimationType.NONE) {
            this.visibility = View.GONE
        }
    }
}

// Extension function to zoom in and show the view
fun View.show(duration: Long = 300, animationType: String = AnimationType.NONE) {
    if (this.visibility == View.GONE || this.visibility == View.INVISIBLE) {
        if(animationType == AnimationType.ZOOM) {
            val scaleAnimation = ScaleAnimation(
                0f, 1f, // Start and end values for the X axis scaling
                0f, 1f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0.5f // Pivot point of Y scaling
            ).apply {
                fillAfter = true // Needed to keep the result of the animation
                this.duration = duration
                setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                        this@show.visibility = View.VISIBLE
                    }

                    override fun onAnimationEnd(animation: Animation) {}

                    override fun onAnimationRepeat(animation: Animation) {}
                })
            }
            this.startAnimation(scaleAnimation)
        } else if (animationType == AnimationType.NONE){
            this.visibility = View.VISIBLE
        }
    }
}
