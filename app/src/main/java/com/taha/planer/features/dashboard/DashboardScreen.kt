package com.taha.planer.features.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taha.planer.features.focus.loadFocusSessions
import com.taha.planer.features.focus.last7DaysFocusSummary
import com.taha.planer.features.focus.todayDate
import com.taha.planer.features.journal.loadJournalEntries
import com.taha.planer.features.journal.todayJournalDate
import com.taha.planer.features.habitbuilder.loadHabitPlans
import com.taha.planer.ui.PlannerCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val progressPercent: Int,
    val highlight: String
)

/**
 * داشبورد مرکزی اپ
 *
 * onNavigate(route) اختیاریه؛ بعداً تو MainActivity می‌تونیم هر id رو به بخش خودش وصل کنیم.
 */
@Composable
fun DashboardScreen(
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current

    val focusSessions = remember { loadFocusSessions(context) }
    val journalEntries = remember { loadJournalEntries(context) }
    val habitPlans = remember { loadHabitPlans(context) }

    val today = todayDate()
    val todayJournalDate = todayJournalDate()

    val todayFocusMinutes = focusSessions
        .filter { it.date == today }
        .sumOf { it.actualMinutes }

    val todayJournalCount = journalEntries.count { it.date == todayJournalDate }
    val activeHabitPlans = habitPlans.size

    val focusLast7 = last7DaysFocusSummary(focusSessions)
    val focusStreakDays = focusLast7
        .asReversed()
        .takeWhile { it.second > 0 }
        .size

    val now = Date()
    val dateFormat = SimpleDateFormat("EEEE d MMMM yyyy", Locale("fa"))
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateText = dateFormat.format(now)
    val timeText = timeFormat.format(now)

    val sections = listOf(
        DashboardSection(
            id = "productivity",
            title = "بهره‌وری و کارها",
            subtitle = "کارها، عادت‌ها و ساخت عادت‌های جدید",
            progressPercent = clampPercent(activeHabitPlans * 10),
            highlight = "پلن عادت فعال: $activeHabitPlans"
        ),
        DashboardSection(
            id = "focus",
            title = "تمرکز عمیق",
            subtitle = "مجموع تمرکز امروز و استریک روزهای پشت‌سرهم",
            progressPercent = focusProgress(todayFocusMinutes),
            highlight = "$todayFocusMinutes دقیقه تمرکز • استریک: $focusStreakDays روز"
        ),
        DashboardSection(
            id = "health",
            title = "سلامت و خواب",
            subtitle = "رژیم، آب، مکمل‌ها، ورزش و خواب",
            progressPercent = 0,
            highlight = "برای سلامتت وقت بگذار 🎧🚰"
        ),
        DashboardSection(
            id = "finance",
            title = "مالی و بودجه",
            subtitle = "درآمد، هزینه، پس‌انداز و بدهی‌ها",
            progressPercent = 0,
            highlight = "یک نگاه به خرج‌های این هفته بنداز"
        ),
        DashboardSection(
            id = "journal",
            title = "ژورنال و ذهن",
            subtitle = "نوشتن و مرور احساس‌ها و فکرها",
            progressPercent = if (todayJournalCount > 0) 100 else 0,
            highlight = if (todayJournalCount > 0)
                "امروز $todayJournalCount یادداشت نوشتی"
            else
                "امروز هنوز چیزی ننوشتی"
        ),
        DashboardSection(
            id = "rewards",
            title = "پاداش‌ها",
            subtitle = "سیستم امتیاز و پاداش برای انگیزه",
            progressPercent = 0,
            highlight = "هر کار انجام‌شده → نزدیک‌تر به پاداش 😌"
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DashboardHeaderCard(
                    dateText = dateText,
                    timeText = timeText,
                    todayFocusMinutes = todayFocusMinutes,
                    todayJournalCount = todayJournalCount,
                    focusStreakDays = focusStreakDays
                )
            }

            item {
                QuickActionsCard(
                    onNavigate = onNavigate
                )
            }

            item {
                FocusSleepChartCard(
                    data = focusLast7
                )
            }

            item {
                TodayInsightsCard(
                    todayFocusMinutes = todayFocusMinutes,
                    todayJournalCount = todayJournalCount,
                    activeHabitPlans = activeHabitPlans,
                    focusStreakDays = focusStreakDays
                )
            }

            item {
                Text(
                    text = "نمای کلی بخش‌ها",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            items(sections, key = { it.id }) { section ->
                DashboardSectionCard(
                    section = section,
                    onClick = { onNavigate(section.id) }
                )
            }
        }
    }
}

private fun focusProgress(todayMinutes: Int): Int {
    val target = 120
    return clampPercent((todayMinutes * 100) / target)
}

private fun clampPercent(value: Int): Int =
    when {
        value < 0 -> 0
        value > 100 -> 100
        else -> value
    }

@Composable
private fun DashboardHeaderCard(
    dateText: String,
    timeText: String,
    todayFocusMinutes: Int,
    todayJournalCount: Int,
    focusStreakDays: Int
) {
    PlannerCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "خوش اومدی ��",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "ساعت: $timeText",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DashboardMiniMetric(
                    label = "تمرکز امروز",
                    value = "$todayFocusMinutes دقیقه"
                )
                DashboardMiniMetric(
                    label = "ژورنال امروز",
                    value = if (todayJournalCount > 0) "$todayJournalCount یادداشت" else "هنوز هیچی"
                )
                DashboardMiniMetric(
                    label = "استریک تمرکز",
                    value = "$focusStreakDays روز"
                )
            }
        }
    }
}

@Composable
private fun DashboardMiniMetric(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.widthIn(min = 0.dp, max = 120.dp)
    ) {
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
private fun QuickActionsCard(
    onNavigate: (String) -> Unit
) {
    PlannerCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "اقدام‌های سریع",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onNavigate("tasks") },
                        modifier = Modifier.weight(1f)
                    ) { Text("اضافه‌کردن کار") }

                    FilledTonalButton(
                        onClick = { onNavigate("focus") },
                        modifier = Modifier.weight(1f)
                    ) { Text("شروع فوکوس 25دقیقه‌ای") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onNavigate("habitbuilder") },
                        modifier = Modifier.weight(1f)
                    ) { Text("ساخت عادت جدید") }

                    FilledTonalButton(
                        onClick = { onNavigate("journal") },
                        modifier = Modifier.weight(1f)
                    ) { Text("نوشتن ژورنال امروز") }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onNavigate("calendar") },
                        modifier = Modifier.weight(1f)
                    ) { Text("برنامه فردا") }

                    FilledTonalButton(
                        onClick = { onNavigate("assistant") },
                        modifier = Modifier.weight(1f)
                    ) { Text("گفتگو با دستیار") }
                }
            }
        }
    }
}

@Composable
private fun DashboardSectionCard(
    section: DashboardSection,
    onClick: () -> Unit
) {
    PlannerCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = section.subtitle,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = (section.progressPercent.coerceIn(0, 100) / 100f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${section.progressPercent.coerceIn(0,100)}٪",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = section.highlight,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            FilledTonalButton(
                onClick = onClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("رفتن به این بخش")
            }
        }
    }
}

@Composable
private fun FocusSleepChartCard(
    data: List<Pair<String, Int>>
) {
    PlannerCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                text = "ریتم تمرکز ۷ روز اخیر",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "بعداً می‌تونیم این نمودار رو با کیفیت خواب هم ترکیب کنیم.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (data.isEmpty()) {
                Text(
                    text = "هنوز داده‌ای برای نمایش نیست.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                )
            } else {
                FocusLineChart(data = data)
            }
        }
    }
}

@Composable
private fun FocusLineChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
) {
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

@Composable
private fun TodayInsightsCard(
    todayFocusMinutes: Int,
    todayJournalCount: Int,
    activeHabitPlans: Int,
    focusStreakDays: Int
) {
    PlannerCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "بینش امروز",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            val suggestions = mutableListOf<String>()

            if (todayFocusMinutes == 0) {
                suggestions += "امروز هنوز تمرکز ثبت نکردی؛ یک جلسه ۲۵ دقیقه‌ای با حالت فوکوس شروع کن."
            } else if (todayFocusMinutes < 60) {
                suggestions += "تمرکز امروز کمتر از ۱ ساعت بوده؛ اگر توانش را داری یک جلسه‌ی دیگر هم انجام بده."
            } else {
                suggestions += "تمرکز امروزت خوب بوده 👌 سعی کن همین ریتم را حفظ کنی."
            }

            if (todayJournalCount == 0) {
                suggestions += "۳–۵ خط ژورنال بنویس؛ فقط کافی است مهم‌ترین فکر و احساس امروز را بنویسی."
            }

            if (activeHabitPlans == 0) {
                suggestions += "هیچ پلن ساخت عادتی فعالی نداری؛ یکی از عادت‌های مهمت را انتخاب کن و یک پلن کوچک برایش بساز."
            }

            if (focusStreakDays >= 3) {
                suggestions += "استریک تمرکزت ${focusStreakDays} روزه است؛ این استریک را مثل یک بازی حفظ کن."
            }

            if (suggestions.isEmpty()) {
                suggestions += "همه‌چیز روی روال است؛ فقط حواست به استراحت و خواب کافی هم باشد."
            }

            suggestions.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}
