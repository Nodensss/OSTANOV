package ru.upestop.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import ru.upestop.app.AppContainer
import ru.upestop.app.R
import ru.upestop.app.data.AlgorithmLoad
import ru.upestop.app.domain.ValidationError
import ru.upestop.app.ui.components.LoadingScreen
import ru.upestop.app.ui.components.notificationsAllowed
import ru.upestop.app.ui.navigation.AppNavigation
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText

@Composable
fun AppRoot(container: AppContainer, openPhaseRequests: StateFlow<Int?>, onOpenPhaseHandled: () -> Unit) {
    val load by container.algorithmRepository.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { container.algorithmRepository.load() }
    AskNotificationsOnce()

    when (val current = load) {
        AlgorithmLoad.Loading -> LoadingScreen()
        is AlgorithmLoad.Ready -> AppNavigation(current.algorithm, openPhaseRequests, onOpenPhaseHandled)
        is AlgorithmLoad.ParseError -> DataErrorScreen(listOf(stringResource(R.string.parse_error, current.details)))
        is AlgorithmLoad.Invalid -> DataErrorScreen(current.errors.map { describe(it) })
    }
}

/** Разрешение на уведомления нужно для сигнала таймера; спрашиваем один раз при первом показе. */
@Composable
private fun AskNotificationsOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (!asked && !context.notificationsAllowed()) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun DataErrorScreen(messages: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UpeColors.Bg)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.data_error_title), style = MaterialTheme.typography.headlineSmall, color = UpeColors.Danger)
        Text(stringResource(R.string.data_error_hint), color = UpeColors.TextDim)
        messages.forEach { Text("• $it", style = UpeText.mono, color = UpeColors.Text) }
    }
}

@Composable
private fun describe(error: ValidationError): String = when (error) {
    ValidationError.NoPhases -> stringResource(R.string.validation_no_phases)
    is ValidationError.DuplicatePhaseId -> stringResource(R.string.validation_duplicate_phase, error.phaseId)
    is ValidationError.DuplicateCheckId -> stringResource(R.string.validation_duplicate_check, error.checkId)
    is ValidationError.EmptyCheckText -> stringResource(R.string.validation_empty_check, error.checkId)
    is ValidationError.ParamWithoutValue -> stringResource(R.string.validation_param_without_value, error.checkId)
    is ValidationError.NonPositiveTimer -> stringResource(R.string.validation_timer, error.phaseId)
    is ValidationError.DuplicateRuleId -> stringResource(R.string.validation_duplicate_rule, error.ruleId)
    is ValidationError.DuplicateQuestionId -> stringResource(R.string.validation_duplicate_question, error.questionId)
    is ValidationError.QuestionWithoutOptions -> stringResource(R.string.validation_no_options, error.questionId)
    is ValidationError.CorrectOutOfRange -> stringResource(R.string.validation_correct_range, error.questionId)
    is ValidationError.UnknownQuestionPhase ->
        stringResource(R.string.validation_unknown_phase, error.questionId, error.phaseId)
}
