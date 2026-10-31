package ru.upestop.app.ui.journal

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.upestop.app.AppContainer
import ru.upestop.app.R
import ru.upestop.app.data.StopSnapshot
import ru.upestop.app.domain.ReportStrings
import ru.upestop.app.domain.StopReportBuilder
import ru.upestop.app.ui.components.LoadingScreen
import ru.upestop.app.ui.components.UpeTopBar
import ru.upestop.app.ui.components.appViewModel
import ru.upestop.app.ui.components.rememberNow
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText
import java.time.ZoneId

class JournalViewModel(container: AppContainer) : ViewModel() {
    val snapshot: StateFlow<StopSnapshot?> =
        container.stopSnapshot.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

fun Context.reportStrings() = ReportStrings(
    header = getString(R.string.report_header),
    generated = getString(R.string.report_generated),
    done = getString(R.string.report_done),
    marksRange = getString(R.string.report_marks_range),
    noMarks = getString(R.string.report_no_marks),
    timersHeader = getString(R.string.report_timers_header),
    timerLine = getString(R.string.report_timer_line),
    timerIdle = getString(R.string.report_timer_idle),
    timerRunning = getString(R.string.report_timer_running),
    timerPaused = getString(R.string.report_timer_paused),
    timerFinished = getString(R.string.report_timer_finished),
    phaseLine = getString(R.string.report_phase_line),
    critical = getString(R.string.report_critical),
)

@Composable
fun JournalScreen(
    onBack: () -> Unit,
    viewModel: JournalViewModel = appViewModel { container, _ -> JournalViewModel(container) },
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val state = snapshot ?: return LoadingScreen()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val strings = remember(context) { context.reportStrings() }
    // Минутной точности достаточно: журнал пересобирается раз в минуту и при любом изменении отметок.
    val minute = rememberNow() / 60_000L
    val report = remember(state, minute) {
        StopReportBuilder.build(state.algorithm, state.checked, state.timers, System.currentTimeMillis(), ZoneId.systemDefault(), strings)
    }
    val copiedMessage = stringResource(R.string.journal_copied)
    val chooserTitle = stringResource(R.string.journal_share_chooser)

    Scaffold(
        containerColor = UpeColors.Bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { UpeTopBar(title = stringResource(R.string.journal_title), onBack = onBack) },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(UpeColors.Surface)
                    .navigationBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_SUBJECT, state.algorithm.title)
                            .putExtra(Intent.EXTRA_TEXT, report)
                        context.startActivity(Intent.createChooser(send, chooserTitle))
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UpeColors.Cool, contentColor = UpeColors.Bg),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text(stringResource(R.string.journal_share), modifier = Modifier.padding(start = 8.dp))
                }
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(report))
                        scope.launch { snackbar.showSnackbar(copiedMessage) }
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UpeColors.Surface2, contentColor = UpeColors.Text),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Text(stringResource(R.string.journal_copy), modifier = Modifier.padding(start = 8.dp))
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SelectionContainer {
                Text(
                    text = report,
                    style = UpeText.mono.copy(fontSize = 12.sp, lineHeight = 18.sp),
                    color = UpeColors.Text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(UpeColors.Surface, RectangleShape)
                        .border(1.dp, UpeColors.Border, RectangleShape)
                        .padding(12.dp),
                )
            }
        }
    }
}
