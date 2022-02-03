package com.example.audiosyncronizer

import android.media.MediaRecorder
import java.io.IOException

private const val EMA_FILTER = 0.6

class SoundMeter {
    private var mRecorder: MediaRecorder? = null
    private var mEMA = 0.0

    fun start(filePath: String) {
        if (mRecorder == null) {
            mRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(filePath)
                try {
                    prepare()
                } catch (e: IllegalStateException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                } catch (e: IOException) {
                    // TODO Auto-generated catch block
                    e.printStackTrace()
                }
                start()
            }
        }
    }

    fun stop() {
        if (mRecorder != null) {
            mRecorder!!.apply {
                stop()
                release()
            }
            mRecorder = null
        }
    }

    val amplitude: Double
        get() = if (mRecorder != null) mRecorder!!.maxAmplitude / 2700.0 else 0.0

    fun getAmplitudeEMA(): Double {
        val amp = amplitude
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA
        return mEMA
    }
}