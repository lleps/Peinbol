import org.lwjgl.glfw.GLFW
import java.util.*
import org.lwjgl.glfw.GLFW.*


class Game {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val window = Window()
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
                        affectedByPhysics = false
                )
                window.boxes += box
                physics.boxes += box
            }

            // build player physic box
            val boxForPlayer = physics.boxes.randomElement()
            val playerBox = Box(boxForPlayer.x, boxForPlayer.y + 2, boxForPlayer.z, 1.0, 2.0, 1.0, affectedByPhysics = true)
            physics.boxes += playerBox

            window.cameraPosZ = 20f

            var lastFrame = System.currentTimeMillis()
            while (!window.isKeyPressed(GLFW_KEY_ESCAPE)) {
                updateCameraByKeys(window)
                val delta = System.currentTimeMillis() - lastFrame
                lastFrame = System.currentTimeMillis()
                physics.simulate(delta.toDouble())
                window.draw()
            }
            window.destroy()
        }

        private fun randBetween(min: Int, max: Int) = min + Random().nextInt(max-min)
        private fun <T> List<T>.randomElement() = get(randBetween(0, size))

        private fun updateCameraByKeys(window: Window) {
            with(window) {
                if (isKeyPressed(GLFW_KEY_D)) {
                    cameraPosX += 0.1f
                } else if (isKeyPressed(GLFW_KEY_A)) {
                    cameraPosX -= 0.1f
                }

                if (isKeyPressed(GLFW_KEY_W)) {
                    cameraPosZ -= 0.1f
                } else if (isKeyPressed(GLFW_KEY_S)) {
                    cameraPosZ += 0.1f
                }

                if (isKeyPressed(GLFW_KEY_UP)) {
                    cameraRotX += 3
                } else if (isKeyPressed(GLFW_KEY_DOWN)) {
                    cameraRotX -= 3
                }
                if (isKeyPressed(GLFW_KEY_RIGHT)) {
                    cameraRotY -= 3
                } else if (isKeyPressed(GLFW_KEY_LEFT)) {
                    cameraRotY += 3
                }
            }
        }

    }
}


