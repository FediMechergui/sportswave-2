package com.sportswave.app

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class EventScheduleTest {
    private val event=Catalog.events.first().copy(home="Team A",away="Team B",demo=false,startUtc="2026-10-22T18:00:00Z",endUtc="2026-10-22T20:00:00Z")
    @Test fun watchWindowIncludesStartAndExcludesEnd() {
        assertEquals(AiringPhase.UPCOMING,EventSchedule.phase(event,Instant.parse("2026-10-22T17:59:59Z")))
        assertEquals(AiringPhase.AIRING,EventSchedule.phase(event,Instant.parse(event.startUtc)))
        assertEquals(AiringPhase.ENDED,EventSchedule.phase(event,Instant.parse(event.endUtc)))
        assertEquals(AiringPhase.ENDED,EventSchedule.phase(event.copy(status="Postponed"),Instant.parse("2026-10-22T19:00:00Z")))
    }
    @Test fun matchingNeedsBothTeamsAndNearbyTime() {
        val linked=event.copy(id="guide",broadcasts=listOf(Broadcast(provider="beIN",channelName="beIN SPORTS 1")))
        assertEquals(linked.broadcasts,EventSchedule.enrich(event,listOf(linked)).broadcasts)
        assertTrue(EventSchedule.enrich(event,listOf(linked.copy(away="Team C"))).broadcasts.isEmpty())
        assertTrue(EventSchedule.enrich(event,listOf(linked.copy(startUtc="2026-10-23T18:00:00Z"))).broadcasts.isEmpty())
    }
    @Test fun reversedParticipantsCannotInheritScoresOrBroadcasts() {
        val reverse=event.copy(id="reverse",home=event.away,away=event.home,score="2-1",broadcasts=listOf(Broadcast(provider="beIN",channelName="beIN SPORTS 9")))
        assertFalse(EventSchedule.sameContest(event,reverse))
        assertTrue(EventSchedule.enrich(event,listOf(reverse)).broadcasts.isEmpty())
        assertEquals(2,EventSchedule.merge(listOf(event,reverse)).size)
    }
    @Test fun asianAndAfricanFootballListingsKeepTheirSport() {
        assertEquals("Football",ScheduleFeeds.sport("Sports General AFC Champions League Elite"))
        assertEquals("Football",ScheduleFeeds.sport("CAF Confederation Cup Draw"))
        assertEquals("Basketball",ScheduleFeeds.sport("Basketball Champions League"))
    }
    @Test fun mergingKeepsEveryAssignedProviderAndCrests() {
        val a=event.copy(broadcasts=listOf(Broadcast(provider="beIN",channelName="beIN SPORTS 1")))
        val b=event.copy(id="second",broadcasts=listOf(Broadcast(provider="Alkass",channelName="Alkass 1")),homeCrest="https://example.com/team.png")
        val merged=EventSchedule.merge(listOf(a,b)).single()
        assertEquals(2,merged.broadcasts.size); assertEquals(b.homeCrest,merged.homeCrest)
    }
    @Test fun raceSessionsUseTheirActualBroadcastWindow() {
        val race=event.copy(sport="Motorsport",home="Grand Prix practice",away="",endUtc="2026-10-22T19:00:00Z")
        assertEquals(AiringPhase.AIRING,EventSchedule.phase(race,Instant.parse("2026-10-22T18:30:00Z")))
        assertEquals(AiringPhase.ENDED,EventSchedule.phase(race,Instant.parse("2026-10-22T19:01:00Z")))
        assertFalse(EventSchedule.sameContest(race,race.copy(id="another")))
    }
    @Test(timeout=5000) fun denseCalendarMergesWithoutComparingEveryPair() {
        val fixtures=(0 until 1000).map { event.copy(id="match-$it",home="Home $it",away="Away $it",broadcasts=listOf(Broadcast(provider="beIN",channelName="beIN SPORTS 1"))) }
        val otherProvider=fixtures.map { it.copy(id="other-${it.id}",broadcasts=listOf(Broadcast(provider="Alkass",channelName="Alkass 1"))) }
        val merged=EventSchedule.merge(fixtures+otherProvider)
        assertEquals(1000,merged.size)
        assertTrue(merged.all { it.broadcasts.size==2 })
    }
    @Test fun guideAliasesPreserveMenaChannelIdentity() {
        val source=Source("x2","beIN XTRA 2","Multi-sport","https://example.com/live",provider="beIN")
        assertEquals(listOf(source),BroadcastRouting.matches(event.copy(broadcasts=listOf(Broadcast(provider="beIN",channelName="beIN SPORTS XTRA 2"))),listOf(source)))
        assertTrue(BroadcastRouting.matches(event.copy(broadcasts=listOf(Broadcast(provider="beIN",channelName="beIN SPORTS XTRA 3"))),listOf(source)).isEmpty())
    }
    @Test fun arabicAliasesKeepChannelNumbersAndXtraSeparate() {
        val sources=listOf(Source("x2","beIN XTRA 2","Multi-sport","https://example.com/live",provider="beIN"),Source("s2","beIN SPORTS 2","Multi-sport","https://example.com/two",provider="beIN"))
        val match=event.copy(broadcasts=listOf(Broadcast(provider="beIN",channelName="بي ان سبورت اكسترا ٢")))
        assertEquals(listOf(sources.first()),BroadcastRouting.matches(match,sources))
        assertTrue(BroadcastRouting.matches(match.copy(broadcasts=listOf(Broadcast(provider="beIN",channelName="بي ان سبورت ماكس ٢"))),sources).isEmpty())
    }
    @Test fun footballDateUsesLocalKickoffDay() {
        val a=FootballMatch("a","A","B","","","2026-10-22T23:30:00Z","0-0","Scheduled")
        val b=a.copy(id="b",start="2026-10-22T18:00:00Z")
        assertEquals(listOf(a),SportsFeeds.onDate(listOf(a,b),java.time.LocalDate.of(2026,10,23),java.time.ZoneId.of("Africa/Lagos")))
        assertEquals(listOf(a,b),SportsFeeds.onDate(listOf(a,b),java.time.LocalDate.of(2026,10,22),java.time.ZoneOffset.UTC))
    }
}
