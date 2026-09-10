package io.github.cloolalang.notspotdetector.network

import android.content.Context
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import io.github.cloolalang.notspotdetector.model.CarrierConfigEntry
import io.github.cloolalang.notspotdetector.model.CarrierConfigSnapshot
import io.github.cloolalang.notspotdetector.model.MonitoringSettings

/**
 * Reads a curated set of [CarrierConfigManager] keys that affect actual UE (device) behavior —
 * VoLTE, WiFi calling, VoNR, NR/5G availability, network selection, and emergency calling — for
 * research/diagnostics. Deliberately excludes the hundreds of purely cosmetic/display/icon keys
 * `CarrierConfigManager` also exposes. Read-only: this never writes carrier config, and nothing
 * else in the app depends on the result (see [CarrierConfigSnapshot]).
 */
object CarrierConfigReader {

    fun read(
        context: Context,
        subscriptionId: Int = MonitoringSettings.DEFAULT_SUBSCRIPTION_ID
    ): CarrierConfigSnapshot {
        if (!CellularSignalReader.hasPhoneStatePermission(context)) {
            return CarrierConfigSnapshot(available = false)
        }
        return try {
            readInternal(context, subscriptionId)
        } catch (_: SecurityException) {
            CarrierConfigSnapshot(available = false)
        } catch (_: RuntimeException) {
            CarrierConfigSnapshot(available = false)
        }
    }

    private fun readInternal(context: Context, subscriptionId: Int): CarrierConfigSnapshot {
        val carrierConfigManager = context.getSystemService(CarrierConfigManager::class.java)
            ?: return CarrierConfigSnapshot(available = false)
        val effectiveSubId = SimSubscriptionHelper.resolveEffectiveSubscriptionId(context, subscriptionId)

        val bundle = (
            if (effectiveSubId != null) {
                carrierConfigManager.getConfigForSubId(effectiveSubId)
            } else {
                carrierConfigManager.config
            }
            ) ?: return CarrierConfigSnapshot(available = false, subscriptionId = effectiveSubId ?: -1)

        if (bundle.isEmpty) {
            return CarrierConfigSnapshot(available = false, subscriptionId = effectiveSubId ?: -1)
        }

        val baseEntries = KEY_DEFINITIONS.map { definition ->
            CarrierConfigEntry(
                category = definition.category,
                label = definition.label,
                key = definition.key,
                value = definition.format(bundle)
            )
        }
        val entries = insertNrStandaloneAllowedSummary(baseEntries, bundle)
        val carrierName = SimSubscriptionHelper.resolveCarrierName(context, subscriptionId)

        return CarrierConfigSnapshot(
            available = true,
            subscriptionId = effectiveSubId ?: -1,
            carrierName = carrierName,
            entries = entries
        )
    }

    /**
     * Inserts a plain-English "5G SA allowed by SIM" summary row right before the raw
     * `carrier_nr_availabilities_int_array` row, derived from whether that array contains
     * [CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA]. This answers "will the SIM allow the UE
     * to work on 5G SA?" directly — a `"No"` here means SA is hard-disabled by carrier config,
     * regardless of device modem capability or network coverage. A `"Yes"` means carrier config
     * *permits* SA; actually seeing SA in the field additionally requires device/modem support
     * and SA coverage from the network.
     */
    private fun insertNrStandaloneAllowedSummary(
        entries: List<CarrierConfigEntry>,
        bundle: PersistableBundle
    ): List<CarrierConfigEntry> {
        val nrAvailabilities = bundle.getIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY)
            ?: intArrayOf()
        val standaloneAllowed = nrAvailabilities.contains(CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA)
        val summaryEntry = CarrierConfigEntry(
            category = "NR/5G availability",
            label = "5G SA allowed by SIM",
            key = "derived from carrier_nr_availabilities_int_array",
            value = if (standaloneAllowed) "Yes" else "No"
        )

        return entries.flatMap { entry ->
            if (entry.key == CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY) {
                listOf(summaryEntry, entry)
            } else {
                listOf(entry)
            }
        }
    }

    private class KeyDefinition(
        val category: String,
        val label: String,
        val key: String,
        val format: (PersistableBundle) -> String
    )

    private fun boolDef(category: String, label: String, key: String) =
        KeyDefinition(category, label, key) { bundle -> bundle.getBoolean(key).toString() }

    private fun intDef(
        category: String,
        label: String,
        key: String,
        decode: (Int) -> String? = { null }
    ) = KeyDefinition(category, label, key) { bundle ->
        val raw = bundle.getInt(key)
        decode(raw)?.let { "$raw ($it)" } ?: raw.toString()
    }

    private fun intArrayDef(
        category: String,
        label: String,
        key: String,
        decode: (Int) -> String? = { null }
    ) = KeyDefinition(category, label, key) { bundle ->
        val raw = bundle.getIntArray(key) ?: intArrayOf()
        raw.joinToString(prefix = "[", postfix = "]") { value ->
            decode(value)?.let { "$value ($it)" } ?: value.toString()
        }
    }

    private fun wfcModeLabel(value: Int): String? = when (value) {
        0 -> "Wi-Fi only"
        1 -> "prefer mobile"
        2 -> "prefer Wi-Fi"
        else -> null
    }

    private fun nrAvailabilityLabel(value: Int): String? = when (value) {
        CarrierConfigManager.CARRIER_NR_AVAILABILITY_NSA -> "NSA"
        CarrierConfigManager.CARRIER_NR_AVAILABILITY_SA -> "SA"
        else -> null
    }

    private val KEY_DEFINITIONS: List<KeyDefinition> = listOf(
        // VoLTE
        boolDef("VoLTE", "VoLTE available", CarrierConfigManager.KEY_CARRIER_VOLTE_AVAILABLE_BOOL),
        boolDef(
            "VoLTE",
            "VoLTE provisioning required",
            CarrierConfigManager.KEY_CARRIER_VOLTE_PROVISIONING_REQUIRED_BOOL
        ),
        boolDef(
            "VoLTE",
            "VoLTE provisioning also gates WiFi calling",
            CarrierConfigManager.KEY_CARRIER_VOLTE_OVERRIDE_WFC_PROVISIONING_BOOL
        ),
        intDef(
            "VoLTE",
            "RAT reported during a VoLTE call",
            CarrierConfigManager.KEY_VOLTE_REPLACEMENT_RAT_INT
        ),
        // KEY_SUPPORT_DOWNGRADE_VT_TO_AUDIO_BOOL is @hide (not part of the public SDK), so the
        // key name is passed as a raw string. The underlying config value is still readable via
        // getConfigForSubId() for any app holding READ_PHONE_STATE.
        boolDef(
            "VoLTE",
            "Video call can downgrade to audio",
            "support_downgrade_vt_to_audio_bool"
        ),
        // WiFi calling (VoWiFi)
        boolDef("WiFi calling", "WFC available", CarrierConfigManager.KEY_CARRIER_WFC_IMS_AVAILABLE_BOOL),
        boolDef(
            "WiFi calling",
            "WFC supports Wi-Fi-only mode",
            CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL
        ),
        boolDef(
            "WiFi calling",
            "WFC enabled by default",
            CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL
        ),
        intDef(
            "WiFi calling",
            "WFC default mode (home)",
            CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_MODE_INT,
            ::wfcModeLabel
        ),
        // KEY_CARRIER_DEFAULT_WFC_IMS_ROAMING_ENABLED_BOOL is @hide; raw string key used (see note above).
        boolDef(
            "WiFi calling",
            "WFC roaming enabled by default",
            "carrier_default_wfc_ims_roaming_enabled_bool"
        ),
        intDef(
            "WiFi calling",
            "WFC default mode (roaming)",
            CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ROAMING_MODE_INT,
            ::wfcModeLabel
        ),
        // VoNR
        boolDef("VoNR", "VoNR enabled", CarrierConfigManager.KEY_VONR_ENABLED_BOOL),
        // NR/5G availability (actual attach behavior, not display)
        intArrayDef(
            "NR/5G availability",
            "Carrier NR availabilities",
            CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
            ::nrAvailabilityLabel
        ),
        // Network selection / roaming
        boolDef(
            "Network selection",
            "Manual selection restricted to home network",
            CarrierConfigManager.KEY_ONLY_AUTO_SELECT_IN_HOME_NETWORK_BOOL
        ),
        // Emergency
        boolDef(
            "Emergency",
            "Emergency SMS over IMS supported",
            CarrierConfigManager.KEY_SUPPORT_EMERGENCY_SMS_OVER_IMS_BOOL
        ),
        // KEY_USE_ONLY_DIALED_SIM_ECC_LIST_BOOL is @hide; raw string key used (see note above).
        boolDef(
            "Emergency",
            "Emergency numbers limited to dialed SIM",
            "use_only_dialed_sim_ecc_list_bool"
        )
    )
}
