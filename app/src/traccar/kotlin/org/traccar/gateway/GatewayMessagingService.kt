package org.traccar.gateway

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class GatewayMessagingService : FirebaseMessagingService() {

    private val handler = Handler(Looper.getMainLooper())
    private val SENT_ACTION = "SMS_SENT"
    private val DELIVERY_ACTION = "SMS_DELIVERED"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val phone = remoteMessage.data["phone"]
        val message = remoteMessage.data["message"]
        val messageId = remoteMessage.messageId

        if (phone != null && message != null) {
            val sentIntent = Intent(SENT_ACTION).apply {
                putExtra("phone", phone)
                putExtra("messageId", messageId)
            }
            val deliveryIntent = Intent(DELIVERY_ACTION).apply {
                putExtra("phone", phone)
                putExtra("messageId", messageId)
            }

            try {
                this.getSystemService(SmsManager::class.java).sendTextMessage(phone, null, message,
                    PendingIntent.getBroadcast(this, 0, sentIntent, PendingIntent.FLAG_IMMUTABLE),
                    PendingIntent.getBroadcast(this, 0, deliveryIntent, PendingIntent.FLAG_IMMUTABLE)
                )
                Firestore().log(phone, messageId, "SMS Sending", message)
            } catch (e: Exception) {
                handler.post {
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Register BroadcastReceiver for SENT_ACTION
        ContextCompat.registerReceiver(this, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val phone = intent?.getStringExtra("phone")
                val messageId = intent?.getStringExtra("messageId")
                if (phone != null) {
                    if (resultCode == RESULT_OK) {
                        onSmsSent(phone, messageId)
                    } else {
                        onSmsFailed(phone, messageId)
                    }
                }
            }
        }, IntentFilter(SENT_ACTION), ContextCompat.RECEIVER_EXPORTED)

        // Register BroadcastReceiver for DELIVERY_ACTION
        ContextCompat.registerReceiver(this, object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val phone = intent?.getStringExtra("phone")
                val messageId = intent?.getStringExtra("messageId")
                if (phone != null) {
                    when (resultCode) {
                        RESULT_OK -> onSmsDelivered(phone, messageId)
                        else -> onSmsDeliveryFailed(phone, messageId)
                    }
                }
            }
        }, IntentFilter(DELIVERY_ACTION), ContextCompat.RECEIVER_EXPORTED)
    }

    private fun onSmsSent(phone: String, messageId: String?) {
        Firestore().log(phone, messageId, "SMS Sent")
    }

    private fun onSmsFailed(phone: String, messageId: String?) {
        Firestore().log(phone, messageId, "SMS Failed")
    }

    private fun onSmsDelivered(phone: String, messageId: String?) {
        Firestore().log(phone, messageId, "SMS Delivered")
    }

    private fun onSmsDeliveryFailed(phone: String, messageId: String?) {
        Firestore().log(phone, messageId, "SMS Delivery Failed")
    }
}
