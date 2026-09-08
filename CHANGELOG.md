# Changelog

All notable changes to Notspot detector are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.19.0] - 2026-09-08

### Changed

- **Home screen layout** — renamed to NotSpotter; start/stop controls sit directly under the title; the RSRP histogram replaces the reception LED panel; removed the tagline and background-monitoring info cards.
- **Background monitoring** — battery exemption is requested automatically when you start monitoring (no separate panel).

## [1.18.8] - 2026-09-08

### Changed

- **RSRP histogram** — each bar now shows its share of all samples in the window as a percentage below the count.

## [1.18.7] - 2026-09-08

### Added

- **RSRP histogram sample window** — slider below the graph to choose how far back samples are counted, from 30 seconds to 5 minutes (default 30 seconds). Saved with your other settings and profiles.

## [1.18.6] - 2026-09-08

### Changed

- **RSRP histogram colours** — bars now use fixed signal bands: light blue above −80 dBm, green −80 to −95, yellow −95 to −110, orange −110 to −120, and bright red below −120.

## [1.18.5] - 2026-09-08

### Changed

- **RSRP histogram** — only bins with samples in the 30-second window are shown, and each active bar grows wider when fewer bins are visible.

## [1.18.4] - 2026-09-08

### Fixed

- **RSRP histogram axis labels** — dBm values were clipped to dots because the rotated labels sat in a box as narrow as the bars; labels now use a wider slot so −70, −75, etc. display fully.

## [1.18.3] - 2026-09-08

### Changed

- **RSRP histogram layout** — narrower bars and vertically oriented dBm labels so all 12 bins fit on one row without wrapping.

## [1.18.2] - 2026-09-08

### Changed

- **RSRP histogram bins** — bin width reduced from 10 dB to 5 dB (12 bins from −70 to −126 dBm).

## [1.18.1] - 2026-09-08

### Changed

- **RSRP histogram layout** — bars are now vertical with dBm labels along the bottom, like a classic histogram.

## [1.18.0] - 2026-09-08

### Added

- **RSRP histogram** — while monitoring, a new panel shows how often recent RSRP readings fell into each 10 dB bin from −70 to −126 dBm, using a rolling 30-second window.

## [1.17.2] - 2026-09-08

### Changed

- **Cellular metrics SIM row** — SIM value uses a smaller font and stays on one line so long labels (for example “System default · …”) no longer wrap.

## [1.17.1] - 2026-09-08

### Changed

- **Cellular metrics during no signal** — RSRP, RSRQ, EARFCN, and PCI show blank (—) while the debounced no-signal state is active, instead of stale readings from the modem.

## [1.17.0] - 2026-09-08

### Changed

- **Passive no-signal voice alerts** — in passive-only monitoring, while no signal is active the app plays the no-signal tone and announces the operator, radio technology, and “no signal” every 30 seconds (for example “E E, 4 G, no signal”). If 2G fallback is enabled and the phone drops to 2G while still out of signal, the technology change is announced and the 30-second no-signal reminders continue with the updated technology.

## [1.16.0] - 2026-09-08

### Changed

- **Settings profiles as JSON files** — each profile is saved as its own JSON file in app document storage, so profiles survive clearing app cache. Existing saved profiles are migrated automatically on first launch.
- **Share and import profiles** — export a profile via the system share sheet (email, Drive, messaging, etc.) and import a `.json` profile file received from someone else.

## [1.15.6] - 2026-09-08

### Changed

- **Passive signal thresholds layout** — tier settings are grouped and ordered Tier 1 (strongest) through Tier 6, then No signal. Each tier shows its click interval and RSRP boundary together instead of interleaved out of order.

## [1.15.5] - 2026-09-08

### Fixed

- **App crash on launch** — startup no longer crashes on physical devices when reconciling tier click intervals; the ViewModel init path was reading `audioVolumes` before that StateFlow was created.

## [1.15.4] - 2026-09-08

### Fixed

- **Physical device stability** — passive signal threshold sliders no longer use mismatched step counts that could crash when opening the settings panel; telephony/SIM reads on startup are wrapped so permission or OEM radio errors cannot take down the app.

## [1.15.3] - 2026-09-08

### Changed

- **Passive signal settings tier colours** — RSRP boundary sliders and tier click-interval controls now use the same numbered tier colours as Cellular metrics (blue Tier 1, dark/light green Tiers 2–3, amber/orange/red for Tiers 4–6, dark red for no signal).

## [1.15.2] - 2026-09-08

### Changed

- **Signal tier colours** — each numbered tier in Cellular metrics now has a distinct colour: Tier 1 blue, Tier 2 dark green, Tier 3 light green, then amber, orange, and red for Tiers 4–6.

## [1.15.1] - 2026-09-08

### Changed

- **Numbered RSRP tiers** — passive signal tiers are now labelled **Tier 1** (strongest) through **Tier 6** (weakest) in Cellular metrics and passive signal settings, replacing Mild/Good/Fair/Poor/Critical/Very strong names.

## [1.15.0] - 2026-09-08

### Added

- **Signal tier in Cellular metrics** — shows the passive alert tier (Very strong, Mild, Good, Fair, Poor, Critical, No signal, or Limited service) for the latest RSRP/RSRQ measurement, using the same boundaries as your passive signal settings.

## [1.14.2] - 2026-09-08

### Fixed

- **Noisy RSRQ passive clicks on very strong signal** — white noise now mixes into tier clicks when RSRQ is below your fair threshold even if RSRP is in the very strong band (previously noise was skipped for very strong RSRP).

## [1.14.1] - 2026-09-08

### Changed

- **Settings panel titles** — Alert sound volume → **Alert sound settings**; Alert thresholds → **Active mode alert thresholds**; Passive signal ranges → **Passive signal thresholds and signal pulse settings**.

## [1.14.0] - 2026-09-08

### Added

- **Very strong RSRP threshold** — adjustable in Passive signal ranges (−90 to −30 dBm, default −80 dBm). Replaces the fixed −75 dBm boundary.

### Changed

- **Very strong tone frequency** — plays at **125%** of your signal pulse frequency (Alert sound volume), so it stays distinct from other tiers as you retune the pulse.

## [1.13.0] - 2026-09-08

### Removed

- **Passive sound speed** — removed now that each RSRP tier has its own click interval. No-signal flatline and limited-service tones use fixed timing (same as the previous default speed of 5).

## [1.12.3] - 2026-09-08

### Fixed

- **Voice alert delay** — uses `Duration`-based coroutine delay (lint fix).

## [1.12.2] - 2026-09-08

### Fixed

- **Voice after alert tone** — spoken announcements now wait for the actual alert tone length plus a short gap (not a fixed 500 ms), so limited-service and other longer tones finish before TTS starts.

## [1.12.1] - 2026-09-08

### Added

- **Settings compatibility UI** — tier click sliders now enforce a minimum based on signal pulse duration, show effective play intervals (passive-only and with ping monitoring), and warn when an interval is much slower than the measurement cycle.
- **Profile round-trip tests** — verify saved profiles include per-tier click intervals, signal pulse frequency, passive sound speed, and voice alert toggles.

### Changed

- **Signal pulse duration** — increasing duration automatically raises tier click intervals that would otherwise overlap the pulse.
- **Profile load** — tier click intervals are reconciled with signal pulse duration when a profile is loaded or normalized.

## [1.12.0] - 2026-09-08

### Added

- **Per-tier signal pulse click intervals** — under Passive signal ranges, each RSRP tier has its own click interval slider (10–5000 ms). Defaults match the previous passive-only rates.

### Changed

- **Passive sound speed** — moved from Alert sound volume to Passive signal ranges. It now scales only no-signal flatline and limited-service tones; tier click timing uses the per-tier intervals above.
- **Settings compatibility** — at runtime, tier click intervals are never shorter than the signal pulse duration plus a 25 ms gap, so pulses cannot overlap. Combined ping + passive sessions use double the configured tier interval (same behaviour as before).

## [1.11.3] - 2026-09-08

### Added

- **Signal pulse frequency** — adjustable tone frequency (400–5000 Hz, default 600 Hz) for RSRP/RSRQ signal pulse alerts.

### Changed

- **Alert sound volume** — “Low &amp; very strong signal clicks” renamed to **Signal pulse**; “600 Hz signal pulse duration” renamed to **Signal pulse duration** (Test removed — use Test on Signal pulse volume above).

## [1.11.2] - 2026-09-08

### Changed

- **No-signal sensitivity** — flatline tone and voice alerts now require **two consecutive polls** before entering or leaving the no-signal state, reducing false triggers from brief signal dips.

## [1.11.1] - 2026-09-08

### Changed

- **No-signal tone frequency** — flatline / no-signal alert now uses **554 Hz** (D♭) instead of 600 Hz.

## [1.11.0] - 2026-09-08

### Added

- **Voice announcements for technology, no-signal, and limited-service changes** — optional spoken alerts (default off) under each alert’s volume control, with separate voice volume and Test button. Tone plays first, then the announcement, only when the state actually changes. Technology changes announce the new RAT (2 G, 4 G, 5 G); no-signal and limited-service announce entry and exit (“No signal” / “Signal restored”, “Limited service” / “Full service”).

## [1.10.3] - 2026-09-08

### Changed

- **Cell-change voice announcements** — channel, PCI, ARFCN, and BSIC values are now spoken digit by digit (e.g. “6 4 0 0” and “1 2 3”) instead of as whole numbers.

## [1.10.2] - 2026-09-08

### Fixed

- **Network name missing from cell-change voice test** — operator name is now read live for Test (and when idle on the main screen), with fallbacks from SIM/carrier info when the camped network name is blank.
- **Short operator names (e.g. EE) hard to hear** — acronyms are spaced for clearer TTS (“E E, Cell reselect…”).

## [1.10.1] - 2026-09-08

### Changed

- **Cell-change voice announcements** — now lead with the mobile network operator name (e.g. “EE, Cell reselect, channel 6400, PCI 123”) and speak at a slightly faster pace.

## [1.10.0] - 2026-09-08

### Added

- **Voice announcement on cell change** (default off) — optional spoken alert after the cell change bell, e.g. “Cell reselect, channel 6400, PCI 123”. Separate volume slider and Test button under Alert sound volume; included in settings profiles.

## [1.9.2] - 2026-09-08

### Fixed

- **Intermittent EARFCN/PCI display** — cell identity is now held across poll cycles when Android returns partial or transient nulls. Values are only cleared on genuine no-service or 2G-without-fallback conditions, not when RSRP dips briefly or the cellular ping path is unavailable. Registered serving cells also merge missing PCI/EARFCN fields from the same EARFCN when the OS splits them across reads.

## [1.9.1] - 2026-09-08

### Fixed

- **Alert audio stopping after ~10 minutes** — passive-only monitoring no longer enters a silent idle phase; tier clicks, flatline, and limited-service tones keep playing until you tap Stop. Active ping monitoring still stops ping tests after 10 minutes, but signal alert sounds now continue alongside KPI polling.

## [1.9.0] - 2026-09-08

### Added

- **Settings profiles** panel below Cellular metrics — save, load, and delete named profiles for all app settings: ping target and test interval, monitoring options (SIM, 2G fallback, quiet passive alerts, measurement cycle, passive sound speed), passive signal ranges (RSRP/RSRQ tiers, noisy RSRQ clicks, quiet alert thresholds), mock signal, alert thresholds, and alert sound volumes. Saving requires settings that differ from factory defaults; up to 24 named profiles are stored on device. Load applies immediately; changing SIM while monitoring restarts the session.

## [1.8.9] - 2026-09-08

### Changed

- **Noisy RSRQ passive clicks** — when enabled and RSRQ is below your fair threshold, passive click rate now follows **RSRP only**; RSRQ no longer forces faster critical-tier intervals while the noisy tone is active.

## [1.8.8] - 2026-09-08

### Added

- **Noisy RSRQ passive clicks** — optional checkbox under the RSRQ fair boundary (default off). When enabled, passive-only 600 Hz tier clicks mix in white noise while RSRQ is below your fair threshold, making poor signal quality easier to distinguish by ear.

### Changed

- **RSRQ fair at or above** slider range is now **−30 to −13 dB** (was −30 to −1 dB).

## [1.8.7] - 2026-09-08

### Changed

- **Passive sound speed** range extended to **1–20** (was 1–10); default remains 5. Higher values speed up passive tier clicks, flatline pulses, and limited-service tones further.

## [1.8.6] - 2026-09-08

### Added

- **Alert sound Test buttons** — each volume control in Alert sound volume has a Test button that plays a sample at the current level (disabled while monitoring is running).

### Changed

- **Passive sound speed** moved from Passive signal ranges to the Alert sound volume panel, alongside the other audio controls.

## [1.8.5] - 2026-09-08

### Added

- **Passive sound speed** slider (1–10, default 5) in Passive signal ranges — one control scales tier click intervals, no-signal flatline pulses, and limited-service tones together during passive-only monitoring. 5 matches the current rate; lower values slow all tiers proportionally; higher values speed them up.

## [1.8.4] - 2026-09-08

### Added

- **Quiet alert thresholds** — adjustable RSRP and RSRQ sliders under Monitoring options for quiet passive alerts (defaults −105 dBm and −20 dB), independent of tier band settings.

## [1.8.3] - 2026-09-08

### Fixed

- **Quiet passive alerts thresholds** — unmute conditions now derive correctly from your fair band settings: RSRP at or below the fair boundary, RSRQ below fair minus 2 dB (defaults −105 dBm and −20 dB). The monitoring options hint shows the live thresholds.

## [1.8.2] - 2026-09-08

### Added

- **Passive measurement cycle** slider (1–10 s, default 5 s) in Passive signal ranges — controls how often RSRP, RSRQ, and cell identity refresh during passive-only monitoring and passive idle KPI polling, independent of ping test interval.

## [1.8.1] - 2026-09-08

### Changed

- **Passive alert click rate** — passive-only signal tier clicks, no-signal flatline pulses, and limited-service tones now play at **2×** the previous rate (intervals halved). Active ping monitoring is unchanged.

## [1.8.0] - 2026-09-07

### Added

- **Mock signal mode** — in Passive signal ranges, enable mock signal to drive passive-only monitoring with manual RSRP and RSRQ sliders (−126 to −50 dBm, −30 to −1 dB) instead of live radio readings. Adjust values while running to test alert tiers, reception LED, and audio without moving.

## [1.7.3] - 2026-09-07

### Changed

- **RSRQ passive ranges** — simplified to a single adjustable fair/critical boundary (default −18 dB). Below it triggers critical alerts and poor reception; at or above is fair.

## [1.7.2] - 2026-09-07

### Added

- **Good RSRP tier** — adjustable boundary between fair and mild (default −100 dBm), with a 4 s alert interval between fair (2.5 s) and mild (5 s).

### Changed

- **Very strong** is fixed at **> −75 dBm** (800 Hz tone) and is no longer adjustable in passive signal ranges.

## [1.7.1] - 2026-09-07

### Changed

- **Passive signal ranges** — RSRP tiers simplified to five boundary sliders within a fixed **−126 to −50 dBm** range; RSRQ uses two tier boundaries within **−30 to −1 dB**. Each slider shows the band it defines instead of separate lower/upper controls per tier.

## [1.7.0] - 2026-09-07

### Added

- **Passive signal ranges** panel — configure upper and lower RSRP and RSRQ band edges for passive monitoring: alert click tiers, reception LED colours, no-signal flatline, and quiet-alert thresholds all follow your settings. Reset to defaults restores the original factory bands.

## [1.6.10] - 2026-09-07

### Fixed

- **EARFCN/PCI when there is no signal** — camped-cell identity is cleared whenever no-signal conditions apply (flatline, RSRP at or below −125 dBm, no cellular path, etc.), so stale neighbour cells from the OS are no longer shown.

## [1.6.9] - 2026-09-07

### Fixed

- **EARFCN/PCI after 2G ↔ 4G changes in passive mode** — cell identity now follows the active LTE/NR bearer from signal strength instead of a stale 2G network-type report; PLMN filtering is applied per radio type so a camped 2G neighbour no longer blocks LTE EARFCN/PCI after returning to 4G.

## [1.6.8] - 2026-09-07

### Added

- **Quiet passive alerts** checkbox (default off) under Monitoring options — during passive-only monitoring, mutes signal-tier clicks, cell-change bell, technology-change tone, and limited-service tone unless there is no signal, RSRQ is below −20 dB, or RSRP is at or below −105 dBm. No-signal tone is always played.

## [1.6.7] - 2026-09-07

### Changed

- **600 Hz signal pulse duration** slider range extended to **10–600 ms** (was 100–600 ms), with 10 ms steps for finer control at short lengths.

## [1.6.6] - 2026-09-07

### Changed

- **Very strong passive alert** (RSRP above −75 dBm) — 800 Hz pulse interval reduced from 5 s to **2.5 s**.

## [1.6.5] - 2026-09-07

### Changed

- **No-signal tone** (pulsed and continuous flatline) now uses **600 Hz**, matching the weak-signal tier pulses. Ping click sounds remain at 3200 Hz.

## [1.6.4] - 2026-09-07

### Changed

- **Very strong signal alert** (RSRP above −75 dBm) now uses an **800 Hz** sustained pulse so it is distinct from the 600 Hz tiers below.

## [1.6.3] - 2026-09-07

### Fixed

- **Very strong signal alert** — RSRP above −75 dBm now uses the same sustained **600 Hz** pulse as other signal tiers (respecting the pulse duration slider), instead of a short two-tone chirp.
- **Mild signal band (−76 to −95 dBm)** — interval clicks now play in this range; previously only very strong and fair-or-worse tiers triggered, leaving a gap with no 600 Hz alert or the wrong ping click sound.

## [1.6.2] - 2026-09-07

### Fixed

- **EARFCN/PCI on dual-SIM phones** — “System default (data SIM)” now uses a subscription-scoped telephony manager; serving cells are chosen by primary/secondary connection status and filtered by PLMN so the other SIM’s camped channel is no longer shown intermittently.

## [1.6.1] - 2026-09-07

### Added

- **600 Hz signal pulse duration** slider (100–600 ms, default 250 ms) under Alert sound volume; scales the burst length for all RSRP/RSRQ tier clicks proportionally.

## [1.6.0] - 2026-09-07

### Added

- **Technology change alert** — an upward frequency sweep (320–720 Hz) when the registered technology changes (e.g. 4G ↔ 5G, 5G EN-DC ↔ 4G, 2G ↔ 4G), separate from the EARFCN/PCI cell-change bell.
- **Technology change sweep** volume slider under Alert sound volume.

## [1.5.4] - 2026-09-07

### Changed

- **No usable signal** RSRP threshold lowered from −130 dBm to **−125 dBm** (no-signal tone at or below this level).

## [1.5.3] - 2026-09-07

### Fixed

- **Passive monitoring** — RSRP at or below **−130 dBm** is now treated as no usable signal, triggering the no-signal tone instead of weak-signal tier clicks (the modem can still report a camped cell at very low levels).

## [1.5.2] - 2026-09-07

### Changed

- **Low-signal tier alerts** frequency changed from 3200 Hz to **600 Hz** (250 ms sine burst; intervals unchanged).

## [1.5.1] - 2026-09-07

### Changed

- **Low-signal tier alerts** now use a sustained **3200 Hz sine burst** (same timbre as the no-signal tone, **250 ms**) instead of short decaying clicks, so the pitch is clearly audible at every interval.

## [1.5.0] - 2026-09-07

### Changed

- **Low-signal clicks** now use RSRP/RSRQ tiers on the low-signal volume slider (3200 Hz, same pitch as no-signal for weak tiers):
  - **RSRP above −95 dBm** (when alerting): every **5 s**, **150 ms** present click
  - **−95 to −105 dBm**: every **2.5 s**, **250 ms** sine burst
  - **−105 to −120 dBm**: every **1 s**, **250 ms** sine burst
  - **Below −120 dBm** or **RSRQ below −18 dB**: every **0.5 s**, **250 ms** sine burst

## [1.4.4] - 2026-09-07

### Changed

- **Passive-only monitoring** — RTT, jitter, packet loss, and successful ping counts are hidden from the cellular metrics card.

## [1.4.3] - 2026-09-07

### Changed

- Refactored `MainActivity` UI into a dedicated `MonitorApp` composable and updated lifecycle Compose dependencies (fixes Android Studio highlighting/errors on the `setContent` block).

## [1.4.2] - 2026-09-07

### Changed

- **Passive-only monitoring** — low-signal and very-strong-signal clicks now play every **5 seconds** (fixed interval, not RSRP-scaled).

## [1.4.1] - 2026-09-07

### Changed

- **Passive-only monitoring** — the status LED shows **Reception** from RSRP/RSRQ instead of connection quality: red below −105 dBm or RSRQ below −18 dB, yellow from −105 to −95 dBm, green above −95 dBm (including stronger than −85 dBm).

## [1.4.0] - 2026-09-07

### Added

- **Passive-only monitoring** — second start control runs signal KPI polling and alert audio with no TCP ping tests; uses the same 10-minute timeout to silent KPI-only polling until Stop.
- **SIM selector** — choose which SIM to monitor (defaults to the system data SIM); monitoring restarts automatically when the selection changes.
- **2G fallback toggle** — optionally read 2G signal KPIs when LTE/NR is unavailable (no ping tests on 2G).
- **Limited service** — network service state shown in metrics (in service / limited / no service) with a dedicated **limited service tone** (alternating 880/660 Hz beeps; 300 ms on/off pulse on 2G limited service without home GSM).
- **Limited service tone** volume slider under Alert sound volume.
- **Continuous no-signal tone** when completely out of service on all technologies and no emergency SOS is available on any SIM (pulsed no-signal tone in all other cases).
- **2G metrics** — GSM signal (dBm), ARFCN, and BSIC when registered on 2G.

### Changed

- **Low signal** and **very strong signal** volume sliders merged into a single **Low & very strong signal clicks** control.
- With **2G fallback** enabled, the no-signal tone still plays when 4G/5G is lost even if 2G GSM remains registered.
- RTT graph hidden during passive-only monitoring sessions.

### Fixed

- **Low-signal clicks** did not play when connection-test clicks were suppressed (“Silent when connection is good”) but low-signal volume was turned up.

## [1.3.0] - 2026-09-07

### Added

- **Cell change bell volume** slider under Alert sound volume (independent of connection clicks).

### Changed

- **No-signal tone** is now a pulsed alert: 250 ms on, 1000 ms off, at 3200 Hz (was a continuous tone).
- **Low-signal** and **degraded/poor** clicks use 3200 Hz with a **150 ms** duration (same frequency as no-signal; good-connection clicks stay short at 6 ms).

### Fixed

- **Packet loss** could stay stuck (e.g. at 20%) after the connection recovered; it now uses the last 5 test cycles (matching jitter) and clears old failure history after 3 consecutive clean cycles.

## [1.2.0] - 2026-09-07

### Added

- **Passive idle mode** — after 10 minutes of active monitoring, ping tests and Geiger audio stop automatically; the foreground service continues polling RSRP, RSRQ, EARFCN, PCI, and related signal KPIs until you tap Stop.
- **Cell change bell** — a bell chime when LTE or NR EARFCN or PCI changes (including 5G EN-DC anchor and NR leg).
- **Very strong signal chirp** — when RSRP is above −75 dBm on a good connection, a two-tone chirp plays shortly after the connection test clicks.
- **Per-alert audio volume** sliders for connection clicks, low signal, very strong signal, and no-signal tone.
- Configurable **good-connection clicks per test** (1–10) on the alert thresholds card.
- App **version label** in the top-right of the monitor screen.

### Changed

- **5G EN-DC** now shows both the LTE anchor and NR EARFCN/PCI; RSRP and RSRQ continue to reflect the LTE anchor only (not SS-RSRP/SS-RSRQ).
- Jitter uses a rolling last-5-sample window with stale high RTT values pruned on recovery.
- When RTT or jitter exceeds 1000 ms, Geiger click interval is divided by 10.
- Low-signal warning click duration increased from 12 ms to 100 ms.

## [1.1.0] - 2026-09-07

### Added

- EARFCN / NR-ARFCN and PCI for the registered serving cell in cellular metrics.
- Location permission request for cell identity (required by Android 10+; GPS is not tracked).
- **Silent when connection is good** option in alert thresholds (suppresses Geiger clicks when the last test rates as good connection).
- Rolling 20-cycle window for packet loss, aligned with RTT jitter sampling.

### Changed

- Technology labels now show **4G** (LTE) and **5G** (NR) instead of LTE / 5G NR.
- Each test cycle always sends one RRC warm-up probe plus the configured measured pings (e.g. setting **1** runs 2 probes: warm-up + 1 measured).

### Fixed

- Crash on some devices when reading cell identity without location permission.
- EARFCN and PCI staying blank until location permission was granted.
- Packet loss staying elevated and Geiger clicks continuing after returning to good signal and RTT (loss was cumulative for the whole session).

## [1.0.0] - 2026-09-07

### Added

- Geiger-counter-style monitoring of cellular mobile data quality (cellular-only TCP probes, not Wi‑Fi).
- Foreground service with background monitoring, wake lock, and persistent notification.
- Metrics: operator, technology, RTT, jitter, packet loss, RSRP, RSRQ, successful pings.
- RTT graph (rolling 60 s window, reversed Y-axis).
- Configurable ping target, pings per test, and test cycle interval.
- Configurable alert thresholds for good/poor RTT, jitter, and packet loss.
- Geiger audio: click rate scales with connection quality; flatline tone when no mobile data; warning click on weak RSRP/RSRQ.
- Battery optimization prompt for reliable background monitoring.
