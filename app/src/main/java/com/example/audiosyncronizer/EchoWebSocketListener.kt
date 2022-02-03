package com.example.audiosyncronizer

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class EchoWebSocketListener(private val callback: (result: BroadcastMessages?) -> Unit) :
    WebSocketListener() {

    private fun printMessage(message: BroadcastMessages) {
        callback.invoke(message)
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        /*webSocket.send("What's up ?")
        webSocket.send(ByteString.decodeHex("abcd"))
        webSocket.close(CLO
        SE_STATUS, "Socket Closed !!")*/
    }

    fun sendMessage(webSocket: WebSocket, message: String) {
        webSocket.send(message)
    }

    override fun onMessage(webSocket: WebSocket, message: String) {
        Log.d("Server", "Receive Message: $message")
        try {
            val broadcastMessages = Gson().fromJson(message, BroadcastMessages::class.java)
            printMessage(broadcastMessages)
        } catch (ex: JsonSyntaxException) {
            Log.d("Server", "Invalid JSON: $message")
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d("Server", "Receive Bytes : " + bytes.hex())
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(CLOSE_STATUS, null)
        Log.d("Server", "Closing Socket : $code / $reason")
    }

    companion object {
        private const val CLOSE_STATUS = 1000
    }
}