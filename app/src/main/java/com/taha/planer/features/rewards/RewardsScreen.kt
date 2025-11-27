package com.taha.planer.features.rewards

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val PREF_REWARDS = "planner_rewards"
private const val KEY_REWARDS = "rewards_v1"
private const val MILLIS_PER_DAY = 86_400_000L

private const val TASK_PREF_NAME = "planner_tasks"
private const val TASK_KEY_TASKS = "tasks_v1"

private const val HABIT_PREF_NAME = "planner_habits"
private const val HABIT_KEY_HABITS = "habits_v1"

private const val ROUTINE_PREF_NAME = "planner_routines"
private const val ROUTINE_KEY_ROUTINES = "routines_v1"

data class Reward(
    val id: Long,
    val title: String,
    val description: String,
    val targetPoints: Int,
    val isClaimed: Boolean
)

private enum class RewardFilter {
    ALL, CLAIMABLE, CLAIMED
}

@Composable
fun RewardsScreen() {
    val context = LocalContext.current

    var rewards by remember { mutableStateOf(loadRewards(context)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newDescription by remember { mutableStateOf("") }
    var newTargetText by remember { mutableStateOf("50") }

    var filter by remember { mutableStateOf(RewardFilter.ALL) }

    val availablePoints = calculateAvailablePoints(context)

    fun persist(updated: List<Reward>) {
        rewards = updated
        saveRewards(context, updated)
    }

    val highestTarget = rewards.maxOfOrNull { it.targetPoints } ?: 0
    val overallProgress =
        if (highestTarget > 0)
            (availablePoints.toFloat() / highestTarget.toFloat()).coerceIn(0f, 1f)
        else 0f

    val filteredRewards = when (filter) {
        RewardFilter.ALL -> rewards
        RewardFilter.CLAIMABLE -> rewards.filter { !it.isClaimed && availablePoints >= it.targetPoints }
        RewardFilter.CLAIMED -> rewards.filter { it.isClaimed }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Text(
                    text = "پاداش‌ها",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "امتیاز فعلی از کارها، عادت‌ها و روال‌ها: $availablePoints",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (highestTarget > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "بیشترین هدف پاداش: $highestTarget امتیاز",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = overallProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "هنوز پاداشی تعریف نکردی؛ روی + بزن و اولین پاداش رو بساز.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RewardFilterChip(
                        text = "همه",
                        selected = filter == RewardFilter.ALL
                    ) { filter = RewardFilter.ALL }

                    RewardFilterChip(
                        text = "قابل دریافت",
                        selected = filter == RewardFilter.CLAIMABLE
                    ) { filter = RewardFilter.CLAIMABLE }

                    RewardFilterChip(
                        text = "گرفته‌شده",
                        selected = filter == RewardFilter.CLAIMED
                    ) { filter = RewardFilter.CLAIMED }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (rewards.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("مثلاً: سینما، بازی یک‌ساعته، خرید یه چیز کوچیک...")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("برای هر پاداش، هدف امتیاز مشخص کن.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredRewards, key = { it.id }) { reward ->
                            RewardRow(
                                reward = reward,
                                availablePoints = availablePoints,
                                onClaim = {
                                    if (!reward.isClaimed && availablePoints >= reward.targetPoints) {
                                        val updated = rewards.map {
                                            if (it.id == reward.id) it.copy(isClaimed = true) else it
                                        }
                                        persist(updated)
                                    }
                                },
                                onDelete = {
                                    val updated = rewards.filterNot { it.id == reward.id }
                                    persist(updated)
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                RewardsLineChart(
                    rewards = rewards,
                    availablePoints = availablePoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = {
                newTitle = ""
                newDescription = ""
                newTargetText = "50"
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "پاداش جدید")
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("پاداش جدید") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("مثلاً: دیدن فیلم، سفارش غذا، استراحت طولانی") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newDescription,
                            onValueChange = { newDescription = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            placeholder = { Text("توضیح کوتاه درباره این پاداش و شرایطش.") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newTargetText,
                            onValueChange = { newTargetText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("هدف امتیاز") },
                            placeholder = { Text("مثلاً ۵۰") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val titleTrimmed = newTitle.trim()
                            val descTrimmed = newDescription.trim()
                            val target = newTargetText.toIntOrNull() ?: 50
                            if (titleTrimmed.isNotEmpty() && target > 0) {
                                val updated = rewards + Reward(
                                    id = System.currentTimeMillis(),
                                    title = titleTrimmed,
                                    description = descTrimmed,
                                    targetPoints = target,
                                    isClaimed = false
                                )
                                persist(updated)
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("اضافه کن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("بی‌خیال")
                    }
                }
            )
        }
    }
}

@Composable
private fun RewardFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Text(
            text = text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RewardRow(
    reward: Reward,
    availablePoints: Int,
    onClaim: () -> Unit,
    onDelete: () -> Unit
) {
    val progress =
        if (reward.targetPoints > 0)
            (availablePoints.toFloat() / reward.targetPoints.toFloat()).coerceIn(0f, 1f)
        else 0f

    val claimable = !reward.isClaimed && availablePoints >= reward.targetPoints

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = reward.title,
                    fontWeight = FontWeight.Bold
                )
                if (reward.description.isNotBlank()) {
                    Text(
                        text = reward.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "هدف: ${reward.targetPoints} امتیاز",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "پیشرفت امروز: $availablePoints / ${reward.targetPoints}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (reward.isClaimed) {
                Text(
                    text = "گرفته شد 🎁",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                TextButton(
                    onClick = onClaim,
                    enabled = claimable
                ) {
                    Text(if (claimable) "بگیرش" else "منتظر امتیاز")
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "حذف پاداش"
                )
            }
        }

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )
    }
}

@Composable
private fun RewardsLineChart(
    rewards: List<Reward>,
    availablePoints: Int,
    modifier: Modifier = Modifier
) {
    if (rewards.isEmpty()) {
        Text(
            text = "نمودار بعد از تعریف پاداش‌ها فعال می‌شود.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    val progressValues = rewards.map { r ->
        if (r.targetPoints > 0)
            (availablePoints.toFloat() / r.targetPoints.toFloat()).coerceIn(0f, 1f)
        else 0f
    }

    val maxVal = progressValues.maxOrNull() ?: 0f
    if (maxVal <= 0f) {
        Text(
            text = "هنوز به هیچ پاداشی نزدیک نشدی؛ امتیاز جمع کن 🙂",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (progressValues.size == 1) {
            val y = height * (1f - (progressValues[0] / maxVal))
            drawCircle(
                color = MaterialTheme.colorScheme.primary,
                radius = 6f,
                center = Offset(width / 2f, y)
            )
            return@Canvas
        }

        val stepX = width / (progressValues.size - 1).coerceAtLeast(1)
        val path = Path()

        progressValues.forEachIndexed { index, v ->
            val normalized = if (maxVal > 0f) v / maxVal else 0f
            val x = stepX * index
            val y = height * (1f - normalized)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = MaterialTheme.colorScheme.primary,
            style = Stroke(
                width = 4f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        progressValues.forEachIndexed { index, v ->
            val normalized = if (maxVal > 0f) v / maxVal else 0f
            val x = stepX * index
            val y = height * (1f - normalized)
            drawCircle(
                color = MaterialTheme.colorScheme.primary,
                radius = 5f,
                center = Offset(x, y)
            )
        }
    }
}

// ------------ helpers for points ------------

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

private data class RewardTask(
    val done: Boolean,
    val points: Int
)

private data class RewardHabit(
    val streak: Int,
    val lastDoneDayIndex: Int,
    val points: Int
)

private data class RewardRoutine(
    val streak: Int,
    val lastDoneDayIndex: Int,
    val points: Int
)

private fun calculateAvailablePoints(context: Context): Int {
    val dayIndex = currentDayIndex()
    val taskPoints = loadTasksForRewards(context).sumOf { if (it.done) it.points else 0 }
    val habitPoints = loadHabitsForRewards(context).sumOf { h ->
        if (h.lastDoneDayIndex == dayIndex) h.streak * h.points else 0
    }
    val routinePoints = loadRoutinesForRewards(context).sumOf { r ->
        if (r.lastDoneDayIndex == dayIndex) r.streak * r.points else 0
    }
    return taskPoints + habitPoints + routinePoints
}

private fun loadTasksForRewards(context: Context): List<RewardTask> {
    val prefs = context.getSharedPreferences(TASK_PREF_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(TASK_KEY_TASKS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 4) return@mapNotNull null
            val done = parts[2] == "1"
            val points = parts[3].toIntOrNull() ?: 0
            RewardTask(done = done, points = points)
        }
}

private fun loadHabitsForRewards(context: Context): List<RewardHabit> {
    val prefs = context.getSharedPreferences(HABIT_PREF_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(HABIT_KEY_HABITS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 5) return@mapNotNull null
            val streak = parts[2].toIntOrNull() ?: 0
            val lastIdx = parts[3].toIntOrNull() ?: -1
            val points = parts[4].toIntOrNull() ?: 0
            RewardHabit(streak = streak, lastDoneDayIndex = lastIdx, points = points)
        }
}

private fun loadRoutinesForRewards(context: Context): List<RewardRoutine> {
    val prefs = context.getSharedPreferences(ROUTINE_PREF_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(ROUTINE_KEY_ROUTINES, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 6) return@mapNotNull null
            val streak = parts[3].toIntOrNull() ?: 0
            val lastIdx = parts[4].toIntOrNull() ?: -1
            val points = parts[5].toIntOrNull() ?: 0
            RewardRoutine(streak = streak, lastDoneDayIndex = lastIdx, points = points)
        }
}

// ------------ load / save rewards ------------

private fun loadRewards(context: Context): List<Reward> {
    val prefs = context.getSharedPreferences(PREF_REWARDS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_REWARDS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 5) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val title = parts[1]
            val desc = parts[2]
            val target = parts[3].toIntOrNull() ?: return@mapNotNull null
            val claimed = parts[4] == "1"
            Reward(
                id = id,
                title = title,
                description = desc,
                targetPoints = target,
                isClaimed = claimed
            )
        }
}

private fun saveRewards(context: Context, rewards: List<Reward>) {
    val prefs = context.getSharedPreferences(PREF_REWARDS, Context.MODE_PRIVATE)
    val raw = rewards.joinToString("\n") { r ->
        val safeTitle = r.title.replace("\n", " ")
        val safeDesc = r.description.replace("\n", " ")
        "${r.id}||$safeTitle||$safeDesc||${r.targetPoints}||${if (r.isClaimed) "1" else "0"}"
    }
    prefs.edit().putString(KEY_REWARDS, raw).apply()
}
