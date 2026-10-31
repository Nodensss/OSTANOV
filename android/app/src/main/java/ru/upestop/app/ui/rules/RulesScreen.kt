package ru.upestop.app.ui.rules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.upestop.app.R
import ru.upestop.app.data.model.CriticalRule
import ru.upestop.app.data.model.StopAlgorithm
import ru.upestop.app.ui.components.SectionLabel
import ru.upestop.app.ui.components.UpeTopBar
import ru.upestop.app.ui.theme.UpeColors

/** Критические правила (app/view/critical-rules/page.tsx веб-версии). */
@Composable
fun RulesScreen(algorithm: StopAlgorithm, onBack: () -> Unit) {
    Scaffold(
        containerColor = UpeColors.Bg,
        topBar = { UpeTopBar(title = stringResource(R.string.rules_title), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionLabel(stringResource(R.string.rules_overline), color = UpeColors.Danger)
                    Text(stringResource(R.string.rules_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
            }
            items(algorithm.criticalRules, key = { it.id }) { RuleCard(it) }
        }
    }
}

@Composable
private fun RuleCard(rule: CriticalRule) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(UpeColors.Danger.copy(alpha = 0.10f), RectangleShape)
            .border(1.dp, UpeColors.Danger.copy(alpha = 0.5f), RectangleShape)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.GppMaybe, contentDescription = null, tint = UpeColors.Danger, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel(rule.id, color = UpeColors.Danger)
            Text(rule.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = UpeColors.Text)
            Text(rule.rule, color = UpeColors.Text)
            if (rule.consequence.isNotBlank()) {
                Text(
                    rule.consequence,
                    color = UpeColors.TextDim,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(UpeColors.Bg.copy(alpha = 0.5f), RectangleShape)
                        .border(1.dp, UpeColors.Border, RectangleShape)
                        .padding(12.dp),
                )
            }
        }
    }
}
