package ru.upestop.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.upestop.app.R
import ru.upestop.app.data.model.Phase
import ru.upestop.app.data.model.Scheme
import ru.upestop.app.data.model.WarningItem
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = UpeColors.TextMute) {
    Text(text = text.uppercase(), style = UpeText.label, color = color, modifier = modifier)
}

/** Прямоугольная панель с тонкой рамкой — основной блок интерфейса. */
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    border: Color = UpeColors.Border,
    background: Color = UpeColors.Surface,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    spacing: Int = 12,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background, RectangleShape)
            .border(1.dp, border, RectangleShape)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content,
    )
}

@Composable
fun ProgressLine(fraction: Float, modifier: Modifier = Modifier, color: Color = UpeColors.Cool) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(6.dp),
        color = color,
        trackColor = UpeColors.Surface2,
        strokeCap = StrokeCap.Butt,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** Плашка контрольного параметра (янтарная). */
@Composable
fun ParamChip(text: String, modifier: Modifier = Modifier, large: Boolean = false) {
    Text(
        text = text,
        style = UpeText.mono.copy(
            fontSize = if (large) 22.sp else 12.sp,
            fontWeight = if (large) FontWeight.SemiBold else FontWeight.Normal,
        ),
        color = UpeColors.Amber,
        modifier = modifier
            .background(UpeColors.Amber.copy(alpha = 0.10f), RectangleShape)
            .border(1.dp, UpeColors.Amber.copy(alpha = 0.35f), RectangleShape)
            .padding(horizontal = if (large) 12.dp else 8.dp, vertical = if (large) 8.dp else 4.dp),
    )
}

@Composable
fun CriticalBadge(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.phase_critical).uppercase(),
        style = UpeText.label,
        color = UpeColors.Danger,
        modifier = modifier
            .background(UpeColors.Danger.copy(alpha = 0.12f), RectangleShape)
            .border(1.dp, UpeColors.Danger.copy(alpha = 0.6f), RectangleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun WarningBlock(warning: WarningItem, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(UpeColors.Warn.copy(alpha = 0.08f), RectangleShape)
            .border(1.dp, UpeColors.Warn.copy(alpha = 0.55f), RectangleShape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = UpeColors.Warn, modifier = Modifier.size(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(warning.title, color = UpeColors.Text, fontWeight = FontWeight.SemiBold)
            Text(warning.text, color = UpeColors.TextDim, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Текстовая схема: строки без переноса, с горизонтальной прокруткой, чтобы псевдографика не разъезжалась. */
@Composable
fun SchemeBlock(scheme: Scheme, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(UpeColors.Bg, RectangleShape)
            .border(1.dp, UpeColors.Cool.copy(alpha = 0.4f), RectangleShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionLabel(scheme.title, color = UpeColors.Cool)
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            scheme.lines.forEach { line ->
                Text(line, style = UpeText.mono.copy(fontSize = 13.sp, lineHeight = 18.sp), color = UpeColors.Text, softWrap = false, maxLines = 1)
            }
        }
    }
}

@Composable
fun PhaseNumber(phase: Phase, done: Boolean, fontSize: TextUnit = 34.sp) {
    Text(
        text = phase.id.toString().padStart(2, '0'),
        style = UpeText.mono.copy(fontSize = fontSize, fontWeight = FontWeight.Bold),
        color = if (done) UpeColors.Safe else UpeColors.Cool,
    )
}

@Composable
fun OutlineAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = UpeColors.TextDim) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, UpeColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
    ) { Text(text) }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = UpeColors.Surface,
        title = { Text(title) },
        text = { Text(text, color = UpeColors.TextDim) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) UpeColors.Danger else UpeColors.Cool)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpeTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = UpeColors.Bg,
            titleContentColor = UpeColors.Text,
            navigationIconContentColor = UpeColors.Text,
            actionIconContentColor = UpeColors.TextDim,
        ),
    )
}

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize().background(UpeColors.Bg), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = UpeColors.Cool)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.loading), color = UpeColors.TextDim)
        }
    }
}
