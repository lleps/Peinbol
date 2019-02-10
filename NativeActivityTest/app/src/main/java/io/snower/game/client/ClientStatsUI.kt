package io.snower.game.client

import io.snower.game.common.Network
import io.snower.game.common.PhysicsInterface

/** To shot latency, fps, etc. For devs. */
class ClientStatsUI(
    private val window: Window,
    private val physics: PhysicsInterface,
    private val network: Network.Client
) : UIDrawable {

    companion object {
        private const val BLACK_TRANSPARENT = 0x00000064
    }

    var visible = false

    override fun draw(drawer: UIDrawer, screenWidth: Float, screenHeight: Float) {
        if (!visible) return
        val statCount = 3 // cpu+net fps 60  phys xms   draw xms |  ping 50ms  out 25kb/s  in 25/s   mem used/alloc/Max
        if (drawer.begin("Stats", 20f, 20f, 300f, statCount*25f,
                background = BLACK_TRANSPARENT,
                flags = drawer.WINDOW_NO_SCROLLBAR)) {
            drawer.layoutRowDynamic(20f, 1)
            drawer.label(
                "fps %d  physics %.1fms  draw %.1fms".format(window.fps, physics.lastSimulationMillis, window.lastDrawMillis),
                drawer.TEXT_LEFT
            )
            drawer.layoutRowDynamic(20f, 1)
            drawer.label(
                "lat %dms  in %.1fKB/s  out %.1fKB/s  pps %d"
                    .format(
                        network.latency,
                        network.bytesPerSecIn / 1024f,
                        network.bytesPerSecOut / 1024f,
                        network.pps
                    ),
                drawer.TEXT_LEFT
            )
            val runtime = Runtime.getRuntime()
            val maxMemoryMb = runtime.maxMemory() / 1024.0 / 1024.0
            val allocatedMemoryMb = runtime.totalMemory() / 1024.0 / 1024.0
            val freeMemoryMb = runtime.freeMemory() / 1024.0 / 1024.0
            val usedMemoryMb = allocatedMemoryMb - freeMemoryMb
            drawer.layoutRowDynamic(20f, 1)
            drawer.label(
                "mem %.1fM  alloc %.1fM  max %.1fM".format(usedMemoryMb, allocatedMemoryMb, maxMemoryMb),
                drawer.TEXT_LEFT
            )
        }
        drawer.end()
    }
}