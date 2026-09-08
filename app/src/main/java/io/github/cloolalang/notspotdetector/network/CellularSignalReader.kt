package io.github.cloolalang.notspotdetector.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import io.github.cloolalang.notspotdetector.model.CellularRadioMetrics
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.NetworkServiceMode

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
        val operatorName = readNetworkOperatorName(context, telephonyManager, subscriptionId)
        val plmn = readPlmn(telephonyManager)
        val networkReports2g = isServing2gNetwork(telephonyManager)
        val networkServiceMode = readNetworkServiceMode(telephonyManager)
        val simSlotIndex = SimSubscriptionHelper.resolveSlotIndex(context, subscriptionId)
        val simDisplayName = SimSubscriptionHelper.resolveSubscriptionLabel(context, subscriptionId)
        val hasLimitedServiceOnAnySim = hasLimitedServiceOnAnySubscription(context)
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
            networkReports2g = networkReports2g
        )

        var metrics = signalMetrics.copy(
            networkOperatorName = operatorName,
            plmn = plmn,
            lteEarfcn = servingCell.lteEarfcn,
            ltePci = servingCell.ltePci,
            nrEarfcn = servingCell.nrEarfcn,
            nrPci = servingCell.nrPci,
            gsmEarfcn = servingCell.gsmEarfcn,
            gsmBsic = servingCell.gsmBsic,
            cellIdentityPermissionGranted = cellIdentityPermissionGranted,
            isOn2g = isOn2g,
            isLimitedService = isLimitedService,
            networkServiceMode = networkServiceMode,
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
        networkReports2g: Boolean
    ): ServingCellIdentity {
        if (!cellIdentityPermissionGranted) return ServingCellIdentity()

        return when {
            shouldReadLteNrCellIdentity(signalMetrics, networkReports2g) ->
                readServingCellIdentities(telephonyManager, monitor2gFallback = false, expectedPlmn)
            shouldReadGsmCellIdentity(signalMetrics, networkReports2g, monitor2gFallback) ->
                readServingCellIdentities(telephonyManager, monitor2gFallback = true, expectedPlmn)
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
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
        return hasPhoneStatePermission(context)
    }

    @SuppressLint("MissingPermission")
    private fun readNetworkOperatorName(
        context: Context,
        telephonyManager: TelephonyManager,
        subscriptionId: Int
    ): String? {
        return normalizeOperatorName(telephonyManager.networkOperatorName)
            ?: normalizeOperatorName(telephonyManager.simOperatorName)
            ?: SimSubscriptionHelper.resolveCarrierName(context, subscriptionId)
    }

    private fun normalizeOperatorName(raw: String?): String? {
        return raw?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    @SuppressLint("MissingPermission")
    private fun readPlmn(telephonyManager: TelephonyManager): String? {
        return telephonyManager.networkOperator
            .takeIf { it.isNotBlank() && it.length >= 5 }
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
        expectedPlmn: String?
    ): ServingCellIdentity {
        return try {
            val cellInfoList = telephonyManager.allCellInfo ?: return ServingCellIdentity()
            val registered = extractServingCellIdentities(
                cellInfoList,
                registeredOnly = true,
                monitor2gFallback,
                expectedPlmn
            )
            val fromAllCells = extractServingCellIdentities(
                cellInfoList,
                registeredOnly = false,
                monitor2gFallback,
                expectedPlmn
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
        expectedPlmn: String?
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
                    if (!shouldUseCellIdentity(info.cellIdentity, expectedPlmn, hasExplicitLtePlmnMatches)) {
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
                        if (!shouldUseCellIdentity(info.cellIdentity, expectedPlmn, hasExplicitNrPlmnMatches)) {
                            continue
                        }
                        val identity = info.cellIdentity
                        if (identity is CellIdentityNr) {
                            val candidate = RankedServingCell(
                                connectionRank = connectionRank,
                                nrEarfcn = identity.nrarfcn.takeIf { isValidCellIdentityValue(it) },
                                nrPci = identity.pci.takeIf { isValidCellIdentityValue(it) }
                            )
                            if (candidate.hasValues && (bestNr == null || candidate beats bestNr)) {
                                bestNr = candidate
                            }
                        }
                    }
                }
                is CellInfoGsm -> {
                    if (monitor2gFallback) {
                        if (!shouldUseCellIdentity(info.cellIdentity, expectedPlmn, hasExplicitGsmPlmnMatches)) {
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
            gsmEarfcn = gsmEarfcn,
            gsmBsic = gsmBsic
        )
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
        val gsmEarfcn: Int? = null,
        val gsmBsic: Int? = null
    ) {
        val hasValues: Boolean
            get() = lteEarfcn != null || ltePci != null || nrEarfcn != null || nrPci != null ||
                gsmEarfcn != null || gsmBsic != null

        infix fun beats(other: RankedServingCell): Boolean = connectionRank > other.connectionRank
    }

    private fun shouldUseCellIdentity(
        identity: CellIdentity,
        expectedPlmn: String?,
        hasExplicitPlmnMatches: Boolean
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
