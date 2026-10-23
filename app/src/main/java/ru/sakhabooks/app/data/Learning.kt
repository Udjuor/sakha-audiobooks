package ru.sakhabooks.app.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Учебные материалы раздела «Сахалыы саҥар» — загружаемый контент: приложение ничего не знает
 * о конкретных уроках, а показывает то, что пришло в файле. Сейчас файл встроен в приложение,
 * позже тот же формат будет отдавать сервер. Описание формата — docs/learning-content.md.
 */
@Serializable
data class LearningFile(
    val version: Int = 2,
    @SerialName("is_demo") val isDemo: Boolean = false,
    val sections: List<LearningSection> = emptyList(),
)

@Serializable
data class LearningSection(
    val id: String,
    val title: Localized = emptyMap(),
    val subtitle: Localized = emptyMap(),
    val items: List<LearningItem> = emptyList(),
)

/** Карточка: слово, фраза или буква на якутском, переводы и подсказка (например, как произносить). */
@Serializable
data class LearningItem(
    val sah: String,
    /** Переводы: {"ru": "…", "en": "…"}. */
    val tr: Localized = emptyMap(),
    val hint: Localized = emptyMap(),
    /** Запись произношения (позже); null — записи нет. */
    val audio: String? = null,
)

@Singleton
class LearningRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    val content: LearningFile by lazy {
        runCatching {
            context.assets.open(DEMO_LEARNING).bufferedReader().use { json.decodeFromString<LearningFile>(it.readText()) }
        }.getOrElse { LearningFile() }
    }

    private companion object {
        const val DEMO_LEARNING = "demo/learning.json"
    }
}
