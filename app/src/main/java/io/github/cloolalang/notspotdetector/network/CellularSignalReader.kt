package io.github.cloolalang.notspotdetector.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.AccessNetworkConstants
import android.telephony.CellIdentity
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.NetworkRegistrationInfo
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import io.github.cloolalang.notspotdetector.model.CellularRadioMetrics
import io.github.cloolalang.notspotdetector.model.DetectedLteCell
import io.github.cloolalang.notspotdetector.model.LteLayerResilience
import io.github.cloolalang.notspotdetector.model.LteLayerResilienceReading
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.NetworkModePreference
import io.github.cloolalang.notspotdetector.model.NetworkServiceMode
import io.github.cloolalang.notspotdetector.model.isNoCellularService
import io.github.cloolalang.notspotdetector.model.isRadioPoweredOff
import io.github.cloolalang.notspotdetector.model.readNetworkModePreference
import io.github.cloolalang.notspotdetector.model.isVoiceOnlyNoData
import io.github.cloolalang.notspotdetector.model.SinrMetric
import io.github.cloolalang.notspotdetector.model.resolveNetworkServiceMode
import io.github.cloolalang.notspotdetector.model.resolveServingOperatorFromCell

object CellularSignalReader {

    const val RADIO_2G = "2G"
    const val RADIO_4G = "4G"
    const val RADIO_5G = "5G"
    const val RADIO_5G_ENDC = "5G EN-DC"

    fun read(
        context: Context,
        monitor2gFallback: Boolean = false,
        subscriptionId: Int = MonitoringSettings.DEFAULT_SUBSCRIPTION_ID
    ): CellularRadioMetrics {
        if (!hasPhoneStatePermission(context)) {
            return CellularRadioMetrics(permissionGranted = false)
        }

        return try {
            readInternal(context, monitor2gFallback, subscriptionId)
        } catch (_: SecurityException) {
            CellularRadioMetrics(permissionGranted = true)
        } catch (_: RuntimeException) {
            CellularRadioMetrics(permissionGranted = true)
        }
    }

    private fun readInternal(
        context: Context,
        monitor2gFallback: Boolean,
        subscriptionId: Int
    ): CellularRadioMetrics {
        val telephonyManager = SimSubscriptionHelper.telephonyManagerFor(context, subscriptionId)
            ?: return CellularRadioMetrics(permissionGranted = true)

        val cellIdentityPermissionGranted = hasCellIdentityPermission(context)
        val operatorInfo = readOperatorInfo(context, telephonyManager, subscriptionId)
        val plmn = operatorInfo.servingPlmn
        val networkReports2g = isServing2gNetwork(telephonyManager)
        val networkModePreference = readNetworkModePreference(telephonyManager, context)
        val restrictedTo2gNetwork = networkModePreference == NetworkModePreference.FORCED_2G
        val serviceState = telephonyManager.serviceState
        val networkServiceMode = readNetworkServiceMode(telephonyManager, serviceState)
        val packetSwitchedRegistered = hasPacketSwitchedRegistration(
            serviceState,
            telephonyManager
        )
        val isWifiCallingActive = readWifiCallingActive(telephonyManager)
        val simSlotIndex = SimSubscriptionHelper.resolveSlotIndex(context, subscriptionId)
        val simDisplayName = SimSubscriptionHelper.resolveSubscriptionLabel(context, subscriptionId)
        val hasLimitedServiceOnAnySim = hasLimitedServiceOnAnySubscription(context)
        val isDualSimActive = SimSubscriptionHelper.listActiveSubscriptions(context).size > 1
        val radioOff = networkServiceMode.isRadioPoweredOff()
        val isLimitedService = networkServiceMode == NetworkServiceMode.LIMITED_SERVICE
        val hasHomeGsmSignal = hasHomeGsmSignal(
            signalStrength = telephonyManager.signalStrength,
            monitor2gFallback = monitor2gFallback
        )
        val signalMetrics = readSignalStrength(
            signalStrength = telephonyManager.signalStrength,
            monitor2gFallback = monitor2gFallback,
            isOn2g = networkReports2g
        )
        val servingCell = if (radioOff || networkServiceMode == NetworkServiceMode.OUT_OF_SERVICE) {
            ServingCellIdentity()
        } else {
            readServingCellForSignal(
                telephonyManager = telephonyManager,
                cellIdentityPermissionGranted = cellIdentityPermissionGranted,
                monitor2gFallback = monitor2gFallback,
                expectedPlmn = plmn,
                homePlmn = operatorInfo.homePlmn,
                signalMetrics = signalMetrics,
                networkReports2g = networkReports2g,
                isDualSimActive = isDualSimActive,
                isLimitedService = isLimitedService ||
                    networkServiceMode == NetworkServiceMode.OUT_OF_SERVICE,
                registeredOnly = networkServiceMode == NetworkServiceMode.OUT_OF_SERVICE,
                registeredKeys = readRegisteredServingKeys(serviceState),
                signalLtePcis = readSignalLtePcis(telephonyManager.signalStrength),
                signalNrPcis = readSignalNrPcis(telephonyManager.signalStrength)
            )
        }
        val isOn2g = when {
            servingCell.radioAccessType in LTE_NR_RADIO_TYPES -> false
            servingCell.radioAccessType == RADIO_2G -> true
            else -> isEffectivelyOn2g(signalMetrics, networkReports2g)
        }
        val servingPlmn = servingCell.servingPlmn ?: plmn
        val servingOperatorName = resolveServingOperatorFromCell(
            telephonyServingName = operatorInfo.servingOperatorName,
            homeName = operatorInfo.homeOperatorName,
            homePlmn = operatorInfo.homePlmn,
            cellServingName = servingCell.servingOperatorName,
            cellServingPlmn = servingCell.servingPlmn
        )
        val lteLayerResilience = if (radioOff) {
            null
        } else {
            readLteLayerResilience(
                telephonyManager = telephonyManager,
                cellIdentityPermissionGranted = cellIdentityPermissionGranted,
                expectedPlmns = expectedPlmns(servingPlmn, operatorInfo.homePlmn),
                isDualSimActive = isDualSimActive,
                primaryLteEarfcn = servingCell.lteEarfcn,
                primaryLtePci = servingCell.ltePci,
                acceptRegisteredPlmnMismatch = isLimitedService && servingCell.servingPlmn == null
            )
        }

        val signalMatchesServing = !servingCell.hasCampedIdentity ||
            servingCell.matchesSignalPcis(
                readSignalLtePcis(telephonyManager.signalStrength),
                readSignalNrPcis(telephonyManager.signalStrength)
            )
        var metrics = signalMetrics.copy(
            rsrpDbm = servingCell.rsrpDbm
                ?: signalMetrics.rsrpDbm.takeIf { signalMatchesServing },
            rsrqDb = servingCell.rsrqDb
                ?: signalMetrics.rsrqDb.takeIf { signalMatchesServing },
            lteSinrDb = servingCell.lteSinrDb
                ?: signalMetrics.lteSinrDb.takeIf { signalMatchesServing },
            nrSinrDb = servingCell.nrSinrDb
                ?: signalMetrics.nrSinrDb.takeIf { signalMatchesServing },
            radioAccessType = mergeRadioAccessType(
                fromSignal = signalMetrics.radioAccessType,
                fromCell = servingCell.radioAccessType
            ),
            hasLteNrSignal = signalMetrics.hasLteNrSignal ||
                servingCell.radioAccessType in LTE_NR_RADIO_TYPES,
            networkOperatorName = operatorInfo.displayOperatorName,
            homeNetworkOperatorName = operatorInfo.homeOperatorName,
            servingNetworkOperatorName = servingOperatorName ?: operatorInfo.servingOperatorName,
            plmn = servingPlmn,
            homePlmn = operatorInfo.homePlmn,
            lteEarfcn = servingCell.lteEarfcn,
            ltePci = servingCell.ltePci,
            nrEarfcn = servingCell.nrEarfcn,
            nrPci = servingCell.nrPci,
            nrBand = servingCell.nrBand,
            gsmEarfcn = servingCell.gsmEarfcn,
            gsmBsic = servingCell.gsmBsic,
            cellIdentityPermissionGranted = cellIdentityPermissionGranted,
            isOn2g = isOn2g,
            networkModePreference = networkModePreference,
            restrictedTo2gNetwork = restrictedTo2gNetwork,
            isLimitedService = isLimitedService,
            networkServiceMode = networkServiceMode,
            isVoiceOnlyNoData = isVoiceOnlyNoData(networkServiceMode, packetSwitchedRegistered),
            isWifiCallingActive = isWifiCallingActive,
            hasHomeGsmSignal = hasHomeGsmSignal,
            subscriptionId = subscriptionId.takeIf {
                it != MonitoringSettings.DEFAULT_SUBSCRIPTION_ID
            },
            simSlotIndex = simSlotIndex,
            simDisplayName = simDisplayName,
            lteLayerResilience = lteLayerResilience
        )

        if (isOn2g && !monitor2gFallback && metrics.radioAccessType !in LTE_NR_RADIO_TYPES) {
            metrics = metrics.copy(
                radioAccessType = RADIO_2G,
                rsrpDbm = null,
                rsrqDb = null,
                lteSinrDb = null,
                nrSinrDb = null,
                lteEarfcn = null,
                ltePci = null,
                nrEarfcn = null,
                nrPci = null,
                gsmEarfcn = null,
                gsmBsic = null
            )
        } else if (isOn2g && monitor2gFallback && metrics.radioAccessType == null) {
            metrics = metrics.copy(radioAccessType = RADIO_2G)
        } else if (monitor2gFallback && restrictedTo2gNetwork && metrics.radioAccessType == null) {
            metrics = metrics.copy(
                radioAccessType = RADIO_2G,
                isOn2g = true
            )
        }

        if (radioOff || networkServiceMode == NetworkServiceMode.OUT_OF_SERVICE) {
            metrics = metrics.withoutCampedRadio().copy(
                isWifiCallingActive = if (radioOff) false else metrics.isWifiCallingActive
            )
        }
        val limitedOnAnySim = hasLimitedServiceOnAnySim || metrics.isLimitedService
        metrics = metrics.copy(
            hasLimitedServiceOnAnySim = limitedOnAnySim,
            isCompleteNoService = isCompleteNoService(metrics, limitedOnAnySim)
        )

        return metrics
    }

    /**
     * Telephony can still report 2G while LTE/NR is the active data bearer after an IRAT change
     * (e.g. voice camped on GSM with [TelephonyManager.getDataNetworkType] briefly unknown).
     */
    private fun isEffectivelyOn2g(
        signalMetrics: CellularRadioMetrics,
        networkReports2g: Boolean
    ): Boolean {
        return when (signalMetrics.radioAccessType) {
            RADIO_2G -> true
            RADIO_4G, RADIO_5G, RADIO_5G_ENDC -> false
            else -> networkReports2g && !signalMetrics.hasLteNrSignal
        }
    }

    private fun shouldReadLteNrCellIdentity(
        signalMetrics: CellularRadioMetrics,
        networkReports2g: Boolean
    ): Boolean {
        if (signalMetrics.radioAccessType in LTE_NR_RADIO_TYPES) return true
        if (signalMetrics.hasLteNrSignal) return true
        if (signalMetrics.radioAccessType == RADIO_2G) return false
        return !networkReports2g
    }

    private fun shouldReadGsmCellIdentity(
        signalMetrics: CellularRadioMetrics,
        networkReports2g: Boolean,
        monitor2gFallback: Boolean
    ): Boolean {
        if (!monitor2gFallback) return false
        if (signalMetrics.radioAccessType == RADIO_2G) return true
        return networkReports2g &&
            !signalMetrics.hasLteNrSignal &&
            signalMetrics.radioAccessType !in LTE_NR_RADIO_TYPES
    }

    @SuppressLint("MissingPermission")
    private fun readServingCellForSignal(
        telephonyManager: TelephonyManager,
        cellIdentityPermissionGranted: Boolean,
        monitor2gFallback: Boolean,
        expectedPlmn: String?,
        homePlmn: String?,
        signalMetrics: CellularRadioMetrics,
        networkReports2g: Boolean,
        isDualSimActive: Boolean,
        isLimitedService: Boolean,
        registeredOnly: Boolean = false,
        registeredKeys: RegisteredServingKeys = RegisteredServingKeys(),
        signalLtePcis: Set<Int> = emptySet(),
        signalNrPcis: Set<Int> = emptySet()
    ): ServingCellIdentity {
        if (!cellIdentityPermissionGranted) return ServingCellIdentity()

        val expectedPlmns = expectedPlmns(expectedPlmn, homePlmn)
        return when {
            isLimitedService || registeredOnly ->
                readServingCellIdentities(
                    telephonyManager = telephonyManager,
                    monitor2gFallback = monitor2gFallback,
                    expectedPlmns = expectedPlmns,
                    isDualSimActive = isDualSimActive,
                    acceptRegisteredPlmnMismatch = true,
                    registeredOnly = registeredOnly,
                    registeredKeys = registeredKeys,
                    signalLtePcis = signalLtePcis,
                    signalNrPcis = signalNrPcis
                )
            shouldReadLteNrCellIdentity(signalMetrics, networkReports2g) ->
                readServingCellIdentities(
                    telephonyManager = telephonyManager,
                    monitor2gFallback = false,
                    expectedPlmns = expectedPlmns,
                    isDualSimActive = isDualSimActive,
                    acceptRegisteredPlmnMismatch = false,
                    registeredOnly = registeredOnly,
                    registeredKeys = registeredKeys,
                    signalLtePcis = signalLtePcis,
                    signalNrPcis = signalNrPcis
                )
            shouldReadGsmCellIdentity(signalMetrics, networkReports2g, monitor2gFallback) ->
                readServingCellIdentities(
                    telephonyManager = telephonyManager,
                    monitor2gFallback = true,
                    expectedPlmns = expectedPlmns,
                    isDualSimActive = isDualSimActive,
                    acceptRegisteredPlmnMismatch = false,
                    registeredOnly = registeredOnly,
                    registeredKeys = registeredKeys,
                    signalLtePcis = signalLtePcis,
                    signalNrPcis = signalNrPcis
                )
            else -> ServingCellIdentity()
        }
    }

    private fun mergeRadioAccessType(fromSignal: String?, fromCell: String?): String? {
        if (fromCell in LTE_NR_RADIO_TYPES && (fromSignal == null || fromSignal == RADIO_2G)) {
            return fromCell
        }
        return fromSignal ?: fromCell
    }

    private fun expectedPlmns(servingPlmn: String?, homePlmn: String?): List<String> {
        return listOfNotNull(
            servingPlmn?.trim()?.takeIf { it.length >= 5 },
            homePlmn?.trim()?.takeIf { it.length >= 5 }
        ).distinct()
    }

    @SuppressLint("MissingPermission")
    fun hasLimitedServiceOnAnySubscription(context: Context): Boolean {
        if (!hasPhoneStatePermission(context)) {
            return false
        }

        val subscriptions = SimSubscriptionHelper.listActiveSubscriptions(context)
        if (subscriptions.isEmpty()) {
            val telephonyManager = context.getSystemService(TelephonyManager::class.java)
                ?: return false
            return readNetworkServiceMode(telephonyManager) == NetworkServiceMode.LIMITED_SERVICE
        }

        return subscriptions.any { subscription ->
            val telephonyManager = SimSubscriptionHelper.telephonyManagerFor(
                context,
                subscription.subscriptionId
            ) ?: return@any false
            readNetworkServiceMode(telephonyManager) == NetworkServiceMode.LIMITED_SERVICE
        }
    }

    private fun isCompleteNoService(
        metrics: CellularRadioMetrics,
        hasLimitedServiceOnAnySim: Boolean
    ): Boolean {
        if (hasLimitedServiceOnAnySim) return false
        if (!metrics.permissionGranted) return false
        if (!metrics.networkServiceMode.isNoCellularService()) return false
        if (metrics.isOn2g) return false
        if (metrics.radioAccessType != null || metrics.rsrpDbm != null || metrics.rsrqDb != null) {
            return false
        }
        return true
    }

    private fun hasHomeGsmSignal(
        signalStrength: SignalStrength?,
        monitor2gFallback: Boolean
    ): Boolean {
        if (!monitor2gFallback || signalStrength == null) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (strength in signalStrength.cellSignalStrengths) {
                if (strength is CellSignalStrengthGsm && isValidMetric(strength.dbm)) {
                    return true
                }
            }
        } else {
            val dbm = gsmDbmFromLegacy(signalStrength)
            if (dbm != null) {
                return true
            }
        }

        return false
    }

    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCellIdentityPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fineGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return fineGranted || coarseGranted
        }
        return hasPhoneStatePermission(context)
    }

    private data class OperatorInfo(
        val homeOperatorName: String?,
        val servingOperatorName: String?,
        val homePlmn: String?,
        val servingPlmn: String?,
        val displayOperatorName: String?
    )

    @SuppressLint("MissingPermission")
    private fun readOperatorInfo(
        context: Context,
        telephonyManager: TelephonyManager,
        subscriptionId: Int
    ): OperatorInfo {
        val homeOperatorName = normalizeOperatorName(telephonyManager.simOperatorName)
            ?: SimSubscriptionHelper.resolveCarrierName(context, subscriptionId)
        val servingOperatorName = normalizeOperatorName(telephonyManager.networkOperatorName)
        val homePlmn = telephonyManager.simOperator
            .takeIf { it.isNotBlank() && it.length >= 5 }
        val servingPlmn = telephonyManager.networkOperator
            .takeIf { it.isNotBlank() && it.length >= 5 }
        val displayOperatorName = servingOperatorName ?: homeOperatorName
        return OperatorInfo(
            homeOperatorName = homeOperatorName,
            servingOperatorName = servingOperatorName,
            homePlmn = homePlmn,
            servingPlmn = servingPlmn,
            displayOperatorName = displayOperatorName
        )
    }

    private fun normalizeOperatorName(raw: String?): String? {
        return raw?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    @SuppressLint("MissingPermission")
    private fun readNetworkServiceMode(telephonyManager: TelephonyManager): NetworkServiceMode {
        return readNetworkServiceMode(telephonyManager, telephonyManager.serviceState)
    }

    @SuppressLint("MissingPermission")
    private fun readNetworkServiceMode(
        telephonyManager: TelephonyManager,
        serviceState: ServiceState?
    ): NetworkServiceMode {
        if (serviceState == null) return NetworkServiceMode.UNKNOWN
        return resolveNetworkServiceMode(
            serviceState = serviceState.state,
            circuitSwitchedRegistered = hasCircuitSwitchedRegistration(
                serviceState,
                telephonyManager
            ),
            emergencyCamp = hasEmergencyVoiceCamp(serviceState)
        )
    }

    /**
     * Packet-switched / data on WWAN. WLAN (WiFi calling) does not count as cellular data.
     */
    private fun hasPacketSwitchedRegistration(
        serviceState: ServiceState?,
        telephonyManager: TelephonyManager
    ): Boolean {
        if (serviceState != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val fromRegistration = runCatching {
                serviceState.networkRegistrationInfoList.any { info ->
                    info.transportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN &&
                        info.isPacketSwitchedDomain() &&
                        @Suppress("DEPRECATION") info.isRegistered
                }
            }.getOrDefault(false)
            if (fromRegistration) {
                return true
            }
            return false
        }
        val dataType = telephonyManager.dataNetworkType
        return dataType != TelephonyManager.NETWORK_TYPE_UNKNOWN &&
            dataType != TelephonyManager.NETWORK_TYPE_IWLAN
    }

    private fun NetworkRegistrationInfo.isPacketSwitchedDomain(): Boolean {
        return domain == NetworkRegistrationInfo.DOMAIN_PS ||
            domain == NetworkRegistrationInfo.DOMAIN_CS_PS
    }

    /**
     * Voice/CS or GERAN camp is what the status-bar service icon follows. PS-only leftover
     * LTE (common after a 4G RF cut) must not count.
     */
    private fun hasCircuitSwitchedRegistration(
        serviceState: ServiceState,
        telephonyManager: TelephonyManager
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val fromRegistration = runCatching {
                serviceState.networkRegistrationInfoList.any { info ->
                    info.transportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN &&
                        info.isVoiceOrGeranCamp() &&
                        @Suppress("DEPRECATION") info.isRegistered
                }
            }.getOrDefault(false)
            if (fromRegistration) {
                return true
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return false
        }
        return telephonyManager.voiceNetworkType in TWO_G_NETWORK_TYPES
    }

    private fun hasEmergencyVoiceCamp(serviceState: ServiceState): Boolean {
        if (readEnhancedLimitedServiceState(serviceState)) {
            return true
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false
        }
        return runCatching {
            serviceState.networkRegistrationInfoList.any { info ->
                info.transportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN &&
                    info.isVoiceOrGeranCamp() &&
                    invokeBooleanMethod(info, "isEmergencyEnabled")
            }
        }.getOrDefault(false)
    }

    private fun NetworkRegistrationInfo.isVoiceOrGeranCamp(): Boolean {
        return isCircuitSwitchedDomain() || accessNetworkTechnology in TWO_G_NETWORK_TYPES
    }

    private fun NetworkRegistrationInfo.isCircuitSwitchedDomain(): Boolean {
        return domain == NetworkRegistrationInfo.DOMAIN_CS ||
            domain == NetworkRegistrationInfo.DOMAIN_CS_PS
    }

    /**
     * API 30+ exposes registration states that can indicate emergency-only service even when
     * [ServiceState.getState] has not yet moved to [ServiceState.STATE_EMERGENCY_ONLY].
     */
    private fun readEnhancedLimitedServiceState(serviceState: ServiceState): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            invokeBooleanMethod(serviceState, "isEmergencyOnly")
        ) {
            return true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val notRegEmergency = runCatching {
                ServiceState::class.java.getField("REGISTRATION_STATE_NOT_REG_EMERGENCY").getInt(null)
            }.getOrNull() ?: return false
            val dataReg = invokeIntMethod(serviceState, "getDataRegistrationState")
            val voiceReg = invokeIntMethod(serviceState, "getVoiceRegistrationState")
            if (dataReg == notRegEmergency || voiceReg == notRegEmergency) {
                return true
            }
        }

        return false
    }

    /**
     * True when the modem is registered for service over a WLAN transport (WiFi calling / VoWiFi)
     * per [ServiceState.getNetworkRegistrationInfoList] — a public API added in API 30. Uses
     * [NetworkRegistrationInfo.isRegistered] rather than the registration-state getter/constants
     * (`getRegistrationState()`, `REGISTRATION_STATE_HOME`), which are `@SystemApi`-restricted and
     * unavailable to third-party apps. Falls back to the legacy public
     * [TelephonyManager.getDataNetworkType] IWLAN check below API 30 (less precise, OEM variable
     * — [ServiceState.getDataNetworkType] is `@SystemApi`-restricted, unlike the `TelephonyManager`
     * equivalent). See `RXSS_CATALOGUE.md` RXSS 31.
     */
    private fun readWifiCallingActive(telephonyManager: TelephonyManager): Boolean {
        val serviceState = telephonyManager.serviceState ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return runCatching {
                serviceState.networkRegistrationInfoList.any { info ->
                    info.transportType == AccessNetworkConstants.TRANSPORT_TYPE_WLAN &&
                        @Suppress("DEPRECATION") info.isRegistered
                }
            }.getOrDefault(false)
        }
        @Suppress("DEPRECATION")
        return runCatching {
            telephonyManager.dataNetworkType == TelephonyManager.NETWORK_TYPE_IWLAN
        }.getOrDefault(false)
    }

    private fun invokeBooleanMethod(target: Any, methodName: String): Boolean {
        return runCatching {
            target.javaClass.getMethod(methodName).invoke(target) as Boolean
        }.getOrDefault(false)
    }

    private fun invokeIntMethod(target: Any, methodName: String): Int {
        return runCatching {
            target.javaClass.getMethod(methodName).invoke(target) as Int
        }.getOrDefault(Int.MIN_VALUE)
    }

    @SuppressLint("MissingPermission")
    private fun isServing2gNetwork(telephonyManager: TelephonyManager): Boolean {
        val dataNetworkType = telephonyManager.dataNetworkType
        if (dataNetworkType in TWO_G_NETWORK_TYPES) {
            return true
        }
        val voiceNetworkType = telephonyManager.voiceNetworkType
        return voiceNetworkType in TWO_G_NETWORK_TYPES &&
            dataNetworkType == TelephonyManager.NETWORK_TYPE_UNKNOWN
    }

    @SuppressLint("MissingPermission")
    private fun readSignalStrength(
        signalStrength: SignalStrength?,
        monitor2gFallback: Boolean,
        isOn2g: Boolean
    ): CellularRadioMetrics {
        if (signalStrength == null) {
            return CellularRadioMetrics(permissionGranted = true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var lteMetrics: CellularRadioMetrics? = null
            var nrMetrics: CellularRadioMetrics? = null
            var gsmMetrics: CellularRadioMetrics? = null
            var hasLteNrSignal = false

            for (strength in signalStrength.cellSignalStrengths) {
                when (strength) {
                    is CellSignalStrengthNr -> {
                        val rsrp = strength.ssRsrp
                        val rsrq = strength.ssRsrq
                        val sinr = SinrMetric.takeNrSsSinr(strength.ssSinr)
                        if (isValidMetric(rsrp) || isValidMetric(rsrq) || sinr != null) {
                            hasLteNrSignal = true
                            nrMetrics = CellularRadioMetrics(
                                rsrpDbm = rsrp.takeIf { isValidMetric(it) },
                                rsrqDb = rsrq.takeIf { isValidMetric(it) },
                                nrSinrDb = sinr,
                                permissionGranted = true
                            )
                        }
                    }
                    is CellSignalStrengthLte -> {
                        val rsrp = strength.rsrp
                        val rsrq = strength.rsrq
                        val sinr = SinrMetric.takeLteRssnr(strength.rssnr)
                        if (isValidMetric(rsrp) || isValidMetric(rsrq) || sinr != null) {
                            hasLteNrSignal = true
                            lteMetrics = CellularRadioMetrics(
                                rsrpDbm = rsrp.takeIf { isValidMetric(it) },
                                rsrqDb = rsrq.takeIf { isValidMetric(it) },
                                lteSinrDb = sinr,
                                permissionGranted = true
                            )
                        }
                    }
                    is CellSignalStrengthGsm -> {
                        if (monitor2gFallback && isOn2g) {
                            val dbm = strength.dbm
                            if (isValidMetric(dbm)) {
                                gsmMetrics = CellularRadioMetrics(
                                    rsrpDbm = dbm,
                                    permissionGranted = true
                                )
                            }
                        }
                    }
                }
            }

            val serving = when {
                lteMetrics != null && nrMetrics != null ->
                    lteMetrics.copy(
                        radioAccessType = RADIO_5G_ENDC,
                        nrSinrDb = nrMetrics.nrSinrDb
                    )
                nrMetrics != null ->
                    nrMetrics.copy(radioAccessType = RADIO_5G)
                lteMetrics != null ->
                    lteMetrics.copy(radioAccessType = RADIO_4G)
                gsmMetrics != null ->
                    gsmMetrics.copy(radioAccessType = RADIO_2G)
                else -> CellularRadioMetrics(permissionGranted = true)
            }
            return serving.copy(hasLteNrSignal = hasLteNrSignal)
        } else if (monitor2gFallback && isOn2g) {
            val dbm = gsmDbmFromLegacy(signalStrength)
            if (dbm != null) {
                return CellularRadioMetrics(
                    rsrpDbm = dbm,
                    radioAccessType = RADIO_2G,
                    permissionGranted = true,
                    hasLteNrSignal = false
                )
            }
        }

        return CellularRadioMetrics(permissionGranted = true)
    }

    private fun gsmDbmFromLegacy(signalStrength: SignalStrength): Int? {
        val asu = signalStrength.gsmSignalStrength
        if (asu == 99 || asu == CellInfo.UNAVAILABLE) {
            return null
        }
        return -113 + 2 * asu
    }

    /**
     * "4G layers detected" — splits currently visible LTE frequency layers (grouped by EARFCN)
     * into the primary channel (the one matching [primaryLteEarfcn], i.e. the serving/anchor
     * layer this device is actually camped on) and every other, "alternate", layer:
     *
     * - `primaryLayerCellCount`: number of detected cells (sectors) sharing the primary EARFCN.
     * - `alternateLayerCount`: number of distinct *other* EARFCNs detected.
     * - `alternateLayerCellCount`: total cells across all of those alternate EARFCNs combined.
     * - `primaryLayerDominanceDb`: RSRP gap (dB) between the primary/serving sector and the
     *   next-strongest *other* sector on that same primary EARFCN.
     *
     * Deliberately uses whatever the modem/UE already reports with no additional signal-quality
     * floor. Neighbours with a valid PCI still count when EARFCN or PLMN is blank — Android
     * often omits both on intra-frequency sectors. See [LteLayerResilience].
     *
     * Idle-mode *inter-frequency* visibility is still limited by 3GPP TS 36.304 — a strong
     * serving cell typically suppresses other EARFCNs (UE battery-save). This reading is
     * currently detected layers/cells, not an exhaustive survey.
     *
     * Returns null when the cell-identity permission isn't granted or the read fails.
     */
    @SuppressLint("MissingPermission")
    @Suppress("UNUSED_PARAMETER")
    private fun readLteLayerResilience(
        telephonyManager: TelephonyManager,
        cellIdentityPermissionGranted: Boolean,
        expectedPlmns: Collection<String>,
        isDualSimActive: Boolean,
        primaryLteEarfcn: Int?,
        primaryLtePci: Int?,
        acceptRegisteredPlmnMismatch: Boolean
    ): LteLayerResilienceReading? {
        if (!cellIdentityPermissionGranted) return null

        return try {
            val cellInfoList = telephonyManager.allCellInfo ?: return null
            val detected = cellInfoList
                .filterIsInstance<CellInfoLte>()
                .filter { info ->
                    LteLayerResilience.shouldCountNeighbour(
                        isPlmnMismatch = plmnMatchAny(
                            info.cellIdentity,
                            expectedPlmns
                        ) == PlmnMatchStatus.MISMATCH,
                        isRegistered = info.isRegistered,
                        acceptRegisteredPlmnMismatch = acceptRegisteredPlmnMismatch
                    )
                }
                .map { info ->
                    DetectedLteCell(
                        earfcn = info.cellIdentity.earfcn.takeIf { isValidCellIdentityValue(it) },
                        pci = info.cellIdentity.pci.takeIf { isValidCellIdentityValue(it) },
                        rsrpDbm = info.cellSignalStrength.rsrp.takeIf { isValidMetric(it) },
                        isRegistered = info.isRegistered
                    )
                }

            LteLayerResilience.fromDetectedCells(
                cells = detected,
                primaryEarfcn = primaryLteEarfcn,
                primaryPci = primaryLtePci
            )
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun readServingCellIdentities(
        telephonyManager: TelephonyManager,
        monitor2gFallback: Boolean,
        expectedPlmns: Collection<String>,
        isDualSimActive: Boolean,
        acceptRegisteredPlmnMismatch: Boolean,
        registeredOnly: Boolean = false,
        registeredKeys: RegisteredServingKeys = RegisteredServingKeys(),
        signalLtePcis: Set<Int> = emptySet(),
        signalNrPcis: Set<Int> = emptySet()
    ): ServingCellIdentity {
        val first = readServingCellIdentitiesOnce(
            telephonyManager = telephonyManager,
            monitor2gFallback = monitor2gFallback,
            expectedPlmns = expectedPlmns,
            isDualSimActive = isDualSimActive,
            acceptRegisteredPlmnMismatch = acceptRegisteredPlmnMismatch,
            registeredOnly = registeredOnly,
            registeredKeys = registeredKeys,
            signalLtePcis = signalLtePcis,
            signalNrPcis = signalNrPcis
        )
        if (!first.hasCampedIdentity) return first
        val second = readServingCellIdentitiesOnce(
            telephonyManager = telephonyManager,
            monitor2gFallback = monitor2gFallback,
            expectedPlmns = expectedPlmns,
            isDualSimActive = isDualSimActive,
            acceptRegisteredPlmnMismatch = acceptRegisteredPlmnMismatch,
            registeredOnly = registeredOnly,
            registeredKeys = registeredKeys,
            signalLtePcis = signalLtePcis,
            signalNrPcis = signalNrPcis
        )
        return when {
            second.hasCampedIdentity && first.sameServingKeysAs(second) -> second
            !second.hasCampedIdentity -> first
            else -> ServingCellIdentity()
        }
    }

    @SuppressLint("MissingPermission")
    private fun readServingCellIdentitiesOnce(
        telephonyManager: TelephonyManager,
        monitor2gFallback: Boolean,
        expectedPlmns: Collection<String>,
        isDualSimActive: Boolean,
        acceptRegisteredPlmnMismatch: Boolean,
        registeredOnly: Boolean,
        registeredKeys: RegisteredServingKeys,
        signalLtePcis: Set<Int>,
        signalNrPcis: Set<Int>
    ): ServingCellIdentity {
        return try {
            val cellInfoList = telephonyManager.allCellInfo ?: return ServingCellIdentity()
            val registered = extractServingCellIdentities(
                cellInfoList,
                registeredOnly = true,
                monitor2gFallback,
                expectedPlmns,
                isDualSimActive,
                acceptRegisteredPlmnMismatch,
                registeredKeys,
                signalLtePcis,
                signalNrPcis
            )
            if (registeredOnly) {
                return registered ?: ServingCellIdentity()
            }
            val fromAllCells = extractServingCellIdentities(
                cellInfoList,
                registeredOnly = false,
                monitor2gFallback,
                expectedPlmns,
                isDualSimActive,
                acceptRegisteredPlmnMismatch,
                registeredKeys,
                signalLtePcis,
                signalNrPcis
            )
            when {
                registered == null -> fromAllCells ?: ServingCellIdentity()
                fromAllCells == null -> registered
                else -> registered.fillGapsFrom(fromAllCells)
            }
        } catch (_: SecurityException) {
            ServingCellIdentity()
        } catch (_: RuntimeException) {
            ServingCellIdentity()
        }
    }

    private fun extractServingCellIdentities(
        cellInfoList: List<CellInfo>,
        registeredOnly: Boolean,
        monitor2gFallback: Boolean,
        expectedPlmns: Collection<String>,
        isDualSimActive: Boolean,
        acceptRegisteredPlmnMismatch: Boolean,
        registeredKeys: RegisteredServingKeys,
        signalLtePcis: Set<Int>,
        signalNrPcis: Set<Int>
    ): ServingCellIdentity? {
        var bestLte: RankedServingCell? = null
        var bestNr: RankedServingCell? = null
        var bestGsm: RankedServingCell? = null
        val hasExplicitLtePlmnMatches = hasExplicitPlmnMatchForRat(cellInfoList, expectedPlmns) { it is CellInfoLte }
        val hasExplicitNrPlmnMatches = hasExplicitPlmnMatchForRat(cellInfoList, expectedPlmns) { it is CellInfoNr }
        val hasExplicitGsmPlmnMatches = hasExplicitPlmnMatchForRat(cellInfoList, expectedPlmns) { it is CellInfoGsm }

        for (info in cellInfoList) {
            if (registeredOnly && !info.isRegistered) continue
            val connectionRank = cellConnectionRank(info)
            if (registeredOnly && connectionRank == 0) continue

            when (info) {
                is CellInfoLte -> {
                    if (!shouldUseCellIdentity(
                            identity = info.cellIdentity,
                            expectedPlmns = expectedPlmns,
                            hasExplicitPlmnMatches = hasExplicitLtePlmnMatches,
                            isDualSimActive = isDualSimActive,
                            isRegistered = info.isRegistered,
                            acceptRegisteredPlmnMismatch = acceptRegisteredPlmnMismatch
                        )
                    ) {
                        continue
                    }
                    val identity = info.cellIdentity
                    val rsrp = info.cellSignalStrength.rsrp.takeIf { isValidMetric(it) }
                    val rsrq = info.cellSignalStrength.rsrq.takeIf { isValidMetric(it) }
                    val lteSinr = SinrMetric.takeLteRssnr(info.cellSignalStrength.rssnr)
                    val earfcn = identity.earfcn.takeIf { isValidCellIdentityValue(it) }
                    val pci = identity.pci.takeIf { isValidCellIdentityValue(it) }
                    val candidate = RankedServingCell(
                        connectionRank = connectionRank,
                        plmnRank = plmnMatchAny(identity, expectedPlmns).rank,
                        matchesRegisteredKeys = registeredKeys.matchesLte(earfcn, pci),
                        pciMatchesSignal = pci != null && pci in signalLtePcis,
                        lteEarfcn = earfcn,
                        ltePci = pci,
                        rsrpDbm = rsrp,
                        rsrqDb = rsrq,
                        lteSinrDb = lteSinr,
                        servingPlmn = formatIdentityPlmn(identity),
                        servingOperatorName = readIdentityOperatorName(identity)
                    )
                    if (candidate.hasValues && (bestLte == null || candidate beats bestLte)) {
                        bestLte = candidate
                    }
                }
                is CellInfoNr -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (!shouldUseCellIdentity(
                                identity = info.cellIdentity,
                                expectedPlmns = expectedPlmns,
                                hasExplicitPlmnMatches = hasExplicitNrPlmnMatches,
                                isDualSimActive = isDualSimActive,
                                isRegistered = info.isRegistered,
                                acceptRegisteredPlmnMismatch = acceptRegisteredPlmnMismatch
                            )
                        ) {
                            continue
                        }
                        val identity = info.cellIdentity
                        if (identity is CellIdentityNr) {
                            val strength = info.cellSignalStrength
                            val rsrp = if (strength is CellSignalStrengthNr) {
                                strength.ssRsrp.takeIf { isValidMetric(it) }
                            } else {
                                null
                            }
                            val rsrq = if (strength is CellSignalStrengthNr) {
                                strength.ssRsrq.takeIf { isValidMetric(it) }
                            } else {
                                null
                            }
                            val nrSinr = if (strength is CellSignalStrengthNr) {
                                SinrMetric.takeNrSsSinr(strength.ssSinr)
                            } else {
                                null
                            }
                            val earfcn = identity.nrarfcn.takeIf { isValidCellIdentityValue(it) }
                            val pci = identity.pci.takeIf { isValidCellIdentityValue(it) }
                            val candidate = RankedServingCell(
                                connectionRank = connectionRank,
                                plmnRank = plmnMatchAny(identity, expectedPlmns).rank,
                                matchesRegisteredKeys = registeredKeys.matchesNr(earfcn, pci),
                                pciMatchesSignal = pci != null && pci in signalNrPcis,
                                nrEarfcn = earfcn,
                                nrPci = pci,
                                nrBand = readNrBand(identity),
                                rsrpDbm = rsrp,
                                rsrqDb = rsrq,
                                nrSinrDb = nrSinr,
                                servingPlmn = formatIdentityPlmn(identity),
                                servingOperatorName = readIdentityOperatorName(identity)
                            )
                            if (candidate.hasValues && (bestNr == null || candidate beats bestNr)) {
                                bestNr = candidate
                            }
                        }
                    }
                }
                is CellInfoGsm -> {
                    if (monitor2gFallback) {
                        if (!shouldUseCellIdentity(
                                identity = info.cellIdentity,
                                expectedPlmns = expectedPlmns,
                                hasExplicitPlmnMatches = hasExplicitGsmPlmnMatches,
                                isDualSimActive = isDualSimActive,
                                isRegistered = info.isRegistered,
                                acceptRegisteredPlmnMismatch = acceptRegisteredPlmnMismatch
                            )
                        ) {
                            continue
                        }
                        val identity = info.cellIdentity
                        val dbm = info.cellSignalStrength.dbm.takeIf { isValidMetric(it) }
                        val earfcn = identity.arfcn.takeIf { isValidCellIdentityValue(it) }
                        val bsic = identity.bsic.takeIf { isValidCellIdentityValue(it) }
                        val candidate = RankedServingCell(
                            connectionRank = connectionRank,
                            plmnRank = plmnMatchAny(identity, expectedPlmns).rank,
                            matchesRegisteredKeys = registeredKeys.matchesGsm(earfcn, bsic),
                            gsmEarfcn = earfcn,
                            gsmBsic = bsic,
                            rsrpDbm = dbm,
                            servingPlmn = formatIdentityPlmn(identity),
                            servingOperatorName = readIdentityOperatorName(identity)
                        )
                        if (candidate.hasValues && (bestGsm == null || candidate beats bestGsm)) {
                            bestGsm = candidate
                        }
                    }
                }
            }
        }

        return servingCellFromRanked(bestLte, bestNr, bestGsm)
    }

    private fun servingCellFromRanked(
        bestLte: RankedServingCell?,
        bestNr: RankedServingCell?,
        bestGsm: RankedServingCell?
    ): ServingCellIdentity? {
        val radioAccessType = when {
            bestLte != null && bestNr != null -> RADIO_5G_ENDC
            bestNr != null -> RADIO_5G
            bestLte != null -> RADIO_4G
            bestGsm != null -> RADIO_2G
            else -> null
        }
        val primary = when (radioAccessType) {
            RADIO_5G_ENDC, RADIO_4G -> bestLte
            RADIO_5G -> bestNr
            RADIO_2G -> bestGsm
            else -> bestLte ?: bestNr ?: bestGsm
        }
        val lteEarfcn = bestLte?.lteEarfcn
        val ltePci = bestLte?.ltePci
        val nrEarfcn = bestNr?.nrEarfcn
        val nrPci = bestNr?.nrPci
        val nrBand = bestNr?.nrBand
        val gsmEarfcn = bestGsm?.gsmEarfcn
        val gsmBsic = bestGsm?.gsmBsic

        if (lteEarfcn == null && ltePci == null && nrEarfcn == null && nrPci == null &&
            gsmEarfcn == null && gsmBsic == null
        ) {
            return null
        }
        return ServingCellIdentity(
            lteEarfcn = lteEarfcn,
            ltePci = ltePci,
            nrEarfcn = nrEarfcn,
            nrPci = nrPci,
            nrBand = nrBand,
            gsmEarfcn = gsmEarfcn,
            gsmBsic = gsmBsic,
            rsrpDbm = primary?.rsrpDbm,
            rsrqDb = primary?.rsrqDb,
            lteSinrDb = bestLte?.lteSinrDb,
            nrSinrDb = bestNr?.nrSinrDb,
            radioAccessType = radioAccessType,
            servingPlmn = primary?.servingPlmn,
            servingOperatorName = primary?.servingOperatorName
        )
    }

    /**
     * Reads the serving NR operating band directly from the modem via
     * [CellIdentityNr.getBands] (API 30+), rather than deriving it from the NR-ARFCN — NR-ARFCN
     * channel ranges overlap across multiple bands (e.g. n1/n66), so only the modem-reported band
     * is unambiguous. Returns the first reported band, or null below API 30 / when unavailable.
     */
    private fun readNrBand(identity: CellIdentityNr): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching { identity.bands.firstOrNull() }.getOrNull()
    }

    private fun hasExplicitPlmnMatchForRat(
        cellInfoList: List<CellInfo>,
        expectedPlmns: Collection<String>,
        ratFilter: (CellInfo) -> Boolean
    ): Boolean {
        return cellInfoList.any { info ->
            ratFilter(info) &&
                plmnMatchAny(info.cellIdentity, expectedPlmns) == PlmnMatchStatus.MATCH
        }
    }

    private enum class PlmnMatchStatus(val rank: Int) {
        MATCH(2),
        UNKNOWN(1),
        MISMATCH(0)
    }

    private data class RegisteredServingKeys(
        val lteEarfcn: Int? = null,
        val ltePci: Int? = null,
        val nrEarfcn: Int? = null,
        val nrPci: Int? = null,
        val gsmEarfcn: Int? = null,
        val gsmBsic: Int? = null
    ) {
        fun matchesLte(earfcn: Int?, pci: Int?): Boolean {
            if (ltePci != null && pci != null) return ltePci == pci
            if (lteEarfcn != null && earfcn != null) return lteEarfcn == earfcn
            return false
        }

        fun matchesNr(earfcn: Int?, pci: Int?): Boolean {
            if (nrPci != null && pci != null) return nrPci == pci
            if (nrEarfcn != null && earfcn != null) return nrEarfcn == earfcn
            return false
        }

        fun matchesGsm(earfcn: Int?, bsic: Int?): Boolean {
            if (gsmBsic != null && bsic != null) return gsmBsic == bsic
            if (gsmEarfcn != null && earfcn != null) return gsmEarfcn == earfcn
            return false
        }
    }

    private data class RankedServingCell(
        val connectionRank: Int,
        val plmnRank: Int = 0,
        val matchesRegisteredKeys: Boolean = false,
        val pciMatchesSignal: Boolean = false,
        val lteEarfcn: Int? = null,
        val ltePci: Int? = null,
        val nrEarfcn: Int? = null,
        val nrPci: Int? = null,
        val nrBand: Int? = null,
        val gsmEarfcn: Int? = null,
        val gsmBsic: Int? = null,
        val rsrpDbm: Int? = null,
        val rsrqDb: Int? = null,
        val lteSinrDb: Int? = null,
        val nrSinrDb: Int? = null,
        val servingPlmn: String? = null,
        val servingOperatorName: String? = null
    ) {
        val hasValues: Boolean
            get() = lteEarfcn != null || ltePci != null || nrEarfcn != null || nrPci != null ||
                gsmEarfcn != null || gsmBsic != null

        infix fun beats(other: RankedServingCell): Boolean {
            if (plmnRank != other.plmnRank) return plmnRank > other.plmnRank
            if (matchesRegisteredKeys != other.matchesRegisteredKeys) return matchesRegisteredKeys
            if (connectionRank != other.connectionRank) return connectionRank > other.connectionRank
            if (pciMatchesSignal != other.pciMatchesSignal) return pciMatchesSignal
            return false
        }
    }

    /**
     * Decides whether a candidate [CellInfo] entry's identity may be used as the serving cell
     * for the *currently monitored* subscription.
     *
     * An explicit PLMN mismatch against every [expectedPlmns] value is rejected, except when
     * [acceptRegisteredPlmnMismatch] is set and the cell is registered — limited-service SOS
     * camp often keeps `TelephonyManager.networkOperator` on the home PLMN while the registered
     * cell is the visited operator. Cells with
     * [PlmnMatchStatus.UNKNOWN] (identity doesn't expose usable MCC/MNC) are accepted as
     * best-effort *unless* another cell of the same RAT in the same read explicitly matched —
     * in that case the explicit match already found the real serving cell, so the unverifiable
     * one is almost certainly stale/neighbor data and is dropped.
     *
     * [isDualSimActive] previously made this stricter — requiring an explicit PLMN match before
     * accepting anything — to guard against a suspected dual-SIM cross-contamination leak.
     * That turned out to be unsafe in practice: [expectedPlmn] (derived from
     * `TelephonyManager.networkOperator`) and/or the cell identity's own MCC/MNC are frequently
     * blank/unreliable on real devices (especially via a subscription-scoped `TelephonyManager`
     * from `createForSubscriptionId`), which made the strict gate reject *every* cell — wiping
     * out EARFCN/PCI/network-mode display for affected users regardless of SIM count. Reverted
     * to the lenient behavior; [isDualSimActive] is kept as a parameter (currently unused) so a
     * future, better-verified mitigation for the cross-SIM leak can reuse the plumbing without
     * re-touching every call site.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun shouldUseCellIdentity(
        identity: CellIdentity,
        expectedPlmns: Collection<String>,
        hasExplicitPlmnMatches: Boolean,
        isDualSimActive: Boolean,
        isRegistered: Boolean,
        acceptRegisteredPlmnMismatch: Boolean
    ): Boolean {
        return when (plmnMatchAny(identity, expectedPlmns)) {
            PlmnMatchStatus.MATCH -> true
            PlmnMatchStatus.UNKNOWN -> !hasExplicitPlmnMatches
            PlmnMatchStatus.MISMATCH -> acceptRegisteredPlmnMismatch && isRegistered
        }
    }

    private fun plmnMatchAny(
        identity: CellIdentity,
        expectedPlmns: Collection<String>
    ): PlmnMatchStatus {
        if (expectedPlmns.isEmpty()) return PlmnMatchStatus.UNKNOWN
        var sawMismatch = false
        for (plmn in expectedPlmns) {
            when (plmnMatchStatus(identity, plmn)) {
                PlmnMatchStatus.MATCH -> return PlmnMatchStatus.MATCH
                PlmnMatchStatus.UNKNOWN -> Unit
                PlmnMatchStatus.MISMATCH -> sawMismatch = true
            }
        }
        return if (sawMismatch) PlmnMatchStatus.MISMATCH else PlmnMatchStatus.UNKNOWN
    }

    private fun plmnMatchStatus(identity: CellIdentity, expectedPlmn: String?): PlmnMatchStatus {
        if (expectedPlmn.isNullOrBlank() || expectedPlmn.length < 5) {
            return PlmnMatchStatus.UNKNOWN
        }

        val (mcc, mnc) = readIdentityPlmn(identity) ?: return PlmnMatchStatus.UNKNOWN
        val expectedMcc = expectedPlmn.take(3)
        val expectedMnc = expectedPlmn.substring(3)
        return if (mcc == expectedMcc && normalizeMnc(mnc) == normalizeMnc(expectedMnc)) {
            PlmnMatchStatus.MATCH
        } else {
            PlmnMatchStatus.MISMATCH
        }
    }

    private fun readIdentityPlmn(identity: CellIdentity): Pair<String, String>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val mcc = when (identity) {
                is CellIdentityLte -> identity.mccString
                is CellIdentityNr -> identity.mccString
                is CellIdentityGsm -> identity.mccString
                else -> null
            }?.takeIf { it.isNotBlank() }
            val mnc = when (identity) {
                is CellIdentityLte -> identity.mncString
                is CellIdentityNr -> identity.mncString
                is CellIdentityGsm -> identity.mncString
                else -> null
            }?.takeIf { it.isNotBlank() }
            if (mcc != null && mnc != null) {
                return mcc to mnc
            }
        }

        val mccInt = when (identity) {
            is CellIdentityLte -> identity.mcc
            is CellIdentityGsm -> identity.mcc
            else -> null
        }?.takeIf { isValidCellIdentityValue(it) }

        val mncInt = when (identity) {
            is CellIdentityLte -> identity.mnc
            is CellIdentityGsm -> identity.mnc
            else -> null
        }?.takeIf { isValidCellIdentityValue(it) }

        if (mccInt == null || mncInt == null) {
            return null
        }
        return mccInt.toString() to mncInt.toString()
    }

    private fun formatIdentityPlmn(identity: CellIdentity): String? {
        val (mcc, mnc) = readIdentityPlmn(identity) ?: return null
        val paddedMnc = if (mnc.length == 1) mnc.padStart(2, '0') else mnc
        return mcc + paddedMnc
    }

    private fun readIdentityOperatorName(identity: CellIdentity): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return normalizeOperatorName(identity.operatorAlphaLong?.toString())
            ?: normalizeOperatorName(identity.operatorAlphaShort?.toString())
    }

    private fun normalizeMnc(mnc: String): String {
        return mnc.trim().trimStart('0').ifEmpty { "0" }
    }

    private fun cellConnectionRank(cellInfo: CellInfo): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return when (cellInfo.cellConnectionStatus) {
                CellInfo.CONNECTION_PRIMARY_SERVING -> 3
                CellInfo.CONNECTION_SECONDARY_SERVING -> 2
                else -> if (cellInfo.isRegistered) 1 else 0
            }
        }
        return if (cellInfo.isRegistered) 1 else 0
    }

    private data class ServingCellIdentity(
        val lteEarfcn: Int? = null,
        val ltePci: Int? = null,
        val nrEarfcn: Int? = null,
        val nrPci: Int? = null,
        val nrBand: Int? = null,
        val gsmEarfcn: Int? = null,
        val gsmBsic: Int? = null,
        val rsrpDbm: Int? = null,
        val rsrqDb: Int? = null,
        val lteSinrDb: Int? = null,
        val nrSinrDb: Int? = null,
        val radioAccessType: String? = null,
        val servingPlmn: String? = null,
        val servingOperatorName: String? = null
    ) {
        val hasCampedIdentity: Boolean
            get() = lteEarfcn != null || ltePci != null || nrEarfcn != null || nrPci != null ||
                gsmEarfcn != null || gsmBsic != null

        fun sameServingKeysAs(other: ServingCellIdentity): Boolean {
            if (!hasCampedIdentity || !other.hasCampedIdentity) return false
            return lteEarfcn == other.lteEarfcn &&
                ltePci == other.ltePci &&
                nrEarfcn == other.nrEarfcn &&
                nrPci == other.nrPci &&
                gsmEarfcn == other.gsmEarfcn &&
                gsmBsic == other.gsmBsic
        }

        fun matchesSignalPcis(ltePcis: Set<Int>, nrPcis: Set<Int>): Boolean {
            if (ltePci != null && ltePci in ltePcis) return true
            if (nrPci != null && nrPci in nrPcis) return true
            return ltePcis.isEmpty() && nrPcis.isEmpty() && !hasCampedIdentity
        }

        fun fillGapsFrom(fallback: ServingCellIdentity): ServingCellIdentity {
            val nextLteEarfcn = lteEarfcn ?: fallback.lteEarfcn?.takeIf {
                ltePci == null || fallback.ltePci == null || ltePci == fallback.ltePci
            }
            val nextLtePci = ltePci ?: fallback.ltePci?.takeIf {
                val earfcn = lteEarfcn ?: nextLteEarfcn
                earfcn == null || fallback.lteEarfcn == null || earfcn == fallback.lteEarfcn
            }
            val nextNrEarfcn = nrEarfcn ?: fallback.nrEarfcn?.takeIf {
                nrPci == null || fallback.nrPci == null || nrPci == fallback.nrPci
            }
            val nextNrPci = nrPci ?: fallback.nrPci?.takeIf {
                val earfcn = nrEarfcn ?: nextNrEarfcn
                earfcn == null || fallback.nrEarfcn == null || earfcn == fallback.nrEarfcn
            }
            val nextGsmEarfcn = gsmEarfcn ?: fallback.gsmEarfcn?.takeIf {
                gsmBsic == null || fallback.gsmBsic == null || gsmBsic == fallback.gsmBsic
            }
            val nextGsmBsic = gsmBsic ?: fallback.gsmBsic?.takeIf {
                val earfcn = gsmEarfcn ?: nextGsmEarfcn
                earfcn == null || fallback.gsmEarfcn == null || earfcn == fallback.gsmEarfcn
            }
            val sameCell = copy(
                lteEarfcn = nextLteEarfcn,
                ltePci = nextLtePci,
                nrEarfcn = nextNrEarfcn,
                nrPci = nextNrPci,
                gsmEarfcn = nextGsmEarfcn,
                gsmBsic = nextGsmBsic
            ).sameServingKeysAs(fallback) ||
                (!fallback.hasCampedIdentity)
            return copy(
                lteEarfcn = nextLteEarfcn,
                ltePci = nextLtePci,
                nrEarfcn = nextNrEarfcn,
                nrPci = nextNrPci,
                nrBand = nrBand ?: fallback.nrBand?.takeIf {
                    val earfcn = nrEarfcn ?: nextNrEarfcn
                    earfcn == null || fallback.nrEarfcn == null || earfcn == fallback.nrEarfcn
                },
                gsmEarfcn = nextGsmEarfcn,
                gsmBsic = nextGsmBsic,
                rsrpDbm = rsrpDbm ?: fallback.rsrpDbm.takeIf { sameCell },
                rsrqDb = rsrqDb ?: fallback.rsrqDb.takeIf { sameCell },
                lteSinrDb = lteSinrDb ?: fallback.lteSinrDb.takeIf { sameCell },
                nrSinrDb = nrSinrDb ?: fallback.nrSinrDb.takeIf { sameCell },
                radioAccessType = radioAccessType ?: fallback.radioAccessType,
                servingPlmn = servingPlmn ?: fallback.servingPlmn,
                servingOperatorName = servingOperatorName ?: fallback.servingOperatorName
            )
        }
    }

    private fun readRegisteredServingKeys(serviceState: ServiceState?): RegisteredServingKeys {
        if (serviceState == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return RegisteredServingKeys()
        }
        return runCatching {
            val registered = serviceState.networkRegistrationInfoList.filter { info ->
                info.transportType == AccessNetworkConstants.TRANSPORT_TYPE_WWAN &&
                    @Suppress("DEPRECATION") info.isRegistered
            }
            var lteEarfcn: Int? = null
            var ltePci: Int? = null
            var nrEarfcn: Int? = null
            var nrPci: Int? = null
            var gsmEarfcn: Int? = null
            var gsmBsic: Int? = null
            for (info in registered) {
                when (val identity = info.cellIdentity) {
                    is CellIdentityLte -> {
                        lteEarfcn = identity.earfcn.takeIf { isValidCellIdentityValue(it) } ?: lteEarfcn
                        ltePci = identity.pci.takeIf { isValidCellIdentityValue(it) } ?: ltePci
                    }
                    is CellIdentityNr -> {
                        nrEarfcn = identity.nrarfcn.takeIf { isValidCellIdentityValue(it) } ?: nrEarfcn
                        nrPci = identity.pci.takeIf { isValidCellIdentityValue(it) } ?: nrPci
                    }
                    is CellIdentityGsm -> {
                        gsmEarfcn = identity.arfcn.takeIf { isValidCellIdentityValue(it) } ?: gsmEarfcn
                        gsmBsic = identity.bsic.takeIf { isValidCellIdentityValue(it) } ?: gsmBsic
                    }
                }
            }
            RegisteredServingKeys(lteEarfcn, ltePci, nrEarfcn, nrPci, gsmEarfcn, gsmBsic)
        }.getOrDefault(RegisteredServingKeys())
    }

    private fun readSignalLtePcis(signalStrength: SignalStrength?): Set<Int> {
        if (signalStrength == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return emptySet()
        }
        val pcis = mutableSetOf<Int>()
        for (strength in signalStrength.cellSignalStrengths) {
            if (strength is CellSignalStrengthLte) {
                val pci = invokeIntMethod(strength, "getPci")
                if (pci != Int.MIN_VALUE && isValidCellIdentityValue(pci)) {
                    pcis.add(pci)
                }
            }
        }
        return pcis
    }

    private fun readSignalNrPcis(signalStrength: SignalStrength?): Set<Int> {
        if (signalStrength == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return emptySet()
        }
        val pcis = mutableSetOf<Int>()
        for (strength in signalStrength.cellSignalStrengths) {
            if (strength is CellSignalStrengthNr) {
                val pci = invokeIntMethod(strength, "getPci")
                if (pci != Int.MIN_VALUE && isValidCellIdentityValue(pci)) {
                    pcis.add(pci)
                }
            }
        }
        return pcis
    }

    private fun isValidCellIdentityValue(value: Int): Boolean {
        return value != CellInfo.UNAVAILABLE && value != Int.MAX_VALUE
    }

    private fun isValidMetric(value: Int): Boolean {
        return value != CellInfo.UNAVAILABLE && value != Int.MAX_VALUE && value != 0
    }

    private val TWO_G_NETWORK_TYPES = setOf(
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GSM
    )

    private val LTE_NR_RADIO_TYPES = setOf(RADIO_4G, RADIO_5G, RADIO_5G_ENDC)
}
