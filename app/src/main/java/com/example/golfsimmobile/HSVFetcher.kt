package com.example.golfsimmobile

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


/**
 * Periodically fetches HSV values from a remote source via HTTP.
 *
 * Responsibilities:
 * - Sends GET requests to the provided URL.
 * - Parses JSON responses into HSV thresholds.
 * - Invokes [onUpdate] callback with updated HSV values.
 * - Runs periodically on the main thread using [Handler].
 *
 * Expected JSON format:
 * ```json
 * {
 *   "hsv_vals": {
 *     "hue_min": 0,
 *     "hue_max": 255,
 *     "saturation_min": 0,
 *     "saturation_max": 255,
 *     "value_min": 0,
 *     "value_max": 255
 *   }
 * }
 * ```
 *
 * @property onUpdate callback triggered when new HSV values are received
 */
class HSVFetcher(
    private val onUpdate: (Map<String, Int>) -> Unit
) {
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val fetchRunnable = object : Runnable {
        override fun run() {
            fetchHSV()
            if (isRunning) {
                handler.postDelayed(this, 5000)
            }
        }
    }

    /**
     * Starts periodic fetching of HSV values.
     *
     * Requests are sent every 5 seconds until [stopFetching] is called.
     */
    fun startFetching() {
        if (!isRunning) {
            isRunning = true
            handler.post(fetchRunnable)
        }
    }

    /**
     * Stops periodic fetching of HSV values.
     */
    fun stopFetching() {
        isRunning = false
        handler.removeCallbacks(fetchRunnable)
    }

    /**
     * Sends an HTTP request to fetch HSV values and parses the response.
     *
     * Invokes [onUpdate] if parsing succeeds.
     */
    private fun fetchHSV() {
        val ip = IpStorage.getIp()
        if (ip.isNullOrEmpty()) {
            Log.e("HSVFetcher", "IP is not set")
            return
        }

        val request = Request.Builder()
            .url("http://$ip:7878/get-hsv") // dynamically generate the URL
            .build()

        Log.d("HSVFetcher", "Sending request to URL: $ip")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HSVFetcher", "HSV request error: ${e.message}")
                Log.e("HSVFetcher", "Request failed with exception: ${e.localizedMessage}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e("HSVFetcher", "Response was not successful: ${response.code}")
                    return
                }

                Log.d("HSVFetcher", "Response received with code: ${response.code}")

                response.body?.string()?.let { json ->
                    try {
                        Log.d("HSVFetcher", "Parsing response JSON")
                        val jsonObj = JSONObject(json)
                        Log.d("HSVFetcher", "Successfully parsed JSON")
                        val hsv = jsonObj.getJSONObject("hsv_vals")
                        val hsvMap = mapOf(
                            "hmin" to hsv.getInt("hue_min"),
                            "hmax" to hsv.getInt("hue_max"),
                            "smin" to hsv.getInt("saturation_min"),
                            "smax" to hsv.getInt("saturation_max"),
                            "vmin" to hsv.getInt("value_min"),
                            "vmax" to hsv.getInt("value_max")
                        )
                        Log.d("HSVFetcher", "HSV values parsed: $hsvMap")
                        onUpdate(hsvMap)
                    } catch (e: Exception) {
                        Log.e("HSVFetcher", "HSV parsing error: ${e.message}")
                        Log.e("HSVFetcher", "Error parsing JSON: ${e.localizedMessage}")
                    }
                }
            }
        })
    }
}