package com.peinbol.server

import com.peinbol.*
import javax.vecmath.Color4f
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
        val name: String,
        val connection: Network.PlayerConnection,
        val collisionBox: Box,
        var inputState: Messages.InputState,
        var lastShot: Long = 0L,
        var health: Int = 100
    )

    private lateinit var physics: Physics
    private lateinit var network: Network.Server
    private var boxes = mutableListOf<Box>()
    private var bulletsAddTimestamp = hashMapOf<Box, Long>()
    private var bulletEmitter = hashMapOf<Box, Player>()
    private val playersByConnections = hashMapOf<Network.PlayerConnection, Player>()

    fun run() {
        println("Init network...")
        network = Network.createServer("0.0.0.0", 8080)
        network.onConnect { client -> handleConnection(client) }
        network.onDisconnect { client -> handleDisconnection(client) }
        network.onClientMessage { client, message -> handleClientMessage(client, message) }

        println("Init main thread...")
        physics = Physics(Physics.Mode.SERVER)
        physics.init()
        physics.onCollision { box1, box2 -> handleCollision(box1, box2) }
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
            physics.simulate(delta.toDouble(), true)
            if (System.currentTimeMillis() - lastBroadcast > 16) {
                broadcastCurrentWorldState()
                lastBroadcast = System.currentTimeMillis()
            }
            globalUpdate()
            Thread.sleep(16)
        }
    }

    private fun generateWorld() {
        // build boxes
        for (i in 0..20) {
            val box = Box(
                id = generateId(),
                mass = 3f,
                position = Vector3f(
                    randBetween(-40, 40).toFloat(),
                    2f,
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
            position = Vector3f(0f, 0f, 0f),
            size = Vector3f(100f, 1f, 100f),
            affectedByPhysics = false,
            textureId = Textures.GRASS_ID,
            textureMultiplier = 50.0
        )
        addBox(base)

        // build 4 walls
        addBox(Box(
            id = generateId(),
            position = Vector3f(-50f, -15f, 0f),
            size = Vector3f(2f, 50f, 100f),
            affectedByPhysics = false,
            textureId = Textures.BRICKS_GREY_ID,
            textureMultiplier = 10.0
        ))
        addBox(Box(
            id = generateId(),
            position = Vector3f(50f, -15f, 0f),
            size = Vector3f(2f, 50f, 100f),
            affectedByPhysics = false,
            textureId = Textures.BRICKS_GREY_ID,
            textureMultiplier = 10.0
        ))
        addBox(Box(
            id = generateId(),
            position = Vector3f(0f, -15f, -50f),
            size = Vector3f(100f, 50f, 2f),
            affectedByPhysics = false,
            textureId = Textures.BRICKS_GREY_ID,
            textureMultiplier = 10.0
        ))
        addBox(Box(
            id = generateId(),
            position = Vector3f(0f, -15f, 50f),
            size = Vector3f(100f, 50f, 2f),
            affectedByPhysics = false,
            textureId = Textures.BRICKS_GREY_ID,
            textureMultiplier = 10.0
        ))

        // some random walls
        for (i in 0..25) {
            val length = 6f
            val axis = randBetween(0, 2)
            val height = randBetween(3, 10).toFloat()
            addBox(Box(
                id = generateId(),
                position = Vector3f(
                    randBetween(-50, 50).toFloat(),
                    2f,
                    randBetween(-50, 50).toFloat()
                ),
                size = Vector3f(
                    if (axis == 0) 2f else length,
                    height,
                    if (axis == 0) length else 2f
                ),
                affectedByPhysics = false,
                mass = 0f,
                textureId = Textures.METAL_ID,
                textureMultiplier = 1.0
            ))
        }
    }

    private fun handleCollision(box1: Box, box2: Box) {
        var sphere: Box? = null
        var other: Box? = null
        if (box1.isSphere) {
            sphere = box1
            other = box2
        } else if (box2.isSphere) {
            sphere = box2
            other = box1
        }

        if (sphere != null && other != null) {
            // check if other box corresponds to a player
            val shotTarget = playersByConnections.values.firstOrNull { it.collisionBox == other }
            val shotEmitter = bulletEmitter[sphere]

            if (shotEmitter != null && shotTarget != null) {
                onPlayerHitPlayer(shotEmitter, shotTarget, sphere)
            }
            removeBox(sphere)
            bulletsAddTimestamp.remove(sphere)
        }
    }

    private fun onPlayerHitPlayer(emitter: Player, target: Player, ball: Box) {
        target.health -= 10
        network.send(Messages.SetHealth(target.health), target.connection)
        network.broadcast(Messages.NotifyHit(emitter.collisionBox.id, target.collisionBox.id))
        if (target.health <= 0) {
            val messageFormat = listOf(
                "{killer} se la dio a {victim}",
                "{killer} no tuvo piedad con {victim}",
                "{victim} no midio las consecuencias al meterse con {killer}",
                "{victim} no vio venir a {killer}",
                "la bala de {killer} atravezo la cabeza de {victim}",
                "{killer} esta dominando a {victim}"
            ).random()
            val msg = messageFormat
                .replace("{victim}", target.name)
                .replace("{killer}", emitter.name)
            broadcastMessage(msg)

            target.health = 100
            network.send(Messages.SetHealth(target.health), target.connection)

            target.collisionBox.angularVelocity = Vector3f()
            target.collisionBox.position = Vector3f(randBetween(-20, 20).toFloat(), 5f, randBetween(-20, 20).toFloat())
        }
    }

    private fun broadcastMessage(message: String) {
        network.broadcast(Messages.ServerMessage(message))
    }

    /** Update boxes motion for all players */
    private fun broadcastCurrentWorldState() {
        for (box in boxes) {
            if (box.shouldTransmit && box.rigidBody!!.isActive) {
                network.broadcast(Messages.BoxUpdateMotion(
                    id = box.id,
                    position = box.position,
                    linearVelocity = box.linearVelocity,
                    angularVelocity = box.angularVelocity,
                    rotation = box.rotation
                ))
                if (box.isSphere) box.shouldTransmit = false // transmit spheres only once, to save pps
            }
        }
    }

    private fun handleConnection(connection: Network.PlayerConnection) {
        println("Connection request: $connection")
    }

    private fun handleDisconnection(connection: Network.PlayerConnection) {
        val player = playersByConnections[connection]
        if (player != null) {
            println("Player disconnected: ${player.name}")
            playersByConnections.remove(connection)
            removeBox(player.collisionBox)
            broadcastMessage("${player.name} se desconecto.")
        }
    }

    private fun handleClientMessage(connection: Network.PlayerConnection, message: Any) {
        val player = playersByConnections[connection]
        when (message) {
            is Messages.ConnectionInfo -> {
                if (player != null) return

                // build box
                val playerBox = Box(
                    id = generateId(),
                    mass = 30f,
                    position = Vector3f(
                        randBetween(-20, 20).toFloat(),
                        15f,
                        randBetween(-20, 20).toFloat()
                    ),
                    size = Vector3f(
                        1f,
                        2f,
                        1f
                    ),
                    textureId = Textures.METAL_ID,
                    affectedByPhysics = false,
                    bounceMultiplier = 0.0f,
                    textureMultiplier = 0.01,
                    isCharacter = true
                )
                addBox(playerBox)

                // build player
                val newPlayer = Player(message.name, connection, playerBox, Messages.InputState())
                playersByConnections[connection] = newPlayer

                // stream current boxes and spawn
                for (worldBox in boxes) network.send(buildStreamBoxMsg(worldBox), connection)
                network.send(Messages.Spawn(playerBox.id), connection)
                println("Player connected: ${newPlayer.name}")
                broadcastMessage("${newPlayer.name} se conecto.")
            }
            is Messages.InputState -> {
                if (player == null) return
                player.inputState = message
            }
        }
    }

    /** Misc routines the server may do. */
    private fun globalUpdate() {
        // del bullets after 15 secs
        for ((bullet, timestamp) in bulletsAddTimestamp.toList()) {
            if (System.currentTimeMillis() - timestamp > 8000) {
                bulletsAddTimestamp.remove(bullet)
                removeBox(bullet)
            }
        }
    }

    /** Update [player] collision box based on the given [inputState]. */
    private fun updatePlayer(player: Player, inputState: Messages.InputState, delta: Long) {
        doPlayerMovement(player.collisionBox, inputState, delta)

        // Shot
        if (inputState.fire && System.currentTimeMillis() - player.lastShot > 200) {
            player.lastShot = System.currentTimeMillis()
            val shotForce = 300.0f
            val frontPos = 1.5f
            val box = Box(
                id = generateId(),
                mass = 3f,
                position = player.collisionBox.position + Vector3f(0f, 0.8f, 0f) + vectorFront(inputState.cameraY, inputState.cameraX, frontPos),
                size = Vector3f(0.2f, 0.2f, 0.2f),
                textureId = Textures.METAL_ID,
                textureMultiplier = 1.0,
                bounceMultiplier = 0.8f,
                theColor = Color4f(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat(), 1f),
                isSphere = true
            )
            addBox(box)
            bulletsAddTimestamp[box] = System.currentTimeMillis()
            bulletEmitter[box] = player
            box.applyForce(vectorFront(inputState.cameraY, inputState.cameraX, shotForce))
        }
        if (inputState.fire2 && System.currentTimeMillis() - player.lastShot > 1000) {
            player.lastShot = System.currentTimeMillis()
            val shotForce = 300.0f
            val frontPos = 1.5f
            repeat(5) { idx ->
                val i = idx - 2
                val angleXOrientation = inputState.cameraY// + (i * 20f)
                val angleXOrigin = inputState.cameraY + (i * 20f)
                val box = Box(
                    id = generateId(),
                    mass = 3f,
                    position = player.collisionBox.position + Vector3f(0f, 0.8f, 0f) + vectorFront(angleXOrigin, inputState.cameraX, frontPos),
                    size = Vector3f(0.2f, 0.2f, 0.2f),
                    textureId = Textures.METAL_ID,
                    textureMultiplier = 1.0,
                    bounceMultiplier = 0.8f,
                    theColor = Color4f(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat(), 1f),
                    isSphere = true
                )
                addBox(box)
                bulletsAddTimestamp[box] = System.currentTimeMillis()
                bulletEmitter[box] = player
                box.applyForce(vectorFront(angleXOrientation, inputState.cameraX, shotForce))
            }
        }
    }

    private fun buildStreamBoxMsg(box: Box): Messages.BoxAdded {
        return Messages.BoxAdded(
            id = box.id,
            position = box.position,
            size = box.size,
            linearVelocity = box.linearVelocity,
            angularVelocity = box.angularVelocity,
            rotation = box.rotation,
            mass = box.mass,
            affectedByPhysics = box.affectedByPhysics,
            textureId = box.textureId,
            textureMultiplier = box.textureMultiplier,
            bounceMultiplier = box.bounceMultiplier,
            color = box.theColor,
            isSphere = box.isSphere,
            isCharacter = box.isCharacter
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
            network.broadcast(Messages.RemoveBox(box.id))
        }
    }
}