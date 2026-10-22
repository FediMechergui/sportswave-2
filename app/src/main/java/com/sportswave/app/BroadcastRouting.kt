package com.sportswave.app

import java.text.Normalizer
import java.util.Locale

object BroadcastRouting {
    private val names=java.util.concurrent.ConcurrentHashMap<String,String>()
    private val quality=Regex("\\s*\\(?\\b(?:2160p|1080p|720p|480p|360p|244p|240p|uhd|fhd|hd|sd)\\)?$")
    private val punctuation=Regex("[^\\p{L}\\p{N}]")
    private fun normalized(value: String): String {
        names[value]?.let { return it }
        val result=Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).replace("bein sports xtra","bein xtra").replace("bein sports en ","bein english ").replace("bein sports fr ","bein france ")
        .replace("bein sports news","bein news").replace(quality, "")
        .replace(punctuation, "")
        if(names.size>=4096) names.clear()
        names[value]=result
        return result
    }

    /** Exact identity/alias matching only. Never infer a channel from the sport alone. */
    fun matches(event: Event, sources: List<Source>): List<Source> {
        val refs = (if(event.sourceId.isNotBlank()) listOf(Broadcast(sourceId=event.sourceId)) else emptyList()) + event.broadcasts
        return refs.flatMap { ref ->
            sources.filter { source ->
                val providerMatches = ref.provider.isBlank() || normalized(ref.provider)==normalized(source.provider)
                providerMatches && when {
                    ref.sourceId.isNotBlank() -> ref.sourceId==source.id
                    ref.channelId.isNotBlank() -> ref.channelId==source.channelId
                    ref.channelName.isNotBlank() -> (source.aliases+source.name).any { normalized(it)==normalized(ref.channelName) }
                    else -> false
                }
            }
        }.distinctBy { it.id }
    }
}
