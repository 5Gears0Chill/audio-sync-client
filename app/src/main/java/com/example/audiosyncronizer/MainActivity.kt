package com.example.audiosyncronizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var startRecordingButton: Button
    private lateinit var currentTime: EditText
    private val handler = Handler()
    private var mSensor: SoundMeter? = null
    private lateinit var mWakeLock: PowerManager.WakeLock
    private val mThreshold = 8
    private val POLL_INTERVAL = 300L
    private var isRunning = false

    private val mSleepTask = Runnable {
        start()
    }

    private val mPollTask: Runnable = object : Runnable {
        override fun run() {
            val amp: Double = mSensor!!.amplitude
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
        currentTime = findViewById(R.id.textTime)
        startTimer(currentTime)

        mSensor = SoundMeter()
        startRecordingButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,  arrayOf(Manifest.permission.RECORD_AUDIO),
                    10)
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
        mSensor!!.start()
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

    private fun startTimer(editText: EditText) {
        val someHandler = Handler(mainLooper)
        someHandler.postDelayed(object : Runnable {
            override fun run() {
                editText.setText(SimpleDateFormat("HH:mm:ss.SSS", Locale.UK).format(Date()))
                someHandler.postDelayed(this, 1)
            }
        }, 10)
    }
}