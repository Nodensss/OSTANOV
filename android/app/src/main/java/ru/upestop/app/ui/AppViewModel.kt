package ru.upestop.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ru.upestop.app.*
import ru.upestop.app.R
import ru.upestop.app.data.*
import ru.upestop.app.data.db.AttemptEntity
import ru.upestop.app.data.model.*
import ru.upestop.app.domain.*

data class StoredState(val checks: List<CheckState> = emptyList(), val timers: List<TimerState> = emptyList(),
    val attempts: List<AttemptEntity> = emptyList(), val settings: Settings = Settings(), val ready: Boolean = false)
class AppViewModel(application: Application) : AndroidViewModel(application) {
    val app = (application as UpeStopApp).container
    val algorithm = MutableStateFlow<StopAlgorithm?>(null)
    val dataError = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val stored = combine(app.progress.checks, app.timers.timers, app.trainer.attempts, app.settings.settings) { checks, timers, attempts, settings ->
        StoredState(checks, timers, attempts, settings, true)
    }.catch { app.errors.value = application.getString(R.string.storage_error, it.localizedMessage.orEmpty()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StoredState())
    init { load() }
    fun load() { viewModelScope.launch {
        dataError.value = null
        try { algorithm.value = app.algorithm.load(); app.launch { app.timers.restore() } }
        catch (error: Exception) { if (error is CancellationException) throw error; dataError.value = error.localizedMessage.orEmpty() }
    } }
    fun perform(block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        app.launch { try { block() } finally { busy.value = false } }
    }
    fun check(id: String, checked: Boolean, after: () -> Unit = {}) = perform {
        app.progress.set(id, checked, System.currentTimeMillis())
        withContext(Dispatchers.Main) { after() }
    }
    fun timer(id: Int, action: String) = perform { app.timers.action(id, action) }
    fun resetStop() = perform { app.timers.resetStop() }
    fun setting(setting: Setting, value: Boolean) = perform {
        if (setting == Setting.TEST_MODE) app.timers.setTestMode(value) else app.settings.set(setting, value)
    }
    fun startAttempt(set: QuestionSet, label: String, after: (String, List<String>) -> Unit) = perform {
        val questions = set.questions.shuffled()
        val attempt = app.trainer.start(set, label, System.currentTimeMillis())
        withContext(Dispatchers.Main) { after(attempt.id, questions.map { it.id }) }
    }
    fun answer(attempt: String, question: TrainerQuestion, index: Int) = perform { app.trainer.answer(attempt, question, index) }
    fun finish(attempt: String, after: () -> Unit) = perform {
        app.trainer.finish(attempt, System.currentTimeMillis())
        withContext(Dispatchers.Main) { after() }
    }
    fun clearHistory() = perform { app.trainer.clear() }
}
