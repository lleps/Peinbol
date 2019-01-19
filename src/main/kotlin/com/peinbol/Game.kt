package com.peinbol

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import org.lwjgl.glfw.GLFW.*
import java.util.concurrent.ConcurrentHashMap
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
    private lateinit var networldChannel: Channel
    private var lastInputStateSent: Long = 0L
    private var networkInitialized = false
    private var lastShot: Long = 0
    private var mouseDownMillis: Long = 0
    private val boxes = ConcurrentHashMap<Int, Box>()
    private var myBoxId = -1

    fun init(args: Array<String>) {
        val host = args[0]
        val port = args[1].toInt()

        println("Init window...")
        window = Window()
        window.init()
        txts
        window.centerCursor()
        physics = Physics()

        println("Init network...")
        thread { initNetwork(host, port) }
        while (!networkInitialized) Thread.sleep(50)

        var lastFrame = System.currentTimeMillis()
        while (!window.isKeyPressed(GLFW_KEY_ESCAPE)) {
            val deltaMoveX = window.mouseX - 100.0 // since was centered on last frame, get how much moved since then
            val deltaMoveY = window.mouseY - 100.0
            val delta = System.currentTimeMillis() - lastFrame
            lastFrame = System.currentTimeMillis()
            update(window, deltaMoveX, deltaMoveY, delta.toDouble())
            physics.simulate(delta.toDouble())
            window.centerCursor()
            window.draw()
        }
        window.destroy()
    }

    /** Connects to the server and set networkInitialized to true */
    private fun initNetwork(host: String, port: Int) {
        val workerGroup = NioEventLoopGroup()

        try {
            val b = Bootstrap() // (1)
            b.group(workerGroup) // (2)
            b.channel(NioSocketChannel::class.java) // (3)
            b.option(ChannelOption.SO_KEEPALIVE, true) // (4)
            b.handler(object : ChannelInitializer<SocketChannel>() {
                @Throws(Exception::class)
                public override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(Messages.MessagesDecoder(), ClientNetworkHandler())
                }
            })

            println("Connecting to $host:$port...")
            val f = b.connect(host, port).sync() // (5)
            networldChannel = f.channel()
            networkInitialized = true
            println("Connected!")
            f.channel().closeFuture().sync()
        } catch (e: Exception) {
            println("Error connecting: $e")
            e.printStackTrace(System.err)
            System.exit(1)
        } finally {
            workerGroup.shutdownGracefully()
        }
    }

    /** Send input state if appropiate, and update camera pos */
    private fun update(window: Window, mouseDX: Double, mouseDY: Double, delta: Double) {
        // sync camera pos (and delta). Pos is synced with myBoxId
        val playerBox = boxes[myBoxId]
        if (playerBox != null) {
            window.cameraPosX = playerBox.x
            window.cameraPosY = playerBox.y + 0.8
            window.cameraPosZ = playerBox.z
        }

        window.cameraRotX -= mouseDY * 0.4
        window.cameraRotY -= mouseDX * 0.4

        // send input state
        if (System.currentTimeMillis() - lastInputStateSent > INPUT_SYNC_RATE) {
            lastInputStateSent = System.currentTimeMillis()
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
            Messages.send(networldChannel, inputState)
        }
    }

    private fun addBox(box: Box) {
        if (box.id !in boxes) {
            println("add box: $box")
            boxes[box.id] = box
            physics.boxes += box
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

    /** Handle all packets from the server, updating internal state */
    private inner class ClientNetworkHandler : ChannelInboundHandlerAdapter() {
        override fun channelRead(ctx: ChannelHandlerContext, msgRaw: Any) {
            val buf = msgRaw as ByteBuf // (1)
            try {
                val msg = Messages.receive(buf)
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
                            x = msg.x, y = msg.y, z = msg.z,
                            sx = msg.sx, sy = msg.sy, sz = msg.sz,
                            vx = msg.vx, vy = msg.vy, vz = msg.vz,
                            affectedByPhysics = msg.affectedByPhysics
                        )
                        if (!box.affectedByPhysics) {
                            //box.txt = "wood"
                            box.txtMultiplier = 50.0
                        } else {
                            box.txt = txts.randomElement()
                        }
                        addBox(box)
                    }
                    is Messages.BoxUpdateMotion -> {
                        val box = boxes[msg.id]
                        if (box != null) {
                            box.x = msg.x
                            box.y = msg.y
                            box.z = msg.z
                            box.vx = msg.vx
                            box.vy = msg.vy
                            box.vz = msg.vz
                        } else {
                            println("Can't find box for id ${msg.id}")
                        }
                    }

                }
            } finally {
                buf.release()
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            ctx.close()
        }
    }
}