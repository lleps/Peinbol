package com.peinbol.client

import com.peinbol.Network
import com.peinbol.Physics
import org.lwjgl.nuklear.NkColor
import org.lwjgl.nuklear.NkContext
import org.lwjgl.nuklear.Nuklear
import org.lwjgl.nuklear.Nuklear.*
import java.lang.management.ManagementFactory

/** To shot latency, fps, etc. For devs. */
class ClientStatsUI(
    private val window: Window,
    private val physics: Physics,
    private val network: Network.Client
) : NkUIDrawable {

    override fun draw(ctx: NkContext, screenWidth: Float, screenHeight: Float) {
        val statCount = 3 // cpu+net fps 60  phys xms   draw xms |  ping 50ms  out 25kb/s  in 25/s   mem used/alloc/Max
        nkBeginDefaultWindow(
            ctx,
            "Stats",
            20f, 20f, 300f, statCount*25f
        ) {
            nk_layout_row_dynamic(ctx, 20f, 1)
            nk_label(ctx,
                "fps %d  physics %.1fms  draw %.1fms".format(window.fps, physics.lastSimulationMillis, window.lastDrawMillis),
                NK_TEXT_LEFT
            )
            nk_layout_row_dynamic(ctx, 20f, 1)
            nk_label(ctx,
                "lat %dms  in %.1fKB/s  out %.1fKB/s  pps %d"
                    .format(
                        network.latency,
                        network.bytesPerSecIn / 1024f,
                        network.bytesPerSecOut / 1024f,
                        network.pps
                    ),
                NK_TEXT_LEFT
            )
            val runtime = Runtime.getRuntime()
            val maxMemoryMb = runtime.maxMemory() / 1024.0 / 1024.0
            val allocatedMemoryMb = runtime.totalMemory() / 1024.0 / 1024.0
            val freeMemoryMb = runtime.freeMemory() / 1024.0 / 1024.0
            val usedMemoryMb = allocatedMemoryMb - freeMemoryMb
            nk_layout_row_dynamic(ctx, 20f, 1)
            nk_label(ctx,
                "mem %.1fM  alloc %.1fM  max %.1fM".format(usedMemoryMb, allocatedMemoryMb, maxMemoryMb),
                NK_TEXT_LEFT
            )
        }
    }
}