package com.example.sonet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sonet.ui.theme.SonetTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SonetTheme {
                WorkoutPlannerScreen()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WorkoutPlannerScreen() {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val workoutOptions = remember { mutableStateListOf("Rest", "Climbing", "Leg", "Cardio") }
    val selections = remember { mutableStateMapOf<String, String>() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3), // 👈 3 columns per row
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
                    onSelect = { selections[day] = it }
                )
            }
        }

        Button(
            onClick = {
                workoutOptions.add("New Option ${workoutOptions.size + 1}")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Option")
        }
    }
}

@Composable
fun WorkoutDayCard(
    day: String,
    workoutOptions: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp), // control height if you want square-ish cards
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(day, style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Cursive
            ))

            Box {
                TextButton(onClick = { expanded = true }) {
                    Text(selected)
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
