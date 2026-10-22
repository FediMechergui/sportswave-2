package com.sportswave.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.Instant
import javax.net.ssl.HttpsURLConnection

data class FootballMatch(val id: String, val home: String, val away: String, val homeCrest: String, val awayCrest: String, val start: String, val score: String, val status: String, val sport: String = "Football", val competition: String = "Bundesliga", val scoreDetail: String = "")
data class RacePosition(val position: Int, val driver: String, val team: String, val gap: String, val laps: Int)
data class RaceResult(val title: String, val date: String, val rows: List<RacePosition>)

object SportsFeeds {
    val leagues = linkedMapOf("Champions League" to "soccer/uefa.champions", "All football" to "soccer/all", "Premier League" to "soccer/eng.1", "LaLiga" to "soccer/esp.1", "Ligue 1" to "soccer/fra.1", "Serie A" to "soccer/ita.1", "Bundesliga" to "openliga", "NBA" to "basketball/nba", "NFL" to "football/nfl", "ATP tennis" to "tennis/atp", "WTA tennis" to "tennis/wta")
    private fun read(url: String): String {
        val c = URL(url).openConnection() as HttpsURLConnection
        c.connectTimeout = 12000; c.readTimeout = 15000
        c.setRequestProperty("Accept", "application/json")
        try {
            check(c.responseCode in 200..299) { "Data provider unavailable (${c.responseCode})." }
            val bytes = c.inputStream.use { input ->
                val out = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                while(true) { val count = input.read(buffer); if(count<0) break; out.write(buffer,0,count); check(out.size()<=4_000_000) { "Data response too large." } }
                out.toByteArray()
            }
            check(bytes.size <= 4_000_000) { "Data response too large." }
            return bytes.toString(Charsets.UTF_8)
        } finally { c.disconnect() }
    }
    private fun get(url: String) = JSONArray(read(url))
    fun scoreboard(league: String, date: java.time.LocalDate = java.time.LocalDate.now()): List<FootballMatch> {
        val route = requireNotNull(leagues[league])
        if(route == "openliga") return football()
        val dates=date.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
        val data=JSONObject(read("https://site.api.espn.com/apis/site/v2/sports/$route/scoreboard?limit=1000&dates=$dates"))
        if(route.startsWith("tennis/")) return parseTennis(data,league)
        return parseScoreboard(data,league,if(league=="NBA") "Basketball" else if(league=="NFL") "American football" else "Football")
    }
    fun parseTennis(data: JSONObject, league: String): List<FootballMatch> {
        val matches=mutableListOf<FootballMatch>()
        fun visit(value: Any?) {
            if(value is JSONObject) {
                val competitors=value.optJSONArray("competitors")
                val status=value.optJSONObject("status")
                if(competitors!=null && competitors.length()==2 && status!=null) {
                    val players=competitors.objects()
                    val home=players.firstOrNull { it.optString("homeAway")=="home" } ?: players[0]
                    val away=players.firstOrNull { it.optString("homeAway")=="away" } ?: players[1]
                    fun name(p:JSONObject)=p.optJSONObject("athlete")?.optString("displayName") ?: p.optJSONObject("roster")?.optString("displayName") ?: "Unknown player"
                    fun flag(p:JSONObject)=p.optJSONObject("athlete")?.optJSONObject("flag")?.optString("href") ?: ""
                    val h=home.optJSONArray("linescores") ?: JSONArray(); val a=away.optJSONArray("linescores") ?: JSONArray()
                    val count=minOf(h.length(),a.length())
                    val sets=(0 until count).map { "${h.getJSONObject(it).optInt("value")}–${a.getJSONObject(it).optInt("value")}" }
                    val type=status.getJSONObject("type"); val state=type.getString("state")
                    val detail=type.optString("detail").ifBlank { type.optString("description") }
                    val venue=value.optJSONObject("venue")?.optString("fullName") ?: league
                    matches.add(FootballMatch("espn-tennis-${value.getString("id")}",name(home),name(away),flag(home),flag(away),java.time.OffsetDateTime.parse(value.getString("date")).toInstant().toString(),
                        if(state=="pre" || sets.isEmpty()) "—" else sets.last(),if(state=="in") "Live · $detail" else detail,"Tennis",venue,if(sets.isEmpty()) "" else "Sets: ${sets.joinToString("  ")}"))
                } else value.keys().asSequence().forEach { visit(value.opt(it)) }
            } else if(value is JSONArray) for(i in 0 until value.length()) visit(value.opt(i))
        }
        visit(data)
        return matches.distinctBy { it.id }.sortedWith(compareBy<FootballMatch> { if(it.status.startsWith("Live")) 0 else if(it.score=="—") 1 else 2 }.thenBy { it.start })
    }
    fun parseScoreboard(data: JSONObject, league: String, sport: String): List<FootballMatch> {
        return (data.optJSONArray("events") ?: JSONArray()).objects().flatMap { e ->
            (e.optJSONArray("competitions") ?: JSONArray()).objects().mapNotNull { c ->
                val teams=(c.optJSONArray("competitors") ?: JSONArray()).objects()
                val home=teams.firstOrNull { it.optString("homeAway")=="home" } ?: return@mapNotNull null
                val away=teams.firstOrNull { it.optString("homeAway")=="away" } ?: return@mapNotNull null
                val h=home.optJSONObject("team") ?: return@mapNotNull null
                val a=away.optJSONObject("team") ?: return@mapNotNull null
                val status=c.optJSONObject("status") ?: e.getJSONObject("status")
                val state=status.getJSONObject("type").getString("state")
                val description=status.getJSONObject("type").optString("description")
                FootballMatch("espn-${c.getString("id")}",h.getString("displayName"),a.getString("displayName"),h.optString("logo"),a.optString("logo"),java.time.OffsetDateTime.parse(c.getString("date")).toInstant().toString(),
                    if(state=="pre") "—" else "${home.optString("score","—")} – ${away.optString("score","—")}",
                    if(state=="in") "Live · ${status.optString("displayClock")} · $description" else description,sport,league)
            }
        }
    }
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
    fun football(): List<FootballMatch> = parseFootball(get("https://api.openligadb.de/getmatchdata/bl1"))
    fun parseFootball(data: JSONArray, now: Instant = Instant.now()): List<FootballMatch> = data.objects().map { o ->
        val home = o.getJSONObject("team1"); val away = o.getJSONObject("team2")
        val result = o.getJSONArray("matchResults").objects().lastOrNull { it.optInt("resultTypeID") == 2 }
            ?: o.getJSONArray("matchResults").objects().lastOrNull()
        val finished = o.optBoolean("matchIsFinished")
        val started = Instant.parse(o.getString("matchDateTimeUTC")).isBefore(now)
        FootballMatch(o.getInt("matchID").toString(),home.getString("teamName"),away.getString("teamName"),home.optString("teamIconUrl"),away.optString("teamIconUrl"),o.getString("matchDateTimeUTC"),
            if(result == null) "—" else "${result.getInt("pointsTeam1")} – ${result.getInt("pointsTeam2")}",
            if(finished) "Full time" else if(started && result != null) "In progress · provider score" else if(started) "Awaiting provider update" else "Scheduled")
    }
    fun race(): RaceResult {
        val session = get("https://api.openf1.org/v1/sessions?session_key=latest").getJSONObject(0)
        val key = session.getInt("session_key")
        return parseRace(session,get("https://api.openf1.org/v1/drivers?session_key=$key"),get("https://api.openf1.org/v1/session_result?session_key=$key"))
    }
    fun parseRace(session: JSONObject, driverData: JSONArray, resultData: JSONArray): RaceResult {
        val drivers = driverData.objects().associateBy { it.getInt("driver_number") }
        val result = resultData.objects()
        val rows = result.map { r ->
            val driver = drivers[r.getInt("driver_number")]
            val gap = when { r.optBoolean("dsq") -> "DSQ"; r.optBoolean("dns") -> "DNS"; r.optBoolean("dnf") -> "DNF"; r.optInt("position") == 1 -> "Leader"; else -> "+${r.opt("gap_to_leader") ?: "—"}" }
            RacePosition(r.optInt("position",99),driver?.optString("full_name") ?: "#${r.getInt("driver_number")}",driver?.optString("team_name") ?: "",gap,r.optInt("number_of_laps"))
        }.sortedBy { it.position }
        return RaceResult("${session.getString("country_name")} · ${session.getString("session_name")}",session.getString("date_start"),rows)
    }
}
