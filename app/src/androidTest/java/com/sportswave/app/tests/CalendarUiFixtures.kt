package com.sportswave.app.tests

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Explicit emulator-only fixtures. Restores the previous imported calendar after UI checks. */
object CalendarUiFixtures {
    fun apply(targetContext: android.content.Context, mode: String) {
        val prefs=targetContext.getSharedPreferences("sportswave",0)
        val backup=java.io.File(targetContext.filesDir,"calendar-ui-backup.json")
        if(mode=="restore") {
            check(backup.exists())
            val old=JSONObject(backup.readText()); val editor=prefs.edit()
            for(key in listOf("events","saved_event_details")) { if(old.isNull(key)) editor.remove(key) else editor.putString(key,old.getString(key)) }
            val saved=old.getJSONArray("saved"); editor.putStringSet("saved",(0 until saved.length()).map { saved.getString(it) }.toSet()).commit()
            backup.delete()
        } else {
            check(mode=="seed" && !backup.exists()) { "Restore the previous fixture run first" }
            backup.writeText(JSONObject().put("events",prefs.getString("events",null)).put("saved_event_details",prefs.getString("saved_event_details",null)).put("saved",JSONArray(prefs.getStringSet("saved",emptySet())!!.toList())).toString())
            val now=Instant.now(); val events=JSONArray()
            for(sport in listOf("Football","Basketball","Tennis","Motorsport","Cycling","Cricket")) {
                events.put(JSONObject().put("id","ui-airing-$sport").put("sport",sport).put("home","UI airing $sport").put("competition","Emulator routing check").put("startUtc",now.minusSeconds(120).toString()).put("endUtc",now.plusSeconds(3600).toString()).put("sourceId","demo-hls"))
            }
            events.put(JSONObject().put("id","ui-upcoming").put("sport","Football").put("home","UI upcoming fixture").put("startUtc",now.plusSeconds(7200).toString()).put("endUtc",now.plusSeconds(10800).toString()).put("sourceId","demo-hls"))
            events.put(JSONObject().put("id","ui-ended").put("sport","Football").put("home","UI ended fixture").put("startUtc",now.minusSeconds(7200).toString()).put("endUtc",now.minusSeconds(3600).toString()).put("sourceId","demo-hls"))
            com.sportswave.app.Store(targetContext).import(JSONObject().put("sources",JSONArray()).put("events",events).toString())
        }
    }
}
