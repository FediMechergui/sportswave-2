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
    private var demoSchedule = true
    private val bitmaps = mutableMapOf<String, Drawable?>()
    private val today get() = LocalDate.now()
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        tab = savedInstanceState?.getString("tab") ?: tab
        sport = savedInstanceState?.getString("sport") ?: sport
        day = savedInstanceState?.getLong("day") ?: 0
        query = savedInstanceState?.getString("query") ?: ""
        demoSchedule = store.events().isEmpty()
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        render()
    }
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
        gravity = Gravity.CENTER; setPadding(dp(16), dp(13), dp(16), dp(13)); background = shape(if (filled) red else panel, 12)
        isClickable = true; isFocusable = true; setOnClickListener { action() }
    }
    private fun iconButton(label: String, description: String, action: () -> Unit) = button(label, false, action).apply { contentDescription = description }
    private fun image(name: String): ImageView = ImageView(this).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        val drawable = bitmaps.getOrPut(name) { if(name=="racing.jpg") getDrawable(R.drawable.racetrack) else runCatching { assets.open(name).use { Drawable.createFromStream(it, null) } }.getOrNull() }
        setImageDrawable(drawable?.constantState?.newDrawable())
        setBackgroundColor(panel)
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }
    private fun render() {
        val root = column().apply { setBackgroundColor(bg) }
        root.setOnApplyWindowInsetsListener { view, insets -> view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom); insets }
        setContentView(root)
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        body = column()
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        nav = row().apply { setPadding(dp(8), dp(10), dp(8), dp(10)); background = shape(bg, 0) }
        listOf("Discover" to "⌂", "Schedule" to "▦", "Channels" to "▷", "Saved" to "♡").forEach { (name, icon) ->
            val item = column().apply { gravity = Gravity.CENTER; setPadding(0, dp(7), 0, dp(7)); isClickable = true; isFocusable = true; contentDescription = name }
            item.addView(text(icon, 22, if (tab == name) red else muted, true))
            item.addView(text(name, 10, if (tab == name) Color.WHITE else muted, tab == name).apply { setPadding(0, dp(5), 0, 0) })
            item.setOnClickListener { tab = name; query = ""; render() }
            nav.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        root.addView(nav)
        header()
        when(tab) {
            "Discover" -> discover()
            "Schedule" -> schedule()
            "Channels" -> channels()
            "Saved" -> saved()
        }
        space(body, 24)
    }
    private fun header() {
        val header = row()
        header.addView(text("≋", 35, red, true).apply { setPadding(0, 0, dp(9), 0) })
        header.addView(text("sportswave", 23, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(iconButton("＋", "Add a playback source") { addSource() })
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
            line.addView(button("${s.symbol}  ${s.name}", sport == s.name) { sport = s.name; render() }, LinearLayout.LayoutParams(-2, -2).apply { marginEnd = dp(8) })
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
        val events = eventList().filter { sport == "All sports" || it.sport == sport }.take(3)
        if (events.isEmpty()) empty("Nothing scheduled here yet", "Import a calendar or explore another sport.") else events.forEach { eventRow(it) }
        section("Built for your screen")
        val note = column().apply { background = shape(panel); setPadding(dp(18), dp(18), dp(18), dp(18)) }
        note.addView(text("The player stays with you.", 17, Color.WHITE, true)); space(note, 7)
        note.addView(text("HLS, DASH and your own channels. Watch inside SportsWave, without a second app.", 13, muted))
        space(note, 14); note.addView(button("Try the built-in player  ▷", true) { play(Catalog.samples.first()) })
        padded(body, note)
    }
    private fun eventList() = if (demoSchedule) Catalog.events else store.events()
    private fun demoNotice() {
        padded(body, text(if (demoSchedule) "DEMO SCHEDULE · Illustrative events, not live listings" else "IMPORTED CALENDAR · Times shown on your device", 10, muted, true), bottom = 12)
    }
    private fun search(hint: String, refresh: () -> Unit) {
        val field = EditText(this).apply {
            setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(muted); textSize = 14f; this.hint = hint
            background = shape(panel, 13); setPadding(dp(16), dp(11), dp(16), dp(11)); setText(query)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
        }
        padded(body, field, bottom = 18)
        field.setOnEditorActionListener { _, _, _ -> query = field.text.toString(); (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(field.windowToken, 0); refresh(); true }
        // Keep keyboard input smooth; apply queries using the keyboard Search action.
        field.addTextChangedListener(object : TextWatcher { override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {} ; override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { query = s.toString() }; override fun afterTextChanged(s: Editable?) {} })
    }
    private fun schedule() {
        padded(body, text("The schedule", 32, Color.WHITE, true), bottom = 8)
        padded(body, text("Find your next can't-miss moment.", 13, muted), bottom = 21)
        search("Search teams, events or competitions") { render() }
        sports()
        val dates = row()
        for (offset in -1L..3L) {
            val date = today.plusDays(offset)
            val label = if(offset == 0L) "Today\n${date.dayOfMonth}" else date.format(DateTimeFormatter.ofPattern("EEE\ndd", Locale.ENGLISH))
            dates.addView(button(label, day == offset) { day = offset; render() }.apply { textSize=12f; setPadding(dp(4),dp(8),dp(4),dp(8)) }, LinearLayout.LayoutParams(0, dp(63), 1f).apply { marginEnd = dp(5) })
        }
        padded(body, dates, 20, 18)
        val tools = row()
        tools.addView(button("beIN TV guide ↗") { openGuide() }, LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd = dp(8) })
        tools.addView(button("Import calendar") { importCatalog() }, LinearLayout.LayoutParams(0,-2,1f))
        padded(body, tools, bottom = 18)
        demoNotice()
        val selected = today.plusDays(day)
        val events = eventList().filter { it.kickoff(today).toLocalDate() == selected && (sport == "All sports" || it.sport == sport) && "${it.home} ${it.away} ${it.competition} ${it.sport}".contains(query.trim(),true) }.sortedBy { it.kickoff(today) }
        if (events.isEmpty()) empty("A quiet day on this court", "Try another date or sport, or import your event calendar.") else events.forEach { eventRow(it) }
        padded(body, text("${java.time.ZoneId.systemDefault().id} · Calendar and broadcast availability vary by region.", 11, muted), 17)
        if (store.events().isNotEmpty()) padded(body, button(if(demoSchedule) "Show imported calendar" else "Explore demo calendar") { demoSchedule = !demoSchedule; render() }, 16)
    }
    private fun eventRow(event: Event) {
        val matches=BroadcastRouting.matches(event,store.all())
        val card = row().apply { background = shape(panel, 15); setPadding(dp(15), dp(17), dp(12), dp(17)); isClickable = true; isFocusable = true; setOnClickListener { if(!event.demo && matches.isNotEmpty()) play(matches.first(),matches) else eventDetail(event) }; setOnLongClickListener { eventDetail(event); true } }
        val left = column()
        left.addView(text(event.sport.uppercase(), 9, red, true)); space(left, 9)
        left.addView(text(event.kickoff(today).format(timeFormat), 15, Color.WHITE, true))
        card.addView(left, LinearLayout.LayoutParams(dp(86), -2))
        val details = column()
        details.addView(text(event.home, 15, Color.WHITE, true)); space(details, 5)
        details.addView(text(event.away.ifBlank { event.competition }, 14, 0xFFD7DCE4.toInt())); space(details, 8)
        details.addView(text(event.competition, 9, muted))
        if(matches.isNotEmpty()) { space(details,7); details.addView(text("▷ ${matches.first().name}${if(matches.size>1) " · +${matches.size-1}" else ""}",10,red,true)) }
        card.addView(details, LinearLayout.LayoutParams(0,-2,1f))
        card.addView(iconButton(if(store.saved().contains(event.id)) "♥" else "♡", "Save ${event.home}") { store.toggle(event.id); render() }.apply { setTextColor(if(store.saved().contains(event.id)) red else muted) }, LinearLayout.LayoutParams(dp(44),dp(44)))
        padded(body, card, bottom = 9)
    }
    private fun eventDetail(event: Event) {
        val content = column().apply { setPadding(dp(22),dp(12),dp(22),dp(12)) }
        content.addView(text(event.sport.uppercase(), 11, red, true)); space(content, 16)
        content.addView(text(event.home + if(event.away.isNotBlank()) "\nvs ${event.away}" else "", 26, Color.WHITE, true)); space(content, 14)
        content.addView(text(event.competition, 13, muted)); space(content, 8)
        content.addView(text(event.kickoff(today).format(DateTimeFormatter.ofPattern("EEE d MMM · HH:mm z")), 13, muted)); space(content, 20)
        val matches=BroadcastRouting.matches(event,store.all())
        val source = matches.firstOrNull()
        content.addView(text(if(event.demo) "This is an illustrative fixture. Demo playback is a separate test video, not this event." else if(source == null) "No playback source is linked to this event." else "Watch with ${source.name} inside SportsWave.",13, muted))
        val dialog = AlertDialog.Builder(this).setView(content).setNegativeButton("Close",null).create()
        space(content, 18)
        if (source != null) {
            content.addView(button("Watch ${source.name}  ▷",true) { dialog.dismiss(); play(source,matches) })
            if(matches.size>1) { space(content,8); content.addView(button("Other broadcasts (${matches.size-1})") { dialog.dismiss(); chooseBroadcast(matches) }) }
        }
        else if(event.demo) content.addView(button("Test player with demo video  ▷",true) { dialog.dismiss(); play(Catalog.samples.first()) })
        else content.addView(button("Manage channels") { dialog.dismiss(); tab = "Channels"; render() })
        dialog.show(); dialog.window?.setBackgroundDrawable(shape(panel, 20))
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
        section("Your collection", "${store.custom().size} sources")
        val sources = store.custom().filter { (sport=="All sports" || it.sport==sport || it.sport=="Multi-sport") && it.name.contains(query,true) }
        if(sources.isEmpty()) empty("Make room for your favorites", "Add an HTTPS channel URL or import a provider catalog. Your sources play here, inside the app.") else sources.forEach { sourceRow(it) }
        section("Player test streams")
        padded(body,text("Public sample videos for testing HLS and DASH. These are not live sports channels.",12,muted),bottom=14)
        Catalog.samples.filter { it.name.contains(query,true) }.forEach { sourceRow(it) }
    }
    private fun sourceRow(source: Source) {
        val card = row().apply { background=shape(panel,15); setPadding(dp(16),dp(17),dp(10),dp(17)); isFocusable=true; setOnClickListener { play(source) } }
        card.addView(text("▷",26,red,true),LinearLayout.LayoutParams(dp(42),-2))
        val info=column(); info.addView(text(source.name,15,Color.WHITE,true)); space(info,7)
        info.addView(text(if(source.demo) "DEMO VIDEO · ${if(source.id.contains("hls")) "HLS" else "DASH"}" else if(source.sessionSnapshot) "${source.provider} · Temporary playback session" else "${source.provider.ifBlank { source.sport }} · ${if(source.resolverUrl.isNotBlank()) "Provider playback" else "Direct stream"}",10,muted))
        card.addView(info,LinearLayout.LayoutParams(0,-2,1f))
        if(!source.demo) card.addView(iconButton("×","Remove ${source.name}") { AlertDialog.Builder(this).setTitle("Remove channel?").setMessage(source.name).setNegativeButton("Cancel",null).setPositiveButton("Remove") { _,_ -> store.remove(source.id); render() }.show() })
        padded(body,card,bottom=9)
    }
    private fun saved() {
        padded(body,text("Worth coming back for.",30,Color.WHITE,true),bottom=10)
        padded(body,text("Your saved events, all in one place.",13,muted),bottom=22)
        val selected=(store.events()+Catalog.events).distinctBy { it.id }.filter { store.saved().contains(it.id) }
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
            runOnUiThread { if(!isFinishing && !isDestroyed) { result.onSuccess { count -> demoSchedule=store.events().isEmpty(); Toast.makeText(this,"Imported $count items",Toast.LENGTH_LONG).show(); render() }.onFailure { AlertDialog.Builder(this).setTitle("Couldn't import catalog").setMessage(it.message ?: "Check the JSON format.").setPositiveButton("OK",null).show() } } }
        }.start()
    }
    private fun openGuide() { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.beinsports.com/en-mena/tv-guide"))) }
    private fun chooseBroadcast(sources:List<Source>) { AlertDialog.Builder(this).setTitle("Available broadcasts").setItems(sources.map { "${it.provider.ifBlank { "Channel" }} · ${it.name}" }.toTypedArray()) { _,index -> play(sources[index],sources) }.setNegativeButton("Cancel",null).show() }
    private fun play(source:Source,alternatives:List<Source> = listOf(source)) { startActivity(Intent(this,PlayerActivity::class.java).putExtra("sourceId",source.id).putExtra("alternatives",alternatives.map { it.id }.toTypedArray())) }
}
