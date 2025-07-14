package com.example.golfsimmobile.utils  // ✅ важно

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

fun showToast(context: Context, message: String) {
    Handler(Looper.getMainLooper()).post {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}