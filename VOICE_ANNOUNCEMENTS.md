# Voice announcements

How spoken alerts work in Notspot Detector: what triggers them, the order they play when several fire at once, and when an announcement is deliberately skipped.

This document describes **implemented behaviour** in the current app. For RXSS state names and catalogue rows, see [RXSS_CATALOGUE.md](RXSS_CATALOGUE.md).

**Primary code**

| Area | File |
|------|------|
| Message text | [`SignalStateAnnouncement.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/SignalStateAnnouncement.kt), [`CellIdentityAnnouncement.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/CellIdentityAnnouncement.kt), [`LimitedServiceOperator.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/LimitedServiceOperator.kt) |
| VA IDs & priority | [`VoiceAnnouncement.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/VoiceAnnouncement.kt) |
| Edge detection | [`MonitorState.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/MonitorState.kt) |
| Playback & timers | [`ConnectivityMonitorService.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/service/ConnectivityMonitorService.kt) |
| Immediate order | [`MonitoringUpdateEvents.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/MonitoringUpdateEvents.kt) |
| Suppression helpers | [`FlatlineCondition.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/FlatlineCondition.kt) |
| User toggles / volume | [`AudioVolumeSettings.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/AudioVolumeSettings.kt) |
| TTS engine | [`CellVoiceAnnouncer.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/audio/CellVoiceAnnouncer.kt) |

---

## Announcement list (VA-1 … VA-18)

Stable IDs used throughout this document. `{operator}` / `{tech}` are spoken operator and RAT labels.

**Design principle — announce exceptions only.** Voice calls out states that need attention: dead zone, no signal, limited service, 2G fallback / weak / search, signal low, technology and cell changes. **Full service is not spoken** — it is the assumed default.

**Full service (implicit)** — camped on **4G or 5G** (LTE/NR), home registered, **not** limited service, **not** no-signal, **not** dead zone. RXSS **1–6** (and optional **14** overlay) on LTE/NR are quality bands within full service; only **VA-8** / **VA-15** speak when RSRP drops to tier 5/6. Recovering from limited service or no-signal **onto 4G/5G full service** needs no “full service” phrase — **4 G** / **5 G** camp *is* full service. **2G is not treated as implicit full service**; it uses **VA-7**, **VA-16**, **VA-17**, **VA-18** instead.

**VA-5** is retired (was “Full service, 4 G” on leaving limited service).

**Phrase order** (all announcements follow this where applicable):

1. **Operator** — camped network name; in **dual-PLMN** limited service, home then visited each with a **role word** (see below); on a visited camp, cell reselect adds **visited** to the camped operator only  
2. **Tech** — spoken RAT (**2 G**, **4 G**, **5 G**, **5 G E N D C**); omitted for dead zone (no camped RAT)  
3. **Signal state** — `no signal`, `signal restored`, or `signal low` when RSRP quality matters  
4. **Service state** — only when **not** implicit 4G/5G full service: `limited service`, `searching 2 G`, `deadzone, no service, no SOS calls`, `cell reselect, …`, etc.

Implemented in [`SignalStateAnnouncement.joinAnnouncementParts()`](app/src/main/java/io/github/cloolalang/notspotdetector/model/SignalStateAnnouncement.kt) and [`CellIdentityAnnouncement`](app/src/main/java/io/github/cloolalang/notspotdetector/model/CellIdentityAnnouncement.kt).

### Operator role words (home / visited)

When the home PLMN and camped PLMN differ (`resolveCampedVisitedOperatorName()` in [`LimitedServiceOperator.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/LimitedServiceOperator.kt)):

| Announcement | Operator phrasing |
|--------------|-------------------|
| **VA-4**, **VA-6**, **VA-14** (limited service) | `{home} home, {visited} visited` — home SIM operator first, then camped visited operator |
| **VA-10** (cell reselect) | `{visited} visited` — camped operator only, with **visited** appended |

**Single-operator** limited service (home PLMN only, or home and serving names/PLMNs match): `{operator}, {tech}, limited service` — no role words.

Short acronyms are spaced for TTS via [`NetworkOperatorSpeech`](app/src/main/java/io/github/cloolalang/notspotdetector/model/NetworkOperatorSpeech.kt) (e.g. **EE** → “E E”).

### Mock network (VA testing)

Full scenario trigger states: [MOCK_NETWORK_SCENARIOS.md](MOCK_NETWORK_SCENARIOS.md). Mock enable, scenario, RSRP, and RSRQ are saved in profiles — [SETTINGS_PROFILES.md](SETTINGS_PROFILES.md).

Mock scenarios use real UK operator labels so spoken output matches field testing:

| Role | Mock name | PLMN |
|------|-----------|------|
| Home (SIM) | **Vodafone** | 23415 |
| Visited (camp) | **EE** | 23430 |

**Worked examples** (mock visited-operator limited service on 4G):

- **VA-4 / VA-14:** “Vodafone home, E E visited, 4 G, limited service”  
- **VA-8 / VA-15** (overlay 6): “E E visited, 4 G, signal low”  
- **VA-1 / VA-12** (overlay 20): “E E visited, 4 G, no signal” · exit **VA-2:** “… signal restored”  
- **VA-10:** “E E visited, 4 G, cell reselect, channel …, PCI …”  
- **VA-10** on mock visited 2G: “E E visited, 2 G, cell reselect, channel …, BSIC …”  
- **VA-10** with `cellChangeSpeakBandEnabled` (`BAND_NUMBER`): “E E visited, 4 G, band, twenty” (EARFCN 6300 → band 20)  
- **VA-10** with `cellChangeSpeakBandEnabled` (`MHZ_NICKNAME`): “E E visited, 4 G, band, L eight hundred”  
- **VA-18** (overlay 8 on 13): “E E visited, 2 G, signal low”  
- **VA-3** (mock **NO_SERVICE**): “Vodafone, deadzone, no service, no SOS calls” — **not** “Vodafone, 4 G, no signal”  
- **VA-11** (mock **SEARCHING_2G**): “Vodafone, 4 G, no signal, searching 2 G” (~5 s)

Home-only mock (e.g. Home 4G): operator is **Vodafone** with no role suffix — “Vodafone, 4 G, no signal”, etc.

**Priority** — when more than one VA is queued in a short window, **lower number speaks first** (service state before technology detail; severity before milder repeats). **VA-1** and **VA-2** share **p2** (entry vs exit — only one applies per poll). Implemented for immediate batch order in [`MonitoringUpdateEvents.immediateAnnouncements()`](app/src/main/java/io/github/cloolalang/notspotdetector/model/MonitoringUpdateEvents.kt); periodic jobs share one mutex so the lowest applicable priority wins when timers coincide.

### Immediate (state change, same poll)

| ID | Pri | Name | Typical phrase | RXSS | Trigger | Suppressed when |
|----|-----|------|----------------|------|---------|-----------------|
| **VA-1** | **2** | No signal entry | `{operator}, {tech}, no signal` | 10 | Debounced `noSignalActive` **false→true** on LTE/NR (not 2G); monitoring running; no-signal baseline ready. | On **2G** (RXSS **15** — **VA-17** periodic instead); **dead zone** (**RXSS 0** — **VA-3** instead, including debounced no-signal entry while `isCompleteNoService`). |
| **VA-2** | **2** | Signal restored | `{operator}, {tech}, signal restored` | 10 exit | Debounced `noSignalActive` **true→false**, **or** `isCompleteNoService` **true→false** (dead-zone exit) before debounce clears. | [Signal restored skipped](#signal-restored-skipped): dead zone→**5**/**6**; tier **10**→**6**; LTE/NR no-signal exit→**2G** camp; duplicate after dead zone. |
| **VA-3** | **1** | Dead zone entry | `{operator}, deadzone, no service, no SOS calls` | 0 | `isCompleteNoService` **false→true**; once per no-signal episode. | Already announced this episode. |
| **VA-4** | **4** | Limited service entry | Dual PLMN: `{home} home, {visited} visited, {tech}, limited service` · single: `{operator}, {tech}, limited service` | 12 / 13 | `isLimitedService` **false→true**; limited-service baseline ready. | — |
| **VA-6** | **8** | Limited service operator change | Same as **VA-4** | 12 / 13 | Visited operator changes while still in limited service (`limitedServiceVisitedOperatorChanged`). | — |
| **VA-7** | **6** | 2G camped after LTE/NR loss | `{operator}, 2 G` | G2 fallback | First poll on **2G** after LTE/NR no-signal episode; `monitor2gFallback` enabled; G2-fallback baseline ready. | — |
| **VA-8** | **5** | Signal low (immediate) | `{operator}, {tech}, signal low` | 5 / 6 | `tier5Immediate`: dead zone→tier 5, or tier 10→tier 6 recovery; `tier5AnnouncerEnabled`. | — |
| **VA-9** | **8** | Technology change | `{operator}, {tech}` | 28–30 | Camped `radioAccessType` changes after radio baseline. | LTE/NR→**2G** after no-signal (RXSS **10** exit path — **VA-7** instead). |
| **VA-10** | **9** | Cell reselect (lowest immediate) | Home camp: `{operator}, {tech}, cell reselect, channel …, PCI …` · visited camp: `{operator} visited, {tech}, cell reselect, …` (2G: `channel …, BSIC …`; EN-DC: `LTE channel …, PCI …, NR channel …, PCI …`). Alternative (`cellChangeSpeakBandEnabled`): replaces `cell reselect, channel …, PCI …` with `band, …` — the E-UTRA band from the LTE channel (EARFCN), spoken as whole-number words — the band number (`cellChangeBandNamingStyle = BAND_NUMBER`, e.g. “band, twenty”) or MHz nickname (`MHZ_NICKNAME`, e.g. “band, L eight hundred”); falls back to the normal phrasing with no LTE channel (2G-only reselect) | 9 | LTE/NR PCI or channel change, or 2G BSIC/channel change, after cell-identity baseline. Visited suffix when `resolveCampedVisitedOperatorName()` is non-null. | Any [no-signal RXSS](#no-signal-rxss-voice-rules): **0**, **10**, **15**, **20**, **21**, **23**, **26**, **27**. |

### Delayed immediate

| ID | Pri | Name | Typical phrase | RXSS | Trigger | Suppressed when |
|----|-----|------|----------------|------|---------|-----------------|
| **VA-11** | 10 | Searching for home 2G | `{operator}, {tech}, no signal, searching 2 G` | 11 | **~5 s** after `searching2gFallbackActive` becomes true during a no-signal episode; **once per episode**. | **2G** camp, dead zone (**0**), search ends, or already announced this episode. |

### Periodic (every 30 s while condition holds)

| ID | Pri | Name | Typical phrase | RXSS | Trigger | Suppressed when |
|----|-----|------|----------------|------|---------|-----------------|
| **VA-12** | 12 | No signal repeat | `{operator} visited, {tech}, no signal` on visited PLMN; else `{operator}, {tech}, no signal` | 10 / **20** / **23** | `shouldPlayNoSignalVoiceAnnouncements()` — LTE/NR flatline, RXSS 10 camp, or limited visited no-signal overlays (**20**, **23**). Entry and exit (**VA-2** restored phrasing on visited PLMN). | On home **2G** or dead zone (**0**). |
| **VA-13** | 11 | Dead zone repeat | `{operator}, deadzone, no service, no SOS calls` | 0 | `isCompleteNoService`; monitoring active; not passive idle. | — |
| **VA-14** | 14 | Limited service repeat | Same phrasing as **VA-4** | 12 / 13 | `shouldAllowLimitedServicePeriodicVoice()` — limited service with measurable signal; every **30 s**. | Any [no-signal RXSS](#no-signal-rxss-voice-rules) while limited (incl. overlays **20**, **21**, **23**, **26**, **27**). |
| **VA-15** | 16 | Signal low repeat | `{operator} visited, {tech}, signal low` on visited PLMN when applicable | **6** / **12·6** / **13·8** | RSRP in RXSS **6**, limited 4G overlay **6**, or limited visited 2G weak **8**; `tier5AnnouncerEnabled`; first repeat after **5 s** when entered via VA-8, else 5 s then 30 s. RXSS **5** is signal pulses only. | [Quiet passive alerts](#passive-alert-gating) (not RXSS-specific). |
| **VA-16** | 17 | 2G camped repeat | `{operator}, 2 G` | 7 | On 2G (RXSS 7), `shouldAllowG2CampedPeriodicVoice()`. | Any [no-signal RXSS](#no-signal-rxss-voice-rules); defers to **VA-17** (**15**) / **VA-18** (**8**); **suppressed when VA-14** applies (RXSS **12** / **13** limited service with signal). |
| **VA-17** | 13 | 2G no signal repeat | `{operator}, 2 G, no signal` | 15 | `shouldPlayG2NoSignalVoiceAnnouncements()` on 2G fallback path. | — (voice **for** RXSS **15**). |
| **VA-18** | 15 | 2G weak repeat | `{operator} visited, {tech}, signal low` on limited visited 2G | 8 / **13·8** | On 2G, `shouldAllowG2WeakPeriodicVoice()` — including limited visited 2G weak overlay when **VA-14** is suppressed. | Any [no-signal RXSS](#no-signal-rxss-voice-rules); **suppressed when VA-14** applies on measurable **12** / **13** camp (not overlay **8**). |

All announcements also respect monitoring off, relevant voice toggles / zero volume, first-poll baselines, and passive idle where noted in [When monitoring is silent](#when-monitoring-is-silent).

### Not spoken (by design)

| Situation | Instead |
|-----------|---------|
| Dead zone exit | **VA-2** “Signal restored” (unless suppressed) — no dedicated dead-zone exit phrase. |
| Limited service exit to **4G/5G** | Silent — **VA-5** retired; LTE/NR full camp is assumed. |
| LTE/NR full service (RXSS **1–6**, tech **4G/5G**) | No “full service” entry voice — assumed whenever not dead zone / no-signal / limited. **VA-8** / **VA-15** only if RSRP tier 5/6; tiers 1–4 use signal pulses only. |
| Camp on **2G** | Not implicit full service — **VA-7** / **VA-16** / **VA-17** / **VA-18** as applicable. |
| RXSS 15 entry on 2G | No immediate no-signal; **VA-17** periodic handles it. |
| RSRP tiers 1–4, RSRQ overlay (14) | Signal pulses / white-noise only — no voice. |

---

## How playback works

### Tone then voice

Most alerts follow the same pattern:

1. Play a short alert tone (bell, sweep, or no-signal tone).
2. Wait **tone duration + 50 ms** (`AudioVolumeSettings.ALERT_VOICE_GAP_MS`).
3. Speak the message via TTS if the relevant **voice enabled** toggle is on and volume is above zero.

**Exception:** tier 5 / “signal low” announcer (`tier5AnnouncerEnabled`) speaks **TTS only** — no preceding tone.

### One alert at a time

Immediate announcements run **sequentially** inside a mutex in `ConnectivityMonitorService`. Periodic jobs also acquire the mutex before speaking, so two voice clips never overlap.

### Immediate vs periodic

| Mode | When | Repeat interval |
|------|------|-----------------|
| **Immediate (entry / exit)** | State edge on a monitoring poll | Once per transition |
| **Periodic** | While a condition stays true | Every **30 s** (`PERIODIC_ANNOUNCEMENT_MS`) |

Special delays:

- **VA-11:** **5 s** after entering search (`SEARCHING_2G_ANNOUNCEMENT_DELAY_MS`), once per no-signal episode.
- **VA-15:** first repeat after **5 s** when entered via **VA-8** (`TIER5_INITIAL_ANNOUNCEMENT_DELAY_MS`); otherwise 5 s then 30 s cycles.

### When monitoring is silent

Voice jobs are cancelled or never started when:

- Monitoring is stopped.
- **Passive idle** mode is active (active ping session timed out — cellular metrics only, no quality alerts).
- The relevant **voice enabled** toggle is off, or volume is zero.
- **Quiet passive alerts** is on and signal has not crossed the quiet-alert RSRP/RSRQ thresholds (applies to tier 5 periodic restarts and signal-pulse alerts; see [Passive alert gating](#passive-alert-gating)).

---

## Playback priority (summary)

Full **Pri** values are in the tables above. Rationale: tell the listener **what kind of service** they have before **how** it changed (technology, cell).

| Pri | IDs | When queued |
|-----|-----|-------------|
| **1** | **VA-3** | Dead zone entry — speaks before any other immediate on the same poll |
| **2** | **VA-1**, **VA-2** | No-signal entry or signal restored (one per poll) |
| **4** | **VA-4** | Limited service entry |
| **5** | **VA-8** | Immediate signal low |
| **6** | **VA-7** | 2G camp after LTE/NR loss |
| **8** | **VA-6**, **VA-9** | Limited-service operator change, then technology change |
| **9** | **VA-10** | Cell reselect — **lowest immediate** |
| **10** | **VA-11** | ~5 s after search starts (after the immediate batch) |
| **11–17** | **VA-13** … **VA-16** | Periodic repeats — worst state first (dead zone → no signal → limited → weak/low → 2G camp) |

On one poll, priorities **1–9** run back-to-back via `immediateAnnouncements()` (**VA-1** and **VA-2** share one queue slot — only one applies per poll). **VA-11** is scheduled separately. Periodic timers (**11–17**) restart from the new state; if several fire together, the shared alert mutex lets the **lowest Pri** speak first. On 2G, the G2 periodic job picks **VA-17** → **VA-18** → **VA-16** internally (priorities **13**, **15**, **17**).

---

## Settings by announcement

| IDs | Voice toggle | Volume | Alert tone |
|-----|--------------|--------|------------|
| **VA-1**, **VA-2**, **VA-11**, **VA-12**, **VA-17** | `noSignalVoiceEnabled` | `noSignalVoiceVolume` | `noSignalToneVolume` |
| **VA-3**, **VA-13** | `noSignalVoiceEnabled` | `noSignalVoiceVolume` | `noSignalToneVolume` |
| **VA-3** (vibration) | — | — | `noSignalVibrationEnabled` |
| **VA-8**, **VA-15**, **VA-18** | `tier5AnnouncerEnabled` | `tier5AnnouncerVolume` | — (TTS only) |
| **VA-4**, **VA-6**, **VA-14** | `limitedServiceVoiceEnabled` | `limitedServiceVoiceVolume` | `limitedServiceToneVolume` |
| **VA-7**, **VA-16** | `technologyChangeTo2gVoiceEnabled` | `technologyChangeTo2gVoiceVolume` | `technologyChangeTo2gToneVolume` |
| **VA-9** → 2G / 4G / 5G | `technologyChangeTo{2g,4g,5gEndc}VoiceEnabled` | matching `…VoiceVolume` | matching `…ToneVolume` |
| **VA-10** | `cellChangeVoiceEnabled` | `cellChangeVoiceVolume` | `cellChangeBellVolume` |
| **VA-10** band phrasing | `cellChangeSpeakBandEnabled`, `cellChangeBandNamingStyle` | — | — |
| All | `voiceAnnouncerChoice`, `voiceAnnouncerEngineId` | — | — |

UI controls live under **Signal thresholds** (per RXSS) and **Alert sound volume** (global announcer picker).

---

## No-signal RXSS voice rules

**VA-10**, **VA-14**, **VA-16**, and **VA-18** never fire while the primary RXSS has Signal = No in the catalogue:

| RXSS | State |
|------|--------|
| **0** | Dead zone |
| **10** | LTE/NR no signal |
| **15** | Home 2G no signal |
| **20** | Limited 4G no signal (placeholder) |
| **21** | Limited home 2G no signal (placeholder) |
| **23** | Limited alt 2G no signal (placeholder) |
| **26** | Limited alt 4G no signal, SIM suspended (placeholder) |
| **27** | Limited alt 2G no signal, SIM suspended (placeholder) |

**Not** included: **RXSS 11** (searching for home 2G) — transition/search, Signal column is n/a. `isInNoSignalRxss()` still treats it as no-signal for voice-suppression purposes (see below), since there is no camped cell to report despite the catalogue's "—" column.

Limited-service camps on RXSS **12** / **13** with RSRP at or below the no-signal threshold are treated as no-signal overlays (**20** / **23**) via `isLimitedServiceNoSignalCamp()`.

`isInNoSignalRxss()` also treats the debounced `noSignalActive` flag and the `UNAVAILABLE` measurement tier (camped with no RSRP/RSRQ reading yet — e.g. mid-debounce right after losing signal) as no-signal, in addition to the catalogue tier-number list above. This closes gaps where a stale/flickering PCI or EARFCN carried over between polls (`coalesceWith` in `CellIdentityStabilizer.kt`) could otherwise trigger a voice announcement while there is no usable cell to report.

Enforcement:

| Announcement | Guard |
|--------------|--------|
| **VA-10** | Cell identity cleared while `noSignalActive` or dead zone (`shouldClearCellIdentity()`). `consumeCellIdentityChange()` returns null when `isInNoSignalRxss()`. |
| **VA-14** | `updateLimitedServicePeriodicAnnouncements()` uses `shouldAllowLimitedServicePeriodicVoice()` — false when `isInNoSignalRxss()`. |
| **VA-16** | `playG2ModePeriodicAnnouncement()` uses `shouldAllowG2CampedPeriodicVoice()` — false when **VA-14** applies (`shouldAllowLimitedServicePeriodicVoice()`), when `isInNoSignalRxss()`, when **VA-17** applies, or when **VA-18** applies. |
| **VA-18** | `playG2ModePeriodicAnnouncement()` uses `shouldAllowG2WeakPeriodicVoice()` — false when **VA-14** applies or when `isInNoSignalRxss()`. |

**VA-17** is the 2G periodic voice **for** RXSS **15** no-signal camp. **VA-18** is the weak 2G voice **for** RXSS **8** when signal is still measurable.

All four guarded announcements resume once the primary RXSS is any camped state with measurable signal (tiers **1–8**, **12–14**, etc.).

---

## Suppression rules

### “Signal restored” skipped

`shouldSuppressSignalRestoredForWeakSignalRecovery()` — **VA-2** is skipped when recovery should use **VA-8** instead:

| Transition | Behaviour |
|------------|-----------|
| **Dead zone → tier 5** (poor RSRP) | No **VA-2**; immediate **VA-8**. |
| **Tier 10 → tier 6** (critical RSRP) | No **VA-2**; immediate **VA-8**. |
| **Tier 10 → tier 5** | **VA-2** plays; no **VA-15** (RXSS 5 has no periodic signal low). |
| **Dead zone → fair/good camp** | **VA-2** on dead-zone exit (first recovery poll). |
| **Dead zone → tier 5** | Suppress **VA-2** on dead-zone exit **and** on later no-signal debounce clear. |

### LTE/NR no-signal exit → 2G camp

When leaving no-signal by camping on **2G** after LTE/NR loss:

- **No** **VA-2** “Signal restored”.
- **No** **VA-9** technology-change voice.
- **Yes** **VA-7** G2 fallback.

### Technology change after no-signal → 2G

If previous RAT was LTE/NR, next is 2G, 2G fallback is enabled, and the previous poll was still in no-signal, **VA-9** is **not** emitted (**VA-7** handles it).

### Duplicate “Signal restored” after dead zone

Leaving dead zone sets an internal flag so when `noSignalActive` clears on the next poll, **VA-2** is not spoken a second time.

### Dead zone entry

Only **one** **VA-3** per no-signal episode (`deadzoneAnnouncedThisEpisode`). Entering dead zone also marks **VA-11** as announced so the search voice does not double up.

### First poll after monitoring starts (baselines)

To avoid spurious speech on startup, these baselines suppress the **first** edge until initial state is recorded:

- No-signal active/inactive  
- Limited service  
- Radio technology  
- Cell identity  
- G2 fallback  
- Tier 5  

Tier 5 is the exception: if the **first** reading is already tier 5, **VA-8** **can** fire once baseline is set.

### Passive idle

While in passive idle, **VA-8** / **VA-15** periodic restart is gated off. Most periodic jobs (**VA-12–VA-18**) require `!isPassiveIdleMode`.

---

## Passive alert gating

When **Quiet passive alerts** is enabled (`passiveQuietUntilCritical`), signal pulses and **VA-15** restart wait until signal is “bad enough”:

- No-signal / flatline active, or  
- RSRQ below `quietAlertRsrqDb`, or  
- RSRP at or below `quietAlertRsrpMaxDbm`

**Not gated** by quiet mode: **VA-1–VA-14**, **VA-16–VA-18**, technology change, cell change, and G2 fallback — those follow their own toggles.

---

## No-signal debounce

`noSignalActive` requires **two consecutive polls** agreeing before entering or leaving. This affects voice timing:

- Entry/exit phrases align with debounced edges, not raw single-poll flicker.
- Dead-zone recovery may speak **VA-2** when `isCompleteNoService` clears **before** `noSignalActive` clears.

---

## Operator and technology in speech

- Operator names pass through [`NetworkOperatorSpeech.formatForSpeech()`](app/src/main/java/io/github/cloolalang/notspotdetector/model/NetworkOperatorSpeech.kt): short all-caps acronyms (≤4 chars, e.g. **EE**, **O2**) are letter-spaced (“E E”, “O 2”); longer names (e.g. **Vodafone**, **Vodafone UK**) are spoken as-is.
- **Home / visited role words** — see [Operator role words](#operator-role-words-home--visited). Limited service uses both; cell reselect uses **visited** on the camped operator only when on a visited PLMN.
- **Cell identity** — LTE/NR and 2G use **channel** (digits spaced), not “ARFCN”. PCI and BSIC are also digit-spaced (`SpeechDigits`).
- Technologies: **2 G**, **4 G**, **5 G**, **5 G E N D C** (`SignalStateAnnouncement.formatTechnologyForSpeech`).
- If the modem omits RAT during no-signal, the last known LTE/NR type from the episode is used; on 2G fallback paths, **2 G** is assumed when appropriate.

---

## Related tests

Unit tests document expected phrasing, ordering, and suppression:

- [`SignalStateAnnouncementTest.kt`](app/src/test/java/io/github/cloolalang/notspotdetector/model/SignalStateAnnouncementTest.kt) — limited-service home/visited phrasing, mock Vodafone/EE  
- [`CellIdentityAnnouncementTest.kt`](app/src/test/java/io/github/cloolalang/notspotdetector/model/CellIdentityAnnouncementTest.kt) — cell reselect text, visited suffix, 2G channel/BSIC  
- [`LimitedServiceOperatorTest.kt`](app/src/test/java/io/github/cloolalang/notspotdetector/model/LimitedServiceOperatorTest.kt) — visited PLMN detection  
- [`MonitoringAnnouncementPlaybackOrderTest.kt`](app/src/test/java/io/github/cloolalang/notspotdetector/model/MonitoringAnnouncementPlaybackOrderTest.kt) — same-poll immediate order  
- [`MonitorStateDeadzoneToTier5Test.kt`](app/src/test/java/io/github/cloolalang/notspotdetector/MonitorStateDeadzoneToTier5Test.kt) — signal-restored suppression  
- [`MonitorStateTechnologyAnnouncementTest.kt`](app/src/test/java/io/github/cloolalang/notspotdetector/MonitorStateTechnologyAnnouncementTest.kt) — technology vs limited-service ordering  
- [`MonitorStateCellReselectTest.kt`](app/src/test/java/io/github/cloolalang/notspotdetector/MonitorStateCellReselectTest.kt) — periodic voice guards on limited/2G camps  
- [`PassiveMockSettingsTest.kt`](app/src/test/java/io/github/cloolalang/notspotdetector/model/PassiveMockSettingsTest.kt) — mock Vodafone home / EE visited metrics  
