package com.sportswave.app

import android.content.Context
import org.json.JSONArray

object SportsBranding {
    private val glyphs=mapOf("Football" to "ball-football","Basketball" to "ball-basketball","Tennis" to "ball-tennis","Motorsport" to "flag-check","Rugby" to "ball-american-football","Handball" to "play-handball","Volleyball" to "ball-volleyball","American football" to "ball-american-football","Padel" to "ping-pong","Cricket" to "cricket","Combat" to "karate","Cycling" to "bike","Athletics" to "run","Swimming" to "swimming","Golf" to "golf","Ice hockey" to "hockey","Baseball" to "ball-baseball","Horse racing" to "horse","Darts" to "target-arrow","Snooker" to "circle-number-8","Aussie rules" to "ball-american-football")
    private val glyphCache=mutableMapOf<String,android.graphics.Bitmap>()
    fun glyph(context:Context,sport:String):android.graphics.Bitmap? = runCatching {
        val name=glyphs[sport] ?: "trophy"
        glyphCache.getOrPut(name) {
            val svg=com.caverock.androidsvg.SVG.getFromAsset(context.assets,"sport-icons/$name.svg")
            svg.setDocumentWidth(64f);svg.setDocumentHeight(64f)
            android.graphics.Bitmap.createBitmap(64,64,android.graphics.Bitmap.Config.ARGB_8888).also { svg.renderToCanvas(android.graphics.Canvas(it)) }
        }
    }.getOrNull()
    private data class Mark(val sport:String,val name:String,val logo:String)
    @Volatile private var initialized=false
    private var marks=emptyList<Mark>()
    @Volatile private var index=emptyMap<String,List<String>>()
    @Synchronized fun initialize(context:Context) {
        if(initialized) return
        val rows=JSONArray(context.assets.open("team-logos.json").bufferedReader().use { it.readText() })
        marks=(0 until rows.length()).map { i ->
            val row=rows.getJSONObject(i);val aliases=row.getJSONArray("aliases")
            TeamIdentity.register(row.getString("sport"),row.getString("name"),(0 until aliases.length()).map { aliases.getString(it) })
            Mark(row.getString("sport"),row.getString("name"),row.getString("logo"))
        }
        val groups=JSONArray(context.assets.open("team-aliases.json").bufferedReader().use { it.readText() })
        for(i in 0 until groups.length()) {
            val group=groups.getJSONObject(i);val names=group.getJSONArray("names");val sport=group.getString("sport")
            TeamIdentity.reviewed(sport,(0 until names.length()).map { names.getString(it) })
        }
        val scoped=JSONArray(context.assets.open("team-context-aliases.json").bufferedReader().use { it.readText() })
        for(i in 0 until scoped.length()) {
            val row=scoped.getJSONObject(i);val names=row.getJSONArray("names")
            TeamIdentity.contextual(row.getString("sport"),row.getString("league"),row.getString("canonical"),(0 until names.length()).map { names.getString(it) })
        }
        index=marks.groupBy { "${it.sport}|${TeamIdentity.key(it.sport,it.name)}" }.mapValues { (_,items) -> items.map { it.logo }.distinct() }
        initialized=true
    }
    fun logos(sport:String,name:String,competition:String=""):List<String> = index["$sport|${TeamIdentity.key(sport,name,competition)}"].orEmpty()
    fun enrich(e:Event):Event=e.copy(homeCrest=e.homeCrest.ifBlank { logos(e.sport,e.home,e.competition).firstOrNull().orEmpty() },awayCrest=e.awayCrest.ifBlank { logos(e.sport,e.away,e.competition).firstOrNull().orEmpty() })
}
