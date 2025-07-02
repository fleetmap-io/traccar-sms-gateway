package org.traccar.gateway

import android.os.Build
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

class Firestore {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun log(phone: String, messageId: String?, state: String, message: String? = null) {
        val logEntry = hashMapOf(
            "state" to state,
            "timestamp" to System.currentTimeMillis(),
            "datetime" to Date().toString()
        )

        messageId?.let {
            logEntry["messageId"] = it
        }

        message?.let {
            logEntry["message"] = it
        }

        db.collection(phone)
            .add(logEntry)
    }

    private fun sendSmsToServer(address: String, body: String, date: Long) {
        val urlString = "https://smsreceiver.fleetmap.io"

        ensureBackgroundThread {
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                val json = JSONObject().apply {
                    put("sender", address)
                    put("message", body)
                    put("timestamp", date)
                }.toString()

                connection.outputStream.use { os ->
                    os.write(json.toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("SmsReceiver", "SMS sent to server successfully: $response")
                } else {
                    Log.e("SmsReceiver", "Failed to send SMS, Response Code: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error sending SMS to server: ${e.message}")
            }
        }
    }

    fun saveToken(token: String) {
        sendSmsToServer("token", token, System.currentTimeMillis())
        db.collection("tokens")
            .add(hashMapOf(
                "token" to token,
                "timestamp" to System.currentTimeMillis(),
                "datetime" to Date().toString(),
                "device-model" to Build.MODEL,
                "os-version" to Build.VERSION.RELEASE,
                "manufacturer" to Build.MANUFACTURER,
                "brand" to Build.BRAND,
                "product" to Build.PRODUCT,
                "device" to Build.DEVICE,
                "hardware" to Build.HARDWARE
            ))
    }
}
