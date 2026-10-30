package ru.upestop.app

import java.io.File
import ru.upestop.app.data.model.*
fun realAlgorithm() = AlgorithmJson.decodeFromString<StopAlgorithm>(File(System.getProperty("algorithm.json")).readText())
