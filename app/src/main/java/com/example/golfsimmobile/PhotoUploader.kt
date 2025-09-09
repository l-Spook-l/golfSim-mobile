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

/**
 * Handles uploading photos to the server.
 *
 * Responsibilities:
 * - Compresses [Bitmap] into JPEG format.
 * - Wraps the image into a multipart HTTP request.
 * - Sends the request asynchronously using OkHttp.
 * - Provides upload result feedback via [Toast].
 *
 * Example usage:
 * ```kotlin
 * val bitmap: Bitmap = textureView.bitmap!!
 * PhotoUploader.uploadPhoto(context, bitmap)
 * ```
 */
object PhotoUploader {
    /** Server endpoint for image uploads. */
    private const val SERVER_URL = "http://192.168.50.107:7878/upload/"

    /**
     * Uploads a [bitmap] image to the server asynchronously.
     *
     * - Compresses the bitmap to JPEG (90% quality).
     * - Sends a multipart/form-data POST request.
     * - Shows a [Toast] message upon success or failure.
     *
     * @param context application context used for showing [Toast]
     * @param bitmap the image to upload
     */
    fun uploadPhoto(context: Context, bitmap: Bitmap) {
        val client = OkHttpClient()
        val byteArrayOutputStream = ByteArrayOutputStream()

        // Compresses Bitmap to JPEG with 90% quality and writes to ByteArrayOutputStream.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)

        // Converts image bytes into a RequestBody with MIME type image/jpeg.
        val imageBytes = byteArrayOutputStream.toByteArray()

        // Creates a multipart request containing the image named photo.jpg with the key "file".
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "photo.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        // Builds the actual HTTP POST request to the server
        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build()

        // Execute request on IO thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                // Returns the result to the main thread to show a Toast.
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Upload failed: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                // Returns the result to the main thread to display a Toast.
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Upload error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}