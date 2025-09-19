package com.example.golfsimmobile

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.golfsimmobile.utils.showToast
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader

/**
 * Fragment responsible for game mode and ball tracking.
 *
 * Responsibilities:
 * - Initializes camera, [CameraController] and [BallDetector].
 * - Requests and manages camera/microphone/storage permissions.
 * - Provides UI for taking photos, ball tracking, and preview mode.
 * - Handles OpenCV initialization and frame processing.
 * - Manages overlay updates and recording state.
 *
 * @property cameraController manages camera lifecycle and preview
 * @property ballDetector detects ball in frames and handles recording
 * @property requestPermissionLauncher launcher for runtime permissions
 * @property textureView displays camera preview
 * @property imageView shows processed frames with overlays
 * @property handler background handler for camera operations
 * @property cameraManager system service for accessing camera
 * @property cameraDevice active camera instance
 * @property takePhotoButton button for taking a photo
 * @property trackBallButton button for starting/stopping ball tracking
 * @property previewBallDetectorButton button for previewing detector output
 * @property progressBar shows upload progress after taking a photo
 * @property previewSize resolution of the preview stream
 * @property isTracking indicates whether ball tracking is active
 * @property isPreviewBallDetector indicates whether preview mode is active
 * @property isUploading indicates whether a photo is being uploaded
 */
class GameFragment : Fragment() {
    private lateinit var cameraController: CameraController
    private lateinit var ballDetector: BallDetector
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var handler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var takePhotoButton: MaterialButton
    private lateinit var trackBallButton: MaterialButton
    private lateinit var previewBallDetectorButton: MaterialButton
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
            cameraController.openCamera() // Switch camera back to preview mode
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

        // OpenCV initialization
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed")
        } else {
            Log.d("OpenCV", "OpenCV initialization succeeded")
        }

        // Initialization of the permission handler
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.values.all { it }) {
                showToast(requireContext(), "Permissions granted")
                cameraController.openCamera()
            } else {
                showToast(requireContext(), "Not all permissions granted")
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
            if (!isUploading) {
                takePhoto()
            }
        }

        checkAndRequestPermissions()
    }

    /**
     * Listener for [TextureView] surface events.
     *
     * Handles preview configuration, frame updates and delegates frame processing
     * to the [BallDetector].
     */
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
            cameraController.openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean =
            false

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            // Logic for processing frames using OpenCV
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

    /**
     * Toggles ball detection or preview mode.
     *
     * Updates button state, UI, and starts/stops [BallDetector] accordingly.
     *
     * @param isEnabled whether the detection/preview is active
     * @param button reference to the toggled button
     * @param mode mode type ("game" or "preview")
     */
    private fun toggleBallDetection(
        isEnabled: Boolean,
        button: MaterialButton,
        mode: String
    ) {
        if (isEnabled) {
            when (mode) {
                "game" -> button.text = "Stop tracking ball"
                "preview" -> {
                    button.text = "Stop preview"
                    takePhotoButton.isEnabled = false
                    mainActivity.setTabButtonsEnabled(false)  // Disable tab buttons
                }
            }
            button.backgroundTintList = ColorStateList.valueOf(Color.RED)
            button.strokeColor = ColorStateList.valueOf(Color.TRANSPARENT)

            Toast.makeText(context, "Start tracking the ball", Toast.LENGTH_SHORT).show()
            handler.post {
                ballDetector.processFrame(mode)
            }
        } else {
            when (mode) {
                "game" -> button.text = "Start tracking ball"
                "preview" -> {
                    button.text = "Preview ball"
                    takePhotoButton.isEnabled = true
                    mainActivity.setTabButtonsEnabled(true)  // Enable the tab buttons
                }
            }
            button.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#99000000"))
            button.strokeColor = ColorStateList.valueOf(Color.parseColor("#80FFFFFF"))

            Toast.makeText(context, "Tracking has been stopped", Toast.LENGTH_SHORT).show()
            handler.removeCallbacksAndMessages(null)
            Handler(Looper.getMainLooper()).postDelayed({
                cameraController.openCamera()
            }, 300)
            ballDetector.reset()
            ballDetector.clearOverlay()
        }
    }

    /**
     * Checks if required permissions are granted and requests them if needed.
     *
     * Permissions:
     * - CAMERA
     * - RECORD_AUDIO
     * - WRITE_EXTERNAL_STORAGE
     */
    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(
                requireContext(),
                it
            ) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions)
        } else {
            if (textureView.isAvailable) {
                cameraController.openCamera()
            }
        }
    }

    /**
     * Shows buttons relevant for "Find Ball" mode.
     */
    fun showFindBallButtons() {
        view?.findViewById<MaterialButton>(R.id.trackBallButton)?.visibility = View.GONE
        view?.findViewById<MaterialButton>(R.id.takePhotoButton)?.visibility = View.VISIBLE
        view?.findViewById<MaterialButton>(R.id.previewBallDetectorButton)?.visibility =
            View.VISIBLE
    }

    /**
     * Hides "Find Ball" mode buttons and restores game UI.
     */
    fun hideFindBallButtons() {
        view?.findViewById<MaterialButton>(R.id.trackBallButton)?.visibility = View.VISIBLE
        view?.findViewById<MaterialButton>(R.id.takePhotoButton)?.visibility = View.GONE
        view?.findViewById<MaterialButton>(R.id.previewBallDetectorButton)?.visibility = View.GONE
    }

    /**
     * Captures the current frame as a photo and uploads it.
     *
     * Displays progress bar during upload and restores UI after completion.
     */
    private fun takePhoto() {
        val bitmap = textureView.bitmap ?: return

        isUploading = true
        takePhotoButton.isEnabled = false
        progressBar.visibility = View.VISIBLE

        // Start coroutine
        lifecycleScope.launch {
            delay(1500)
            PhotoUploader.uploadPhoto(requireContext(), bitmap)
            // Restore UI
            progressBar.visibility = View.GONE
            takePhotoButton.isEnabled = true
            isUploading = false
        }
    }

    /**
     * Configures preview transformation for the [TextureView] and [ImageView].
     *
     * Applies scaling, rotation, and centering to properly display the camera stream.
     *
     * @param viewWidth width of the TextureView
     * @param viewHeight height of the TextureView
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val rotation = requireActivity().windowManager.defaultDisplay.rotation
        val matrix = android.graphics.Matrix()
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f

        val previewWidth = previewSize.width.toFloat()
        val previewHeight = previewSize.height.toFloat()
        val viewRect = android.graphics.RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = android.graphics.RectF(0f, 0f, previewHeight, previewWidth)

        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())

        val scale = maxOf(
            viewHeight / previewHeight,
            viewWidth / previewWidth
        )

        when (rotation) {
            android.view.Surface.ROTATION_0 -> {}
            android.view.Surface.ROTATION_90,
            android.view.Surface.ROTATION_270 -> {
                matrix.setRectToRect(viewRect, bufferRect, android.graphics.Matrix.ScaleToFit.FILL)
                matrix.postScale(scale, scale, centerX, centerY)
                matrix.postRotate(90f * (rotation - 2), centerX, centerY)
            }

            android.view.Surface.ROTATION_180 -> {
                matrix.postRotate(180f, centerX, centerY)
            }
        }

        textureView.setTransform(matrix)
        imageView.imageMatrix = matrix
        imageView.scaleType = ImageView.ScaleType.MATRIX
    }


    override fun onPause() {
        super.onPause()
        if (this::cameraDevice.isInitialized) {
            cameraDevice.close()
        }
        ballDetector.stopHsvFetching() // Stop HSV updates
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
