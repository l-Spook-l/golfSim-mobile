package com.example.golfsimmobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.golfsimmobile.utils.showToast
import org.opencv.android.OpenCVLoader

class GameFragment : Fragment() {
    private lateinit var cameraController: CameraController
    private lateinit var ballDetector: BallDetector
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var handler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private var previewSize: Size = Size(1280, 720)
    private var isTracking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.game, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.imageView)
        textureView = view.findViewById(R.id.textureView)

        val thread = HandlerThread("CameraThread")
        thread.start()
        handler = Handler(thread.looper)

        ballDetector = BallDetector(requireContext(), textureView, imageView) {
            cameraController.openCamera() // вернём камеру в режим превью
        }

        cameraController = CameraController(
            requireContext(),
            textureView,
            previewSize,
            handler,
            ballDetector,
        )

        val trackButton = view.findViewById<Button>(R.id.trackBallButton)  // изменить назв кнопки
        val takePhotoButton = view.findViewById<Button>(R.id.takePhotoButton)

        // Инициализация обработчика разрешений
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                showToast(requireContext(),"Разрешения предоставлены")
                cameraController.openCamera()
            } else {
                showToast(requireContext(),"Не все разрешения предоставлены")
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
                // 👇Запускаем тяжелую задачу в фоновом потоке
                handler.post {
                    ballDetector.processFrame()
                }
            } else {
                trackButton.text = "Start tracking ball"
                trackButton.setBackgroundColor(Color.GREEN)
                Toast.makeText(context, "Отслеживание остановлено", Toast.LENGTH_SHORT).show()
//                isTracking = true
                handler.removeCallbacksAndMessages(null)
                // Перезапустить превью
                Handler(Looper.getMainLooper()).postDelayed({
                    cameraController.openCamera()
                }, 300)
                ballDetector.reset()
                ballDetector.clearOverlay()
            }
        }
        checkAndRequestPermissions()

        takePhotoButton.setOnClickListener {
            takePhoto() // функция снимка
        }
    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            cameraController.openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true  // у меня false

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // OpenCV логика обработки кадра — вставляется сюда
            if (isTracking) {
                ballDetector.processFrame()
            } else {
                ballDetector.reset()
                ballDetector.clearOverlay()
            }
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
                cameraController.openCamera()
            }
        }
    }

    private fun takePhoto() {
        val bitmap = textureView.bitmap ?: return
        PhotoUploader.uploadPhoto(requireContext(), bitmap)
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
            cameraController.openCamera()
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }
}
