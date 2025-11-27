package com.taha.planer.features.assistant

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * این آبجکت مسئول کارهای شبکه‌ای دستیار است:
 * - تماس با Tavily برای سرچ وب
 * - تماس با OpenAI برای جواب هوشمند
 * - استفاده از پروفایل کاربر در پرامپت سیستم
 */
object AssistantNetwork {

    /**
     * تابع اصلی برای حالت «آنلاین».
     * ۱) پروفایل کاربر را از حافظه می‌خواند.
     * ۲) اگر توانست، از Tavily یک خلاصه وب می‌گیرد.
     * ۳) بعد با OpenAI و پرامپت بزرگ + پروفایل + خلاصه وب، جواب نهایی را می‌سازد.
     */
    fun askOnline(
        context: Context,
        question: String,
        onResult: (String) -> Unit
    ) {
        Thread {
            val mainHandler = Handler(Looper.getMainLooper())
            try {
                val profile = loadAssistantProfile(context)
                val profileText = buildProfileText(profile)

                // مرحله ۱: تلاش برای گرفتن جواب از Tavily
                val webAnswer = try {
                    searchWithTavily(question)
                } catch (e: Exception) {
                    ""
                }

                // مرحله ۲: تلاش برای گرفتن جواب از OpenAI
                val aiAnswer = try {
                    askOpenAi(question, profileText, webAnswer)
                } catch (e: Exception) {
                    null
                }

                val finalText = when {
                    !aiAnswer.isNullOrBlank() -> aiAnswer
                    !webAnswer.isNullOrBlank() -> "نتیجه‌ی خلاصه از وب:\n\n$webAnswer"
                    else -> "نتوانستم از اینترنت نتیجه‌ای بگیرم. بعداً دوباره تلاش کن یا از حالت آفلاین استفاده کن."
                }

                mainHandler.post {
                    onResult(finalText)
                }
            } catch (e: Exception) {
                mainHandler.post {
                    onResult("مشکلی در اتصال به اینترنت یا سرویس پیش آمد. بعداً دوباره تلاش کن.")
                }
            }
        }.start()
    }

    // ------ Tavily ------

    @Throws(Exception::class)
    private fun searchWithTavily(query: String): String {
        if (SearchApiConfig.SEARCH_API_KEY.isBlank()) {
            return ""
        }

        val url = URL("${SearchApiConfig.SEARCH_BASE_URL}/search")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 20000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        val bodyJson = """
            {
              "api_key": "${escapeJson(SearchApiConfig.SEARCH_API_KEY)}",
              "query": "${escapeJson(query)}",
              "search_depth": "basic",
              "include_answer": true,
              "max_results": 5
            }
        """.trimIndent()

        conn.outputStream.use { os: OutputStream ->
            os.write(bodyJson.toByteArray(Charsets.UTF_8))
        }

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        } else {
            conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText) ?: ""
        }

        conn.disconnect()

        if (responseText.isBlank()) return ""

        val json = JSONObject(responseText)
        val answer = json.optString("answer", "")
        return answer
    }

    // ------ OpenAI Chat ------

    @Throws(Exception::class)
    private fun askOpenAi(
        question: String,
        profileText: String?,
        webAnswer: String?
    ): String? {
        if (AssistantApiConfig.OPENAI_API_KEY.isBlank()) return null

        val url = URL("${AssistantApiConfig.OPENAI_BASE_URL}/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 30000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${AssistantApiConfig.OPENAI_API_KEY}")
        }

        val systemContent = buildString {
            append(ASSISTANT_MEGA_PROMPT)
            if (!profileText.isNullOrBlank()) {
                append("\n\n---\nپروفایل فعلی کاربر (از جلسات قبلی):\n")
                append(profileText)
            }
            if (!webAnswer.isNullOrBlank()) {
                append("\n\n---\nاین خلاصه‌ی نتایج وب درباره‌ی سوال کاربر است، از آن برای پاسخ بهتر استفاده کن:\n")
                append(webAnswer)
            }
        }

        val bodyJson = """
            {
              "model": "${escapeJson(AssistantApiConfig.OPENAI_MODEL)}",
              "messages": [
                {
                  "role": "system",
                  "content": "${escapeJson(systemContent)}"
                },
                {
                  "role": "user",
                  "content": "${escapeJson(question)}"
                }
              ]
            }
        """.trimIndent()

        conn.outputStream.use { os ->
            os.write(bodyJson.toByteArray(Charsets.UTF_8))
        }

        val responseCode = conn.responseCode
        val responseText = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
        } else {
            conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use(BufferedReader::readText) ?: ""
        }

        conn.disconnect()

        if (responseText.isBlank()) return null

        val json = JSONObject(responseText)
        val choices = json.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null
        val first = choices.getJSONObject(0)
        val message = first.optJSONObject("message") ?: return null
        return message.optString("content", null)
    }

    // ------ ابزار کوچک برای escape کردن متن در JSON ------

    private fun escapeJson(input: String): String {
        val sb = StringBuilder(input.length + 16)
        input.forEach { ch ->
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
