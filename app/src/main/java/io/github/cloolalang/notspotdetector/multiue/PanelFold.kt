package io.github.cloolalang.notspotdetector.multiue

import io.github.cloolalang.notspotdetector.model.MultiUeProtocol
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which panels are open. The master publishes changes to connected slaves.
 * A missing id keeps that panel's own default.
 * [MultiUeProtocol.LOCAL_PANELS] stay on this phone. Role, pairing, and the
 * connection are not part of that publish.
 */
object PanelFold {
    private val _open = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val open: StateFlow<Map<String, Boolean>> = _open.asStateFlow()

    fun snapshot(): Map<String, Boolean> = _open.value

    fun set(id: String, expanded: Boolean) {
        val current = _open.value
        if (current[id] == expanded) return
        _open.value = current + (id to expanded)
    }

    fun replace(open: Map<String, Boolean>) {
        val kept = _open.value.filterKeys { it in MultiUeProtocol.LOCAL_PANELS }
        val next = open.filterKeys { it !in MultiUeProtocol.LOCAL_PANELS } + kept
        if (_open.value == next) return
        _open.value = next
    }
}
