package io.github.cloolalang.notspotdetector.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.cloolalang.notspotdetector.multiue.PanelFold

/**
 * Open or collapsed state for one panel. A master toggle is stored for the slaves.
 * [defaultExpanded] is used until this panel has been set.
 */
@Composable
fun panelFold(id: String, defaultExpanded: Boolean): MutableState<Boolean> {
    val open by PanelFold.open.collectAsStateWithLifecycle()
    val shown = open[id] ?: defaultExpanded
    return object : MutableState<Boolean> {
        override var value: Boolean
            get() = shown
            set(value) {
                PanelFold.set(id, value)
            }

        override fun component1(): Boolean = value

        override fun component2(): (Boolean) -> Unit = { value = it }
    }
}
