package io.github.cloolalang.notspotdetector.data

import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import io.github.cloolalang.notspotdetector.model.AudioVolumeSettings
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.PassiveMockSettings
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.PingSettings
import io.github.cloolalang.notspotdetector.model.SettingsProfile
import io.github.cloolalang.notspotdetector.model.ThresholdSettings
import org.json.JSONArray
import org.json.JSONObject

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
        return AppSettingsSnapshot(
            thresholds = decodeThresholds(json.optJSONObject("thresholds")),
            pingSettings = decodePing(json.optJSONObject("ping")),
            monitoringSettings = decodeMonitoring(json.optJSONObject("monitoring")),
            passiveSignalSettings = decodePassiveSignal(json.optJSONObject("passiveSignal")),
            passiveMockSettings = decodePassiveMock(json.optJSONObject("passiveMock")),
            audioVolumes = decodeAudio(json.optJSONObject("audio"))
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
            .put("noisyRsrqPassiveClicks", settings.noisyRsrqPassiveClicks)
            .put("quietAlertRsrqDb", settings.quietAlertRsrqDb)
            .put("quietAlertRsrpMaxDbm", settings.quietAlertRsrpMaxDbm)
            .put("criticalTierClickIntervalMs", settings.criticalTierClickIntervalMs)
            .put("poorTierClickIntervalMs", settings.poorTierClickIntervalMs)
            .put("fairTierClickIntervalMs", settings.fairTierClickIntervalMs)
            .put("goodTierClickIntervalMs", settings.goodTierClickIntervalMs)
            .put("mildTierClickIntervalMs", settings.mildTierClickIntervalMs)
            .put("veryStrongTierClickIntervalMs", settings.veryStrongTierClickIntervalMs)
    }

    private fun decodePassiveSignal(json: JSONObject?): PassiveSignalSettings {
        if (json == null) return PassiveSignalSettings()
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
            noisyRsrqPassiveClicks = json.optBoolean(
                "noisyRsrqPassiveClicks",
                PassiveSignalSettings.DEFAULT_NOISY_RSRQ_PASSIVE_CLICKS
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
            )
        )
    }

    private fun encodePassiveMock(settings: PassiveMockSettings): JSONObject {
        return JSONObject()
            .put("enabled", settings.enabled)
            .put("rsrpDbm", settings.rsrpDbm)
            .put("rsrqDb", settings.rsrqDb)
    }

    private fun decodePassiveMock(json: JSONObject?): PassiveMockSettings {
        if (json == null) return PassiveMockSettings()
        return PassiveMockSettings(
            enabled = json.optBoolean("enabled", PassiveMockSettings.DEFAULT_ENABLED),
            rsrpDbm = json.optInt("rsrpDbm", PassiveMockSettings.DEFAULT_RSRP_DBM),
            rsrqDb = json.optInt("rsrqDb", PassiveMockSettings.DEFAULT_RSRQ_DB)
        )
    }

    private fun encodeAudio(settings: AudioVolumeSettings): JSONObject {
        return JSONObject()
            .put("pingClickVolume", settings.pingClickVolume.toDouble())
            .put("lowSignalClickVolume", settings.lowSignalClickVolume.toDouble())
            .put("signalPulseFrequencyHz", settings.signalPulseFrequencyHz)
            .put("signalPulseDurationMs", settings.signalPulseDurationMs)
            .put("cellChangeBellVolume", settings.cellChangeBellVolume.toDouble())
            .put("cellChangeVoiceEnabled", settings.cellChangeVoiceEnabled)
            .put("cellChangeVoiceVolume", settings.cellChangeVoiceVolume.toDouble())
            .put("technologyChangeVolume", settings.technologyChangeVolume.toDouble())
            .put("technologyChangeVoiceEnabled", settings.technologyChangeVoiceEnabled)
            .put("technologyChangeVoiceVolume", settings.technologyChangeVoiceVolume.toDouble())
            .put("noSignalToneVolume", settings.noSignalToneVolume.toDouble())
            .put("noSignalVoiceEnabled", settings.noSignalVoiceEnabled)
            .put("noSignalVoiceVolume", settings.noSignalVoiceVolume.toDouble())
            .put("limitedServiceToneVolume", settings.limitedServiceToneVolume.toDouble())
            .put("limitedServiceVoiceEnabled", settings.limitedServiceVoiceEnabled)
            .put("limitedServiceVoiceVolume", settings.limitedServiceVoiceVolume.toDouble())
    }

    private fun decodeAudio(json: JSONObject?): AudioVolumeSettings {
        if (json == null) return AudioVolumeSettings()
        return AudioVolumeSettings(
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
            signalPulseDurationMs = json.optInt(
                "signalPulseDurationMs",
                AudioVolumeSettings.DEFAULT_SIGNAL_PULSE_DURATION_MS
            ),
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
            technologyChangeVolume = json.optDouble(
                "technologyChangeVolume",
                AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
            ).toFloat(),
            technologyChangeVoiceEnabled = json.optBoolean(
                "technologyChangeVoiceEnabled",
                AudioVolumeSettings.DEFAULT_VOICE_ANNOUNCEMENT_ENABLED
            ),
            technologyChangeVoiceVolume = json.optDouble(
                "technologyChangeVoiceVolume",
                AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
            ).toFloat(),
            noSignalToneVolume = json.optDouble(
                "noSignalToneVolume",
                AudioVolumeSettings.DEFAULT_VOLUME.toDouble()
            ).toFloat(),
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
            ).toFloat()
        )
    }
}
