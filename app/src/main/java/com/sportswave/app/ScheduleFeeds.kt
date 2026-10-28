package com.sportswave.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.net.ssl.HttpsURLConnection

data class ScheduleResult(val events: List<Event>, val errors: List<String>)

/** Calendar metadata only. Playback sessions are still resolved at the time of watching. */
object ScheduleFeeds {
    fun eventJson(e: Event): JSONObject = JSONObject().put("id",e.id).put("sport",e.sport).put("competition",e.competition).put("home",e.home).put("away",e.away).put("startUtc",e.startUtc).put("endUtc",e.endUtc)
        .put("homeCrest",e.homeCrest).put("awayCrest",e.awayCrest).put("status",e.status).put("listingSource",e.listingSource).put("description",e.description).put("sourceId",e.sourceId).put("demo",e.demo).put("score",e.score)
        .put("broadcasts",JSONArray(e.broadcasts.map { JSONObject().put("provider",it.provider).put("channelId",it.channelId).put("channelName",it.channelName).put("sourceId",it.sourceId) })).put("watchPages",WebProviders.pagesJson(e.watchPages))
    fun eventFromJson(o: JSONObject): Event {
        val refs=o.optJSONArray("broadcasts") ?: JSONArray()
        return Event(o.getString("id"),o.getString("sport"),o.optString("competition"),o.getString("home"),o.optString("away"),0,0,imageFor(o.getString("sport")),startUtc=o.getString("startUtc"),endUtc=o.optString("endUtc"),demo=o.optBoolean("demo"),sourceId=o.optString("sourceId"),score=o.optString("score"),
            homeCrest=o.optString("homeCrest"),awayCrest=o.optString("awayCrest"),status=o.optString("status"),listingSource=o.optString("listingSource"),description=o.optString("description"),
            broadcasts=(0 until refs.length()).map { val r=refs.getJSONObject(it); Broadcast(r.optString("provider"),r.optString("channelId"),r.optString("channelName"),r.optString("sourceId")) },watchPages=WebProviders.parsePages(o.optJSONArray("watchPages")))
    }
    private fun get(url: String): JSONObject {
        val c=URL(url).openConnection() as HttpsURLConnection
        c.connectTimeout=12000; c.readTimeout=18000
        c.setRequestProperty("Accept","application/json"); c.setRequestProperty("User-Agent","Mozilla/5.0 SportsWave/1.0")
        try {
            check(c.responseCode in 200..299) { "Guide unavailable (${c.responseCode})" }
            val out=java.io.ByteArrayOutputStream()
            c.inputStream.use { input -> val buffer=ByteArray(8192); while(true) { val n=input.read(buffer); if(n<0) break; out.write(buffer,0,n); check(out.size()<=8_000_000) { "Guide response too large" } } }
            return JSONObject(out.toString("UTF-8"))
        } finally { c.disconnect() }
    }
    fun load(date: LocalDate, endpoint: String): ScheduleResult {
        val pool=java.util.concurrent.Executors.newFixedThreadPool(3)
        try {
            val jobs=listOf("beIN guide" to pool.submit<List<Event>> { bein(date) },"Yacine calendar" to pool.submit<List<Event>> { YacineApi.events(endpoint) })
            val extra=pool.submit<ScheduleResult> { ExtraScheduleFeeds.load(date) }
            val errors=mutableListOf<String>(); val events=mutableListOf<Event>()
            jobs.forEach { (name,job) -> runCatching { job.get() }.onSuccess { events.addAll(it) }.onFailure { errors.add(name) } }
            runCatching { extra.get(100,java.util.concurrent.TimeUnit.SECONDS) }.onSuccess { events+=it.events;errors+=it.errors }.onFailure { errors+="Additional providers";extra.cancel(true) }
            return ScheduleResult(EventSchedule.merge(events.filter { ExtraScheduleFeeds.onDate(it,date) }),errors)
        } finally { pool.shutdownNow() }
    }
    private fun bein(date: LocalDate): List<Event> {
        val channels=get("https://www.beinsports.com/api/opta/tv-channel?region=en-mena").getJSONArray("rows")
        val ids=(0 until channels.length()).map { channels.getJSONObject(it).getString("id") }.toSet()
        require(ids.isNotEmpty() && ids.all { it.matches(Regex("[a-fA-F0-9-]{36}")) })
        val start=date.atStartOfDay(ZoneId.systemDefault()).toInstant(); val end=date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val base="https://www.beinsports.com/api/opta/tv-event?startBefore=$end&endAfter=$start&limit=1000&"+ids.joinToString("&") { "channelIds=$it" }
        val result=mutableListOf<Event>(); var offset=0
        do {
            val data=get("$base&offset=$offset"); val rows=data.getJSONArray("rows")
            result.addAll(parseBein(rows,ids)); offset+=rows.length()
            if(offset>=data.optInt("count") || rows.length()==0) break
            check(offset<4000) { "Guide exceeds supported date range" }
        } while(true)
        return result
    }
    fun parseBein(rows: JSONArray, channelIds: Set<String>): List<Event> = (0 until rows.length()).mapNotNull { i ->
        val o=rows.getJSONObject(i)
        if(o.optString("channelId") !in channelIds || !o.optBoolean("live") || o.optBoolean("replay")) return@mapNotNull null
        val start=runCatching { Instant.parse(o.getString("startDate")) }.getOrNull() ?: return@mapNotNull null
        val end=runCatching { Instant.parse(o.getString("endDate")) }.getOrNull() ?: return@mapNotNull null
        if(end<=start) return@mapNotNull null
        val channel=o.optJSONObject("channel")?.optString("name").orEmpty()
        val title=o.optString("title").trim(); if(title.isBlank() || channel.isBlank()) return@mapNotNull null
        val data=o.optJSONObject("data") ?: JSONObject()
        val pair=title.substringBefore(" - ").split(Regex("\\s+(?i:vs\\.?|v\\.)\\s+"),limit=2)
        val home=data.optString("teama").ifBlank { pair.first() }
        val away=data.optString("teamb").ifBlank { pair.getOrElse(1) { "" } }
        val category=o.optString("category"); val sport=sport(o.optJSONObject("sportType")?.optString("name").orEmpty()+" "+o.optString("genre")+" "+category+" "+title)
        Event("bein-guide-${o.getString("id")}",sport,category.ifBlank { sport },home,away,0,0,imageFor(sport),startUtc=start.toString(),endUtc=end.toString(),demo=false,
            broadcasts=listOf(Broadcast(provider="beIN",channelName=channel)),listingSource="beIN MENA guide",description=title,status="Live broadcast listing")
    }
    fun parseYacine(rows: JSONArray): List<Event> = (0 until rows.length()).mapNotNull { i ->
        val o=rows.getJSONObject(i); val start=o.optLong("start_time"); val end=o.optLong("end_time")
        if(start<=0 || end<=start) return@mapNotNull null
        val a=o.optJSONObject("team_1") ?: JSONObject(); val b=o.optJSONObject("team_2") ?: JSONObject()
        val channel=o.optString("channel").trim(); val name=a.optString("name").ifBlank { o.optString("title") }; if(name.isBlank()) return@mapNotNull null
        val competition=o.optString("champions"); val sport=if(o.optString("sport").isBlank()) "Football" else sport(o.optString("sport"))
        val provider=when { channel.contains("bein",true) -> "beIN"; channel.contains("alkass",true) || channel.contains("الكأس") -> "Alkass"; else -> "" }
        Event("yacine-event-${o.getString("id")}",sport,competition,name,b.optString("name"),0,0,imageFor(sport),startUtc=Instant.ofEpochSecond(start).toString(),endUtc=Instant.ofEpochSecond(end).toString(),demo=false,
            broadcasts=if(channel.isBlank()) emptyList() else listOf(Broadcast(provider=provider,channelName=channel)),homeCrest=a.optString("logo"),awayCrest=b.optString("logo"),listingSource="Yacine calendar")
    }
    fun imageFor(sport: String)=when(sport) { "Motorsport","Cycling" -> "racing.jpg"; "Tennis","Padel","Volleyball" -> "tennis.jpg"; "Basketball","Combat","Handball" -> "basketball.jpg"; else -> "football.jpg" }
    fun sport(value: String): String {
        val s=value.lowercase(java.util.Locale.ROOT)
        return when {
            "padel" in s -> "Padel"; listOf("tennis","atp ","wta ","wimbledon").any { it in s } -> "Tennis"
            listOf("formula","motogp","motorsport","grand prix","nascar","indycar","superbike","rally","dtm").any { it in s } -> "Motorsport"
            "basket" in s || "nba" in s -> "Basketball"; "american football" in s || "nfl" in s -> "American football"
            "baseball" in s || "mlb" in s -> "Baseball"; "ice hockey" in s || "nhl" in s -> "Ice hockey"
            "horse racing" in s || "equestrian" in s || "global champions tour" in s -> "Horse racing"
            "australian football" in s || "aussie rules" in s -> "Aussie rules"; "darts" in s -> "Darts"; "snooker" in s -> "Snooker"
            "handball" in s -> "Handball"; "volley" in s -> "Volleyball"; "rugby" in s -> "Rugby"; "cricket" in s -> "Cricket"
            listOf("boxing","ufc","mma","wrestling","judo").any { it in s } -> "Combat"
            "cycling" in s || "tour de" in s -> "Cycling"; "athletic" in s -> "Athletics"; "swim" in s -> "Swimming"; "golf" in s -> "Golf"
            listOf("football","soccer","uefa","fifa","ligue","liga","premier league","sudamericana","libertadores","afc champions league","caf champions league","caf confederation cup").any { it in s } -> "Football"
            else -> "Other"
        }
    }
}
