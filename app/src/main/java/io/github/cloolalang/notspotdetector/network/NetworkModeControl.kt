package io.github.cloolalang.notspotdetector.network

import android.os.Build
import android.telephony.TelephonyManager
import java.util.concurrent.TimeUnit

/**
 * Changes the phone’s allowed radio technologies. A normal app cannot do this;
 * the working path is a root (`su`) shell running the same commands Settings uses.
 */
object NetworkModeControl {

    /** AOSP [RILConstants.NETWORK_MODE_NR_LTE_WCDMA] — 5G/4G/3G, no GSM. */
    const val PREFERRED_MODE_INHIBIT_2G = 28

    /** AOSP [RILConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA] — includes 2G. */
    const val PREFERRED_MODE_ALL_TECHNOLOGIES = 26

    fun twoGNetworkBitmask(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            (
                TelephonyManager.NETWORK_TYPE_BITMASK_GSM or
                    TelephonyManager.NETWORK_TYPE_BITMASK_GPRS or
                    TelephonyManager.NETWORK_TYPE_BITMASK_EDGE
                ).toLong()
        } else {
            GSM_GPRS_EDGE_BITMASK
        }
    }

    fun allTechnologiesBitmask(): Long = ALL_TECHNOLOGIES_BITMASK

    fun inhibit2gBitmask(): Long = ALL_TECHNOLOGIES_BITMASK and twoGNetworkBitmask().inv()

    fun preferredNetworkMode(inhibit2g: Boolean): Int {
        return if (inhibit2g) PREFERRED_MODE_INHIBIT_2G else PREFERRED_MODE_ALL_TECHNOLOGIES
    }

    fun allowedBitmask(inhibit2g: Boolean): Long {
        return if (inhibit2g) inhibit2gBitmask() else allTechnologiesBitmask()
    }

    fun shellCommands(inhibit2g: Boolean, subscriptionId: Int?): List<String> {
        val bitmask = allowedBitmask(inhibit2g)
        val mode = preferredNetworkMode(inhibit2g)
        val commands = mutableListOf(
            "cmd phone set-allowed-network-types-for-reason user $bitmask",
            "cmd phone set-allowed-network-types-for-reason USER $bitmask",
            "settings put global preferred_network_mode $mode"
        )
        if (subscriptionId != null && subscriptionId >= 0) {
            commands += "settings put global preferred_network_mode$subscriptionId $mode"
            commands += "settings put global preferred_network_mode_$subscriptionId $mode"
        }
        return commands
    }

    fun setInhibit2g(inhibit: Boolean, subscriptionId: Int?): Boolean {
        if (!isRootAvailable()) return false
        return shellCommands(inhibit, subscriptionId).any { runRoot(it) }
    }

    fun isRootAvailable(): Boolean {
        return runCatching {
            val result = runRootCapture("id")
            result.exitCode == 0 && result.output.contains("uid=0")
        }.getOrDefault(false)
    }

    private fun runRoot(command: String): Boolean {
        val result = runRootCapture(command)
        if (result.exitCode != 0) return false
        val output = result.output.lowercase()
        return "unknown command" !in output &&
            "usage:" !in output &&
            "permission denied" !in output &&
            "not allowed" !in output
    }

    private fun runRootCapture(command: String): RootCommandResult {
        val process = ProcessBuilder("su", "-c", command)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val finished = process.waitFor(ROOT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return RootCommandResult(-1, output)
        }
        return RootCommandResult(process.exitValue(), output)
    }

    private data class RootCommandResult(
        val exitCode: Int,
        val output: String
    )

    private const val ROOT_TIMEOUT_SECONDS = 8L

    /** GSM + GPRS + EDGE — same bits as [TelephonyManager] network-type bitmasks. */
    private const val GSM_GPRS_EDGE_BITMASK = 32771L

    /**
     * Common 2G–5G RAT bits (GPRS through NR, including IWLAN).
     * Used when the privileged API needs an explicit allowed-types mask.
     */
    private const val ALL_TECHNOLOGIES_BITMASK = 1_048_575L
}
