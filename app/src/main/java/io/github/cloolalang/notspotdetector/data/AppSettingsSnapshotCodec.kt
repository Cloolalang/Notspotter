package io.github.cloolalang.notspotdetector.data

import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.CellReselectBandNamingStyle
import io.github.cloolalang.notspotdetector.model.MockNetworkScenario
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.SettingsProfile
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import io.github.cloolalang.notspotdetector.model.VoiceAnnouncerChoice
import kotlin.math.roundToInt
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON codec for settings profiles (schema v1).
 *
 * `settings` sections: `thresholds`, `ping`, `monitoring`, `passiveSignal`, `passiveMock`, `audio`.
 * Mock block keys: `enabled`, `scenario` ([MockNetworkScenario.name]), `rsrpDbm`, `rsrqDb`.
 * Full field lists: [AppSettingsSnapshotCodecCompletenessTest].
 * Documentation: [SETTINGS_PROFILES.md].
 */
object AppSettingsSnapshotCodec {

    private const val SCHEMA_VERSION = 1

    fun encodeProfile(profile: SettingsProfile): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("profile", encodeProfileObject(profile))
        return root.toString(2)
    }

    fun decodeProfile(json: String): SettingsProfile? {
        if (json.isBlank()) return null
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        root.optJSONObject("profile")?.let { return decodeProfileObject(it) }
        root.optJSONArray("profiles")?.let { array ->
            if (array.length() > 0) {
                return decodeProfileObject(array.getJSONObject(0))
            }
        }
        if (root.has("settings")) {
            return decodeProfileObject(root)
        }
        return null
    }

    fun encodeProfiles(profiles: List<SettingsProfile>): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        val array = JSONArray()
        for (profile in profiles) {
            array.put(encodeProfileObject(profile))
        }
        root.put("profiles", array)
        return root.toString()
    }

    fun decodeProfiles(json: String): List<SettingsProfile> {
        if (json.isBlank()) return emptyList()
        val root = JSONObject(json)
        val array = root.optJSONArray("profiles") ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                decodeProfileObject(array.getJSONObject(index))?.let(::add)
            }
        }
    }

    private fun encodeProfileObject(profile: SettingsProfile): JSONObject {
        return JSONObject()
            .put("id", profile.id)
            .put("name", profile.name)
            .put("savedAtMs", profile.savedAtMs)
            .put("settings", encodeSettings(profile.settings))
    }

    private fun decodeProfileObject(json: JSONObject): SettingsProfile? {
        val id = json.optString("id", "")
        val name = json.optString("name", "")
        if (id.isBlank() || name.isBlank()) return null
        val settingsJson = json.optJSONObject("settings") ?: return null
        return SettingsProfile(
            id = id,
            name = name,
            savedAtMs = json.optLong("savedAtMs", 0L),
            settings = decodeSettings(settingsJson)
        )
    }

    private fun encodeSettings(settings: AppSettingsSnapshot): JSONObject {
        return JSONObject()
            .put("thresholds", encodeThresholds(settings.thresholds))
            .put("ping", encodePing(settings.pingSettings))
            .put("monitoring", encodeMonitoring(settings.monitoringSettings))
            .put("passiveSignal", encodePassiveSignal(settings.passiveSignalSettings))
            .put("passiveMock", encodePassiveMock(settings.passiveMockSettings))
            .put("audio", encodeAudio(settings.audioVolumes))
    }

    private fun decodeSettings(json: JSONObject): AppSettingsSnapshot {
        val audioVolumes = decodeAudio(json.optJSONObject("audio"))
        return AppSettingsSnapshot(
            thresholds = decodeThresholds(json.optJSONObject("thresholds")),
            pingSettings = decodePing(json.optJSONObject("ping")),
            monitoringSettings = decodeMonitoring(json.optJSONObject("monitoring")),
            passiveSignalSettings = decodePassiveSignal(json.optJSONObject("passiveSignal"), audioVolumes),
            passiveMockSettings = decodePassiveMock(json.optJSONObject("passiveMock")),
            audioVolumes = audioVolumes
        ).normalized()
    }

    private fun encodeThresholds(settings: ThresholdSettings): JSONObject {
        return JSONObject()
            .put("goodRttMs", settings.goodRttMs)
            .put("poorRttMs", settings.poorRttMs)
            .put("poorJitterMs", settings.poorJitterMs)
            .put("poorPacketLossPercent", settings.poorPacketLossPercent.toDouble())
            .put("suppressClicksOnGoodConnection", settings.suppressClicksOnGoodConnection)
            .put("goodConnectionClicksPerPing", settings.goodConnectionClicksPerPing)
    }

    private fun decodeThresholds(json: JSONObject?): ThresholdSettings {
        if (json == null) return ThresholdSettings()
        return ThresholdSettings(
            goodRttMs = json.optLong("goodRttMs", ThresholdSettings.DEFAULT_GOOD_RTT_MS),
            poorRttMs = json.optLong("poorRttMs", ThresholdSettings.DEFAULT_POOR_RTT_MS),
            poorJitterMs = json.optLong("poorJitterMs", ThresholdSettings.DEFAULT_POOR_JITTER_MS),
            poorPacketLossPercent = json.optDouble(
                "poorPacketLossPercent",
                ThresholdSettings.DEFAULT_POOR_PACKET_LOSS_PERCENT.toDouble()
            ).toFloat(),
            suppressClicksOnGoodConnection = json.optBoolean("suppressClicksOnGoodConnection", false),
            goodConnectionClicksPerPing = json.optInt(
                "goodConnectionClicksPerPing",
                ThresholdSettings.DEFAULT_GOOD_CONNECTION_CLICKS_PER_PING
            )
        )
    }

    private fun encodePing(settings: PingSettings): JSONObject {
        return JSONObject()
            .put("host", settings.host)
            .put("port", settings.port)
            .put("pingsPerTest", settings.pingsPerTest)
            .put("testIntervalMs", settings.testIntervalMs)
    }

    private fun decodePing(json: JSONObject?): PingSettings {
        if (json == null) return PingSettings()
        return PingSettings(
            host = json.optString("host", PingSettings.DEFAULT_HOST),
            port = json.optInt("port", PingSettings.DEFAULT_PORT),
            pingsPerTest = json.optInt("pingsPerTest", PingSettings.DEFAULT_PINGS_PER_TEST),
            testIntervalMs = json.optLong("testIntervalMs", PingSettings.DEFAULT_TEST_INTERVAL_MS)
        )
    }

    private fun encodeMonitoring(settings: MonitoringSettings): JSONObject {
        return JSONObject()
            .put("monitor2gFallback", settings.monitor2gFallback)
            .put("subscriptionId", settings.subscriptionId)
            .put("passiveQuietUntilCritical", settings.passiveQuietUntilCritical)
            .put("passiveMeasurementIntervalMs", settings.passiveMeasurementIntervalMs)
            .put("rsrpHistogramWindowMs", settings.rsrpHistogramWindowMs)
    }

    private fun decodeMonitoring(json: JSONObject?): MonitoringSettings {
        if (json == null) return MonitoringSettings()
        return MonitoringSettings(
            monitor2gFallback = json.optBoolean(
                "monitor2gFallback",
                MonitoringSettings.DEFAULT_MONITOR_2G_FALLBACK
            ),
            subscriptionId = json.optInt(
                "subscriptionId",
                MonitoringSettings.DEFAULT_SUBSCRIPTION_ID
            ),
            passiveQuietUntilCritical = json.optBoolean(
                "passiveQuietUntilCritical",
                MonitoringSettings.DEFAULT_PASSIVE_QUIET_UNTIL_CRITICAL
            ),
            passiveMeasurementIntervalMs = json.optLong(
                "passiveMeasurementIntervalMs",
                MonitoringSettings.DEFAULT_PASSIVE_MEASUREMENT_INTERVAL_MS
            ),
            rsrpHistogramWindowMs = json.optLong(
                "rsrpHistogramWindowMs",
                MonitoringSettings.DEFAULT_RSRP_HISTOGRAM_WINDOW_MS
            )
        )
    }

    private fun encodePassiveSignal(settings: PassiveSignalSettings): JSONObject {
        return JSONObject()
            .put("noSignalRsrpDbm", settings.noSignalRsrpDbm)
            .put("poorRsrpMinDbm", settings.poorRsrpMinDbm)
            .put("fairRsrpMinDbm", settings.fairRsrpMinDbm)
            .put("goodRsrpMinDbm", settings.goodRsrpMinDbm)
            .put("mildRsrpMinDbm", settings.mildRsrpMinDbm)
            .put("veryStrongRsrpMinDbm", settings.veryStrongRsrpMinDbm)
            .put("rsrqFairMinDb", settings.rsrqFairMinDb)
            .put("rsrqTierSoundEnabled", settings.rsrqTierSoundEnabled)
            .put("rsrqTierCoupledToSignalTier", settings.rsrqTierCoupledToSignalTier)
            .put("rsrqTierWhiteNoiseVolume", settings.rsrqTierWhiteNoiseVolume.toDouble())
            .put("rsrqTierClickIntervalMs", settings.rsrqTierClickIntervalMs)
            .put("rsrqTierPulseDurationMs", settings.rsrqTierPulseDurationMs)
            .put("quietAlertRsrqDb", settings.quietAlertRsrqDb)
            .put("quietAlertRsrpMaxDbm", settings.quietAlertRsrpMaxDbm)
            .put("criticalTierClickIntervalMs", settings.criticalTierClickIntervalMs)
            .put("criticalTierPulseDurationMs", settings.criticalTierPulseDurationMs)
            .put("mildTierPulseDurationMs", settings.mildTierPulseDurationMs)
            .put("goodTierPulseDurationMs", settings.goodTierPulseDurationMs)
            .put("fairTierPulseDurationMs", settings.fairTierPulseDurationMs)
            .put("poorTierPulseDurationMs", settings.poorTierPulseDurationMs)
            .put("levelRangeAbcdClickIntervalMs", settings.levelRangeAbcdClickIntervalMs)
            .put("poorTierClickIntervalMs", settings.poorTierClickIntervalMs)
            .put("fairTierClickIntervalMs", settings.fairTierClickIntervalMs)
            .put("goodTierClickIntervalMs", settings.goodTierClickIntervalMs)
            .put("mildTierClickIntervalMs", settings.mildTierClickIntervalMs)
            .put("veryStrongTierClickIntervalMs", settings.veryStrongTierClickIntervalMs)
            .put("veryStrongTierSoundEnabled", settings.veryStrongTierSoundEnabled)
            .put("mildTierSoundEnabled", settings.mildTierSoundEnabled)
            .put("goodTierSoundEnabled", settings.goodTierSoundEnabled)
            .put("fairTierSoundEnabled", settings.fairTierSoundEnabled)
            .put("poorTierSoundEnabled", settings.poorTierSoundEnabled)
            .put("criticalTierSoundEnabled", settings.criticalTierSoundEnabled)
            .put("g2StrongTierClickIntervalMs", settings.g2StrongTierClickIntervalMs)
            .put("g2StrongTierPulseDurationMs", settings.g2StrongTierPulseDurationMs)
            .put("g2WeakTierClickIntervalMs", settings.g2WeakTierClickIntervalMs)
            .put("g2WeakTierPulseDurationMs", settings.g2WeakTierPulseDurationMs)
            .put("g2StrongTierSoundEnabled", settings.g2StrongTierSoundEnabled)
            .put("g2WeakTierSoundEnabled", settings.g2WeakTierSoundEnabled)
            .put("g2NoSignalTierClickIntervalMs", settings.g2NoSignalTierClickIntervalMs)
            .put("g2NoSignalTierSoundEnabled", settings.g2NoSignalTierSoundEnabled)
            .put("g2NoSignalTierPulseDurationMs", settings.g2NoSignalTierPulseDurationMs)
            .put("deadzoneTierClickIntervalMs", settings.deadzoneTierClickIntervalMs)
            .put("deadzoneTierSoundEnabled", settings.deadzoneTierSoundEnabled)
            .put("deadzoneTierPulseDurationMs", settings.deadzoneTierPulseDurationMs)
            .put("noSignalTierClickIntervalMs", settings.noSignalTierClickIntervalMs)
            .put("noSignalTierSoundEnabled", settings.noSignalTierSoundEnabled)
            .put("noSignalTierPulseDurationMs", settings.noSignalTierPulseDurationMs)
            .put("searching2gTierClickIntervalMs", settings.searching2gTierClickIntervalMs)
            .put("searching2gTierSoundEnabled", settings.searching2gTierSoundEnabled)
            .put("searching2gTierPulseDurationMs", settings.searching2gTierPulseDurationMs)
            .put("limitedServiceTierClickIntervalMs", settings.limitedServiceTierClickIntervalMs)
            .put("limitedServiceTierSoundEnabled", settings.limitedServiceTierSoundEnabled)
            .put("limitedServiceTierPulseDurationMs", settings.limitedServiceTierPulseDurationMs)
            .put("limitedAlt2gTierClickIntervalMs", settings.limitedAlt2gTierClickIntervalMs)
            .put("limitedAlt2gTierSoundEnabled", settings.limitedAlt2gTierSoundEnabled)
            .put("limitedAlt2gTierPulseDurationMs", settings.limitedAlt2gTierPulseDurationMs)
            .put("wifiCallingTierClickIntervalMs", settings.wifiCallingTierClickIntervalMs)
            .put("wifiCallingTierSoundEnabled", settings.wifiCallingTierSoundEnabled)
            .put("wifiCallingTierPulseDurationMs", settings.wifiCallingTierPulseDurationMs)
    }

    private fun decodePassiveSignal(json: JSONObject?, audioVolumes: AudioVolumeSettings): PassiveSignalSettings {
        if (json == null) {
            return PassiveSignalSettings(
                criticalTierPulseDurationMs = audioVolumes.signalPulseDurationMs,
                mildTierPulseDurationMs = audioVolumes.levelRangeBcdPulseDurationMs,
                goodTierPulseDurationMs = audioVolumes.levelRangeBcdPulseDurationMs,
                fairTierPulseDurationMs = audioVolumes.levelRangeBcdPulseDurationMs,
                poorTierPulseDurationMs = audioVolumes.levelRangeBcdPulseDurationMs
            )
        }
        return PassiveSignalSettings(
            noSignalRsrpDbm = json.optInt("noSignalRsrpDbm", PassiveSignalSettings.DEFAULT_NO_SIGNAL_RSRP_DBM),
            poorRsrpMinDbm = json.optInt("poorRsrpMinDbm", PassiveSignalSettings.DEFAULT_POOR_RSRP_MIN_DBM),
            fairRsrpMinDbm = json.optInt("fairRsrpMinDbm", PassiveSignalSettings.DEFAULT_FAIR_RSRP_MIN_DBM),
            goodRsrpMinDbm = json.optInt("goodRsrpMinDbm", PassiveSignalSettings.DEFAULT_GOOD_RSRP_MIN_DBM),
            mildRsrpMinDbm = json.optInt("mildRsrpMinDbm", PassiveSignalSettings.DEFAULT_MILD_RSRP_MIN_DBM),
            veryStrongRsrpMinDbm = json.optInt(
                "veryStrongRsrpMinDbm",
                PassiveSignalSettings.DEFAULT_VERY_STRONG_RSRP_MIN_DBM
            ),
            rsrqFairMinDb = json.optInt("rsrqFairMinDb", PassiveSignalSettings.DEFAULT_RSRQ_FAIR_MIN_DB),
            rsrqTierSoundEnabled = json.optBoolean(
                "rsrqTierSoundEnabled",
                json.optBoolean("noisyRsrqPassiveClicks", PassiveSignalSettings.DEFAULT_RSRQ_TIER_SOUND_ENABLED)
            ),
            rsrqTierCoupledToSignalTier = json.optBoolean(
                "rsrqTierCoupledToSignalTier",
                PassiveSignalSettings.DEFAULT_RSRQ_TIER_COUPLED_TO_SIGNAL_TIER
            ),
            rsrqTierWhiteNoiseVolume = json.optDouble(
                "rsrqTierWhiteNoiseVolume",
                PassiveSignalSettings.DEFAULT_RSRQ_TIER_WHITE_NOISE_VOLUME.toDouble()
            ).toFloat(),
            rsrqTierClickIntervalMs = json.optInt(
                "rsrqTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_RSRQ_TIER_CLICK_INTERVAL_MS
            ),
            rsrqTierPulseDurationMs = json.optInt(
                "rsrqTierPulseDurationMs",
                PassiveSignalSettings.DEFAULT_RSRQ_TIER_PULSE_DURATION_MS
            ),
            quietAlertRsrqDb = json.optInt("quietAlertRsrqDb", PassiveSignalSettings.DEFAULT_QUIET_ALERT_RSRQ_DB),
            quietAlertRsrpMaxDbm = json.optInt(
                "quietAlertRsrpMaxDbm",
                PassiveSignalSettings.DEFAULT_QUIET_ALERT_RSRP_MAX_DBM
            ),
            criticalTierClickIntervalMs = json.optInt(
                "criticalTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_CRITICAL_TIER_CLICK_INTERVAL_MS
            ),
            criticalTierPulseDurationMs = json.optInt(
                "criticalTierPulseDurationMs",
                audioVolumes.signalPulseDurationMs
            ),
            mildTierPulseDurationMs = json.optInt(
                "mildTierPulseDurationMs",
                audioVolumes.levelRangeBcdPulseDurationMs
            ),
            goodTierPulseDurationMs = json.optInt(
                "goodTierPulseDurationMs",
                audioVolumes.levelRangeBcdPulseDurationMs
            ),
            fairTierPulseDurationMs = json.optInt(
                "fairTierPulseDurationMs",
                audioVolumes.levelRangeBcdPulseDurationMs
            ),
            poorTierPulseDurationMs = json.optInt(
                "poorTierPulseDurationMs",
                audioVolumes.levelRangeBcdPulseDurationMs
            ),
            levelRangeAbcdClickIntervalMs = decodeLevelRangeAbcdClickIntervalMs(json),
            poorTierClickIntervalMs = json.optInt(
                "poorTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_POOR_TIER_CLICK_INTERVAL_MS
            ),
            fairTierClickIntervalMs = json.optInt(
                "fairTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_FAIR_TIER_CLICK_INTERVAL_MS
            ),
            goodTierClickIntervalMs = json.optInt(
                "goodTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_GOOD_TIER_CLICK_INTERVAL_MS
            ),
            mildTierClickIntervalMs = json.optInt(
                "mildTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_MILD_TIER_CLICK_INTERVAL_MS
            ),
            veryStrongTierClickIntervalMs = json.optInt(
                "veryStrongTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_VERY_STRONG_TIER_CLICK_INTERVAL_MS
            ),
            veryStrongTierSoundEnabled = json.optBoolean(
                "veryStrongTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            mildTierSoundEnabled = json.optBoolean(
                "mildTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            goodTierSoundEnabled = json.optBoolean(
                "goodTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            fairTierSoundEnabled = json.optBoolean(
                "fairTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            poorTierSoundEnabled = json.optBoolean(
                "poorTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            criticalTierSoundEnabled = json.optBoolean(
                "criticalTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            g2StrongTierClickIntervalMs = json.optInt(
                "g2StrongTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_G2_STRONG_TIER_CLICK_INTERVAL_MS
            ),
            g2StrongTierPulseDurationMs = json.optInt(
                "g2StrongTierPulseDurationMs",
                PassiveSignalSettings.DEFAULT_G2_STRONG_TIER_PULSE_DURATION_MS
            ),
            g2WeakTierClickIntervalMs = json.optInt(
                "g2WeakTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_G2_WEAK_TIER_CLICK_INTERVAL_MS
            ),
            g2WeakTierPulseDurationMs = json.optInt(
                "g2WeakTierPulseDurationMs",
                PassiveSignalSettings.DEFAULT_G2_WEAK_TIER_PULSE_DURATION_MS
            ),
            g2StrongTierSoundEnabled = json.optBoolean(
                "g2StrongTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            g2WeakTierSoundEnabled = json.optBoolean(
                "g2WeakTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            g2NoSignalTierClickIntervalMs = json.optInt(
                "g2NoSignalTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_G2_NO_SIGNAL_TIER_CLICK_INTERVAL_MS
            ),
            g2NoSignalTierSoundEnabled = json.optBoolean(
                "g2NoSignalTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            g2NoSignalTierPulseDurationMs = json.optInt(
                "g2NoSignalTierPulseDurationMs",
                PassiveSignalSettings.DEFAULT_G2_NO_SIGNAL_TIER_PULSE_DURATION_MS
            ),
            deadzoneTierClickIntervalMs = json.optInt(
                "deadzoneTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_DEADZONE_TIER_CLICK_INTERVAL_MS
            ),
            deadzoneTierSoundEnabled = json.optBoolean(
                "deadzoneTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            deadzoneTierPulseDurationMs = json.optInt(
                "deadzoneTierPulseDurationMs",
                PassiveSignalSettings.DEFAULT_DEADZONE_TIER_PULSE_DURATION_MS
            ),
            noSignalTierClickIntervalMs = json.optInt(
                "noSignalTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_NO_SIGNAL_TIER_CLICK_INTERVAL_MS
            ),
            noSignalTierSoundEnabled = json.optBoolean(
                "noSignalTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            noSignalTierPulseDurationMs = json.optInt(
                "noSignalTierPulseDurationMs",
                PassiveSignalSettings.DEFAULT_NO_SIGNAL_TIER_PULSE_DURATION_MS
            ),
            searching2gTierClickIntervalMs = json.optInt(
                "searching2gTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_SEARCHING_2G_TIER_CLICK_INTERVAL_MS
            ),
            searching2gTierSoundEnabled = json.optBoolean(
                "searching2gTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            searching2gTierPulseDurationMs = json.optInt(
                "searching2gTierPulseDurationMs",
                PassiveSignalSettings.DEFAULT_SEARCHING_2G_TIER_PULSE_DURATION_MS
            ),
            limitedServiceTierClickIntervalMs = json.optInt(
                "limitedServiceTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_LIMITED_SERVICE_TIER_CLICK_INTERVAL_MS
            ),
            limitedServiceTierSoundEnabled = json.optBoolean(
                "limitedServiceTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            limitedServiceTierPulseDurationMs = json.optInt(
                "limitedServiceTierPulseDurationMs",
                PassiveSignalSettings.DEFAULT_LIMITED_SERVICE_TIER_PULSE_DURATION_MS
            ),
            limitedAlt2gTierClickIntervalMs = json.optInt(
                "limitedAlt2gTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_LIMITED_ALT_2G_TIER_CLICK_INTERVAL_MS
            ),
            limitedAlt2gTierSoundEnabled = json.optBoolean(
                "limitedAlt2gTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            limitedAlt2gTierPulseDurationMs = json.optInt(
                "limitedAlt2gTierPulseDurationMs",
                PassiveSignalSettings.DEFAULT_LIMITED_ALT_2G_TIER_PULSE_DURATION_MS
            ),
            wifiCallingTierClickIntervalMs = json.optInt(
                "wifiCallingTierClickIntervalMs",
                PassiveSignalSettings.DEFAULT_WIFI_CALLING_TIER_CLICK_INTERVAL_MS
            ),
            wifiCallingTierSoundEnabled = json.optBoolean(
                "wifiCallingTierSoundEnabled",
                PassiveSignalSettings.DEFAULT_TIER_SOUND_ENABLED
            ),
            wifiCallingTierPulseDurationMs = json.optInt(
                "wifiCallingTierPulseDurationMs",
                PassiveSignalSettings.DEFAULT_WIFI_CALLING_TIER_PULSE_DURATION_MS
            )
        )
    }

    private fun encodePassiveMock(settings: PassiveMockSettings): JSONObject {
        return JSONObject()
            .put("enabled", settings.enabled)
            .put("scenario", settings.scenario.name)
            .put("rsrpDbm", settings.rsrpDbm)
            .put("rsrqDb", settings.rsrqDb)
    }

    private fun decodePassiveMock(json: JSONObject?): PassiveMockSettings {
        if (json == null) return PassiveMockSettings()
        return PassiveMockSettings(
            enabled = json.optBoolean("enabled", PassiveMockSettings.DEFAULT_ENABLED),
            scenario = MockNetworkScenario.fromStoredName(json.optString("scenario", null)),
            rsrpDbm = json.optInt("rsrpDbm", PassiveMockSettings.DEFAULT_RSRP_DBM),
            rsrqDb = json.optInt("rsrqDb", PassiveMockSettings.DEFAULT_RSRQ_DB)
        )
    }

    private fun encodeAudio(settings: AudioVolumeSettings): JSONObject {
        return JSONObject()
            .put("masterVoiceAnnouncementsEnabled", settings.masterVoiceAnnouncementsEnabled)
            .put("pingClickVolume", settings.pingClickVolume.toDouble())
            .put("lowSignalClickVolume", settings.lowSignalClickVolume.toDouble())
            .put("signalPulseFrequencyHz", settings.signalPulseFrequencyHz)
            .put("noSignalTierPulseFrequencyHz", settings.noSignalTierPulseFrequencyHz)
            .put("limitedServiceTierPulseFrequencyHz", settings.limitedServiceTierPulseFrequencyHz)
            .put("levelRangeBcdPulseFrequencyHz", settings.levelRangeBcdPulseFrequencyHz)
            .put("veryStrongTierPulseFrequencyHz", settings.veryStrongTierPulseFrequencyHz)
            .put("g2StrongTierPulseFrequencyHz", settings.g2StrongTierPulseFrequencyHz)
            .put("g2WeakTierPulseFrequencyHz", settings.g2WeakTierPulseFrequencyHz)
            .put("signalPulseDurationMs", settings.signalPulseDurationMs)
            .put("levelRangeBcdPulseDurationMs", settings.levelRangeBcdPulseDurationMs)
            .put("levelRangeBcdClickVolume", settings.levelRangeBcdClickVolume.toDouble())
            .put("cellChangeBellVolume", settings.cellChangeBellVolume.toDouble())
            .put("cellChangeVoiceEnabled", settings.cellChangeVoiceEnabled)
            .put("cellChangeVoiceVolume", settings.cellChangeVoiceVolume.toDouble())
            .put("cellChangeSpeakBandEnabled", settings.cellChangeSpeakBandEnabled)
            .put("cellChangeBandNamingStyle", settings.cellChangeBandNamingStyle.id)
            .put("technologyChangeTo2gToneVolume", settings.technologyChangeTo2gToneVolume.toDouble())
            .put("technologyChangeTo2gVoiceEnabled", settings.technologyChangeTo2gVoiceEnabled)
            .put("technologyChangeTo2gVoiceVolume", settings.technologyChangeTo2gVoiceVolume.toDouble())
            .put("technologyChangeTo4gToneVolume", settings.technologyChangeTo4gToneVolume.toDouble())
            .put("technologyChangeTo4gVoiceEnabled", settings.technologyChangeTo4gVoiceEnabled)
            .put("technologyChangeTo4gVoiceVolume", settings.technologyChangeTo4gVoiceVolume.toDouble())
            .put("technologyChangeTo5gEndcToneVolume", settings.technologyChangeTo5gEndcToneVolume.toDouble())
            .put("technologyChangeTo5gEndcVoiceEnabled", settings.technologyChangeTo5gEndcVoiceEnabled)
            .put("technologyChangeTo5gEndcVoiceVolume", settings.technologyChangeTo5gEndcVoiceVolume.toDouble())
            .put("tier5AnnouncerEnabled", settings.tier5AnnouncerEnabled)
            .put("tier5AnnouncerVolume", settings.tier5AnnouncerVolume.toDouble())
            .put("voiceAnnouncerChoice", settings.voiceAnnouncerChoice.id)
            .put("voiceAnnouncerEngineId", settings.voiceAnnouncerEngineId)
            .put("noSignalToneVolume", settings.noSignalToneVolume.toDouble())
            .put("noSignalVibrationEnabled", settings.noSignalVibrationEnabled)
            .put("noSignalVoiceEnabled", settings.noSignalVoiceEnabled)
            .put("noSignalVoiceVolume", settings.noSignalVoiceVolume.toDouble())
            .put("limitedServiceToneVolume", settings.limitedServiceToneVolume.toDouble())
            .put("limitedServiceVoiceEnabled", settings.limitedServiceVoiceEnabled)
            .put("limitedServiceVoiceVolume", settings.limitedServiceVoiceVolume.toDouble())
            .put("speakOperatorNameEnabled", settings.speakOperatorNameEnabled)
            .put("speakTechnologyEnabled", settings.speakTechnologyEnabled)
    }

    private fun decodeVeryStrongTierPulseFrequencyHz(json: JSONObject): Int {
        if (json.has("veryStrongTierPulseFrequencyHz")) {
            return json.optInt(
                "veryStrongTierPulseFrequencyHz",
                AudioVolumeSettings.DEFAULT_VERY_STRONG_TIER_PULSE_FREQUENCY_HZ
            )
        }
        val legacySignalHz = json.optInt(
            "signalPulseFrequencyHz",
            AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
        )
        return (legacySignalHz * AudioVolumeSettings.LEGACY_VERY_STRONG_FREQUENCY_MULTIPLIER).roundToInt()
    }

    private fun decodeG2TierPulseFrequencyHz(json: JSONObject, key: String): Int {
        if (json.has(key)) {
            return json.optInt(key, AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ)
        }
        return json.optInt(
            "signalPulseFrequencyHz",
            AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
        )
    }

    private fun decodeNoSignalTierPulseFrequencyHz(json: JSONObject): Int {
        if (json.has("noSignalTierPulseFrequencyHz")) {
            return json.optInt(
                "noSignalTierPulseFrequencyHz",
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
            )
        }
        return json.optInt(
            "signalPulseFrequencyHz",
            AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
        )
    }

    private fun decodeLimitedServiceTierPulseFrequencyHz(json: JSONObject): Int {
        if (json.has("limitedServiceTierPulseFrequencyHz")) {
            return json.optInt(
                "limitedServiceTierPulseFrequencyHz",
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
            )
        }
        return json.optInt(
            "signalPulseFrequencyHz",
            AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
        )
    }

    private fun decodeLevelRangeBcdPulseFrequencyHz(json: JSONObject): Int {
        if (json.has("levelRangeBcdPulseFrequencyHz")) {
            return json.optInt(
                "levelRangeBcdPulseFrequencyHz",
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
            )
        }
        return json.optInt(
            "signalPulseFrequencyHz",
            AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
        )
    }

    private fun decodeLevelRangeBcdPulseDurationMs(json: JSONObject): Int {
        if (json.has("levelRangeBcdPulseDurationMs")) {
            return json.optInt(
                "levelRangeBcdPulseDurationMs",
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
            )
        }
        return json.optInt(
            "signalPulseDurationMs",
            AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
        )
    }

    private fun decodeLevelRangeAbcdClickIntervalMs(json: JSONObject): Int {
        if (json.has("levelRangeAbcdClickIntervalMs")) {
            return json.optInt(
                "levelRangeAbcdClickIntervalMs",
                PassiveSignalSettings.DEFAULT_LEVEL_RANGE_ABCD_CLICK_INTERVAL_MS
            )
        }
        return json.optInt(
            "goodTierClickIntervalMs",
            PassiveSignalSettings.DEFAULT_LEVEL_RANGE_ABCD_CLICK_INTERVAL_MS
        )
    }

    private fun decodeAudio(json: JSONObject?): AudioVolumeSettings {
        if (json == null) return AudioVolumeSettings()
        return AudioVolumeSettings(
            masterVoiceAnnouncementsEnabled = json.optBoolean(
                "masterVoiceAnnouncementsEnabled",
                AudioVolumeSettings.DEFAULT_MASTER_VOICE_ANNOUNCEMENTS_ENABLED
            ),
            pingClickVolume = json.optDouble("pingClickVolume", AudioVolumeSettings.DEFAULT_VOLUME.toDouble())
                .toFloat(),
            lowSignalClickVolume = json.optDouble(
                "lowSignalClickVolume",
                AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
            ).toFloat(),
            signalPulseFrequencyHz = json.optInt(
                "signalPulseFrequencyHz",
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_FREQUENCY_HZ
            ),
            noSignalTierPulseFrequencyHz = decodeNoSignalTierPulseFrequencyHz(json),
            limitedServiceTierPulseFrequencyHz = decodeLimitedServiceTierPulseFrequencyHz(json),
            levelRangeBcdPulseFrequencyHz = decodeLevelRangeBcdPulseFrequencyHz(json),
            veryStrongTierPulseFrequencyHz = decodeVeryStrongTierPulseFrequencyHz(json),
            g2StrongTierPulseFrequencyHz = decodeG2TierPulseFrequencyHz(json, "g2StrongTierPulseFrequencyHz"),
            g2WeakTierPulseFrequencyHz = decodeG2TierPulseFrequencyHz(json, "g2WeakTierPulseFrequencyHz"),
            signalPulseDurationMs = json.optInt(
                "signalPulseDurationMs",
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
            ),
            levelRangeBcdPulseDurationMs = decodeLevelRangeBcdPulseDurationMs(json),
            levelRangeBcdClickVolume = json.optDouble(
                "levelRangeBcdClickVolume",
                json.optDouble("lowSignalClickVolume", AudioVolumeSettings.DEFAULT_VOLUME.toDouble())
            ).toFloat(),
            cellChangeBellVolume = json.optDouble(
                "cellChangeBellVolume",
                AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
            ).toFloat(),
            cellChangeVoiceEnabled = json.optBoolean(
                "cellChangeVoiceEnabled",
                AudioVolumeSettings.DEFAULT_CELL_CHANGE_VOICE_ENABLED
            ),
            cellChangeVoiceVolume = json.optDouble(
                "cellChangeVoiceVolume",
                AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
            ).toFloat(),
            cellChangeSpeakBandEnabled = json.optBoolean(
                "cellChangeSpeakBandEnabled",
                AudioVolumeSettings.DEFAULT_CELL_CHANGE_SPEAK_BAND_ENABLED
            ),
            cellChangeBandNamingStyle = CellReselectBandNamingStyle.fromId(
                json.optString("cellChangeBandNamingStyle", CellReselectBandNamingStyle.DEFAULT.id)
            ),
            technologyChangeTo2gToneVolume = decodeTechnologyChangeToneVolume(json, "technologyChangeTo2gToneVolume"),
            technologyChangeTo2gVoiceEnabled = decodeTechnologyChangeVoiceEnabled(json, "technologyChangeTo2gVoiceEnabled"),
            technologyChangeTo2gVoiceVolume = decodeTechnologyChangeVoiceVolume(json, "technologyChangeTo2gVoiceVolume"),
            technologyChangeTo4gToneVolume = decodeTechnologyChangeToneVolume(json, "technologyChangeTo4gToneVolume"),
            technologyChangeTo4gVoiceEnabled = decodeTechnologyChangeVoiceEnabled(json, "technologyChangeTo4gVoiceEnabled"),
            technologyChangeTo4gVoiceVolume = decodeTechnologyChangeVoiceVolume(json, "technologyChangeTo4gVoiceVolume"),
            technologyChangeTo5gEndcToneVolume = decodeTechnologyChangeToneVolume(json, "technologyChangeTo5gEndcToneVolume"),
            technologyChangeTo5gEndcVoiceEnabled = decodeTechnologyChangeVoiceEnabled(json, "technologyChangeTo5gEndcVoiceEnabled"),
            technologyChangeTo5gEndcVoiceVolume = decodeTechnologyChangeVoiceVolume(json, "technologyChangeTo5gEndcVoiceVolume"),
            tier5AnnouncerEnabled = json.optBoolean(
                "tier5AnnouncerEnabled",
                AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED
            ),
            tier5AnnouncerVolume = json.optDouble(
                "tier5AnnouncerVolume",
                AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
            ).toFloat(),
            voiceAnnouncerChoice = VoiceAnnouncerChoice.fromId(
                json.optString("voiceAnnouncerChoice", VoiceAnnouncerChoice.SYSTEM_DEFAULT.id)
            ),
            voiceAnnouncerEngineId = json.optString("voiceAnnouncerEngineId").ifBlank { null },
            noSignalToneVolume = json.optDouble(
                "noSignalToneVolume",
                AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
            ).toFloat(),
            noSignalVibrationEnabled = json.optBoolean(
                "noSignalVibrationEnabled",
                AudioVolumeSettings.DEFAULT_NO_SIGNAL_VIBRATION_ENABLED
            ),
            noSignalVoiceEnabled = json.optBoolean(
                "noSignalVoiceEnabled",
                AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED
            ),
            noSignalVoiceVolume = json.optDouble(
                "noSignalVoiceVolume",
                AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
            ).toFloat(),
            limitedServiceToneVolume = json.optDouble(
                "limitedServiceToneVolume",
                AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
            ).toFloat(),
            limitedServiceVoiceEnabled = json.optBoolean(
                "limitedServiceVoiceEnabled",
                AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED
            ),
            limitedServiceVoiceVolume = json.optDouble(
                "limitedServiceVoiceVolume",
                AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
            ).toFloat(),
            speakOperatorNameEnabled = json.optBoolean(
                "speakOperatorNameEnabled",
                AudioVolumeSettings.DEFAULT_SPEAK_OPERATOR_NAME_ENABLED
            ),
            speakTechnologyEnabled = json.optBoolean(
                "speakTechnologyEnabled",
                AudioVolumeSettings.DEFAULT_SPEAK_TECHNOLOGY_ENABLED
            )
        )
    }

    private fun decodeTechnologyChangeToneVolume(json: JSONObject, key: String): Float {
        if (json.has(key)) {
            return json.optDouble(key, AudioVolumeSettings.DEFAULT_VOLUME.toDouble()).toFloat()
        }
        return json.optDouble(
            "technologyChangeVolume",
            AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
        ).toFloat()
    }

    private fun decodeTechnologyChangeVoiceEnabled(json: JSONObject, key: String): Boolean {
        if (json.has(key)) {
            return json.optBoolean(key, AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED)
        }
        return json.optBoolean(
            "technologyChangeVoiceEnabled",
            AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED
        )
    }

    private fun decodeTechnologyChangeVoiceVolume(json: JSONObject, key: String): Float {
        if (json.has(key)) {
            return json.optDouble(key, AudioVolumeSettings.DEFAULT_VOLUME.toDouble()).toFloat()
        }
        return json.optDouble(
            "technologyChangeVoiceVolume",
            AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
        ).toFloat()
    }
}
