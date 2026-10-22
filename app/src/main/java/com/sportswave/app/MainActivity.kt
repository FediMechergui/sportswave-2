package com.sportswave.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : Activity() {
    private val bg = Color.rgb(12, 15, 20)
    private val panel = Color.rgb(25, 29, 36)
    private val muted = Color.rgb(151, 160, 174)
    private val red = Color.rgb(255, 77, 67)
    private lateinit var store: Store
    private lateinit var body: LinearLayout
    private lateinit var nav: LinearLayout
    private var tab = "Discover"
    private var sport = "All sports"
    private var query = ""
    private var day = 0L
    private var eventPageSize=40
    private var demoSchedule = false
    private var football = emptyList<FootballMatch>()
    private var scoreLeague = "All football"
    private var footballDate=LocalDate.now()
    private var calendarEvents=emptyList<Event>()
    private var renderSources=emptyList<Source>()
    private var calendarDate=LocalDate.now()
    private var calendarLoading=false
    private var calendarError=""
    private var calendarGeneration=0
    private var footballError = ""
    private var footballLoading = false
    private var footballUpdated = ""
    private var race: RaceResult? = null
    private var raceError = ""
    private var raceLoading = false
    private var liveSport = "Football"
    private val imageWorkers = java.util.concurrent.Executors.newFixedThreadPool(3)
    private val crestCache = android.util.LruCache<String, android.graphics.Bitmap>(48)
    private val missingCrests = mutableSetOf<String>()
    private var channelPackages = emptyList<ChannelPackage>()
    private var providerPackageId = ""
    private var providerChannels = emptyList<Source>()
    private var providerLoading = false
    private var providerError = ""
    @Volatile private var providerGeneration = 0
    private var providerPageSize = 40
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshLive = object : Runnable {
        override fun run() { if(tab == "Live") { if(liveSport == "Football") loadFootball() else loadRace() }; if(tab in listOf("Schedule","Discover","Saved","Live") && !demoSchedule) loadCalendar(true); handler.postDelayed(this,30_000) }
    }
    private val bitmaps = mutableMapOf<String, Drawable?>()
    private val today get() = LocalDate.now()
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        providerPackageId=""
        channelPackages=store.providerPackages()
        providerChannels=ChannelCatalog.sorted(store.providerSources())
        tab = savedInstanceState?.getString("tab") ?: tab
        sport = savedInstanceState?.getString("sport") ?: sport
        day = savedInstanceState?.getLong("day") ?: 0
        query = savedInstanceState?.getString("query") ?: ""
        demoSchedule = false
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        render()
        loadFootball()
        loadCalendar()
        loadYacine()
    }
    override fun onResume() { super.onResume(); handler.postDelayed(refreshLive,30_000) }
    override fun onPause() { handler.removeCallbacks(refreshLive); super.onPause() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); imageWorkers.shutdownNow(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("tab", tab); outState.putString("sport", sport); outState.putLong("day", day); outState.putString("query", query)
        super.onSaveInstanceState(outState)
    }
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 16, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke != null) setStroke(dp(1), stroke)
    }
    private fun text(value: String, size: Int = 14, color: Int = Color.WHITE, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color); typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = false
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun space(parent: LinearLayout, height: Int) { parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun padded(parent: LinearLayout, child: View, top: Int = 0, bottom: Int = 0) {
        parent.addView(child, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(22), dp(top), dp(22), dp(bottom)) })
    }
    private fun button(label: String, filled: Boolean = false, action: () -> Unit): TextView = text(label, 13, if (filled) Color.WHITE else muted, true).apply {
        gravity = Gravity.CENTER; setPadding(dp(16), dp(13), dp(16), dp(13)); background = android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33FFFFFF),shape(if (filled) red else panel,12),null)
        isClickable = true; isFocusable = true; minHeight=dp(48); setOnClickListener { action() }
    }
    private fun iconButton(label: String, description: String, action: () -> Unit) = button(label, false, action).apply { contentDescription = description }
    private fun image(name: String): ImageView = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        val drawable = bitmaps.getOrPut(name) { if(name=="racing.jpg") getDrawable(R.drawable.racetrack) else runCatching { assets.open(name).use { Drawable.createFromStream(it, null) } }.getOrNull() }
        setImageDrawable(drawable?.constantState?.newDrawable())
        setBackgroundColor(panel)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private fun render(keepScroll: Boolean = false) {
        if(keepScroll && currentFocus is EditText) return
        renderSources=store.all()
        val previousY = if(keepScroll && ::body.isInitialized) (body.parent as? ScrollView)?.scrollY ?: 0 else 0
        val root = column().apply { setBackgroundColor(bg); isFocusableInTouchMode=true }
        root.setOnApplyWindowInsetsListener { view, insets ->
            if(android.os.Build.VERSION.SDK_INT>=30) { val bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()); view.setPadding(bars.left,bars.top,bars.right,bars.bottom) }
            else view.setPadding(insets.systemWindowInsetLeft,insets.systemWindowInsetTop,insets.systemWindowInsetRight,insets.systemWindowInsetBottom)
            insets
        }
        setContentView(root)
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        body = column()
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        nav = row().apply { setPadding(dp(8), dp(10), dp(8), dp(10)); background = shape(bg, 0) }
        listOf("Discover", "Schedule", "Live", "Channels", "Saved").forEach { name ->
            val item = column().apply { gravity = Gravity.CENTER; setPadding(0, dp(7), 0, dp(7)); isClickable = true; isFocusable = true; contentDescription = name; isSelected = tab == name }
            item.addView(NavigationIcon(this,name,if(tab == name) red else muted),LinearLayout.LayoutParams(dp(24),dp(24)))
            item.addView(text(name, 10, if (tab == name) Color.WHITE else muted, tab == name).apply { setPadding(0, dp(5), 0, 0) })
            item.setOnClickListener { tab = name; query = ""; if(name in listOf("Discover","Live")) day=0; render(); if(name in listOf("Schedule","Discover","Saved","Live")) loadCalendar(); if(name == "Live") { if(liveSport == "Motorsport") loadRace() else loadFootball() } }
            nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        header()
        when(tab) {
            "Discover" -> discover()
            "Schedule" -> schedule()
            "Live" -> liveScores()
            "Channels" -> channels()
            "Saved" -> saved()
        }
        space(body, 24)
        if(keepScroll) scroll.post { scroll.scrollTo(0,previousY) }
    }
    private fun header() {
        val header = row()
        header.addView(ImageView(this).apply { setImageResource(R.mipmap.ic_launcher); importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO },LinearLayout.LayoutParams(dp(34),dp(34)).apply { marginEnd=dp(10) })
        header.addView(text("sportswave", 23, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(drawIcon("Add", "Add a playback source") { addSource() })
        padded(body, header, 17, 22)
    }
    private fun section(title: String, action: String = "", callback: (() -> Unit)? = null) {
        val line = row()
        line.addView(text(title, 21, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (action.isNotEmpty()) line.addView(text(action, 12, red, true).apply { setPadding(dp(8), dp(12), 0, dp(12)); isFocusable = true; setOnClickListener { callback?.invoke() } })
        padded(body, line, 22, 14)
    }
    private fun sports() {
        val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val line = row().apply { setPadding(dp(22), 0, dp(14), 0) }
        Catalog.sports.forEach { s ->
            line.addView(button(s.name, sport == s.name) { sport = s.name; render() }, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
        }
        scroll.addView(line); body.addView(scroll)
    }
    private fun discover() {
        padded(body, text(today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)).uppercase(), 11, muted, true), bottom = 11)
        padded(body, text("Every sport.\nOne place.", 37, Color.WHITE, true), bottom = 22)
        sports()
        val featured = Catalog.events.firstOrNull { sport == "All sports" || it.sport == sport } ?: Catalog.events.first()
        val hero = FrameLayout(this).apply { background = shape(panel, 20); clipToOutline = true }
        hero.addView(image(featured.image), FrameLayout.LayoutParams(-1, -1))
        hero.addView(View(this).apply { background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x05000000, 0xEE080A0D.toInt())) }, FrameLayout.LayoutParams(-1, -1))
        val overlay = column().apply { setPadding(dp(22), dp(20), dp(22), dp(20)) }
        overlay.addView(text("THE SPOTLIGHT  /  ${featured.sport.uppercase()}", 10, Color.WHITE, true))
        overlay.addView(View(this), LinearLayout.LayoutParams(1, 0, 1f))
        overlay.addView(text("Made for\nthe moments.", 32, Color.WHITE, true))
        space(overlay, 9)
        overlay.addView(text("Explore your sport. Keep the action close.", 12, 0xFFDBDFE6.toInt()))
        space(overlay, 16)
        overlay.addView(button("Explore ${if(sport == "All sports") "the schedule" else sport}  ↗", true) { tab = "Schedule"; render() })
        hero.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        body.addView(hero, LinearLayout.LayoutParams(-1, dp(318)).apply { setMargins(dp(22), dp(22), dp(22), 0) })
        section("Your next watch", "Full schedule  →") { tab = "Schedule"; render() }
        demoNotice()
        val events = eventList().filter { (sport == "All sports" || it.sport == sport) && EventSchedule.phase(it)!=AiringPhase.ENDED }.sortedBy { it.startUtc }.take(3)
        if (events.isEmpty()) empty("Nothing scheduled here yet", "Import a calendar or explore another sport.") else events.take(eventPageSize).forEach { eventRow(it) }
        if(events.size>eventPageSize) padded(body,button("Show more events (${events.size-eventPageSize})") { eventPageSize+=40; render(keepScroll=true) },bottom=12)
        section("Built for your screen")
        val note = column().apply { background = shape(panel); setPadding(dp(18), dp(18), dp(18), dp(18)) }
        note.addView(text("The player stays with you.", 17, Color.WHITE, true)); space(note, 7)
        note.addView(text("HLS, DASH and your own channels. Watch inside SportsWave, without a second app.", 13, muted))
        space(note, 14); note.addView(button("Try the built-in player  ▷", true) { play(Catalog.samples.first()) })
        padded(body, note)
    }
    private fun eventList(): List<Event> {
        if(demoSchedule) return Catalog.events
        val fixtures=if(footballDate==today.plusDays(day)) football.map { matchEvent(it) } else emptyList()
        return EventSchedule.merge(store.events()+calendarEvents+fixtures)
    }
    private fun matchEvent(m:FootballMatch)=Event("openliga-${m.id}",m.sport,m.competition,m.home,m.away,0,0,ScheduleFeeds.imageFor(m.sport),startUtc=m.start,demo=false,homeCrest=m.homeCrest,awayCrest=m.awayCrest,score=m.score,status=m.status,listingSource="Public scores")
    private fun demoNotice() {
        val checked=store.scheduleUpdated(today.plusDays(day)).takeIf { it>0 }?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).format(timeFormat) }
        padded(body,text(if(demoSchedule) "Illustrative calendar · Player tests only" else if(calendarLoading) "Refreshing broadcast calendar…" else "Broadcast calendar${checked?.let { " · Updated $it" } ?: ""}",11,muted),bottom=12)
    }
    private fun loadCalendar(force:Boolean=false) {
        val date=today.plusDays(day)
        if(calendarLoading && calendarDate==date) return
        if(!force && calendarDate==date && calendarEvents.isNotEmpty() && System.currentTimeMillis()-store.scheduleUpdated(date)<120_000) return
        val request=++calendarGeneration; calendarDate=date; calendarLoading=true; calendarError=""
        val cached=store.schedule(date); calendarEvents=cached
        val endpoint=store.providerEndpoint()
        Thread {
            val result=runCatching { ScheduleFeeds.load(date,endpoint) }
            runOnUiThread {
                if(isDestroyed || request!=calendarGeneration) return@runOnUiThread
                calendarLoading=false
                result.onSuccess { loaded ->
                    val retained=cached.filter { (it.listingSource=="beIN MENA guide" && "beIN guide" in loaded.errors) || (it.listingSource=="Yacine calendar" && "Yacine calendar" in loaded.errors) }
                    calendarEvents=EventSchedule.merge(loaded.events+retained)
                    if(loaded.errors.size<2) store.cacheSchedule(date,calendarEvents)
                    calendarError=if(loaded.errors.isEmpty()) "" else "${loaded.errors.joinToString(" and ")} unavailable.${if(retained.isNotEmpty()) " Showing saved listings for that provider." else ""}"
                }.onFailure { calendarError="Calendar unavailable. Showing saved listings." }
                if(tab in listOf("Schedule","Discover","Saved","Live") && !demoSchedule) render(keepScroll=true)
            }
        }.start()
    }
    private fun search(hint: String, refresh: () -> Unit) {
        val field = EditText(this).apply {
            setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(muted); textSize = 14f; this.hint = hint
            background = shape(panel, 13); setPadding(dp(16), dp(11), dp(16), dp(11)); setText(query)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
        }
        padded(body, field, bottom = 18)
        field.setOnEditorActionListener { _, _, _ -> query = field.text.toString(); eventPageSize=40; field.clearFocus(); (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(field.windowToken, 0); refresh(); true }
        // Keep keyboard input smooth; apply queries using the keyboard Search action.
        field.addTextChangedListener(object : TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {} ; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { query = s.toString() }; override fun afterTextChanged(s: Editable?) {} })
    }
    private fun schedule() {
        padded(body, text("The schedule", 32, Color.WHITE, true), bottom = 8)
        padded(body, text("Matches, races and sessions. Tap an airing event to watch.", 13, muted), bottom = 21)
        search("Search teams, events or competitions") { render() }
        sports()
        val dates = row()
        for (offset in -1L..5L) {
            val date = today.plusDays(offset)
            val label = if(offset == 0L) "Today\n${date.dayOfMonth}" else date.format(DateTimeFormatter.ofPattern("EEE\ndd", Locale.ENGLISH))
            dates.addView(button(label, day == offset) { day = offset; eventPageSize=40; demoSchedule=false; loadCalendar(); loadFootball(); render() }.apply { textSize=12f; setPadding(dp(4),dp(8),dp(4),dp(8)) }, LinearLayout.LayoutParams(0, dp(63), 1f).apply { marginEnd = dp(5) })
        }
        padded(body, dates, 20, 10)
        padded(body,button(today.plusDays(day).format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy"))+" · Change date") {
            val selected=today.plusDays(day)
            android.app.DatePickerDialog(this,{ _,year,month,date -> day=java.time.temporal.ChronoUnit.DAYS.between(today,LocalDate.of(year,month+1,date)); demoSchedule=false; loadCalendar(); loadFootball(); render() },selected.year,selected.monthValue-1,selected.dayOfMonth).show()
        },bottom=12)
        val tools = row()
        tools.addView(button("Refresh calendar") { loadCalendar(true); loadFootball(); render(keepScroll=true) }, LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd = dp(8) })
        tools.addView(button("Import calendar") { importCatalog() }, LinearLayout.LayoutParams(0,-2,1f))
        padded(body, tools, bottom = 18)
        demoNotice()
        val selected = today.plusDays(day)
        if(!demoSchedule && calendarError.isNotBlank()) padded(body,text(calendarError,12,red),bottom=12)
        if(!demoSchedule && footballLoading && football.isEmpty()) padded(body,text("Loading provider fixtures…",12,muted),bottom=12)
        if(!demoSchedule && footballError.isNotBlank()) padded(body,button("Fixture feed unavailable · Retry") { loadFootball() },bottom=12)
        val events = eventList().filter { it.kickoff(today).toLocalDate() == selected && (sport == "All sports" || it.sport == sport) && "${it.home} ${it.away} ${it.competition} ${it.sport}".contains(query.trim(),true) }.sortedBy { it.kickoff(today) }
        if (events.isEmpty()) empty(if(calendarLoading) "Loading the calendar" else "No events for this selection", "Try another date or sport. Listings appear when a broadcaster publishes them.") else events.take(eventPageSize).forEach { eventRow(it) }
        if(events.size>eventPageSize) padded(body,button("Show more events (${events.size-eventPageSize})") { eventPageSize+=40; render(keepScroll=true) },bottom=12)
        padded(body, text("${java.time.ZoneId.systemDefault().id} · Calendar and broadcast availability vary by region.", 11, muted), 17)
        padded(body, button(if(demoSchedule) "Show provider calendar" else "Explore illustrative calendar") { demoSchedule = !demoSchedule; render() }, 16)
    }
    private fun drawIcon(name:String,description:String,active:Boolean=false,action:()->Unit):FrameLayout=FrameLayout(this).apply {
        contentDescription=description; isClickable=true; isFocusable=true; minimumWidth=dp(48); minimumHeight=dp(48)
        background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x33FFFFFF),shape(panel,12),null)
        addView(NavigationIcon(this@MainActivity,name,if(active) red else muted),FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER))
        setOnClickListener { action() }
    }
    private fun eventRow(event: Event) {
        val matches=BroadcastRouting.matches(event,renderSources)
        val phase=EventSchedule.phase(event)
        val watchable=!event.demo && phase==AiringPhase.AIRING && matches.isNotEmpty()
        val card=column().apply {
            background=android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x22FFFFFF),shape(panel,15),null)
            setPadding(dp(16),dp(14),dp(12),dp(14)); isClickable=true; isFocusable=true
            setOnClickListener { openEvent(event) }; setOnLongClickListener { eventDetail(event); true }
            contentDescription="${event.home} ${event.away}, ${event.sport}, ${if(watchable) "Watch now" else phase.name.lowercase()}"
        }
        val heading=row()
        val timing=when { event.demo -> "Sample"; phase==AiringPhase.AIRING -> "On air"; phase==AiringPhase.ENDED -> "Ended"; else -> event.kickoff(today).format(timeFormat) }
        heading.addView(text("${event.sport} · $timing",11,if(phase==AiringPhase.AIRING) red else muted,true),LinearLayout.LayoutParams(0,-2,1f))
        heading.addView(drawIcon("Saved","Save ${event.home}",store.saved().contains(event.id)) { store.toggleEvent(event); render(keepScroll=true) }.apply { isSelected=store.saved().contains(event.id); alpha=if(isSelected) 1f else .65f },LinearLayout.LayoutParams(dp(44),dp(44)))
        card.addView(heading)
        listOf(event.home to event.homeCrest,event.away to event.awayCrest).filter { it.first.isNotBlank() }.forEach { (name,url) ->
            val participant=row()
            if(event.away.isNotBlank()) participant.addView(crest(url,name),LinearLayout.LayoutParams(dp(30),dp(30)).apply { marginEnd=dp(10) })
            participant.addView(text(name,if(event.away.isBlank()) 17 else 15,Color.WHITE,true),LinearLayout.LayoutParams(0,-2,1f))
            card.addView(participant); space(card,7)
        }
        if(event.score.isNotBlank() && event.score!="—") { card.addView(text(event.score,19,Color.WHITE,true).apply { fontFeatureSettings="tnum" }); space(card,8) }
        card.addView(text(event.competition,11,muted)); space(card,10)
        val names=ChannelCatalog.unique(matches).map { it.name }
        card.addView(text(when { watchable -> "Watch now · ${names.first()}${if(names.size>1) " +${names.size-1}" else ""}"; names.isNotEmpty() -> "${names.first()} · ${if(phase==AiringPhase.ENDED) "Broadcast ended" else "Starts ${event.kickoff(today).format(timeFormat)}"}"; event.broadcasts.isNotEmpty() -> "${event.broadcasts.first().channelName} · Channel not in catalog"; else -> "Broadcast not listed" },12,if(watchable) red else muted,watchable))
        padded(body,card,bottom=10)
    }
    private fun openEvent(event:Event) {
        val matches=BroadcastRouting.matches(event,store.all())
        if(!event.demo && EventSchedule.phase(event)==AiringPhase.AIRING && matches.isNotEmpty()) play(matches.first(),eventAlternatives(matches),event)
        else eventDetail(event)
    }
    private fun eventAlternatives(matches:List<Source>):List<Source> = matches.flatMap { source -> if(source.integration=="yacine") ChannelCatalog.alternatives(source,store.providerSources()) else listOf(source) }.distinctBy { it.id }
    private fun eventDetail(event: Event) {
        val content=column().apply { setPadding(dp(22),dp(12),dp(22),dp(12)) }
        content.addView(text(event.sport,11,red,true)); space(content,14)
        content.addView(text(event.home+if(event.away.isNotBlank()) "\nvs ${event.away}" else "",24,Color.WHITE,true)); space(content,12)
        content.addView(text(event.competition,13,muted)); space(content,8)
        content.addView(text(event.kickoff(today).format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm z")),13,muted)); space(content,12)
        val matches=BroadcastRouting.matches(event,store.all()); val phase=EventSchedule.phase(event)
        val message=when { event.demo -> "Illustrative event. Player tests are separate videos."; phase==AiringPhase.UPCOMING -> "Starts at ${event.kickoff(today).format(timeFormat)}. Open this event when the broadcast begins."; phase==AiringPhase.ENDED -> "This broadcast window has ended."; matches.isEmpty() -> if(event.broadcasts.isEmpty()) "The broadcaster has not listed a channel for this event." else "The listed channel is not available in your channel catalog. Refresh Channels to check again."; else -> "Watch on ${ChannelCatalog.unique(matches).joinToString { it.name }}." }
        content.addView(text(message,13,muted)); space(content,12)
        if(event.listingSource.isNotBlank()) content.addView(text("Listing: ${event.listingSource}",11,muted))
        if(event.description.isNotBlank() && event.description!=event.home) { space(content,10); content.addView(text(event.description,12,muted)) }
        val scroll=ScrollView(this).apply { addView(content) }
        val dialog=AlertDialog.Builder(this).setView(scroll).setNegativeButton("Close",null).create()
        if(!event.demo && phase==AiringPhase.AIRING && matches.isNotEmpty()) {
            space(content,18); content.addView(button("Watch now",true) { dialog.dismiss(); play(matches.first(),eventAlternatives(matches),event) })
            if(ChannelCatalog.unique(matches).size>1) { space(content,8); content.addView(button("Choose broadcast") { dialog.dismiss(); chooseBroadcast(matches) }) }
        }
        dialog.show()
    }
    private fun channels() {
        padded(body, text("Your channels",32,Color.WHITE,true), bottom=8)
        padded(body,text("A direct line to the sports you love.",13,muted),bottom=21)
        search("Search channels") { render() }
        sports()
        val tools = row()
        tools.addView(button("＋ Add source",true) { addSource() },LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=dp(8) })
        tools.addView(button("Import catalog") { importCatalog() },LinearLayout.LayoutParams(0,-2,1f))
        padded(body,tools,20)
        val included=Catalog.channels.filter { (sport=="All sports" || it.sport==sport || it.sport=="Multi-sport") && it.name.contains(query,true) }
        if(included.isNotEmpty()) {
            section("Included live channels")
            included.forEach { sourceRow(it) }
            if(included.any { it.id=="bein-xtra-us" }) padded(body,text("beIN SPORTS XTRA is the English US feed, with its own programming. Availability varies by region.",12,muted),bottom=12)
        }
        section("Yacine channels",if(providerLoading) "Loading…" else "${providerChannels.size} channels")
        val providerTools=row()
        providerTools.addView(button(channelPackages.firstOrNull { it.id==providerPackageId }?.name ?: "All channels - sports first") { choosePackage(channelPackages) },LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=dp(8) })
        providerTools.addView(button("Refresh") { loadYacine(true); render() },LinearLayout.LayoutParams(-2,-2))
        padded(body,providerTools,bottom=12)
        if(providerError.isNotBlank()) padded(body,text(providerError,12,red),bottom=12)
        if(providerLoading && providerChannels.isEmpty()) padded(body,text("Loading the provider's channel catalog…",12,muted),bottom=12)
        val visible=ChannelCatalog.unique(providerChannels.filter { (providerPackageId.isBlank() || it.catalogCategoryId==providerPackageId) && (sport=="All sports" || it.sport==sport || it.sport=="Multi-sport") && (it.name+" "+it.catalogCategory).contains(query,true) })
        padded(body,text("${visible.size} channels in this view - ${channelPackages.size} packages. Sports first; quality variants are available in the player.",11,muted),bottom=12)
        visible.take(providerPageSize).forEach { sourceRow(it) }
        if(visible.size>providerPageSize) padded(body,button("Show more channels (${visible.size-providerPageSize})") { providerPageSize+=40; render(keepScroll=true) },bottom=12)
        if(!providerLoading && providerChannels.isEmpty()) padded(body,text("No channels loaded. Refresh the catalog or check the service address.",12,muted),bottom=10)
        padded(body,button("Service address") { editServiceAddress() },bottom=14)
        section("Your collection", "${store.custom().size} sources")
        val sources = store.custom().filter { (sport=="All sports" || it.sport==sport || it.sport=="Multi-sport") && it.name.contains(query,true) }
        if(sources.isEmpty()) empty("Make room for your favorites", "Add an HTTPS channel URL or import a provider catalog. Your sources play here, inside the app.") else sources.forEach { sourceRow(it) }
        section("Player test streams")
        padded(body,text("Public sample videos for testing HLS and DASH. These are not live sports channels.",12,muted),bottom=14)
        Catalog.samples.filter { it.name.contains(query,true) }.forEach { sourceRow(it) }
    }
    private fun sourceRow(source: Source) {
        val card = row().apply { background=shape(panel,15); setPadding(dp(16),dp(17),dp(10),dp(17)); isFocusable=true; setOnClickListener { play(source,if(source.integration=="yacine") ChannelCatalog.alternatives(source,store.providerSources()) else listOf(source)) } }
        if(source.logoUrl.isNotBlank()) card.addView(crest(source.logoUrl,source.name).apply { contentDescription="${source.name} logo" },LinearLayout.LayoutParams(dp(34),dp(34)).apply { marginEnd=dp(8) })
        else card.addView(NavigationIcon(this,"Channels",red),LinearLayout.LayoutParams(dp(28),dp(28)).apply { marginEnd=dp(14) })
        val info=column(); info.addView(text(source.name,15,Color.WHITE,true)); space(info,7)
        info.addView(text(if(source.demo) "DEMO VIDEO · ${if(source.id.contains("hls")) "HLS" else "DASH"}" else if(source.integration=="yacine") "${source.catalogCategory.ifBlank { source.provider }} - Watch channel" else if(source.sessionSnapshot) "${source.provider} · Temporary playback session" else "${source.provider.ifBlank { source.sport }} · ${if(source.resolverUrl.isNotBlank()) "Provider playback" else "Direct stream"}",10,muted))
        card.addView(info,LinearLayout.LayoutParams(0,-2,1f))
        if(!source.demo && source.integration.isBlank() && Catalog.channels.none { it.id == source.id }) card.addView(iconButton("×","Remove ${source.name}") { AlertDialog.Builder(this).setTitle("Remove channel?").setMessage(source.name).setNegativeButton("Cancel",null).setPositiveButton("Remove") { _,_ -> store.remove(source.id); render() }.show() })
        padded(body,card,bottom=9)
    }
    private fun saved() {
        padded(body,text("Worth coming back for.",30,Color.WHITE,true),bottom=10)
        padded(body,text("Your saved events, all in one place.",13,muted),bottom=22)
        val selected=(eventList()+store.events()+Catalog.events+store.savedEvents()).distinctBy { it.id }.filter { store.saved().contains(it.id) }
        if(selected.isEmpty()) empty("Your front-row collection", "Tap the heart on any event to save it here.") else selected.forEach { eventRow(it) }
    }
    private fun empty(title: String, message: String) {
        val box=column().apply { setPadding(dp(21),dp(26),dp(21),dp(26)); background=shape(panel,16) }
        box.addView(text(title,17,Color.WHITE,true)); space(box,9); box.addView(text(message,13,muted))
        padded(body,box,bottom=10)
    }
    private fun addSource() {
        val content=column().apply { setPadding(dp(24),dp(8),dp(24),0) }
        fun field(hint:String)=EditText(this).apply { this.hint=hint; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(muted); textSize=14f; content.addView(this) }
        val name=field("Channel name")
        val url=field("https://provider.example/live.m3u8")
        url.inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
        val spinner=Spinner(this)
        val choices=Catalog.sports.drop(1).map { it.name }
        spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,choices)
        spinner.setSelection(choices.indexOf("Other")); content.addView(spinner)
        val error=text("",12,red); content.addView(error)
        val dialog=AlertDialog.Builder(this).setTitle("Add a channel").setView(content).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if(name.text.isBlank() || !Catalog.validUrl(url.text.toString().trim())) error.text="Enter a name and a valid HTTPS playback URL."
            else { val value=url.text.toString().trim(); store.put(Source("custom-${value.hashCode()}",name.text.toString().trim(),choices[spinner.selectedItemPosition],value)); dialog.dismiss(); tab="Channels"; sport="All sports"; render() }
        } }
        dialog.show()
    }
    private fun importCatalog() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type="*/*"; putExtra(Intent.EXTRA_MIME_TYPES,arrayOf("application/json","text/plain")) },41)
    }
    @Deprecated("Activity result API used without external activity dependency")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode!=41 || resultCode!=RESULT_OK) return
        val uri=data?.data ?: return
        Thread {
            val result=runCatching {
                val bytes=contentResolver.openInputStream(uri)!!.use { input -> val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192); while(true) { val count=input.read(buffer); if(count<0) break; out.write(buffer,0,count); require(out.size()<=2_000_000) { "Catalog files must be smaller than 2 MB." } }; out.toByteArray() }
                store.import(bytes.toString(Charsets.UTF_8))
            }
            runOnUiThread { if(!isFinishing && !isDestroyed) { result.onSuccess { count -> demoSchedule=false; Toast.makeText(this,"Imported $count items",Toast.LENGTH_LONG).show(); render() }.onFailure { AlertDialog.Builder(this).setTitle("Couldn't import catalog").setMessage(it.message ?: "Check the JSON format.").setPositiveButton("OK",null).show() } } }
        }.start()
    }
    private fun loadYacine(force: Boolean = false) {
        if(!force && providerChannels.isNotEmpty() && channelPackages.isNotEmpty() && System.currentTimeMillis()-store.catalogSyncedAt()<3_600_000) return
        val request=++providerGeneration
        val endpoint=store.providerEndpoint()
        providerLoading=true; providerError=""
        Thread {
            var processed=0
            val result=runCatching {
                YacineApi.syncCatalog(endpoint) { category,sources ->
                    if(request!=providerGeneration || isDestroyed) throw java.util.concurrent.CancellationException("Catalog refresh superseded.")
                    store.cacheProviderChannels(category.id,sources); processed++
                    if(processed<=2 || processed%4==0) runOnUiThread { if(!isDestroyed && request==providerGeneration) {
                        providerChannels=ChannelCatalog.sorted(store.providerSources())
                        if(tab=="Channels") render(keepScroll=true)
                    } }
                }.also { store.cacheProviderPackages(it.packages,it.failed.isEmpty()) }
            }
            runOnUiThread { if(!isDestroyed && request==providerGeneration) {
                providerLoading=false
                providerChannels=ChannelCatalog.sorted(store.providerSources()); channelPackages=store.providerPackages()
                result.onSuccess { if(it.failed.isNotEmpty()) providerError="${it.failed.size} packages could not refresh. Cached channels remain available; retry Refresh." }
                    .onFailure { providerError="Couldn't refresh the catalog. Cached channels remain available. Retry or check the service address." }
                if(tab=="Channels") render(keepScroll=true)
            } }
        }.start()
    }
    private fun choosePackage(packages: List<ChannelPackage>) {
        if(packages.isEmpty()) { loadYacine(true); render(); return }
        AlertDialog.Builder(this).setTitle("Channel package").setItems((listOf("All channels - sports first")+packages.map { it.name }).toTypedArray()) { _,i ->
            providerPackageId=if(i==0) "" else packages[i-1].id
            providerPageSize=40; render()
        }.setNegativeButton("Close",null).show()
    }
    private fun editServiceAddress() {
        val field=EditText(this).apply { setSingleLine(true); setText(store.providerEndpoint()); inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI }
        val dialog=AlertDialog.Builder(this).setTitle("Yacine service address").setMessage("The provider can change its API host. Enter the current base address ending in /api/.").setView(field).setNegativeButton("Cancel",null).setPositiveButton("Save",null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val value=field.text.toString().trim()
            if(!YacineApi.validEndpoint(value)) field.error="Enter an HTTP(S) address ending in /api/, without credentials or query parameters."
            else { providerGeneration++; store.setProviderEndpoint(value); providerChannels=emptyList(); channelPackages=emptyList(); dialog.dismiss(); loadYacine(true); render() }
        } }
        dialog.show()
    }
    private fun crest(url:String,name:String): FrameLayout {
        val box=FrameLayout(this).apply { contentDescription="$name crest" }
        val initials=text(name.split(" ").take(2).mapNotNull { it.firstOrNull() }.joinToString(""),10,muted,true).apply { gravity=Gravity.CENTER }
        box.addView(initials,FrameLayout.LayoutParams(-1,-1))
        val image=ImageView(this).apply { scaleType=ImageView.ScaleType.FIT_CENTER; importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO }
        box.addView(image,FrameLayout.LayoutParams(-1,-1))
        fun show(bitmap:android.graphics.Bitmap) { image.setImageBitmap(bitmap); initials.visibility=View.GONE }
        val cached=crestCache.get(url)
        if(cached!=null) show(cached)
        else if(Catalog.validUrl(url) && !missingCrests.contains(url)) imageWorkers.execute {
            val bitmap=runCatching {
                val connection=java.net.URL(url).openConnection() as javax.net.ssl.HttpsURLConnection
                connection.connectTimeout=8000; connection.readTimeout=8000
                try {
                    check(connection.responseCode in 200..299)
                    val bytes=connection.inputStream.use { input ->
                        val out=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
                        while(true) { val n=input.read(buffer); if(n<0) break; out.write(buffer,0,n); check(out.size()<=1_000_000) }; out.toByteArray()
                    }
                    val bounds=android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds=true }
                    android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
                    val options=android.graphics.BitmapFactory.Options().apply { inSampleSize=(maxOf(bounds.outWidth,bounds.outHeight)/128).coerceAtLeast(1) }
                    android.graphics.BitmapFactory.decodeByteArray(bytes,0,bytes.size,options) ?: run {
                        val svg=com.caverock.androidsvg.SVG.getFromString(bytes.toString(Charsets.UTF_8))
                        svg.setDocumentWidth(128f); svg.setDocumentHeight(128f)
                        android.graphics.Bitmap.createBitmap(128,128,android.graphics.Bitmap.Config.ARGB_8888).also { svg.renderToCanvas(android.graphics.Canvas(it)) }
                    }
                } finally { connection.disconnect() }
            }.getOrNull()
            runOnUiThread { if(!isDestroyed) { if(bitmap!=null) { crestCache.put(url,bitmap); show(bitmap) } else missingCrests.add(url) } }
        }
        return box
    }
    private fun loadFootball() {
        if(footballLoading) return
        footballLoading=true
        val requestedLeague=scoreLeague
        val requestedDate=if(tab=="Schedule") today.plusDays(day) else today
        Thread {
            val result=runCatching { SportsFeeds.scoreboard(requestedLeague,requestedDate) }
            runOnUiThread { if(!isDestroyed) {
                footballLoading=false
                if(requestedLeague!=scoreLeague || requestedDate!=(if(tab=="Schedule") today.plusDays(day) else today)) { loadFootball(); return@runOnUiThread }
                result.onSuccess { football=it; footballDate=requestedDate; footballError=""; footballUpdated=java.time.LocalTime.now().format(timeFormat) }.onFailure { footballError="Scores unavailable. Retry shortly." }
                if(tab=="Live" || (tab=="Schedule" && !demoSchedule)) render(keepScroll=true)
            } }
        }.start()
    }
    private fun loadRace() {
        if(raceLoading) return
        raceLoading=true
        Thread {
            val result=runCatching { SportsFeeds.race() }
            runOnUiThread { if(!isDestroyed) {
                raceLoading=false
                result.onSuccess { race=it; raceError="" }.onFailure { raceError="Race results unavailable. The provider may still be processing this session." }
                if(tab=="Live") render(keepScroll=true)
            } }
        }.start()
    }
    private fun liveScores() {
        padded(body,text("Scores & race results",28,Color.WHITE,true),bottom=10)
        padded(body,text("Public scores, team crests and Formula 1 classifications.",13,muted),bottom=18)
        val tabs=row()
        listOf("Football","Motorsport").forEach { name -> tabs.addView(button(if(name=="Football") "Matches" else name,liveSport==name) { liveSport=name; render(); if(name=="Motorsport") loadRace() else loadFootball() },LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=dp(8) }) }
        padded(body,tabs,bottom=16)
        if(liveSport=="Football") {
            padded(body,button("$scoreLeague · Change competition") {
                val choices=SportsFeeds.leagues.keys.toTypedArray()
                AlertDialog.Builder(this).setTitle("Score competition").setItems(choices) { _,i -> scoreLeague=choices[i]; football=emptyList(); footballError=""; loadFootball(); render() }.setNegativeButton("Cancel",null).show()
            },bottom=12)
            padded(body,text("$scoreLeague · ${if(scoreLeague=="Bundesliga") "OpenLigaDB" else "ESPN"} · ${if(footballLoading) "Refreshing…" else "Checked $footballUpdated"}",11,muted),bottom=12)
            padded(body,button("Refresh scores") { loadFootball(); render() },bottom=12)
            if(footballError.isNotBlank()) padded(body,text("$footballError${if(football.isNotEmpty()) " Showing the last fetched scores." else ""}",13,red),bottom=12)
            if(football.isEmpty()) empty(if(footballLoading) "Loading the current matchday" else "No matches available","The provider may have no games for its current date. Try another competition.")
            football.forEach { m ->
                val event=EventSchedule.enrich(matchEvent(m),calendarEvents+store.events())
                val box=column().apply { background=shape(panel,14); setPadding(dp(16),dp(16),dp(16),dp(16)); isFocusable=true; setOnClickListener { openEvent(event) } }
                val local=java.time.Instant.parse(m.start).atZone(java.time.ZoneId.systemDefault())
                box.addView(text("${local.format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm"))} · ${m.status}",10,muted)); space(box,14)
                val teams=row()
                val names=column()
                listOf(m.home to m.homeCrest,m.away to m.awayCrest).forEach { (name,url) -> val team=row(); team.addView(crest(url,name),LinearLayout.LayoutParams(dp(28),dp(28))); team.addView(text(name,13,Color.WHITE,true).apply { setPadding(dp(10),0,0,0) }); names.addView(team); space(names,8) }
                teams.addView(names,LinearLayout.LayoutParams(0,-2,1f))
                teams.addView(text(m.score,20,Color.WHITE,true)); box.addView(teams)
                if(m.scoreDetail.isNotBlank()) { space(box,10); box.addView(text(m.scoreDetail,12,muted)) }
                val matches=BroadcastRouting.matches(event,renderSources)
                if(matches.isNotEmpty()) { space(box,10); box.addView(text(if(EventSchedule.phase(event)==AiringPhase.AIRING) "Watch now · ${matches.first().name}" else "Broadcast · ${matches.first().name}",12,red,true)) }
                padded(body,box,bottom=10)
            }
            padded(body,text("Updates every 30 seconds while this screen is open. Community data may be delayed; no match clock is inferred.",11,muted),top=8)
        } else {
            padded(body,text("Formula 1 · OpenF1 · Latest available classification",11,muted),bottom=12)
            padded(body,button(if(raceLoading) "Refreshing results…" else "Refresh classification") { loadRace(); render() },bottom=12)
            padded(body,text("Public access provides session results. Live positions and timing are not connected; these rows are not a live leaderboard.",12,muted),bottom=16)
            if(raceError.isNotBlank()) padded(body,text(raceError,13,red),bottom=12)
            val data=race
            if(data==null) empty(if(raceLoading) "Loading race classification" else "No classification loaded","Select Refresh to load the latest available session results.")
            else {
                section(data.title)
                padded(body,text(java.time.OffsetDateTime.parse(data.date).atZoneSameInstant(java.time.ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm")),12,muted),bottom=16)
                if(data.rows.isEmpty()) empty("Results not published yet","Refresh once the provider has published this session's classification.")
                data.rows.forEach { r ->
                    val line=row().apply { setPadding(dp(14),dp(14),dp(14),dp(14)); background=shape(panel,10) }
                    line.addView(text(if(r.position==99) "—" else r.position.toString(),20,red,true),LinearLayout.LayoutParams(dp(34),-2))
                    val driver=column(); driver.addView(text(r.driver,14,Color.WHITE,true)); space(driver,5); driver.addView(text(r.team,11,muted))
                    line.addView(driver,LinearLayout.LayoutParams(0,-2,1f))
                    val timing=column().apply { gravity=Gravity.END }; timing.addView(text(r.gap,12,Color.WHITE,true).apply { maxLines=2; ellipsize=android.text.TextUtils.TruncateAt.END }); space(timing,5); timing.addView(text("${r.laps} laps",10,muted)); line.addView(timing,LinearLayout.LayoutParams(dp(84),-2))
                    padded(body,line,bottom=7)
                }
            }
        }
    }
    private fun openGuide() { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.beinsports.com/en-mena/tv-guide"))) }
    private fun chooseBroadcast(sources:List<Source>) { AlertDialog.Builder(this).setTitle("Available broadcasts").setItems(sources.map { "${it.provider.ifBlank { "Channel" }} · ${it.name}" }.toTypedArray()) { _,index -> play(sources[index],sources) }.setNegativeButton("Cancel",null).show() }
    private fun play(source:Source,alternatives:List<Source> = listOf(source),event:Event?=null) { startActivity(Intent(this,PlayerActivity::class.java).putExtra("sourceId",source.id).putExtra("alternatives",alternatives.map { it.id }.toTypedArray()).putExtra("eventTitle",event?.let { it.home+if(it.away.isNotBlank()) " vs ${it.away}" else "" })) }
}
