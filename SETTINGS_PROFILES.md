# Settings profiles — save, load, and JSON scope

Named profiles capture **all user-configurable app settings** for backup, sharing, and quick recall.

**UI:** Monitor screen → **Settings profiles** panel (save, load, delete, export, import).  
**Codec:** [`AppSettingsSnapshotCodec.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/data/AppSettingsSnapshotCodec.kt)  
**Model:** [`AppSettingsSnapshot.kt`](app/src/main/java/io/github/cloolalang/notspotdetector/model/AppSettingsSnapshot.kt)

Completeness is enforced by [`AppSettingsSnapshotCodecCompletenessTest.kt`](app/src/test/java/io/github/cloolalang/notspotdetector/data/AppSettingsSnapshotCodecCompletenessTest.kt).

---

## File format

```json
{
  "schemaVersion": 1,
  "profile": {
    "id": "<uuid>",
    "name": "<display name>",
    "savedAtMs": 1234567890,
    "settings": { ... }
  }
}
```

Multi-profile export uses a top-level `"profiles"` array with the same object shape per entry.

**Import:** Accepts single-profile files (`profile` or legacy `settings` root) or multi-profile arrays. Unknown `passiveMock.scenario` values default to `HOME_4G`.

---

## What is saved (`settings` object)

| Section | JSON key | Kotlin type | Purpose |
|---------|----------|-------------|---------|
| Ping / active test | `thresholds` | `ThresholdSettings` | RTT, jitter, packet loss, good-connection click suppression |
| Ping target | `ping` | `PingSettings` | Host, port, pings per test, interval |
| Monitoring | `monitoring` | `MonitoringSettings` | 2G fallback, SIM, quiet passive alerts, measurement interval, RSRP histogram window |
| Signal thresholds & tiers | `passiveSignal` | `PassiveSignalSettings` | RSRP/RSRQ bands, per-RXSS click interval, pulse duration, sound toggles (tiers 0–15, 12, 13, RSRQ 14) |
| **Mock network** | `passiveMock` | `PassiveMockSettings` | Mock enable, scenario, RSRP, RSRQ — see below |
| Alert audio & voice | `audio` | `AudioVolumeSettings` | Volumes, voice toggles, TTS engine choice, tier-5 announcer |

### `passiveMock` keys (full capture)

| Key | Type | Description |
|-----|------|-------------|
| `enabled` | boolean | Mock network card master toggle |
| `scenario` | string | One of `MockNetworkScenario` enum names — see [MOCK_NETWORK_SCENARIOS.md](MOCK_NETWORK_SCENARIOS.md) |
| `rsrpDbm` | int | Mock RSRP / 2G RX level (−130 … max tier RSRP) |
| `rsrqDb` | int | Mock RSRQ (LTE/NR scenarios only) |

Scenario reference and trigger states: [MOCK_NETWORK_SCENARIOS.md](MOCK_NETWORK_SCENARIOS.md).

### `passiveSignal` keys (summary)

All tier click intervals, pulse durations, and sound-enable flags for:

- RSRP tiers **1–6** (very strong → critical)
- 2G tiers **7–8**, **15**
- Camp tiers **0**, **10**, **11**, **12**, **13**
- RSRQ overlay **14**
- No-signal / dead-zone / searching / limited-service / limited-alt-2G camp settings

Full key list: `PASSIVE_SIGNAL_KEYS` in `AppSettingsSnapshotCodecCompletenessTest`.

### `audio` keys (summary)

Ping click volume, signal pulse frequency/duration, per-tier pulse frequencies, cell-change bell/voice, technology-change tone/voice (2G/4G/5G ENDC), tier-5 announcer, no-signal tone/voice/vibration, limited-service tone/voice, voice announcer choice + engine id.

Full key list: `AUDIO_KEYS` in `AppSettingsSnapshotCodecCompletenessTest`.

---

## What is **not** saved

| Item | Notes |
|------|--------|
| Monitoring running / stopped | Session state only |
| Current `ConnectivityStats` / RXSS display | Recomputed after load |
| Passive-only vs active ping session | User starts monitoring again after load |
| Saved profile list metadata beyond each profile | Each file is self-contained |
| Android permissions | Re-requested at runtime |
| App version / `versionCode` | Profiles are forward-compatible within schema v1 |

Loading a profile **applies settings immediately** to repositories and `MonitorState`. Changing SIM while monitoring may restart the session.

---

## Limits & behaviour

- Up to **24** named profiles on device (app document storage).
- Save rejected if settings match factory defaults (`AppSettingsSnapshot.isDefault()`).
- Duplicate profile names rejected.
- Export: `Downloads/NotSpotter/NotSpotter - {name}.json`
- On load, passive signal settings are **normalized** with audio pulse durations (`SettingsCompatibility`).

---

## Related docs

- [MOCK_NETWORK_SCENARIOS.md](MOCK_NETWORK_SCENARIOS.md) — mock scenario trigger states
- [RXSS_CATALOGUE.md](RXSS_CATALOGUE.md) — tier / RXSS mapping
- [VOICE_ANNOUNCEMENTS.md](VOICE_ANNOUNCEMENTS.md) — voice toggles in `audio` section
