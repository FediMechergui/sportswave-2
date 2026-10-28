package com.sportswave.app

import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URI
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

object ExtraScheduleFeeds {
    private val cache=ConcurrentHashMap<String,Pair<Long,List<Event>>>()
    val viprowSports=linkedMapOf("football" to "Football","basketball" to "Basketball","ice-hockey" to "Ice hockey","motorsports" to "Motorsport","boxing" to "Combat","mma-events" to "Combat","american-football" to "American football","rugby" to "Rugby","baseball" to "Baseball","tennis" to "Tennis","aussie-rules" to "Aussie rules","golf" to "Golf","darts" to "Darts","snooker" to "Snooker","volleyball" to "Volleyball","handball" to "Handball","horse-racing" to "Horse racing","cycling" to "Cycling","more-sports" to "Other")

    fun get(url: String, hosts: Set<String> = setOf(URI(url).host)): String {
        var current=url
        repeat(5) {
            val uri=WebProviders.uri(current)
            require(uri!=null && uri.host in hosts) { "Unapproved provider redirect." }
            val c=URL(current).openConnection() as javax.net.ssl.HttpsURLConnection
            c.connectTimeout=10000;c.readTimeout=12000;c.instanceFollowRedirects=false
            c.setRequestProperty("User-Agent","Mozilla/5.0");c.setRequestProperty("Accept","application/json,text/html;q=0.9")
            try {
                val status=c.responseCode
                if(status in 300..399) { current=URI(current).resolve(c.getHeaderField("Location") ?: error("Missing redirect")).toString() }
                else {
                    check(status in 200..299) { "Provider HTTP $status" }
                    val out=java.io.ByteArrayOutputStream()
                    c.inputStream.use { input -> val buf=ByteArray(8192);while(true) { if(Thread.currentThread().isInterrupted) throw InterruptedException();val n=input.read(buf);if(n<0) break;out.write(buf,0,n);check(out.size()<=4_000_000) { "Provider response exceeds limit" } } }
                    return out.toString("UTF-8")
                }
            } finally { c.disconnect() }
        }
        error("Too many provider redirects")
    }
    private fun cached(key: String,ttl: Long=120_000,load:()->List<Event>):List<Event> {
        cache[key]?.takeIf { System.currentTimeMillis()-it.first<ttl }?.let { return it.second }
        return load().also { cache[key]=System.currentTimeMillis() to it }
    }
    fun load(date: LocalDate): ScheduleResult {
        val tasks=listOf<Pair<String,()->List<Event>>>(
            "VIPRow" to { cached("viprow") { viprow() } },
            "StreamCenter" to { cached("streamcenter") { parseStreamCenter(JSONArray(get("https://streamcenter.st/api/matches/all"))) } },
            "Livekora" to { cached("livekora") { parseLivekora(get("https://www.livekora.vip/",setOf("www.livekora.vip","livekora.vip"))) } },
            "Yalla" to { cached("yalla-$date") { val today=LocalDate.now();val path=when(date) { today->"/ar/";today.minusDays(1)->"/ar/yesterday-matches/";today.plusDays(1)->"/ar/tomorrow-matches/";else->return@cached emptyList<Event>() };parseYalla(get("https://yallashootkoora.online$path")) } },
            "FilGoal" to { cached("filgoal-$date") { parseFilGoal(get("https://www.filgoal.com/matches?date=$date"),date) } },
            "ESPN F1" to { cached("f1-${date.year}",3_600_000) { parseEspn(JSONObject(get("https://site.api.espn.com/apis/site/v2/sports/racing/f1/scoreboard?dates=${date.year}&limit=1000")),"Motorsport","ESPN F1") } },
            "ESPN NHL" to { cached("nhl-$date") { espnDays("hockey/nhl","Ice hockey","ESPN NHL",date) } },
            "ESPN MLB" to { cached("mlb-$date") { espnDays("baseball/mlb","Baseball","ESPN MLB",date) } },
            "Raceweek" to { cached("raceweek",3_600_000) { raceweek() } }
        )
        val pool=java.util.concurrent.Executors.newFixedThreadPool(5)
        try {
            val jobs=tasks.map { (name,task) -> name to pool.submit<List<Event>> { task() } }
            val errors=mutableListOf<String>();val events=mutableListOf<Event>()
            for((name,job) in jobs) runCatching { job.get(90,java.util.concurrent.TimeUnit.SECONDS) }.onSuccess { events+=it }.onFailure { failure ->
                errors+=name;job.cancel(true)
                val reason=failure.cause ?: failure
                android.util.Log.w("SportsWaveFeeds","$name: ${reason.javaClass.simpleName}: ${reason.message?.take(140)}")
            }
            return ScheduleResult(EventSchedule.merge(events.filter { onDate(it,date) }),errors)
        } finally { pool.shutdownNow() }
    }
    fun onDate(e: Event,date: LocalDate,zone: ZoneId=ZoneId.systemDefault()): Boolean = runCatching {
        val start=Instant.parse(e.startUtc)
        if(e.listingSource=="Raceweek" && e.endUtc.isNotBlank()) start<date.plusDays(1).atStartOfDay(zone).toInstant() && Instant.parse(e.endUtc)>date.atStartOfDay(zone).toInstant()
        else start.atZone(zone).toLocalDate()==date
    }.getOrDefault(false)
    /** Read the site's inline JSON values without executing its JavaScript. */
    fun inlineValue(script: String,key: String): String? {
        val m=Regex("\\b${Regex.escape(key)}\\s*:\\s*([\\[{])").find(script) ?: return null
        val start=m.range.last;var depth=0;var quoted=false;var escaped=false
        for(i in start until script.length) {
            val ch=script[i]
            if(quoted) { if(escaped) escaped=false else if(ch=='\\') escaped=true else if(ch=='"') quoted=false }
            else when(ch) { '"'->quoted=true;'[','{'->depth++;']','}'->{ depth--;if(depth==0) return script.substring(start,i+1) } }
        }
        return null
    }
    private fun viprow(): List<Event> {
        val pool=java.util.concurrent.Executors.newFixedThreadPool(4)
        try {
            val jobs=viprowSports.map { (slug,sport) -> pool.submit<List<Event>> { val url="https://viprow.im/sports/$slug/";parseViprow(get(url),slug,sport) } }
            val results=jobs.map { runCatching { it.get(45,java.util.concurrent.TimeUnit.SECONDS) } }
            check(results.all { it.isSuccess }) { "VIPRow sections incomplete; saved listings retained" }
            return results.flatMap { it.getOrThrow() }
        } finally { pool.shutdownNow() }
    }
    private val pair=Regex("\\s+(?i:vs\\.?|v\\.?|[-–])\\s+")
    fun parseViprow(html: String,slug: String,sport: String): List<Event> {
        val script=Jsoup.parse(html).select("script").firstOrNull { it.data().contains("window.ftbPageData") }?.data() ?: error("VIPRow data unavailable")
        val rows=JSONArray(inlineValue(script,"schedule") ?: error("Missing schedule"))
        val leagueValue=inlineValue(script,"league") ?: "{}"
        val leagues=if(leagueValue.trim()=="[]") JSONObject() else JSONObject(leagueValue)
        return (0 until minOf(rows.length(),2000)).mapNotNull { i -> runCatching {
            val o=rows.getJSONObject(i);val id=o.optString("id");require(id.matches(Regex("[0-9]{1,20}")))
            val title=o.getString("name").trim();val start=o.getLong("start");require(start>0 && title.isNotBlank())
            val sides=title.split(pair,limit=2)
            val nonTeam=sport in setOf("Motorsport","Golf","Cycling","Horse racing","Combat","Snooker","Darts","Other")
            if(sides.size!=2 && !nonTeam) return@runCatching null
            if(sport=="Motorsport" && !Regex("(?i)race|grand prix|gran premio|practice|qualif|sprint|nascar|indycar|motogp").containsMatchIn(title)) return@runCatching null
            val end=o.optLong("end").takeIf { it>start }?.let { Instant.ofEpochSecond(it).toString() }.orEmpty()
            val league=leagues.optJSONObject(o.optString("league_id"))?.optString("name").orEmpty()
            Event("viprow-$id",sport,league,if(nonTeam) title else sides[0],if(nonTeam) "" else sides[1],0,0,ScheduleFeeds.imageFor(sport),startUtc=Instant.ofEpochSecond(start).toString(),endUtc=end,demo=false,listingSource="VIPRow",status="Schedule time only",
                watchPages=listOf(WatchPage("viprow","https://viprow.im/sports/$slug/",id,"VIPRow")))
        }.getOrNull() }.distinctBy { it.id }
    }
    fun parseStreamCenter(rows: JSONArray): List<Event> = (0 until minOf(rows.length(),2000)).mapNotNull { i -> runCatching {
        val o=rows.getJSONObject(i);val id=o.getString("id");require(id.matches(Regex("[a-zA-Z0-9_-]{1,200}")))
        val sport=when(o.getString("category")) { "soccer"->"Football";"football"->"American football";"basketball"->"Basketball";"baseball"->"Baseball";else->return@runCatching null }
        val start=o.getLong("date");require(start>0)
        if((o.optJSONArray("sources")?.length() ?: 0)==0) return@runCatching null
        val teams=o.optJSONObject("teams");val home=teams?.optJSONObject("home")?.optString("name").orEmpty();val away=teams?.optJSONObject("away")?.optString("name").orEmpty()
        Event("streamcenter-$id",sport,"StreamCenter",home.ifBlank { o.getString("title") },away,0,0,ScheduleFeeds.imageFor(sport),startUtc=Instant.ofEpochMilli(start).toString(),demo=false,listingSource="StreamCenter",status="Schedule time only",
            watchPages=listOf(WatchPage("streamcenter","https://streamcenter.st/",id,"StreamCenter")))
    }.getOrNull() }
    fun streamCenterPlayers(id: String): List<WebPlayerChoice> {
        require(id.matches(Regex("[a-zA-Z0-9_-]{1,200}")))
        val rows=JSONArray(get("https://streamcenter.st/api/matches/all"))
        val match=(0 until rows.length()).map { rows.getJSONObject(it) }.firstOrNull { it.optString("id")==id } ?: error("This event is no longer listed. Refresh the calendar.")
        val refs=match.optJSONArray("sources") ?: return emptyList()
        val players=mutableListOf<WebPlayerChoice>()
        for(i in 0 until minOf(refs.length(),6)) {
            val ref=refs.getJSONObject(i);val source=ref.getString("source");val key=ref.getString("id")
            if(!source.matches(Regex("[a-zA-Z0-9_-]{1,200}")) || !key.matches(Regex("[a-zA-Z0-9_-]{1,200}"))) continue
            val streams=JSONArray(get("https://streamcenter.st/api/stream/$source/$key"))
            for(j in 0 until minOf(streams.length(),20)) {
                val s=streams.getJSONObject(j);val url=s.optString("embedUrl");val rule=WebProviders.playerRule(url) ?: continue
                players+=WebPlayerChoice(url,rule.id,"${s.optString("language").ifBlank { "Language unverified" }} · Stream ${s.optInt("streamNo",j+1)}")
            }
        }
        return players.distinctBy { it.url }
    }
    /** Resolve only the player frames actually published by a channel's watch page. */
    fun channelPlayers(url: String): List<WebPlayerChoice> {
        var current=url
        repeat(3) {
            val rule=WebProviders.playerRule(current) ?: error("Unsupported channel player")
            if(rule.id=="daddy" && URI(current).path.startsWith("/stream/")) return listOf(WebPlayerChoice(current,rule.id,rule.id))
            if(rule.id!="daddy") return listOf(WebPlayerChoice(current,rule.id,rule.id))
            val doc=Jsoup.parse(get(current,rule.hosts),current)
            val frames=doc.select("iframe[src],iframe[data-src]").mapNotNull { element ->
                val value=element.attr("src").ifBlank { element.attr("data-src") }
                val target=runCatching { URI(current).resolve(value).toString() }.getOrNull() ?: return@mapNotNull null
                val provider=WebProviders.playerRule(target) ?: return@mapNotNull null
                WebPlayerChoice(target,provider.id,provider.id)
            }.distinctBy { it.url }
            frames.firstOrNull { it.provider=="daddy" && URI(it.url).path.startsWith("/stream/") }?.let { return listOf(it) }
            frames.filter { it.provider!="daddy" }.takeIf { it.isNotEmpty() }?.let { return it }
            current=frames.firstOrNull { it.url!=current }?.url ?: error("No embeddable player published for this channel")
        }
        error("Channel player discovery exceeded redirect limit")
    }
    fun parseLivekora(html: String): List<Event> {
        val doc=Jsoup.parse(html,"https://www.livekora.vip/")
        return doc.select(".match-container").mapNotNull { card -> runCatching {
            val home=card.selectFirst(".right-team .team-name")?.text().orEmpty();val away=card.selectFirst(".left-team .team-name")?.text().orEmpty();require(home.isNotBlank() && away.isNotBlank())
            val start=Instant.parse(card.selectFirst(".date[data-start]")!!.attr("data-start"))
            val url=card.parents().firstOrNull { it.tagName()=="a" }?.absUrl("href").orEmpty();val page=WatchPage("livekora",url,label="Livekora");require(WebProviders.validPage(page))
            Event("livekora-${start.epochSecond}-${home.hashCode()}-${away.hashCode()}","Football",card.select(".match-info li").getOrNull(2)?.text().orEmpty(),home,away,0,0,"football.jpg",startUtc=start.toString(),demo=false,listingSource="Livekora",watchPages=listOf(page))
        }.getOrNull() }
    }
    fun parseYalla(html: String): List<Event> = Jsoup.parse(html).select("a.AY_Match[data-kickoff-utc][href]").mapNotNull { card -> runCatching {
        val page=WatchPage("yalla",card.attr("href"),label="Yalla");require(WebProviders.validPage(page))
        val home=card.selectFirst(".TM1 .TM_Name")?.text().orEmpty();val away=card.selectFirst(".TM2 .TM_Name")?.text().orEmpty();require(home.isNotBlank() && away.isNotBlank())
        val start=java.time.OffsetDateTime.parse(card.attr("data-kickoff-utc")).toInstant()
        Event("yalla-${card.attr("data-match-id")}","Football",card.select(".MT_Info li").getOrNull(2)?.text().orEmpty(),home,away,0,0,"football.jpg",startUtc=start.toString(),demo=false,listingSource="Yalla",watchPages=listOf(page))
    }.getOrNull() }
    fun parseFilGoal(html: String,date: LocalDate): List<Event> {
        val viewer=Jsoup.parse(html).selectFirst("#match-list-viewer") ?: error("FilGoal fixture markup unavailable")
        check(viewer.attr("data-current-date")==date.toString()) { "FilGoal returned another date" }
        return viewer.select(".mc-block").flatMap { block ->
            val league=block.selectFirst("h6 span")?.text().orEmpty()
            if(listOf("كرة السلة","كرة اليد","كرة الطائرة","تنس","كرة الصالات").any { it in league }) return@flatMap emptyList<Event>()
            block.select(".cin_cntnr").mapNotNull { card -> runCatching {
                val id=Regex("^/matches/([0-9]+)").find(card.selectFirst("a[href^=/matches/]")?.attr("href").orEmpty())?.groupValues?.get(1) ?: return@runCatching null
                val home=card.selectFirst(".f strong")?.text().orEmpty();val away=card.selectFirst(".s strong")?.text().orEmpty();require(home.isNotBlank() && away.isNotBlank())
                val raw=Regex("\\b[0-9]{2}-[0-9]{2}-[0-9]{4} - [0-9]{2}:[0-9]{2}\\b").find(card.select(".match-aux").text())?.value ?: return@runCatching null
                val start=LocalDateTime.parse(raw,DateTimeFormatter.ofPattern("dd-MM-yyyy - HH:mm")).atZone(ZoneId.of("Africa/Cairo")).toInstant()
                val label=card.selectFirst(".status")?.text().orEmpty();val status=when { "انتهت" in label->"finished";"تأجيل" in label || "مؤجل" in label->"postponed";"إلغاء" in label || "ملغ" in label->"cancelled";else->"Schedule time only" }
                Event("filgoal-$id","Football",league,home,away,0,0,"football.jpg",startUtc=start.toString(),demo=false,listingSource="FilGoal",status=status)
            }.getOrNull() }
        }
    }
    fun parseEspn(root: JSONObject,sport: String,provider: String): List<Event> {
        val rows=root.getJSONArray("events");val result=mutableListOf<Event>()
        for(i in 0 until minOf(rows.length(),2000)) {
            val event=rows.getJSONObject(i);val comps=event.optJSONArray("competitions") ?: continue
            for(j in 0 until comps.length()) runCatching {
                val c=comps.getJSONObject(j);val start=java.time.OffsetDateTime.parse(c.optString("date").ifBlank { event.getString("date") }).toInstant()
                val teams=c.optJSONArray("competitors") ?: JSONArray();val sides=(0 until teams.length()).map { teams.getJSONObject(it) }
                val home=sides.firstOrNull { it.optString("homeAway")=="home" };val away=sides.firstOrNull { it.optString("homeAway")=="away" }
                fun name(o: JSONObject?)=o?.optJSONObject("team")?.optString("displayName").orEmpty()
                val session=c.optJSONObject("type")?.optString("abbreviation").orEmpty()
                val status=c.optJSONObject("status")?.optJSONObject("type") ?: event.optJSONObject("status")?.optJSONObject("type")
                val phase=when { status?.optBoolean("completed")==true->"finished";status?.optString("state")=="in"->"live";else->"scheduled" }
                result+=Event("${provider.replace(" ","-")}-${event.getString("id")}-${c.getString("id")}",sport,provider,if(sport=="Motorsport") event.getString("name")+" · "+session else name(home).ifBlank { event.getString("name") },if(sport=="Motorsport") "" else name(away),0,0,ScheduleFeeds.imageFor(sport),startUtc=start.toString(),demo=false,listingSource=provider,status=phase,
                    score=if(sport!="Motorsport" && phase in setOf("live","finished")) "${home?.optString("score").orEmpty()} – ${away?.optString("score").orEmpty()}" else "")
            }
        }
        return result.distinctBy { it.id }
    }
    private fun espnDays(route: String,sport: String,provider: String,date: LocalDate): List<Event> = (-1L..1L).flatMap { offset ->
        val key=date.plusDays(offset).toString().replace("-","")
        parseEspn(JSONObject(get("https://site.api.espn.com/apis/site/v2/sports/$route/scoreboard?dates=$key&limit=1000")),sport,provider)
    }.distinctBy { it.id }
    private fun raceweek(): List<Event> {
        val base="https://raceweek.io/";val doc=Jsoup.parse(get(base),base)
        val urls=doc.select("a[href]").map { it.absUrl("href") }.filter { WebProviders.uri(it)?.host=="raceweek.io" && URI(it).path in setOf("/f1/","/motogp/","/wec/","/imsa/","/indycar/","/nascar/") }.distinct()
        check(urls.isNotEmpty()) { "Raceweek series unavailable" }
        return urls.flatMap { url -> parseRaceweek(get(url)) }.distinctBy { it.id }
    }
    fun parseRaceweek(html: String): List<Event> = Jsoup.parse(html).select("script[type=application/ld+json]").flatMap { script ->
        val graph=runCatching { JSONObject(script.data()).optJSONArray("@graph") }.getOrNull() ?: return@flatMap emptyList<Event>()
        (0 until graph.length()).mapNotNull { i -> runCatching {
            val o=graph.getJSONObject(i);if(o.optString("@type")!="SportsEvent") return@runCatching null
            val url=o.getString("url");require(WebProviders.uri(url)?.host=="raceweek.io")
            val start=java.time.OffsetDateTime.parse(o.getString("startDate")).toInstant();val end=java.time.OffsetDateTime.parse(o.getString("endDate")).toInstant();require(end>start)
            Event("raceweek-${url.hashCode()}","Motorsport","Meeting calendar",o.getString("name"),"",0,0,"racing.jpg",startUtc=start.toString(),endUtc=end.toString(),demo=false,listingSource="Raceweek",status="Meeting window")
        }.getOrNull() }
    }
}
