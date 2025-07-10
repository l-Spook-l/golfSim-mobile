package com.example.golfsimmobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {
    // Объявляем кнопки
    private lateinit var gameTabButton: Button
    private lateinit var findBallTabButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Устанавливаем XML-макет активности (main_menu.xml)
        setContentView(R.layout.main_menu)

        // Получаем ссылки на кнопки по ID
        gameTabButton = findViewById(R.id.btnGame)
        findBallTabButton = findViewById(R.id.btnFindBall)

        // При первом запуске сразу загружаем GameFragment в контейнер
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, GameFragment())
            .commit()

        // Обработчик нажатия на кнопку "Game"
        gameTabButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, GameFragment())
                .commit()
        }

        // Обработчик нажатия на кнопку "Find Ball"
        findBallTabButton.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FindBallFragment())
                .commit()
        }
    }
}
