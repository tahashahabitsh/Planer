package com.taha.planer.features.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.taha.planer.ui.PlannerCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var account by remember { mutableStateOf(loadUserAccount(context)) }
    var isLoggedIn by remember { mutableStateOf(isUserLoggedIn(context)) }
    var accountConfigured by remember { mutableStateOf(isAccountConfigured(context)) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    fun persist(newAcc: UserAccount) {
        account = newAcc
        saveUserAccount(context, newAcc)
        accountConfigured = isAccountConfigured(context)
    }

    val createdText = remember(account.createdAt) {
        if (account.createdAt == 0L) ""
        else {
            val df = SimpleDateFormat("d MMM yyyy", Locale("fa"))
            df.format(Date(account.createdAt))
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "حساب کاربری",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                when {
                    !accountConfigured -> {
                        // ساخت حساب جدید
                        CreateAccountCard(
                            snackbarHostState = snackbarHostState,
                            onAccountCreated = { fullName, username, email, goal, bio, password ->
                                val now = System.currentTimeMillis()
                                val newAcc = UserAccount(
                                    fullName = fullName.trim(),
                                    username = username.trim(),
                                    email = email.trim(),
                                    bio = bio.trim(),
                                    currentGoal = goal.trim(),
                                    planType = PlanType.FREE,
                                    createdAt = now
                                )
                                saveUserAccount(context, newAcc)
                                setAccountPassword(context, password)
                                setUserLoggedIn(context, true)
                                account = newAcc
                                isLoggedIn = true
                                accountConfigured = true
                                scope.launch {
                                    snackbarHostState.showSnackbar("حساب ساخته شد و وارد شدی.")
                                }
                            }
                        )
                    }

                    accountConfigured && !isLoggedIn -> {
                        // ورود به حساب
                        LoginCard(
                            snackbarHostState = snackbarHostState,
                            emailOrUser = account.email.ifBlank { account.username },
                            onLogin = { pwd ->
                                val ok = verifyAccountPassword(context, pwd)
                                if (ok) {
                                    setUserLoggedIn(context, true)
                                    isLoggedIn = true
                                    scope.launch {
                                        snackbarHostState.showSnackbar("ورود موفقیت‌آمیز بود.")
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("پسورد اشتباه است.")
                                    }
                                }
                            }
                        )
                    }

                    else -> {
                        // کاربر وارد شده
                        LoggedInAccountContent(
                            account = account,
                            createdText = createdText,
                            onEdit = { showEditDialog = true },
                            onLogout = {
                                setUserLoggedIn(context, false)
                                isLoggedIn = false
                            },
                            onReset = { showResetDialog = true },
                            onChangePassword = { showChangePasswordDialog = true }
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )
        }
    }

    if (showEditDialog) {
        AccountEditDialog(
            initial = account,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                persist(updated)
                showEditDialog = false
            }
        )
    }

    if (showResetDialog) {
        ConfirmResetDialog(
            onDismiss = { showResetDialog = false },
            onConfirm = {
                resetUserAccount(context)
                account = loadUserAccount(context)
                isLoggedIn = false
                accountConfigured = isAccountConfigured(context)
                showResetDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("اطلاعات حساب پاک شد. همچنان داده‌های برنامه محفوظ ماند.")
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            snackbarHostState = snackbarHostState,
            onDismiss = { showChangePasswordDialog = false },
            onPasswordChanged = {
                showChangePasswordDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("پسورد با موفقیت تغییر کرد.")
                }
            }
        )
    }
}

// ------------------ حالت: ساخت حساب جدید ------------------

@Composable
private fun CreateAccountCard(
    snackbarHostState: SnackbarHostState,
    onAccountCreated: (
        fullName: String,
        username: String,
        email: String,
        goal: String,
        bio: String,
        password: String
    ) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    PlannerCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "ساخت حساب جدید",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "اطلاعاتت فقط روی این دستگاه ذخیره می‌شود. بعداً اگر سیستم ابری اضافه شد، می‌توانی همین حساب را وصل کنی.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نام کامل") }
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.filter { ch -> !ch.isWhitespace() } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("نام کاربری (اختیاری)") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("ایمیل (اختیاری)") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = goal,
                onValueChange = { goal = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("هدف فعلی (مثلاً: ساخت اپ شخصی، کنکور، تناسب اندام)") }
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp),
                label = { Text("چند خط درباره‌ی خودت (اختیاری)") }
            )
            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("پسورد حساب") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("تکرار پسورد") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(4.dp))

            FilledTonalButton(
                onClick = { showPassword = !showPassword },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (showPassword) "مخفی‌کردن پسورد" else "نمایش پسورد")
            }

            Spacer(modifier = Modifier.height(8.dp))

            FilledTonalButton(
                onClick = {
                    if (fullName.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("نام کامل را وارد کن.")
                        }
                        return@FilledTonalButton
                    }
                    if (password.length < 4) {
                        scope.launch {
                            snackbarHostState.showSnackbar("پسورد باید حداقل ۴ کاراکتر باشد.")
                        }
                        return@FilledTonalButton
                    }
                    if (password != confirmPassword) {
                        scope.launch {
                            snackbarHostState.showSnackbar("پسورد و تکرار آن یکی نیست.")
                        }
                        return@FilledTonalButton
                    }
                    onAccountCreated(fullName, username, email, goal, bio, password)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("ساخت حساب و ورود")
            }
        }
    }
}

// ------------------ حالت: ورود ------------------

@Composable
private fun LoginCard(
    snackbarHostState: SnackbarHostState,
    emailOrUser: String,
    onLogin: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    PlannerCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "ورود به حساب",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (emailOrUser.isNotBlank())
                    "برای ورود، پسورد حساب مربوط به $emailOrUser را وارد کن."
                else
                    "پسورد حسابی که قبلاً ساخته‌ای را وارد کن.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("پسورد") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(4.dp))
            FilledTonalButton(
                onClick = { showPassword = !showPassword },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (showPassword) "مخفی‌کردن" else "نمایش پسورد")
            }

            Spacer(modifier = Modifier.height(8.dp))

            FilledTonalButton(
                onClick = {
                    if (password.isBlank()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("پسورد را وارد کن.")
                        }
                    } else {
                        onLogin(password)
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("ورود")
            }
        }
    }
}

// ------------------ حالت: کاربر وارد شده ------------------

@Composable
private fun LoggedInAccountContent(
    account: UserAccount,
    createdText: String,
    onEdit: () -> Unit,
    onLogout: () -> Unit,
    onReset: () -> Unit,
    onChangePassword: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // کارت پروفایل
        PlannerCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val initials = remember(account.fullName, account.username) {
                    val base = when {
                        account.fullName.isNotBlank() -> account.fullName.trim()
                        account.username.isNotBlank() -> account.username.trim()
                        else -> "شما"
                    }
                    base.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                }

                Surface(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (account.fullName.isNotBlank())
                            account.fullName
                        else
                            "نام خود را تنظیم کنید",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (account.username.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "@${account.username}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (account.email.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = account.email,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (createdText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "عضویت از: $createdText",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    FilledTonalButton(onClick = onEdit) {
                        Text("ویرایش")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    FilledTonalButton(onClick = onLogout) {
                        Text("خروج از حساب")
                    }
                }
            }
        }

        // کارت هدف و بیو
        PlannerCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "هدف فعلی",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (account.currentGoal.isNotBlank())
                        account.currentGoal
                    else
                        "یک هدف کلی برای چند ماه آینده‌ات بنویس (مثلاً: قبولی در دانشگاه، وزن سالم، ساخت اپ شخصی).",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "درباره‌ی تو",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (account.bio.isNotBlank())
                        account.bio
                    else
                        "چند خط درباره‌ی خودت، سبک زندگی‌ات یا چیزی که دوست داری اینجا داشته باشی بنویس.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // کارت پلن و اشتراک
        PlannerCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "پلن و اشتراک",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val planLabel = when (account.planType) {
                    PlanType.FREE -> "پلن فعلی: رایگان"
                    PlanType.PREMIUM -> "پلن فعلی: پریمیوم"
                }
                Text(
                    text = planLabel,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "در نسخه‌ی فعلی همه‌چیز روی دستگاه خودت ذخیره می‌شود. اگر بعداً اشتراک آنلاین اضافه شود، از همین‌جا می‌توانی مدیریت کنی.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // کارت حریم خصوصی و امنیت
        PlannerCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "حریم خصوصی و امنیت حساب",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "اطلاعات حساب (اسم، ایمیل، هدف و بیو) فقط روی همین دستگاه ذخیره می‌شود و جایی ارسال نمی‌شود.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onChangePassword,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تغییر پسورد")
                    }
                    FilledTonalButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ریست اطلاعات حساب")
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountEditDialog(
    initial: UserAccount,
    onDismiss: () -> Unit,
    onSave: (UserAccount) -> Unit
) {
    var fullName by remember { mutableStateOf(initial.fullName) }
    var username by remember { mutableStateOf(initial.username) }
    var email by remember { mutableStateOf(initial.email) }
    var goal by remember { mutableStateOf(initial.currentGoal) }
    var bio by remember { mutableStateOf(initial.bio) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "ویرایش حساب")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نام کامل") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.filter { ch -> !ch.isWhitespace() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("نام کاربری (اختیاری)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ایمیل (اختیاری)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("هدف فعلی") }
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    label = { Text("درباره‌ی تو") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fixedName = fullName.trim()
                    onSave(
                        initial.copy(
                            fullName = fixedName,
                            username = username.trim(),
                            email = email.trim(),
                            bio = bio.trim(),
                            currentGoal = goal.trim()
                        )
                    )
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
private fun ConfirmResetDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "ریست اطلاعات حساب؟")
        },
        text = {
            Text(
                text = "فقط اطلاعات پروفایل (نام، ایمیل، هدف و بیو) و پسورد حساب پاک می‌شود.\nهیچ‌کدام از کارها، عادت‌ها، تمرکزها یا داده‌های دیگر حذف نمی‌شوند.",
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("بله، ریست کن")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("خیر")
            }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onPasswordChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تغییر پسورد") },
        text = {
            Column {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("پسورد فعلی") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("پسورد جدید") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmNewPassword,
                    onValueChange = { confirmNewPassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("تکرار پسورد جدید") },
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
                )
                Spacer(modifier = Modifier.height(6.dp))
                FilledTonalButton(
                    onClick = { showPassword = !showPassword },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (showPassword) "مخفی‌کردن پسوردها" else "نمایش پسوردها")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        oldPassword.isBlank() || newPassword.isBlank() || confirmNewPassword.isBlank() -> {
                            scope.launch {
                                snackbarHostState.showSnackbar("همه‌ی فیلدها را پر کن.")
                            }
                        }
                        !verifyAccountPassword(context, oldPassword) -> {
                            scope.launch {
                                snackbarHostState.showSnackbar("پسورد فعلی اشتباه است.")
                            }
                        }
                        newPassword.length < 4 -> {
                            scope.launch {
                                snackbarHostState.showSnackbar("پسورد جدید باید حداقل ۴ کاراکتر باشد.")
                            }
                        }
                        newPassword != confirmNewPassword -> {
                            scope.launch {
                                snackbarHostState.showSnackbar("پسورد جدید و تکرار آن یکی نیست.")
                            }
                        }
                        else -> {
                            setAccountPassword(context, newPassword)
                            onPasswordChanged()
                        }
                    }
                }
            ) {
                Text("ذخیره پسورد جدید")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بی‌خیال")
            }
        }
    )
}
