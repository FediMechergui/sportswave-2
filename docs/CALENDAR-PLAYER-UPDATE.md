# Calendar and player update - v1.0.9

The selected local date now drives the beIN MENA guide request and ESPN fixtures. Yacine's event API and imported events contribute explicit broadcast assignments. The guide is paginated and restricted to MENA channel IDs; replay entries are excluded. Partial provider failures retain cached listings and show an unavailable-feed message.

A tap during the published airing window opens the assigned channel inside SportsWave, resolving its current playback session. Upcoming and ended entries show details. Matches, individual sports, races, and sessions share the same rules. Exact participant/sport/time matches can attach published broadcast references to public scoreboards; unmatched fixtures remain unlinked. Provider outages still affect video delivery. Published schedules can also disagree with the provider video. The player identifies the event as a scheduled listing, rather than verified on-screen content.

Event cards show participants, available crests, timing, scores, and broadcast names. Favorites retain event details across date changes. Search input survives background updates. Calendar cards load in batches of 40; today lists airing events first, followed by upcoming and ended entries. Duplicate merging uses an identity index to avoid comparing every fixture pair.

Fullscreen reparents the existing Media3 view and retains the player, fills the landscape window, and hides system bars. Back returns to portrait playback before leaving the player. Error and retry controls remain available in fullscreen.

## Validation

- Signed release, debug APK, and Android test APK builds passed using cached Gradle 8.14.3. The user's wrapper setting was preserved.
- All 23 JVM tests passed. The 2,000-entry duplicate-calendar regression completed in 0.106 seconds in this run.
- Android instrumentation passed guide parsing, MENA/replay filtering, race/tennis classification, schedule cache serialization, saved event details, and existing catalog/import validation.
- A real Android calendar request returned 77 listings with no provider errors during this investigation. This verifies metadata delivery, not successful playback of every listed channel.
- Signed v1.0.8 Android UI checks passed upcoming/ended playback gating and internal sample playback from football, basketball, tennis, motorsport, cycling, and cricket event cards. Two fullscreen/Back cycles retained the player without another ExoPlayer initialization.
- Signed v1.0.9 UI checks passed airing-first calendar ordering and two more fullscreen/Back cycles. Visual checks confirmed hidden system bars and unobstructed video after controls timed out; Back retained portrait playback. Android data instrumentation passed again on v1.0.9.
- A real airing tennis listing opened beIN SPORTS 7 in the signed v1.0.9 APK and remained playing at a second check 15 seconds later. Visual inspection showed MotoGP footage with the beIN SPORTS 7 watermark, despite the guide listing tennis. This confirms channel playback and routing, **not correct event content or real-time broadcast freshness**. The app cannot correct an upstream schedule/feed mismatch.
- Temporary routing fixtures were restored after each completed run. Earlier emulator failures were resolved by restarting the device; they were not counted as passing checks.

The optional Android test argument `uiFixtures=seed` installs clearly identified public-sample routing fixtures; `uiFixtures=restore` restores the backed-up imported calendar and favorites. Production APKs contain no test fixture classes. Public sample playback cannot establish real sports broadcast coverage.
