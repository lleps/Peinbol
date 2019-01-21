package com.peinbol

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ReplayingDecoder
import javax.vecmath.Quat4f
import javax.vecmath.Vector3f
import kotlin.reflect.KClass


/**
 * To define, serialize and deserialize data to be sent over the wire.
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
        registerMessageType(0, InputState, InputState::class)
        registerMessageType(1, Spawn, Spawn::class)
        registerMessageType(2, BoxAdded, BoxAdded::class)
        registerMessageType(3, BoxUpdateMotion, BoxUpdateMotion::class)
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

    /** Send a message to the given channel */
    @Suppress("UNCHECKED_CAST")
    fun send(channel: Channel, obj: Any) {
        val typeId = classesId[obj::class.java] ?: error("invalid obj class on send: ${obj::class.java}")
        val type = messageTypes[typeId]!! as MessageType<Any>
        val buf = channel.alloc().buffer(4 + type.bytes)
        buf.writeInt(typeId)
        type.write(obj, buf)
        channel.writeAndFlush(buf)
    }

    // Messages

    /** Encapsulate a player's key state on a given moment */
    data class InputState(
        val forward: Boolean = false, // 1
        val backwards: Boolean = false, // 1
        val left: Boolean = false, // 1
        val right: Boolean = false, // 1
        val fire: Boolean = false, // 1
        val jump: Boolean = false, // 1
        val walk: Boolean = false, // 1
        val cameraX: Float = 0f, // 4
        val cameraY: Float = 0f // 4
    ) {
        companion object : MessageType<InputState> {
            override val bytes: Int
                get() = (1*7) + (4*2)

            override fun write(msg: InputState, buf: ByteBuf) {
                buf.writeBoolean(msg.forward)
                buf.writeBoolean(msg.backwards)
                buf.writeBoolean(msg.left)
                buf.writeBoolean(msg.right)
                buf.writeBoolean(msg.fire)
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

    private fun ByteBuf.readQuat4f(): Quat4f {
        return Quat4f(readFloat(), readFloat(), readFloat(), readFloat())
    }

    /** Add some box to the world. */
    // TODO: add quat to boxAdded
    class BoxAdded(
        val id: Int, // 4
        val position: Vector3f, // 4*3
        val size: Vector3f, // 4*3
        val velocity: Vector3f, // 4*3
        val mass: Float, // 4
        val affectedByPhysics: Boolean, // 1
        val textureId: Int, // 4
        val textureMultiplier: Double, // 8
        val bounceMultiplier: Float // 4
    ) {
        companion object : MessageType<BoxAdded> {
            override val bytes: Int
                get() = 4+(4*3)+(4*3)+(4*3)+4+1+4+8+4

            override fun write(msg: BoxAdded, buf: ByteBuf) {
                buf.writeInt(msg.id)
                buf.writeVector3f(msg.position)
                buf.writeVector3f(msg.size)
                buf.writeVector3f(msg.velocity)
                buf.writeFloat(msg.mass)
                buf.writeBoolean(msg.affectedByPhysics)
                buf.writeInt(msg.textureId)
                buf.writeDouble(msg.textureMultiplier)
                buf.writeFloat(msg.bounceMultiplier)
            }

            override fun read(buf: ByteBuf): BoxAdded {
                return BoxAdded(
                    buf.readInt(),
                    buf.readVector3f(),
                    buf.readVector3f(),
                    buf.readVector3f(),
                    buf.readFloat(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readDouble(),
                    buf.readFloat()
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
}