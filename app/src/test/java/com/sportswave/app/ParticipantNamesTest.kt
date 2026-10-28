package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test

class ParticipantNamesTest {
    @Test fun flagsNeverBecomeBrokenCrestCharacters() {
        assertEquals("AF",ParticipantNames.initials("🇲🇦 AS FAR"))
        assertEquals("BM",ParticipantNames.initials("🇩🇪Bayern München"))
        assertEquals("",ParticipantNames.initials("😀"))
    }
    @Test fun keepsArabicAndSupplementaryLettersIntact() {
        assertEquals("بس",ParticipantNames.initials("باريس سان جيرمان"))
        val text=ParticipantNames.initials("𝔄lpha Beta")
        assertEquals(2,text.codePointCount(0,text.length))
        assertTrue(Character.isSurrogatePair(text[0],text[1]))
    }
}
