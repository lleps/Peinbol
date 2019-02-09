package io.snower.game.client

import android.view.MotionEvent
import io.snower.game.common.degrees
import javax.vecmath.Vector2f
import kotlin.math.atan2


/** Implements controls using a touch interface. */
class AndroidControls : Controls, UIDrawable {
    companion object {
        private const val RING_RADIUS = 90f
        private const val INNER_RADIUS = 20f
        private const val RING_COLOR = 0xFF8080FF.toInt()
        private const val INNER_COLOR = 0x5FF050FF
        private const val X_PAD = 100f //
        private const val Y_PAD = 100f // padding from bottom-left
    }

    private var forward = false
    private var backwards = false
    private var left = false
    private var right = false
    private var width = 100
    private var height = 100

    // For movement panel
    private var controlsFingerId = -1 // the id of the action in the motionEvent
    private var controlsX = 0f // last registered position in screen for the controls finger
    private var controlsY = 0f

    // For rotation
    private var rotFingerId = -1
    private var lastRotX = 0f
    private var lastRotY = 0f
    private var deltaRotX = 0f
    private var deltaRotY = 0f

    // called from the UI thread
    fun handleTouchEvent(event: MotionEvent) {
        processEvent(event)
    }

    private fun pointerUpdate(id: Int, action: Int, x: Float, y: Float) {
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                if (x > width / 2) { // rotation
                    if (rotFingerId == -1) {
                        rotFingerId = id
                        lastRotX = x
                        lastRotY = y
                        deltaRotX = 0f
                        deltaRotY = 0f
                    }
                } else {
                    if (controlsFingerId == -1) { // movement
                        controlsFingerId = id
                        controlsX = x
                        controlsY = y
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (id == rotFingerId) {
                    deltaRotX += (x - lastRotX) / 10f
                    deltaRotY += (y - lastRotY) / 10f
                    lastRotX = x
                    lastRotY = y
                } else if (id == controlsFingerId) {
                    controlsX = x
                    controlsY = y
                }
            }
            MotionEvent.ACTION_UP -> {
                if (id == rotFingerId) {
                    rotFingerId = -1
                } else if (id == controlsFingerId) {
                    controlsFingerId = -1
                }
            }
        }
    }

    private fun processEvent(e: MotionEvent) {
        val pointerIndex = e.actionIndex
        var pointerId = e.getPointerId(pointerIndex)
        val maskedAction = e.actionMasked

        when (maskedAction) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                pointerUpdate(pointerId, MotionEvent.ACTION_DOWN, e.getX(pointerIndex), e.getY(pointerIndex))
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerCount = e.pointerCount
                for (i in 0 until pointerCount) {
                    if (e.historySize > 0) {
                        if (e.getX(i).toInt() != e.getHistoricalX(i, 0).toInt() ||
                            e.getY(i).toInt() != e.getHistoricalY(i, 0).toInt()
                        ) {
                            pointerId = e.getPointerId(i)
                            pointerUpdate(pointerId, MotionEvent.ACTION_MOVE, e.getX(i), e.getY(i))
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                pointerUpdate(pointerId, MotionEvent.ACTION_UP, e.getX(pointerIndex), e.getY(pointerIndex))
            }
        }
    }

    override fun draw(drawer: UIDrawer, screenWidth: Float, screenHeight: Float) {
        width = screenWidth.toInt()
        height = screenHeight.toInt()
        updateAndDrawMovement(drawer, screenWidth, screenHeight)
    }

    // here should reset rotation delta
    override fun readDone() {
        deltaRotX *= 0.7f
        deltaRotY *= 0.7f
    }

    // Update the movement panel

    private val directionVec = Vector2f()

    private fun updateAndDrawMovement(drawer: UIDrawer, screenWidth: Float, screenHeight: Float) {
        val centerX = X_PAD + RING_RADIUS
        val centerY = screenHeight - Y_PAD - RING_RADIUS
        if (drawer.begin(
                "controls",
                0f, screenHeight / 2f,
                screenWidth / 2f, screenHeight / 2f,
                background = 0x0,
                flags = drawer.WINDOW_NO_SCROLLBAR)) {

            // draw ring
            drawCircle(drawer,
                centerX, centerY,
                RING_RADIUS,
                RING_COLOR,
                10f)

            // draw movement point
            if (controlsFingerId == -1) { // on the center
                drawCircle(drawer,
                    centerX, centerY,
                    INNER_RADIUS,
                    INNER_COLOR,
                    0f)
                left = false
                right = false
                forward = false
                backwards = false
            } else {
                directionVec.set(controlsX - centerX, controlsY - centerY)
                if (directionVec.length() > RING_RADIUS) {
                    directionVec.normalize()
                    directionVec.scale(RING_RADIUS)
                }

                val angle = degrees(atan2(directionVec.y, directionVec.x)).toInt() / (360/8)
                left = angle == 3 || angle == -3
                right = angle == 0
                forward = angle == -1 || angle == -2
                backwards = angle == 1 || angle == 2
                drawCircle(drawer,
                    centerX + directionVec.x, centerY + directionVec.y,
                    INNER_RADIUS,
                    INNER_COLOR,
                    0f)
            }
        }
        drawer.end()
    }

    private fun drawCircle(
        drawer: UIDrawer,
        x: Float, y: Float,
        radius: Float,
        color: Int,
        thickness: Float = 0f) {
        if (thickness != 0f) {
            drawer.strokeCircle(x - radius, y - radius, radius*2f, thickness, color)
        } else {
            drawer.fillCircle(x - radius, y - radius, radius*2f, color)
        }
    }

    override fun checkForward(): Boolean = forward

    override fun checkBackwards(): Boolean = backwards

    override fun checkRight(): Boolean = right

    override fun checkLeft(): Boolean = left

    override fun checkFire(): Boolean = false

    override fun getCameraRotationX(): Float = deltaRotX

    override fun getCameraRotationY(): Float = deltaRotY

}