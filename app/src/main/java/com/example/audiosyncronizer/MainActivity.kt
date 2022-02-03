package com.example.audiosyncronizer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var mClient: OkHttpClient
    private lateinit var startRecordingButton: Button
    private lateinit var currentTime: EditText
    private lateinit var messageText: EditText
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

        mClient = OkHttpClient()

        startRecordingButton = findViewById(R.id.btnStartRecording)
        currentTime = findViewById(R.id.textTime)
        messageText = findViewById(R.id.messageTextView)

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

        startWebSockerListener()
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


    private fun startWebSockerListener() {
        val request = Request.Builder().url("wss://demo.piesocket.com/v3/channel_1?api_key=oCdCMcMPQpbvNjUIzqtvF1d2X2okWpDQj4AwARJuAgtjhzKxVEjQU6IdCjwm&notify_self").build()
        val listener = EchoWebSocketListener(messageText)
        val webSocket: WebSocket = mClient.newWebSocket(request, listener)
        mClient.dispatcher().executorService().shutdown()
    }

    private class EchoWebSocketListener(messageText: EditText) : WebSocketListener() {

        val messageText = messageText

        private fun printMessage(message: String) {
            messageText.setText(messageText.text.toString() + "\n" + message)
        }


        override fun onOpen(webSocket: WebSocket, response: Response) {
            /*webSocket.send("What's up ?")
            webSocket.send(ByteString.decodeHex("abcd"))
            webSocket.close(CLOSE_STATUS, "Socket Closed !!")*/
        }

        override fun onMessage(webSocket: WebSocket, message: String) {
            printMessage("Receive Message: $message")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            printMessage("Receive Bytes : " + bytes.hex())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(CLOSE_STATUS, null)
            printMessage("Closing Socket : $code / $reason")
        }

        override fun onFailure(webSocket: WebSocket, throwable: Throwable, response: Response) {
            printMessage("Error : " + throwable.message)
        }

        companion object {
            private const val CLOSE_STATUS = 1000
        }
    }
}