package org.traccar.gateway

import android.os.Build
import com.google.firebase.firestore.FirebaseFirestore
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

    fun saveToken(token: String) {
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
