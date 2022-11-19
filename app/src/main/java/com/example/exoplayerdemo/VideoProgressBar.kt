package com.example.exoplayerdemo

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.abs


class VideoProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

   var progress: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

  var listener: Listener? = null

  private val bgPaint = Paint()
  private val paint = Paint()
  private val lineHeight = dp2px(2f)
  private val blockHeight = dp2px(13f)

    init {
        bgPaint.color = Color.WHITE
        bgPaint.alpha = 127
        bgPaint.isAntiAlias = true
        paint.color = ContextCompat.getColor(context, R.color.purple_200)
        paint.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        var drawProgress = if (isDrag) {
            dragProgress
        } else {
            progress
        }
        canvas?.drawRoundRect(
            0f,
            (measuredHeight / 2 - lineHeight / 2),
            measuredWidth.toFloat(),
            (measuredHeight / 2 + lineHeight / 2),
            (lineHeight / 2),
            (lineHeight / 2), bgPaint
        )
        canvas?.drawRoundRect(
            0f,
            (measuredHeight / 2 - lineHeight / 2),
            (measuredWidth.toFloat() - lineHeight * 2) * drawProgress,
            (measuredHeight / 2 + lineHeight / 2),
            (lineHeight / 2),
            (lineHeight / 2), paint
        )
        if (!isEnabled) {
            return
        }
        if (outerDragProgress >= 0) {
            drawProgress = outerDragProgress
        }
        canvas?.drawCircle(
            (measuredWidth.toFloat() - blockHeight.toFloat()) * drawProgress +
                    blockHeight.toFloat() / 2,
            measuredHeight.toFloat() / 2,
            blockHeight.toFloat() / 2,
            paint
        )
    }

    var outerDragProgress = -1f
    private var dragProgress = -100f
    private var downTimeStamp = 0L
    private var downProgress = -100f
    private var isDrag = false

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downTimeStamp = System.currentTimeMillis()
                downProgress = event.x / measuredWidth.toFloat()
                isDrag = abs(downProgress - progress) < 0.05
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDrag) {
                    dragProgress = event.x / measuredWidth.toFloat()
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (isDrag) {
                    if (System.currentTimeMillis() - downTimeStamp > 500) {
                        progress = adjustProgress(dragProgress)
                        listener?.onChangeProgress(progress)
                    }
                } else {
                    if (System.currentTimeMillis() - downTimeStamp < 500) {
                        progress = adjustProgress(downProgress)
                        listener?.onChangeProgress(progress)
                    }
                }
                isDrag = false
            }
        }
        return true
    }

    private fun adjustProgress(adjustProgress: Float): Float {
        if (adjustProgress < 0) {
            return 0f
        }
        if (adjustProgress > 1f) {
            return 1f
        }
        return adjustProgress
    }

    interface Listener {
        /**
         * progress is in 0f ~ 1f
         * */
        fun onChangeProgress(progress: Float)
    }

    private fun dp2px(dpValue: Float): Float {
        return 0.5f + dpValue * Resources.getSystem().displayMetrics.density
    }
}
