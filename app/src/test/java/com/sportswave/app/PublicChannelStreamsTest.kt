package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test

class PublicChannelStreamsTest {
    @Test fun legacyEndpointMigratesWithoutRedirectingCustomProviders() {
        assertEquals(YacineApi.DEFAULT_ENDPOINT,YacineApi.canonicalEndpoint(YacineApi.LEGACY_ENDPOINT))
        assertEquals("https://example.com/api/",YacineApi.canonicalEndpoint("https://example.com/api/"))
        assertEquals(PublicChannelStreams.resolve(source)!!.url,PublicChannelStreams.resolve(source.copy(serviceUrl=YacineApi.LEGACY_ENDPOINT))!!.url)
        val current=YacineChannelIds.current(source.channelId)
        assertNotEquals(source.channelId,current)
        assertEquals(source.channelId,YacineChannelIds.previous(current))
        assertEquals(PublicChannelStreams.resolve(source)!!.url,PublicChannelStreams.resolve(source.copy(channelId=current))!!.url)
        assertEquals("123",YacineChannelIds.current("123"))
    }
    @Test fun bundledCatalogDoesNotDependOnRemoteDiscovery() {
        val bundled=PublicChannelStreams.channels
        assertEquals(PublicChannelStreams.replacementCount,bundled.size)
        assertEquals(bundled.size,bundled.map { it.id }.toSet().size)
        for(channel in bundled) {
            assertTrue(Catalog.channels.any { it.id==channel.id })
            assertEquals(channel.url,PublicChannelStreams.resolve(channel)!!.url)
        }
    }
    private val source=Source("yacine-8093614023560863457","France 24","Other","",integration="yacine",channelId="8093614023560863457",serviceUrl=YacineApi.DEFAULT_ENDPOINT,headers=mapOf("Authorization" to "old-provider-header"),licenseUrl="https://example.com/old-license")
    @Test fun replacementKeepsIdentityAndClearsOldProviderCredentials() {
        val result=PublicChannelStreams.resolve(source)!!
        assertEquals(source.id,result.id)
        assertEquals(source.name,result.name)
        assertTrue(result.url.startsWith("https://live.france24.com/"))
        assertTrue(result.headers.isEmpty())
        assertTrue(result.licenseUrl.isEmpty())
        assertFalse(result.allowHttp)
    }
    @Test fun otherCatalogsAndUnmappedChannelsAreUntouched() {
        assertNull(PublicChannelStreams.resolve(source.copy(serviceUrl="https://example.com/api/")))
        assertNull(PublicChannelStreams.resolve(source.copy(integration="")))
        assertNull(PublicChannelStreams.resolve(source.copy(channelId="1")))
    }
    @Test fun publicSportsAreAvailableBeforeSyncAndReplaceOnlyTheirExactProviderIds() {
        val sports=PublicChannelStreams.sports
        assertEquals(6,sports.size)
        assertEquals(sports.size,sports.map { it.id }.toSet().size)
        for(channel in sports) {
            assertEquals("yacine",channel.integration)
            assertEquals(channel.url,PublicChannelStreams.resolve(channel)!!.url)
            assertTrue(ProviderRecovery.shouldTryOriginalProvider(channel,channel.url,emptySet()))
            assertTrue(Catalog.channels.any { it.id==channel.id })
            assertTrue(Catalog.validUrl(channel.url))
            val providerSource=source.copy(channelId=channel.channelId)
            assertEquals(channel.url,PublicChannelStreams.resolve(providerSource)!!.url)
            assertNull(PublicChannelStreams.resolve(providerSource.copy(serviceUrl="https://example.com/api/")))
        }
    }
}
