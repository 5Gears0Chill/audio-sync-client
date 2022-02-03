package com.example.audiosyncronizer

import android.util.Log
import android.widget.EditText
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString


class EchoWebSocketListener(private val callback: (result: String?) -> Unit) : WebSocketListener() {

    private fun printMessage(message: String) {
        callback.invoke(message)
    }


    override fun onOpen(webSocket: WebSocket, response: Response) {
        /*webSocket.send("What's up ?")
        webSocket.send(ByteString.decodeHex("abcd"))
        webSocket.close(CLOSE_STATUS, "Socket Closed !!")*/
    }

    override fun onMessage(webSocket: WebSocket, message: String) {
        Log.d("Server", "Receive Message: $message")
        printMessage(message)
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.d("Server", "Receive Bytes : " + bytes.hex())
        printMessage(bytes.hex())
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(CLOSE_STATUS, null)
        Log.d("Server","Closing Socket : $code / $reason")
    }

    companion object {
        private const val CLOSE_STATUS = 1000
    }
}