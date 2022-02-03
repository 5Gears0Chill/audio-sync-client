package com.example.audiosyncronizer

import android.widget.EditText
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString


class EchoWebSocketListener(messageText: EditText) : WebSocketListener() {

    val messageText = messageText

    private fun printMessage(message: String) {
        //messageText.setText(messageText.text.toString() + "\n" + message)
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

    companion object {
        private const val CLOSE_STATUS = 1000
    }
}