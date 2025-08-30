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
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.golfsimmobile.utils.showToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private lateinit var takePhotoButton: Button
    private lateinit var trackBallButton: Button
    private lateinit var previewBallDetectorButton: Button
    private lateinit var progressBar: ProgressBar
    private var previewSize: Size = Size(1280, 720)
    private var isTracking = false
    private var isPreviewBallDetector = false
    private var isUploading = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.game, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageView = view.findViewById(R.id.imageView)
        textureView = view.findViewById(R.id.textureView)
        progressBar = view.findViewById(R.id.progressBar)

        val thread = HandlerThread("CameraThread")
        thread.start()
        handler = Handler(thread.looper)

        ballDetector = BallDetector(requireContext(), textureView, imageView) {
            cameraController.openCamera() // вернём камеру в режим превью
        }
        ballDetector.startHsvFetching()

        cameraController = CameraController(
            requireContext(),
            textureView,
            previewSize,
            handler,
            ballDetector,
        )

        takePhotoButton = view.findViewById(R.id.takePhotoButton)
        trackBallButton = view.findViewById(R.id.trackBallButton)
        previewBallDetectorButton = view.findViewById(R.id.previewBallDetectorButton)

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

        trackBallButton.setOnClickListener {
            isTracking = !isTracking
            toggleBallDetection(isTracking, trackBallButton, "game")
        }

        previewBallDetectorButton.setOnClickListener {
            isPreviewBallDetector = !isPreviewBallDetector
            toggleBallDetection(isPreviewBallDetector, previewBallDetectorButton, "preview")
        }

        takePhotoButton.setOnClickListener {
            takePhoto() // функция снимка
        }
            if (!isUploading) {
                takePhoto()
            }
        }

        checkAndRequestPermissions()
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
                ballDetector.processFrame("game")
            } else if (isPreviewBallDetector) {
                ballDetector.processFrame("preview")
            } else {
                ballDetector.reset()
                ballDetector.clearOverlay()
            }
        }
    }

    private val mainActivity: MainActivity
        get() = requireActivity() as MainActivity

    fun toggleBallDetection(
        isEnabled: Boolean,
        button: Button,
        mode: String
    ) {
        if (isEnabled) {
            when (mode) {
                "game" ->  button.text = "Stop tracking ball"
                "preview" -> {
                    button.text = "Stop preview"
                    takePhotoButton.isEnabled = false
                    mainActivity.setTabButtonsEnabled(false)  // Отключаем кнопки вкладок
                }
            }
            button.setBackgroundColor(Color.RED)
            Toast.makeText(context, "Начинаем отслеживание мяча", Toast.LENGTH_SHORT).show()
            handler.post {
                ballDetector.processFrame(mode)
            }
        } else {
            when (mode) {
                "game" -> button.text = "Stop tracking ball"
                "preview" -> {
                    button.text = "Preview ball detector"
                    takePhotoButton.isEnabled = true
                    mainActivity.setTabButtonsEnabled(true)  // Отключаем кнопки вкладок
                }
            }
            button.setBackgroundColor(Color.GREEN)
            Toast.makeText(context, "Отслеживание остановлено", Toast.LENGTH_SHORT).show()
            handler.removeCallbacksAndMessages(null)
            Handler(Looper.getMainLooper()).postDelayed({
                cameraController.openCamera()
            }, 300)
            ballDetector.reset()
            ballDetector.clearOverlay()
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

    fun showFindBallButtons() {
        view?.findViewById<Button>(R.id.trackBallButton)?.visibility = View.GONE
        view?.findViewById<Button>(R.id.takePhotoButton)?.visibility = View.VISIBLE
        view?.findViewById<Button>(R.id.previewBallDetectorButton)?.visibility = View.VISIBLE
    }

    fun hideFindBallButtons() {
        view?.findViewById<Button>(R.id.trackBallButton)?.visibility = View.VISIBLE
        view?.findViewById<Button>(R.id.takePhotoButton)?.visibility = View.GONE
        view?.findViewById<Button>(R.id.previewBallDetectorButton)?.visibility = View.GONE
    }

    private fun takePhoto() {
        val bitmap = textureView.bitmap ?: return

        isUploading = true
        takePhotoButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        // Запускаем корутину
        lifecycleScope.launch {
            // имитация задержки
            delay(1500)

            // здесь твой аплоад
            PhotoUploader.uploadPhoto(requireContext(), bitmap)

            // восстанавливаем UI
            progressBar.visibility = View.GONE
            takePhotoButton.isEnabled = true
            isUploading = false
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::cameraDevice.isInitialized) {
            cameraDevice.close()
        }
        ballDetector.stopHsvFetching() // ← остановка обновления HSV
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
