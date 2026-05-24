package com.laborbook.keep.screen.home.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.View.MeasureSpec
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val confettiParticles = mutableListOf<ConfettiParticle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isAnimating = false
    private var hasAnimated = false
    private var centerX = 0f
    private var centerY = 0f

    private val confettiColors = listOf(
        Color.parseColor("#FFB6C1"), // Pink
        Color.parseColor("#87CEEB"), // Blue
        Color.parseColor("#FFD700"), // Yellow
        Color.parseColor("#98FB98"), // Green
        Color.parseColor("#DDA0DD")  // Purple
    )

    data class ConfettiParticle(
        var startX: Float,
        var startY: Float,
        var x: Float,
        var y: Float,
        var size: Float,
        var color: Int,
        var rotation: Float,
        var rotationSpeed: Float,
        var velocityX: Float,
        var velocityY: Float,
        var angle: Float, // Angle for burst direction
        var distance: Float, // Distance from center
        var shapeType: Int // 0=circle, 1=square, 2=triangle, 3=star
    )

    fun startConfettiAnimation() {
        if (hasAnimated || isAnimating) return
        
        isAnimating = true
        hasAnimated = true
        
        // Calculate center point (middle of the view, slightly above center for better effect)
        centerX = width / 2f
        centerY = height / 3f // Slightly above center
        
        // Create confetti particles
        confettiParticles.clear()
        val particleCount = 60
        
        for (i in 0 until particleCount) {
            // Random angle for burst direction (360 degrees)
            val angle = Random.nextFloat() * 360f
            val angleRad = Math.toRadians(angle.toDouble())
            
            // Random distance for burst (particles spread outward)
            val distance = Random.nextFloat() * 200 + 50 // 50-250 pixels
            
            // Calculate initial velocity based on angle
            val speed = Random.nextFloat() * 15 + 10 // 10-25 pixels per frame
            val velocityX = (cos(angleRad) * speed).toFloat()
            val velocityY = (sin(angleRad) * speed).toFloat()
            
            val particle = ConfettiParticle(
                startX = centerX,
                startY = centerY,
                x = centerX,
                y = centerY,
                size = Random.nextFloat() * 16 + 6, // Size between 6-22
                color = confettiColors[Random.nextInt(confettiColors.size)],
                rotation = Random.nextFloat() * 360,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 15, // Faster rotation
                velocityX = velocityX,
                velocityY = velocityY,
                angle = angle,
                distance = distance,
                shapeType = Random.nextInt(4) // 0=circle, 1=square, 2=triangle, 3=star
            )
            confettiParticles.add(particle)
        }

        // Animate particles bursting outward
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500 // 1.5 seconds for burst effect
            interpolator = DecelerateInterpolator() // Slow down as they spread
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                updateParticles(progress)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    // Hide the confetti view after animation completes
                    visibility = View.GONE
                }
            })
        }
        animator.start()
    }

    private fun updateParticles(progress: Float) {
        confettiParticles.forEach { particle ->
            // Calculate position based on burst progress
            // Particles move outward from center, then slow down
            val easedProgress = 1f - (1f - progress) * (1f - progress) // Ease out
            
            // Update position based on velocity and progress
            particle.x = particle.startX + particle.velocityX * easedProgress * 20
            particle.y = particle.startY + particle.velocityY * easedProgress * 20
            
            // Add gravity effect (slight downward pull after initial burst)
            if (progress > 0.3f) {
                val gravityProgress = (progress - 0.3f) / 0.7f
                particle.y += gravityProgress * 30 // Slight downward drift
            }
            
            // Update rotation
            particle.rotation += particle.rotationSpeed
            
            // Add some random wiggle for more natural movement
            if (Random.nextFloat() < 0.1f) { // 10% chance
                particle.velocityX += (Random.nextFloat() - 0.5f) * 0.3f
                particle.velocityY += (Random.nextFloat() - 0.5f) * 0.3f
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (confettiParticles.isEmpty() || width == 0 || height == 0) return

        confettiParticles.forEach { particle ->
            // Only draw particles that are within or slightly outside the view bounds
            if (particle.y < height + 100 && particle.x > -50 && particle.x < width + 50 && particle.y > -50) {
                canvas.save()
                canvas.translate(particle.x, particle.y)
                canvas.rotate(particle.rotation)
                
                paint.color = particle.color
                // Fade out as particles move away from center
                val distanceFromCenter = sqrt(
                    (particle.x - centerX) * (particle.x - centerX) + 
                    (particle.y - centerY) * (particle.y - centerY)
                )
                val maxDistance = width.coerceAtLeast(height) * 0.8f
                val alpha = if (distanceFromCenter > maxDistance) {
                    (255 * (1 - (distanceFromCenter - maxDistance) / 100)).toInt().coerceIn(0, 255)
                } else {
                    255
                }
                paint.alpha = alpha
                
                // Draw different shapes based on shapeType
                when (particle.shapeType) {
                    0 -> {
                        // Circle
                        canvas.drawCircle(0f, 0f, particle.size / 2, paint)
                    }
                    1 -> {
                        // Square
                        val halfSize = particle.size / 2
                        canvas.drawRect(
                            -halfSize,
                            -halfSize,
                            halfSize,
                            halfSize,
                            paint
                        )
                    }
                    2 -> {
                        // Triangle
                        val halfSize = particle.size / 2
                        val path = android.graphics.Path().apply {
                            moveTo(0f, -halfSize) // Top point
                            lineTo(-halfSize, halfSize) // Bottom left
                            lineTo(halfSize, halfSize) // Bottom right
                            close()
                        }
                        canvas.drawPath(path, paint)
                    }
                    3 -> {
                        // Star (5-pointed)
                        val halfSize = particle.size / 2
                        val path = android.graphics.Path().apply {
                            val outerRadius = halfSize
                            val innerRadius = halfSize * 0.4f
                            val angle = Math.PI / 5 // 36 degrees
                            
                            for (i in 0 until 10) {
                                val radius = if (i % 2 == 0) outerRadius else innerRadius
                                val x = (cos(i * angle) * radius).toFloat()
                                val y = (sin(i * angle) * radius).toFloat()
                                if (i == 0) {
                                    moveTo(x, y)
                                } else {
                                    lineTo(x, y)
                                }
                            }
                            close()
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                
                canvas.restore()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!hasAnimated && width > 0 && height > 0) {
            post {
                startConfettiAnimation()
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Start animation when view gets proper size
        if (w > 0 && h > 0 && !hasAnimated && !isAnimating) {
            post {
                startConfettiAnimation()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Match the content height in FrameLayout
        val parent = parent
        if (parent is FrameLayout) {
            // Get the content LinearLayout (second child)
            val contentView = (parent as ViewGroup).getChildAt(1)
            if (contentView != null) {
                // Measure content first if not already measured
                if (contentView.measuredHeight == 0) {
                    contentView.measure(
                        MeasureSpec.makeMeasureSpec(
                            MeasureSpec.getSize(widthMeasureSpec),
                            MeasureSpec.EXACTLY
                        ),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                    )
                }
                // Match content height
                setMeasuredDimension(
                    MeasureSpec.getSize(widthMeasureSpec),
                    contentView.measuredHeight
                )
                return
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
