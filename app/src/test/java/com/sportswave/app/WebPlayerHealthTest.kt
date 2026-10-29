package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test

class WebPlayerHealthTest {
    @Test fun onlyAcceptsBoundedStructuredErrors() {
        assertEquals("232403",WebPlayerHealth.code("SportsWaveJW:232403"))
        assertEquals("library",WebPlayerHealth.code("SportsWaveJW:library"))
        for(value in listOf("SportsWaveJW:232403 https://example.com","other:232403","SportsWaveJW:1234567","SportsWaveJW:alert(1)")) assertNull(WebPlayerHealth.code(value))
    }
    @Test fun nextServerIsPublishedUntriedAndBounded() {
        val choices=(1..4).map { WebPlayerChoice("https://todaystreams.cc/play/$it","todaystreams","Server $it") }
        assertEquals(1,WebPlayerHealth.next(choices,setOf(choices[0].url)))
        assertNull(WebPlayerHealth.next(choices,choices.take(3).map { it.url }.toSet()))
        assertNull(WebPlayerHealth.next(choices.take(1),setOf(choices[0].url)))
    }
    @Test fun distinguishesProviderDenialFromLibraryAndLicenceFailures() {
        assertTrue(WebPlayerHealth.message("232403").contains("denied access"))
        assertTrue(WebPlayerHealth.message("100013").contains("licence"))
        assertTrue(WebPlayerHealth.message("library").contains("library"))
    }
}
