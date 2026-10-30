package ru.upestop.app.ui

import android.content.*
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.upestop.app.*
import ru.upestop.app.R
import ru.upestop.app.data.Setting
import ru.upestop.app.data.model.*
import ru.upestop.app.ui.components.*
import ru.upestop.app.ui.theme.*

@Composable
fun ReportScreen(data: StopAlgorithm, stored: StoredState, snackbar: SnackbarHostState) {
    val context = LocalContext.current
    val report = remember(data, stored.checks, stored.timers, stored.settings.testMode) { buildReport(context, data, stored.checks, stored.timers, stored.settings.testMode) }
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.copied)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Action(stringResource(R.string.share)) { shareReport(context, report) } }
        item { Action(stringResource(R.string.copy)) {
            context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(context.getString(R.string.report), report))
            scope.launch { snackbar.showSnackbar(copied) }
        } }
        item { androidx.compose.foundation.text.selection.SelectionContainer { Text(report, fontFamily = FontFamily.Monospace) } }
    }
}
@Composable
fun SettingsScreen(data: StopAlgorithm, stored: StoredState, vm: AppViewModel, revision: Int, allowNotifications: () -> Unit) {
    val context = LocalContext.current
    val allowed = remember(revision) { vm.app.timers.notifications.allowed() }
    val exact = remember(revision) { vm.app.timers.scheduler.exactAllowed() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SettingSwitch(stringResource(R.string.keep_screen), stored.settings.keepScreen) { vm.setting(Setting.KEEP_SCREEN, it) } }
        item { SettingSwitch(stringResource(R.string.sound), stored.settings.sound) { vm.setting(Setting.SOUND, it) } }
        item { SettingSwitch(stringResource(R.string.vibration), stored.settings.vibration) { vm.setting(Setting.VIBRATION, it) } }
        if (BuildConfig.DEBUG) item { Panel(color = Danger) {
            Label(stringResource(R.string.test_only), Danger)
            val enabled = stored.timers.none { it.running }
            SettingSwitch(stringResource(R.string.test_mode), stored.settings.testMode, enabled) { vm.setting(Setting.TEST_MODE, it) }
            if (!enabled) Text(stringResource(R.string.test_blocked))
        } }
        item { Panel {
            Label(stringResource(R.string.permissions), Cool)
            Text(stringResource(R.string.notification_status, stringResource(R.string.notifications), stringResource(if (allowed) R.string.enabled else R.string.disabled)))
            Action(stringResource(R.string.open_settings), onClick = allowNotifications)
            Text(stringResource(R.string.notification_status, stringResource(R.string.exact_alarms), stringResource(if (exact) R.string.enabled else R.string.disabled)))
            Action(stringResource(R.string.open_settings)) {
                val intent = if (Build.VERSION.SDK_INT >= 31) Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                    else Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                try { context.startActivity(intent) } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                }
            }
        } }
        item { Panel {
            Label(stringResource(R.string.about), Cool)
            Text(stringResource(R.string.app_name)); Text(stringResource(R.string.version, BuildConfig.VERSION_NAME))
            Text(data.title); Text(data.subtitle)
            data.sources.forEach { Text(it, color = Dim) }
            Text(stringResource(R.string.metrics, data.phases.size, data.phases.sumOf { it.checks.size }, data.criticalRules.size, data.trainerQuestions.size))
            Text(stringResource(R.string.offline)); Text(stringResource(R.string.disclaimer), color = Dim)
        } }
    }
}
@Composable
private fun SettingSwitch(text: String, value: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(text, Modifier.weight(1f)); Switch(checked = value, onCheckedChange = onChange, enabled = enabled)
    }
}
