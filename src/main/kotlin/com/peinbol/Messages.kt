package com.peinbol

import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ReplayingDecoder
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

    /** Decoder to make messages processed properly, in chunks of message-size */
    class MessagesDecoder : ReplayingDecoder<Void>() {
        override fun decode(ctx: ChannelHandlerContext, inBuf: ByteBuf, out: MutableList<Any>) {
            val typeId = inBuf.readInt()
            val type = messageTypes[typeId] ?: error("invalid typeId on decode: $typeId")
            val msgByteCount = type.bytes
            val payload = inBuf.readBytes(msgByteCount)
            val newBuf = ctx.alloc().buffer(4 + msgByteCount)
            newBuf.writeInt(typeId)
            newBuf.writeBytes(payload)
            out.add(newBuf)
        }
    }

    /** Read a message from the given buffer. */
    fun receive(buf: ByteBuf): Any {
        val typeId = buf.readInt()
        val type = messageTypes[typeId] ?: error("invalid typeId on receive: $typeId")
        return type.read(buf)
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
    class InputState(
        val forward: Boolean = false, // 1
        val backwards: Boolean = false, // 1
        val left: Boolean = false, // 1
        val right: Boolean = false, // 1
        val fire: Boolean = false, // 1
        val jump: Boolean = false, // 1
        val walk: Boolean = false, // 1
        val cameraX: Double = 0.0, // 8
        val cameraY: Double = 0.0 // 8
    ) {
        companion object : MessageType<InputState> {
            override val bytes: Int
                get() = (1*7) + (8*2)

            override fun write(msg: InputState, buf: ByteBuf) {
                buf.writeBoolean(msg.forward)
                buf.writeBoolean(msg.backwards)
                buf.writeBoolean(msg.left)
                buf.writeBoolean(msg.right)
                buf.writeBoolean(msg.fire)
                buf.writeBoolean(msg.jump)
                buf.writeBoolean(msg.walk)
                buf.writeDouble(msg.cameraX)
                buf.writeDouble(msg.cameraY)
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
                    buf.readDouble(),
                    buf.readDouble()
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

    /** Add some box to the world. */
    class BoxAdded(
        val id: Int, // 4
        val x: Double, val y: Double, val z: Double, // 8*3
        val sx: Double, val sy: Double, val sz: Double, // 8*3
        val vx: Double, val vy: Double, val vz: Double, // 8*3
        val affectedByPhysics: Boolean, // 1
        val textureId: Int, // 4
        val textureMultiplier: Double, // 8
        val bounceMultiplier: Double // 8
    ) {
        companion object : MessageType<BoxAdded> {
            override val bytes: Int
                get() = 4+(8*3)+(8*3)+(8*3)+1+4+8+8

            override fun write(msg: BoxAdded, buf: ByteBuf) {
                buf.writeInt(msg.id)
                buf.writeDouble(msg.x)
                buf.writeDouble(msg.y)
                buf.writeDouble(msg.z)
                buf.writeDouble(msg.sx)
                buf.writeDouble(msg.sy)
                buf.writeDouble(msg.sz)
                buf.writeDouble(msg.vx)
                buf.writeDouble(msg.vy)
                buf.writeDouble(msg.vz)
                buf.writeBoolean(msg.affectedByPhysics)
                buf.writeInt(msg.textureId)
                buf.writeDouble(msg.textureMultiplier)
                buf.writeDouble(msg.bounceMultiplier)
            }

            override fun read(buf: ByteBuf): BoxAdded {
                return BoxAdded(
                    buf.readInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readBoolean(),
                    buf.readInt(),
                    buf.readDouble(),
                    buf.readDouble()
                )
            }
        }
    }

    /** Update box movement */
    class BoxUpdateMotion(
        val id: Int, // 4
        val x: Double, val y: Double, val z: Double, // 8*3
        val vx: Double, val vy: Double, val vz: Double // 8*3
    ) {
        companion object : MessageType<BoxUpdateMotion> {
            override val bytes: Int
                get() = 4+(8*3)+(8*3)

            override fun write(msg: BoxUpdateMotion, buf: ByteBuf) {
                buf.writeInt(msg.id)
                buf.writeDouble(msg.x)
                buf.writeDouble(msg.y)
                buf.writeDouble(msg.z)
                buf.writeDouble(msg.vx)
                buf.writeDouble(msg.vy)
                buf.writeDouble(msg.vz)
            }

            override fun read(buf: ByteBuf): BoxUpdateMotion {
                return BoxUpdateMotion(
                    buf.readInt(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble()
                )
            }
        }
    }
}