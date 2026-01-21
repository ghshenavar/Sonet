package com.example.sonet.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.sonet.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TodoItem(val id: String, val text: String, val done: Boolean = false)

class TodoRepository(private val context: Context) {

    private fun todayKey() = stringPreferencesKey("todos_today")
    private fun eventuallyKey() = stringPreferencesKey("todos_eventually")

    fun getTodosFlow(type: String): Flow<List<TodoItem>> {
        val key = if (type == "today") todayKey() else eventuallyKey()

        return context.dataStore.data.map { prefs ->
            val json = prefs[key] ?: ""
            if (json.isBlank()) emptyList()
            else json.split("||").mapNotNull { item ->
                val parts = item.split("|")
                if (parts.size == 3) {
                    TodoItem(parts[0], parts[1], parts[2].toBoolean())
                } else null
            }
        }
    }

    suspend fun saveTodos(type: String, todos: List<TodoItem>) {
        val key = if (type == "today") todayKey() else eventuallyKey()

        context.dataStore.edit { prefs ->
            val json = todos.joinToString("||") { "${it.id}|${it.text}|${it.done}" }
            prefs[key] = json
        }
    }
}