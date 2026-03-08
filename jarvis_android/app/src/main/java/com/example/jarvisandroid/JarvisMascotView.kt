package com.example.jarvisandroid

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.os.Handler
import android.os.Looper
import kotlin.random.Random

class JarvisMascotView(context: Context, attrs: AttributeSet? = null)
    : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var eyeClosed = false
    private var mouthOpen = false
    private var isSpeaking = false

    private var emotion = "idle"

    private var eyeOffsetX = 0f
    private var eyeOffsetY = 0f
    
    private var pulseScale = 1.0f
    private var pulseDirection = 1
    private var rotationAngle = 0f

    private val handler = Handler(Looper.getMainLooper())

    init {
        startBlink()
        startIdleAnimation()
        startPulseAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val baseRadius = 80f * pulseScale

        // Draw Glow/Aura based on state
        glowPaint.style = Paint.Style.FILL
        when(emotion) {
            "happy" -> glowPaint.color = Color.argb(120, 120, 255, 120)
            "thinking" -> glowPaint.color = Color.argb(120, 255, 220, 120)
            "confused" -> glowPaint.color = Color.argb(120, 255, 150, 150)
            "alert" -> glowPaint.color = Color.argb(150, 255, 50, 50)
            "listening" -> glowPaint.color = Color.argb(180, 100, 200, 255)
            "speaking" -> glowPaint.color = Color.argb(150, 180, 120, 255)
            else -> glowPaint.color = Color.argb(100, 120, 170, 255)
        }
        
        // Add a secondary pulsating glow for active states
        if (emotion == "listening" || emotion == "thinking" || isSpeaking) {
            canvas.drawCircle(centerX, centerY, baseRadius + 25f, glowPaint)
        }
        canvas.drawCircle(centerX, centerY, baseRadius + 15f, glowPaint)

        // Draw Main Body
        when(emotion) {
            "happy" -> paint.color = Color.rgb(120, 255, 120)
            "thinking" -> paint.color = Color.rgb(255, 220, 120)
            "confused" -> paint.color = Color.rgb(255, 180, 180)
            "alert" -> paint.color = Color.rgb(255, 80, 80)
            "listening" -> paint.color = Color.rgb(100, 200, 255)
            "speaking" -> paint.color = Color.rgb(180, 120, 255)
            else -> paint.color = Color.rgb(120, 170, 255)
        }
        
        canvas.save()
        if (emotion == "thinking") {
            rotationAngle += 2f
            canvas.rotate(rotationAngle, centerX, centerY)
        }
        canvas.drawCircle(centerX, centerY, baseRadius, paint)
        canvas.restore()

        // Draw Eyes
        paint.color = Color.BLACK
        val eyeY = centerY - (20f * pulseScale)
        val eyeDist = 30f * pulseScale
        val eyeSize = 8f * pulseScale

        if (!eyeClosed) {
            // Expressive eyes based on emotion
            when(emotion) {
                "confused" -> {
                    canvas.drawCircle(centerX - eyeDist + eyeOffsetX, eyeY + eyeOffsetY - 5f, eyeSize, paint)
                    canvas.drawCircle(centerX + eyeDist + eyeOffsetX, eyeY + eyeOffsetY + 5f, eyeSize, paint)
                }
                "happy" -> {
                    val rectW = 20f * pulseScale
                    val rectH = 5f * pulseScale
                    canvas.drawRect(centerX - eyeDist - rectW/2, eyeY - rectH, centerX - eyeDist + rectW/2, eyeY, paint)
                    canvas.drawRect(centerX + eyeDist - rectW/2, eyeY - rectH, centerX + eyeDist + rectW/2, eyeY, paint)
                }
                else -> {
                    canvas.drawCircle(centerX - eyeDist + eyeOffsetX, eyeY + eyeOffsetY, eyeSize, paint)
                    canvas.drawCircle(centerX + eyeDist + eyeOffsetX, eyeY + eyeOffsetY, eyeSize, paint)
                }
            }
        } else {
            val rectWidth = 16f * pulseScale
            val rectHeight = 4f * pulseScale
            canvas.drawRect(centerX - eyeDist - rectWidth/2, eyeY - rectHeight/2,
                centerX - eyeDist + rectWidth/2, eyeY + rectHeight/2, paint)
            canvas.drawRect(centerX + eyeDist - rectWidth/2, eyeY - rectHeight/2,
                centerX + eyeDist + rectWidth/2, eyeY + rectHeight/2, paint)
        }

        // Draw Mouth
        paint.color = if (emotion == "alert") Color.BLACK else Color.rgb(200, 50, 50)
        if (mouthOpen || isSpeaking) {
            val mouthW = 15f * pulseScale
            val mouthH = if (isSpeaking) (15f + Random.nextFloat() * 10f) * pulseScale else 20f * pulseScale
            canvas.drawOval(centerX - mouthW, centerY + (10f * pulseScale),
                centerX + mouthW, centerY + (10f * pulseScale) + mouthH, paint)
        } else {
            val mouthW = 15f * pulseScale
            val mouthH = 2f * pulseScale
            canvas.drawRect(centerX - mouthW, centerY + (20f * pulseScale),
                centerX + mouthW, centerY + (20f * pulseScale) + mouthH, paint)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            performClick()
        }
        eyeOffsetX = (event.x - width/2) / 40f
        eyeOffsetY = (event.y - height/2) / 40f
        invalidate()
        return true
    }

    private fun startBlink() {
        handler.postDelayed({
            if (emotion != "listening") eyeClosed = true
            invalidate()
            handler.postDelayed({
                eyeClosed = false
                invalidate()
                startBlink()
            }, 150)
        }, 2000L + Random.nextLong(4000))
    }

    private fun startIdleAnimation() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (emotion == "idle") {
                    val shiftX = (Random.nextFloat() - 0.5f) * 8f
                    val shiftY = (Random.nextFloat() - 0.5f) * 8f
                    animate().translationX(shiftX).translationY(shiftY).setDuration(1000).start()
                }
                handler.postDelayed(this, 1500)
            }
        }, 1000)
    }

    private fun startPulseAnimation() {
        handler.post(object : Runnable {
            override fun run() {
                val speed = when(emotion) {
                    "listening" -> 0.04f
                    "thinking" -> 0.02f
                    "speaking" -> 0.03f
                    else -> 0.005f
                }
                
                pulseScale += speed * pulseDirection
                val maxScale = if (emotion == "listening") 1.2f else 1.1f
                val minScale = if (emotion == "listening") 0.9f else 0.95f
                
                if (pulseScale > maxScale) pulseDirection = -1
                if (pulseScale < minScale) pulseDirection = 1
                
                invalidate()
                handler.postDelayed(this, 30)
            }
        })
    }

    fun setSpeaking(speaking: Boolean) {
        isSpeaking = speaking
        if (speaking) {
            emotion = "speaking"
            startMouthAnimation()
        } else if (emotion == "speaking") {
            emotion = "idle"
        }
        invalidate()
    }

    private fun startMouthAnimation() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isSpeaking) {
                    mouthOpen = false
                    invalidate()
                    return
                }
                mouthOpen = !mouthOpen
                invalidate()
                handler.postDelayed(this, 120)
            }
        })
    }

    fun setEmotion(type: String) {
        emotion = type
        invalidate()
    }
}