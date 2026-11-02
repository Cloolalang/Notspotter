package io.github.cloolalang.notspotdetector

import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.RxssStateFilterSettings

/**
 * Signal settings the monitor announcement tests were written against.
 * Factory defaults now follow the 31 October profile; these tests keep the
 * previous no-signal floor and confirmation filters so they still describe
 * the announcement, not the new factory timing.
 */
fun monitorTestPassiveSettings(): PassiveSignalSettings = PassiveSignalSettings(
    noSignalRsrpDbm = -130,
    rsrqFairMinDb = -18,
    mildTierSoundEnabled = true,
    goodTierSoundEnabled = true,
    fairTierSoundEnabled = true,
    levelRangeDFilter = RxssStateFilterSettings.INACTIVE,
    lowSignalFilter = RxssStateFilterSettings.INACTIVE,
    noSignalFilter = RxssStateFilterSettings(
        enabled = true,
        windowSeconds = 1,
        balancePercent = RxssStateFilterSettings.DEFAULT_BALANCE_PERCENT
    ),
    rsrqFilter = RxssStateFilterSettings.INACTIVE
)
