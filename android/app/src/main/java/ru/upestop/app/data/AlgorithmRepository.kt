package ru.upestop.app.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.upestop.app.data.model.AlgorithmJson
import ru.upestop.app.data.model.StopAlgorithm
import ru.upestop.app.domain.AlgorithmValidator
import ru.upestop.app.domain.ValidationError
import java.io.IOException

sealed interface AlgorithmLoad {
    data object Loading : AlgorithmLoad
    data class Ready(val algorithm: StopAlgorithm) : AlgorithmLoad
    data class ParseError(val details: String) : AlgorithmLoad
    data class Invalid(val errors: List<ValidationError>) : AlgorithmLoad
}

/**
 * Загружает assets/stop-algorithm.json (это файл data/stop-algorithm.json из корня репозитория,
 * подключённый в build.gradle.kts) один раз, проверяет и кэширует.
 */
class AlgorithmRepository(private val assets: AssetManager) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<AlgorithmLoad>(AlgorithmLoad.Loading)

    val state: StateFlow<AlgorithmLoad> = _state.asStateFlow()
    val algorithm: Flow<StopAlgorithm> = state.filterIsInstance<AlgorithmLoad.Ready>().map { it.algorithm }

    suspend fun load(): AlgorithmLoad = mutex.withLock {
        val current = _state.value
        if (current != AlgorithmLoad.Loading) return current
        withContext(Dispatchers.IO) { read() }.also { _state.value = it }
    }

    suspend fun require(): StopAlgorithm? = (load() as? AlgorithmLoad.Ready)?.algorithm

    private fun read(): AlgorithmLoad = try {
        val text = assets.open(FILE_NAME).bufferedReader().use { it.readText() }
        val algorithm = AlgorithmJson.parse(text)
        val errors = AlgorithmValidator.validate(algorithm)
        if (errors.isEmpty()) AlgorithmLoad.Ready(algorithm) else AlgorithmLoad.Invalid(errors)
    } catch (e: IOException) {
        AlgorithmLoad.ParseError(e.message ?: e.javaClass.simpleName)
    } catch (e: IllegalArgumentException) {
        // SerializationException — наследник IllegalArgumentException.
        AlgorithmLoad.ParseError(e.message ?: e.javaClass.simpleName)
    }

    companion object {
        const val FILE_NAME = "stop-algorithm.json"
    }
}
