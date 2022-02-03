package com.example.audiosyncronizer

import android.Manifest
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.animation.AccelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
private const val POLL_INTERVAL = 100L
private const val THRESHOLD = 9.0

@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
class MainActivity : AppCompatActivity() {

    private lateinit var mClient: OkHttpClient
    private lateinit var startRecordingButton: Button
    private lateinit var currentTime: TextView
    private lateinit var messageText: TextView
    private lateinit var animator: ValueAnimator       // Custom animator to drive the lottie
    private lateinit var lottieView: LottieAnimationView

    private val handler = Handler()
    private var mSensor: SoundMeter? = null
    private lateinit var mWakeLock: PowerManager.WakeLock
    private var isRunning = false
    var filePath = ""

    private val mSleepTask = Runnable {
        start()
    }

    private val mPollTask: Runnable = object : Runnable {
        override fun run() {
            val amp: Double = mSensor!!.amplitude
            Log.d("Amplitude", amp.toString())
            if (amp > THRESHOLD) {
                lottieView.playAnimation()
                sendTimestampToServer()
            }
            handler.postDelayed(this, POLL_INTERVAL)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mClient = OkHttpClient()

        startRecordingButton = findViewById(R.id.btnStartRecording)
        lottieView = findViewById(R.id.lottieView)
        lottieView.setMinAndMaxFrame(0, 33)
        filePath = "${baseContext.externalCacheDir!!.absolutePath}/audiorecordtest.3gp"

        currentTime = findViewById(R.id.textTime)
        messageText = findViewById(R.id.messageTextView)
        findViewById<TextView>(R.id.textDeviceID).text =
            Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        startTimer(currentTime)

        mSensor = SoundMeter()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        mWakeLock =
            pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "audiosyncronizer:my_wake_lock_tag")
        startRecordingButton.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO_PERMISSION
                )
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

        startWebSocketListener()
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
        startWebSocketListener()
    }

    private fun startTimer(editText: TextView) {
        val someHandler = Handler(mainLooper)
        someHandler.postDelayed(object : Runnable {
            override fun run() {
                editText.text = SimpleDateFormat("HH:mm:ss.SSS", Locale.UK).format(Date())
                someHandler.postDelayed(this, 1)
            }
        }, 10)
    }

    private fun startWebSocketListener() {
        val request = Request.Builder()
            .url("wss://demo.piesocket.com/v3/channel_1?api_key=oCdCMcMPQpbvNjUIzqtvF1d2X2okWpDQj4AwARJuAgtjhzKxVEjQU6IdCjwm&notify_self")
            .build()
        val listener = EchoWebSocketListener { result ->
            messageText.text = result
        }
        val webSocket: WebSocket = mClient.newWebSocket(request, listener)
        mClient.dispatcher.executorService.shutdown()
    }
}
