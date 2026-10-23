package ru.sakhabooks.app.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Запоминает место остановки в каждой главе и выбранную скорость. */
@Singleton
class ProgressStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("listening_progress", Context.MODE_PRIVATE)

    fun position(chapterId: String): Long = prefs.getLong(chapterId, 0L)

    fun save(chapterId: String, positionMs: Long) {
        prefs.edit { putLong(chapterId, positionMs) }
    }

    fun speed(): Float = prefs.getFloat(KEY_SPEED, 1f)

    fun saveSpeed(speed: Float) {
        prefs.edit { putFloat(KEY_SPEED, speed) }
    }

    private companion object {
        const val KEY_SPEED = "playback_speed"
    }
}
