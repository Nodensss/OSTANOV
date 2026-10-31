package ru.upestop.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.upestop.app.R
import ru.upestop.app.data.model.Phase
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText

/** Сворачиваемая карточка этапа (как components/ui/PhaseCard.tsx веб-версии). */
@Composable
fun PhaseCard(
    phase: Phase,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    done: Boolean = false,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(UpeColors.Surface, RectangleShape)
            .border(1.dp, if (phase.critical) UpeColors.Danger.copy(alpha = 0.5f) else UpeColors.Border, RectangleShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onToggle)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.widthIn(min = 48.dp)) { PhaseNumber(phase, done) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SectionLabel(stringResource(R.string.phase_label, phase.id, phase.section))
                Text(
                    text = phase.title,
                    color = if (done) UpeColors.Safe else UpeColors.Text,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                )
                phase.condition?.let {
                    Text(it, style = UpeText.mono.copy(fontSize = 12.sp), color = UpeColors.Amber)
                }
                if (phase.critical) CriticalBadge()
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                trailing?.let { Text(it, style = UpeText.mono.copy(fontSize = 12.sp), color = UpeColors.TextMute) }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(if (expanded) R.string.cd_collapse else R.string.cd_expand),
                    tint = UpeColors.TextMute,
                )
            }
        }
        if (expanded) {
            HorizontalDivider(color = UpeColors.Border)
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}
