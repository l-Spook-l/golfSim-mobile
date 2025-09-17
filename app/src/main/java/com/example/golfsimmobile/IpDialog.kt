package com.example.golfsimmobile

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import kotlinx.coroutines.*

object IpDialog {
    /**
     * Displays a non-cancelable dialog prompting the user to enter a server IP address.
     *
     * Performs the following steps:
     * - Shows an EditText for manual IP input
     * - On confirmation, sends a network request to verify server availability
     * - If successful, saves the IP using [IpStorage.saveIp] and triggers [onSuccess]
     *
     * @param context host [Context], usually an Activity
     * @param onSuccess callback invoked when the IP is valid and saved
     */
    fun show(context: Context, onSuccess: () -> Unit) {
        val editText = EditText(context).apply {
            hint = "Enter IP (e.g., 192.168.50.107)"
        }

        val progressBar = ProgressBar(context).apply {
            visibility = View.GONE
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
            addView(editText)
            addView(progressBar)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("No Connection")
            .setMessage("Enter server IP:")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("Connect", null)
            .create()

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val ip = editText.text.toString().trim()
                if (ip.isNotEmpty()) {
                    progressBar.visibility = View.VISIBLE
                    button.isEnabled = false

                    CoroutineScope(Dispatchers.IO).launch {
                        val isAlive = IpStorage.checkIpStatus(ip)
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            button.isEnabled = true

                            if (isAlive) {
                                IpStorage.saveIp(context, ip)
                                Toast.makeText(context, "Connection successful", Toast.LENGTH_SHORT)
                                    .show()
                                dialog.dismiss()
                                onSuccess()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Server not available at $ip",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(context, "Enter IP", Toast.LENGTH_SHORT).show()
                }
            }
        }
        dialog.show()
    }
}
