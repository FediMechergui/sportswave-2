package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test

class BroadcastRoutingTest {
    private val bein=Source("b1","beIN SPORTS 1 (720P)","Football","https://example.com/1",provider="beIN",channelId="101",aliases=listOf("beIN SPORTS 1"))
    private val bein11=bein.copy(id="b11",name="beIN SPORTS 11",channelId="111",aliases=emptyList())
    private val alkass=bein.copy(id="ak1",name="Alkass One",provider="Alkass",channelId="101",aliases=listOf("ALKASS 1"))
    private fun event(vararg refs:Broadcast)=Catalog.events.first().copy(sourceId="",broadcasts=refs.toList())
    @Test fun scopesChannelIdsByProvider() {
        assertEquals(listOf(alkass),BroadcastRouting.matches(event(Broadcast(provider="Alkass",channelId="101")),listOf(bein,alkass)))
    }
    @Test fun preservesChannelNumbersAndMatchesAliasesExactly() {
        assertEquals(listOf(bein),BroadcastRouting.matches(event(Broadcast(channelName="BEIN SPORTS 1 HD")),listOf(bein11,bein)))
        assertTrue(BroadcastRouting.matches(event(Broadcast(channelName="beIN SPORTS")),listOf(bein)).isEmpty())
    }
    @Test fun supportsMultipleProvidersInCalendarOrderAndDeduplicates() {
        val e=event(Broadcast(sourceId="ak1"),Broadcast(provider="beIN",channelId="101"),Broadcast(channelName="ALKASS 1"))
        assertEquals(listOf(alkass,bein),BroadcastRouting.matches(e,listOf(bein,bein11,alkass)))
    }
    @Test fun neverGuessesABroadcastFromSport() { assertTrue(BroadcastRouting.matches(event(),listOf(bein)).isEmpty()) }
    @Test fun usXtraCannotSubstituteForMenaBroadcasts() {
        val us=Catalog.channels.first { it.id=="bein-xtra-us" }
        assertTrue(BroadcastRouting.matches(event(Broadcast(provider="beIN",channelName=us.name)),listOf(us)).isEmpty())
        assertEquals(listOf(us),BroadcastRouting.matches(event(Broadcast(provider="beIN US",channelName=us.name)),listOf(us)))
    }
    @Test fun explicitLegacySourceTakesPrecedence() { assertEquals(listOf(bein,alkass),BroadcastRouting.matches(event(Broadcast(sourceId="ak1")).copy(sourceId="b1"),listOf(alkass,bein))) }
    @Test fun requiresExplicitOptInForHttpMedia() { assertFalse(Catalog.validMediaUrl("http://example.com/live.m3u8")); assertTrue(Catalog.validMediaUrl("http://example.com/live.m3u8",true)); assertFalse(Catalog.validMediaUrl("file:///test",true)) }
}
