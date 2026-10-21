package com.sportswave.app

import java.text.Normalizer
import java.util.Locale

object BroadcastRouting {
    private fun normalized(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT).replace(Regex("\\s*\\(?\\b(?:2160p|1080p|720p|480p|360p|244p|240p|uhd|fhd|hd|sd)\\)?$"), "")
        .replace(Regex("[^\\p{L}\\p{N}]"), "")

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
