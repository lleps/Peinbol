package io.snower.game.client

/** Handles UI rendering */
interface UIRenderer {
    fun <T : UIDrawable> registerUIElement(clazz: Class<T>, drawable: T)
    fun <T : UIDrawable> getUIElement(clazz: Class<T>): T?
    fun <T : UIDrawable> unregisterUIElement(clazz: Class<T>)

    fun setResolution(width: Int, height: Int)

    fun preloadAssets()
    fun init()
    fun draw()
    fun destroy()
}