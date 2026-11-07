package io.github.cloolalang.notspotdetector.multiue

import io.github.cloolalang.notspotdetector.model.MultiUeLinkSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Process-wide Multi-UE link status. The link service writes it and the screen reads it. */
object MultiUeState {
    private val _snapshot = MutableStateFlow(MultiUeLinkSnapshot())
    val snapshot: StateFlow<MultiUeLinkSnapshot> = _snapshot.asStateFlow()

    fun update(transform: (MultiUeLinkSnapshot) -> MultiUeLinkSnapshot) {
        _snapshot.update(transform)
    }
}
