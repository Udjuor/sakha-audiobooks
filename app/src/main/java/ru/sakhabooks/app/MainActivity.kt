package ru.sakhabooks.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import ru.sakhabooks.app.ui.navigation.AppRoot
import ru.sakhabooks.app.ui.theme.SakhaBooksTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        /** Признак «открыть экран играющей книги» — приходит при нажатии на уведомление плеера. */
        const val EXTRA_OPEN_NOW_PLAYING = "open_now_playing"
    }

    private val openNowPlayingRequests = Channel<Unit>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            SakhaBooksTheme {
                AppRoot(openNowPlayingRequests = openNowPlayingRequests.receiveAsFlow())
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_NOW_PLAYING, false) == true) {
            openNowPlayingRequests.trySend(Unit)
        }
    }
}
