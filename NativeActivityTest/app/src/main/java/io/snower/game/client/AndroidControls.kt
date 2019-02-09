package io.snower.game.client

import android.view.MotionEvent
import io.snower.game.common.degrees
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
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
    private var touchingControls = false // if any finger is down in the controls panel
    private var controlsFingerIndex = -1 // the id of the action in the motionEvent
    private var controlsX = 0f // last registered position in screen for the controls finger
    private var controlsY = 0f

    // For rotation
    private var lastRotX = 0f
    private var lastRotY = 0f
    private var deltaRotX = 0f
    private var deltaRotY = 0f
    private var rotFingerIndex = -1

    // called from the UI thread
    fun handleTouchEvent(event: MotionEvent) {
        processEvent(event)
    }

    private fun processEvent(e: MotionEvent) {
        println("motionEvent $e")

        if (e.x < width / 2) { // movement control zone
            when (e.actionMasked) {
                // if in any event, the user is touching the controls zone, set the variables
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                    if (!touchingControls) {
                        touchingControls = true
                        controlsX = e.getX(e.actionIndex)
                        controlsY = e.getY(e.actionIndex)
                        controlsFingerIndex = e.actionIndex
                    } else {
                        if (controlsFingerIndex == e.actionIndex) {
                            controlsX = e.getX(controlsFingerIndex)
                            controlsY = e.getY(controlsFingerIndex)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> { // stopped touching controls BUT
                    if (e.actionIndex == controlsFingerIndex) {
                        touchingControls = false
                        controlsFingerIndex = -1
                    }
                    // Cancel if drag from the rotation to the movement area
                    if (rotFingerIndex == e.actionIndex) {
                        rotFingerIndex = -1
                    }
                }
            }
        } else { // rotation
            // Cancel movement when you drag to the other part of the screen
            if (e.actionIndex == controlsFingerIndex) {
                touchingControls = false
                controlsFingerIndex = -1
            } else {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                        if (rotFingerIndex == -1) {
                            rotFingerIndex = e.actionIndex
                            lastRotX = e.getX(e.actionIndex)
                            lastRotY = e.getY(e.actionIndex)
                            deltaRotX = 0f
                            deltaRotY = 0f
                            //println("ACTION_DOWN and no index. Set to $rotFingerIndex and everything to 0. lr: ${lastRotX} $lastRotY")
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                        if (rotFingerIndex == e.actionIndex) {
                            //println("ACTION_UP for $rotFingerIndex. Set to -1")
                            rotFingerIndex = -1
                        } else {
                            //println("Invalid index in ACTION_UP: ${e.actionIndex}")
                        }
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (e.actionIndex == rotFingerIndex) {
                            val x = e.getX(e.actionIndex)
                            val y = e.getY(e.actionIndex)
                            deltaRotX += (x - lastRotX) / 10f
                            deltaRotY += (y - lastRotY) / 10f
                            //println("Movement for rot index ($rotFingerIndex): $deltaRotX $deltaRotY. lr: ${lastRotX} $lastRotY")
                            lastRotX = e.x
                            lastRotY = e.y
                        } else {
                            //println("Another index (rot: $rotFingerIndex curr: ${e.action}). Ignore event.")
                        }
                    }
                }
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
        deltaRotX *= 0.9f
        deltaRotY *= 0.9f
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
            if (!touchingControls) { // on the center
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