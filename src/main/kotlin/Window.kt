import org.lwjgl.*
import org.lwjgl.glfw.*
import org.lwjgl.opengl.*
import org.lwjgl.system.*

import java.nio.*

import org.lwjgl.glfw.Callbacks.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL12.GL_TEXTURE_3D
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil.*

class Window(val width: Int = 1366, val height: Int = 768) {
    var boxes: List<Box> = emptyList()

    var cameraPosX = 0.0
    var cameraPosY = 0.0
    var cameraPosZ = 0.0

    var cameraRotX = 0.0
        set(value) {
            field = value.coerceIn(-85.0, 85.0) % 360.0
        }

    var cameraRotY = 0.0
        set(value) {
            field = value % 360.0
        }

    var cameraRotZ = 0.0
        set(value) {
            field = value % 360.0
        }

    private var window: Long = 0

    val mouseX: Double
        get() {
            val buffX = doubleArrayOf(0.0)
            val buffY = doubleArrayOf(0.0)
            glfwGetCursorPos(window, buffX, buffY)
            return buffX[0]
        }

    val mouseY: Double
        get() {
            val buffX = doubleArrayOf(0.0)
            val buffY = doubleArrayOf(0.0)
            glfwGetCursorPos(window, buffX, buffY)
            return buffY[0]
        }

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

        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)

        stackPush().use { stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*
            glfwGetWindowSize(window, pWidth, pHeight)
            val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())

            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
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

    fun centerCursor() {
        glfwSetCursorPos(window, 100.0, 100.0)
    }

    fun draw() {

        glEnable(GL_TEXTURE_2D)

        val u = -1f
        val v = -1f
        val w = -1f
        val u2 = 1f
        val v2 = 1f
        val w2 = 1f

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT) // clear the framebuffer
        glLoadIdentity()
        glRotated(-cameraRotX, 1.0, 0.0, 0.0)
        glRotated(-cameraRotY, 0.0, 1.0, 0.0)
        glRotated(-cameraRotZ, 0.0, 0.0, 1.0)
        glTranslated(-cameraPosX, -cameraPosY, -cameraPosZ)

        for (box in boxes) {
            box.txt.bind()

            glColor3d(box.color.r, box.color.g, box.color.b)

            glPushMatrix()
            glTranslated(box.x, box.y, box.z)
            glScaled(box.sx / 2.0, box.sy / 2.0, box.sz / 2.0)
            glBegin(GL_QUADS)
            val top = box.txtMultiplier
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
        glfwSetErrorCallback(null).free()
    }

}