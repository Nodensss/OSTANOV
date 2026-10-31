package ru.upestop.app.ui.reference

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GppMaybe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import ru.upestop.app.AppContainer
import ru.upestop.app.R
import ru.upestop.app.data.model.CheckType
import ru.upestop.app.domain.PhaseMatch
import ru.upestop.app.domain.PositionSearch
import ru.upestop.app.ui.components.LoadingScreen
import ru.upestop.app.ui.components.ParamChip
import ru.upestop.app.ui.components.PhaseCard
import ru.upestop.app.ui.components.SchemeBlock
import ru.upestop.app.ui.components.SectionLabel
import ru.upestop.app.ui.components.UpeTopBar
import ru.upestop.app.ui.components.WarningBlock
import ru.upestop.app.ui.components.appViewModel
import ru.upestop.app.ui.theme.UpeColors
import ru.upestop.app.ui.theme.UpeText

class ReferenceViewModel(container: AppContainer, private val handle: SavedStateHandle) : ViewModel() {
    val query: StateFlow<String> = handle.getStateFlow(QUERY, "")
    val results: StateFlow<List<PhaseMatch>?> = combine(container.algorithmRepository.algorithm, query) { algorithm, query ->
        PositionSearch.search(algorithm, query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setQuery(value: String) {
        handle[QUERY] = value
    }

    private companion object {
        const val QUERY = "query"
    }
}

@Composable
fun ReferenceScreen(
    onOpenRules: () -> Unit,
    viewModel: ReferenceViewModel = appViewModel { container, handle -> ReferenceViewModel(container, handle) },
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val resultsState by viewModel.results.collectAsStateWithLifecycle()
    val results = resultsState ?: return LoadingScreen()
    val searching = query.isNotBlank()
    var expanded by rememberSaveable { mutableStateOf(results.take(2).map { it.phase.id }) }
    var collapsedInSearch by rememberSaveable { mutableStateOf(emptyList<Int>()) }

    Scaffold(
        containerColor = UpeColors.Bg,
        topBar = {
            UpeTopBar(title = stringResource(R.string.reference_title)) {
                IconButton(onClick = onOpenRules) {
                    Icon(Icons.Filled.GppMaybe, contentDescription = stringResource(R.string.rules_title), tint = UpeColors.Danger)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "search") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(stringResource(R.string.reference_search_label))
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            viewModel.setQuery(it)
                            collapsedInSearch = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(2.dp),
                        placeholder = { Text(stringResource(R.string.reference_search_hint), color = UpeColors.TextMute) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = UpeColors.TextMute) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setQuery("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.reference_clear))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = UpeColors.Cool,
                            unfocusedBorderColor = UpeColors.Border,
                            focusedContainerColor = UpeColors.Surface,
                            unfocusedContainerColor = UpeColors.Surface,
                            cursorColor = UpeColors.Cool,
                        ),
                    )
                }
            }
            if (searching && results.isEmpty()) {
                item(key = "empty") {
                    Text(stringResource(R.string.reference_not_found, query.trim()), color = UpeColors.TextDim)
                }
            }
            items(results, key = { "phase:${it.phase.id}" }) { match ->
                val id = match.phase.id
                val isExpanded = if (searching) id !in collapsedInSearch else id in expanded
                ReferencePhase(
                    match = match,
                    expanded = isExpanded,
                    onToggle = {
                        if (searching) {
                            collapsedInSearch = if (isExpanded) collapsedInSearch + id else collapsedInSearch - id
                        } else {
                            expanded = if (isExpanded) expanded - id else expanded + id
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReferencePhase(match: PhaseMatch, expanded: Boolean, onToggle: () -> Unit) {
    val phase = match.phase
    PhaseCard(
        phase = phase,
        expanded = expanded,
        onToggle = onToggle,
        trailing = stringResource(R.string.reference_positions_count, phase.positions.size),
    ) {
        if (phase.positions.isNotEmpty()) {
            SectionLabel(stringResource(R.string.reference_positions))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                phase.positions.forEach { position ->
                    val hit = position in match.positions
                    Text(
                        text = position,
                        style = UpeText.mono.copy(fontSize = 13.sp, lineHeight = 18.sp),
                        color = if (hit) UpeColors.Bg else UpeColors.TextDim,
                        modifier = Modifier
                            .background(if (hit) UpeColors.Cool else UpeColors.Bg, RectangleShape)
                            .border(1.dp, if (hit) UpeColors.Cool else UpeColors.Border, RectangleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
        phase.warnings.forEach { WarningBlock(it) }
        phase.scheme?.let { SchemeBlock(it) }
        SectionLabel(stringResource(R.string.reference_operations))
        phase.checks.forEach { check ->
            val hit = check.id in match.checkIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(UpeColors.Surface2, RectangleShape)
                    .border(1.dp, if (hit) UpeColors.Cool else UpeColors.Border, RectangleShape)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(check.id, style = UpeText.mono.copy(fontSize = 12.sp), color = UpeColors.TextMute)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(check.text, color = UpeColors.Text, fontSize = 15.sp, lineHeight = 21.sp)
                    if (check.type == CheckType.PARAM && !check.param.isNullOrBlank()) ParamChip(check.param)
                }
            }
        }
    }
}
