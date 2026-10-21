package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CatalogTest {
    @Test fun acceptsOnlyHttpsWithoutEmbeddedCredentials() {
        assertTrue(Catalog.validUrl("https://example.com/live.m3u8?session=123"))
        listOf("http://example.com", "file:///etc/passwd", "https://user:pass@example.com", "garbage").forEach { assertFalse(Catalog.validUrl(it)) }
    }
    @Test fun combinesSportQueryAndDay() {
        assertEquals(1, Catalog.filtered("Football", "madrid", 0).size)
        assertTrue(Catalog.filtered("Tennis", "madrid", 0).isEmpty())
        assertTrue(Catalog.filtered("All sports", "", 1).all { it.day == 1L })
    }
    @Test fun computesCalendarOffsetsAcrossMonthBoundaries() {
        val event = Catalog.events.first { it.day == 1L }
        assertEquals(LocalDate.of(2026, 11, 1), event.kickoff(LocalDate.of(2026, 10, 31)).toLocalDate())
    }
}
