package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Holy Moly ! — Vibrant 3D Clay Studio Palette (iOS-Smooth Edition)
// Directly sampled from the 3 attached vibrant 3D clay reference images:
//   - Juicy Tangerine Orange (#FF881B) & Sunny Mango (#FFB854)
//   - Electric Sky Cyan (#18C1EE) & Bright Ice Highlight (#6CE4FF)
//   - Playful Periwinkle Lavender (#9675FF) & Soft Lilac (#C4B0FF)
//   - Joyful Coral Pink (#FF5E6B) & Warm Peach Clay (#FFE0C8)
//   - Fresh Mint Teal (#22C9A6) on an airy pastel aqua-to-lavender canvas
// =============================================================================

// Warm Studio Backdrop Accents (for doorway/call overlays)
val AtelierDeskDarkTop = Color(0xFF162438)
val AtelierDeskDarkMid = Color(0xFF101B2B)
val AtelierDeskDarkBottom = Color(0xFF0B1320)
val AtelierLampGlow = Color(0xFFFFD88A)
val AtelierWoodFrameLight = Color(0xFF7ADCF5)
val AtelierWoodFrameMid = Color(0xFF3DA7C7)
val AtelierWoodFrameDeep = Color(0xFF1A5E7A)
val AtelierDoorInterior = Color(0xFF0E1B2B)
val AtelierBrassHandle = Color(0xFFFFC15E)
val AtelierRedPresenceDot = Color(0xFFFF5263)

// Luminous Pastel Aqua-to-Lavender 3D Clay Studio Canvas
val ClayTealWallTop = Color(0xFFEBFBF9)         // Airy sunlit mint-aqua mist
val ClayTealWallMid = Color(0xFFE4F5FA)         // Crisp sky-aqua studio canvas
val ClayTealWallBottom = Color(0xFFEDE9FF)      // Soft pastel lavender-cloud base
val ClayTealShadow = Color(0xFF23476B)          // Clean cool studio ambient shadow

// Warm Peach / Apricot 3D Clay Cards & Circular Badges (from Reference Image 2)
val ClayPeachLight = Color(0xFFFFF3E8)          // Bright creamy apricot highlight
val ClayPeach = Color(0xFFFFE1C9)               // Signature soft peach clay disc/card
val ClayPeachDeep = Color(0xFFF2B68B)           // Sculpted warm peach bevel

// Crisp Porcelain White Elevated 3D Surfaces (from Reference Images 1 & 3)
val ClayVanillaLight = Color(0xFFFFFFFF)        // Pure sunlit iOS porcelain white
val ClayVanilla = Color(0xFFF4FAFF)             // Soft airy ice-white card
val ClayVanillaDeep = Color(0xFFD2E5F6)         // Delicate sky-frost bevel rim

// Electric Sky Cyan 3D Block (from Reference Images 1 & 3 middle tile)
val ClayAquaLight = Color(0xFF6CE4FF)           // Bright glossy cyan rim
val ClayAqua = Color(0xFF18C1EE)                // Vibrant electric sky cyan
val ClayAquaDeep = Color(0xFF0985B2)            // Rich ocean-cyan 3D bevel

// Joyful Coral Pink / Strawberry 3D Block (from Reference Image 2)
val ClayCoralLight = Color(0xFFFF989E)          // Bright rosy coral highlight
val ClayCoral = Color(0xFFFF5E6B)               // Vibrant playful coral pink
val ClayCoralDeep = Color(0xFFD6364A)           // Deep berry-coral 3D bevel

// Juicy Tangerine Orange / Mango 3D Block (from Reference Images 1 & 3 top tile)
val ClayHoneyLight = Color(0xFFFFBD59)          // Sunny mango highlight
val ClayHoney = Color(0xFFFF881B)               // Vibrant tangerine orange
val ClayHoneyDeep = Color(0xFFD15700)           // Deep marmalade 3D bevel

// Fresh Mint Aqua / Emerald 3D Block (from Reference Image 2 backdrop & live green)
val ClaySageLight = Color(0xFF6CEAD0)           // Bright seafoam mint highlight
val ClaySage = Color(0xFF1FC7A4)                // Vibrant fresh mint teal
val ClaySageDeep = Color(0xFF0C8F74)            // Deep emerald-teal 3D bevel

// Playful Periwinkle Lavender 3D Block (from Reference Images 1 & 3 bottom tile)
val VibrantVioletTop = Color(0xFFC5B2FF)
val VibrantViolet = Color(0xFF9675FF)
val VibrantVioletDeep = Color(0xFF6745D6)

// Crisp iPhone-Grade High-Contrast Ink Tokens
val ClayTextCream = Color(0xFFFFFFFF)           // Crisp white text on vibrant 3D tiles
val ClayTextPeachExtrude = Color(0xFF1C3A5E)    // Clean depth shadow behind white display text
val ClayTextTealCast = Color(0xFF102840)        // Soft cast shadow for 3D headlines
val ClayDeepTealInk = Color(0xFF13263C)         // Deep midnight-teal primary ink (high contrast!)
val ClayCocoaInk = Color(0xFF284360)            // Secondary crisp slate-navy ink
val ClayMutedInk = Color(0xFF5D7792)            // Refined cool muted caption ink

// Sleek Vibrant iOS Dark Theme
val DarkBackgroundTop = Color(0xFF11192A)
val DarkBackgroundBottom = Color(0xFF19243B)
val DarkSurfaceClay = Color(0xFF22304C)
val DarkOnSurface = Color(0xFFF4F8FF)
val DarkOnSurfaceMuted = Color(0xFFAEBEDA)

// =============================================================================
// Semantic & Compatibility Token Mapping (100% cohesive across all screens)
// =============================================================================
val VibrantOrangeTop = ClayHoneyLight
val VibrantOrange = ClayHoney
val VibrantOrangeDeep = ClayHoneyDeep

val VibrantCyanTop = ClayAquaLight
val VibrantCyan = ClayAqua
val VibrantCyanDeep = ClayAquaDeep

val VibrantMagentaTop = ClayCoralLight
val VibrantMagenta = ClayCoral
val VibrantMagentaDeep = ClayCoralDeep

val VibrantMintTop = ClaySageLight
val VibrantMint = ClaySage
val VibrantMintDeep = ClaySageDeep

val VibrantYellowTop = Color(0xFFFFDF73)
val VibrantYellow = Color(0xFFFFB82E)
val VibrantYellowDeep = Color(0xFFD68600)

val PlushCobaltLight = ClayAquaLight
val PlushCobalt = ClayAqua
val PlushCobaltDeep = ClayAquaDeep

val PlushCrimsonLight = ClayCoralLight
val PlushCrimson = ClayCoral
val PlushCrimsonDeep = ClayCoralDeep

val PlushEmeraldLight = ClaySageLight
val PlushEmerald = ClaySage
val PlushEmeraldDeep = ClaySageDeep

val PlushMarigoldLight = ClayHoneyLight
val PlushMarigold = ClayHoney
val PlushMarigoldDeep = ClayHoneyDeep

val PlushCreamLight = ClayVanillaLight
val PlushCreamFleece = ClayVanilla
val PlushCreamDeep = ClayVanillaDeep

val PlushPeachLight = ClayPeachLight
val PlushPeachFleece = ClayPeach
val PlushPeachDeep = ClayPeachDeep

val PlushInkDark = ClayDeepTealInk

val VibrantBackdropTop = ClayTealWallTop
val VibrantBackdropMid = ClayTealWallMid
val VibrantBackdropLow = ClayTealWallBottom
val VibrantBackdropWarm = ClayVanilla

val Text3DShadowDark = Color(0x3D102840)
val Text3DExtrudeTint = ClayTextPeachExtrude

val PastelSkyPink = Color(0xFFFFE4EC)
val PastelSkyLavender = Color(0xFFECE6FF)
val PastelSkyPeach = ClayPeach
val PastelSkyCream = ClayVanillaLight

val ClayBlushCard = ClayPeach
val ClayButterCard = ClayVanilla
val ClayButterSoft = ClayVanillaLight
val ClayLavenderCard = Color(0xFFECE6FF)
val ClayPinkMistCard = Color(0xFFFFE5EC)
val ClaySeafoamCard = ClayAqua

val AmalfiBerry = ClayCoral
val AmalfiBerryDeep = ClayCoralDeep
val CoralRoseLight = ClayCoralLight
val CoralRose = ClayCoral
val WisteriaLight = VibrantVioletTop
val WisteriaCore = VibrantViolet
val WisteriaDeep = VibrantVioletDeep
val ApricotLight = ClayHoneyLight
val ApricotCore = ClayHoney
val ApricotDeep = ClayHoneyDeep
val SeafoamLight = ClaySageLight
val SeafoamCore = ClaySage
val SeafoamDeep = ClaySageDeep

val CoastalInk = ClayDeepTealInk
val PlumInk = ClayCocoaInk
val SoftPlumMuted = ClayMutedInk

// Bottom Dock 4 Vibrant 3D Candy Medallions
val TabLightHome = ClayAqua             // Radio — Electric Sky Cyan
val TabLightCall = ClayCoral            // Calls — Joyful Coral Pink
val TabLightChat = ClayHoney            // Chat — Juicy Tangerine Orange
val TabLightPlay = VibrantViolet        // Play — Playful Periwinkle Lavender
val TabLightUs = ClaySage

val StudioTeal = ClayAqua
val StudioTealDeep = ClayAquaDeep
val StudioTealSoft = ClayAquaLight

val CoralSalmon = ClayCoral
val CoralSalmonDeep = ClayCoralDeep
val CoralSalmonSoft = ClayCoralLight

val OchreGold = ClayHoney
val OchreGoldBright = ClayHoneyLight
val OchreGoldSoft = ClayVanilla

val PowderSky = ClayAquaLight
val PowderSkyDeep = ClayAqua

val PeriwinklePop = VibrantViolet
val OrchidPink = ClayCoral
val CitrusLime = ClaySageLight

val MintTreehouse = ClaySage
val MintTreehouseDeep = ClaySageDeep
val MintTreehouseSoft = ClaySageLight

val ElectricRocket = ClayAqua
val ElectricRocketSoft = ClayAquaLight

val BubblePink = ClayCoral
val BubblePinkSoft = ClayCoralLight

val PurpleChest = VibrantViolet
val PurpleChestSoft = VibrantVioletTop

val ConstellationGold = VibrantYellow
val ConstellationSoft = VibrantYellowTop

val WalkieAmber = ClayHoney
val WalkieAmberDeep = ClayCoral
val EmeraldAccept = Color(0xFF1CC98A)
val CrimsonDecline = ClayCoral

// Per-Member Vibrant Halos
val MithunSkyHalo = ClayAqua
val MafithaRoseHalo = ClayCoral
val ZaynSunHalo = ClayHoney

// Light Theme Canvas & Surface Tokens
val LightBackgroundTop = ClayTealWallTop
val LightBackgroundBottom = ClayTealWallBottom
val LightSurfaceClay = ClayVanillaLight
val LightOnSurface = ClayDeepTealInk
val LightOnSurfaceMuted = ClayMutedInk
