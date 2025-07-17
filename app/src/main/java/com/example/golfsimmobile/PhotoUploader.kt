package com.example.golfsimmobile

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException

object PhotoUploader {

    private const val SERVER_URL = "http://192.168.50.107:8000/upload/" // можно параметризовать

    fun uploadPhoto(context: Context, bitmap: Bitmap) {
        val client = OkHttpClient()

        val byteArrayOutputStream = ByteArrayOutputStream()
        // Сжимает Bitmap в JPEG с качеством 90% и записывает в ByteArrayOutputStream.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        // Преобразует байты изображения в RequestBody с MIME-типом image/jpeg.
        val imageBytes = byteArrayOutputStream.toByteArray()

        //Формирует multipart-запрос, содержащий изображение под именем photo.jpg и ключом "file".
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "photo.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()
        // Собирает сам HTTP POST-запрос на сервер
        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build()
        // Выполняет сетевой запрос в фоновом потоке, не блокируя основной UI.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                // Возвращает результат на главный поток для показа Toast.
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Upload failed: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                // Возвращает результат на главный поток для показа Toast.
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Upload error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
