# RX Signal State (RXSS) catalogue

Working reference for state names, concurrency, and numbering. Not all rows are implemented in the app yet.

**How does App detect** column — left as-is for now (mostly threshold text). Rewrite per state during implementation.

**Planned for app development** — rows with Home SIM **Suspended** (**19**, **21**, **22**, **26**, **27**) are catalogue-only initially (**Development deferred**).

## Development plan (using this table)

Implement in passes, using **Planned for app development** and the two alert columns as the spec:

1. **Pass A — Primary RXSS detection** — Map API metrics to one primary RXSS (+ overlays **14**, **20**, **23**) per poll; replace tier-number UI with RXSS numbers where ready.
2. **Pass B — Standard sound** — Wire each row’s **Standard sound alert controls** to `PassiveSignalSettings` camp-tier / interval-click paths in `GeigerCounterPlayer`.
3. **Pass C — Voice** — Wire entry / exit / 30 s jobs in `ConnectivityMonitorService` and `MonitorState` per row; add formatters in `SignalStateAnnouncement`.
4. **Pass D — New search & overlay RXSS** — **24**, **25**, **20**, **23**; split **12** vs **19**; defer suspended rows until README revisit.

**Alert column legend**

| Abbrev | Meaning |
|--------|---------|
| **Entry** | Immediate voice + alert tone on RXSS enter (edge-triggered) |
| **Exit** | Immediate voice on leave when the recovery is non-obvious (e.g. **VA-2** “Signal restored” from no-signal). Normal full service is usually **silent** — see [VOICE_ANNOUNCEMENTS.md](VOICE_ANNOUNCEMENTS.md). |
| **30s** | Repeating voice every 30 s while RXSS holds |
| **Camp signal pulses** | Interval pulses via camp-tier path |
| **RSRP signal pulses** | Signal pulse interval from RSRP band |
| **Flatline** | Continuous or pulsed no-signal tone (legacy pre-camp-tier path) |
| **Two-tone** | Alternating limited-service loop (legacy when camp tier off) |
| **TBD** | Not implemented — define during Pass C/D |

Settings: [AudioVolumeSettings](app/src/main/java/io/github/cloolalang/notspotdetector/model/AudioVolumeSettings.kt) (voice/tone volumes, toggles) · [PassiveSignalSettings](app/src/main/java/io/github/cloolalang/notspotdetector/model/PassiveSignalSettings.kt) (per-tier click interval, pulse length, sound enable).

Voice behaviour (triggers, playback order, suppression): [VOICE_ANNOUNCEMENTS.md](VOICE_ANNOUNCEMENTS.md). **Full service** = camped **4G/5G**, registered, not limited / no-signal / dead zone — not spoken; **2G** uses its own voice paths.

## State table

| RXSS | State name | Tech | Kind | Home / visited operator | Service type | Home SIM state | UE state | Signal | Concurrent RXSS | How does App detect | Implemented | Planned for app development | Entry, exit & 30s voice announce and controls | Standard sound alert controls |
|------|------------|------|------|---------------------|--------------|----------------|----------|--------|-----------------|-----------------|-------------|----------------------------|-----------------------------------------------|------------------------------|
| **0** | Dead zone | All | Camp | — | — | Active | Search all home and alternative technologies and bands | No | **—** | **API only:** modem reports fully out-of-service — no camp on any RAT, no SOS/limited service on any SIM. **Not** inferred from search timeouts. Mock: `NO_SERVICE` | Yes | | **Entry / 30s:** “deadzone, no service, no SOS calls” (**VA-3** / **VA-13**) · **VA-1** suppressed while `isCompleteNoService` · **Exit:** none · `noSignalVoiceEnabled`, `noSignalVoiceVolume`, `noSignalVibrationEnabled` (tone: `noSignalToneVolume`) | **Camp signal pulses** · `deadzoneTierSoundEnabled`, `deadzoneTierClickIntervalMs`, `deadzoneTierPulseDurationMs`, `lowSignalClickVolume` |
| **1** | Signal high | 4G/5G | RSRP | Home | Full home† | Active | Registered idle/connected | Yes | Optional **12 or 19**; optional **14**† | RSRP above Level Range A | Yes | | None | **RSRP signal pulses** (tier 1) · `veryStrongTierSoundEnabled`, `veryStrongTierClickIntervalMs`, `veryStrongTierPulseFrequencyHz`, `lowSignalClickVolume`, pulse duration shared with Global alert sound settings (`signalPulseDurationMs`) |
| **2** | Level Range A | 4G/5G | RSRP | Home | Full home† | Active | Registered idle/connected | Yes | Optional **12 or 19**; optional **14**† | Highest RSRP band | Yes | | None | **RSRP signal pulses** (mild) · `mildTierSoundEnabled`, `mildTierClickIntervalMs`, `mildTierPulseDurationMs` (own click interval and pulse duration; volume and frequency shared with RXSS 3–5) |
| **3** | Level Range B | 4G/5G | RSRP | Home | Full home† | Active | Registered idle/connected | Yes | Optional **12 or 19**; optional **14**† | Second RSRP band (between A and C) | Yes | | None | **RSRP signal pulses** (good) · `goodTierSoundEnabled`, `goodTierClickIntervalMs`, `goodTierPulseDurationMs` (own click interval and pulse duration; volume and frequency shared with RXSS 2, 4, 5) |
| **4** | Level Range C | 4G/5G | RSRP | Home | Full home† | Active | Registered idle/connected | Yes | Optional **12 or 19**; optional **14**† | Third RSRP band (between B and D) | Yes | | None | **RSRP signal pulses** (fair) · `fairTierSoundEnabled`, `fairTierClickIntervalMs`, `fairTierPulseDurationMs` (own click interval and pulse duration; volume and frequency shared with RXSS 2, 3, 5) |
| **5** | Level Range D | 4G/5G | RSRP | Home | Full home† | Active | Registered idle/connected | Yes | Optional **12 or 19**; optional **14**† | Lowest RSRP band before signal low | Yes | | **Entry:** “signal low” only on dead zone→tier 5 (**VA-8**); no **30s** repeat | **RSRP signal pulses** (poor) · `poorTierSoundEnabled`, `poorTierClickIntervalMs`, `poorTierPulseDurationMs` (own click interval and pulse duration; volume and frequency shared with RXSS 2–4) |
| **6** | Signal low | 4G/5G | RSRP | Home | Full home† | Active | Registered idle/connected | Yes | Optional **12 or 19**; optional **14**† | Below Level Range D, still above no-signal threshold | Yes | | **Entry:** “signal low” (**VA-8**, incl. tier 10→6) · **30s:** repeat (**VA-15**) · `tier5AnnouncerEnabled`, `tier5AnnouncerVolume` | **RSRP signal pulses** (critical) · `criticalTierSoundEnabled`, `criticalTierClickIntervalMs`, `criticalTierPulseDurationMs` |
| **7** | 2G good signal | 2G | RX level | Home‡ | Full home‡ | Active | Registered idle/connected | Yes | Exactly one of **7** or **8** whenever 2G RX is measurable‡ — **never 11 or 18** | Any 2G camp with RX ≥ −100 dBm (normal home, **13**, or **22**) | Yes | | **Entry:** “{op}, 2 G” after LTE/NR loss · **30s:** same while on 2G · via **28** voice when camp changes to 2G | **RSRP signal pulses** (2G strong) · `g2StrongTierSoundEnabled`, `g2StrongTierClickIntervalMs`, `g2StrongTierPulseDurationMs`, `lowSignalClickVolume` |
| **8** | 2G weak | 2G | RX level | Home‡ | Full home‡ | Active | Registered idle/connected | Yes | Exactly one of **7** or **8** whenever 2G RX is measurable‡ — **never 11 or 18** | Any 2G camp with RX below −100 dBm (normal home, **13**, or **22**) | Yes | | **30s:** “signal low” (not “2 G”) · `tier5AnnouncerEnabled`, `tier5AnnouncerVolume` | **RSRP signal pulses** (2G weak) · `g2WeakTierSoundEnabled`, `g2WeakTierClickIntervalMs`, `g2WeakTierPulseDurationMs`, `lowSignalClickVolume` |
| **9** | Cell change | All | Event | Follows camp | Follows camp | Active | Registered idle/connected | Yes††† | **—** (momentary; does not replace primary RXSS) | LTE/NR PCI or channel change, or 2G BSIC or channel change, on any camped technology | Yes | | **Entry:** bell + optional voice (**VA-10**) — home camp: `{op}, {tech}, cell reselect, channel …, PCI …`; visited camp: `{op} visited, …` · `cellChangeVoiceEnabled`, `cellChangeVoiceVolume`. Optional alternative phrasing via `cellChangeSpeakBandEnabled`: replaces `cell reselect, channel …, PCI …` with `band, …`, the E-UTRA band derived from the LTE channel (EARFCN), spoken as whole-number words (not digit-by-digit) — the band number (`cellChangeBandNamingStyle = BAND_NUMBER`, e.g. “band, twenty”) or its MHz nickname (`MHZ_NICKNAME`, e.g. “band, L eight hundred”); falls back to the normal phrasing when there is no LTE channel (2G-only reselect) | **Bell** · `cellChangeBellVolume` |
| **28** | Technology change → 2G | 2G | Event | Follows camp | Follows camp | Active | Registered idle/connected | Yes | **—** (momentary; does not replace primary RXSS) | Camped RAT becomes 2G (from LTE/NR or other) | Yes | | **Entry:** sweep + optional voice (“{op}, 2 G” only — no “Technology change” prefix) · `technologyChangeTo2gVoiceEnabled`, `technologyChangeTo2gVoiceVolume` | **Sweep tone** · `technologyChangeTo2gToneVolume` |
| **29** | Technology change → 4G | 4G | Event | Follows camp | Follows camp | Active | Registered idle/connected | Yes | **—** (momentary; does not replace primary RXSS) | Camped RAT becomes LTE | Yes | | **Entry:** sweep + optional voice (“{op}, 4 G” only) · `technologyChangeTo4gVoiceEnabled`, `technologyChangeTo4gVoiceVolume` | **Sweep tone** · `technologyChangeTo4gToneVolume` |
| **30** | Technology change → 5G/EN-DC | 5G | Event | Follows camp | Follows camp | Active | Registered idle/connected | Yes | **—** (momentary; does not replace primary RXSS) | Camped RAT becomes NR standalone or EN-DC | Yes | | **Entry:** sweep + optional voice (“{op}, 5 G” or “5 G E N D C”) · `technologyChangeTo5gEndcVoiceEnabled`, `technologyChangeTo5gEndcVoiceVolume` | **Sweep tone** · `technologyChangeTo5gEndcToneVolume` |
| **10** | 4G/5G no signal | 4G/5G | Camp | Home | Full home | Active | Transitory state | No | **—** | Camped 4G/5G, debounced no signal; **~5 s** hold before **24** | Yes | Add **24** handoff | **Entry:** `{op}, {tech}, no signal` · **Exit:** `{op}, {tech}, signal restored` · **30s:** repeat · `noSignalVoiceEnabled`, `noSignalVoiceVolume`, `noSignalVibrationEnabled` | **Camp signal pulses** · `noSignalTierSoundEnabled`, `noSignalTierClickIntervalMs`, `noSignalTierPulseDurationMs` |
| **11** | Searching for home 2G | → 2G | Transition | Home | n/a | Active | Search 2G home channels | — | Optional **18** — **not 7 or 8**; **not 10, 15, 24, or 25** | Follows **24**/**25** or **15**; UE scans home 2G first — **before** **18**. Mock: `SEARCHING_2G` | Yes | Split **11** vs **18** voice | **Entry:** ~5 s — “no signal, searching 2 G” (**VA-11**, once/episode) · `noSignalVoiceEnabled`, `noSignalVoiceVolume` | **Camp signal pulses** · `searching2gTierSoundEnabled`, `searching2gTierClickIntervalMs`, `searching2gTierPulseDurationMs` |
| **12** | Limited service alt 4G | 4G/5G | Camp / SOS | Alternate | Visited operator | Active | Camped, not registered | Yes | **(1–6 + optional 14) or 20 or 26** — **14 not with 20/26**; **20 not with 26**; **not 13** | Emergency-only on visited-operator 4G/5G. Mock: `ALT_OPERATOR_4G` | Partial§ | Split home **19** vs alt **12** | **Entry:** `{home} home, {visited} visited, {tech}, limited service` (**VA-4**) · **Exit:** silent · **30s:** (**VA-14**) when no overlay · Overlays: **VA-15** (6), **VA-12**/**VA-2** (20) · **VA-6** operator change · `limitedServiceVoiceEnabled`, `limitedServiceVoiceVolume` | **Camp signal pulses** or legacy **Two-tone** · `limitedServiceTierSoundEnabled`, `limitedServiceTierClickIntervalMs`, `limitedServiceToneVolume` · overlay **20** uses **RXSS 10** voice/tone |
| **13** | Limited alt 2G | 2G (alt) | Camp / SOS | Alternate | Visited operator | Active | Camped, not registered; background scan home 2G + 4G | Yes | **7 or 8 or 23 or 27** + **16 + 17 (always paired)**‡ — **23/27 not with 7/8**; **23 not with 27**; **not 12** | visited-operator 2G SOS camp; UE keeps scanning home 2G and home/alt 4G in background | Yes | Background scans (**16**/**17**, home 2G) — not intended for monitoring | Same limited voice as **12** (**VA-4** / **VA-14** / **VA-6**); mock: Vodafone home + EE visited | **Camp signal pulses** or legacy **2G pulse** · `limitedAlt2gTierSoundEnabled`, `limitedAlt2gTierClickIntervalMs`, `limitedServiceToneVolume` |
| **14** | RSRQ poor | 4G/5G | Overlay | Follows base | Follows base | Active | n/a | Yes | **1–6, 12, 19 only** — **never 0, 10, 15, 20, 21, 23, 26, 27** | RSRQ below fair boundary; stacks on 4G/5G with measurable RSRP | Yes | | None | **RSRQ overlay** · `rsrqTierSoundEnabled`, `rsrqTierCoupledToSignalTier`, `rsrqTierClickIntervalMs`, `rsrqTierWhiteNoiseVolume` |
| **15** | Home 2G no signal | 2G | Camp | Home | Full home | Active | Transitory state | No | **—** | Home 2G camp, debounced no signal; leads to **11** → **18** → **13**/**7**/**8**/**0** | Yes | | **Entry:** none (2G skips immediate) · **30s:** “no signal” via G2 periodic · `noSignalVoiceEnabled`, `noSignalVoiceVolume` | **Camp signal pulses** · `g2NoSignalTierSoundEnabled`, `g2NoSignalTierClickIntervalMs`, `g2NoSignalTierPulseDurationMs` |
| **16** | Searching for home 4G (2G fallback) | 2G → 4G/5G | Transition | Home | n/a | Active | Search 4G home channels | — | **16 + 17 (always paired)** on **7, 8, 13, or 22** — **not 24 or 25** | Background home 4G scan whenever UE is on 2G fallback | No | Not intended for monitoring — background scan only | None · not monitored | None · not monitored |
| **17** | Searching for visited operator 4G (2G fallback) | 2G → 4G/5G | Transition | Alternate | n/a | Active | Search 4G Visited operator channels | — | **16 + 17 (always paired)** on **7, 8, 13, or 22** — **not 25** | Background alt 4G scan whenever UE is on 2G fallback | No | Not intended for monitoring — background scan only | None · not monitored | None · not monitored |
| **18** | Searching for visited operator 2G | → 2G | Transition | Alternate | n/a | Active | Search 2G Visited operator channels | — | **11** (required) — **not 7 or 8** | After home 2G search (**11**) fails; may camp **13** *(API: generic “searching 2G”)* | No | | Voice folded into **11** (“… searching 2 G”) | Same **Camp signal pulses** as **11** · `searching2gTier*` |
| **19** | Limited service home 4G | 4G/5G | Camp / SOS | Home | Limited home | Suspended | Camped, not registered | Yes | **(1–6 + optional 14) or 20** — **14 not with 20**; **not 22** | Emergency-only on home-operator 4G/5G (e.g. SIM account suspended) | No | Development deferred | **TBD** — mirror **12** with home operator · reuse `limitedServiceVoice*` until split | **TBD** — mirror **12** camp signal pulses · reuse `limitedServiceTier*` |
| **20** | Limited service 4G no signal (alt/active) | 4G/5G | Camp / SOS overlay | Alternate or home | Follows base | Follows base†† | Transitory state | No | **12** (alt/active) **or 19** (home/susp) | No RSRP on **12** (active SIM) or **19** (home/suspended); **not 10**; **not 26**. Mock: `ALT_OPERATOR_4G` weak RSRP | Yes (on **12**) | **19** path deferred | **Entry / 30s / exit:** `{visited} visited, {tech}, no signal` / `signal restored` (**VA-12** / **VA-2**) · shares **RXSS 10** controls | **Camp signal pulses** · `noSignalTierSoundEnabled`, `noSignalTierClickIntervalMs` |
| **21** | Limited home 2G no signal | 2G | Camp / SOS overlay | Home | Limited home | Suspended | Transitory state | No | **22** | Limited home 2G camp with no usable RX; **not** RXSS 15 | Placeholder | Development deferred | **TBD** · deferred with **22** | **TBD** · deferred |
| **22** | Limited service home 2G | 2G | Camp / SOS | Home | Limited home | Suspended | Camped, not registered | Yes | **7 or 8 or 21** + **16 + 17 (always paired)**‡ — **21 not with 7/8**; **not 19** | Emergency-only on home-operator 2G (e.g. SIM account suspended) | Placeholder | Development deferred | **TBD** — mirror **13** limited voice · deferred | **TBD** — mirror **13** camp signal pulses · deferred |
| **23** | Limited alt 2G no signal | 2G (alt) | Camp / SOS overlay | Alternate | Visited operator | Active | Transitory state | No | **13** | Limited alt 2G camp with no usable RX. Mock: `ALT_OPERATOR_2G` weak RSRP | Yes | — | **Entry / 30s / exit:** `{visited} visited, 2 G, no signal` / `signal restored` · shares **RXSS 15** / no-signal voice | **Camp signal pulses** · `g2NoSignalTierSoundEnabled`, `g2NoSignalTierClickIntervalMs` |
| **24** | Searching for home 4G (4G loss) | 4G/5G | Transition | Home | n/a | Active | Search 4G home channels | — | **not 7, 8, 10, 11, 16, 25** | After **~5 s** in **10** with no recovery; success → **1–6**; else **25** | Placeholder | After **10** | **TBD** — e.g. “searching 4 G” entry; **Controls:** TBD | **TBD** — new camp-tier slot or reuse search pattern · Pass D |
| **25** | Searching for visited operator 4G (4G loss) | 4G/5G | Transition | Alternate | n/a | Active | Search 4G Visited operator channels | — | **not 7, 8, 10, 11, 17, 24** | After **24** (or **10**), UE rescans visited-operator 4G/5G before 2G fallback — success → **12** (+ **1–6** or **20**) | Placeholder | After **24** | **TBD** — e.g. “searching visited operator 4 G” · **Controls:** TBD | **TBD** — paired with **24** · Pass D |
| **26** | Limited alt 4G no signal (SIM suspended) | 4G/5G | Camp / SOS overlay | Alternate | Visited operator | Suspended | Transitory state | No | **12** | Alt 4G SOS camp, home SIM suspended, no RSRP; **not 10**; **not 20** | Placeholder | Development deferred | **TBD** · deferred (mirror **20** on **12**) | **TBD** · deferred |
| **27** | Limited alt 2G no signal (SIM suspended) | 2G (alt) | Camp / SOS overlay | Alternate | Visited operator | Suspended | Transitory state | No | **13** | Alt 2G SOS camp, home SIM suspended, no RX; **not 15**; **not 23** | Placeholder | Development deferred | **TBD** · deferred (mirror **23** on **13**) | **TBD** · deferred |

† **RXSS 1–6** — Service type **Full home** in normal service. When stacked on **12**, service type is **Visited operator**; when stacked on **19**, **Limited home**.

†† **RXSS 20 on 19** — Home SIM **Suspended** (follows **19**); **20 on 12** — Home SIM **Active**.

‡ **RXSS 7–8** — Shared 2G RX level bands for **any** camp with measurable 2G signal: normal home 2G, **13** (limited alt), or **22** (limited home). Exactly one of **7** or **8** when RX is readable; **21**, **23**, or **27** replaces **7**/**8** when not (**27** on **13** with suspended SIM only). **Home / visited operator** and **Service type** follow the primary camp (**Full home** on normal 2G; **Visited operator** on **13**; **Limited home** on **22**). **2G fallback background search** — on **7**/**8**/**13**/**22**, **16** + **17** are **always paired** (not monitored). Primary 2G search (**11** → **18**) is **never** concurrent with measurable **7**/**8**.

§ App currently maps all 4G limited service to RXSS 12; home vs alt split (12 / 19) not yet implemented.

¶ App maps dead zone to **RXSS 0** via `Rxss.DEADZONE`. **RXSS 9** is a momentary cell-reselect event (`Rxss.CELL_CHANGE`) on LTE, NR, or 2G — not a camp state.

††† **RXSS 9** — Event only; primary RXSS on the metrics line stays the camp state. Requires cell-identity permission for PCI/channel/BSIC reads.

**RXSS 16 vs 24** — Both search home 4G/5G: **16** stacks on 2G fallback (**7**/**8**/**13**/**22**); **24** follows **10** (4G loss, no 2G camp).

**RXSS 17 vs 25** — Both search visited-operator 4G/5G: **17** stacks on 2G fallback; **25** follows **24** (4G loss, no 2G camp). **16**/**17** always paired on 2G fallback; **24**/**25** mutually exclusive after **10**.

**Dead zone (**0**)** — Enter only when the cellular API reports fully out-of-service (no camp on any RAT). Do **not** promote **10**/**24**/**25**/**11**/**18** to **0** based on elapsed time alone. **12**/**13**/**19**/**22** (SOS/limited camp) are never **0**.

**Suspended SIM (interim)** — One unified fallback ladder for all SIM states (`10 → 24 → 25 → 11 → …`); distinguish **19**/**22**/**13**/**12** only once camped. 3GPP limited-service behaviour may differ — see [README.md](README.md) open items.

### No-signal matrix (8 monitored camps)

|  | **Home SIM active** | **Home SIM suspended** |
|---|---------------------|-------------------------|
| **4G home** | **10** | **20** on **19** |
| **4G alt** | **20** on **12** | **26** on **12** |
| **2G home** | **15** | **21** on **22** |
| **2G alt** | **23** on **13** | **27** on **13** |

**0** (dead zone) is separate — API out-of-service, not a camp no-signal state.

### Service type values

| Value | Meaning |
|-------|---------|
| **Full home** | Normal registered service on home operator (RXSS 1–8, 10, 15) |
| **Limited home** | Emergency / limited service on home operator — SIM suspended (RXSS 19, 21, 22) |
| **Visited operator** | Emergency / limited service on Visited operator (RXSS 12, 13, 23, 26, 27) |
| **Follows base** | Same as stacked primary (RXSS 14, 20 on **12** only) |
| **n/a** | Pure search / transition — not camped for service yet (RXSS 11, 16, 17, 18, 24, 25) |
| **—** | No service (RXSS 0) |

### Signal column

| Value | Meaning |
|-------|---------|
| **Yes** | Usable RSRP or RX level measurable |
| **No** | No-signal state — no usable measurement (RXSS 0, 10, 15, 20, 21, 23, 26, 27) |
| **—** | Transition / search — scanning behaviour only. **16** + **17** always paired on 2G fallback (**7**/**8**/**13**/**22**); **24**/**25** standalone after **10**; primary **11**/**18** 2G search does not stack on camp |

### Home SIM state values

| Value | Meaning |
|-------|---------|
| **Active** | Home SIM account active (RXSS 0–18, 23–25). **23** on **13** keeps **Active** |
| **Suspended** | Home SIM account suspended (RXSS 19, 21, 22, **26**, **27**). **21** on **22**, **26** on **12**, **27** on **13** keep **Suspended** |
| **Follows base** | **20** on **12** → **Active**; **20** on **19** → **Suspended** (††) |

### UE state values

| Value | Meaning |
|-------|---------|
| **Registered idle/connected** | UE camped and registered on network; idle or connected (RXSS 1–8) |
| **Camped, not registered** | UE camped for emergency/SOS only — not fully registered (RXSS 12, 19, 22). **13** also background-scans home 2G + home/alt 4G |
| **Search all home and alternative technologies and bands** | UE scanning all RATs and bands (RXSS 0) |
| **Transitory state** | Short-lived camp with no usable signal (RXSS 10, 15, 20, 21, 23, 26, 27) |
| **Search 2G home channels** | UE scanning home-operator 2G channels (RXSS 11) |
| **Search 4G home channels** | UE scanning home-operator 4G channels (RXSS **16** on 2G fallback; **24** after 4G loss) |
| **Search 4G Visited operator channels** | UE scanning visited-operator 4G channels (RXSS **17** on 2G fallback; **25** after 4G loss) |
| **Search 2G Visited operator channels** | UE scanning visited-operator 2G channels (RXSS **18** after **11** fails) |
| **n/a** | Overlay — no separate UE state (RXSS 14) |

### Limited service breakdown

| RXSS | State | Operator | RAT | Typical cause |
|------|-------|----------|-----|----------------|
| **12** | Limited service alt 4G | Alternate | 4G/5G | SOS on other network’s 4G |
| **13** | Limited alt 2G | Alternate | 2G | SOS on other network’s 2G |
| **19** | Limited service home 4G | Home | 4G/5G | SIM suspended — camped home 4G/5G in limited mode |
| **20** | Limited service 4G no signal | Alt/active or home/susp | 4G/5G | No RSRP on **12** (active SIM) or **19** |
| **21** | Limited home 2G no signal | Home | 2G | SIM suspended — limited home 2G, no usable RX |
| **22** | Limited service home 2G | Home | 2G | SIM suspended — camped home 2G in limited mode |
| **23** | Limited alt 2G no signal | Alt/active | 2G | Limited alt 2G, active SIM, no usable RX |
| **26** | Limited alt 4G no signal | Alt/suspended | 4G/5G | Alt 4G SOS, suspended SIM, no RSRP |
| **27** | Limited alt 2G no signal | Alt/suspended | 2G | Alt 2G SOS, suspended SIM, no usable RX |

## Concurrency rules

**4G/5G RSRP level (exactly one when measurable):** `1` · `2` · `3` · `4` · `5` · `6`

**2G RX level (exactly one when measurable):** `7` · `8` — apply on **any** 2G camp with readable RX (normal home, or stacked on **13** or **22**); replaced by **21**, **23**, or **27** when no usable RX

**Dead zone (**0**):** API-reported out-of-service only — mutually exclusive with all other primaries including **12**/**13** (SOS counts as camp, not dead zone)

**Camp / SOS primary (mutually exclusive):** `0` · `10` · `24` · `25` · `11` · `12` · `13` · `15` · `19` · `22` — **12** and **13** never together (one SOS camp / RAT at a time); **19** and **22** never together (same rule for suspended home SOS). **10 → 24 → 25 → 11** and **15 → 11** are sequential (**~5 s** debounce in **10** before **24**); **24**/**25** alternate; **18** stacks on **11** (not a primary)

**No-signal overlays (mutually exclusive with level on same camp):** `20` on **12** (active SIM) or **19**; `26` on **12** (suspended SIM); `21` on **22**; `23` or `27` on **13** (**23** active, **27** suspended — not together)

**Limited 4G on same camp:**

- Measurable RSRP → **12 or 19** + one of **1–6** (+ optional **14**)
- No usable RSRP → **12** + **20** or **26**; **19** + **20** (instead of **1–6**; **14** off)

**Limited 2G on same camp:**

- Measurable RX → **13** or **22** + **7 or 8**
- No usable RX → **22** + **21**; **13** + **23** (active SIM) or **27** (suspended SIM)

**RSRQ overlay (**14**):** stacks only on **1–6** (with or without **12**/**19**). **Never** with no-signal states **0, 10, 15, 20, 21, 23, 26, 27**.

**4G background search on 2G fallback (always paired):** **16** + **17** on **7**, **8**, **13**, or **22** — UE always scans home and alt 4G while camped on 2G

**4G rescan after 4G loss (sequential, one at a time):** **24** then **25** follow **10** — **~5 s** debounced hold in **10** before **24** starts; UE rescans home then visited-operator 4G/5G before 2G fallback; **24** and **25** mutually exclusive; service type **n/a**; **not** **16**/**17** (require 2G fallback camp). **Success:** **24** → **1–6** (home recovered); **25** → **12** (+ **1–6** or **20**)

**Primary 2G search (sequential, exclusive with camp):** **11** scans home 2G after **24**/**25** fail or after **15**. **18** stacks on **11** when home 2G camp not found — may result in **13** (limited alt 2G). Service type **n/a**. **Not** concurrent with **7**, **8**, **10**, **15**, **24**, or **25**. If all search/camp fails → API out-of-service → **0**

**After home 2G no signal (**15**):** `15 → 11 → 18 → 13` (alt 2G limited) or `7`/`8` (home 2G camp) or `0` (API OOS). While on **13**, background **16** + **17** + home 2G scan continue

**Overlays / paired states**

| RXSS | Stacks on | Notes |
|------|-----------|--------|
| **14** | **1–6, 12, 19** | Never with **0, 10, 15, 20, 21, 23, 26, 27** |
| **16** | **7, 8, 13, 22** | Always paired with **17**; **never 24, 25** |
| **17** | **7, 8, 13, 22** | Always paired with **16**; **never 24, 25** |
| **24** | **—** | Follows **10** after **~5 s** debounce; precedes **25** or **11**; mutually exclusive with **25**; **never 7, 8, 16, 17** |
| **25** | **—** | Follows **24** (typical) or **10**; precedes **11**; mutually exclusive with **24**; **never 7, 8, 16, 17** |
| **18** | **11** | Requires **11** first; **never 7, 8** |
| **11** | **18** (optional) | Follows **15** or **24**/**25**; **18** after home 2G fails; **never 7, 8, 10, 15, 24, 25** |
| **20** | **12** (active), **19** | Never with **14**, **26** |
| **26** | **12** (suspended) | Never with **14**, **20** |
| **21** | **22** | Never with **7, 8**; distinct from **15** |
| **23** | **13** (active) | Never with **7, 8**, **27** |
| **27** | **13** (suspended) | Never with **7, 8**, **23** |
| **7, 8** | Primary camp (home 2G, **13**, or **22**) | Shared RX level — not a camp; **never 11 or 18** |
| **13** | **7, 8 or 23 or 27**; **16 + 17** (always) | **23**/**27** by SIM state; background scan |
| **22** | **7, 8 or 21**; **16 + 17** (always) | Background 4G scan while on limited home 2G |

## Ladders

- **4G/5G RSRP (normal):** `1 → A → B → C → D → 6 → 10`
- **4G/5G RSRP (limited):** `12 or 19` + (`1–6` or `20` or `26` on **12**)
- **Home 2G (normal):** `7 → 8 → 15` *(**7**/**8** carry **16** + **17** throughout)*
- **Home 2G no signal → recovery:** `15 → 11 → 18 → 13 or 7/8 or 0`
- **Home 2G (limited / SIM suspended):** `22` + (`7 → 8` or `21`)
- **Alt 2G (limited):** `13` + (`7 → 8` or `23` or `27`) + **16** + **17** *(background home 2G + 4G scan)*
- **4G loss → recovery / fallback:** `10 (~5 s) → 24 → 25 → 11 → 18 → 7/8 or 13` *(success on **24** → **1–6**; success on **25** → **12** + levels; **18** may camp **13**)*
- **Dead zone:** `0` only when API reports no camp on any RAT — may follow any failed search/camp state; **not** timeout-inferred

## Passive mock mapping

For VA and tier testing without live signal, see [MOCK_NETWORK_SCENARIOS.md](MOCK_NETWORK_SCENARIOS.md).

| Mock scenario | Simulated primary RXSS | Mock RSRP slider |
|---------------|------------------------|------------------|
| `HOME_4G` | **1–6** (+ **14**) | Yes (RSRP + RSRQ) |
| `HOME_2G` | **7–8** / **15** | Yes (RX level) |
| `ALT_OPERATOR_4G` | **12** (+ **1–6** / **20**) | Yes |
| `ALT_OPERATOR_2G` | **13** (+ **7–8** / **23**) | Yes |
| `NO_SERVICE` | **0** | No |
| `SEARCHING_2G` | **11** | No |
| `HOME_5G_ENDC` | **1–6** (+ **14**) | Yes (RSRP + RSRQ) |

Mock settings (`enabled`, `scenario`, `rsrpDbm`, `rsrqDb`) are included in saved profiles — [SETTINGS_PROFILES.md](SETTINGS_PROFILES.md).
