package com.example.golfsimmobile

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

/**
 * Main activity of the application.
 *
 * Handles:
 * - Configuring system bars (transparency, icon colors).
 * - Setting up navigation buttons between modes.
 * - Inserting and managing the [GameFragment].
 *
 * @property gameTabButton button for switching to game mode
 * @property findBallTabButton button for switching to ball search mode
 * @property gameFragment fragment that handles game logic and UI
 */
class MainActivity : AppCompatActivity() {
    private lateinit var gameTabButton: Button
    private lateinit var findBallTabButton: Button
    private lateinit var gameFragment: GameFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set system bars to transparent
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        setContentView(R.layout.main_menu)

        // Prevent screen from turning off during gameplay
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Get references to buttons
        gameTabButton = findViewById(R.id.btnGame)
        findBallTabButton = findViewById(R.id.btnFindBall)

        // Create and insert GameFragment only once
        gameFragment = GameFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, gameFragment)
            .commit()

        // Set up handlers for mode switch buttons
        gameTabButton.setOnClickListener {
            gameFragment.hideFindBallButtons()
        }
        findBallTabButton.setOnClickListener {
            gameFragment.showFindBallButtons()
        }
    }

    /**
     * Enables or disables the tab buttons.
     *
     * Used to temporarily block navigation between modes,
     * e.g. during active gameplay or animations.
     *
     * @param enabled `true` to make buttons active, `false` to disable them
     */
    fun setTabButtonsEnabled(enabled: Boolean) {
        gameTabButton.isEnabled = enabled
        findBallTabButton.isEnabled = enabled
    }
}
