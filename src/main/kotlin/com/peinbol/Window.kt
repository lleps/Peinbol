package com.peinbol

import com.bulletphysics.linearmath.Transform
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.glEnd
import org.lwjgl.opengl.GL11.glVertex3f
import org.lwjgl.opengl.GL11.glNormal3f
import org.lwjgl.opengl.GL11.glBegin
import org.lwjgl.opengl.GL11.GL_QUAD_STRIP
import java.lang.Math.PI
import kotlin.math.cos
import kotlin.math.sin


class Window(val width: Int = 1366, val height: Int = 768) {
    var boxes: List<Box> = emptyList()

    var cameraPosX = 0f
    var cameraPosY = 0f
    var cameraPosZ = 0f

    var cameraRotX = 0f
        set(value) {
            field = value.coerceIn(-85f, 85f) % 360f
        }

    var cameraRotY = 0f
        set(value) {
            field = value % 360f
        }

    var cameraRotZ = 0f
        set(value) {
            field = value % 360f
        }

    private var window: Long = 0

    /** Set the mouse visible (i.e for UI) or invisible, for FPS camera */
    var mouseVisible: Boolean = false
        set(value) {
            field = value
            if (!value) {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL)
            } else {
                glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)
            }
        }

    private val textures = mutableMapOf<Int, Texture>()
    private val uiDrawer = NuklearGLDrawer()

    var mouseDeltaX: Float = 0f
        private set

    var mouseDeltaY: Float = 0f
        private set

    fun init() {

        GLFWErrorCallback.createPrint(System.err).set()

        if (!glfwInit())
            throw IllegalStateException("Unable to initialize GLFW")

        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        window = glfwCreateWindow(width, height, "Hello World!", glfwGetPrimaryMonitor(), NULL)
        if (window == NULL)
            throw RuntimeException("Failed to create the GLFW window")

        //glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)


        stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*
            glfwGetWindowSize(window, pWidth, pHeight)
            val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())

            glfwSetWindowPos(
                window,
                (vidmode!!.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            )
        }

        glfwMakeContextCurrent(window)
        glfwSwapInterval(1) // Enable v-sync
        glfwShowWindow(window)

        GL.createCapabilities()

        uiDrawer.run(window)

        for ((txtId, txtFile) in Textures.FILES) {
            textures[txtId] = Texture(javaClass.classLoader.getResource(txtFile))
        }
    }

    private fun setupOpenGLToDraw3D() {
        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        gluPerspective(30.toFloat().toDouble(), width.toDouble() / height.toDouble(), 0.001, 1000.0)
        glMatrixMode(GL_MODELVIEW)
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)

    }

    private fun gluPerspective(fovY: Double, aspect: Double, zNear: Double, zFar: Double) {
        val fH: Double = Math.tan(Math.toRadians(fovY)) * zNear
        val fW = fH * aspect
        glFrustum(-fW, fW, -fH, fH, zNear, zFar)
    }

    private var fpsCount: Long = 0
    private var countFpsExpiry = System.currentTimeMillis() + 1000

    // cached for drawing with bullet matrix
    private var matrix = FloatArray(16)
    private var transform = Transform()
    private var transformBuffer = BufferUtils.createFloatBuffer(16)

    fun centerCursor() {
        //glfwSetCursorPos(window, 100.0, 100.0)
    }

    fun draw() {
        // Get mouse movement
        if (mouseVisible) {
            stackPush().use { stack ->
                val x = stack.mallocDouble(1)
                val y = stack.mallocDouble(1)
                glfwGetCursorPos(window, x, y)
                mouseDeltaX = (x.get(0) - 100.0).toFloat()
                mouseDeltaY = (y.get(0) - 100.0).toFloat()
                glfwSetCursorPos(window, 100.0, 100.0)
            }
        }

        setupOpenGLToDraw3D()
        glEnable(GL_TEXTURE_2D)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glLoadIdentity()
        glRotatef(-cameraRotX, 1f, 0f, 0f)
        glRotatef(-cameraRotY, 0f, 1f, 0f)
        glRotatef(-cameraRotZ, 0f, 0f, 1f)
        glTranslatef(-cameraPosX, -cameraPosY, -cameraPosZ)

        val skyColor = Color(152.0/255.0, 209.0/255.0, 214.0/255.0)
        glClearColor(
            skyColor.r.toFloat(),
            skyColor.g.toFloat(),
            skyColor.b.toFloat(),
            skyColor.a.toFloat()
        )

        val ambientLight = 0.8f
        for (box in boxes) {
            textures[box.textureId]?.bind()


            // TODO: based on box size, keep the texture relation to 1/1
            glColor3f(box.theColor.x * ambientLight, box.theColor.y * ambientLight, box.theColor.z * ambientLight)

            glPushMatrix()

            // set position and rotation based on the rigid body
            val bodyMotionState = box.rigidBody!!.motionState
            bodyMotionState.getWorldTransform(transform)
            transform.getOpenGLMatrix(matrix)
            transformBuffer.clear()
            transformBuffer.put(matrix)
            transformBuffer.flip()
            glMultMatrixf(transformBuffer)

            if (box.isSphere) {
                drawSphere(box.size.x, 20, 20)
            } else {
                glScalef(box.size.x / 2f, box.size.y / 2f, box.size.z / 2f)
                glBegin(GL_QUADS)
                val top = box.textureMultiplier
                glTexCoord2d(0.0, 0.0); glVertex3d(-1.0, -1.0,  1.0)
                glTexCoord2d(top, 0.0); glVertex3d( 1.0, -1.0,  1.0)
                glTexCoord2d(top, top); glVertex3d( 1.0,  1.0,  1.0)
                glTexCoord2d(0.0, top); glVertex3d(-1.0,  1.0,  1.0)
                glTexCoord2d(top, 0.0); glVertex3d(-1.0, -1.0, -1.0)
                glTexCoord2d(top, top); glVertex3d(-1.0,  1.0, -1.0)
                glTexCoord2d(0.0, top); glVertex3d( 1.0,  1.0, -1.0)
                glTexCoord2d(0.0, 0.0); glVertex3d( 1.0, -1.0, -1.0)
                glTexCoord2d(0.0, top); glVertex3d(-1.0,  1.0, -1.0)
                glTexCoord2d(0.0, 0.0); glVertex3d(-1.0,  1.0,  1.0)
                glTexCoord2d(top, 0.0); glVertex3d( 1.0,  1.0,  1.0)
                glTexCoord2d(top, top); glVertex3d( 1.0,  1.0, -1.0)
                glTexCoord2d(top, top); glVertex3d(-1.0, -1.0, -1.0)
                glTexCoord2d(0.0, top); glVertex3d( 1.0, -1.0, -1.0)
                glTexCoord2d(0.0, 0.0); glVertex3d( 1.0, -1.0,  1.0)
                glTexCoord2d(top, 0.0); glVertex3d(-1.0, -1.0,  1.0)
                glTexCoord2d(top, 0.0); glVertex3d( 1.0, -1.0, -1.0)
                glTexCoord2d(top, top); glVertex3d( 1.0,  1.0, -1.0)
                glTexCoord2d(0.0, top); glVertex3d( 1.0,  1.0,  1.0)
                glTexCoord2d(0.0, 0.0); glVertex3d( 1.0, -1.0,  1.0)
                glTexCoord2d(0.0, 0.0); glVertex3d(-1.0, -1.0, -1.0)
                glTexCoord2d(top, 0.0); glVertex3d(-1.0, -1.0,  1.0)
                glTexCoord2d(top, top); glVertex3d(-1.0,  1.0,  1.0)
                glTexCoord2d(0.0, top); glVertex3d(-1.0,  1.0, -1.0)
                glEnd()
            }
            glPopMatrix()
        }



        fpsCount++
        if (System.currentTimeMillis() > countFpsExpiry) {
            glfwSetWindowTitle(window,
                "%d FPS | pos: %.03f %.03f %.03f  cam: %.03f %.03f %.03f"
                    .format(fpsCount, cameraPosX, cameraPosY, cameraPosZ, cameraRotX, cameraRotY, cameraRotZ)
            )
            fpsCount = 0
            countFpsExpiry = System.currentTimeMillis() + 1000
        }


        // now, the death...
        uiDrawer.draw(window)

        glfwSwapBuffers(window) // swap the color buffers
        glfwPollEvents()
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