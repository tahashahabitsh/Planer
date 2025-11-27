package com.taha.planer

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.taha.planer.features.calendar.CalendarScreen
import com.taha.planer.features.calm.CalmScreen
import com.taha.planer.features.dashboard.DashboardScreen
import com.taha.planer.features.finance.FinanceScreen
import com.taha.planer.features.focus.FocusScreen
import com.taha.planer.features.habits.HabitsScreen
import com.taha.planer.features.health.HealthScreen
import com.taha.planer.features.habitbuild.HabitBuildScreen
import com.taha.planer.features.journal.JournalScreen
import com.taha.planer.features.longterm.LongTermPlanningScreen
import com.taha.planer.features.media.MediaScreen
import com.taha.planer.features.mood.MoodScreen
import com.taha.planer.features.passwords.PasswordsScreen
import com.taha.planer.features.rewards.RewardsScreen
import com.taha.planer.features.routines.RoutinesScreen
import com.taha.planer.features.sport.SportScreen
import com.taha.planer.features.tasks.TasksScreen
import com.taha.planer.features.alarms.AlarmsScreen
import com.taha.planer.features.assistant.AssistantScreen
import com.taha.planer.security.AppLockScreen
import com.taha.planer.security.isAppLockEnabled
import com.taha.planer.security.isSecureFlagEnabled
import com.taha.planer.security.setAppLockEnabled
import com.taha.planer.security.setSecureFlagEnabled
import com.taha.planer.ui.theme.PlanerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isSecureFlagEnabled(this)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        setContent {
            PlanerTheme {
                val context = LocalContext.current
                val appLockEnabled = isAppLockEnabled(context)
                var isUnlocked by remember { mutableStateOf(!appLockEnabled) }

                if (appLockEnabled && !isUnlocked) {
                    AppLockScreen(
                        onUnlocked = { isUnlocked = true }
                    )
                } else {
                    val navController = rememberNavController()
                    MainScaffold(navController = navController)
                }
            }
        }
    }
}

enum class MainRoute(val route: String, val title: String) {
    Dashboard("dashboard", "داشبورد"),
    Productivity("productivity", "کارها / عادت‌ها / روال‌ها"),
    AssistantSettings("assistant_settings", "دستیار / تنظیمات")
}

data class BottomNavItem(
    val route: MainRoute,
    val label: String,
    val icon: ImageVector
)

data class DrawerItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(navController: NavHostController) {
    val bottomItems = listOf(
        BottomNavItem(MainRoute.Productivity, "کارها / عادت‌ها", Icons.Filled.List),
        BottomNavItem(MainRoute.Dashboard, "داشبورد", Icons.Filled.Dashboard),
        BottomNavItem(MainRoute.AssistantSettings, "دستیار / تنظیمات", Icons.Filled.Settings)
    )

    val drawerItems = listOf(
        DrawerItem("section_tasks", "کارها", Icons.Filled.List),
        DrawerItem("section_habits", "عادت‌ها", Icons.Filled.Star),
        DrawerItem("section_routines", "روال‌های روزانه", Icons.Filled.Timeline),
        DrawerItem("section_health", "سلامت", Icons.Filled.FavoriteBorder),
        DrawerItem("section_media", "مدیا / فیلم / کتاب", Icons.Filled.Book),
        DrawerItem("section_sport", "ورزش", Icons.Filled.FitnessCenter),
        DrawerItem("section_mood", "مود", Icons.Filled.Mood),
        DrawerItem("section_calm", "آرامش", Icons.Filled.Nightlight),
        DrawerItem("section_rewards", "پاداش‌ها", Icons.Filled.Star),
        DrawerItem("section_calendar", "تقویم", Icons.Filled.Event),
        DrawerItem("section_alarms", "آلارم‌ها و نوتیف", Icons.Filled.Alarm),
        DrawerItem("section_finance", "مالی", Icons.Filled.AccountBalanceWallet),
        DrawerItem("section_journal", "ژورنال نویسی", Icons.Filled.Book),
        DrawerItem("section_passwords", "مدیریت پسورد", Icons.Filled.Lock),
        DrawerItem("section_focus", "تمرکز", Icons.Filled.Timeline),
        DrawerItem("section_habit_build", "ساخت عادت", Icons.Filled.Star),
        DrawerItem("section_long_term", "برنامه‌ریزی بلندمدت", Icons.Filled.Timeline),
        DrawerItem("section_settings", "تنظیمات", Icons.Filled.Settings),
        DrawerItem("section_account", "حساب کاربری", Icons.Filled.AccountCircle)
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var selectedBottom by remember { mutableStateOf(MainRoute.Dashboard) }
    var currentTitle by remember { mutableStateOf(MainRoute.Dashboard.title) }
    var selectedDrawerRoute by remember { mutableStateOf<String?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                drawerItems.forEach { item ->
                    val selected = selectedDrawerRoute == item.route
                    NavigationDrawerItem(
                        label = { Text(text = item.label) },
                        selected = selected,
                        onClick = {
                            selectedDrawerRoute = item.route
                            currentTitle = item.label
                            scope.launch { drawerState.close() }
                            navController.navigate(item.route) {
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(imageVector = item.icon, contentDescription = item.label) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(text = currentTitle) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "منو"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = selectedBottom == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                selectedBottom = item.route
                                selectedDrawerRoute = null
                                currentTitle = item.route.title
                                navController.navigate(item.route.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(text = item.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                MainNavHost(navController = navController)
            }
        }
    }
}

@Composable
fun MainNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = MainRoute.Dashboard.route
    ) {
        composable(MainRoute.Dashboard.route) {
            DashboardScreen(navController)
        }
        composable(MainRoute.Productivity.route) {
            ProductivityHubScreen()
        }
        composable(MainRoute.AssistantSettings.route) {
            AssistantAndSettingsHubScreen()
        }

        composable("section_tasks") { TasksScreen() }
        composable("section_habits") { HabitsScreen() }
        composable("section_routines") { RoutinesScreen() }
        composable("section_health") { HealthScreen() }
        composable("section_media") { MediaScreen() }
        composable("section_sport") { SportScreen() }
        composable("section_mood") { MoodScreen() }
        composable("section_calm") { CalmScreen() }
        composable("section_rewards") { RewardsScreen() }
        composable("section_calendar") { CalendarScreen() }
        composable("section_alarms") { AlarmsScreen() }
        composable("section_finance") { FinanceScreen() }
        composable("section_journal") { JournalScreen() }
        composable("section_passwords") { PasswordsScreen() }
        composable("section_focus") { FocusScreen() }
        composable("section_habit_build") { HabitBuildScreen() }
        composable("section_long_term") { LongTermPlanningScreen() }
        composable("section_settings") { SettingsScreen() }
        composable("section_account") { AccountScreen() }
    }
}
@Composable
fun SectionPlaceholder(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ProductivityHubScreen() = SectionPlaceholder(
    title = "کارها / عادت‌ها / روال‌ها",
    subtitle = "از منوی کناری وارد بخش کارها، عادت‌ها و روال‌ها می‌شی."
)

@Composable
fun AssistantAndSettingsHubScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // بالایش یک توضیح کوتاه
        Text(
            text = "دستیار هوش مصنوعی",
            modifier = Modifier
                .padding(start = 16.dp, top = 12.dp, end = 16.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "در پایین این صفحه چت با دستیار را داری. تنظیمات ظاهری و امنیتی را از منوی کناری → تنظیمات عوض می‌کنی.",
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            AssistantScreen()
        }
    }
}

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val activity = context as? Activity

    var secureEnabled by remember { mutableStateOf(isSecureFlagEnabled(context)) }
    var appLockEnabled by remember { mutableStateOf(isAppLockEnabled(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "تنظیمات",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "امنیت",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // قفل پین برنامه
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "قفل پین برنامه",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "وقتی فعال باشد، هنگام باز کردن برنامه باید پین را وارد کنی.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = appLockEnabled,
                onCheckedChange = { enabled ->
                    appLockEnabled = enabled
                    setAppLockEnabled(context, enabled)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // جلوگیری از اسکرین‌شات
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "جلوگیری از اسکرین‌شات",
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "وقتی فعال باشد، از گرفتن اسکرین‌شات و ضبط صفحه داخل برنامه جلوگیری می‌شود.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = secureEnabled,
                onCheckedChange = { enabled ->
                    secureEnabled = enabled
                    setSecureFlagEnabled(context, enabled)
                    if (enabled) {
                        activity?.window?.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    } else {
                        activity?.window?.clearFlags(
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    }
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "سایر تنظیمات (تم، استایل گلاس/نئو/Illustration و...) را بعداً اینجا اضافه می‌کنیم.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun AccountScreen() = SectionPlaceholder(
    title = "حساب کاربری",
    subtitle = "پروفایل، سینک، و بعداً اشتراک‌ها."
)
