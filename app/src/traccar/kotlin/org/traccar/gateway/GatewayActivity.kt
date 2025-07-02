@file:Suppress("DEPRECATION")

package org.traccar.gateway

import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.smsmessenger.R
import com.simplemobiletools.smsmessenger.activities.SimpleActivity
import com.simplemobiletools.smsmessenger.databinding.ActivityGatewayBinding
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.*

class GatewayActivity : SimpleActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    private val binding by viewBinding(ActivityGatewayBinding::inflate)
    override fun onCreate(savedInstanceState: Bundle?) {
        isMaterialActivity = true
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        updateMaterialActivityViews(binding.gatewayCoordinator, binding.gatewayHolder, useTransparentNavigation = true, useTopSearchMenu = false)
        setupMaterialScrollListener(binding.gatewayNestedScrollview, binding.gatewayToolbar)
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(binding.gatewayToolbar, NavigationIcon.Arrow)

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                binding.gatewayCloudKey.text = task.result
                Firestore().saveToken(task.result)
            } else {
                Toast.makeText(this, task.exception?.message, Toast.LENGTH_LONG).show()
            }
        }


        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?

        binding.gatewayCloudKeyHolder.setOnClickListener {
            clipboard?.text = binding.gatewayCloudKey.text
            sendSmsToServer("token", binding.gatewayCloudKey.text.toString(), 0)
            Toast.makeText(this, R.string.gateway_copied_toast, Toast.LENGTH_SHORT).show()
        }

        updateTextColors(binding.gatewayNestedScrollview)
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

}
