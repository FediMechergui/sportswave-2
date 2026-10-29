package com.sportswave.app

import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** Exact, reviewed aliases only: no substring or edit-distance team matching. */
object TeamIdentity {
    private val aliases=ConcurrentHashMap<String,Set<String>>()
    private val contextual=ConcurrentHashMap<String,Set<String>>()
    private val leaguePattern=Regex("(?i)\\b(?:wnba|nba|nhl|mlb|nfl)\\b")
    fun contextual(sport:String,league:String,canonical:String,names:List<String>) {
        for(name in names.filter { it.isNotBlank() }) contextual.compute("$sport|${league.uppercase(Locale.ROOT)}|${normalized(name)}") { _,old -> old.orEmpty()+normalized(canonical) }
    }
    private val normalizedCache=ConcurrentHashMap<String,String>()
    private val marks=Regex("\\p{M}+")
    private val punctuation=Regex("[^\\p{L}\\p{N}]+")
    private val spaces=Regex("\\s+")
    private val suffixes=setOf("fc","cf","afc","sc","club")
    private val countrySuffix=Regex("\\s*\\(([A-Z]{3})\\)\\s*$")
    private val countryCodes="ABW AFG AGO AIA ALA ALB AND ARE ARG ARM ASM ATA ATF ATG AUS AUT AZE BDI BEL BEN BES BFA BGD BGR BHR BHS BIH BLM BLR BLZ BMU BOL BRA BRB BRN BTN BVT BWA CAF CAN CCK CHE CHL CHN CIV CMR COD COG COK COL COM CPV CRI CUB CUW CXR CYM CYP CZE DEU DJI DMA DNK DOM DZA ECU EGY ERI ESH ESP EST ETH FIN FJI FLK FRA FRO FSM GAB GBR GEO GGY GHA GIB GIN GLP GMB GNB GNQ GRC GRD GRL GTM GUF GUM GUY HKG HMD HND HRV HTI HUN IDN IMN IND IOT IRL IRN IRQ ISL ISR ITA JAM JEY JOR JPN KAZ KEN KGZ KHM KIR KNA KOR KWT LAO LBN LBR LBY LCA LIE LKA LSO LTU LUX LVA MAC MAF MAR MCO MDA MDG MDV MEX MHL MKD MLI MLT MMR MNE MNG MNP MOZ MRT MSR MTQ MUS MWI MYS MYT NAM NCL NER NFK NGA NIC NIU NLD NOR NPL NRU NZL OMN PAK PAN PCN PER PHL PLW PNG POL PRI PRK PRT PRY PSE PYF QAT REU ROU RUS RWA SAU SDN SEN SGP SGS SHN SJM SLB SLE SLV SMR SOM SPM SRB SSD STP SUR SVK SVN SWE SWZ SXM SYC SYR TCA TCD TGO THA TJK TKL TKM TLS TON TTO TUN TUR TUV TWN TZA UGA UKR UMI URY USA UZB VAT VCT VEN VGB VIR VNM VUT WLF WSM YEM ZAF ZMB ZWE ENG SCO WAL NIR GER SUI POR NED GRE DEN KSA UAE CHI URU PAR VIE CRC HON SLV".split(" ").toSet()
    fun country(name:String):Pair<String,String> {
        val suffix=countrySuffix.find(name) ?: return name to ""
        return if(suffix.groupValues[1] in countryCodes) name.substring(0,suffix.range.first).trim() to suffix.groupValues[1] else name to ""
    }
    private val redirects=ConcurrentHashMap<String,String>()
    private val sharedNames=setOf("الهلال","الاهلي","الاتحاد","النصر","الوحدة","الشباب","الفتح","الفيصلي")
    private fun root(sport:String,value:String):String {
        var current=value
        repeat(12) { current=redirects["$sport|$current"] ?: return current }
        return current
    }
    fun normalized(name:String):String {
        normalizedCache[name]?.let { return it }
        val words=Normalizer.normalize(name.lowercase(Locale.ROOT),Normalizer.Form.NFD)
            .replace(marks,"").replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي').replace("ـ","")
            .replace(punctuation," ").trim().split(spaces)
        val result=words.filter { it !in suffixes }.joinToString("")
        if(normalizedCache.size>=4096) normalizedCache.clear()
        normalizedCache[name]=result
        return result
    }
    fun register(sport:String,canonical:String,names:List<String>) {
        val identity=normalized(canonical)
        for(name in (names+canonical).filter { it.isNotBlank() }) {
            val key="$sport|${normalized(name)}"
            aliases.compute(key) { _,old -> (old.orEmpty()+identity).toSet() }
        }
    }
    fun key(sport:String,name:String,competition:String=""):String {
        val plain=normalized(name)
        val league=leaguePattern.find(competition)?.value?.uppercase(Locale.ROOT)
        if(league!=null) contextual["$sport|$league|$plain"]?.map { root(sport,it) }?.distinct()?.singleOrNull()?.let { return it }
        if(plain in sharedNames) return plain // Several countries have these clubs; no guessed badge or translation.
        return aliases["$sport|$plain"]?.map { root(sport,it) }?.distinct()?.singleOrNull() ?: root(sport,plain)
    }
    @Synchronized fun reviewed(sport:String,names:List<String>) {
        if(names.isEmpty()) return
        val target=key(sport,names.first())
        for(name in names) {
            val old=key(sport,name)
            if(old!=target) redirects["$sport|$old"]=target
        }
        register(sport,target,names)
    }
}
