package com.peinbol.client

import com.peinbol.Network
import com.peinbol.Physics
import org.lwjgl.nuklear.NkColor
import org.lwjgl.nuklear.NkContext
import org.lwjgl.nuklear.Nuklear
import org.lwjgl.nuklear.Nuklear.*

/** To shot latency, fps, etc. For devs. */
class ClientStatsUI(
    private val window: Window,
    private val physics: Physics,
    private val network: Network.Client
) : NkUIDrawable {
    companion object {
        private val BLACK_TRANSPARENT = Nuklear.nk_rgba(0, 0, 0, 100, NkColor.callocStack())
        private const val WIDTH_PER_CARACTER = 7.5f
        private const val MSG_EXPIRY_MILLIS = 10 * 1000
    }

    override fun draw(ctx: NkContext, screenWidth: Float, screenHeight: Float) {
        val statCount = 2 // cpu+net fps 60  phys xms   draw xms |  ping 50ms  out 25kb/s  in 25/s
        nkBeginDefaultWindow(
            ctx,
            "Stats",
            20f, 20f, 300f, statCount*25f
        ) {
            nk_layout_row_dynamic(ctx, 20f, 1)
            nk_label(ctx,
                "fps %d  physics %.1fs  draw %.1fms".format(window.fps, physics.lastSimulationMillis, window.lastDrawMillis),
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
        }
    }
}