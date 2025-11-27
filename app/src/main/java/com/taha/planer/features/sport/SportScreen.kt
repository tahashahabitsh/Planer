package com.taha.planer.features.sport

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val MILLIS_PER_DAY = 86_400_000L

private const val PREF_SPORT_GOALS = "planner_sport_goals"
private const val KEY_SPORT_GOALS = "sport_goals_v1"

private const val PREF_SPORT_SESSIONS = "planner_sport_sessions"
private const val KEY_SPORT_SESSIONS = "sport_sessions_v1"

data class SportGoal(
    val id: Long,
    val name: String,
    val targetSessionsPerWeek: Int,
    val targetMinutesPerSession: Int
)

data class SportSession(
    val id: Long,
    val dayIndex: Int,
    val goalId: Long?,
    val minutes: Int,
    val intensity: Int // ۱ تا ۵
)

@Composable
fun SportScreen() {
    val context = LocalContext.current
    var goals by remember { mutableStateOf(loadSportGoals(context)) }
    var sessions by remember { mutableStateOf(loadSportSessions(context)) }

    fun persistGoals(updated: List<SportGoal>) {
        goals = updated
        saveSportGoals(context, updated)
    }

    fun persistSessions(updated: List<SportSession>) {
        sessions = updated
        saveSportSessions(context, updated)
    }

    val todayIndex = currentDayIndex()
    val last7Indices = (todayIndex - 6..todayIndex).toList()

    val sessionsLast7 = sessions.filter { it.dayIndex in last7Indices }

    val totalMinutesWeek = sessionsLast7.sumOf { it.minutes }
    val totalSessionsWeek = sessionsLast7.size

    val weeklyMinutesByDay: List<Int> =
        last7Indices.map { day ->
            sessions.filter { it.dayIndex == day }.sumOf { it.minutes }
        }

    var showGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SportGoal?>(null) }

    var draftName by remember { mutableStateOf("") }
    var draftTargetSessions by remember { mutableStateOf("3") }
    var draftTargetMinutes by remember { mutableStateOf("30") }

    fun openNewGoal() {
        editingGoal = null
        draftName = ""
        draftTargetSessions = "3"
        draftTargetMinutes = "30"
        showGoalDialog = true
    }

    fun openEditGoal(goal: SportGoal) {
        editingGoal = goal
        draftName = goal.name
        draftTargetSessions = goal.targetSessionsPerWeek.toString()
        draftTargetMinutes = goal.targetMinutesPerSession.toString()
        showGoalDialog = true
    }

    var showSessionDialog by remember { mutableStateOf(false) }
    var selectedGoalForSession by remember { mutableStateOf<SportGoal?>(null) }
    var draftSessionMinutes by remember { mutableStateOf("30") }
    var draftIntensity by remember { mutableStateOf("3") }

    fun openSessionDialog(goal: SportGoal?) {
        selectedGoalForSession = goal
        draftSessionMinutes = goal?.targetMinutesPerSession?.toString() ?: "30"
        draftIntensity = "3"
        showSessionDialog = true
    }

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
                    text = "خلاصه‌ی هفته‌ی ورزشی",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "این هفته $totalSessionsWeek جلسه، مجموعاً $totalMinutesWeek دقیقه ورزش ثبت شده.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                WeeklyMinutesChart(weeklyMinutesByDay)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "برنامه‌های ورزشی",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "برای هر برنامه هدف تعداد جلسه و زمان هر جلسه را تعیین کن.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(goals, key = { it.id }) { goal ->
                val goalSessionsThisWeek = sessionsLast7.filter { it.goalId == goal.id }
                val sessionsCount = goalSessionsThisWeek.size
                val minutesCount = goalSessionsThisWeek.sumOf { it.minutes }

                SportGoalCard(
                    goal = goal,
                    sessionsThisWeek = sessionsCount,
                    minutesThisWeek = minutesCount,
                    onQuickSession = {
                        val s = SportSession(
                            id = System.currentTimeMillis(),
                            dayIndex = todayIndex,
                            goalId = goal.id,
                            minutes = goal.targetMinutesPerSession,
                            intensity = 3
                        )
                        persistSessions(sessions + s)
                    },
                    onAddSession = { openSessionDialog(goal) },
                    onEdit = { openEditGoal(goal) },
                    onDelete = {
                        persistGoals(goals.filterNot { it.id == goal.id })
                        persistSessions(sessions.filterNot { it.goalId == goal.id })
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { openSessionDialog(null) }) {
                Text("ثبت جلسه‌ی آزاد (بدون برنامه)")
            }
            FloatingActionButton(onClick = { openNewGoal() }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "برنامه جدید")
            }
        }
    }

    if (showGoalDialog) {
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = {
                Text(
                    text = if (editingGoal == null) "برنامه‌ی ورزشی جدید"
                    else "ویرایش برنامه‌ی ورزشی"
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("نام برنامه (مثلاً: تمرین باشگاه، دویدن)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = draftTargetSessions,
                            onValueChange = { draftTargetSessions = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("هدف جلسه در هفته") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = draftTargetMinutes,
                            onValueChange = { draftTargetMinutes = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("دقیقه هر جلسه") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = draftName.trim()
                        val targetSessions = draftTargetSessions.toIntOrNull() ?: 0
                        val targetMinutes = draftTargetMinutes.toIntOrNull() ?: 0
                        if (name.isNotEmpty() && targetSessions > 0 && targetMinutes > 0) {
                            val goal = SportGoal(
                                id = editingGoal?.id ?: System.currentTimeMillis(),
                                name = name,
                                targetSessionsPerWeek = targetSessions,
                                targetMinutesPerSession = targetMinutes
                            )
                            val updated =
                                if (editingGoal == null) goals + goal
                                else goals.map { if (it.id == goal.id) goal else it }
                            persistGoals(updated)
                            showGoalDialog = false
                        }
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("بی‌خیال")
                }
            }
        )
    }

    if (showSessionDialog) {
        AlertDialog(
            onDismissRequest = { showSessionDialog = false },
            title = {
                Text(
                    text = selectedGoalForSession?.let { "ثبت جلسه برای ${it.name}" }
                        ?: "ثبت جلسه آزاد"
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftSessionMinutes,
                        onValueChange = { draftSessionMinutes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("مدت جلسه (دقیقه)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = draftIntensity,
                        onValueChange = { draftIntensity = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("شدت از ۱ تا ۵ (اختیاری)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = draftSessionMinutes.toIntOrNull() ?: 0
                        val intensity = draftIntensity.toIntOrNull() ?: 3
                        if (minutes > 0) {
                            val s = SportSession(
                                id = System.currentTimeMillis(),
                                dayIndex = todayIndex,
                                goalId = selectedGoalForSession?.id,
                                minutes = minutes,
                                intensity = intensity.coerceIn(1, 5)
                            )
                            persistSessions(sessions + s)
                            showSessionDialog = false
                        }
                    }
                ) {
                    Text("ثبت")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSessionDialog = false }) {
                    Text("بی‌خیال")
                }
            }
        )
    }
}

@Composable
private fun SportGoalCard(
    goal: SportGoal,
    sessionsThisWeek: Int,
    minutesThisWeek: Int,
    onQuickSession: () -> Unit,
    onAddSession: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val totalTargetMinutes =
        goal.targetSessionsPerWeek * goal.targetMinutesPerSession
    val progressMinutes =
        if (totalTargetMinutes > 0)
            (minutesThisWeek.toFloat() / totalTargetMinutes.toFloat()).coerceIn(0f, 1f)
        else 0f

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
                        text = goal.name,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "هدف: ${goal.targetSessionsPerWeek} جلسه در هفته، هر کدام ${goal.targetMinutesPerSession} دقیقه",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "این هفته: $sessionsThisWeek جلسه، $minutesThisWeek دقیقه",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(onClick = onQuickSession) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "ثبت جلسه به‌صورت سریع"
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "ویرایش برنامه"
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "حذف برنامه"
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = progressMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(onClick = onAddSession) {
                Text("ثبت جلسه‌ی دلخواه برای این برنامه")
            }
        }
    }
}

@Composable
private fun WeeklyMinutesChart(points: List<Int>) {
    val maxVal = points.maxOrNull() ?: 0
    val safeMax = if (maxVal <= 0) 1 else maxVal

    Column {
        Text(
            text = "نمودار خطی دقیقه‌های ورزش در ۷ روز اخیر",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

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
                val x = stepX * index
                val ratio = value.toFloat() / safeMax.toFloat()
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
    }
}

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

// ---------- ذخیره / لود برنامه‌ها ----------

private fun loadSportGoals(context: Context): List<SportGoal> {
    val prefs = context.getSharedPreferences(PREF_SPORT_GOALS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SPORT_GOALS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 4) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val name = parts[1]
            val targetSessions = parts[2].toIntOrNull() ?: 0
            val targetMinutes = parts[3].toIntOrNull() ?: 0
            SportGoal(
                id = id,
                name = name,
                targetSessionsPerWeek = targetSessions,
                targetMinutesPerSession = targetMinutes
            )
        }
}

private fun saveSportGoals(context: Context, goals: List<SportGoal>) {
    val prefs = context.getSharedPreferences(PREF_SPORT_GOALS, Context.MODE_PRIVATE)
    val raw = goals.joinToString("\n") { g ->
        val safeName = g.name.replace("\n", " ")
        "${g.id}||$safeName||${g.targetSessionsPerWeek}||${g.targetMinutesPerSession}"
    }
    prefs.edit().putString(KEY_SPORT_GOALS, raw).apply()
}

// ---------- ذخیره / لود جلسات ----------

private fun loadSportSessions(context: Context): List<SportSession> {
    val prefs = context.getSharedPreferences(PREF_SPORT_SESSIONS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SPORT_SESSIONS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 5) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val dayIndex = parts[1].toIntOrNull() ?: return@mapNotNull null
            val goalIdRaw = parts[2]
            val goalId = if (goalIdRaw == "null") null else goalIdRaw.toLongOrNull()
            val minutes = parts[3].toIntOrNull() ?: 0
            val intensity = parts[4].toIntOrNull() ?: 3
            SportSession(
                id = id,
                dayIndex = dayIndex,
                goalId = goalId,
                minutes = minutes,
                intensity = intensity
            )
        }
}

private fun saveSportSessions(context: Context, sessions: List<SportSession>) {
    val prefs = context.getSharedPreferences(PREF_SPORT_SESSIONS, Context.MODE_PRIVATE)
    val raw = sessions.joinToString("\n") { s ->
        val goalId = s.goalId?.toString() ?: "null"
        "${s.id}||${s.dayIndex}||$goalId||${s.minutes}||${s.intensity}"
    }
    prefs.edit().putString(KEY_SPORT_SESSIONS, raw).apply()
}
