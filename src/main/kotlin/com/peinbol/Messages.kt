package com.peinbol

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ReplayingDecoder


/**
 * To define, serialize and deserialize data to be sent over the wire.
 * Pretty much bound to netty.
 */
object Messages {
    // client-to-server
    private const val SYNC_INPUT_STATE = 1 // to report my keys and camera data

    // server-to-client
    private const val SPAWN_BOX = 2 // to create a box (included other players!)
    private const val UPDATE_BOX = 3 // To update a box position and velocity
    private const val SPAWN_PLAYER = 4 // to attach the client to the given box

    /** How many extra bytes carry the message for the given type */
    private val MESSAGE_TYPE_BYTES = mapOf(
        SYNC_INPUT_STATE to (1*7)+(8*2),
        SPAWN_PLAYER to 4,
        UPDATE_BOX to 4+(8*3)+(8*3),
        SPAWN_BOX to 4+(8*3)+(8*3)+(8*3)+1
    )

    /** Use this to ensure messages are divided by frames */
    class MessagesDecoder : ReplayingDecoder<Void>() {
        override fun decode(ctx: ChannelHandlerContext, inBuf: ByteBuf, out: MutableList<Any>) {
            val type = inBuf.readInt()
            val msgByteCount = MESSAGE_TYPE_BYTES[type]!!
            val payload = inBuf.readBytes(msgByteCount)
            val newBuf = ctx.alloc().buffer(4 + msgByteCount)
            newBuf.writeInt(type)
            newBuf.writeBytes(payload)
            out.add(newBuf)
        }
    }

    /** Get the message from the stream. */
    fun receive(buf: ByteBuf): Any {
        val msgType = buf.readInt()
        return when (msgType) {
            SYNC_INPUT_STATE -> readInputState(buf)
            SPAWN_PLAYER -> readSpawn(buf)
            SPAWN_BOX -> readBoxAdded(buf)
            UPDATE_BOX -> readBoxUpdateMotion(buf)
            else -> error("invalid message type: $msgType")
        }
    }

    /** Send a message to the given channel */
    fun send(channel: Channel, obj: Any) {
        val messageType = when(obj) {
            is InputState -> SYNC_INPUT_STATE
            is Spawn -> SPAWN_PLAYER
            is BoxAdded -> SPAWN_BOX
            is BoxUpdateMotion -> UPDATE_BOX
            else -> error("invalid message obj: $obj")
        }

        val buf = channel.alloc().buffer(4 + MESSAGE_TYPE_BYTES[messageType]!!)
        buf.writeInt(messageType)
        when (obj) {
            is InputState -> writeInputState(obj, buf)
            is Spawn -> writeSpawn(obj, buf)
            is BoxAdded -> writeBoxAdded(obj, buf)
            is BoxUpdateMotion -> writeBoxUpdateMotion(obj, buf)
            else -> error("invalid message obj: $obj")
        }
        channel.writeAndFlush(buf)
    }

    /** Encapsulate a player's key state on a given moment */
    class InputState(
        val forward: Boolean = false,
        val backwards: Boolean = false,
        val left: Boolean = false,
        val right: Boolean = false,
        val fire: Boolean = false,
        val jump: Boolean = false,
        val walk: Boolean = false,
        val cameraX: Double = 0.0,
        val cameraY: Double = 0.0
    )

    private fun readInputState(byteBuf: ByteBuf): InputState {
        return InputState(
            byteBuf.readBoolean(),
            byteBuf.readBoolean(),
            byteBuf.readBoolean(),
            byteBuf.readBoolean(),
            byteBuf.readBoolean(),
            byteBuf.readBoolean(),
            byteBuf.readBoolean(),
            byteBuf.readDouble(),
            byteBuf.readDouble()
        )
    }

    private fun writeInputState(obj: InputState, byteBuf: ByteBuf) {
        byteBuf.writeBoolean(obj.forward)
        byteBuf.writeBoolean(obj.backwards)
        byteBuf.writeBoolean(obj.left)
        byteBuf.writeBoolean(obj.right)
        byteBuf.writeBoolean(obj.fire)
        byteBuf.writeBoolean(obj.jump)
        byteBuf.writeBoolean(obj.walk)
        byteBuf.writeDouble(obj.cameraX)
        byteBuf.writeDouble(obj.cameraY)
    }

    /** Spawn a player, specify their box id */
    class Spawn(
        val boxId: Int
    )

    private fun writeSpawn(obj: Spawn, byteBuf: ByteBuf) {
        byteBuf.writeInt(obj.boxId)
    }

    private fun readSpawn(byteBuf: ByteBuf): Spawn {
        return Spawn(
            byteBuf.readInt()
        )
    }

    /** Add some box to the world. */
    class BoxAdded(
        val id: Int,// 4+(8*3)+(8*3)+(8*3)+1
        val x: Double, val y: Double, val z: Double,
        val sx: Double, val sy: Double, val sz: Double,
        val vx: Double, val vy: Double, val vz: Double,
        val affectedByPhysics: Boolean
    )

    private fun writeBoxAdded(obj: BoxAdded, byteBuf: ByteBuf) {
        byteBuf.writeInt(obj.id)
        byteBuf.writeDouble(obj.x)
        byteBuf.writeDouble(obj.y)
        byteBuf.writeDouble(obj.z)
        byteBuf.writeDouble(obj.sx)
        byteBuf.writeDouble(obj.sy)
        byteBuf.writeDouble(obj.sz)
        byteBuf.writeDouble(obj.vx)
        byteBuf.writeDouble(obj.vy)
        byteBuf.writeDouble(obj.vz)
        byteBuf.writeBoolean(obj.affectedByPhysics)
    }

    private fun readBoxAdded(byteBuf: ByteBuf): BoxAdded {
        return BoxAdded(
            byteBuf.readInt(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readBoolean()
        )
    }

    /** Update box movement */
    class BoxUpdateMotion( // 4+(8*3)+(8*3)
        val id: Int,
        val x: Double, val y: Double, val z: Double,
        val vx: Double, val vy: Double, val vz: Double
    )

    private fun writeBoxUpdateMotion(obj: BoxUpdateMotion, byteBuf: ByteBuf) {
        byteBuf.writeInt(obj.id)
        byteBuf.writeDouble(obj.x)
        byteBuf.writeDouble(obj.y)
        byteBuf.writeDouble(obj.z)
        byteBuf.writeDouble(obj.vx)
        byteBuf.writeDouble(obj.vy)
        byteBuf.writeDouble(obj.vz)
    }

    private fun readBoxUpdateMotion(byteBuf: ByteBuf): BoxUpdateMotion {
        return BoxUpdateMotion(
            byteBuf.readInt(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readDouble(),
            byteBuf.readDouble()
        )
    }
}