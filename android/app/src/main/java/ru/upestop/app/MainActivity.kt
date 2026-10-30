package ru.upestop.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.flow.MutableStateFlow
import ru.upestop.app.timer.TimerScheduler
import ru.upestop.app.ui.UpeApp
import ru.upestop.app.ui.theme.UpeTheme

class MainActivity : ComponentActivity() {
    val pendingPhase = MutableStateFlow<Int?>(null)
    val permissionRevision = mutableIntStateOf(0)
    private val app get() = (application as UpeStopApp).container
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) readIntent(intent)
        setContent { UpeTheme { UpeApp(this) } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); readIntent(intent) }
    private fun readIntent(intent: Intent) {
        if (intent.hasExtra(TimerScheduler.EXTRA_PHASE)) pendingPhase.value = intent.getIntExtra(TimerScheduler.EXTRA_PHASE, -1)
    }
    override fun onStart() { super.onStart(); app.foreground = true; app.launch { app.timers.restore() } }
    override fun onStop() { app.foreground = false; super.onStop() }
    override fun onResume() { super.onResume(); permissionRevision.intValue++ }
}
