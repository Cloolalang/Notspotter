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
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.NetworkModePreference
import io.github.cloolalang.notspotdetector.model.NetworkServiceMode
import io.github.cloolalang.notspotdetector.model.readNetworkModePreference

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
        val networkModePreference = readNetworkModePreference(telephonyManager)
        val restrictedTo2gNetwork = networkModePreference == NetworkModePreference.FORCED_2G
        val networkServiceMode = readNetworkServiceMode(telephonyManager)
        val isWifiCallingActive = readWifiCallingActive(telephonyManager)
        val simSlotIndex = SimSubscriptionHelper.resolveSlotIndex(context, subscriptionId)
        val simDisplayName = SimSubscriptionHelper.resolveSubscriptionLabel(context, subscriptionId)
        val hasLimitedServiceOnAnySim = hasLimitedServiceOnAnySubscription(context)
        val isDualSimActive = SimSubscriptionHelper.listActiveSubscriptions(context).size > 1
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
        val isOn2g = isEffectivelyOn2g(signalMetrics, networkReports2g)
        val servingCell = readServingCellForSignal(
            telephonyManager = telephonyManager,
            cellIdentityPermissionGranted = cellIdentityPermissionGranted,
            monitor2gFallback = monitor2gFallback,
            expectedPlmn = plmn,
            signalMetrics = signalMetrics,
            networkReports2g = networkReports2g,
            isDualSimActive = isDualSimActive
        )

        var metrics = signalMetrics.copy(
            networkOperatorName = operatorInfo.displayOperatorName,
            homeNetworkOperatorName = operatorInfo.homeOperatorName,
            servingNetworkOperatorName = operatorInfo.servingOperatorName,
            plmn = plmn,
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
            isWifiCallingActive = isWifiCallingActive,
            hasHomeGsmSignal = hasHomeGsmSignal,
            subscriptionId = subscriptionId.takeIf {
                it != MonitoringSettings.DEFAULT_SUBSCRIPTION_ID
            },
            simSlotIndex = simSlotIndex,
            simDisplayName = simDisplayName
        )

        if (isOn2g && !monitor2gFallback) {
            metrics = metrics.copy(
                radioAccessType = RADIO_2G,
                rsrpDbm = null,
                rsrqDb = null,
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

        metrics = metrics.copy(
            hasLimitedServiceOnAnySim = hasLimitedServiceOnAnySim,
            isCompleteNoService = isCompleteNoService(metrics, hasLimitedServiceOnAnySim)
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
        signalMetrics: CellularRadioMetrics,
        networkReports2g: Boolean,
        isDualSimActive: Boolean
    ): ServingCellIdentity {
        if (!cellIdentityPermissionGranted) return ServingCellIdentity()

        return when {
            shouldReadLteNrCellIdentity(signalMetrics, networkReports2g) ->
                readServingCellIdentities(telephonyManager, monitor2gFallback = false, expectedPlmn, isDualSimActive)
            shouldReadGsmCellIdentity(signalMetrics, networkReports2g, monitor2gFallback) ->
                readServingCellIdentities(telephonyManager, monitor2gFallback = true, expectedPlmn, isDualSimActive)
            else -> ServingCellIdentity()
        }
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
        if (metrics.networkServiceMode != NetworkServiceMode.OUT_OF_SERVICE) return false
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
        val serviceState = telephonyManager.serviceState ?: return NetworkServiceMode.UNKNOWN

        if (readEnhancedLimitedServiceState(serviceState)) {
            return NetworkServiceMode.LIMITED_SERVICE
        }

        return when (serviceState.state) {
            ServiceState.STATE_IN_SERVICE -> NetworkServiceMode.IN_SERVICE
            ServiceState.STATE_EMERGENCY_ONLY -> NetworkServiceMode.LIMITED_SERVICE
            ServiceState.STATE_OUT_OF_SERVICE,
            ServiceState.STATE_POWER_OFF -> NetworkServiceMode.OUT_OF_SERVICE
            else -> NetworkServiceMode.UNKNOWN
        }
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
                        if (isValidMetric(rsrp) || isValidMetric(rsrq)) {
                            hasLteNrSignal = true
                            nrMetrics = CellularRadioMetrics(
                                rsrpDbm = rsrp.takeIf { isValidMetric(it) },
                                rsrqDb = rsrq.takeIf { isValidMetric(it) },
                                permissionGranted = true
                            )
                        }
                    }
                    is CellSignalStrengthLte -> {
                        val rsrp = strength.rsrp
                        val rsrq = strength.rsrq
                        if (isValidMetric(rsrp) || isValidMetric(rsrq)) {
                            hasLteNrSignal = true
                            lteMetrics = CellularRadioMetrics(
                                rsrpDbm = rsrp.takeIf { isValidMetric(it) },
                                rsrqDb = rsrq.takeIf { isValidMetric(it) },
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
                    lteMetrics.copy(radioAccessType = RADIO_5G_ENDC)
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

    @SuppressLint("MissingPermission")
    private fun readServingCellIdentities(
        telephonyManager: TelephonyManager,
        monitor2gFallback: Boolean,
        expectedPlmn: String?,
        isDualSimActive: Boolean
    ): ServingCellIdentity {
        return try {
            val cellInfoList = telephonyManager.allCellInfo ?: return ServingCellIdentity()
            val registered = extractServingCellIdentities(
                cellInfoList,
                registeredOnly = true,
                monitor2gFallback,
                expectedPlmn,
                isDualSimActive
            )
            val fromAllCells = extractServingCellIdentities(
                cellInfoList,
                registeredOnly = false,
                monitor2gFallback,
                expectedPlmn,
                isDualSimActive
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
        expectedPlmn: String?,
        isDualSimActive: Boolean
    ): ServingCellIdentity? {
        var bestLte: RankedServingCell? = null
        var bestNr: RankedServingCell? = null
        var bestGsm: RankedServingCell? = null
        val hasExplicitLtePlmnMatches = hasExplicitPlmnMatchForRat(cellInfoList, expectedPlmn) { it is CellInfoLte }
        val hasExplicitNrPlmnMatches = hasExplicitPlmnMatchForRat(cellInfoList, expectedPlmn) { it is CellInfoNr }
        val hasExplicitGsmPlmnMatches = hasExplicitPlmnMatchForRat(cellInfoList, expectedPlmn) { it is CellInfoGsm }

        for (info in cellInfoList) {
            if (registeredOnly && !info.isRegistered) continue
            val connectionRank = cellConnectionRank(info)
            if (registeredOnly && connectionRank == 0) continue

            when (info) {
                is CellInfoLte -> {
                    if (!shouldUseCellIdentity(info.cellIdentity, expectedPlmn, hasExplicitLtePlmnMatches, isDualSimActive)) {
                        continue
                    }
                    val identity = info.cellIdentity
                    val candidate = RankedServingCell(
                        connectionRank = connectionRank,
                        lteEarfcn = identity.earfcn.takeIf { isValidCellIdentityValue(it) },
                        ltePci = identity.pci.takeIf { isValidCellIdentityValue(it) }
                    )
                    if (candidate.hasValues && (bestLte == null || candidate beats bestLte)) {
                        bestLte = candidate
                    }
                }
                is CellInfoNr -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (!shouldUseCellIdentity(info.cellIdentity, expectedPlmn, hasExplicitNrPlmnMatches, isDualSimActive)) {
                            continue
                        }
                        val identity = info.cellIdentity
                        if (identity is CellIdentityNr) {
                            val candidate = RankedServingCell(
                                connectionRank = connectionRank,
                                nrEarfcn = identity.nrarfcn.takeIf { isValidCellIdentityValue(it) },
                                nrPci = identity.pci.takeIf { isValidCellIdentityValue(it) },
                                nrBand = readNrBand(identity)
                            )
                            if (candidate.hasValues && (bestNr == null || candidate beats bestNr)) {
                                bestNr = candidate
                            }
                        }
                    }
                }
                is CellInfoGsm -> {
                    if (monitor2gFallback) {
                        if (!shouldUseCellIdentity(info.cellIdentity, expectedPlmn, hasExplicitGsmPlmnMatches, isDualSimActive)) {
                            continue
                        }
                        val identity = info.cellIdentity
                        val candidate = RankedServingCell(
                            connectionRank = connectionRank,
                            gsmEarfcn = identity.arfcn.takeIf { isValidCellIdentityValue(it) },
                            gsmBsic = identity.bsic.takeIf { isValidCellIdentityValue(it) }
                        )
                        if (candidate.hasValues && (bestGsm == null || candidate beats bestGsm)) {
                            bestGsm = candidate
                        }
                    }
                }
            }
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
            gsmBsic = gsmBsic
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
        expectedPlmn: String?,
        ratFilter: (CellInfo) -> Boolean
    ): Boolean {
        return cellInfoList.any { info ->
            ratFilter(info) &&
                plmnMatchStatus(info.cellIdentity, expectedPlmn) == PlmnMatchStatus.MATCH
        }
    }

    private enum class PlmnMatchStatus {
        MATCH,
        UNKNOWN,
        MISMATCH
    }

    private data class RankedServingCell(
        val connectionRank: Int,
        val lteEarfcn: Int? = null,
        val ltePci: Int? = null,
        val nrEarfcn: Int? = null,
        val nrPci: Int? = null,
        val nrBand: Int? = null,
        val gsmEarfcn: Int? = null,
        val gsmBsic: Int? = null
    ) {
        val hasValues: Boolean
            get() = lteEarfcn != null || ltePci != null || nrEarfcn != null || nrPci != null ||
                gsmEarfcn != null || gsmBsic != null

        infix fun beats(other: RankedServingCell): Boolean = connectionRank > other.connectionRank
    }

    /**
     * Decides whether a candidate [CellInfo] entry's identity may be used as the serving cell
     * for the *currently monitored* subscription.
     *
     * An explicit PLMN mismatch against [expectedPlmn] is always rejected. Cells with
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
        expectedPlmn: String?,
        hasExplicitPlmnMatches: Boolean,
        isDualSimActive: Boolean
    ): Boolean {
        return when (plmnMatchStatus(identity, expectedPlmn)) {
            PlmnMatchStatus.MATCH -> true
            PlmnMatchStatus.UNKNOWN -> !hasExplicitPlmnMatches
            PlmnMatchStatus.MISMATCH -> false
        }
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
        val gsmBsic: Int? = null
    ) {
        fun fillGapsFrom(fallback: ServingCellIdentity): ServingCellIdentity {
            return copy(
                lteEarfcn = lteEarfcn ?: fallback.lteEarfcn,
                ltePci = ltePci ?: fallback.ltePci?.takeIf {
                    lteEarfcn == null ||
                        fallback.lteEarfcn == null ||
                        lteEarfcn == fallback.lteEarfcn
                },
                nrEarfcn = nrEarfcn ?: fallback.nrEarfcn,
                nrPci = nrPci ?: fallback.nrPci?.takeIf {
                    nrEarfcn == null ||
                        fallback.nrEarfcn == null ||
                        nrEarfcn == fallback.nrEarfcn
                },
                nrBand = nrBand ?: fallback.nrBand?.takeIf {
                    nrEarfcn == null ||
                        fallback.nrEarfcn == null ||
                        nrEarfcn == fallback.nrEarfcn
                },
                gsmEarfcn = gsmEarfcn ?: fallback.gsmEarfcn,
                gsmBsic = gsmBsic ?: fallback.gsmBsic?.takeIf {
                    gsmEarfcn == null ||
                        fallback.gsmEarfcn == null ||
                        gsmEarfcn == fallback.gsmEarfcn
                }
            )
        }
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
