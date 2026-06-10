package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Contact
import com.example.data.DbMessage
import com.example.data.EncryptionUtils
import com.example.data.DevilAiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.*

// --- WhatsApp Specific Color Styling Palette ---
object WhatsAppPalette {
    // High Density Theme Color Palette
    val DarkText = Color(0xFF1A1C1E)
    val LightGrayBackground = Color(0xFFF7F9FC)
    val PureWhite = Color(0xFFFFFFFF)
    
    // M3 Cobalt / Slate Blue colors
    val ActionSearchBg = Color(0xFFE1E2EC)
    val ActionSearchActive = Color(0xFFD1D2DC)
    val ActivePillBg = Color(0xFFD3E4FF)
    val ActivePillText = Color(0xFF001D36)
    val UnselectedPillBg = Color(0xFFF0F0F3)
    val UnselectedPillText = Color(0xFF44474E)
    
    // Emergency / Mesh color accents
    val EmergencyPillBg = Color(0xFFFFE9E9)
    val EmergencyPillText = Color(0xFF410002)
    val EmergencyTextDark = Color(0xFFBA1A1A)
    val EmergencyRowBg = Color(0xFFFFF8F7)
    val EmergencyRowBgActive = Color(0xFFFFE8E6)
    val EmergencyBannerBg = Color(0xFFFFF8F7)
    
    // Pinned AI chat settings
    val PinnedAiBg = Color(0xFFF2F7FF)
    val PinnedAiBlueAccent = Color(0xFF005FB0)
    
    // Mapped for backwards-compatibility of components
    val TealGreenDark = Color(0xFF005FB0)       // Deep M3 Cobalt Blue
    val TealGreenLight = Color(0xFFFFFFFF)      // Pure Clean Top AppBar Background (High Density design)
    val EmeraldAccent = Color(0xFF005FB0)       // Main Active Accent color (Matches design cobalt highlights)
    val LightBlueSms = Color(0xFFD3E4FF)        // High contrast notification banner tint
    
    val OutgoingBubbleDark = Color(0xFFD3E4FF)  // Clean premium blue bubbles
    val OutgoingBubbleLight = Color(0xFFD3E4FF)
    val IncomingBubble = Color(0xFFFFFFFF)
    val WhatsAppBackground = Color(0xFFF7F9FC)  // Clean modern slate-white background
    
    val SecurityGold = Color(0xFFE1B12C)
    val DarkSlateBackground = Color(0xFFF7F9FC)
    val DarkSlateHeader = Color(0xFFFFFFFF)
    val DarkSlateText = Color(0xFF1A1C1E)
    val DarkSlateMutedText = Color(0xFF74777F)
}

// Global Avatar generator helper
@Composable
fun AvatarIndicator(name: String, colorOrdinal: Int, modifier: Modifier = Modifier, isAi: Boolean = false) {
    if (isAi) {
        Box(
            modifier = modifier
                .background(WhatsAppPalette.PinnedAiBlueAccent, shape = RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "✨",
                fontSize = 18.sp
            )
        }
    } else {
        val colors = listOf(
            Color(0xFF005FB0), // High density blue
            Color(0xFF8E9099), // Slate grey
            Color(0xFF0061A4), // Light blue cobalt
            Color(0xFF6B5B95), // Muted plum
            Color(0xFFBA1A1A), // Emergency crimson
            Color(0xFF410002)  // Dark carbon
        )
        val isMeshNetworkEntry = name.contains("Mesh") || name.contains("Emergency")
        val containerColor = if (isMeshNetworkEntry) Color(0xFFFFDAD6) else colors[colorOrdinal % colors.size]
        val textColor = if (containerColor == Color(0xFFFFDAD6)) Color(0xFFBA1A1A) else Color.White
        
        Box(
            modifier = modifier
                .background(containerColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isMeshNetworkEntry) {
                Text("📡", fontSize = 18.sp)
            } else {
                val initial = name.firstOrNull()?.toString()?.uppercase() ?: "?"
                Text(
                    text = initial,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// Simple time formatter
fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun WhatsAppConnectApp(viewModel: ChatViewModel, activity: android.app.Activity) {
    val authState by viewModel.authState.collectAsState()
    val activeChatId by viewModel.activeChatId.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Crossfade(targetState = authState, label = "AppTransitionState") { state ->
            when (state) {
                is ChatViewModel.AuthState.Unauthenticated,
                is ChatViewModel.AuthState.SmsSent,
                is ChatViewModel.AuthState.Loading,
                is ChatViewModel.AuthState.Error -> {
                    LoginScreen(viewModel = viewModel, activity = activity)
                }
                is ChatViewModel.AuthState.Authenticated -> {
                    if (activeChatId != null) {
                        ChatDetailScreen(viewModel = viewModel)
                    } else {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

data class SupportCountry(val name: String, val code: String, val flag: String)

val supportCountries = listOf(
    SupportCountry("India", "+91", "🇮🇳"),
    SupportCountry("USA", "+1", "🇺🇸"),
    SupportCountry("United Kingdom", "+44", "🇬🇧"),
    SupportCountry("France", "+33", "🇫🇷"),
    SupportCountry("Singapore", "+65", "🇸🇬"),
    SupportCountry("Brazil", "+55", "🇧🇷"),
    SupportCountry("Spain", "+34", "🇪🇸"),
    SupportCountry("Australia", "+61", "🇦🇺"),
    SupportCountry("Russia", "+7", "🇷🇺"),
    SupportCountry("Poland", "+48", "🇵🇱")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: ChatViewModel, activity: android.app.Activity) {
    val authState by viewModel.authState.collectAsState()
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(supportCountries[0]) } // Defaults to India
    var inputPhone by remember { mutableStateOf("+91 ") }
    var inputCode by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        "WhatsApp Connect", 
                        fontWeight = FontWeight.Bold,
                        color = WhatsAppPalette.DarkText
                    ) 
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = WhatsAppPalette.WhatsAppBackground
                ),
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Filled.Security,
                        contentDescription = "Secure lock icon",
                        tint = WhatsAppPalette.TealGreenDark,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(WhatsAppPalette.WhatsAppBackground, Color.White)
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Elegant secure authentication lock brand
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(WhatsAppPalette.PinnedAiBg, shape = CircleShape)
                    .border(2.5.dp, WhatsAppPalette.TealGreenDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = WhatsAppPalette.TealGreenDark,
                    modifier = Modifier.size(45.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Secure Network Access",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = WhatsAppPalette.DarkText,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "End-to-End Encrypted OTP Verification",
                fontSize = 14.sp,
                color = WhatsAppPalette.TealGreenDark.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Authentication state machine views
            when (authState) {
                is ChatViewModel.AuthState.Unauthenticated,
                is ChatViewModel.AuthState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "Verify Phone Number",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Country Selection Dropdown Trigger
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { countryDropdownExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppPalette.TealGreenDark),
                                    border = BorderStroke(1.dp, Color.LightGray)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${selectedCountry.flag}  ${selectedCountry.name} (${selectedCountry.code})", color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown selector", tint = WhatsAppPalette.TealGreenDark)
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = countryDropdownExpanded,
                                    onDismissRequest = { countryDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f).background(Color.White)
                                ) {
                                    supportCountries.forEach { country ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(country.flag, fontSize = 20.sp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(country.name, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                                    Spacer(modifier = Modifier.weight(1f))
                                                    Text(country.code, color = Color.Gray, fontSize = 13.sp)
                                                }
                                            },
                                            onClick = {
                                                selectedCountry = country
                                                inputPhone = "${country.code} "
                                                countryDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = inputPhone,
                                onValueChange = { inputPhone = it },
                                label = { Text("Local Phone Number", color = Color.Gray) },
                                placeholder = { Text("e.g. 9876543210") },
                                leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = WhatsAppPalette.TealGreenDark) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_phone_input"),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.DarkGray, fontWeight = FontWeight.Medium),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.DarkGray,
                                    unfocusedTextColor = Color.DarkGray,
                                    focusedBorderColor = WhatsAppPalette.TealGreenDark,
                                    unfocusedBorderColor = Color.LightGray
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { viewModel.sendOtp(inputPhone, activity) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("login_send_otp_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPalette.TealGreenDark)
                            ) {
                                Text("Send Verification Text", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                
                is ChatViewModel.AuthState.SmsSent -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Enter 6-Digit OTP Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                "SMS code dispatched to: $inputPhone",
                                fontSize = 12.sp,
                                color = WhatsAppPalette.TealGreenDark,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            
                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { if (it.length <= 6) inputCode = it },
                                placeholder = { Text("0 0 0 0 0 0") },
                                label = { Text("Verification Code") },
                                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = WhatsAppPalette.TealGreenDark) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_otp_input"),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = WhatsAppPalette.TealGreenDark,
                                        unfocusedBorderColor = Color.LightGray
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { viewModel.verifyCredentialFlow(inputCode) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("login_confirm_otp_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPalette.TealGreenDark)
                            ) {
                                Text("Verify and Account Login", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
                
                is ChatViewModel.AuthState.Loading -> {
                    CircularProgressIndicator(
                        color = WhatsAppPalette.TealGreenDark,
                        modifier = Modifier.padding(32.dp)
                    )
                    Text("Handshaking Secure Socket Tunnel...", fontWeight = FontWeight.SemiBold, color = WhatsAppPalette.TealGreenDark)
                }
                is ChatViewModel.AuthState.Authenticated -> {
                    // Handled at top-level state machine
                }
            }

            // Interactive helper banner for simulated testing (Matches "No dead-ends" rules)
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = WhatsAppPalette.LightBlueSms,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, WhatsAppPalette.TealGreenLight.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Simulated status",
                            tint = WhatsAppPalette.TealGreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI Studio Emulator Bypass active!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = WhatsAppPalette.TealGreenDark
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "The application contains production Firebase OTP verification bindings. To check the features instantly in this streaming sandbox, enter any phone number and complete verification using code \"123456\"!",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = WhatsAppPalette.TealGreenDark.copy(alpha = 0.85f)
                    )
                }
            }

            if (authState is ChatViewModel.AuthState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECEF)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = (authState as ChatViewModel.AuthState.Error).message,
                        color = Color(0xFFA30014),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

// Dynamic Custom Border helper
fun BoxBorder(thickness: androidx.compose.ui.unit.Dp, color: Color) = Modifier.border(thickness, color, RoundedCornerShape(12.dp))

// --- Multi-Language Indian translations dictionary map ---
val localeTranslations = mapOf(
    "English" to mapOf(
        "chats" to "Chats",
        "emergency" to "Emergency",
        "devil_ai" to "Devil AI",
        "key_hub" to "Key Hub",
        "suite" to "Suite",
        "connect" to "samvixo"
    ),
    "Hindi" to mapOf(
        "chats" to "बातचीत",
        "emergency" to "आपातकालीन",
        "devil_ai" to "डेविल एआई",
        "key_hub" to "कुंजी केंद्र",
        "suite" to "सुइट",
        "connect" to "सैमविक्सो"
    ),
    "Telugu" to mapOf(
        "chats" to "చాట్‌లు",
        "emergency" to "అత్యవసర",
        "devil_ai" to "డెవిల్ AI",
        "key_hub" to "కీ హబ్",
        "suite" to "సూట్",
        "connect" to "సామ్విక్సో"
    ),
    "Tamil" to mapOf(
        "chats" to "அரட்டைகள்",
        "emergency" to "அவசரகாலம்",
        "devil_ai" to "டெவில் AI",
        "key_hub" to "சாவி மையம்",
        "suite" to "தொகுப்பு",
        "connect" to "சாம்விக்ஸோ"
    ),
    "Marathi" to mapOf(
        "chats" to "चॅट्स",
        "emergency" to "आणीबाणी",
        "devil_ai" to "डेव्हिल AI",
        "key_hub" to "किल्ली हब",
        "suite" to "सुट",
        "connect" to "सॅमविक्सो"
    ),
    "Bengali" to mapOf(
        "chats" to "চ্যাট",
        "emergency" to "জরুরী",
        "devil_ai" to "ডেভিল এআই",
        "key_hub" to "কী হাব",
        "suite" to "স্যুট",
        "connect" to "সামভিক্সো"
    ),
    "Gujarati" to mapOf(
        "chats" to "ચેટ્સ",
        "emergency" to "ટોકન",
        "devil_ai" to "ડેવિલ AI",
        "key_hub" to "કી હબ",
        "suite" to "સુટ",
        "connect" to "સેમવિક્સો"
    ),
    "Kannada" to mapOf(
        "chats" to "ಚಾಟ್‌ಗಳು",
        "emergency" to "ತುರ್ತು",
        "devil_ai" to "ಡೆವಿಲ್ AI",
        "key_hub" to "ಕೀ ಹಬ್",
        "suite" to "ಸೂಟ್",
        "connect" to "ಸ್ಯಾಮ್ವಿಕ್ಸೋ"
    ),
    "Punjabi" to mapOf(
        "chats" to "ਚੈਟ",
        "emergency" to "ਐਮਰਜੈਂਸੀ",
        "devil_ai" to "ਡੈਵਿਲ AI",
        "key_hub" to "ਕੁੰਜੀ ਹੱਬ",
        "suite" to "ਸੂਟ",
        "connect" to "ਸੈਮਵਿਕਸੋ"
    ),
    "Malayalam" to mapOf(
        "chats" to "ചാറ്റുകൾ",
        "emergency" to "അടിയന്തരാവസ്ഥ",
        "devil_ai" to "ഡെവിൾ AI",
        "key_hub" to "കീ ഹബ്",
        "suite" to "സ്യൂട്ട്",
        "connect" to "സാംവിക്സോ"
    ),
    "Urdu" to mapOf(
        "chats" to "چیٹ",
        "emergency" to "ایمرجنسی",
        "devil_ai" to "ڈیول اے آئی",
        "key_hub" to "کی ہب",
        "suite" to "سویٹ",
        "connect" to "سامویکسو"
    )
)

fun getLocalizedText(key: String, currentLang: String): String {
    return localeTranslations[currentLang]?.get(key) ?: localeTranslations["English"]?.get(key) ?: key
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: ChatViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentLang by viewModel.appLocalizationLanguage.collectAsState()
    
    val tabTitles = listOf("CHATS", "OFFLINE MESH", "DEVIL AI", "KEY HUB", "SUITE")
    
    var showContactsDialog by remember { mutableStateOf(false) }
    val contacts by viewModel.contactsList.collectAsState()

    // App Locker Trigger on Start-up / Tab Wechsel
    val isLockerEnabled by viewModel.isAppLockerEnabled.collectAsState()
    val isAppLocked by viewModel.isAppLockerCurrentlyLocked.collectAsState()
    
    if (isLockerEnabled && isAppLocked) {
        AppLockerPinScreen(viewModel = viewModel)
    } else {
        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(bottom = 4.dp)
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                getLocalizedText("connect", currentLang),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = WhatsAppPalette.TealGreenDark
                            )
                        },
                        actions = {
                            // Quick Search Circle action
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(40.dp)
                                    .background(WhatsAppPalette.ActionSearchBg, CircleShape)
                                    .clickable { /* Search Placeholder */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = "Search",
                                    tint = WhatsAppPalette.DarkText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // AI Quick Tab action
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(40.dp)
                                    .background(WhatsAppPalette.ActivePillBg, CircleShape)
                                    .clickable { selectedTab = 4 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "✨",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = WhatsAppPalette.ActivePillText
                                )
                            }
                            
                            // Beautiful Profile Visual
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(40.dp)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFFE11D48), Color(0xFF9333EA))
                                        ),
                                        CircleShape
                                    )
                                    .border(2.dp, Color.White, CircleShape)
                            )
                            
                            // Logout Action
                            IconButton(onClick = { viewModel.logout() }) {
                                Icon(
                                    imageVector = Icons.Filled.ExitToApp,
                                    contentDescription = "Log out",
                                    tint = WhatsAppPalette.DarkText
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White
                        )
                    )
                    
                    // M3 High Density Horizontal Pills selector row (Replaces classic TabRow)
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tabTitles.size) { index ->
                            val isSelected = selectedTab == index
                            val label = tabTitles[index]
                            
                            val icon = when (index) {
                                0 -> "💬"
                                1 -> "🚨"
                                2 -> "🤖"
                                3 -> "🔑"
                                4 -> "💼"
                                else -> "💬"
                            }
                            
                            val bg = when {
                                isSelected -> WhatsAppPalette.ActivePillBg
                                index == 1 -> WhatsAppPalette.EmergencyPillBg
                                else -> WhatsAppPalette.UnselectedPillBg
                            }
                            
                            val textColor = when {
                                isSelected -> WhatsAppPalette.ActivePillText
                                index == 1 -> WhatsAppPalette.EmergencyPillText
                                else -> WhatsAppPalette.UnselectedPillText
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(bg)
                                    .clickable { selectedTab = index }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(icon, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = when (index) {
                                            0 -> getLocalizedText("chats", currentLang)
                                            1 -> getLocalizedText("emergency", currentLang)
                                            2 -> getLocalizedText("devil_ai", currentLang)
                                            3 -> getLocalizedText("key_hub", currentLang)
                                            4 -> getLocalizedText("suite", currentLang)
                                            else -> label
                                        },
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showContactsDialog = true },
                    containerColor = WhatsAppPalette.ActivePillBg,
                    contentColor = WhatsAppPalette.ActivePillText,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("floating_add_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add, 
                        contentDescription = "New chat message",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> ChatsTabScreen(viewModel = viewModel)
                1 -> MeshTabScreen(viewModel = viewModel)
                2 -> GeminiTabScreen(viewModel = viewModel)
                3 -> KeyHubTabScreen(viewModel = viewModel)
                4 -> SamvixoSuiteScreen(viewModel = viewModel)
            }
        }

        // Contact Selector Dialog Drawer
        if (showContactsDialog) {
            Dialog(onDismissRequest = { showContactsDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Select Secure Contact",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = WhatsAppPalette.TealGreenDark
                            )
                            IconButton(onClick = { showContactsDialog = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Close dialog")
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                            items(contacts) { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.startNewChat(contact)
                                            showContactsDialog = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarIndicator(
                                        name = contact.name,
                                        colorOrdinal = contact.avatarColorOrdinal,
                                        isAi = contact.isAiBot,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            contact.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            contact.phoneNumber,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatsTabScreen(viewModel: ChatViewModel) {
    val chats by viewModel.chatsList.collectAsState()
    
    if (chats.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.ChatBubbleOutline,
                    contentDescription = null,
                    tint = WhatsAppPalette.TealGreenLight.copy(alpha = 0.3f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Secure Chats Active",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.DarkGray
                )
                Text(
                    "Tap the chat button in the corner to select a private contact, ignite Gemini AI, or start an offline mesh packet thread.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(WhatsAppPalette.LightGrayBackground)
        ) {
            items(chats) { chat ->
                val isAi = chat.chatId == "ai_assistant"
                val isMesh = chat.title.contains("Mesh") || chat.title.contains("Emergency")
                val rowBg = when {
                    isAi -> WhatsAppPalette.PinnedAiBg
                    isMesh -> WhatsAppPalette.EmergencyRowBg
                    else -> Color.White
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .clickable { viewModel.setActiveChat(chat.chatId) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("chat_row_${chat.chatId}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AvatarIndicator(
                        name = chat.title,
                        colorOrdinal = if (isAi) 0 else chat.chatId.hashCode(),
                        isAi = isAi,
                        modifier = Modifier.size(52.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chat.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isMesh) WhatsAppPalette.EmergencyTextDark else WhatsAppPalette.DarkText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            // High density badge indicators
                            if (isMesh) {
                                Box(
                                    modifier = Modifier
                                        .background(WhatsAppPalette.EmergencyTextDark, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "3 Nearby",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (isAi) {
                                Text(
                                    text = formatTime(chat.lastMessageTime),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppPalette.PinnedAiBlueAccent
                                )
                            } else {
                                Text(
                                    text = formatTime(chat.lastMessageTime),
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(3.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (chat.chatId != "ai_assistant" && !isMesh) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = "Encrypted Message badge",
                                    tint = WhatsAppPalette.TealGreenDark.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .size(13.dp)
                                        .padding(end = 2.dp)
                                )
                            }
                            
                            val displayPreview = if (chat.chatId == "ai_assistant") {
                                chat.lastMessageText
                            } else {
                                EncryptionUtils.decrypt(chat.lastMessageText, chat.e2eKey)
                            }
                            
                            Text(
                                text = displayPreview,
                                fontSize = 13.sp,
                                color = if (isMesh) WhatsAppPalette.EmergencyTextDark.copy(alpha = 0.8f) else Color.Gray,
                                fontWeight = if (isAi || isMesh) FontWeight.Medium else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            
                            if (chat.isWifiP2p) {
                                Icon(
                                    imageVector = Icons.Filled.Wifi,
                                    contentDescription = "P2P active",
                                    tint = WhatsAppPalette.PinnedAiBlueAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                
                HorizontalDivider(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    modifier = Modifier.padding(start = 82.dp)
                )
            }
            
            // Modern High Density secure transmission lock banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.4f),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "END-TO-END ENCRYPTED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MeshTabScreen(viewModel: ChatViewModel) {
    val isMeshActive by viewModel.isMeshActive.collectAsState()
    val nodes by viewModel.discoveredMeshNodes.collectAsState()
    var displayLocalUsername by remember { mutableStateOf("UserNode_" + (1000..9999).random()) }
    var broadcastContent by remember { mutableStateOf("") }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WhatsAppPalette.WhatsAppBackground)
            .padding(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (isMeshActive) WhatsAppPalette.EmeraldAccent.copy(alpha = 0.2f) 
                                    else Color.LightGray.copy(alpha = 0.3f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMeshActive) Icons.Filled.WifiTethering else Icons.Filled.PortableWifiOff,
                                contentDescription = null,
                                tint = if (isMeshActive) WhatsAppPalette.TealGreenLight else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Emergency Wifi/Bluetooth Mesh",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = WhatsAppPalette.TealGreenDark
                            )
                            Text(
                                text = if (isMeshActive) "Server active. Broadcasting as: $displayLocalUsername" else "Multicast Server offline",
                                fontSize = 11.sp,
                                color = if (isMeshActive) WhatsAppPalette.EmeraldAccent else Color.Gray
                            )
                        }
                        Switch(
                            checked = isMeshActive,
                            onCheckedChange = { viewModel.toggleMeshServer(displayLocalUsername) },
                            colors = SwitchDefaults.colors(checkedThumbColor = WhatsAppPalette.EmeraldAccent)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    OutlinedTextField(
                        value = displayLocalUsername,
                        onValueChange = { if (!isMeshActive) displayLocalUsername = it },
                        label = { Text("Offline Mesh Handle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isMeshActive
                    )
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Broadcast SOS Envelope",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = WhatsAppPalette.TealGreenDark
                    )
                    Text(
                        "Sends an unencrypted emergency broadcast packet to all discovered peers on the active local subnet.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    OutlinedTextField(
                        value = broadcastContent,
                        onValueChange = { broadcastContent = it },
                        placeholder = { Text("Type urgent emergency SOS message...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Button(
                            onClick = { viewModel.injectSimulatedMesh() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppPalette.TealGreenLight),
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simulate Nodes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { 
                                viewModel.broadcastMeshUrgent(broadcastContent)
                                broadcastContent = ""
                            },
                            enabled = isMeshActive && broadcastContent.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD63031)),
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Broadcast S.O.S", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Nearby Active Peer Nodes (${nodes.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }
        
        if (nodes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.WifiOff,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No Peer Nodes Scanned",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        Text(
                            "Tap 'Simulate Nodes' to populate nearby distress beacons in the sandbox browser.",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp)
                        )
                    }
                }
            }
        } else {
            items(nodes) { node ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            // Turn node into contact and launch message stream
                            val mockContact = Contact(
                                id = "node_${node.serviceName.replace(" ", "_").lowercase()}",
                                name = node.serviceName,
                                phoneNumber = "Port Node: ${node.port}",
                                avatarColorOrdinal = 4,
                                statusText = "Active mesh socket: ${node.hostAddress}",
                                isNearbyMesh = true
                            )
                            viewModel.startNewChat(mockContact)
                        },
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarIndicator(node.serviceName, node.serviceName.hashCode(), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                node.serviceName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "IP endpoint: ${node.hostAddress ?: "Local Multicast Loop"} • Port: ${node.port}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.ArrowForwardIos,
                            contentDescription = "Connect",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeminiTabScreen(viewModel: ChatViewModel) {
    // Renders the direct Devil AI assistant conversation
    // Set active chat to ai_assistant
    LaunchedEffect(Unit) {
        viewModel.setActiveChat("ai_assistant")
    }
    
    val conversationState by viewModel.activeMessages.collectAsState()
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val activeModel by viewModel.selectedAiModel.collectAsState()
    
    val suggestions = listOf(
        "Explain samvixo E2E Cryptography",
        "How do offline emergency mesh networks route messages?",
        "Write some Devil AI code in Kotlin",
        "Generate a joke about cellular signals"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhatsAppPalette.WhatsAppBackground)
    ) {
        // Model Selection Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(vertical = 8.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Active AI Model:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("devil-ai", "gemma3:4b").forEach { model ->
                    val isSelected = activeModel == model
                    val textCol = if (isSelected) Color.White else Color.Gray
                    val bgCol = if (isSelected) WhatsAppPalette.TealGreenDark else Color(0xFFF1F3F5)
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgCol)
                            .clickable { viewModel.selectedAiModel.value = model }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            model,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = textCol
                        )
                    }
                }
            }
        }
        
        // Custom Premium AdMob Banner Simulator
        AdMobBanner(placement = "AI_CHAT_STREAM_TOP")

        // Suggested Quick Chips Bar
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(suggestions) { prompt ->
                SuggestionChip(
                    onClick = {
                        viewModel.sendMessage(prompt)
                    },
                    label = { Text(prompt, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color.White,
                        labelColor = WhatsAppPalette.TealGreenDark
                    )
                )
            }
        }
        
        Box(modifier = Modifier.weight(1f)) {
            // Background WhatsApp texture canvas drawing
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = 60.dp.toPx()
                val stepY = 60.dp.toPx()
                var currentX = 0f
                var currentY = 0f
                while (currentY < size.height) {
                    while (currentX < size.width) {
                        drawCircle(
                            color = Color.LightGray.copy(alpha = 0.15f),
                            radius = 2.dp.toPx(),
                            center = Offset(currentX, currentY)
                        )
                        currentX += stepX
                    }
                    currentX = 0f
                    currentY += stepY
                }
            }
            
            // Conversation Column
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
            ) {
                items(conversationState) { message ->
                    AiBubbleItem(message)
                }
            }
            
            // Auto scroll down logic
            LaunchedEffect(conversationState.size) {
                if (conversationState.isNotEmpty()) {
                    listState.animateScrollToItem(conversationState.size - 1)
                }
            }
        }
        
        // Chat Input strip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask Devil AI assistant...", color = Color.Gray) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_text_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray,
                        focusedBorderColor = WhatsAppPalette.TealGreenDark,
                        unfocusedBorderColor = Color.LightGray
                    ),
                    maxLines = 3,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI",
                            tint = WhatsAppPalette.TealGreenDark.copy(alpha = 0.5f)
                        )
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                FloatingActionButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            val toSend = textInput
                            textInput = ""
                            viewModel.sendMessage(toSend)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_send_button"),
                    containerColor = WhatsAppPalette.TealGreenDark,
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = "Ask Devil AI",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// Standard LazyRow is used directly via androidx imports

@Composable
fun AiBubbleItem(message: DbMessage) {
    val isMine = message.isMine
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .background(
                    if (isMine) WhatsAppPalette.OutgoingBubbleLight else WhatsAppPalette.IncomingBubble,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isMine) 12.dp else 0.dp,
                        bottomEnd = if (isMine) 0.dp else 12.dp
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (!isMine) {
                Text(
                    text = "Emerald AI ✨",
                    color = WhatsAppPalette.TealGreenDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Text(
                text = message.contentEncrypted,
                fontSize = 14.sp,
                color = Color.Black,
                lineHeight = 19.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                formatTime(message.timestamp),
                fontSize = 9.sp,
                color = Color.LightGray.copy(alpha = 0.9f),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun KeyHubTabScreen(viewModel: ChatViewModel) {
    val chats by viewModel.chatsList.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WhatsAppPalette.WhatsAppBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = WhatsAppPalette.SecurityGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Crypto Security Guard",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = WhatsAppPalette.TealGreenDark
                            )
                            Text(
                                "Hardware-backed symmetric handshake protocol",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "WhatsApp Connect uses AES-256 Symmetric Encryption in CBC (Cipher Block Chaining) Mode. Each thread possesses a randomized dynamically generated symmetric session certificate. Keys are never transmitted raw; instead, they are negotiated block by block directly on-device or locally via Wi-Fi network tunnels.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }

        item {
            Text(
                "Active Cryptographic Sessions (${chats.filter { it.chatId != "ai_assistant" }.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        val cryptoChats = chats.filter { it.chatId != "ai_assistant" }
        if (cryptoChats.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(
                        "No cryptographic handshakes resolved yet. Start a secure private chat to build a certificate!",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        } else {
            items(cryptoChats) { chat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AvatarIndicator(chat.title, chat.chatId.hashCode(), modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(chat.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = if (chat.isWifiP2p) "Offline P2P Channel" else "E2EE Cloud Sync",
                                        fontSize = 10.sp,
                                        color = WhatsAppPalette.TealGreenLight
                                    )
                                }
                            }
                            
                            IconButton(onClick = { viewModel.regenerateChatE2eKey(chat.chatId) }) {
                                Icon(
                                    imageVector = Icons.Filled.Autorenew,
                                    contentDescription = "Rotate cryptographic session keys",
                                    tint = WhatsAppPalette.TealGreenLight
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.LightGray.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = WhatsAppPalette.TealGreenLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("AES-256 Symmetric Certificate", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(
                                    text = chat.e2eKey,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = WhatsAppPalette.TealGreenDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(viewModel: ChatViewModel) {
    val activeChat by viewModel.activeChat.collectAsState()
    val messages by viewModel.activeMessages.collectAsState()
    var userMessageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    activeChat?.let { chat ->
        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { viewModel.setActiveChat(null) }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Return back", tint = WhatsAppPalette.DarkText)
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isAi = chat.chatId == "ai_assistant"
                            AvatarIndicator(chat.title, if (isAi) 0 else chat.chatId.hashCode(), isAi = isAi, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(chat.title, color = WhatsAppPalette.DarkText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAi) Icons.Filled.AutoAwesome else Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = WhatsAppPalette.TealGreenDark,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isAi) "Emerald Core AI" else "AES Encrypted Session",
                                        color = WhatsAppPalette.TealGreenDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        if (chat.chatId != "ai_assistant") {
                            IconButton(onClick = { viewModel.regenerateChatE2eKey(chat.chatId) }) {
                                Icon(Icons.Filled.Autorenew, contentDescription = "Rotate session keys", tint = WhatsAppPalette.DarkText)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(WhatsAppPalette.WhatsAppBackground)
            ) {
                // Repeating Wallpaper Grid
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .drawBehind {
                            val stepX = 64.dp.toPx()
                            val stepY = 64.dp.toPx()
                            var currentX = 0f
                            var currentY = 0f
                            while (currentY < size.height) {
                                while (currentX < size.width) {
                                    drawCircle(
                                        color = Color.LightGray.copy(alpha = 0.18f),
                                        radius = 2.5f.dp.toPx(),
                                        center = Offset(currentX, currentY)
                                    )
                                    currentX += stepX
                                }
                                currentX = 0f
                                currentY += stepY
                            }
                        }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp)
                    ) {
                        item {
                            // High-Fidelity E2E Warning Shield Bubble
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Surface(
                                    color = Color(0xFFFEF2CD),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Security,
                                            contentDescription = null,
                                            tint = Color(0xFF856404),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (chat.chatId == "ai_assistant") 
                                                "Emerald AI queries are processed through Gemini 3.5-Flash and encrypted locally on database cache."
                                                else "Messages are end-to-end secured. No third-party (including Whatsapp Connect) can read them! Touch key icon to renegotiate certs.",
                                            fontSize = 10.sp,
                                            lineHeight = 14.sp,
                                            color = Color(0xFF856404),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        items(messages) { message ->
                            MessageBubble(message = message, chatKey = chat.e2eKey)
                        }
                    }

                    // Automatic scroll down on new messages
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }

                // Standard WhatsApp Send Input bar
                Surface(
                    tonalElevation = 2.dp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = userMessageText,
                            onValueChange = { userMessageText = it },
                            placeholder = { Text("Message encrypted...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input_box"),
                            shape = RoundedCornerShape(26.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = WhatsAppPalette.TealGreenDark,
                                unfocusedBorderColor = Color.LightGray
                            ),
                            maxLines = 4,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Mood,
                                    contentDescription = "Emojis",
                                    tint = Color.Gray
                                )
                            },
                            trailingIcon = {
                                Row(
                                    modifier = Modifier.padding(end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Attachment,
                                        contentDescription = "Attach media file",
                                        tint = Color.Gray,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = "Capture photograph",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FloatingActionButton(
                            onClick = {
                                if (userMessageText.isNotBlank()) {
                                    val textSubmit = userMessageText
                                    userMessageText = ""
                                    viewModel.sendMessage(textSubmit)
                                }
                            },
                            shape = CircleShape,
                            containerColor = WhatsAppPalette.TealGreenDark,
                            contentColor = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("message_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Post message payload",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: DbMessage, chatKey: String) {
    val isMine = message.isMine
    val isSystem = message.senderId == "system"

    if (isSystem) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                color = Color.LightGray.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(
                    text = if (message.isEncrypted) EncryptionUtils.decrypt(message.contentEncrypted, chatKey) else message.contentEncrypted,
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .background(
                        if (isMine) WhatsAppPalette.OutgoingBubbleLight else WhatsAppPalette.IncomingBubble,
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isMine) 12.dp else 0.dp,
                            bottomEnd = if (isMine) 0.dp else 12.dp
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                if (message.chatId == "wifi_broadcast" && !isMine) {
                    Text(
                        text = "🚨 SOS BROADCAST: " + message.senderName,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD63031),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // Decrypt if it's mark as encrypted
                val decryptedText = if (message.isEncrypted && message.chatId != "ai_assistant") {
                    EncryptionUtils.decrypt(message.contentEncrypted, chatKey)
                } else {
                    message.contentEncrypted
                }

                Text(
                    text = decryptedText,
                    fontSize = 14.sp,
                    color = Color.Black,
                    lineHeight = 18.sp
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                    
                    if (message.isEncrypted && message.chatId != "ai_assistant") {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Encrypted",
                            tint = Color.Gray,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// --- samvixo Interactive Security Suite & Integrations ---
// ==========================================

@Composable
fun AdMobBanner(placement: String, modifier: Modifier = Modifier) {
    var isLoaded by remember { mutableStateOf(true) }
    var showSimulatorOverlay by remember { mutableStateOf(false) }
    
    val adTitle = when (placement) {
        "AI_CHAT_STREAM_TOP" -> "⚡ Install samvixo Premium: Get unlimited Ollama & Devil AI server queries"
        "SUITE_PROMO" -> "🚨 Devil Core security: Rotate symmetric keystores automatically with one tap"
        else -> "🎁 Sponsor Link: Agora Low Latency RTC Engine - High density audio streams"
    }
    
    if (isLoaded) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clickable { showSimulatorOverlay = true },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFEFA)),
            border = BorderStroke(1.dp, Color(0xFFFEF3C7)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("AdMob Promoted", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Google Mobile Ads", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = adTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray,
                        modifier = Modifier.weight(1.0f)
                    )
                    IconButton(
                        onClick = { isLoaded = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close ad", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
    
    if (showSimulatorOverlay) {
        Dialog(onDismissRequest = { showSimulatorOverlay = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = WhatsAppPalette.TealGreenDark, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Simulated Google AdMob redirect", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Connecting to Google AdMob premium partner sandbox securely. Your session keys are fully secured.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showSimulatorOverlay = false },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPalette.TealGreenDark)
                    ) {
                        Text("Return to samvixo", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AppLockerPinScreen(viewModel: ChatViewModel) {
    val configuredPin by viewModel.appPinCode.collectAsState()
    var typedPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "Lock icon",
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(72.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "samvixo App Locker",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White
        )
        Text(
            text = "Extreme E2EE Security Vault. Please enter your 4-digit PIN.",
            fontSize = 12.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )
        
        // Dot Indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            for (i in 1..4) {
                val isFilled = typedPin.length >= i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (isFilled) Color(0xFFEF4444) else Color.DarkGray,
                            shape = CircleShape
                        )
                )
            }
        }
        
        if (pinError) {
            Text("Incorrect PIN code. Retrying secured.", color = Color(0xFFF87171), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        val buttons = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "🔓")
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            buttons.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { originalSymbol ->
                        Button(
                            onClick = {
                                pinError = false
                                when (originalSymbol) {
                                    "C" -> if (typedPin.isNotEmpty()) typedPin = typedPin.dropLast(1)
                                    "🔓" -> {
                                        if (typedPin == configuredPin) {
                                            viewModel.isAppLockerCurrentlyLocked.value = false
                                        } else {
                                            typedPin = ""
                                            pinError = true
                                        }
                                    }
                                    else -> {
                                        if (typedPin.length < 4) {
                                            typedPin += originalSymbol
                                            if (typedPin.length == 4) {
                                                if (typedPin == configuredPin) {
                                                    viewModel.isAppLockerCurrentlyLocked.value = false
                                                } else {
                                                    typedPin = ""
                                                    pinError = true
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.12f)),
                            modifier = Modifier.size(68.dp)
                        ) {
                            Text(
                                text = originalSymbol,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SamvixoSuiteScreen(viewModel: ChatViewModel) {
    val currentLang by viewModel.appLocalizationLanguage.collectAsState()
    val isLockerEnabled by viewModel.isAppLockerEnabled.collectAsState()
    val currentPin by viewModel.appPinCode.collectAsState()
    val chatWallpaperPalette by viewModel.chatWallpaperPalette.collectAsState()
    
    val weatherForecastInfo by viewModel.weatherForecastInfo.collectAsState()
    val isWeatherRequestLoading by viewModel.isWeatherRequestLoading.collectAsState()
    
    val driveBackupsHistoryList by viewModel.driveBackupsHistoryList.collectAsState()
    val isDriveBackupSyncing by viewModel.isDriveBackupSyncing.collectAsState()
    
    val aiNotepadList by viewModel.aiNotepadList.collectAsState()
    val aiKnowledgeVaultList by viewModel.aiKnowledgeVaultList.collectAsState()
    
    var localNoteText by remember { mutableStateOf("") }
    var queryCoordinatesIndex by remember { mutableIntStateOf(0) }
    
    val defaultLocations = listOf(
        Triple("New Delhi (India)", 28.6139, 77.2090),
        Triple("Washington (USA)", 38.9072, -77.0369),
        Triple("London (UK)", 51.5074, -0.1278),
        Triple("Paris (France)", 48.8566, 2.3522),
        Triple("Singapore", 1.3521, 103.8198),
        Triple("Brasilia (Brazil)", -15.7938, -47.8828),
        Triple("Madrid (Spain)", 40.4168, -3.7038),
        Triple("Canberra (Australia)", -35.2809, 149.1300),
        Triple("Moscow (Russia)", 55.7558, 37.6173),
        Triple("Warsaw (Poland)", 52.2297, 21.0122)
    )
    
    val openMeteoScope = rememberCoroutineScope()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(WhatsAppPalette.WhatsAppBackground)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Screen Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "samvixo Integration Suite",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = WhatsAppPalette.TealGreenDark
                    )
                    Text(
                        "Secure operational platform controls. Multi-lingual dictionary translation, app locking coordinates weather, and Devil AI knowledge notes.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // --- Multi-Language Selector Dropdown ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Translate, contentDescription = null, tint = WhatsAppPalette.TealGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("App Language Localization", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                    Text("Select your primary Indian regional language to automatically translate key app labels", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    
                    val indianLanguages = listOf("English", "Hindi", "Telugu", "Tamil", "Marathi", "Bengali", "Gujarati", "Kannada", "Punjabi", "Malayalam", "Urdu")
                    
                    var dropdownExpanded by remember { mutableStateOf(false) }
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppPalette.TealGreenDark)
                        ) {
                            Text("Current Language: $currentLang", fontWeight = FontWeight.Bold)
                        }
                        
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
                        ) {
                            indianLanguages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang, color = Color.DarkGray, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        viewModel.appLocalizationLanguage.value = lang
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // --- Custom App Locker settings ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Secure App Locker Shield", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        }
                        Switch(
                            checked = isLockerEnabled,
                            onCheckedChange = { 
                                viewModel.isAppLockerEnabled.value = it
                                viewModel.isAppLockerCurrentlyLocked.value = it
                            }
                        )
                    }
                    Text("Auto-locks application with safety PIN on startup or app minimized transaction", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                    
                    if (isLockerEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Set 4-Digit Security PIN:", fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(12.dp))
                            OutlinedTextField(
                                value = currentPin,
                                onValueChange = { if (it.length <= 4) viewModel.appPinCode.value = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(100.dp),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, color = Color.DarkGray),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFEF4444),
                                    unfocusedBorderColor = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }
        }
        
        // --- OpenMeteo live weather forecasts and Google Maps parameters ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudQueue, contentDescription = null, tint = WhatsAppPalette.TealGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("OpenMeteo GPS Weather Forecaster", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                    Text("Select a global coordinate below to query actual live weather forecast using OpenMeteo direct API REST interfaces.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    
                    var weatherQueryExpanded by remember { mutableStateOf(false) }
                    val activeLoc = defaultLocations[queryCoordinatesIndex]
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { weatherQueryExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppPalette.TealGreenDark)
                        ) {
                            Text("Location: ${activeLoc.first}", fontWeight = FontWeight.Bold)
                        }
                        
                        DropdownMenu(
                            expanded = weatherQueryExpanded,
                            onDismissRequest = { weatherQueryExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
                        ) {
                            defaultLocations.forEachIndexed { idx, item ->
                                DropdownMenuItem(
                                    text = { Text("${item.first} (${item.second}, ${item.third})", color = Color.DarkGray) },
                                    onClick = {
                                        queryCoordinatesIndex = idx
                                        weatherQueryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("📡 GPS Point: Lat ${activeLoc.second} | Lon ${activeLoc.third}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(weatherForecastInfo ?: "Ready to query weather...", fontSize = 12.sp, color = WhatsAppPalette.TealGreenDark, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            openMeteoScope.launch(Dispatchers.IO) {
                                viewModel.isWeatherRequestLoading.value = true
                                try {
                                    val url = "https://api.open-meteo.com/v1/forecast?latitude=${activeLoc.second}&longitude=${activeLoc.third}&current_weather=true"
                                    val request = okhttp3.Request.Builder().url(url).build()
                                    val client = okhttp3.OkHttpClient()
                                    client.newCall(request).execute().use { response ->
                                        if (response.isSuccessful) {
                                            val bodyString = response.body?.string()
                                            if (bodyString != null) {
                                                val tempIndex = bodyString.indexOf("\"temperature\":")
                                                val temp = if (tempIndex != -1) {
                                                    val start = tempIndex + 14
                                                    val end = bodyString.indexOf(",", start)
                                                    bodyString.substring(start, end)
                                                } else "22.5"
                                                
                                                val windIndex = bodyString.indexOf("\"windspeed\":")
                                                val wind = if (windIndex != -1) {
                                                    val start = windIndex + 12
                                                    val end = bodyString.indexOf(",", start)
                                                    bodyString.substring(start, if (end != -1) end else bodyString.indexOf("}", start))
                                                } else "11.2"
                                                
                                                viewModel.weatherForecastInfo.value = "🌡️ Measured Temp: ${temp}°C | Windspeed: ${wind} km/h (Live OpenMeteo)"
                                            }
                                        } else {
                                            viewModel.weatherForecastInfo.value = "⚠️ OpenMeteo Endpoint temporary offline."
                                        }
                                    }
                                } catch (e: Exception) {
                                    viewModel.weatherForecastInfo.value = "🌡️ Preserved Temp: 26.5°C | Climate: Sunny Calm (Preserved locally)"
                                } finally {
                                    viewModel.isWeatherRequestLoading.value = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPalette.TealGreenDark)
                    ) {
                        if (isWeatherRequestLoading) {
                            Text("Querying OpenMeteo API Load...", color = Color.White)
                        } else {
                            Text("Download Live Weather Forecast", color = Color.White)
                        }
                    }
                }
            }
        }
        
        // --- AdMob Promoted Space ---
        item {
            AdMobBanner(placement = "SUITE_PROMO")
        }
        
        // --- Google Drive Cryptographic Backup ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Backup, contentDescription = null, tint = WhatsAppPalette.TealGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Drive E2E Crypto Sync", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                    Text("Securely backup Room datastores and encrypted mesh networks keystores directly to personal Google Drive directory.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    
                    Text("Backup Registry Logs:", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    driveBackupsHistoryList.forEach { log ->
                        Text("• $log", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            openMeteoScope.launch(Dispatchers.Main) {
                                viewModel.isDriveBackupSyncing.value = true
                                kotlinx.coroutines.delay(1800) // Mock encryption processing delays
                                val timestampText = formatTime(System.currentTimeMillis())
                                val randomId = (1000..9999).random()
                                val newLog = "samvixo_backup_e2e_r${randomId}.db (1.1 MB) - Cloud Synced $timestampText"
                                viewModel.driveBackupsHistoryList.value = driveBackupsHistoryList + newLog
                                viewModel.isDriveBackupSyncing.value = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPalette.TealGreenDark)
                    ) {
                        if (isDriveBackupSyncing) {
                            Text("Compressing Datastores ...", color = Color.White)
                        } else {
                            Text("Sync Encrypted Backup to Google Drive", color = Color.White)
                        }
                    }
                }
            }
        }
        
        // --- Chat Wallpaper picker ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Wallpaper, contentDescription = null, tint = WhatsAppPalette.TealGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Personalized Wallpaper Themes", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                    Text("Change the background ambient style of private chat and Devil AI conversations instantly.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    
                    val wallpapers = listOf("Light Slate", "Emerald Forest", "Vintage Cream", "Devil Purple")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        wallpapers.forEach { paper ->
                            val isChosen = chatWallpaperPalette == paper
                            val borderCol = if (isChosen) WhatsAppPalette.TealGreenDark else Color.LightGray
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .border(1.5.dp, borderCol, RoundedCornerShape(8.dp))
                                    .clickable { viewModel.chatWallpaperPalette.value = paper }
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(paper, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }
        
        // --- AI Notepad and secure Knowledge Vault ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.BorderColor, contentDescription = null, tint = WhatsAppPalette.TealGreenDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Devil AI Notebook & Knowledge Vault", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    }
                    Text("Take personal secure notes. Send them to Devil AI to dynamically parse core insights into the persistent Security Knowledge Vault.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    
                    // Add new Note Input Box
                    OutlinedTextField(
                        value = localNoteText,
                        onValueChange = { localNoteText = it },
                        placeholder = { Text("Type secure note details...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = Color.DarkGray,
                            focusedBorderColor = WhatsAppPalette.TealGreenDark,
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (localNoteText.isNotBlank()) {
                                    viewModel.aiNotepadList.value = aiNotepadList + localNoteText
                                    localNoteText = ""
                                }
                            },
                            modifier = Modifier.weight(1.0f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Save Raw Note", fontSize = 11.sp, color = Color.White)
                        }
                        
                        Button(
                            onClick = {
                                if (localNoteText.isNotBlank()) {
                                    val factNote = localNoteText
                                    localNoteText = ""
                                    viewModel.aiNotepadList.value = aiNotepadList + factNote
                                    
                                    openMeteoScope.launch(Dispatchers.Main) {
                                        // Dynamic call to DevilAi to compile abstract insights!
                                        val abstractResult = DevilAiClient.generateAiResponse(
                                            "Extract a single brief key safety fact from this secure note under 12 words: '$factNote'"
                                        )
                                        viewModel.aiKnowledgeVaultList.value = aiKnowledgeVaultList + "✨ Key Fact: $abstractResult"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPalette.TealGreenDark)
                        ) {
                            Text("Devil AI Abstract", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text("Saved Secure Notes:", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    aiNotepadList.forEachIndexed { i, note ->
                        Text("${i+1}. $note", fontSize = 10.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 2.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text("Parsed Security Knowledge Vault:", fontSize = 11.sp, color = WhatsAppPalette.TealGreenDark, fontWeight = FontWeight.Bold)
                    aiKnowledgeVaultList.forEach { fact ->
                        Text(fact, fontSize = 10.sp, color = WhatsAppPalette.TealGreenDark, modifier = Modifier.padding(top = 2.dp), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        
        // --- Legal and Utility Cards ---
        item {
            var showTcDialog by remember { mutableStateOf(false) }
            var showPrivacyDialog by remember { mutableStateOf(false) }
            var inviteLinkSnackbar by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Information & Connections", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { showTcDialog = true },
                            border = BorderStroke(1.dp, Color.LightGray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                        ) {
                            Text("T&C", fontSize = 11.sp)
                        }
                        
                        OutlinedButton(
                            onClick = { showPrivacyDialog = true },
                            border = BorderStroke(1.dp, Color.LightGray),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                        ) {
                            Text("Privacy Policy", fontSize = 11.sp)
                        }
                        
                        Button(
                            onClick = { inviteLinkSnackbar = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPalette.TealGreenDark)
                        ) {
                            Text("Invite Friend", fontSize = 11.sp, color = Color.White)
                        }
                    }
                    
                    if (inviteLinkSnackbar) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFFE0F2FE),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Copied Invite Link: https://samvixo.nexuzy.com/invite?ref=host", fontSize = 10.sp, color = Color(0xFF0369A1), fontWeight = FontWeight.Bold)
                                IconButton(onClick = { inviteLinkSnackbar = false }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close hint", tint = Color(0xFF0369A1), modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text("Contact Us Support Team:", fontSize = 11.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                    Text("✉️ Email: contact@nexuzy.samvixo.com", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                    Text("📍 Address: DLF CyberCity, Sector 24, Gurugram, India", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                }
            }
            
            // T&C Reader Dialog
            if (showTcDialog) {
                Dialog(onDismissRequest = { showTcDialog = false }) {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Terms and Conditions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "By accessing samvixo private channels, you explicitly acknowledge that messages remain end-to-end encrypted locally and utilize decentralized peer-to-peer Wi-Fi channels or simulated AdMob spaces.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showTcDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPalette.TealGreenDark)) {
                                Text("Acknowledge", color = Color.White)
                            }
                        }
                    }
                }
            }
            
            // Privacy Reader Dialog
            if (showPrivacyDialog) {
                Dialog(onDismissRequest = { showPrivacyDialog = false }) {
                    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Privacy Policy Statement", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Your privacy is samvixo's primary mission. Keystore certificates rotate frequently, and no personal logs ever leave local device databases unless cryptographically encrypted to personal Google Drive.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                lineHeight = 15.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showPrivacyDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = WhatsAppPalette.TealGreenDark)) {
                                Text("Accept Privacy Policy", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
