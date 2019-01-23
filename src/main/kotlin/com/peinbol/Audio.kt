package com.peinbol

import org.lwjgl.openal.*
import org.lwjgl.system.*

import java.nio.*

import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.ALC10.*
import org.lwjgl.stb.STBVorbis.*
import org.lwjgl.system.MemoryStack.*
import org.lwjgl.system.libc.LibCStdlib.*
import org.lwjgl.openal.ALC10.alcCloseDevice
import org.lwjgl.openal.ALC10.alcDestroyContext
import org.lwjgl.openal.AL10.alSourcePlay
import org.lwjgl.openal.AL10.AL_BUFFER
import org.lwjgl.openal.AL10.alSourcei
import org.lwjgl.openal.AL10.AL_FORMAT_STEREO16
import org.lwjgl.openal.AL10.AL_FORMAT_MONO16
import java.nio.IntBuffer
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryStack
import java.nio.ShortBuffer
import org.lwjgl.openal.AL
import org.lwjgl.openal.ALCapabilities
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALCCapabilities
import org.lwjgl.openal.ALC10.alcMakeContextCurrent
import org.lwjgl.openal.ALC10.ALC_DEFAULT_DEVICE_SPECIFIER
import org.lwjgl.openal.ALC10.alcGetString

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

            rawAudioBuffer = stb_vorbis_decode_filename("/media/lleps/Compartido/Dev/KotlinFun/src/main/resources/steps.ogg", channelsBuffer, sampleRateBuffer)

            println("rab: $rawAudioBuffer")
            //Retreive the extra information that was stored in the buffers by the function
            channels = channelsBuffer.get(0)
            sampleRate = sampleRateBuffer.get(0)
        }

//Find the correct OpenAL format
        var format = -1
        if (channels!! == 1) {
            format = AL_FORMAT_MONO16
        } else if (channels!! == 2) {
            format = AL_FORMAT_STEREO16
        }

//Request space for the buffer
        val bufferPointer = alGenBuffers()

//Send the data to OpenAL
        alBufferData(bufferPointer, format, rawAudioBuffer!!, sampleRate!!)

//Free the memory allocated by STB
        free(rawAudioBuffer)

//Request a source
        val sourcePointer = alGenSources()

//Assign the sound we just loaded to the source
        alSourcei(sourcePointer, AL_BUFFER, bufferPointer)

//Play the sound
        alSourcePlay(sourcePointer)

        try {
            //Wait for a second
            //Thread.sleep(1000)
        } catch (ignored: InterruptedException) {
        }


//Terminate OpenAL
        alDeleteSources(sourcePointer)
        alDeleteBuffers(bufferPointer)
        alcDestroyContext(context)
        alcCloseDevice(device)
    }

}