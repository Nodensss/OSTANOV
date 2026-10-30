package ru.upestop.app.domain

import java.util.Locale
import ru.upestop.app.data.model.Phase

object PositionSearch {
    private val latin = "abcehkmoptxy"
    private val cyrillic = "авсенкмортху"
    fun normalize(value: String): String = value.lowercase(Locale.ROOT).mapNotNull { char ->
        if (char.isWhitespace() || char in "-–—") null
        else latin.indexOf(char).let { if (it < 0) char else cyrillic[it] }
    }.joinToString("")
    fun matches(value: String, query: String) = normalize(query).let { it.isNotEmpty() && normalize(value).contains(it) }
    fun filter(phases: List<Phase>, query: String): List<Phase> = if (normalize(query).isEmpty()) phases else phases.filter { phase ->
        phase.positions.any { matches(it, query) } || phase.checks.any { matches(it.text, query) || matches(it.param.orEmpty(), query) }
    }
}
