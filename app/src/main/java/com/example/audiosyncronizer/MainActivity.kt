package com.example.audiosyncronizer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val POLL_INTERVAL = 100L

class MainActivity : AppCompatActivity() {

    private lateinit var startRecordingButton: Button
    private val handler = Handler()
    private var mSensor: SoundMeter? = null
    private lateinit var mWakeLock: PowerManager.WakeLock
    private val mThreshold = 8.0
    private var isRunning = false

    var filePath = ""

    private val mSleepTask = Runnable {
        start()
    }

    private val mPollTask: Runnable = object : Runnable {
        override fun run() {
            val amp: Double = mSensor!!.amplitude
            Log.d("Amplitude", amp.toString())
            if (amp > mThreshold) {
                sendTimestampToServer()
            }
            handler.postDelayed(this, POLL_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startRecordingButton = findViewById(R.id.btnStartRecording)
        filePath = "${baseContext.externalCacheDir!!.absolutePath}/audiorecordtest.3gp"


        mSensor = SoundMeter()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "audiosyncronizer:my_wake_lock_tag")
        startRecordingButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,  arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO_PERMISSION)
            } else {
                start()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isRunning) {
            isRunning = true
        }
    }

    override fun onStop() {
        super.onStop()
        stop()
    }

    private fun start() {
        mSensor!!.start(filePath)
        handler.postDelayed(mPollTask, POLL_INTERVAL)
    }

    private fun stop() {
        if (mWakeLock.isHeld) {
            mWakeLock.release()
        }
        handler.removeCallbacks(mSleepTask)
        handler.removeCallbacks(mPollTask)
        mSensor!!.stop()
        isRunning = false
    }

    private fun sendTimestampToServer() {
        Log.d("PITCH HEARD", "PITCH")
        stop()
    }
}