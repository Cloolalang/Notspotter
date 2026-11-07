package io.github.cloolalang.notspotdetector.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MultiUeProtocolTest {

    @Test
    fun messagesRoundTrip() {
        assertEquals(MultiUeMessage.Hello, MultiUeProtocol.parse(MultiUeProtocol.hello()))
        assertEquals(MultiUeMessage.Passive(on = true), MultiUeProtocol.parse(MultiUeProtocol.passive(true)))
        assertEquals(MultiUeMessage.Passive(on = false), MultiUeProtocol.parse(MultiUeProtocol.passive(false)))
        assertEquals(MultiUeMessage.Full, MultiUeProtocol.parse(MultiUeProtocol.full()))
        assertEquals(MultiUeMessage.Sync(on = true), MultiUeProtocol.parse(MultiUeProtocol.sync(true)))
        assertEquals(MultiUeMessage.Sync(on = false), MultiUeProtocol.parse(MultiUeProtocol.sync(false)))
        assertEquals(
            MultiUeMessage.Settings("e30="),
            MultiUeProtocol.parse(MultiUeProtocol.settings("e30="))
        )
        assertEquals(
            MultiUeMessage.Session(MultiUeRunMode.ACTIVE),
            MultiUeProtocol.parse(MultiUeProtocol.session(MultiUeRunMode.ACTIVE))
        )
        assertEquals(
            MultiUeMessage.Session(MultiUeRunMode.PASSIVE),
            MultiUeProtocol.parse(MultiUeProtocol.session(MultiUeRunMode.PASSIVE))
        )
        assertEquals(
            MultiUeMessage.Session(MultiUeRunMode.STOPPED),
            MultiUeProtocol.parse(MultiUeProtocol.session(MultiUeRunMode.STOPPED))
        )
        assertEquals(MultiUeMessage.Clear, MultiUeProtocol.parse(MultiUeProtocol.clearGraphs()))
        assertEquals(MultiUeMessage.ClearHistory, MultiUeProtocol.parse(MultiUeProtocol.clearHistory()))
        assertEquals(
            MultiUeMessage.Zone(running = true, wholeTest = true),
            MultiUeProtocol.parse(MultiUeProtocol.zone(running = true, wholeTest = true))
        )
        assertEquals(
            MultiUeMessage.Zone(running = false, wholeTest = false),
            MultiUeProtocol.parse(MultiUeProtocol.zone(running = false, wholeTest = false))
        )
        assertNull(MultiUeProtocol.parse("ZONE 1"))
        assertEquals(
            MultiUeMessage.ZoneName("site 1"),
            MultiUeProtocol.parse(MultiUeProtocol.zoneName("site 1"))
        )
        assertEquals(MultiUeMessage.ZoneName(""), MultiUeProtocol.parse(MultiUeProtocol.zoneName("")))
        assertEquals(MultiUeMessage.ZoneName(""), MultiUeProtocol.parse("ZONENAME"))
        assertEquals(MultiUeMessage.Scroll(2_500), MultiUeProtocol.parse(MultiUeProtocol.scroll(2_500)))
        assertEquals(MultiUeMessage.Scroll(0), MultiUeProtocol.parse(MultiUeProtocol.scroll(-1)))
        assertEquals(
            MultiUeMessage.Scroll(MultiUeProtocol.SCROLL_SCALE),
            MultiUeProtocol.parse(MultiUeProtocol.scroll(MultiUeProtocol.SCROLL_SCALE + 20))
        )
        assertNull(MultiUeProtocol.parse("SCROLL"))
        assertNull(MultiUeProtocol.parse("SCROLL 10001"))
        assertEquals(
            MultiUeMessage.Panels(mapOf("histogram" to true, "ping-chart" to false)),
            MultiUeProtocol.parse(
                MultiUeProtocol.panels(mapOf("ping-chart" to false, "histogram" to true))
            )
        )
        assertEquals(MultiUeMessage.Panels(emptyMap()), MultiUeProtocol.parse(MultiUeProtocol.panels(emptyMap())))
        assertEquals(MultiUeMessage.Passive(on = true), MultiUeProtocol.parse("  PASSIVE   1  "))
        assertNull(MultiUeProtocol.parse(""))
        assertNull(MultiUeProtocol.parse("PASSIVE"))
        assertNull(MultiUeProtocol.parse("OTHER 1"))
        assertNull(MultiUeProtocol.parse("PANELS nope"))
    }

    @Test
    fun runPlanStartsStopsAndSwitchesMode() {
        assertEquals(
            MultiUeRunPlan(startPassive = true),
            multiUeRunPlan(false, false, false, MultiUeRunMode.PASSIVE)
        )
        assertEquals(
            MultiUeRunPlan(startActive = true),
            multiUeRunPlan(false, false, false, MultiUeRunMode.ACTIVE)
        )
        assertEquals(
            MultiUeRunPlan(),
            multiUeRunPlan(true, true, true, MultiUeRunMode.PASSIVE)
        )
        assertEquals(
            MultiUeRunPlan(),
            multiUeRunPlan(true, true, false, MultiUeRunMode.ACTIVE)
        )
        assertEquals(
            MultiUeRunPlan(stopFirst = true, startActive = true),
            multiUeRunPlan(true, true, true, MultiUeRunMode.ACTIVE)
        )
        assertEquals(
            MultiUeRunPlan(stopFirst = true, startPassive = true),
            multiUeRunPlan(true, true, false, MultiUeRunMode.PASSIVE)
        )
        assertEquals(
            MultiUeRunPlan(stopFirst = true),
            multiUeRunPlan(true, true, false, MultiUeRunMode.STOPPED)
        )
        assertEquals(
            MultiUeRunPlan(),
            multiUeRunPlan(false, false, false, MultiUeRunMode.STOPPED)
        )
    }

    @Test
    fun passivePlanStartsStopsAndSwitchesActiveMode() {
        assertEquals(
            MultiUePassivePlan(startPassive = true),
            multiUePassivePlan(isRunning = false, passiveSession = false, commandOn = true)
        )
        assertEquals(
            MultiUePassivePlan(),
            multiUePassivePlan(isRunning = true, passiveSession = true, commandOn = true)
        )
        assertEquals(
            MultiUePassivePlan(stopFirst = true, startPassive = true),
            multiUePassivePlan(isRunning = true, passiveSession = false, commandOn = true)
        )
        assertEquals(
            MultiUePassivePlan(stop = true),
            multiUePassivePlan(isRunning = true, passiveSession = true, commandOn = false)
        )
        assertEquals(
            MultiUePassivePlan(stop = true),
            multiUePassivePlan(isRunning = true, passiveSession = false, commandOn = false)
        )
        assertEquals(
            MultiUePassivePlan(),
            multiUePassivePlan(isRunning = false, passiveSession = false, commandOn = false)
        )
    }
}
