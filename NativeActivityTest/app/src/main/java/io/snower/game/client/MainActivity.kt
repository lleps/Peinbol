package io.snower.game.client

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import io.snower.game.common.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import javax.vecmath.Color4f
import javax.vecmath.Vector3f
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

/** Entry point for the app. Initializes high-level modules and synchronizes them. */
class MainActivity : Activity() {

    companion object {
        init {
            System.loadLibrary("native-lib")
        }

        private const val TAG = "snower"
    }

    // Major modules
    private var surface: GLSurfaceView? = null
    private lateinit var physics: PhysicsInterface
    private lateinit var controls: AndroidControls
    private lateinit var window: Window
    private lateinit var network: Network.Client
    private lateinit var audioManager: AudioManager
    private lateinit var worldRenderer: WorldRenderer
    private lateinit var uiRenderer: UIRenderer
    private lateinit var assetResolver: AssetResolver

    // Player state. Must be moved to a custom game logic class to make reusable
    private val boxes = hashMapOf<Int, Box>()
    private val playerSoundSources = hashMapOf<Box, AudioSource>()
    private var myBoxId = -1

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            getWindow().decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect to the server as quick as possible, in parallel
        val host = "35.198.14.75"
        val port = 8080
        val name = "lleps"
        Log.i(TAG, "Connecting to $host:$port as '$name'...")
        network = Network.createClientAndConnect(host, port) { error ->
            if (error != null) {
                Log.e(TAG, "Can't connect to $host:$port", error)
                runOnUiThread { finishActivity(1) }
            } else {
                Log.i(TAG, "Connected!")
                network.send(Messages.ConnectionInfo(name))
            }
        }
        network.onServerMessage { msg -> handleNetworkMessage(msg) }

        // Preload assets. Too slow to be blocking, must be moved somewhere else.
        Log.i(TAG, "Loading assets...")
        physics = BulletPhysicsNativeImpl().apply { init() }
        assetResolver = AndroidAssetResolver(assets)
        worldRenderer = WorldRenderer(assetResolver, GLESImpl(), physics, AndroidMatrixOps())
        worldRenderer.preloadAssets()

        // Create controls instance
        controls = AndroidControls(worldRenderer)

        // Ensure GLES 2 support
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val configurationInfo = activityManager.deviceConfigurationInfo
        val supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000
        if (!supportsEs2) error("Device doesn't support OpenGL ES 2")

        // Create surface and pass touch events to the AndroidControls instance
        surface = GLSurfaceView(this)
        surface!!.setEGLContextClientVersion(2)
        surface!!.setRenderer(RendererWrapper())
        surface!!.setOnTouchListener { v, event ->
            controls.handleTouchEvent(event)
            true
        }
        setContentView(surface)
    }

    inner class RendererWrapper : GLSurfaceView.Renderer {
        private var frameReportCounter = 0

        override fun onDrawFrame(gl: GL10?) {
            network.pollMessages()
            update(window, 0f, 0f, 16)
            val physicsTime = measureTimeMillis { physics.simulate(16, true, myBoxId) }
            val worldDrawTime = measureTimeMillis { worldRenderer.draw() }
            val uiDrawTime = measureTimeMillis { uiRenderer.draw() }
            if (frameReportCounter++ % 10 == 0) {
                Log.i(TAG, "Physics time: $physicsTime, world draw time: $worldDrawTime, ui draw time: $uiDrawTime")
            }
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            Log.i(TAG, "Surface changed to: $width x $height")
            worldRenderer.setResolution(width, height)
            uiRenderer.setResolution(width, height)
        }

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            audioManager = AudioManager()
            audioManager.init()

            worldRenderer.init()

            uiRenderer = NuklearUIRenderer(assetResolver)
            uiRenderer.preloadAssets()
            uiRenderer.init()

            window = Window(assetResolver) // TODO remove this. is totally useless

            uiRenderer.registerUIElement(ClientStatsUI::class.java, ClientStatsUI(window, physics, network))
            uiRenderer.registerUIElement(HealthUI::class.java, HealthUI())
            uiRenderer.registerUIElement(CrosshairUI::class.java, CrosshairUI {
                val box = boxes[myBoxId]
                box?.linearVelocity ?: Vector3f()
            })
            uiRenderer.registerUIElement(ChatUI::class.java, ChatUI())
            uiRenderer.registerUIElement(PlayersInfoUI::class.java, PlayersInfoUI())
            uiRenderer.registerUIElement(AndroidControls::class.java, controls)
        }
    }

    override fun onResume() {
        super.onResume()
        println("onResume()")
        surface!!.onResume()
    }

    override fun onPause() {
        super.onPause()
        println("onPause()")
        surface!!.onPause()
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
                uiRenderer.getUIElement(HealthUI::class.java)!!.health = msg.health
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
                uiRenderer.getUIElement(ChatUI::class.java)!!.addMessage(msg.message)
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
        worldRenderer.cameraRotY -= controls.getCameraRotationX()
        worldRenderer.cameraRotX -= controls.getCameraRotationY()

        val inputState = Messages.InputState(
            forward = controls.checkForward(),
            backwards = controls.checkBackwards(),
            left = controls.checkLeft(),
            right = controls.checkRight(),
            fire = false,
            fire2 = false,
            jump = false,
            walk = false,
            cameraX = worldRenderer.cameraRotX,
            cameraY = worldRenderer.cameraRotY
        )

        controls.readDone()

        // Camera pos update
        val playerBox = boxes[myBoxId]
        if (playerBox != null) {
            doPlayerMovement(playerBox, inputState, delta)
            worldRenderer.cameraPosX = playerBox.position.x
            worldRenderer.cameraPosY = playerBox.position.y + 0.8f
            worldRenderer.cameraPosZ = playerBox.position.z
        }

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
