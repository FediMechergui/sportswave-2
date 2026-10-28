package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test

class WebProvidersTest {
    private val entry="https://todaystreams.cc/play/game-123"
    @Test fun rejectsUntrustedSchemesHostsCredentialsAndPorts() {
        for(url in listOf("http://todaystreams.cc/play/a","https://todaystreams.cc.evil.com/play/a","https://user:pass@todaystreams.cc/play/a","file:///player","javascript:alert(1)","https://todaystreams.cc:8443/play/a")) assertNull(url,WebProviders.playerRule(url))
        assertNull(WebProviders.playerRule("https://yasirtv.com/playerv5.php.other"))
    }
    @Test fun separatesTopLevelNavigationFramesAndMediaResources() {
        assertTrue(WebProviders.allowsPlayerNavigation(entry,"https://thedaddy.pk/stream/stream-99.php",false))
        assertFalse(WebProviders.allowsPlayerNavigation(entry,"https://thedaddy.pk/stream/stream-99.php",true))
        assertTrue(WebProviders.allowsPlayerResource(entry,"https://edge.wedd48c.top/segment.ts"))
        assertFalse(WebProviders.allowsPlayerNavigation(entry,"https://edge.wedd48c.top/segment.ts",false))
        assertFalse(WebProviders.allowsPlayerResource(entry,"https://ad-network.example/track.js"))
        assertTrue(WebProviders.allowsPlayerResource("https://streamec.st/embed/example","https://edgestream3.pro/media.m3u8"))
        assertFalse(WebProviders.allowsPlayerNavigation("https://streamec.st/embed/example","https://edgestream3.pro/media.m3u8",true))
    }
    @Test fun eventDiscoveryCannotSwitchToAnotherFixture() {
        val page=WatchPage("viprow","https://viprow.im/sports/football/","123")
        assertTrue(WebProviders.validPage(page))
        assertTrue(WebProviders.metadataUrl(page,"https://viprow.im/sports/football/team-a-team-b/123/"))
        assertFalse(WebProviders.metadataUrl(page,"https://viprow.im/sports/football/other-match/124/"))
        assertFalse(WebProviders.metadataUrl(page,"https://evil.example/sports/football/a/123/"))
        assertFalse(WebProviders.validPage(page.copy(eventId="../123")))
    }
    @Test fun candidatesCannotAutomaticallyReplaceGuideBroadcasts() {
        val candidate=WebProviders.channels.first { it.id=="daddy-bein9-candidate" }
        val event=Catalog.events.first().copy(broadcasts=listOf(Broadcast(provider="beIN",channelName="beIN SPORTS 9")))
        assertTrue(BroadcastRouting.matches(event,listOf(candidate)).isEmpty())
    }
}
