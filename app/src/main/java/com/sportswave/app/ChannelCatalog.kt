package com.sportswave.app

object ChannelCatalog {
    fun isSports(source: Source): Boolean = Regex("(?i)(sports?|alkass|الكأس|الكاس|رياضي|arryadia|arriyadia|eurosport|bein.*sports|racing|\\bssc\\b|\\byas\\b)").containsMatchIn(source.name+" "+source.catalogCategory)
    private fun quality(source: Source): Int = when {
        source.catalogCategory.contains("720P",true) -> 0
        source.provider.equals("Alkass",true) -> 1
        source.catalogCategory.contains("1080P",true) -> 2
        source.catalogCategory.contains("360P",true) -> 4
        source.catalogCategory.contains("244P",true) -> 5
        else -> 3
    }
    fun sorted(sources: List<Source>): List<Source> = sources.distinctBy { it.id }.sortedWith(compareBy<Source> { if(isSports(it)) 0 else 1 }.thenBy { quality(it) })
    private fun identity(source: Source) = source.provider.lowercase()+":"+source.name.lowercase().replace(Regex("\\b(1080p|720p|360p|244p|fhd|hd|sd)\\b"),"").filter { it.isLetterOrDigit() }
    fun unique(sources: List<Source>): List<Source> = sorted(sources).distinctBy { identity(it) }
    fun alternatives(source: Source, sources: List<Source>): List<Source> = sorted(sources.filter { it.integration=="yacine" && identity(it)==identity(source) })
    fun nextAlternative(source: Source, sources: List<Source>, attempted: Set<String>): Source? = alternatives(source,sources).firstOrNull { it.id!=source.id && it.id !in attempted }
}
