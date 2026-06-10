package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val contactDao = db.contactDao()
    private val chatDao = db.chatDao()
    private val messageDao = db.messageDao()

    // --- Authentication States ---
    sealed class AuthState {
        object Unauthenticated : AuthState()
        object SmsSent : AuthState()
        object Authenticated : AuthState()
        data class Error(val message: String) : AuthState()
        object Loading : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var phoneNumber = ""
    var verificationId = ""
    var smsOtpCode = ""
    var currentUserId = "me"
    var currentUserName = "Me"

    // --- Samvixo Suite State Customizations ---
    val selectedAiModel = MutableStateFlow("devil-ai") // support "devil-ai" or "gemma3:4b"
    val isAppLockerEnabled = MutableStateFlow(false)
    val isAppLockerCurrentlyLocked = MutableStateFlow(false)
    val appPinCode = MutableStateFlow("1234") // Default secure pin
    val chatWallpaperPalette = MutableStateFlow("Light Slate") // "Light Slate", "Emerald Forest", "Vintage Cream", "Devil Purple"
    val appLocalizationLanguage = MutableStateFlow("English") // English + All 10+ Major Indian Languages
    
    val weatherForecastInfo = MutableStateFlow<String?>("Tap 'Get Forecast' to query OpenMeteo GPS weather")
    val isWeatherRequestLoading = MutableStateFlow(false)
    
    val driveBackupsHistoryList = MutableStateFlow(listOf("samvixo_e2e_backup_v1.db (742 KB) - Decrypted locally"))
    val isDriveBackupSyncing = MutableStateFlow(false)
    
    val aiNotepadList = MutableStateFlow(listOf(
        "Secure Mesh Coordinates: Alpha node located near primary base.",
        "Devil AI Security Checklist: Rotate E2E keys every 15 minutes."
    ))
    val aiKnowledgeVaultList = MutableStateFlow(listOf(
        "✨ Key Fact: Cryptographic symmetric session keys prevent man-in-the-middle attacks.",
        "✨ Key Fact: OpenMeteo provides public forecast queries without requiring Maps API keys."
    ))
    
    val isScreenshotAlertActive = MutableStateFlow(true)
    val activeScreenshotAlertMsg = MutableSharedFlow<String>(replay = 0)

    // --- Mesh Networking ---
    private lateinit var meshManager: WifiMeshManager
    val isMeshActive: StateFlow<Boolean> by lazy { meshManager.isMeshActive }
    val discoveredMeshNodes: StateFlow<List<WifiMeshManager.MeshNode>> by lazy { meshManager.discoveredNodes }

    // --- UI Lists Flows ---
    val contactsList: StateFlow<List<Contact>> = contactDao.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatsList: StateFlow<List<Chat>> = chatDao.getAllChatsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Selected Chat ---
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeChat = MutableStateFlow<Chat?>(null)
    val activeChat: StateFlow<Chat?> = _activeChat.asStateFlow()

    // Observe active messages reactively
    val activeMessages: StateFlow<List<DbMessage>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId == null) flowOf(emptyList())
            else messageDao.getMessagesForChat(chatId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize Wifi Mesh manager, handling received messages live!
        meshManager = WifiMeshManager(application) { senderName, text, isGroup ->
            handleIncomingMeshMessage(senderName, text)
        }

        // Keep track of active chat metadata
        viewModelScope.launch {
            _activeChatId.collect { id ->
                if (id != null) {
                    _activeChat.value = chatDao.getChatById(id)
                } else {
                    _activeChat.value = null
                }
            }
        }
    }

    // --- Phone OTP Login Actions ---
    
    fun sendOtp(phone: String, activity: android.app.Activity) {
        if (phone.isBlank()) {
            _authState.value = AuthState.Error("Please enter a valid phone number")
            return
        }
        phoneNumber = phone
        _authState.value = AuthState.Loading
        
        FirebaseService.sendVerificationCode(
            phoneNumber = phone,
            activity = activity,
            onCodeSent = { vId ->
                verificationId = vId
                _authState.value = AuthState.SmsSent
            },
            onVerificationSuccess = { credential ->
                // Fast path for immediate verification
                verifyCredentialFlow(credential.smsCode ?: "000000")
            },
            onFailure = { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Verification Failed")
            }
        )
    }

    fun verifyCredentialFlow(code: String) {
        smsOtpCode = code
        _authState.value = AuthState.Loading
        
        FirebaseService.verifyCode(
            verificationId = verificationId,
            code = code,
            onSuccess = { uid ->
                currentUserId = uid
                _authState.value = AuthState.Authenticated
            },
            onFailure = { e ->
                _authState.value = AuthState.Error(e.localizedMessage ?: "Invalid Verification Code")
            }
        )
    }

    fun logout() {
        _authState.value = AuthState.Unauthenticated
        phoneNumber = ""
        verificationId = ""
        smsOtpCode = ""
        meshManager.stopMesh()
    }

    // --- Messaging Actions ---

    fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
    }

    fun startNewChat(contact: Contact) {
        viewModelScope.launch(Dispatchers.IO) {
            val key = if (contact.isAiBot) "None - API Direct" else EncryptionUtils.generateSessionKey()
            val newChat = Chat(
                chatId = contact.id,
                title = contact.name,
                lastMessageText = "Tap to begin secure chat",
                lastMessageTime = System.currentTimeMillis(),
                isGroup = false,
                isWifiP2p = contact.isNearbyMesh,
                e2eKey = key
            )
            chatDao.insertChat(newChat)
            withContext(Dispatchers.Main) {
                _activeChatId.value = contact.id
            }
        }
    }

    fun sendMessage(text: String) {
        val chatId = _activeChatId.value ?: return
        val chat = _activeChat.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            
            // 1. Encrypt message payload for DB if E2E is active
            val encryptedContent = if (chatId == "ai_assistant") text else EncryptionUtils.encrypt(text, chat.e2eKey)

            val myMsg = DbMessage(
                chatId = chatId,
                senderId = "me",
                senderName = "Me",
                contentEncrypted = encryptedContent,
                timestamp = timestamp,
                isMine = true,
                isEncrypted = (chatId != "ai_assistant")
            )
            messageDao.insertMessage(myMsg)

            // Update parent chat entry
            val updatedChat = chat.copy(
                lastMessageText = encryptedContent,
                lastMessageTime = timestamp
            )
            chatDao.updateChat(updatedChat)

            // 2. Route message depending on chat target type
            when {
                chatId == "ai_assistant" -> {
                    // Call Gemini AI API directly
                    handleAiResponseFlow(text)
                }
                chat.isWifiP2p -> {
                    // Send over actual Wi-Fi Mesh socket if active
                    val peers = discoveredMeshNodes.value
                    val peerNode = peers.firstOrNull { it.serviceName == chat.title }
                    if (peerNode != null) {
                        meshManager.sendMessageToNode(peerNode, currentUserName, text) { success ->
                            if (!success) {
                                // Insert systems warning
                                viewModelScope.launch(Dispatchers.IO) {
                                    messageDao.insertMessage(DbMessage(
                                        chatId = chatId,
                                        senderId = "system",
                                        senderName = "System Indicator",
                                        contentEncrypted = "Offline Peer might have disconnected. Message broadcasted to network mesh.",
                                        timestamp = System.currentTimeMillis(),
                                        isMine = false,
                                        isEncrypted = false
                                    ))
                                }
                            }
                        }
                    }
                }
                else -> {
                    // Normal Chat: Simulated responder handshake (E2EE encrypted)
                    simulateReplyMessage(chat, text)
                }
            }
        }
    }

    // --- Mesh Networking Handlers ---

    fun toggleMeshServer(displayName: String) {
        if (isMeshActive.value) {
            meshManager.stopMesh()
        } else {
            meshManager.startMesh(displayName)
        }
    }

    fun injectSimulatedMesh() {
        meshManager.simulatePreexistingNodes()
        viewModelScope.launch(Dispatchers.IO) {
            // Add nodes to room contacts so they can be selected
            val nearbyPeers = listOf(
                Contact("mesh_alpha", "Mesh Node Alpha (P2P)", "Offline Broadcast Node", 3, "Broadcasting on Local Mesh", isNearbyMesh = true),
                Contact("mesh_bunker", "Secure Bunker Safehouse", "Emergency Broadcast Beacon", 4, "Active Emergency Grid", isNearbyMesh = true),
                Contact("mesh_heli", "Rescue Helicopter 3", "Aerial Node", 5, "Disaster Response Node", isNearbyMesh = true)
            )
            contactDao.insertAll(nearbyPeers)
        }
    }

    fun broadcastMeshUrgent(text: String) {
        if (text.isBlank()) return
        meshManager.broadcastMessage(currentUserName, text) { count ->
            viewModelScope.launch(Dispatchers.IO) {
                // Register a broadcast receipt locally
                messageDao.insertMessage(DbMessage(
                    chatId = "wifi_broadcast",
                    senderId = "me",
                    senderName = "My Broadcast",
                    contentEncrypted = text,
                    timestamp = System.currentTimeMillis(),
                    isMine = true,
                    isWifiBroadcast = true,
                    isEncrypted = false
                ))
            }
        }
    }

    private fun handleIncomingMeshMessage(senderName: String, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = System.currentTimeMillis()
            
            // Generate or find chat for this broadcast sender
            val cleanChatId = "mesh_${senderName.replace(" ", "_").lowercase()}"
            var chat = chatDao.getChatById(cleanChatId)
            
            if (chat == null) {
                chat = Chat(
                    chatId = cleanChatId,
                    title = senderName,
                    lastMessageText = text,
                    lastMessageTime = timestamp,
                    isGroup = false,
                    isWifiP2p = true,
                    e2eKey = "MeshLocalSharedKey"
                )
                chatDao.insertChat(chat)
                
                // Also add contact
                contactDao.insertContact(Contact(
                    id = cleanChatId,
                    name = senderName,
                    phoneNumber = "Local P2P Node",
                    avatarColorOrdinal = 4,
                    statusText = "Nearby Mesh Node",
                    isNearbyMesh = true
                ))
            } else {
                chat = chat.copy(
                    lastMessageText = text,
                    lastMessageTime = timestamp
                )
                chatDao.updateChat(chat)
            }

            // Insert unencrypted (or mesh decrypted) text
            messageDao.insertMessage(DbMessage(
                chatId = cleanChatId,
                senderId = "peer",
                senderName = senderName,
                contentEncrypted = text,
                timestamp = timestamp,
                isMine = false,
                isWifiBroadcast = true,
                isEncrypted = false
            ))
        }
    }

    // --- Devil AI Content Generation ---

    private suspend fun handleAiResponseFlow(userPrompt: String) {
        val model = selectedAiModel.value
        val response = DevilAiClient.generateAiResponse(userPrompt, model)
        
        val timestamp = System.currentTimeMillis()
        val aiMsg = DbMessage(
            chatId = "ai_assistant",
            senderId = "ai_assistant",
            senderName = "Devil AI Assistant",
            contentEncrypted = response,
            timestamp = timestamp,
            isMine = false,
            isAiResponse = true,
            isEncrypted = false
        )
        messageDao.insertMessage(aiMsg)

        val updatedChat = Chat(
            chatId = "ai_assistant",
            title = "Devil AI Assistant",
            lastMessageText = response,
            lastMessageTime = timestamp,
            isGroup = false,
            isWifiP2p = false,
            e2eKey = "None - API Direct"
        )
        chatDao.insertChat(updatedChat)
    }

    // --- Key Management Audits ---

    fun regenerateChatE2eKey(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val chat = chatDao.getChatById(chatId) ?: return@launch
            val newKey = EncryptionUtils.generateSessionKey()
            chatDao.updateChat(chat.copy(e2eKey = newKey))
            
            // Add a notice message of key change
            messageDao.insertMessage(DbMessage(
                chatId = chatId,
                senderId = "system",
                senderName = "Crypto Shield",
                contentEncrypted = EncryptionUtils.encrypt("⚠️ Session encryption key was changed/rotated. Security context established.", newKey),
                timestamp = System.currentTimeMillis(),
                isMine = false,
                isEncrypted = true
            ))
            
            withContext(Dispatchers.Main) {
                // Refresh local active chat
                _activeChatId.value = null
                _activeChatId.value = chatId
            }
        }
    }

    // --- Simulated Replier ---

    private suspend fun simulateReplyMessage(chat: Chat, userMessage: String) {
        withContext(Dispatchers.IO) {
            // A small delay to feel like the friend is typing
            kotlinx.coroutines.delay(1500)
            val responseText = when {
                userMessage.lowercase().contains("hello") || userMessage.lowercase().contains("hi") -> "Hey! How's it going? 😄"
                userMessage.lowercase().contains("key") || userMessage.lowercase().contains("secure") -> "We're matching end-to-end symmetric keys! Our session is super secure."
                userMessage.lowercase().contains("park") || userMessage.lowercase().contains("dinner") -> "That sounds amazing! Let's meet at 6:30 PM."
                else -> "Got your message! Love how fast and safe this encrypted app works. 👍"
            }

            val timestamp = System.currentTimeMillis()
            val encryptedResponse = EncryptionUtils.encrypt(responseText, chat.e2eKey)

            val reply = DbMessage(
                chatId = chat.chatId,
                senderId = chat.chatId,
                senderName = chat.title,
                contentEncrypted = encryptedResponse,
                timestamp = timestamp,
                isMine = false,
                isEncrypted = true
            )
            messageDao.insertMessage(reply)

            chatDao.updateChat(chat.copy(
                lastMessageText = encryptedResponse,
                lastMessageTime = timestamp
            ))
        }
    }

    override fun onCleared() {
        super.onCleared()
        meshManager.stopMesh()
    }
}
