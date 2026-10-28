package com.sportswave.app

import java.net.URI
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class Sport(val name: String, val symbol: String)
data class Broadcast(val provider: String = "", val channelId: String = "", val channelName: String = "", val sourceId: String = "")
data class Event(val id: String, val sport: String, val competition: String, val home: String, val away: String, val hour: Int, val minute: Int, val image: String, val day: Long = 0, val startUtc: String = "", val endUtc: String = "", val sourceId: String = "", val demo: Boolean = true, val broadcasts: List<Broadcast> = emptyList(), val homeCrest: String = "", val awayCrest: String = "", val score: String = "", val status: String = "", val listingSource: String = "", val description: String = "", val watchPages: List<WatchPage> = emptyList()) {
    fun kickoff(base: LocalDate): ZonedDateTime = if (startUtc.isNotBlank()) java.time.Instant.parse(startUtc).atZone(ZoneId.systemDefault()) else base.plusDays(day).atTime(hour, minute).atZone(ZoneId.systemDefault())
}
data class Source(val id: String, val name: String, val sport: String, val url: String, val mime: String = "", val userAgent: String = "SportsWave/1.0", val headers: Map<String, String> = emptyMap(), val demo: Boolean = false, val licenseUrl: String = "", val licenseHeaders: Map<String, String> = emptyMap(), val resolverUrl: String = "", val resolverHeaders: Map<String, String> = emptyMap(), val provider: String = "", val channelId: String = "", val aliases: List<String> = emptyList(), val allowHttp: Boolean = false, val sessionSnapshot: Boolean = false, val integration: String = "", val serviceUrl: String = "", val logoUrl: String = "", val catalogCategory: String = "", val catalogCategoryId: String = "", val playbackIndex: Int = 0, val playbackCount: Int = 1)

object Catalog {
    val sports = listOf(Sport("All sports", "◉"), Sport("Football", "⚽"), Sport("Basketball", "◈"), Sport("Tennis", "◉"), Sport("Motorsport", "⚑"), Sport("Rugby", "◆"), Sport("Handball", "◌"), Sport("Volleyball", "◌"), Sport("American football", "◆"), Sport("Padel", "◉"), Sport("Cricket", "⌁"), Sport("Combat", "✦"), Sport("Cycling", "◎"), Sport("Athletics", "↗"), Sport("Swimming", "≈"), Sport("Golf", "⚑"), Sport("Ice hockey", "H"), Sport("Baseball", "B"), Sport("Horse racing", "R"), Sport("Darts", "D"), Sport("Snooker", "S"), Sport("Aussie rules", "A"), Sport("Other", "＋"))
    val events = listOf(
        Event("football-1", "Football", "EUROPEAN NIGHTS", "Madrid", "Manchester", 20, 0, "football.jpg"),
        Event("basketball-1", "Basketball", "BASKETBALL SHOWCASE", "Los Angeles", "Boston", 22, 30, "basketball.jpg"),
        Event("tennis-1", "Tennis", "THE BASELINE SERIES", "Court 01", "Singles session", 16, 0, "tennis.jpg"),
        Event("motorsport-1", "Motorsport", "RACE WEEKEND", "Grand Prix", "Practice session", 14, 0, "racing.jpg"),
        Event("cricket-1", "Cricket", "CRICKET SHOWCASE", "India", "Australia", 13, 30, "football.jpg"),
        Event("rugby-1", "Rugby", "INTERNATIONAL RUGBY", "South Africa", "New Zealand", 18, 0, "football.jpg", 1),
        Event("combat-1", "Combat", "FIGHT NIGHT", "Main card", "Heavyweight bout", 23, 0, "basketball.jpg", 1),
        Event("cycling-1", "Cycling", "ROAD SERIES", "Stage 04", "Mountain finish", 12, 0, "racing.jpg", -1),
        Event("volleyball-1", "Volleyball", "VOLLEYBALL SHOWCASE", "Brazil", "Italy", 17, 0, "tennis.jpg", 1),
        Event("handball-1", "Handball", "HANDBALL SHOWCASE", "Paris", "Nantes", 19, 0, "basketball.jpg"),
        Event("padel-1", "Padel", "PADEL SHOWCASE", "Center court", "Doubles session", 15, 0, "tennis.jpg"),
        Event("nfl-1", "American football", "GRIDIRON SHOWCASE", "Kansas City", "Buffalo", 21, 0, "football.jpg", 1)
    )
    val channels = PublicChannelStreams.channels + WebProviders.channels + listOf(
        Source("bein-xtra-us","beIN SPORTS XTRA (US)","Multi-sport","https://bein-xtra-bein.amagi.tv/playlist.m3u8","application/x-mpegURL",provider="beIN US",channelId="bein-xtra-us",aliases=listOf("beIN SPORTS XTRA US")),
        Source("redbull-live","Red Bull TV","Multi-sport","https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master_3360.m3u8","application/x-mpegURL",provider="Red Bull TV",channelId="redbull-tv")
    )
    val samples = listOf(
        Source("demo-hls", "HLS playback test", "Other", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "application/x-mpegURL", demo = true),
        Source("demo-dash", "DASH playback test", "Other", "https://storage.googleapis.com/shaka-demo-assets/angel-one/dash.mpd", "application/dash+xml", demo = true)
    )
    fun validUrl(value: String): Boolean = runCatching { val uri = URI(value); uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null }.getOrDefault(false)
    fun validMediaUrl(value: String, allowHttp: Boolean = false): Boolean = validUrl(value) || (allowHttp && runCatching { val uri=URI(value); uri.scheme=="http" && !uri.host.isNullOrBlank() && uri.userInfo==null }.getOrDefault(false))
    fun filtered(sport: String, query: String, day: Long? = null): List<Event> = events.filter {
        (sport == "All sports" || it.sport == sport) && (day == null || it.day == day) && "${it.home} ${it.away} ${it.sport} ${it.competition}".contains(query.trim(), ignoreCase = true)
    }
}
