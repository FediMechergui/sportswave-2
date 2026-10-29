package com.sportswave.app

import java.time.Instant
import java.time.Duration
import java.util.Locale

enum class AiringPhase { UPCOMING, AIRING, ENDED }

object EventSchedule {
    private val squadPattern=Regex("(?i)women|ladies|female|feminin|سيدات|نساء|u[ -]?(?:17|18|19|20|21|23)|under[ -]?(?:17|18|19|20|21|23)")
    private val squads=java.util.concurrent.ConcurrentHashMap<String,String>()
    fun phase(event: Event, now: Instant = Instant.now()): AiringPhase {
        val start=runCatching { Instant.parse(event.startUtc) }.getOrNull() ?: return AiringPhase.UPCOMING
        val end=runCatching { Instant.parse(event.endUtc) }.getOrNull() ?: start.plus(Duration.ofHours(when(event.sport) { "Cricket", "Golf", "Cycling" -> 8L; "Motorsport", "Tennis" -> 4L; else -> 3L }))
        return when { now<start -> AiringPhase.UPCOMING; now>=end || event.status.lowercase(Locale.ROOT) in setOf("full time","final","finished","cancelled","canceled","postponed") -> AiringPhase.ENDED; else -> AiringPhase.AIRING }
    }
    private fun participant(e:Event,name:String)=TeamIdentity.key(e.sport,TeamIdentity.country(name).first,e.competition)
    private fun squad(e:Event):String {
        val text=e.competition+" "+e.home+" "+e.away
        squads[text]?.let { return it }
        val value=squadPattern.findAll(text).map { match ->
            val marker=match.value.lowercase(Locale.ROOT).replace(" ","").replace("-","")
            if(marker.startsWith("u")) marker.replace("under","u") else "women"
        }.distinct().sorted().joinToString("|")
        if(squads.size>=4096) squads.clear()
        squads[text]=value
        return value
    }
    private fun contestKey(e:Event):String? = if(e.home.isBlank()) null else e.sport+"|"+squad(e)+"|"+if(e.away.isBlank()) "single|"+participant(e,e.home) else listOf(participant(e,e.home),participant(e,e.away)).sorted().joinToString("|")
    private fun reverse(a:Event,b:Event)=participant(a,a.home)==participant(b,b.away) && participant(a,a.away)==participant(b,b.home)
    /** Both identities, squad qualifiers and nearby starts must agree. */
    fun sameContest(a:Event,b:Event):Boolean {
        if(a.sport!=b.sport || a.demo!=b.demo) return false
        if(contestKey(a)!=contestKey(b)) return false
        val other=if(reverse(a,b)) listOf(b.away,b.home) else listOf(b.home,b.away)
        for((left,right) in listOf(a.home,a.away).zip(other)) {
            val x=TeamIdentity.country(left).second;val y=TeamIdentity.country(right).second
            if(x.isNotBlank() && y.isNotBlank() && x!=y) return false
        }
        if(a.away.isBlank() || b.away.isBlank()) return a.away.isBlank() && b.away.isBlank() && a.listingSource.isNotBlank() && b.listingSource.isNotBlank() && a.listingSource!=b.listingSource && runCatching { Duration.between(Instant.parse(a.startUtc),Instant.parse(b.startUtc)).abs().toMinutes()<=10 }.getOrDefault(false)
        // Cross-provider away/home conventions differ. Within one source keep orientation strict.
        if(reverse(a,b) && (a.listingSource.isBlank() || b.listingSource.isBlank() || a.listingSource==b.listingSource)) return false
        return runCatching { Duration.between(Instant.parse(a.startUtc),Instant.parse(b.startUtc)).abs().toMinutes()<=45 }.getOrDefault(false)
    }
    private fun ordered(reference:Event,value:Event):Event {
        if(!reverse(reference,value)) return value
        val score=Regex("^\\s*([0-9]+)\\s*[-–:]\\s*([0-9]+)\\s*$").matchEntire(value.score)
        return value.copy(home=value.away,away=value.home,homeCrest=value.awayCrest,awayCrest=value.homeCrest,
            score=score?.let { "${it.groupValues[2]} – ${it.groupValues[1]}" }.orEmpty())
    }
    private fun rank(e:Event)=when { e.listingSource.startsWith("ESPN") || e.listingSource=="Public scores"->5;e.listingSource=="FilGoal"->4;e.listingSource=="Yacine calendar"->3;e.listingSource=="beIN MENA guide"->2;else->1 }
    private fun combine(old:Event,raw:Event):Event {
        val value=ordered(old,raw)
        val authoritative=if(rank(value)>rank(old)) value else old
        val secondary=if(authoritative===old) value else old
        return SportsBranding.enrich(old.copy(
            broadcasts=(old.broadcasts+value.broadcasts+(if(old.sourceId.isNotBlank() && value.sourceId.isNotBlank() && old.sourceId!=value.sourceId) listOf(Broadcast(sourceId=old.sourceId),Broadcast(sourceId=value.sourceId)) else emptyList())).distinct(),
            sourceId=old.sourceId.ifBlank { value.sourceId },
            homeCrest=old.homeCrest.ifBlank { value.homeCrest },awayCrest=old.awayCrest.ifBlank { value.awayCrest },
            score=authoritative.score.takeUnless { it.isBlank() || it=="—" } ?: secondary.score,
            status=authoritative.status.ifBlank { secondary.status },endUtc=old.endUtc.ifBlank { value.endUtc },
            watchPages=(old.watchPages+value.watchPages).distinctBy { "${it.provider}|${it.url}|${it.eventId}" }))
    }
    fun merge(events:List<Event>):List<Event> {
        val result=mutableListOf<Event>();val contests=mutableMapOf<String,MutableList<Int>>();val ids=mutableMapOf<String,Int>()
        for(raw in events) {
            val event=SportsBranding.enrich(raw)
            val key=contestKey(event)
            val exact=ids[event.id]?.takeIf { i -> result[i].home==event.home && result[i].away==event.away && result[i].startUtc==event.startUtc && result[i].sport==event.sport }
            val index=exact ?: key?.let { contests[it]?.firstOrNull { i -> sameContest(result[i],event) } } ?: -1
            if(index<0) {
                if(key!=null) contests.getOrPut(key) { mutableListOf() }.add(result.size)
                ids[event.id]=result.size;result.add(event)
            } else { result[index]=combine(result[index],event);ids[event.id]=index }
        }
        return result.sortedBy { it.startUtc }
    }
    fun enrich(event:Event,calendar:List<Event>):Event = calendar.filter { sameContest(event,it) }.fold(SportsBranding.enrich(event)) { result,value -> combine(result,value) }
}
