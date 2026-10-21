# Data and channel coverage

Verified on 21 October 2026.

| Feature | Connected source | Coverage and limits |
| --- | --- | --- |
| Football fixtures and scores | OpenLigaDB `getmatchdata/bl1` | Bundesliga current matchday. Community updates may lag. Future matches have no invented score. |
| Additional scoreboards | ESPN public website JSON | Champions League, Premier League, LaLiga, Ligue 1, Serie A, NBA, NFL. Current provider date; endpoints have no guaranteed integration contract. Live status and clocks are displayed only when supplied by the provider. |
| Team crests | ESPN logos / OpenLigaDB teamIconUrl | PNG/JPEG and SVG rendering, with initials on download failure. No crests assigned to fictional demo teams. |
| Race classification | OpenF1 latest session + drivers + session_result | Latest available Formula 1 results, including position, gaps, laps and retirements. This is not live timing. |
| Included live channel | Red Bull TV publisher HLS | Video rendered inside SportsWave in the emulator. Availability may vary by region. |
| beIN / other Yacine providers | User-imported direct URL or configured HTTPS resolver | No bundled renewable integration. The investigated signed beIN session was temporary and not a permanent source. |
| Event-to-channel routing | Explicit imported broadcast references | Public fixtures do not supply TV broadcast assignments; no channel is guessed from sport or team. |

The provider calendar is now the default. The fictional calendar remains explicitly selectable for UI/player demonstrations. Scores refresh every 30 seconds while the Live screen is visible. F1 public results are distinguished from live race order; the latter requires a separate connected feed. No provider account secrets or captured signed URLs are stored in this repository.
