package io.snower.game.client

import io.snower.game.common.BufferUtils
import java.nio.Buffer
import java.nio.ByteBuffer

/** Implements ui rendering through nuklear, using JNI */
class NuklearUIRenderer(private val assetResolver: AssetResolver) : UIRenderer, UIDrawer {

    private val uiDrawables = hashMapOf<Class<out UIDrawable>, UIDrawable>()

    override fun <T : UIDrawable> registerUIElement(clazz: Class<T>, drawable: T) {
        check(clazz !in uiDrawables) { "class $clazz already has a drawable registered. Remove it first." }
        uiDrawables[clazz] = drawable
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : UIDrawable> getUIElement(clazz: Class<T>): T? {
        return uiDrawables[clazz] as T?
    }

    override fun <T : UIDrawable> unregisterUIElement(clazz: Class<T>) {
        check(clazz in uiDrawables) { "class $clazz doesn't have a drawable registered." }
        uiDrawables -= clazz
    }

    private var width: Int = 800
    private var height: Int = 600
    private var nkContext = 0L
    private var assetsLoaded = false
    private var fontBuffer: ByteBuffer? = null

    override fun setResolution(width: Int, height: Int) {
        this.width = width
        this.height = height
    }


    override fun preloadAssets() {
        if (assetsLoaded) return
        // dumb copy here...
        val fontData = assetResolver.getAsByteArray("demo/FiraSans.ttf")
        val buffer = BufferUtils.createByteBuffer(fontData.size)
        buffer.put(fontData)
        buffer.position(0)
        fontBuffer = buffer
        assetsLoaded = true
    }

    override fun init() {
        nkContext = createNuklearContext(fontBuffer!!)
    }

    override fun draw() {
        setCurrentNuklearContext(nkContext)
        for (drawable in uiDrawables.values) {
            drawable.draw(this, width.toFloat(), height.toFloat())
        }
        drawNuklearOutput(width, height)
    }

    override fun destroy() {
        // should remove nuklear context I guess.
    }

    // Native functions

    // internally used (create and set the context for the functions below)
    private external fun createNuklearContext(fontPointer: Buffer): Long
    private external fun setCurrentNuklearContext(context: Long) // sets the global state to use the given context
    private external fun drawNuklearOutput(width: Int, height: Int)

    // Exposed in the interface
    // why direct access instead of wrapped?
    // essentially, less work at the cost of less flexibility
    external override fun begin(title: String, x: Float, y: Float, width: Float, height: Float, background: Int, flags: Int): Boolean
    external override fun end()
    external override fun layoutRowDynamic(height: Float, columns: Int)
    external override fun layoutRowStatic(height: Float, width: Float, columns: Int)
    external override fun label(text: String, align: Int)
    external override fun strokeCircle(x: Float, y: Float, diameter: Float, thickness: Float, color: Int)
    external override fun fillCircle(x: Float, y: Float, diameter: Float, color: Int)
    external override fun progress(current: Int, max: Int, color: Int, background: Int)

    // Constants
    // Hardcoded to avoid generating a damn method for each one
    private fun nkFlag(flag: Int) = 1 shl flag
    override val WINDOW_TITLE: Int get() = nkFlag(6)
    override val WINDOW_NO_SCROLLBAR: Int get() = nkFlag(5)
    override val TEXT_LEFT: Int get() = 0x01
    override val TEXT_CENTER: Int get() = 0x02
    override val TEXT_RIGHT: Int get() = 0x04
}