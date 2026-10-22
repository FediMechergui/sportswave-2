package com.sportswave.app.tests

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.sportswave.app.Store
import org.json.JSONObject
import org.json.JSONArray

class CatalogInstrumentation : Instrumentation() {
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun auditPublicStreams() {
        val catalog=Store(targetContext).all()
        val sources=if(auditSourceIds.isEmpty()) catalog.mapNotNull { com.sportswave.app.PublicChannelStreams.resolve(it) } else catalog.filter { it.channelId in auditSourceIds }
        check(sources.size==if(auditSourceIds.isEmpty()) 36 else auditSourceIds.size) { "The complete requested catalog is required for this audit." }
        val results=JSONArray()
        for(candidate in sources) {
            val source=runCatching { com.sportswave.app.PlaybackResolver.resolve(candidate) }.getOrNull()
            if(source==null) {
                results.put(JSONObject().put("id",candidate.channelId).put("name",candidate.name).put("package",candidate.catalogCategory).put("result","resolve_error"))
                sendStatus(0,Bundle().apply { putString("stream","${candidate.name}: resolve_error\n") }); continue
            }
            val latch=java.util.concurrent.CountDownLatch(1)
            var player:androidx.media3.exoplayer.ExoPlayer?=null
            var texture:android.graphics.SurfaceTexture?=null
            var surface:android.view.Surface?=null
            var outcome="timeout"
            var liveTimeline=false
            runOnMainSync {
                texture=android.graphics.SurfaceTexture(false)
                surface=android.view.Surface(texture)
                val http=androidx.media3.datasource.DefaultHttpDataSource.Factory().setUserAgent(source.userAgent).setDefaultRequestProperties(source.headers).setConnectTimeoutMs(12000).setReadTimeoutMs(15000).setAllowCrossProtocolRedirects(false)
                player=androidx.media3.exoplayer.ExoPlayer.Builder(targetContext).setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(http)).build().apply {
                    setVideoSurface(surface)
                    addListener(object:androidx.media3.common.Player.Listener {
                        override fun onRenderedFirstFrame() { outcome="video_frame"; liveTimeline=player?.isCurrentMediaItemLive ?: false; latch.countDown() }
                        override fun onPlayerError(error:androidx.media3.common.PlaybackException) { val response=generateSequence(error as Throwable?) { it.cause }.filterIsInstance<androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException>().firstOrNull(); if(outcome!="video_frame") outcome="error_${error.errorCode}"+(response?.responseCode?.let { "_http_$it" } ?: ""); latch.countDown() }
                    })
                    setMediaItem(androidx.media3.common.MediaItem.Builder().setUri(source.url).setMimeType(source.mime).build())
                    prepare(); playWhenReady=true
                }
            }
            latch.await(25,java.util.concurrent.TimeUnit.SECONDS)
            runOnMainSync { player?.release(); surface?.release(); texture?.release() }
            results.put(JSONObject().put("id",source.channelId).put("name",source.name).put("package",source.catalogCategory).put("result",outcome).put("liveTimeline",liveTimeline))
            sendStatus(0,Bundle().apply { putString("stream","${source.name} [${source.catalogCategory}]: $outcome; liveTimeline=$liveTimeline\n") })
        }
        java.io.File(targetContext.filesDir,"public-stream-audit.json").writeText(results.toString(2))
        val passed=(0 until results.length()).count { results.getJSONObject(it).getString("result")=="video_frame" }
        finish(if(passed==sources.size) Activity.RESULT_OK else Activity.RESULT_CANCELED,Bundle().apply { putString("stream","Decoded video frames: $passed/${sources.size}\n"); putString("audit",results.toString()) })
    }
    private var sessionCatalog:String?=null
    private var auditPublic=false
    private var auditSourceIds=emptySet<String>()
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); sessionCatalog=arguments?.getString("sessionCatalog"); auditPublic=arguments?.getString("auditPublic")=="true"; auditSourceIds=arguments?.getString("auditSources")?.split(",")?.toSet() ?: emptySet(); require(auditSourceIds.size<=100 && auditSourceIds.all { it.matches(Regex("[a-zA-Z0-9_-]{1,80}")) }); start() }
    override fun onStart() {
        val result=Bundle()
        val prefs=targetContext.getSharedPreferences("sportswave-tests",0)
        try {
            if(auditPublic || auditSourceIds.isNotEmpty()) { auditPublicStreams(); return }
            if(sessionCatalog!=null) {
                val name=sessionCatalog!!
                require(name.matches(Regex("[a-zA-Z0-9._-]+")))
                val count=Store(targetContext).import(java.io.File(targetContext.filesDir,name).readText())
                result.putString("stream","Imported $count session-test items into the debug app.\n")
                finish(Activity.RESULT_OK,result)
                return
            }
            prefs.edit().clear().commit()
            val store=Store(targetContext,"sportswave-tests")
            val source=JSONObject().put("id","test").put("name","Test source").put("sport","Tennis").put("url","https://example.com/test.m3u8").put("headers",JSONObject().put("X-Playback","sample"))
            val event=JSONObject().put("id","event-test").put("sport","Tennis").put("home","Center court").put("startUtc","2026-10-21T18:00:00Z").put("endUtc","2026-10-21T20:00:00Z").put("sourceId","test")
            val catalog=JSONObject().put("sources",JSONArray().put(source)).put("events",JSONArray().put(event))
            check(store.import(catalog.toString())==2)
            check(store.custom().single().headers["X-Playback"]=="sample")
            check(store.events().single().sourceId=="test")
            event.put("sourceId","").put("broadcasts",JSONArray().put(JSONObject().put("provider","Example broadcaster").put("channelName","Test source")))
            source.put("provider","Example broadcaster").put("aliases",JSONArray().put("Channel One"))
            store.import(catalog.toString())
            check(com.sportswave.app.BroadcastRouting.matches(store.events().single(),store.custom()).single().id=="test")
            check(!store.events().single().demo)
            store.toggle("event-test")
            check(Store(targetContext,"sportswave-tests").saved().contains("event-test"))
            store.toggle("event-test"); check(store.saved().isEmpty())
            // Invalid calendar must not partially change the existing source collection.
            val invalid=JSONObject(catalog.toString())
            invalid.getJSONArray("sources").getJSONObject(0).put("name","Must not persist")
            invalid.getJSONArray("events").getJSONObject(0).put("endUtc","2026-10-20T18:00:00Z")
            check(runCatching { store.import(invalid.toString()) }.isFailure)
            check(store.custom().single().name=="Test source")
            source.put("name","Updated"); store.import(JSONArray().put(source).toString())
            check(store.custom().size==1 && store.custom().single().name=="Updated")
            val resolver=JSONObject().put("id","resolved").put("name","Resolved source").put("resolverUrl","https://example.com/session").put("resolverHeaders",JSONObject().put("X-Provider","test"))
            store.import(JSONArray().put(resolver).toString())
            check(store.custom().first { it.id=="resolved" }.resolverHeaders["X-Provider"]=="test")
            resolver.put("resolverUrl","http://example.com/session")
            check(runCatching { store.import(JSONArray().put(resolver).toString()) }.isFailure)
            store.remove("test"); check(store.custom().none { it.id=="test" })
            val match=JSONObject().put("matchID",1).put("matchDateTimeUTC","2026-10-23T18:30:00Z").put("matchIsFinished",false)
                .put("team1",JSONObject().put("teamName","Home").put("teamIconUrl","https://example.com/home.png"))
                .put("team2",JSONObject().put("teamName","Away")).put("matchResults",JSONArray())
            val now=java.time.Instant.parse("2026-10-21T12:00:00Z")
            val fixture=com.sportswave.app.SportsFeeds.parseFootball(JSONArray().put(match),now).single()
            check(fixture.score=="—" && fixture.status=="Scheduled" && fixture.homeCrest.endsWith("home.png"))
            match.put("matchDateTimeUTC","2026-10-20T18:30:00Z")
            check(com.sportswave.app.SportsFeeds.parseFootball(JSONArray().put(match),now).single().status=="Awaiting provider update")
            match.put("matchIsFinished",true).put("matchResults",JSONArray().put(JSONObject().put("resultTypeID",2).put("pointsTeam1",3).put("pointsTeam2",1)))
            val finished=com.sportswave.app.SportsFeeds.parseFootball(JSONArray().put(match),now).single()
            check(finished.score=="3 – 1" && finished.status=="Full time")
            val race=com.sportswave.app.SportsFeeds.parseRace(JSONObject().put("country_name","Test").put("session_name","Race").put("date_start","2026-10-20T12:00:00Z"),
                JSONArray().put(JSONObject().put("driver_number",4).put("full_name","Driver").put("team_name","Team")),
                JSONArray().put(JSONObject().put("position",1).put("driver_number",4).put("number_of_laps",58)))
            check(race.rows.single().driver=="Driver" && race.rows.single().gap=="Leader" && race.rows.single().laps==58)
            check(store.all().any { it.id=="redbull-live" })
            val home=JSONObject().put("homeAway","home").put("score","0").put("team",JSONObject().put("displayName","Home").put("logo","https://example.com/h.png"))
            val away=JSONObject().put("homeAway","away").put("score","0").put("team",JSONObject().put("displayName","Away"))
            val type=JSONObject().put("state","pre").put("description","Scheduled")
            val competition=JSONObject().put("id","1").put("date","2026-10-21T18:00Z").put("competitors",JSONArray().put(home).put(away)).put("status",JSONObject().put("type",type).put("displayClock","32'"))
            val board=JSONObject().put("events",JSONArray().put(JSONObject().put("competitions",JSONArray().put(competition))))
            check(com.sportswave.app.SportsFeeds.parseScoreboard(board,"Test","Football").single().score=="—")
            type.put("state","in"); home.put("score","2")
            val live=com.sportswave.app.SportsFeeds.parseScoreboard(board,"Test","Football").single()
            check(live.score=="2 – 0" && live.status.startsWith("Live") && live.status.contains("32'"))
            type.put("state","post").put("description","Full Time")
            check(!com.sportswave.app.SportsFeeds.parseScoreboard(board,"Test","Football").single().status.startsWith("Live"))
            val decoded=com.sportswave.app.YacineApi.decode("GAMcOx5KbAN9PGIsEmJMVQAFAxIcEl5RXVVBG1oOD1g6G1saPQ==".toByteArray(),"1700000000")
            check(decoded.getJSONArray("data").getJSONObject(0).getString("name")=="Test")
            check(runCatching { com.sportswave.app.YacineApi.decode("not base64".toByteArray(),"bad") }.isFailure)
            val providerSource=com.sportswave.app.YacineApi.parseChannels(JSONArray().put(JSONObject().put("id","123").put("name","beIN SPORTS 1").put("logo","https://example.com/channel.png")),com.sportswave.app.YacineApi.DEFAULT_ENDPOINT,"beIN SPORTS (720P)").single()
            check(providerSource.url.isEmpty() && providerSource.integration=="yacine" && providerSource.provider=="beIN")
            store.cacheProviderChannels("456",listOf(providerSource))
            check(store.providerChannels("456").single()==providerSource && store.all().any { it.id==providerSource.id })
            val record=JSONObject().put("url_type",3).put("url","https://example.com/live.m3u8").put("user_agent","ProviderAgent").put("headers",JSONObject().put("X-Provider","value")).put("drm",JSONObject.NULL)
            val playback=com.sportswave.app.YacineApi.parsePlayback(JSONArray().put(record),providerSource)
            check(playback.url.endsWith("live.m3u8") && playback.headers["X-Provider"]=="value" && playback.userAgent=="ProviderAgent")
            check(store.providerChannels("456").single().url.isBlank()) // Resolved URLs must not enter the catalog cache.
            record.put("url_type",6)
            check(runCatching { com.sportswave.app.YacineApi.parsePlayback(JSONArray().put(record),providerSource) }.isFailure)
            val brokenProvider=JSONObject().put("name","Bad").put("url","https://example.com/a.m3u8").put("integration","yacine").put("channelId","../other").put("serviceUrl",com.sportswave.app.YacineApi.DEFAULT_ENDPOINT)
            check(runCatching { store.import(JSONArray().put(brokenProvider).toString()) }.isFailure)
            val tennisHome=JSONObject().put("homeAway","home").put("athlete",JSONObject().put("displayName","Home player").put("flag",JSONObject().put("href","https://example.com/flag.png"))).put("linescores",JSONArray().put(JSONObject().put("value",6)).put(JSONObject().put("value",2)))
            val tennisAway=JSONObject().put("homeAway","away").put("athlete",JSONObject().put("displayName","Away player")).put("linescores",JSONArray().put(JSONObject().put("value",3)).put(JSONObject().put("value",1)))
            val tennisType=JSONObject().put("state","in").put("detail","2nd Set")
            val tennisMatch=JSONObject().put("id","42").put("date","2026-10-21T14:00Z").put("status",JSONObject().put("type",tennisType)).put("competitors",JSONArray().put(tennisAway).put(tennisHome))
            val tennisData=JSONObject().put("events",JSONArray().put(JSONObject().put("groupings",JSONArray().put(JSONObject().put("competitions",JSONArray().put(tennisMatch))))))
            val tennis=com.sportswave.app.SportsFeeds.parseTennis(tennisData,"ATP tennis").single()
            check(tennis.home=="Home player" && tennis.score=="2–1" && tennis.scoreDetail=="Sets: 6–3  2–1" && tennis.status=="Live · 2nd Set" && tennis.sport=="Tennis")
            tennisType.put("state","post").put("detail","Final")
            check(!com.sportswave.app.SportsFeeds.parseTennis(tennisData,"ATP tennis").single().status.startsWith("Live"))
            val publicAlkass=JSONObject().put("id",1).put("body","https://example.com/alkass-live.m3u8").put("is_locked",false).put("is_premium",0)
            check(com.sportswave.app.AlkassApi.parse(JSONArray().put(publicAlkass),1,providerSource)?.url?.endsWith("alkass-live.m3u8")==true)
            publicAlkass.put("is_locked",true)
            check(com.sportswave.app.AlkassApi.parse(JSONArray().put(publicAlkass),1,providerSource)==null)
            publicAlkass.put("is_locked",false).put("is_premium",12)
            check(com.sportswave.app.AlkassApi.parse(JSONArray().put(publicAlkass),1,providerSource)==null)
            store.cacheProviderPackages(listOf(com.sportswave.app.ChannelPackage("456","SPORTS",0)),true)
            check(store.providerPackages().single().id=="456" && store.providerSources().any { it.id==providerSource.id })
            result.putString("stream","PASS: catalog/calendar import, atomic validation, source updates, resolver configuration, source deletion, persistent favorites\n")
            finish(Activity.RESULT_OK,result)
        } catch(e:Throwable) {
            result.putString("stream","FAIL: ${e.message}\n${e.stackTraceToString()}")
            finish(Activity.RESULT_CANCELED,result)
        } finally { prefs.edit().clear().commit() }
    }
}
