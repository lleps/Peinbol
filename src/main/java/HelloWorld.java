import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class HelloWorld {

    // The window handle
    private long window;

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");
        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(300, 300, "Hello World!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        long fpsCount = 0;
        long countFpsExpiry = System.currentTimeMillis() + 1000;

        float cameraPosX = 0;
        float cameraPosY = 0;
        float cameraPosZ = 0;

        float cameraRotX = 0;
        float cameraRotY = 0;
        float cameraRotZ = 0;

        GL.createCapabilities();
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        gluPerspective((float) 30, 300f / 300f, 0.001f, 100);
        glMatrixMode(GL_MODELVIEW);
        glEnable(GL_DEPTH_TEST);

        while ( !glfwWindowShouldClose(window) ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
                cameraPosX += 0.1f;
            } else if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
                cameraPosX -= 0.1f;
            }

            if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
                cameraPosZ -= 0.1f;
            } else if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
                cameraPosZ += 0.1f;
            }

            if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS) {
                cameraRotX += 10f;
            } else if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS) {
                cameraRotX -= 10f;
            }
            if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) {
                cameraRotY += 10f;
            } else if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS) {
                cameraRotY -= 10f;
            }

            glLoadIdentity();
            glRotatef(-cameraRotX, 1, 0, 0);
            glRotatef(-cameraRotY, 0, 1, 0);
            glRotatef(-cameraRotZ, 0, 0, 1);
            glTranslatef(-cameraPosX, -cameraPosY, -cameraPosZ);
            glColor3d(0.4, 0.1, 0.0);
            glBegin(GL_QUADS);
                glVertex3d(0.0, 0.0, 0.0);
                glVertex3d(0.0, 1.0, 0.0);
                glVertex3d(1.0, 1.0, 0.0);
                glVertex3d(1.0, 0.0, 0.0);
            glEnd();

            glfwSwapBuffers(window); // swap the color buffers


            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();

            fpsCount++;
            if (System.currentTimeMillis() > countFpsExpiry) {
                glfwSetWindowTitle(window, "Hello world! " + fpsCount + " FPS");
                fpsCount = 0;
                countFpsExpiry = System.currentTimeMillis() + 1000;
            }
        }
    }

    private void gluPerspective(double fovY, double aspect, double zNear, double zFar) {
        double fW, fH;

        fH = Math.tan(Math.toRadians(fovY)) * zNear;
        fW = fH * aspect;
        glFrustum( -fW, fW, -fH, fH, zNear, zFar );
    }

    public static void main(String[] args) {
        new HelloWorld().run();
    }

}