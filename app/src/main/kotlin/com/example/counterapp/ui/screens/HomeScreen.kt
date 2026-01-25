package com.example.counterapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.counterapp.data.Counter
import com.example.counterapp.ui.HomeViewModel
import com.example.counterapp.ui.CounterUiModel
import com.example.counterapp.ui.ImportStatus
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Remove
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToHistory: (Long) -> Unit
) {
    val counters by viewModel.counters.collectAsState()
    val importStatus by viewModel.importStatus.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var editingCounter by remember { mutableStateOf<Counter?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Count It!") }
            )
        },
        floatingActionButton = {
            var showFabMenu by remember { mutableStateOf(false) }
            Box {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, contentDescription = "Add")
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("New Counter") },
                        onClick = {
                            showFabMenu = false
                            showAddDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Import Data") },
                        onClick = {
                            showFabMenu = false
                            showImportDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) }
                    )
                }
            }
        }
    ) { padding ->
        if (counters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Analytics,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Ready to count?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Start fresh or bring in your history.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Create First Counter")
                    }
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Import Historical Data")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(counters) { model ->
                    CounterItem(
                        counter = model.counter,
                        addedToday = model.addedToday,
                        onIncrement = { viewModel.incrementCounter(model.counter, it) },
                        onUpdateIncrementAmount = { newAmount ->
                            viewModel.updateCounter(model.counter.copy(lastIncrementAmount = newAmount))
                        },
                        onUndo = { viewModel.undoLastIncrement(model.counter) },
                        onEdit = { editingCounter = model.counter },
                        onViewHistory = { onNavigateToHistory(model.counter.id) },
                        onToggleExpand = { viewModel.updateCounter(model.counter.copy(isExpanded = it)) }
                    )
                }
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
            },
            onDelete = {
                viewModel.deleteCounter(editingCounter!!)
                editingCounter = null
            }
        )
    }

    if (showImportDialog) {
        ImportDialog(
            onDismiss = { showImportDialog = false },
            onConfirm = { text ->
                viewModel.importData(text)
                showImportDialog = false
            }
        )
    }

    when (val status = importStatus) {
        is ImportStatus.Success -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearImportStatus() },
                title = { Text("Import Successful") },
                text = {
                    Column {
                        status.results.forEach { (name, count) ->
                            Text("$name: $count values added")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.clearImportStatus() }) {
                        Text("OK")
                    }
                }
            )
        }
        is ImportStatus.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearImportStatus() },
                title = { Text("Import Failed") },
                text = { Text(status.message) },
                confirmButton = {
                    Button(onClick = { viewModel.clearImportStatus() }) {
                        Text("OK")
                    }
                }
            )
        }
        else -> {}
    }
}

@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Format:\nCounter Name\nMM/DD/YY [val1 val2 ...]",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("Paste data here...") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text) }) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CounterItem(
    counter: Counter,
    addedToday: Int,
    onIncrement: (Int) -> Unit,
    onUpdateIncrementAmount: (Int) -> Unit,
    onUndo: () -> Unit,
    onEdit: () -> Unit,
    onViewHistory: () -> Unit,
    onToggleExpand: (Boolean) -> Unit
) {
    var showUndoConfirmation by remember { mutableStateOf(false) }

    if (showUndoConfirmation) {
        AlertDialog(
            onDismissRequest = { showUndoConfirmation = false },
            title = { Text("Undo last entry?") },
            text = { Text("This will revert the last count update for \"${counter.name}\".") },
            confirmButton = {
                TextButton(onClick = {
                    onUndo()
                    showUndoConfirmation = false
                }) {
                    Text("Undo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUndoConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand(!counter.isExpanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        if (counter.isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (counter.isExpanded) "Collapse" else "Expand",
                        modifier = Modifier.padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text(
                            text = counter.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!counter.isExpanded) {
                            Text(
                                text = "Count: ${counter.currentCount}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!counter.isExpanded) {
                        Button(
                            onClick = { onIncrement(counter.lastIncrementAmount) },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            modifier = Modifier.height(36.dp).padding(end = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("+${counter.lastIncrementAmount}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    IconButton(onClick = { showUndoConfirmation = true }) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo")
                    }
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            }
            
            AnimatedVisibility(
                visible = counter.isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                ) {
                    Text(
                        text = "${counter.currentCount}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    
                    Text(
                        text = "Today: $addedToday",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Step Control on the Left
                        Box(
                            modifier = Modifier.weight(0.4f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Step",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onUpdateIncrementAmount((counter.lastIncrementAmount - 1).coerceAtLeast(1)) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease Step", modifier = Modifier.size(20.dp))
                                    }
                                    
                                    BasicTextField(
                                        value = counter.lastIncrementAmount.toString(),
                                        onValueChange = { newValue ->
                                            val amount = newValue.filter { it.isDigit() }.toIntOrNull() ?: 1
                                            onUpdateIncrementAmount(amount)
                                        },
                                        modifier = Modifier.width(60.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )

                                    IconButton(
                                        onClick = { onUpdateIncrementAmount(counter.lastIncrementAmount + 1) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase Step", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        // Main Increment Button on the Right
                        Button(
                            modifier = Modifier
                                .weight(0.6f)
                                .height(64.dp),
                            onClick = { onIncrement(counter.lastIncrementAmount) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.headlineLarge
                            )
                        }
                    }
                }
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
    onConfirm: (String, Int) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var amountText by remember { mutableStateOf(initialAmount.toString()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteConfirmName by remember { mutableStateOf("") }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirm = false 
                deleteConfirmName = ""
            },
            title = { Text("Delete Counter?") },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This will permanently delete \"$initialName\" and all its historical data. This action cannot be undone.")
                    Text("Please type \"$initialName\" to confirm:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    TextField(
                        value = deleteConfirmName,
                        onValueChange = { deleteConfirmName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(initialName) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete?.invoke()
                        showDeleteConfirm = false
                        deleteConfirmName = ""
                    },
                    enabled = deleteConfirmName.trim() == initialName.trim(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteConfirm = false 
                    deleteConfirmName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

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

                if (onDelete != null) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Counter")
                    }
                }
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
