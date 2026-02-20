package com.example.sonet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.sonet.data.TodoRepository
import com.example.sonet.data.WorkoutRepository
import com.example.sonet.ui.theme.SonetTheme
import com.example.sonet.viewmodel.StatisticsViewModel
import com.example.sonet.viewmodel.TodoViewModel
import com.example.sonet.viewmodel.WorkoutViewModel
import android.content.Context
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

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
            composable("workout") {
                val repository = remember { WorkoutRepository(appContext) }
                val viewModel: WorkoutViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return WorkoutViewModel(repository) as T
                        }
                    }
                )
                WorkoutPlannerScreen(viewModel)
            }
            composable("statistics") {
                val repository = remember { WorkoutRepository(appContext) }
                val viewModel: StatisticsViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return StatisticsViewModel(repository) as T
                        }
                    }
                )
                StatisticsScreen(viewModel, navController)
            }
            composable("todo") {
                val repository = remember { TodoRepository(appContext) }
                val viewModel: TodoViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return TodoViewModel(repository) as T
                        }
                    }
                )
                TodoScreen(viewModel)
            }
            composable("log/{workout}") { backStackEntry ->
                val workout = backStackEntry.arguments?.getString("workout") ?: ""
                WorkoutLogScreen(workout)
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

@Composable
fun WorkoutPlannerScreen(viewModel: WorkoutViewModel) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val weekData by viewModel.weekData.collectAsState()
    val workoutOptions by viewModel.workoutOptions.collectAsState()

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
                    selected = weekData.selections[day] ?: workoutOptions.first(),
                    today = viewModel.today,
                    onSelect = { viewModel.saveSelection(day, it) }
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
                        if (newWorkoutName.isNotBlank()) {
                            viewModel.addWorkoutOption(newWorkoutName)
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
        selected.equals("No option selected", true) -> Brush.verticalGradient(listOf(Color.DarkGray, Color.Gray))
        selected.equals("Rest", true) -> Brush.verticalGradient(listOf(Color.LightGray, Color.Gray))
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
                Text(day, style = MaterialTheme.typography.titleLarge.copy(color = Color.White, fontFamily = FontFamily.Cursive))
                Box {
                    TextButton(onClick = { expanded = true }) {
                        Text(selected, color = Color.White, fontFamily = FontFamily.Cursive)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        workoutOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, fontFamily = FontFamily.Cursive) },
                                onClick = { onSelect(option); expanded = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel, navController: NavHostController) {
    val stats by viewModel.workoutStats.collectAsState()
    val totalDays = stats.workoutCounts.values.sum()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Data available: $totalDays days", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stats.workoutCounts.keys.toList()) { workout ->
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
                        Text("${stats.workoutCounts[workout] ?: 0} days")
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutLogScreen(workout: String) {
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

@Composable
fun TodoScreen(viewModel: TodoViewModel) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Today", "Eventually")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title, style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Cursive)) })
            }
        }
        if (tabIndex == 0) {
            TodoList(viewModel, "today")
        } else {
            TodoList(viewModel, "eventually")
        }
    }
}

@Composable
fun TodoList(viewModel: TodoViewModel, type: String) {
    val todos by if (type == "today") viewModel.todayTodos.collectAsState()
    else viewModel.eventuallyTodos.collectAsState()

    var newTaskText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        items(todos, key = { it.id }) { task ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Checkbox(
                    checked = task.done,
                    onCheckedChange = { viewModel.toggleTodo(type, task.id) }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    task.text,
                    fontFamily = FontFamily.Cursive,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
                )
            }
        }

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Checkbox(checked = false, onCheckedChange = {})
                Spacer(Modifier.width(8.dp))
                TextField(
                    value = newTaskText,
                    onValueChange = { newTaskText = it },
                    placeholder = { Text("...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newTaskText.isNotBlank()) {
                                viewModel.addTodo(type, newTaskText)
                                newTaskText = ""
                            }
                        }
                    )
                )
            }
        }
    }
}