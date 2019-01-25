package com.peinbol.server

import com.peinbol.*
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
            textureId = Textures.GRASS_ID,
            textureMultiplier = 50.0
        )
        addBox(base)

        // build 4 walls
        addBox(Box(
            id = generateId(),
            position = Vector3f(-50f, -45f, 0f),
            size = Vector3f(2f, 10f, 100f),
            affectedByPhysics = false,
            textureId = Textures.CLOTH_ID,
            textureMultiplier = 35.0
        ))
        addBox(Box(
            id = generateId(),
            position = Vector3f(50f, -45f, 0f),
            size = Vector3f(2f, 10f, 100f),
            affectedByPhysics = false,
            textureId = Textures.CLOTH_ID,
            textureMultiplier = 35.0
        ))
        addBox(Box(
            id = generateId(),
            position = Vector3f(0f, -45f, -50f),
            size = Vector3f(100f, 10f, 2f),
            affectedByPhysics = false,
            textureId = Textures.CLOTH_ID,
            textureMultiplier = 35.0
        ))
        addBox(Box(
            id = generateId(),
            position = Vector3f(0f, -45f, 50f),
            size = Vector3f(100f, 10f, 2f),
            affectedByPhysics = false,
            textureId = Textures.CLOTH_ID,
            textureMultiplier = 35.0
        ))

        // some random walls
        for (i in 0..randBetween(10, 40)) {
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
                    if (axis == 0) 2f else length,
                    9f,
                    if (axis == 0) length else 2f
                ),
                affectedByPhysics = false,
                mass = 0f,
                textureId = Textures.METAL_ID,
                textureMultiplier = 35.0
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

            if (shotEmitter != null) {
                if (shotTarget != null) {
                    shotTarget.health -= 10
                    network.send(Messages.SetHealth(shotTarget.health), shotTarget.connection)
                    network.broadcast(Messages.NotifyHit(shotEmitter.collisionBox.id, shotTarget.collisionBox.id))
                    if (shotTarget.health <= 0) {
                        shotTarget.connection.channel.close()
                        val messageFormat = listOf(
                            "{killer} se la dio a {victim}",
                            "{killer} no tuvo piedad con {victim}",
                            "{victim} no midio las consecuencias al meterse con {killer}",
                            "{victim} no vio venir a{killer}",
                            "la bala de {killer} atravezo la cabeza de {victim}",
                            "{killer} esta dominando a {victim}"
                        ).random()
                        val msg = messageFormat
                            .replace("{victim}", shotTarget.name)
                            .replace("{killer}", shotEmitter.name)
                        network.broadcast(Messages.ServerMessage(msg))
                    }
                }
            }
            removeBox(sphere)
            bulletsAddTimestamp.remove(sphere)
        }
    }

    /** Update boxes motion for all players */
    private fun broadcastCurrentWorldState() {
        for (box in boxes) {
            if (box.rigidBody!!.isActive) {
                network.broadcast(Messages.BoxUpdateMotion(
                    id = box.id,
                    position = box.position,
                    linearVelocity = box.linearVelocity,
                    angularVelocity = box.angularVelocity,
                    rotation = box.rotation
                ))
            }
        }
    }

    private fun handleConnection(connection: Network.PlayerConnection) {
        println("Player connected: $connection")

        // build box
        val playerBox = Box(
            id = generateId(),
            mass = 30f,
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
            affectedByPhysics = false,
            bounceMultiplier = 0.0f,
            textureMultiplier = 0.01,
            isCharacter = true
        )
        addBox(playerBox)

        // build player
        val name = listOf(
            "Bugger",
            "Cabbie",
            "Rooster",
            "Prometheus",
            "Hyper",
            "Midas",
            "Thrasher",
            "Zod",
            "CoolDog",
            "CodeExia",
            "IceDog",
            "Shay",
            "Prone"
        ).random()
        val player = Player(name, connection, playerBox, Messages.InputState())
        playersByConnections[connection] = player

        // stream current boxes and spawn
        for (worldBox in boxes) network.send(buildStreamBoxMsg(worldBox), connection)
        network.send(Messages.Spawn(playerBox.id), connection)
        network.broadcast(Messages.ServerMessage("$name se conecto."))
    }

    private fun handleDisconnection(connection: Network.PlayerConnection) {
        val player = playersByConnections[connection]!!
        println("Player disconnected: $connection")
        playersByConnections.remove(connection)
        removeBox(player.collisionBox)
        network.broadcast(Messages.ServerMessage("${player.name} se desconecto."))
    }

    private fun handleClientMessage(connection: Network.PlayerConnection, message: Any) {
        val player = playersByConnections[connection]!!
        when (message) {
            is Messages.InputState -> {
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
            val shotForce = 120.0f
            val frontPos = 1.5f
            val box = Box(
                id = generateId(),
                mass = 1f,
                position = player.collisionBox.position + Vector3f(0f, 0.8f, 0f) + vectorFront(inputState.cameraY, inputState.cameraX, frontPos),
                size = Vector3f(0.1f, 0.0f, 0.0f),
                textureId = Textures.RUBIK_ID,
                textureMultiplier = 1.0,
                bounceMultiplier = 0.8f,
                isSphere = true
            )
            addBox(box)
            bulletsAddTimestamp[box] = System.currentTimeMillis()
            bulletEmitter[box] = player
            box.applyForce(vectorFront(inputState.cameraY, inputState.cameraX, shotForce))
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