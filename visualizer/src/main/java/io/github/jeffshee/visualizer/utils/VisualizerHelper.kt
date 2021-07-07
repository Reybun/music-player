package io.github.jeffshee.visualizer.utils

import android.media.audiofx.Visualizer
import android.os.Handler
import android.util.Log

class VisualizerHelper(sessionId: Int) {

    private val visualizer: Visualizer = Visualizer(sessionId)
    private val fftBuff: ByteArray
    private val fftMF: FloatArray
    private val fftM: DoubleArray
    private val waveBuff: ByteArray
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    init {
        visualizer.captureSize = Visualizer.getCaptureSizeRange()[1]
        fftBuff = ByteArray(visualizer.captureSize)
        waveBuff = ByteArray(visualizer.captureSize)
        fftMF = FloatArray(fftBuff.size / 2 - 1)
        fftM = DoubleArray(fftBuff.size / 2 - 1)
        visualizer.enabled = true
    }

    fun getFft(): ByteArray {
        if (visualizer.enabled) visualizer.getFft(fftBuff)
        return fftBuff
    }

    fun getWave(): ByteArray {
        if (visualizer.enabled) visualizer.getWaveForm(waveBuff)
        return waveBuff
    }

    fun getFftMagnitude(): DoubleArray {
        getFft()
        for (k in 0 until fftMF.size) {
            val i = (k + 1) * 2
            fftM[k] = Math.hypot(fftBuff[i].toDouble(), fftBuff[i + 1].toDouble())
        }
        return fftM
    }

    /**
     * Get Fft values from startHz to endHz
     */
    fun getFftMagnitudeRange(startHz: Int, endHz: Int): DoubleArray {
        val sIndex = hzToFftIndex(startHz)
        val eIndex = hzToFftIndex(endHz)
        return getFftMagnitude().copyOfRange(sIndex, eIndex)
    }

    /**
     * Equation from documentation, kth frequency = k*Fs/(n/2)
     */
    fun hzToFftIndex(Hz: Int): Int {
        return Math.min(Math.max(Hz * 1024 / (44100 * 2), 0), 255)
    }

    /**
     * Log Waveform and Fft values every 1s
     */
    fun startDebug() {
        handler = Handler()
        runnable = object : Runnable {
            override fun run() {
                Log.d("Waveform", getWave().contentToString())
                Log.d("Fft", getFftMagnitude().contentToString())
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    /**
     * Stop logging
     */
    fun stopDebug() {
        handler.removeCallbacks(runnable)
    }

    /**
     * Release visualizer when not using anymore
     */
    fun release() {
        visualizer.enabled = false
        visualizer.release()
    }

}