package ru.sakhabooks.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import ru.sakhabooks.app.ui.navigation.AppRoot
import ru.sakhabooks.app.ui.theme.SakhaBooksTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        /** Признак «открыть экран играющей книги» — приходит при нажатии на уведомление плеера. */
        const val EXTRA_OPEN_NOW_PLAYING = "open_now_playing"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SakhaBooksTheme {
                AppRoot()
            }
        }
    }
}
