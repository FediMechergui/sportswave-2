# Focused beIN follow-up — 21 October 2026

The [80-record recheck](BEIN-RECHECK.csv) covers every beIN sports quality entry in the current Yacine catalog. The earlier 675-record audit remains a historical snapshot.

## Numbered MENA channels

The redirect service returned HTTP 503 with `No URLs available`, including with the exact supplied user agent and headers. This is an upstream empty feed pool, not evidence that the app needs a different player or that its decoder is broken.

During the follow-up, 38/80 records briefly served an HLS media segment, 24 failed and 18 returned no stream records. All 38 delivery successes were then tested through Media3 in the signed app process: four rendered a video frame (720p beIN SPORTS 1, 5, 6 and 7); 34 returned HTTP-related playback errors. The four successful feeds subsequently returned the same 503 response in a workstation recheck. A later fresh-process Android check rendered beIN SPORTS 1 and 2 again. These changing outcomes demonstrate intermittent provider availability; **38 network successes do not establish 38 reliably playable channels**.

The app continues to request current provider records and offers bounded same-channel quality fallback. Version 1.0.6 identifies the explicit empty-feed response in the player, rather than reporting only a generic decoder error. Existing provider records remain available for Retry when delivery recovers. The 465 unresolved records from the previous audit are not reduced on the basis of transient playback.

A subsequent actual player UI test opened beIN SPORTS 1 and remained in the playing state at observations 50 seconds apart. The screenshots show changing footage with the beIN SPORTS 1 watermark. This validates playback through the normal app flow during that test; the earlier empty-pool responses still prevent a reliability guarantee.

## Independent candidates

| Candidate group | Checks | Result |
| --- | ---: | --- |
| Current public sports/US/Qatar directories | 17 distinct URLs | Numbered MENA candidates returned 404; Turkish candidates had network failures. US XTRA served HLS media. Spanish XTRA had a network failure. |
| Public beIN quality playlist discovered through a GitHub gist | 36 URLs | Delivery host had network failures. No account credentials used. |
| Public AlbaPlayer servers linked by Yalla Shoot | 4 published server links | Two returned signed media URLs that failed with 403; one had no native URL; one failed with 404. No signing keys were derived or forged. |
| Originalpro embedded match player | 1 published embed | Player reported that its match broadcast had ended. |
| Other Arabic search candidates | Site/page checks | Several were directories, articles, expired domains, failed HTTP requests or blog interstitials. None was counted as playable merely because its page loaded. |

Search/discovery pages included [Yalla Shoot](https://yallashoot-2.com/), [Originalpro](https://originalpro.online/live.html), [Habbabi sports](https://habbabihd.com/tv-arabic-sports.aspx), [the submitted playlist](https://gist.github.com/mohannad-art/1f08bae8819eb6ced114bc5fd9920dac), and [IPTV-org](https://github.com/iptv-org/iptv). Captured signed media URLs and raw embedded player responses remain outside the repository. Pages containing third-party account credentials or DRM keys were not used as replacement sources.

## Verified additional beIN channel

`beIN SPORTS XTRA (US)` uses the public Amagi HLS feed discovered in the US directory. The actual signed v1.0.6 app rendered motorsport footage with the beIN SPORTS XTRA watermark and remained playing at a second observation 25 seconds later. An independent fresh-process decoder check also rendered a frame. An initial decoder harness run timed out; the actual player UI and the fresh-process retry passed.

It is listed separately under provider `beIN US`, with an explicit US label and distinct channel identity. Calendar references to MENA beIN cannot match it by provider. It has its own English-language programming and is not a replacement for beIN SPORTS 1–9 MENA. Availability can vary by region and over time.

The broadcaster describes XTRA as a free channel in its [2026 coverage announcement](https://www.beinsports.com/en-us/soccer/pko-bp-ekstraklasa/articles-video/how-and-where-to-watch-the-polish-ekstraklasa-on-bein-sports-2026-07-21). Delivery verification and channel identity were checked separately; that announcement does not promise a third-party integration contract.

## Validation

The signed release/debug builds, 15 JVM tests and signed-release catalog/import instrumentation passed. Routing tests cover the US/MENA distinction. The instrumentation supports `-e auditSources` with comma-separated cached channel IDs and reports decoder outcomes without printing signed playback URLs. HTTP status codes are retained in new diagnostic outcomes.

To repeat a focused network audit without overwriting the complete catalog report, run `python scripts/audit_channels.py --package-prefix "beIN SPORTS" --output artifacts/bein-followup.jsonl`. It discovers the current catalog before checking matching leaf packages. Use a fresh report to compare availability at another time.
