# Notspot Detector

Android app for monitoring cellular signal quality, connection state, and related alerts.

## Documentation

- [RXSS_CATALOGUE.md](RXSS_CATALOGUE.md) — RX Signal State catalogue (numbering, concurrency, ladders)
- [VOICE_ANNOUNCEMENTS.md](VOICE_ANNOUNCEMENTS.md) — voice alert triggers, playback order, and suppression rules
- [MOCK_NETWORK_SCENARIOS.md](MOCK_NETWORK_SCENARIOS.md) — passive mock scenarios, trigger states, and VA testing
- [SETTINGS_PROFILES.md](SETTINGS_PROFILES.md) — profile save/load JSON scope (includes mock settings)
- [CHANGELOG.md](CHANGELOG.md) — release history

## Open items (catalogue / implementation)

Revisit when implementing full RXSS state machine in the app:

### Suspended SIM vs normal fallback (2026-09-09)

**Current catalogue choice:** one unified fallback ladder for active and suspended SIM (`10 → 24 → 25 → 11 → …`); limited home/alt camps (**19**, **22**, **12**, **13**) apply once camped.

**Why revisit:** 3GPP idle-mode specs describe a separate **limited service** path when subscription is barred (e.g. TS 23.122 §3.5, TS 24.301 EMM causes #7 / #14 / #15). The UE may camp for emergency only, continue PLMN search, and not follow normal registered attach — which may not match a single ladder for all SIM states.

**References:**

- [TS 23.122 §3.5 — No suitable cell (limited service state)](https://itecspec.com/3gpp/23.122/s/3.5)
- [TS 24.301 — EPS services not allowed / limited service causes](https://itecspec.com/3gpp/24.301/s/a.2)

**Decision options when revisiting:** separate suspended-SIM ladder vs unified ladder with different outcomes only vs hybrid (shared radio fallback, different NAS outcomes).
