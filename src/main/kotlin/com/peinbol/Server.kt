package com.peinbol

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

/**
 * The server should be responsible of:
 * - Accepting connections
 * - Simulating the world
 * - Reporting to clients world data over a socket
 * - Fetching client keys and updating their states according
 */
class Server {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val server = Server()
            server.run()
        }
    }

    private class Player(
        val collisionBox: Box,
        var inputState: Messages.InputState,
        val channel: Channel,
        var lastShot: Long = 0L
    )

    private lateinit var physics: Physics
    private lateinit var networkChannel: Channel
    private var players = mutableListOf<Player>()
    private var boxes = mutableListOf<Box>()
    private var mainThreadTasks = ConcurrentLinkedQueue<() -> Unit>()
    private var networkInitialized = false
    private val playersByHandlers = hashMapOf<ServerConnectionHandler, Player>()

    fun run() {
        println("Init network thread...")
        thread { initNetworkThread() }
        while (!networkInitialized) Thread.sleep(50)

        println("Init main thread...")
        physics = Physics()
        generateWorld()
        var lastPhysicsSimulate = System.currentTimeMillis()
        var lastBroadcast = System.currentTimeMillis()
        while (true) {
            while (mainThreadTasks.isNotEmpty()) mainThreadTasks.poll()()
            val delta = System.currentTimeMillis() - lastPhysicsSimulate
            lastPhysicsSimulate = System.currentTimeMillis()
            for (player in players) updatePlayer(player, player.inputState, delta)
            physics.simulate(delta.toDouble())
            if (System.currentTimeMillis() - lastBroadcast > 25) {
                broadcastCurrentWorldState()
                lastBroadcast = System.currentTimeMillis()
            }
            Thread.sleep(16)
        }
    }

    private fun generateWorld() {
        // build boxes
        for (i in 0..20) {
            val box = Box(
                id = generateId(),
                x = randBetween(-40, 40).toDouble(),
                y = randBetween(-5, 5).toDouble(),
                z = randBetween(-40, 40).toDouble(),
                sx = 1.0,
                sy = 1.0,
                sz = 1.0,
                affectedByPhysics = true
            )
            addBox(box)
        }

        // build base
        val base = Box(
            id = generateId(),
            x = 0.0, y = -50.0, z = 0.0,
            sx = 100.0, sy = 1.0, sz = 100.0,
            affectedByPhysics = false,
            txtMultiplier = 50.0
        )
        addBox(base)
    }

    /** Update boxes motion for all players */
    private fun broadcastCurrentWorldState() {
        // only if changed...
        for (box in boxes) {
            val msg = Messages.BoxUpdateMotion(
                id = box.id,
                x = box.x,
                y = box.y,
                z = box.z,
                vx = box.vx,
                vy = box.vy,
                vz = box.vz
            )
            //println("broadcast for $box")
            for (p in players) Messages.send(p.channel, msg)
        }
    }

    private fun initNetworkThread() {
        val bossGroup = NioEventLoopGroup() // (1)
        val workerGroup = NioEventLoopGroup()
        try {
            val b = ServerBootstrap() // (2)
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java) // (3)
                .childHandler(object : ChannelInitializer<SocketChannel>() { // (4)
                    @Throws(Exception::class)
                    public override fun initChannel(ch: SocketChannel) {
                        ch.pipeline().addLast(Messages.MessagesDecoder(), ServerConnectionHandler())
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)          // (5)
                .childOption(ChannelOption.SO_KEEPALIVE, true) // (6)

            println("Binding...")
            val channelFuture = b.bind("0.0.0.0", 8080)
            println("Done! Listening on :8080")
            networkChannel = channelFuture.channel()
            networkInitialized = true
            channelFuture.sync()
            networkChannel.closeFuture().sync() // shut down gracefully
        } catch (e: Exception) {
            println("Can't bind: $e")
            e.printStackTrace(System.err)
            System.exit(1)
        } finally {
            workerGroup.shutdownGracefully()
            bossGroup.shutdownGracefully()
        }
    }

    /** Update [player] collision box based on the given [inputState]. */
    private fun updatePlayer(player: Player, inputState: Messages.InputState, delta: Long) {
        val deltaSec = delta / 1000.0
        var speed = 1.5

        if (!player.collisionBox.inGround) speed *= 0.5
        if (inputState.walk && player.collisionBox.inGround) speed = 0.3

        if (inputState.forward) {
            player.collisionBox.vx -= Math.sin(Math.toRadians(inputState.cameraY)) * speed * deltaSec
            player.collisionBox.vz -= Math.cos(Math.toRadians(inputState.cameraY)) * speed * deltaSec
        }
        if (inputState.backwards) {
            player.collisionBox.vx += Math.sin(Math.toRadians(inputState.cameraY)) * speed * deltaSec
            player.collisionBox.vz += Math.cos(Math.toRadians(inputState.cameraY)) * speed * deltaSec
        }
        if (inputState.right) {
            player.collisionBox.vx += Math.sin(Math.toRadians(inputState.cameraY + 90.0)) * speed * deltaSec
            player.collisionBox.vz += Math.cos(Math.toRadians(inputState.cameraY + 90.0)) * speed * deltaSec
        }
        if (inputState.left) {
            player.collisionBox.vx += Math.sin(Math.toRadians(inputState.cameraY - 90.0)) * speed * deltaSec
            player.collisionBox.vz += Math.cos(Math.toRadians(inputState.cameraY - 90.0)) * speed * deltaSec
        }

        if (inputState.jump && player.collisionBox.inGround) {
            player.collisionBox.vy += 0.3
        }

        if (inputState.fire && System.currentTimeMillis() - player.lastShot > 100) {
            player.lastShot = System.currentTimeMillis()
            val shotSpeed = 1.5*0.5
            val frontPos = 0.6
            val box = Box(
                id = generateId(),
                x = player.collisionBox.x + -Math.sin(Math.toRadians(inputState.cameraY)) * frontPos,
                y = player.collisionBox.y,
                z = player.collisionBox.z + -Math.cos(Math.toRadians(inputState.cameraY)) * frontPos,
                sx = 0.2, sy = 0.2, sz = 0.2,
                vx = -Math.sin(Math.toRadians(inputState.cameraY)) * shotSpeed,
                vy = 0.3*0.5, // TODO should be based on cameraX
                vz = -Math.cos(Math.toRadians(inputState.cameraY)) * shotSpeed
            )
            addBox(box)
        }
    }

    private fun streamBox(channel: Channel, box: Box) {
        val msg = Messages.BoxAdded(
            id = box.id,
            x = box.x,
            y = box.y,
            z = box.z,
            sx = box.sx,
            sy = box.sy,
            sz = box.sz,
            vx = box.vx,
            vy = box.vy,
            vz = box.vz,
            affectedByPhysics = box.affectedByPhysics
        )
        Messages.send(channel, msg)
    }

    private fun addBox(box: Box) {
        physics.boxes += box
        boxes.add(box)
        for (p in players) streamBox(p.channel, box)
    }

    private fun addPlayer(player: Player) {
        physics.boxes += player.collisionBox
        players.add(player)
        addBox(player.collisionBox)
    }

    private inner class ServerConnectionHandler : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            println("Player connected (${ctx.channel().remoteAddress()})")

            // Create the player
            val playerBox = Box(
                id = generateId(),
                x = randBetween(-20, 20).toDouble(),
                y = 10.0,
                z = randBetween(-20, 20).toDouble(),
                sx = 0.5,
                sy = 2.0,
                sz = 0.5,
                affectedByPhysics = true
            )
            val player = Player(playerBox, Messages.InputState(), ctx.channel())
            addPlayer(player)
            playersByHandlers[this] = player

            // Stream all the current boxes
            println("Streaming all the boxes...")
            for (worldBox in boxes) streamBox(ctx.channel(), worldBox)

            // Spawn
            println("Spawning...")
            Messages.send(ctx.channel(), Messages.Spawn(playerBox.id))
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            println("Player disconnected: ${ctx.channel().remoteAddress()}")
            playersByHandlers.remove(this)
        }

        override fun channelRead(ctx: ChannelHandlerContext, rawMsg: Any) {
            val buf = rawMsg as ByteBuf
            val player = playersByHandlers[this] ?: error("can't get a player for handler $this")
            val msg = Messages.receive(buf)
            when (msg) {
                is Messages.InputState -> {
                    player.inputState = msg
                }
            }
            buf.release()
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            ctx.close()
        }
    }
}