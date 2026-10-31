package ru.upestop.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.StateFlow
import ru.upestop.app.R
import ru.upestop.app.data.model.StopAlgorithm
import ru.upestop.app.ui.checklist.ChecklistScreen
import ru.upestop.app.ui.history.AttemptScreen
import ru.upestop.app.ui.history.AttemptViewModel
import ru.upestop.app.ui.history.HistoryScreen
import ru.upestop.app.ui.home.HomeNavigation
import ru.upestop.app.ui.home.HomeScreen
import ru.upestop.app.ui.journal.JournalScreen
import ru.upestop.app.ui.reference.ReferenceScreen
import ru.upestop.app.ui.rules.RulesScreen
import ru.upestop.app.ui.settings.SettingsScreen
import ru.upestop.app.ui.step.StepScreen
import ru.upestop.app.ui.step.StepViewModel
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.trainer.TrainerScreen

object Routes {
    const val HOME = "home"
    const val CHECKLIST = "checklist"
    const val REFERENCE = "reference"
    const val TRAINER = "trainer"
    const val RULES = "rules"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val JOURNAL = "journal"
    const val STEP = "step?${StepViewModel.ARG_CHECK}={${StepViewModel.ARG_CHECK}}"
    const val ATTEMPT = "history/{${AttemptViewModel.ARG_ID}}"

    fun step(checkId: String? = null) = if (checkId == null) "step" else "step?${StepViewModel.ARG_CHECK}=$checkId"
    fun attempt(id: String) = "history/$id"
}

private class TopLevel(val route: String, val label: Int, val icon: ImageVector)

private val topLevel = listOf(
    TopLevel(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    TopLevel(Routes.CHECKLIST, R.string.nav_checklist, Icons.Filled.Checklist),
    TopLevel(Routes.REFERENCE, R.string.nav_reference, Icons.AutoMirrored.Filled.MenuBook),
    TopLevel(Routes.TRAINER, R.string.nav_trainer, Icons.Filled.Quiz),
)

@Composable
fun AppNavigation(algorithm: StopAlgorithm, openPhaseRequests: StateFlow<Int?>, onOpenPhaseHandled: () -> Unit) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    var focusPhase by rememberSaveable { mutableStateOf<Int?>(null) }
    val openPhase by openPhaseRequests.collectAsStateWithLifecycle()

    fun navigateTop(route: String) = navController.navigateTopLevel(route)
    fun openPhaseInChecklist(phaseId: Int) {
        focusPhase = phaseId
        navigateTop(Routes.CHECKLIST)
    }

    LaunchedEffect(openPhase) {
        openPhase?.let {
            openPhaseInChecklist(it)
            onOpenPhaseHandled()
        }
    }

    Scaffold(
        containerColor = UpeColors.Bg,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (topLevel.any { it.route == currentRoute }) {
                NavigationBar(containerColor = UpeColors.Surface) {
                    topLevel.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { navigateTop(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = UpeColors.Bg,
                                selectedTextColor = UpeColors.Cool,
                                indicatorColor = UpeColors.Cool,
                                unselectedIconColor = UpeColors.TextDim,
                                unselectedTextColor = UpeColors.TextDim,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    HomeNavigation(
                        onOpenChecklist = { navigateTop(Routes.CHECKLIST) },
                        onOpenPhase = ::openPhaseInChecklist,
                        onOpenStep = { navController.navigate(Routes.step()) },
                        onOpenReference = { navigateTop(Routes.REFERENCE) },
                        onOpenRules = { navController.navigate(Routes.RULES) },
                        onOpenTrainer = { navigateTop(Routes.TRAINER) },
                        onOpenJournal = { navController.navigate(Routes.JOURNAL) },
                        onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    ),
                )
            }
            composable(Routes.CHECKLIST) {
                ChecklistScreen(
                    focusPhase = focusPhase,
                    onFocusHandled = { focusPhase = null },
                    onOpenStep = { navController.navigate(Routes.step(it)) },
                    onOpenJournal = { navController.navigate(Routes.JOURNAL) },
                )
            }
            composable(
                Routes.STEP,
                arguments = listOf(
                    navArgument(StepViewModel.ARG_CHECK) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) {
                StepScreen(
                    onBack = { navController.popBackStack() },
                    onOpenChecklist = { navigateTop(Routes.CHECKLIST) },
                    onOpenJournal = { navController.navigate(Routes.JOURNAL) },
                    onHome = { navigateTop(Routes.HOME) },
                )
            }
            composable(Routes.REFERENCE) { ReferenceScreen(onOpenRules = { navController.navigate(Routes.RULES) }) }
            composable(Routes.RULES) { RulesScreen(algorithm, onBack = { navController.popBackStack() }) }
            composable(Routes.TRAINER) {
                TrainerScreen(onOpenHistory = { navController.navigate(Routes.HISTORY) })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAttempt = { navController.navigate(Routes.attempt(it)) },
                )
            }
            composable(
                Routes.ATTEMPT,
                arguments = listOf(navArgument(AttemptViewModel.ARG_ID) { type = NavType.StringType }),
            ) { AttemptScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.JOURNAL) { JournalScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

/** Переход между разделами нижней панели с сохранением их состояния. */
private fun NavHostController.navigateTopLevel(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
