package ru.sakhabooks.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import ru.sakhabooks.app.ui.book.BookScreen
import ru.sakhabooks.app.ui.catalog.CatalogScreen
import ru.sakhabooks.app.ui.catalog.SectionScreen
import ru.sakhabooks.app.ui.library.LibraryScreen
import ru.sakhabooks.app.ui.player.MiniPlayer
import ru.sakhabooks.app.ui.player.MiniPlayerViewModel
import ru.sakhabooks.app.ui.player.PlayerScreen
import ru.sakhabooks.app.ui.profile.ProfileScreen
import ru.sakhabooks.app.ui.speak.SpeakScreen

@Composable
fun AppRoot(
    openNowPlayingRequests: Flow<Unit> = emptyFlow(),
    miniPlayerViewModel: MiniPlayerViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val openBook: (String) -> Unit = { bookId ->
        navController.navigate(BookRoute(bookId)) { launchSingleTop = true }
    }
    val openTab: (TopLevelTab) -> Unit = { tab ->
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openPlayer = { openTab(TopLevelTab.Player) }

    // Нажали на уведомление плеера — открываем вкладку «Плеер»
    LaunchedEffect(openNowPlayingRequests) {
        openNowPlayingRequests.collect { openPlayer() }
    }

    // В книге — чистая читалка: ни меню, ни мини-плеера
    val isReading = currentDestination?.hasRoute(BookRoute::class) == true

    Scaffold(
        // Отступы от краёв экрана каждый экран делает сам (у читалки их нет совсем)
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isReading) Column {
                // Во вкладке «Плеер» мини-плеер не нужен — там большой
                if (currentDestination?.hasRoute(PlayerRoute::class) != true) {
                    MiniPlayer(onOpenPlayer = openPlayer, viewModel = miniPlayerViewModel)
                }
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        // Раздел каталога — тоже вкладка «Каталог»
                        val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true ||
                            (tab == TopLevelTab.Catalog && currentDestination?.hasRoute(SectionRoute::class) == true)
                        NavigationBarItem(
                            selected = selected,
                            onClick = { openTab(tab) },
                            // Только значок; название вкладки — для экранного диктора
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = CatalogRoute,
            modifier = Modifier.padding(padding),
        ) {
            composable<CatalogRoute> { CatalogScreen(onOpenSection = { navController.navigate(SectionRoute(it)) }) }
            composable<SectionRoute> { SectionScreen(onBack = { navController.popBackStack() }, onOpenBook = openBook) }
            composable<BookRoute> { BookScreen(onBack = { navController.popBackStack() }, onOpenPlayer = openPlayer) }
            composable<PlayerRoute> { PlayerScreen(onOpenBook = openBook) }
            composable<LibraryRoute> { LibraryScreen(onOpenBook = openBook) }
            composable<SpeakRoute> { SpeakScreen() }
            composable<ProfileRoute> { ProfileScreen() }
        }
    }
}
