package com.example.golfsimmobile

import android.os.Handler
import android.os.Looper
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class HSVFetcher(
    private val url: String,
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

    fun startFetching() {
        if (!isRunning) {
            isRunning = true
            handler.post(fetchRunnable)
        }
    }

    fun stopFetching() {
        isRunning = false
        handler.removeCallbacks(fetchRunnable)
    }

    private fun fetchHSV() {
        val request = Request.Builder().url(url).build()
        Log.d("HSVFetcher", "Sending request to URL: $url")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HSVFetcher", "Ошибка запроса HSV: ${e.message}")
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
                        Log.e("HSVFetcher", "Ошибка парсинга HSV: ${e.message}")
                        Log.e("HSVFetcher", "Error parsing JSON: ${e.localizedMessage}")
                    }
                }
            }
        })
    }
}