package com.sportswave.app

/** Verified public delivery alternatives. Exact catalog IDs prevent cross-channel substitution. */
object PublicChannelStreams {
    private val streams = mapOf(
        "8093614023458564860" to "https://d6izdil55uftn.cloudfront.net/out/v1/0a06d1d6377c47edbd48721ed724bd08/index.m3u8",
        "8093614023559369576" to "https://rt-fra.rttv.com/dvr/rtfrance/playlist.m3u8",
        "8093614023560863457" to "https://live.france24.com/hls/live/2037179-b/F24_FR_HI_HLS/master_5000.m3u8",
        "8093614023261401122" to "https://tv3v.live-kd.com/live/tv3v/livestream/index.m3u8",
        "8093614023262236961" to "https://tv7.live-kd.com/live/tv7/livestream/playlist.m3u8",
        "8093614025026209384" to "https://rnttwmjcin.turknet.ercdn.net/lcpmvefbyo/ahaber/ahaber.m3u8",
        "8093614025016419951" to "https://canli.tgrthaber.com/tgrt.m3u8",
        "8093614024965950670" to "https://cdn.live.easybroadcast.io/abr_corp/83_medi1tv-maghreb_jnbspmg/playlist.m3u8",
        "8093614024140651373" to "https://cdn.live.easybroadcast.io/abr_corp/83_medi1tv-arabic_g90v4ec/playlist.m3u8",
        "8093614025301693421" to "https://cdn.live.easybroadcast.io/abr_corp/83_medi1tv-afrique_tm7tu45/playlist.m3u8",
        "8093614024227420934" to "https://sw1.tanitweb.net/TunisiaTV/_definst_/watania1/chunklist.m3u8",
        "8093614024230932058" to "https://rp.tactivemedia.com/watantv_source/live/playlist.m3u8",
        "8093614024149882669" to "https://shd-gcp-live.edgenextcdn.net/live/bitmovin-sbc/90e09c0c28db26435799b4a14892a167/index.m3u8",
        "8093614024151139926" to "https://shd-gcp-live.edgenextcdn.net/live/bitmovin-saudi-tv/2ad66056b51fd8c1b624854623112e43/index.m3u8",
        "8093614025038246133" to "https://shd-gcp-live.edgenextcdn.net/live/bitmovin-ksa-now/71ed3aa814c643306c0a8bc4fcc7d17f/index.m3u8",
        "8093614024152011503" to "https://shd-gcp-live.edgenextcdn.net/live/bitmovin-al-ekhbaria/297b3ef1cd0633ad9cfba7473a686a06/index.m3u8",
        "8093614024161880591" to "https://shd-gcp-live.edgenextcdn.net/live/bitmovin-thikrayat/8cfa8fe0c96275735745b89c683b27e3/index.m3u8",
        "8093614024937033755" to "https://shd-gcp-live.edgenextcdn.net/live/bitmovin-thaqafeyyah/28c0d2a20dbf1dc049ce15d3973f494b/index.m3u8",
        "8093614024030986244" to "https://live.kwikmotion.com/smc1live/smc1tv.smil/playlist.m3u8",
        "8093614023841282422" to "https://live.kwikmotion.com/royatvpublic/royatv.smil/playlist.m3u8",
        "8093614023894604072" to "https://live.kwikmotion.com/futurelive/ftv.smil/playlist.m3u8",
        "8093614023909332824" to "https://live.kwikmotion.com/syriatvlive/syriatv.smil/playlist_dvr.m3u8",
        "8093614023927927569" to "https://cdn.streamlane.tv/hls/ltv/master.m3u8",
        "8093614024912123203" to "https://live.bradosti.net/live/samaraweb_playlist.m3u8",
        "8093614024662537983" to "https://hlspackager.akamaized.net/live/DB/RUDAW/HLS/RUDAW.m3u8",
        "8093614024664044198" to "https://hlspackager.akamaized.net/live/DB/PAYAM_TV/HLS/PAYAM_TV.m3u8",
        "8093614024675847342" to "https://65f16f0fdfc51.streamlock.net/afarinTV/livestream/playlist.m3u8",
        "8093614025224770506" to "https://live.i-news.tv/hls/stream.m3u8",
        "8093614024671252786" to "https://live.alissahost.net/hls/test.m3u8",
        "8093614025030095024" to "https://hlspackager.akamaized.net/live/DB/KURDSAT_NEWS/HLS/KURDSAT_NEWS.m3u8",
        "8093614025031295721" to "https://hlspackager.akamaized.net/live/DB/K24/HLS/K24.m3u8",
        "8093614025031958335" to "https://app-live.org/live/3268334b/index.m3u8",
        "8093614024096366832" to "https://kwtktvata.cdn.mangomolo.com/ktva/smil:tktva.stream.smil/chunklist.m3u8",
        "8093614023887165507" to "https://partneta.cdn.mgmlcdn.com/omantv/smil:omantv.stream.smil/chunklist.m3u8",
        "8093614023954693392" to "https://cdn-globecast.akamaized.net/live/eds/sudan_tv/hls_roku/index.m3u8",
        "8093614024256066742" to "https://live-news.asharq.com/asharq.m3u8"
    )
    fun resolve(source: Source): Source? {
        if(source.integration!="yacine" || source.serviceUrl.ifBlank { YacineApi.DEFAULT_ENDPOINT }!=YacineApi.DEFAULT_ENDPOINT) return null
        val url=streams[source.channelId] ?: return null
        return source.copy(url=url,mime="application/x-mpegURL",headers=emptyMap(),userAgent="SportsWave/1.0",allowHttp=false,licenseUrl="",licenseHeaders=emptyMap(),sessionSnapshot=false)
    }
}
