package io.snower.game.client

import android.opengl.Matrix
import android.util.Log
import io.snower.game.common.*
import org.joml.Planed
import org.joml.Planef
import org.joml.Vector4f
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.vecmath.Color4f
import javax.vecmath.Vector3f
import kotlin.concurrent.thread
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.system.measureTimeMillis

/**
 * Renders the world and preloadAssets the assets that are necessary for
 * that, such as textures and shaders.
 */
class WorldRenderer(
    private val assetResolver: AssetResolver,
    private val gl: GLInterface,
    private val physicsInterface: PhysicsInterface
) {

    companion object {
        private const val BYTES_PER_FLOAT = 4
        private const val POSITION_DATA_SIZE = 3
        private const val COLOR_DATA_SIZE = 4
        private const val NORMAL_DATA_SIZE = 3
        private const val TEXTURE_COORDS_DATA_SIZE = 2

        private val TXT_BASE_COORDS = floatArrayOf(
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, // Front face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, // Right face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, // Back face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, // Left face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f, // Top face
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, 0.0f // Bottom face
        )
    }

    private val mCubePositions: FloatBuffer
    private val mCubeNormals: FloatBuffer
    private val mCubeTextureCoordinates: FloatBuffer
    private val mCubeTextureCoordinatesRef: FloatBuffer

    private var width: Int = 0
    private var height: Int = 0



    /** Encapsulate data necessary to speed up rendering of boxes */
    private class BoxRenderer(private val box: Box) {
        val colorBuffer: FloatBuffer = BufferUtils.createFloatBuffer(6*6*4)
        val textureCoordsBuffer: FloatBuffer = BufferUtils.createFloatBuffer(6*6*2)

        init {
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
        }
    }

    init {
        val cubePositionData = floatArrayOf(
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

        val cubeNormalData = floatArrayOf(
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

        val cubeTextureCoordinateData = floatArrayOf(
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


        mCubePositions = ByteBuffer.allocateDirect(cubePositionData.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubePositions.put(cubePositionData).position(0)

        mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubeNormals.put(cubeNormalData).position(0)
        mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0)
        mCubeTextureCoordinatesRef = ByteBuffer.allocateDirect(cubeTextureCoordinateData.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubeTextureCoordinatesRef.put(cubeTextureCoordinateData).position(0)
    }

    /** Defines matrix-vector and matrix-matrix operations used in the renderer. */
    interface MatrixOps {
        fun multiplyMM(dst: FloatArray, lhs: FloatArray, rhs: FloatArray)
        fun multiplyMV(dstVec: FloatArray, lhsMat: FloatArray, rhsVec: FloatArray)
        fun identity(dst: FloatArray)
        fun perspective(dst: FloatArray, fovy: Float, aspect: Float, zNear: Float, zFar: Float)
        fun lookAt(dst: FloatArray,
                   eyeX: Float, eyeY: Float, eyeZ: Float,
                   centerX: Float, centerY: Float, centerZ: Float,
                   upX: Float, upY: Float, upZ: Float)
        fun translate(dst: FloatArray, x: Float, y: Float, z: Float)
        fun rotate(dst: FloatArray, angleDegrees: Float, x: Float, y: Float, z: Float)
        fun scale(dst: FloatArray, x: Float, y: Float, z: Float)
    }

    class AndroidMatrixOps : MatrixOps {
        override fun multiplyMM(dst: FloatArray, lhs: FloatArray, rhs: FloatArray) {
            Matrix.multiplyMM(dst, 0, lhs, 0, rhs, 0)
        }

        override fun multiplyMV(dstVec: FloatArray, lhsMat: FloatArray, rhsVec: FloatArray) {
            Matrix.multiplyMV(dstVec, 0, lhsMat, 0, rhsVec, 0)
        }

        override fun identity(dst: FloatArray) {
            Matrix.setIdentityM(dst, 0)
        }

        override fun perspective(dst: FloatArray, fovy: Float, aspect: Float, zNear: Float, zFar: Float) {
            Matrix.perspectiveM(dst, 0, fovy, aspect, zNear, zFar)
        }

        override fun lookAt(
            dst: FloatArray,
            eyeX: Float,
            eyeY: Float,
            eyeZ: Float,
            centerX: Float,
            centerY: Float,
            centerZ: Float,
            upX: Float,
            upY: Float,
            upZ: Float
        ) {
            Matrix.setLookAtM(dst, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
        }

        override fun translate(dst: FloatArray, x: Float, y: Float, z: Float) {
            Matrix.translateM(dst, 0, x, y, z)
        }

        override fun rotate(dst: FloatArray, angleDegrees: Float, x: Float, y: Float, z: Float) {
            Matrix.rotateM(dst, 0, angleDegrees, x, y, z)
        }

        override fun scale(dst: FloatArray, x: Float, y: Float, z: Float) {
            Matrix.scaleM(dst, 0, x, y, z)
        }
    }

    private val matrixOps = AndroidMatrixOps()
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val lightModelMatrix = FloatArray(16)

    private val lightPosInModelSpace = floatArrayOf(0f, 0f, 0f, 1f)
    private val lightPosInWorldSpace = FloatArray(4)
    private val lightPosInEyeSpace = FloatArray(4)

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
    private val textures = mutableMapOf<Int, GLTextureWrapper>()
    private var vertexShaderSource = ""
    private var fragmentShaderSource = ""

    /** Adds [box] to the drawing list.*/
    fun addBox(box: Box) {
        if (box !in boxes) {
            boxes += box
            box.rendererHandle = BoxRenderer(box)
        }
    }

    /** Removes [box] from the drawing list. */
    fun removeBox(box: Box) {
        if (box in boxes) {
            boxes -= box
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

    private var assetsLoaded: Boolean = false

    /** Preload renderer assets */
    fun preloadAssets() {
        // This will need a rewrite without a JVM.
        if (!assetsLoaded) {
            assetsLoaded = true
            val executor = Executors.newCachedThreadPool()
            val result = ConcurrentHashMap<Int, GLTextureWrapper>()
            for ((txtId, file) in Textures.FILES) {
                executor.submit {
                    val data = assetResolver.getAsByteArray(file)
                    val wrapper = ByteArrayInputStream(data).use { stream ->
                        GLTextureWrapper.createFromPNGInputStream(stream)
                    }
                    println("register $txtId for $file!")
                    result[txtId] = wrapper
                }
            }
            executor.submit {
                vertexShaderSource = assetResolver.getAsString("vertexShader.glsl")
                fragmentShaderSource = assetResolver.getAsString("fragmentShader.glsl")
            }
            executor.shutdown() // wait till all tasks end
            executor.awaitTermination(1, TimeUnit.MINUTES)
            result.toMap(textures)
            println("textuers: $textures")
        }
    }

    private var glInited = false

    fun init() {
        if (!glInited) {
            textures.values.forEach { it.load(gl) }
            val vertexShader = loadShaderFromSource(vertexShaderSource, gl.GL_VERTEX_SHADER)
            val fragmentShader = loadShaderFromSource(fragmentShaderSource, gl.GL_FRAGMENT_SHADER)
            program = createAndLinkProgram(vertexShader, fragmentShader, arrayOf("a_Position", "a_Color", "a_Normal", "a_TexCoordinate"))
            glInited = true
        }
    }

    fun setResolution(width: Int, height: Int) {
        this.width = width
        this.height = height
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

    fun draw() {
        mvpMatrixHandle = gl.glGetUniformLocation(program, "u_MVPMatrix")
        mvMatrixHandle = gl.glGetUniformLocation(program, "u_MVMatrix")
        lightPosHandle = gl.glGetUniformLocation(program, "u_LightPos")
        positionHandle = gl.glGetAttribLocation(program, "a_Position")
        colorHandle = gl.glGetAttribLocation(program, "a_Color")
        normalHandle = gl.glGetAttribLocation(program, "a_Normal")
        textureUniformHandle = gl.glGetUniformLocation(program, "u_Texture")
        textureCoordinateHandle = gl.glGetAttribLocation(program, "a_TexCoordinate")

        gl.glClearColor(6f/255f,2f/255f,20f/255f, 1f) //  sky
        gl.glClear(gl.GL_DEPTH_BUFFER_BIT or gl.GL_COLOR_BUFFER_BIT)
        gl.glViewport(0, 0, width, height)
        gl.glEnable(gl.GL_CULL_FACE)
        gl.glEnable(gl.GL_DEPTH_TEST)
        gl.glUseProgram(program)
        gl.glActiveTexture(gl.GL_TEXTURE0)
        gl.glBlendFunc(gl.GL_SRC_ALPHA, gl.GL_ONE_MINUS_SRC_ALPHA)

        matrixOps.identity(projectionMatrix)
        matrixOps.perspective(projectionMatrix, 30f, width.toFloat()/height.toFloat(), 0.001f, 1000f)

        matrixOps.identity(viewMatrix)
        matrixOps.lookAt(viewMatrix,
            cameraPosX,
            cameraPosY,
            cameraPosZ,
            cameraPosX + cos(-cameraRotY - 90f),
            cameraPosY - sin(-cameraRotX),
            cameraPosZ + sin(-cameraRotY - 90f),
            0f,
            1f,
            0f)

        // Draw the light as a box
        val time = System.currentTimeMillis() % 10000L
        val angleInDegrees = (360.0f / 10000.0f) * time.toInt()
        matrixOps.identity(lightModelMatrix)
        matrixOps.translate(lightModelMatrix, 0f, 20f, 0f)
        matrixOps.rotate(lightModelMatrix, angleInDegrees, 0f, 1f, 0f)
        matrixOps.translate(lightModelMatrix, 0f, 0f, 50f)

        // Convert to lightPosInEyeSpace
        matrixOps.multiplyMV(lightPosInWorldSpace, lightModelMatrix, lightPosInModelSpace)
        matrixOps.multiplyMV(lightPosInEyeSpace, viewMatrix, lightPosInWorldSpace)
        gl.glUniform3f(lightPosHandle, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2])

        // Draw all boxes
        for (box in boxes) {
            physicsInterface.getBoxOpenGLMatrix(box, modelMatrix)
            matrixOps.scale(modelMatrix, box.size.x / 2f, box.size.y / 2f, box.size.z / 2f)

            if (box.theColor.w < 1f) {
                gl.glEnable(gl.GL_BLEND)
            } else {
                gl.glDisable(gl.GL_BLEND)
            }

            textures[box.textureId]?.bind(gl)
            gl.glUniform1i(textureUniformHandle, 0)
            drawCube(box)
        }
    }


    private fun drawCube(box: Box) {
        val renderer = box.rendererHandle as BoxRenderer

        // pass vertex position
        mCubePositions.position(0)
        gl.glVertexAttribPointer(positionHandle, POSITION_DATA_SIZE, gl.GL_FLOAT, false, 0, mCubePositions)
        gl.glEnableVertexAttribArray(positionHandle)

        // pass color
        renderer.colorBuffer.position(0)
        gl.glVertexAttribPointer(colorHandle, COLOR_DATA_SIZE, gl.GL_FLOAT, false, 0, renderer.colorBuffer)
        gl.glEnableVertexAttribArray(colorHandle)

        // pass normals
        mCubeNormals.position(0)
        gl.glVertexAttribPointer(normalHandle, NORMAL_DATA_SIZE, gl.GL_FLOAT, false, 0, mCubeNormals)
        gl.glEnableVertexAttribArray(normalHandle)

        // pass texture coordinates
        renderer.textureCoordsBuffer.position(0)
        gl.glVertexAttribPointer(textureCoordinateHandle, TEXTURE_COORDS_DATA_SIZE, gl.GL_FLOAT, false, 0, renderer.textureCoordsBuffer)
        gl.glEnableVertexAttribArray(textureCoordinateHandle)

        matrixOps.multiplyMM(mvpMatrix, viewMatrix, modelMatrix)
        gl.glUniformMatrix4fv(mvMatrixHandle, false, mvpMatrix)

        matrixOps.multiplyMM(mvpMatrix, projectionMatrix, mvpMatrix)
        gl.glUniformMatrix4fv(mvpMatrixHandle, false, mvpMatrix)

        gl.glDrawArrays(gl.GL_TRIANGLES, 0, 36)
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
}