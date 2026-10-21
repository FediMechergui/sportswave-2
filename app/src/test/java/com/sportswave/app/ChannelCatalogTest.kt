package com.sportswave.app

import org.junit.Assert.*
import org.junit.Test

class ChannelCatalogTest {
    private fun channel(id:String,name:String,group:String,provider:String="beIN")=Source(id,name,"Other","",provider=provider,integration="yacine",catalogCategory=group)
    @Test fun sportsPrecedeEntertainmentAcrossGroups() {
        val cartoon=channel("c","beIN Boomerang","beIN ENTERTAINMENT")
        val alkass=channel("a","Alkass 1","ALKASS","Alkass")
        val french=channel("f","Eurosport 1","FRANCE CHANNELS","France")
        assertFalse(ChannelCatalog.isSports(cartoon))
        assertEquals(listOf("a","f","c"),ChannelCatalog.sorted(listOf(cartoon,french,alkass)).map { it.id })
    }
    @Test fun preferredQualityIsVisibleAndAlternatesRemainAvailable() {
        val hd=channel("hd","beIN SPORTS 1","beIN SPORTS (1080P)")
        val medium=channel("m","beIN SPORTS 1","beIN SPORTS (720P)")
        assertEquals("m",ChannelCatalog.unique(listOf(hd,medium)).single().id)
        assertEquals(listOf("m","hd"),ChannelCatalog.alternatives(hd,listOf(hd,medium)).map { it.id })
        assertEquals(hd,ChannelCatalog.nextAlternative(medium,listOf(hd,medium),setOf("m")))
        assertNull(ChannelCatalog.nextAlternative(medium,listOf(hd,medium),setOf("m","hd")))
    }
    @Test fun channelNumbersAndProvidersRemainDistinct() {
        val one=channel("1","beIN SPORTS 1","beIN SPORTS (720P)")
        val eleven=channel("11","beIN SPORTS 11","beIN SPORTS (720P)")
        val other=channel("x","beIN SPORTS 1","Other","Other provider")
        assertEquals(3,ChannelCatalog.unique(listOf(one,eleven,other)).size)
        assertEquals(listOf(one),ChannelCatalog.alternatives(one,listOf(one,eleven,other)))
    }
}
