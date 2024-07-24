package org.traccar.gateway

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
            .addOnSuccessListener { documentReference ->
                // Log success if needed
            }
            .addOnFailureListener { e ->
                // Log failure if needed
            }
    }
}
