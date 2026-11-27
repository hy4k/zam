package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Color Palette inspired by the attached Mid-Century Studio & Editorial Pastel reference boards:
// Image 2: Warm Sand Cream (#F5E8C7), Flame Coral Orange (#EB5E28), Midnight Ink (#0D1B2A),
//          Aqua Cerulean (#48A9B8), Deep Ocean Petrol (#115E78)
// Image 3: Warm Coral Rose (#F78888), Sunny Butter Yellow (#F3D250), Cloud Mist (#ECECEC),
//          Soft Sky Blue (#90CCF4), Ocean Azure (#5DA2D5)

val StudioTeal = Color(0xFF48A9B8)
val StudioTealDeep = Color(0xFF115E78)
val StudioTealSoft = Color(0xFFD9F1F4)

val CoralSalmon = Color(0xFFF78888)
val CoralSalmonDeep = Color(0xFFEB5E28)
val CoralSalmonSoft = Color(0xFFFFE5E5)

val OchreGold = Color(0xFFE0B422)
val OchreGoldBright = Color(0xFFF3D250)
val OchreGoldSoft = Color(0xFFFFF6D1)

val PowderSky = Color(0xFF90CCF4)
val PowderSkyDeep = Color(0xFF5DA2D5)

val PeriwinklePop = Color(0xFF5DA2D5)
val OrchidPink = Color(0xFFF78888)
val CitrusLime = Color(0xFFF3D250)

// Liquid Glass Illuminated Bottom Dock Light-Up Colors (one radiant glow per tab)
val TabLightHome = Color(0xFF48A9B8)      // A12 E — Aqua Cerulean Glow
val TabLightCall = Color(0xFF5DA2D5)      // C4L L — Ocean Azure Glow
val TabLightChat = Color(0xFFF78888)      // T4L K — Warm Coral Rose Glow
val TabLightPlay = Color(0xFFF3D250)      // P1A Y — Sunny Butter Gold Glow
val TabLightUs = Color(0xFFEB5E28)        // F4M 3 — Vibrant Flame Coral Glow

// Backward-aligned Semantic Role Tokens (mapped cleanly to the attached palettes)
val MintTreehouse = StudioTeal
val MintTreehouseDeep = StudioTealDeep
val MintTreehouseSoft = StudioTealSoft

val ElectricRocket = PowderSkyDeep
val ElectricRocketSoft = PowderSky

val BubblePink = CoralSalmon
val BubblePinkSoft = CoralSalmonSoft

val PurpleChest = StudioTealDeep
val PurpleChestSoft = Color(0xFFDCEFF6)

val ConstellationGold = OchreGoldBright
val ConstellationSoft = OchreGoldSoft

// Call & Walkie-Talkie Controls
val WalkieAmber = CoralSalmonDeep
val WalkieAmberDeep = Color(0xFFC44619)
val EmeraldAccept = Color(0xFF16B981)
val CrimsonDecline = Color(0xFFD6453A)

// Per-Member Dynamic Halo Skins (Mithun, Mafitha, Zayn Adam)
val MithunSkyHalo = PowderSkyDeep          // Ocean Azure (#5DA2D5)
val MafithaRoseHalo = CoralSalmon          // Warm Coral Rose (#F78888)
val ZaynSunHalo = OchreGoldBright          // Sunny Butter Yellow (#F3D250)

// Light Studio Canvas & Surface Tokens
val LightBackgroundTop = Color(0xFFF6F3EB)     // Warm Sand Ivory
val LightBackgroundBottom = Color(0xFFECECEC)  // Crisp Cloud Mist (#ECECEC)
val LightSurfaceClay = Color(0xFFFFFDF9)       // Luminous Warm White
val LightOnSurface = Color(0xFF0D1B2A)         // Deep Midnight Ink (#0D1B2A)
val LightOnSurfaceMuted = Color(0xFF475569)    // Structured Slate

// Dark Velvet Studio Canvas & Surface Tokens
val DarkBackgroundTop = Color(0xFF0D1B2A)      // Deep Midnight Navy (#0D1B2A)
val DarkBackgroundBottom = Color(0xFF0E3446)   // Deep Ocean Petrol
val DarkSurfaceClay = Color(0xFF13293B)        // Dark Frosted Petrol Clay
val DarkOnSurface = Color(0xFFF5F5F4)          // Warm Alabaster
val DarkOnSurfaceMuted = Color(0xFFB5CBD4)     // Cool Mist Silver
