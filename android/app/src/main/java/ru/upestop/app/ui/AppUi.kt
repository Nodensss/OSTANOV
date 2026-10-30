package ru.upestop.app.ui

import android.Manifest
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.*
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import kotlinx.coroutines.*
import ru.upestop.app.*
import ru.upestop.app.R
import ru.upestop.app.ui.components.*
import ru.upestop.app.domain.TimerMath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpeApp(activity: MainActivity, vm: AppViewModel = viewModel()) {
    val data by vm.algorithm.collectAsStateWithLifecycle()
    val error by vm.dataError.collectAsStateWithLifecycle()
    val stored by vm.stored.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val dbError by vm.app.errors.collectAsStateWithLifecycle()
    val phase by activity.pendingPhase.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val snackbar = remember { SnackbarHostState() }
    val owner = LocalLifecycleOwner.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var menu by remember { mutableStateOf(false) }
    var reset by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { activity.permissionRevision.intValue++ }
    val permissionRevision = activity.permissionRevision.intValue
    val notificationsAllowed = remember(permissionRevision) { vm.app.timers.notifications.allowed() }
    fun allowNotifications() {
        activity.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, activity.packageName))
    }
    LaunchedEffect(Unit) { if (Build.VERSION.SDK_INT >= 33 && !notificationsAllowed) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    LaunchedEffect(owner) { owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { while (isActive) { now = System.currentTimeMillis(); delay(1000) } } }
    LaunchedEffect(now, stored.timers, stored.settings.testMode, data) {
        val expired = stored.timers.any { state ->
            val spec = data?.phases?.firstOrNull { it.id == state.phaseId }?.timer
            state.running && spec != null && TimerMath.remaining(state, TimerMath.duration(spec.minutes, stored.settings.testMode), now) == 0L
        }
        if (expired) vm.app.launch { vm.app.timers.restore(now) }
    }
    LaunchedEffect(dbError) { dbError?.let { snackbar.showSnackbar(it); vm.app.errors.value = null } }
    LaunchedEffect(phase, data, stored.ready) {
        if (phase != null && data != null && stored.ready) { nav.navigate("checklist?phase=$phase"); activity.pendingPhase.value = null }
    }
    val keepScreen = stored.settings.keepScreen && (route.startsWith("checklist") || route == "step")
    DisposableEffect(keepScreen) {
        if (keepScreen) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val tabs = listOf(Triple("home", R.string.home, Icons.Default.Home), Triple("checklist?phase=-1", R.string.checklist, Icons.Default.Checklist),
        Triple("reference", R.string.reference, Icons.Default.Search), Triple("trainer", R.string.trainer, Icons.Default.School))
    val titles = mapOf("home" to R.string.home, "reference" to R.string.reference, "trainer" to R.string.trainer,
        "step" to R.string.step, "rules" to R.string.rules, "history" to R.string.history, "attempt/{id}" to R.string.attempt_details,
        "settings" to R.string.settings, "report" to R.string.report, "timers" to R.string.timers)
    val isRoot = route in listOf("home", "reference", "trainer") || route.startsWith("checklist")
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(if (route.startsWith("checklist")) R.string.checklist else titles[route] ?: R.string.app_name)) },
            navigationIcon = { if (!isRoot && route.isNotBlank()) IconButton( onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } },
            actions = {
                if (route == "reference") IconButton( onClick = { nav.navigate("rules") }) { Icon(Icons.Default.Warning, stringResource(R.string.rules)) }
                if (route.startsWith("checklist")) Box {
                    IconButton( onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.menu)) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.report)) }, onClick = { menu = false; nav.navigate("report") })
                        DropdownMenuItem(text = { Text(stringResource(R.string.timers)) }, onClick = { menu = false; nav.navigate("timers") })
                        DropdownMenuItem(text = { Text(stringResource(R.string.reset_progress)) }, onClick = { menu = false; reset = true })
                    }
                }
            }) },
        bottomBar = { NavigationBar { tabs.forEach { (target, label, icon) ->
            NavigationBarItem(enabled = data != null && stored.ready && error == null, selected = route.substringBefore('?') == target.substringBefore('?'), onClick = {
                nav.navigate(target) { popUpTo(nav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true; restoreState = true }
            }, icon = { Icon(icon, stringResource(label)) }, label = { Text(stringResource(label)) })
        } } }, snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (error != null) Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.data_error)); Text(error.orEmpty()); Action(stringResource(R.string.retry), onClick = { vm.load() })
            } else if (data == null || !stored.ready) Text(stringResource(R.string.loading), Modifier.padding(24.dp))
            else {
                val algorithm = data!!
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") { HomeScreen(algorithm, stored, now) { nav.navigate(it) } }
                    composable("reference") { ReferenceScreen(algorithm) }
                    composable("rules") { RulesScreen(algorithm) }
                    composable("checklist?phase={phase}", arguments = listOf(navArgument("phase") { type = NavType.IntType; defaultValue = -1 })) { backStack ->
                        ChecklistScreen(algorithm, stored, vm, now, busy, backStack.arguments?.getInt("phase") ?: -1, notificationsAllowed, ::allowNotifications)
                    }
                    composable("step") { StepScreen(algorithm, stored, vm, now, busy, notificationsAllowed, ::allowNotifications) { nav.navigate(it) } }
                    composable("timers") { TimersScreen(algorithm, stored, vm, now, busy, notificationsAllowed, ::allowNotifications) }
                    composable("trainer") { TrainerScreen(algorithm, stored, vm, busy) { nav.navigate(it) } }
                    composable("history") { HistoryScreen(stored, vm) { nav.navigate("attempt/$it") } }
                    composable("attempt/{id}") { backStack -> AttemptScreen(algorithm, stored, backStack.arguments?.getString("id").orEmpty(), vm) }
                    composable("report") { Text(stringResource(R.string.report)) }
                    composable("settings") { Text(stringResource(R.string.settings)) }
                }
                if (reset) Confirm(stringResource(R.string.reset_question), { reset = false }, { vm.resetStop(); reset = false },
                    extra = { TextButton(shape = MaterialTheme.shapes.small, onClick = { shareReport(activity, buildReport(activity, algorithm, stored.checks, stored.timers, stored.settings.testMode)) }) { Text(stringResource(R.string.save_report)) } })
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
}
