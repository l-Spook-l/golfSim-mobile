package com.example.golfsimmobile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {
    // Объявляем кнопки
    private lateinit var btnGameTab: Button
    private lateinit var btnFindBallTab: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Устанавливаем XML-макет активности (main_menu.xml)
        setContentView(R.layout.main_menu)

        // Получаем ссылки на кнопки по ID
        btnGameTab = findViewById(R.id.btnGameTab)
        btnFindBallTab = findViewById(R.id.btnFindBallTab)

        // При первом запуске сразу загружаем GameFragment в контейнер
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, GameFragment())
            .commit()

        // Обработчик нажатия на кнопку "Game"
        btnGameTab.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, GameFragment())
                .commit()
        }

        // Обработчик нажатия на кнопку "Find Ball"
        btnFindBallTab.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, FindBallFragment())
                .commit()
        }
    }
}
