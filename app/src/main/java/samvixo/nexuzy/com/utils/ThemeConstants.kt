package samvixo.nexuzy.com.utils

/**
 * ThemeConstants — Samvixo UI Color & Text Visibility Fixes
 * Ensures white text is readable across all dark/light surfaces
 */
object ThemeConstants {

    // ── Primary Brand ─────────────────────────────────────────────
    const val COLOR_PRIMARY = "#6C63FF"          // Purple
    const val COLOR_PRIMARY_DARK = "#4A44CC"
    const val COLOR_ACCENT = "#00D2FF"            // Cyan

    // ── Background Surfaces ───────────────────────────────────────
    const val COLOR_BG_DARK = "#050816"           // Splash/Main bg
    const val COLOR_BG_SURFACE = "#0D1117"        // Card bg
    const val COLOR_BG_ELEVATED = "#161B22"       // Bottom sheet / Dialog bg
    const val COLOR_BG_INPUT = "#1C2333"          // Input field bg

    // ── Text Colors (FIXED — no more invisible white-on-white) ────
    const val COLOR_TEXT_PRIMARY = "#FFFFFF"       // Headings, main text
    const val COLOR_TEXT_SECONDARY = "#B0B8C1"    // Subtitles, hints
    const val COLOR_TEXT_TERTIARY = "#6B7280"     // Timestamps, placeholders
    const val COLOR_TEXT_ON_LIGHT = "#0D1117"     // Text on light backgrounds
    const val COLOR_TEXT_LINK = "#6C63FF"         // Hyperlinks
    const val COLOR_TEXT_ERROR = "#FF4D4D"        // Error messages
    const val COLOR_TEXT_SUCCESS = "#4CAF50"      // Success messages
    const val COLOR_TEXT_WARNING = "#FFA726"      // Warnings

    // ── Message Bubbles ───────────────────────────────────────────
    const val COLOR_BUBBLE_SENT = "#6C63FF"       // Sent message bubble
    const val COLOR_BUBBLE_RECEIVED = "#1C2333"   // Received message bubble
    const val COLOR_BUBBLE_SENT_TEXT = "#FFFFFF"
    const val COLOR_BUBBLE_RECEIVED_TEXT = "#E8EAF0"
    const val COLOR_BUBBLE_TIMESTAMP = "#99FFFFFF"

    // ── Status Bar & Navigation ───────────────────────────────────
    const val COLOR_STATUS_BAR = "#050816"
    const val COLOR_NAV_BAR = "#0D1117"

    // ── Dividers & Borders ────────────────────────────────────────
    const val COLOR_DIVIDER = "#1E2536"
    const val COLOR_BORDER = "#2D3748"

    // ── Icon Tints ────────────────────────────────────────────────
    const val COLOR_ICON_ACTIVE = "#FFFFFF"
    const val COLOR_ICON_INACTIVE = "#6B7280"
}
