package com.example.golfsimmobile

import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.media.MediaPlayer
import android.media.MediaRecorder
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

/**
 * Handles video recording and uploading.
 *
 * Responsibilities:
 * - Configures and controls [MediaRecorder].
 * - Records video with camera preview.
 * - Plays sound cues at recording start/stop.
 * - Uploads recorded video to a server.
 *
 * Usage flow:
 * 1. Call [startRecording] to begin capturing video.
 * 2. Call [stopRecording] to finish recording.
 * 3. Optionally call [uploadVideo] to send the file to the server.
 *
 * @param context application [Context] for UI and media operations
 * @param textureView surface for preview display
 * @param cameraDevice active [CameraDevice] for capture
 * @param handler background [Handler] for camera operations
 */
class VideoRecorder(
    private val context: Context,
    private val textureView: TextureView,
    private val cameraDevice: CameraDevice,
    private val handler: Handler
) {
    /** Android system recorder for audio/video. */
    lateinit var mediaRecorder: MediaRecorder
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession
    private var isRecording = false
    private lateinit var videoFilePath: String
    private var sensorOrientation: Int = 0

    /**
     * Get the current server address on the fly.
     * If the IP is not set → return null.
     */
    private val serverUrl: String?
        get() {
            val ip = IpStorage.getIp() ?: return null
            return "http://$ip:7878/upload/"
        }

    /**
     * Prepares [MediaRecorder] for recording:
     * - Configures sources and encoders.
     * - Sets file output path with timestamp.
     * - Applies orientation based on device rotation.
     */
    private fun setupMediaRecorder() {
        try {
            mediaRecorder = MediaRecorder()
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            // Add orientation
            val windowManager = (context as Activity).windowManager
            val rotation = windowManager.defaultDisplay.rotation

            val orientation = when (rotation) {
                Surface.ROTATION_0 -> 90
                Surface.ROTATION_90 -> 0
                Surface.ROTATION_180 -> 270
                Surface.ROTATION_270 -> 180
                else -> 0
            }
            mediaRecorder.setOrientationHint(orientation)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val videoFileName = "VIDEO_$timeStamp.mp4"
            val videoFile = File(context.cacheDir, videoFileName) // save video to cache

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

    /**
     * Starts video recording:
     * - Closes previous capture session if exists.
     * - Sets up [MediaRecorder] and camera session.
     * - Plays start sound cue.
     * - Begins recording to file.
     */
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

    /**
     * Stops recording:
     * - Stops and releases [MediaRecorder].
     * - Plays stop sound cue.
     * - Marks recording as inactive.
     *
     * Must call [startRecording] again before new recording.
     */
    fun stopRecording() {
        try {
            playSound(context, R.raw.ding_sfx)

            mediaRecorder.stop()
            mediaRecorder.reset()
            mediaRecorder.release()

            isRecording = false
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            showToast(context,"Stop recording failed: ${e.message}")
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            showToast(context,"Stop recording failed: ${e.message}")
        }
    }

    /**
     * Uploads the last recorded video file to the server.
     *
     * - Uses OkHttp for HTTP POST request.
     * - Wraps video file in multipart/form-data request.
     * - Displays upload result as a toast.
     */
    fun uploadVideo() {
        val url = serverUrl ?: run {
            showToast(context, "The server's IP address is not set")
            return
        }

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
            .url(url)
            .post(requestBody)
            .build()

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
            } finally {
                file.delete()  // always delete temporary file
            }
        }
    }

    /**
     * Sets the orientation of the camera sensor.
     *
     * @param orientation sensor orientation in degrees
     */
    fun setSensorOrientation(orientation: Int) {
        sensorOrientation = orientation
    }

    /**
     * Plays a short sound from resources.
     *
     * @param context application [Context]
     * @param soundResourceId raw resource ID of the sound file
     */
    private fun playSound(context: Context, soundResourceId: Int) {
        val mediaPlayer = MediaPlayer.create(context, soundResourceId)
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
        mediaPlayer.start()
    }
}