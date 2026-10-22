package com.sportswave.app

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.util.concurrent.Executors

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class PlayerActivity : Activity() {
    private var player: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private lateinit var retry: TextView
    private lateinit var container: FrameLayout
    private var fullscreen=false
    private var resizeMode=AspectRatioFrameLayout.RESIZE_MODE_FIT
    private var feedbackView: View?=null
    private val executor = Executors.newSingleThreadExecutor()
    private var original: Source? = null
    private var alternatives: List<Source> = emptyList()
    private var position = 0L
    private var shouldPlay = true
    private var stopped = false
    private var generation = 0
    private var automaticRenewals = 0
    private val attemptedSources = mutableSetOf<String>()
    private val dark = Color.rgb(12,15,20)
    private val red = Color.rgb(255,77,67)
    private val muted = Color.rgb(153,162,176)

    private fun dp(n:Int) = (n * resources.displayMetrics.density).toInt()
    private fun label(value:String,size:Int=14,color:Int=Color.WHITE,bold:Boolean=false) = TextView(this).apply { text=value; textSize=size.toFloat(); setTextColor(color); typeface=Typeface.create("sans-serif",if(bold) Typeface.BOLD else Typeface.NORMAL); includeFontPadding=false }
    private fun button(value:String, action:()->Unit) = label(value,13,Color.WHITE,true).apply { gravity=Gravity.CENTER; setPadding(dp(13),dp(14),dp(13),dp(14)); isFocusable=true; setOnClickListener { action() } }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=dark; window.navigationBarColor=dark
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        original=Store(this).all().firstOrNull { it.id==(savedInstanceState?.getString("selectedSource") ?: intent.getStringExtra("sourceId")) }
        val ids=intent.getStringArrayExtra("alternatives")?.toSet() ?: emptySet()
        alternatives=Store(this).all().filter { it.id in ids }
        position=savedInstanceState?.getLong("position") ?: 0L
        shouldPlay=savedInstanceState?.getBoolean("play") ?: true
        fullscreen=savedInstanceState?.getBoolean("fullscreen") ?: (resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE)
        render()
    }
    private fun icon(name:String,description:String,action:()->Unit):FrameLayout = FrameLayout(this).apply {
        contentDescription=description; isClickable=true; isFocusable=true; minimumWidth=dp(48); minimumHeight=dp(48)
        val style=obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackgroundBorderless)); foreground=style.getDrawable(0); style.recycle()
        addView(NavigationIcon(this@PlayerActivity,name,Color.WHITE),FrameLayout.LayoutParams(dp(24),dp(24),Gravity.CENTER))
        setOnClickListener { action() }
    }
    private fun render() {
        if(::playerView.isInitialized) (playerView.parent as? android.view.ViewGroup)?.removeView(playerView)
        else playerView=PlayerView(this).apply {
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING); setShowSubtitleButton(true)
            setBackgroundColor(Color.BLACK); contentDescription="Video player"; controllerShowTimeoutMs=3000
            setFullscreenButtonClickListener { toggleFullscreen() }
        }
        playerView.resizeMode=resizeMode
        playerView.player=player
        container=FrameLayout(this).apply { setBackgroundColor(if(fullscreen) Color.BLACK else dark) }
        container.setOnApplyWindowInsetsListener { v,i ->
            v.setPadding(if(fullscreen) 0 else i.systemWindowInsetLeft,if(fullscreen) 0 else i.systemWindowInsetTop,if(fullscreen) 0 else i.systemWindowInsetRight,if(fullscreen) 0 else i.systemWindowInsetBottom); i
        }
        setContentView(container)
        val bar=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; setBackgroundColor(if(fullscreen) 0x99000000.toInt() else dark) }
        bar.addView(icon("Back","Back") { if(fullscreen) exitFullscreen() else finish() })
        bar.addView(label(original?.name ?: "Source unavailable",16,Color.WHITE,true).apply { maxLines=1; ellipsize=android.text.TextUtils.TruncateAt.END },LinearLayout.LayoutParams(0,-2,1f))
        bar.addView(icon(if(fullscreen) "Collapse" else "Fullscreen","Toggle fullscreen") { toggleFullscreen() })
        status=label("Preparing your stream…",12,muted).apply { setPadding(dp(20),dp(12),dp(20),dp(8)); accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE }
        retry=button("Retry playback") { automaticRenewals=0; attemptedSources.clear(); position=0; load() }.apply { visibility=View.GONE; setTextColor(red) }
        val feedback=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; addView(status); addView(retry) }
        feedbackView=feedback
        if(fullscreen) {
            container.addView(playerView,FrameLayout.LayoutParams(-1,-1))
            container.addView(bar,FrameLayout.LayoutParams(-1,dp(52),Gravity.TOP))
            feedback.setBackgroundColor(0xAA000000.toInt())
            container.addView(feedback,FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM).apply { bottomMargin=dp(78) })
            playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                bar.visibility=visibility
                feedback.visibility=if(visibility==View.VISIBLE || player?.playerError!=null || retry.visibility==View.VISIBLE) View.VISIBLE else View.GONE
            })
        } else {
            playerView.setControllerVisibilityListener(null as PlayerView.ControllerVisibilityListener?)
            val scroll=ScrollView(this).apply { isFillViewport=true }
            val content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            scroll.addView(content); container.addView(scroll,FrameLayout.LayoutParams(-1,-1))
            content.addView(bar,LinearLayout.LayoutParams(-1,dp(56)))
            val width=resources.displayMetrics.widthPixels/resources.displayMetrics.density
            content.addView(playerView,LinearLayout.LayoutParams(-1,dp((width*9f/16f).toInt())))
            content.addView(feedback)
            if(alternatives.size>1) content.addView(button("Choose another broadcast") { chooseBroadcast() })
            val options=LinearLayout(this)
            options.addView(button("Quality") { quality() },LinearLayout.LayoutParams(0,-2,1f))
            options.addView(button("Speed") { speed() },LinearLayout.LayoutParams(0,-2,1f))
            options.addView(button("Fit / fill") { resizeMode=if(resizeMode==AspectRatioFrameLayout.RESIZE_MODE_FIT) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT; playerView.resizeMode=resizeMode },LinearLayout.LayoutParams(0,-2,1f))
            content.addView(options)
            val info=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(22),dp(24),dp(22),dp(24)) }
            val eventTitle=intent.getStringExtra("eventTitle").orEmpty()
            info.addView(label(if(original?.demo==true) "PLAYER TEST" else original?.provider?.ifBlank { "Your channel" } ?: "Your channel",11,red,true))
            info.addView(label(eventTitle.ifBlank { original?.name ?: "No source selected" },24,Color.WHITE,true).apply { setPadding(0,dp(12),0,dp(10)) })
            info.addView(label(if(original?.demo==true) "Public sample video." else "${original?.name.orEmpty()} · Playback stays inside SportsWave.",14,muted))
            content.addView(info)
        }
        applySystemBars(); updateStatus()
    }
    private fun chooseBroadcast() {
        AlertDialog.Builder(this).setTitle("Available broadcasts").setItems(alternatives.map { "${it.catalogCategory.ifBlank { it.provider }} · ${it.name}" }.toTypedArray()) { _,index ->
            original=alternatives[index]; automaticRenewals=0; attemptedSources.clear(); position=0; shouldPlay=true; render(); load()
        }.show()
    }
    private fun applySystemBars() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility=if(fullscreen) View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE else 0
        if(android.os.Build.VERSION.SDK_INT>=30) {
            window.setDecorFitsSystemWindows(!fullscreen)
            window.insetsController?.apply {
                systemBarsBehavior=android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if(fullscreen) hide(android.view.WindowInsets.Type.systemBars()) else show(android.view.WindowInsets.Type.systemBars())
            }
        }
        container.requestApplyInsets()
    }
    private fun toggleFullscreen() {
        fullscreen=!fullscreen
        requestedOrientation=if(fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        render()
    }
    private fun exitFullscreen() { if(fullscreen) toggleFullscreen() }
    @Deprecated("Back exits fullscreen before leaving playback")
    override fun onBackPressed() { if(fullscreen) exitFullscreen() else super.onBackPressed() }
    override fun onWindowFocusChanged(hasFocus:Boolean) { super.onWindowFocusChanged(hasFocus); if(hasFocus && ::container.isInitialized) applySystemBars() }
    override fun onConfigurationChanged(newConfig:Configuration) { super.onConfigurationChanged(newConfig); fullscreen=newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE; render() }
    override fun onStart() { super.onStart(); stopped=false; if(player==null) load() }
    private fun load() {
        val source=original
        if(source==null) { status.text="This channel is no longer in your collection."; return }
        attemptedSources.add(source.id)
        generation++
        val request=generation
        playerView.player=null
        player?.release(); player=null
        retry.visibility=View.GONE
        feedbackView?.visibility=View.VISIBLE
        status.text=if(source.resolverUrl.isNotBlank() || source.integration=="yacine") "Requesting a fresh playback session…" else "Connecting to your source…"
        executor.execute {
            val result=runCatching { PlaybackResolver.resolve(source) }
            runOnUiThread {
                if(isDestroyed || isFinishing || stopped || request!=generation) return@runOnUiThread
                result.onSuccess { prepare(it) }.onFailure {
                    status.text="Couldn't resolve playback. Check your provider or connection."
                    retry.visibility=View.VISIBLE
                    feedbackView?.visibility=View.VISIBLE
                }
            }
        }
    }
    private fun prepare(source:Source) {
        try {
            require(Catalog.validMediaUrl(source.url,source.allowHttp)) { "Unsupported playback URL." }
            val http=DefaultHttpDataSource.Factory().setUserAgent(source.userAgent).setDefaultRequestProperties(source.headers).setConnectTimeoutMs(12000).setReadTimeoutMs(15000).setAllowCrossProtocolRedirects(false)
            val instance=ExoPlayer.Builder(this).setMediaSourceFactory(DefaultMediaSourceFactory(http)).build()
            player=instance
            instance.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true)
            instance.setHandleAudioBecomingNoisy(true)
            instance.addListener(object:Player.Listener {
                override fun onPlaybackStateChanged(playbackState:Int) { updateStatus() }
                override fun onIsPlayingChanged(isPlaying:Boolean) { updateStatus() }
                override fun onPlayerError(error:PlaybackException) {
                    val failedResponse=generateSequence(error as Throwable?) { it.cause }.filterIsInstance<androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException>().firstOrNull()
                    val networkUnavailable=error.errorCode in listOf(PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
                    if(original?.integration=="yacine" && ProviderRecovery.shouldRetry(failedResponse?.responseCode,error.errorCode==PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW,automaticRenewals,networkUnavailable)) {
                        automaticRenewals++; position=0
                        val failedGeneration=generation
                        status.text="Refreshing the channel session…"
                        playerView.postDelayed({ if(!isDestroyed && !isFinishing && !stopped && generation==failedGeneration) load() },ProviderRecovery.delayMs(automaticRenewals))
                        return
                    }
                    val unavailable=networkUnavailable || failedResponse?.responseCode in listOf(401,403,404,410,502,503)
                    val next=original?.takeIf { it.integration=="yacine" && unavailable }?.let { ChannelCatalog.nextAlternative(it,alternatives,attemptedSources) }
                    if(next!=null) {
                        val failedGeneration=generation
                        status.text="Trying another quality for this channel…"
                        playerView.post { if(!isDestroyed && !isFinishing && !stopped && generation==failedGeneration) { original=next; position=0; render(); load() } }
                        return
                    }
                    val response=generateSequence(error as Throwable?) { it.cause }.filterIsInstance<androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException>().firstOrNull()
                    status.text=if(original?.integration=="yacine" && response?.responseCode==503 && response.responseBody.toString(Charsets.UTF_8).trim()=="No URLs available") "The provider has no feed available for this channel. Try again later." else "Playback unavailable (${response?.responseCode?.let { "HTTP $it" } ?: error.errorCodeName}). Retry or check your provider."
                    retry.visibility=View.VISIBLE
                    feedbackView?.visibility=View.VISIBLE
                }
            })
            val item=MediaItem.Builder().setUri(source.url)
            if(source.mime.isNotBlank()) item.setMimeType(source.mime)
            if(source.licenseUrl.isNotBlank()) item.setDrmConfiguration(MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID).setLicenseUri(source.licenseUrl).setLicenseRequestHeaders(source.licenseHeaders).build())
            playerView.player=instance
            instance.setMediaItem(item.build())
            if(position>0) instance.seekTo(position)
            instance.prepare(); instance.playWhenReady=shouldPlay
        } catch(e:Exception) { player?.release(); player=null; status.text="Couldn't initialize this stream. Check its format and provider settings."; feedbackView?.visibility=View.VISIBLE; retry.visibility=View.VISIBLE }
    }
    private fun updateStatus() {
        if(!::status.isInitialized) return
        val p=player ?: return
        if(p.playerError!=null) return // Preserve the actionable failure message and retry button.
        status.text=when(p.playbackState) {
            Player.STATE_BUFFERING -> "Buffering…"
            Player.STATE_READY -> if(p.isPlaying) "Playing inside SportsWave" else "Paused"
            Player.STATE_ENDED -> "This video has ended."
            else -> "Connecting…"
        }
    }
    private fun quality() {
        val labels=arrayOf("Auto (recommended)","Up to 360p","Up to 720p","Up to 1080p")
        AlertDialog.Builder(this).setTitle("Video quality").setItems(labels) { _,which ->
            val p=player ?: return@setItems
            val builder=p.trackSelectionParameters.buildUpon().clearVideoSizeConstraints()
            if(which>0) builder.setMaxVideoSize(Int.MAX_VALUE,intArrayOf(0,360,720,1080)[which])
            p.trackSelectionParameters=builder.build()
            Toast.makeText(this,"${labels[which]} · Uses available source tracks",Toast.LENGTH_SHORT).show()
        }.show()
    }
    private fun speed() { AlertDialog.Builder(this).setTitle("Playback speed").setItems(arrayOf("0.75×","1× · Normal","1.25×","1.5×","2×")) { _,i -> player?.setPlaybackSpeed(floatArrayOf(.75f,1f,1.25f,1.5f,2f)[i]) }.show() }
    override fun onSaveInstanceState(outState:Bundle) { outState.putString("selectedSource",original?.id); outState.putBoolean("fullscreen",fullscreen); outState.putLong("position",if(player?.isCurrentMediaItemLive==true) 0 else player?.currentPosition ?: position); outState.putBoolean("play",player?.playWhenReady ?: shouldPlay); super.onSaveInstanceState(outState) }
    override fun onStop() {
        stopped=true; generation++
        player?.let { position=if(it.isCurrentMediaItemLive) 0 else it.currentPosition; shouldPlay=it.playWhenReady; it.release() }
        player=null; if(::playerView.isInitialized) playerView.player=null
        super.onStop()
    }
    override fun onDestroy() { executor.shutdownNow(); player?.release(); super.onDestroy() }
}
