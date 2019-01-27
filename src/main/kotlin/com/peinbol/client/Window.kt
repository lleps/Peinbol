package com.peinbol.client

import com.peinbol.Box
import com.peinbol.Textures
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.lang.Math.PI
import kotlin.collections.List
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.contains
import kotlin.collections.emptyList
import kotlin.collections.hashMapOf
import kotlin.collections.iterator
import kotlin.collections.minusAssign
import kotlin.collections.mutableMapOf
import kotlin.collections.set
import kotlin.math.cos
import kotlin.math.sin
import kotlin.properties.Delegates

class Window {
    private var width = 800
    private var height = 600

    // TODO: move to methods.
    var boxes: List<Box> by Delegates.observable(emptyList()) { _, old, new ->
        val added = new - old
        added.forEach { worldDrawer.addBox(it) }
        val removed = old - new
        removed.forEach { worldDrawer.removeBox(it) }
    }

    var fov: Float = 30f
    var aspectRatioMultiplier = 1f


    var cameraPosX: Float
        get() = worldDrawer.cameraPosX
        set(value) {
            worldDrawer.cameraPosX = value
        }
    var cameraPosY: Float
        get() = worldDrawer.cameraPosY
        set(value) {
            worldDrawer.cameraPosY = value
        }
    var cameraPosZ: Float
        get() = worldDrawer.cameraPosZ
        set(value) {
            worldDrawer.cameraPosZ = value
        }

    var cameraRotX: Float
        get() = worldDrawer.cameraRotX
        set(value) {
            worldDrawer.cameraRotX = value
        }
    var cameraRotY: Float
        get() = worldDrawer.cameraRotY
        set(value) {
            worldDrawer.cameraRotY = value
        }
    var cameraRotZ = 0f

    var mouseDeltaX: Float = 0f
        private set

    var mouseDeltaY: Float = 0f
        private set

    var fps: Int = 0
        private set

    var lastDrawMillis: Float = 0f
        private set

    private var window: Long = 0
    private val textures = mutableMapOf<Int, Texture>()
    private val uiDrawer = NkGLBackend()
    private val worldDrawer = WorldRenderer()
    private val uiDrawables = hashMapOf<Class<out NkUIDrawable>, NkUIDrawable>()

    /** Register a drawable instance for the given class. Throws an exception if
     * other drawable is already registered for the class.
     */
    fun <T : NkUIDrawable> registerUIElement(clazz: Class<T>, drawable: T) {
        check(clazz !in uiDrawables) { "class $clazz already has a drawable registered. Remove it first." }
        uiDrawables[clazz] = drawable
        uiDrawer.addDrawable(drawable)
    }

    /** Get the drawable instance for the class, or null if it isn't registered. */
    @Suppress("UNCHECKED_CAST")
    fun <T : NkUIDrawable> getUIElement(clazz: Class<T>): T? {
        return uiDrawables[clazz] as T?
    }

    /** Remove the drawable. Thows an exception if no drawable for [clazz] is registered. */
    fun <T : NkUIDrawable> unregisterUIElement(clazz: Class<T>) {
        check(clazz in uiDrawables) { "class $clazz doesn't have a drawable registered." }
        if (clazz in uiDrawables) {
            val drawable = uiDrawables[clazz]!!
            uiDrawables -= clazz
            uiDrawer.removeDrawable(drawable)
        }
    }

    fun init() {
        GLFWErrorCallback.createPrint(System.err).set()

        if (!glfwInit())
            throw IllegalStateException("Unable to initialize GLFW")

        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0)

        val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())
        width = vidmode!!.width()
        height = vidmode.height()

        window = glfwCreateWindow(width, height, "Peinbol", glfwGetPrimaryMonitor(), NULL)
        if (window == NULL)
            throw RuntimeException("Failed to create the GLFW window")

        stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*
            glfwGetWindowSize(window, pWidth, pHeight)
            glfwSetWindowPos(
                window,
                (width - pWidth.get(0)) / 2,
                (height - pHeight.get(0)) / 2
            )
        }

        glfwMakeContextCurrent(window)
        glfwSwapInterval(1) // Enable v-sync
        glfwShowWindow(window)
        glfwSetWindowTitle(window, "Snower")
        GL.createCapabilities()

        worldDrawer.init(width, height)
        uiDrawer.init(window)

        for ((txtId, txtFile) in Textures.FILES) {
            textures[txtId] = Texture(javaClass.classLoader.getResource(txtFile))
        }
    }

    private var fpsCount: Long = 0
    private var countFpsExpiry = System.currentTimeMillis() + 1000

    fun draw() {
        val start = System.nanoTime()

        // Draw world & UI
        worldDrawer.draw()
        uiDrawer.draw(window)

        // Stats
        fpsCount++
        if (System.currentTimeMillis() > countFpsExpiry) {
            fps = fpsCount.toInt()
            fpsCount = 0
            countFpsExpiry = System.currentTimeMillis() + 1000
        }

        lastDrawMillis = (System.nanoTime() - start) / 1000000f

        // Window sync
        glfwSwapBuffers(window)

        // on next frame, get the fresh new events.
        glfwPollEvents()
        if (mouseVisible) {
            stackPush().use { stack ->
                val x = stack.mallocDouble(1)
                val y = stack.mallocDouble(1)
                glfwGetCursorPos(window, x, y)
                val centerX = width / 2f
                val centerY = height / 2f
                mouseDeltaX = (x.get(0) - centerX).toFloat()
                mouseDeltaY = (y.get(0) - centerY).toFloat()
                glfwSetCursorPos(window, centerX.toDouble(), centerY.toDouble())
            }
        }
    }

    /** Set the mouse visible (i.e for UI) or invisible, for FPS camera */
    var mouseVisible: Boolean = false
        set(value) {
            if (!value) {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
            } else {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)
            }
            glfwSetCursorPos(window, (width / 2f).toDouble(), (height / 2f).toDouble())
            mouseDeltaX = 0f
            mouseDeltaY = 0f
            field = value
        }

    fun isKeyPressed(key: Int): Boolean {
        return glfwGetKey(window, key) == GLFW_PRESS
    }

    fun isMouseButtonDown(button: Int): Boolean {
        return glfwGetMouseButton(window, button) == GLFW_PRESS
    }

    fun destroy() {
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)
        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
    }

    private fun drawSphere(radius: Float, slices: Int, stacks: Int) {
        var rho: Float
        val drho: Float
        var theta: Float
        val dtheta: Float
        var x: Float
        var y: Float
        var z: Float
        var s: Float
        var t: Float
        val ds: Float
        val dt: Float
        var i: Int
        var j: Int
        val imin: Int
        val imax: Int
        val nsign = 1.0f // -1.0 if going inside?
        val useTextures = true
        drho = (PI / stacks).toFloat()
        dtheta = (2.0f * PI / slices).toFloat()

        if (!useTextures) {
            glBegin(GL_TRIANGLE_FAN)
            glNormal3f(0.0f, 0.0f, 1.0f)
            glVertex3f(0.0f, 0.0f, nsign * radius)
            j = 0
            while (j <= slices) {
                theta = if (j == slices) 0.0f else j * dtheta
                x = (-sin(theta) * sin(drho))
                y = (cos(theta) * sin(drho))
                z = (nsign * cos(drho))
                glNormal3f(x * nsign, y * nsign, z * nsign)
                glVertex3f(x * radius, y * radius, z * radius)
                j++
            }
            glEnd()
        }

        ds = 1.0f / slices
        dt = 1.0f / stacks
        t = 1.0f // because loop now runs from 0
        if (useTextures) {
            imin = 0
            imax = stacks
        } else {
            imin = 1
            imax = stacks - 1
        }

        // draw intermediate stacks as quad strips
        i = imin
        while (i < imax) {
            rho = (i * drho)
            glBegin(GL_QUAD_STRIP)
            s = 0.0f
            j = 0
            while (j <= slices) {
                theta = if (j == slices) 0.0f else (j * dtheta)
                x = -sin(theta) * sin(rho)
                y = cos(theta) * sin(rho)
                z = nsign * cos(rho)
                glNormal3f(x * nsign, y * nsign, z * nsign)
                glTexCoord2f(s, t) //TXTR_COORD(s, t)
                glVertex3f(x * radius, y * radius, z * radius)
                x = (-sin(theta) * sin(rho + drho))
                y = (cos(theta) * sin(rho + drho))
                z = (nsign * cos(rho + drho))
                glNormal3f(x * nsign, y * nsign, z * nsign)
                glTexCoord2f(s, t - dt)//TXTR_COORD(s, t - dt)
                s += ds
                glVertex3f(x * radius, y * radius, z * radius)
                j++
            }
            glEnd()
            t -= dt
            i++
        }

        if (!useTextures) {
            // draw -Z end as a triangle fan
            glBegin(GL_TRIANGLE_FAN)
            glNormal3f(0.0f, 0.0f, -1.0f)
            glVertex3f(0.0f, 0.0f, -radius * nsign)
            rho = (PI - drho).toFloat()
            s = 1.0f
            j = slices
            while (j >= 0) {
                theta = if (j == slices) 0.0f else (j * dtheta)
                x = -sin(theta) * sin(rho)
                y = cos(theta) * sin(rho)
                z = nsign * cos(rho)
                glNormal3f(x * nsign, y * nsign, z * nsign)
                s -= ds
                glVertex3f(x * radius, y * radius, z * radius)
                j--
            }
            glEnd()
        }
    }
}