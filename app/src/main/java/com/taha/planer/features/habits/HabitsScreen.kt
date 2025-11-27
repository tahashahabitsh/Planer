package com.taha.planer.features.habits

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
fun HabitsScreen() {
    val context = LocalContext.current
    var habits by remember { mutableStateOf(loadHabits(context)) }

    var showDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<PlannerHabit?>(null) }

    fun persist(newList: List<PlannerHabit>) {
        habits = newList
        saveHabits(context, newList)
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
                            text = "عادت‌ها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "عادت‌های خوبت رو اینجا ثبت کن. می‌تونی از دستیار هم بخوای عادت اضافه/حذف/ویرایش کنه.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val enabledCount = habits.count { it.enabled }
                        val totalCount = habits.size
                        Text(
                            text = "عادت‌های فعال: $enabledCount از $totalCount",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (habits.isEmpty()) {
                    Text(
                        text = "هنوز عادتی ثبت نکردی.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(habits, key = { it.id }) { habit ->
                            HabitRow(
                                habit = habit,
                                onToggleEnabled = { enabled ->
                                    val updated = habits.map {
                                        if (it.id == habit.id) it.copy(enabled = enabled) else it
                                    }
                                    persist(updated)
                                },
                                onEdit = {
                                    editingHabit = habit
                                    showDialog = true
                                },
                                onDelete = {
                                    val updated = habits.filterNot { it.id == habit.id }
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
                    editingHabit = null
                    showDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "عادت جدید")
            }
        }
    }

    if (showDialog) {
        HabitDialog(
            initial = editingHabit,
            onDismiss = { showDialog = false },
            onSave = { newHabit ->
                val updated = if (editingHabit == null) {
                    habits + newHabit
                } else {
                    habits.map { if (it.id == newHabit.id) newHabit else it }
                }
                persist(updated)
                showDialog = false
            }
        )
    }
}

@Composable
private fun HabitRow(
    habit: PlannerHabit,
    onToggleEnabled: (Boolean) -> Unit,
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
                checked = habit.enabled,
                onCheckedChange = onToggleEnabled
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = habit.name.ifBlank { "بدون نام" },
                    fontWeight = FontWeight.Bold
                )
                if (habit.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "هدف روزانه: ${habit.targetPerDay}",
                    style = MaterialTheme.typography.bodySmall
                )
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
private fun HabitDialog(
    initial: PlannerHabit?,
    onDismiss: () -> Unit,
    onSave: (PlannerHabit) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var targetText by remember { mutableStateOf(initial?.targetPerDay?.toString() ?: "1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initial == null) "عادت جدید" else "ویرایش عادت")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نام عادت") }
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
                    value = targetText,
                    onValueChange = {
                        targetText = it.filter { ch -> ch.isDigit() }.ifBlank { "1" }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("هدف روزانه (مثلاً تعداد تکرار)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = initial?.id ?: System.currentTimeMillis()
                    val target = targetText.toIntOrNull() ?: 1
                    onSave(
                        PlannerHabit(
                            id = id,
                            name = name.trim(),
                            description = description.trim(),
                            targetPerDay = if (target <= 0) 1 else target,
                            enabled = initial?.enabled ?: true
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
