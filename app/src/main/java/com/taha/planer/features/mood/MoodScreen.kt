package com.taha.planer.features.mood

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
private const val PREF_MOOD = "planner_mood"
private const val KEY_MOOD = "mood_entries_v1"

data class MoodEntry(
    val id: Long,
    val dayIndex: Int,
    val mood: Int,   // 1 تا 5
    val energy: Int, // 1 تا 5
    val note: String
)

@Composable
fun MoodScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(loadMoodEntries(context)) }

    fun persist(updated: List<MoodEntry>) {
        entries = updated
        saveMoodEntries(context, updated)
    }

    val todayIndex = currentDayIndex()
    val today = entries.find { it.dayIndex == todayIndex }

    val last7 = entries.filter { it.dayIndex >= todayIndex - 6 }
    val last30 = entries.filter { it.dayIndex >= todayIndex - 29 }

    fun avgMood(list: List<MoodEntry>): Double? =
        if (list.isEmpty()) null else list.map { it.mood }.average()

    val avg7 = avgMood(last7)
    val avg30 = avgMood(last30)

    val last14Indices = (todayIndex - 13..todayIndex).toList()
    val chartPoints = last14Indices.map { index ->
        entries.find { it.dayIndex == index }?.mood ?: 0
    }

    var showTodayDialog by remember { mutableStateOf(false) }

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
                    text = "خلاصه‌ی مود",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("میانگین ۷ روز اخیر: ")
                        append(avg7?.let { String.format("%.1f", it) } ?: "-")
                        append(" / 5   |   ")
                        append("میانگین ۳۰ روز اخیر: ")
                        append(avg30?.let { String.format("%.1f", it) } ?: "-")
                        append(" / 5")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                MoodHistoryChart(points = chartPoints)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "مود امروز",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (today == null) {
                        Text(
                            text = "هنوز برای امروز چیزی ثبت نکردی.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text(
                            text = "مود: ${today.mood}/5  |  انرژی: ${today.energy}/5",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (today.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = today.note,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                FilledTonalButton(
                    onClick = { showTodayDialog = true }
                ) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (today == null) "ثبت امروز" else "ویرایش امروز")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "تاریخچه‌ی اخیر",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        val recent = entries.sortedByDescending { it.dayIndex }.take(20)

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(recent, key = { it.id }) { entry ->
                MoodHistoryRow(
                    entry = entry,
                    todayIndex = todayIndex
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    if (showTodayDialog) {
        val existing = today
        var draftMood by remember { mutableStateOf(existing?.mood?.toString() ?: "3") }
        var draftEnergy by remember { mutableStateOf(existing?.energy?.toString() ?: "3") }
        var draftNote by remember { mutableStateOf(existing?.note ?: "") }

        AlertDialog(
            onDismissRequest = { showTodayDialog = false },
            title = {
                Text(text = "ثبت / ویرایش مود امروز")
            },
            text = {
                Column {
                    Text(
                        text = "از ۱ (خیلی بد) تا ۵ (عالی) انتخاب کن.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = draftMood,
                        onValueChange = { draftMood = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("مود (۱ تا ۵)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = draftEnergy,
                        onValueChange = { draftEnergy = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("انرژی (۱ تا ۵)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = draftNote,
                        onValueChange = { draftNote = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        label = { Text("یادداشت (اختیاری)") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val mood = draftMood.toIntOrNull()?.coerceIn(1, 5) ?: 3
                        val energy = draftEnergy.toIntOrNull()?.coerceIn(1, 5) ?: 3
                        val note = draftNote.trim()

                        val entry = MoodEntry(
                            id = existing?.id ?: System.currentTimeMillis(),
                            dayIndex = todayIndex,
                            mood = mood,
                            energy = energy,
                            note = note
                        )

                        val withoutToday = entries.filterNot { it.dayIndex == todayIndex }
                        persist(withoutToday + entry)
                        showTodayDialog = false
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTodayDialog = false }) {
                    Text("بی‌خیال")
                }
            }
        )
    }
}

@Composable
private fun MoodHistoryRow(
    entry: MoodEntry,
    todayIndex: Int
) {
    val diff = todayIndex - entry.dayIndex
    val dayLabel = when (diff) {
        0 -> "امروز"
        1 -> "دیروز"
        in 2..7 -> "$diff روز پیش"
        else -> "${diff} روز پیش"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = dayLabel,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "مود: ${entry.mood}/5  |  انرژی: ${entry.energy}/5",
                style = MaterialTheme.typography.bodySmall
            )
            if (entry.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MoodHistoryChart(points: List<Int>) {
    val maxScale = 5f

    Column {
        Text(
            text = "نمودار خطی مود در ۱۴ روز اخیر",
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
                val moodVal = value.coerceIn(0, 5)
                val ratio = if (moodVal == 0) 0f else moodVal.toFloat() / maxScale
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
    }
}

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

private fun loadMoodEntries(context: Context): List<MoodEntry> {
    val prefs = context.getSharedPreferences(PREF_MOOD, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_MOOD, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 5) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val dayIndex = parts[1].toIntOrNull() ?: return@mapNotNull null
            val mood = parts[2].toIntOrNull() ?: 3
            val energy = parts[3].toIntOrNull() ?: 3
            val note = parts[4]
            MoodEntry(
                id = id,
                dayIndex = dayIndex,
                mood = mood.coerceIn(1, 5),
                energy = energy.coerceIn(1, 5),
                note = note
            )
        }
}

private fun saveMoodEntries(context: Context, entries: List<MoodEntry>) {
    val prefs = context.getSharedPreferences(PREF_MOOD, Context.MODE_PRIVATE)
    val raw = entries.joinToString("\n") { e ->
        val safeNote = e.note.replace("\n", " ")
        "${e.id}||${e.dayIndex}||${e.mood}||${e.energy}||$safeNote"
    }
    prefs.edit().putString(KEY_MOOD, raw).apply()
}
