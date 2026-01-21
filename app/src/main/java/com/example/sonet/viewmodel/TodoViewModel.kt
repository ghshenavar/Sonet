package com.example.sonet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sonet.data.TodoItem
import com.example.sonet.data.TodoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class TodoViewModel(
    private val repository: TodoRepository
) : ViewModel() {

    private val _todayTodos = MutableStateFlow<List<TodoItem>>(emptyList())
    val todayTodos: StateFlow<List<TodoItem>> = _todayTodos.asStateFlow()

    private val _eventuallyTodos = MutableStateFlow<List<TodoItem>>(emptyList())
    val eventuallyTodos: StateFlow<List<TodoItem>> = _eventuallyTodos.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getTodosFlow("today").collect { _todayTodos.value = it }
        }
        viewModelScope.launch {
            repository.getTodosFlow("eventually").collect { _eventuallyTodos.value = it }
        }
    }

    fun addTodo(type: String, text: String) {
        if (text.isBlank()) return

        val newItem = TodoItem(UUID.randomUUID().toString(), text.trim())
        val currentList = if (type == "today") _todayTodos.value else _eventuallyTodos.value
        val updatedList = currentList + newItem

        if (type == "today") _todayTodos.value = updatedList
        else _eventuallyTodos.value = updatedList

        viewModelScope.launch {
            repository.saveTodos(type, updatedList)
        }
    }

    fun toggleTodo(type: String, id: String) {
        val currentList = if (type == "today") _todayTodos.value else _eventuallyTodos.value
        val updatedList = currentList.map {
            if (it.id == id) it.copy(done = !it.done) else it
        }

        if (type == "today") _todayTodos.value = updatedList
        else _eventuallyTodos.value = updatedList

        viewModelScope.launch {
            repository.saveTodos(type, updatedList)
        }
    }
}