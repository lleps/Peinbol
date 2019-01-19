package com.peinbol

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
        val connection: Network.PlayerConnection,
        val collisionBox: Box,
        var inputState: Messages.InputState,
        var lastShot: Long = 0L
    )

    private lateinit var physics: Physics
    private lateinit var network: Network.Server
    private var boxes = mutableListOf<Box>()
    private val playersByConnections = hashMapOf<Network.PlayerConnection, Player>()

    fun run() {
        println("Init network...")
        network = Network.createServer("0.0.0.0", 8080)
        network.onConnect { client -> handleConnection(client) }
        network.onDisconnect { client -> handleDisconnection(client) }
        network.onClientMessage { client, message -> handleClientMessage(client, message) }

        println("Init main thread...")
        physics = Physics()
        generateWorld()
        var lastPhysicsSimulate = System.currentTimeMillis()
        var lastBroadcast = System.currentTimeMillis()
        while (true) {
            val delta = System.currentTimeMillis() - lastPhysicsSimulate
            lastPhysicsSimulate = System.currentTimeMillis()
            network.pollMessages()
            for (player in playersByConnections.values) {
                updatePlayer(player, player.inputState, delta)
            }
            physics.simulate(delta.toDouble())
            if (System.currentTimeMillis() - lastBroadcast > 10) {
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
        for (box in boxes) { // TODO update only moved boxes
            network.broadcast(Messages.BoxUpdateMotion(
                id = box.id,
                x = box.x,
                y = box.y,
                z = box.z,
                vx = box.vx,
                vy = box.vy,
                vz = box.vz
            ))
        }
    }

    private fun handleConnection(connection: Network.PlayerConnection) {
        println("Player connected: $connection")

        // build box
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
        addBox(playerBox)

        // build player
        val player = Player(connection, playerBox, Messages.InputState())
        playersByConnections[connection] = player

        // stream current boxes and spawn
        for (worldBox in boxes) network.send(buildStreamBoxMsg(worldBox), connection)
        network.send(Messages.Spawn(playerBox.id), connection)
    }

    private fun handleDisconnection(connection: Network.PlayerConnection) {
        val player = playersByConnections[connection]!!
        println("Player disconnected: $connection")
        removeBox(player.collisionBox)
    }

    private fun handleClientMessage(connection: Network.PlayerConnection, message: Any) {
        val player = playersByConnections[connection]!!
        when (message) {
            is Messages.InputState -> {
                player.inputState = message
            }
        }
    }

    /** Update [player] collision box based on the given [inputState]. */
    private fun updatePlayer(player: Player, inputState: Messages.InputState, delta: Long) {
        val deltaSec = delta / 1000.0
        var speed = 1.5

        if (!player.collisionBox.inGround) speed *= 0.8
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

    private fun buildStreamBoxMsg(box: Box): Messages.BoxAdded {
        return Messages.BoxAdded(
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
    }

    private fun addBox(box: Box) {
        physics.boxes += box
        boxes.add(box)
        network.broadcast(buildStreamBoxMsg(box))
    }

    private fun removeBox(box: Box) {
        if (box in boxes) {
            physics.boxes -= box
            boxes.remove(box)
            // TODO we need a message to remove boxes!
        }
    }
}