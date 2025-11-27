package com.taha.planer.features.journal

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
private const val PREF_JOURNAL = "planner_journal"
private const val KEY_JOURNAL = "journal_entries_v1"

enum class JournalTemplateType(val code: Int, val label: String) {
    FREE(0, "آزاد"),
    GRATITUDE(1, "شکرگزاری"),
    REFLECTION(2, "بازتاب روز"),
    BRAIN_DUMP(3, "خالی کردن ذهن");

    companion object {
        fun fromCode(code: Int): JournalTemplateType =
            values().find { it.code == code } ?: FREE
    }
}

data class JournalEntry(
    val id: Long,
    val dayIndex: Int,
    val title: String,
    val content: String,
    val templateType: JournalTemplateType,
    val tags: List<String>,
    val isFavorite: Boolean
)

@Composable
fun JournalScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(loadJournalEntries(context)) }

    fun persist(updated: List<JournalEntry>) {
        entries = updated
        saveJournalEntries(context, updated)
    }

    val todayIndex = currentDayIndex()
    val last30 = entries.filter { it.dayIndex >= todayIndex - 29 }
    val totalEntries30 = last30.size
    val todayEntries = entries.filter { it.dayIndex == todayIndex }.size

    val last14Indices = (todayIndex - 13..todayIndex).toList()
    val chartPoints = last14Indices.map { idx ->
        entries.count { it.dayIndex == idx }
    }

    var searchQuery by remember { mutableStateOf("") }
    var onlyFavorites by remember { mutableStateOf(false) }

    var showEditor by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var currentTemplate by remember { mutableStateOf(JournalTemplateType.FREE) }

    var showDetail by remember { mutableStateOf<JournalEntry?>(null) }

    val filtered = entries
        .filter { e ->
            val q = searchQuery.trim()
            if (q.isBlank()) true
            else {
                e.title.contains(q, ignoreCase = true) ||
                        e.content.contains(q, ignoreCase = true) ||
                        e.tags.any { it.contains(q, ignoreCase = true) }
            }
        }
        .filter { e -> if (onlyFavorites) e.isFavorite else true }
        .sortedByDescending { it.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // خلاصه + نمودار
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "خلاصه‌ی ژورنال",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "در ۳۰ روز اخیر $totalEntries30 یادداشت ثبت شده. امروز: $todayEntries یادداشت.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                JournalEntriesChart(points = chartPoints)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // قالب‌های سریع
        Text(
            text = "قالب‌های سریع",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TemplateChip("آزاد") {
                currentTemplate = JournalTemplateType.FREE
                editingEntry = null
                showEditor = true
            }
            TemplateChip("شکرگزاری") {
                currentTemplate = JournalTemplateType.GRATITUDE
                editingEntry = null
                showEditor = true
            }
            TemplateChip("بازتاب روز") {
                currentTemplate = JournalTemplateType.REFLECTION
                editingEntry = null
                showEditor = true
            }
            TemplateChip("خالی کردن ذهن") {
                currentTemplate = JournalTemplateType.BRAIN_DUMP
                editingEntry = null
                showEditor = true
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // جستجو + فیلتر
        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "جستجو در عنوان، متن، یا تگ‌ها",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("عبارت جستجو") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { onlyFavorites = !onlyFavorites }
                    ) {
                        Icon(
                            imageVector = if (onlyFavorites) Icons.Filled.Favorite
                            else Icons.Filled.FavoriteBorder,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (onlyFavorites) "فقط علاقه‌مندی‌ها" else "همه‌ی یادداشت‌ها")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "یادداشت‌های اخیر",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(filtered, key = { it.id }) { entry ->
                JournalEntryRow(
                    entry = entry,
                    todayIndex = todayIndex,
                    onToggleFavorite = {
                        val updated = entry.copy(isFavorite = !entry.isFavorite)
                        persist(
                            entries.map { if (it.id == entry.id) updated else it }
                        )
                    },
                    onClick = { showDetail = entry },
                    onEdit = {
                        currentTemplate = entry.templateType
                        editingEntry = entry
                        showEditor = true
                    },
                    onDelete = {
                        persist(entries.filterNot { it.id == entry.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = {
                    currentTemplate = JournalTemplateType.FREE
                    editingEntry = null
                    showEditor = true
                }
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "یادداشت جدید")
            }
        }
    }

    if (showEditor) {
        JournalEditorDialog(
            template = currentTemplate,
            initial = editingEntry,
            onDismiss = { showEditor = false },
            onSave = { newEntry ->
                val updated = if (editingEntry == null) {
                    entries + newEntry
                } else {
                    entries.map { if (it.id == newEntry.id) newEntry else it }
                }
                persist(updated)
                showEditor = false
            }
        )
    }

    showDetail?.let { entry ->
        JournalDetailDialog(
            entry = entry,
            onClose = { showDetail = null },
            onEdit = {
                currentTemplate = entry.templateType
                editingEntry = entry
                showEditor = true
                showDetail = null
            },
            onDelete = {
                persist(entries.filterNot { it.id == entry.id })
                showDetail = null
            },
            onToggleFavorite = {
                val updated = entry.copy(isFavorite = !entry.isFavorite)
                persist(
                    entries.map { if (it.id == entry.id) updated else it }
                )
            }
        )
    }
}

@Composable
private fun TemplateChip(
    text: String,
    onClick: () -> Unit
) {
    FilledTonalButton(onClick = onClick) {
        Text(text)
    }
}

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

@Composable
private fun JournalEntryRow(
    entry: JournalEntry,
    todayIndex: Int,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val diff = todayIndex - entry.dayIndex
    val dayLabel = when (diff) {
        0 -> "امروز"
        1 -> "دیروز"
        in 2..7 -> "$diff روز پیش"
        else -> "${diff} روز پیش"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                        text = if (entry.title.isNotBlank()) entry.title else "بدون عنوان",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$dayLabel • ${entry.templateType.label}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (entry.isFavorite) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                        contentDescription = null
                    )
                }
            }

            if (entry.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "تگ‌ها: " + entry.tags.joinToString("، "),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            val preview = entry.content.trim().take(120)
            if (preview.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preview + if (entry.content.length > 120) "..." else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
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
private fun JournalEditorDialog(
    template: JournalTemplateType,
    initial: JournalEntry?,
    onDismiss: () -> Unit,
    onSave: (JournalEntry) -> Unit
) {
    val todayIndex = currentDayIndex()

    var draftTitle by remember { mutableStateOf(initial?.title ?: "") }
    var draftTags by remember {
        mutableStateOf(
            if (initial != null) initial.tags.joinToString(",")
            else ""
        )
    }
    var draftContent by remember { mutableStateOf(initial?.content ?: "") }

    val helperText = when (template) {
        JournalTemplateType.FREE ->
            "آزاد بنویس؛ هرچیزی تو ذهنت هست."

        JournalTemplateType.GRATITUDE ->
            "۳ تا چیزی که امروز بابت‌شون شکرگزار بودی بنویس."

        JournalTemplateType.REFLECTION ->
            "امروز چی خوب بود؟ چی بد بود؟ چی یاد گرفتی؟"

        JournalTemplateType.BRAIN_DUMP ->
            "هر فکر، نگرانی یا کار نیمه‌تمامی در ذهنت هست همین‌جا خالی کن."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (initial == null) "یادداشت جدید (${template.label})"
            else "ویرایش یادداشت")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = helperText,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = { draftTitle = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان (اختیاری)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftTags,
                    onValueChange = { draftTags = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("تگ‌ها (با کاما جدا کن، مثلا: کار،رابطه,ایده)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftContent,
                    onValueChange = { draftContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    label = { Text("متن یادداشت") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val tags = draftTags
                        .split(",", "،")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    val entry = JournalEntry(
                        id = initial?.id ?: System.currentTimeMillis(),
                        dayIndex = initial?.dayIndex ?: todayIndex,
                        title = draftTitle.trim(),
                        content = draftContent.trim(),
                        templateType = template,
                        tags = tags,
                        isFavorite = initial?.isFavorite ?: false
                    )
                    onSave(entry)
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
private fun JournalDetailDialog(
    entry: JournalEntry,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (entry.title.isNotBlank()) entry.title else "یادداشت",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (entry.isFavorite) Icons.Filled.Favorite
                        else Icons.Filled.FavoriteBorder,
                        contentDescription = null
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "قالب: ${entry.templateType.label}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (entry.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تگ‌ها: " + entry.tags.joinToString("، "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onEdit) {
                Text("ویرایش")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("حذف")
                }
                TextButton(onClick = onClose) {
                    Text("بستن")
                }
            }
        }
    )
}

@Composable
private fun JournalEntriesChart(points: List<Int>) {
    val maxVal = points.maxOrNull() ?: 0
    val safeMax = if (maxVal <= 0) 1 else maxVal

    Column {
        Text(
            text = "نمودار تعداد یادداشت در ۱۴ روز اخیر",
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
    }
}

private fun loadJournalEntries(context: Context): List<JournalEntry> {
    val prefs = context.getSharedPreferences(PREF_JOURNAL, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_JOURNAL, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 7) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val dayIndex = parts[1].toIntOrNull() ?: return@mapNotNull null
            val templateCode = parts[2].toIntOrNull() ?: 0
            val isFav = parts[3] == "1"
            val title = parts[4]
            val tagsRaw = parts[5]
            val content = parts[6]

            val tags = if (tagsRaw.isBlank()) {
                emptyList()
            } else {
                tagsRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }

            JournalEntry(
                id = id,
                dayIndex = dayIndex,
                title = title,
                content = content,
                templateType = JournalTemplateType.fromCode(templateCode),
                tags = tags,
                isFavorite = isFav
            )
        }
}

private fun saveJournalEntries(context: Context, entries: List<JournalEntry>) {
    val prefs = context.getSharedPreferences(PREF_JOURNAL, Context.MODE_PRIVATE)
    val raw = entries.joinToString("\n") { e ->
        val safeTitle = e.title.replace("\n", " ")
        val safeContent = e.content.replace("\n", " ")
        val tagsJoined = e.tags.joinToString(",") { it.replace("\n", " ") }
        "${e.id}||${e.dayIndex}||${e.templateType.code}||${if (e.isFavorite) "1" else "0"}||$safeTitle||$tagsJoined||$safeContent"
    }
    prefs.edit().putString(KEY_JOURNAL, raw).apply()
}
