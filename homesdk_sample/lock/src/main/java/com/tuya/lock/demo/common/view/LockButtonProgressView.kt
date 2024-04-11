package com.tuya.lock.demo.common.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import com.tuya.lock.demo.R
import java.util.Timer
import java.util.TimerTask

/**
 *
 * Created by HuiYao on 2024/2/29
 */
class LockButtonProgressView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private var mBackPaint: Paint = Paint() // 背景

    private var mRingPaint: Paint = Paint() // 绘制画笔

    private var mRectF: RectF? = null // 绘制区域

    private var mRingRectF: RectF? = null // 绘制区域

    private var mProgress = 0 // 圆环进度(0-100)

    private var title: String? = null // 中心点文案

    private var paint: Paint = Paint()

    private var timer: Timer? = null
    private var timerTask: TimerTask? = null
    private var isClick = false

    companion object {
        const val LONELIEST_TIME = 300 //长按超过0.3秒，触发长按事件
    }

    private var clickCallback: ClickCallback? = null

    private var isEnabled = false

    init {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.LockButtonProgressView)

        // 初始化背景圆环画笔
        mBackPaint.style = Paint.Style.FILL // 填充
        mBackPaint.strokeCap = Paint.Cap.ROUND // 设置圆角
        mBackPaint.isAntiAlias = true // 设置抗锯齿
        mBackPaint.isDither = true // 设置抖动
        mBackPaint.color =
            typedArray.getColor(R.styleable.LockButtonProgressView_buttonBgColor, Color.RED)

        // 初始化进度圆环画笔
        mRingPaint.style = Paint.Style.STROKE // 只描边，不填充
        mRingPaint.strokeCap = Paint.Cap.ROUND // 设置圆角
        mRingPaint.isAntiAlias = true // 设置抗锯齿
        mRingPaint.isDither = true // 设置抖动
        mRingPaint.strokeWidth =
            typedArray.getDimension(R.styleable.LockButtonProgressView_progressWidth, 10f)
        mRingPaint.color =
            typedArray.getColor(
                R.styleable.LockButtonProgressView_progressColor,
                Color.WHITE
            )

        // 中心点文案
        title = typedArray.getString(R.styleable.LockButtonProgressView_buttonTitle)
        typedArray.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!TextUtils.isEmpty(title)) {
            paint.isAntiAlias = true
            paint.textSize = 40f
            paint.color = resources.getColor(R.color.white)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val viewWide = measuredWidth - paddingLeft - paddingRight
        val viewHigh = measuredHeight - paddingTop - paddingBottom
        val mRectLength = (viewWide.coerceAtMost(viewHigh) - mBackPaint.strokeWidth.coerceAtLeast(
            mRingPaint.strokeWidth
        )).toInt()
        val mRectL = paddingLeft + (viewWide - mRectLength) / 2
        val mRectT = paddingTop + (viewHigh - mRectLength) / 2
        val padding = 20
        mRectF = getRectBg(
            mRectL.toFloat(),
            mRectT.toFloat(),
            (mRectL + mRectLength).toFloat(),
            (mRectT + mRectLength).toFloat()
        )
        mRingRectF = getRingRectBg(
            (mRectL + padding).toFloat(),
            (mRectT + padding).toFloat(),
            (mRectL + mRectLength - padding).toFloat(),
            (mRectT + mRectLength - padding).toFloat()
        )
    }

    private fun getRectBg(left: Float, top: Float, right: Float, bottom: Float): RectF? {
        return RectF(left, top, right, bottom)
    }

    private fun getRingRectBg(left: Float, top: Float, right: Float, bottom: Float): RectF? {
        return RectF(left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawOval(mRectF!!, mBackPaint)
        if (!TextUtils.isEmpty(title)) {
            val viewWide = measuredWidth - paddingLeft - paddingRight
            val viewHeight = measuredHeight - paddingTop - paddingBottom
            drawTextCenterInVertical(canvas, viewWide / 2, viewHeight / 2, title, paint)
        }
        canvas.drawArc(mRingRectF!!, 275f, (360 * mProgress / 100).toFloat(), false, mRingPaint)
    }

    fun resetProgress(animTime: Long) {
        if (mProgress > 0) {
            val animator = ValueAnimator.ofInt(mProgress, 0)
            animator.addUpdateListener { animation: ValueAnimator ->
                mProgress = animation.animatedValue as Int
                invalidate()
            }
            animator.interpolator = LinearInterpolator()
            animator.setDuration(animTime)
            animator.start()
        }
    }

    /**
     * df
     * 设置当前进度
     *
     * @param progress 当前进度（0-100）
     */
    fun setProgress(progress: Int) {
        mProgress = progress
        post { invalidate() }
    }

    fun setTitle(title: String?) {
        this.title = title
        post { invalidate() }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        isEnabled = enabled
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return isClick
        if (event.action == MotionEvent.ACTION_DOWN) {
            //长按逻辑触发，isClick置为false，手指移开后，不触发点击事件
            timerTask = object : TimerTask() {
                override fun run() {
                    //长按逻辑触发，isClick置为false，手指移开后，不触发点击事件
                    isClick = false
                    mProgress += 2
                    if (mProgress <= 100) {
                        doLongPress(mProgress)
                    } else {
                        timerTask!!.cancel()
                        timer!!.cancel()
                    }
                }
            }
            isClick = true
            timer = Timer()
            timer!!.schedule(timerTask, LONELIEST_TIME.toLong(), 20)
        } else if (event.action == MotionEvent.ACTION_UP) {
            //没有触发长按逻辑，进行点击事件
            if (mProgress < 100) {
                resetProgress(500)
            }
            if (null != timerTask) {
                timerTask!!.cancel()
            }
            if (null != timer) {
                timer!!.cancel()
            }
        }
        return isClick
    }

    //回归主线程回调
    private fun doLongPress(progress: Int) {
        setProgress(progress)
        if (null != clickCallback && progress == 100) {
            clickCallback!!.doLongPress()
        }
    }

    fun addClickCallback(callback: ClickCallback?) {
        clickCallback = callback
    }


    interface ClickCallback {
        /**
         * 等于100的时候执行
         */
        fun doLongPress()
    }


    /**
     * 竖直居中绘制文字
     *
     * @param canvas
     * @param centerX
     * @param centerY
     * @param text
     * @param paint
     */
    private fun drawTextCenterInVertical(
        canvas: Canvas,
        centerX: Int,
        centerY: Int,
        text: String?,
        paint: Paint?
    ) {
        //获取文本的宽度，但是是一个比较粗略的结果
        val textWidth = paint!!.measureText(text)
        //文字度量
        val fontMetrics = paint.fontMetrics
        //得到基线的位置
        val baselineY = centerY + (fontMetrics.bottom - fontMetrics.top) / 2 - fontMetrics.bottom
        //绘制
        canvas.drawText(text!!, centerX - textWidth / 2, baselineY, paint)
    }
}