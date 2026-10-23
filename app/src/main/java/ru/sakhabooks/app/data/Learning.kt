package ru.sakhabooks.app.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Учебные материалы раздела «Сахалыы саҥар». Позже тот же формат будет отдавать сервер. */
@Serializable
data class LearningFile(
    val version: Int = 1,
    @SerialName("is_demo") val isDemo: Boolean = false,
    val sections: List<LearningSection> = emptyList(),
)

@Serializable
data class LearningSection(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val items: List<LearningItem> = emptyList(),
)

/** Карточка: слово, фраза или буква на якутском, перевод и подсказка (например, как произносить). */
@Serializable
data class LearningItem(
    val sah: String,
    val ru: String,
    val hint: String? = null,
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
