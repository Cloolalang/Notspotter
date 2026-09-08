package io.github.cloolalang.notspotdetector.model

data class SimSubscriptionOption(
    val subscriptionId: Int,
    val slotIndex: Int,
    val displayName: String,
    val carrierName: String?
)
