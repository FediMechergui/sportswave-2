package com.sportswave.app

object ParticipantNames {
    /** Country flags and other supplementary characters must never be split into surrogate halves. */
    fun initials(name: String): String = name.split(Regex("\\s+")).mapNotNull { word ->
        word.codePoints().toArray().firstOrNull { Character.isLetterOrDigit(it) }?.let { String(Character.toChars(it)) }
    }.take(2).joinToString("").uppercase(java.util.Locale.ROOT)
}
