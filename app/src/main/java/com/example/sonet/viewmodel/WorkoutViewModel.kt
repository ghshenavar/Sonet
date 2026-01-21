package com.example.sonet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sonet.data.WorkoutRepository
import com.example.sonet.data.WeekData
import com.example.sonet.data.WorkoutStats
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val weekData: StateFlow<WeekData> = repository.getWeekDataFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, WeekData(0, emptyMap()))

    val workoutOptions: StateFlow<List<String>> = repository.getWorkoutOptionsFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            listOf("No option selected", "Climbing", "Leg", "Cardio")
        )

    val today: String = LocalDate.now().dayOfWeek.name.take(3)
        .lowercase().replaceFirstChar { it.uppercase() }

    fun saveSelection(day: String, workout: String) {
        viewModelScope.launch {
            repository.saveWorkoutSelection(day, workout)
        }
    }

    fun addWorkoutOption(option: String) {
        viewModelScope.launch {
            repository.addWorkoutOption(option.trim())
        }
    }
}

class StatisticsViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    val workoutStats: StateFlow<WorkoutStats> = repository.getWorkoutStatsFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, WorkoutStats(emptyMap()))
}