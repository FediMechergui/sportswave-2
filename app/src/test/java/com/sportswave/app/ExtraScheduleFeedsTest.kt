package com.sportswave.app

import java.time.LocalDate
import java.time.ZoneOffset
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ExtraScheduleFeedsTest {
    @Test fun expandedSportsAreNotMisclassifiedAsFootballOrOther() {
        assertEquals("Baseball",ScheduleFeeds.sport("MLB World Series"))
        assertEquals("Ice hockey",ScheduleFeeds.sport("Ice hockey NHL"))
        assertEquals("Horse racing",ScheduleFeeds.sport("Longines Global Champions Tour"))
        assertEquals("Aussie rules",ScheduleFeeds.sport("Australian Football"))
        assertEquals("Snooker",ScheduleFeeds.sport("Snooker championship"))
    }
    @Test fun emptyViprowSectionsUseAnArrayForTheirLeagueMap() {
        assertTrue(ExtraScheduleFeeds.parseViprow("<script>window.ftbPageData={schedule:[],league:[]}</script>","boxing","Combat").isEmpty())
    }
    @Test fun inlineDataHandlesEscapedQuotesAndBracketCharactersWithoutRunningCode() {
        val value="""[{"id":1,"name":"A [cup] vs B \"City\""}]"""
        val script="window.ftbPageData={schedule: $value,league:{}};evil()"
        assertEquals(value,ExtraScheduleFeeds.inlineValue(script,"schedule"))
        assertNull(ExtraScheduleFeeds.inlineValue(script,"unknown"))
    }
    @Test fun viprowKeepsActualEventIdAndDoesNotInventAnEventUrl() {
        val html="""<script>window.ftbPageData={schedule:[{"id":123,"name":"A vs B","start":1793230200,"end":1793241000,"league_id":7}],league:{"7":{"name":"League"}}}</script>"""
        val e=ExtraScheduleFeeds.parseViprow(html,"football","Football").single()
        assertEquals("A",e.home);assertEquals("B",e.away)
        assertEquals(WatchPage("viprow","https://viprow.im/sports/football/","123","VIPRow"),e.watchPages.single())
        assertEquals("League",e.competition)
    }
    @Test fun excludesChannelPostsFromRaceFixtures() {
        val html="""<script>window.ftbPageData={schedule:[{"id":1,"name":"Sky Sports F1 UK","start":1793230200},{"id":2,"name":"Mexico Grand Prix Practice 1","start":1793230200}],league:{}}</script>"""
        assertEquals("viprow-2",ExtraScheduleFeeds.parseViprow(html,"motorsports","Motorsport").single().id)
    }
    @Test fun streamcenterSoccerAndAmericanFootballAreDistinct() {
        fun row(id: String,category: String)=JSONObject().put("id",id).put("title","A vs B").put("category",category).put("date",1793230200000L).put("sources",JSONArray().put(JSONObject().put("source","iframe").put("id",id)))
        val events=ExtraScheduleFeeds.parseStreamCenter(JSONArray().put(row("soccer-a","soccer")).put(row("nfl-a","football")))
        assertEquals(listOf("Football","American football"),events.map { it.sport })
        assertTrue(events.all { it.watchPages.single().provider=="streamcenter" })
    }
    @Test fun webPagesSurvivePersistenceAndExactContestMerging() {
        val page=WatchPage("viprow","https://viprow.im/sports/football/","123","VIPRow")
        val e=Catalog.events.first().copy(id="web",home="A",away="B",startUtc="2026-10-29T18:00:00Z",demo=false,watchPages=listOf(page))
        assertEquals(e.watchPages,ScheduleFeeds.eventFromJson(ScheduleFeeds.eventJson(e)).watchPages)
        val native=e.copy(id="guide",watchPages=emptyList(),broadcasts=listOf(Broadcast(provider="beIN",channelName="beIN SPORTS 1")))
        val merged=EventSchedule.merge(listOf(native,e)).single()
        assertEquals(1,merged.broadcasts.size);assertEquals(listOf(page),merged.watchPages)
        assertTrue(EventSchedule.enrich(native,listOf(e.copy(away="C"))).watchPages.isEmpty())
    }
    @Test fun raceMeetingSpansDaysWithoutBeingAWatchableSession() {
        val e=Catalog.events.first().copy(listingSource="Raceweek",startUtc="2026-10-29T00:00:00Z",endUtc="2026-11-02T00:00:00Z",watchPages=emptyList())
        assertTrue(ExtraScheduleFeeds.onDate(e,LocalDate.of(2026,10,31),ZoneOffset.UTC))
        assertFalse(ExtraScheduleFeeds.onDate(e,LocalDate.of(2026,11,2),ZoneOffset.UTC))
    }
    @Test fun filgoalRejectsAnotherDateRatherThanAttachingItsMatches() {
        assertTrue(runCatching { ExtraScheduleFeeds.parseFilGoal("<div id='match-list-viewer' data-current-date='2026-10-27'></div>",LocalDate.of(2026,10,28)) }.isFailure)
    }
}
