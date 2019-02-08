package io.snower.game.common

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

/** Client/server abstract network implementations for the game. To be used with [Messages]. */
class Network {
    companion object {
        /** Create a connected client to [host]:[port]. */
        fun createClientAndConnect(host: String, port: Int, onConnect: (Throwable?) -> Unit): Client {
            val client = Client(host, port)
            client.connectAsync(onConnect)
            return client
        }

        /** Create a server listening to [bindIp]:[bindPort] */
        fun createServer(bindIp: String, bindPort: Int): Server {
            val server = Server(bindIp, bindPort)
            server.bind()
            return server
        }
    }

    /** Network client implementation. */
    class Client(val host: String, val port: Int) {
        private var ppsCounter = 0
        var pps: Int = 0
            private set

        private var bytesPerSecInCounter = 0
        var bytesPerSecIn = 0
            private set

        private var bytesPerSecOutCounter = 0
        var bytesPerSecOut = 0
            private set

        private var sentPingTimestamp = 0L // to grab lantecy
        private var lastPingSentSec = 0L // to send a ping request each 1 sec
        var latency: Int = 0
            private set

        private var lastStatsSet = 0L
        /** Copy to per-second stats variables and reset counter if a second passed. */
        private fun checkStats() {
            if (System.currentTimeMillis() - lastStatsSet >= 1000) {
                lastStatsSet = System.currentTimeMillis()
                pps = ppsCounter
                ppsCounter = 0
                bytesPerSecIn = bytesPerSecInCounter
                bytesPerSecInCounter = 0
                bytesPerSecOut = bytesPerSecOutCounter
                bytesPerSecOutCounter = 0
            }
        }

        /** This handler decodes MessageWrapper instances into the message objects, and push them to the queue. */
        private inner class ClientNetworkHandler : ChannelInboundHandlerAdapter() {
            override fun channelRead(ctx: ChannelHandlerContext, msgRaw: Any) {
                val nettyMsg = msgRaw as Messages.MessageWrapper

                try {
                    val msg = Messages.receive(nettyMsg)
                    if (msg !is Messages.Ping) { // regular msg, add to queue
                        messagesQueue.add(msg)
                    } else {
                        // grab latency and allow to send a new ping
                        latency = (System.currentTimeMillis() - sentPingTimestamp).toInt()
                        sentPingTimestamp = 0L
                    }

                    // each 1 sec send ping if not waiting for a response
                    if (System.currentTimeMillis() / 1000 != lastPingSentSec && sentPingTimestamp == 0L) {
                        lastPingSentSec = System.currentTimeMillis() / 1000
                        sentPingTimestamp = System.currentTimeMillis()
                        Messages.send(ctx.channel(), Messages.Ping())
                    }

                    ppsCounter++
                    bytesPerSecInCounter += nettyMsg.buf.capacity()
                    checkStats()
                } finally {
                    nettyMsg.buf.release()
                }
            }

            override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                cause.printStackTrace()
                ctx.close()
            }
        }

        @Volatile
        var connected = false
            private set

        private lateinit var channel: Channel
        private var serverMessageCallback: (message: Any) -> Unit = {}
        private var messagesQueue = ConcurrentLinkedQueue<Any>() // to push to main thread messages from the server.

        /** Try to connect, calls onConnect with the exception, which is null if succeed. */
        fun connectAsync(onConnect: (Throwable?) -> Unit) {
            check(!connected) { "must not be connected." }

            thread {
                val workerGroup = NioEventLoopGroup()
                try {
                    val b = Bootstrap()
                    b.group(workerGroup)
                    b.channel(NioSocketChannel::class.java)
                    b.option(ChannelOption.SO_KEEPALIVE, true)
                    b.handler(object : ChannelInitializer<SocketChannel>() {
                        @Throws(Exception::class)
                        public override fun initChannel(ch: SocketChannel) {
                            ch.pipeline().addLast(Messages.MessagesDecoder(), ClientNetworkHandler())
                        }
                    })

                    val f = b.connect(host, port).sync() // (5)
                    connected = true
                    channel = f.channel()
                    onConnect(null)
                    f.channel().closeFuture().sync()
                } catch (e: Exception) {
                    connected = false
                    onConnect(e)
                } finally {
                    workerGroup.shutdownGracefully()
                }
            }
        }

        /** Try to connect to the server, may throw an error. Blocking. */
        fun connect() {
            var connectionException: Throwable? = null
            var connectionDone = false
            connectAsync { error ->
                if (error != null) {
                    connectionException = error
                }
                connectionDone = true
            }

            // wait until thread fails or connects.
            while (!connectionDone) Thread.sleep(50)
            if (connectionException != null) {
                throw connectionException!!
            }
        }

        /** To be called when a server message comes. */
        fun onServerMessage(callback: (message: Any) -> Unit) {
            serverMessageCallback = callback
        }

        /** Send [message] to the server. */
        fun send(message: Any) {
            val bytes = Messages.send(channel, message)
            bytesPerSecOutCounter += bytes
            checkStats()
        }

        /** Poll all messages, invoke callbacks. */
        fun pollMessages() {
            while (messagesQueue.isNotEmpty()) {
                val msg = messagesQueue.poll()
                serverMessageCallback(msg)
            }
        }

        /** Close the connection. */
        fun close() {
            check(connected) { "must be connected." }
            channel.close()
        }
    }

    enum class EventType { CONNECT, DISCONNECT, MESSAGE }
    class EnqueuedEvent(val type: EventType, val connection: PlayerConnection, val message: Any? = null)

    /** This handler pushes events to [queue], decoded from the given message wrappers. */
    class PlayerConnection(val channel: Channel, val queue: Queue<EnqueuedEvent>)  : ChannelInboundHandlerAdapter() {
        override fun channelActive(ctx: ChannelHandlerContext) {
            queue.offer(EnqueuedEvent(EventType.CONNECT, this))
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            queue.offer(EnqueuedEvent(EventType.DISCONNECT, this))
        }

        override fun channelRead(ctx: ChannelHandlerContext, rawMsg: Any) {
            val wrapper = rawMsg as Messages.MessageWrapper
            try {
                val msg = Messages.receive(wrapper)
                if (msg !is Messages.Ping) {
                    queue.offer(EnqueuedEvent(EventType.MESSAGE, this, msg))
                } else {
                    // Send back a "Ping" so the client grab the RTT
                    Messages.send(ctx.channel(), Messages.Ping())
                }
            } finally {
                wrapper.buf.release()
            }
        }

        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
            cause.printStackTrace()
            ctx.close()
        }
    }

    /** Network server implementation */
    class Server(val bindIp: String, val bindPort: Int) {
        private lateinit var channel: Channel
        private var connected = false
        private val connections = mutableListOf<PlayerConnection>()
        private var connectCallback: (PlayerConnection) -> Unit = {}
        private var disconnectCallback: (PlayerConnection) -> Unit = {}
        private var messageCallback: (PlayerConnection, Any) -> Unit = { _, _ -> }

        private val eventsQueue = ConcurrentLinkedQueue<EnqueuedEvent>()

        /** Try to bind to the server ip:port, may throw errors. */
        fun bind() {
            var connectionDone = false
            var connectionException: Throwable? = null
            var channel: Channel? = null

            thread {
                val bossGroup = NioEventLoopGroup()
                val workerGroup = NioEventLoopGroup()
                try {
                    val b = ServerBootstrap()
                    b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel::class.java)
                        .childHandler(object : ChannelInitializer<SocketChannel>() {
                            @Throws(Exception::class)
                            public override fun initChannel(ch: SocketChannel) {
                                val connection = PlayerConnection(ch, eventsQueue)
                                ch.pipeline().addLast(Messages.MessagesDecoder(), connection)
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)

                    val channelFuture = b.bind(bindIp, bindPort)
                    channel = channelFuture.channel()
                    connectionDone = true
                    channelFuture.sync()
                    channel!!.closeFuture().sync() // shut down gracefully
                } catch (e: Exception) {
                    connectionException = e
                } finally {
                    connectionDone = true
                    workerGroup.shutdownGracefully()
                    bossGroup.shutdownGracefully()
                }
            }

            // wait until thread fails or connects.
            while (!connectionDone) Thread.sleep(25)
            if (connectionException != null) {
                throw connectionException!!
            } else {
                this.channel = channel!!
                this.connected = true
            }
        }

        /** Send [message] to [connection] */
        fun send(message: Any, connection: PlayerConnection) {
            Messages.send(connection.channel, message)
        }

        /** Send [message] to all connections */
        fun broadcast(message: Any) {
            for (connection in connections) Messages.send(connection.channel, message)
        }

        /** To be called when a new player connects */
        fun onConnect(callback: (PlayerConnection) -> Unit) {
            connectCallback = callback
        }

        /** To be called when a connected player disconnects. */
        fun onDisconnect(callback: (PlayerConnection) -> Unit) {
            disconnectCallback = callback
        }

        /** To be called when a message by any connection is received. */
        fun onClientMessage(callback: (client: PlayerConnection, message: Any) -> Unit) {
            messageCallback = callback
        }

        /** Close the given [connection]. */
        fun kick(connection: PlayerConnection) {
            connection.channel.close()
        }

        /** Fetch all enqueued messages and call callbacks if necessary. Should be called on main loop. */
        fun pollMessages() {
            while (eventsQueue.isNotEmpty()) {
                val event = eventsQueue.poll()
                when (event.type) {
                    EventType.CONNECT -> {
                        connections.add(event.connection)
                        connectCallback(event.connection)
                    }
                    EventType.DISCONNECT -> {
                        disconnectCallback(event.connection)
                        connections.remove(event.connection)
                    }
                    EventType.MESSAGE -> {
                        messageCallback(event.connection, event.message!!)
                    }
                }
            }
        }
    }
}