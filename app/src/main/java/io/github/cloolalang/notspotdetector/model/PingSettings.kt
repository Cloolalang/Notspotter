package io.github.cloolalang.notspotdetector.model

data class PingSettings(
    val host: String = DEFAULT_HOST,
    val port: Int = DEFAULT_PORT,
    val pingsPerTest: Int = DEFAULT_PINGS_PER_TEST,
    val testIntervalMs: Long = DEFAULT_TEST_INTERVAL_MS
) {
    fun normalized(): PingSettings {
        return copy(
            host = host.trim().ifEmpty { DEFAULT_HOST },
            port = port.coerceIn(MIN_PORT, MAX_PORT),
            pingsPerTest = pingsPerTest.coerceIn(MIN_PINGS_PER_TEST, MAX_PINGS_PER_TEST),
            testIntervalMs = testIntervalMs.coerceIn(MIN_TEST_INTERVAL_MS, MAX_TEST_INTERVAL_MS)
        )
    }

    val displayAddress: String
        get() = if (port == DEFAULT_PORT) host else "$host:$port"

    /** Total TCP probes per test cycle, including one RRC warm-up. */
    val totalPingsPerTest: Int
        get() = pingsPerTest + RRC_WARMUP_PINGS

    companion object {
        const val RRC_WARMUP_PINGS = 1
        const val DEFAULT_HOST = "8.8.8.8"
        const val DEFAULT_PORT = 443
        const val DEFAULT_PINGS_PER_TEST = 2
        const val DEFAULT_TEST_INTERVAL_MS = 10_000L

        const val MIN_PINGS_PER_TEST = 1
        const val MAX_PINGS_PER_TEST = 10
        const val MIN_PORT = 1
        const val MAX_PORT = 65_535
        const val MIN_TEST_INTERVAL_MS = 5_000L
        const val MAX_TEST_INTERVAL_MS = 60_000L

        fun parseAddress(input: String): Pair<String, Int> {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) {
                return DEFAULT_HOST to DEFAULT_PORT
            }

            val colonIndex = trimmed.lastIndexOf(':')
            if (colonIndex in 1 until trimmed.lastIndex) {
                val hostPart = trimmed.substring(0, colonIndex).trim()
                val portPart = trimmed.substring(colonIndex + 1).trim().toIntOrNull()
                if (hostPart.isNotEmpty() && portPart != null && portPart in MIN_PORT..MAX_PORT) {
                    return hostPart to portPart
                }
            }

            return trimmed to DEFAULT_PORT
        }
    }
}
