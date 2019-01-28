package com.peinbol.client

import com.peinbol.*
import org.lwjgl.glfw.GLFW.*
import javax.vecmath.Color4f
import javax.vecmath.Vector3f
import kotlin.concurrent.thread
import kotlin.math.absoluteValue
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

    // modules
    private lateinit var physics: Physics
    private lateinit var window: Window
    private lateinit var network: Network.Client
    private lateinit var audioManager: AudioManager

    // player state
    private val boxes = hashMapOf<Int, Box>()
    private val playerSoundSources = hashMapOf<Box, AudioSource>()
    private var myBoxId = -1

    fun init(args: Array<String>) {
        val host = args[0]
        val port = args[1].toInt()
        val name = args[2]
        if (name.length > 40) error("name too long: $name")

        println("Connecting to $host:$port as $name...")
        network = Network.createClient(host, port)
        network.onServerMessage { msg -> handleNetworkMessage(msg) }
        network.send(Messages.ConnectionInfo(name))
        network.pollMessages()

        println("Setup audio...")
        audioManager = AudioManager()
        audioManager.init()

        println("Setup physics...")
        physics = Physics(Physics.Mode.CLIENT)
        physics.init()

        println("Setup window and OpenGL...")
        window = Window()
        window.init()

        println("Setup UI...")
        window.registerUIElement(ClientStatsUI::class.java, ClientStatsUI(window, physics, network))
        window.registerUIElement(HealthUI::class.java, HealthUI())
        window.registerUIElement(CrosshairUI::class.java, CrosshairUI {
            val box = boxes[myBoxId]
            if (box != null) box.linearVelocity
            else Vector3f()
        })
        window.registerUIElement(ChatUI::class.java, ChatUI())
        window.registerUIElement(PlayersInfoUI::class.java, PlayersInfoUI())

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
                    theColor = msg.color,
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
                    audioManager.registerSource(AudioSource(
                        position = emitterBox.position,
                        volume = 1f,
                        audioId = Audios.HIT,
                        ratio = 2f,
                        loop = false
                    ))

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

    // useful variables to track input
    private var lastKeyPressLock = System.currentTimeMillis()
    private var onDrugs = false
    private var showPlayerInfo = false
    private var tabDownLast = false
    private var lastSentInputState = Messages.InputState()

    /** Send input state if appropiate, and update camera pos */
    private fun update(window: Window, mouseDX: Float, mouseDY: Float, delta: Long) {
        val inputState = Messages.InputState(
            forward = window.isKeyPressed(GLFW_KEY_W),
            backwards = window.isKeyPressed(GLFW_KEY_S),
            left = window.isKeyPressed(GLFW_KEY_A),
            right = window.isKeyPressed(GLFW_KEY_D),
            fire = window.isMouseButtonDown(GLFW_MOUSE_BUTTON_LEFT),
            fire2 = window.isMouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT),
            jump = window.isKeyPressed(GLFW_KEY_SPACE),
            walk = window.isKeyPressed(GLFW_KEY_LEFT_SHIFT),
            cameraX = window.cameraRotX,
            cameraY = window.cameraRotY
        )

        // Useful keys to toggle some experimental features
        if (window.isKeyPressed(GLFW_KEY_U)) {
            if (System.currentTimeMillis() - lastKeyPressLock > 400) {
                lastKeyPressLock = System.currentTimeMillis()
                window.mouseVisible = !window.mouseVisible
            }
        }
        if (window.isKeyPressed(GLFW_KEY_K)) {
            if (System.currentTimeMillis() - lastKeyPressLock > 400) {
                lastKeyPressLock = System.currentTimeMillis()
                onDrugs = !onDrugs
            }
        }
        if (window.isKeyPressed(GLFW_KEY_L)) {
            if (System.currentTimeMillis() - lastKeyPressLock > 150) {
                lastKeyPressLock = System.currentTimeMillis()
                audioManager.registerSource(AudioSource(
                    position = Vector3f(window.cameraPosX, window.cameraPosY, window.cameraPosZ),
                    volume = 1f,
                    audioId = Audios.WALK,
                    ratio = 5f,
                    loop = true
                ))
            }
        }

        // Scoreboard
        val tabDownNow = window.isKeyPressed(GLFW_KEY_TAB)
        if (tabDownNow != tabDownLast) {
            showPlayerInfo = !showPlayerInfo
            tabDownLast= tabDownNow

            window.getUIElement(ChatUI::class.java)!!.visible = !showPlayerInfo
            window.getUIElement(ClientStatsUI::class.java)!!.visible = !showPlayerInfo
            window.getUIElement(CrosshairUI::class.java)!!.visible = !showPlayerInfo
            window.getUIElement(HealthUI::class.java)!!.visible = !showPlayerInfo

            window.getUIElement(PlayersInfoUI::class.java)!!.visible = showPlayerInfo
        }

        // Camera update
        val playerBox = boxes[myBoxId]
        if (playerBox != null) {
            doPlayerMovement(playerBox, inputState, delta)
            window.cameraPosX = playerBox.position.x
            window.cameraPosY = playerBox.position.y + 0.8f
            window.cameraPosZ = playerBox.position.z
        }
        window.cameraRotX -= mouseDY * 0.4f
        window.cameraRotY -= mouseDX * 0.4f

        // Drugs test
        if (onDrugs) {
            window.fov = 30 + (timedOscillator(1000) / 1000f)*10f
            window.aspectRatioMultiplier = 0.8f + (timedOscillator(1200) / 1200)
        }

        // send input state if the keys changed
        if (inputState != lastSentInputState) {
            network.send(inputState)
            lastSentInputState = inputState
        }

        // Update player sound souces
        for ((box, source) in playerSoundSources) {
            if (box.isCharacter) {
                val vel = box.linearVelocity.length()

                source.position = box.position

                if (vel < 0.5f) {
                    source.volume = 0f
                } else {
                    source.volume = (vel * 0.4f).coerceAtMost(1f)
                    source.pitch = (vel * 0.4f).coerceIn(0.5f, 1f)
                }
            }
        }
    }

    private fun addBox(box: Box) {
        if (box.id !in boxes) {
            boxes[box.id] = box
            physics.register(box)
            window.boxes += box

            if (box.isCharacter) {
                playerSoundSources[box] = AudioSource(
                    position = box.position,
                    volume = 1f,
                    audioId = Audios.WALK,
                    ratio = 3f,
                    loop = true
                )
                audioManager.registerSource(playerSoundSources[box]!!)
            } else if (box.isSphere) {
                audioManager.registerSource(AudioSource(
                    position = box.position,
                    volume = 0.5f,
                    audioId = Audios.SHOOT,
                    ratio = 2f,
                    loop = false
                ))
            }
        }
    }

    private fun removeBox(box: Box) {
        if (box.id in boxes) {
            boxes.remove(box.id)
            physics.unRegister(box)
            window.boxes -= box

            if (box.isCharacter) {
                audioManager.unregisterSource(playerSoundSources[box]!!)
                playerSoundSources.remove(box)
            } else if (box.isSphere) {
                audioManager.registerSource(AudioSource(
                    position = box.position,
                    volume = 1f,
                    audioId = Audios.SPLASH,
                    ratio = 2f,
                    loop = false
                ))
            }
        }
    }
}