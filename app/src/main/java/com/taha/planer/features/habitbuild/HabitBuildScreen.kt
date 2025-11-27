package com.taha.planer.features.habitbuild

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private const val MILLIS_PER_DAY = 86_400_000L

private const val PREF_HABIT_BUILD = "planner_habit_build"
private const val KEY_PROGRAMS = "habit_build_programs_v1"
private const val KEY_CHECKS = "habit_build_checks_v1"

data class HabitProgram(
    val id: Long,
    val title: String,
    val why: String,
    val trigger: String,
    val action: String,
    val reward: String,
    val startDayIndex: Int,
    val targetDays: Int,
    val difficulty: Int, // 1 = خیلی کوچک، 2 = متوسط، 3 = چالش‌برانگیز
    val isArchived: Boolean
)

data class HabitProgramCheck(
    val id: Long,
    val programId: Long,
    val dayIndex: Int,
    val note: String
)

@Composable
fun HabitBuildScreen() {
    val context = LocalContext.current
    var programs by remember { mutableStateOf(loadHabitPrograms(context)) }
    var checks by remember { mutableStateOf(loadHabitProgramChecks(context)) }

    fun persist(
        newPrograms: List<HabitProgram> = programs,
        newChecks: List<HabitProgramCheck> = checks
    ) {
        programs = newPrograms
        checks = newChecks
        saveHabitPrograms(context, newPrograms)
        saveHabitProgramChecks(context, newChecks)
    }

    val todayIndex = currentDayIndex()

    val activePrograms = programs.filter { !it.isArchived }
    val todayPlanned = activePrograms.count { isProgramActiveToday(it, todayIndex) }
    val todayDone = activePrograms.count { p ->
        checks.any { it.programId == p.id && it.dayIndex == todayIndex }
    }

    val last7Indices = (todayIndex - 6..todayIndex).toList()
    val chartPoints = last7Indices.map { day ->
        checks.count { it.dayIndex == day }
    }

    var showArchived by remember { mutableStateOf(false) }

    var showProgramDialog by remember { mutableStateOf(false) }
    var editingProgram by remember { mutableStateOf<HabitProgram?>(null) }

    var showCheckInDialog by remember { mutableStateOf(false) }
    var checkInProgram by remember { mutableStateOf<HabitProgram?>(null) }

    val visiblePrograms = programs
        .filter { if (!showArchived) !it.isArchived else true }
        .sortedWith(
            compareBy<HabitProgram> { it.isArchived }
                .thenByDescending { it.difficulty }
                .thenBy { it.startDayIndex }
        )

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // خلاصه و نمودار ۷ روز اخیر
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "ساخت عادت (برنامه‌های ۳۰ روزه)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "برنامه‌های فعال: ${activePrograms.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "عادت‌های برنامه‌ریزی‌شده برای امروز: $todayPlanned   •   انجام‌شده: $todayDone",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تعداد روزهایی که در ۷ روز اخیر روی عادت‌های جدید کار کردی",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    HabitBuildLineChart(points = chartPoints)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { showArchived = !showArchived }
                ) {
                    Text(
                        text = if (showArchived) "نمایش فقط برنامه‌های فعال" else "نمایش برنامه‌های آرشیوشده هم"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "برنامه‌های ساخت عادت",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (visiblePrograms.isEmpty()) {
                Text(
                    text = "هنوز برنامه‌ای نساختی. روی + بزن و اولین عادت را با چرایی، تریگر، عمل و پاداش طراحی کن.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(visiblePrograms, key = { it.id }) { program ->
                        val programChecks = checks.filter { it.programId == program.id }
                        val progress = calculateProgramProgress(program, programChecks)
                        val todayDoneForProgram =
                            programChecks.any { it.dayIndex == todayIndex }
                        HabitProgramRow(
                            program = program,
                            todayIndex = todayIndex,
                            progressPercent = progress,
                            todayDone = todayDoneForProgram,
                            totalDoneDays = countDistinctDays(programChecks),
                            onCheckInToday = {
                                checkInProgram = program
                                showCheckInDialog = true
                            },
                            onToggleArchive = {
                                val updated = program.copy(isArchived = !program.isArchived)
                                val newPrograms = programs.map {
                                    if (it.id == program.id) updated else it
                                }
                                persist(newPrograms, checks)
                            },
                            onEditProgram = {
                                editingProgram = program
                                showProgramDialog = true
                            },
                            onDeleteProgram = {
                                val newPrograms =
                                    programs.filterNot { it.id == program.id }
                                val newChecks =
                                    checks.filterNot { it.programId == program.id }
                                persist(newPrograms, newChecks)
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
                        editingProgram = null
                        showProgramDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "برنامه‌ی جدید ساخت عادت"
                    )
                }
            }
        }
    }

    if (showProgramDialog) {
        HabitProgramDialog(
            initial = editingProgram,
            onDismiss = { showProgramDialog = false },
            onSave = { newProgram ->
                val updated = if (editingProgram == null) {
                    programs + newProgram
                } else {
                    programs.map { if (it.id == newProgram.id) newProgram else it }
                }
                persist(updated, checks)
                showProgramDialog = false
            }
        )
    }

    if (showCheckInDialog && checkInProgram != null) {
        CheckInDialog(
            program = checkInProgram!!,
            todayIndex = todayIndex,
            onDismiss = {
                showCheckInDialog = false
                checkInProgram = null
            },
            onSave = { note ->
                val filtered = checks.filterNot {
                    it.programId == checkInProgram!!.id && it.dayIndex == todayIndex
                }
                val newCheck = HabitProgramCheck(
                    id = System.currentTimeMillis(),
                    programId = checkInProgram!!.id,
                    dayIndex = todayIndex,
                    note = note.trim()
                )
                persist(programs, filtered + newCheck)
                showCheckInDialog = false
                checkInProgram = null
            }
        )
    }
}
@Composable
private fun HabitProgramRow(
    program: HabitProgram,
    todayIndex: Int,
    progressPercent: Int,
    todayDone: Boolean,
    totalDoneDays: Int,
    onCheckInToday: () -> Unit,
    onToggleArchive: () -> Unit,
    onEditProgram: () -> Unit,
    onDeleteProgram: () -> Unit
) {
    val dayNumber = dayNumberWithinProgram(program, todayIndex)
    val statusLabel = when {
        dayNumber <= 0 -> "برنامه از امروز شروع می‌شود (یا تازه شروع شده)."
        dayNumber > program.targetDays ->
            "برنامه تمام شده است. می‌توانی آن را آرشیو کنی یا تمدیدش کنی."
        else -> "روز $dayNumber از ${program.targetDays}"
    }

    val difficultyLabel = when (program.difficulty) {
        1 -> "شدت: خیلی کوچک (آسون برای شروع)"
        3 -> "شدت: چالش‌برانگیز"
        else -> "شدت: متوسط"
    }

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
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (program.title.isNotBlank()) program.title else "برنامه‌ی بدون عنوان",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = difficultyLabel,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (program.isArchived) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "این برنامه در حالت آرشیو است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$progressPercent%",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = (progressPercent.coerceIn(0, 100) / 100f),
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "روزهای انجام‌شده: $totalDoneDays",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "چرایی عادت:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (program.why.isNotBlank()) program.why else "هنوز چرایی مشخص نشده.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "تریگر (محرک) → عمل → پاداش:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "تریگر: ${program.trigger.ifBlank { "نامشخص" }}\n" +
                        "عمل: ${program.action.ifBlank { "نامشخص" }}\n" +
                        "پاداش: ${program.reward.ifBlank { "نامشخص" }}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = todayDone,
                    onCheckedChange = { onCheckInToday() }
                )
                Text(
                    text = if (todayDone) "امروز این عادت را انجام دادی ✅" else "امروز انجام شد؟ تیک بزن و یادداشت بگذار.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onCheckInToday) {
                    Text(if (todayDone) "ویرایش ثبت امروز" else "ثبت امروز")
                }
                TextButton(onClick = onEditProgram) {
                    Text("ویرایش برنامه")
                }
                TextButton(onClick = onDeleteProgram) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف")
                }
                TextButton(onClick = onToggleArchive) {
                    Icon(
                        imageVector = if (program.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (program.isArchived) "فعال‌سازی" else "آرشیو")
                }
            }
        }
    }
}

@Composable
private fun HabitProgramDialog(
    initial: HabitProgram?,
    onDismiss: () -> Unit,
    onSave: (HabitProgram) -> Unit
) {
    val todayIndex = currentDayIndex()

    var draftTitle by remember { mutableStateOf(initial?.title ?: "") }
    var draftWhy by remember { mutableStateOf(initial?.why ?: "") }
    var draftTrigger by remember { mutableStateOf(initial?.trigger ?: "") }
    var draftAction by remember { mutableStateOf(initial?.action ?: "") }
    var draftReward by remember { mutableStateOf(initial?.reward ?: "") }
    var draftTargetDays by remember { mutableStateOf(initial?.targetDays ?: 30) }
    var draftDifficulty by remember { mutableStateOf(initial?.difficulty ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "برنامه‌ی جدید ساخت عادت" else "ویرایش برنامه",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان عادت (مثلا: ورزش ۱۰ دقیقه‌ای)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftWhy,
                    onValueChange = { draftWhy = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    label = { Text("چرایی این عادت برای تو چیست؟") }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "حلقه‌ی عادت:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = draftTrigger,
                    onValueChange = { draftTrigger = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("تریگر (مثلا: بعد از مسواک صبح)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = draftAction,
                    onValueChange = { draftAction = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عمل (مثلا: ۵ دقیقه حرکات کششی)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = draftReward,
                    onValueChange = { draftReward = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("پاداش (مثلا: یک لیوان چای موردعلاقه‌ات)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "شدت عادت:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(onClick = { draftDifficulty = 1 }) {
                        Text(
                            text = "خیلی کوچک",
                            fontWeight = if (draftDifficulty == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    FilledTonalButton(onClick = { draftDifficulty = 2 }) {
                        Text(
                            text = "متوسط",
                            fontWeight = if (draftDifficulty == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    FilledTonalButton(onClick = { draftDifficulty = 3 }) {
                        Text(
                            text = "چالش‌برانگیز",
                            fontWeight = if (draftDifficulty == 3) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "مدت برنامه (روز):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(21, 30, 60).forEach { d ->
                        FilledTonalButton(onClick = { draftTargetDays = d }) {
                            Text(
                                text = "$d",
                                fontWeight = if (draftTargetDays == d) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedTextField(
                        value = draftTargetDays.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull()
                            if (v != null && v in 1..365) {
                                draftTargetDays = v
                            }
                        },
                        modifier = Modifier.width(100.dp),
                        singleLine = true,
                        label = { Text("دلخواه") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val title = draftTitle.trim()
                    val why = draftWhy.trim()
                    if (title.isEmpty()) return@TextButton
                    val program = HabitProgram(
                        id = initial?.id ?: System.currentTimeMillis(),
                        title = title,
                        why = why,
                        trigger = draftTrigger.trim(),
                        action = draftAction.trim(),
                        reward = draftReward.trim(),
                        startDayIndex = initial?.startDayIndex ?: todayIndex,
                        targetDays = draftTargetDays.coerceIn(1, 365),
                        difficulty = draftDifficulty.coerceIn(1, 3),
                        isArchived = initial?.isArchived ?: false
                    )
                    onSave(program)
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
private fun CheckInDialog(
    program: HabitProgram,
    todayIndex: Int,
    onDismiss: () -> Unit,
    onSave: (note: String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    val dayNumber = dayNumberWithinProgram(program, todayIndex).coerceAtLeast(1)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "ثبت امروز برای «${program.title}»",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "امروز روز $dayNumber از ${program.targetDays} روز برنامه است. یک جمله کوتاه بنویس که چه حسی داشتی.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    label = { Text("یادداشت (اختیاری)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(note) }
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
private fun HabitBuildLineChart(points: List<Int>) {
    val maxVal = points.maxOrNull() ?: 0
    val safeMax = if (maxVal <= 0) 1 else maxVal

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        if (points.isEmpty()) return@Canvas

        val stepX =
            if (points.size <= 1) size.width
            else size.width / (points.size - 1).toFloat()

        val path = Path()

        points.forEachIndexed { index, value ->
            val ratio = value.toFloat() / safeMax.toFloat()
            val x = stepX * index
            val y = size.height - (ratio * size.height)
            val p = Offset(x, y)
            if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }

        drawPath(
            path = path,
            color = MaterialTheme.colorScheme.primary,
            style = Stroke(width = 4f)
        )
    }

    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "۶ روز قبل",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Text(
            text = "امروز",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

private fun dayNumberWithinProgram(program: HabitProgram, todayIndex: Int): Int {
    return todayIndex - program.startDayIndex + 1
}

private fun isProgramActiveToday(program: HabitProgram, todayIndex: Int): Boolean {
    val dayNum = dayNumberWithinProgram(program, todayIndex)
    return dayNum in 1..program.targetDays
}

private fun countDistinctDays(checks: List<HabitProgramCheck>): Int {
    return checks.map { it.dayIndex }.toSet().size
}

private fun calculateProgramProgress(
    program: HabitProgram,
    checks: List<HabitProgramCheck>
): Int {
    if (program.targetDays <= 0) return 0
    val doneDays = countDistinctDays(checks)
    val ratio = doneDays.toFloat() / program.targetDays.toFloat()
    return (ratio * 100f).toInt().coerceIn(0, 100)
}

// ---------- ذخیره‌سازی در SharedPreferences ----------

private fun loadHabitPrograms(context: Context): List<HabitProgram> {
    val prefs = context.getSharedPreferences(PREF_HABIT_BUILD, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_PROGRAMS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 10) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val startDay = parts[1].toIntOrNull() ?: return@mapNotNull null
            val targetDays = parts[2].toIntOrNull() ?: 30
            val difficulty = parts[3].toIntOrNull() ?: 1
            val archived = parts[4] == "1"
            val title = parts[5]
            val why = parts[6]
            val trigger = parts[7]
            val action = parts[8]
            val reward = parts[9]
            HabitProgram(
                id = id,
                title = title,
                why = why,
                trigger = trigger,
                action = action,
                reward = reward,
                startDayIndex = startDay,
                targetDays = targetDays,
                difficulty = difficulty,
                isArchived = archived
            )
        }
}

private fun saveHabitPrograms(context: Context, programs: List<HabitProgram>) {
    val prefs = context.getSharedPreferences(PREF_HABIT_BUILD, Context.MODE_PRIVATE)
    val raw = programs.joinToString("\n") { p ->
        val safeTitle = p.title.replace("\n", " ")
        val safeWhy = p.why.replace("\n", " ")
        val safeTrigger = p.trigger.replace("\n", " ")
        val safeAction = p.action.replace("\n", " ")
        val safeReward = p.reward.replace("\n", " ")
        "${p.id}||${p.startDayIndex}||${p.targetDays}||${p.difficulty}||${if (p.isArchived) "1" else "0"}||" +
                "$safeTitle||$safeWhy||$safeTrigger||$safeAction||$safeReward"
    }
    prefs.edit().putString(KEY_PROGRAMS, raw).apply()
}

private fun loadHabitProgramChecks(context: Context): List<HabitProgramCheck> {
    val prefs = context.getSharedPreferences(PREF_HABIT_BUILD, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_CHECKS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 4) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val programId = parts[1].toLongOrNull() ?: return@mapNotNull null
            val dayIndex = parts[2].toIntOrNull() ?: return@mapNotNull null
            val note = parts[3]
            HabitProgramCheck(
                id = id,
                programId = programId,
                dayIndex = dayIndex,
                note = note
            )
        }
}

private fun saveHabitProgramChecks(context: Context, checks: List<HabitProgramCheck>) {
    val prefs = context.getSharedPreferences(PREF_HABIT_BUILD, Context.MODE_PRIVATE)
    val raw = checks.joinToString("\n") { c ->
        val safeNote = c.note.replace("\n", " ")
        "${c.id}||${c.programId}||${c.dayIndex}||$safeNote"
    }
    prefs.edit().putString(KEY_CHECKS, raw).apply()
}
