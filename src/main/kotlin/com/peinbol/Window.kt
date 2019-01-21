package com.peinbol

import com.bulletphysics.linearmath.Transform
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import javax.vecmath.Color3f
import javax.vecmath.Color4f
import org.lwjgl.BufferUtils
import java.nio.FloatBuffer
import org.lwjgl.opengl.GL11
import com.sun.xml.internal.ws.addressing.EndpointReferenceUtil.transform
import com.bulletphysics.linearmath.MotionState





class Window(val width: Int = 800, val height: Int = 600) {
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

    val mouseX: Float
        get() {
            val buffX = doubleArrayOf(0.0)
            val buffY = doubleArrayOf(0.0)
            glfwGetCursorPos(window, buffX, buffY)
            return buffX[0].toFloat()
        }

    val mouseY: Float
        get() {
            val buffX = doubleArrayOf(0.0)
            val buffY = doubleArrayOf(0.0)
            glfwGetCursorPos(window, buffX, buffY)
            return buffY[0].toFloat()
        }

    private val textures = mutableMapOf<Int, Texture>()

    fun init() {

        GLFWErrorCallback.createPrint(System.err).set()

        if (!glfwInit())
            throw IllegalStateException("Unable to initialize GLFW")

        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable


        window = glfwCreateWindow(width, height, "Hello World!", NULL/*glfwGetPrimaryMonitor()*/, NULL)
        if (window == NULL)
            throw RuntimeException("Failed to create the GLFW window")

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)

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
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        gluPerspective(30.toFloat().toDouble(), width.toDouble() / height.toDouble(), 0.001, 1000.0)
        glMatrixMode(GL_MODELVIEW)
        glEnable(GL_DEPTH_TEST)

        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)

        for ((txtId, txtFile) in Textures.FILES) {
            textures[txtId] = Texture(javaClass.classLoader.getResource(txtFile))
        }
    }

    private fun gluPerspective(fovY: Double, aspect: Double, zNear: Double, zFar: Double) {
        val fW: Double
        val fH: Double

        fH = Math.tan(Math.toRadians(fovY)) * zNear
        fW = fH * aspect
        glFrustum(-fW, fW, -fH, fH, zNear, zFar)
    }

    private var fpsCount: Long = 0
    private var countFpsExpiry = System.currentTimeMillis() + 1000

    // cached for drawing with bullet matrix
    private var matrix = FloatArray(16)
    private var transform = Transform()
    private var transformBuffer = BufferUtils.createFloatBuffer(16)

    fun centerCursor() {
        glfwSetCursorPos(window, 100.0, 100.0)
    }

    fun draw() {
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
            glPopMatrix()
        }

        glfwSwapBuffers(window) // swap the color buffers
        glfwPollEvents()

        fpsCount++
        if (System.currentTimeMillis() > countFpsExpiry) {
            glfwSetWindowTitle(window,
                "%d FPS | pos: %.03f %.03f %.03f  cam: %.03f %.03f %.03f"
                    .format(fpsCount, cameraPosX, cameraPosY, cameraPosZ, cameraRotX, cameraRotY, cameraRotZ)
            )
            fpsCount = 0
            countFpsExpiry = System.currentTimeMillis() + 1000
        }
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

}