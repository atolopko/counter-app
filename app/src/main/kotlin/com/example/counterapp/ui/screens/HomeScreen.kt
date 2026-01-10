package com.example.counterapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.counterapp.data.Counter
import com.example.counterapp.ui.HomeViewModel
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToHistory: (Long) -> Unit
) {
    val counters by viewModel.counters.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCounter by remember { mutableStateOf<Counter?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Counter App") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Counter")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(counters) { counter ->
                CounterItem(
                    counter = counter,
                    onIncrement = { viewModel.incrementCounter(counter, it) },
                    onEdit = { editingCounter = counter },
                    onViewHistory = { onNavigateToHistory(counter.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddEditCounterDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount ->
                viewModel.addCounter(name, amount)
                showAddDialog = false
            }
        )
    }

    if (editingCounter != null) {
        AddEditCounterDialog(
            initialName = editingCounter!!.name,
            initialAmount = editingCounter!!.lastIncrementAmount,
            title = "Edit Counter",
            onDismiss = { editingCounter = null },
            onConfirm = { name, amount ->
                viewModel.updateCounter(editingCounter!!.copy(name = name, lastIncrementAmount = amount))
                editingCounter = null
            }
        )
    }
}

@Composable
fun CounterItem(
    counter: Counter,
    onIncrement: (Int) -> Unit,
    onEdit: () -> Unit,
    onViewHistory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = counter.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            }
            
            Text(
                text = "${counter.currentCount}",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 32.dp)
            )

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                onClick = { onIncrement(counter.lastIncrementAmount) },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "+ ${counter.lastIncrementAmount}",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
fun AddEditCounterDialog(
    initialName: String = "",
    initialAmount: Int = 10,
    title: String = "New Counter",
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var amountText by remember { mutableStateOf(initialAmount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Default Increment") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                val amount = amountText.toIntOrNull() ?: 10
                onConfirm(name, amount) 
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
