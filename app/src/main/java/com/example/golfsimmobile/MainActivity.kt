package com.example.golfsimmobile

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class MainActivity : AppCompatActivity() {
    // Объявляем кнопки
    private lateinit var gameTabButton: Button
    private lateinit var findBallTabButton: Button
    private lateinit var gameFragment: GameFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Не гасить экран
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Устанавливаем XML-макет активности (main_menu.xml)
        setContentView(R.layout.main_menu)

        // Получаем ссылки на кнопки по ID
        gameTabButton = findViewById(R.id.btnGame)
        findBallTabButton = findViewById(R.id.btnFindBall)

        // Создаём и вставляем GameFragment один раз
        gameFragment = GameFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, gameFragment)
            .commit()

        // Нажата "Игра" — скрыть кнопки поиска мяча
        gameTabButton.setOnClickListener {
            gameFragment.hideFindBallButtons()
        }

        // Нажата "Найти мяч" — показать кнопки поиска мяча
        findBallTabButton.setOnClickListener {
            gameFragment.showFindBallButtons()
        }
    }
}
