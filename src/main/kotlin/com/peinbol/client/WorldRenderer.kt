package com.peinbol.client

import com.peinbol.*
import org.joml.Matrix4f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11.glClearColor
import org.lwjgl.opengl.GL20.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

class WorldRenderer {
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
    private val textures = mutableMapOf<Int, Texture>()

    fun addBox(box: Box) {
        boxes += box
    }

    fun removeBox(box: Box) {
        boxes -= box
    }

    var cameraPosX = 0f
    var cameraPosY = 0f
    var cameraPosZ = 0f

    // maybe camera can be converted to vectors
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

    fun init(width: Int, height: Int) {
        for ((txtId, txtFile) in Textures.FILES) {
            textures[txtId] = Texture(javaClass.classLoader.getResource(txtFile))
        }

        this.width = width
        this.height = height
        val vertexShader = loadShaderFromFile("vertexShader.glsl", GL_VERTEX_SHADER)
        val fragmentShader = loadShaderFromFile("fragmentShader.glsl", GL_FRAGMENT_SHADER)
        program = createAndLinkProgram(vertexShader, fragmentShader, arrayOf("a_Position", "a_Color", "a_Normal", "a_TexCoordinate"))
    }

    fun draw() {
        mvpMatrixHandle = glGetUniformLocation(program, "u_MVPMatrix")
        mvMatrixHandle = glGetUniformLocation(program, "u_MVMatrix")
        lightPosHandle = glGetUniformLocation(program, "u_LightPos")
        positionHandle = glGetAttribLocation(program, "a_Position")
        colorHandle = glGetAttribLocation(program, "a_Color")
        normalHandle = glGetAttribLocation(program, "a_Normal")
        textureUniformHandle = glGetUniformLocation(program, "u_Texture")
        textureCoordinateHandle = glGetAttribLocation(program, "a_TexCoordinate")

        glClearColor(10f/255f,2f/255f,36f/255f, 1f) //  sky
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)
        glViewport(0, 0, width, height)
        glEnable(GL_CULL_FACE)
        glEnable(GL_DEPTH_TEST)
        glUseProgram(program)
        glActiveTexture(GL_TEXTURE0)

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

        val toX = cameraPosX + cos(radians(cameraRotY))
        val toY = cameraPosY + cos(radians(cameraRotX))
        val toZ = cameraPosZ + sin(radians(cameraRotY))
        //println("from: $cameraPosX $cameraPosY $cameraPosZ  to: $toX $toY $toZ   rot: $cameraRotX $cameraRotY")

        val time = System.currentTimeMillis() % 10000L
        val angleInDegrees = (360.0f / 10000.0f) * time.toInt()

        lightModelMatrix
            .identity()
            .translate(0f, -40f, -5f)
            .rotate(radians(angleInDegrees), 0f, 1f, 0f)
            .translate(0f, 0f, 2f)

        lightPosInModelSpace.mul(lightModelMatrix, lightPosInWorldSpace)
        lightPosInWorldSpace.mul(viewMatrix, lightPosInEyeSpace)

        for (box in boxes) {
            modelMatrix
                .identity()
                .translate(box.position.x, box.position.y, box.position.z)
                .scale(box.size.x / 2f, box.size.y / 2f, box.size.z / 2f)

            textures[box.textureId]?.bind()
            glUniform1i(textureUniformHandle, 0)
            drawCube(box)
        }
    }

    private val tmpFloatBuffer = BufferUtils.createFloatBuffer(16)

    private fun drawCube(box: Box) {
        mCubePositions.position(0)
        glVertexAttribPointer(positionHandle, POSITION_DATA_SIZE, GL_FLOAT, false, 0, mCubePositions)
        glEnableVertexAttribArray(positionHandle)

        // fill with box info the color.
        mCubeColors.position(0)
        val (r,g,b,a) = box.theColor
        repeat(mCubeColors.capacity() / COLOR_DATA_SIZE) {
            mCubeColors.put(r)
            mCubeColors.put(g)
            mCubeColors.put(b)
            mCubeColors.put(a)
        }
        mCubeColors.position(0)
        glVertexAttribPointer(colorHandle, COLOR_DATA_SIZE, GL_FLOAT, false, 0, mCubeColors)
        glEnableVertexAttribArray(colorHandle)

        mCubeNormals.position(0)
        glVertexAttribPointer(normalHandle, NORMAL_DATA_SIZE, GL_FLOAT, false, 0, mCubeNormals)
        glEnableVertexAttribArray(normalHandle)

        mCubeTextureCoordinates.position(0)
        repeat(mCubeTextureCoordinates.capacity()) { i ->
            mCubeTextureCoordinates.put(mCubeTextureCoordinatesRef.get(i) * box.textureMultiplier.toFloat())
        }
        mCubeTextureCoordinates.position(0)
        glVertexAttribPointer(textureCoordinateHandle, TEXTURE_COORDS_DATA_SIZE, GL_FLOAT, false, 0, mCubeTextureCoordinates)
        glEnableVertexAttribArray(textureCoordinateHandle)

        viewMatrix.mul(modelMatrix, mvpMatrix)
        tmpFloatBuffer.position(0)
        mvpMatrix.get(tmpFloatBuffer)
        glUniformMatrix4fv(mvMatrixHandle, false, tmpFloatBuffer)

        projectionMatrix.mul(mvpMatrix, mvpMatrix)
        tmpFloatBuffer.position(0)
        mvpMatrix.get(tmpFloatBuffer)
        glUniformMatrix4fv(mvpMatrixHandle, false, tmpFloatBuffer)

        glUniform3f(lightPosHandle, lightPosInEyeSpace.x, lightPosInEyeSpace.y, lightPosInEyeSpace.z)
        glDrawArrays(GL_TRIANGLES, 0, 36)
    }

    private fun loadShaderFromFile(file: String, type: Int): Int {
        val src = javaClass.classLoader.getResourceAsStream(file)
            .readBytes()
            .toString(Charset.defaultCharset())

        return loadShaderFromSource(src, type, file)
    }

    private fun loadShaderFromSource(source: String, type: Int, file: String? = null): Int {
        val shaderHandle = glCreateShader(type)
        if (shaderHandle != 0) {
            glShaderSource(shaderHandle, source)
            glCompileShader(shaderHandle)
            val compileStatus = IntArray(1)
            glGetShaderiv(shaderHandle, GL_COMPILE_STATUS, compileStatus)
            if (compileStatus[0] == 0) {
                val errorMsg = glGetShaderInfoLog(shaderHandle)
                glDeleteShader(shaderHandle)
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
        var programHandle = glCreateProgram()
        if (programHandle != 0) {
            glAttachShader(programHandle, vertexShaderHandle)
            glAttachShader(programHandle, fragmentShaderHandle)
            if (attributes != null) {
                val size = attributes.size
                for (i in 0 until size) {
                    glBindAttribLocation(programHandle, i, attributes[i])
                }
            }

            glLinkProgram(programHandle)
            val linkStatus = IntArray(1)
            glGetProgramiv(programHandle, GL_LINK_STATUS, linkStatus)

            if (linkStatus[0] == 0) {
                glDeleteProgram(programHandle)
                programHandle = 0
                error("Error compiling program: " + glGetProgramInfoLog(programHandle))
            }
        }

        if (programHandle == 0) error("Error creating program.")
        return programHandle
    }
}