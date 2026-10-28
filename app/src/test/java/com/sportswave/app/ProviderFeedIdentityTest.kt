package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test

class ProviderFeedIdentityTest {
    private val url="https://example.com/eight.m3u8"
    private val fingerprint=java.security.MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
    private val paths=mapOf(fingerprint to "beIN SPORTS 8")
    private val source=Source("x1","beIN XTRA 1","Multi-sport","",integration="yacine",serviceUrl=YacineApi.DEFAULT_ENDPOINT)
    @Test fun rejectsKnownWrongChannelDespiteFreshSessionQuery() {
        assertEquals("beIN SPORTS 8",ProviderFeedIdentity.mismatch(source,"$url?expires=123&signature=fresh",paths))
        assertNull(ProviderFeedIdentity.mismatch(source.copy(name="beIN SPORTS 8 HD"),url,paths))
    }
    @Test fun doesNotApplyProviderEvidenceToOtherServicesOrUnknownPaths() {
        assertNull(ProviderFeedIdentity.mismatch(source.copy(serviceUrl="https://example.com/api/"),url,paths))
        assertNull(ProviderFeedIdentity.mismatch(source,"https://example.com/repaired.m3u8",paths))
    }
}
