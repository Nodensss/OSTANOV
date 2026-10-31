package ru.upestop.app.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.upestop.app.AppContainer
import ru.upestop.app.BuildConfig
import ru.upestop.app.R
import ru.upestop.app.data.StopSnapshot
import ru.upestop.app.timer.TimerNotifications
import ru.upestop.app.ui.components.LoadingScreen
import ru.upestop.app.ui.components.Panel
import ru.upestop.app.ui.components.SectionLabel
import ru.upestop.app.ui.components.UpeTopBar
import ru.upestop.app.ui.components.appViewModel
import ru.upestop.app.ui.components.rememberNotificationAccess
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText

class SettingsViewModel(private val container: AppContainer) : ViewModel() {
    val snapshot: StateFlow<StopSnapshot?> =
        container.stopSnapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun canScheduleExactAlarms() = container.alarmScheduler.canScheduleExact()

    fun setKeepScreenOn(value: Boolean) = launch { container.settingsRepository.setKeepScreenOn(value) }

    fun setHaptics(value: Boolean) = launch { container.settingsRepository.setHaptics(value) }

    fun setTimerTestMode(value: Boolean) = launch { container.settingsRepository.setTimerTestMode(value) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = appViewModel { container, _ -> SettingsViewModel(container) },
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val state = snapshot ?: return LoadingScreen()
    val settings = state.settings
    val algorithm = state.algorithm
    val context = LocalContext.current
    val notifications = rememberNotificationAccess()
    var exactAlarms by remember { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    LifecycleResumeEffect(Unit) {
        exactAlarms = viewModel.canScheduleExactAlarms()
        onPauseOrDispose { }
    }
    val anyTimerRunning = state.timers.values.any { it.running }

    Scaffold(
        containerColor = UpeColors.Bg,
        topBar = { UpeTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Panel {
                SettingSwitch(
                    title = stringResource(R.string.settings_keep_screen),
                    note = stringResource(R.string.settings_keep_screen_note),
                    checked = settings.keepScreenOn,
                    onChange = viewModel::setKeepScreenOn,
                )
                SettingSwitch(
                    title = stringResource(R.string.settings_haptics),
                    note = stringResource(R.string.settings_haptics_note),
                    checked = settings.haptics,
                    onChange = viewModel::setHaptics,
                )
                SettingAction(
                    title = stringResource(R.string.settings_timer_signal),
                    note = stringResource(R.string.settings_timer_signal_note),
                    action = stringResource(R.string.settings_open_channel),
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .putExtra(Settings.EXTRA_CHANNEL_ID, TimerNotifications.CHANNEL_FINISHED),
                        )
                    },
                )
            }

            Panel {
                SectionLabel(stringResource(R.string.settings_permissions))
                SettingAction(
                    title = stringResource(R.string.settings_notifications),
                    note = stringResource(if (notifications.enabled) R.string.status_allowed else R.string.status_denied),
                    noteColor = if (notifications.enabled) UpeColors.Safe else UpeColors.Warn,
                    action = stringResource(if (notifications.enabled) R.string.settings_open else R.string.action_allow),
                    onClick = notifications.request,
                )
                SettingAction(
                    title = stringResource(R.string.settings_exact_alarms),
                    note = stringResource(if (exactAlarms) R.string.status_allowed else R.string.status_denied),
                    noteColor = if (exactAlarms) UpeColors.Safe else UpeColors.Warn,
                    action = if (exactAlarms || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) null else stringResource(R.string.action_allow),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                            )
                        }
                    },
                )
            }

            if (BuildConfig.DEBUG) {
                Panel(border = UpeColors.Danger.copy(alpha = 0.6f)) {
                    SectionLabel(stringResource(R.string.settings_debug), color = UpeColors.Danger)
                    SettingSwitch(
                        title = stringResource(R.string.settings_test_mode),
                        note = if (anyTimerRunning) stringResource(R.string.settings_test_mode_locked) else null,
                        checked = settings.timerTestMode,
                        enabled = !anyTimerRunning,
                        onChange = viewModel::setTimerTestMode,
                    )
                }
            }

            Panel {
                SectionLabel(stringResource(R.string.about_title))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = UpeText.mono, color = UpeColors.TextDim)
                Text(algorithm.title, color = UpeColors.Text)
                if (algorithm.subtitle.isNotBlank()) Text(algorithm.subtitle, color = UpeColors.TextDim, fontSize = 14.sp, lineHeight = 20.sp)
                Text(
                    stringResource(
                        R.string.about_counts,
                        algorithm.phases.size,
                        state.total,
                        algorithm.criticalRules.size,
                        algorithm.trainerQuestions.size,
                    ),
                    style = UpeText.mono.copy(fontSize = 12.sp),
                    color = UpeColors.TextDim,
                )
                if (algorithm.sources.isNotEmpty()) {
                    SectionLabel(stringResource(R.string.about_sources))
                    algorithm.sources.forEach { Text("• $it", color = UpeColors.TextDim, fontSize = 14.sp, lineHeight = 20.sp) }
                }
                Text(stringResource(R.string.about_offline), color = UpeColors.Safe, fontSize = 14.sp, lineHeight = 20.sp)
                Text(stringResource(R.string.disclaimer), color = UpeColors.TextMute, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    note: String?,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = if (enabled) UpeColors.Text else UpeColors.TextMute)
            note?.let { Text(it, color = UpeColors.TextMute, fontSize = 13.sp, lineHeight = 18.sp) }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = UpeColors.Bg,
                checkedTrackColor = UpeColors.Cool,
                uncheckedThumbColor = UpeColors.TextMute,
                uncheckedTrackColor = UpeColors.Surface2,
                uncheckedBorderColor = UpeColors.Border,
            ),
        )
    }
}

@Composable
private fun SettingAction(
    title: String,
    note: String,
    action: String?,
    onClick: () -> Unit,
    noteColor: Color = UpeColors.TextMute,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = UpeColors.Text)
            Text(note, color = noteColor, fontSize = 13.sp, lineHeight = 18.sp)
        }
        action?.let { TextButton(onClick = onClick) { Text(it) } }
    }
}
