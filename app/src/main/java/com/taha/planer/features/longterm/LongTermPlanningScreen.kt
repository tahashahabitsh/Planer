package com.taha.planer.features.longterm

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import java.util.Calendar

private const val PREF_LONG_TERM = "planner_long_term"
private const val KEY_GOALS = "long_term_goals_v1"
private const val KEY_MILESTONES = "long_term_milestones_v1"

data class LongTermGoal(
    val id: Long,
    val title: String,
    val description: String,
    val category: GoalCategory,
    val targetYear: Int,
    val targetQuarter: Int, // 1..4
    val priority: Int, // 1=کم،2=متوسط،3=بالا
    val isArchived: Boolean
)

data class GoalMilestone(
    val id: Long,
    val goalId: Long,
    val title: String,
    val isDone: Boolean
)

enum class GoalCategory(val code: Int, val label: String) {
    HEALTH(0, "سلامت"),
    CAREER(1, "شغل / بیزنس"),
    LEARNING(2, "یادگیری / مهارت"),
    FINANCE(3, "مالی"),
    RELATIONSHIPS(4, "روابط"),
    PERSONAL(5, "زندگی شخصی / سبک زندگی");

    companion object {
        fun fromCode(code: Int): GoalCategory =
            values().find { it.code == code } ?: PERSONAL
    }
}

private fun currentYearQuarter(): Pair<Int, Int> {
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) // 0..11
    val quarter = (month / 3) + 1 // 1..4
    return year to quarter
}

@Composable
fun LongTermPlanningScreen() {
    val context = LocalContext.current
    var goals by remember { mutableStateOf(loadLongTermGoals(context)) }
    var milestones by remember { mutableStateOf(loadGoalMilestones(context)) }

    fun persist(
        newGoals: List<LongTermGoal> = goals,
        newMilestones: List<GoalMilestone> = milestones
    ) {
        goals = newGoals
        milestones = newMilestones
        saveLongTermGoals(context, newGoals)
        saveGoalMilestones(context, newMilestones)
    }

    val (currentYear, currentQuarter) = currentYearQuarter()

    val activeGoals = goals.filter { !it.isArchived }
    val currentYearGoals = activeGoals.filter { it.targetYear == currentYear }
    val avgProgress = if (currentYearGoals.isNotEmpty()) {
        currentYearGoals.map { calculateGoalProgress(it, milestones) }.average().toInt()
    } else 0

    val quarterPoints = (1..4).map { q ->
        val goalsInQuarter = activeGoals.filter { it.targetYear == currentYear && it.targetQuarter == q }
        if (goalsInQuarter.isEmpty()) 0
        else goalsInQuarter.map { calculateGoalProgress(it, milestones) }.average().toInt()
    }

    var showArchived by remember { mutableStateOf(false) }
    var onlyCurrentYear by remember { mutableStateOf(true) }

    var showGoalDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<LongTermGoal?>(null) }

    var showMilestoneDialog by remember { mutableStateOf(false) }
    var milestoneGoalId by remember { mutableStateOf<Long?>(null) }

    val visibleGoals = goals
        .filter { g -> if (!showArchived) !g.isArchived else true }
        .filter { g -> if (onlyCurrentYear) g.targetYear == currentYear else true }
        .sortedWith(
            compareByDescending<LongTermGoal> { it.priority }
                .thenBy { it.targetYear }
                .thenBy { it.targetQuarter }
        )

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // خلاصه
            ElevatedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "برنامه‌ریزی بلندمدت",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اهداف فعال: ${activeGoals.size}   •   اهداف امسال: ${currentYearGoals.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "میانگین پیشرفت اهداف امسال: $avgProgress%",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "پیشرفت فصل‌های سال $currentYear",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    QuarterProgressChart(points = quarterPoints, currentQuarter = currentQuarter)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // فیلترها
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { onlyCurrentYear = !onlyCurrentYear }
                ) {
                    Text(
                        text = if (onlyCurrentYear) "فقط اهداف $currentYear" else "نمایش همه سال‌ها"
                    )
                }

                FilledTonalButton(
                    onClick = { showArchived = !showArchived }
                ) {
                    Text(
                        text = if (showArchived) "همه (شامل آرشیو)" else "فقط اهداف فعال"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "لیست اهداف",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (visibleGoals.isEmpty()) {
                Text(
                    text = "هنوز هدف بلندمدتی ثبت نکردی. روی دکمه‌ی + بزن و اولین هدف سال را بساز.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(visibleGoals, key = { it.id }) { goal ->
                        val goalMilestones = milestones.filter { it.goalId == goal.id }
                        val progress = calculateGoalProgress(goal, milestones)
                        GoalRow(
                            goal = goal,
                            milestones = goalMilestones,
                            progressPercent = progress,
                            onToggleMilestone = { milestone ->
                                val updatedMilestone = milestone.copy(isDone = !milestone.isDone)
                                val newList = milestones.map {
                                    if (it.id == milestone.id) updatedMilestone else it
                                }
                                persist(goals, newList)
                            },
                            onAddMilestone = {
                                milestoneGoalId = goal.id
                                showMilestoneDialog = true
                            },
                            onEditGoal = {
                                editingGoal = goal
                                showGoalDialog = true
                            },
                            onDeleteGoal = {
                                val newGoals = goals.filterNot { it.id == goal.id }
                                val newMilestones = milestones.filterNot { it.goalId == goal.id }
                                persist(newGoals, newMilestones)
                            },
                            onToggleArchive = {
                                val updated = goal.copy(isArchived = !goal.isArchived)
                                val newGoals = goals.map { if (it.id == goal.id) updated else it }
                                persist(newGoals, milestones)
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
                        editingGoal = null
                        showGoalDialog = true
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "هدف جدید"
                    )
                }
            }
        }
    }

    if (showGoalDialog) {
        GoalDialog(
            initial = editingGoal,
            onDismiss = { showGoalDialog = false },
            onSave = { newGoal ->
                val updated = if (editingGoal == null) {
                    goals + newGoal
                } else {
                    goals.map { if (it.id == newGoal.id) newGoal else it }
                }
                persist(updated, milestones)
                showGoalDialog = false
            }
        )
    }

    if (showMilestoneDialog && milestoneGoalId != null) {
        MilestoneDialog(
            goalId = milestoneGoalId!!,
            onDismiss = {
                showMilestoneDialog = false
                milestoneGoalId = null
            },
            onSave = { newMilestone ->
                persist(goals, milestones + newMilestone)
                showMilestoneDialog = false
                milestoneGoalId = null
            }
        )
    }
}
@Composable
private fun GoalRow(
    goal: LongTermGoal,
    milestones: List<GoalMilestone>,
    progressPercent: Int,
    onToggleMilestone: (GoalMilestone) -> Unit,
    onAddMilestone: () -> Unit,
    onEditGoal: () -> Unit,
    onDeleteGoal: () -> Unit,
    onToggleArchive: () -> Unit
) {
    val quarterLabel = "فصل ${goal.targetQuarter} / ${goal.targetYear}"
    val priorityLabel = when (goal.priority) {
        3 -> "اولویت: بالا"
        2 -> "اولویت: متوسط"
        else -> "اولویت: کم"
    }

    val doneCount = milestones.count { it.isDone }
    val totalCount = milestones.size

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
                        text = if (goal.title.isNotBlank()) goal.title else "هدف بدون عنوان",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${goal.category.label} • $quarterLabel",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = priorityLabel,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (goal.isArchived) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "در وضعیت آرشیو",
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
                }
            }

            if (goal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "گام‌ها (${doneCount}/$totalCount انجام‌شده):",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (milestones.isEmpty()) {
                Text(
                    text = "هنوز گامی برای این هدف ثبت نشده. یک گام کوچک اضافه کن.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    milestones.forEach { m ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = m.isDone,
                                onCheckedChange = { onToggleMilestone(m) }
                            )
                            Text(
                                text = m.title,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onAddMilestone) {
                    Text("گام جدید")
                }
                TextButton(onClick = onEditGoal) {
                    Text("ویرایش هدف")
                }
                TextButton(onClick = onDeleteGoal) {
                    Text("حذف")
                }
                TextButton(onClick = onToggleArchive) {
                    Icon(
                        imageVector = if (goal.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (goal.isArchived) "فعال‌سازی" else "آرشیو")
                }
            }
        }
    }
}

@Composable
private fun GoalDialog(
    initial: LongTermGoal?,
    onDismiss: () -> Unit,
    onSave: (LongTermGoal) -> Unit
) {
    val (currentYear, currentQuarter) = currentYearQuarter()

    var draftTitle by remember { mutableStateOf(initial?.title ?: "") }
    var draftDesc by remember { mutableStateOf(initial?.description ?: "") }
    var draftCategory by remember { mutableStateOf(initial?.category ?: GoalCategory.PERSONAL) }
    var draftYear by remember { mutableStateOf(initial?.targetYear ?: currentYear) }
    var draftQuarter by remember { mutableStateOf(initial?.targetQuarter ?: currentQuarter) }
    var draftPriority by remember { mutableStateOf(initial?.priority ?: 2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "هدف جدید" else "ویرایش هدف",
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
                    label = { Text("عنوان هدف") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftDesc,
                    onValueChange = { draftDesc = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    label = { Text("توضیحات (اختیاری)") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "دسته‌ی هدف:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryChip("سلامت", draftCategory == GoalCategory.HEALTH) {
                            draftCategory = GoalCategory.HEALTH
                        }
                        CategoryChip("شغل / بیزنس", draftCategory == GoalCategory.CAREER) {
                            draftCategory = GoalCategory.CAREER
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryChip("یادگیری", draftCategory == GoalCategory.LEARNING) {
                            draftCategory = GoalCategory.LEARNING
                        }
                        CategoryChip("مالی", draftCategory == GoalCategory.FINANCE) {
                            draftCategory = GoalCategory.FINANCE
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryChip("روابط", draftCategory == GoalCategory.RELATIONSHIPS) {
                            draftCategory = GoalCategory.RELATIONSHIPS
                        }
                        CategoryChip("شخصی / سبک زندگی", draftCategory == GoalCategory.PERSONAL) {
                            draftCategory = GoalCategory.PERSONAL
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "زمان‌بندی هدف:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = draftYear.toString(),
                        onValueChange = {
                            val y = it.toIntOrNull()
                            if (y != null && y in 2000..2100) draftYear = y
                        },
                        modifier = Modifier.width(120.dp),
                        singleLine = true,
                        label = { Text("سال") }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "فصل:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.width(4.dp))
                    (1..4).forEach { q ->
                        FilledTonalButton(
                            onClick = { draftQuarter = q }
                        ) {
                            Text(
                                text = q.toString(),
                                fontWeight = if (draftQuarter == q) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "اولویت:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(onClick = { draftPriority = 1 }) {
                        Text(
                            text = "کم",
                            fontWeight = if (draftPriority == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    FilledTonalButton(onClick = { draftPriority = 2 }) {
                        Text(
                            text = "متوسط",
                            fontWeight = if (draftPriority == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    FilledTonalButton(onClick = { draftPriority = 3 }) {
                        Text(
                            text = "بالا",
                            fontWeight = if (draftPriority == 3) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val title = draftTitle.trim()
                    val desc = draftDesc.trim()
                    if (title.isEmpty()) return@TextButton
                    val goal = LongTermGoal(
                        id = initial?.id ?: System.currentTimeMillis(),
                        title = title,
                        description = desc,
                        category = draftCategory,
                        targetYear = draftYear,
                        targetQuarter = draftQuarter.coerceIn(1, 4),
                        priority = draftPriority.coerceIn(1, 3),
                        isArchived = initial?.isArchived ?: false
                    )
                    onSave(goal)
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
private fun CategoryChip(
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

@Composable
private fun MilestoneDialog(
    goalId: Long,
    onDismiss: () -> Unit,
    onSave: (GoalMilestone) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "گام جدید",
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
                    singleLine = false,
                    label = { Text("عنوان گام") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val t = title.trim()
                    if (t.isEmpty()) return@TextButton
                    val m = GoalMilestone(
                        id = System.currentTimeMillis(),
                        goalId = goalId,
                        title = t,
                        isDone = false
                    )
                    onSave(m)
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
private fun QuarterProgressChart(
    points: List<Int>,
    currentQuarter: Int
) {
    val maxVal = points.maxOrNull() ?: 0
    val safeMax = if (maxVal <= 0) 1 else maxVal

    Column {
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
            (1..4).forEach { q ->
                val label = "فصل $q"
                val isCurrent = q == currentQuarter
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else Color.Unspecified
                )
            }
        }
    }
}
private fun calculateGoalProgress(
    goal: LongTermGoal,
    milestones: List<GoalMilestone>
): Int {
    val ms = milestones.filter { it.goalId == goal.id }
    if (ms.isEmpty()) return 0
    val done = ms.count { it.isDone }
    return ((done.toFloat() / ms.size.toFloat()) * 100f).toInt()
}

private fun loadLongTermGoals(context: Context): List<LongTermGoal> {
    val prefs = context.getSharedPreferences(PREF_LONG_TERM, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_GOALS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 8) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val year = parts[1].toIntOrNull() ?: return@mapNotNull null
            val quarter = parts[2].toIntOrNull() ?: 1
            val priority = parts[3].toIntOrNull() ?: 2
            val archived = parts[4] == "1"
            val categoryCode = parts[5].toIntOrNull() ?: GoalCategory.PERSONAL.code
            val title = parts[6]
            val desc = parts[7]
            LongTermGoal(
                id = id,
                title = title,
                description = desc,
                category = GoalCategory.fromCode(categoryCode),
                targetYear = year,
                targetQuarter = quarter,
                priority = priority,
                isArchived = archived
            )
        }
}

private fun saveLongTermGoals(context: Context, goals: List<LongTermGoal>) {
    val prefs = context.getSharedPreferences(PREF_LONG_TERM, Context.MODE_PRIVATE)
    val raw = goals.joinToString("\n") { g ->
        val safeTitle = g.title.replace("\n", " ")
        val safeDesc = g.description.replace("\n", " ")
        "${g.id}||${g.targetYear}||${g.targetQuarter}||${g.priority}||${if (g.isArchived) "1" else "0"}||${g.category.code}||$safeTitle||$safeDesc"
    }
    prefs.edit().putString(KEY_GOALS, raw).apply()
}

private fun loadGoalMilestones(context: Context): List<GoalMilestone> {
    val prefs = context.getSharedPreferences(PREF_LONG_TERM, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_MILESTONES, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 4) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val goalId = parts[1].toLongOrNull() ?: return@mapNotNull null
            val isDone = parts[2] == "1"
            val title = parts[3]
            GoalMilestone(
                id = id,
                goalId = goalId,
                title = title,
                isDone = isDone
            )
        }
}

private fun saveGoalMilestones(context: Context, milestones: List<GoalMilestone>) {
    val prefs = context.getSharedPreferences(PREF_LONG_TERM, Context.MODE_PRIVATE)
    val raw = milestones.joinToString("\n") { m ->
        val safeTitle = m.title.replace("\n", " ")
        "${m.id}||${m.goalId}||${if (m.isDone) "1" else "0"}||$safeTitle"
    }
    prefs.edit().putString(KEY_MILESTONES, raw).apply()
}
