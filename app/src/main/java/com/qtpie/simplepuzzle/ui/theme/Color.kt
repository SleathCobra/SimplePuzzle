package com.qtpie.simplepuzzle.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Primary Brand Colors ────────────────────────────────────────────
// These are your game's signature colors

val PurpleBlue = Color(0xFF6C63FF)      // Primary brand color
val CoralPink = Color(0xFFFF6584)       // Secondary brand color
val WarmYellow = Color(0xFFFFD166)      // Accent / highlight color
val TealGreen = Color(0xFF06D6A0)       // Success / completion color

// ─── Gradients ────────────────────────────────────────────────────────
// Used for backgrounds to create that vibrant, game-like feel

// Main menu gradient: Purple → Coral
val MainMenuGradientStart = PurpleBlue
val MainMenuGradientEnd = CoralPink

// Button gradient: Purple → Warm Yellow (optional)
val ButtonGradientStart = PurpleBlue
val ButtonGradientEnd = WarmYellow

// ─── UI Colors (Semantic) ────────────────────────────────────────────
// These describe WHAT the color is used for, not just the color itself

val Primary = PurpleBlue
val PrimaryVariant = Color(0xFF5A52D9)  // Slightly darker purple for pressed states
val Secondary = CoralPink
val SecondaryVariant = Color(0xFFE55574) // Slightly darker coral

// Background colors
val BackgroundLight = Color(0xFFF8F9FA)  // Off-white for cards/containers
val BackgroundDark = Color(0xFF1A1A2E)   // For dark mode (future)

// Text colors
val TextPrimary = Color(0xFFFFFFFF)      // White text on dark backgrounds
val TextSecondary = Color(0xFFE0E0E0)    // Light gray for less important text
val TextOnLight = Color(0xFF2D2D2D)      // Dark text on light backgrounds

// Feedback colors
val Success = TealGreen
val Error = Color(0xFFFF6B6B)            // Red for wrong answers
val Warning = WarmYellow

// UI Element colors
val CardBackground = Color(0xFFFFFFFF).copy(alpha = 0.15f)  // Semi-transparent cards
val ButtonText = Color(0xFFFFFFFF)
val ShadowColor = Color(0xFF000000).copy(alpha = 0.2f)

// Puzzle piece states
val PieceLocked = Color(0xFF888888)      // Gray for locked pieces
val PieceUnlocked = Color(0xFFFFFFFF)    // White glow for unlocked pieces
val PiecePlaced = Color(0xFFFFFFFF)      // Normal placed piece

// ─── Dark Mode Colors (Future-Proofing) ─────────────────────────────
// Even if you only use light mode now, having these defined helps later

val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2D2D2D)
val DarkTextPrimary = Color(0xFFFFFFFF)
val DarkTextSecondary = Color(0xFFB0B0B0)

val AllColors = listOf(
    PurpleBlue,
    CoralPink,
    WarmYellow,
    TealGreen,
    MainMenuGradientStart,
    MainMenuGradientEnd,
    ButtonGradientStart,
    ButtonGradientEnd,
    Primary,
    PrimaryVariant,
    Secondary,
    SecondaryVariant,
    BackgroundLight,
    BackgroundDark,
    TextPrimary,
    TextSecondary,
    TextOnLight,
    Success,
    Error,
    Warning,
    CardBackground,
    ButtonText,
    ShadowColor,
    PieceLocked,
    PieceUnlocked,
    PiecePlaced,
    DarkBackground,
    DarkSurface,
    DarkSurfaceVariant,
    DarkTextPrimary,
    DarkTextSecondary,
)