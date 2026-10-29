# SportsWave logo sources

Snapshot: 29 October 2026. The bundled index contains 1018 image records (several sources may represent the same club). Images load on demand; this is not a claim of 1018 unique clubs or complete worldwide coverage.

- ESPN public team metadata: 622 records across football, basketball (NBA/WNBA), American football, baseball, ice hockey and rugby. URLs and original league endpoint are retained per record in app/src/main/assets/team-logos.json. Example primary endpoint: https://site.api.espn.com/apis/site/v2/sports/basketball/nba/teams
- luukhopman/football-logos: 396 current-season European club records, pinned to commit 2a3978f0b4730645c205d855a4bda54c161183e9. https://github.com/luukhopman/football-logos
- Live fixture artwork: the Yacine calendar, ESPN scoreboards, FilGoal, Livekora and Yalla retain their published participant images. These extend coverage beyond the bundled directory. Placeholder and non-HTTPS image values are rejected.
- Tabler Icons: sport-category artwork across the sport filters; source https://github.com/tabler/tabler-icons and MIT licence included in app/src/main/assets/sport-icons/LICENSE.txt. Hockey artwork is an original vector in the same folder. Category icons do not pretend to be club crests.

Logo requests have bounded timeouts, response sizes and bitmap dimensions. Failed primary images try alternative indexed marks of the same identified team; ambiguous/unidentified teams retain initials. Team marks belong to their respective owners; source references identify the collections, not a blanket licence to every trademark.

Team-name matching uses punctuation/accent normalization, reviewed multilingual aliases and league-specific city aliases. It does not use substring or fuzzy similarity. NBA/WNBA and other city-name collisions cannot choose an arbitrary team. Men/women/youth squad markers, sport and kickoff window constrain fixture merging.
