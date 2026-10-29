package com.sportswave.app.tests

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.sportswave.app.PlayerActivity
import com.sportswave.app.Store
import java.net.ServerSocket
import org.json.JSONArray
import org.json.JSONObject

/** A live playlist whose segment never becomes decodable must not buffer indefinitely. */
object BufferingAudit {
    fun run(test: Instrumentation) {
        val server=ServerSocket(0,10,java.net.InetAddress.getByName("127.0.0.1"))
        val worker=Thread {
            while(!server.isClosed) {
                runCatching {
                    server.accept().use { socket ->
                        socket.soTimeout=3000
                        val input=socket.getInputStream().bufferedReader()
                        val request=input.readLine().orEmpty()
                        while(true) { val line=input.readLine() ?: break; if(line.isEmpty()) break }
                        if(request.contains("/segment.ts")) {
                            socket.getOutputStream().apply {
                                write("HTTP/1.1 200 OK\r\nContent-Type: video/mp2t\r\nContent-Length: 100000\r\nConnection: close\r\n\r\n".toByteArray());flush()
                                repeat(200) { write(0);flush();Thread.sleep(200) }
                            }
                            return@use
                        }
                        val body="#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:2\n#EXT-X-MEDIA-SEQUENCE:1\n#EXTINF:2.0,\nsegment.ts\n".toByteArray()
                        socket.getOutputStream().apply {
                            write("HTTP/1.1 200 OK\r\nContent-Type: application/vnd.apple.mpegurl\r\nContent-Length: ${body.size}\r\nConnection: close\r\n\r\n".toByteArray())
                            write(body);flush()
                        }
                    }
                }
            }
        }.apply { isDaemon=true;start() }
        val store=Store(test.targetContext)
        val id="buffering-audit-local"
        var activity:Activity?=null
        try {
            store.import(JSONArray().put(JSONObject().put("id",id).put("name","Local stalled playlist test").put("url","http://127.0.0.1:${server.localPort}/live.m3u8").put("mime","application/x-mpegURL").put("allowHttp",true)).toString())
            val started=android.os.SystemClock.elapsedRealtime()
            activity=test.startActivitySync(Intent(test.targetContext,PlayerActivity::class.java).putExtra("sourceId",id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            var failureVisible=false
            var failureMessage=""
            while(android.os.SystemClock.elapsedRealtime()-started<40_000 && !failureVisible) {
                Thread.sleep(250)
                test.runOnMainSync {
                    fun texts(view:View):List<String> = when(view) {
                        is TextView -> if(view.visibility==View.VISIBLE) listOf(view.text.toString()) else emptyList()
                        is ViewGroup -> (0 until view.childCount).flatMap { texts(view.getChildAt(it)) }
                        else -> emptyList()
                    }
                    failureMessage=texts(activity!!.window.decorView).firstOrNull { it.startsWith("Playback unavailable") }.orEmpty()
                    failureVisible=failureMessage.isNotEmpty()
                }
            }
            val elapsed=android.os.SystemClock.elapsedRealtime()-started
            check(failureVisible && elapsed>=27_000 && failureMessage.contains("ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT")) { "Expected buffering timeout recovery after 30s; failure=$failureVisible elapsed=$elapsed message=$failureMessage" }
            test.finish(Activity.RESULT_OK,Bundle().apply { putString("stream","PASS: stalled HTTP-200 live segment left buffering with actionable retry after ${elapsed/1000}s.\n") })
        } finally {
            activity?.let { current -> test.runOnMainSync { current.finish() } }
            store.remove(id);server.close();worker.join(3500)
        }
    }
}
