package com.peinbol.client

import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.AL11
import org.lwjgl.openal.AL11.AL_LINEAR_DISTANCE
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.libc.LibCStdlib.free
import java.nio.ShortBuffer
import javax.vecmath.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL11.AL_EXPONENT_DISTANCE
import java.nio.FloatBuffer
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption


class AudioManager {
    private val audioBuffers = mutableMapOf<Int, Int>()
    private var context: Long = 0L
    private var device: Long = 0L
    private val sources = mutableListOf<AudioSource>()

    fun init() {
        val defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER)
        device = alcOpenDevice(defaultDeviceName)
        val attributes = intArrayOf(0)
        context = alcCreateContext(device, attributes)
        alcMakeContextCurrent(context)
        AL.createCapabilities(ALC.createCapabilities(device))

        alDistanceModel(AL_EXPONENT_DISTANCE)
        for ((id, file) in Audios.FILES) {
            val bufferPointer = createBufferForAudioFile(file)
            audioBuffers[id] = bufferPointer
        }
    }

    fun registerSource(source: AudioSource) {
        if (source in sources) return
        val bufferForId = audioBuffers[source.audioId] ?: error("can't get buffer for audio id: ${source.audioId}")
        val sourcePointer = alGenSources()
        alSourcei(sourcePointer, AL_BUFFER, bufferForId)
        alSourcei(sourcePointer, AL_LOOPING, if (source.loop) AL_TRUE else AL_FALSE)
        source.sourceId = sourcePointer
        alSourcePlay(sourcePointer)
        sources.add(source)
    }

    fun unregisterSource(source: AudioSource) {
        if (source !in sources) return
        alSourceStop(source.sourceId)
        alDeleteSources(source.sourceId)
        sources.remove(source)
    }

    fun update(listenerPosition: Vector3f, cameraVector: Vector3f) {
        val listenerOri = BufferUtils.createFloatBuffer(6).put(floatArrayOf(0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f))
        alListener3f(AL_POSITION, listenerPosition.x, listenerPosition.y, listenerPosition.z)
        alListenerfv(AL_ORIENTATION, floatArrayOf(cameraVector.x, cameraVector.y, cameraVector.z, 0.0f, 1.0f, 0.0f))
        alListener3f(AL_VELOCITY, 0f, 0f, 0f)
        val iterator = sources.iterator()
        while (iterator.hasNext()) {
            val source = iterator.next()
            var playing = true
            stackPush().use { stack ->
                val soundState = stack.mallocInt(1)
                alGetSourcei(source.sourceId, AL_SOURCE_STATE, soundState)
                playing = soundState[0] == AL_PLAYING
            }
            // Check if the sound finished
            if (!playing && !source.loop) {
                alSourceStop(source.sourceId)
                alDeleteSources(source.sourceId)
                iterator.remove()
            } else {
                alSource3f(source.sourceId, AL_POSITION, source.position.x, source.position.y, source.position.z)
                alSourcef(source.sourceId, AL_GAIN, source.volume * 0.7f)
                alSourcef(source.sourceId, AL_PITCH, source.pitch)
                alSourcef(source.sourceId, AL_ROLLOFF_FACTOR, 2.5f)
                alSourcef(source.sourceId, AL_MAX_DISTANCE, source.ratio)
                alSourcef(source.sourceId, AL_REFERENCE_DISTANCE, source.ratio)
            }
        }
    }

    fun destroy() {
        alcDestroyContext(context)
        alcCloseDevice(device)
        for (bufferId in audioBuffers.values) alDeleteBuffers(bufferId)
    }

    private fun createBufferForAudioFile(file: String): Int {
        var rawAudioBuffer: ShortBuffer? = null
        var channels: Int? = null
        var sampleRate: Int? = null

        stackPush().use { stack ->
            val channelsBuffer = stack.mallocInt(1)
            val sampleRateBuffer = stack.mallocInt(1)
            Files.write(Paths.get("audio-file.ogg"), javaClass.classLoader.getResourceAsStream(file).readBytes())
            rawAudioBuffer = stb_vorbis_decode_filename("audio-file.ogg", channelsBuffer, sampleRateBuffer)
            Files.delete(Paths.get("audio-file.ogg"))
            if (rawAudioBuffer == null) error("can't load audio file: '$file'")
            channels = channelsBuffer.get(0)
            sampleRate = sampleRateBuffer.get(0)
        }
        var format = -1
        if (channels!! == 1) {
            format = AL_FORMAT_MONO16
        } else if (channels!! == 2) {
            format = AL_FORMAT_STEREO16
        }
        val bufferPointer = alGenBuffers()
        alBufferData(bufferPointer, format, rawAudioBuffer!!, sampleRate!!)
        free(rawAudioBuffer)
        return bufferPointer
    }
}