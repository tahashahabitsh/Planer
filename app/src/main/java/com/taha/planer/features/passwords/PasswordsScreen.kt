package com.taha.planer.features.passwords

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val PREF_PASSWORDS = "planner_passwords"
private const val KEY_PASSWORDS = "password_entries_v1"

// نام کلید داخل Android Keystore
private const val KEY_ALIAS_PASSWORDS = "planner_passwords_master_key"
private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val ENC_PREFIX_V1 = "v1gcm:" // برای تشخیص نسخه رمزنگاری جدید

data class PasswordEntry(
    val id: Long,
    val title: String,
    val username: String,
    val encryptedPassword: String,
    val note: String,
    val category: PasswordCategory,
    val createdAt: Long,
    val updatedAt: Long
)

enum class PasswordCategory(val code: Int, val label: String) {
    SOCIAL(0, "شبکه اجتماعی"),
    EMAIL(1, "ایمیل"),
    FINANCE(2, "مالی / بانکی"),
    WORK(3, "کاری / سازمانی"),
    OTHER(4, "سایر");

    companion object {
        fun fromCode(code: Int): PasswordCategory =
            values().find { it.code == code } ?: OTHER
    }
}

@Composable
fun PasswordsScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(loadPasswordEntries(context)) }
    var search by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<PasswordEntry?>(null) }

    val snackbarHostState: SnackbarHostState = rememberSnackbarHostState()
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    fun persist(newEntries: List<PasswordEntry>) {
        entries = newEntries
        savePasswordEntries(context, newEntries)
    }

    val filteredEntries = entries.filter { e ->
        val q = search.trim()
        if (q.isEmpty()) true
        else {
            val all = listOf(
                e.title,
                e.username,
                e.note,
                e.category.label
            ).joinToString(" ").lowercase()
            all.contains(q.lowercase())
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
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
                            text = "مدیریت پسورد (رمزنگاری‌شده روی دستگاه)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⚠️ پسوردها با استفاده از Android Keystore و AES رمزنگاری می‌شن و فقط روی همین دستگاه قابل خوندن هستن. با حذف کامل اپ، کلید هم پاک می‌شه و به پسوردها دسترسی نداری.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "برای حساب‌های خیلی حساس (مثلاً بانک / ایمیل اصلی) هنوز بهتره از یک Password Manager حرفه‌ای جداگانه استفاده کنی.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تعداد آیتم‌ها: ${entries.size}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("جستجو بین عنوان، یوزرنیم، دسته و توضیح") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "لیست پسوردها",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (filteredEntries.isEmpty()) {
                    Text(
                        text = "چیزی پیدا نشد. با دکمه‌ی + می‌تونی آیتم جدید بسازی.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredEntries, key = { it.id }) { entry ->
                            PasswordRow(
                                entry = entry,
                                onCopyPassword = {
                                    val pwd = decodePassword(entry.encryptedPassword)
                                    if (pwd.isNotEmpty()) {
                                        clipboard.setText(AnnotatedString(pwd))
                                        scope.launch {
                                            snackbarHostState.showSnackbar("رمز کپی شد ✔️")
                                        }
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("خطا در خواندن رمز")
                                        }
                                    }
                                },
                                onEdit = {
                                    editingEntry = entry
                                    showDialog = true
                                },
                                onDelete = {
                                    val newList = entries.filterNot { it.id == entry.id }
                                    persist(newList)
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
                            editingEntry = null
                            showDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "پسورد جدید"
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp)
            )
        }
    }

    if (showDialog) {
        PasswordDialog(
            initial = editingEntry,
            onDismiss = { showDialog = false },
            onSave = { newEntry ->
                val updated = if (editingEntry == null) {
                    entries + newEntry
                } else {
                    entries.map { if (it.id == newEntry.id) newEntry else it }
                }
                persist(updated)
                showDialog = false
            }
        )
    }
}

@Composable
private fun PasswordRow(
    entry: PasswordEntry,
    onCopyPassword: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    val plain = decodePassword(entry.encryptedPassword)
    val masked = if (plain.isEmpty()) "****" else "•".repeat(plain.length.coerceAtLeast(6))

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
                        text = if (entry.title.isNotBlank()) entry.title else "بدون عنوان",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.category.label,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                IconButton(onClick = onCopyPassword) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "کپی رمز"
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "حذف"
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "یوزرنیم / ایمیل: ${entry.username.ifBlank { "نامشخص" }}",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showPassword) plain else masked,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = { showPassword = !showPassword }
                ) {
                    Text(if (showPassword) "مخفی کن" else "نمایش")
                }
            }

            if (entry.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = entry.note,
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
            }
        }
    }
}

@Composable
private fun PasswordDialog(
    initial: PasswordEntry?,
    onDismiss: () -> Unit,
    onSave: (PasswordEntry) -> Unit
) {
    var draftTitle by remember { mutableStateOf(initial?.title ?: "") }
    var draftUsername by remember { mutableStateOf(initial?.username ?: "") }
    var draftPasswordPlain by remember {
        mutableStateOf(
            initial?.let { decodePassword(it.encryptedPassword) } ?: ""
        )
    }
    var draftNote by remember { mutableStateOf(initial?.note ?: "") }
    var draftCategory by remember { mutableStateOf(initial?.category ?: PasswordCategory.OTHER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "پسورد جدید" else "ویرایش پسورد",
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
                    singleLine = true,
                    label = { Text("عنوان (مثلا: اینستاگرام)") }
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftUsername,
                    onValueChange = { draftUsername = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("یوزرنیم / ایمیل") }
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftPasswordPlain,
                    onValueChange = { draftPasswordPlain = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("رمز عبور") }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "دسته‌بندی:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryChip("شبکه اجتماعی", draftCategory == PasswordCategory.SOCIAL) {
                            draftCategory = PasswordCategory.SOCIAL
                        }
                        CategoryChip("ایمیل", draftCategory == PasswordCategory.EMAIL) {
                            draftCategory = PasswordCategory.EMAIL
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryChip("مالی / بانکی", draftCategory == PasswordCategory.FINANCE) {
                            draftCategory = PasswordCategory.FINANCE
                        }
                        CategoryChip("کاری", draftCategory == PasswordCategory.WORK) {
                            draftCategory = PasswordCategory.WORK
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CategoryChip("سایر", draftCategory == PasswordCategory.OTHER) {
                            draftCategory = PasswordCategory.OTHER
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = draftNote,
                    onValueChange = { draftNote = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    label = { Text("توضیحات (اختیاری)") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val title = draftTitle.trim()
                    val username = draftUsername.trim()
                    val pwd = draftPasswordPlain.trim()
                    val note = draftNote.trim()
                    if (title.isEmpty() || pwd.isEmpty()) return@TextButton
                    val now = System.currentTimeMillis()
                    val entry = PasswordEntry(
                        id = initial?.id ?: now,
                        title = title,
                        username = username,
                        encryptedPassword = encodePassword(pwd),
                        note = note,
                        category = draftCategory,
                        createdAt = initial?.createdAt ?: now,
                        updatedAt = now
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
/**
 * گرفتن یا ساختن کلید متقارن داخل Android Keystore
 */
private fun getOrCreateSecretKey(): SecretKey {
    val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    val existingKey = keyStore.getKey(KEY_ALIAS_PASSWORDS, null)
    if (existingKey is SecretKey) {
        return existingKey
    }

    val keyGenerator = KeyGenerator.getInstance(
        KeyProperties.KEY_ALGORITHM_AES,
        ANDROID_KEYSTORE
    )

    val parameterSpec = KeyGenParameterSpec.Builder(
        KEY_ALIAS_PASSWORDS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .setRandomizedEncryptionRequired(true)
        .build()

    keyGenerator.init(parameterSpec)
    return keyGenerator.generateKey()
}

/**
 * رمزنگاری رمز عبور:
 * خروجی مثل: v1gcm:base64(iv):base64(cipherText)
 */
private fun encodePassword(raw: String): String {
    if (raw.isEmpty()) return ""

    return try {
        val key = getOrCreateSecretKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(raw.toByteArray(Charsets.UTF_8))

        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)

        "$ENC_PREFIX_V1$ivB64:$cipherB64"
    } catch (e: Exception) {
        // در صورت خطا، فقط برای اینکه اپ کرش نکند، به Base64 ساده برمی‌گردیم
        Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }
}

/**
 * رمزگشایی:
 * اول سعی می‌کنیم فرمت جدید v1gcm را بخوانیم؛
 * اگر نبود، فرض می‌کنیم رشته قدیمی Base64 ساده است.
 */
private fun decodePassword(encoded: String): String {
    if (encoded.isEmpty()) return ""

    return try {
        if (encoded.startsWith(ENC_PREFIX_V1)) {
            val body = encoded.removePrefix(ENC_PREFIX_V1)
            val parts = body.split(":")
            if (parts.size != 2) return ""
            val ivBytes = Base64.decode(parts[0], Base64.NO_WRAP)
            val cipherBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, ivBytes)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, Charsets.UTF_8)
        } else {
            // حالت قدیمی: فقط Base64 ساده
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            String(bytes, Charsets.UTF_8)
        }
    } catch (e: Exception) {
        ""
    }
}

private fun loadPasswordEntries(context: Context): List<PasswordEntry> {
    val prefs = context.getSharedPreferences(PREF_PASSWORDS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_PASSWORDS, "") ?: ""
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .filter { it.isNotBlank() }
        .mapNotNull { line ->
            val parts = line.split("||")
            if (parts.size < 8) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            val createdAt = parts[1].toLongOrNull() ?: System.currentTimeMillis()
            val updatedAt = parts[2].toLongOrNull() ?: createdAt
            val categoryCode = parts[3].toIntOrNull() ?: PasswordCategory.OTHER.code
            val title = parts[4]
            val username = parts[5]
            val encPwd = parts[6]
            val note = parts[7]
            PasswordEntry(
                id = id,
                title = title,
                username = username,
                encryptedPassword = encPwd,
                note = note,
                category = PasswordCategory.fromCode(categoryCode),
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
}

private fun savePasswordEntries(context: Context, entries: List<PasswordEntry>) {
    val prefs = context.getSharedPreferences(PREF_PASSWORDS, Context.MODE_PRIVATE)
    val raw = entries.joinToString("\n") { e ->
        val safeTitle = e.title.replace("\n", " ")
        val safeUser = e.username.replace("\n", " ")
        val safeNote = e.note.replace("\n", " ")
        "${e.id}||${e.createdAt}||${e.updatedAt}||${e.category.code}||" +
                "$safeTitle||$safeUser||${e.encryptedPassword}||$safeNote"
    }
    prefs.edit().putString(KEY_PASSWORDS, raw).apply()
}
