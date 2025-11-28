package com.taha.planer.security

import androidx.compose.foundation.text.KeyboardOptions

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest

private const val PREF_APP_SECURITY = "planner_app_security"
private const val KEY_PIN_HASH = "app_pin_hash"
private const val KEY_FLAG_SECURE = "flag_secure_enabled"
private const val KEY_PIN_ENABLED = "app_pin_enabled"

// ---------- وضعیت پین برنامه ----------

fun hasAppPin(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREF_APP_SECURITY, Context.MODE_PRIVATE)
    return prefs.getString(KEY_PIN_HASH, "").orEmpty().isNotEmpty()
}

fun isAppLockEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREF_APP_SECURITY, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_PIN_ENABLED, false)
}

fun setAppLockEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREF_APP_SECURITY, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_PIN_ENABLED, enabled).apply()
}

private fun hashPin(pin: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(pin.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
}

fun verifyAppPin(context: Context, pin: String): Boolean {
    if (pin.isBlank()) return false
    val prefs = context.getSharedPreferences(PREF_APP_SECURITY, Context.MODE_PRIVATE)
    val stored = prefs.getString(KEY_PIN_HASH, "") ?: ""
    if (stored.isEmpty()) return false
    return stored == hashPin(pin)
}

fun saveAppPin(context: Context, pin: String) {
    val prefs = context.getSharedPreferences(PREF_APP_SECURITY, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_PIN_HASH, hashPin(pin)).apply()
    // اگر پین تنظیم می‌شود، منطقی است که قفل برنامه فعال باشد
    prefs.edit().putBoolean(KEY_PIN_ENABLED, true).apply()
}

fun clearAppPin(context: Context) {
    val prefs = context.getSharedPreferences(PREF_APP_SECURITY, Context.MODE_PRIVATE)
    prefs.edit()
        .remove(KEY_PIN_HASH)
        .putBoolean(KEY_PIN_ENABLED, false)
        .apply()
}

// ---------- فلگ جلوگیری از اسکرین‌شات ----------

fun isSecureFlagEnabled(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREF_APP_SECURITY, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_FLAG_SECURE, false)
}

fun setSecureFlagEnabled(context: Context, enabled: Boolean) {
    val prefs = context.getSharedPreferences(PREF_APP_SECURITY, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_FLAG_SECURE, enabled).apply()
}

// ---------- صفحه‌ی قفل برنامه ----------

@Composable
fun AppLockScreen(
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var hasPin by remember { mutableStateOf(hasAppPin(context)) }
    var step by remember { mutableStateOf(if (hasPin) "enter" else "set") }

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "قفل برنامه",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (step == "enter") {
                        Text(
                            text = "برای ورود، پین برنامه را وارد کن.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pin,
                            onValueChange = {
                                if (it.length <= 10) pin = it.filter { ch -> ch.isDigit() }
                            },
                            label = { Text("پین") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (error != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (verifyAppPin(context, pin)) {
                                    error = null
                                    onUnlocked()
                                } else {
                                    error = "پین اشتباه است."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("باز کردن قفل")
                        }
                    } else {
                        Text(
                            text = "اولین بار است. یک پین برای قفل برنامه تعیین کن.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pin,
                            onValueChange = {
                                if (it.length <= 10) pin = it.filter { ch -> ch.isDigit() }
                            },
                            label = { Text("پین (۴ تا ۱۰ رقم)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = {
                                if (it.length <= 10) confirmPin = it.filter { ch -> ch.isDigit() }
                            },
                            label = { Text("تکرار پین") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (error != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (pin.length < 4) {
                                    error = "پین باید حداقل ۴ رقم باشد."
                                    return@Button
                                }
                                if (pin != confirmPin) {
                                    error = "پین و تکرار پین یکی نیستند."
                                    return@Button
                                }
                                saveAppPin(context, pin)
                                hasPin = true
                                step = "enter"
                                error = null
                                onUnlocked()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ذخیره پین و ورود")
                        }
                    }
                }
            }
        }
    }
}
