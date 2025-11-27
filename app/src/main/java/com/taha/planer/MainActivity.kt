package com.taha.planer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.taha.planer.features.account.AccountScreen
import com.taha.planer.features.alarms.AlarmsScreen
import com.taha.planer.features.assistant.AssistantScreen
import com.taha.planer.features.calendar.CalendarScreen
import com.taha.planer.features.calm.CalmScreen
import com.taha.planer.features.dashboard.DashboardScreen
import com.taha.planer.features.finance.FinanceScreen
import com.taha.planer.features.focus.FocusScreen
import com.taha.planer.features.habits.HabitsScreen
import com.taha.planer.features.habitbuilder.HabitBuilderScreen
import com.taha.planer.features.health.HealthScreen
import com.taha.planer.features.journal.JournalScreen
import com.taha.planer.features.longterm.LongTermPlanningScreen
import com.taha.planer.features.media.MediaScreen
import com.taha.planer.features.mood.MoodScreen
import com.taha.planer.features.passwords.PasswordsScreen
import com.taha.planer.features.rewards.RewardsScreen
import com.taha.planer.features.routines.RoutinesScreen
import com.taha.planer.features.settings.SettingsScreen
import com.taha.planer.features.sport.SportScreen
import com.taha.planer.features.tasks.TasksScreen
import com.taha.planer.features.security.SecurityScreen
import com.taha.planer.ui.Theme.PlannerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * رشته‌ی روت‌های ناوبری – همگی ساده و خواناست
 */
object PlannerDestinations {
    const val DASHBOARD = "dashboard"
    const val TASKS = "tasks"
    const val HABITS = "habits"
    const val ROUTINES = "routines"
    const val HEALTH = "health"
    const val SPORT = "sport"
    const val MOOD = "mood"
    const val CALM = "calm"
    const val FOCUS = "focus"
    const val REWARDS = "rewards"
    const val MEDIA = "media"
    const val FINANCE = "finance"
    const val JOURNAL = "journal"
    const val LONG_TERM = "longterm"
    const val PASSWORDS = "passwords"
    const val CALENDAR = "calendar"
    const val ALARMS = "alarms"
    const val ASSISTANT = "assistant"
    const val SETTINGS = "settings"
    const val ACCOUNT = "account"
    const val SECURITY = "security"
}

/**
 * تب‌های پایین صفحه (Bottom Bar)
 */
enum class BottomTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Productivity(
        route = PlannerDestinations.TASKS,
        label = "کارها و عادت‌ها",
        icon = Icons.Filled.List
    ),
    Dashboard(
        route = PlannerDestinations.DASHBOARD,
        label = "داشبورد",
        icon = Icons.Filled.Dashboard
    ),
    Assistant(
        route = PlannerDestinations.ASSISTANT,
        label = "دستیار",
        icon = Icons.Filled.AccountCircle
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PlannerTheme {
                PlannerRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerRoot() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            PlannerDrawerContent(
                currentDestination = currentDestination,
                onDestinationSelected = { route ->
                    scope.launch {
                        drawerState.close()
                    }
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = {
                        Text(text = currentTopBarTitle(currentDestination))
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.toggleDrawer(drawerState)
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "منو"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavigationBar(
                    navController = navController,
                    currentDestination = currentDestination
                )
            }
        ) { paddingValues ->
            PlannerNavHost(
                navController = navController,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

private fun CoroutineScope.toggleDrawer(drawerState: DrawerState) {
    launch {
        if (drawerState.isOpen) {
            drawerState.close()
        } else {
            drawerState.open()
        }
    }
}

@Composable
private fun PlannerDrawerContent(
    currentDestination: NavDestination?,
    onDestinationSelected: (String) -> Unit
) {
    val items = listOf(
        Triple(PlannerDestinations.DASHBOARD, "داشبورد اصلی", Icons.Filled.Dashboard),
        Triple(PlannerDestinations.TASKS, "کارها", Icons.Filled.List),
        Triple(PlannerDestinations.HABITS, "عادت‌ها", Icons.Filled.Star),
        Triple(PlannerDestinations.ROUTINES, "روال‌های روزانه", Icons.Filled.Timeline),
        Triple(PlannerDestinations.HEALTH, "سلامت و رژیم", Icons.Filled.FavoriteBorder),
        Triple(PlannerDestinations.SPORT, "ورزش", Icons.Filled.FitnessCenter),
        Triple(PlannerDestinations.MOOD, "مود روزانه", Icons.Filled.Mood),
        Triple(PlannerDestinations.CALM, "آرامش", Icons.Filled.Nightlight),
        Triple(PlannerDestinations.FOCUS, "تمرکز", Icons.Filled.Timeline),
        Triple(PlannerDestinations.REWARDS, "سیستم پاداش‌ها", Icons.Filled.Star),
        Triple(PlannerDestinations.MEDIA, "مدیا / فیلم / کتاب", Icons.Filled.Book),
        Triple(PlannerDestinations.FINANCE, "مالی", Icons.Filled.AccountCircle),
        Triple(PlannerDestinations.JOURNAL, "ژورنال‌نویسی", Icons.Filled.Book),
        Triple(PlannerDestinations.LONG_TERM, "برنامه‌ریزی بلندمدت", Icons.Filled.Timeline),
        Triple(PlannerDestinations.PASSWORDS, "مدیریت پسورد", Icons.Filled.Lock),
        Triple(PlannerDestinations.CALENDAR, "تقویم", Icons.Filled.Event),
        Triple(PlannerDestinations.ALARMS, "آلارم‌ها و یادآوری‌ها", Icons.Filled.Alarm),
        Triple(PlannerDestinations.ASSISTANT, "دستیار هوش مصنوعی", Icons.Filled.AccountCircle),
        Triple(PlannerDestinations.SETTINGS, "تنظیمات", Icons.Filled.Settings),
        Triple(PlannerDestinations.ACCOUNT, "حساب کاربری", Icons.Filled.AccountCircle),
        Triple(PlannerDestinations.SECURITY, "امنیت برنامه", Icons.Filled.Lock)
    )

    androidx.compose.foundation.layout.Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "بخش‌ها",
            style = MaterialTheme.typography.titleMedium
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))

        items.forEach { (route, label, icon) ->
            val selected = currentDestination.isInHierarchy(route)
            NavigationDrawerItem(
                icon = {
                    Icon(imageVector = icon, contentDescription = label)
                },
                label = {
                    Text(text = label)
                },
                selected = selected,
                onClick = { onDestinationSelected(route) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

@Composable
private fun BottomNavigationBar(
    navController: NavHostController,
    currentDestination: NavDestination?
) {
    NavigationBar {
        BottomTab.values().forEach { tab ->
            val selected = currentDestination.isInHierarchy(tab.route)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label
                    )
                },
                label = {
                    Text(text = tab.label)
                }
            )
        }
    }
}

private fun NavDestination?.isInHierarchy(route: String): Boolean {
    return this?.hierarchy?.any { it.route == route } == true
}

@Composable
private fun currentTopBarTitle(currentDestination: NavDestination?): String {
    return when (currentDestination?.route) {
        PlannerDestinations.DASHBOARD -> "داشبورد"
        PlannerDestinations.TASKS -> "کارها"
        PlannerDestinations.HABITS -> "عادت‌ها"
        PlannerDestinations.ROUTINES -> "روال‌های روزانه"
        PlannerDestinations.HEALTH -> "سلامت"
        PlannerDestinations.SPORT -> "ورزش"
        PlannerDestinations.MOOD -> "مود"
        PlannerDestinations.CALM -> "آرامش"
        PlannerDestinations.FOCUS -> "تمرکز"
        PlannerDestinations.REWARDS -> "پاداش‌ها"
        PlannerDestinations.MEDIA -> "مدیا، فیلم و کتاب"
        PlannerDestinations.FINANCE -> "مالی"
        PlannerDestinations.JOURNAL -> "ژورنال‌نویسی"
        PlannerDestinations.LONG_TERM -> "برنامه‌ریزی بلندمدت"
        PlannerDestinations.PASSWORDS -> "مدیریت پسورد"
        PlannerDestinations.CALENDAR -> "تقویم"
        PlannerDestinations.ALARMS -> "آلارم‌ها"
        PlannerDestinations.ASSISTANT -> "دستیار هوش مصنوعی"
        PlannerDestinations.SETTINGS -> "تنظیمات"
        PlannerDestinations.ACCOUNT -> "حساب کاربری"
        PlannerDestinations.SECURITY -> "امنیت برنامه"
        else -> "پلنر"
    }
}

@Composable
private fun PlannerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = PlannerDestinations.DASHBOARD,
        modifier = modifier
    ) {
        composable(PlannerDestinations.DASHBOARD) {
            DashboardScreen(
                onNavigateToTasks = { navController.navigate(PlannerDestinations.TASKS) },
                onNavigateToHabits = { navController.navigate(PlannerDestinations.HABITS) },
                onNavigateToHealth = { navController.navigate(PlannerDestinations.HEALTH) },
                onNavigateToSleep = { navController.navigate(PlannerDestinations.HEALTH) },
                onNavigateToFinance = { navController.navigate(PlannerDestinations.FINANCE) },
                onNavigateToFocus = { navController.navigate(PlannerDestinations.FOCUS) },
                onNavigateToLongTerm = { navController.navigate(PlannerDestinations.LONG_TERM) }
            )
        }

        composable(PlannerDestinations.TASKS) { TasksScreen() }
        composable(PlannerDestinations.HABITS) { HabitsScreen() }
        composable(PlannerDestinations.ROUTINES) { RoutinesScreen() }
        composable(PlannerDestinations.HEALTH) { HealthScreen() }
        composable(PlannerDestinations.SPORT) { SportScreen() }
        composable(PlannerDestinations.MOOD) { MoodScreen() }
        composable(PlannerDestinations.CALM) { CalmScreen() }
        composable(PlannerDestinations.FOCUS) { FocusScreen() }
        composable(PlannerDestinations.REWARDS) { RewardsScreen() }
        composable(PlannerDestinations.MEDIA) { MediaScreen() }
        composable(PlannerDestinations.FINANCE) { FinanceScreen() }
        composable(PlannerDestinations.JOURNAL) { JournalScreen() }
        composable(PlannerDestinations.LONG_TERM) { LongTermPlanningScreen() }
        composable(PlannerDestinations.PASSWORDS) { PasswordsScreen() }
        composable(PlannerDestinations.CALENDAR) { CalendarScreen() }
        composable(PlannerDestinations.ALARMS) { AlarmsScreen() }
        composable(PlannerDestinations.ASSISTANT) { AssistantScreen() }
        composable(PlannerDestinations.SETTINGS) { SettingsScreen() }
        composable(PlannerDestinations.ACCOUNT) { AccountScreen() }
        composable(PlannerDestinations.SECURITY) { SecurityScreen() }
    }
}
