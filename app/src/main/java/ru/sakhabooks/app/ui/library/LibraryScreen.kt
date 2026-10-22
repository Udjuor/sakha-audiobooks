package ru.sakhabooks.app.ui.library

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.sakhabooks.app.R
import ru.sakhabooks.app.ui.components.TabScreen

@Composable
fun LibraryScreen() {
    TabScreen(title = stringResource(R.string.library_title)) {
        Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.bodyLarge)
    }
}
