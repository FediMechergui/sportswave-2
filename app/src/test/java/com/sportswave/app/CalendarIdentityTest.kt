package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test

class CalendarIdentityTest {
    @Test fun countryDecorationsMergeButConflictingCountriesAndYouthDoNot() {
        val a=event("a","Adelaide United FC (AUS)","Lion City Sailors FC (SGP)","beIN")
        val b=event("b","Adelaide United","Lion City Sailors","VIPRow")
        assertEquals(1,EventSchedule.merge(listOf(a,b)).size)
        assertEquals(2,EventSchedule.merge(listOf(a,b.copy(home="Adelaide United FC (NZL)"))).size)
        assertEquals(2,EventSchedule.merge(listOf(a,b.copy(home="Adelaide United U21"))).size)
    }
    @Test fun cityAbbreviationsRequireTheCorrectLeagueContext() {
        TeamIdentity.contextual("Basketball","NBA","Scope City North",listOf("Scope City"))
        TeamIdentity.contextual("Basketball","WNBA","Scope City South",listOf("Scope City"))
        assertEquals(TeamIdentity.key("Basketball","Scope City North"),TeamIdentity.key("Basketball","Scope City","Basketball - NBA"))
        assertEquals(TeamIdentity.key("Basketball","Scope City South"),TeamIdentity.key("Basketball","Scope City","WNBA"))
        assertNotEquals(TeamIdentity.key("Basketball","Scope City North"),TeamIdentity.key("Basketball","Scope City"))
    }
    private fun event(id:String,home:String,away:String,provider:String)=Catalog.events.first().copy(id=id,home=home,away=away,demo=false,startUtc="2026-10-29T18:00:00Z",listingSource=provider)
    @Test fun multilingualDuplicateKeepsBothProvidersAndImages() {
        TeamIdentity.reviewed("Football",listOf("Barcelona","برشلونة"))
        TeamIdentity.reviewed("Football",listOf("Real Madrid","ريال مدريد"))
        val a=event("guide","FC Barcelona","Real Madrid","beIN MENA guide").copy(broadcasts=listOf(Broadcast(provider="beIN",channelName="beIN SPORTS 1")))
        val page=WatchPage("yalla","https://yallashootkoora.online/ar/match/a-1/",label="Yalla")
        val b=event("arabic","برشلونة","ريال مدريد","Yalla").copy(watchPages=listOf(page),homeCrest="https://example.com/barca.png")
        val merged=EventSchedule.merge(listOf(a,b)).single()
        assertEquals(a.broadcasts,merged.broadcasts);assertEquals(listOf(page),merged.watchPages);assertEquals(b.homeCrest,merged.homeCrest)
    }
    @Test fun reverseProviderOrderReorientsScoresAndCrests() {
        val a=event("a","Example United","Example City","VIPRow")
        val b=event("b","Example City","Example United","ESPN Football").copy(score="2-1",status="finished",homeCrest="https://example.com/city.png",awayCrest="https://example.com/united.png")
        val merged=EventSchedule.merge(listOf(a,b)).single()
        assertEquals("1 – 2",merged.score);assertEquals(b.awayCrest,merged.homeCrest);assertEquals("finished",merged.status)
    }
    @Test fun squadsRematchesAndDifferentSportsStaySeparate() {
        val a=event("a","Calendar United","Calendar City","First")
        for(b in listOf(a.copy(id="b",competition="Women's league",listingSource="Second"),a.copy(id="b",home="Calendar United U21",listingSource="Second"),a.copy(id="b",startUtc="2026-10-30T18:00:00Z"),a.copy(id="b",sport="Basketball"))) assertEquals(2,EventSchedule.merge(listOf(a,b)).size)
    }
    @Test fun duplicateIdsDoNotDiscardWatchOptions() {
        val a=event("same","Calendar A","Calendar B","First")
        val b=a.copy(broadcasts=listOf(Broadcast(provider="Other",channelName="Other channel")))
        assertEquals(b.broadcasts,EventSchedule.merge(listOf(a,b)).single().broadcasts)
    }
    @Test fun raceSessionDuplicatesMergeButPracticeNumbersStaySeparate() {
        val a=event("a","Mexico Grand Prix Practice 1","","VIPRow").copy(sport="Motorsport")
        val b=a.copy(id="b",listingSource="Second calendar")
        assertEquals(1,EventSchedule.merge(listOf(a,b)).size)
        assertEquals(2,EventSchedule.merge(listOf(a,b.copy(home="Mexico Grand Prix Practice 2"))).size)
    }
    @Test fun ambiguousShortAliasesNeverPickATeam() {
        TeamIdentity.register("Basketball","Identity City North",listOf("Identity City"))
        TeamIdentity.register("Basketball","Identity City South",listOf("Identity City"))
        assertNotEquals(TeamIdentity.key("Basketball","Identity City North"),TeamIdentity.key("Basketball","Identity City"))
        assertNotEquals(TeamIdentity.key("Football","Manchester City"),TeamIdentity.key("Football","Manchester United"))
    }
}
