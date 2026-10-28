# SportsWave 2 v1.0.13

Restores catalogue and playback resolution after the provider changed its API hostname and channel IDs. Migrates 142 verified channel identities, refreshes old catalogue caches, and tries alternate supported stream descriptors for the same channel.

- Makes all 49 bundled public alternatives discoverable before remote catalogue sync, with provider recovery for the six built-in Arabic sports feeds.
- Prevents reversed fixtures from merging scores or broadcasts; fixes AFC/CAF football classification and selected-date Bundesliga results.
- Adds playback history and channel-interface improvements accumulated since v1.0.9.
- Stops known XTRA/generic-feed mislabels that share SPORTS 7/8 media paths, with an explicit explanation in the player.

Validation: signed release built and installed on an Android emulator; 35 JVM tests and three Python audit tests passed. All 80 beIN Sports and 13 entertainment records were sampled individually. SPORTS 1–8 decoded at 720P; SPORTS 1/8 also played through the app interface. The catalogue refreshed to 675 listings, and calendar checks returned 67 listings without provider errors.

SPORTS 9 still returns upstream 404. Genuine Arabic XTRA and MAX are unresolved. Provider video may contain replays or advertisements; delivery does not prove a particular current event is airing. No temporary signed sessions are bundled.

Install `SportsWave-1.0.13.apk` (Android 8.0+). This is a signed APK update using the existing release certificate, not a Google Play publication. Full evidence: [beIN recovery report](BEIN-RECOVERY-2026-10-28.md).
