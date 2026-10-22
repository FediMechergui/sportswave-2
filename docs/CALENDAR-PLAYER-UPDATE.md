# Calendar and player update - v1.0.8

The selected local date now drives the beIN MENA guide request and ESPN fixtures. Yacine's event API and imported events contribute explicit broadcast assignments. The guide is paginated and restricted to MENA channel IDs; replay entries are excluded. Partial provider failures retain cached listings and show an unavailable-feed message.

A tap during the published airing window opens the assigned channel inside SportsWave, resolving its current playback session. Upcoming and ended entries show details. Matches, individual sports, races, and sessions share the same rules. Exact participant/sport/time matches can attach published broadcast references to public scoreboards; unmatched fixtures remain unlinked. Provider outages still affect video delivery.

Event cards show participants, available crests, timing, scores, and broadcast names. Favorites retain event details across date changes. Search input survives background updates. Calendar cards load in batches of 40. Duplicate merging uses an identity index to avoid comparing every fixture pair.

Fullscreen reparents the existing Media3 view and retains the player, fills the landscape window, and hides system bars. Back returns to portrait playback before leaving the player. Error and retry controls remain available in fullscreen.

## Validation

- Signed release, debug APK, and Android test APK builds passed using cached Gradle 8.14.3. The user's wrapper setting was preserved.
- All 23 JVM tests passed. The 2,000-entry duplicate-calendar regression completed in 0.106 seconds in this run.
- Android instrumentation passed guide parsing, MENA/replay filtering, race/tennis classification, schedule cache serialization, saved event details, and existing catalog/import validation.
- A real Android calendar request returned 77 listings with no provider errors during this investigation. This verifies metadata delivery, not successful playback of every listed channel.
- Final UI verification is incomplete. Emulator attempts encountered application-not-responding dialogs, a rendering stall, Android system restarts during cold boot, and offline ADB connections. Fullscreen round trips and the six-sport sample routing harness did not complete successfully. They are not counted as passing checks.

The optional Android test argument `uiFixtures=seed` installs clearly identified public-sample routing fixtures; `uiFixtures=restore` restores the backed-up imported calendar and favorites. Production APKs contain no test fixture classes. Public sample playback cannot establish real sports broadcast coverage.
