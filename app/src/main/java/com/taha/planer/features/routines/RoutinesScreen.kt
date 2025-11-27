package com.taha.planer.features.routines

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
import androidx.compose.material3.Checkbox
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

private const val PREF_NAME = "planner_routines"
private const val KEY_ROUTINES = "routines_v1"
private const val DEFAULT_POINTS = 8
private const val MILLIS_PER_DAY = 86_400_000L

data class Routine(
    val id: Long,
    val title: String,
    val note: String,
    val streak: Int,
    val lastDoneDayIndex: Int,
    val points: Int
)

private enum class RoutineFilter {
    ALL, ACTIVE, DONE
}

@Composable
fun RoutinesScreen() {
    val context = LocalContext.current

    var routines by remember { mutableStateOf(loadRoutines(context)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }
    var newPointsText by remember { mutableStateOf(DEFAULT_POINTS.toString()) }

    var editTitle by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    var editPointsText by remember { mutableStateOf(DEFAULT_POINTS.toString()) }
    var editingRoutineId by remember { mutableStateOf<Long?>(null) }

    var filter by remember { mutableStateOf(RoutineFilter.ALL) }

    val todayIndex = remember { currentDayIndex() }

    fun persist(updated: List<Routine>) {
        routines = updated
        saveRoutines(context, updated)
    }

    val doneTodayCount = routines.count { isDoneToday(it, todayIndex) }
    val total = routines.size
    val percent = progressPercent(doneTodayCount, total)

    val earnedPoints = routines.filter { isDoneToday(it, todayIndex) }.sumOf { it.points }
    val totalPoints = routines.sumOf { it.points }

    val filteredRoutines = when (filter) {
        RoutineFilter.ALL -> routines
        RoutineFilter.ACTIVE -> routines.filter { !isDoneToday(it, todayIndex) }
        RoutineFilter.DONE -> routines.filter { isDoneToday(it, todayIndex) }
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
                    text = "روال‌های روزانه",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (total == 0)
                        "هنوز هیچ روالی ثبت نشده."
                    else
                        "روال انجام‌شده امروز: $doneTodayCount از $total (${percent}٪)",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (totalPoints > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "امتیاز روال‌های امروز: $earnedPoints از $totalPoints",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = if (total == 0) 0f else percent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChipSimple(
                        text = "همه",
                        selected = filter == RoutineFilter.ALL
                    ) { filter = RoutineFilter.ALL }

                    FilterChipSimple(
                        text = "انجام‌نشده امروز",
                        selected = filter == RoutineFilter.ACTIVE
                    ) { filter = RoutineFilter.ACTIVE }

                    FilterChipSimple(
                        text = "انجام‌شده امروز",
                        selected = filter == RoutineFilter.DONE
                    ) { filter = RoutineFilter.DONE }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (routines.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("مثلاً: روال صبح، روال قبل خواب، روال کار...")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("روی دکمه + بزن تا اولین روال رو بسازی.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredRoutines, key = { it.id }) { routine ->
                            RoutineRow(
                                routine = routine,
                                todayIndex = todayIndex,
                                onCheckedChange = { checked ->
                                    val updated = routines.map {
                                        if (it.id == routine.id) {
                                            if (checked) {
                                                updateRoutineOnDone(it, todayIndex)
                                            } else {
                                                it.copy(streak = 0, lastDoneDayIndex = -1)
                                            }
                                        } else it
                                    }
                                    persist(updated)
                                },
                                onDelete = {
                                    val updated = routines.filterNot { it.id == routine.id }
                                    persist(updated)
                                },
                                onEdit = {
                                    editingRoutineId = routine.id
                                    editTitle = routine.title
                                    editNote = routine.note
                                    editPointsText = routine.points.toString()
                                    showEditDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoutinesLineChart(
                    routines = routines,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = {
                newTitle = ""
                newNote = ""
                newPointsText = DEFAULT_POINTS.toString()
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "روال جدید")
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("روال جدید") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("مثلاً: روال صبح، روال قبل خواب") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newNote,
                            onValueChange = { newNote = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            placeholder = { Text("لیست کارهای این روال رو اینجا بنویس (به‌صورت متن آزاد).") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPointsText,
                            onValueChange = { newPointsText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("امتیاز این روال") },
                            placeholder = { Text(DEFAULT_POINTS.toString()) }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val titleTrimmed = newTitle.trim()
                            val noteTrimmed = newNote.trim()
                            val pts = newPointsText.toIntOrNull() ?: DEFAULT_POINTS
                            if (titleTrimmed.isNotEmpty()) {
                                val updated = routines + Routine(
                                    id = System.currentTimeMillis(),
                                    title = titleTrimmed,
                                    note = noteTrimmed,
                                    streak = 0,
                                    lastDoneDayIndex = -1,
                                    points = pts
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

        if (showEditDialog && editingRoutineId != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("ویرایش روال") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editPointsText,
                            onValueChange = { editPointsText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("امتیاز این روال") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = editingRoutineId
                            val titleTrimmed = editTitle.trim()
                            val noteTrimmed = editNote.trim()
                            val pts = editPointsText.toIntOrNull() ?: DEFAULT_POINTS
                            if (id != null && titleTrimmed.isNotEmpty()) {
                                val updated = routines.map {
                                    if (it.id == id)
                                        it.copy(title = titleTrimmed, note = noteTrimmed, points = pts)
                                    else it
                                }
                                persist(updated)
                            }
                            showEditDialog = false
                        }
                    ) {
                        Text("ذخیره")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("بی‌خیال")
                    }
                }
            )
        }
    }
}

@Composable
private fun RoutineRow(
    routine: Routine,
    todayIndex: Int,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isDoneToday(routine, todayIndex),
            onCheckedChange = onCheckedChange
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = routine.title)
            if (routine.note.isNotBlank()) {
                Text(
                    text = routine.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "رشته: ${routine.streak} روز",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${routine.points} امتیاز",
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "ویرایش"
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "حذف"
            )
        }
    }
}

@Composable
private fun FilterChipSimple(
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

// نمودار خطی بر اساس streak روال‌ها
@Composable
private fun RoutinesLineChart(
    routines: List<Routine>,
    modifier: Modifier = Modifier
) {
    if (routines.isEmpty()) {
        Text(
            text = "نمودار بعد از اضافه‌کردن روال فعال می‌شود.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    val streaks = routines.map { it.streak.toFloat() }
    val maxStreak = streaks.maxOrNull() ?: 0f
    if (maxStreak <= 0f) {
        Text(
            text = "هنوز رشته‌ای ساخته نشده؛ چند روز پشت‌سرهم روال‌ها را انجام بده.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (streaks.size == 1) {
            val y = height * (1f - (streaks[0] / maxStreak))
            drawCircle(
                color = MaterialTheme.colorScheme.primary,
                radius = 6f,
                center = Offset(width / 2f, y)
            )
            return@Canvas
        }

        val stepX = width / (streaks.size - 1).coerceAtLeast(1)
        val path = Path()

        streaks.forEachIndexed { index, v ->
            val normalized = v / maxStreak
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

        streaks.forEachIndexed { index, v ->
            val normalized = v / maxStreak
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

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

private fun isDoneToday(routine: Routine, todayIndex: Int): Boolean =
    routine.lastDoneDayIndex == todayIndex

private fun updateRoutineOnDone(routine: Routine, todayIndex: Int): Routine {
    val last = routine.lastDoneDayIndex
    val newStreak = when {
        last == todayIndex -> routine.streak
        last == todayIndex - 1 -> routine.streak + 1
        else -> 1
    }
    return routine.copy(
        streak = newStreak,
        lastDoneDayIndex = todayIndex
    )
}

private fun progressPercent(done: Int, total: Int): Int {
    if (total == 0) return 0
    return ((done.toFloat() / total.toFloat()) * 100f).toInt()
}

private fun loadRoutines(context: Context): List<Routine> {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_ROUTINES, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 2) return@mapNotNull null
            val id = parts[0].toLongOrNull()mkdir -p app/src/main/java/com/taha/planer/features/routines

cat > app/src/main/java/com/taha/planer/features/routines/RoutinesScreen.kt << 'EOF'
package com.taha.planer.features.routines

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
import androidx.compose.material3.Checkbox
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

private const val PREF_NAME = "planner_routines"
private const val KEY_ROUTINES = "routines_v1"
private const val DEFAULT_POINTS = 8
private const val MILLIS_PER_DAY = 86_400_000L

data class Routine(
    val id: Long,
    val title: String,
    val note: String,
    val streak: Int,
    val lastDoneDayIndex: Int,
    val points: Int
)

private enum class RoutineFilter {
    ALL, ACTIVE, DONE
}

@Composable
fun RoutinesScreen() {
    val context = LocalContext.current

    var routines by remember { mutableStateOf(loadRoutines(context)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }
    var newPointsText by remember { mutableStateOf(DEFAULT_POINTS.toString()) }

    var editTitle by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    var editPointsText by remember { mutableStateOf(DEFAULT_POINTS.toString()) }
    var editingRoutineId by remember { mutableStateOf<Long?>(null) }

    var filter by remember { mutableStateOf(RoutineFilter.ALL) }

    val todayIndex = remember { currentDayIndex() }

    fun persist(updated: List<Routine>) {
        routines = updated
        saveRoutines(context, updated)
    }

    val doneTodayCount = routines.count { isDoneToday(it, todayIndex) }
    val total = routines.size
    val percent = progressPercent(doneTodayCount, total)

    val earnedPoints = routines.filter { isDoneToday(it, todayIndex) }.sumOf { it.points }
    val totalPoints = routines.sumOf { it.points }

    val filteredRoutines = when (filter) {
        RoutineFilter.ALL -> routines
        RoutineFilter.ACTIVE -> routines.filter { !isDoneToday(it, todayIndex) }
        RoutineFilter.DONE -> routines.filter { isDoneToday(it, todayIndex) }
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
                    text = "روال‌های روزانه",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (total == 0)
                        "هنوز هیچ روالی ثبت نشده."
                    else
                        "روال انجام‌شده امروز: $doneTodayCount از $total (${percent}٪)",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (totalPoints > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "امتیاز روال‌های امروز: $earnedPoints از $totalPoints",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = if (total == 0) 0f else percent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChipSimple(
                        text = "همه",
                        selected = filter == RoutineFilter.ALL
                    ) { filter = RoutineFilter.ALL }

                    FilterChipSimple(
                        text = "انجام‌نشده امروز",
                        selected = filter == RoutineFilter.ACTIVE
                    ) { filter = RoutineFilter.ACTIVE }

                    FilterChipSimple(
                        text = "انجام‌شده امروز",
                        selected = filter == RoutineFilter.DONE
                    ) { filter = RoutineFilter.DONE }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (routines.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("مثلاً: روال صبح، روال قبل خواب، روال کار...")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("روی دکمه + بزن تا اولین روال رو بسازی.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredRoutines, key = { it.id }) { routine ->
                            RoutineRow(
                                routine = routine,
                                todayIndex = todayIndex,
                                onCheckedChange = { checked ->
                                    val updated = routines.map {
                                        if (it.id == routine.id) {
                                            if (checked) {
                                                updateRoutineOnDone(it, todayIndex)
                                            } else {
                                                it.copy(streak = 0, lastDoneDayIndex = -1)
                                            }
                                        } else it
                                    }
                                    persist(updated)
                                },
                                onDelete = {
                                    val updated = routines.filterNot { it.id == routine.id }
                                    persist(updated)
                                },
                                onEdit = {
                                    editingRoutineId = routine.id
                                    editTitle = routine.title
                                    editNote = routine.note
                                    editPointsText = routine.points.toString()
                                    showEditDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoutinesLineChart(
                    routines = routines,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = {
                newTitle = ""
                newNote = ""
                newPointsText = DEFAULT_POINTS.toString()
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "روال جدید")
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("روال جدید") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("مثلاً: روال صبح، روال قبل خواب") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newNote,
                            onValueChange = { newNote = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            placeholder = { Text("لیست کارهای این روال رو اینجا بنویس (به‌صورت متن آزاد).") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPointsText,
                            onValueChange = { newPointsText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("امتیاز این روال") },
                            placeholder = { Text(DEFAULT_POINTS.toString()) }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val titleTrimmed = newTitle.trim()
                            val noteTrimmed = newNote.trim()
                            val pts = newPointsText.toIntOrNull() ?: DEFAULT_POINTS
                            if (titleTrimmed.isNotEmpty()) {
                                val updated = routines + Routine(
                                    id = System.currentTimeMillis(),
                                    title = titleTrimmed,
                                    note = noteTrimmed,
                                    streak = 0,
                                    lastDoneDayIndex = -1,
                                    points = pts
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

        if (showEditDialog && editingRoutineId != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("ویرایش روال") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editPointsText,
                            onValueChange = { editPointsText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("امتیاز این روال") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = editingRoutineId
                            val titleTrimmed = editTitle.trim()
                            val noteTrimmed = editNote.trim()
                            val pts = editPointsText.toIntOrNull() ?: DEFAULT_POINTS
                            if (id != null && titleTrimmed.isNotEmpty()) {
                                val updated = routines.map {
                                    if (it.id == id)
                                        it.copy(title = titleTrimmed, note = noteTrimmed, points = pts)
                                    else it
                                }
                                persist(updated)
                            }
                            showEditDialog = false
                        }
                    ) {
                        Text("ذخیره")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("بی‌خیال")
                    }
                }
            )
        }
    }
}

@Composable
private fun RoutineRow(
    routine: Routine,
    todayIndex: Int,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isDoneToday(routine, todayIndex),
            onCheckedChange = onCheckedChange
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = routine.title)
            if (routine.note.isNotBlank()) {
                Text(
                    text = routine.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "رشته: ${routine.streak} روز",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${routine.points} امتیاز",
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "ویرایش"
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "حذف"
            )
        }
    }
}

@Composable
private fun FilterChipSimple(
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

// نمودار خطی بر اساس streak روال‌ها
@Composable
private fun RoutinesLineChart(
    routines: List<Routine>,
    modifier: Modifier = Modifier
) {
    if (routines.isEmpty()) {
        Text(
            text = "نمودار بعد از اضافه‌کردن روال فعال می‌شود.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    val streaks = routines.map { it.streak.toFloat() }
    val maxStreak = streaks.maxOrNull() ?: 0f
    if (maxStreak <= 0f) {
        Text(
            text = "هنوز رشته‌ای ساخته نشده؛ چند روز پشت‌سرهم روال‌ها را انجام بده.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (streaks.size == 1) {
            val y = height * (1f - (streaks[0] / maxStreak))
            drawCircle(
                color = MaterialTheme.colorScheme.primary,
                radius = 6f,
                center = Offset(width / 2f, y)
            )
            return@Canvas
        }

        val stepX = width / (streaks.size - 1).coerceAtLeast(1)
        val path = Path()

        streaks.forEachIndexed { index, v ->
            val normalized = v / maxStreak
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

        streaks.forEachIndexed { index, v ->
            val normalized = v / maxStreak
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

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

private fun isDoneToday(routine: Routine, todayIndex: Int): Boolean =
    routine.lastDoneDayIndex == todayIndex

private fun updateRoutineOnDone(routine: Routine, todayIndex: Int): Routine {
    val last = routine.lastDoneDayIndex
    val newStreak = when {
        last == todayIndex -> routine.streak
        last == todayIndex - 1 -> routine.streak + 1
        else -> 1
    }
    return routine.copy(
        streak = newStreak,
        lastDoneDayIndex = todayIndex
    )
}

private fun progressPercent(done: Int, total: Int): Int {
    if (total == 0) return 0
    return ((done.toFloat() / total.toFloat()) * 100f).toInt()
}

private fun loadRoutines(context: Context): List<Routine> {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_ROUTINES, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 2) return@mapNotNull null
mkdir -p app/src/main/java/com/taha/planer/features/routines

cat > app/src/main/java/com/taha/planer/features/routines/RoutinesScreen.kt << 'EOF'
package com.taha.planer.features.routines

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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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

private const val PREF_NAME = "planner_routines"
private const val KEY_ROUTINES = "routines_v1"
private const val DEFAULT_POINTS = 8
private const val MILLIS_PER_DAY = 86_400_000L

data class Routine(
    val id: Long,
    val title: String,
    val note: String,
    val streak: Int,
    val lastDoneDayIndex: Int,
    val points: Int
)

private enum class RoutineFilter {
    ALL, ACTIVE, DONE
}

@Composable
fun RoutinesScreen() {
    val context = LocalContext.current

    var routines by remember { mutableStateOf(loadRoutines(context)) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    var newTitle by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }
    var newPointsText by remember { mutableStateOf(DEFAULT_POINTS.toString()) }

    var editTitle by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    var editPointsText by remember { mutableStateOf(DEFAULT_POINTS.toString()) }
    var editingRoutineId by remember { mutableStateOf<Long?>(null) }

    var filter by remember { mutableStateOf(RoutineFilter.ALL) }

    val todayIndex = remember { currentDayIndex() }

    fun persist(updated: List<Routine>) {
        routines = updated
        saveRoutines(context, updated)
    }

    val doneTodayCount = routines.count { isDoneToday(it, todayIndex) }
    val total = routines.size
    val percent = progressPercent(doneTodayCount, total)

    val earnedPoints = routines.filter { isDoneToday(it, todayIndex) }.sumOf { it.points }
    val totalPoints = routines.sumOf { it.points }

    val filteredRoutines = when (filter) {
        RoutineFilter.ALL -> routines
        RoutineFilter.ACTIVE -> routines.filter { !isDoneToday(it, todayIndex) }
        RoutineFilter.DONE -> routines.filter { isDoneToday(it, todayIndex) }
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
                    text = "روال‌های روزانه",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (total == 0)
                        "هنوز هیچ روالی ثبت نشده."
                    else
                        "روال انجام‌شده امروز: $doneTodayCount از $total (${percent}٪)",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (totalPoints > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "امتیاز روال‌های امروز: $earnedPoints از $totalPoints",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = if (total == 0) 0f else percent / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChipSimple(
                        text = "همه",
                        selected = filter == RoutineFilter.ALL
                    ) { filter = RoutineFilter.ALL }

                    FilterChipSimple(
                        text = "انجام‌نشده امروز",
                        selected = filter == RoutineFilter.ACTIVE
                    ) { filter = RoutineFilter.ACTIVE }

                    FilterChipSimple(
                        text = "انجام‌شده امروز",
                        selected = filter == RoutineFilter.DONE
                    ) { filter = RoutineFilter.DONE }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (routines.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 24.dp),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("مثلاً: روال صبح، روال قبل خواب، روال کار...")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("روی دکمه + بزن تا اولین روال رو بسازی.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredRoutines, key = { it.id }) { routine ->
                            RoutineRow(
                                routine = routine,
                                todayIndex = todayIndex,
                                onCheckedChange = { checked ->
                                    val updated = routines.map {
                                        if (it.id == routine.id) {
                                            if (checked) {
                                                updateRoutineOnDone(it, todayIndex)
                                            } else {
                                                it.copy(streak = 0, lastDoneDayIndex = -1)
                                            }
                                        } else it
                                    }
                                    persist(updated)
                                },
                                onDelete = {
                                    val updated = routines.filterNot { it.id == routine.id }
                                    persist(updated)
                                },
                                onEdit = {
                                    editingRoutineId = routine.id
                                    editTitle = routine.title
                                    editNote = routine.note
                                    editPointsText = routine.points.toString()
                                    showEditDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                RoutinesLineChart(
                    routines = routines,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                )
            }
        }

        FloatingActionButton(
            onClick = {
                newTitle = ""
                newNote = ""
                newPointsText = DEFAULT_POINTS.toString()
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "روال جدید")
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("روال جدید") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("مثلاً: روال صبح، روال قبل خواب") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newNote,
                            onValueChange = { newNote = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            placeholder = { Text("لیست کارهای این روال رو اینجا بنویس (به‌صورت متن آزاد).") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPointsText,
                            onValueChange = { newPointsText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("امتیاز این روال") },
                            placeholder = { Text(DEFAULT_POINTS.toString()) }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val titleTrimmed = newTitle.trim()
                            val noteTrimmed = newNote.trim()
                            val pts = newPointsText.toIntOrNull() ?: DEFAULT_POINTS
                            if (titleTrimmed.isNotEmpty()) {
                                val updated = routines + Routine(
                                    id = System.currentTimeMillis(),
                                    title = titleTrimmed,
                                    note = noteTrimmed,
                                    streak = 0,
                                    lastDoneDayIndex = -1,
                                    points = pts
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

        if (showEditDialog && editingRoutineId != null) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("ویرایش روال") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editPointsText,
                            onValueChange = { editPointsText = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("امتیاز این روال") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val id = editingRoutineId
                            val titleTrimmed = editTitle.trim()
                            val noteTrimmed = editNote.trim()
                            val pts = editPointsText.toIntOrNull() ?: DEFAULT_POINTS
                            if (id != null && titleTrimmed.isNotEmpty()) {
                                val updated = routines.map {
                                    if (it.id == id)
                                        it.copy(title = titleTrimmed, note = noteTrimmed, points = pts)
                                    else it
                                }
                                persist(updated)
                            }
                            showEditDialog = false
                        }
                    ) {
                        Text("ذخیره")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("بی‌خیال")
                    }
                }
            )
        }
    }
}

@Composable
private fun RoutineRow(
    routine: Routine,
    todayIndex: Int,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isDoneToday(routine, todayIndex),
            onCheckedChange = onCheckedChange
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = routine.title)
            if (routine.note.isNotBlank()) {
                Text(
                    text = routine.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = "رشته: ${routine.streak} روز",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${routine.points} امتیاز",
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = "ویرایش"
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "حذف"
            )
        }
    }
}

@Composable
private fun FilterChipSimple(
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

// نمودار خطی بر اساس streak روال‌ها
@Composable
private fun RoutinesLineChart(
    routines: List<Routine>,
    modifier: Modifier = Modifier
) {
    if (routines.isEmpty()) {
        Text(
            text = "نمودار بعد از اضافه‌کردن روال فعال می‌شود.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    val streaks = routines.map { it.streak.toFloat() }
    val maxStreak = streaks.maxOrNull() ?: 0f
    if (maxStreak <= 0f) {
        Text(
            text = "هنوز رشته‌ای ساخته نشده؛ چند روز پشت‌سرهم روال‌ها را انجام بده.",
            style = MaterialTheme.typography.bodySmall
        )
        return
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        if (streaks.size == 1) {
            val y = height * (1f - (streaks[0] / maxStreak))
            drawCircle(
                color = MaterialTheme.colorScheme.primary,
                radius = 6f,
                center = Offset(width / 2f, y)
            )
            return@Canvas
        }

        val stepX = width / (streaks.size - 1).coerceAtLeast(1)
        val path = Path()

        streaks.forEachIndexed { index, v ->
            val normalized = v / maxStreak
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

        streaks.forEachIndexed { index, v ->
            val normalized = v / maxStreak
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

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

private fun isDoneToday(routine: Routine, todayIndex: Int): Boolean =
    routine.lastDoneDayIndex == todayIndex

private fun updateRoutineOnDone(routine: Routine, todayIndex: Int): Routine {
    val last = routine.lastDoneDayIndex
    val newStreak = when {
        last == todayIndex -> routine.streak
        last == todayIndex - 1 -> routine.streak + 1
        else -> 1
    }
    return routine.copy(
        streak = newStreak,
        lastDoneDayIndex = todayIndex
    )
}

private fun progressPercent(done: Int, total: Int): Int {
    if (total == 0) return 0
    return ((done.toFloat() / total.toFloat()) * 100f).toInt()
}

private fun loadRoutines(context: Context): List<Routine> {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_ROUTINES, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 2) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val title = parts[1]
            val note = parts.getOrNull(2) ?: ""
            val streak = parts.getOrNull(3)?.toIntOrNull() ?: 0
            val lastIdx = parts.getOrNull(4)?.toIntOrNull() ?: -1
            val points = parts.getOrNull(5)?.toIntOrNull() ?: DEFAULT_POINTS
            Routine(
                id = id,
                title = title,
                note = note,
                streak = streak,
                lastDoneDayIndex = lastIdx,
                points = points
            )
        }
}

private fun saveRoutines(context: Context, routines: List<Routine>) {
    val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    val raw = routines.joinToString("\n") { r ->
        val safeTitle = r.title.replace("\n", " ")
        val safeNote = r.note.replace("\n", " ")
        "${r.id}||$safeTitle||$safeNote||${r.streak}||${r.lastDoneDayIndex}||${r.points}"
    }
    prefs.edit().putString(KEY_ROUTINES, raw).apply()
}
