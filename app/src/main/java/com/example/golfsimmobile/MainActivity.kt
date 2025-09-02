package com.example.golfsimmobile

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

/**
 * Главная активность приложения.
 *
 * Здесь настраиваются:
 * - системные панели (прозрачность, цвет иконок);
 * - кнопки навигации между режимами;
 * - вставка и управление [GameFragment].
 */
class MainActivity : AppCompatActivity() {
    private lateinit var gameTabButton: Button
    private lateinit var findBallTabButton: Button
    private lateinit var gameFragment: GameFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Делаем системные панели прозрачными
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        setContentView(R.layout.main_menu)

        // Не даём экрану гаснуть во время игры
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Получаем ссылки на кнопки
        gameTabButton = findViewById(R.id.btnGame)
        findBallTabButton = findViewById(R.id.btnFindBall)

        // Создаём и вставляем GameFragment один раз
        gameFragment = GameFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, gameFragment)
            .commit()

        // Навешиваем обработчики на кнопки переключения режимов
        gameTabButton.setOnClickListener {
            gameFragment.hideFindBallButtons()
        }
        findBallTabButton.setOnClickListener {
            gameFragment.showFindBallButtons()
        }
    }

    /**
     * Включает или выключает кнопки вкладок.
     *
     * @param enabled true — кнопки активны, false — заблокированы.
     */
    fun setTabButtonsEnabled(enabled: Boolean) {
        gameTabButton.isEnabled = enabled
        findBallTabButton.isEnabled = enabled
    }
}
