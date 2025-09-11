package com.example.golfsimmobile

import android.app.Activity
import android.content.Context
import android.hardware.camera2.CameraDevice
import android.os.Handler
import android.util.Log
import android.view.TextureView
import android.widget.ImageView
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.util.ArrayList

/**
 * Handles ball detection and video recording using OpenCV.
 *
 * Responsibilities:
 * - Initializes the camera through [VideoRecorder].
 * - Fetches and updates HSV values for dynamic color detection.
 * - Processes camera frames to detect a yellow ball.
 * - Starts/stops video recording based on ball presence.
 * - Draws overlays (rectangles, masks) on detected objects.
 *
 * @property context application context
 * @property textureView surface for displaying camera frames
 * @property imageView overlay for processed frames (with contours)
 * @property onRecordingStopped callback invoked when recording is finished and preview should be restored
 */
class BallDetector(
    private val context: Context,
    private val textureView: TextureView,
    private val imageView: ImageView,
    private val onRecordingStopped: () -> Unit
) {
    private lateinit var videoRecorder: VideoRecorder
    private var isRecording = false
    private var wasRecording = false
    private var ballDetected = false
    private var ballDetectionStartTime: Long = 0
    private var ballDetectionStopTime: Long = 0
    private var hsvFetcher: HSVFetcher? = null
    private var lowerYellow = Scalar(0.0, 0.0, 0.0)  // Lower HSV bound for yellow
    private var upperYellow = Scalar(255.0, 255.0, 255.0)  // Upper bound of HSV range for yellow
    private var sensorOrientation: Int = 0
    private var frameCounter: Int = 0

    /**
     * Initializes the video recorder with camera parameters.
     *
     * @param cameraDevice active camera instance
     * @param handler background handler for camera operations
     * @param sensorOrientation orientation of the camera sensor
     */
    fun initCamera(cameraDevice: CameraDevice, handler: Handler, sensorOrientation: Int) {
        this.sensorOrientation = sensorOrientation
        videoRecorder = VideoRecorder(context, textureView, cameraDevice, handler)
        videoRecorder.setSensorOrientation(sensorOrientation)
    }

    // Initialize hsvVals
    private var hsvVals = mapOf(
        "hmin" to 0,
        "smin" to 0,
        "vmin" to 0,
        "hmax" to 255,
        "smax" to 255,
        "vmax" to 255
    )

    /**
     * Resets the detector state.
     *
     * Stops tracking and clears detection/recording flags.
     */
    fun reset() {
        isRecording = false
        wasRecording = false
        ballDetected = false
    }

    /**
     * Clears the overlay image view (removes drawn contours/masks).
     */
    fun clearOverlay() {
        if (context is Activity) {
            context.runOnUiThread {
                imageView.setImageBitmap(null)  // Reset the ImageView
            }
        }
    }

    /**
     * Updates current HSV values for color detection.
     *
     * @param newHsvVals map of HSV thresholds
     */
    private fun updateHsvValues(newHsvVals: Map<String, Int>) {
        Log.d("BallDetector", "New HSV values: $newHsvVals")
        if (hsvVals == newHsvVals) {
            Log.d("BallDetector", "HSV values did not change")
        } else {
            Log.d("BallDetector", "HSV values changed")
            hsvVals = newHsvVals
        }
    }

    /**
     * Starts periodic fetching of HSV values from a remote source.
     */
    fun startHsvFetching() {
        hsvFetcher = HSVFetcher("http://192.168.50.107:7878/get-hsv") { hsvMap ->
            updateHsvValues(hsvMap)
        }
        hsvFetcher?.startFetching()
    }

    /**
     * Stops fetching HSV values.
     */
    fun stopHsvFetching() {
        hsvFetcher?.stopFetching()
    }

    /**
     * Processes a single frame for ball detection.
     *
     * Steps:
     * - Converts the frame to HSV and applies thresholding.
     * - Creates a mask for yellow regions.
     * - Finds contours and checks their size to detect the ball.
     * - Draws rectangles around detected objects and updates the overlay.
     * - Starts/stops video recording depending on detection state.
     *
     * @param mode current mode of the app (e.g., `"game"`)
     */
    fun processFrame(mode: String) {
        frameCounter++

        if (frameCounter % 5 != 0) return

        if (!::videoRecorder.isInitialized) {
            Log.e("BallDetector", "VideoRecorder not initialized")
            return
        }

        val bitmap = textureView.bitmap ?: return

        // Convert Bitmap to Mat format for OpenCV processing
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convert image to HSV color space to extract yellow color
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2HSV)

        // Ranges for the color
        lowerYellow = Scalar(
            hsvVals["hmin"]!!.toDouble(),
            hsvVals["smin"]!!.toDouble(),
            hsvVals["vmin"]!!.toDouble()
        )
        upperYellow = Scalar(
            hsvVals["hmax"]!!.toDouble(),
            hsvVals["smax"]!!.toDouble(),
            hsvVals["vmax"]!!.toDouble()
        )

        // Create a mask to isolate yellow regions
        val mask = Mat()
        Core.inRange(mat, lowerYellow, upperYellow, mask)

        // New black background
        val blackMat = Mat.zeros(mat.size(), mat.type())
        // Apply the mask to the black background to keep only yellow objects
        mat.copyTo(blackMat, mask)
        // Find contours on the mask
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            mask,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        var ballDetectedInCurrentFrame = false
        // Handling the found contours
        for (contour in contours) {
            val boundingRect = Imgproc.boundingRect(contour)
            // Draw rectangle around the found object
            Imgproc.rectangle(blackMat, boundingRect, Scalar(255.0, 0.0, 0.0), 2)
            // If the contour area is sufficient, assume it is a ball
            if (boundingRect.width > 50 && boundingRect.height > 50) {
                ballDetectedInCurrentFrame = true
            }
        }

        // Convert back to Bitmap and refresh the ImageView
        Utils.matToBitmap(blackMat, bitmap)
        if (context is Activity) {
            context.runOnUiThread {
                imageView.setImageBitmap(bitmap)
            }
        }

        // Free resources
        mat.release()
        mask.release()
        hierarchy.release()
        blackMat.release()

        when (mode) {
            "game" -> {
                // Video recording logic
                if (ballDetectedInCurrentFrame) {
                    if (!ballDetected) {
                        ballDetectionStartTime = System.currentTimeMillis()
                        ballDetected = true
                    } else if (System.currentTimeMillis() - ballDetectionStartTime > 1500) {
                        if (!isRecording) {
                            videoRecorder.startRecording()
                            isRecording = true
                            wasRecording = true
                        }
                    }
                } else {
                    if (ballDetected) {
                        ballDetectionStopTime = System.currentTimeMillis()
                        ballDetected = false
                    } else if (System.currentTimeMillis() - ballDetectionStopTime > 1500) {
                        if (isRecording) {
                            videoRecorder.stopRecording()
                            videoRecorder.uploadVideo() // Load video after recording stops
                            isRecording = false

                            // Invoke callback to restore the preview
                            onRecordingStopped()
                            wasRecording = false
                        }
                    }
                }
            }
        }
    }
}