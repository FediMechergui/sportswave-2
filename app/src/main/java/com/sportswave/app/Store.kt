package com.sportswave.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class Store(context: Context, preferenceName: String = "sportswave") {
    private val prefs = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    private fun playbackKey(source: Source): String {
        val bundledProviderChannel=PublicChannelStreams.sports.any { it.id==source.id } && YacineApi.canonicalEndpoint(source.serviceUrl) in listOf("",YacineApi.DEFAULT_ENDPOINT)
        val identity=if(bundledProviderChannel) "${source.id}|${YacineApi.DEFAULT_ENDPOINT}|bundled-sports" else "${source.id}|${source.serviceUrl}|${source.url}|${source.resolverUrl}"
        return "playback_"+java.security.MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString("") { "%02x".format(it) }
    }
    fun recordPlayback(source: Source, working: Boolean) { prefs.edit().putString(playbackKey(source),"${System.currentTimeMillis()}|$working").apply() }
    fun playbackCheck(source: Source): Boolean? = runCatching {
        val parts=prefs.getString(playbackKey(source),null)?.split('|') ?: return null
        val age=System.currentTimeMillis()-parts[0].toLong()
        if(age !in 0L..21_600_000L) null else parts[1].toBooleanStrict()
    }.getOrNull()
    fun saved(): Set<String> = prefs.getStringSet("saved", emptySet())!!.toSet()
    fun toggle(id: String) { val set = saved().toMutableSet(); if (!set.add(id)) set.remove(id); prefs.edit().putStringSet("saved", set).apply() }
    fun savedEvents(): List<Event> = runCatching {
        val a=JSONArray(prefs.getString("saved_event_details","[]")); (0 until a.length()).map { ScheduleFeeds.eventFromJson(a.getJSONObject(it)) }
    }.getOrDefault(emptyList())
    fun toggleEvent(event: Event) {
        val ids=saved().toMutableSet(); val previous=savedEvents()
        val matches=previous.filter { it.id==event.id || EventSchedule.sameContest(event,it) }
        val wasSaved=event.id in ids || matches.any { it.id in ids }
        ids.remove(event.id);matches.forEach { ids.remove(it.id) }
        val details=previous.filterNot { old -> matches.any { it.id==old.id } }.toMutableList()
        if(!wasSaved) { ids.add(event.id);details.add(event) }
        prefs.edit().putStringSet("saved",ids).putString("saved_event_details",JSONArray(details.map { ScheduleFeeds.eventJson(it) }).toString()).apply()
    }
    fun isEventSaved(event:Event):Boolean = event.id in saved() || savedEvents().any { it.id in saved() && EventSchedule.sameContest(event,it) }
    fun custom(): List<Source> = runCatching { parse(prefs.getString("sources", "[]")!!) }.getOrDefault(emptyList())
    fun all(): List<Source> = (custom() + providerSources() + Catalog.channels + Catalog.samples).distinctBy { it.id }
    fun providerEndpoint(): String = YacineApi.canonicalEndpoint(prefs.getString("yacine_endpoint",YacineApi.DEFAULT_ENDPOINT)!!)
    @Synchronized fun setProviderEndpoint(value: String) { require(YacineApi.validEndpoint(value)); val editor=prefs.edit().putString("yacine_endpoint",value).remove("yacine_packages").remove("yacine_synced_at"); prefs.all.keys.filter { it.startsWith("yacine_channels_") }.forEach { editor.remove(it) }; editor.apply() }
    fun selectedPackage(): String = prefs.getString("yacine_package","")!!
    fun catalogSyncedAt(): Long = if(prefs.getString("yacine_catalog_revision","")=="2026-10-29") prefs.getLong("yacine_synced_at",0) else 0
    fun providerPackages(): List<ChannelPackage> = runCatching { val a=JSONArray(prefs.getString("yacine_packages","[]")); (0 until a.length()).map { val o=a.getJSONObject(it); ChannelPackage(o.getString("id"),o.getString("name"),0) } }.getOrDefault(emptyList())
    fun cacheProviderPackages(packages: List<ChannelPackage>, complete: Boolean) {
        val merged=if(complete) packages else (providerPackages()+packages).distinctBy { it.id }
        val editor=prefs.edit().putString("yacine_packages",JSONArray(merged.map { JSONObject().put("id",it.id).put("name",it.name) }).toString())
        if(complete) {
            val ids=packages.map { it.id }.toSet()
            prefs.all.keys.filter { it.startsWith("yacine_channels_") && it.removePrefix("yacine_channels_") !in ids }.forEach { editor.remove(it) }
            editor.putLong("yacine_synced_at",System.currentTimeMillis())
            editor.putString("yacine_catalog_revision","2026-10-29")
        }
        editor.apply()
    }
    fun selectPackage(id: String) { prefs.edit().putString("yacine_package",id).apply() }
    fun providerChannels(id: String): List<Source> = runCatching { parse(prefs.getString("yacine_channels_$id","[]")!!) }.getOrDefault(emptyList())
    @Synchronized fun cacheProviderChannels(id: String, sources: List<Source>) { require(id.matches(Regex("[0-9]+"))); require(sources.all { it.serviceUrl==providerEndpoint() }); prefs.edit().putString("yacine_channels_$id",JSONArray(sources.map { json(it) }).toString()).apply() }
    fun providerSources(): List<Source> = prefs.all.keys.filter { it.startsWith("yacine_channels_") }.flatMap { providerChannels(it.removePrefix("yacine_channels_")) }.distinctBy { it.id }
    fun events(): List<Event> = runCatching {
        val a = JSONArray(prefs.getString("events", "[]"))
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val refs=o.optJSONArray("broadcasts") ?: JSONArray()
            val broadcasts=(0 until refs.length()).map { j -> val r=refs.getJSONObject(j); Broadcast(r.optString("provider"),r.optString("channelId"),r.optString("channelName"),r.optString("sourceId")) }
            Event(o.getString("id"), o.getString("sport"), o.optString("competition"), o.getString("home"), o.optString("away"), 0, 0, imageFor(o.getString("sport")), startUtc = o.getString("startUtc"), endUtc = o.optString("endUtc"), sourceId = o.optString("sourceId"), demo = false, broadcasts=broadcasts,watchPages=WebProviders.parsePages(o.optJSONArray("watchPages")))
        }
    }.getOrDefault(emptyList())
    private fun imageFor(sport: String) = when(sport) { "Basketball", "Handball", "Combat" -> "basketball.jpg"; "Tennis", "Padel", "Volleyball" -> "tennis.jpg"; "Motorsport", "Cycling" -> "racing.jpg"; else -> "football.jpg" }
    fun clearEvents() { prefs.edit().remove("events").apply() }
    fun schedule(date: java.time.LocalDate): List<Event> = runCatching {
        val rows=JSONArray(prefs.getString("guide_$date","[]"))
        (0 until rows.length()).map { i -> ScheduleFeeds.eventFromJson(rows.getJSONObject(i)) }
    }.getOrDefault(emptyList())
    fun scheduleUpdated(date: java.time.LocalDate): Long = prefs.getLong("guide_time_$date",0)
    fun cacheSchedule(date: java.time.LocalDate, events: List<Event>) {
        val editor=prefs.edit().putString("guide_$date",JSONArray(events.map { ScheduleFeeds.eventJson(it) }).toString()).putLong("guide_time_$date",System.currentTimeMillis())
        prefs.all.keys.filter { it.startsWith("guide_") && !it.startsWith("guide_time_") }.forEach { key ->
            val old=runCatching { java.time.LocalDate.parse(key.removePrefix("guide_")) }.getOrNull()
            if(old!=null && (old<date.minusDays(7) || old>date.plusDays(7))) editor.remove(key).remove("guide_time_$old")
        }
        editor.apply()
    }
    fun put(source: Source) { persist(custom().filter { it.id != source.id } + source) }
    fun remove(id: String) { persist(custom().filter { it.id != id }) }
    private fun persist(sources: List<Source>) { prefs.edit().putString("sources", JSONArray(sources.map { json(it) }).toString()).apply() }
    fun import(text: String): Int {
        val obj = if (text.trim().startsWith("{")) JSONObject(text) else null
        val incoming = parse(obj?.optJSONArray("sources")?.toString() ?: if (obj == null) text else "[]")
        val events = obj?.optJSONArray("events")
        if (events != null) {
            require(events.length() <= 2000) { "Import up to 2,000 events at a time." }
            for (i in 0 until events.length()) {
                val e = events.getJSONObject(i)
                listOf("id", "sport", "home", "startUtc").forEach { require(e.getString(it).isNotBlank()) { "Event ${i+1} needs $it." } }
                val start = java.time.Instant.parse(e.getString("startUtc"))
                if (e.optString("endUtc").isNotBlank()) require(java.time.Instant.parse(e.getString("endUtc")).isAfter(start)) { "Event end must follow start." }
                WebProviders.parsePages(e.optJSONArray("watchPages"))
                val refs=e.optJSONArray("broadcasts") ?: JSONArray()
                require(refs.length()<=30) { "An event can reference up to 30 broadcasts." }
                for(j in 0 until refs.length()) {
                    val ref=refs.getJSONObject(j)
                    require(listOf("sourceId","channelId","channelName").any { ref.optString(it).isNotBlank() }) { "Broadcast ${j+1} needs a source ID, channel ID or channel name." }
                }
            }
        }
        require(incoming.isNotEmpty() || (events != null && events.length() > 0)) { "The catalog is empty." }
        val merged=(custom()+incoming).associateBy { it.id }.values.toList()
        val editor=prefs.edit().putString("sources",JSONArray(merged.map { json(it) }).toString())
        if(events!=null) editor.putString("events",events.toString())
        check(editor.commit()) { "Couldn't save the imported catalog." }
        return incoming.size + (events?.length() ?: 0)
    }
    private fun json(s: Source) = JSONObject().put("id", s.id).put("name", s.name).put("sport", s.sport).put("url", s.url).put("mime", s.mime).put("userAgent", s.userAgent).put("headers", JSONObject(s.headers)).put("licenseUrl", s.licenseUrl).put("licenseHeaders", JSONObject(s.licenseHeaders)).put("resolverUrl", s.resolverUrl).put("resolverHeaders", JSONObject(s.resolverHeaders)).put("provider",s.provider).put("channelId",s.channelId).put("aliases",JSONArray(s.aliases)).put("allowHttp",s.allowHttp).put("sessionSnapshot",s.sessionSnapshot).put("integration",s.integration).put("serviceUrl",s.serviceUrl).put("logoUrl",s.logoUrl).put("catalogCategory",s.catalogCategory).put("catalogCategoryId",s.catalogCategoryId)
    private fun parse(text: String): List<Source> {
        val a = JSONArray(text)
        require(a.length() <= 500) { "Import up to 500 sources at a time." }
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            val url = o.optString("url")
            val resolver = o.optString("resolverUrl")
            val integration=o.optString("integration")
            require(integration in setOf("","yacine","web")) { "Unknown source integration." }
            require(integration!="web" || WebProviders.playerRule(url)!=null) { "Unsupported web player." }
            val nativeProvider=integration=="yacine" && o.optString("channelId").matches(Regex("[0-9]+")) && YacineApi.validEndpoint(o.optString("serviceUrl"))
            require(integration!="yacine" || nativeProvider) { "Yacine sources need a numeric channel ID and a valid service address." }
            require(nativeProvider || (url.isNotBlank() && Catalog.validMediaUrl(url,o.optBoolean("allowHttp"))) || (resolver.isNotBlank() && Catalog.validUrl(resolver))) { "Source ${i + 1} needs a valid media URL or HTTPS resolver." }
            require(resolver.isEmpty() || Catalog.validUrl(resolver)) { "Resolver URLs must use HTTPS." }
            val license = o.optString("licenseUrl")
            require(license.isEmpty() || Catalog.validUrl(license)) { "License URLs must use HTTPS." }
            fun map(key: String): Map<String, String> { val obj = o.optJSONObject(key) ?: return emptyMap(); return obj.keys().asSequence().associateWith { obj.getString(it) } }
            val aliases=o.optJSONArray("aliases") ?: JSONArray()
            Source(o.optString("id").ifBlank { "custom-${(url+resolver).hashCode()}" }, o.getString("name").also { require(it.isNotBlank()) { "Every source needs a name." } }, o.optString("sport", "Other"), url, o.optString("mime"), o.optString("userAgent", "SportsWave/1.0"), map("headers"), licenseUrl = license, licenseHeaders = map("licenseHeaders"), resolverUrl = resolver, resolverHeaders = map("resolverHeaders"), provider=o.optString("provider"),channelId=o.optString("channelId"),aliases=(0 until aliases.length()).map { aliases.getString(it) },allowHttp=o.optBoolean("allowHttp"),sessionSnapshot=o.optBoolean("sessionSnapshot"),integration=integration,serviceUrl=o.optString("serviceUrl"),logoUrl=o.optString("logoUrl"),catalogCategory=o.optString("catalogCategory"),catalogCategoryId=o.optString("catalogCategoryId"))
        }
    }
}
