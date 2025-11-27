package com.taha.planer.features.habitbuilder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HabitBuilderScreen() {
    val context = LocalContext.current
    var plans by remember { mutableStateOf(loadHabitPlans(context)) }
    var showDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<HabitPlan?>(null) }

    fun persist(newList: List<HabitPlan>) {
        plans = newList
        saveHabitPlans(context, newList)
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
                Text(
                    text = "سیستم ساخت عادت",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "اینجا عادت‌های جدیدت رو مثل یک پروژه طراحی می‌کنی: چرا، چه زمانی، چطور و با چه پاداشی.",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (plans.isEmpty()) {
                    Text(
                        text = "هنوز هیچ پلن عادتی نساختی.\nاز دکمه پایین برای ساخت اولین عادت استفاده کن.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(plans, key = { it.id }) { plan ->
                            HabitPlanCard(
                                plan = plan,
                                onEdit = {
                                    editingPlan = plan
                                    showDialog = true
                                },
                                onToggleStatus = {
                                    val newStatus = when (plan.status) {
                                        HabitPlanStatus.ACTIVE -> HabitPlanStatus.PAUSED
                                        HabitPlanStatus.PAUSED -> HabitPlanStatus.ACTIVE
                                        HabitPlanStatus.COMPLETED -> HabitPlanStatus.ACTIVE
                                    }
                                    val updated = plan.copy(status = newStatus)
                                    persist(plans.map { if (it.id == plan.id) updated else it })
                                },
                                onComplete = {
                                    val updated = plan.copy(status = HabitPlanStatus.COMPLETED)
                                    persist(plans.map { if (it.id == plan.id) updated else it })
                                },
                                onDelete = {
                                    persist(plans.filterNot { it.id == plan.id })
                                }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                HabitBuilderTipsCard()
            }

            FloatingActionButton(
                onClick = {
                    editingPlan = null
                    showDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "پلن عادت جدید"
                )
            }
        }
    }

    if (showDialog) {
        HabitPlanDialog(
            initial = editingPlan,
            onDismiss = { showDialog = false },
            onSave = { plan ->
                val updated = if (editingPlan == null) {
                    plans + plan
                } else {
                    plans.map { if (it.id == plan.id) plan else it }
                }
                persist(updated)
                showDialog = false
            }
        )
    }
}

@Composable
private fun HabitPlanCard(
    plan: HabitPlan,
    onEdit: () -> Unit,
    onToggleStatus: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "چرا: ${plan.why}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "تریگر: ${plan.cue}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "عمل: ${plan.action}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "پاداش: ${plan.reward}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    val statusText = when (plan.status) {
                        HabitPlanStatus.ACTIVE -> "فعال"
                        HabitPlanStatus.PAUSED -> "متوقف موقت"
                        HabitPlanStatus.COMPLETED -> "تکمیل‌شده"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row {
                        IconButton(onClick = onToggleStatus) {
                            Icon(
                                imageVector = when (plan.status) {
                                    HabitPlanStatus.ACTIVE -> Icons.Filled.Pause
                                    else -> Icons.Filled.PlayArrow
                                },
                                contentDescription = "تغییر وضعیت"
                            )
                        }
                        IconButton(onClick = onComplete) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "اتمام"
                            )
                        }
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "ویرایش"
                            )
                        }
                        IconButton(onClick = onDelete) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "حذف"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "نسخه خیلی کوچک عادت: ${plan.minVersion}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "دفعات در هفته: ${plan.frequencyPerWeek} بار",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "مانع‌ها: ${plan.obstacles}",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "راه‌حل مانع‌ها: ${plan.antiObstacles}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun HabitPlanDialog(
    initial: HabitPlan?,
    onDismiss: () -> Unit,
    onSave: (HabitPlan) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var why by remember { mutableStateOf(initial?.why ?: "") }
    var cue by remember { mutableStateOf(initial?.cue ?: "") }
    var action by remember { mutableStateOf(initial?.action ?: "") }
    var reward by remember { mutableStateOf(initial?.reward ?: "") }
    var freqText by remember { mutableStateOf(initial?.frequencyPerWeek?.toString() ?: "3") }
    var minVersion by remember { mutableStateOf(initial?.minVersion ?: "") }
    var obstacles by remember { mutableStateOf(initial?.obstacles ?: "") }
    var antiObstacles by remember { mutableStateOf(initial?.antiObstacles ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initial == null) "پلن عادت جدید" else "ویرایش پلن عادت")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نام عادت") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = why,
                    onValueChange = { why = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("چرا می‌خوای این عادت رو بسازی؟") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = cue,
                    onValueChange = { cue = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("تریگر / بعد از چه کاری؟ (مثلاً بعد از صبحانه)") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = action,
                    onValueChange = { action = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عمل دقیق (مثلاً ۵ دقیقه مطالعه)") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = reward,
                    onValueChange = { reward = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("پاداش (مثلاً ۵ دقیقه شبکه اجتماعی)") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = freqText,
                    onValueChange = {
                        freqText = it.filter { ch -> ch.isDigit() }.ifBlank { "3" }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("چند بار در هفته؟") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = minVersion,
                    onValueChange = { minVersion = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نسخه خیلی کوچک عادت (مثلاً فقط ۱ دقیقه)") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = obstacles,
                    onValueChange = { obstacles = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("چه مانع‌هایی ممکنه جلوت رو بگیرن؟") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = antiObstacles,
                    onValueChange = { antiObstacles = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("برای هر مانع چه راه‌حلی داری؟") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = initial?.id ?: System.currentTimeMillis()
                    val freq = freqText.toIntOrNull() ?: 3
                    onSave(
                        HabitPlan(
                            id = id,
                            name = name.trim().ifBlank { "عادت جدید" },
                            why = why.trim(),
                            cue = cue.trim(),
                            action = action.trim(),
                            reward = reward.trim(),
                            frequencyPerWeek = if (freq <= 0) 3 else freq,
                            minVersion = minVersion.trim(),
                            obstacles = obstacles.trim(),
                            antiObstacles = antiObstacles.trim(),
                            startDate = initial?.startDate ?: "",
                            status = initial?.status ?: HabitPlanStatus.ACTIVE
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

@Composable
private fun HabitBuilderTipsCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "نکته‌های ساخت عادت",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "• عادت باید خیلی کوچک و واضح شروع شود.\n" +
                        "• همیشه بعد از یک تریگر ثابت انجامش بده (مثلاً بعد از مسواک).\n" +
                        "• نسخه‌ی خیلی کوچک عادت را حتی در بدترین روز انجام بده تا زنجیره نشکند.\n" +
                        "• برای مانع‌های تکراری از قبل راه‌حل بنویس.\n" +
                        "• وقتی چند هفته پایدار شد، می‌توانی از این پلن، عادت را به بخش «عادت‌ها» منتقل کنی.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
