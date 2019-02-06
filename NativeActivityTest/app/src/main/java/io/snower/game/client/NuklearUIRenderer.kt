package io.snower.game.client

/** Implements ui rendering through nuklear, using JNI */
class NuklearUIRenderer : UIRenderer {
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

    override fun setResolution(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    // Now implement this using JNI.
    // first, completely ignore input. Just nuklear output.
    // Must have methods to, draw everything in UIDrawer (this may implement the interface natively)
    // And, methods to draw to openGL nuklear output


    override fun init() {
    }

    override fun draw() {
    }

    override fun destroy() {
    }

}