package io.github.cloolalang.notspotdetector.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TelephonyNetworkSpecifier
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.telephony.SubscriptionManager
import io.github.cloolalang.notspotdetector.model.ApnCandidate
import io.github.cloolalang.notspotdetector.model.apnFromDataConnectionState
import io.github.cloolalang.notspotdetector.model.pickPreferredApnDisplay
import io.github.cloolalang.notspotdetector.model.sanitizeApnExtra

/**
 * Reads the selected/active data APN.
 *
 * The telephony carrier table (`preferapn`) needs [android.Manifest.permission.WRITE_APN_SETTINGS],
 * which a normal app cannot get. Public fallbacks are the cellular [android.net.NetworkInfo]
 * extra, the last telephony data-state broadcast, and the SIM APN list (API 31+).
 */
object SelectedApnReader {

    private val APN_COLUMNS = arrayOf(
        Telephony.Carriers.NAME,
        Telephony.Carriers.APN,
        Telephony.Carriers.TYPE,
        Telephony.Carriers.CURRENT,
        Telephony.Carriers.CARRIER_ENABLED
    )

    @Volatile
    private var lastKnownApn: String? = null

    fun read(context: Context, subscriptionId: Int): String? {
        val subId = SimSubscriptionHelper.resolveEffectiveSubscriptionId(context, subscriptionId)
        val fresh = readFromCellularConnection(context, subId)
            ?: readFromMobileNetworkInfo(context)
            ?: readFromDataStateBroadcast(context, subId)
            ?: readFromSimApnList(context, subId)
            ?: preferredUris(subId).firstNotNullOfOrNull { readFromUri(context, it) }
            ?: readCurrentCarrier(context, subId)
        if (fresh != null) lastKnownApn = fresh
        return fresh ?: lastKnownApn
    }

    @SuppressLint("MissingPermission", "DEPRECATION")
    private fun readFromCellularConnection(context: Context, subId: Int?): String? {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return null
        data class RankedApn(val apn: String, val matchedSub: Boolean, val hasInternet: Boolean)

        val ranked = connectivity.allNetworks.mapNotNull { network ->
            val caps = connectivity.getNetworkCapabilities(network) ?: return@mapNotNull null
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return@mapNotNull null
            val extra = sanitizeApnExtra(connectivity.getNetworkInfo(network)?.extraInfo)
                ?: return@mapNotNull null
            RankedApn(
                apn = extra,
                matchedSub = matchesSubscription(caps, subId),
                hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_SUPL)
            )
        }
        return ranked
            .sortedWith(
                compareByDescending<RankedApn> { it.matchedSub }
                    .thenByDescending { it.hasInternet }
            )
            .firstOrNull()
            ?.apn
    }

    @SuppressLint("DEPRECATION")
    private fun readFromMobileNetworkInfo(context: Context): String? {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val infos = buildList {
            add(connectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE))
            add(connectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_HIPRI))
            add(connectivity.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_DUN))
            connectivity.allNetworkInfo?.forEach { add(it) }
        }
        return infos.firstNotNullOfOrNull { info ->
            if (info?.type != ConnectivityManager.TYPE_MOBILE &&
                info?.type != ConnectivityManager.TYPE_MOBILE_HIPRI &&
                info?.type != ConnectivityManager.TYPE_MOBILE_DUN
            ) {
                return@firstNotNullOfOrNull null
            }
            sanitizeApnExtra(info.extraInfo)
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun readFromDataStateBroadcast(context: Context, subId: Int?): String? {
        val filter = IntentFilter("android.intent.action.ANY_DATA_STATE")
        val intent = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(null, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(null, filter)
            }
        }.getOrNull() ?: return null
        val fromMatchingSub = apnFromDataConnectionState(
            apn = intent.getStringExtra("apn"),
            apnType = intent.getStringExtra("apnType"),
            subscriptionId = intent.dataStateSubscriptionId(),
            wantedSubscriptionId = subId
        )
        if (fromMatchingSub != null) return fromMatchingSub
        return apnFromDataConnectionState(
            apn = intent.getStringExtra("apn"),
            apnType = intent.getStringExtra("apnType")
        )
    }

    private fun Intent.dataStateSubscriptionId(): Int? {
        val keys = listOf(
            "subscription",
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            "subId"
        )
        for (key in keys) {
            if (!hasExtra(key)) continue
            val asInt = getIntExtra(key, Int.MIN_VALUE)
            if (asInt != Int.MIN_VALUE && asInt >= 0) return asInt
            getStringExtra(key)?.toIntOrNull()?.takeIf { it >= 0 }?.let { return it }
            getLongExtra(key, -1L).takeIf { it >= 0 }?.toInt()?.let { return it }
        }
        return null
    }

    private fun matchesSubscription(caps: NetworkCapabilities, subId: Int?): Boolean {
        if (subId == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        val specifier = caps.networkSpecifier as? TelephonyNetworkSpecifier
            ?: caps.transportInfo as? TelephonyNetworkSpecifier
            ?: return true
        return specifier.subscriptionId == subId
    }

    private fun preferredUris(subId: Int?): List<Uri> {
        return buildList {
            if (subId != null) {
                add(Uri.parse("content://telephony/carriers/preferapn/subId/$subId"))
            }
            add(Uri.parse("content://telephony/carriers/preferapn"))
            add(Uri.parse("content://telephony/carriers/current"))
        }
    }

    private fun readFromSimApnList(context: Context, subId: Int?): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val uris = buildList {
            if (subId != null) {
                add(Uri.withAppendedPath(Telephony.Carriers.SIM_APN_URI, subId.toString()))
                add(Uri.parse("content://telephony/carriers/sim_apn_list/subId/$subId"))
            }
            add(Telephony.Carriers.SIM_APN_URI)
        }
        return uris.firstNotNullOfOrNull { queryBestDisplay(context, it, selection = null, args = null) }
    }

    private fun readCurrentCarrier(context: Context, subId: Int?): String? {
        val selection = if (subId != null) {
            "${Telephony.Carriers.CURRENT} = ? AND sub_id = ?"
        } else {
            "${Telephony.Carriers.CURRENT} = ?"
        }
        val args = if (subId != null) {
            arrayOf("1", subId.toString())
        } else {
            arrayOf("1")
        }
        return queryBestDisplay(context, Telephony.Carriers.CONTENT_URI, selection, args)
            ?: queryBestDisplay(
                context,
                Telephony.Carriers.CONTENT_URI,
                "${Telephony.Carriers.CURRENT} = ?",
                arrayOf("1")
            )
    }

    private fun readFromUri(context: Context, uri: Uri): String? {
        return queryBestDisplay(context, uri, selection = null, args = null)
    }

    private fun queryBestDisplay(
        context: Context,
        uri: Uri,
        selection: String?,
        args: Array<String>?
    ): String? {
        return queryCandidates(context, uri, APN_COLUMNS, selection, args)
            ?: queryCandidates(context, uri, null, selection, args)
    }

    private fun queryCandidates(
        context: Context,
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        args: Array<String>?
    ): String? {
        return runCatching {
            context.contentResolver.query(uri, projection, selection, args, null)?.use { rows ->
                val candidates = buildList {
                    while (rows.moveToNext()) {
                        add(
                            ApnCandidate(
                                name = column(rows, Telephony.Carriers.NAME),
                                apn = column(rows, Telephony.Carriers.APN),
                                type = column(rows, Telephony.Carriers.TYPE),
                                current = column(rows, Telephony.Carriers.CURRENT) == "1",
                                enabled = column(rows, Telephony.Carriers.CARRIER_ENABLED)
                                    ?.let { it == "1" || it.equals("true", ignoreCase = true) }
                                    ?: true
                            )
                        )
                    }
                }
                pickPreferredApnDisplay(candidates)
            }
        }.getOrNull()
    }

    private fun column(cursor: Cursor, name: String): String? {
        val index = cursor.getColumnIndex(name)
        if (index < 0 || cursor.isNull(index)) return null
        return when (cursor.getType(index)) {
            android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getInt(index).toString()
            else -> cursor.getString(index)
        }
    }
}
