package com.example.nativeactivitytest

import android.opengl.GLSurfaceView
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity() {

    private var renderer: GLSurfaceView.Renderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val surface = GLSurfaceView(this)
        surface.setEGLContextClientVersion(2)
        renderer = object : GLSurfaceView.Renderer {
            override fun onDrawFrame(gl: GL10?) {
                step()
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                init(width, height)
            }

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            }
        }

        surface.setRenderer(renderer!!)
        setContentView(surface)
    }

    external fun init(width: Int, height: Int)
    external fun step()

    companion object {

        // Used to load the 'native-lib' library on application startup.
        init {
            System.loadLibrary("native-lib")
        }
    }
}
