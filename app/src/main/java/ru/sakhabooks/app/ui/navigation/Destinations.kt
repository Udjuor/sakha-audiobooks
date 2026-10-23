package ru.sakhabooks.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import ru.sakhabooks.app.R

// Адреса экранов.
@Serializable data object CatalogRoute
@Serializable data object LibraryRoute
@Serializable data object ProfileRoute
@Serializable data class BookRoute(val bookId: String)

enum class TopLevelTab(
    val route: Any,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    Catalog(CatalogRoute, R.string.nav_catalog, Icons.Outlined.Headphones),
    Library(LibraryRoute, R.string.nav_library, Icons.AutoMirrored.Outlined.LibraryBooks),
    Profile(ProfileRoute, R.string.nav_profile, Icons.Outlined.Person),
}
