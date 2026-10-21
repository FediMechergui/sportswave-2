package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test

class PublicChannelStreamsTest {
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
}
