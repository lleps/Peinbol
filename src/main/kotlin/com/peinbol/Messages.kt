package com.peinbol

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ReplayingDecoder
import java.lang.StringBuilder
import javax.vecmath.Color4f
import javax.vecmath.Quat4f
import javax.vecmath.Vector3f
import kotlin.reflect.KClass

/**
 * To define, serialize and deserialize data to be exchanged between client/server.
 * Pretty much bound to netty.
 */
object Messages {
    interface MessageType<T> {
        val bytes: Int
        fun write(msg: T, buf: ByteBuf)
        fun read(buf: ByteBuf): T
    }

    private val messageTypes = mutableMapOf<Int, MessageType<out Any>>()
    private val classesId = mutableMapOf<Class<*>, Int>()

    private fun registerMessageType(id: Int, type: MessageType<out Any>, clazz: KClass<out Any>) {
        messageTypes[id] = type
        classesId[clazz.java] = id
    }

    init {
        // client-to-server
        registerMessageType(0, ConnectionInfo, ConnectionInfo::class)
        registerMessageType(1, InputState, InputState::class)
        // server-to-client
        registerMessageType(2, Spawn, Spawn::class)
        registerMessageType(3, BoxAdded, BoxAdded::class)
        registerMessageType(4, BoxUpdateMotion, BoxUpdateMotion::class)
        registerMessageType(5, RemoveBox, RemoveBox::class)
        registerMessageType(6, SetHealth, SetHealth::class)
        registerMessageType(7, NotifyHit, NotifyHit::class)
        registerMessageType(8, ServerMessage, ServerMessage::class)
        registerMessageType(9, Ping, Ping::class) // used as ping and pong
    }

    /** Wraps the message ID and their body */
    class MessageWrapper(val id: Int, val buf: ByteBuf)

    /** Decoder to make messages processed properly, in chunks of message-size */
    class MessagesDecoder : ReplayingDecoder<Void>() {
        override fun decode(ctx: ChannelHandlerContext, inBuf: ByteBuf, out: MutableList<Any>) {
            val typeId = inBuf.readInt()
            val type = messageTypes[typeId] ?: return//error("invalid typeId on decode: $typeId")
            val msgByteCount = type.bytes
            out.add(MessageWrapper(typeId, inBuf.readBytes(msgByteCount)))
        }
    }

    /** Read a message from the given MessageWrapper. */
    fun receive(wrapper: MessageWrapper): Any {
        val typeId = wrapper.id
        val type = messageTypes[typeId] ?: error("invalid typeId on receive: $typeId")
        return type.read(wrapper.buf)
    }

    /** Send a message to the given channel. Return the number of bytes of the message. */
    @Suppress("UNCHECKED_CAST")
    fun send(channel: Channel, obj: Any): Int {
        val typeId = classesId[obj::class.java] ?: error("invalid obj class on send: ${obj::class.java}")
        val type = messageTypes[typeId]!! as MessageType<Any>
        val buf = channel.alloc().buffer(4 + type.bytes)
        buf.writeInt(typeId)
        type.write(obj, buf)
        channel.writeAndFlush(buf)
        return type.bytes
    }

    // Messages

    /** Encapsulate a player's key state on a given moment */
    data class InputState(
        val forward: Boolean = false, // 1
        val backwards: Boolean = false, // 1
        val left: Boolean = false, // 1
        val right: Boolean = false, // 1
        val fire: Boolean = false, // 1
        val fire2: Boolean = false, // 1
        val jump: Boolean = false, // 1
        val walk: Boolean = false, // 1
        val cameraX: Float = 0f, // 4
        val cameraY: Float = 0f // 4
    ) {
        companion object : MessageType<InputState> {
            override val bytes: Int
                get() = (1*8) + (4*2)

            override fun write(msg: InputState, buf: ByteBuf) {
                buf.writeBoolean(msg.forward)
                buf.writeBoolean(msg.backwards)
                buf.writeBoolean(msg.left)
                buf.writeBoolean(msg.right)
                buf.writeBoolean(msg.fire)
                buf.writeBoolean(msg.fire2)
                buf.writeBoolean(msg.jump)
                buf.writeBoolean(msg.walk)
                buf.writeFloat(msg.cameraX)
                buf.writeFloat(msg.cameraY)
            }

            override fun read(buf: ByteBuf): InputState {
                return InputState(
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readFloat(),
                    buf.readFloat()
                )
            }
        }
    }

    /** Spawn a player, specify their box id */
    class Spawn(
        val boxId: Int // 4
    ) {
        companion object : MessageType<Spawn> {
            override val bytes: Int
                get() = 4

            override fun write(msg: Spawn, buf: ByteBuf) {
                buf.writeInt(msg.boxId)
            }

            override fun read(buf: ByteBuf): Spawn {
                return Spawn(buf.readInt())
            }
        }
    }

    private fun ByteBuf.writeVector3f(vector: Vector3f) {
        writeFloat(vector.x)
        writeFloat(vector.y)
        writeFloat(vector.z)
    }

    private fun ByteBuf.readVector3f(): Vector3f {
        return Vector3f(readFloat(), readFloat(), readFloat())
    }

    private fun ByteBuf.writeQuat4f(quat: Quat4f) {
        writeFloat(quat.x)
        writeFloat(quat.y)
        writeFloat(quat.z)
        writeFloat(quat.w)
    }

    private fun ByteBuf.writeColor4f(color: Color4f) {
        writeFloat(color.x)
        writeFloat(color.y)
        writeFloat(color.z)
        writeFloat(color.w)
    }

    private fun ByteBuf.readColor4f(): Color4f {
        return Color4f(readFloat(), readFloat(), readFloat(), readFloat())
    }

    private fun ByteBuf.readQuat4f(): Quat4f {
        return Quat4f(readFloat(), readFloat(), readFloat(), readFloat())
    }

    /** Writes [maxLength]*2 bytes in the buffer with [string] on it. Throws an exception if the string is too long. */
    private fun ByteBuf.writeString(string: String, maxLength: Int) {
        if (string.length > maxLength) error("string length too big (${string.length}, max $maxLength)")
        val stringLength = string.length
        repeat(maxLength) { i ->
            if (i < stringLength) {
                writeChar(string[i].toInt())
            } else {
                writeChar(' '.toInt())
            }
        }
    }

    /** Read [length]*2 bytes as a string. Trims end spaces. */
    private fun ByteBuf.readString(length: Int): String {
        val result = StringBuilder()
        repeat(length) { i ->
            result.append(readChar())
        }
        return result.toString().trimEnd()
    }

    /** Add some box to the world. */
    class BoxAdded(
        val id: Int, // 4
        val position: Vector3f, // 4*3
        val size: Vector3f, // 4*3
        val linearVelocity: Vector3f, // 4*3
        val angularVelocity: Vector3f, // 4*3
        val rotation: Quat4f, // 4*4
        val mass: Float, // 4
        val affectedByPhysics: Boolean, // 1
        val textureId: Int, // 4
        val textureMultiplier: Double, // 8
        val bounceMultiplier: Float, // 4
        val color: Color4f, // 4*4
        val isSphere: Boolean, // 1
        val isCharacter: Boolean // 1
    ) {
        companion object : MessageType<BoxAdded> {
            override val bytes: Int
                get() = 4+(4*3)+(4*3)+(4*3)+(4*3)+(4*4)+4+1+4+8+4+(4*4)+1+1

            override fun write(msg: BoxAdded, buf: ByteBuf) {
                buf.writeInt(msg.id)
                buf.writeVector3f(msg.position)
                buf.writeVector3f(msg.size)
                buf.writeVector3f(msg.linearVelocity)
                buf.writeVector3f(msg.angularVelocity)
                buf.writeQuat4f(msg.rotation)
                buf.writeFloat(msg.mass)
                buf.writeBoolean(msg.affectedByPhysics)
                buf.writeInt(msg.textureId)
                buf.writeDouble(msg.textureMultiplier)
                buf.writeFloat(msg.bounceMultiplier)
                buf.writeColor4f(msg.color)
                buf.writeBoolean(msg.isSphere)
                buf.writeBoolean(msg.isCharacter)
            }

            override fun read(buf: ByteBuf): BoxAdded {
                return BoxAdded(
                    buf.readInt(),
                    buf.readVector3f(),
                    buf.readVector3f(),
                    buf.readVector3f(),
                    buf.readVector3f(),
                    buf.readQuat4f(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readDouble(),
                    buf.readFloat(),
                    buf.readColor4f(),
                    buf.readBoolean(),
                    buf.readBoolean()
                )
            }
        }
    }

    /** Update box movement */
    class BoxUpdateMotion(
        val id: Int, // 4
        val position: Vector3f, // 4*3
        val linearVelocity: Vector3f, // 4*3
        val angularVelocity: Vector3f, // 4*3
        val rotation: Quat4f // 4*4
    ) {
        companion object : MessageType<BoxUpdateMotion> {
            override val bytes: Int
                get() = 4+(4*3)+(4*3)+(4*3)+(4*4)

            override fun write(msg: BoxUpdateMotion, buf: ByteBuf) {
                buf.writeInt(msg.id)
                buf.writeVector3f(msg.position)
                buf.writeVector3f(msg.linearVelocity)
                buf.writeVector3f(msg.angularVelocity)
                buf.writeQuat4f(msg.rotation)
            }

            override fun read(buf: ByteBuf): BoxUpdateMotion {
                return BoxUpdateMotion(
                    buf.readInt(),
                    buf.readVector3f(),
                    buf.readVector3f(),
                    buf.readVector3f(),
                    buf.readQuat4f()
                )
            }
        }
    }


    /** Remove a box */
    class RemoveBox(
        val boxId: Int // 4
    ) {
        companion object : MessageType<RemoveBox> {
            override val bytes: Int
                get() = 4

            override fun write(msg: RemoveBox, buf: ByteBuf) {
                buf.writeInt(msg.boxId)
            }

            override fun read(buf: ByteBuf): RemoveBox {
                return RemoveBox(buf.readInt())
            }
        }
    }

    /** Notify hit, set player health. */
    class SetHealth(
        val health: Int // 4
    ) {
        companion object : MessageType<SetHealth> {
            override val bytes: Int
                get() = 4

            override fun write(msg: SetHealth, buf: ByteBuf) {
                buf.writeInt(msg.health)
            }

            override fun read(buf: ByteBuf): SetHealth {
                return SetHealth(buf.readInt())
            }
        }
    }

    /** To notify when someone hits another player and apply corresponding visual effects. */
    class NotifyHit(
            val emitterBoxId: Int, // 4
            val victimBoxId: Int // 4
    ) {
        companion object : MessageType<NotifyHit> {
            override val bytes: Int
                get() = 8

            override fun write(msg: NotifyHit, buf: ByteBuf) {
                buf.writeInt(msg.emitterBoxId)
                buf.writeInt(msg.victimBoxId)
            }

            override fun read(buf: ByteBuf): NotifyHit {
                return NotifyHit(buf.readInt(), buf.readInt())
            }
        }
    }

    /** To check latency */
    class Ping {
        companion object : MessageType<Ping> {
            override val bytes: Int
                get() = 0

            override fun write(msg: Ping, buf: ByteBuf) {
            }

            override fun read(buf: ByteBuf) = Ping()
        }
    }

    /** To send chat messages to clients */
    class ServerMessage(
        val message: String // 1024
    ) {
        companion object : MessageType<ServerMessage> {
            override val bytes: Int
                get() = 1024

            override fun write(msg: ServerMessage, buf: ByteBuf) {
                var strTruncated = ""
                if (msg.message.length > 512) {
                    strTruncated = msg.message.substring(0, 512)
                } else if (msg.message.length < 512) {
                    strTruncated = msg.message
                    while (strTruncated.length < 512) {
                        strTruncated += " "
                    }
                }

                for (char in strTruncated) {
                    buf.writeChar(char.toInt())
                }
            }

            override fun read(buf: ByteBuf): ServerMessage {
                var str = ""
                var i = 0
                while (i++ < 512) {
                    str += buf.readChar()
                }
                return ServerMessage(str.trim())
            }
        }
    }

    /** To notify the server about the player info. */
    class ConnectionInfo(
        val name: String // 40 chars max (40*8 bytes)
    ) {
        companion object : MessageType<ConnectionInfo> {
            override val bytes: Int
                get() = 40*2 // 2 bytes per string character

            override fun write(msg: ConnectionInfo, buf: ByteBuf) {
                buf.writeString(msg.name, 40)
            }

            override fun read(buf: ByteBuf): ConnectionInfo {
                return ConnectionInfo(buf.readString(40))
            }
        }
    }
}