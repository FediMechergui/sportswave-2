package com.sportswave.app

import org.json.JSONArray
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/** Public, unlocked broadcaster streams only. No account or entitlement is impersonated. */
object AlkassApi {
    fun resolve(source: Source): Source? {
        if(!source.provider.equals("Alkass",true)) return null
        val number=Regex("(?i)alkass\\s*(\\d+)").find(source.name)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val c=URL("https://shoofapi.alkass.net/Shoof/liveV3.php").openConnection() as HttpsURLConnection
        c.connectTimeout=10000; c.readTimeout=12000; c.useCaches=false
        try {
            check(c.responseCode in 200..299)
            val bytes=c.inputStream.use { input ->
                val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
                while(true) { val n=input.read(buffer); if(n<0) break; out.write(buffer,0,n); check(out.size()<=2_000_000) }; out.toByteArray()
            }
            return parse(JSONArray(bytes.toString(Charsets.UTF_8)),number,source)
        } finally { c.disconnect() }
    }
    fun parse(data: JSONArray, number: Int, source: Source): Source? {
        val channel=(0 until data.length()).map { data.getJSONObject(it) }.firstOrNull { it.optInt("id")==number } ?: return null
        if(channel.optBoolean("is_locked") || channel.optInt("is_premium")>0) return null
        val url=channel.optString("body")
        if(!Catalog.validUrl(url)) return null
        return source.copy(url=url,mime="application/x-mpegURL",headers=emptyMap(),userAgent="SportsWave/1.0",allowHttp=false,licenseUrl="",licenseHeaders=emptyMap(),sessionSnapshot=false)
    }
}
