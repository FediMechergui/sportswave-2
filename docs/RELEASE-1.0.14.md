# SportsWave 2 v1.0.14

Fresh signed APK rebuilt from the current main branch, versionCode 15, using the existing release signing certificate. Includes the provider endpoint recovery, channel-ID migration, same-channel fallback and identity guards shipped in 1.0.13.

This rebuild does not add VIPRow or the other newly researched web-player providers. Their availability and integration limits are recorded in the provider investigation reports; research documentation is not a playback integration.

Install `SportsWave-1.0.14.apk` as an update over the previous signed release. Provider-side failures, including unavailable or mismatched beIN feeds, remain subject to the limits documented in the recovery report.

Validation: release/debug/test APK builds succeeded; all 35 JVM tests passed. Verified APK version 1.0.14/versionCode 15 and the existing release certificate. No new emulator playback test was performed for this version-only rebuild.
