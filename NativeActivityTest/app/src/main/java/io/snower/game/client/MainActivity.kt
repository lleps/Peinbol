package io.snower.game.client

import android.app.ActivityManager
import android.content.Context
import android.opengl.GLSurfaceView
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import io.snower.game.common.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.vecmath.Color4f
import javax.vecmath.Vector3f
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

/** Entry point for the app. Initializes high-level modules and synchronizes them. */
class MainActivity : AppCompatActivity() {

    companion object {

        init {
            System.loadLibrary("native-lib")
        }

        private const val TAG = "snower"
    }

    private var glSurfaceView: GLSurfaceView? = null
    private lateinit var physics: PhysicsInterface
    private lateinit var window: Window
    private lateinit var network: Network.Client
    private lateinit var audioManager: AudioManager
    private lateinit var worldRenderer: WorldRenderer
    private lateinit var assetResolver: AssetResolver

    // player state
    private val boxes = hashMapOf<Int, Box>()
    private val playerSoundSources = hashMapOf<Box, AudioSource>()
    private var myBoxId = -1

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
        if (supportsEs2) {
            glSurfaceView = GLSurfaceView(this)
            glSurfaceView!!.setEGLContextClientVersion(2)
            glSurfaceView!!.setRenderer(RendererWrapper())
            setContentView(glSurfaceView)
        } else {
            error("This device doesn't support OpenGL ES 2.0")
        }
    }

    inner class RendererWrapper : GLSurfaceView.Renderer {
        override fun onDrawFrame(gl: GL10?) {
            network.pollMessages()
            update(window, window.mouseDeltaX, window.mouseDeltaY, 16)
            val physicsTime = measureTimeMillis { physics.simulate(16, true, myBoxId) }
            val drawTime = measureTimeMillis { worldRenderer.draw() }
            Log.i(TAG, "Physics time: $physicsTime, draw time: $drawTime")
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            Log.i(TAG, "Change resolution to $width x $height")
            worldRenderer.setResolution(width, height)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            val host = "35.198.14.75"
            val port = 8080
            val name = "lleps"

            println("Connecting to $host:$port as '$name'...")
            network = Network.createClient(host, port)
            network.onServerMessage { msg -> handleNetworkMessage(msg) }
            network.send(Messages.ConnectionInfo(name))
            println("ok.")

            println("Setup audio...")
            audioManager = AudioManager()
            audioManager.init()
            println("ok.")

            println("Setup physics...")
            physics = Physics(Physics.Mode.CLIENT).apply { init() }
            println("ok.")

            assetResolver = AndroidAssetResolver(assets)
            worldRenderer = WorldRenderer(assetResolver, GLESImpl(), physics)

            println("Loading renderer assets...")
            worldRenderer.loadAssets()
            println("ok.")

            println("Initializing renderer...")
            worldRenderer.init()

            println("Initialize window...")
            window = Window(assetResolver)
            window.init()
            println("Everything initialized!")

            PhysicsImpl()
        }
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView!!.onPause()
    }


    /** Called when a message from the server arrives. */
    private fun handleNetworkMessage(msg: Any) {
        when (msg) {
            is Messages.Spawn -> {
                myBoxId = msg.boxId
                val myBox = boxes[myBoxId]
                if (myBox != null) {
                    worldRenderer.removeBox(myBox)
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
                println(msg.message)
                //window.getUIElement(ChatUI::class.java)!!.addMessage(msg.message)
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
    private fun update(window: Window, mouseDX: Float, mouseDY: Float, delta: Int) {
        val inputState = Messages.InputState(
            forward = false,
            backwards = false,
            left = false,
            right = false,
            fire = false,
            fire2 = false,
            jump = false,
            walk = false,
            cameraX = worldRenderer.cameraRotX,
            cameraY = worldRenderer.cameraRotY
        )

        // Camera update
        val playerBox = boxes[myBoxId]
        if (playerBox != null) {
            doPlayerMovement(playerBox, inputState, delta)
            worldRenderer.cameraPosX = playerBox.position.x
            worldRenderer.cameraPosY = playerBox.position.y + 0.8f
            worldRenderer.cameraPosZ = playerBox.position.z
        }
        worldRenderer.cameraRotX -= mouseDY * 0.4f
        worldRenderer.cameraRotY -= mouseDX * 0.4f

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
            worldRenderer.addBox(box)

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
            worldRenderer.removeBox(box)

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

    external fun init(width: Int, height: Int)
    external fun step()
}
