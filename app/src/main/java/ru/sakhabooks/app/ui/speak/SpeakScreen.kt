package ru.sakhabooks.app.ui.speak

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import ru.sakhabooks.app.R
import ru.sakhabooks.app.data.LearningFile
import ru.sakhabooks.app.data.LearningItem
import ru.sakhabooks.app.data.LearningRepository
import ru.sakhabooks.app.data.LearningSection
import ru.sakhabooks.app.data.pick
import ru.sakhabooks.app.ui.components.TabScreen
import javax.inject.Inject

@HiltViewModel
class SpeakViewModel @Inject constructor(
    repository: LearningRepository,
) : ViewModel() {
    val content: LearningFile = repository.content
}

/** Раздел «Сахалыы саҥар»: темы для изучения якутского — буквы, фразы, слова из сказок. */
@Composable
fun SpeakScreen(viewModel: SpeakViewModel = hiltViewModel()) {
    val content = viewModel.content
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }

    TabScreen(title = stringResource(R.string.speak_title)) {
        if (content.sections.isEmpty()) {
            Text(stringResource(R.string.speak_placeholder), style = MaterialTheme.typography.bodyLarge)
            return@TabScreen
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(R.string.speak_intro), style = MaterialTheme.typography.bodyMedium)
                if (content.isDemo) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.speak_demo_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(content.sections, key = { it.id }) { section ->
                SectionCard(
                    section = section,
                    isExpanded = expanded == section.id,
                    onToggle = { expanded = if (expanded == section.id) null else section.id },
                )
            }
        }
    }
}

@Composable
private fun SectionCard(section: LearningSection, isExpanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(section.title.pick(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val subtitle = section.subtitle.pick()
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    pluralStringResource(R.plurals.speak_cards_count, section.items.size, section.items.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = stringResource(if (isExpanded) R.string.collapse else R.string.expand),
            )
        }
        AnimatedVisibility(visible = isExpanded) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) {
                section.items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider()
                    LearningRow(item)
                }
            }
        }
    }
}

@Composable
private fun LearningRow(item: LearningItem) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            item.sah,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(item.tr.pick(), style = MaterialTheme.typography.bodyLarge)
        item.hint.pick().takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
