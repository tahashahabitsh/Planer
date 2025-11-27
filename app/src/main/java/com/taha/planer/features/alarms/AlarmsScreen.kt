package com.taha.planer.features.alarms

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private const val PREF_ALARMS = "planner_alarms"
private const val KEY_ALARMS = "alarms_v1"

enum class AlarmRepeatType(val code: Int, val label: String) {
    ONCE(0, "فقط یک بار"),
    DAILY(1, "هر روز");

    companion object {
        fun fromCode(code: Int): AlarmRepeatType =
            values().find { it.code == code } ?: ONCE
    }
}

/**
 * sectionTag اینجا مشخص می‌کند آلارم مربوط کدام بخش است:
 * مثلا: "کارها"، "عادت‌ها"، "خواب"، "آب"، "مکمل‌ها"، "ورزش"، "ژورنال"، ...
 */
data class PlannerAlarm(
    val id: Long,
    val title: String,
    val message: String,
    val hour: Int,
    val minute: Int,
    val repeatType: AlarmRepeatType,
    val enabled: Boolean,
    val sectionTag: String
)

@Composable
fun AlarmsScreen() {
    val context = LocalContext.current
    var alarms by remember { mutableStateOf(loadAlarms(context)) }

    var showDialog by remember { mutableStateOf(false) }
    var editingAlarm by remember { mutableStateOf<PlannerAlarm?>(null) }

    val snackbarHostState: SnackbarHostState = rememberSnackbarHostState()
    val scope = rememberCoroutineScope()

    fun persist(newAlarms: List<PlannerAlarm>) {
        alarms = newAlarms
        saveAlarms(context, newAlarms)
        rescheduleAllAlarms(context, newAlarms)
    }

    // بار اول که صفحه باز می‌شود، آلارم‌های فعال دوباره زمان‌بندی می‌شوند
    LaunchedEffect(Unit) {
        rescheduleAllAlarms(context, alarms)
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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
                            text = "آلارم‌ها و نوتیف‌ها",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "برای هر بخش (کارها، عادت‌ها، سلامت، خواب، آب، مکمل‌ها، ورزش، ژورنال و...) می‌تونی یادآوری بسازی.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "آلارم‌ها بر اساس ساعت و دقیقه تنظیم می‌شن و می‌تونن یک‌بار یا هر روز تکرار بشن.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تعداد آلارم‌ها: ${alarms.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // کارت ساخت سریع آلارم برای بخش‌های مهم
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "ساخت سریع آلارم برای بخش‌ها",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "با یک کلیک برای کارها، عادت‌ها، خواب، آب، مکمل‌ها و ورزش آلارم بساز.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        fun quickAdd(
                            title: String,
                            message: String,
                            hour: Int,
                            minute: Int,
                            tag: String
                        ) {
                            val id = System.currentTimeMillis()
                            val alarm = PlannerAlarm(
                                id = id,
                                title = title,
                                message = message,
                                hour = hour,
                                minute = minute,
                                repeatType = AlarmRepeatType.DAILY,
                                enabled = true,
                                sectionTag = tag
                            )
                            val updated = alarms + alarm
                            persist(updated)
                            scope.launch {
                                snackbarHostState.showSnackbar("آلارم \"$title\" ساخته شد")
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "مرور کارهای امروز",
                                            message = "کارها و تسک‌های امروزت رو چک کن 👀",
                                            hour = 8,
                                            minute = 0,
                                            tag = "کارها"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("کارها (۸ صبح)")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "مرور عادت‌ها",
                                            message = "عادت‌های روزانه‌ات رو ثبت و تیک بزن ✅",
                                            hour = 21,
                                            minute = 0,
                                            tag = "عادت‌ها"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("عادت‌ها (۹ شب)")
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "یادآور خواب",
                                            message = "لطفاً برای خواب آماده شو 🌙",
                                            hour = 23,
                                            minute = 0,
                                            tag = "خواب"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("خواب (۱۱ شب)")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "یادآور آب",
                                            message = "یک لیوان آب بخور 💧",
                                            hour = 11,
                                            minute = 0,
                                            tag = "آب"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("آب (۱۱ صبح)")
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "مکمل‌ها",
                                            message = "مکمل‌ها / ویتامین‌های امروزت رو یادت نره 💊",
                                            hour = 9,
                                            minute = 0,
                                            tag = "مکمل‌ها"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("مکمل‌ها (۹ صبح)")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "ورزش",
                                            message = "وقت ورزشه 🏋️‍♂️",
                                            hour = 18,
                                            minute = 0,
                                            tag = "ورزش"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("ورزش (۶ عصر)")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "لیست آلارم‌ها",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (alarms.isEmpty()) {
                    Text(
                        text = "هنوز آلارمی نساختی. از الگوهای سریع یا دکمه + استفاده کن.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(alarms, key = { it.id }) { alarm ->
                            AlarmRow(
                                alarm = alarm,
                                onToggleEnabled = { enabled ->
                                    val updated = alarms.map {
                                        if (it.id == alarm.id) it.copy(enabled = enabled) else it
                                    }
                                    persist(updated)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (enabled) "آلارم فعال شد" else "آلارم غیرفعال شد"
                                        )
                                    }
                                },
                                onEdit = {
                                    editingAlarm = alarm
                                    showDialog = true
                                },
                                onDelete = {
                                    val newList = alarms.filterNot { it.id == alarm.id }
                                    persist(newList)
                                    cancelAlarm(context, alarm)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    FloatingActionButton(
                        onClick = {
                            editingAlarm = null
                            showDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "آلارم جدید"
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
            )
        }
    }

    if (showDialog) {
        AlarmDialog(
            initial = editingAlarm,
            onDismiss = { showDialog = false },
            onSave = { newAlarm ->
                val updated = if (editingAlarm == null) {
                    alarms + newAlarm
                } else {
                    alarms.map { if (it.id == newAlarm.id) newAlarm else it }
                }
                persist(updated)
                showDialog = false
            }
        )
    }
}
                // کارت ساخت سریع آلارم برای بخش‌های مهم
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "ساخت سریع آلارم برای بخش‌ها",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "با یک کلیک برای کارها، عادت‌ها، خواب، آب، مکمل‌ها و ورزش آلارم بساز.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        fun quickAdd(
                            title: String,
                            message: String,
                            hour: Int,
                            minute: Int,
                            tag: String
                        ) {
                            val id = System.currentTimeMillis()
                            val alarm = PlannerAlarm(
                                id = id,
                                title = title,
                                message = message,
                                hour = hour,
                                minute = minute,
                                repeatType = AlarmRepeatType.DAILY,
                                enabled = true,
                                sectionTag = tag
                            )
                            val updated = alarms + alarm
                            persist(updated)
                            scope.launch {
                                snackbarHostState.showSnackbar("آلارم \"$title\" ساخته شد")
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "مرور کارهای امروز",
                                            message = "کارها و تسک‌های امروزت رو چک کن 👀",
                                            hour = 8,
                                            minute = 0,
                                            tag = "کارها"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("کارها (۸ صبح)")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "مرور عادت‌ها",
                                            message = "عادت‌های روزانه‌ات رو ثبت و تیک بزن ✅",
                                            hour = 21,
                                            minute = 0,
                                            tag = "عادت‌ها"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("عادت‌ها (۹ شب)")
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "یادآور خواب",
                                            message = "لطفاً برای خواب آماده شو 🌙",
                                            hour = 23,
                                            minute = 0,
                                            tag = "خواب"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("خواب (۱۱ شب)")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "یادآور آب",
                                            message = "یک لیوان آب بخور 💧",
                                            hour = 11,
                                            minute = 0,
                                            tag = "آب"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("آب (۱۱ صبح)")
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "مکمل‌ها",
                                            message = "مکمل‌ها / ویتامین‌های امروزت رو یادت نره 💊",
                                            hour = 9,
                                            minute = 0,
                                            tag = "مکمل‌ها"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("مکمل‌ها (۹ صبح)")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        quickAdd(
                                            title = "ورزش",
                                            message = "وقت ورزشه 🏋️‍♂️",
                                            hour = 18,
                                            minute = 0,
                                            tag = "ورزش"
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("ورزش (۶ عصر)")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "لیست آلارم‌ها",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (alarms.isEmpty()) {
                    Text(
                        text = "هنوز آلارمی نساختی. از الگوهای سریع یا دکمه + استفاده کن.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(alarms, key = { it.id }) { alarm ->
                            AlarmRow(
                                alarm = alarm,
                                onToggleEnabled = { enabled ->
                                    val updated = alarms.map {
                                        if (it.id == alarm.id) it.copy(enabled = enabled) else it
                                    }
                                    persist(updated)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (enabled) "آلارم فعال شد" else "آلارم غیرفعال شد"
                                        )
                                    }
                                },
                                onEdit = {
                                    editingAlarm = alarm
                                    showDialog = true
                                },
                                onDelete = {
                                    val newList = alarms.filterNot { it.id == alarm.id }
                                    persist(newList)
                                    cancelAlarm(context, alarm)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    FloatingActionButton(
                        onClick = {
                            editingAlarm = null
                            showDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "آلارم جدید"
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
            )
        }
    }

    if (showDialog) {
        AlarmDialog(
            initial = editingAlarm,
            onDismiss = { showDialog = false },
            onSave = { newAlarm ->
                val updated = if (editingAlarm == null) {
                    alarms + newAlarm
                } else {
                    alarms.map { if (it.id == newAlarm.id) newAlarm else it }
                }
                persist(updated)
                showDialog = false
            }
        )
    }
}
@Composable
private fun AlarmRow(
    alarm: PlannerAlarm,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeLabel = "%02d:%02d".format(alarm.hour, alarm.minute)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (alarm.title.isNotBlank()) alarm.title else "بدون عنوان",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$timeLabel  •  ${alarm.repeatType.label}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (alarm.sectionTag.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "بخش مرتبط: ${alarm.sectionTag}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "حذف"
                    )
                }
            }

            if (alarm.message.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = alarm.message,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onEdit) {
                    Text("ویرایش")
                }
            }
        }
    }
}

@Composable
private fun AlarmDialog(
    initial: PlannerAlarm?,
    onDismiss: () -> Unit,
    onSave: (PlannerAlarm) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var message by remember { mutableStateOf(initial?.message ?: "") }
    var hourText by remember { mutableStateOf(initial?.hour?.toString() ?: "") }
    var minuteText by remember { mutableStateOf(initial?.minute?.toString() ?: "") }
    var repeatType by remember { mutableStateOf(initial?.repeatType ?: AlarmRepeatType.ONCE) }
    var sectionTag by remember { mutableStateOf(initial?.sectionTag ?: "") }

    var error by remember { mutableStateOf<String?>(null) }

    val sectionOptions = listOf(
        "عمومی",
        "کارها",
        "عادت‌ها",
        "روال‌ها",
        "سلامت",
        "خواب",
        "آب",
        "مکمل‌ها",
        "ورزش",
        "مود",
        "آرامش",
        "پاداش‌ها",
        "مدیا / فیلم / کتاب",
        "مالی",
        "ژورنال",
        "تمرکز",
        "ساخت عادت",
        "برنامه‌ریزی بلندمدت"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "آلارم جدید" else "ویرایش آلارم",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("عنوان (مثلا: ورزش / مکمل / کار مهم)") }
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    label = { Text("متن نوتیف (اختیاری)") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "زمان آلارم:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = {
                            val filtered = it.filter { ch -> ch.isDigit() }
                            hourText = filtered.take(2)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("ساعت (0-23)") },
                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = {
                            val filtered = it.filter { ch -> ch.isDigit() }
                            minuteText = filtered.take(2)
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("دقیقه (0-59)") },
                        keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "نوع تکرار:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { repeatType = AlarmRepeatType.ONCE }
                    ) {
                        Text(
                            text = "فقط یک بار",
                            fontWeight = if (repeatType == AlarmRepeatType.ONCE) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    FilledTonalButton(
                        onClick = { repeatType = AlarmRepeatType.DAILY }
                    ) {
                        Text(
                            text = "هر روز",
                            fontWeight = if (repeatType == AlarmRepeatType.DAILY) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ارتباط با بخش‌ها:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sectionOptions.chunked(3).forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowItems.forEach { option ->
                                SectionTagChip(
                                    text = option,
                                    selected = sectionTag == option
                                ) {
                                    sectionTag = option
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = sectionTag,
                    onValueChange = { sectionTag = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("نام دلخواه بخش مرتبط (می‌تونی خالی بذاری)") }
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val h = hourText.toIntOrNull()
                    val m = minuteText.toIntOrNull()
                    if (h == null || m == null || h !in 0..23 || m !in 0..59) {
                        error = "ساعت یا دقیقه نامعتبر است."
                        return@TextButton
                    }
                    val nowId = initial?.id ?: System.currentTimeMillis()
                    val alarm = PlannerAlarm(
                        id = nowId,
                        title = title.trim(),
                        message = message.trim(),
                        hour = h,
                        minute = m,
                        repeatType = repeatType,
                        enabled = true,
                        sectionTag = sectionTag.trim()
                    )
                    onSave(alarm)
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
private fun SectionTagChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilledTonalButton(onClick = onClick) {
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
private fun loadAlarms(context: Context): List<PlannerAlarm> {
    val prefs = context.getSharedPreferences(PREF_ALARMS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_ALARMS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 7) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val hour = parts[1].toIntOrNull() ?: return@mapNotNull null
            val minute = parts[2].toIntOrNull() ?: return@mapNotNull null
            val repeatCode = parts[3].toIntOrNull() ?: 0
            val enabled = parts[4] == "1"
            val title = parts[5]
            val message = parts[6]
            val sectionTag = if (parts.size >= 8) parts[7] else ""
            PlannerAlarm(
                id = id,
                title = title,
                message = message,
                hour = hour,
                minute = minute,
                repeatType = AlarmRepeatType.fromCode(repeatCode),
                enabled = enabled,
                sectionTag = sectionTag
            )
        }
}

private fun saveAlarms(context: Context, alarms: List<PlannerAlarm>) {
    val prefs = context.getSharedPreferences(PREF_ALARMS, Context.MODE_PRIVATE)
    val raw = alarms.joinToString("\n") { a ->
        val safeTitle = a.title.replace("\n", " ")
        val safeMsg = a.message.replace("\n", " ")
        val safeSection = a.sectionTag.replace("\n", " ")
        "${a.id}||${a.hour}||${a.minute}||${a.repeatType.code}||${if (a.enabled) "1" else "0"}||" +
                "$safeTitle||$safeMsg||$safeSection"
    }
    prefs.edit().putString(KEY_ALARMS, raw).apply()
}

// ---------- زمان‌بندی با AlarmManager (تو AlarmReceiver پیاده شده) ----------

fun rescheduleAllAlarms(context: Context, alarms: List<PlannerAlarm>) {
    alarms.forEach { alarm ->
        if (alarm.enabled) {
            scheduleAlarm(context, alarm)
        } else {
            cancelAlarm(context, alarm)
        }
    }
}

fun scheduleAlarm(context: Context, alarm: PlannerAlarm) {
    schedulePlannerAlarm(context, alarm)
}

fun cancelAlarm(context: Context, alarm: PlannerAlarm) {
    cancelPlannerAlarm(context, alarm)
}
