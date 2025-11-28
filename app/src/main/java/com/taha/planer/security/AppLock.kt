package com.taha.planer.security

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.taha.planer.ui.PlannerCard

// -------------------- ذخیره‌سازی ساده برای قفل برنامه --------------------

private const val PREF_APP_LOCK = "planner_app_lock"
private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
private const val KEY_APP_LOCK_PIN = "app_lock_pin"

private fun prefs(context: Context) =
    context.getSharedPreferences(PREF_APP_LOCK, Context.MODE_PRIVATE)

fun isAppLockEnabled(context: Context): Boolean {
    val p = prefs(context)
    val enabled = p.getBoolean(KEY_APP_LOCK_ENABLED, false)
    val pin = p.getString(KEY_APP_LOCK_PIN, null)
    return enabled && !pin.isNullOrBlank()
}

fun isAppLockConfigured(context: Context): Boolean {
    val pin = prefs(context).getString(KEY_APP_LOCK_PIN, null)
    return !pin.isNullOrBlank()
}

fun setAppLockEnabled(context: Context, enabled: Boolean) {
    prefs(context).edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
}

fun saveAppLockPin(context: Context, pin: String) {
    prefs(context).edit().putString(KEY_APP_LOCK_PIN, pin).apply()
}

fun clearAppLock(context: Context) {
    prefs(context).edit()
        .remove(KEY_APP_LOCK_PIN)
        .putBoolean(KEY_APP_LOCK_ENABLED, false)
        .apply()
}

fun verifyAppLockPin(context: Context, pin: String): Boolean {
    val saved = prefs(context).getString(KEY_APP_LOCK_PIN, null)
    return !saved.isNullOrBlank() && saved == pin
}

// -------------------- UI تنظیمات قفل برنامه --------------------

/**
 * صفحه‌ی تنظیم قفل برنامه (PIN)
 * این همون جاییه که از Security/Settings بهش می‌رسی.
 */
@Composable
fun AppLockSettingsScreen() {
    val context = LocalContext.current

    var isEnabled by remember { mutableStateOf(false) }
    var hasPin by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isEnabled = isAppLockEnabled(context)
        hasPin = isAppLockConfigured(context)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "قفل برنامه (PIN)",
            style = MaterialTheme.typography.titleLarge
        )

        PlannerCard(
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
                        text = "فعال‌سازی قفل برنامه",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (hasPin)
                            "با فعال بودن این گزینه، برای ورود به اپ باید PIN وارد کنی."
                        else
                            "اول باید یک PIN بسازی، بعد می‌توانی قفل برنامه را فعال کنی.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            // اگر PIN نداریم، اول از کاربر می‌خواهیم بسازد
                            if (!hasPin) {
                                showPinDialog = true
                            } else {
                                setAppLockEnabled(context, true)
                                isEnabled = true
                            }
                        } else {
                            setAppLockEnabled(context, false)
                            isEnabled = false
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        PlannerCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "مدیریت PIN",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (hasPin)
                        "در حال حاضر برای برنامه PIN تنظیم کرده‌ای. می‌توانی آن را تغییر بدهی یا پاک کنی."
                    else
                        "هنوز PIN تنظیم نکرده‌ای. از دکمه‌ی زیر برای ساخت آن استفاده کن.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showPinDialog = true }
                    ) {
                        Text(if (hasPin) "تغییر PIN" else "ساخت PIN")
                    }

                    if (hasPin) {
                        FilledTonalButton(
                            onClick = { showClearDialog = true }
                        ) {
                            Text("حذف PIN")
                        }
                    }
                }
            }
        }

        PlannerCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "نکات امنیتی",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "PIN فقط روی همین دستگاه ذخیره می‌شود و جایی ارسال نمی‌شود. اگر برنامه را حذف کنی، PIN هم پاک می‌شود.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showPinDialog) {
        PinEditDialog(
            initialHasPin = hasPin,
            onDismiss = { showPinDialog = false },
            onPinSaved = { newPin ->
                saveAppLockPin(context, newPin)
                setAppLockEnabled(context, true)
                isEnabled = true
                hasPin = true
                showPinDialog = false
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("حذف PIN؟") },
            text = {
                Text(
                    "با حذف PIN، قفل برنامه غیرفعال می‌شود و برای ورود به اپ دیگر رمزی لازم نیست."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearAppLock(context)
                        isEnabled = false
                        hasPin = false
                        showClearDialog = false
                    }
                ) {
                    Text("بله، حذف کن")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("بی‌خیال")
                }
            }
        )
    }
}

// -------------------- دیالوگ ساخت / تغییر PIN --------------------

@Composable
private fun PinEditDialog(
    initialHasPin: Boolean,
    onDismiss: () -> Unit,
    onPinSaved: (String) -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialHasPin) "تغییر PIN برنامه" else "ساخت PIN برای برنامه")
        },
        text = {
            Column {
                Text(
                    text = "PIN باید حداقل ۴ رقم باشد. چیزی انتخاب کن که هم یادت بماند و هم قابل حدس زدن نباشد.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { value ->
                        newPin = value.filter { it.isDigit() }.take(8)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("PIN جدید") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { value ->
                        confirmPin = value.filter { it.isDigit() }.take(8)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("تکرار PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    visualTransformation = PasswordVisualTransformation()
                )

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        newPin.length < 4 -> {
                            errorText = "PIN باید حداقل ۴ رقم باشد."
                        }

                        newPin != confirmPin -> {
                            errorText = "PIN و تکرار آن یکی نیست."
                        }

                        else -> {
                            onPinSaved(newPin)
                        }
                    }
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
