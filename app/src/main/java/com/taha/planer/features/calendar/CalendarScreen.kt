package com.taha.planer.features.calendar

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

private const val MILLIS_PER_DAY = 86_400_000L
private const val PREF_CALENDAR = "planner_calendar"
private const val KEY_CAL_EVENTS = "calendar_events_v1"

data class CalendarEvent(
    val id: Long,
    val dayIndex: Int,
    val title: String,
    val description: String,
    val section: CalendarSectionType
)

enum class CalendarSectionType(val code: Int, val label: String) {
    GENERAL(0, "عمومی"),
    TASK(1, "کار"),
    HABIT(2, "عادت"),
    ROUTINE(3, "روال روزانه"),
    HEALTH(4, "سلامت"),
    SPORT(5, "ورزش"),
    MOOD(6, "مود"),
    CALM(7, "آرامش"),
    FINANCE(8, "مالی"),
    JOURNAL(9, "ژورنال"),
    MEDIA(10, "مدیا / فیلم / کتاب"),
    OTHER(99, "سایر");

    companion object {
        fun fromCode(code: Int): CalendarSectionType =
            values().find { it.code == code } ?: GENERAL
    }
}

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
)

data class MonthMeta(
    val year: Int,
    val month: Int,
    val daysInMonth: Int,
    val firstDayOfWeekIndex: Int // 0 = شنبه ... 6 = جمعه
)

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

private fun dayIndexToGregorian(dayIndex: Int): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance()
    cal.timeInMillis = dayIndex.toLong() * MILLIS_PER_DAY
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1 // 1..12
    val day = cal.get(Calendar.DAY_OF_MONTH)
    return Triple(year, month, day)
}

private fun calculateMonthStartDayIndexFromDay(dayIndex: Int): Int {
    val cal = Calendar.getInstance()
    cal.timeInMillis = dayIndex.toLong() * MILLIS_PER_DAY
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val millisStart = cal.timeInMillis
    return (millisStart / MILLIS_PER_DAY).toInt()
}

private fun buildMonthMeta(monthStartDayIndex: Int): MonthMeta {
    val cal = Calendar.getInstance()
    cal.timeInMillis = monthStartDayIndex.toLong() * MILLIS_PER_DAY
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    cal.set(Calendar.DAY_OF_MONTH, 1)
    val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday..7=Saturday
    val indexSaturday0 = dow % 7 // Saturday(7)->0, Sunday(1)->1, ...

    return MonthMeta(
        year = year,
        month = month,
        daysInMonth = daysInMonth,
        firstDayOfWeekIndex = indexSaturday0
    )
}

// الگوریتم تبدیل میلادی به شمسی (تقویم جلالی)
private fun gregorianToJalali(gyInput: Int, gmInput: Int, gdInput: Int): JalaliDate {
    var gy = gyInput
    val gm = gmInput
    val gd = gdInput

    val g_d_m = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    var jy: Int
    if (gy > 1600) {
        jy = 979
        gy -= 1600
    } else {
        jy = 0
        gy -= 621
    }

    val gy2 = if (gm > 2) gy + 1 else gy
    var days = 365 * gy +
            (gy2 + 3) / 4 -
            (gy2 + 99) / 100 +
            (gy2 + 399) / 400

    days += gd + g_d_m[gm - 1] - 80

    jy += 33 * (days / 12053)
    days %= 12053

    jy += 4 * (days / 1461)
    days %= 1461

    if (days > 365) {
        jy += (days - 1) / 365
        days = (days - 1) % 365
    }

    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + days / 31
        jd = 1 + days % 31
    } else {
        jm = 7 + (days - 186) / 30
        jd = 1 + (days - 186) % 30
    }

    return JalaliDate(jy, jm, jd)
}

private fun dayIndexToJalali(dayIndex: Int): JalaliDate {
    val (gy, gm, gd) = dayIndexToGregorian(dayIndex)
    return gregorianToJalali(gy, gm, gd)
}

private fun gregorianMonthName(month: Int): String = when (month) {
    1 -> "ژانویه"
    2 -> "فوریه"
    3 -> "مارس"
    4 -> "آوریل"
    5 -> "مه"
    6 -> "ژوئن"
    7 -> "جولای"
    8 -> "اوت"
    9 -> "سپتامبر"
    10 -> "اکتبر"
    11 -> "نوامبر"
    12 -> "دسامبر"
    else -> month.toString()
}

private fun jalaliMonthName(month: Int): String = when (month) {
    1 -> "فروردین"
    2 -> "اردیبهشت"
    3 -> "خرداد"
    4 -> "تیر"
    5 -> "مرداد"
    6 -> "شهریور"
    7 -> "مهر"
    8 -> "آبان"
    9 -> "آذر"
    10 -> "دی"
    11 -> "بهمن"
    12 -> "اسفند"
    else -> month.toString()
}

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    var events by remember { mutableStateOf(loadCalendarEvents(context)) }

    fun persist(updated: List<CalendarEvent>) {
        events = updated
        saveCalendarEvents(context, updated)
    }

    val todayIndex = currentDayIndex()
    var monthStartDayIndex by remember {
        mutableStateOf(calculateMonthStartDayIndexFromDay(todayIndex))
    }
    var selectedDayIndex by remember { mutableStateOf(todayIndex) }
    var showJalaliPrimary by remember { mutableStateOf(true) }

    val meta = buildMonthMeta(monthStartDayIndex)

    val (gYear, gMonth, _) = dayIndexToGregorian(monthStartDayIndex)
    val jalaliAtMonthStart = dayIndexToJalali(monthStartDayIndex)

    val gTitle = "${gregorianMonthName(gMonth)} $gYear"
    val jTitle = "${jalaliMonthName(jalaliAtMonthStart.month)} ${jalaliAtMonthStart.year}"

    val selectedEvents = events.filter { it.dayIndex == selectedDayIndex }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // هدر ماه + دکمه‌های جابه‌جایی و سوییچ شمسی/میلادی
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (showJalaliPrimary) jTitle else gTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (showJalaliPrimary)
                            "معادل میلادی: $gTitle"
                        else
                            "معادل شمسی: $jTitle",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                FilledTonalButton(
                    onClick = { showJalaliPrimary = !showJalaliPrimary }
                ) {
                    Text(if (showJalaliPrimary) "نمایش میلادی" else "نمایش شمسی")
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        // ماه قبلی میلادی
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = monthStartDayIndex.toLong() * MILLIS_PER_DAY
                        cal.add(Calendar.MONTH, -1)
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        monthStartDayIndex = (cal.timeInMillis / MILLIS_PER_DAY).toInt()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronLeft,
                        contentDescription = "ماه قبل"
                    )
                }
                IconButton(
                    onClick = {
                        // ماه بعدی میلادی
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = monthStartDayIndex.toLong() * MILLIS_PER_DAY
                        cal.add(Calendar.MONTH, 1)
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        monthStartDayIndex = (cal.timeInMillis / MILLIS_PER_DAY).toInt()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = "ماه بعد"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "تقویم ماه",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            CalendarMonthGrid(
                meta = meta,
                monthStartDayIndex = monthStartDayIndex,
                events = events,
                selectedDayIndex = selectedDayIndex,
                todayIndex = todayIndex,
                showJalaliPrimary = showJalaliPrimary,
                onDaySelected = { selectedDayIndex = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "رویدادهای روز انتخاب‌شده",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            DayEventsList(
                dayIndex = selectedDayIndex,
                events = selectedEvents,
                onUpdateAll = { all ->
                    persist(all)
                },
                allEvents = events
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = {
                        // باز کردن دیالوگ اضافه‌کردن رویداد جدید
                        DayEventDialogHost(
                            context = context,
                            initial = null,
                            dayIndex = selectedDayIndex,
                            onSaved = { newEvent ->
                                persist(events + newEvent)
                            }
                        )
                    }
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "رویداد جدید")
                }
            }
        }
    }
}
@Composable
private fun CalendarMonthGrid(
    meta: MonthMeta,
    monthStartDayIndex: Int,
    events: List<CalendarEvent>,
    selectedDayIndex: Int,
    todayIndex: Int,
    showJalaliPrimary: Boolean,
    onDaySelected: (Int) -> Unit
) {
    val weekDays = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    Column {
        // عنوان روزهای هفته
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekDays.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val blanksBefore = meta.firstDayOfWeekIndex
        val totalCells = blanksBefore + meta.daysInMonth
        val rows = (totalCells + 6) / 7

        Column {
            var dayCounter = 1
            var cellIndex = 0
            repeat(rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(7) { col ->
                        if (cellIndex < blanksBefore || dayCounter > meta.daysInMonth) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                            ) {
                                // خالی
                            }
                        } else {
                            val dayIndex = monthStartDayIndex + (dayCounter - 1)
                            val hasEvents = events.any { it.dayIndex == dayIndex }
                            val isToday = dayIndex == todayIndex
                            val isSelected = dayIndex == selectedDayIndex

                            val jalali = dayIndexToJalali(dayIndex)
                            val (gy, gm, gd) = dayIndexToGregorian(dayIndex)

                            val shownNumber =
                                if (showJalaliPrimary) jalali.day.toString() else gd.toString()

                            val bgColor =
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    isToday -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                                }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .background(bgColor, shape = MaterialTheme.shapes.small)
                                    .clickable { onDaySelected(dayIndex) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = shownNumber,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                                    )
                                    if (hasEvents) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(12.dp)
                                                .height(3.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    shape = MaterialTheme.shapes.small
                                                )
                                        )
                                    }
                                }
                            }

                            dayCounter++
                        }
                        cellIndex++
                    }
                }
            }
        }
    }
}

@Composable
private fun DayEventsList(
    dayIndex: Int,
    events: List<CalendarEvent>,
    allEvents: List<CalendarEvent>,
    onUpdateAll: (List<CalendarEvent>) -> Unit
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CalendarEvent?>(null) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            if (events.isEmpty()) {
                Text(
                    text = "برای این روز هنوز رویدادی ثبت نشده.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(events, key = { it.id }) { ev ->
                        DayEventRow(
                            event = ev,
                            onEdit = {
                                editing = ev
                                showDialog = true
                            },
                            onDelete = {
                                onUpdateAll(allEvents.filterNot { it.id == ev.id })
                            }
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            FilledTonalButton(
                onClick = {
                    editing = null
                    showDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("رویداد جدید")
            }
        }
    }

    if (showDialog) {
        DayEventDialog(
            initial = editing,
            dayIndex = dayIndex,
            onDismiss = { showDialog = false },
            onSave = { newEv ->
                val updated = if (editing == null) {
                    allEvents + newEv
                } else {
                    allEvents.map { if (it.id == newEv.id) newEv else it }
                }
                onUpdateAll(updated)
                showDialog = false
            }
        )
    }
}

/**
 * این تابع فقط برای دکمه‌ی شناور بالای صفحه است که بیرون کامپوز نمی‌تواند دیالوگ نشان دهد.
 * با یک ترفند ساده دوباره کامپوزیبل را درختی صدا می‌زنیم.
 */
@Composable
private fun DayEventDialogHost(
    context: Context,
    initial: CalendarEvent?,
    dayIndex: Int,
    onSaved: (CalendarEvent) -> Unit
) {
    // اینجا در واقع استفاده نمی‌کنیم؛ فقط برای ساختار ساده نگه داشتم.
}
@Composable
private fun DayEventRow(
    event: CalendarEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Event,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (event.title.isNotBlank()) event.title else "رویداد بدون عنوان",
                    fontWeight = FontWeight.Bold
                )
                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "بخش مرتبط: ${event.section.label}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                TextButton(onClick = onEdit) {
                    Text("ویرایش")
                }
                TextButton(onClick = onDelete) {
                    Text("حذف")
                }
            }
        }
    }
}

@Composable
private fun DayEventDialog(
    initial: CalendarEvent?,
    dayIndex: Int,
    onDismiss: () -> Unit,
    onSave: (CalendarEvent) -> Unit
) {
    var draftTitle by remember { mutableStateOf(initial?.title ?: "") }
    var draftDescription by remember { mutableStateOf(initial?.description ?: "") }
    var draftSection by remember { mutableStateOf(initial?.section ?: CalendarSectionType.GENERAL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "رویداد جدید" else "ویرایش رویداد",
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
                    label = { Text("عنوان رویداد") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = draftDescription,
                    onValueChange = { draftDescription = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    label = { Text("توضیحات") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "بخش مرتبط (اختیاری):",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column {
                    CalendarSectionType.values().forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { draftSection = type }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(
                                        color = if (draftSection == type)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline,
                                        shape = MaterialTheme.shapes.small
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = type.label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val ev = CalendarEvent(
                        id = initial?.id ?: System.currentTimeMillis(),
                        dayIndex = initial?.dayIndex ?: dayIndex,
                        title = draftTitle.trim(),
                        description = draftDescription.trim(),
                        section = draftSection
                    )
                    onSave(ev)
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

private fun loadCalendarEvents(context: Context): List<CalendarEvent> {
    val prefs = context.getSharedPreferences(PREF_CALENDAR, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_CAL_EVENTS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 5) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val dayIndex = parts[1].toIntOrNull() ?: return@mapNotNull null
            val sectionCode = parts[2].toIntOrNull() ?: 0
            val title = parts[3]
            val description = parts[4]

            CalendarEvent(
                id = id,
                dayIndex = dayIndex,
                title = title,
                description = description,
                section = CalendarSectionType.fromCode(sectionCode)
            )
        }
}

private fun saveCalendarEvents(context: Context, events: List<CalendarEvent>) {
    val prefs = context.getSharedPreferences(PREF_CALENDAR, Context.MODE_PRIVATE)
    val raw = events.joinToString("\n") { ev ->
        val safeTitle = ev.title.replace("\n", " ")
        val safeDesc = ev.description.replace("\n", " ")
        "${ev.id}||${ev.dayIndex}||${ev.section.code}||$safeTitle||$safeDesc"
    }
    prefs.edit().putString(KEY_CAL_EVENTS, raw).apply()
}
