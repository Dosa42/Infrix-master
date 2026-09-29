package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Dark Theme Foundations
val DarkAppBackground = Color(0xFF090D16)      // Deep dark canvas
val DarkCardSurface = Color(0xFF131B2E)        // Elevated card surface
val DarkCardSurfaceVariant = Color(0xFF1E293B) // Secondary card surface
val DarkCardBorder = Color(0xFF334155)         // Clean visible border
val DarkTextField = Color(0xFF0F172A)          // Dark input field background
val DarkTextFieldBorder = Color(0xFF334155)    // Dark input field border

// Text & Content High Contrast
val TextPrimary = Color(0xFFF8FAFC)            // Crisp white (Primary headings & main text)
val TextSecondary = Color(0xFFCBD5E1)          // Light silver (Readable secondary info)
val TextMuted = Color(0xFF94A3B8)              // Slate gray (Timestamps & metadata)
val TextInverse = Color(0xFF0F172A)            // Dark on bright buttons

// Brand & Accent Colors (Vibrant on dark)
val PrimaryBlue = Color(0xFF3B82F6)
val PrimaryBlueGlow = Color(0xFF60A5FA)
val SecondarySlate = Color(0xFF64748B)

// Role specific badges & themes
val AdminPrimary = Color(0xFFA78BFA)           // Bright Purple (Infrix-dev / Authority)
val AdminContainer = Color(0xFF2E1065)
val AdminOnContainer = Color(0xFFDDD6FE)

val WerkerPrimary = Color(0xFF38BDF8)          // Bright Cyan (Monteur / Planning)
val WerkerContainer = Color(0xFF082F49)
val WerkerOnContainer = Color(0xFFBAE6FD)

val KlantPrimary = Color(0xFF2DD4BF)           // Bright Teal (Klantportaal)
val KlantContainer = Color(0xFF042F2E)
val KlantOnContainer = Color(0xFF99F6E4)

// Status Indicators
val StatusSuccess = Color(0xFF34D399)          // Green (Active / Success)
val StatusWarning = Color(0xFFFBBF24)          // Amber (Pending / Warning)
val StatusDanger = Color(0xFFF87171)           // Red (Locked / Deleted)
val StatusInfo = Color(0xFF60A5FA)             // Blue (Info / Gepland)
