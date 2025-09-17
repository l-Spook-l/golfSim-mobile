package com.example.golfsimmobile

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Centralized storage for server IP address.
 *
 * Responsibilities:
 * - Persists IP address in SharedPreferences..
 * - Exposes current IP via [StateFlow] for reactive updates.
 * - Provides synchronous getter [getIp].
 * - Allows runtime change of server IP without app restart.
 * - Verifies server availability via [checkIpStatus].
 *
 * Usage flow:
 * 1. Call init once in Application or MainActivity to restore last saved IP.
 * 2. Use [saveIp] to persist a new IP and notify observers.
 * 3. Access the current IP with [getIp] or observe [ipFlow].
 * 4. Call [checkIpStatus] to test if server at given IP responds to `/ping`.
 */
object IpStorage {
    private const val PREF_NAME = "AppPrefs"
    private const val KEY_SERVER_IP = "server_ip"

    /** Backing flow for server IP. Emits `null` if not set. */
    private val _ipFlow = MutableStateFlow<String?>(null)

    /** Public read-only [StateFlow] for observing current server IP. */
    val ipFlow: StateFlow<String?> get() = _ipFlow

    /**
     * Initializes [IpStorage] by restoring saved IP from SharedPreferences.
     *
     * @param context any valid [Context]
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        _ipFlow.value = prefs.getString(KEY_SERVER_IP, null)
    }

    /**
     * Saves a new server IP and updates [ipFlow].
     *
     * @param context any valid [Context]
     * @param ip IPv4 string, e.g. `"192.168.0.100"`
     */
    fun saveIp(context: Context, ip: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_IP, ip).apply()
        _ipFlow.value = ip
    }

    /**
     * Returns current server IP synchronously.
     *
     * @return current IP string or `null` if not set
     */
    fun getIp(): String? = _ipFlow.value

    /**
     * Checks availability of server at given IP.
     *
     * Performs HTTP GET request to `http://<ip>:7878/ping`.
     *
     * @param ip IP string to test
     * @return `true` if server responds with HTTP 200, otherwise `false`
     */
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
