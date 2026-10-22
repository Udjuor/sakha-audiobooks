package ru.sakhabooks.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Общая «рамка» экрана вкладки: заголовок, под ним полоса орнамента, затем содержимое. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabScreen(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        TopAppBar(title = { Text(title, style = MaterialTheme.typography.headlineSmall) })
        OrnamentBand(Modifier.padding(horizontal = 16.dp))
        Column(Modifier.fillMaxSize().padding(16.dp), content = content)
    }
}
