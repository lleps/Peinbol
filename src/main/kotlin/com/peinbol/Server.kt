package com.peinbol

import javax.vecmath.Vector3f

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
        physics.init()
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
                mass = 1f,
                position = Vector3f(
                    randBetween(-40, 40).toFloat(),
                    randBetween(-5, 5).toFloat(),
                    randBetween(-40, 40).toFloat()
                ),
                size = Vector3f(1f, 1f, 1f),
                textureId = listOf(
                    Textures.CLOTH_ID,
                    Textures.METAL_ID,
                    Textures.CREEPER_ID,
                    Textures.FOOTBALL_ID,
                    Textures.RUBIK_ID
                ).random(),
                affectedByPhysics = true
            )
            addBox(box)
        }

        // build base
        val base = Box(
            id = generateId(),
            position = Vector3f(0f, -50f, 0f),
            size = Vector3f(100f, 1f, 100f),
            affectedByPhysics = false,
            textureId = Textures.WOOD_ID,
            textureMultiplier = 50.0
        )
        addBox(base)

        // build 4 walls
        addBox(Box(
            id = generateId(),
            position = Vector3f(-50f, -45f, 0f),
            size = Vector3f(2f, 10f, 100f),
            affectedByPhysics = false,
            textureId = Textures.METAL_ID,
            textureMultiplier = 35.0
        ))
        addBox(Box(
            id = generateId(),
            position = Vector3f(50f, -45f, 0f),
            size = Vector3f(2f, 10f, 100f),
            affectedByPhysics = false,
            textureId = Textures.METAL_ID,
            textureMultiplier = 35.0
        ))
        addBox(Box(
            id = generateId(),
            position = Vector3f(0f, -45f, -50f),
            size = Vector3f(100f, 10f, 2f),
            affectedByPhysics = false,
            textureId = Textures.METAL_ID,
            textureMultiplier = 35.0
        ))
        addBox(Box(
            id = generateId(),
            position = Vector3f(0f, -45f, 50f),
            size = Vector3f(100f, 10f, 2f),
            affectedByPhysics = false,
            textureId = Textures.METAL_ID,
            textureMultiplier = 35.0
        ))

        // some random walls
        for (i in 0..randBetween(10, 100)) {
            val length = randBetween(5, 10).toFloat()
            val axis = randBetween(0, 2)
            addBox(Box(
                id = generateId(),
                position = Vector3f(
                    randBetween(-50, 50).toFloat(),
                    -45f,
                    randBetween(-50, 50).toFloat()
                ),
                size = Vector3f(
                    if (axis == 0) 1f else length,
                    10f,
                    if (axis == 0) length else 1f
                ),
                affectedByPhysics = false,
                textureId = Textures.METAL_ID,
                textureMultiplier = 35.0
            ))
        }
    }

    /** Update boxes motion for all players */
    private fun broadcastCurrentWorldState() {
        for (box in boxes) {
            network.broadcast(Messages.BoxUpdateMotion(
                id = box.id,
                position = box.position,
                velocity = box.velocity,
                angularVelocity = box.angularVelocity
            ))
        }
    }

    private fun handleConnection(connection: Network.PlayerConnection) {
        println("Player connected: $connection")

        // build box
        val playerBox = Box(
            id = generateId(),
            mass = 60f,
            position = Vector3f(
                randBetween(-20, 20).toFloat(),
                10f,
                randBetween(-20, 20).toFloat()
            ),
            size = Vector3f(
                1f,
                2f,
                1f
            ),
            textureId = Textures.METAL_ID,
            affectedByPhysics = true,
            bounceMultiplier = 0.0f,
            textureMultiplier = 0.01
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
        playersByConnections.remove(connection)
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

    private var lastJump = System.currentTimeMillis()

    /** Update [player] collision box based on the given [inputState]. */
    private fun updatePlayer(player: Player, inputState: Messages.InputState, delta: Long) {
        val deltaSec = delta / 1000f
        var force = 5000f

        // TODO: find a way to balance this, and set a high friction on the ground or something like that.

        // Shift
        if (inputState.walk && player.collisionBox.inGround) force *= 0.2f

        // W,A,S,D
        var velVector = Vector3f()
        if (inputState.forward) velVector += vectorFront(inputState.cameraY, 0f, force * deltaSec)
        if (inputState.backwards) velVector -= vectorFront(inputState.cameraY, 0f, force * deltaSec)
        if (inputState.right) velVector -= vectorFront(inputState.cameraY + 90f, 0f, force * deltaSec)
        if (inputState.left) velVector -= vectorFront(inputState.cameraY - 90f, 0f, force * deltaSec)
        player.collisionBox.applyForce(velVector)

        // Jump
        if (inputState.jump && System.currentTimeMillis() - lastJump > 1000) {
            lastJump = System.currentTimeMillis()
            player.collisionBox.applyForce(Vector3f(0f, force * 3f * deltaSec, 0f))
        }

        // Shot
        if (inputState.fire && System.currentTimeMillis() - player.lastShot > 450) {
            player.lastShot = System.currentTimeMillis()
            val shotForce = 200.0f
            val frontPos = 1.2f
            val box = Box(
                id = generateId(),
                mass = 1f,
                position = player.collisionBox.position + vectorFront(inputState.cameraY, inputState.cameraX, frontPos),
                size = Vector3f(0.2f, 0.2f, 0.2f),
                textureId = Textures.RUBIK_ID,
                textureMultiplier = 0.01,
                bounceMultiplier = 0.8f
            )
            addBox(box)
            box.applyForce(vectorFront(inputState.cameraY, inputState.cameraX, shotForce))
        }
    }

    private fun buildStreamBoxMsg(box: Box): Messages.BoxAdded {
        return Messages.BoxAdded(
            id = box.id,
            position = box.position,
            size = box.size,
            velocity = box.velocity,
            mass = box.mass,
            affectedByPhysics = box.affectedByPhysics,
            textureId = box.textureId,
            textureMultiplier = box.textureMultiplier,
            bounceMultiplier = box.bounceMultiplier
        )
    }

    private fun addBox(box: Box) {
        physics.register(box)
        boxes.add(box)
        network.broadcast(buildStreamBoxMsg(box))
    }

    private fun removeBox(box: Box) {
        if (box in boxes) {
            physics.unRegister(box)
            boxes.remove(box)
        }
    }
}