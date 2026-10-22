# Original-app beIN comparison — 22 October 2026

Yacine TV 3.1 (`ver3.ycntivi.off`) and YTV Player Pro 15.0, version code 18 (`com.ytv.pronew`), were installed on an Android 15 emulator. Individual catalog selections were opened through the original UI using ADB taps. Screenshots were inspected visually and results recorded individually. Navigation and screenshot capture were scripted in small batches; the visual outcomes were not inferred from API responses or automatically classified. Ordinary test-ad interstitials were dismissed and were not counted as channel video.

The scope is 95 **catalog entries**, including quality duplicates: 20 sports entries in each of 1080p, 720p, 360p and 244p, 13 beIN entertainment entries, and two beIN entries in Kids. The [comparison CSV](BEIN-ORIGINAL-COMPARISON.csv) records each catalog ID, observation, timestamp, screenshot names and separate SportsWave decoder result. Screenshots remain in the local ignored `artifacts/original-yacine-2026-10-22/` directory; selected evidence accompanies the APK release.

## Original-app results

All 95 entries were selected and reviewed: 38 showed video, six showed video with different branding, 32 did not start, 18 returned to the catalog, and one opened Chrome without verified channel video. This does not establish that all beIN channels work in Yacine itself.

| Package | Entries | Video visible | Different branding | No video observed |
| --- | ---: | ---: | ---: | ---: |
| beIN SPORTS (1080P) | 20 | 7 | 2 | 11 |
| beIN SPORTS (720P) | 20 | 8 | 2 | 10 |
| beIN SPORTS (360P) | 20 | 7 | 1 | 12 |
| beIN SPORTS (244P) | 20 | 6 | 1 | 13 |
| beIN ENTERTAINMENT | 13 | 8 | 0 | 5 |
| KIDS CHANNELS | 2 | 2 | 0 | 0 |

The first three 1080p checks preceded proxy capture and have UI/screenshot evidence only. The remaining 92 selections have a matching observed API channel ID. For the first two follow-up cases, the CSV retains the initial ad activity while the notes and additional result screenshots record the subsequent channel video. The emulator needed a cold restart before the final Box Office/Kids checks; their later timestamps are preserved.

## Interpreting the observations

`video_visible` means channel/promo footage was visible at inspection. It is not a guarantee of uninterrupted playback, channel identity when no watermark was visible, or current live-event programming. `video_visible_different_brand` flags a watermark different from the selected catalog label; this may be a provider mapping problem or simulcast. `did_not_start` means no video was observed within that test window, rather than a permanent outage. `returned_to_catalog` means the original app returned to its channel grid without channel video. Several failing entries were revisited for longer observations.

The initial capture starts after a 12-second wait from selection, followed by visible ad dismissal when required, then two screenshots separated by three seconds. Startup delays, retries and longer checks vary, so these are availability observations, not a controlled startup-time benchmark. Original and SportsWave tests occurred at different times against a changing redirect pool.

## Same-feed integration

The observed native beIN flow requests `channel/{id}` from the same backend SportsWave already uses. The original API user agent is `okhttp/4.12.0`; SportsWave now matches it. The inspected native requests had no account Authorization header or extra client-generated signature. Returned playback mode 3 URLs already contained the server-issued signature and expiry. SportsWave decodes the current response and forwards its media URL, user agent and headers to Media3.

Compared original-player captures with fresh API requests: both paths produced the same media path and supplied playback headers, with expected differences in signature/time. The inspected expiry was approximately 18,000 seconds (five hours). Both the former and updated API user agents could obtain media in workstation checks, so the user-agent adjustment alone is not credited with fixing availability. Hardcoding a captured signature would eventually reproduce an expired session; the APK continues to obtain current sessions on opening, Retry and bounded automatic recovery.

The original redirect service rotates between CDN hosts. Some returned 404 or could not connect even while another quality of the channel played. Version 1.0.7 extends session recovery to HTTP 404/502 and network connection failures/timeouts, alongside the previous authorization, 503 and lost-live-window cases. It requests the same channel again at most twice, after one and two seconds, then can try unused qualities of that same channel. Lifecycle/generation guards cancel stale recovery work. Unrelated channels are not substituted.

## Live transport versus live programming

A 36-second seek bar is not proof of a 36-second video clip. One inspected original-player HLS playlist had a six-segment sliding window, no `ENDLIST`, and an advancing media sequence (2105 to 2186 between observations). SportsWave's decoder audit separately records Media3's live-timeline flag on first frame. These establish live-style transport, not that the shown match is happening now. Some visible graphics referenced older seasons; programming freshness was not independently established.

## SportsWave verification

All 95 entries were separately resolved and tested through Media3 in the signed SportsWave app process on a fresh second Android 15 emulator without either companion app. The decoder harness uses the real resolver and returned headers; it tests the selected descriptor once and does not exercise PlayerActivity's automatic quality fallback. Four first-frame successes were initially overwritten by a SurfaceTexture cleanup timeout in the harness; preserving the first-frame result fixed the accounting, and all four also rendered video on individual recheck.

After those rechecks, 47 entries rendered video with a live timeline. The other results were 26 HTTP 404 errors, 20 resolution failures, one network connection error and one timeout. These are a snapshot, not a promise that 47 entries always work. Channel watermark identity is assessed in the original-app visual notes, not by the decoder harness.

Normal signed-APK UI checks separately showed beIN Sports 1 and Sports 8 playing at observations 50 seconds apart. An explicit selection of Sports 8's 1080p catalog entry also reached playback in the rebuilt APK; same-channel quality fallback was allowed, so this does not establish the final selected quality. Seventeen JVM tests and release/debug/test-APK builds passed.

The HTTP observation proxy only forwarded ordinary traffic and tunneled TLS unchanged; no HTTPS certificate validation was disabled. Captured signed URLs and full request headers remain in ignored local artifacts and are not distributed with the APK.
