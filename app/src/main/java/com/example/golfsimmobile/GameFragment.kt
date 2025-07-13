package com.example.golfsimmobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import org.opencv.android.OpenCVLoader

class GameFragment : Fragment() {
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var handler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var captureSession: CameraCaptureSession
    private lateinit var mediaRecorder: MediaRecorder
    private var previewSize: Size = Size(1280, 720)

    private var isRecording = false
    private var ballDetected = false
    private var ballDetectionStartTime = 0L
    private var ballDetectionStopTime = 0L
    private lateinit var bitmap: Bitmap
    private lateinit var videoFilePath: String

    private var isTracking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.game, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textureView = view.findViewById(R.id.textureView)
        val trackButton = view.findViewById<Button>(R.id.recordButton)  // изменить назв кнопки
        imageView = view.findViewById(R.id.imageView)
        textureView = view.findViewById(R.id.textureView)

        val thread = HandlerThread("CameraThread")
        thread.start()
        handler = Handler(thread.looper)

        // Инициализация обработчика разрешений
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                showToast("Разрешения предоставлены")
                openCamera()
            } else {
                showToast("Не все разрешения предоставлены")
            }
        }

        cameraManager = requireContext().getSystemService(Context.CAMERA_SERVICE) as CameraManager

        textureView.surfaceTextureListener = surfaceTextureListener

        trackButton.setOnClickListener {
            isTracking = !isTracking
            if (isTracking) {
                trackButton.text = "Stop tracking ball"
                trackButton.setBackgroundColor(Color.RED)
                Toast.makeText(context, "Начинаем отслеживание мяча", Toast.LENGTH_SHORT).show()
                // TODO: Запустить отслеживание
            } else {
                trackButton.text = "Start tracking ball"
                trackButton.setBackgroundColor(Color.GREEN)
                Toast.makeText(context, "Отслеживание остановлено", Toast.LENGTH_SHORT).show()
                // TODO: Остановить отслеживание
            }
        }
        checkAndRequestPermissions()
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true  // у меня false

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // OpenCV логика обработки кадра — вставляется сюда
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions)
        } else {
            if (textureView.isAvailable) {
                openCamera()
            }
        }
    }

    private fun openCamera() {
        // Убеждаемся, что Fragment действительно отображается
        if (!isVisible || !textureView.isAvailable) return

        try {
            val cameraId = cameraManager.cameraIdList.first()
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                showToast("Нет разрешения на камеру")
                return
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(device: CameraDevice) {
                    cameraDevice = device
                    val surfaceTexture = textureView.surfaceTexture ?: return
                    surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
                    val previewSurface = Surface(surfaceTexture)

                    captureRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    captureRequestBuilder.addTarget(previewSurface)

                    Log.d("Camera", "Camera opened successfully 1")

                    cameraDevice.createCaptureSession(listOf(previewSurface), object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            captureSession = session
                            captureSession.setRepeatingRequest(captureRequestBuilder.build(), null, handler)
                            Log.d("Camera", "Camera opened successfully 2")
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {
                            showToast("Не удалось настроить камеру")
                        }
                    }, handler)
                }

                override fun onDisconnected(device: CameraDevice) {
                    device.close()
                }

                override fun onError(device: CameraDevice, error: Int) {
                    device.close()
                    showToast("Ошибка камеры: $error")
                }
            }, handler)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Ошибка открытия камеры: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::cameraDevice.isInitialized) {
            cameraDevice.close()
        }
    }

    override fun onResume() {
        super.onResume()
        if (textureView.isAvailable) {
            openCamera()
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }
    private fun showToast(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }
}
