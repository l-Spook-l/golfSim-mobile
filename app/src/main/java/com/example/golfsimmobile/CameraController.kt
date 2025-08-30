package com.example.golfsimmobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import com.example.golfsimmobile.utils.showToast


class CameraController(
    private val context: Context,
    private val textureView: TextureView,
    private val previewSize: Size,
    private val handler: Handler,
    private val ballDetector: BallDetector,
) {
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraId: String = ""
    private var sensorOrientation: Int = 0

    fun openCamera() {
        if (!textureView.isAvailable) return

        try {
            cameraId = cameraManager.cameraIdList.first()
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                showToast(context,"Нет разрешения на камеру")
                return
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
                    ballDetector.initCamera(cameraDevice, handler, sensorOrientation)
                    startPreview()
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                }

                override fun onError(device: CameraDevice, error: Int) {
                    device.close()
                    showToast(context,"Ошибка камеры: $error")
                }
            }, handler)

        } catch (e: Exception) {
            e.printStackTrace()
            showToast(context,"Ошибка открытия камеры: ${e.message}")
        }
    }

    fun startPreview() {
        if (!::cameraDevice.isInitialized || !textureView.isAvailable) return

        val surfaceTexture = textureView.surfaceTexture ?: return
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        val previewSurface = Surface(surfaceTexture)

        try {
            captureRequestBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(previewSurface)

            cameraDevice.createCaptureSession(
                listOf(previewSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        captureSession.setRepeatingRequest(
                            captureRequestBuilder.build(),
                            null,
                            handler
                        )
                        Log.d("Camera", "Preview started")
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        showToast(context,"Не удалось настроить превью")
                    }
                },
                handler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            showToast(context,"Ошибка запуска превью: ${e.message}")
        }
    }
}