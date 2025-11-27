package com.taha.planer.features.health

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

private const val MILLIS_PER_DAY = 86_400_000L

private const val PREF_WATER = "planner_health_water"
private const val KEY_WATER_LOGS = "water_logs_v1"
private const val KEY_WATER_TARGET = "water_target_ml_v1"
private const val DEFAULT_WATER_TARGET = 2000

private const val PREF_SLEEP = "planner_health_sleep"
private const val KEY_SLEEP_LOGS = "sleep_logs_v1"

private enum class HealthTab {
    WATER, NUTRITION, SUPPLEMENTS, SLEEP
}

data class WaterLog(
    val dayIndex: Int,
    val consumedMl: Int,
    val targetMl: Int
)

data class SleepLog(
    val dayIndex: Int,
    val minutes: Int,
    val quality: Int,
    val note: String
)

@Composable
fun HealthScreen() {
    var selectedTab by remember { mutableStateOf(HealthTab.WATER) }

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
                    text = "سلامت",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "آب بدن، رژیم، مکمل‌ها و خواب در یک جا.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                HealthTabsRow(
                    selected = selectedTab,
                    onSelect = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    HealthTab.WATER -> WaterTab()
                    HealthTab.NUTRITION -> NutritionTab()
                    HealthTab.SUPPLEMENTS -> SupplementsTab()
                    HealthTab.SLEEP -> SleepTab()
                }
            }
        }
    }
}

@Composable
private fun HealthTabsRow(
    selected: HealthTab,
    onSelect: (HealthTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SimpleTabChip(
            text = "آب بدن",
            selected = selected == HealthTab.WATER
        ) { onSelect(HealthTab.WATER) }

        SimpleTabChip(
            text = "رژیم غذایی",
            selected = selected == HealthTab.NUTRITION
        ) { onSelect(HealthTab.NUTRITION) }

        SimpleTabChip(
            text = "مکمل‌ها",
            selected = selected == HealthTab.SUPPLEMENTS
        ) { onSelect(HealthTab.SUPPLEMENTS) }

        SimpleTabChip(
            text = "خواب",
            selected = selected == HealthTab.SLEEP
        ) { onSelect(HealthTab.SLEEP) }
    }
}

@Composable
private fun SimpleTabChip(
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

// -------------- تب آب بدن --------------

@Composable
private fun WaterTab() {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(loadWaterLogs(context)) }
    var targetMl by remember { mutableStateOf(loadWaterTarget(context)) }
    val todayIndex = remember { currentDayIndex() }

    fun persist(updatedLogs: List<WaterLog>, updatedTarget: Int = targetMl) {
        logs = updatedLogs
        targetMl = updatedTarget
        saveWaterState(context, updatedLogs, updatedTarget)
    }

    var showTargetDialog by remember { mutableStateOf(false) }
    var targetText by remember { mutableStateOf(targetMl.toString()) }

    val todayLog = logs.find { it.dayIndex == todayIndex }
        ?: WaterLog(dayIndex = todayIndex, consumedMl = 0, targetMl = targetMl)

    val percent =
        if (todayLog.targetMl > 0)
            ((todayLog.consumedMl.toFloat() / todayLog.targetMl.toFloat()) * 100f).toInt()
        else 0

    val todayDisplay =
        "${todayLog.consumedMl} / ${todayLog.targetMl} میلی‌لیتر (${percent.coerceIn(0, 200)}٪)"

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "مدیریت آب بدن",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "هدف روزانه: ${targetMl} میلی‌لیتر (قابل تنظیم)",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = if (todayLog.targetMl > 0)
                (todayLog.consumedMl.toFloat() / todayLog.targetMl.toFloat()).coerceIn(0f, 1.5f)
            else 0f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "وضعیت امروز: $todayDisplay",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = {
                    val updated = todayLog.copy(
                        consumedMl = todayLog.consumedMl + 250
                    )
                    persist(
                        mergeWaterLog(logs, updated),
                        targetMl
                    )
                }
            ) { Text("+ ۲۵۰ میلی‌لیتر") }

            TextButton(
                onClick = {
                    val updated = todayLog.copy(
                        consumedMl = todayLog.consumedMl + 500
                    )
                    persist(
                        mergeWaterLog(logs, updated),
                        targetMl
                    )
                }
            ) { Text("+ ۵۰۰ میلی‌لیتر") }

            TextButton(
                onClick = {
                    val updated = todayLog.copy(consumedMl = 0)
                    persist(mergeWaterLog(logs, updated), targetMl)
                }
            ) { Text("ریست امروز") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {
            targetText = targetMl.toString()
            showTargetDialog = true
        }) {
            Text("تنظیم هدف روزانه آب")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "نمودار ۷ روز اخیر (نسبت مصرف به هدف)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        val last7 = logs
            .sortedBy { it.dayIndex }
            .takeLast(7)

        WaterLineChart(
            logs = last7,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "تاریخچه خلاصه",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            items(last7.reversed()) { log ->
                val p =
                    if (log.targetMl > 0)
                        ((log.consumedMl.toFloat() / log.targetMl.toFloat()) * 100f).toInt()
                    else 0
                Text(
                    text = "روز ${log.dayIndex}: ${log.consumedMl}/${log.targetMl} ml (${p.coerceIn(0, 200)}٪)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showTargetDialog) {
        AlertDialog(
            onDismissRequest = { showTargetDialog = false },
            title = { Text("هدف روزانه آب (میلی‌لیتر)") },
            text = {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("مثلاً ۲۰۰۰") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTarget = targetText.toIntOrNull() ?: targetMl
                        if (newTarget > 0) {
                            val updatedToday = todayLog.copy(targetMl = newTarget)
                            val newLogs = mergeWaterLog(logs, updatedToday)
                            persist(newLogs, newTarget)
                        }
                        showTargetDialog = false
                    }
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTargetDialog = false }) {
                    Text("بی‌خیال")
                }
            }
        )
    }
}

private fun mergeWaterLog(all: List<WaterLog>, updated: WaterLog): List<WaterLog> {
    val without = all.filterNot { it.dayIndex == updated.dayIndex }
    return (without + updated).sortedBy { it.dayIndex }
}

@Composable
private fun WaterLineChart(
    logs: List<WaterLog>,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) {
        Text(
            text = "برای دیدن نمودار، چند روز ثبت آب انجام بده.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    val ratios = logs.map { log ->
        if (log.targetMl > 0)
            (log.consumedMl.toFloat() / log.targetMl.toFloat()).coerceIn(0f, 1.5f)
        else 0f
    }

    val maxVal = ratios.maxOrNull() ?: 0f
    if (maxVal <= 0f) {
        Text(
            text = "هنوز مصرف قابل‌توجهی ثبت نشده.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (ratios.size == 1) {
            val y = height * (1f - (ratios[0] / maxVal))
            drawCircle(
                color = MaterialTheme.colorScheme.primary,
                radius = 6f,
                center = Offset(width / 2f, y)
            )
            return@Canvas
        }

        val stepX = width / (ratios.size - 1).coerceAtLeast(1)
        val path = Path()

        ratios.forEachIndexed { index, v ->
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

        ratios.forEachIndexed { index, v ->
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

// -------------- تب رژیم غذایی --------------

@Composable
private fun NutritionTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Text(
            text = "رژیم برای قد ۱۷۷ / وزن ۵۵",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "هدف کلی: کمی افزایش وزن سالم (با عضله)، تمرکز روی پروتئین کافی، کربوهیدرات پیچیده و چربی مفید.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "قواعد روزانه:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        Text("• ۳ وعده اصلی + ۲ میان‌وعده سبک.", style = MaterialTheme.typography.bodySmall)
        Text("• در هر وعده یک منبع پروتئین: تخم‌مرغ، مرغ، ماهی، حبوبات، لبنیات.", style = MaterialTheme.typography.bodySmall)
        Text("• کربوهیدراتِ پیچیده: نان سبوس‌دار، برنج، جو دوسر، سیب‌زمینی.", style = MaterialTheme.typography.bodySmall)
        Text("• چربی مفید: مغزها، کنجد، روغن زیتون، در صورت امکان آووکادو.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "نمونه برنامه روز:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            item {
                Text("صبحانه:", fontWeight = FontWeight.Bold)
                Text("• نان سبوس‌دار + ۲ عدد تخم‌مرغ + پنیر یا خوراک لوبیا + گوجه/سبزی.", style = MaterialTheme.typography.bodySmall)
                Text("• یک لیوان شیر (یا شیر گیاهی غنی‌شده).", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Text("میان‌وعده ۱:", fontWeight = FontWeight.Bold)
                Text("• یک مشت آجیل مخلوط + یک میوه (مثل موز یا سیب).", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Text("ناهار:", fontWeight = FontWeight.Bold)
                Text("• برنج یا نان + مرغ/ماهی/گوشت کم‌چرب یا خوراک حبوبات.", style = MaterialTheme.typography.bodySmall)
                Text("• سالاد سبزیجات با کمی روغن زیتون و لیمو.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Text("میان‌وعده ۲:", fontWeight = FontWeight.Bold)
                Text("• ماست + جو دوسر + کمی عسل یا خرما.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Text("شام:", fontWeight = FontWeight.Bold)
                Text("• املت سبزیجات / مرغ و سبزیجات / عدس‌پلو سبک.", style = MaterialTheme.typography.bodySmall)
                Text("• یک میوه سبک در صورت نیاز.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                Text("قبل خواب (در صورت گرسنگی):", fontWeight = FontWeight.Bold)
                Text("• یک لیوان شیر گرم + خرما یا بیسکویت سبوس‌دار.", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "در کنار این برنامه، هیدراته‌بودن، خواب کافی و تمرین مقاومتی سبک کمک زیادی به افزایش وزن سالم می‌کند.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// -------------- تب مکمل‌ها --------------

@Composable
private fun SupplementsTab() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "مکمل‌ها",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "این بخش بهت کمک می‌کند درباره نقش مکمل‌ها فکر کنی؛ تصمیم‌گیری نهایی باید با پزشک یا متخصص تغذیه باشد.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "راهنمای کلی:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("• اگر رژیم غذایی‌ات متعادل باشد، خیلی از افراد بدون مکمل هم شرایط خوبی دارند.", style = MaterialTheme.typography.bodySmall)
        Text("• مکمل‌های رایج: مولتی‌ویتامین ساده، ویتامین D، امگا۳، کراتین، پودر پروتئین.", style = MaterialTheme.typography.bodySmall)
        Text("• قبل از شروع هر مکمل (به‌خصوص اگر دارو مصرف می‌کنی) با پزشک مشورت کن.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "چطور از این بخش استفاده کنی:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text("• می‌تونی نام و زمان مصرف مکمل‌ها را در بخش عادت‌ها ثبت کنی.", style = MaterialTheme.typography.bodySmall)
        Text("• برای یادآوری، از بخش آلارم‌ها و نوتیف استفاده می‌کنیم.", style = MaterialTheme.typography.bodySmall)
    }
}

// -------------- تب خواب (با لاگ روزانه) --------------

@Composable
private fun SleepTab() {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(loadSleepLogs(context)) }
    val todayIndex = remember { currentDayIndex() }

    var hoursText by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("") }
    var qualityText by remember { mutableStateOf("4") }
    var noteText by remember { mutableStateOf("") }

    val todayLog = logs.find { it.dayIndex == todayIndex }
    if (todayLog != null) {
        if (hoursText.isEmpty() && minutesText.isEmpty()) {
            val h = todayLog.minutes / 60
            val m = todayLog.minutes % 60
            hoursText = h.toString()
            minutesText = if (m == 0) "" else m.toString()
            qualityText = todayLog.quality.toString()
            noteText = todayLog.note
        }
    }

    val last7 = logs
        .sortedBy { it.dayIndex }
        .takeLast(7)

    val avgMinutes = if (last7.isNotEmpty()) last7.map { it.minutes }.average() else 0.0
    val avgHours = avgMinutes / 60.0
    val avgQuality = if (last7.isNotEmpty()) last7.map { it.quality }.average() else 0.0

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "پایش خواب",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "هر شب بعد از بیدار شدن، مدت خواب و کیفیت را اینجا ثبت کن.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = hoursText,
                onValueChange = { hoursText = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("ساعت خواب") },
                placeholder = { Text("مثلاً ۷") }
            )
            OutlinedTextField(
                value = minutesText,
                onValueChange = { minutesText = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("دقیقه") },
                placeholder = { Text("مثلاً ۳۰") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = qualityText,
            onValueChange = { qualityText = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("کیفیت خواب (۱ تا ۵)") },
            placeholder = { Text("۴") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            label = { Text("یادداشت کوتاه") },
            placeholder = { Text("مثلاً: دیر خوابیدم / گوشی تا دیر وقت...") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = {
                val h = hoursText.toIntOrNull() ?: 0
                val m = minutesText.toIntOrNull() ?: 0
                val q = qualityText.toIntOrNull()?.coerceIn(1, 5) ?: 3
                val minutes = h * 60 + m
                val note = noteText.trim()

                if (minutes > 0) {
                    val updated = SleepLog(
                        dayIndex = todayIndex,
                        minutes = minutes,
                        quality = q,
                        note = note
                    )
                    logs = mergeSleepLog(logs, updated)
                    saveSleepLogs(context, logs)
                }
            }
        ) {
            Text("ثبت خواب امروز")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "میانگین ۷ روز اخیر: ${"%.1f".format(avgHours)} ساعت، کیفیت ${"%.1f".format(avgQuality)} از ۵",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "نمودار ۷ روز اخیر (ساعت خواب)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        SleepLineChart(
            logs = last7,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "تاریخچه خواب",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f, fill = false)
        ) {
            items(last7.reversed()) { log ->
                val h = log.minutes / 60
                val m = log.minutes % 60
                Text(
                    text = "روز ${log.dayIndex}: ${h}ساعت ${m}دقیقه، کیفیت ${log.quality}/5",
                    style = MaterialTheme.typography.bodySmall
                )
                if (log.note.isNotBlank()) {
                    Text(
                        text = "  یادداشت: ${log.note}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SleepLineChart(
    logs: List<SleepLog>,
    modifier: Modifier = Modifier
) {
    if (logs.isEmpty()) {
        Text(
            text = "برای دیدن نمودار، چند روز خواب را ثبت کن.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    val values = logs.map { it.minutes.toFloat() }
    val maxVal = values.maxOrNull() ?: 0f
    if (maxVal <= 0f) {
        Text(
            text = "داده کافی برای نمودار نیست.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (values.size == 1) {
            val y = height * (1f - (values[0] / maxVal))
            drawCircle(
                color = MaterialTheme.colorScheme.primary,
                radius = 6f,
                center = Offset(width / 2f, y)
            )
            return@Canvas
        }

        val stepX = width / (values.size - 1).coerceAtLeast(1)
        val path = Path()

        values.forEachIndexed { index, v ->
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

        values.forEachIndexed { index, v ->
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

// -------------- helpers & storage --------------

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

private fun loadWaterLogs(context: Context): List<WaterLog> {
    val prefs = context.getSharedPreferences(PREF_WATER, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_WATER_LOGS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 3) return@mapNotNull null
            val dayIndex = parts[0].toIntOrNull() ?: return@mapNotNull null
            val consumed = parts[1].toIntOrNull() ?: 0
            val target = parts[2].toIntOrNull() ?: DEFAULT_WATER_TARGET
            WaterLog(
                dayIndex = dayIndex,
                consumedMl = consumed,
                targetMl = target
            )
        }
}

private fun loadWaterTarget(context: Context): Int {
    val prefs = context.getSharedPreferences(PREF_WATER, Context.MODE_PRIVATE)
    return prefs.getInt(KEY_WATER_TARGET, DEFAULT_WATER_TARGET)
}

private fun saveWaterState(
    context: Context,
    logs: List<WaterLog>,
    targetMl: Int
) {
    val prefs = context.getSharedPreferences(PREF_WATER, Context.MODE_PRIVATE)
    val raw = logs.joinToString("\n") { log ->
        "${log.dayIndex}||${log.consumedMl}||${log.targetMl}"
    }
    prefs.edit()
        .putString(KEY_WATER_LOGS, raw)
        .putInt(KEY_WATER_TARGET, targetMl)
        .apply()
}

private fun loadSleepLogs(context: Context): List<SleepLog> {
    val prefs = context.getSharedPreferences(PREF_SLEEP, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_SLEEP_LOGS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 4) return@mapNotNull null
            val dayIndex = parts[0].toIntOrNull() ?: return@mapNotNull null
            val minutes = parts[1].toIntOrNull() ?: 0
            val quality = parts[2].toIntOrNull() ?: 3
            val note = parts[3]
            SleepLog(
                dayIndex = dayIndex,
                minutes = minutes,
                quality = quality,
                note = note
            )
        }
}

private fun saveSleepLogs(context: Context, logs: List<SleepLog>) {
    val prefs = context.getSharedPreferences(PREF_SLEEP, Context.MODE_PRIVATE)
    val raw = logs.joinToString("\n") { log ->
        "${log.dayIndex}||${log.minutes}||${log.quality}||${log.note.replace("\n", " ")}"
    }
    prefs.edit()
        .putString(KEY_SLEEP_LOGS, raw)
        .apply()
}

private fun mergeSleepLog(all: List<SleepLog>, updated: SleepLog): List<SleepLog> {
    val without = all.filterNot { it.dayIndex == updated.dayIndex }
    return (without + updated).sortedBy { it.dayIndex }
}
