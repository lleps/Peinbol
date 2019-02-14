package io.snower.game.client

import io.snower.game.common.*
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.*
import javax.vecmath.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import android.opengl.GLES20



/**
 * Renders the world and preloadAssets the assets that are necessary for
 * that, such as textures and shaders.
 */
class WorldRenderer(
    private val assetResolver: AssetResolver,
    private val gl: GLInterface,
    private val physicsInterface: PhysicsInterface,
    private val matrixOps: MatrixOps
) {

    /** Implements rendering behavior for the given [box] */
    private inner class BoxRenderer(private val box: Box) {

        val modelMatrix = FloatArray(16)
        val mvMatrix = FloatArray(16)
        val mvpMatrix = FloatArray(16)

        var bufVertex = -1
        var bufNormals = -1
        var bufTextCoords = -1
        var bufColors = -1

        var inited = false

        // Init stuff in GL, like buffers and textures
        fun init(gl: GLInterface) {
            if (inited) return
            inited = true
            // allocate necessary buffers
            val colorBuffer: FloatBuffer = BufferUtils.createFloatBuffer(6*6*4)
            val textureCoordsBuffer: FloatBuffer = BufferUtils.createFloatBuffer(6*6*2)
            val (r,g,b,a) = box.theColor
            val m = box.textureMultiplier.toFloat()
            repeat(6*6) {
                colorBuffer.put(r)
                colorBuffer.put(g)
                colorBuffer.put(b)
                colorBuffer.put(a)
            }
            repeat(6*6*2) { i ->
                textureCoordsBuffer.put(TXT_BASE_COORDS[i] * m)
            }
            val arr = IntArray(4)
            GLES20.glGenBuffers(4, arr, 0)

            // Vertex buffer
            bufVertex = arr[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, arr[0])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER, mCubePositions.capacity() * BYTES_PER_FLOAT,
                mCubePositions, GLES20.GL_STATIC_DRAW
            )
            println("create buffer: $bufVertex")

            // Normals buffer
            bufNormals = arr[3]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, arr[3])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER, mCubeNormals.capacity() * BYTES_PER_FLOAT,
                mCubeNormals, GLES20.GL_STATIC_DRAW
            )
            println("create buffer: $bufNormals")


            // Textures buffer
            bufTextCoords = arr[2]
            textureCoordsBuffer.position(0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, arr[2])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER, textureCoordsBuffer.capacity() * BYTES_PER_FLOAT,
                textureCoordsBuffer, GLES20.GL_STATIC_DRAW
            )
            println("create buffer: $bufTextCoords")


            // Colors buffer
            bufColors = arr[1]
            colorBuffer.position(0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, arr[1])
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER, colorBuffer.capacity() * BYTES_PER_FLOAT,
                colorBuffer, GLES20.GL_STATIC_DRAW
            )
            println("create buffer: $bufColors")

        }

        // May be called from any thread. Should do as much thread-safe setup as possible.
        fun preDraw() {
            physicsInterface.getBoxOpenGLMatrix(box, modelMatrix)
            matrixOps.scale(modelMatrix, box.size.x / 2f, box.size.y / 2f, box.size.z / 2f)
            matrixOps.multiplyMM(mvMatrix, viewMatrix, modelMatrix)
            matrixOps.multiplyMM(mvpMatrix, projectionMatrix, mvMatrix)
        }

        // Draw in the current gl
        fun draw(gl: GLInterface) {
            // 11 gl calls per cube. should be reduced. maybe to 5. If just objects shared text coords.
            // can be like
            // (once) enableAttribs, bindVBO
            // (for each) setTransform, bindTexture, passTransform, drawArray
            // TODO: make only 1 stride buffer.
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufVertex)
            GLES20.glVertexAttribPointer(positionHandle, POSITION_DATA_SIZE, gl.GL_FLOAT, false, 0, 0)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufNormals)
            GLES20.glVertexAttribPointer(normalHandle, NORMAL_DATA_SIZE, gl.GL_FLOAT, false, 0, 0)

            // isn't this line redundant? and may be ignored? its the same data for all cubes...
            // well. gl calls reduced? no. even increased. but time for each call reduced a bit.
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufColors)
            GLES20.glVertexAttribPointer(colorHandle, COLOR_DATA_SIZE, gl.GL_FLOAT, false, 0, 0)

            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufTextCoords)
            GLES20.glVertexAttribPointer(textureCoordinateHandle, TEXTURE_COORDS_DATA_SIZE, gl.GL_FLOAT, false, 0, 0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

            gl.glUniformMatrix4fv(mvMatrixHandle, false, mvMatrix)
            gl.glUniformMatrix4fv(mvpMatrixHandle, false, mvpMatrix)
            gl.glDrawArrays(gl.GL_TRIANGLES, 0, 36)
        }

        // TODO: use the gl object instead of GLES20
        // Should clear GL stuff
        fun destroy(gl: GLInterface) {
            GLES20.glDeleteBuffers(
                4,
                intArrayOf(bufVertex, bufNormals, bufColors, bufTextCoords),
                0)
        }
    }

    // used matrices
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val lightModelMatrix = FloatArray(16)
    private val lightPosInModelSpace = floatArrayOf(0f, 0f, 0f, 1f)
    private val lightPosInWorldSpace = FloatArray(4)
    private val lightPosInEyeSpace = FloatArray(4)

    // Reference buffers
    private val mCubePositions: FloatBuffer
    private val mCubeNormals: FloatBuffer
    private val mCubeTextureCoordinates: FloatBuffer
    private val mCubeTextureCoordinatesRef: FloatBuffer
    init {
        mCubePositions = ByteBuffer.allocateDirect(CUBE_VERTEX_DATA.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubePositions.put(CUBE_VERTEX_DATA).position(0)

        mCubeNormals = ByteBuffer.allocateDirect(CUBE_NORMAL_DATA.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubeNormals.put(CUBE_NORMAL_DATA).position(0)
        mCubeTextureCoordinates = ByteBuffer.allocateDirect(CUBE_TXT_DATA.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubeTextureCoordinates.put(CUBE_TXT_DATA).position(0)
        mCubeTextureCoordinatesRef = ByteBuffer.allocateDirect(CUBE_TXT_DATA.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubeTextureCoordinatesRef.put(CUBE_TXT_DATA).position(0)
    }

    // other
    private var width: Int = 0
    private var height: Int = 0
    var fov: Float = 45f
        set(value) {
            field = value.coerceIn(5f, 175f)
        }

    // handles to GL stuff
    private var program: Int = 0
    private var mvpMatrixHandle = 0
    private var mvMatrixHandle = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var normalHandle = 0
    private var lightPosHandle = 0
    private var textureUniformHandle = 0
    private var textureCoordinateHandle = 0

    private val boxes = mutableSetOf<Box>()

    // Assets. Kept in memory for performance.
    private val textures = ConcurrentHashMap<Int, GLTextureWrapper>()
    private var vertexShaderSource = ""
    private var fragmentShaderSource = ""

    /** Adds [box] to the drawing list.*/
    fun addBox(box: Box) {
        if (box !in boxes) {
            boxes += box
            val renderer = BoxRenderer(box)
            box.rendererHandle = renderer
        }
    }

    /** Removes [box] from the drawing list. */
    fun removeBox(box: Box) {
        if (box in boxes) {
            boxes -= box
            val renderer = box.rendererHandle as BoxRenderer
            renderer.destroy(gl)
            box.rendererHandle = null
        }
    }

    /** Sets where should sit the camera. */
    var cameraPosX = 0f
    var cameraPosY = 0f
    var cameraPosZ = 0f

    /** Sets where the camera should be looking. */
    // TODO: when a class is available, camera can be converted to vectors
    var cameraRotX = 0f
        set(value) {
            field = value.coerceIn(-85f, 85f) % 360f
        }
    var cameraRotY = 0f
        set(value) {
            field = value % 360f
        }
    var cameraRotZ = 0f
        set(value) {
            field = value % 360f
        }

    private val textureLoaderExecutor = Executors.newCachedThreadPool()
    private var assetsLoaded: Boolean = false

    /** Preload renderer assets */
    fun preloadAssets() {
        if (!assetsLoaded) {
            assetsLoaded = true

            vertexShaderSource = assetResolver.getAsString("vertexShader.glsl")
            fragmentShaderSource = assetResolver.getAsString("fragmentShader.glsl")

            // load textures lazily
            for ((txtId, file) in Textures.FILES) {
                textureLoaderExecutor.submit {
                    val data = assetResolver.getAsByteArray(file)
                    val wrapper = ByteArrayInputStream(data).use { stream ->
                        GLTextureWrapper.createFromPNGInputStream(stream)
                    }
                    println("Load texture ID $txtId from $file")
                    textures[txtId] = wrapper
                }
            }
        }
    }

    private var glInited = false

    fun init() {
        if (!glInited) {
            val vertexShader = loadShaderFromSource(vertexShaderSource, gl.GL_VERTEX_SHADER)
            val fragmentShader = loadShaderFromSource(fragmentShaderSource, gl.GL_FRAGMENT_SHADER)
            program = createAndLinkProgram(vertexShader, fragmentShader, arrayOf("a_Position", "a_Color", "a_Normal", "a_TexCoordinate"))
            glInited = true
        }
    }

    fun setResolution(width: Int, height: Int) {
        this.width = width
        this.height = height
        gl.glViewport(0, 0, width, height)
    }

    /** Optimization. Used to check if objects are inside the projected frustum, to avoid drawing when not necessary. */
    private class FrustumTester {
        enum class TestResult { OUTSIDE, INTERSECTS, INSIDE }
        enum class CameraPlane(val idx: Int) { TOP(0), BOTTOM(1), LEFT(2), RIGHT(3), NEARP(4), FARP(5) }

        private var fov: Float = 0f
        private var ratio: Float = 0f
        private var zNear: Float = 0f
        private var zFar: Float = 0f

        private var tang: Float = 0f
        private var fw: Float = 0f
        private var fh: Float = 0f
        private var nw: Float = 0f
        private var nh: Float = 0f

        fun setPerspective(fov: Float, ratio: Float, zNear: Float, zFar: Float) {
            this.fov = fov
            this.ratio = ratio
            this.zNear = zNear
            this.zFar = zFar

            tang = tan(radians(fov * 0.5f))
            nh = zNear * tang
            nw = nh * ratio
            fh = zFar * tang
            fw = fh * ratio
        }

        fun setLookAt(p: Vector3f, l: Vector3f, u: Vector3f) {
        }
    }

    // Parallel pre-rendering state
    private val rendererExecutor = Executors.newFixedThreadPool(4)
    private val rendererExecutorList = ArrayList<Callable<Unit>>(4)
    private val threadBoxes = Array<ArrayList<BoxRenderer>>(4) { ArrayList(500) }

    private var x = false

    fun draw() {
        // program
        gl.glUseProgram(program)
        mvpMatrixHandle = gl.glGetUniformLocation(program, "u_MVPMatrix")
        mvMatrixHandle = gl.glGetUniformLocation(program, "u_MVMatrix")
        lightPosHandle = gl.glGetUniformLocation(program, "u_LightPos")
        positionHandle = gl.glGetAttribLocation(program, "a_Position")
        colorHandle = gl.glGetAttribLocation(program, "a_Color")
        normalHandle = gl.glGetAttribLocation(program, "a_Normal")
        textureUniformHandle = gl.glGetUniformLocation(program, "u_Texture")
        textureCoordinateHandle = gl.glGetAttribLocation(program, "a_TexCoordinate")

        // setup 3d used state
        gl.glClearColor(6f/255f,2f/255f,20f/255f, 1f) //  sky
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT or gl.GL_COLOR_BUFFER_BIT)
        gl.glEnable(gl.GL_CULL_FACE)
        gl.glEnable(gl.GL_DEPTH_TEST)
        gl.glActiveTexture(gl.GL_TEXTURE0)

        // perspective and view
        matrixOps.identity(projectionMatrix)
        matrixOps.perspective(projectionMatrix, fov, width.toFloat()/height.toFloat(), 0.2f, 1000f)
        matrixOps.identity(viewMatrix)
        matrixOps.lookAt(viewMatrix,
            cameraPosX,
            cameraPosY,
            cameraPosZ,
            cameraPosX + cos(radians(-cameraRotY - 90f)),
            cameraPosY - sin(radians(-cameraRotX)),
            cameraPosZ + sin(radians(-cameraRotY - 90f)),
            0f,
            1f,
            0f)

        // Light
        val time = System.currentTimeMillis() % 10000L
        val angleInDegrees = (360.0f / 10000.0f) * time.toInt()
        matrixOps.identity(lightModelMatrix)
        matrixOps.translate(lightModelMatrix, 0f, 20f, 0f)
        matrixOps.rotate(lightModelMatrix, angleInDegrees, 0f, 1f, 0f)
        matrixOps.translate(lightModelMatrix, 0f, 0f, 50f)
        matrixOps.multiplyMV(lightPosInWorldSpace, lightModelMatrix, lightPosInModelSpace)
        matrixOps.multiplyMV(lightPosInEyeSpace, viewMatrix, lightPosInWorldSpace)
        gl.glUniform3f(lightPosHandle, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2])

        // Enable vertex attributes
        gl.glEnableVertexAttribArray(positionHandle)
        gl.glEnableVertexAttribArray(colorHandle)
        gl.glEnableVertexAttribArray(normalHandle)
        gl.glEnableVertexAttribArray(textureCoordinateHandle)

        // Do parallel pre-render operations
        rendererExecutorList.clear()
        val boxesCount = boxes.size
        val boxesPerThread = boxesCount / 4
        val iterator = boxes.iterator()
        repeat(4) { threadId ->
            val boxesForThatThread = threadBoxes[threadId]
            boxesForThatThread.clear()
            var threadBoxCounter = 0
            while (iterator.hasNext() && threadBoxCounter++ < boxesPerThread) {
                boxesForThatThread.add(iterator.next().rendererHandle as BoxRenderer)
            }

            rendererExecutorList.add(Callable {
                for (boxRenderer in boxesForThatThread) {
                    boxRenderer.preDraw()
                }
            })
        }
        rendererExecutor.invokeAll(rendererExecutorList)

        // Draw
        for (box in boxes) {
            val renderer = box.rendererHandle as BoxRenderer
            renderer.init(gl)
            val txt = textures[box.textureId]
            if (txt == null) {
                // not loaded or invalid
                gl.glBindTexture(gl.GL_TEXTURE_2D, 0)
            } else {
                if (!txt.loadedInGL) txt.load(gl)
                txt.bind(gl)
            }
            // textures should be set in "draw" as well.
            // the problem with this is that draw can't be made at least somewhat globally.
            // some things may be done only once for every box.
            gl.glUniform1i(textureUniformHandle, 0)
            renderer.draw(gl)
            //drawCube(box)
        }
    }

    private fun loadShaderFromSource(source: String, type: Int, file: String? = null): Int {
        val shaderHandle = gl.glCreateShader(type)
        if (shaderHandle != 0) {
            gl.glShaderSource(shaderHandle, source)
            gl.glCompileShader(shaderHandle)
            val compileStatus = IntArray(1)
            gl.glGetShaderiv(shaderHandle, gl.GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] == 0) {
                val errorMsg = gl.glGetShaderInfoLog(shaderHandle)
                gl.glDeleteShader(shaderHandle)
                error("Error compiling shader (file: $file, type: $type): $errorMsg")
            }
        }
        if (shaderHandle == 0) error("Error creating shader.")
        return shaderHandle
    }

    private fun createAndLinkProgram(
        vertexShaderHandle: Int,
        fragmentShaderHandle: Int,
        attributes: Array<String>?
    ): Int {
        var programHandle = gl.glCreateProgram()
        if (programHandle != 0) {
            gl.glAttachShader(programHandle, vertexShaderHandle)
            gl.glAttachShader(programHandle, fragmentShaderHandle)
            if (attributes != null) {
                val size = attributes.size
                for (i in 0 until size) {
                    gl.glBindAttribLocation(programHandle, i, attributes[i])
                }
            }

            gl.glLinkProgram(programHandle)
            val linkStatus = IntArray(1)
            gl.glGetProgramiv(programHandle, gl.GL_LINK_STATUS, linkStatus)

            if (linkStatus[0] == 0) {
                gl.glDeleteProgram(programHandle)
                programHandle = 0
                error("Error compiling program: " + gl.glGetProgramInfoLog(programHandle))
            }
        }

        if (programHandle == 0) error("Error creating program.")
        return programHandle
    }

    companion object {
        private const val BYTES_PER_FLOAT = 4
        private const val POSITION_DATA_SIZE = 3
        private const val COLOR_DATA_SIZE = 4
        private const val NORMAL_DATA_SIZE = 3
        private const val TEXTURE_COORDS_DATA_SIZE = 2

        private val CUBE_VERTEX_DATA = floatArrayOf(
            // Front face
            -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f,
            // Right face
            1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f,
            // Back face
            1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f,
            // Left face
            -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f,
            // Top face
            -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f,
            // Bottom face
            1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f)

        private val CUBE_NORMAL_DATA = floatArrayOf(
            // Front face
            0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            // Right face
            1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f,
            // Back face
            0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f,
            // Left face
            -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f,
            // Top face
            0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f,
            // Bottom face
            0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f, 0.0f, -1.0f, 0.0f)

        private val CUBE_TXT_DATA = floatArrayOf(
            // Front face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
            // Right face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
            // Back face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
            // Left face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
            // Top face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f,
            // Bottom face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f)

        private val TXT_BASE_COORDS = floatArrayOf(
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, // Front face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, // Right face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, // Back face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, // Left face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, // Top face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f // Bottom face
        )
    }
}