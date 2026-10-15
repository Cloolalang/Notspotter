package io.github.cloolalang.notspotdetector.model

data class RttSample(
    val timestampMs: Long,
    val rttMs: Long?,
    val jitterMs: Long = 0,
    val packetLossPercent: Float = 0f
)
