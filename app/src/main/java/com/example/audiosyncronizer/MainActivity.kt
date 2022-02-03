package com.example.audiosyncronizer

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.airbnb.lottie.LottieAnimationView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val POLL_INTERVAL = 100L

@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
class MainActivity : AppCompatActivity() {

    private lateinit var startRecordingButton: Button
    private lateinit var currentTime: EditText
    private lateinit var animator: ValueAnimator       // Custom animator to drive the lottie
    private lateinit var lottieView: LottieAnimationView

    private val handler = Handler()
    private var mSensor: SoundMeter? = null
    private lateinit var mWakeLock: PowerManager.WakeLock
    private val mThreshold = 7.0
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
                lottieView.playAnimation()
                sendTimestampToServer()
            }
            handler.postDelayed(this, POLL_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startRecordingButton = findViewById(R.id.btnStartRecording)
        lottieView = findViewById(R.id.lottieView)
        lottieView.setMinAndMaxFrame(0, 33)
        filePath = "${baseContext.externalCacheDir!!.absolutePath}/audiorecordtest.3gp"

        currentTime = findViewById(R.id.textTime)
        startTimer(currentTime)

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

        animator = with(ValueAnimator.ofFloat(0.0f)) {
            interpolator = AccelerateInterpolator()

            // bind it to the lottie
            addUpdateListener {
                val progress = it.animatedValue as Float
                lottieView.progress = progress
            }
            this
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
        animator.setCurrentFraction(0.0f)
        mSensor!!.start(filePath)
        handler.postDelayed(mPollTask, POLL_INTERVAL)
    }

    private fun stop() {
        if (mWakeLock.isHeld) {
            mWakeLock.release()
        }
        handler.removeCallbacks(mPollTask)
        handler.removeCallbacks(mSleepTask)
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