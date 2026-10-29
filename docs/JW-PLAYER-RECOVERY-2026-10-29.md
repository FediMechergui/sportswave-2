# JW Player error capture and recovery - 29 October 2026

A live VIPRow Adelaide United / Home United listing published TodayStreams links for Nova Sport Bulgaria and ESPN 1 NL. The first link used TodayStreams -> DaddyLive -> Dembed. Browser samples with the current resource policy loaded JW Player components successfully and rendered 1920-wide video; removing the Chrome namespace did not reproduce a library failure.

An Android WebView sample loaded JW Player but stopped with code 232403 and a displayed protected-content access error. The JW [error reference](https://docs.jwplayer.com/players/docs/jw8-player-errors-reference) defines the 232400-232599 range as HLS manifest HTTP failures. This establishes a failed sample, not the cause of every reported VIPRow error or proof of why the upstream denied that request. The initial Android emulator renderer was unstable; testing resumed after restarting the dedicated emulator and lowering its test resolution.

Version 1.0.18 observes JW fatal/setup events, failed library script loads and visible player error codes in reviewed player origins. It sends only a bounded error code to the activity console observer: no media URLs, keys, cookies, license changes, SSL exceptions or authentication workarounds. The normal player state remains authoritative; no HTTP-200 page is treated as successful video.

On failure the activity tries another player actually published for the same event. Automatic attempts are bounded to three distinct URLs, with no loops; manual Servers/Retry/Refresh remain available. Final messages distinguish script/library failures, provider player-license failures and observed stream rejection. A later generic timeout or unrelated HTTP failure cannot overwrite the captured JW failure message.

Validation includes JVM tests for strict code parsing and bounded source choice, and JavaScript execution tests for callback errors, visible DOM error codes, failed JW script loads, single-signal behavior, source attribution and no false alarm on a healthy page. Native test results are recorded separately in ignored artifacts. No statement is made that rejected upstream feeds now play.

Final validation: 64 JVM tests passed. Four browser execution cases passed (callback, DOM code, library load failure, healthy page). Android instrumentation injected a synthetic visible 232403 message into an isolated player, confirmed the native explanation and observed automatic selection of the second supplied server. This is a controller/integration test, not a claim of successful playback on that server.
