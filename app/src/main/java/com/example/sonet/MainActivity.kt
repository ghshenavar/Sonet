package com.example.sonet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.sonet.ui.theme.SonetTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import android.content.Context
import android.os.Build
import java.time.LocalDate
import java.time.temporal.IsoFields

// DataStore extension
val Context.dataStore by preferencesDataStore("workout_prefs")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SonetTheme {
                MainApp(applicationContext)
            }
        }
    }
}

// ---------------- MAIN APP WITH NAVIGATION ----------------
@Composable
fun MainApp(appContext: Context) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "workout",
            modifier = Modifier.padding(padding)
        ) {
            composable("workout") { WorkoutPlannerScreen(appContext) }
            composable("statistics") { StatisticsScreen(appContext, navController) }
            composable("todo") { TodoScreen() }
            composable("log/{workout}") { backStackEntry ->
                val workout = backStackEntry.arguments?.getString("workout") ?: ""
                WorkoutLogScreen(appContext, workout)
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        NavItem("workout", "Workout", Icons.Default.FitnessCenter),
        NavItem("statistics", "Statistics", Icons.Default.BarChart),
        NavItem("todo", "Todo", Icons.Default.CheckCircle)
    )
    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

data class NavItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

// ---------------- WORKOUT PLANNER ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPlannerScreen(appContext: Context) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val workoutOptions = remember { mutableStateListOf("No option selected", "Climbing", "Leg", "Cardio") }

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
        if (currentWeek != savedWeek) {
            selections.clear()
            savedWeek = currentWeek
        } else {
            days.forEach { day ->
                val key = stringPreferencesKey("week_${currentWeek}_$day")
                prefs[key]?.let { selections[day] = it }
            }
        }
    }

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

    // Add new option dialog
    var showDialog by remember { mutableStateOf(false) }
    var newWorkoutName by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
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
        Button(onClick = { showDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Option")
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Add Workout Option") },
                text = {
                    TextField(value = newWorkoutName, onValueChange = { newWorkoutName = it })
                },
                confirmButton = {
                    Button(onClick = {
                        val trimmed = newWorkoutName.trim()
                        if (trimmed.isNotBlank() && !workoutOptions.any { it.equals(trimmed, true) }) {
                            workoutOptions.add(trimmed)
                        }
                        newWorkoutName = ""
                        showDialog = false
                    }) { Text("Add") }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) { Text("Cancel") }
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
    val backgroundBrush = when {
        selected.equals("No option selected", true) -> Brush.verticalGradient(listOf(Color.LightGray, Color.Gray))
        day == today -> Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFE57F)))
        else -> Brush.verticalGradient(listOf(Color(0xFF4A148C), Color(0xFF7B1FA2)))
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            Modifier.fillMaxSize().background(backgroundBrush).padding(8.dp)
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Text(day, style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontFamily = FontFamily.Cursive))
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(selected, color = Color.White)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        workoutOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { onSelect(option); expanded = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- STATISTICS ----------------
@Composable
fun StatisticsScreen(appContext: Context, navController: NavHostController) {
    val scope = rememberCoroutineScope()
    var workoutData by remember { mutableStateOf<Map<String, List<LocalDate>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        scope.launch {
            val prefs = appContext.dataStore.data.first()
            val map = mutableMapOf<String, MutableList<LocalDate>>()
            prefs.asMap().forEach { (key, value) ->
                if (key.name.startsWith("week_") && value is String) {
                    val workout = value
                    if (workout != "No option selected") {
                        val date = LocalDate.now() // simplification
                        map.getOrPut(workout) { mutableListOf() }.add(date)
                    }
                }
            }
            workoutData = map
        }
    }

    val totalDays = workoutData.values.sumOf { it.size }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Data available: $totalDays days", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(workoutData.keys.toList()) { workout ->
                Card(
                    modifier = Modifier.fillMaxWidth().height(100.dp).clickable {
                        navController.navigate("log/$workout")
                    },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(workout.replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Bold)
                        Text("${workoutData[workout]?.size ?: 0} days")
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutLogScreen(appContext: Context, workout: String) {
    // placeholder log
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Workout log for $workout", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(5) { i ->
                Text("Date ${(i + 1)}")
            }
        }
    }
}

// ---------------- TODO LIST ----------------
@Composable
fun TodoScreen() {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Today", "Eventually")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title) })
            }
        }
        if (tabIndex == 0) TodoList("today") else TodoList("eventually")
    }
}

@Composable
fun TodoList(type: String) {
    val items = remember { mutableStateListOf("Task 1", "Task 2") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Show up to 5 tasks
        items.take(5).forEach { task ->
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Checkbox(checked = false, onCheckedChange = {})
                    Spacer(Modifier.width(8.dp))
                    Text(task)
                }
            }
        }

        // "Add task" row if less than 5 tasks
        if (items.size < 5) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable { items.add("New Task") }
                ) {
                    Checkbox(checked = false, onCheckedChange = {})
                    Spacer(Modifier.width(8.dp))
                    Text("...")
                }
            }
        }
    }
}

