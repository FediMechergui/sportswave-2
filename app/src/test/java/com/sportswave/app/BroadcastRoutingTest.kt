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
    @Test fun savedChannelReferencesFollowExactProviderIdMigration() {
        val oldId="8093614023570873384"
        val id=YacineChannelIds.current(oldId)
        assertNotEquals(oldId,id)
        val current=bein.copy(id="yacine-$id",channelId=id,integration="yacine")
        assertEquals(listOf(current),BroadcastRouting.matches(event(Broadcast(sourceId="yacine-$oldId")),listOf(current)))
        assertTrue(BroadcastRouting.matches(event(Broadcast(sourceId="yacine-123")),listOf(current)).isEmpty())
    }
    @Test fun numberedXtraMaxAndLanguageFeedsStaySeparate() {
        val names=listOf("beIN SPORTS 9","beIN XTRA 1","beIN SPORTS MAX 1","beIN SPORTS EN 1","beIN SPORTS FR 1","beIN SPORTS 1 AFC","beIN SPORTS 1")
        val sources=names.mapIndexed { i,name -> bein.copy(id="channel-$i",name=name,channelId="$i",aliases=emptyList()) }
        for((i,name) in names.withIndex()) {
            assertEquals(name,listOf(sources[i]),BroadcastRouting.matches(event(Broadcast(provider="beIN",channelName="$name HD")),sources))
        }
    }
    @Test fun usXtraCannotSubstituteForMenaBroadcasts() {
        val us=Catalog.channels.first { it.id=="bein-xtra-us" }
        assertTrue(BroadcastRouting.matches(event(Broadcast(provider="beIN",channelName=us.name)),listOf(us)).isEmpty())
        assertEquals(listOf(us),BroadcastRouting.matches(event(Broadcast(provider="beIN US",channelName=us.name)),listOf(us)))
    }
    @Test fun explicitLegacySourceTakesPrecedence() { assertEquals(listOf(bein,alkass),BroadcastRouting.matches(event(Broadcast(sourceId="ak1")).copy(sourceId="b1"),listOf(alkass,bein))) }
    @Test fun requiresExplicitOptInForHttpMedia() { assertFalse(Catalog.validMediaUrl("http://example.com/live.m3u8")); assertTrue(Catalog.validMediaUrl("http://example.com/live.m3u8",true)); assertFalse(Catalog.validMediaUrl("file:///test",true)) }
}
