import org.lwjgl.glfw.GLFW
import java.util.*
import org.lwjgl.glfw.GLFW.*


class Game {
    companion object {

        lateinit var playerBox: Box

        @JvmStatic
        fun main(args: Array<String>) {
            val window = Window()
            window.init()
            val physics = Physics()

            // build boxes
            for (i in 0..20) {
                val box = Box(
                    x = randBetween(-40, 40).toDouble(),
                    y = randBetween(-5, 5).toDouble(),
                    z = randBetween(-40, 40).toDouble(),
                    sx = 1.0,
                    sy = 1.0,
                    sz = 1.0,
                    affectedByPhysics = true,
                    color = Color.RED
                )
                window.boxes += box
                physics.boxes += box
            }

            // build base
            val base = Box(
                x = 0.0, y = -50.0, z = 0.0,
                sx = 100.0, sy = 1.0, sz = 100.0,
                affectedByPhysics = false,
                color = Color.GREEN
            )


            window.boxes += base
            physics.boxes += base

            // build player physic box
            playerBox = Box(0.0,20.0,0.0,  1.0,2.0,1.0, affectedByPhysics = true)
            physics.boxes += playerBox

            window.cameraPosX = playerBox.x
            window.cameraPosY = playerBox.y
            window.cameraPosZ = playerBox.z

            var lastFrame = System.currentTimeMillis()
            var lastMouseX: Double = 0.0
            var lastMouseY: Double = 0.0
            while (!window.isKeyPressed(GLFW_KEY_ESCAPE)) {
                val deltaMoveX = window.mouseX - lastMouseX
                val deltaMoveY = window.mouseY - lastMouseY
                updateCameraByKeys(window, deltaMoveX, deltaMoveY)
                lastMouseX = window.mouseX
                lastMouseY = window.mouseY
                val delta = System.currentTimeMillis() - lastFrame
                lastFrame = System.currentTimeMillis()
                physics.simulate(delta.toDouble())

                window.draw()
            }
            window.destroy()
        }

        private fun randBetween(min: Int, max: Int) = min + Random().nextInt(max-min)
        private fun <T> List<T>.randomElement() = get(randBetween(0, size))

        private fun updateCameraByKeys(window: Window, mouseDX: Double, mouseDY: Double) {
            println("delta: ${mouseDX} ${mouseDY}")
            if (window.isKeyPressed(GLFW_KEY_D)) {
                //playerBox.x += 0.1f
            } else if (window.isKeyPressed(GLFW_KEY_A)) {
                //playerBox.x -= 0.1f
            }

            if (window.isKeyPressed(GLFW_KEY_W)) {
                playerBox.x -= Math.sin(Math.toRadians(window.cameraRotY))
                playerBox.z -= Math.cos(Math.toRadians(window.cameraRotY))
            } else if (window.isKeyPressed(GLFW_KEY_S)) {
                playerBox.x += Math.sin(Math.toRadians(window.cameraRotY))
                playerBox.z += Math.cos(Math.toRadians(window.cameraRotY))
            }

            window.cameraPosX = playerBox.x
            window.cameraPosY = playerBox.y
            window.cameraPosZ = playerBox.z

            window.cameraRotX -= mouseDY
            window.cameraRotY -= mouseDX

            if (window.isKeyPressed(GLFW_KEY_LEFT)) {
                window.cameraRotZ += 1.0
            } else if (window.isKeyPressed(GLFW_KEY_RIGHT)) {
                window.cameraRotZ -= 1.0
            }
        }
    }
}