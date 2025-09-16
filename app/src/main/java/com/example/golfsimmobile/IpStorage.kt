package com.example.golfsimmobile

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object IpStorage {
    private const val PREF_NAME = "AppPrefs"
    private const val KEY_SERVER_IP = "server_ip"


    private val _ipFlow = MutableStateFlow<String?>(null)
    val ipFlow: StateFlow<String?> get() = _ipFlow

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _ipFlow.value = prefs.getString(KEY_SERVER_IP, null)
    }

    fun saveIp(context: Context, ip: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_IP, ip).apply()
        _ipFlow.value = ip
    }

    fun getIp(): String? = _ipFlow.value

    suspend fun checkIpStatus(ip: String): Boolean = withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://$ip:7878/ping")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: IOException) {
            false
        }
    }
}
