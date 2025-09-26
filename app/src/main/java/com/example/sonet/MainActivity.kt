package com.example.sonet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.sonet.ui.theme.SonetTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.temporal.IsoFields

// DataStore extension

//todo: fix the min version
//todo: I am not sure getting the current date works
val Context.dataStore by preferencesDataStore("workout_prefs")

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SonetTheme {
                WorkoutPlannerScreen(appContext = applicationContext)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlannerScreen(appContext: Context) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val workoutOptions = remember { mutableStateListOf("Rest", "Climbing", "Leg", "Cardio") }

    val today = LocalDate.now().dayOfWeek.name.take(3).lowercase()
        .replaceFirstChar { it.uppercase() }
    val currentWeek = LocalDate.now().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

    val selections = remember { mutableStateMapOf<String, String>() }
    var savedWeek by remember { mutableStateOf(currentWeek) }

    val scope = rememberCoroutineScope()

    // Load saved data
    LaunchedEffect(Unit) {
        val prefs = appContext.dataStore.data.first()
        val storedWeek = prefs[intPreferencesKey("week_number")] ?: currentWeek
        savedWeek = storedWeek

        // Load last 4 weeks (week:day -> workout)
        for (weekOffset in 0..3) {
            val weekNum = currentWeek - weekOffset
            days.forEach { day ->
                val key = stringPreferencesKey("week_${weekNum}_$day")
                prefs[key]?.let {
                    if (weekOffset == 0 && weekNum == currentWeek) {
                        selections[day] = it
                    }
                }
            }
        }

        // Reset if week changed
        if (currentWeek != savedWeek) {
            selections.clear()
            savedWeek = currentWeek
        }
    }

    // Save selections on change
    fun saveSelection(day: String, workout: String) {
        selections[day] = workout
        scope.launch {
            appContext.dataStore.edit { prefs ->
                prefs[intPreferencesKey("week_number")] = currentWeek
                val key = stringPreferencesKey("week_${currentWeek}_$day")
                prefs[key] = workout
            }
        }
    }

    // Dialog state for adding options
    var showDialog by remember { mutableStateOf(false) }
    var newWorkoutName by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days) { day ->
                WorkoutDayCard(
                    day = day,
                    workoutOptions = workoutOptions,
                    selected = selections[day] ?: workoutOptions.first(),
                    today = today,
                    onSelect = { saveSelection(day, it) }
                )
            }
        }

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Option")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    newWorkoutName = ""
                },
                title = { Text("Add Workout Option") },
                text = {
                    Column {
                        Text("Enter workout name:")
                        TextField(
                            value = newWorkoutName,
                            onValueChange = { newWorkoutName = it },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val trimmedName = newWorkoutName.trim()
                        val exists = workoutOptions.any {
                            it.equals(trimmedName, ignoreCase = true)
                        }
                        if (trimmedName.isNotBlank() && !exists) {
                            workoutOptions.add(trimmedName)
                        }
                        showDialog = false
                        newWorkoutName = ""
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        showDialog = false
                        newWorkoutName = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun WorkoutDayCard(
    day: String,
    workoutOptions: List<String>,
    selected: String,
    today: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val dayGradients = mapOf(
        "Mon" to Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))),
        "Tue" to Brush.verticalGradient(listOf(Color(0xFF6A1B9A), Color(0xFF9C27B0))),
        "Wed" to Brush.verticalGradient(listOf(Color(0xFF8E24AA), Color(0xFFBA68C8))),
        "Thu" to Brush.verticalGradient(listOf(Color(0xFFAB47BC), Color(0xFFD05CE3))),
        "Fri" to Brush.verticalGradient(listOf(Color(0xFFCE93D8), Color(0xFFE1BEE7))),
        "Sat" to Brush.verticalGradient(listOf(Color(0xFFD1C4E9), Color(0xFFEDE7F6))),
        "Sun" to Brush.verticalGradient(listOf(Color(0xFFD1C4E9), Color(0xFFEDE7F6)))
    )

    val backgroundBrush = when {
        selected.equals("Rest", ignoreCase = true) -> Brush.verticalGradient(
            listOf(Color.LightGray, Color.Gray)
        )
        day == today && selected == workoutOptions.first() -> Brush.verticalGradient(
            listOf(Color(0xFFFFD700), Color(0xFFFFE57F)) // Golden gradient
        )
        else -> dayGradients[day] ?: Brush.verticalGradient(
            listOf(Color.White, Color.LightGray)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(8.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    day,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Cursive,
                        color = Color.White
                    )
                )

                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(selected, color = Color.White)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        workoutOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onSelect(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
