package com.taha.planer.features.assistant

/**
 * تنظیمات مربوط به سرچ وب برای دستیار با Tavily.
 *
 * ⚠️ اگر ریپو گیت‌هابت پابلیک است، کلید واقعی را اینجا نگذار یا
 * قبل از push کردن، این رشته را عوض کن.
 */
object SearchApiConfig {
    // اینجا کلید Tavily را قرار بده (مثلاً tvly-...)
    const val SEARCH_API_KEY: String = "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"

    // آدرس پایه Tavily
    const val SEARCH_BASE_URL: String = "https://api.tavily.com"

    // فقط برای اینکه بدونیم داریم از چی استفاده می‌کنیم
    const val SEARCH_PROVIDER: String = "tavily"
}
