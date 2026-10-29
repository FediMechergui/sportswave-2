package com.sportswave.app

/** Observe public player errors; never read media URLs, cookies, licences or keys. */
object WebPlayerHealth {
    const val SOURCE="https://sportswave.invalid/jw-health.js"
    private val report=Regex("^SportsWaveJW:(library|[0-9]{6})$")
    fun code(message:String):String?=report.matchEntire(message)?.groupValues?.get(1)
    fun message(code:String):String=when {
        code=="library" -> "The provider's JW Player library could not load"
        code in setOf("100011","100012","100013","100014") -> "The provider's player licence is unavailable (JW $code)"
        code=="232403" -> "The provider denied access to this stream (JW $code)"
        else -> "The provider player failed (JW $code)"
    }
    fun next(choices:List<WebPlayerChoice>,attempted:Set<String>):Int? =
        if(attempted.size>=3) null else choices.indices.firstOrNull { choices[it].url !in attempted }
    val script="""
        (function(){
          if(window.__sportswaveJWHealth)return;window.__sportswaveJWHealth=true;
          var reported=false,attached=false,tries=0;
          function emit(code){if(!reported&&/^(library|[0-9]{6})$/.test(String(code))){reported=true;console.info('SportsWaveJW:'+code);}}
          window.addEventListener('error',function(e){var t=e.target;if(t&&t.tagName==='SCRIPT'){try{var u=new URL(t.src);if((u.hostname==='ssl.p.jwpcdn.com'||u.hostname==='cdn.jwplayer.com')&&/jwplayer|\/libraries\//i.test(u.pathname))emit('library');}catch(ignore){}}},true);
          function poll(){
            if(reported||++tries>180)return;
            if(!attached&&typeof window.jwplayer==='function'){try{var p=window.jwplayer();if(p&&typeof p.on==='function'){p.on('setupError',function(e){if(e)emit(e.code);});p.on('error',function(e){if(e)emit(e.code);});attached=true;}}catch(ignore){}}
            var n=document.querySelector('.jw-error-msg,.jw-error-text');
            if(n&&n.getClientRects().length){var m=(n.textContent||'').match(/Error\s*Code\s*:\s*([0-9]{6})/i);if(m)emit(m[1]);}
            var text=document.body?(document.body.innerText||''):'';
            if(/jw\s*player failed to load/i.test(text))emit('library');
            if(!reported)setTimeout(poll,500);
          }
          poll();
        })();
        //# sourceURL=https://sportswave.invalid/jw-health.js
    """.trimIndent()
}
