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
    private lateinit var container: LinearLayout
    private val executor = Executors.newSingleThreadExecutor()
    private var original: Source? = null
    private var alternatives: List<Source> = emptyList()
    private var position = 0L
    private var shouldPlay = true
    private var stopped = false
    private var generation = 0
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
        original=Store(this).all().firstOrNull { it.id==intent.getStringExtra("sourceId") }
        val ids=intent.getStringArrayExtra("alternatives")?.toSet() ?: emptySet()
        alternatives=Store(this).all().filter { it.id in ids }
        position=savedInstanceState?.getLong("position") ?: 0L
        shouldPlay=savedInstanceState?.getBoolean("play") ?: true
        render()
    }
    private fun render() {
        if(::playerView.isInitialized) playerView.player=null
        container=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(dark) }
        container.setOnApplyWindowInsetsListener { v,i -> v.setPadding(i.systemWindowInsetLeft,i.systemWindowInsetTop,i.systemWindowInsetRight,i.systemWindowInsetBottom); i }
        setContentView(container)
        val bar=LinearLayout(this).apply { gravity=Gravity.CENTER_VERTICAL; setPadding(dp(5),dp(3),dp(8),dp(3)) }
        bar.addView(button("‹ Back") { finish() })
        bar.addView(label(original?.name ?: "Source unavailable",16,Color.WHITE,true),LinearLayout.LayoutParams(0,-2,1f))
        bar.addView(button("⛶") { toggleFullscreen() }.apply { contentDescription="Toggle fullscreen" })
        container.addView(bar)
        playerView=PlayerView(this).apply { player=this@PlayerActivity.player; setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING); setShowSubtitleButton(true); resizeMode=AspectRatioFrameLayout.RESIZE_MODE_FIT; setBackgroundColor(Color.BLACK) }
        val landscape=resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE
        container.addView(playerView, if(landscape) LinearLayout.LayoutParams(-1,0,1f) else LinearLayout.LayoutParams(-1,dp(238)))
        status=label("Preparing your stream…",12,muted).apply { setPadding(dp(20),dp(12),dp(20),dp(8)) }
        container.addView(status)
        retry=button("Retry playback") { load() }.apply { visibility=View.GONE; setTextColor(red) }
        container.addView(retry)
        if(alternatives.size>1) container.addView(button("Choose another broadcast") {
            AlertDialog.Builder(this).setTitle("Event broadcasts").setItems(alternatives.map { "${it.provider} · ${it.name}" }.toTypedArray()) { _,index ->
                original=alternatives[index]; position=0; shouldPlay=true; render(); load()
            }.show()
        })
        if(!landscape) {
            val options=LinearLayout(this)
            options.addView(button("Quality") { quality() },LinearLayout.LayoutParams(0,-2,1f))
            options.addView(button("Speed") { speed() },LinearLayout.LayoutParams(0,-2,1f))
            options.addView(button("Fit / fill") { playerView.resizeMode=if(playerView.resizeMode==AspectRatioFrameLayout.RESIZE_MODE_FIT) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT },LinearLayout.LayoutParams(0,-2,1f))
            container.addView(options)
            val info=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(23),dp(24),dp(23),dp(16)) }
            info.addView(label(if(original?.demo==true) "DEMO PLAYBACK" else if(original?.sessionSnapshot==true) "TEMPORARY SESSION · ${original?.provider}" else original?.provider?.uppercase()?.ifBlank { "YOUR CHANNEL" } ?: "YOUR CHANNEL",10,red,true))
            info.addView(label(original?.name ?: "No source selected",25,Color.WHITE,true).apply { setPadding(0,dp(13),0,dp(12)) })
            info.addView(label(if(original?.demo==true) "A public sample stream to test SportsWave's built-in player. This video is not the broadcast of a scheduled event." else if(original?.sessionSnapshot==true) "This captured playback session can expire. Retry cannot renew it without a connected provider resolver." else "Playback stays inside SportsWave. Stream quality and availability depend on your source provider.",14,muted))
            info.addView(label("SPORTSWAVE  /  ALL IN PLAY",10,muted,true).apply { setPadding(0,dp(36),0,0) })
            container.addView(info)
        }
        updateStatus()
    }
    private fun toggleFullscreen() {
        requestedOrientation=if(resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    override fun onConfigurationChanged(newConfig:Configuration) { super.onConfigurationChanged(newConfig); render() }
    override fun onStart() { super.onStart(); stopped=false; if(player==null) load() }
    private fun load() {
        val source=original
        if(source==null) { status.text="This channel is no longer in your collection."; return }
        generation++
        val request=generation
        playerView.player=null
        player?.release(); player=null
        retry.visibility=View.GONE
        status.text=if(source.resolverUrl.isNotBlank()) "Requesting a fresh playback session…" else "Connecting to your source…"
        executor.execute {
            val result=runCatching { PlaybackResolver.resolve(source) }
            runOnUiThread {
                if(isDestroyed || isFinishing || stopped || request!=generation) return@runOnUiThread
                result.onSuccess { prepare(it) }.onFailure {
                    status.text="Couldn't resolve playback. Check your provider or connection."
                    retry.visibility=View.VISIBLE
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
                    status.text="Playback unavailable (${error.errorCodeName}). Retry or check your provider."
                    retry.visibility=View.VISIBLE
                }
            })
            val item=MediaItem.Builder().setUri(source.url)
            if(source.mime.isNotBlank()) item.setMimeType(source.mime)
            if(source.licenseUrl.isNotBlank()) item.setDrmConfiguration(MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID).setLicenseUri(source.licenseUrl).setLicenseRequestHeaders(source.licenseHeaders).build())
            playerView.player=instance
            instance.setMediaItem(item.build())
            if(position>0) instance.seekTo(position)
            instance.prepare(); instance.playWhenReady=shouldPlay
        } catch(e:Exception) { player?.release(); player=null; status.text="Couldn't initialize this stream. Check its format and provider settings."; retry.visibility=View.VISIBLE }
    }
    private fun updateStatus() {
        if(!::status.isInitialized) return
        val p=player ?: return
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
    override fun onSaveInstanceState(outState:Bundle) { outState.putLong("position",player?.currentPosition ?: position); outState.putBoolean("play",player?.playWhenReady ?: shouldPlay); super.onSaveInstanceState(outState) }
    override fun onStop() {
        stopped=true; generation++
        player?.let { position=it.currentPosition; shouldPlay=it.playWhenReady; it.release() }
        player=null; if(::playerView.isInitialized) playerView.player=null
        super.onStop()
    }
    override fun onDestroy() { executor.shutdownNow(); player?.release(); super.onDestroy() }
}
