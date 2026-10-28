# Provider integration ? 29 October 2026

Version 1.0.15 (16) implements the researched provider workflow in the Android app. It does not ship temporary media URLs or authentication workarounds.

## Implemented

- VIPRow: public event metadata across 19 sections, selected-date filtering and exact event-ID routing. Opening an event discovers its actual published event link and reviewed player choices, rather than guessing a URL from team names.
- Additional calendars: StreamCenter, Livekora, Yalla, FilGoal, ESPN F1/NHL/MLB and Raceweek meeting dates. Soccer and American football remain distinct. Meeting dates and scheduled web events are not presented as verified live scores.
- In-app WebView players with server selection, retry, refreshed source discovery, provider fullscreen and Back handling. HTTPS host restrictions, blocked popups, no JavaScript/native bridge and no file access. Native HLS/DASH sources still use Media3.
- Official SNRT Arryadia web source. DaddyLive SPORTS 9 and XTRA are explicitly labeled candidates, excluded from automatic matching to official beIN broadcasts. Arabic MAX was not added as a working feed.
- Exact channel-name and package recovery after provider IDs rotate, only on API 404. Alternate native descriptors remain available. Emoji/flag-safe participant initials fix UI hierarchy failures.

## Verification

Release/debug builds and 52 JVM tests passed. Android instrumentation passed calendar/catalog import, atomic validation, source updates, resolver configuration, deletion and persistent favourites.

On Android 15 emulator:

- Native Schedule ? Santos/Palmeiras ? VIPRow ? published Premiere BR player produced 1920-wide advancing video, with SAN/PAL scoreboard and AO VIVO watermark matching the selected fixture. The server menu offered the five published choices for this event.
- SNRT Arryadia produced advancing 854-wide video. Provider fullscreen entered landscape; Back returned to the player.
- An old beIN SPORTS 1 / 720P channel ID recovered to its current exact-name/package record and decoded video through alternate descriptor 1. Descriptor 0 failed upstream.
- Calendar snapshots: October 28 had 232 events/111 web-enabled; October 29 had 215/121; October 30 had 173/99, including two F1 practice sessions. These are time-specific snapshots, not availability guarantees.

## Remaining failures

StreamCenter resolved a published player but its media service returned HTTP 503. One VIPRow Premiere 2 BR source did not decode video; the separately tested Santos fixture did. Daddy XTRA played through the full public watch page earlier, but the final restricted embedded-wrapper retest returned provider HTTP 404. Its candidate label is intentional: current availability, Arabic commentary and numbered XTRA identity are not established. SPORTS 9 remains intermittent and not independently schedule-verified; genuine Arabic MAX remains unresolved.

Server selection cannot repair an unavailable upstream feed. No claim is made that every channel or web server works. Debug screenshots, calendar snapshots and decoder logs remain in ignored local artifacts/v115; no signed playback sessions are committed.
