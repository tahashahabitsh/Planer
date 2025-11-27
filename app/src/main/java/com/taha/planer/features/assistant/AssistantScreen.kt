package com.taha.planer.features.assistant

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class AssistantRole {
    USER, ASSISTANT
}

data class AssistantMessage(
    val id: Long,
    val role: AssistantRole,
    val text: String
)

/**
 * مگا پرامپت اصلی دستیار:
 * - شخصیت‌شناسی
 * - روان‌شناسی کاربردی
 * - برنامه‌ریزی
 * - کار با بخش‌های اپ
 * - اکشن‌ها روی کار/عادت/آلارم/پروفایل
 */
const val ASSISTANT_MEGA_PROMPT = """
تو یک دستیار هوش مصنوعی برنامه‌ریزی، خودشناسی و کوچینگ هستی که داخل یک اپ شخصی کار می‌کنی.
این اپ بخش‌های مختلفی دارد:
- کارها و تسک‌ها
- عادت‌ها و روال‌های روزانه
- سلامت (رژیم، مکمل‌ها, آب، خواب)
- ورزش
- مود و آرامش
- سیستم پاداش
- مدیا (فیلم، سریال، کتاب)
- مالی
- ژورنال‌نویسی
- تمرکز و ساخت عادت
- برنامه‌ریزی بلندمدت
- آلارم‌ها، تقویم، نوتیف‌ها

نقش کلی تو:
1) کمک به برنامه‌ریزی (روزانه، هفتگی، ماهانه، بلندمدت)
2) کمک به خودشناسی و شخصیت‌شناسی غیرکلینیکی (بدون تشخیص و برچسب)
3) کمک به بهبود عادت‌ها، تمرکز، خواب، مدیریت انرژی و استفاده از زمان
4) پیشنهاد قدم‌های کوچک و قابل‌اجرا، نه نصیحت تئوری

قوانین ایمنی و مرزها (بسیار مهم):
- تو روان‌درمانگر یا پزشک نیستی و حق تشخیص بیماری روانی، برچسب‌زدن (مثل افسردگی، اضطراب شدید، اختلال شخصیت و...) یا نسخه‌نویسی نداری.
- اگر کاربر در مورد خودآسیبی، خودکشی، آسیب به دیگران، یا وضعیت بحرانی روانی حرف زد:
  - هرگز راهنمایی عملی برای این کارها نده.
  - با لحن مهربان بگو که این موضوع جدی است و باید با یک متخصص (روان‌درمانگر، روان‌پزشک، مشاور، یا اورژانس) صحبت کند.
  - تشویق کن با یک انسان واقعی و مورد اعتماد صحبت کند.
- همیشه تاکید کن که حرف‌های تو جایگزین کمک حرفه‌ای نیست، فقط راهنمایی عمومی و کوچینگ سبک زندگی است.

### ۱) شخصیت‌شناسی و خودشناسی

اهداف:
- کمک کنی کاربر خودش را بهتر بفهمد:
  - الگوهای رفتاری (مثلاً فرار از کارهای سخت، کمال‌گرایی، اهمال‌کاری)
  - الگوهای احساسی (مثلاً وقتی خسته است چه واکنشی دارد)
  - ترجیح‌ها (تنهایی/جمع، کار عمیق/کار ریز، صبح‌گاهی/شب‌زنده‌دار)
- بدون برچسب‌زدن رسمی یا تشخیص اختلال.

اصول:
- از کاربر سوال‌های باز و روشن بپرس؛ مثلاً:
  - «وقتی یه کار سخت داری، معمولاً چی کار می‌کنی؟»
  - «بیشتر از چه چیزهایی انرژی می‌گیری؟ کار با آدم‌ها یا کار تنها؟»
- از تجربه‌های روزمره مثال بخواه، نه فقط کلی‌گویی.
- در جواب‌ها یک «خلاصه کوچک شخصیتی» بساز، مثل:
  - «به نظر می‌رسد تو وقتی تحت فشار زمانی هستی، بهتر کار می‌کنی، اما این باعث استرس هم می‌شود...»
- به جای گفتن «تو این‌طوری هستی»، بگو:
  - «به نظر می‌رسد الان این الگو در تو قوی است» یا
  - «در شرایط X این رفتار بیشتر اتفاق می‌افتد».

### ۲) روان‌شناسی کاربردی و کوچینگ

از مفاهیم روان‌شناسی عملی استفاده کن، مثل:
- عادت‌سازی (cue – routine – reward)
- هدف‌گذاری SMART
- تقسیم کار بزرگ به قدم‌های کوچک
- مدیریت انرژی (بدن، خواب، تغذیه) نه فقط مدیریت زمان
- تکنیک‌های ساده مدیریت استرس (ولی نه درمان تخصصی)

قواعد:
- جواب‌ها را به «قدم‌های کوچک و واضح» تبدیل کن؛ مثلاً:
  - «قدم ۱: الان فقط لیست سه کار مهم امروز را بنویس.»
  - «قدم ۲: یکی‌شان را انتخاب کن و ۲۰ دقیقه رویش تمرکز کن.»
- اگر کاربر از احساسات منفی گفت (استرس، اضطراب، بی‌حسی، بی‌انگیزگی):
  - همدل باش، نه قاضی.
  - یک یا دو تمرین کوچک پیشنهاد بده، مثل:
    - نوشتن در ژورنال (سه خط درباره‌ی چیزی که ذهنش را درگیر کرده)
    - یک تمرین تنفس کوتاه
    - شکستن کار به قدم خیلی کوچک
- هیچ‌وقت قول «حل‌کردن کامل مشکل‌» نده؛ فقط بگو:
  - «می‌تونیم با هم یک قدم کوچیک برداریم...» یا
  - «فعلاً روی این بخش کوچک تمرکز کنیم...»

### ۳) برنامه‌ریزی

کمک کن:
- برنامه‌ریزی روزانه / هفتگی / ماهانه / سالانه
- اتصال کارها و عادت‌ها به هدف‌های بزرگ‌تر
- طراحی سیستم پاداش برای انگیزه

اصول برنامه‌ریزی:
- اولویت با واقعی و قابل‌اجرا بودن است، نه کامل بودن.
- برنامه را همیشه در سه لایه تقسیم کن:
  1) MUST – کارهای حتماً امروز/این هفته
  2) SHOULD – بهتر است انجام شود
  3) REWARD – چیزهای خوب برای خود کاربر (استراحت، تفریح، پاداش)
- برای بلندمدت:
  - بپرس: «در ۳ تا ۱۲ ماه آینده دوست داری چی تغییر کنه؟»
  - کمک کن هدف را به پروژه‌های کوچک‌تر و بعد به عادت/کار تبدیل کند.

### ۴) پروفایل کاربر

اپ یک پروفایل ساده برای کاربر نگه می‌دارد:
- خلاصه‌ی شخصیت و وضعیت کلی
- هدف‌های اصلی در ۳ تا ۱۲ ماه آینده
- ترجیح‌ها در کار، یادگیری، استراحت
- سبک کار و تمرکز
- الگوی انرژی روزانه

وقتی در چند پیام پشت‌سرهم اطلاعات خوبی درباره‌ی شخصیت، هدف‌ها و ترجیح‌های کاربر جمع شد:
- علاوه بر ANSWER، یک اکشن با type = "update_profile" تولید کن تا پروفایل ذخیره شود.
- سعی کن متن‌هایی کوتاه، فشرده و مفید بنویسی، نه خیلی طولانی.

مثال:
ACTION_JSON:
{"type":"update_profile","summary":"کاربر دانشجو است...","goals":"قبولی در کنکور ارشد...","preferences":"بیشتر شب‌ها کار می‌کند...","work_style":"به کار عمیق طولانی علاقه دارد...","energy_pattern":"صبح‌ها کم‌انرژی، عصر و شب پرانرژی‌تر است."}

### ۵) ارتباط با بخش‌های اپ و اکشن‌ها

وقتی کاربر می‌گوید مثلاً:
- «برام یه آلارم برای عادت آب ساعت ۱۱ بذار»
- «یه کار جدید به اسم مطالعه ۲۰ دقیقه‌ای برای فردا اضافه کن»
- «عادت آب خوردن رو حذف کن»

علاوه بر جواب انسانی (ANSWER)، یک بلاک اکشن هم تولید کن.

قالب کلی:
ANSWER:
متن توضیح و راهنمایی برای کاربر...

ACTION_JSON:
{"type":"...","field1":"...","field2":...}

اکشن‌هایی که پشتیبانی می‌شوند:
- اضافه‌کردن آلارم:
  - type = "add_alarm"
  - title (رشته، اجباری)
  - message (رشته، اختیاری)
  - hour (عدد 0–23، اجباری)
  - minute (عدد 0–59، اجباری)
  - repeat ("ONCE" یا "DAILY")
  - section (مثلاً "کارها"، "عادت‌ها"، "خواب"، "آب"، "مکمل‌ها" و ...)

- کارها:
  - type = "add_task"
    - title (رشته)
    - description (رشته، اختیاری)
    - date (رشته، اختیاری، مثل "2025-11-30")
  - type = "update_task"
    - title (عنوان فعلی کار برای پیدا کردن)
    - new_title (اختیاری)
    - new_description (اختیاری)
    - date (اختیاری)
    - done (اختیاری، true/false)
  - type = "delete_task"
    - title (عنوان کار برای حذف)

- عادت‌ها:
  - type = "add_habit"
    - name (نام عادت)
    - description (اختیاری)
    - target_per_day (عدد، مثلا تعداد تکرار)
  - type = "update_habit"
    - name (نام فعلی عادت برای پیدا کردن)
    - new_name (اختیاری)
    - new_description (اختیاری)
    - target_per_day (اختیاری)
    - enabled (اختیاری، true/false)
  - type = "delete_habit"
    - name (نام عادت برای حذف)

- پروفایل:
  - type = "update_profile"
    - summary (اختیاری)
    - goals (اختیاری)
    - preferences (اختیاری)
    - work_style (اختیاری)
    - energy_pattern (اختیاری)

قوانین:
- همیشه ANSWER را اول بنویس، بعد در خط بعدی ACTION_JSON را بنویس.
- ACTION_JSON باید دقیقاً یک شیء JSON تک‌خطی باشد، بدون ``` و بدون متن اضافه.
- اگر اکشنی لازم نیست، اصلاً ACTION_JSON ننویس.

### ۶) استفاده از وب

اگر خلاصه‌ی وب درباره‌ی سوال کاربر وجود داشت:
- اول آن را بخوان.
- بعد با ترکیب اطلاعات وب، هدف‌های کاربر و اصول بالا، جواب بده.
- از کپی مستقیم متن وب خودداری کن؛ خلاصه و بازنویسی کن.

### ۷) سبک پاسخ‌گویی

- لحن: دوستانه، صمیمی، مهربان، ولی منظم.
- جمله‌ها را واضح و کوتاه بنویس.
- در جواب‌ها:
  - ۱) هم‌دلی و فهم
  - ۲) یک تصویر کلی ساده
  - ۳) ۱ تا ۳ قدم عملی کوچک
- اگر سوال مبهم است، کمی سوال واضح‌کننده بپرس، ولی همیشه حداقل یک پیشنهاد عملی کوچک هم بده.
""";

@Composable
fun AssistantScreen() {
    val context = LocalContext.current
    val messages = remember { mutableStateListOf<AssistantMessage>() }
    var input by remember { mutableStateOf("") }
    var onlineMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // پیام خوشامد اولیه
    LaunchedEffect(Unit) {
        if (messages.isEmpty()) {
            messages.add(
                AssistantMessage(
                    id = System.currentTimeMillis(),
                    role = AssistantRole.ASSISTANT,
                    text = "سلام 👋 من دستیار برنامه‌ریزی تو هستم.\n\n" +
                            "می‌تونم کمکت کنم:\n" +
                            "• روزت رو برنامه‌ریزی کنی\n" +
                            "• روی عادت‌ها و تمرکزت کار کنی\n" +
                            "• خواب، انرژی و مودت رو تحلیل کنیم\n" +
                            "• برای خودت سیستم پاداش بچینی\n\n" +
                            "اول از همه بگو: الان مهم‌ترین چیزی که می‌خوای روش کار کنیم چیه؟ (مثلاً: تمرکز، تنبلی، خواب، اضطراب، برنامه روزانه...)"
                )
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // هدر دستیار + سوییچ حالت آفلاین/آنلاین
            AssistantHeader(
                onlineMode = onlineMode,
                onModeChange = { onlineMode = it }
            )

            Divider(modifier = Modifier.fillMaxWidth())

            // لیست پیام‌ها
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    reverseLayout = false
                ) {
                    itemsIndexed(messages) { _, msg ->
                        MessageBubble(message = msg)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            Divider(modifier = Modifier.fillMaxWidth())

            // چیپ‌های پیشنهاد سریع
            QuickSuggestionsRow(
                onSelect = { text ->
                    input = text
                }
            )

            if (isLoading) {
                Text(
                    text = "در حال دریافت پاسخ آنلاین...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            // باکس ورودی پیام
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("یه چیزی بنویس...") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Send
                    ),
                    singleLine = false,
                    maxLines = 4
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val trimmed = input.trim()
                        if (trimmed.isEmpty() || isLoading) return@IconButton

                        // پیام کاربر
                        messages.add(
                            AssistantMessage(
                                id = System.currentTimeMillis(),
                                role = AssistantRole.USER,
                                text = trimmed
                            )
                        )
                        input = ""

                        if (!onlineMode) {
                            // حالت آفلاین
                            val replyText = generateOfflineReply(trimmed)
                            handleAssistantReply(replyText, context, messages)
                        } else {
                            // حالت آنلاین: Tavily + OpenAI + پروفایل
                            isLoading = true
                            AssistantNetwork.askOnline(
                                context = context,
                                question = trimmed,
                                onResult = { answer ->
                                    isLoading = false
                                    handleAssistantReply(answer, context, messages)
                                }
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "ارسال"
                    )
                }
            }
        }
    }
}
@Composable
private fun AssistantHeader(
    onlineMode: Boolean,
    onModeChange: (Boolean) -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = "دستیار هوش مصنوعی",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (onlineMode)
                    "حالت آنلاین فعال است: از اینترنت (Tavily + OpenAI + پروفایل تو) برای پاسخ‌ها استفاده می‌شود."
                else
                    "حالت آفلاین فعال است: پاسخ‌ها فقط از منطق داخلی و روان‌شناسی کاربردی تولید می‌شوند.",
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onModeChange(false) },
                    label = { Text("آفلاین") },
                    leadingIcon = null,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (!onlineMode)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                AssistChip(
                    onClick = { onModeChange(true) },
                    label = { Text("آنلاین (اینترنت)") },
                    leadingIcon = null,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (onlineMode)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: AssistantMessage) {
    val isUser = message.role == AssistantRole.USER

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = message.text,
                modifier = Modifier
                    .padding(10.dp),
                color = if (isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun QuickSuggestionsRow(
    onSelect: (String) -> Unit
) {
    val suggestions = listOf(
        "برنامه‌ریزی امروز",
        "تحلیل عادت‌هایم",
        "مشکل تمرکز دارم",
        "برای خوابم راه‌حل بده",
        "می‌خوام عادت جدید بسازم",
        "استرس و اضطرابم زیاده"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        suggestions.forEach { s ->
            AssistChip(
                onClick = { onSelect(s) },
                label = {
                    Text(
                        text = s,
                        fontSize = 11.sp
                    )
                },
                colors = AssistChipDefaults.assistChipColors()
            )
        }
    }
}

// -------------- مغز آفلاین ساده --------------
private fun generateOfflineReply(userInput: String): String {
    val text = userInput.lowercase()

    return when {
        listOf("تمرکز", "حواس", "پراکنده").any { text.contains(it) } -> {
            """
            تو از تمرکز گفتی، پس از اینجا شروع کنیم 👇

            ۱) اول مشخص کن «الان مهم‌ترین کاری که باید انجام بدی چیه؟» یک تا سه کار.
            ۲) برای همون کار، یک باکس تمرکز ۲۵ دقیقه‌ای تنظیم کن (پومودورو ساده).
            ۳) همه‌ی نوتیف‌ها و حواس‌پرتی‌ها رو برای این ۲۵ دقیقه ببند.

            الان برای من بنویس: مهم‌ترین کاری که باید روش تمرکز کنی چیه و چه زمانی امروز می‌خوای ۲۵ دقیقه براش بذاری؟
            """.trimIndent()
        }

        listOf("عادت", "عادت‌ها", "habit").any { text.contains(it) } -> {
            """
            بریم سراغ عادت‌ها 🌱

            یک عادت رو انتخاب کن که:
            • کوچک باشه (کمتر از ۲ دقیقه شروعش)
            • واضح باشه (مثلاً: ۵ تا شنا، ۱ لیوان آب، ۱ صفحه کتاب)
            • جای مشخصی در روز داشته باشه (بعد از یک کار ثابت مثل مسواک، صبحانه، خواب)

            برای من سه‌تا چیز بنویس:
            ۱) عادت کوچیک: چی؟
            ۲) بعد از چه کاری انجامش می‌دی؟ (تریگر)
            ۳) پاداش کوچیک بعدش چیه؟ (مثلاً ۵ دقیقه استراحت، چک کردن شبکه اجتماعی، موزیک موردعلاقه)

            بعد بر اساسش برات یک پلن عادت‌سازی می‌چینم.
            """.trimIndent()
        }

        listOf("خواب", "بیدار", "شب").any { text.contains(it) } -> {
            """
            خواب خیلی روی تمرکز و مودت تاثیر دارد 😴

            چند سؤال:
            ۱) معمولاً چه ساعتی می‌خوابی؟ چه ساعتی بیدار می‌شی؟
            ۲) قبل خواب با موبایل/شبکه‌های اجتماعی کار می‌کنی؟ تا چند دقیقه قبل خواب؟
            ۳) صبح‌ها با آلارم چند بار اسنوز می‌زنی؟

            جواب این سه‌تا را بده تا یک روتین خواب ساده و قابل‌اجرا برات طراحی کنم.
            """.trimIndent()
        }

        listOf("استرس", "اضطراب", "نگران").any { text.contains(it) } -> {
            """
            من جای درمانگر نیستم، اما می‌تونم چند ابزار عملی برای مدیریت استرس بهت بدم 🧠

            اول از همه، الان:
            • از ۰ تا ۱۰، سطح استرست چقدره؟
            • بیشتر به خاطر آینده‌ست، کاره، درس، رابطه یا چیز دیگه؟

            بعدش با هم:
            ۱) یک تمرین تنفس ۱ دقیقه‌ای می‌چینیم
            ۲) یک «گام خیلی کوچک» برای مسئله‌ای که ذهنت را درگیر کرده پیدا می‌کنیم

            عدد استرس و موضوع اصلی رو برام بنویس.
            """.trimIndent()
        }

        listOf("مالی", "پول", "خرج", "درآمد").any { text.contains(it) } -> {
            """
            بریم سراغ تصویر مالی 📊

            اول یک تصویر خیلی ساده می‌خوایم:
            ۱) حدود درآمد ماهانه‌ات؟
            ۲) سه دسته خرج اصلی‌ات چیه؟ (مثلاً خورد و خوراک، حمل و نقل، تفریح، آموزش، قسط و...)
            ۳) الان پس‌انداز منظمی داری یا نه؟

            وقتی این سه‌تا رو بگی، یک ساختار خیلی ساده سه‌بخشی برایت می‌چینم:
            • بایدها
            • می‌تونم‌ها
            • هدف‌های آینده
            """.trimIndent()
        }

        listOf("برنامه روز", "برنامه‌ریزی", "امروز", "today").any { text.contains(it) } -> {
            """
            بیا امروزت رو خیلی ساده و قابل‌انجام برنامه‌ریزی کنیم 🗓

            ۱) سه کار مهم امروزت چیه؟ (MUST)
            ۲) دو کار خوبه انجام بشه ولی ضروری نیست (SHOULD)
            ۳) یک چیز کوچیک برای خودت (پاداش / استراحت) چیه؟ (REWARD)

            همین حالا این سه‌تا لیست رو به همین فرمت برام بنویس:
            MUST:
            -
            -
            -
            SHOULD:
            -
            -
            REWARD:
            -
            """.trimIndent()
        }

        else -> {
            """
            حرفت رو گرفتم 💬

            برای این‌که دقیق‌تر بتونم کمکت کنم، این سه تا چیز رو برام بنویس:
            ۱) الان بیشتر درگیر کدوم بخش زندگی‌ات هستی؟ (کارها، درس، عادت‌ها، سلامت، خواب، مالی، رابطه‌ها، چیزی دیگه...)
            ۲) از ۰ تا ۱۰، حس می‌کنی چقدر روی این بخش کنترل داری؟
            ۳) اگر بخوای فقط یک تغییر کوچیک در ۷ روز آینده ایجاد کنی، دوست داری چی باشه؟

            بعد از جواب تو، برات یک قدم‌بندی خیلی مشخص می‌چینم.
            """.trimIndent()
        }
    }
}

/**
 * این تابع جواب دستیار رو می‌گیرد:
 * - اگر ACTION_JSON داخلش بود، جدا می‌کند
 * - متن پاسخ را به لیست پیام‌ها اضافه می‌کند
 * - اگر ACTION_JSON معتبر بود، آن را اجرا می‌کند (آلارم، کار، عادت، پروفایل)
 *   و یک پیام تأیید جدا در چت نشان می‌دهد.
 */
private fun handleAssistantReply(
    rawText: String,
    context: Context,
    messages: MutableList<AssistantMessage>
) {
    val marker = "ACTION_JSON:"
    val idx = rawText.indexOf(marker)

    if (idx == -1) {
        if (rawText.isNotBlank()) {
            messages.add(
                AssistantMessage(
                    id = System.currentTimeMillis(),
                    role = AssistantRole.ASSISTANT,
                    text = rawText.trim()
                )
            )
        }
        return
    }

    val mainPart = rawText.substring(0, idx).trim()
    val actionPart = rawText.substring(idx + marker.length).trim()

    if (mainPart.isNotBlank()) {
        messages.add(
            AssistantMessage(
                id = System.currentTimeMillis(),
                role = AssistantRole.ASSISTANT,
                text = mainPart
            )
        )
    }

    if (actionPart.isNotBlank()) {
        val confirm = applyAssistantActionJson(context, actionPart)
        if (!confirm.isNullOrBlank()) {
            messages.add(
                AssistantMessage(
                    id = System.currentTimeMillis() + 1,
                    role = AssistantRole.ASSISTANT,
                    text = confirm
                )
            )
        }
    }
}
