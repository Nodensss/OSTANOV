package ru.upestop.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.upestop.app.data.model.*
import ru.upestop.app.domain.AlgorithmValidator

class AlgorithmRepository(private val context: Context) {
    private val mutex = Mutex()
    private var cached: StopAlgorithm? = null
    suspend fun load(): StopAlgorithm = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            val data = AlgorithmJson.decodeFromString<StopAlgorithm>(context.assets.open("stop-algorithm.json").bufferedReader().use { it.readText() })
            val errors = AlgorithmValidator.validate(data)
            require(errors.isEmpty()) { errors.joinToString("\n") }
            data.also { cached = it }
        }
    }
}
