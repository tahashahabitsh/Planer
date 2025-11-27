package com.taha.planer.features.account

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class PlanType {
    FREE,
    PREMIUM
}

data class UserAccount(
    val fullName: String,
    val username: String,
    val email: String,
    val bio: String,
    val currentGoal: String,
    val planType: PlanType,
    val createdAt: Long
)

private const val PREF_ACCOUNT = "planner_account_profile"
private const val KEY_FULL_NAME = "full_name"
private const val KEY_USERNAME = "username"
private const val KEY_EMAIL = "email"
private const val KEY_BIO = "bio"
private const val KEY_GOAL = "goal"
private const val KEY_PLAN = "plan"
private const val KEY_CREATED_AT = "created_at"

// پسورد و وضعیت ورود
private const val KEY_PASSWORD_HASH = "password_hash"
private const val KEY_PASSWORD_SALT = "password_salt"
private const val KEY_LOGGED_IN = "logged_in"

// ------------------ پروفایل (بدون پسورد) ------------------

fun loadUserAccount(context: Context): UserAccount {
    val prefs = context.getSharedPreferences(PREF_ACCOUNT, Context.MODE_PRIVATE)
    val fullName = prefs.getString(KEY_FULL_NAME, "") ?: ""
    val username = prefs.getString(KEY_USERNAME, "") ?: ""
    val email = prefs.getString(KEY_EMAIL, "") ?: ""
    val bio = prefs.getString(KEY_BIO, "") ?: ""
    val goal = prefs.getString(KEY_GOAL, "") ?: ""
    val planStr = prefs.getString(KEY_PLAN, "FREE") ?: "FREE"
    val createdAt = prefs.getLong(KEY_CREATED_AT, 0L)

    val planType = when (planStr) {
        "PREMIUM" -> PlanType.PREMIUM
        else -> PlanType.FREE
    }

    val fixedCreatedAt = if (createdAt == 0L) {
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_CREATED_AT, now).apply()
        now
    } else createdAt

    return UserAccount(
        fullName = fullName,
        username = username,
        email = email,
        bio = bio,
        currentGoal = goal,
        planType = planType,
        createdAt = fixedCreatedAt
    )
}

fun saveUserAccount(context: Context, account: UserAccount) {
    val prefs = context.getSharedPreferences(PREF_ACCOUNT, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(KEY_FULL_NAME, account.fullName)
        .putString(KEY_USERNAME, account.username)
        .putString(KEY_EMAIL, account.email)
        .putString(KEY_BIO, account.bio)
        .putString(KEY_GOAL, account.currentGoal)
        .putString(
            KEY_PLAN,
            when (account.planType) {
                PlanType.FREE -> "FREE"
                PlanType.PREMIUM -> "PREMIUM"
            }
        )
        .putLong(KEY_CREATED_AT, account.createdAt)
        .apply()
}

fun resetUserAccount(context: Context) {
    val prefs = context.getSharedPreferences(PREF_ACCOUNT, Context.MODE_PRIVATE)
    prefs.edit()
        .remove(KEY_FULL_NAME)
        .remove(KEY_USERNAME)
        .remove(KEY_EMAIL)
        .remove(KEY_BIO)
        .remove(KEY_GOAL)
        .remove(KEY_PLAN)
        .remove(KEY_CREATED_AT)
        .remove(KEY_PASSWORD_HASH)
        .remove(KEY_PASSWORD_SALT)
        .putBoolean(KEY_LOGGED_IN, false)
        .apply()
}

// ------------------ وضعیت حساب و ورود ------------------

fun isAccountConfigured(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREF_ACCOUNT, Context.MODE_PRIVATE)
    val hasPassword = !prefs.getString(KEY_PASSWORD_HASH, "").isNullOrBlank()
    val hasName = !prefs.getString(KEY_FULL_NAME, "").isNullOrBlank()
    val hasEmail = !prefs.getString(KEY_EMAIL, "").isNullOrBlank()
    return hasPassword || hasName || hasEmail
}

fun isUserLoggedIn(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREF_ACCOUNT, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_LOGGED_IN, false)
}

fun setUserLoggedIn(context: Context, value: Boolean) {
    val prefs = context.getSharedPreferences(PREF_ACCOUNT, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()
}

// ------------------ مدیریت پسورد (هش‌شده) ------------------

private const val PBKDF2_ITERATIONS = 12000
private const val PBKDF2_KEY_LENGTH = 256 // bits
private const val SALT_LENGTH_BYTES = 16

/**
 * ست کردن پسورد جدید (یا اولین بار) برای حساب.
 */
fun setAccountPassword(context: Context, plainPassword: String) {
    val salt = ByteArray(SALT_LENGTH_BYTES)
    SecureRandom().nextBytes(salt)

    val hash = pbkdf2Hash(plainPassword, salt)

    val prefs = context.getSharedPreferences(PREF_ACCOUNT, Context.MODE_PRIVATE)
    prefs.edit()
        .putString(KEY_PASSWORD_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        .putString(KEY_PASSWORD_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
        .apply()
}

/**
 * چک کردن پسورد واردشده با پسورد ذخیره‌شده.
 */
fun verifyAccountPassword(context: Context, plainPassword: String): Boolean {
    val prefs = context.getSharedPreferences(PREF_ACCOUNT, Context.MODE_PRIVATE)
    val saltBase64 = prefs.getString(KEY_PASSWORD_SALT, null) ?: return false
    val hashBase64 = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false

    val salt = try {
        Base64.decode(saltBase64, Base64.NO_WRAP)
    } catch (e: Exception) {
        return false
    }

    val expectedHash = try {
        Base64.decode(hashBase64, Base64.NO_WRAP)
    } catch (e: Exception) {
        return false
    }

    val actualHash = pbkdf2Hash(plainPassword, salt)
    if (actualHash.size != expectedHash.size) return false

    var diff = 0
    for (i in actualHash.indices) {
        diff = diff or (actualHash[i].toInt() xor expectedHash[i].toInt())
    }
    return diff == 0
}

private fun pbkdf2Hash(password: String, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
    val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    return skf.generateSecret(spec).encoded
}
