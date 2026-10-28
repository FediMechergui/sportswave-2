package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test

class ProviderIdRecoveryTest {
    private val old=Source("yacine-old","beIN SPORTS 1","Multi-sport","",integration="yacine",serviceUrl=YacineApi.LEGACY_ENDPOINT,catalogCategory="beIN SPORTS (720P)",channelId="7840681113696983416")
    @Test fun recoversOnlyAnExactChannelPackageAndEndpoint() {
        val current=old.copy(id="yacine-4",channelId="4",serviceUrl=YacineApi.DEFAULT_ENDPOINT)
        val otherChannel=current.copy(name="beIN SPORTS 7")
        val otherQuality=current.copy(catalogCategory="beIN SPORTS (1080P)")
        assertEquals(current,YacineApi.matchingRecord(old,listOf(otherQuality,otherChannel,current)))
        assertNull(YacineApi.matchingRecord(old,listOf(otherQuality,otherChannel)))
        assertNull(YacineApi.matchingRecord(old,listOf(current.copy(serviceUrl="https://example.com/api/"))))
        assertNull(YacineApi.matchingRecord(old,listOf(current,current.copy(id="duplicate"))))
    }
    @Test fun publicReplacementsSurviveProviderIdChangesWithoutNameGuessing() {
        val (id,name,category)=PublicChannelMetadata.records.first()
        val before=Source("old",name,"Other","",integration="yacine",serviceUrl=YacineApi.DEFAULT_ENDPOINT,catalogCategory=category,channelId=id)
        assertEquals(PublicChannelStreams.resolve(before)!!.url,PublicChannelStreams.resolve(before.copy(channelId="987654"))!!.url)
        assertNull(PublicChannelStreams.resolve(before.copy(channelId="987654",name=name+" Other")))
    }
}
