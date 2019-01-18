import java.util.*
import org.lwjgl.glfw.GLFW.*


class Game {
    companion object {

        lateinit var playerBox: Box
        lateinit var physics: Physics

        @JvmStatic
        fun main(args: Array<String>) {
            val window = Window()
            window.init()
            physics = Physics()

            // build boxes
            for (i in 0..20) {
                val randint = randBetween(0, 5)
                var randtxt: Texture

                if(randint == 0) randtxt = Textures.METAL
                else if(randint == 1) randtxt = Textures.CLOTH
                else if(randint == 2) randtxt = Textures.CREEPER
                else if(randint == 3) randtxt = Textures.RUBIK
                else randtxt = Textures.FOOTBALL

                val box = Box(
                    x = randBetween(-40, 40).toDouble(),
                    y = randBetween(-5, 5).toDouble(),
                    z = randBetween(-40, 40).toDouble(),
                    sx = 1.0,
                    sy = 1.0,
                    sz = 1.0,
                    affectedByPhysics = true,
                    //color = Color.WHITE,
                    txt = randtxt
                )
                window.boxes += box
                physics.boxes += box
            }

            // build base
            val base = Box(
                x = 0.0, y = -50.0, z = 0.0,
                sx = 100.0, sy = 1.0, sz = 100.0,
                affectedByPhysics = false,
                //color = Color.GREEN,
                txt = Textures.WOOD,
                txtMultiplier = 50.0
            )

            window.boxes += base
            physics.boxes += base

            // build player physic box
            playerBox = Box(0.0, 20.0, 0.0, 0.5, 2.0, 0.5, affectedByPhysics = true)
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
                lastMouseX = window.mouseX
                lastMouseY = window.mouseY
                val delta = System.currentTimeMillis() - lastFrame
                lastFrame = System.currentTimeMillis()
                updateCameraByKeys(window, deltaMoveX, deltaMoveY, delta.toDouble())
                physics.simulate(delta.toDouble())
                window.draw()
            }
            window.destroy()
        }

        private fun randBetween(min: Int, max: Int) = min + Random().nextInt(max-min)
        private fun <T> List<T>.randomElement() = get(randBetween(0, size))

        private var lastShot: Long = 0
        private var mouseDownMillis: Long = 0

        private fun updateCameraByKeys(window: Window, mouseDX: Double, mouseDY: Double, delta: Double) {
            val deltaSec = delta / 1000.0
            val speed = 1.0

            if (window.isKeyPressed(GLFW_KEY_W)) {
                playerBox.vx -= Math.sin(Math.toRadians(window.cameraRotY)) * speed * deltaSec
                playerBox.vz -= Math.cos(Math.toRadians(window.cameraRotY)) * speed * deltaSec
            }
            if (window.isKeyPressed(GLFW_KEY_S)) {
                playerBox.vx += Math.sin(Math.toRadians(window.cameraRotY)) * speed * deltaSec
                playerBox.vz += Math.cos(Math.toRadians(window.cameraRotY)) * speed * deltaSec
            }
            if (window.isKeyPressed(GLFW_KEY_D)) {
                playerBox.vx += Math.sin(Math.toRadians(window.cameraRotY + 90.0)) * speed * deltaSec
                playerBox.vz += Math.cos(Math.toRadians(window.cameraRotY + 90.0)) * speed * deltaSec
            }
            if (window.isKeyPressed(GLFW_KEY_A)) {
                playerBox.vx += Math.sin(Math.toRadians(window.cameraRotY - 90.0)) * speed * deltaSec
                playerBox.vz += Math.cos(Math.toRadians(window.cameraRotY - 90.0)) * speed * deltaSec
            }

            if (window.isKeyPressed(GLFW_KEY_SPACE) && playerBox.inGround) {
                playerBox.vy += 1.0
            }

            if (window.isKeyPressed(GLFW_KEY_LEFT)) {
                window.cameraRotZ += 1.0
            } else if (window.isKeyPressed(GLFW_KEY_RIGHT)) {
                window.cameraRotZ -= 1.0
            }

            if (window.isMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT) &&
                (System.currentTimeMillis() - lastShot) > 500) {
                lastShot = System.currentTimeMillis()
                val shotSpeed = 0.3
                val frontPos = 0.6
                val box = Box(
                    x = playerBox.x + -Math.sin(Math.toRadians(window.cameraRotY)) * frontPos,
                    y = playerBox.y,
                    z = playerBox.z + -Math.cos(Math.toRadians(window.cameraRotY)) * frontPos,
                    sx = 0.2, sy = 0.2, sz = 0.2,
                    vx = -Math.sin(Math.toRadians(window.cameraRotY)) * shotSpeed,
                    vy = 0.0,
                    vz = -Math.cos(Math.toRadians(window.cameraRotY)) * shotSpeed
                )
                window.boxes += box
                physics.boxes += box
            }

            window.cameraPosX = playerBox.x
            window.cameraPosY = playerBox.y + 0.8
            window.cameraPosZ = playerBox.z

            window.cameraRotX -= mouseDY
            window.cameraRotY -= mouseDX
        }
    }
}