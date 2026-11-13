package io.github.cloolalang.notspotdetector.multiue

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import io.github.cloolalang.notspotdetector.model.MultiUeMessage
import io.github.cloolalang.notspotdetector.model.MultiUePeer
import io.github.cloolalang.notspotdetector.model.MultiUeProtocol
import io.github.cloolalang.notspotdetector.model.MultiUeRunMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicInteger

/**
 * Classic Bluetooth RFCOMM session. The master accepts up to [MultiUeProtocol.MAX_SLAVES]
 * slaves. A slave keeps one connection to the master.
 */
@SuppressLint("MissingPermission")
class MultiUeSession(
    private val scope: CoroutineScope,
    private val peerOf: (BluetoothDevice) -> MultiUePeer,
    private val listener: Listener
) {
    interface Listener {
        fun onSlavesChanged(slaves: List<MultiUePeer>)
        fun onMasterReady()
        fun onListenFailed()
        fun onConnecting(peer: MultiUePeer)
        fun onMasterConnected(peer: MultiUePeer)
        fun onMasterLost()
        fun onConnectionFailed()
        fun onMasterFull()
        fun onBondRequired(device: BluetoothDevice)
        fun onPassiveCommand(on: Boolean)
        fun onSession(mode: MultiUeRunMode)
        fun onClearGraphs()
        fun onClearHistory()
        fun onZone(running: Boolean, wholeTest: Boolean)
        fun onZoneName(suffix: String)
        fun onPlan(jpegBase64: String?)
        fun onPinAdd(x: Float, y: Float)
        fun onPinUndo()
        fun onPinClear()
        fun onZoneClear()
        fun onZoneSave(includeHistogram: Boolean)
        fun onZoneHistogram(on: Boolean)
        fun onProfile(apply: Boolean, payload: String)
        fun onPanels(open: Map<String, Boolean>)
        fun onScroll(fraction: Int)
        fun onSlaveSync(address: String, on: Boolean)
        fun onSettings(payload: String)
        fun runMode(): MultiUeRunMode
        fun panelsLine(): String
        fun scrollLine(): String?
        fun zoneLine(): String?
        fun zoneNameLine(): String?
        fun zoneHistogramLine(): String?
        fun planLine(): String?
        fun pinLines(): List<String>
    }

    private val links = mutableListOf<SlaveLink>()
    private val clientGeneration = AtomicInteger()

    @Volatile
    private var serverSocket: BluetoothServerSocket? = null

    @Volatile
    private var masterStarting = false

    @Volatile
    private var clientSocket: BluetoothSocket? = null

    private var masterJob: Job? = null
    private var clientJob: Job? = null

    val listening: Boolean
        get() = serverSocket != null || masterStarting

    val slaveBusy: Boolean
        get() = clientSocket != null || clientJob?.isActive == true

    fun startMaster(adapter: BluetoothAdapter) {
        if (serverSocket != null || masterStarting) return
        masterStarting = true
        masterJob?.cancel()
        masterJob = scope.launch(Dispatchers.IO) {
            val server = try {
                adapter.listenUsingRfcommWithServiceRecord(
                    MultiUeProtocol.SERVICE_NAME,
                    MultiUeProtocol.SERVICE_UUID
                )
            } catch (_: IOException) {
                masterStarting = false
                listener.onListenFailed()
                return@launch
            } catch (_: SecurityException) {
                masterStarting = false
                listener.onListenFailed()
                return@launch
            }
            serverSocket = server
            masterStarting = false
            listener.onMasterReady()
            try {
                while (isActive && serverSocket === server) {
                    val socket = try {
                        server.accept()
                    } catch (_: IOException) {
                        break
                    }
                    adoptSlave(socket)
                }
            } finally {
                if (serverSocket === server) serverSocket = null
                runCatching { server.close() }
            }
        }
    }

    fun connect(adapter: BluetoothAdapter, address: String) {
        val generation = clientGeneration.incrementAndGet()
        clientJob?.cancel()
        closeClientSocket()
        clientJob = scope.launch(Dispatchers.IO) {
            if (!isActive || generation != clientGeneration.get()) return@launch
            val device = try {
                adapter.getRemoteDevice(address)
            } catch (_: IllegalArgumentException) {
                listener.onConnectionFailed()
                return@launch
            }
            val peer = try {
                peerOf(device)
            } catch (_: SecurityException) {
                listener.onConnectionFailed()
                return@launch
            }
            listener.onConnecting(peer)
            val bonded = try {
                device.bondState == BluetoothDevice.BOND_BONDED
            } catch (_: SecurityException) {
                listener.onConnectionFailed()
                return@launch
            }
            if (!bonded) {
                listener.onBondRequired(device)
                return@launch
            }
            runCatching { adapter.cancelDiscovery() }
            val socket = try {
                device.createRfcommSocketToServiceRecord(MultiUeProtocol.SERVICE_UUID)
            } catch (_: IOException) {
                if (generation == clientGeneration.get()) listener.onConnectionFailed()
                return@launch
            } catch (_: SecurityException) {
                if (generation == clientGeneration.get()) listener.onConnectionFailed()
                return@launch
            }
            try {
                socket.connect()
            } catch (_: IOException) {
                runCatching { socket.close() }
                if (generation == clientGeneration.get()) listener.onConnectionFailed()
                return@launch
            }
            if (generation != clientGeneration.get()) {
                runCatching { socket.close() }
                return@launch
            }
            clientSocket = socket
            listener.onMasterConnected(peer)
            try {
                writeLine(socket, MultiUeProtocol.hello())
            } catch (_: IOException) {
                runCatching { socket.close() }
                if (generation == clientGeneration.get()) listener.onMasterLost()
                return@launch
            }
            readMaster(socket, generation)
        }
    }

    fun sendToMaster(line: String) {
        val socket = clientSocket ?: return
        try {
            writeLine(socket, line)
        } catch (_: IOException) {
            // The read loop reports the dropped link.
        }
    }

    fun sendToSlaves(addresses: Set<String>, line: String) {
        if (addresses.isEmpty()) return
        val sockets = synchronized(links) {
            links.filter { it.peer.address in addresses }.map { it.socket }
        }
        sockets.forEach { socket ->
            try {
                writeLine(socket, line)
            } catch (_: IOException) {
                dropLink(socket)
            }
        }
    }

    fun broadcast(line: String) {
        val sockets = synchronized(links) { links.map { it.socket } }
        sockets.forEach { socket ->
            try {
                writeLine(socket, line)
            } catch (_: IOException) {
                dropLink(socket)
            }
        }
    }

    fun close() {
        clientGeneration.incrementAndGet()
        masterStarting = false
        masterJob?.cancel()
        clientJob?.cancel()
        val server = serverSocket
        serverSocket = null
        runCatching { server?.close() }
        closeClientSocket()
        val open = synchronized(links) {
            val copy = links.toList()
            links.clear()
            copy
        }
        open.forEach { runCatching { it.socket.close() } }
        listener.onSlavesChanged(emptyList())
    }

    private fun adoptSlave(socket: BluetoothSocket) {
        val peer = try {
            peerOf(socket.remoteDevice)
        } catch (_: IOException) {
            runCatching { socket.close() }
            return
        } catch (_: SecurityException) {
            runCatching { socket.close() }
            return
        }
        val rejected = synchronized(links) {
            val previous = links.filter { it.peer.address == peer.address }
            previous.forEach { previousLink ->
                links.remove(previousLink)
                runCatching { previousLink.socket.close() }
            }
            if (links.size >= MultiUeProtocol.MAX_SLAVES) {
                true
            } else {
                links.add(SlaveLink(socket, peer))
                false
            }
        }
        if (rejected) {
            try {
                writeLine(socket, MultiUeProtocol.full())
            } catch (_: IOException) {
                // The slave sees the socket close either way.
            }
            runCatching { socket.close() }
            publishSlaves()
            return
        }
        publishSlaves()
        try {
            writeLine(socket, MultiUeProtocol.hello())
            writeLine(socket, MultiUeProtocol.session(listener.runMode()))
            writeLine(socket, listener.panelsLine())
            listener.scrollLine()?.let { writeLine(socket, it) }
            listener.zoneLine()?.let { writeLine(socket, it) }
            listener.zoneNameLine()?.let { writeLine(socket, it) }
            listener.zoneHistogramLine()?.let { writeLine(socket, it) }
            listener.planLine()?.let { plan ->
                writeLine(socket, plan)
                listener.pinLines().forEach { writeLine(socket, it) }
            }
        } catch (_: IOException) {
            dropLink(socket)
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
                while (isActive) {
                    val line = reader.readLine() ?: break
                    when (val message = MultiUeProtocol.parse(line)) {
                        is MultiUeMessage.Sync -> listener.onSlaveSync(peer.address, message.on)
                        else -> Unit
                    }
                }
            } catch (_: IOException) {
                // Slave hung up.
            } finally {
                dropLink(socket)
            }
        }
    }

    private fun readMaster(socket: BluetoothSocket, generation: Int) {
        var reportLost = true
        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream, Charsets.UTF_8))
            while (generation == clientGeneration.get()) {
                val line = reader.readLine() ?: break
                when (val message = MultiUeProtocol.parse(line)) {
                    is MultiUeMessage.Passive -> listener.onPassiveCommand(message.on)
                    is MultiUeMessage.Session -> listener.onSession(message.mode)
                    MultiUeMessage.Clear -> listener.onClearGraphs()
                    MultiUeMessage.ClearHistory -> listener.onClearHistory()
                    is MultiUeMessage.Zone -> listener.onZone(message.running, message.wholeTest)
                    is MultiUeMessage.ZoneName -> listener.onZoneName(message.suffix)
                    is MultiUeMessage.Plan -> listener.onPlan(message.jpegBase64)
                    is MultiUeMessage.PinAdd -> listener.onPinAdd(message.x, message.y)
                    MultiUeMessage.PinUndo -> listener.onPinUndo()
                    MultiUeMessage.PinClear -> listener.onPinClear()
                    MultiUeMessage.ZoneClear -> listener.onZoneClear()
                    is MultiUeMessage.ZoneSave -> listener.onZoneSave(message.includeHistogram)
                    is MultiUeMessage.ZoneHistogram -> listener.onZoneHistogram(message.on)
                    is MultiUeMessage.Profile -> listener.onProfile(message.apply, message.payload)
                    is MultiUeMessage.Panels -> listener.onPanels(message.open)
                    is MultiUeMessage.Scroll -> listener.onScroll(message.fraction)
                    is MultiUeMessage.Settings -> listener.onSettings(message.payload)
                    MultiUeMessage.Full -> {
                        reportLost = false
                        listener.onMasterFull()
                        return
                    }
                    else -> Unit
                }
            }
        } catch (_: IOException) {
            // Master hung up or the radio dropped.
        } finally {
            val current = clientSocket === socket && generation == clientGeneration.get()
            runCatching { socket.close() }
            if (clientSocket === socket) clientSocket = null
            if (current && reportLost) listener.onMasterLost()
        }
    }

    private fun dropLink(socket: BluetoothSocket) {
        val removed = synchronized(links) { links.removeAll { it.socket == socket } }
        runCatching { socket.close() }
        if (removed) publishSlaves()
    }

    private fun publishSlaves() {
        val peers = synchronized(links) { links.map { it.peer } }
        listener.onSlavesChanged(peers)
    }

    private fun closeClientSocket() {
        val socket = clientSocket
        clientSocket = null
        runCatching { socket?.close() }
    }

    private fun writeLine(socket: BluetoothSocket, line: String) {
        val payload = (line + "\n").toByteArray(Charsets.UTF_8)
        synchronized(socket) {
            socket.outputStream.write(payload)
            socket.outputStream.flush()
        }
    }

    private class SlaveLink(
        val socket: BluetoothSocket,
        val peer: MultiUePeer
    )
}
