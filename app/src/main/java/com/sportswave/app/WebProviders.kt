package com.sportswave.app

import java.net.URI
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class WatchPage(val provider: String, val url: String, val eventId: String = "", val label: String = "")
data class WebPlayerChoice(val url: String, val provider: String, val label: String)
data class WebProviderRule(val id: String, val hosts: Set<String>, val prefixes: List<String>, val resources: Set<String>, val children: Set<String> = emptySet())

object WebProviders {
    private val common=setOf("cdn.jsdelivr.net","ssl.p.jwpcdn.com","cdn.jwplayer.com","entitlements.jwplayer.com","code.jquery.com","cdnjs.cloudflare.com")
    val rules=listOf(
        WebProviderRule("todaystreams",setOf("todaystreams.cc"),listOf("/play/"),common+"todaystreams.cc",setOf("daddy","dembed","wikisport","xstream","embedsports")),
        WebProviderRule("daddy",setOf("thedaddy.pk","dlive.sx"),listOf("/watch.php","/stream/"),common+setOf("thedaddy.pk","dlive.sx"),setOf("dembed","wikisport","xstream","embedsports")),
        WebProviderRule("dembed",setOf("dembed.st"),listOf("/premiumtv/"),common+setOf("dembed.st","edge.wedd48c.top","edge.cowedd4855ws.sbs","p16-common-sign.tiktokcdn-us.com","p19-common-sign.tiktokcdn-us.com")),
        WebProviderRule("snrt",setOf("snrt.player.easybroadcast.io"),listOf("/events/"),setOf("snrt.player.easybroadcast.io","token.easybroadcast.io","libs.easybroadcast.io","cdn.live.easybroadcast.io","cdn.jsdelivr.net","fonts.googleapis.com","fonts.gstatic.com","imasdk.googleapis.com")),
        WebProviderRule("alba",setOf("pl.matchlivehd.com"),listOf("/albaplayer/"),common+setOf("pl.matchlivehd.com","pull.gosh6.app","www.gstatic.com"),setOf("snrt")),
        WebProviderRule("streamcenter",setOf("streamec.st","embedcenter.st"),listOf("/"),common+setOf("streamec.st","embedcenter.st","edgestream3.pro")),
        WebProviderRule("wikisport",setOf("wikisport.info"),listOf("/"),common+"wikisport.info",setOf("xstream")),
        WebProviderRule("xstream",setOf("xstream.st"),listOf("/"),common+setOf("xstream.st","edge.wedd48c.top","p16-common-sign.tiktokcdn-us.com","p19-common-sign.tiktokcdn-us.com")),
        WebProviderRule("embedsports",setOf("embedsports.me"),listOf("/"),common+"embedsports.me"),
        WebProviderRule("yasirtv",setOf("yasirtv.com"),listOf("/playerv5.php"),setOf("yasirtv.com"))
    )
    fun uri(url: String): URI? = runCatching { URI(url).takeIf { it.scheme=="https" && !it.host.isNullOrBlank() && it.userInfo==null && url.length<=4096 && it.port in listOf(-1,443) } }.getOrNull()
    fun playerRule(url: String): WebProviderRule? {
        val u=uri(url) ?: return null
        return rules.firstOrNull { u.host.lowercase(Locale.ROOT) in it.hosts && it.prefixes.any { prefix -> if(prefix.endsWith(".php")) u.path==prefix else u.path.startsWith(prefix) } }
    }
    fun validPage(page: WatchPage): Boolean {
        val u=uri(page.url) ?: return false
        return when(page.provider) {
            "direct" -> playerRule(page.url)!=null
            "viprow" -> u.host=="viprow.im" && Regex("^/sports/[a-z-]+/$").matches(u.path) && page.eventId.matches(Regex("[0-9]{1,20}"))
            "livekora" -> u.host in setOf("pl.koralive1.cc","pl.matchlivehd.com") && Regex("^/[a-zA-Z0-9_-]+/$").matches(u.path)
            "yalla" -> u.host=="yallashootkoora.online" && u.path.startsWith("/ar/match/")
            "streamcenter" -> page.url=="https://streamcenter.st/" && page.eventId.matches(Regex("[a-zA-Z0-9_-]{1,200}"))
            else -> false
        }
    }
    fun viprowEvent(url: String): Boolean = uri(url)?.let { it.host=="viprow.im" && Regex("^/sports/[^/]+/[^/]+/[0-9]+/$").matches(it.rawPath) } ?: false
    fun metadataUrl(page: WatchPage,url: String): Boolean {
        val u=uri(url) ?: return false
        return when(page.provider) {
            "viprow" -> u.host=="viprow.im" && (url==page.url || (viprowEvent(url) && u.path.endsWith("/${page.eventId}/")))
            "livekora" -> u.host in setOf("pl.koralive1.cc","pl.matchlivehd.com")
            "yalla" -> u.host=="yallashootkoora.online"
            else -> false
        }
    }
    private fun family(rule: WebProviderRule): List<WebProviderRule> {
        val found=linkedMapOf(rule.id to rule)
        repeat(4) { found.values.toList().forEach { r -> r.children.forEach { id -> rules.firstOrNull { it.id==id }?.let { found[id]=it } } } }
        return found.values.toList()
    }
    fun allowsPlayerNavigation(entry: String,url: String,mainFrame: Boolean): Boolean {
        val selected=playerRule(entry) ?: return false
        val target=playerRule(url) ?: return false
        return if(mainFrame) target.id==selected.id else target in family(selected)
    }
    fun allowsPlayerResource(entry: String,url: String): Boolean {
        val u=uri(url) ?: return false
        val selected=playerRule(entry) ?: return false
        return family(selected).any { u.host.lowercase(Locale.ROOT) in it.resources }
    }
    fun pagesJson(pages: List<WatchPage>)=JSONArray(pages.map { JSONObject().put("provider",it.provider).put("url",it.url).put("eventId",it.eventId).put("label",it.label) })
    fun parsePages(rows: JSONArray?): List<WatchPage> {
        if(rows==null) return emptyList()
        require(rows.length()<=12) { "An event can have up to twelve web providers." }
        return (0 until rows.length()).map { val o=rows.getJSONObject(it); WatchPage(o.getString("provider"),o.getString("url"),o.optString("eventId"),o.optString("label")).also { p -> require(validPage(p)) { "Unsupported web provider page." } } }.distinct()
    }
    val channels=listOf(
        Source("snrt-arryadia-web","Arryadia · SNRT","Multi-sport","https://snrt.player.easybroadcast.io/events/73_arryadia_k2tgcj0",provider="SNRT",integration="web",catalogCategory="Broadcaster web players"),
        Source("daddy-bein9-candidate","beIN SPORTS 9 · Web candidate","Multi-sport","https://thedaddy.pk/watch.php?id=99",provider="DaddyLive candidate",integration="web",catalogCategory="Web candidates · Programme unverified"),
        Source("daddy-xtra-candidate","beIN XTRA · Web candidate","Multi-sport","https://thedaddy.pk/watch.php?id=100",provider="DaddyLive candidate",integration="web",catalogCategory="Web candidates · Language and number unverified")
    )
}
