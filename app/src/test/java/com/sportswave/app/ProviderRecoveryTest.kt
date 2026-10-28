package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test

class ProviderRecoveryTest {
    @Test fun failedRedirectCanRetryButCannotLoopForever() {
        assertTrue(ProviderRecovery.shouldRetry(404,false,0))
        assertTrue(ProviderRecovery.shouldRetry(404,false,1))
        assertFalse(ProviderRecovery.shouldRetry(404,false,2))
        assertTrue(ProviderRecovery.shouldRetry(null,false,0,networkUnavailable=true))
        assertFalse(ProviderRecovery.shouldRetry(null,false,2,networkUnavailable=true))
        assertTrue(ProviderRecovery.delayMs(2)>ProviderRecovery.delayMs(1))
    }
    @Test fun unrelatedFailuresDoNotTriggerSessionRequests() {
        assertFalse(ProviderRecovery.shouldRetry(null,false,0))
        assertFalse(ProviderRecovery.shouldRetry(400,false,0))
        assertTrue(ProviderRecovery.shouldRetry(null,true,0))
        assertFalse(ProviderRecovery.shouldRetry(null,true,2))
    }
    @Test fun failedPublicAlternativeCanReturnToItsProviderOnlyOnce() {
        val source=Source("f24","France 24","Other","",integration="yacine",channelId="8093614023560863457",serviceUrl=YacineApi.DEFAULT_ENDPOINT)
        val url=PublicChannelStreams.resolve(source)!!.url
        assertTrue(ProviderRecovery.shouldTryOriginalProvider(source,url,emptySet()))
        assertFalse(ProviderRecovery.shouldTryOriginalProvider(source,url,setOf(source.id)))
        assertFalse(ProviderRecovery.shouldTryOriginalProvider(source,"https://example.com/unrelated.m3u8",emptySet()))
        assertFalse(ProviderRecovery.shouldTryOriginalProvider(source.copy(serviceUrl="https://example.com/api/"),url,emptySet()))
    }
}
