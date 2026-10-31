package ru.upestop.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import ru.upestop.app.ui.AppRoot
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeStopTheme

class MainActivity : ComponentActivity() {

    /** Этап, который нужно открыть в чек-листе (нажатие на уведомление таймера). */
    private val openPhaseRequests = MutableStateFlow<Int?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val barStyle = SystemBarStyle.dark(UpeColors.Bg.toArgb())
        enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)

        val container = (application as UpeStopApp).container
        setContent {
            UpeStopTheme {
                AppRoot(
                    container = container,
                    openPhaseRequests = openPhaseRequests,
                    onOpenPhaseHandled = { openPhaseRequests.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val phaseId = intent?.getIntExtra(EXTRA_OPEN_PHASE, -1) ?: -1
        if (phaseId >= 0) openPhaseRequests.value = phaseId
    }

    companion object {
        const val EXTRA_OPEN_PHASE = "open_phase"
    }
}
