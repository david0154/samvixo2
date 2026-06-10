package com.example.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class WifiMeshManager(
    private val context: Context,
    private val onMessageReceived: (senderName: String, text: String, isGroup: Boolean) -> Unit
) {
    private val TAG = "WifiMeshManager"
    private val SERVICE_TYPE = "_chatmesh._tcp"
    
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverSocket: ServerSocket? = null
    private var localPort: Int = -1
    private var isServerRunning = false
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    // State flow of discovered active mesh nodes
    private val _discoveredNodes = MutableStateFlow<List<MeshNode>>(emptyList())
    val discoveredNodes: StateFlow<List<MeshNode>> = _discoveredNodes.asStateFlow()

    private val _isMeshActive = MutableStateFlow(false)
    val isMeshActive: StateFlow<Boolean> = _isMeshActive.asStateFlow()

    data class MeshNode(
        val serviceName: String,
        val hostAddress: String?,
        val port: Int,
        val isSelf: Boolean = false
    )

    /**
     * Start local mesh node: Sets up a TCP Server Socket on an ephemeral port
     * and registers it on Wi-Fi multicast network via NsdManager.
     */
    fun startMesh(displayName: String) {
        if (_isMeshActive.value) return
        Log.d(TAG, "Starting Local Mesh Node as: $displayName")
        
        scope.launch {
            try {
                // 1. Start TCP Server Socket
                serverSocket = ServerSocket(0) // Bind to any available port
                localPort = serverSocket!!.localPort
                isServerRunning = true
                Log.d(TAG, "Local TCP Server listening on port: $localPort")
                
                // Handle compilation & socket loop
                launch {
                    while (isServerRunning) {
                        try {
                            val clientSocket = serverSocket?.accept() ?: break
                            handleIncomingConnection(clientSocket)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error accepting client socket: ${e.message}")
                        }
                    }
                }

                // 2. Register service via NSD
                val serviceInfo = NsdServiceInfo().apply {
                    serviceName = "WC_$displayName"
                    serviceType = SERVICE_TYPE
                    setPort(localPort)
                }

                registrationListener = object : NsdManager.RegistrationListener {
                    override fun onServiceRegistered(info: NsdServiceInfo) {
                        Log.d(TAG, "NSD Service registered successfully: ${info.serviceName}")
                        _isMeshActive.value = true
                    }

                    override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "NSD Registration failed: $errorCode")
                        stopMesh()
                    }

                    override fun onServiceUnregistered(info: NsdServiceInfo) {
                        Log.d(TAG, "NSD Service unregistered: ${info.serviceName}")
                        _isMeshActive.value = false
                    }

                    override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                        Log.e(TAG, "NSD Unregistration failed: $errorCode")
                    }
                }

                nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
                
                // 3. Start scanning for nearby nodes
                startDiscovery()
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start local mesh socket server: ${e.message}")
                stopMesh()
            }
        }
    }

    /**
     * Scan Wi-Fi for other registered WhatsApp Connect nodes
     */
    private fun startDiscovery() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed to start: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed to stop: $errorCode")
                nsdManager.stopServiceDiscovery(this)
            }

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "NSD Discovery started successfully.")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "NSD Discovery stopped.")
                _discoveredNodes.value = emptyList()
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "NSD Service found: ${serviceInfo.serviceName}")
                
                if (serviceInfo.serviceType != SERVICE_TYPE) return
                
                if (serviceInfo.serviceName.startsWith("WC_")) {
                    // Resolve details of found service to get IP/port
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                        }

                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                            // Verify it's not our own registered service
                            if (resolvedInfo.port == localPort) {
                                return
                            }
                            Log.d(TAG, "Resolved node IP: ${resolvedInfo.host?.hostAddress}, Port: ${resolvedInfo.port}")
                            
                            val cleanedName = resolvedInfo.serviceName.removePrefix("WC_")
                            val node = MeshNode(
                                serviceName = cleanedName,
                                hostAddress = resolvedInfo.host?.hostAddress,
                                port = resolvedInfo.port
                            )
                            
                            val currentList = _discoveredNodes.value.toMutableList()
                            if (currentList.none { it.serviceName == node.serviceName }) {
                                currentList.add(node)
                                _discoveredNodes.value = currentList
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
                val nameToRemove = serviceInfo.serviceName.removePrefix("WC_")
                _discoveredNodes.value = _discoveredNodes.value.filter { it.serviceName != nameToRemove }
            }
        }

        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    /**
     * Stop server socket, unregister NSD service, and clear states.
     */
    fun stopMesh() {
        Log.d(TAG, "Shutting down Wifi Mesh local node")
        isServerRunning = false
        
        try {
            registrationListener?.let { nsdManager.unregisterService(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering NSD: ${e.message}")
        }
        registrationListener = null

        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery NSD: ${e.message}")
        }
        discoveryListener = null

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket: ${e.message}")
        }
        serverSocket = null
        localPort = -1
        
        _discoveredNodes.value = emptyList()
        _isMeshActive.value = false
    }

    /**
     * Handle incoming peer-to-peer TCP messaging stream
     */
    private fun handleIncomingConnection(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val inputLine = reader.readLine()
                if (inputLine != null) {
                    // Packet schema: "SenderName:MessageText"
                    val parts = inputLine.split(":", limit = 2)
                    if (parts.size == 2) {
                        val senderName = parts[0]
                        val messageText = parts[1]
                        onMessageReceived(senderName, messageText, false)
                    }
                }
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error processing socket payload: ${e.message}")
            }
        }
    }

    /**
     * Send message to a specific peer Node over Wi-Fi sockets
     */
    fun sendMessageToNode(node: MeshNode, senderName: String, messageText: String, onCompleted: (Boolean) -> Unit) {
        scope.launch {
            var success = false
            try {
                val host = node.hostAddress ?: "localhost"
                Log.d(TAG, "Connecting to Socket endpoint: $host on port ${node.port}")
                withContext(Dispatchers.IO) {
                    val socket = Socket(host, node.port)
                    socket.soTimeout = 5000 // 5 seconds timeout
                    val writer = PrintWriter(socket.getOutputStream(), true)
                    writer.println("$senderName:$messageText")
                    writer.close()
                    socket.close()
                }
                success = true
                Log.d(TAG, "Message payload sent successfully to: ${node.serviceName}")
            } catch (e: Exception) {
                Log.e(TAG, "Could not send message to peer socket: ${e.message}")
            }
            withContext(Dispatchers.Main) {
                onCompleted(success)
            }
        }
    }

    /**
     * Broadcast message to ALL discovered active nodes on the mesh network
     */
    fun broadcastMessage(senderName: String, messageText: String, onSentCount: (Int) -> Unit) {
        scope.launch {
            val targets = _discoveredNodes.value
            var successCount = 0
            val jobs = targets.map { node ->
                launch {
                    sendMessageToNode(node, senderName, messageText) { ok ->
                        if (ok) successCount++
                    }
                }
            }
            jobs.joinAll()
            withContext(Dispatchers.Main) {
                onSentCount(successCount)
            }
        }
    }

    /**
     * Simulated Mesh Injector (Mock mesh fallback) 
     * Essential for proving offline functionality inside single-emulator sandboxes where multicast loopback in router is disabled.
     */
    fun simulatePreexistingNodes() {
        val simulatedNodes = listOf(
            MeshNode("Mesh Node Alpha (P2P)", "192.168.1.121", 5400),
            MeshNode("Secure Bunker Node", "192.168.1.189", 5401),
            MeshNode("Rescue Helicopter 3", "192.168.1.250", 5402)
        )
        _discoveredNodes.value = simulatedNodes
        _isMeshActive.value = true
    }
}
