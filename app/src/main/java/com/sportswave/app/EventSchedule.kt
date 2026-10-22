package com.sportswave.app

import java.time.Instant
import java.time.Duration
import java.util.Locale

enum class AiringPhase { UPCOMING, AIRING, ENDED }

object EventSchedule {
    fun phase(event: Event, now: Instant = Instant.now()): AiringPhase {
        val start=runCatching { Instant.parse(event.startUtc) }.getOrNull() ?: return AiringPhase.UPCOMING
        val end=runCatching { Instant.parse(event.endUtc) }.getOrNull() ?: start.plus(Duration.ofHours(when(event.sport) { "Cricket", "Golf", "Cycling" -> 8L; "Motorsport", "Tennis" -> 4L; else -> 3L }))
        return when { now<start -> AiringPhase.UPCOMING; now>=end || event.status.lowercase(Locale.ROOT) in setOf("full time","final","finished","cancelled","canceled","postponed") -> AiringPhase.ENDED; else -> AiringPhase.AIRING }
    }
    private fun identity(value: String)=value.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
    private fun contestKey(event: Event)=if(event.away.isBlank()) null else event.sport+"|"+listOf(identity(event.home),identity(event.away)).sorted().joinToString("|")
    /** Both participant identities and nearby start times must agree; sport alone never supplies a channel. */
    fun sameContest(a: Event,b: Event): Boolean {
        if(a.away.isBlank() || b.away.isBlank() || a.sport!=b.sport) return false
        if(contestKey(a)!=contestKey(b)) return false
        return runCatching { Duration.between(Instant.parse(a.startUtc),Instant.parse(b.startUtc)).abs().toMinutes()<=45 }.getOrDefault(false)
    }
    fun merge(events: List<Event>): List<Event> {
        val result=mutableListOf<Event>()
        val contests=mutableMapOf<String,MutableList<Int>>()
        for(event in events.distinctBy { it.id }) {
            val key=contestKey(event)
            val index=key?.let { contests[it]?.firstOrNull { i -> sameContest(result[i],event) } } ?: -1
            if(index<0) { if(key!=null) contests.getOrPut(key) { mutableListOf() }.add(result.size); result.add(event) } else {
                val old=result[index]
                result[index]=old.copy(broadcasts=(old.broadcasts+event.broadcasts).distinct(),
                    sourceId=old.sourceId.ifBlank { event.sourceId },homeCrest=old.homeCrest.ifBlank { event.homeCrest },awayCrest=old.awayCrest.ifBlank { event.awayCrest },
                    score=old.score.ifBlank { event.score },status=old.status.ifBlank { event.status },endUtc=old.endUtc.ifBlank { event.endUtc })
            }
        }
        return result.sortedBy { it.startUtc }
    }
    fun enrich(event: Event, calendar: List<Event>): Event {
        val matches=calendar.filter { sameContest(event,it) }
        return event.copy(broadcasts=(event.broadcasts+matches.flatMap { it.broadcasts }).distinct(),endUtc=event.endUtc.ifBlank { matches.firstOrNull()?.endUtc ?: "" })
    }
}
