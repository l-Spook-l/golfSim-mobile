package com.example.golfsimmobile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
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

    private var isTracking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            val allPermissionsGranted = permissions.values.all { it }
            if (allPermissionsGranted) {
                Toast.makeText(requireContext(), "Все разрешения получены!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Некоторые разрешения не были предоставлены.", Toast.LENGTH_SHORT).show()
            }
        }

        getPermissions()

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
    }

    private fun getPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 101
            )
        }
    }
}
