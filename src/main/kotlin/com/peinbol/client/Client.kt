package com.peinbol.client

import com.peinbol.*
import org.lwjgl.glfw.GLFW.*
import javax.vecmath.Color4f
import javax.vecmath.Vector3f
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin

/**
 * Peinbol game client. Essentially, draw what the server says.
 * And report input state (keys, cursor pos) back to the server.
 */
class Client {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val game = Client()
            game.init(args)
        }
    }

    private var lastSentInputState = Messages.InputState()
    private lateinit var physics: Physics
    private lateinit var window: Window
    private lateinit var network: Network.Client
    private lateinit var audioManager: AudioManager
    private var lastInputStateSent: Long = 0L
    private var lastShot: Long = 0
    private var mouseDownMillis: Long = 0
    private val boxes = hashMapOf<Int, Box>()
    private var myBoxId = -1
    private lateinit var notifyHitAudio: AudioSource
    private lateinit var shootAudio: AudioSource
    private lateinit var splashAudio: AudioSource

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
        physics = Physics(Physics.Mode.CLIENT)
        physics.init()

        // setup ui
        window.registerUIElement(ClientStatsUI::class.java, ClientStatsUI(window, physics, network))
        window.registerUIElement(HealthUI::class.java, HealthUI())
        window.registerUIElement(CrosshairUI::class.java, CrosshairUI {
            val box = boxes[myBoxId]
            if (box != null) box.linearVelocity
            else Vector3f()
        })
        window.registerUIElement(ChatUI::class.java, ChatUI())

        // setup audio
        audioManager = AudioManager()
        audioManager.init()

        var lastFrame = System.currentTimeMillis()
        while (!window.isKeyPressed(GLFW_KEY_ESCAPE)) {
            val deltaMoveX = window.mouseDeltaX
            val deltaMoveY = window.mouseDeltaY
            val delta = System.currentTimeMillis() - lastFrame
            lastFrame = System.currentTimeMillis()
            network.pollMessages()
            update(window, deltaMoveX, deltaMoveY, delta)
            physics.simulate(delta.toDouble(), true, myBoxId)
            val cameraPosition = Vector3f(window.cameraPosX, window.cameraPosY, window.cameraPosZ)
            val cameraVector = Vector3f(cos(radians(window.cameraRotY)), 0f, sin(radians(window.cameraRotY)))
            audioManager.update(cameraPosition, cameraVector)
            window.draw()
        }
        window.destroy()
        network.close()
        audioManager.destroy()
    }

    private fun debug(msg: String) {
        window.getUIElement(ChatUI::class.java)!!.addMessage(msg)
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
                    linearVelocity = msg.linearVelocity,
                    angularVelocity = msg.angularVelocity,
                    rotation = msg.rotation,
                    mass = msg.mass,
                    affectedByPhysics = msg.affectedByPhysics,
                    textureId = msg.textureId,
                    textureMultiplier = msg.textureMultiplier,
                    bounceMultiplier = msg.bounceMultiplier,
                    isSphere = msg.isSphere,
                    isCharacter = msg.isCharacter
                )
                addBox(box)
            }
            is Messages.BoxUpdateMotion -> {
                val box = boxes[msg.id]
                if (box != null) {
                    var shouldMove = true
                    if (myBoxId == box.id) {
                        val newPos = Vector3f(msg.position)
                        // sync pos only if they report different.
                        // this is sensible to latency.
                        if (boxes[myBoxId]!!.position.distance3D(newPos) >= 1f) {
                            shouldMove = true
                        } else {
                            //shouldMove = false
                        }
                    }
                    if (shouldMove) {
                        box.rotation = msg.rotation
                        box.position = msg.position
                        box.linearVelocity = msg.linearVelocity
                        box.angularVelocity = msg.angularVelocity
                    }
                } else {
                    println("Can't find box for id ${msg.id}")
                }
            }
            is Messages.RemoveBox -> {
                val theBox = boxes[msg.boxId]
                if (theBox != null) {
                    removeBox(theBox)
                }
            }
            is Messages.SetHealth -> {
                window.getUIElement(HealthUI::class.java)!!.health = msg.health
            }
            is Messages.NotifyHit -> {
                val victimBox = boxes[msg.victimBoxId]
                val emitterBox = boxes[msg.emitterBoxId]

                if (victimBox != null && emitterBox != null) {
                    victimBox.theColor = Color4f(1f, 0f, 0f, 1f)

                    notifyHitAudio = AudioSource(
                            position = emitterBox.position,
                            volume = 1f,
                            audioId = Audios.HIT,
                            ratio = 2f
                    )
                    audioManager.registerSource(notifyHitAudio, 0)

                    thread {
                        Thread.sleep(150)
                        victimBox.theColor = Color4f(1f, 1f, 1f, 1f)
                    }
                }
            }
            is Messages.ServerMessage -> {
                window.getUIElement(ChatUI::class.java)!!.addMessage(msg.message)
            }
        }
    }

    private var lastCursorModeSwitch = System.currentTimeMillis()
    private var onDrugs = false

    /** Send input state if appropiate, and update camera pos */
    private fun update(window: Window, mouseDX: Float, mouseDY: Float, delta: Long) {

        val inputState = Messages.InputState(
            forward = window.isKeyPressed(GLFW_KEY_W),
            backwards = window.isKeyPressed(GLFW_KEY_S),
            left = window.isKeyPressed(GLFW_KEY_A),
            right = window.isKeyPressed(GLFW_KEY_D),
            fire = window.isMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT),
            jump = window.isKeyPressed(GLFW_KEY_SPACE),
            walk = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT),
            cameraX = window.cameraRotX,
            cameraY = window.cameraRotY
        )

        // sync camera pos (and delta). Pos is synced with myBoxId
        if (window.isKeyPressed(GLFW_KEY_U)) {
            if (System.currentTimeMillis() - lastCursorModeSwitch > 400) {
                lastCursorModeSwitch = System.currentTimeMillis()
                window.mouseVisible = !window.mouseVisible
            }
        }
        if (window.isKeyPressed(GLFW_KEY_K)) {
            if (System.currentTimeMillis() - lastCursorModeSwitch > 400) {
                lastCursorModeSwitch = System.currentTimeMillis()
                onDrugs = !onDrugs
            }
        }
        if (window.isKeyPressed(GLFW_KEY_L)) {
            if (System.currentTimeMillis() - lastCursorModeSwitch > 150) {
                lastCursorModeSwitch = System.currentTimeMillis()
                audioManager.registerSource(AudioSource(
                    position = Vector3f(window.cameraPosX, window.cameraPosY, window.cameraPosZ),
                    volume = 1f,
                    audioId = Audios.HIT,
                    ratio = 5f
                ), 1)
            }
        }

        val playerBox = boxes[myBoxId]
        if (playerBox != null) {
            val vel = playerBox.linearVelocity.length()
            val velOscillator = 0.5f - (timedOscillator(250) / 250f)
            doPlayerMovement(playerBox, inputState, delta)
            //window.fov = 30f + vel*0.7f
            window.cameraPosX = playerBox.position.x
            window.cameraPosY = playerBox.position.y + 0.8f// + velOscillator*vel*0.07f
            window.cameraPosZ = playerBox.position.z
        }

        val moveMillisStep = 200
        val moveMillisAmp = 400f // lower: bigger strafe
        val mouseDxAdd = if (inputState.forward || inputState.backwards || inputState.left || inputState.right) {
            (timedOscillator(moveMillisStep) - moveMillisStep/2) / moveMillisAmp
        } else {
            0f
        }
        window.cameraRotX -= mouseDY * 0.4f
        window.cameraRotY -= mouseDX * 0.4f
        //window.cameraRotZ += mouseDxAdd

        if (onDrugs) {
            window.fov = 30 + (timedOscillator(1000) / 1000f)*10f
            window.aspectRatioMultiplier = 0.8f + (timedOscillator(1200) / 1200)
        }

        // send input state if the keys changed
        if (inputState != lastSentInputState) {
            network.send(inputState)
            lastSentInputState = inputState
        }

        for ((box, source) in playerSources) {
            if (box.isCharacter) {
                val vel = box.linearVelocity.length()

                source.position = box.position
                source.volume = (vel * 0.2f).coerceAtMost(1f)
                source.pitch = (vel * 0.4f).coerceAtMost(1f)
            }
        }
    }

    private val playerSources = hashMapOf<Box, AudioSource>()

    private fun addBox(box: Box) {
        if (box.id !in boxes) {
            boxes[box.id] = box
            physics.register(box)
            window.boxes += box

            if (box.isCharacter) {
                playerSources[box] = AudioSource(
                        position = box.position,
                        volume = 1f,
                        audioId = Audios.WALK,
                        ratio = 3f
                )
                audioManager.registerSource(playerSources[box]!!, 1)
            } else if (box.isSphere) {
                shootAudio = AudioSource(
                        position = box.position,
                        volume = 0.5f,
                        audioId = Audios.SHOOT,
                        ratio = 2f
                )
                audioManager.registerSource(shootAudio, 0)
            }
        }
    }

    private fun removeBox(box: Box) {
        if (box.id in boxes) {
            boxes.remove(box.id)
            physics.unRegister(box)
            window.boxes -= box

            if (box.isCharacter) {
                audioManager.unregisterSource(playerSources[box]!!)
                playerSources.remove(box)
            } else if (box.isSphere) {
                splashAudio = AudioSource(
                        position = box.position,
                        volume = 1f,
                        audioId = Audios.SPLASH,
                        ratio = 2f
                )
                audioManager.registerSource(splashAudio, 0)
            }
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