package com.peinbol.client

import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.libc.LibCStdlib.free
import java.nio.ShortBuffer

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
        alSourcei(sourcePointer, AL_LOOPING, AL_TRUE)
        alSourcePlay(sourcePointer)
        sources.add(source)
    }

    fun unregisterSource(source: AudioSource) {
        if (source !in sources) return
        alSourceStop(source.sourceId)
        alDeleteSources(source.sourceId)
        sources.remove(source)
    }

    fun update() {
        for (source in sources) {
            alSource3f(source.sourceId, AL_POSITION, source.position.x, source.position.y, source.position.z)
            alSourcef(source.sourceId, AL_GAIN, source.volume)
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
            rawAudioBuffer = stb_vorbis_decode_filename(file, channelsBuffer, sampleRateBuffer)
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