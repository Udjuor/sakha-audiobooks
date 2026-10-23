package ru.sakhabooks.app.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import ru.sakhabooks.app.ui.book.BookScreen
import ru.sakhabooks.app.ui.catalog.CatalogScreen
import ru.sakhabooks.app.ui.library.LibraryScreen
import ru.sakhabooks.app.ui.player.MiniPlayer
import ru.sakhabooks.app.ui.player.MiniPlayerViewModel
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

    // Нажали на уведомление плеера — открываем экран играющей книги
    LaunchedEffect(openNowPlayingRequests) {
        openNowPlayingRequests.collect {
            val playing = withTimeoutOrNull(3_000) { miniPlayerViewModel.state.filterNotNull().first() }
            if (playing != null) openBook(playing.book.id)
        }
    }

    Scaffold(
        bottomBar = {
            Column {
                // На экране книги свой большой плеер — мини-плеер там не нужен
                if (currentDestination?.hasRoute(BookRoute::class) != true) {
                    MiniPlayer(onOpenBook = openBook, viewModel = miniPlayerViewModel)
                }
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
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
            composable<CatalogRoute> { CatalogScreen(onOpenBook = openBook) }
            composable<BookRoute> { BookScreen(onBack = { navController.popBackStack() }) }
            composable<LibraryRoute> { LibraryScreen() }
            composable<SpeakRoute> { SpeakScreen() }
            composable<ProfileRoute> { ProfileScreen() }
        }
    }
}
