package com.taha.planer.features.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit

@Composable
fun TasksScreen() {
    val context = LocalContext.current
    var tasks by remember { mutableStateOf(loadTasks(context)) }

    var showDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<PlannerTask?>(null) }

    fun persist(newList: List<PlannerTask>) {
        tasks = newList
        saveTasks(context, newList)
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "کارها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "کارهای مهمت رو بنویس، تیک بزن، و با دستیار هم می‌تونی اضافه/حذف/ویرایش‌شون کنی.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val doneCount = tasks.count { it.isDone }
                        val totalCount = tasks.size
                        val percent = if (totalCount == 0) 0 else (doneCount * 100 / totalCount)
                        Text(
                            text = "پیشرفت امروز: $doneCount از $totalCount کار • $percent٪",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (tasks.isEmpty()) {
                    Text(
                        text = "هنوز کاری ثبت نکردی. از دکمه + یا دستیار استفاده کن.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggleDone = { done ->
                                    val updated = tasks.map {
                                        if (it.id == task.id) it.copy(isDone = done) else it
                                    }
                                    persist(updated)
                                },
                                onEdit = {
                                    editingTask = task
                                    showDialog = true
                                },
                                onDelete = {
                                    val updated = tasks.filterNot { it.id == task.id }
                                    persist(updated)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "کار جدید")
            }
        }
    }

    if (showDialog) {
        TaskDialog(
            initial = editingTask,
            onDismiss = { showDialog = false },
            onSave = { newTask ->
                val updated = if (editingTask == null) {
                    tasks + newTask
                } else {
                    tasks.map { if (it.id == newTask.id) newTask else it }
                }
                persist(updated)
                showDialog = false
            }
        )
    }
}

@Composable
private fun TaskRow(
    task: PlannerTask,
    onToggleDone: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = onToggleDone
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title.ifBlank { "بدون عنوان" },
                    fontWeight = FontWeight.Bold
                )
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (task.date.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "تاریخ: ${task.date}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = "ویرایش")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "حذف")
            }
        }
    }
}

@Composable
private fun TaskDialog(
    initial: PlannerTask?,
    onDismiss: () -> Unit,
    onSave: (PlannerTask) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initial == null) "کار جدید" else "ویرایش کار")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان کار") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("توضیحات (اختیاری)") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("تاریخ (مثال: 2025-11-30، اختیاری)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = initial?.id ?: System.currentTimeMillis()
                    onSave(
                        PlannerTask(
                            id = id,
                            title = title.trim(),
                            description = description.trim(),
                            isDone = initial?.isDone ?: false,
                            date = date.trim()
                        )
                    )
                }
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بی‌خیال")
            }
        }
    )
}
