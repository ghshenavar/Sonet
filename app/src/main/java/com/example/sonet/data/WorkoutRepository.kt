package com.example.sonet.data

import android.content.Context
import androidx.datastore.preferences.core.*
import com.example.sonet.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.IsoFields

data class WeekData(
    val weekNumber: Int,
    val selections: Map<String, String>
)

data class WorkoutStats(
    val workoutCounts: Map<String, Int>
)

class WorkoutRepository(private val context: Context) {

    private val weekNumberKey = intPreferencesKey("week_number")
    private val workoutOptionsKey = stringPreferencesKey("workout_options")

    // Observes current week's workout selections
    fun getWeekDataFlow(): Flow<WeekData> {
        val currentWeek = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

        return context.dataStore.data.map { prefs ->
            val savedWeek = prefs[weekNumberKey] ?: currentWeek
            val selections = mutableMapOf<String, String>()

            if (savedWeek == currentWeek) {
                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                    val key = stringPreferencesKey("week_${currentWeek}_$day")
                    prefs[key]?.let { selections[day] = it }
                }
            }

            WeekData(currentWeek, selections)
        }
    }

    // Observes workout options
    fun getWorkoutOptionsFlow(): Flow<List<String>> {
        return context.dataStore.data.map { prefs ->
            val saved = prefs[workoutOptionsKey]
            if (saved.isNullOrBlank()) {
                listOf("No option selected", "Climbing", "Leg", "Cardio")
            } else {
                saved.split("|")
            }
        }
    }

    // Saves a workout selection (runs on IO dispatcher)
    suspend fun saveWorkoutSelection(day: String, workout: String) {
        val currentWeek = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        context.dataStore.edit { prefs ->
            prefs[weekNumberKey] = currentWeek
            prefs[stringPreferencesKey("week_${currentWeek}_$day")] = workout
        }
    }

    // Adds a new workout option (runs on IO dispatcher)
    suspend fun addWorkoutOption(option: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[workoutOptionsKey]
            val options = if (current.isNullOrBlank()) {
                listOf("No option selected", "Climbing", "Leg", "Cardio")
            } else {
                current.split("|")
            }

            if (!options.any { it.equals(option, true) }) {
                prefs[workoutOptionsKey] = (options + option).joinToString("|")
            }
        }
    }

    // Gets workout statistics (runs on IO dispatcher)
    fun getWorkoutStatsFlow(): Flow<WorkoutStats> {
        return context.dataStore.data.map { prefs ->
            val counts = mutableMapOf<String, Int>()

            prefs.asMap().forEach { (key, value) ->
                if (key.name.startsWith("week_") &&
                    key.name.contains("_") &&
                    value is String &&
                    value != "No option selected") {
                    counts[value] = counts.getOrDefault(value, 0) + 1
                }
            }

            WorkoutStats(counts)
        }
    }
}