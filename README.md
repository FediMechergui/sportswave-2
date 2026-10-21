# SportsWave 2

<img src="docs/icon.svg" width="108" alt="SportsWave launcher icon">

Native Android sports discovery, calendar, and internal video player. Kotlin, native Android views, and AndroidX Media3. Android 8.0+; no companion player required.

## Build

Open the repository in Android Studio, set the SDK directory in ignored `local.properties`, and run:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Install `app/build/outputs/apk/debug/app-debug.apk`. The application package is `com.sportswave.app`, separate from the investigated apps.

Download the signed preview APK from this repository?s GitHub Releases. Preview builds include public playback samples; live broadcast sessions are not bundled.

## Verification

The build and nine JVM tests pass. The emulator instrumentation checks catalog/calendar import, rejection of invalid calendars without partial source changes, duplicate-ID updates, broadcast reference matching, resolver configuration, deletion, and persistent favorites. Tests use a separate preferences file. A temporary beIN session rendered video during local testing, with intermittent buffering. That expiring session is not distributed.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.sportswave.app.test/com.sportswave.app.tests.CatalogInstrumentation
python scripts/smoke_android.py
```

The UI smoke script defaults to `emulator-5556`; set `SPORTSWAVE_DEVICE` for another device. It exercises discovery, schedule, favorites, internal HLS/DASH playback, and landscape fullscreen. It needs network access to the public samples. Screenshots and logs are written to `artifacts/`.

## Included behavior

- Discover, Schedule, Channels, and Saved navigation.
- Sport filters covering football, basketball, tennis, motorsport, rugby, handball, volleyball, American football, padel, cricket, combat, cycling, athletics, swimming, golf, and other sports.
- Local-time dates, event search, event details, and persistent favorites.
- Direct HTTPS source entry, source removal, and JSON catalog/calendar import through Android's file picker.
- Internal HLS, DASH, and progressive playback; buffering, pause/seek, retry, quality cap, speed, fit/fill, fullscreen, and optional Widevine configuration.
- Provider-neutral HTTPS resolver support for fetching fresh playback sessions rather than hardcoding expiring URLs.

The bundled calendar is explicitly illustrative. The included HLS and DASH sources are public playback tests, not broadcasts of the illustrative fixtures. No live scores or production sports broadcasts are claimed. An official beIN guide link is available from Schedule. The official MENA/France guide was researched for the breadth of sports; its listings were not copied into a purported live feed.

## Add real sources and events

Use Channels → Add source for a direct HTTPS media URL, or import a JSON array of sources. Import a JSON object with `sources` and `events` for a combined catalog. See `examples/catalog.json` for a usable sample. Sample event dates are fixed, not automatically rolled forward.

Source fields: `id`, `name`, `sport`, `url`, optional `mime`, `userAgent`, `headers`, `licenseUrl`, `licenseHeaders`, `resolverUrl`, `resolverHeaders`, `provider`, `channelId`, and `aliases`. Supply a direct `url` or a `resolverUrl`. License configuration is Widevine; other schemes are not implemented. HTTPS is the default. A catalog source may explicitly set `allowHttp: true` for an HTTP media source; resolver and license endpoints still require HTTPS. Android permits HTTP transport for these opted-in media sources; TLS certificate validation remains enabled for HTTPS. Source settings live in application-private preferences, excluded from Android backup. Prefer a resolver to persisting short-lived credentials on the device.

Event fields: `id`, `sport`, `competition`, `home`, `away`, `startUtc` (ISO-8601 UTC timestamp), optional `endUtc` and `sourceId`. `sourceId` links a calendar event to a source. Imported calendars replace the previous imported events; imported sources merge by ID. Favorites remain stored separately. Search applies on the keyboard Search action.

## Calendar-to-broadcast routing

Events may also include `broadcasts`, an ordered array of references using `provider` and `channelId`, `provider` and `channelName`, or an explicit `sourceId`. Provider names need to agree with the source catalog, rather than referring to its distributor: use `provider: "beIN"` or `provider: "Alkass"`, even when both were found through Yacine.

```json
{
  "broadcasts": [
    {"provider": "beIN", "channelName": "beIN SPORTS 1"},
    {"provider": "Alkass", "channelName": "Alkass 1"}
  ]
}
```

The importer validates every broadcast reference before saving the catalog and calendar together. Matching uses exact source IDs first, then channel IDs scoped by provider, or exact normalized names/aliases. Quality suffixes are ignored, but channel numbers are preserved: channel 1 never matches channel 11. It never guesses a channel from a sport or competition. A source's `sport: "Multi-sport"` keeps it visible across sport filters.

Tapping a mapped event opens its first matching channel directly inside SportsWave. Long-press the event for details and other broadcasts. When multiple sources match, the player offers **Choose another broadcast**, restricted to the event's matching sources. No unrelated channel is substituted after a playback failure. The calendar must supply the broadcast references; the app does not automatically obtain them from beIN or Yacine's encoded API.

See `examples/multi-provider-calendar.json` for a working public-video routing demonstration using two clearly named sample providers.

## Resolver contract

Configure a source with your provider's HTTPS `resolverUrl`. SportsWave performs a GET on each player opening and retry, applies `resolverHeaders` to that request only, and expects a plain JSON object:

```json
{
  "url": "https://your-provider.example/session/manifest.m3u8",
  "mime": "application/x-mpegURL",
  "userAgent": "SportsWave/1.0",
  "headers": {},
  "licenseUrl": "",
  "licenseHeaders": {}
}
```

`url` is required. Supply stream-specific headers, not credentials intended only for the resolver. The response remains in memory. Playback headers and license headers are configured separately. The client does not log full stream URLs. Retry refreshes the session; mid-stream automatic token renewal is not implemented. Headers applied to media requests must be scoped appropriately by the provider, particularly if its delivery redirects across hosts.

The Yacine investigation established a rotating API → redirect → HLS chain, not a supported API for a new app. No reverse-engineered token generation, API response decryption, proprietary native code, edge URLs, or branded assets are embedded. Production operation requires an authorized content provider and a calendar source. The beIN website is a guide reference, not a streaming API.

## References and assets

- Media3: https://developer.android.com/media/media3/exoplayer/hello-world
- beIN MENA guide: https://www.beinsports.com/en-mena/tv-guide
- beIN France guide: https://www.beinsports.com/fr-fr/tv-guide
- HLS sample: https://test-streams.mux.dev/
- DASH sample: https://storage.googleapis.com/shaka-demo-assets/angel-one/dash.mpd
- Bundled sports photography: Unsplash image IDs `photo-1522778119026-d647f0596c20`, `photo-1546519638-68e109498ffc`, `photo-1554068865-24cecd4e34b8`. These are illustrative editorial images, not fixture imagery. Racing uses an original vector illustration.

This first version is an Android app, not a website or Android TV client. Arabic translations, push notifications, authentication, automated live calendar ingestion, casting, downloads, background playback, and billing are outside the implemented scope.

## Signed release builds

Set `SPORTSWAVE_KEYSTORE`, `SPORTSWAVE_STORE_PASSWORD`, and `SPORTSWAVE_KEY_PASSWORD` in your build environment, using a private keystore with alias `sportswave`. Run `./gradlew :app:assembleRelease`. Keep the keystore and credentials outside this repository; future updates must use the same signing key.

The original launcher artwork is a coral wave monogram with a white play accent, including adaptive and monochrome Android icons.
