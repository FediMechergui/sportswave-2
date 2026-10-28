# VIPRow and reference-app coverage — 28 October 2026

The signed SportsWave 2 [v1.0.13 release](https://github.com/FediMechergui/sportswave-2/releases/tag/v1.0.13) was published before this investigation. These additional findings are research, not an announcement that VIPRow is integrated into that APK.

## Useful coverage

Read every sports-section link published by [VIPRow's football page](https://viprow.im/sports/football/) in a browser. The later snapshot contained **442 listings across 19 sections**, with 14 populated sections. The earlier same-day snapshot contained 475. Every later record had a published event-page link. Counts change as the site updates; they are listings, not unique verified live streams or scored fixtures.

The later snapshot includes football (280), basketball (56), ice hockey (27), horse racing (11), rugby (10), tennis (9), American football (9), handball (9), golf (7), volleyball (7), motorsports (3), MMA (2), baseball (1) and more sports (11). Boxing, Aussie rules, darts, snooker and cycling were empty. See [section results](VIPROW-COVERAGE-2026-10-28.csv).

Sampled one event page in each of the 14 populated sections: 12 exposed compatible players, totalling 23 links. All compatible sampled players belonged to TodayStreams. The sampled rugby page exposed no compatible player; the more-sports page returned HTTP 502. This does not establish that every event page uses the same provider.

## Playback samples

Opened the first published player for six different sports using ordinary browser navigation. Five decoded video and showed advancing playback clocks. This is a bounded playback sample, not a latency or long-term reliability benchmark.

| Sample | Observation |
| --- | --- |
| Tortona / Rostock basketball | Moving video; Magenta Sport branding and DERTHONA / ROSTOCK scoreboard match the listed fixture, allowing Tortona's Derthona club name. |
| Pioneers Vorarlberg / Olimpija ice hockey | Moving video; Sport TV 2 branding and PIV / OLL scoreboard match the listed service and teams. |
| Sky Sports F1 UK | Moving video and matching service watermark; US race footage, not evidence of the upcoming Mexico practice being live. This record is a channel entry, not a race session. |
| Mielec / Rzeszow volleyball | Decoded video and advancing clock; final capture showed a buffering overlay. Treat as unstable rather than a reliable continuous feed. |
| Sky Sports Racing | Moving video and matching service watermark; captured advertising, not verified racing action. |
| Hurkacz / Rune tennis, first server | No decoded frame; repeated media-edge HTTP 404 despite the page returning HTTP 200. |

See [sample results](VIPROW-PLAYBACK-2026-10-28.csv). An earlier Bertola/Shapovalov sample displayed matching players on the scoreboard, but that older sample must not be used to certify the later Hurkacz/Rune server.

Tested both remaining Hurkacz/Rune choices as well. Server 2 produced no video; server 3 decoded 1920-pixel-wide video and its clock advanced, using `wikisport.info` and `xstream.st` nested frames. However, the final page screenshot retained a black loading overlay. This is not a verified usable tennis player. The reference registry also does not review those new frame providers; adding the two DaddyLive/Dembed host entries alone is not sufficient for that route.

## How the linked app works

Reviewed [SportsWave-Android](https://github.com/FediMechergui/SportsWave-Android), reference commit `2a9ea16b4bf98771bb59a1663cd11c2990d41ca9`, and installed/launched its published v1.0.2 APK on emulator-5560.

1. `MainActivity.loadViprow` loads `/sports/<slug>/` in a JavaScript-enabled metadata WebView. It reads `window.ftbPageData.schedule` plus `league`/`leagueList`; anchors provide actual event URLs. The data is not a live-score API.
2. `ResolveActivity` opens the selected event and reads published anchor/frame attributes. It matches URLs against `providers.json`. It does not resolve the event into a direct native HLS URL.
3. `WebPlayerActivity` opens the chosen provider webpage, permitting only approved page/frame/resource hosts. The native `playback_sources.json` is empty.

In the actual Android APK, selecting VIPRow populated football and tennis calendars. Opening the Hurkacz/Rune event exposed three TodayStreams choices. Selecting one opened a blank player with only Back / Switch server visible.

## Reproduced compatibility defect

Observed the normal public player chain:

`VIPRow event → TodayStreams /play/ → thedaddy.pk /stream/ → dembed.st player → media edge / CDN`

The reference registry permits the older DaddyLive host `dlive.sx`, but not the observed `thedaddy.pk` player frame. Its Dembed resource list also omits the observed media host `edge.wedd48c.top`.

Reproducing the reference host policy in Chromium blocked the `thedaddy.pk` frame and produced no video. Adding only that alias reached the video element but still blocked the new media host. Adding both reviewed host changes allowed the sampled basketball player to decode at 1280 pixels wide and advance from 20.33 to 25.36 seconds during a five-second check. Other advertising/tracking hosts remained blocked. A popup-library JavaScript error was observed but did not prevent that sample's video.

The corrected-policy screenshot visibly displayed the basketball court and DERTHONA / ROSTOCK scoreboard, not just a hidden advancing video element.

The same updated policy could not repair the first Hurkacz/Rune source: its media requests returned 404 both with the restrictive policy and in an ordinary browser. Distinguish an app-side blocked host from a provider-side missing stream.

[Minimal JSON Patch](VIPROW-PROVIDER-PATCH-2026-10-28.json) targets `app/src/main/assets/providers.json` in the reference commit above: provider index 1 is `daddylive`, index 2 is `dembed`. This is a browser-verified compatibility proposal; it has **not** been shipped or verified in a rebuilt Android version of that other app. Do not apply these array indices to a registry with a different order.

## Implications for SportsWave 2

- VIPRow adds a useful event catalogue and multiple provider choices. Its HTML player pages cannot be pasted into the current native HLS/DASH source field. A dedicated event discovery and web-player path is required.
- Keep event IDs, published URLs and explicit provider choices together. Do not guess streams from a sport or replace a match's advertised provider with an unrelated working channel.
- Several records lack an end time. Some are channels, such as Sky Sports F1 UK, mixed into race listings. Preserve unknown status and separate channel records from actual races and matches.
- Calendar timestamps and live-score data remain separate. A published start time, moving player clock or channel watermark does not prove the current scheduled event is airing.
- Keep server failover limited to choices actually published for the selected event. Show a useful error for blocked required frames and failed media; an HTTP 200 document must not be labelled successful playback.
- Use the reviewed host additions, not a blanket permit for every advertising host. Recheck hosts as providers rotate. Do not bundle expiring media URLs or copy signatures.

Other reference-app resources remain relevant: ESPN/FilGoal/OpenLigaDB for fixture and score metadata, Raceweek for race calendars, Livekora for published channel/player pages, and Streamed/KorahLive/Yalla for separate catalogue adapters. Prior checks found Streamed's expected JSON URL redirecting to HTML and KorahLive unavailable locally; their presence in code is not proof of current working coverage. No additional Arabic beIN 9, XTRA or MAX feed was verified by this investigation.
