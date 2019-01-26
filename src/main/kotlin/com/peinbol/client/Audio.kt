package com.peinbol.client

import com.peinbol.timedOscillator
import org.lwjgl.openal.AL
import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.libc.LibCStdlib.free
import java.nio.ShortBuffer

fun main(args: Array<String>) {
    Audio().init()
}

class Audio {
    fun init() {
        val defaultDeviceName = alcGetString(0, ALC_DEFAULT_DEVICE_SPECIFIER)
        val device = alcOpenDevice(defaultDeviceName)

        val attributes = intArrayOf(0)
        val context = alcCreateContext(device, attributes)
        alcMakeContextCurrent(context)

        val alcCapabilities = ALC.createCapabilities(device)
        val alCapabilities = AL.createCapabilities(alcCapabilities)

        var rawAudioBuffer: ShortBuffer? = null

        var channels: Int? = null
        var sampleRate: Int? = null

        stackPush().use { stack ->
            //Allocate space to store return information from the function
            val channelsBuffer = stack.mallocInt(1)
            val sampleRateBuffer = stack.mallocInt(1)

            rawAudioBuffer = stb_vorbis_decode_filename("D:\\Dev\\KotlinFun\\src\\main\\resources\\steps.ogg", channelsBuffer, sampleRateBuffer)

            println("rab: $rawAudioBuffer")
            //Retreive the extra information that was stored in the buffers by the function
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

        val sourcePointer = alGenSources()
        alSourcei(sourcePointer, AL_BUFFER, bufferPointer)
        alSourcei(sourcePointer, AL_LOOPING, AL_TRUE)
        alSourcePlay(sourcePointer)

        try {
            var xCounter = 0f
            //Wait for a second
            while (true) {
                alSource3f(sourcePointer, AL_POSITION, xCounter, 0f, 0f)
                Thread.sleep(16)
                xCounter = (timedOscillator(5000) - 2500f) / 200f
            }
        } catch (ignored: InterruptedException) {
        }

        alDeleteSources(sourcePointer)
        alDeleteBuffers(bufferPointer)
        alcDestroyContext(context)
        alcCloseDevice(device)
    }

}