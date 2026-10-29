package com.sportswave.app.tests

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.webkit.WebView
import android.widget.TextView
import com.sportswave.app.*
import org.json.JSONArray

/** A synthetic error checks recovery independently of volatile upstream availability. */
object WebRecoveryAudit {
    fun run(test:Instrumentation) {
        val rows=JSONArray(java.io.File(test.targetContext.filesDir,"jw-recovery-fixture.json").readText())
        require(rows.length()==2)
        val choices=(0 until rows.length()).map { i ->
            val url=rows.getJSONObject(i).getString("url")
            WebPlayerChoice(url,requireNotNull(WebProviders.playerRule(url)).id,"Recovery fixture $i")
        }
        val intent=Intent(test.targetContext,WebPlaybackActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("watchPage",WebProviders.pagesJson(listOf(WatchPage("direct",choices[0].url,label="Recovery fixture"))).toString()).putExtra("title","Synthetic JW recovery test")
        val activity=test.startActivitySync(intent)
        fun field(name:String)=activity.javaClass.getDeclaredField(name).apply { isAccessible=true }
        var sawReason=false;var selected=0
        try {
            test.runOnMainSync {
                field("choices").set(activity,choices)
                val view=field("browser").get(activity) as WebView
                view.stopLoading()
                view.loadDataWithBaseURL(choices[0].url,"<html><body><div class='jw-error-msg'>Error Code: 232403</div></body></html>","text/html","UTF-8",choices[0].url)
            }
            val started=android.os.SystemClock.elapsedRealtime()
            while(selected==0 && android.os.SystemClock.elapsedRealtime()-started<12_000) {
                Thread.sleep(200)
                test.runOnMainSync {
                    selected=field("selected").getInt(activity)
                    sawReason=sawReason || (field("status").get(activity) as TextView).text.contains("232403")
                }
            }
            check(sawReason && selected==1) { "JW recovery did not select the second server: reason=$sawReason selected=$selected" }
            test.finish(Activity.RESULT_OK,Bundle().apply { putString("stream","PASS: Android JW DOM error reported 232403 and automatically selected the next supplied server. Synthetic error fixture, not verified playback.\n") })
        } finally { test.runOnMainSync { activity.finish() } }
    }
}
