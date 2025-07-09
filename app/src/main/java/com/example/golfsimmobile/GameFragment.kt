package com.example.golfsimmobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class GameFragment : Fragment() {

    private var isTracking = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.game, container, false)

        val trackButton = view.findViewById<Button>(R.id.trackBallButton)

        trackButton.setOnClickListener {
            isTracking = !isTracking
            if (isTracking) {
                trackButton.text = "Остановить отслеживание"
                Toast.makeText(context, "Начинаем отслеживание мяча", Toast.LENGTH_SHORT).show()
                // TODO: Запустить отслеживание
            } else {
                trackButton.text = "Отслеживать мяч"
                Toast.makeText(context, "Отслеживание остановлено", Toast.LENGTH_SHORT).show()
                // TODO: Остановить отслеживание
            }
        }

        return view
    }
}
