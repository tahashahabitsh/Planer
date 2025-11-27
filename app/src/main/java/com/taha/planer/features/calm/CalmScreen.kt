package com.taha.planer.features.calm

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val MILLIS_PER_DAY = 86_400_000L
private const val PREF_CALM = "planner_calm"
private const val KEY_CALM = "calm_sessions_v1"

data class CalmSession(
    val id: Long,
    val dayIndex: Int,
    val minutes: Int,
    val stressBefore: Int?, // 1..5
    val stressAfter: Int?,  // 1..5
    val note: String
)

@Composable
fun CalmScreen() {
    val context = LocalContext.current
    var sessions by remember { mutableStateOf(loadCalmSessions(context)) }

    fun persist(updated: List<CalmSession>) {
        sessions = updated
        saveCalmSessions(context, updated)
    }

    val todayIndex = currentDayIndex()
    val last7Indices = (todayIndex - 6..todayIndex).toList()
    val last7 = sessions.filter { it.dayIndex in last7Indices }
    val last30 = sessions.filter { it.dayIndex >= todayIndex - 29 }

    val totalMinutesWeek = last7.sumOf { it.minutes }

    fun avgStressImprovement(list: List<CalmSession>): Double? {
        val diffs = list.mapNotNull { s ->
            val b = s.stressBefore
            val a = s.stressAfter
            if (b != null && a != null) (b - a).toDouble() else null
        }
        return if (diffs.isEmpty()) null else diffs.average()
    }

    val avgImprove7 = avgStressImprovement(last7)
    val avgImprove30 = avgStressImprovement(last30)

    val chartPoints = last7Indices.map { day ->
        sessions.filter { it.dayIndex == day }.sumOf { it.minutes }
    }

    val recent = sessions.sortedByDescending { it.dayIndex }.take(20)

    var showDialog by remember { mutableStateOf(false) }

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
                    text = "خلاصه‌ی آرامش و ریلکسیشن",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("این هفته مجموعاً $totalMinutesWeek دقیقه آرامش ثبت شده.\n")
                        append("میانگین کاهش استرس ۷ روز اخیر: ")
                        append(avgImprove7?.let { String.format("%.1f", it) } ?: "-")
                        append(" از ۵  |  ۳۰ روز اخیر: ")
                        append(avgImprove30?.let { String.format("%.1f", it) } ?: "-")
                        append(" از ۵")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                CalmMinutesChart(chartPoints)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "چند پیشنهاد سریع برای آرامش",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• تنفس ۴-۷-۸ (۴ ثانیه دم، ۷ ثانیه نگه‌داشتن، ۸ ثانیه بازدم)\n" +
                            "• ریلکسیشن عضلانی: انقباض و رها کردن عضلات از پا تا صورت\n" +
                            "• اسکن بدن (Body Scan) و تمرکز روی احساسات بدن\n" +
                            "• قدم‌زدن آرام ۵–۱۰ دقیقه بدون موبایل\n" +
                            "• نوشتن نگرانی‌ها روی کاغذ و سوزاندن/پاره کردن آن‌ها",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("ثبت یک جلسه‌ی آرامش")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "تاریخچه‌ی جلسات اخیر",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(recent, key = { it.id }) { s ->
                CalmHistoryRow(
                    session = s,
                    todayIndex = todayIndex
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

    if (showDialog) {
        var draftMinutes by remember { mutableStateOf("10") }
        var draftBefore by remember { mutableStateOf("") }
        var draftAfter by remember { mutableStateOf("") }
        var draftNote by remember {
            mutableStateOf("مثلاً: تنفس ۴-۷-۸، مدیتیشن نفس، ریلکسیشن عضلانی…")
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("ثبت جلسه‌ی آرامش") },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftMinutes,
                        onValueChange = { draftMinutes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("مدت جلسه (دقیقه)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = draftBefore,
                        onValueChange = { draftBefore = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("شدت استرس قبل (۱ تا ۵، اختیاری)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = draftAfter,
                        onValueChange = { draftAfter = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("شدت استرس بعد (۱ تا ۵، اختیاری)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = draftNote,
                        onValueChange = { draftNote = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        label = { Text("نوع تمرین / توضیح کوتاه") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = draftMinutes.toIntOrNull() ?: 0
                        if (minutes <= 0) return@TextButton
                        val before = draftBefore.toIntOrNull()?.coerceIn(1, 5)
                        val after = draftAfter.toIntOrNull()?.coerceIn(1, 5)
                        val note = draftNote.trim()

                        val session = CalmSession(
                            id = System.currentTimeMillis(),
                            dayIndex = currentDayIndex(),
                            minutes = minutes,
                            stressBefore = before,
                            stressAfter = after,
                            note = note
                        )

                        persist(sessions + session)
                        showDialog = false
                    }
                ) { Text("ثبت") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("بی‌خیال")
                }
            }
        )
    }
}

@Composable
private fun CalmHistoryRow(
    session: CalmSession,
    todayIndex: Int
) {
    val diff = todayIndex - session.dayIndex
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
                text = "مدت: ${session.minutes} دقیقه",
                style = MaterialTheme.typography.bodySmall
            )
            val b = session.stressBefore
            val a = session.stressAfter
            if (b != null || a != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "استرس: " +
                            (b?.let { "قبل $it/5 " } ?: "") +
                            (a?.let { "بعد $it/5" } ?: ""),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (session.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = session.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CalmMinutesChart(points: List<Int>) {
    val maxVal = points.maxOrNull() ?: 0
    val safeMax = if (maxVal <= 0) 1 else maxVal

    Column {
        Text(
            text = "نمودار دقیقه‌های آرامش در ۷ روز اخیر",
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
