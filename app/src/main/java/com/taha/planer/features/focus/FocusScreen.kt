package com.taha.planer.features.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun FocusScreen() {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf(loadFocusSessions(context)) }

    var showNewSessionDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<FocusSession?>(null) }

    var runningSession by remember { mutableStateOf<FocusSession?>(null) }
    var remainingSeconds by remember { mutableStateOf(0) }
    var isRunning by remember { mutableStateOf(false) }

    fun persist(newList: List<FocusSession>) {
        sessions = newList
        saveFocusSessions(context, newList)
    }

    // تایمر
    LaunchedEffect(key1 = runningSession?.id, key2 = isRunning) {
        if (runningSession != null && isRunning) {
            while (isRunning && remainingSeconds > 0) {
                delay(1000)
                remainingSeconds -= 1
            }
            if (isRunning && remainingSeconds <= 0 && runningSession != null) {
                val finished = runningSession!!
                val actualMinutes = finished.plannedMinutes
                val updatedSession = finished.copy(
                    actualMinutes = actualMinutes,
                    successful = true
                )
                persist(sessions + updatedSession)
                runningSession = null
                isRunning = false
            }
        }
    }

    val today = todayDate()
    val todaySessions = sessions.filter { it.date == today }
    val totalMinutesToday = todaySessions.sumOf { it.actualMinutes }
    val successfulCount = todaySessions.count { it.successful }
    val totalCount = todaySessions.size
    val successPercent = if (totalCount == 0) 0 else (successfulCount * 100 / totalCount)

    val last7 = last7DaysFocusSummary(sessions)

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // خلاصه امروز
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "تمرکز امروز",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تاریخ: $today",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SummaryItem("دقایق تمرکز", "$totalMinutesToday")
                            SummaryItem("تعداد جلسات", "$totalCount")
                            SummaryItem("موفقیت", "$successPercent٪")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // شروع جلسه تمرکز
                FocusStarterCard(
                    onStart = { title, minutes ->
                        if (minutes <= 0) return@FocusStarterCard
                        runningSession = FocusSession(
                            id = System.currentTimeMillis(),
                            title = title.ifBlank { "جلسه تمرکز" },
                            plannedMinutes = minutes,
                            actualMinutes = 0,
                            date = today,
                            successful = false
                        )
                        remainingSeconds = minutes * 60
                        isRunning = true
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // جلسه در حال اجرا
                if (runningSession != null) {
                    RunningSessionCard(
                        session = runningSession!!,
                        remainingSeconds = remainingSeconds,
                        isRunning = isRunning,
                        onToggle = { isRunning = !isRunning },
                        onFinishEarly = {
                            val finished = runningSession!!
                            val elapsedMinutes =
                                finished.plannedMinutes - (remainingSeconds / 60)
                            val actual = if (elapsedMinutes <= 0) 1 else elapsedMinutes
                            val updated = finished.copy(
                                actualMinutes = actual,
                                successful = actual >= finished.plannedMinutes * 0.7
                            )
                            persist(sessions + updated)
                            runningSession = null
                            isRunning = false
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "جلسات تمرکز امروز",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (todaySessions.isEmpty()) {
                    Text(
                        text = "امروز هنوز جلسه تمرکزی ثبت نشده.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(todaySessions, key = { it.id }) { s ->
                            FocusSessionRow(
                                session = s,
                                onEdit = {
                                    editingSession = s
                                    showNewSessionDialog = true
                                },
                                onDelete = {
                                    val updated = sessions.filterNot { it.id == s.id }
                                    persist(updated)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // نمودار ۷ روز اخیر
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "نمودار تمرکز ۷ روز اخیر",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LineChart(data = last7)
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    editingSession = null
                    showNewSessionDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "ثبت جلسه دستی"
                )
            }
        }
    }

    if (showNewSessionDialog) {
        FocusSessionDialog(
            initial = editingSession,
            onDismiss = { showNewSessionDialog = false },
            onSave = { newSession ->
                val updated = if (editingSession == null) {
                    sessions + newSession
                } else {
                    sessions.map { if (it.id == newSession.id) newSession else it }
                }
                persist(updated)
                showNewSessionDialog = false
            }
        )
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun FocusStarterCard(
    onStart: (String, Int) -> Unit
) {
    var customMinutesText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "شروع جلسه تمرکز",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "یک مدت زمان انتخاب کن و فقط روی یک کار تمرکز کن.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = titleText,
                onValueChange = { titleText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("عنوان جلسه (مثلاً: مطالعه، کدنویسی)") }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FocusPresetButton(minutes = 25, title = titleText, onStart = onStart)
                FocusPresetButton(minutes = 45, title = titleText, onStart = onStart)
                FocusPresetButton(minutes = 60, title = titleText, onStart = onStart)
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = customMinutesText,
                onValueChange = {
                    customMinutesText = it.filter { ch -> ch.isDigit() }.take(3)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("مدت دلخواه (دقیقه)") },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(4.dp))

            FilledTonalButton(
                onClick = {
                    val m = customMinutesText.toIntOrNull() ?: 0
                    if (m > 0) {
                        onStart(titleText, m)
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("شروع جلسه دلخواه")
            }
        }
    }
}

@Composable
private fun FocusPresetButton(
    minutes: Int,
    title: String,
    onStart: (String, Int) -> Unit
) {
    FilledTonalButton(
        onClick = { onStart(title, minutes) },
        modifier = Modifier.weight(1f)
    ) {
        Text("${minutes} دقیقه")
    }
}

@Composable
private fun RunningSessionCard(
    session: FocusSession,
    remainingSeconds: Int,
    isRunning: Boolean,
    onToggle: () -> Unit,
    onFinishEarly: () -> Unit
) {
    val totalSeconds = session.plannedMinutes * 60
    val elapsed = totalSeconds - remainingSeconds
    val progress = if (totalSeconds > 0) elapsed.toFloat() / totalSeconds.toFloat() else 0f
    val minutesLeft = remainingSeconds / 60
    val secondsLeft = remainingSeconds % 60

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "جلسه در حال اجرا",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = session.title,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "زمان باقی‌مانده: %02d:%02d".format(minutesLeft, secondsLeft),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "پیشرفت: ${(progress * 100).toInt()}٪",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onToggle,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isRunning) "توقف موقت" else "ادامه")
                }
                FilledTonalButton(
                    onClick = onFinishEarly,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تمام شد / ذخیره")
                }
            }
        }
    }
}

@Composable
private fun FocusSessionRow(
    session: FocusSession,
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = session.title.ifBlank { "جلسه تمرکز" },
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "برنامه: ${session.plannedMinutes} دقیقه • انجام‌شده: ${session.actualMinutes} دقیقه",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (session.successful) "نتیجه: موفق ✅" else "نتیجه: ناقص ⏳",
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
private fun FocusSessionDialog(
    initial: FocusSession?,
    onDismiss: () -> Unit,
    onSave: (FocusSession) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var plannedText by remember { mutableStateOf(initial?.plannedMinutes?.toString() ?: "25") }
    var actualText by remember { mutableStateOf(initial?.actualMinutes?.toString() ?: "0") }
    var successful by remember { mutableStateOf(initial?.successful ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initial == null) "جلسه تمرکز جدید" else "ویرایش جلسه تمرکز")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان جلسه") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = plannedText,
                    onValueChange = {
                        plannedText = it.filter { ch -> ch.isDigit() }.ifBlank { "25" }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("مدت برنامه‌ریزی‌شده (دقیقه)") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = actualText,
                    onValueChange = {
                        actualText = it.filter { ch -> ch.isDigit() }.ifBlank { "0" }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("مدت انجام‌شده (دقیقه)") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = successful,
                        onCheckedChange = { successful = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("جلسه موفق بوده است")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val id = initial?.id ?: System.currentTimeMillis()
                    val planned = plannedText.toIntOrNull() ?: 25
                    val actual = actualText.toIntOrNull() ?: 0
                    onSave(
                        FocusSession(
                            id = id,
                            title = title.trim(),
                            plannedMinutes = if (planned <= 0) 25 else planned,
                            actualMinutes = if (actual < 0) 0 else actual,
                            date = initial?.date ?: todayDate(),
                            successful = successful
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
private fun LineChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
) {
    if (data.isEmpty()) {
        Text(
            text = "هنوز داده‌ای برای نمودار نیست.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        return
    }

    val maxValue = data.maxOf { it.second }.takeIf { it > 0 } ?: 10
    val points = data.map { it.second.toFloat() }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height
            val stepX = if (points.size <= 1) width else width / (points.size - 1)
            val maxY = maxValue.toFloat()

            val path = Path()
            points.forEachIndexed { index, value ->
                val x = stepX * index
                val normalized = value / maxY
                val y = height - (height * normalized)
                val point = Offset(x, y)
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = path,
                color = MaterialTheme.colorScheme.primary,
                style = Stroke(width = 4f)
            )

            points.forEachIndexed { index, value ->
                val x = stepX * index
                val normalized = value / maxY
                val y = height - (height * normalized)
                drawCircle(
                    color = MaterialTheme.colorScheme.primary,
                    radius = 6f,
                    center = Offset(x, y)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
cat >> app/src/main/java/com/taha/planer/features/focus/FocusScreen.kt << 'EOF'

@Composable
private fun LineChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
) {
    if (data.isEmpty()) {
        Text(
            text = "هنوز داده‌ای برای نمودار نیست.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        return
    }

    val maxValue = data.maxOf { it.second }.takeIf { it > 0 } ?: 10
    val points = data.map { it.second.toFloat() }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height
            val stepX = if (points.size <= 1) width else width / (points.size - 1)
            val maxY = maxValue.toFloat()

            val path = Path()
            points.forEachIndexed { index, value ->
                val x = stepX * index
                val normalized = value / maxY
                val y = height - (height * normalized)
                val point = Offset(x, y)
                if (index == 0) {
                    path.moveTo(point.x, point.y)
                } else {
                    path.lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = path,
                color = MaterialTheme.colorScheme.primary,
                style = Stroke(width = 4f)
            )

            points.forEachIndexed { index, value ->
                val x = stepX * index
                val normalized = value / maxY
                val y = height - (height * normalized)
                drawCircle(
                    color = MaterialTheme.colorScheme.primary,
                    radius = 6f,
                    center = Offset(x, y)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
