package com.taha.planer.features.assistant

/**
 * تنظیمات ثابت و غیرحساس دستیار.
 * دقت کن: اینجا «هیچ‌وقت» کلید واقعی OpenAI یا Tavily را نگذار.
 * کلیدها فقط از تنظیمات داخل برنامه (SharedPreferences) خوانده می‌شوند.
 */
object AssistantApiConfig {

    // آدرس‌های پایه – مشکلی ندارند که تو کد باشند
    const val OPENAI_BASE_URL = "https://api.openai.com/v1"
    const val OPENAI_CHAT_MODEL = "gpt-4.1-mini"
    const val OPENAI_REASONING_MODEL = "gpt-4.1-mini"

    const val TAVILY_BASE_URL = "https://api.tavily.com"

    // متن راهنما – فقط برای این‌که اگر کسی اشتباهی این فایل را باز کرد،
    // متوجه شود نباید اینجا کلید وارد کند.
    const val KEY_HINT: String =
        "کلید‌های OpenAI و Tavily را فقط از داخل تنظیمات اپ وارد کن؛ اینجا چیزی ننویس."
}
