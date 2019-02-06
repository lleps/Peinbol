package io.snower.game.client;

import kotlin.collections.set

class Window(
    val assetManager: AssetResolver,
    private var width: Int = 800,
    private var height: Int = 600
) {

    var fov: Float = 30f
    var aspectRatioMultiplier = 1f


    var mouseDeltaX: Float = 0f
        private set

    var mouseDeltaY: Float = 0f
        private set

    var fps: Int = 0
        private set

    var lastDrawMillis: Float = 0f
        private set


    private var fpsCount: Long = 0
    private var countFpsExpiry = System.currentTimeMillis() + 1000

    fun draw() {
        // Draw world & UI
        val start = System.nanoTime()
        lastDrawMillis = (System.nanoTime() - start) / 1000000f

        // Stats
        fpsCount++
        if (System.currentTimeMillis() > countFpsExpiry) {
            fps = fpsCount.toInt()
            fpsCount = 0
            countFpsExpiry = System.currentTimeMillis() + 1000
        }

        if (mouseVisible) {
            mouseDeltaX = -1f + (Math.random().toFloat()*2f)
            mouseDeltaY = -1f + (Math.random().toFloat()*2f)
        }
    }

    /** Set the mouse visible (i.e for UI) or invisible, for FPS camera */
    var mouseVisible: Boolean = true
        set(value) {
            mouseDeltaX = 0f
            mouseDeltaY = 0f
            field = value
        }

    fun isKeyPressed(key: Int): Boolean {
        return false
        //val state = glfwGetKey(window, key)
        //return state == GLFW_PRESS || state == GLFW_REPEAT
    }

    fun isMouseButtonDown(button: Int): Boolean {
        return false //glfwGetMouseButton(window, button) == GLFW_PRESS
    }

    fun destroy() {
    }
}