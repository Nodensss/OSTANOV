package ru.upestop.app.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import ru.upestop.app.AppContainer
import ru.upestop.app.UpeStopApp

/** ViewModel с доступом к [AppContainer] и SavedStateHandle (аргументы навигации). */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer, SavedStateHandle) -> VM): VM =
    viewModel(
        factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as UpeStopApp
                create(app.container, createSavedStateHandle())
            }
        },
    )

/** Текущее время, обновляется раз в секунду, пока экран виден. */
@Composable
fun rememberNow(): Long {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                now = System.currentTimeMillis()
                delay(1_000L - now % 1_000L)
            }
        }
    }
    return now
}

/** Не даёт экрану погаснуть, пока composable на экране. */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

class NotificationAccess(val enabled: Boolean, val request: () -> Unit)

fun Context.notificationsAllowed(): Boolean {
    if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
}

fun Context.openAppNotificationSettings() {
    startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/**
 * Статус уведомлений (перепроверяется при возврате на экран) и запрос разрешения.
 * Если системный запрос уже невозможен, открываются настройки уведомлений приложения.
 */
@Composable
fun rememberNotificationAccess(): NotificationAccess {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(context.notificationsAllowed()) }
    LifecycleResumeEffect(context) {
        enabled = context.notificationsAllowed()
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        enabled = context.notificationsAllowed()
        if (!granted) context.openAppNotificationSettings()
    }
    return NotificationAccess(enabled) {
        val permissionMissing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (permissionMissing) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else context.openAppNotificationSettings()
    }
}
