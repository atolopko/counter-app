package com.example.counterapp.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.counterapp.data.EventLog
import com.example.counterapp.ui.HistoryViewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.line.LineChart.LineSpec
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val counter by viewModel.counter.collectAsState()
    val logs by viewModel.logs.collectAsState()
    var editingLog by remember { mutableStateOf<EventLog?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(counter?.name ?: "History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                if (logs.isNotEmpty()) {
                    val chartEntryModelProducer = remember { ChartEntryModelProducer() }
                    LaunchedEffect(logs) {
                        val sortedLogs = logs.sortedBy { it.timestamp }
                        chartEntryModelProducer.setEntries(
                            sortedLogs.mapIndexed { index, log ->
                                entryOf(index.toFloat(), log.resultingCount.toFloat())
                            }
                        )
                    }

                    val xAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                        val index = value.toInt()
                        if (index in logs.indices) {
                            val date = Date(logs.sortedBy { it.timestamp }[index].timestamp)
                            SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
                        } else {
                            ""
                        }
                    }

                    val yAxisValueFormatter = AxisValueFormatter<AxisPosition.Vertical.Start> { value, _ ->
                        value.toInt().toString()
                    }

                    Chart(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(16.dp),
                        chart = lineChart(
                            lines = listOf(
                                LineSpec(
                                    point = shapeComponent(Shapes.pillShape, MaterialTheme.colorScheme.primary),
                                    pointSizeDp = 4f
                                )
                            )
                        ),
                        chartModelProducer = chartEntryModelProducer,
                        startAxis = rememberStartAxis(
                            valueFormatter = yAxisValueFormatter,
                            itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 6)
                        ),
                        bottomAxis = rememberBottomAxis(
                            label = textComponent(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textSize = 8.sp,
                                textAlign = Paint.Align.CENTER
                            ),
                            labelRotationDegrees = -45f,
                            valueFormatter = xAxisValueFormatter,
                            itemPlacer = AxisItemPlacer.Horizontal.default(spacing = 3)
                        ),
                    )
                } else {
                    Box(modifier = Modifier.height(250.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No data yet")
                    }
                }
            }

            item { HorizontalDivider() }

            items(logs) { log ->
                LogItem(
                    log = log,
                    onEdit = { editingLog = log },
                    onDelete = { viewModel.deleteLog(log.id) }
                )
            }
        }

        if (editingLog != null) {
            EditLogDialog(
                log = editingLog!!,
                onDismiss = { editingLog = null },
                onConfirm = { amount, timestamp ->
                    if (amount == -1 && timestamp == -1L) {
                        viewModel.deleteLog(editingLog!!.id)
                    } else {
                        viewModel.updateLog(editingLog!!.id, amount, timestamp)
                    }
                    editingLog = null
                }
            )
        }
    }
}

@Composable
fun EditLogDialog(
    log: EventLog,
    onDismiss: () -> Unit,
    onConfirm: (Int, Long) -> Unit
) {
    var amountText by remember { mutableStateOf(log.amountChanged.toString()) }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    var timestampText by remember { mutableStateOf(sdf.format(Date(log.timestamp))) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Changed") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                TextField(
                    value = timestampText,
                    onValueChange = { timestampText = it },
                    label = { Text("Timestamp (yyyy-MM-dd HH:mm)") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                var showDeleteConfirm by remember { mutableStateOf(false) }
                
                if (!showDeleteConfirm) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete this entry")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Are you sure?", color = MaterialTheme.colorScheme.error)
                        Row {
                            TextButton(onClick = { showDeleteConfirm = false }) {
                                Text("No")
                            }
                            TextButton(
                                onClick = { 
                                    onConfirm(-1, -1) // Special signal for delete
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Yes, Delete")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val amount = amountText.toIntOrNull()
                val date = try { sdf.parse(timestampText) } catch (e: Exception) { null }
                
                if (amount == null) {
                    error = "Invalid amount"
                } else if (date == null) {
                    error = "Invalid date format"
                } else {
                    onConfirm(amount, date.time)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LogItem(log: EventLog, onEdit: () -> Unit, onDelete: () -> Unit) {
    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val dateString = remember(log.timestamp) { sdf.format(Date(log.timestamp)) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete entry?") },
            text = { Text("Are you sure you want to delete this entry? The total count will be recalculated.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    ListItem(
        headlineContent = { Text("Result: ${log.resultingCount}") },
        supportingContent = { Text("Change: ${if (log.amountChanged > 0) "+" else ""}${log.amountChanged}") },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dateString, modifier = Modifier.padding(end = 8.dp))
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Entry")
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Entry", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}
