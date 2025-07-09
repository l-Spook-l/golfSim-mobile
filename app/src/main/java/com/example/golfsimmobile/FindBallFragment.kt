package com.example.golfsimmobile

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class FindBallFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.find_ball, container, false)

        val takePhotoButton = view.findViewById<Button>(R.id.takePhotoButton)
        val sendPhotoButton = view.findViewById<Button>(R.id.sendPhotoButton)

        takePhotoButton.setOnClickListener {
            Toast.makeText(context, "Сделано фото (заглушка)", Toast.LENGTH_SHORT).show()
            // TODO: Захват фото с камеры
        }

        sendPhotoButton.setOnClickListener {
            Toast.makeText(context, "Фото отправлено на ПК (заглушка)", Toast.LENGTH_SHORT).show()
            // TODO: Отправить фото через HTTP на ПК
        }

        return view
    }
}
