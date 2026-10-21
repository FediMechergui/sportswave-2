package com.sportswave.app

import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object PlaybackResolver {
    fun resolve(source: Source): Source {
        if(source.integration=="yacine") return YacineApi.resolve(source)
        if (source.resolverUrl.isBlank()) return source
        require(Catalog.validUrl(source.resolverUrl)) { "Invalid playback resolver." }
        val connection = URL(source.resolverUrl).openConnection() as HttpsURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 15000
        connection.setRequestProperty("Accept", "application/json")
        source.resolverHeaders.forEach { (k,v) -> connection.setRequestProperty(k,v) }
        try {
            require(connection.responseCode in 200..299) { "The provider could not authorize playback (${connection.responseCode})." }
            val text = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val size = input.read(buffer)
                    if (size < 0) break
                    output.write(buffer, 0, size)
                    require(output.size() <= 1_048_576) { "The provider response is too large." }
                }
                output.toString("UTF-8")
            }
            val json = JSONObject(text)
            val url = json.getString("url")
            require(Catalog.validMediaUrl(url,source.allowHttp)) { "The provider returned an invalid playback URL." }
            fun headers(key: String): Map<String,String> { val o = json.optJSONObject(key) ?: return emptyMap(); return o.keys().asSequence().associateWith { o.getString(it) } }
            val license = json.optString("licenseUrl")
            require(license.isBlank() || Catalog.validUrl(license)) { "Invalid license server." }
            return source.copy(url = url, mime = json.optString("mime", source.mime), headers = headers("headers"), userAgent = json.optString("userAgent", source.userAgent), licenseUrl = license, licenseHeaders = headers("licenseHeaders"))
        } finally { connection.disconnect() }
    }
}
