package io.snower.game.client;

import io.snower.game.common.*
import org.joml.Matrix4f
import org.joml.Vector4f
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.vecmath.Color4f
import javax.vecmath.Vector3f
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the world and loadAssets the assets that are necessary for
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
    }

    private val mCubePositions: FloatBuffer
    private val mCubeColors: FloatBuffer
    private val mCubeNormals: FloatBuffer
    private val mCubeTextureCoordinates: FloatBuffer
    private val mCubeTextureCoordinatesRef: FloatBuffer

    private var width: Int = 0
    private var height: Int = 0

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

        // R, G, B, A
        val cubeColorData = floatArrayOf(
            // Front face (red)
            1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f,
            // Right face (green)
            0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f,
            // Back face (blue)
            0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f,
            // Left face (yellow)
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f,
            // Top face (cyan)
            0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f,
            // Bottom face (magenta)
            1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f)

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
        mCubeColors = ByteBuffer.allocateDirect(cubeColorData.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        mCubeColors.put(cubeColorData).position(0)
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

    private val viewMatrix = Matrix4f()
    private val projectionMatrix = Matrix4f()
    private val modelMatrix = Matrix4f()
    private val mvpMatrix = Matrix4f()
    private val lightModelMatrix = Matrix4f()

    private val lightPosInModelSpace = Vector4f(0f, 0f, 0f, 1f)
    private val lightPosInWorldSpace = Vector4f()
    private val lightPosInEyeSpace = Vector4f()

    private var program: Int = 0
    private var mvpMatrixHandle = 0
    private var mvMatrixHandle = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var normalHandle = 0
    private var lightPosHandle = 0
    private var textureUniformHandle = 0
    private var textureCoordinateHandle = 0

    private val boxes = mutableListOf<Box>()

    // Assets. Kept in memory for performance.
    private val textures = mutableMapOf<Int, GLTextureWrapper>()
    private var vertexShaderSource = ""
    private var fragmentShaderSource = ""

    /** Adds [box] to the drawing list.*/
    fun addBox(box: Box) {
        boxes += box
    }

    /** Removes [box] from the drawing list. */
    fun removeBox(box: Box) {
        boxes -= box
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

    private var assetsLoaded = false
    private var glInited = false

    /** Load renderer assets into memory, such as textures and shaders. Does't touch OpenGL. */
    fun loadAssets() {
        if (!assetsLoaded) {
            for ((txtId, txtFile) in Textures.FILES) {
                try {
                    val data = assetResolver.getAsByteArray(txtFile)
                    val inputStream = ByteArrayInputStream(data)
                    inputStream.use {
                        textures[txtId] = GLTextureWrapper.createFromPNGInputStream(it)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    error("can't load txt id $txtId (file: $txtFile): $e")
                }
            }

            vertexShaderSource = assetResolver.getAsString("vertexShader.glsl")
            fragmentShaderSource = assetResolver.getAsString("fragmentShader.glsl")

            assetsLoaded = true
        }
    }

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

        projectionMatrix
            .identity()
            .perspective(30f, width.toFloat()/height.toFloat(), 0.001f, 1000f)

        viewMatrix
            .identity()
            .lookAt(
                cameraPosX,
                cameraPosY,
                cameraPosZ,
                cameraPosX + cos(radians(-cameraRotY - 90f)),
                cameraPosY - sin(radians(-cameraRotX)),
                cameraPosZ + sin(radians(-cameraRotY - 90f)),
                0f,
                -1f,
                0f
            )

        // Draw the light as a box
        val time = System.currentTimeMillis() % 10000L
        val angleInDegrees = (360.0f / 10000.0f) * time.toInt()
        lightModelMatrix
            .identity()
            .translate(0f, 20f, 0f)
            .rotate(radians(angleInDegrees), 0f, 1f, 0f)
            .translate(0f, 0f, 50f)
        lightPosInModelSpace.mul(lightModelMatrix, lightPosInWorldSpace)
        lightPosInWorldSpace.mul(viewMatrix, lightPosInEyeSpace)

        modelMatrix.set(lightModelMatrix)
        drawCube(Box(
            textureId = Textures.METAL_ID,
            theColor = Color4f(1.0f, 1f, 1f, 1f)
        ))

        // Draw all boxes
        val tmpMatrix = FloatArray(16)
        for (box in boxes) {
            physicsInterface.getBoxOpenGLMatrix(box, tmpMatrix)

            modelMatrix.set(tmpMatrix)
            modelMatrix.scale(box.size.x / 2f, box.size.y / 2f, box.size.z / 2f)

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

    private val tmpArrayBuffer = FloatArray(16)

    private fun drawCube(box: Box) {
        mCubePositions.position(0)
        gl.glVertexAttribPointer(positionHandle, POSITION_DATA_SIZE, gl.GL_FLOAT, false, 0, mCubePositions)
        gl.glEnableVertexAttribArray(positionHandle)

        // fill with box info the color. may be slow? To profile
        mCubeColors.position(0)
        val (r,g,b,a) = box.theColor
        repeat(mCubeColors.capacity() / COLOR_DATA_SIZE) {
            mCubeColors.put(r)
            mCubeColors.put(g)
            mCubeColors.put(b)
            mCubeColors.put(a)
        }
        mCubeColors.position(0)
        gl.glVertexAttribPointer(colorHandle, COLOR_DATA_SIZE, gl.GL_FLOAT, false, 0, mCubeColors)
        gl.glEnableVertexAttribArray(colorHandle)

        mCubeNormals.position(0)
        gl.glVertexAttribPointer(normalHandle, NORMAL_DATA_SIZE, gl.GL_FLOAT, false, 0, mCubeNormals)
        gl.glEnableVertexAttribArray(normalHandle)

        mCubeTextureCoordinates.position(0)
        repeat(mCubeTextureCoordinates.capacity()) { i ->
            mCubeTextureCoordinates.put(mCubeTextureCoordinatesRef.get(i) * box.textureMultiplier.toFloat())
        }
        mCubeTextureCoordinates.position(0)
        gl.glVertexAttribPointer(textureCoordinateHandle, TEXTURE_COORDS_DATA_SIZE, gl.GL_FLOAT, false, 0, mCubeTextureCoordinates)
        gl.glEnableVertexAttribArray(textureCoordinateHandle)

        // this matrix multiplications are slow? to profile
        viewMatrix.mul(modelMatrix, mvpMatrix)
        mvpMatrix.get(tmpArrayBuffer)
        gl.glUniformMatrix4fv(mvMatrixHandle, false, tmpArrayBuffer)

        projectionMatrix.mul(mvpMatrix, mvpMatrix)
        mvpMatrix.get(tmpArrayBuffer)
        gl.glUniformMatrix4fv(mvpMatrixHandle, false, tmpArrayBuffer)

        gl.glUniform3f(lightPosHandle, lightPosInEyeSpace.x, lightPosInEyeSpace.y, lightPosInEyeSpace.z)
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