import org.lwjgl.*
import org.lwjgl.glfw.*
import org.lwjgl.opengl.*
import org.lwjgl.system.*

import java.nio.*

import org.lwjgl.glfw.Callbacks.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.MemoryUtil.*

class Window(val width: Int = 640, val height: Int = 480) {
    var boxes: List<Box> = emptyList()

    var cameraPosX = 0f
    var cameraPosY = 0f
    var cameraPosZ = 0f

    var cameraRotX = 0f
    var cameraRotY = 0f
    var cameraRotZ = 0f

    private var window: Long = 0

    init {
        init()
    }

    private fun init() {
        GLFWErrorCallback.createPrint(System.err).set()

        if (!glfwInit())
            throw IllegalStateException("Unable to initialize GLFW")

        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable

        window = glfwCreateWindow(width, height, "Hello World!", NULL, NULL)
        if (window == NULL)
            throw RuntimeException("Failed to create the GLFW window")

        stackPush().use({ stack ->
            val pWidth = stack.mallocInt(1) // int*
            val pHeight = stack.mallocInt(1) // int*
            glfwGetWindowSize(window, pWidth, pHeight)
            val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())

            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            )
        })

        glfwMakeContextCurrent(window)
        glfwSwapInterval(1) // Enable v-sync
        glfwShowWindow(window)

        GL.createCapabilities()
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        gluPerspective(30.toFloat().toDouble(), (width / height).toDouble(), 0.001, 100.0)
        glMatrixMode(GL_MODELVIEW)
        glEnable(GL_DEPTH_TEST)
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

    fun draw() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT) // clear the framebuffer
        glLoadIdentity()
        glRotatef(-cameraRotX, 1f, 0f, 0f)
        glRotatef(-cameraRotY, 0f, 1f, 0f)
        glRotatef(-cameraRotZ, 0f, 0f, 1f)
        glTranslatef(-cameraPosX, -cameraPosY, -cameraPosZ)
        glColor3d(0.4, 0.1, 0.0)

        for (box in boxes) {
            glPushMatrix()
            glTranslated(box.x, box.y, box.z)
            glScaled(box.sx / 2.0, box.sy / 2.0, box.sz / 2.0)
            glBegin(GL_QUADS)
            // Cara de arriba
            glVertex3d(-1.0, 1.0, 1.0)
            glVertex3d(1.0, 1.0, 1.0)
            glVertex3d(1.0, 1.0, -1.0)
            glVertex3d(-1.0, 1.0, -1.0)

            // Cara de abajo
            glVertex3d(-1.0, -1.0, 1.0)
            glVertex3d(1.0, -1.0, 1.0)
            glVertex3d(1.0, -1.0, -1.0)
            glVertex3d(-1.0, -1.0, -1.0)

            // Cara frontal
            glVertex3d(-1.0, -1.0, 1.0)
            glVertex3d(1.0, -1.0, 1.0)
            glVertex3d(1.0, 1.0, 1.0)
            glVertex3d(-1.0, 1.0, 1.0)

            // Cara trasera
            glVertex3d(1.0, -1.0, -1.0)
            glVertex3d(-1.0, -1.0, -1.0)
            glVertex3d(-1.0, 1.0, -1.0)
            glVertex3d(1.0, 1.0, -1.0)

            // Cara izquierda
            glVertex3d(-1.0, 1.0, 1.0)
            glVertex3d(-1.0, 1.0, -1.0)
            glVertex3d(-1.0, -1.0, -1.0)
            glVertex3d(-1.0, -1.0, 1.0)

            // Cara derecha
            glVertex3d(1.0, 1.0, -1.0)
            glVertex3d(1.0, 1.0, 1.0)
            glVertex3d(1.0, -1.0, 1.0)
            glVertex3d(1.0, -1.0, -1.0)
            glEnd()
            glPopMatrix()
        }

        glfwSwapBuffers(window) // swap the color buffers
        glfwPollEvents()

        fpsCount++
        if (System.currentTimeMillis() > countFpsExpiry) {
            glfwSetWindowTitle(window, "Hello world! $fpsCount FPS")
            fpsCount = 0
            countFpsExpiry = System.currentTimeMillis() + 1000
        }
    }

    fun isKeyPressed(key: Int): Boolean {
        return glfwGetKey(window, key) == GLFW_PRESS
    }

    fun destroy() {
        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)
        glfwTerminate()
        glfwSetErrorCallback(null).free()
    }

}