package com.taha.planer.features.media

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private const val MILLIS_PER_DAY = 86_400_000L

// SharedPreferences keys
private const val PREF_MEDIA_BOOKS = "planner_media_books"
private const val KEY_MEDIA_BOOKS = "media_books_v1"

private const val PREF_MEDIA_VIDEO = "planner_media_video"
private const val KEY_MEDIA_VIDEO = "media_video_v1"

private const val PREF_MEDIA_ONLINE = "planner_media_online"
private const val KEY_MEDIA_ONLINE_LOGS = "media_online_logs_v1"
private const val KEY_MEDIA_ONLINE_FUN = "media_online_fun_budget"
private const val KEY_MEDIA_ONLINE_LEARN = "media_online_learn_budget"
private const val KEY_MEDIA_ONLINE_WORK = "media_online_work_budget"

private const val DEFAULT_FUN_BUDGET = 60   // دقیقه
private const val DEFAULT_LEARN_BUDGET = 30
private const val DEFAULT_WORK_BUDGET = 60

private enum class MediaTab {
    BOOKS, VIDEO, ONLINE
}

private enum class BookStatus(val code: Int, val label: String) {
    TO_READ(0, "برای خواندن"),
    READING(1, "در حال خواندن"),
    DONE(2, "تمام شده");

    companion object {
        fun fromCode(code: Int): BookStatus =
            values().find { it.code == code } ?: TO_READ
    }
}

private enum class VideoType(val code: Int, val label: String) {
    MOVIE(0, "فیلم"),
    SERIES(1, "سریال");

    companion object {
        fun fromCode(code: Int): VideoType =
            values().find { it.code == code } ?: MOVIE
    }
}

private enum class VideoStatus(val code: Int, val label: String) {
    TO_WATCH(0, "برای دیدن"),
    WATCHING(1, "در حال دیدن"),
    DONE(2, "تمام شده");

    companion object {
        fun fromCode(code: Int): VideoStatus =
            values().find { it.code == code } ?: TO_WATCH
    }
}

data class BookItem(
    val id: Long,
    val title: String,
    val author: String,
    val totalPages: Int,
    val currentPage: Int,
    val status: BookStatus,
    val summary: String
)

data class VideoItem(
    val id: Long,
    val title: String,
    val type: VideoType,
    val season: Int,
    val episode: Int,
    val status: VideoStatus,
    val platform: String,
    val note: String
)

data class OnlineLog(
    val dayIndex: Int,
    val funMinutes: Int,
    val learnMinutes: Int,
    val workMinutes: Int
)

@Composable
fun MediaScreen() {
    var selectedTab by remember { mutableStateOf(MediaTab.BOOKS) }

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
                    text = "مدیا، فیلم/سریال و کتاب",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "کتاب‌ها، فیلم‌ها و زمان فضای مجازی را اینجا مدیریت کن.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                MediaTabsRow(
                    selected = selectedTab,
                    onSelect = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    MediaTab.BOOKS -> BooksTab()
                    MediaTab.VIDEO -> VideoTab()
                    MediaTab.ONLINE -> OnlineTab()
                }
            }
        }
    }
}

@Composable
private fun MediaTabsRow(
    selected: MediaTab,
    onSelect: (MediaTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SimpleTabChip(
            text = "کتاب‌ها",
            selected = selected == MediaTab.BOOKS
        ) { onSelect(MediaTab.BOOKS) }

        SimpleTabChip(
            text = "فیلم و سریال",
            selected = selected == MediaTab.VIDEO
        ) { onSelect(MediaTab.VIDEO) }

        SimpleTabChip(
            text = "فضای مجازی",
            selected = selected == MediaTab.ONLINE
        ) { onSelect(MediaTab.ONLINE) }
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

// ----------------- BOOKS TAB -----------------

@Composable
private fun BooksTab() {
    val context = LocalContext.current
    var books by remember { mutableStateOf(loadBooks(context)) }

    fun persist(updated: List<BookItem>) {
        books = updated
        saveBooks(context, updated)
    }

    val readingCount = books.count { it.status == BookStatus.READING }
    val doneCount = books.count { it.status == BookStatus.DONE }

    var showDialog by remember { mutableStateOf(false) }
    var editingBook by remember { mutableStateOf<BookItem?>(null) }

    var draftTitle by remember { mutableStateOf("") }
    var draftAuthor by remember { mutableStateOf("") }
    var draftTotalPages by remember { mutableStateOf("200") }
    var draftCurrentPage by remember { mutableStateOf("0") }
    var draftStatus by remember { mutableStateOf(BookStatus.TO_READ) }
    var draftSummary by remember { mutableStateOf("") }

    fun openForNew() {
        editingBook = null
        draftTitle = ""
        draftAuthor = ""
        draftTotalPages = "200"
        draftCurrentPage = "0"
        draftStatus = BookStatus.TO_READ
        draftSummary = ""
        showDialog = true
    }

    fun openForEdit(book: BookItem) {
        editingBook = book
        draftTitle = book.title
        draftAuthor = book.author
        draftTotalPages = book.totalPages.toString()
        draftCurrentPage = book.currentPage.toString()
        draftStatus = book.status
        draftSummary = book.summary
        showDialog = true
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "کتاب‌ها",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "در حال خواندن: $readingCount  |  تمام شده: $doneCount",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(books, key = { it.id }) { book ->
                BookRow(
                    book = book,
                    onEdit = { openForEdit(book) },
                    onQuickProgress = {
                        val newPage = (book.currentPage + 10).coerceAtMost(book.totalPages)
                        val updated = book.copy(
                            currentPage = newPage,
                            status = if (newPage >= book.totalPages) BookStatus.DONE else BookStatus.READING
                        )
                        persist(books.map { if (it.id == book.id) updated else it })
                    },
                    onDelete = {
                        persist(books.filterNot { it.id == book.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FloatingActionButton(
            onClick = { openForNew() },
            modifier = Modifier
                .align(Alignment.End)
                .padding(4.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "کتاب جدید")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(text = if (editingBook == null) "کتاب جدید" else "ویرایش کتاب")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("عنوان کتاب") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = draftAuthor,
                        onValueChange = { draftAuthor = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("نویسنده") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = draftTotalPages,
                            onValueChange = { draftTotalPages = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("تعداد صفحات") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = draftCurrentPage,
                            onValueChange = { draftCurrentPage = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("صفحه فعلی") },
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "وضعیت:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatusChip(
                            text = BookStatus.TO_READ.label,
                            selected = draftStatus == BookStatus.TO_READ
                        ) { draftStatus = BookStatus.TO_READ }

                        StatusChip(
                            text = BookStatus.READING.label,
                            selected = draftStatus == BookStatus.READING
                        ) { draftStatus = BookStatus.READING }

                        StatusChip(
                            text = BookStatus.DONE.label,
                            selected = draftStatus == BookStatus.DONE
                        ) { draftStatus = BookStatus.DONE }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = draftSummary,
                        onValueChange = { draftSummary = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        label = { Text("خلاصه / نکته‌ها / نقل‌قول‌ها") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = draftTitle.trim()
                        val author = draftAuthor.trim()
                        val total = draftTotalPages.toIntOrNull() ?: 0
                        val current = draftCurrentPage.toIntOrNull() ?: 0

                        if (title.isNotEmpty() && total > 0) {
                            val clampedCurrent = current.coerceIn(0, total)
                            val finalStatus =
                                if (draftStatus == BookStatus.DONE || clampedCurrent >= total)
                                    BookStatus.DONE
                                else if (draftStatus == BookStatus.READING || clampedCurrent > 0)
                                    BookStatus.READING
                                else
                                    BookStatus.TO_READ

                            val book = BookItem(
                                id = editingBook?.id ?: System.currentTimeMillis(),
                                title = title,
                                author = author,
                                totalPages = total,
                                currentPage = clampedCurrent,
                                status = finalStatus,
                                summary = draftSummary.trim()
                            )

                            val updated = if (editingBook == null) {
                                books + book
                            } else {
                                books.map { if (it.id == book.id) book else it }
                            }
                            persist(updated)
                            showDialog = false
                        }
                    }
                ) {
                    Text("ذخیره")
                }
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
private fun StatusChip(
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
private fun BookRow(
    book: BookItem,
    onEdit: () -> Unit,
    onQuickProgress: () -> Unit,
    onDelete: () -> Unit
) {
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
                        text = book.title,
                        fontWeight = FontWeight.Bold
                    )
                    if (book.author.isNotBlank()) {
                        Text(
                            text = "نویسنده: ${book.author}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val percent =
                        if (book.totalPages > 0)
                            (book.currentPage.toFloat() / book.totalPages.toFloat() * 100f).toInt()
                        else 0
                    Text(
                        text = "${book.currentPage} / ${book.totalPages} صفحه (${percent.coerceIn(0, 100)}٪) - ${book.status.label}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(onClick = onQuickProgress) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "۱۰ صفحه جلو برو"
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

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = if (book.totalPages > 0)
                    (book.currentPage.toFloat() / book.totalPages.toFloat()).coerceIn(0f, 1f)
                else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
            )

            if (book.summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.summary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ----------------- VIDEO TAB -----------------

@Composable
private fun VideoTab() {
    val context = LocalContext.current
    var videos by remember { mutableStateOf(loadVideos(context)) }

    fun persist(updated: List<VideoItem>) {
        videos = updated
        saveVideos(context, updated)
    }

    var showDialog by remember { mutableStateOf(false) }
    var editingVideo by remember { mutableStateOf<VideoItem?>(null) }

    var draftTitle by remember { mutableStateOf("") }
    var draftType by remember { mutableStateOf(VideoType.MOVIE) }
    var draftSeason by remember { mutableStateOf("1") }
    var draftEpisode by remember { mutableStateOf("1") }
    var draftStatus by remember { mutableStateOf(VideoStatus.TO_WATCH) }
    var draftPlatform by remember { mutableStateOf("") }
    var draftNote by remember { mutableStateOf("") }

    fun openNew() {
        editingVideo = null
        draftTitle = ""
        draftType = VideoType.MOVIE
        draftSeason = "1"
        draftEpisode = "1"
        draftStatus = VideoStatus.TO_WATCH
        draftPlatform = ""
        draftNote = ""
        showDialog = true
    }

    fun openEdit(item: VideoItem) {
        editingVideo = item
        draftTitle = item.title
        draftType = item.type
        draftSeason = item.season.toString()
        draftEpisode = item.episode.toString()
        draftStatus = item.status
        draftPlatform = item.platform
        draftNote = item.note
        showDialog = true
    }

    val watchingCount = videos.count { it.status == VideoStatus.WATCHING }
    val toWatchCount = videos.count { it.status == VideoStatus.TO_WATCH }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "فیلم و سریال",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "در حال تماشا: $watchingCount  |  در لیست انتظار: $toWatchCount",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(videos, key = { it.id }) { item ->
                VideoRow(
                    item = item,
                    onNextEpisode = {
                        if (item.type == VideoType.SERIES) {
                            val updated = item.copy(
                                episode = item.episode + 1,
                                status = VideoStatus.WATCHING
                            )
                            persist(videos.map { if (it.id == item.id) updated else it })
                        } else {
                            val updated = item.copy(status = VideoStatus.DONE)
                            persist(videos.map { if (it.id == item.id) updated else it })
                        }
                    },
                    onEdit = { openEdit(item) },
                    onDelete = {
                        persist(videos.filterNot { it.id == item.id })
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FloatingActionButton(
            onClick = { openNew() },
            modifier = Modifier
                .align(Alignment.End)
                .padding(4.dp)
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "مدیای جدید")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(text = if (editingVideo == null) "مدیای جدید" else "ویرایش مدیا")
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("عنوان") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "نوع:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatusChip(
                            text = VideoType.MOVIE.label,
                            selected = draftType == VideoType.MOVIE
                        ) { draftType = VideoType.MOVIE }

                        StatusChip(
                            text = VideoType.SERIES.label,
                            selected = draftType == VideoType.SERIES
                        ) { draftType = VideoType.SERIES }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (draftType == VideoType.SERIES) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = draftSeason,
                                onValueChange = { draftSeason = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("فصل") },
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = draftEpisode,
                                onValueChange = { draftEpisode = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("قسمت فعلی") },
                                singleLine = true
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Text(
                        text = "وضعیت:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatusChip(
                            text = VideoStatus.TO_WATCH.label,
                            selected = draftStatus == VideoStatus.TO_WATCH
                        ) { draftStatus = VideoStatus.TO_WATCH }

                        StatusChip(
                            text = VideoStatus.WATCHING.label,
                            selected = draftStatus == VideoStatus.WATCHING
                        ) { draftStatus = VideoStatus.WATCHING }

                        StatusChip(
                            text = VideoStatus.DONE.label,
                            selected = draftStatus == VideoStatus.DONE
                        ) { draftStatus = VideoStatus.DONE }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = draftPlatform,
                        onValueChange = { draftPlatform = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("پلتفرم (Netflix / YouTube / ...)") },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = draftNote,
                        onValueChange = { draftNote = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 80.dp),
                        label = { Text("یادداشت / دلیل دیدن / نکته‌ها") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = draftTitle.trim()
                        if (title.isNotEmpty()) {
                            val season = draftSeason.toIntOrNull() ?: 1
                            val episode = draftEpisode.toIntOrNull() ?: 1

                            val item = VideoItem(
                                id = editingVideo?.id ?: System.currentTimeMillis(),
                                title = title,
                                type = draftType,
                                season = season,
                                episode = episode,
                                status = draftStatus,
                                platform = draftPlatform.trim(),
                                note = draftNote.trim()
                            )

                            val updated = if (editingVideo == null) {
                                videos + item
                            } else {
                                videos.map { if (it.id == item.id) item else it }
                            }
                            persist(updated)
                            showDialog = false
                        }
                    }
                ) {
                    Text("ذخیره")
                }
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
private fun VideoRow(
    item: VideoItem,
    onNextEpisode: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                        text = item.title,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "نوع: ${item.type.label}  |  وضعیت: ${item.status.label}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (item.type == VideoType.SERIES) {
                        Text(
                            text = "فصل ${item.season} - قسمت ${item.episode}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (item.platform.isNotBlank()) {
                        Text(
                            text = "پلتفرم: ${item.platform}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                IconButton(onClick = onNextEpisode) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "قسمت بعدی / تمام کردن"
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

            if (item.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// ----------------- ONLINE TAB -----------------

@Composable
private fun OnlineTab() {
    val context = LocalContext.current
    var logs by remember { mutableStateOf(loadOnlineLogs(context)) }
    var budgets by remember { mutableStateOf(loadOnlineBudgets(context)) }

    fun persist(newLogs: List<OnlineLog>, newBudgets: Triple<Int, Int, Int> = budgets) {
        logs = newLogs
        budgets = newBudgets
        saveOnlineState(context, newLogs, newBudgets)
    }

    val todayIndex = currentDayIndex()
    val today = logs.find { it.dayIndex == todayIndex }
        ?: OnlineLog(todayIndex, 0, 0, 0)

    var draftFunBudget by remember { mutableStateOf(budgets.first.toString()) }
    var draftLearnBudget by remember { mutableStateOf(budgets.second.toString()) }
    var draftWorkBudget by remember { mutableStateOf(budgets.third.toString()) }

    val last7 = logs.sortedBy { it.dayIndex }.takeLast(7)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "فضای مجازی و زمان آنلاین",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "قبل از تفریح، اول کارهای ضروری آنلاین و آموزش را انجام بده.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "امروز",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        OnlineTodayRow(
            today = today,
            budgets = budgets,
            onChange = { newToday ->
                val updated = mergeOnlineLog(logs, newToday)
                persist(updated, budgets)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "بودجه زمانی روزانه (دقیقه)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = draftFunBudget,
                onValueChange = { draftFunBudget = it },
                modifier = Modifier.weight(1f),
                label = { Text("تفریحی") },
                singleLine = true
            )
            OutlinedTextField(
                value = draftLearnBudget,
                onValueChange = { draftLearnBudget = it },
                modifier = Modifier.weight(1f),
                label = { Text("آموزشی") },
                singleLine = true
            )
            OutlinedTextField(
                value = draftWorkBudget,
                onValueChange = { draftWorkBudget = it },
                modifier = Modifier.weight(1f),
                label = { Text("کاری") },
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        TextButton(
            onClick = {
                val funB = draftFunBudget.toIntOrNull() ?: budgets.first
                val learnB = draftLearnBudget.toIntOrNull() ?: budgets.second
                val workB = draftWorkBudget.toIntOrNull() ?: budgets.third
                val triple = Triple(funB, learnB, workB)
                persist(logs, triple)
            }
        ) {
            Text("ذخیره بودجه‌ها")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "خلاصه ۷ روز اخیر",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(last7.reversed()) { log ->
                Text(
                    text = "روز ${log.dayIndex}: تفریحی ${log.funMinutes} دقیقه، آموزشی ${log.learnMinutes}، کاری ${log.workMinutes}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun OnlineTodayRow(
    today: OnlineLog,
    budgets: Triple<Int, Int, Int>,
    onChange: (OnlineLog) -> Unit
) {
    Column {
        fun line(
            title: String,
            used: Int,
            budget: Int,
            onPlus5: () -> Unit,
            onPlus10: () -> Unit
        ) {
            val percent =
                if (budget > 0) (used.toFloat() / budget.toFloat() * 100f).toInt() else 0
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$title: $used / $budget دقیقه (${percent.coerceIn(0, 300)}٪)",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TextButton(onClick = onPlus5) { Text("+۵ دقیقه") }
                    TextButton(onClick = onPlus10) { Text("+۱۰ دقیقه") }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        line(
            title = "تفریحی",
            used = today.funMinutes,
            budget = budgets.first,
            onPlus5 = { onChange(today.copy(funMinutes = today.funMinutes + 5)) },
            onPlus10 = { onChange(today.copy(funMinutes = today.funMinutes + 10)) }
        )

        line(
            title = "آموزشی",
            used = today.learnMinutes,
            budget = budgets.second,
            onPlus5 = { onChange(today.copy(learnMinutes = today.learnMinutes + 5)) },
            onPlus10 = { onChange(today.copy(learnMinutes = today.learnMinutes + 10)) }
        )

        line(
            title = "کاری",
            used = today.workMinutes,
            budget = budgets.third,
            onPlus5 = { onChange(today.copy(workMinutes = today.workMinutes + 5)) },
            onPlus10 = { onChange(today.copy(workMinutes = today.workMinutes + 10)) }
        )
    }
}

// ----------------- storage helpers -----------------

private fun currentDayIndex(): Int =
    (System.currentTimeMillis() / MILLIS_PER_DAY).toInt()

// BOOKS

private fun loadBooks(context: Context): List<BookItem> {
    val prefs = context.getSharedPreferences(PREF_MEDIA_BOOKS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_MEDIA_BOOKS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 7) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val title = parts[1]
            val author = parts[2]
            val totalPages = parts[3].toIntOrNull() ?: 0
            val currentPage = parts[4].toIntOrNull() ?: 0
            val statusCode = parts[5].toIntOrNull() ?: 0
            val summary = parts[6]
            BookItem(
                id = id,
                title = title,
                author = author,
                totalPages = totalPages,
                currentPage = currentPage,
                status = BookStatus.fromCode(statusCode),
                summary = summary
            )
        }
}

private fun saveBooks(context: Context, books: List<BookItem>) {
    val prefs = context.getSharedPreferences(PREF_MEDIA_BOOKS, Context.MODE_PRIVATE)
    val raw = books.joinToString("\n") { b ->
        val safeTitle = b.title.replace("\n", " ")
        val safeAuthor = b.author.replace("\n", " ")
        val safeSummary = b.summary.replace("\n", " ")
        "${b.id}||$safeTitle||$safeAuthor||${b.totalPages}||${b.currentPage}||${b.status.code}||$safeSummary"
    }
    prefs.edit().putString(KEY_MEDIA_BOOKS, raw).apply()
}

// VIDEO

private fun loadVideos(context: Context): List<VideoItem> {
    val prefs = context.getSharedPreferences(PREF_MEDIA_VIDEO, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_MEDIA_VIDEO, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 8) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val title = parts[1]
            val typeCode = parts[2].toIntOrNull() ?: 0
            val season = parts[3].toIntOrNull() ?: 1
            val episode = parts[4].toIntOrNull() ?: 1
            val statusCode = parts[5].toIntOrNull() ?: 0
            val platform = parts[6]
            val note = parts[7]
            VideoItem(
                id = id,
                title = title,
                type = VideoType.fromCode(typeCode),
                season = season,
                episode = episode,
                status = VideoStatus.fromCode(statusCode),
                platform = platform,
                note = note
            )
        }
}

private fun saveVideos(context: Context, videos: List<VideoItem>) {
    val prefs = context.getSharedPreferences(PREF_MEDIA_VIDEO, Context.MODE_PRIVATE)
    val raw = videos.joinToString("\n") { v ->
        val safeTitle = v.title.replace("\n", " ")
        val safePlatform = v.platform.replace("\n", " ")
        val safeNote = v.note.replace("\n", " ")
        "${v.id}||$safeTitle||${v.type.code}||${v.season}||${v.episode}||${v.status.code}||$safePlatform||$safeNote"
    }
    prefs.edit().putString(KEY_MEDIA_VIDEO, raw).apply()
}

// ONLINE

private fun loadOnlineLogs(context: Context): List<OnlineLog> {
    val prefs = context.getSharedPreferences(PREF_MEDIA_ONLINE, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_MEDIA_ONLINE_LOGS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 4) return@mapNotNull null
            val dayIndex = parts[0].toIntOrNull() ?: return@mapNotNull null
            val funM = parts[1].toIntOrNull() ?: 0
            val learnM = parts[2].toIntOrNull() ?: 0
            val workM = parts[3].toIntOrNull() ?: 0
            OnlineLog(
                dayIndex = dayIndex,
                funMinutes = funM,
                learnMinutes = learnM,
                workMinutes = workM
            )
        }
}

private fun loadOnlineBudgets(context: Context): Triple<Int, Int, Int> {
    val prefs = context.getSharedPreferences(PREF_MEDIA_ONLINE, Context.MODE_PRIVATE)
    val funB = prefs.getInt(KEY_MEDIA_ONLINE_FUN, DEFAULT_FUN_BUDGET)
    val learnB = prefs.getInt(KEY_MEDIA_ONLINE_LEARN, DEFAULT_LEARN_BUDGET)
    val workB = prefs.getInt(KEY_MEDIA_ONLINE_WORK, DEFAULT_WORK_BUDGET)
    return Triple(funB, learnB, workB)
}

private fun saveOnlineState(
    context: Context,
    logs: List<OnlineLog>,
    budgets: Triple<Int, Int, Int>
) {
    val prefs = context.getSharedPreferences(PREF_MEDIA_ONLINE, Context.MODE_PRIVATE)
    val raw = logs.joinToString("\n") { l ->
        "${l.dayIndex}||${l.funMinutes}||${l.learnMinutes}||${l.workMinutes}"
    }
    prefs.edit()
        .putString(KEY_MEDIA_ONLINE_LOGS, raw)
        .putInt(KEY_MEDIA_ONLINE_FUN, budgets.first)
        .putInt(KEY_MEDIA_ONLINE_LEARN, budgets.second)
        .putInt(KEY_MEDIA_ONLINE_WORK, budgets.third)
        .apply()
}

private fun mergeOnlineLog(
    all: List<OnlineLog>,
    updated: OnlineLog
): List<OnlineLog> {
    val without = all.filterNot { it.dayIndex == updated.dayIndex }
    return (without + updated).sortedBy { it.dayIndex }
}
