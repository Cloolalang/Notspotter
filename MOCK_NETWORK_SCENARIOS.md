# Mock network scenarios (passive testing)

Manual network simulation for **passive-only monitoring** — test RXSS tiers, camp clicks, and voice without live cellular API reads.

**UI:** Monitor screen → **Mock network state** card (visible during passive / mock sessions).  
**Code:** [`PassiveMockSettings.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/PassiveMockSettings.kt), [`MockNetworkStateCard.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/ui/MockNetworkStateCard.kt)

Related: [RXSS_CATALOGUE.md](RXSS_CATALOGUE.md) · [VOICE_ANNOUNCEMENTS.md](VOICE_ANNOUNCEMENTS.md) · [SETTINGS_PROFILES.md](SETTINGS_PROFILES.md)

---

## Operators (fixed in mock)

| Role | Name | PLMN |
|------|------|------|
| Home (SIM) | **Vodafone** | 23415 |
| Visited (camp) | **EE** | 23430 |

---

## Scenario reference

Each row is the **agreed trigger state** the mock simulates. Live detection uses real API metrics; mock **sets these flags directly** via `PassiveMockSettings.toRadioMetrics()`.

### 1. Home operator 4G — `HOME_4G`

| Field | Value |
|-------|--------|
| **Primary RXSS** | **1–6** (from mock RSRP/RSRQ slider) · optional **14** (RSRQ overlay) |
| **Operator** | Vodafone home (`homePlmn == plmn`) |
| **Tech** | 4G (`radioAccessType = 4G`, `isOn2g = false`) |
| **Service** | Full home — `isLimitedService = false`, `isCompleteNoService = false` |
| **Signal** | `hasLteNrSignal = true`, `cellularAvailable` from RSRP thresholds |
| **Mock slider** | RSRP + RSRQ |
| **Not** | Limited service, dead zone, RXSS 10/11, 2G |

**Voice (examples):** tier 6 → “Vodafone, 4 G, signal low”; RXSS 10 path when RSRP below no-signal threshold (debounced `noSignalActive`).

---

### 2. Home operator 2G — `HOME_2G`

| Field | Value |
|-------|--------|
| **Primary RXSS** | **7–8** (mock RX level) · **15** when below no-signal threshold |
| **Operator** | Vodafone home |
| **Tech** | 2G (`isOn2g = true`, `radioAccessType = 2G`) |
| **Service** | Full home — `isLimitedService = false`, `isCompleteNoService = false` |
| **Signal** | `hasHomeGsmSignal = true`, `hasLteNrSignal = false` |
| **Requires** | **Monitor 2G fallback** enabled |
| **Mock slider** | RSRP (2G RX level) |
| **Not** | Limited service, searching 2G, visited camp |

**Voice (examples):** “Vodafone, 2 G” (camp); weak → “signal low” (**VA-18**); no signal → **VA-17** periodic.

---

### 2a. Home operator 4G (limited service) — `HOME_LIMITED_4G`

| Field | Value |
|-------|--------|
| **Primary RXSS** | **19** |
| **Overlays** | **1–6** (RSRP band) · **20** (no usable RSRP) |
| **Operator** | Vodafone home (`homePlmn == plmn`) |
| **Tech** | 4G, `isOn2g = false` |
| **Service** | Limited — `isLimitedService = true` |
| **Mock slider** | RSRP + RSRQ |
| **Not** | Visited 4G (**12**), in-service home 4G |

**Voice:** “Vodafone, 4 G, home limited service”.

---

### 2b. Home operator 2G (limited service) — `HOME_LIMITED_2G`

| Field | Value |
|-------|--------|
| **Primary RXSS** | **22** |
| **Overlays** | **7–8** (2G RX) · **21** (no usable RX) |
| **Operator** | Vodafone home |
| **Tech** | 2G, `isOn2g = true` |
| **Service** | Limited — `isLimitedService = true` |
| **Mock slider** | RSRP (2G RX level) |
| **Not** | Searching 2G (**11**), in-service home 2G no signal (**15**), visited 2G (**13**) |

**Voice:** “Vodafone, 2 G, home limited service”; weak → “signal low”; no signal → RXSS **21**.

---

### 3. Visited operator 4G (limited service) — `ALT_OPERATOR_4G`

| Field | Value |
|-------|--------|
| **Primary RXSS** | **12** |
| **Overlays** | **1–6** (RSRP band) · **20** (no usable RSRP) |
| **Operator** | Vodafone home · **EE visited** (`homePlmn ≠ plmn`) |
| **Tech** | 4G, `isOn2g = false` |
| **Service** | Limited — `isLimitedService = true`, `networkServiceMode = LIMITED_SERVICE` |
| **Signal** | `hasLteNrSignal = true`, `hasHomeGsmSignal = false` |
| **Mock slider** | RSRP + RSRQ |

**Overlay examples (mock RSRP):**

| RSRP (typical) | Composite | Voice |
|----------------|-----------|--------|
| Strong (~−75) | RXSS 12·1 | **VA-14** limited service (no overlay) suppressed; signal pulses |
| Weak (~−122) | RXSS 12·6 | “E E visited, 4 G, signal low” (**VA-15**) |
| No signal (~−130) | RXSS 20 | “E E visited, 4 G, no signal”; exit → “signal restored” |

**VA-14** suppressed when signal overlay **1–6** or **20** is active.

---

### 4. Visited operator 2G (limited service) — `ALT_OPERATOR_2G`

| Field | Value |
|-------|--------|
| **Primary RXSS** | **13** |
| **Overlays** | **7–8** (2G RX) · **23** (no usable RX) |
| **Operator** | Vodafone home · **EE visited** |
| **Tech** | 2G, `isOn2g = true` |
| **Service** | Limited — `isLimitedService = true` |
| **Signal** | `hasHomeGsmSignal = false`, `hasLteNrSignal = false` |
| **Requires** | **Monitor 2G fallback** enabled |
| **Mock slider** | RSRP (2G RX level) |

**Overlay examples:**

| RSRP (typical) | Composite | Voice |
|----------------|-----------|--------|
| Strong (~−95) | RXSS 13·7 | Limited-service camp / pulses |
| Weak (~−110) | RXSS 13·8 | “E E visited, 2 G, signal low” (**VA-18**) |
| No signal (~−130) | RXSS 23 | “E E visited, 2 G, no signal”; exit → “signal restored” |

**VA-16** (2G camped repeat) suppressed on all **RXSS 13** camps.

---

### 5. No service (dead zone) — `NO_SERVICE`

| Field | Value |
|-------|--------|
| **Primary RXSS** | **0** |
| **Operator** | Vodafone (home name only — no camp) |
| **Tech** | None (`radioAccessType = null`) |
| **Service** | `isCompleteNoService = true`, `isLimitedService = false` |
| **Signal** | No RSRP/RSRQ, `cellularAvailable = false` |
| **Mock slider** | None |

**Not the same as:** RXSS **10** (LTE no-signal camp), **20** / **23** (limited no-signal overlays).

**Voice:** **VA-3** only — “Vodafone, deadzone, no service, no SOS calls”. **VA-1** “4 G, no signal” is **suppressed** while `isCompleteNoService` (including debounced `noSignalActive` on later polls).

---

### 6. Home operator searching 2G — `SEARCHING_2G`

| Field | Value |
|-------|--------|
| **Primary RXSS** | **11** |
| **Operator** | Vodafone home (no camp yet) |
| **Tech** | None (`radioAccessType = null`) — LTE/NR lost, scanning home 2G |
| **Service** | `isCompleteNoService = false`, `isLimitedService = false` |
| **Signal** | No camp / no RSRP, `cellularAvailable = false` |
| **Requires** | **Monitor 2G fallback** enabled |
| **Mock slider** | None |

**Live path:** Usually follows LTE/NR no-signal (`searching2gFallbackActive` after **RXSS 10**). Mock jumps straight to this state.

**Voice:** **VA-11** ~5 s after search starts — “Vodafone, 4 G, no signal, searching 2 G” (uses last known LTE RAT when set).

**Testing checklist:** See [VOICE_ANNOUNCEMENTS.md](VOICE_ANNOUNCEMENTS.md) · transitions **Home 4G → Searching 2G** and **Searching 2G → Home 2G** camp.

---

### 7. Home operator 5G ENDC — `HOME_5G_ENDC`

| Field | Value |
|-------|--------|
| **Primary RXSS** | **1–6** (from mock RSRP/RSRQ slider) · optional **14** (RSRQ overlay) |
| **Operator** | Vodafone home (`homePlmn == plmn`) |
| **Tech** | 5G NSA / EN-DC (`radioAccessType = "5G EN-DC"`, LTE anchor + NR secondary, `isOn2g = false`) |
| **Service** | Full home — `isLimitedService = false`, `isCompleteNoService = false` |
| **Signal** | `hasLteNrSignal = true`, `cellularAvailable` from RSRP thresholds (same tier math as `HOME_4G`) |
| **Cell identity** | `lteEarfcn`/`ltePci` (anchor) plus `nrEarfcn`/`nrPci` (secondary) — both set, so **RXSS 9** cell-reselect voice can announce the NR leg too |
| **Mock slider** | RSRP + RSRQ |
| **Not** | Limited service, dead zone, RXSS 10/11, 2G |

**Voice (examples):** switching into this scenario from another RAT fires **RXSS 30** technology-change voice — “Vodafone, 5 G E N D C”; tier 6 → “Vodafone, 5 G E N D C, signal low”.

---

### 8. WiFi calling, no cellular signal — `WIFI_CALLING`

| Field | Value |
|-------|--------|
| **Primary RXSS** | **31** |
| **Operator** | Vodafone home (no cellular camp) |
| **Tech** | None (`radioAccessType = null`) |
| **Service** | `networkServiceMode = IN_SERVICE`, `isWifiCallingActive = true`, `isCompleteNoService = false`, `isLimitedService = false` |
| **Signal** | No RSRP/RSRQ, `cellularAvailable = false` |
| **Mock slider** | None |

**Not the same as:** RXSS **0** (dead zone — API reports fully out of service) or RXSS **10** / **11** (camped/searching LTE — real WiFi calling reports `IN_SERVICE` with no cellular RAT, which used to be misclassified as **11** before this scenario was added — see [RXSS_CATALOGUE.md](RXSS_CATALOGUE.md)).

**Voice:** **VA-1** / **VA-12** — “Vodafone, wifi calling, no cellular signal”; exit **VA-2** — “Vodafone, cellular signal restored”. See [VOICE_ANNOUNCEMENTS.md](VOICE_ANNOUNCEMENTS.md#rxss-31--wifi-calling-no-cellular-signal).

---

## Using mock in the app

1. Enable **Mock network state** on the card.
2. Pick a **scenario** (table above).
3. Adjust **RSRP/RSRQ** when the slider is shown.
4. Start **Passive mock** monitoring (or passive-only session).
5. Scenario + slider values are **saved in settings profiles** — see [SETTINGS_PROFILES.md](SETTINGS_PROFILES.md).

**Immediate stats refresh:** Changing scenario or RSRP calls `MonitorState.pushMockStatsIfActive()` (double poll for debounce).

---

## Enum → stored name

Profile JSON stores `passiveMock.scenario` as the enum name:

`HOME_4G` · `HOME_2G` · `ALT_OPERATOR_4G` · `ALT_OPERATOR_2G` · `NO_SERVICE` · `SEARCHING_2G` · `HOME_5G_ENDC` · `WIFI_CALLING`

Unknown values fall back to `HOME_4G` on import.
