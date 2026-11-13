package io.github.cloolalang.notspotdetector.service

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import io.github.cloolalang.notspotdetector.network.CallTestLine
import io.github.cloolalang.notspotdetector.ui.CallTestInCallActivity

/**
 * Bound only while NotSpotter is the phone app. That is what allows [Call.disconnect]
 * when a call test reaches its duration.
 */
class CallTestInCallService : InCallService() {

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallTestLine.track(call)
        if (CallTestLine.isTestActive()) return
        val open = Intent(this, CallTestInCallActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        startActivity(open)
    }

    override fun onCallRemoved(call: Call) {
        CallTestLine.untrack(call)
        super.onCallRemoved(call)
    }
}
