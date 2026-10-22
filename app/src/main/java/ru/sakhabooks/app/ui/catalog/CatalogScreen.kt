package ru.sakhabooks.app.ui.catalog

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.sakhabooks.app.R
import ru.sakhabooks.app.ui.components.TabScreen

@Composable
fun CatalogScreen() {
    TabScreen(title = stringResource(R.string.catalog_title)) {
        Text(stringResource(R.string.app_tagline), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.catalog_placeholder), style = MaterialTheme.typography.bodyLarge)
    }
}
