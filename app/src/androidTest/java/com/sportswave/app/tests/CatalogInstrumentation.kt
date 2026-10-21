package com.sportswave.app.tests

import android.app.Activity
import android.app.Instrumentation
import android.os.Bundle
import com.sportswave.app.Store
import org.json.JSONObject
import org.json.JSONArray

class CatalogInstrumentation : Instrumentation() {
    private var sessionCatalog:String?=null
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); sessionCatalog=arguments?.getString("sessionCatalog"); start() }
    override fun onStart() {
        val result=Bundle()
        val prefs=targetContext.getSharedPreferences("sportswave-tests",0)
        try {
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
            result.putString("stream","PASS: catalog/calendar import, atomic validation, source updates, resolver configuration, source deletion, persistent favorites\n")
            finish(Activity.RESULT_OK,result)
        } catch(e:Throwable) {
            result.putString("stream","FAIL: ${e.message}\n${e.stackTraceToString()}")
            finish(Activity.RESULT_CANCELED,result)
        } finally { prefs.edit().clear().commit() }
    }
}
