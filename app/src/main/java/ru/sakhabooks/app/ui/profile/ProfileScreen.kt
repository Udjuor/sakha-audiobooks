package ru.sakhabooks.app.ui.profile

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.sakhabooks.app.R
import ru.sakhabooks.app.ui.components.TabScreen

@Composable
fun ProfileScreen() {
    TabScreen(title = stringResource(R.string.profile_title)) {
        Text(stringResource(R.string.profile_placeholder), style = MaterialTheme.typography.bodyLarge)
    }
}
