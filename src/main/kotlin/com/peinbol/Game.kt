package com.peinbol

import org.lwjgl.glfw.GLFW.*
import javax.vecmath.Vector3f
import kotlin.concurrent.thread

/**
 * Peinbol game client. Essentially, draw what the server says.
 * And report input state (keys, cursor pos) back to the server.
 */
class Game {
    companion object {
        private val INPUT_SYNC_RATE = 50 // each how many millis will send the state

        @JvmStatic
        fun main(args: Array<String>) {
            val game = Game()
            game.init(args)
        }
    }

    private lateinit var physics: Physics
    private lateinit var window: Window
    private lateinit var network: Network.Client
    private var lastInputStateSent: Long = 0L
    private var lastShot: Long = 0
    private var mouseDownMillis: Long = 0
    private val boxes = hashMapOf<Int, Box>()
    private var myBoxId = -1

    fun init(args: Array<String>) {
        val host = args[0]
        val port = args[1].toInt()

        // Init network
        network = Network.createClient(host, port)
        network.onServerMessage { msg -> handleNetworkMessage(msg) }

        // Init window
        window = Window()
        window.init()
        txts // init txts variable when opengl is already loaded, otherwise jvm crash
        window.centerCursor()
        physics = Physics()
        physics.init()

        var lastFrame = System.currentTimeMillis()
        while (!window.isKeyPressed(GLFW_KEY_ESCAPE)) {
            val deltaMoveX = window.mouseX - 100f // since was centered on last frame, get how much moved since then
            val deltaMoveY = window.mouseY - 100f
            val delta = System.currentTimeMillis() - lastFrame
            lastFrame = System.currentTimeMillis()
            network.pollMessages()
            update(window, deltaMoveX, deltaMoveY, delta.toFloat())
            physics.simulate(delta.toDouble())
            window.centerCursor()
            window.draw()
        }
        window.destroy()
        network.close()
    }

    /** Called when a message from the server arrives. */
    private fun handleNetworkMessage(msg: Any) {
        when (msg) {
            is Messages.Spawn -> {
                myBoxId = msg.boxId
                val myBox = boxes[myBoxId]
                if (myBox != null) {
                    window.boxes -= myBox
                }
            }
            is Messages.BoxAdded -> {
                val box = Box(
                    id = msg.id,
                    position = msg.position,
                    size = msg.size,
                    velocity = msg.velocity,
                    mass = msg.mass,
                    affectedByPhysics = msg.affectedByPhysics,
                    textureId = msg.textureId,
                    textureMultiplier = msg.textureMultiplier,
                    bounceMultiplier = msg.bounceMultiplier
                )
                addBox(box)
            }
            is Messages.BoxUpdateMotion -> {
                val box = boxes[msg.id]
                if (box != null) {
                    box.updatePosition(msg.position)
                    box.updateLinearVelocity(msg.velocity)
                    box.updateAngularVelocity(msg.angularVelocity)
                    // TODO: fetch quaternion also.
                } else {
                    println("Can't find box for id ${msg.id}")
                }
            }
        }
    }

    /** Send input state if appropiate, and update camera pos */
    private fun update(window: Window, mouseDX: Float, mouseDY: Float, delta: Float) {
        // sync camera pos (and delta). Pos is synced with myBoxId
        val playerBox = boxes[myBoxId]
        if (playerBox != null) {
            window.cameraPosX = playerBox.position.x
            window.cameraPosY = playerBox.position.y + 0.8f
            window.cameraPosZ = playerBox.position.z
        }

        window.cameraRotX -= mouseDY * 0.4f
        window.cameraRotY -= mouseDX * 0.4f

        // send input state
        if (System.currentTimeMillis() - lastInputStateSent > INPUT_SYNC_RATE) {
            lastInputStateSent = System.currentTimeMillis()
            network.send(Messages.InputState(
                forward = window.isKeyPressed(GLFW_KEY_W),
                backwards = window.isKeyPressed(GLFW_KEY_S),
                left = window.isKeyPressed(GLFW_KEY_A),
                right = window.isKeyPressed(GLFW_KEY_D),
                fire = window.isMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT),
                jump = window.isKeyPressed(GLFW_KEY_SPACE),
                walk = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT),
                cameraX = window.cameraRotX,
                cameraY = window.cameraRotY
            ))
        }
    }

    private fun addBox(box: Box) {
        if (box.id !in boxes) {
            boxes[box.id] = box
            physics.register(box)
            window.boxes += box
        }
    }

    private val txts by lazy {
        listOf(
            Texture(javaClass.classLoader.getResource("metal.png")),
            Texture(javaClass.classLoader.getResource("cloth.png")),
            Texture(javaClass.classLoader.getResource("creeper.png")),
            Texture(javaClass.classLoader.getResource("rubik.png")),
            Texture(javaClass.classLoader.getResource("football.png"))
        )
    }
}