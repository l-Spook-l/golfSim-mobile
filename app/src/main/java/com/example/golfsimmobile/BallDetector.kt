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

class BallDetector(
    private val context: Context,
    private val textureView: TextureView,
    private val imageView: ImageView,
    private val onRecordingStopped: () -> Unit // колбэк
) {
    private lateinit var videoRecorder: VideoRecorder
    private var isRecording = false
    private var wasRecording = false
    private var ballDetected = false
    private var ballDetectionStartTime: Long = 0
    private var ballDetectionStopTime: Long = 0
    private var hsvFetcher: HSVFetcher? = null

    fun initCamera(cameraDevice: CameraDevice, handler: Handler) {
        videoRecorder = VideoRecorder(context, textureView, cameraDevice, handler)
    }

    // Инициализация hsvVals
    private var hsvVals = mapOf(
        "hmin" to 0,
        "smin" to 0,
        "vmin" to 0,
        "hmax" to 255,
        "smax" to 255,
        "vmax" to 255
    )

    fun reset() {
        isRecording = false
        wasRecording = false
        ballDetected = false
    }

    fun clearOverlay() {
        if (context is Activity) {
            context.runOnUiThread {
                imageView.setImageBitmap(null)  // Очистим ImageView
            }
        }
    }

    fun updateHsvValues(newHsvVals: Map<String, Int>) {
        Log.d("BallDetector", "Новые HSV значения: $newHsvVals")
        if (hsvVals == newHsvVals) {
            Log.d("BallDetector", "HSV значения не изменились")
        } else {
            Log.d("BallDetector", "HSV значения изменились")
            hsvVals = newHsvVals
        }
    }

    fun startHsvFetching() {
        hsvFetcher = HSVFetcher("http://192.168.50.107:8000/get-hsv") { hsvMap ->
            updateHsvValues(hsvMap)
        }
        hsvFetcher?.startFetching()
    }

    fun stopHsvFetching() {
        hsvFetcher?.stopFetching()
    }

    fun processFrame(mode: String){
        if (!::videoRecorder.isInitialized) {
            Log.e("BallDetector", "VideoRecorder not initialized")
            return
        }

        val bitmap = textureView.bitmap ?: return
        // Преобразуем Bitmap в Mat для обработки с помощью OpenCV
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Преобразуем изображение в HSV для выделения желтого цвета
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2HSV)

        // Диапазоны для цвета
        val lowerYellow = Scalar(hsvVals["hmin"]!!.toDouble(), hsvVals["smin"]!!.toDouble(), hsvVals["vmin"]!!.toDouble())
        val upperYellow = Scalar(hsvVals["hmax"]!!.toDouble(), hsvVals["smax"]!!.toDouble(), hsvVals["vmax"]!!.toDouble())

        val mask = Mat()

        // Создаем маску, которая выделяет желтые области
        Core.inRange(mat, lowerYellow, upperYellow, mask)

        // Новый черный фон
        val blackMat = Mat.zeros(mat.size(), mat.type())

        // Накладываем маску на черный фон, чтобы оставить только желтые объекты
        mat.copyTo(blackMat, mask)

        // Поиск контуров на маске
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        var ballDetectedInCurrentFrame = false

        // Обработка найденных контуров
        for (contour in contours) {
            val boundingRect = Imgproc.boundingRect(contour)
            // Рисуем прямоугольник вокруг найденного объекта
            Imgproc.rectangle(blackMat, boundingRect, Scalar(255.0, 0.0, 0.0), 2)
            // Если область достаточна по размеру, считаем, что это мяч
            if (boundingRect.width > 50 && boundingRect.height > 50) {
                ballDetectedInCurrentFrame = true
            }
        }

        // Конвертируем обратно в Bitmap и обновляем ImageView
        Utils.matToBitmap(blackMat, bitmap)
        if (context is Activity) {
            context.runOnUiThread {
                imageView.setImageBitmap(bitmap)
            }
        }

        // Освобождаем ресурсы
        mat.release()
        mask.release()
        hierarchy.release()
        blackMat.release()

        when (mode) {
            "game" -> {
                // Логика для записи видео
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
                            videoRecorder.uploadVideo() // Загружаем видео после остановки записи
                            isRecording = false

                            // 💡 Вызов колбэка для восстановления превью
                            onRecordingStopped()
                            wasRecording = false
                        }
                    }
                }
            }
        }
    }
}