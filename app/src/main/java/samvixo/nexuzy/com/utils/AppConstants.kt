package samvixo.nexuzy.com.utils

object AppConstants {

    // ── App Info ──────────────────────────────────────────────────────────
    const val APP_NAME = "Samvixo"
    const val APP_PACKAGE = "samvixo.nexuzy.com"
    const val POWERED_BY = "Nexuzy Lab"
    const val DEVELOPER = "David"
    const val COMPANY = "Devil One Pvt Ltd"
    const val WEBSITE = "https://devilone.in"
    const val SUPPORT_EMAIL = "support@devilone.in"
    const val TERMS_URL = "https://devilone.in/samvixo/terms"
    const val PRIVACY_URL = "https://devilone.in/samvixo/privacy"
    const val INVITE_LINK = "https://samvixo.nexuzy.com/invite"

    // ── Supported Countries (ISO + Default) ───────────────────────────────
    val SUPPORTED_COUNTRIES = listOf("IN", "US", "GB", "FR", "SG", "BR", "ES", "AU", "RU", "PL")
    const val DEFAULT_COUNTRY_CODE = "+91"
    const val DEFAULT_COUNTRY_ISO = "IN"

    // ── Devil AI ──────────────────────────────────────────────────────────
    const val AI_BASE_URL = "https://aiapi.devilpvt.in/"
    const val AI_ENDPOINT = "api/generate"
    const val AI_TAGS_ENDPOINT = "api/tags"
    const val AI_MODEL_PRIMARY = "devil-ai"
    const val AI_MODEL_FALLBACK = "gemma3:4b"
    const val AI_NAME = "Devil AI"

    // ── AdMob ─────────────────────────────────────────────────────────────
    // TODO: Replace test IDs with real AdMob IDs before production release
    const val ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713" // TEST
    const val ADMOB_BANNER_STATUS = "ca-app-pub-3940256099942544/6300978111" // TEST - Status Screen
    const val ADMOB_BANNER_AI = "ca-app-pub-3940256099942544/6300978111"     // TEST - AI Screen
    const val ADMOB_INTERSTITIAL = "ca-app-pub-3940256099942544/1033173712"  // TEST

    // ── Firebase Collections ──────────────────────────────────────────────
    const val COLLECTION_USERS = "users"
    const val COLLECTION_CHATS = "chats"
    const val COLLECTION_GROUPS = "groups"
    const val COLLECTION_CHANNELS = "channels"
    const val COLLECTION_STATUSES = "statuses"
    const val COLLECTION_BROADCASTS = "broadcasts"
    const val COLLECTION_AI_NOTES = "ai_notes"
    const val COLLECTION_AI_VAULT = "ai_vault"
    const val COLLECTION_POLLS = "polls"
    const val COLLECTION_INBOX = "inbox"
    const val COLLECTION_REPORTS = "reports"

    // ── Status ────────────────────────────────────────────────────────────
    const val STATUS_EXPIRY_HOURS = 24L

    // ── Agora ─────────────────────────────────────────────────────────────
    // TODO: Replace with real Agora App ID
    const val AGORA_APP_ID = "YOUR_AGORA_APP_ID"

    // ── Google Maps / Weather ─────────────────────────────────────────────
    // Set in local.properties / BuildConfig
    // MAPS_API_KEY = BuildConfig.MAPS_API_KEY
    const val WEATHER_BASE_URL = "https://api.open-meteo.com/v1/"

    // ── Giphy ─────────────────────────────────────────────────────────────
    // TODO: Replace with real Giphy API Key
    const val GIPHY_API_KEY = "YOUR_GIPHY_API_KEY"

    // ── App Locker ────────────────────────────────────────────────────────
    const val PREF_APP_LOCK_ENABLED = "app_lock_enabled"
    const val PREF_APP_LOCK_PIN = "app_lock_pin"
    const val PREF_APP_LOCK_BIOMETRIC = "app_lock_biometric"

    // ── Chat ──────────────────────────────────────────────────────────────
    const val MAX_GROUP_MEMBERS = 256
    const val MAX_BROADCAST_RECIPIENTS = 100
    const val MESSAGE_PAGE_SIZE = 50

    // ── Storage ───────────────────────────────────────────────────────────
    const val BACKUP_FOLDER_NAME = "SamvixoBackup"

    // ── Trust Score ───────────────────────────────────────────────────────
    const val TRUST_SCORE_MAX = 100
    const val TRUST_SCORE_NEW_USER = 50
}
