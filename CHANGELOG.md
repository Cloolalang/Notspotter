# Changelog

All notable changes to Notspot detector are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- **Documentation** — [MOCK_NETWORK_SCENARIOS.md](MOCK_NETWORK_SCENARIOS.md) (agreed mock trigger states per scenario) and [SETTINGS_PROFILES.md](SETTINGS_PROFILES.md) (profile JSON capture scope, including full `passiveMock` block). Updated [RXSS_CATALOGUE.md](RXSS_CATALOGUE.md), [VOICE_ANNOUNCEMENTS.md](VOICE_ANNOUNCEMENTS.md), and [README.md](README.md) cross-links; RXSS **0**, **11**, **12**, **20**, **23** rows reflect current voice implementation.

## [2.9.0] - 2026-09-10

### Added

- **WiFi calling detection (RXSS 31)** — When WiFi calling / VoWiFi is registered as the in-service transport (`ServiceState.getNetworkRegistrationInfoList()`, API 30+, with a legacy `TelephonyManager.getDataNetworkType()` IWLAN fallback below API 30) and there is no cellular RAT camped or measurable RSRP/RSRQ, the app now classifies this as its own state — RXSS **31**, "WiFi calling, no cellular signal" — instead of misreporting it as RXSS **10** (LTE/NR no signal) or, worse, RXSS **11** (searching for home 2G). Voice: "{operator}, wifi calling, no cellular signal" on entry / every 30 s, "{operator}, cellular signal restored" on exit — reusing the existing no-signal voice toggles. The **Technology** metric now reads "Wifi Calling, no mobile data" instead of "—" while this state is active. A new **WiFi calling (no cellular)** mock scenario lets this be tested without forcing real WiFi calling on-device. See [RXSS_CATALOGUE.md](RXSS_CATALOGUE.md), [VOICE_ANNOUNCEMENTS.md](VOICE_ANNOUNCEMENTS.md), and [MOCK_NETWORK_SCENARIOS.md](MOCK_NETWORK_SCENARIOS.md).
- **Dedicated RXSS 31 sound settings section** — Passive Signal Settings now has its own "RXSS 31" camp-tier section (sound toggle, pulse duration, and click interval — `wifiCallingTierSoundEnabled`, `wifiCallingTierPulseDurationMs`, `wifiCallingTierClickIntervalMs`), in line with every other RXSS section, instead of being silently folded into RXSS 10's controls. Tone volume and pulse frequency are still shared with the RXSS 10/0/11/15 no-signal group (as those tiers already share with each other), and the voice toggle/volume remain shared too — the section header notes this. Settings profiles capture the three new fields.

### Fixed

- **Searching-2G false positive during WiFi calling** — The RXSS 11 ("searching for home 2G") condition previously triggered whenever LTE/NR signal was lost, regardless of cause. Since WiFi calling reports `IN_SERVICE` with no cellular RAT/RSRP, it satisfied every RXSS 11 condition and was wrongly announced as "searching 2 G". `computeSearching2gFallbackActive()` now also requires WiFi calling to be inactive.
- **Stale EARFCN/PCI shown during no-signal states** — The Metrics panel's EARFCN/PCI/BSIC/NR band rows could keep displaying the last-known cell identity (carried forward by `CellIdentityStabilizer`'s flicker-smoothing) after signal was lost, misleadingly implying the phone was still camped on that cell. These fields are now blanked out (`—`) whenever the current RXSS is any no-signal state (dead zone, LTE/NR/2G no signal, searching 2G, limited-service no-signal overlays, WiFi calling, etc.) — see `ConnectivityStats.isInNoSignalRxss()`.

### Changed

- **App title and section panel titles are now bold Sushi green** — The "NotSpotter" app title and every settings/metrics card's title text (Metrics, Cellular signal thresholds, Ping settings, Monitoring settings, Audio volume, Mock network state, RSRP histogram, RTT graph, Settings profiles, Master voice announcements) are now bold and colored Sushi green `#79A52B`, matching the new brand accent. The passive signal thresholds panel's per-tier readability colors and the RSRP histogram bar colors are unchanged.
- **New brand color scheme** — The main UI's Material theme (primary/secondary/tertiary colors and their container variants, light and dark) now uses the brand palette: Bondi Blue `#0097A6` (cyan, primary), Scarpa Flow `#485463` (slate, secondary), and Sushi `#79A52B` (green, accent/tertiary), replacing the default Material purple palette. Dynamic (Material You wallpaper-based) color is now off by default so this palette is actually shown on Android 12+. This only touches the app's chrome (buttons, top bar, selection highlights, etc.) — the passive signal thresholds panel's tier-readability text colors and the RSRP histogram bar colors are semantic/functional and were intentionally left unchanged.

## [2.7.1] - 2026-09-10

### Fixed

- **Regression: EARFCN/PCI/network mode not displaying at all** — The 2.7.0 dual-SIM cross-contamination mitigation required an *explicit* PLMN match before accepting any cell identity whenever a second SIM was active. In practice, the expected/serving PLMN and/or the cell identity's own MCC/MNC are frequently blank or unreliable on real devices — especially via a subscription-scoped `TelephonyManager` — so the stricter check ended up rejecting every cell and blanking out EARFCN, PCI, and related metrics, regardless of whether one or two SIMs were active. Reverted to the previous best-effort behavior (accept an unverifiable cell only when nothing else explicitly matched; still reject an explicit PLMN mismatch). The underlying dual-SIM cross-contamination report from before 2.7.0 is not further mitigated by this release — a safer fix needs more reliable per-SIM signal than this device class provides before it's worth re-attempting.

## [2.7.0] - 2026-09-10

### Added

- **5G band shown in cellular metrics** — When camped on 5G (standalone or EN-DC), the cellular metrics panel now shows the serving NR operating band (e.g. "n78") alongside NR-ARFCN/PCI. Read directly from the modem via `CellIdentityNr.getBands()` (Android 11+) rather than derived from the NR-ARFCN, since NR-ARFCN channel ranges overlap across multiple bands and only the modem-reported band is unambiguous. Shows "—" on older Android versions or when unavailable.

### Fixed

- **Dual-SIM cell-identity cross-contamination** — On some dual-SIM devices, `getAllCellInfo()` can leak a "registered" cell entry belonging to the *other* active SIM's network with no PLMN attached, which could get displayed as this SIM's EARFCN/PCI. When a second SIM is active, the app now requires an explicit PLMN match before accepting an LTE/NR/GSM cell identity, instead of guessing from unverifiable cells — unverifiable readings are now suppressed (showing "—", or the last-known-good value via the existing stabilizer) rather than potentially showing another operator's channel. This uses only live PLMN comparison; no hardcoded operator/EARFCN tables were added, keeping the app non-geo-specific.
- **Foreground service time-limit handling on long screen-locked runs** — On Android 15+, the monitoring service's `dataSync` foreground service type is limited to ~6 hours of runtime per rolling 24h window; without handling this, the service would be forcibly stopped by the system once the budget ran out (most likely during a long overnight screen-locked session), silently ending monitoring. The service now implements `Service.onTimeout()`, stopping itself cleanly and posting a notification asking the user to reopen the app to resume monitoring, instead of failing silently or risking an ANR.

## [2.6.0] - 2026-09-10

### Added

- **Master voice announcements switch** — New on/off toggle near the top of the main screen that mutes every spoken voice announcement (VA-1 through VA-18 and beyond) with a single tap, without touching any individual voice setting. Alert tones, bells, click sounds, and vibration are unaffected; the "preview" buttons in Settings still work while the master switch is off, so a voice can still be auditioned. Persisted with the rest of the audio settings and included in settings-profile export/import.

### Changed

- **RXSS 9 "speak band" announcement now speaks natural number words** — The alternative cell-reselect band announcement (`cellChangeSpeakBandEnabled`) previously spoke digits one at a time (e.g. "band 2 0", "band L 8 0 0"), which could be misheard or clipped by some TTS engines. It now speaks whole numbers ("band, twenty", "band, L eight hundred"), with a comma pause after "band" to stop the word being swallowed/clipped into the following number (reported as sounding like "bunt").

### Fixed

- **Duplicate "signal low" 30s announcement on limited-service visited 2G weak overlay** — On a limited-service visited-2G camp with a weak (RXSS 8) overlay, both the tier5-style periodic voice job and the 2G-fallback periodic voice job independently qualified to speak the same "…visited, 2G, signal low" announcement, so it played twice back-to-back every 30 s. The 2G-fallback job's weak-voice branch no longer fires for this limited-service case, leaving the tier5-style job as the single source of that announcement.

## [2.5.0] - 2026-09-10

### Added

- **"Home operator 5G ENDC" mock scenario** — New passive mock network scenario simulating a home-operator LTE anchor with an NR secondary carrier (EN-DC), alongside the existing 4G/2G/alt-operator/no-service/searching scenarios, with full settings, UI, and documentation wiring.
- **RXSS 9 (cell reselect) alternative "speak band" announcement** — New opt-in setting (`cellChangeSpeakBandEnabled`) that, on cell reselect, replaces the spoken "cell reselect, channel …, PCI …" with the E-UTRA band derived from the LTE channel (EARFCN), e.g. EARFCN 6300 → band 20. Two styles are available (`cellChangeBandNamingStyle`): **band number** (e.g. "band, twenty") or the band's common **MHz nickname** (e.g. "band, L eight hundred" for band 20). Falls back to the normal channel/PCI phrasing when there is no LTE channel to map (2G-only reselect). New controls added under the RXSS 9 cell-change section of Passive signal thresholds.

### Changed

- **Passive signal thresholds panel colors** — The per-tier accent colors used in the Passive signal thresholds panel are now theme-aware, using higher-contrast light/dark color pairs so text and labels stay readable against both light and dark backgrounds.

### Fixed

- **Cell-reselect voice (VA-10) no longer fires during no-signal / searching states** — Cell-reselect announcements could still speak a stale channel/PCI while searching for signal with no camped cell (e.g. RXSS 11 searching 2G), or while camped with no RSRP/RSRQ reading yet (mid-debounce right after losing signal), because the no-signal guard only checked the catalogue's narrower "Signal = No" tier list. The guard now also checks the debounced no-signal flag and the "no measurement yet" tier directly, suppressing VA-10 correctly in both cases.
- **Limited service 4G/2G missing 30s cycling voice announcement (VA-14)** — VA-14's periodic "limited service" reminder was incorrectly suppressed whenever any measurable RSRP/RX overlay applied, not just the critical/weak overlay that has its own dedicated periodic voice (VA-15/VA-18). This made VA-14 effectively dead for any limited-service state with usable signal. VA-14 now cycles correctly for RXSS 12/13 overlays 1–5 and 7, while VA-15/VA-18 continue to take over on the critical/weak overlay (6/8).

## [2.4.0] - 2026-09-10

### Added

- **Independent Level Range click intervals** — RXSS 2–5 (Level Ranges A–D) each now have their own click-interval slider, instead of a single "Click interval" value shared across all four ranges. Volume and frequency remain shared; each range's interval is floored against its own pulse duration.

## [2.3.0] - 2026-09-10

### Added

- **Independent Level Range signal pulse durations** — RXSS 2–5 (Level Ranges A–D) each now have their own signal pulse duration slider, instead of a single value shared across all four ranges. Volume, frequency, and click interval remain shared.

### Fixed

- **Level Range / signal-low pulse durations silently reset on every restart** — RXSS 2–5 (Level Ranges A–D) and RXSS 6 (signal low / critical) each have their own pulse-duration setting in the data model, but it was never saved to device storage or included in settings-profile exports, so it silently reverted to a 250 ms default every time the app restarted or a profile was reloaded — regardless of what was configured. These durations are now persisted and included in settings-profile JSON, with existing values migrated from the previous shared duration on first load so nobody's configured pulse length changes unexpectedly.
- **RXSS 1 (signal high) pulse duration had no effect** — The "RXSS 1 signal pulse duration" slider (shared with the Global alert sound settings duration) was saved correctly but never actually used during playback; very strong signal pulses always played at a fixed 250 ms regardless of the configured value. Playback now uses the configured duration.

## [2.2.28] - 2026-09-09

### Fixed

- **Title bar operator** — The top **NotSpotter - {operator}** label always uses the **home SIM operator** (`homeNetworkOperatorName`), not the camped visited network. Visited operator remains on the metrics row (`Home · Visited`) and in voice announcements.

## [2.2.27] - 2026-09-09

### Fixed

- **Dead zone voice clash** — Entering **RXSS 0** (mock no service / complete dead zone) no longer also speaks **“{operator}, 4 G, no signal”** (**VA-1**). **VA-3** dead-zone entry is the only immediate no-service voice while `isCompleteNoService` is true, including when `noSignalActive` debounces in on a later poll.

## [2.2.26] - 2026-09-09

### Fixed

- **Visited limited-service signal restored** — Leaving **RXSS 20** (visited 4G no signal) or **RXSS 23** (visited 2G no signal) now speaks **“{operator} visited, {tech}, signal restored”** when RSRP recovers to a usable overlay. Recovery straight into weak signal (**12·6** / **13·8**) still uses **“signal low”** instead of **“signal restored”**, matching home **RXSS 10** behaviour.

## [2.2.25] - 2026-09-09

### Fixed

- **Visited limited-service voice** — Signal-low and no-signal announcements on visited-operator limited service now use **“{operator} visited”** phrasing (e.g. “E E visited, 4 G, signal low” / “E E visited, 4 G, no signal”) instead of naming the visited network as home. **RXSS 20** and **23** play camp signal-pulse clicks and no-signal voice using the same controls as **RXSS 10** / **15**; **RXSS 13·8** weak 2G overlay triggers **VA-18** signal-low voice.

## [2.2.24] - 2026-09-09

### Fixed

- **Mock visited 4G/2G RSRP slider** — Moving the mock RSRP control now refreshes stats immediately during passive mock monitoring, so **RXSS 12/13** overlays (**1–6**, **7–8**, **20**, **23**) update as you slide. Limited-service camps keep RSRP for overlay detection, signal-strength pulses play for concurrent overlays instead of being blocked by camp-tier audio, and no-signal overlays stop tier **12**/**13** camp clicks.

## [2.2.23] - 2026-09-09

### Fixed

- **RXSS 12 / 13 signal overlays** — Visited-operator limited service now detects **no-signal overlays** (**RXSS 20** on 4G, **RXSS 23** on 2G) when RSRP/RX falls below the no-signal threshold, and **signal-level overlays** (**RXSS 1–6** on limited 4G, **RXSS 7–8** on limited visited 2G) when signal is measurable. The signal tier row shows composite labels (e.g. “RXSS 12 · 6”). **VA-14** is suppressed on **20**/**23**; **VA-8**/**VA-15** signal-low detection works on limited 4G with weak RSRP.

## [2.2.22] - 2026-09-09

### Fixed

- **Visited 2G limited-service voice clash** — On RXSS **13** (limited visited 2G), the 30 s **2 G camped** repeat (**VA-16**, e.g. “E E, 2 G”) no longer runs alongside **VA-14** (“Vodafone home, E E visited, 2 G, limited service”). **VA-18** is likewise suppressed while **VA-14** applies.

## [2.2.21] - 2026-09-09

### Fixed

- **Build** — Missing import for `resolveCampedVisitedOperatorName` in `MonitorState` (cell reselect visited-operator voice).

## [2.2.20] - 2026-09-09

### Changed

- **Cell reselect on visited network** — Cell reselect voice (**VA-10**) now appends **visited** to the camped operator when on a visited PLMN (e.g. “E E **visited**, 4 G, cell reselect, …”), matching limited-service operator phrasing.

## [2.2.19] - 2026-09-09

### Changed

- **Mock network operator names** — Mock home operator is now **Vodafone** and visited operator **EE** (replacing “Mock Home” / “Mock Visited”) so limited-service and other voice announcements match real operator phrasing during testing.

## [2.2.18] - 2026-09-09

### Changed

- **Limited-service dual-operator voice** — When home and visited PLMNs differ, limited-service announcements (entry, operator change, and 30 s repeat) now speak **home first, then visited**, each with a role word: e.g. “Mock Home **home**, Mock Visited **visited**, 4 G, limited service”. Single-operator limited service is unchanged.

## [2.2.17] - 2026-09-09

### Fixed

- **Visited-operator limited-service voice** — When camped on a visited network, limited-service announcements now speak the **visited operator first**, then home (e.g. “Mock Visited, Mock Home, 4 G, limited service”). Mock visited-operator scenario renamed from “Mock Alt” to **Mock Visited** so TTS no longer garbles duplicate “Mock …” prefixes.

### Changed

- **Limited-service operator order** — Dual-operator limited-service VA (entry and 30 s repeat) uses visited-then-home order when both PLMNs differ.

## [2.2.16] - 2026-09-09

### Changed

- **2G cell-reselect voice** — On 2G, cell reselect announcements now say **channel** (digit by digit) instead of “ARFCN”, matching LTE/NR wording. BSIC is unchanged.

## [2.2.15] - 2026-09-09

### Changed

- **Visited operator terminology** — “Alternative operator” is now **visited operator** across the UI, mock scenarios, RXSS labels, voice-announcement docs, and code (e.g. limited-service home + visited operator in spoken order). Spoken limited-service announcements are unchanged — they still name the home operator then the camped network when different.

## [2.2.14] - 2026-09-09

### Added

- **Camp tier sound controls** — Not-spot camp tiers now expose full signal pulse controls inline: **RXSS 0** (dead zone), **RXSS 11** (searching 2G), **RXSS 12** (limited service), **RXSS 13** (alt 2G limited), and **RXSS 15** (home 2G no signal) each have volume, frequency, duration, interval, and voice/tone settings (shared where appropriate). RXSS 12/13 gain a dedicated **limited-service pulse frequency** setting.

### Changed

- **RXSS terminology** — User-facing labels drop obsolete “Level Range A–D” and mild/fair band names; tiers are shown as **RXSS** numbers with descriptive state names (e.g. “Highest RSRP band” for RXSS 2).

### Fixed

- **No-signal camp playback** — RXSS 0, 11, and 15 signal pulses now use **no-signal tone volume** and **no-signal pulse frequency** at playback, matching RXSS 10.

## [2.2.13] - 2026-09-09

### Fixed

- **RXSS 6 signal pulse volume** — Signal low (RXSS 6) playback now uses the **signal pulse volume** slider (`lowSignalClickVolume`) explicitly, matching RXSS 1. Previously, tier resolution could fall back to Level Range B (FAIR) and play at the shared Level Ranges A–D volume instead. Test preview and pulse duration for RXSS 6 now use `criticalTierPulseDurationMs` from passive settings.

## [2.2.12] - 2026-09-09

### Changed

- **Passive terminology** — RXSS / passive monitoring UI and docs now say **signal pulses** instead of “Geiger clicks”. **Geiger clicks** remains the term for active ping-test audio (RTT, jitter, packet loss).

## [2.2.11] - 2026-09-09

### Fixed

- **RXSS wiring audit** — Limited service camp Geiger (**RXSS 12/13**) now uses `limitedServiceToneVolume` at playback (was incorrectly using global `lowSignalClickVolume`). Test previews for **RXSS 10/12/13** camp volume now play configured Geiger pulses (frequency, duration, interval) instead of legacy flatline/two-tone samples.

## [2.2.10] - 2026-09-09

### Fixed

- **RXSS 1 pulse volume and duration** — Signal high (RXSS 1) Geiger playback now uses the RXSS 1 **signal pulse volume** and **pulse duration** controls (`lowSignalClickVolume`, `signalPulseDurationMs`) instead of incorrectly borrowing Level Range A (RXSS 2) B/C/D shared settings.

## [2.2.9] - 2026-09-09

### Fixed

- **RXSS 5 periodic voice** — “Signal low” every 30 s (**VA-15**) now plays only in RXSS **6** (signal low / critical RSRP), not Level Range D (RXSS **5**). Immediate **VA-8** on dead zone→tier 5 is unchanged. Voice announcer controls removed from the RXSS 5 settings block (configure under RXSS **6**).

## [2.2.8] - 2026-09-09

### Fixed

- **RXSS 10 volume** — Removed duplicate Camp Geiger volume control; **No signal tone** now drives both the entry alert tone and camp Geiger pulses for LTE/NR no signal (RXSS 10).

## [2.2.7] - 2026-09-09

### Fixed

- **RXSS 10 pulse frequency** — LTE/NR no signal (RXSS 10) now has its own **RXSS 10 pulse frequency** slider in Passive Signal settings and uses `noSignalTierPulseFrequencyHz` at playback, independent of RXSS 6 (signal low). Existing installs migrate from the former shared RXSS 6 value.

## [2.2.6] - 2026-09-09

### Changed

- **Voice announcement design in code** — New [`VoiceAnnouncement.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/VoiceAnnouncement.kt) defines stable VA IDs, playback priorities, and immediate order; [`MonitoringUpdateEvents`](app/src/main/java/io/github/cloolalang/notspotdetector/model/MonitoringUpdateEvents.kt) uses it for same-poll sequencing. Confirmed immediate order: **VA-3** (p1) → **VA-1/2** (p2) → **VA-4** (p4) → **VA-8** (p5) → **VA-7** (p6) → **VA-6/9** (p8) → **VA-10** (p9, lowest immediate). Periodic priorities and **VA-11** remain provisional pending field testing.
- **Voice phrase order** — Operator → tech → signal state → service state (when not implicit 4G/5G full service). Dead zone: “deadzone, no service, no SOS calls”; `signal restored` / `limited service` / `cell reselect` phrasing updated in [`SignalStateAnnouncement.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/SignalStateAnnouncement.kt). Settings hint strings updated for Test previews.
- **VA-5 retired** — No voice on leaving limited service; camp on **4G/5G** is implicit full service.

## [2.2.5] - 2026-09-09

### Changed

- **Voice phrase order (initial)** — Documented operator → tech → signal state → service state in [VOICE_ANNOUNCEMENTS.md](VOICE_ANNOUNCEMENTS.md).
- **Voice playback priority (partial)** — Documented priority columns; **VA-10** / **VA-14** / **VA-16** / **VA-18** no-signal RXSS guards.
- **VA-5 retired (initial)** — Documented silent limited-service exit.

### Fixed

- **Cell reselect voice (VA-10)** — Suppressed while primary RXSS is any no-signal state (0, 10, 15, and catalogue placeholders 20–23, 26–27). Documented in [VOICE_ANNOUNCEMENTS.md](VOICE_ANNOUNCEMENTS.md).
- **2G camped repeat (VA-16)** — Same no-signal RXSS guard; periodic “2 G” defers to VA-17 / VA-18 on 2G no-signal or weak camp.
- **Limited service repeat (VA-14)** — Same no-signal RXSS guard, including limited-service camps with unusable RSRP (RXSS 20 / 23 overlays).
- **2G weak repeat (VA-18)** — Same no-signal RXSS guard on the 2G weak periodic path.
- **Voice announcement doc** — VA tables now include a **Suppressed when** column noting RXSS-specific and transition-based skips per announcement.

## [2.2.4] - 2026-09-09

### Fixed

- **Dead zone recovery voice** — Leaving the dead zone (RXSS 0) for a camped LTE/NR state now announces “Signal restored” on the first recovery poll, instead of staying silent because no-signal debounce had not cleared yet. Dead zone → tier 5 still skips “Signal restored” and uses “signal low” immediately.

## [2.2.3] - 2026-09-09

### Fixed

- **Threshold alert controls completeness** — Camp Geiger tiers (RXSS 0, 11, 15) now include pulse volume sliders with hold-to-repeat **Test**. RXSS 13 adds limited-service alert tone volume + **Test**. RXSS 14 white-noise overlay volume now has **Test** (hold-to-repeat when decoupled from signal tier).

## [2.2.2] - 2026-09-09

### Changed

- **Signal thresholds section order** — RXSS blocks now appear in numerical order (0 through 15, then 28–30). Technology-change tiers (28–30) moved below camp and RSRQ tiers; dead zone (0) moved to the top.

## [2.2.1] - 2026-09-09

### Changed

- **Signal thresholds section titles** — Each RXSS block now shows a short state name in the heading (e.g. “RXSS 10 · LTE/NR no signal”, “RXSS 7 · 2G good signal”) so you can tell what the tier represents without opening the catalogue.

## [2.2.0] - 2026-09-09

### Added

- **RXSS 28–30 — Technology change by target** — Separate threshold sections for camp changes to 2G, 4G, and 5G/EN-DC, each with its own sweep tone and voice announcement controls. Voice now speaks only the new technology (e.g. “E E, 4 G”) without a “Technology change” prefix.
- **RXSS 7 & 8 pulse volume + Test** — 2G good and 2G weak tiers now include a signal pulse volume slider with hold-to-repeat **Test**, matching other RSRP tiers.

### Changed

- **Technology change settings** — Per-target tone and voice volumes replace the single shared technology-change controls that were duplicated under RXSS 7 and Global alert sound.

## [2.1.8] - 2026-09-09

### Fixed

- **RXSS 6 voice controls** — Signal-low voice announcement checkbox, volume slider, and **Test** button now appear in Signal thresholds under **RXSS 6**, below pulse frequency (replacing the “shared with RXSS 5” hint).

## [2.1.7] - 2026-09-09

### Fixed

- **RXSS voice announcement controls** — Volume slider and **Test** button now stay visible even when the voice announcement checkbox is off, so you can adjust and preview before enabling (cell change, no signal, limited service, 2G technology change, and signal-low announcer).

## [2.1.6] - 2026-09-09

### Added

- **RXSS 9 — Cell change** — Catalogue row and thresholds panel section for LTE/NR/2G cell reselect alerts (bell + optional voice). Controls moved from Global alert sound to **RXSS 9** under Signal thresholds.

## [2.1.5] - 2026-09-09

### Changed

- **Level Ranges A–D shared sound** — The shared signal pulse block now controls volume, frequency, duration, and click interval for Level Ranges A through D (RXSS 2–5). Level Range A no longer has duplicate pulse controls.
- **Signal pulse test button** — Hold the **Test** button to repeat pulses at the configured click interval. Preview now uses each tier’s current frequency and duration (including RXSS 1’s separate tone frequency).
- **RXSS 6 RSRP threshold** — Signal low (RXSS 6) now always applies above −125 dBm through the Level Range D boundary. The separate no-signal RSRP slider has been removed.

## [2.1.4] - 2026-09-09

### Changed

- **Threshold panel control order** — Every RXSS section now lists controls in a consistent order: sound on/off, RSRP/RSRQ range, pulse volume (+ test), duration, click interval, frequency, then voice announcer toggle and volume (+ test) where applicable.

## [2.1.3] - 2026-09-09

### Added

- **Level Ranges B–D shared sound** — One **Standard sound** block in the thresholds panel controls signal pulse **volume**, **frequency**, and **duration** for Level Ranges B, C, and D (RXSS 3–5). Each range still has its own enable toggle and click interval.

## [2.1.2] - 2026-09-09

### Fixed

- **Threshold panel tier titles** — Level Range A–D blocks now show **RXSS N · Level Range X** (e.g. **RXSS 3 · Level Range B**), matching the cellular metrics display, instead of the range letter alone.

## [2.1.1] - 2026-09-09

### Fixed

- **RXSS 8 alert settings** — 2G weak now shows **Standard sound (Geiger clicks)** and **Signal-low voice announcer** controls directly under **RXSS 8** in the thresholds panel (previously only a cross-reference to RXSS 5).
- **RXSS 1 alert settings** — Signal high now shows a labelled **Standard sound (Geiger clicks)** block with pulse frequency, shared pulse duration, click interval, and signal pulse volume directly under **RXSS 1** (previously scattered or only in global alert settings).

## [2.1.0] - 2026-09-09

### Changed

- **Alert controls per RXSS** — Voice announcers, alert tones, and Geiger click settings for each implemented RXSS now live together under **Passive signal thresholds and alert settings**. Shared controls (e.g. no-signal voice for RXSS 0/10/11/15) appear once with cross-references; no duplicate sliders in **Global alert sound settings**, which keeps only announcer choice, ping clicks, shared signal-pulse volume, and cell-change alerts.

## [2.0.3] - 2026-09-09

### Changed

- **2G weak (RXSS 8) voice** — While camped on home 2G with weak RX, 30-second voice reminders now speak **“signal low”** (Level Range D announcer) instead of the generic **“2 G”** camp reminder. RXSS 7 (2G good) still uses the **2 G** reminder. Enable **Level Range D announcer** under Alert sound volume.

## [2.0.2] - 2026-09-09

### Fixed

- **RX Signal State in cellular metrics** — Level Ranges A–D (RXSS 2–5) now show the catalogue number in the monitor (e.g. **RXSS 2 · Level Range A**), consistent with every other implemented RXSS state.

## [2.0.1] - 2026-09-09

### Fixed

- **RXSS 15 sound controls** — Home 2G no signal (RXSS 15) Geiger click settings now appear under **Not-spot camp tiers** in Passive signal ranges, alongside RXSS 10 and the other camp states. Previously they were only under 2G fallback tiers, where they were easy to miss.

## [2.0.0] - 2026-09-09

### Changed

- **RX Signal State (RXSS) system** — Major release: the app now classifies implemented signal states using RXSS catalogue numbers (`Rxss` constants, `rxssNumber` on signal enums). Dead zone is **RXSS 0** (was tier 9). States **1–8** and **10–15** align with [RXSS_CATALOGUE.md](RXSS_CATALOGUE.md). Legacy `*_TIER_NUMBER` names and settings keys are kept for profile compatibility.

## [1.35.3] - 2026-09-09

### Changed

- **RXSS classification** — Monitor and settings now use RX Signal State catalogue numbers throughout. Dead zone is **RXSS 0** (was tier 9). Implemented states **1–8** and **10–15** unchanged. New `Rxss` constants and `rxssNumber` on signal enums; legacy `*_TIER_NUMBER` names kept as aliases.

## [1.35.2] - 2026-09-09

### Changed

- **Level Range A–D** — RXSS 2–5 labels renamed from “Band A–D” to **Level Range A** through **Level Range D** in the monitor, passive signal settings, and Level Range D announcer.

## [1.35.1] - 2026-09-09

### Changed

- **RSRP bands A–D** — RXSS 2–5 (mild through poor) are now labelled **Band A** (highest RSRP) through **Band D** (lowest) on the monitor and in passive signal settings. RXSS 1, 6, and camp states keep numeric RXSS labels. The monitor row is renamed **RX Signal State**; Band D announcer replaces “Tier 5 announcer” in settings.

## [1.35.0] - 2026-09-09

### Added

- **Tier 15 — home 2G no signal** — mock and live home 2G no-signal state now shows tier 15 instead of tier 10 (LTE/NR no signal). Tier 15 has its own Geiger click settings under 2G fallback tiers; voice behaviour on 2G no signal is unchanged.

## [1.34.8] - 2026-09-09

### Fixed

- **Tier 10 → tier 6 recovery voice** — now plays an immediate one-off “signal low” (operator, technology, signal low) and continues on the regular 30 s tier 5 announcer schedule while tier 6. Still skips “Signal restored”.

## [1.34.7] - 2026-09-09

### Fixed

- **Tier 10 → tier 6 recovery voice** — leaving LTE/NR no signal into tier 6 (critical) no longer speaks “Signal restored” or an immediate tier announcer. The regular tier 5 announcer schedule applies instead (first “signal low” after 5 s, then every 30 s while tier 6). Tier 5 announcer must be enabled.

## [1.34.6] - 2026-09-09

### Fixed

- **Tier 10 → tier 5 recovery voice** — leaving LTE/NR no signal (tier 10) into tier 5 (poor) again announces “Signal restored”. Only tier 10 → tier 6 (critical) uses operator, technology, and “signal low”. Dead zone → tier 5 is unchanged.

## [1.34.5] - 2026-09-09

### Fixed

- **Tier 10 → tier 5/6 recovery voice** — leaving LTE/NR no signal (tier 10) or dead zone into tier 5 or tier 6 now announces operator, technology, and “signal low” instead of “Signal restored”. Fair signal and above still use “Signal restored”.

## [1.34.4] - 2026-09-09

### Fixed

- **Mock tier 6 → tier 10 voice** — lowering mock Home 4G RSRP into no signal now announces “no signal” immediately (mock updates confirm debounce in one step) and repeats on the 30 s schedule. Mock Home/Alt 4G no longer jumps to tier 11 (searching 2G) while still camped on LTE; tier 10 camp voice reminders work when tier 10 Geiger clicks are enabled.

## [1.34.3] - 2026-09-09

### Fixed

- **Dead zone → signal low** — recovering from all-technologies no service directly into tier 5 (poor signal) no longer speaks “Signal restored” before the tier 5 announcer; you hear operator, technology, and “signal low” immediately instead.

## [1.34.2] - 2026-09-09

### Fixed

- **Mock home 2G no signal voice** — entering no signal on 2G no longer speaks immediately on top of the technology-change alert; the first no-signal reminder plays 30 s after the technology announcement, then repeats every 30 s (same cadence as the 2G camped reminder when signal is present).

## [1.34.1] - 2026-09-09

### Fixed

- **Voice alert order** — no-signal and signal-restored announcements now always play before limited/full-service alerts; limited and full-service alerts always play before technology-change and cell-reselect voices. Fixes overlapping or clashing speech when switching mock scenarios such as home 4G to alternative 4G.

## [1.34.0] - 2026-09-09

### Added

- **2G fallback tiers 7 and 8** — each tier now has its own pulse frequency, pulse duration, and click interval controls (matching tier 1’s independent tone frequency, with per-tier duration like camp tiers). Playback and minimum-interval math use these tier-specific values instead of the global signal pulse settings.

## [1.33.4] - 2026-09-09

### Fixed

- **Alert order** — when technology change and cell reselect occur together, technology change is announced first, then cell reselect.
- **Duplicate mock 2G reminders** — concurrent stats updates could start two 30 s G2 reminder jobs; handling is now serialized and the periodic timer resets after an immediate technology/2G camp announcement so only one reminder plays per interval.

## [1.33.3] - 2026-09-09

### Fixed

- **Cell reselect + technology voice together** — when both fire on a state change (e.g. mock Home 4G ↔ Home 2G), announcements now play back-to-back instead of the second voice cancelling the first. Cell reselect is no longer blocked by quiet-until-critical mode.
- **Mock technology suppression** — stale “searching 2G” episode state no longer blocks technology-change voice on direct mock scenario switches.

### Changed

- **Mock alt-operator cells** — alternative operator 4G/2G scenarios use distinct channel/PCI (or ARFCN/BSIC) values so cell-reselect voice also fires on home ↔ alternative changes at the same RAT.

## [1.33.2] - 2026-09-09

### Fixed

- **Mock scenario technology voice** — changing mock network state (e.g. Home 4G → Home 2G) now triggers technology-change alerts immediately; mock updates were updating stats without notifying the monitor service.

### Changed

- **Mock signal slider label** — renamed from “Mock RSRP” to “Mock RSRP/RX Lev”.

## [1.33.1] - 2026-09-09

### Fixed

- **Technology change voice on mock 4G → 2G** — switching mock scenarios (and other in-service LTE/NR → 2G changes) now triggers the technology-change announcement immediately instead of waiting up to 30 s for the periodic 2G reminder. Quiet-until-critical mode no longer blocks technology-change voice. Real no-signal → 2G camp still uses the dedicated 2G camped announcement.

## [1.33.0] - 2026-09-09

### Added

- **Tier 1 pulse frequency** — separate slider in the Tier 1 section (400–5000 Hz). No longer derived from the Alert sound signal pulse frequency; existing installs migrate from the old 125% coupling on first load.

## [1.32.5] - 2026-09-09

### Changed

- **Tier 14 colour** — accent changed from purple to brown for better label readability in settings and on the monitor screen.

## [1.32.4] - 2026-09-09

### Changed

- **Tier 14 RSRQ trigger** — fair/critical boundary slider now ranges from −20 dB to −13 dB (was −30 dB to −13 dB). Saved values below −20 dB are clamped on load.

## [1.32.3] - 2026-09-09

### Changed

- **Tier 14 click interval** — decoupled white-noise repetition slider now caps at 5 s (was 20 s). Existing values above 5 s are clamped on load.

## [1.32.2] - 2026-09-09

### Changed

- **Mock RSRP slider** — range extended down to −130 dBm so you can simulate values below the passive tier boundary floor (−126 dBm).

## [1.32.1] - 2026-09-09

### Fixed

- **Tier 14 alongside RSRP tiers** — poor RSRQ no longer replaces the RSRP/camp signal tier in the metrics panel or tier-5 announcer logic. When both apply, the UI shows e.g. “Tier 5 · 14”. RSRP tier clicks and announcements continue while tier 14 audio runs in parallel.

## [1.32.0] - 2026-09-09

### Added

- **Tier 14 — RSRQ poor** — poor RSRQ (below your fair boundary) is now its own signal tier with dedicated sound controls in passive signal settings. Toggle tier sound, white noise volume, and optional coupling to RSRP tier clicks. When coupled, white noise mixes into RSRP signal pulses during passive-only monitoring (replacing the old “noisy RSRQ clicks” checkbox). When decoupled, tier 14 plays independent white-noise pulses with their own repetition rate and pulse duration.

### Changed

- **RSRP tiers** — tier 6 (critical) is now driven by RSRP only; poor RSRQ no longer forces critical tier classification or click rate. RSRQ quality is handled by tier 14 instead.

## [1.31.3] - 2026-09-09

### Changed

- **Tier 5 signal-low announcer** — first announcement waits 5 seconds after entering tier 5, then repeats every 30 seconds (was immediate plus every 15 seconds).

## [1.31.2] - 2026-09-09

### Fixed

- **Mock panel signal sliders** — Mock RSRP and Mock RSRQ sliders are shown again whenever mock mode is enabled, for every scenario (matching the old passive-signal mock section). Use them to sweep tier boundaries on 4G/2G camp scenarios.

## [1.31.1] - 2026-09-09

### Fixed

- **Tier click interval slider floor** — the minimum selectable interval now matches signal pulse duration + 25 ms (e.g. 40 ms with a 10 ms pulse) instead of snapping up to the next 50 ms or 100 ms step. Playback no longer forces a separate 125 ms floor when your configured interval is already long enough for the pulse.
- **Compile fix** — exhaustive tier mapping for camp states (10–13) in signal measurement classification.

## [1.31.0] - 2026-09-09

### Added

- **Not-spot camp tiers 10–13** — no signal (10), searching 2G (11), limited service (12), and alternative operator 2G limited (13) now use the same Geiger click controls as tiers 7–9 (sound toggle, pulse duration, click interval). When enabled, camp-tier clicks replace the legacy flatline or limited-service loop for that state. Voice announcements are unchanged.

### Changed

- **Signal tier display** — camp states show as Tier 10–13 in the metrics panel. Alternative operator 2G limited service is tier 13 (distinct from tier 12 limited service on 4G).

## [1.30.0] - 2026-09-09

### Added

- **Mock network state panel** — new settings card (moved from passive signal thresholds) with scenarios: home 4G, home 2G, alternative operator 4G/2G limited service, no service (dead zone), and home operator searching 2G. Use with passive-only (mock signal) start to test tiers, voice, and tones without live radio.

## [1.29.0] - 2026-09-09

### Added

- **Network mode in UI** — shows whether the phone is on all technologies, forced to 2G, or forced to 4G/5G without 2G (from the system network preference on Android 12+).
- **Searching 2G signal tier** — after 4G/5G loss in all-tech mode, the signal tier shows “Searching 2G” while the phone may still camp on home 2G. “Searching 2 G” voice is suppressed when 2G is excluded by a forced LTE/NR network mode.
- **Limited service network row** — when camped on an alternative operator, the network metric shows home and camped operators (e.g. “Vodafone UK · EE”).

### Changed

- **All technologies setting** — renamed from “Monitor 2G fallback” and enabled by default for new installs; clarifies that this is the normal all-RAT monitoring mode including 2G fallback, alt-operator limited service, and dead-zone detection.

## [1.28.3] - 2026-09-09

### Fixed

- **EARFCN / PCI “Location needed”** — declare location permissions in the app manifest so Android can actually grant them. Accept coarse or fine location for cell identity reads.

## [1.28.2] - 2026-09-08

### Fixed

- **Voice announcer** — stores and applies the exact TTS engine voice ID for each selection, runs voice changes on the main thread, and uses per-option pitch so male/female choices sound distinct even when the device only exposes one engine voice.

## [1.28.1] - 2026-09-08

### Fixed

- **Voice announcer preview** — each male/female option now maps to a distinct TTS engine voice. Improved Google voice detection, removed duplicate fallback that sent every choice to the same default voice, and apply the selected voice before each test playback.

## [1.28.0] - 2026-09-08

### Added

- **No signal vibration buzz** — optional toggle in alert sound settings (below no signal tone). When enabled, the phone vibrates in sync with each no-signal alert while monitoring. Test via the no signal tone button when monitoring is stopped.

## [1.27.2] - 2026-09-08

### Fixed

- **Voice announcer selector** — replaced the dropdown with radio buttons so voice choices register taps reliably inside the scrollable settings screen.

## [1.27.1] - 2026-09-08

### Changed

- **Tier 5 announcer** — volume slider and test button are always visible in alert sound settings (below the enable toggle). Test works while monitoring is stopped, even if the announcer is still disabled.

## [1.27.0] - 2026-09-08

### Added

- **Voice announcer selector** — choose from system default plus three male and three female text-to-speech voices for all voice announcements. Each option shows the mapped engine voice when available, with a test button in alert sound settings. Saved in settings profiles.

## [1.26.0] - 2026-09-08

### Added

- **Tier 5 announcer** — new voice-only section in alert sound settings (below technology change). Toggle, volume, and test. Announces on entering tier 5 (poor signal) and every 15 seconds while tier 5 continues (e.g. “E E, 4 G, signal low”). Saved in settings profiles.

## [1.25.0] - 2026-09-08

### Added

- **Export profile to Downloads** — each saved profile now has an **Export to Downloads** button. JSON is written to `Downloads/NotSpotter/NotSpotter - {profile name}.json`, ready to pick up with **Import profile from file**.

## [1.24.2] - 2026-09-08

### Fixed

- **Saved profiles missing** — profiles now always save to a stable app folder and any older copies in the previous external location are migrated in on launch. The profile list refreshes when you return to the app. Failed saves show an error instead of silently doing nothing.

## [1.24.1] - 2026-09-08

### Changed

- **Operator title colour** — the operator name turns red only at signal tier 5 and above (poor, critical, 2G tiers, dead zone, and no signal). Tiers 1–4 keep the brand colour.

## [1.24.0] - 2026-09-08

### Added

- **Operator in home title** — the top bar now shows your home operator next to NotSpotter (e.g. “NotSpotter - EE”). EE is green, Vodafone white, and VMO2 blue when signal is good; all turn red in low or no-signal states.

## [1.23.5] - 2026-09-08

### Fixed

- **App crash on launch** — startup no longer reads ViewModel state flows before they are initialized (same class of bug as the earlier tier-reconciliation fix). Clearing app data or a fresh install no longer crashes immediately on open.

## [1.23.4] - 2026-09-08

### Fixed

- **Monitoring crash on Android 13+** — wake locks now use a supported timed acquire/renew pattern (required when targeting current Android versions). Network callback unregister after a timed cellular request no longer throws and crash the app.

## [1.23.3] - 2026-09-08

### Fixed

- **Screen lock freeze** — monitoring no longer stalls when the screen locks. The foreground service renews its wake lock, the ping monitor keeps reading radio metrics and retries cellular network binding after sleep, and the UI no longer races the service with telephony reads on resume.

## [1.23.2] - 2026-09-08

### Added

- **Tier 9 signal pulse duration** — dead zone tier settings now include a dedicated pulse duration slider that controls how long each tier 9 click sounds. Click interval is kept in sync with this duration. Saved profiles include the new setting.

## [1.23.1] - 2026-09-08

### Changed

- **Dead zone voice** — on entering tier 9 and every 30 seconds while still in a complete dead zone, announces the home operator followed by “all technologies dead zone, scanning” (e.g. “Vodafone UK, all technologies dead zone, scanning”). Works during all monitoring sessions, not only when 2G fallback is enabled.

## [1.23.0] - 2026-09-08

### Added

- **Tier 9 dead zone** — when there is no signal from any network or technology, the monitor shows tier 9 and can play fast Geiger-style clicks instead of the continuous flatline tone. Settings include a sound toggle and click interval, same as other tiers. Saved profiles include the new dead zone tier settings.

## [1.22.0] - 2026-09-08

### Added

- **Limited service home and camped operator voice** — limited-service announcements now speak the SIM/home operator first, then the alternative camped operator when different (e.g. “Vodafone UK, E E, Limited service, 4 G”). If the camped operator changes while still in limited service, the new operator is announced immediately and used in subsequent 30-second repeats.

## [1.21.3] - 2026-09-08

### Fixed

- **Limited service periodic announcements** — now repeat every 30 seconds during all monitoring sessions (not blocked by quiet passive mode), same as no-signal and 2G fallback repeats. Enter/exit limited-service voice alerts also bypass quiet mode.

## [1.21.2] - 2026-09-08

### Fixed

- **2G fallback periodic announcements** — while camped on 2G with fallback enabled, repeats operator and technology every 30 seconds (“Vodafone UK, 2 G” with signal, or “Vodafone UK, 2 G, no signal” without).

## [1.21.1] - 2026-09-08

### Changed

- **Signal restored announcement** — now includes the current radio technology (e.g. “E E, Signal restored, 4 G”).
- **Full service announcement** — now includes the current radio technology when leaving limited service (e.g. “E E, Full service, 4 G”).

## [1.21.0] - 2026-09-08

### Added

- **2G fallback voice alerts** — when the phone camps on 2G after losing LTE/NR, announces operator and technology (e.g. “Vodafone UK, 2 G”). After 5 seconds of 4G no signal with 2G fallback enabled, announces “no signal, searching 2 G”. If no 2G is found, announces “Dead zone” and plays the continuous flatline tone.
- **Limited service periodic announcements** — repeats “Limited service, <technology>” every 30 seconds while in limited service mode.

### Changed

- **No-signal periodic announcements** — 30-second no-signal repeats (including on 2G fallback) now run during all monitoring sessions, not only passive-only starts.
- **Limited service enter announcement** — now includes the current radio technology.

## [1.20.5] - 2026-09-08

### Changed

- **2G fallback tier split** — tier 7 is at or above −100 dBm RX level; tier 8 is below −100 dBm.

## [1.20.4] - 2026-09-08

### Changed

- **Histogram title** — renamed to “RF signal level Histogram (all technologies)”.

## [1.20.3] - 2026-09-08

### Changed

- **2G fallback tier thresholds** — tier 7 now applies above −70 dBm RX level; tier 8 applies at −70 dBm and below (including below −100 dBm).

## [1.20.2] - 2026-09-08

### Fixed

- **2G no-signal voice announcements** — technology is now included when 2G drops to no signal: the app remembers the last RAT, detects 2G-only network mode, and announces “operator, 2 G, no signal” on state-change and periodic passive alerts.

## [1.20.1] - 2026-09-08

### Fixed

- **2G no-signal voice announcements** — no-signal alerts on 2G now follow the same pattern as 4G: operator, technology, then “no signal” (e.g. “Vodafone UK, 2 G, no signal”), including periodic passive-only repeats.

## [1.20.0] - 2026-09-08

### Added

- **2G fallback tiers 7 and 8** — when 2G fallback is enabled and the phone is on 2G, signal tier uses fixed RX level thresholds instead of tiers 1–6: tier 7 above −95 dBm, tier 8 at or below −95 dBm. Each tier has its own signal pulse sound toggle and click interval in Passive signal thresholds.

## [1.19.4] - 2026-09-08

### Fixed

- **2G fallback monitoring** — on pure 2G with 2G fallback enabled, the app no longer reports “no signal” or blanks RX level in cellular metrics when GSM signal is present. LTE/NR-only checks were incorrectly treating every 2G camp as having no usable signal.

## [1.19.3] - 2026-09-08

### Fixed

- **Passive signal thresholds panel** — expanding the panel no longer crashes; tier click interval sliders use a coarser step size when the 20-second maximum would create too many slider stops.

## [1.19.2] - 2026-09-08

### Added

- **Per-tier signal pulse sound** — each tier in Passive signal thresholds has a checkbox to enable or disable its signal-pulse clicks while monitoring.

## [1.19.1] - 2026-09-08

### Changed

- **Signal pulse click intervals** — tier click interval sliders now go up to 20 seconds (was 5 seconds).

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
