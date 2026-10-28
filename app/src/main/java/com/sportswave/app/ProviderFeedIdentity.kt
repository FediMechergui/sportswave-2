package com.sportswave.app

/** Known provider mislabels verified by shared media paths on 28 October 2026. */
object ProviderFeedIdentity {
    private val known = mapOf(
        "0d451eb069579a6ab1e7159a8d2e75673af6e976b90f6a28f898f630e5ce1a11" to "beIN SPORTS 7",
        "96854e188a20071306e49f5d40247489abe197fb88605325b71b06c414b277a4" to "beIN SPORTS 8",
        "837507cf5489422eb57c6c0ad46f5312aa0d3e33943de7407d43b93d83c30a67" to "beIN SPORTS 7",
        "9c2a8b55d6e89f706c5ccb38006365dfd63d78264f3db07b144516f12406c295" to "beIN SPORTS 8",
        "dc81e9db9b110b47d9b66f90f261563996dcb87c85dd3fbb643fc26598f1724b" to "beIN SPORTS 7",
        "da5639b498112b72ebe338b37949d1d70a65dc3998d55faf615cb812e014b3e5" to "beIN SPORTS 8"
    )
    private fun name(value: String) = value.lowercase(java.util.Locale.ROOT).replace(Regex("\\b(?:1080p|720p|360p|244p|hd|sd)\\b"), "").filter { it.isLetterOrDigit() }
    internal fun mismatch(source: Source, url: String, identities: Map<String,String> = known): String? {
        if(source.integration!="yacine" || YacineApi.canonicalEndpoint(source.serviceUrl.ifBlank { YacineApi.DEFAULT_ENDPOINT })!=YacineApi.DEFAULT_ENDPOINT) return null
        val uri=java.net.URI(url)
        val path="${uri.scheme}://${uri.rawAuthority}${uri.rawPath}"
        val key=java.security.MessageDigest.getInstance("SHA-256").digest(path.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        val expected=identities[key] ?: return null
        return expected.takeIf { name(source.name)!=name(it) }
    }
}
class ChannelIdentityException(expected: String, requested: String): IllegalStateException("The provider returned $expected for $requested. This mismatched feed was stopped.")
