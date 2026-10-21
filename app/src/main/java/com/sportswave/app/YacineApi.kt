package com.sportswave.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.time.Instant
import android.util.Base64

data class ChannelPackage(val id: String, val name: String, val children: Int)
data class CatalogSync(val packages: List<ChannelPackage>, val failed: List<String>)

/** Provider metadata and fresh playback descriptors; no native APK code is bundled. */
object YacineApi {
    const val DEFAULT_ENDPOINT = "http://def67.ycnapi.com/api/"
    private const val RESPONSE_PREFIX = "c!xZj+N9&G@Ev@vw"

    fun validEndpoint(value: String): Boolean = runCatching {
        val uri=URI(value)
        uri.scheme in listOf("https","http") && !uri.host.isNullOrBlank() && uri.userInfo==null && uri.query==null && uri.fragment==null && uri.path.endsWith("/api/")
    }.getOrDefault(false)

    fun decode(body: ByteArray, timestamp: String): JSONObject {
        require(timestamp.matches(Regex("[0-9]{1,16}"))) { "Invalid provider response timestamp." }
        val key=(RESPONSE_PREFIX+timestamp).toByteArray(Charsets.UTF_8)
        val decoded=Base64.decode(body,Base64.DEFAULT)
        for(i in decoded.indices) decoded[i]=(decoded[i].toInt() xor key[i%key.size].toInt()).toByte()
        return runCatching { JSONObject(decoded.toString(Charsets.UTF_8)) }.getOrElse { throw IllegalStateException("The provider response format has changed.") }
    }

    private fun request(endpoint: String, route: String): JSONObject {
        require(validEndpoint(endpoint)) { "Invalid Yacine service address." }
        val connection=URL(endpoint+route).openConnection() as HttpURLConnection
        connection.connectTimeout=12000; connection.readTimeout=18000
        connection.setRequestProperty("Accept","application/json")
        connection.setRequestProperty("User-Agent","okhttp/4.9.0")
        connection.useCaches=false
        try {
            check(connection.responseCode in 200..299) { "Yacine service unavailable (${connection.responseCode})." }
            val bytes=connection.inputStream.use { input ->
                val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
                while(true) { val n=input.read(buffer); if(n<0) break; out.write(buffer,0,n); check(out.size()<=4_000_000) { "Provider response too large." } }
                out.toByteArray()
            }
            return decode(bytes,connection.getHeaderField("t") ?: Instant.now().epochSecond.toString())
        } finally { connection.disconnect() }
    }

    fun packages(endpoint: String, parent: String = ""): List<ChannelPackage> {
        require(parent.isBlank() || parent.matches(Regex("[0-9]+")))
        val data=request(endpoint,if(parent.isBlank()) "categories" else "categories/$parent").getJSONArray("data")
        return (0 until data.length()).map { val o=data.getJSONObject(it); ChannelPackage(o.getString("id"),o.getString("name"),o.optInt("child_count")) }
    }

    fun channels(endpoint: String, category: ChannelPackage): List<Source> {
        require(category.id.matches(Regex("[0-9]+")))
        return parseChannels(request(endpoint,"categories/${category.id}/channels").getJSONArray("data"),endpoint,category.name,category.id)
    }

    fun parseChannels(data: JSONArray, endpoint: String, packageName: String, categoryId: String = ""): List<Source> = (0 until data.length()).map { i ->
        val channel=data.getJSONObject(i); val id=channel.getString("id"); val name=channel.getString("name")
        require(id.matches(Regex("[0-9]+")))
        val provider=when { name.contains("bein",true) -> "beIN"; name.contains("alkass",true) -> "Alkass"; else -> packageName }
        val source=Source("yacine-$id",name,"Other","",provider=provider,channelId=id,aliases=listOf(name),integration="yacine",serviceUrl=endpoint,logoUrl=channel.optString("logo"),catalogCategory=packageName,catalogCategoryId=categoryId)
        source.copy(sport=if(ChannelCatalog.isSports(source)) "Multi-sport" else "Other")
    }

    fun syncCatalog(endpoint: String, onPackage: (ChannelPackage,List<Source>) -> Unit): CatalogSync {
        val pending=java.util.ArrayDeque(packages(endpoint).sortedBy { when { it.name.contains("720P",true) -> 0; it.name.contains("ALKASS",true) -> 1; it.name.contains("SPORT",true) -> 2; else -> 3 } })
        val visited=mutableSetOf<String>(); val leaves=mutableListOf<ChannelPackage>(); val failed=mutableListOf<String>()
        while(pending.isNotEmpty()) {
            val category=pending.removeFirst()
            if(!visited.add(category.id)) continue
            check(visited.size<=150) { "The provider catalog exceeds the supported package limit." }
            try {
                if(category.children>0) packages(endpoint,category.id).forEach { pending.addLast(it.copy(name=category.name+" / "+it.name)) }
                else { val sources=channels(endpoint,category); onPackage(category,sources); leaves.add(category) }
            } catch(e: Exception) { if(e is java.util.concurrent.CancellationException) throw e; failed.add(category.name) }
        }
        return CatalogSync(leaves,failed)
    }

    fun resolve(source: Source): Source {
        require(source.channelId.matches(Regex("[0-9]+"))) { "Invalid Yacine channel ID." }
        runCatching { AlkassApi.resolve(source) }.getOrNull()?.let { return it }
        val data=request(source.serviceUrl.ifBlank { DEFAULT_ENDPOINT },"channel/${source.channelId}").getJSONArray("data")
        return parsePlayback(data,source)
    }

    fun parsePlayback(data: JSONArray, source: Source): Source {
        // Modes 1 and 3 are native direct playback in the inspected player.
        val streams=(0 until data.length()).map { data.getJSONObject(it) }
        val stream=streams.firstOrNull { it.optInt("url_type",1) in listOf(1,3) && it.isNull("drm") && Catalog.validMediaUrl(it.optString("url"),true) }
            ?: throw IllegalStateException(if(streams.isEmpty()) "This channel has no streams available." else "This channel needs a playback mode that is not supported yet.")
        val url=stream.getString("url")
        val headerObject=stream.optJSONObject("headers") ?: JSONObject()
        val headers=headerObject.keys().asSequence().associateWith { headerObject.getString(it) }.toMutableMap()
        stream.optString("referer").takeIf { it.isNotBlank() && it!="null" }?.let { headers["Referer"]=it }
        val ua=stream.optString("user_agent").ifBlank { headers.entries.firstOrNull { it.key.equals("User-Agent",true) }?.value ?: "SportsWave/1.0" }
        return source.copy(url=url,mime=if(URI(url).path.endsWith(".m3u8",true)) "application/x-mpegURL" else "",headers=headers,userAgent=ua,allowHttp=URI(url).scheme=="http",sessionSnapshot=false)
    }
}
