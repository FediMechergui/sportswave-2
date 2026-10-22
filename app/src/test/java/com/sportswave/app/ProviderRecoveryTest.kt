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
}
