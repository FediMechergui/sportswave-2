package com.sportswave.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.concurrent.Executors

/** Public provider webpages remain in their own browser engine, separate from native HLS. */
class WebPlaybackActivity: Activity() {
    private lateinit var page: WatchPage
    private lateinit var root: FrameLayout
    private lateinit var layout: LinearLayout
    private lateinit var surface: FrameLayout
    private lateinit var status: TextView
    private lateinit var loading: ProgressBar
    private var browser: WebView?=null
    private var metadata: WebView?=null
    private var choices=emptyList<WebPlayerChoice>()
    private var selected=0
    private var generation=0
    private var stopped=false
    private var customView: View?=null
    private var customCallback: WebChromeClient.CustomViewCallback?=null
    private var previousOrientation=ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private val worker=Executors.newSingleThreadExecutor()
    private val blocked=java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val attemptedPlayers=mutableSetOf<String>()

    companion object {
        fun open(activity: Activity,page: WatchPage,title: String) {
            require(WebProviders.validPage(page))
            activity.startActivity(Intent(activity,WebPlaybackActivity::class.java).putExtra("watchPage",WebProviders.pagesJson(listOf(page)).toString()).putExtra("title",title))
        }
    }
    private fun dp(value: Int)=(value*resources.displayMetrics.density).toInt()
    private fun label(value: String,size: Float=13f)=TextView(this).apply { text=value;textSize=size;setTextColor(Color.WHITE);setPadding(dp(12),dp(10),dp(12),dp(10)) }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        page=runCatching { WebProviders.parsePages(JSONArray(intent.getStringExtra("watchPage"))).single() }.getOrElse { finish();return }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor=Color.rgb(12,15,20);window.navigationBarColor=Color.BLACK
        root=FrameLayout(this).apply { setBackgroundColor(Color.BLACK);fitsSystemWindows=true }
        layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(12,15,20)) }
        val bar=LinearLayout(this)
        bar.addView(label("‹ Back").apply { setOnClickListener { finish() } })
        bar.addView(label(intent.getStringExtra("title").orEmpty(),15f).apply { maxLines=2 },LinearLayout.LayoutParams(0,-2,1f))
        layout.addView(bar)
        status=label("Finding published provider players…",12f).apply { setTextColor(Color.LTGRAY);accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE }
        layout.addView(status)
        surface=FrameLayout(this);layout.addView(surface,LinearLayout.LayoutParams(-1,0,1f))
        loading=ProgressBar(this);surface.addView(loading,FrameLayout.LayoutParams(-2,-2,android.view.Gravity.CENTER))
        val controls=LinearLayout(this)
        listOf("Servers" to { showChoices() },"Retry" to { attemptedPlayers.clear();if(choices.isEmpty()) discover() else play(selected) },"Refresh servers" to { discover() }).forEach { (name,action) -> controls.addView(label(name).apply { setTextColor(Color.rgb(255,77,67));isClickable=true;setOnClickListener { action() } },LinearLayout.LayoutParams(0,dp(52),1f)) }
        layout.addView(controls);root.addView(layout,FrameLayout.LayoutParams(-1,-1));setContentView(root)
        discover()
    }
    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    private fun newBrowser(): WebView = WebView(this).apply {
        settings.javaScriptEnabled=true;settings.domStorageEnabled=true
        settings.allowFileAccess=false;settings.allowContentAccess=false
        settings.mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.mediaPlaybackRequiresUserGesture=false;settings.javaScriptCanOpenWindowsAutomatically=false;settings.setSupportMultipleWindows(false)
        settings.cacheMode=android.webkit.WebSettings.LOAD_NO_CACHE
        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this,false)
        if(applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE!=0) WebView.setWebContentsDebuggingEnabled(true)
    }
    private fun emptyResponse()=WebResourceResponse("text/plain","UTF-8",ByteArrayInputStream(ByteArray(0)))
    private fun disposeMetadata() { val old=metadata;metadata=null;old?.let { root.removeView(it);runCatching { it.stopLoading() };it.destroy() } }
    private fun disposePlayer() { hideFullscreen();val old=browser;browser=null;old?.let { surface.removeView(it);runCatching { it.stopLoading();it.loadUrl("about:blank") };it.destroy() } }
    private fun error(message: String) { loading.visibility=View.GONE;status.text=message }
    private fun discover() {
        generation++;val request=generation;selected=0;choices=emptyList();blocked.clear();attemptedPlayers.clear();disposeMetadata();disposePlayer()
        loading.visibility=View.VISIBLE;status.text="Finding published players · ${page.label.ifBlank { page.provider }}…"
        if(page.provider=="direct" && WebProviders.playerRule(page.url)?.id!="daddy") { choices=listOf(WebPlayerChoice(page.url,WebProviders.playerRule(page.url)!!.id,page.label));play(0);return }
        if(page.provider in setOf("streamcenter","direct")) {
            worker.execute {
                val result=runCatching { if(page.provider=="direct") ExtraScheduleFeeds.channelPlayers(page.url).map { it.copy(label=page.label.ifBlank { it.label }) } else ExtraScheduleFeeds.streamCenterPlayers(page.eventId) }
                runOnUiThread { if(request!=generation || isFinishing || isDestroyed || stopped) return@runOnUiThread
                    result.onSuccess { found(it) }.onFailure { error("Provider discovery unavailable. Refresh servers to retry.") }
                }
            };return
        }
        val view=newBrowser();metadata=view
        root.addView(view,FrameLayout.LayoutParams(1,1))
        view.importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        var eventLoaded=page.provider!="viprow";var documentGeneration=0
        view.webViewClient=object: WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView,r: WebResourceRequest)=!WebProviders.metadataUrl(page,r.url.toString())
            override fun shouldInterceptRequest(v: WebView,r: WebResourceRequest): WebResourceResponse? {
                val allowed=if(r.isForMainFrame) WebProviders.metadataUrl(page,r.url.toString()) else WebProviders.uri(r.url.toString())?.host in when(page.provider) { "viprow"->setOf("viprow.im");"livekora"->setOf("pl.koralive1.cc","pl.matchlivehd.com");else->setOf("yallashootkoora.online") }
                return if(allowed) null else emptyResponse()
            }
            override fun onPageStarted(v: WebView,url: String,icon: android.graphics.Bitmap?) { documentGeneration++ }
            override fun onPageFinished(v: WebView,url: String) {
                if(metadata!==v || request!=generation || !WebProviders.metadataUrl(page,url)) return
                val document=documentGeneration
                fun scan(attempt: Int) {
                    if(metadata!==v || request!=generation || document!=documentGeneration || isDestroyed) return
                    val script=if(!eventLoaded) """(function(){const id=${JSONObject.quote(page.eventId)};const a=[...document.querySelectorAll('a[href]')].find(a=>{try{return new URL(a.href).pathname.split('/').includes(id)}catch(e){return false}});return a?a.href:''})()"""
                    else """(function(){return JSON.stringify([...document.querySelectorAll('a[href],a[data-href],a[data-url],iframe[src],iframe[data-src]')].map(x=>{try{return {url:new URL(x.getAttribute('href')||x.dataset.href||x.dataset.url||x.getAttribute('src')||x.dataset.src||'',document.baseURI).href,label:(x.getAttribute('title')||x.getAttribute('aria-label')||x.innerText||'').replace(/open_in_new/g,'').trim().slice(0,100)}}catch(e){return null}}).filter(Boolean))})()"""
                    v.evaluateJavascript(script) { raw ->
                        if(metadata!==v || request!=generation || document!=documentGeneration) return@evaluateJavascript
                        val decoded=runCatching { org.json.JSONTokener(raw).nextValue() as? String }.getOrNull().orEmpty()
                        if(!eventLoaded) {
                            if(WebProviders.viprowEvent(decoded) && WebProviders.metadataUrl(page,decoded)) { eventLoaded=true;v.loadUrl(decoded);return@evaluateJavascript }
                        } else {
                            val rows=runCatching { JSONArray(decoded) }.getOrNull()
                            val players=if(rows==null) emptyList() else (0 until minOf(rows.length(),5000)).mapNotNull { i ->
                                val o=rows.optJSONObject(i) ?: return@mapNotNull null;val link=o.optString("url");val rule=WebProviders.playerRule(link) ?: return@mapNotNull null
                                WebPlayerChoice(link,rule.id,o.optString("label").ifBlank { rule.id }.take(100))
                            }.distinctBy { it.url }.take(30)
                            if(players.isNotEmpty()) { disposeMetadata();found(players);return@evaluateJavascript }
                        }
                        if(attempt<8) v.postDelayed({ scan(attempt+1) },400) else { disposeMetadata();error(if(eventLoaded) "No compatible published players for this event. Try another listed provider." else "This event is no longer listed. Refresh the calendar.") }
                    }
                }
                scan(0)
            }
            override fun onReceivedError(v: WebView,r: WebResourceRequest,e: android.webkit.WebResourceError) { if(r.isForMainFrame && metadata===v) { disposeMetadata();error("Provider page could not load. Refresh servers to retry.") } }
            override fun onReceivedHttpError(v: WebView,r: WebResourceRequest,e: WebResourceResponse) { if(r.isForMainFrame && metadata===v) { disposeMetadata();error("Provider page returned HTTP ${e.statusCode}.") } }
        }
        view.loadUrl(page.url)
        view.postDelayed({ if(metadata===view && request==generation) { disposeMetadata();error("Provider discovery timed out. Refresh servers to retry.") } },25000)
    }
    private fun found(players: List<WebPlayerChoice>) {
        choices=players
        if(players.isEmpty()) error("No reviewed player is available for this event.") else play(0)
    }
    private fun showChoices() {
        if(choices.isEmpty()) { error("No players loaded. Use Refresh servers.");return }
        AlertDialog.Builder(this).setTitle("Published servers · language may be unverified").setItems(choices.map { "${it.label} · ${it.provider}" }.toTypedArray()) { _,i -> attemptedPlayers.clear();play(i) }.setNegativeButton("Cancel",null).show()
    }
    private fun play(index: Int) {
        selected=index;disposePlayer();blocked.clear()
        val choice=choices.getOrNull(index) ?: return
        val entry=choice.url;check(WebProviders.playerRule(entry)!=null)
        attemptedPlayers.add(entry)
        var failed=false
        loading.visibility=View.VISIBLE
        status.text="${choice.label} · Provider programming may differ. Use Servers if video stalls."
        val view=newBrowser();browser=view;surface.addView(view,0,FrameLayout.LayoutParams(-1,-1))
        val earlyScripts=androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT)
        if(earlyScripts) androidx.webkit.WebViewCompat.addDocumentStartJavaScript(view,WebPlayerHealth.script,WebProviders.rules.flatMap { it.hosts }.map { "https://$it" }.toSet())
        if(choice.provider=="alba" && androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.DOCUMENT_START_SCRIPT)) {
            androidx.webkit.WebViewCompat.addDocumentStartJavaScript(view,"if(typeof window.chrome==='undefined')window.chrome={};",setOf("https://pl.matchlivehd.com"))
        }
        view.webViewClient=object: WebViewClient() {
            override fun shouldOverrideUrlLoading(v: WebView,r: WebResourceRequest)=!WebProviders.allowsPlayerNavigation(entry,r.url.toString(),r.isForMainFrame)
            override fun shouldInterceptRequest(v: WebView,r: WebResourceRequest): WebResourceResponse? {
                val allowed=if(r.isForMainFrame) WebProviders.allowsPlayerNavigation(entry,r.url.toString(),true) else WebProviders.allowsPlayerResource(entry,r.url.toString())
                if(!allowed && blocked.add(r.url.host.orEmpty()) && applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE!=0) android.util.Log.i("SportsWaveWeb","Blocked host=${r.url.host} main=${r.isForMainFrame}")
                return if(allowed) null else emptyResponse()
            }
            override fun onPageFinished(v: WebView,url: String) { if(browser===v) { loading.visibility=View.GONE;if(!earlyScripts) v.evaluateJavascript(WebPlayerHealth.script,null) } }
            override fun onReceivedError(v: WebView,r: WebResourceRequest,e: android.webkit.WebResourceError) { if(r.isForMainFrame && browser===v) error("Player page unavailable. Use Servers or Retry.") }
            override fun onReceivedHttpError(v: WebView,r: WebResourceRequest,e: WebResourceResponse) { if(browser!==v || failed) return;if(r.isForMainFrame) { failed=true;error("Player page returned HTTP ${e.statusCode}. Use Servers.") } else status.text="Provider request failed (HTTP ${e.statusCode}). Switch server if video stalls." }
            override fun onRenderProcessGone(v: WebView,detail: android.webkit.RenderProcessGoneDetail): Boolean { if(browser===v) { disposePlayer();error("Web player stopped. Use Retry to reopen it.") };return true }
        }
        view.webChromeClient=object: WebChromeClient() {
            override fun onConsoleMessage(message:android.webkit.ConsoleMessage):Boolean {
                val code=WebPlayerHealth.code(message.message()) ?: return false
                if(browser!==view || stopped || failed || (message.sourceId()!=WebPlayerHealth.SOURCE && !WebProviders.allowsPlayerResource(entry,message.sourceId()))) return false
                failed=true
                val reason=WebPlayerHealth.message(code)
                val next=WebPlayerHealth.next(choices,attemptedPlayers)
                if(applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE!=0) android.util.Log.i("SportsWaveWeb","JW code=$code; next=${next ?: -1}")
                if(next==null) error("$reason. Use Servers, Retry or Refresh servers.") else {
                    status.text="$reason. Trying another published server…"
                    view.postDelayed({ if(browser===view && !stopped && !isDestroyed && !isFinishing) play(next) },1200)
                }
                return true
            }
            override fun onCreateWindow(v: WebView?,dialog: Boolean,gesture: Boolean,result: android.os.Message?)=false
            override fun onPermissionRequest(request: android.webkit.PermissionRequest) { request.deny() }
            override fun onShowCustomView(v: View,callback: CustomViewCallback) {
                if(customView!=null) { callback.onCustomViewHidden();return }
                previousOrientation=requestedOrientation;requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;customView=v;customCallback=callback;layout.visibility=View.GONE;root.addView(v,FrameLayout.LayoutParams(-1,-1))
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            override fun onHideCustomView() { hideFullscreen() }
        }
        view.loadUrl(entry)
        view.postDelayed({ if(browser===view && !failed) { loading.visibility=View.GONE;status.text="${choice.label} · If video is blank or buffering, use Servers or Retry." } },25000)
    }
    private fun hideFullscreen() {
        customView?.let { root.removeView(it);requestedOrientation=previousOrientation };customView=null;val callback=customCallback;customCallback=null;callback?.onCustomViewHidden()
        if(::layout.isInitialized) layout.visibility=View.VISIBLE
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=0
    }
    @Deprecated("Back leaves fullscreen first")
    override fun onBackPressed() { if(customView!=null) hideFullscreen() else super.onBackPressed() }
    override fun onStop() { super.onStop();stopped=true;generation++;disposeMetadata();disposePlayer() }
    override fun onStart() { super.onStart();if(stopped) { stopped=false;discover() } }
    override fun onDestroy() { generation++;disposeMetadata();disposePlayer();worker.shutdownNow();super.onDestroy() }
}
