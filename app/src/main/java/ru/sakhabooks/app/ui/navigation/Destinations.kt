package ru.sakhabooks.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import ru.sakhabooks.app.R

// Адреса экранов.
@Serializable data object CatalogRoute
@Serializable data object LibraryRoute
@Serializable data object PlayerRoute
@Serializable data object SpeakRoute
@Serializable data object ProfileRoute
@Serializable data class SectionRoute(val sectionId: String)
@Serializable data class BookRoute(val bookId: String)

enum class TopLevelTab(
    val route: Any,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Catalog(CatalogRoute, R.string.nav_catalog, Icons.Outlined.AutoStories),
    Library(LibraryRoute, R.string.nav_library, Icons.Outlined.CollectionsBookmark),
    Player(PlayerRoute, R.string.nav_player, Icons.Outlined.Headphones),
    Speak(SpeakRoute, R.string.nav_speak, Icons.Outlined.RecordVoiceOver),
    Profile(ProfileRoute, R.string.nav_profile, Icons.Outlined.Person),
}
