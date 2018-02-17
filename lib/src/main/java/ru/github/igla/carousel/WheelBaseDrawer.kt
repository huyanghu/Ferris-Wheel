package ru.github.igla.carousel

import android.content.Context
import android.graphics.*
import kotlin.math.cos
import kotlin.math.sin


/**
 * Created by igor-lashkov on 11/01/2018.
 */

internal class WheelBaseDrawer(val context: Context, private val config: WheelViewConfig) {

    private val PILL_ANGLE_FROM = 110.0
    private val PILL_ANGLE_TO = 70.0

    val MIN_RADIUS: Double by lazyNonSafe { (context.dpF(100f)).toDouble() }

    var radius = 0.0
        set(value) {
            field = if (value < MIN_RADIUS) MIN_RADIUS else value
        }

    val centerPoint by lazyNonSafe { PointF() }

    private var drawCenterPoint: PointF? = null
    var rotateAngle = 0f


    private val baseGroundPaint by lazyNonSafe {
        smoothPaint(config.rimColor).apply {
            style = Paint.Style.FILL
        }
    }

    private val patternPaint by lazyNonSafe {
        smoothPaint(config.wheelColor).apply {
            strokeWidth = context.dpF(2f)
            style = Paint.Style.STROKE
        }
    }

    private val innerCirclePaint by lazyNonSafe {
        smoothPaint(config.wheelColor).apply {
            strokeWidth = dp6
            style = Paint.Style.STROKE
        }
    }

    private val pillLinePaint by lazyNonSafe {
        smoothPaint(config.rimColor).apply {
            strokeWidth = context.dpF(8f)
            style = Paint.Style.STROKE
        }
    }

    private val circleOuterPaint by lazyNonSafe {
        smoothPaint(config.wheelColor).apply {
            strokeWidth = dp6
            style = Paint.Style.STROKE
        }
    }

    private val circleInnerPaint by lazyNonSafe {
        smoothPaint(config.wheelColor).apply {
            strokeWidth = context.dpF(4f)
            style = Paint.Style.STROKE
        }
    }

    private val circleInnerPaint2 by lazyNonSafe {
        smoothPaint(Color.parseColor("#96D0B6")).apply {
            style = Paint.Style.FILL
        }
    }

    private val dp6 by lazyNonSafe { context.dpF(6f) }
    private val dp14 by lazyNonSafe { context.dpF(14f) }
    private val dp16 by lazyNonSafe { context.dpF(16f) }
    private val dp56 by lazyNonSafe { context.dpF(56f) }
    private val dp28 by lazyNonSafe { context.dpF(28f) }
    private val dp32 by lazyNonSafe { context.dpF(32f) }
    private val dp34 by lazyNonSafe { context.dpF(34f) }
    private val dp10 by lazyNonSafe { context.dpF(10f).toDouble() }

    private val pillLeftStart1 = PointF()
    private val pillRightStart2 = PointF()

    private val pillLeftEnd1 = PointF()
    private val pillRightEnd2 = PointF()

    private val pillGroundBlock1 = PointF()
    private val pillGroundBlock2 = PointF()

    private val roundRect = RectF()


    private val patternStep = 3
    private val patternPoints = 360 / (patternStep * 2)

    private val patternPoints1 = Array(patternPoints + 1) { PointF() }
    private val patternPoints2 = Array(patternPoints + 1) { PointF() }


    private val paintStar by lazyNonSafe {
        smoothPaint(getColorRes(context, R.color.fwv_black)).apply {
            style = Paint.Style.FILL
        }
    }
    private val pathStar by lazyNonSafe { Path() }

    private fun getGroundPadding(): Double = config.cabinSize + CABIN_TILT + dp10 + dp16

    private fun getPaddingOutside(): Double = dp6.toDouble()

    fun configure(rect: Rect) {
        val minAvailableWidth = MIN_RADIUS * 2.0 + config.cabinSize
        val minAvailableHeight = MIN_RADIUS * 2.0 + getGroundPadding()
        val parentWidth = rect.width()
        val parentHeight = rect.height()
        if (parentWidth < minAvailableWidth || parentHeight < minAvailableHeight) {
            //no space
            return
        }

        val centerX = parentWidth / 2.0f
        val centerY = (parentHeight - getGroundPadding().toFloat()) / 2.0f
        this.centerPoint.set(centerX, centerY)
        val minSize = minOf(centerX - config.cabinSize / 2f, centerY)
        this.radius = minSize - getPaddingOutside()
    }

    fun setPointPosAsWheel(outPoint: PointF, angle: Double) {
        setPointPos(outPoint, this.centerPoint, angle, this.radius)
    }

    fun onPostDraw(canvas: Canvas) {
        canvas.drawCircle(centerPoint, dp16, circleInnerPaint)
        canvas.drawCircle(centerPoint, dp14, circleInnerPaint2)

        drawStar(canvas)
    }

    fun onPreDraw(canvas: Canvas) {

        val radiusF = radius.toFloat()
        calcNewCenter(centerPoint, radiusF)

        canvas.apply {
            save()
            rotate(rotateAngle, centerPoint.x, centerPoint.y)

            drawCircle(centerPoint, dp56, innerCirclePaint)

            drawCircle(centerPoint, dp28, patternPaint)
            drawCircle(centerPoint, dp34, patternPaint)

            drawCircle(centerPoint, radiusF, circleOuterPaint)

            for (i in 0..patternPoints) {
                drawLine(patternPoints1[i], patternPoints2[i], patternPaint)
                if (i > 0) {
                    drawLine(patternPoints2[i], patternPoints1[i - 1], patternPaint)
                }
            }
            drawCircle(centerPoint, getPatternRadiusInner(radiusF).toFloat(), patternPaint)

            restore()

            drawLine(pillLeftStart1, pillLeftEnd1, pillLinePaint)
            drawLine(pillRightStart2, pillRightEnd2, pillLinePaint)

            drawRoundRect(
                    roundRect,
                    dp6,
                    dp6,
                    baseGroundPaint)
        }
    }

    private fun calcNewCenter(centerPoint: PointF, radius: Float) {
        if (centerPoint != this.drawCenterPoint) {
            this.drawCenterPoint = centerPoint

            setPointPos(pillLeftStart1, centerPoint, PILL_ANGLE_TO, dp16.toDouble())
            setPointPos(pillRightStart2, centerPoint, PILL_ANGLE_FROM, dp16.toDouble())

            val groundPoint = radius + getGroundPadding()
            setPointPos(pillLeftEnd1, centerPoint, PILL_ANGLE_TO, groundPoint)
            setPointPos(pillRightEnd2, centerPoint, PILL_ANGLE_FROM, groundPoint)

            setPointPos(pillGroundBlock1, centerPoint, PILL_ANGLE_TO, groundPoint - dp16)
            setPointPos(pillGroundBlock2, centerPoint, PILL_ANGLE_FROM, groundPoint - dp16)

            roundRect.set(
                    pillRightEnd2.x - dp28,
                    pillRightEnd2.y - dp6,
                    pillLeftEnd1.x + dp28,
                    pillLeftEnd1.y - dp6 + dp16
            )

            var angle1 = patternStep.toDouble()
            var angle2 = 0.0
            val outPatternRadius = getPatternRadiusOutter(radius)
            val innerPatternRadius = getPatternRadiusInner(radius)
            val stepBy = patternStep * 2.0
            for (i in 0..patternPoints) {
                setPointPos(patternPoints1[i], centerPoint, angle1, outPatternRadius)
                setPointPos(patternPoints2[i], centerPoint, angle2, innerPatternRadius)

                angle1 += stepBy
                angle2 += stepBy
            }
            calcStarPath(centerPoint, dp32)
        }
    }

    private fun calcStarPath(centerPoint: PointF, size: Float) {
        val half = size / 2f
        val fromX = centerPoint.x - half
        val fromY = centerPoint.y - half
        pathStar.apply {
            rewind()
            // top left
            moveTo(fromX + half * 0.5f, fromY + half * 0.84f)
            // top right
            lineTo(fromX + half * 1.5f, fromY + half * 0.84f)
            // bottom left
            lineTo(fromX + half * 0.68f, fromY + half * 1.45f)
            // top tip
            lineTo(fromX + half * 1.0f, fromY + half * 0.5f)
            // bottom right
            lineTo(fromX + half * 1.32f, fromY + half * 1.45f)
            // top left
            lineTo(fromX + half * 0.5f, fromY + half * 0.84f)
            close()
        }
    }

    private fun drawStar(canvas: Canvas) {
        canvas.drawPath(pathStar, paintStar)
    }

    private fun getPatternRadiusOutter(radius: Float): Double = radius - circleOuterPaint.strokeWidth / 2.0
    private fun getPatternRadiusInner(radius: Float): Double = getPatternRadiusOutter(radius) - dp34
}

fun Canvas.drawLine(p1: PointF, p2: PointF, paint: Paint) {
    drawLine(p1.x, p1.y, p2.x, p2.y, paint)
}

fun Canvas.drawCircle(point: PointF, radius: Float, paint: Paint) {
    drawCircle(point.x, point.y, radius, paint)
}

fun setPointPos(outPoint: PointF, centerPoint: PointF, angle: Double, radius: Double) {
    outPoint.x = getXPos(centerPoint.x, radius, angle).toFloat()
    outPoint.y = getYPos(centerPoint.y, radius, angle).toFloat()
}


/***
 * https://en.wikipedia.org/wiki/Sine#Relation_to_the_unit_circle
 */
private fun getXPos(centerX: Float, R: Double, angle: Double): Double = centerX + R * cos(getRadians(angle))

private fun getYPos(centerY: Float, R: Double, angle: Double): Double = centerY + R * sin(getRadians(angle))

private fun getRadians(angle: Double): Double = Math.toRadians(angle)