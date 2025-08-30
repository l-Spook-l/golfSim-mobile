package com.example.golfsimmobile

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.os.Handler
import android.util.Range
import android.view.Surface
import android.view.TextureView
import com.example.golfsimmobile.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoRecorder(
    private val context: Context,
    private val textureView: TextureView,
    private val cameraDevice: CameraDevice,
    private val handler: Handler
) {
    lateinit var mediaRecorder: MediaRecorder
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession
    private var isRecording = false
    private lateinit var videoFilePath: String

    companion object {
        private const val SERVER_URL = "http://192.168.50.107:7878/upload/"
    }

    private fun setupMediaRecorder() {
        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val videoFileName = "VIDEO_$timeStamp.mp4"
            val videoFile = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), videoFileName)
            videoFilePath = videoFile.absolutePath

            mediaRecorder.setOutputFile(videoFilePath)
            mediaRecorder.setVideoEncodingBitRate(10000000)
            mediaRecorder.setVideoSize(1280, 720)
            mediaRecorder.setVideoFrameRate(240)
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.prepare()

        } catch (e: IOException) {
            e.printStackTrace()
            showToast(context,"Setup MediaRecorder failed")
        }
    }

    fun startRecording() {
        try {
            if (::captureSession.isInitialized) {
                captureSession.close()
            }

            setupMediaRecorder()
            val recordingSurface = mediaRecorder.surface
            val previewSurface = Surface(textureView.surfaceTexture)

            playSound(context, R.raw.ding_sfx)

            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            captureRequestBuilder.addTarget(recordingSurface)
            captureRequestBuilder.addTarget(previewSurface)

            cameraDevice.createCaptureSession(listOf(recordingSurface, previewSurface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(240, 240))
                    captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, handler)
                    mediaRecorder.start()
                    isRecording = true
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    showToast(context,"Configuration failed")
                }
            }, handler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            showToast(context,"Start recording failed: ${e.message}")
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            showToast(context,"Start recording failed: ${e.message}")
        }
    }

    // после этого метода надо вызывать - startPreview()
    fun stopRecording() {
        try {
            playSound(context, R.raw.ding_sfx)

            mediaRecorder.stop()
            mediaRecorder.reset()
            mediaRecorder.release()  // Добавь эту строку!

            isRecording = false
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            showToast(context,"Stop recording failed: ${e.message}")
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            showToast(context,"Stop recording failed: ${e.message}")
        }
    }

//    fun uploadVideo(filePath: String) {
    fun uploadVideo() {
        val client = OkHttpClient()
        val file = File(videoFilePath)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("video/mp4".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build()

        // Вместо Thread запускаем корутину
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        showToast(context,"Upload successful")
                    } else {
                        showToast(context,"Upload failed: ${response.message}")
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast(context,"Upload failed: ${e.message}")
                }
            }
        }
    }

    fun playSound(context: Context, soundResourceId: Int) {
        val mediaPlayer = MediaPlayer.create(context, soundResourceId)
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
        mediaPlayer.start()
    }
}