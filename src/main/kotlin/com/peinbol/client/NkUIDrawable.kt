package com.peinbol.client

import org.lwjgl.nuklear.*

/** An UI element which supports custom nuklear logic to draw */
interface NkUIDrawable {
    fun draw(ctx: NkContext, screenWidth: Float, screenHeight: Float)
}

inline fun nkBeginTransparentWindow(
    ctx: NkContext,
    title: String,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    flags: Int = Nuklear.NK_WINDOW_NO_SCROLLBAR,
    block: () -> Unit
) {
    val style = ctx.style()
    Nuklear.nk_style_push_color(ctx, style.window().background(), Nuklear.nk_rgba(0, 0, 0, 0, NkColor.callocStack()))
    Nuklear.nk_style_push_style_item(
        ctx,
        style.window().fixed_background(),
        Nuklear.nk_style_item_color(Nuklear.nk_rgba(0, 0, 0, 0, NkColor.callocStack()), NkStyleItem.callocStack())
    )
    val rect = NkRect.mallocStack()

    if (Nuklear.nk_begin(
            ctx,
            title,
            Nuklear.nk_rect(x, y, w, h, rect),
            flags
        )) {
        block()
    }

    Nuklear.nk_end(ctx)
    Nuklear.nk_style_pop_style_item(ctx)
    Nuklear.nk_style_pop_color(ctx)
}

inline fun nkBeginDefaultWindow(
    ctx: NkContext,
    title: String,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    flags: Int = Nuklear.NK_WINDOW_NO_SCROLLBAR,
    block: () -> Unit
) {

    val rect = NkRect.mallocStack()

    if (Nuklear.nk_begin(
            ctx,
            title,
            Nuklear.nk_rect(x, y, w, h, rect),
            flags
        )) {
        block()
    }

    Nuklear.nk_end(ctx)
}