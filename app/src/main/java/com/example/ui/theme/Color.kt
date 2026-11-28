package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =============================================================================
// Holy Moly ! — Pixar-Style 3D Matte Claymorphism & Clay-Glass Palette
// Directly sampled from the attached studio clay illustration:
// Mint-Teal studio wall, Warm Peach-Cream clay discs, Molded Aqua, Watermelon Coral,
// Honey-Tangerine Gold, and 3D Extruded Cream-White & Deep-Teal Ink typography.
// =============================================================================

// Studio Matte Mint-Teal Backdrop
val ClayTealWallTop = Color(0xFF5EB3AF)         // Soft sunlit studio teal top
val ClayTealWallMid = Color(0xFF7CD0C9)         // Signature bright mint-teal clay wall
val ClayTealWallBottom = Color(0xFF69BEB8)      // Balanced lower teal wall
val ClayTealShadow = Color(0xFF236668)          // Crisp directional studio cast shadow

// Warm Peach-Cream Sculpted Clay Discs & Cards (the 6 floating discs in reference!)
val ClayPeachLight = Color(0xFFFFEFE0)          // Sunlit peach-cream top highlight
val ClayPeach = Color(0xFFFDD8BA)               // Signature matte peach clay disc
val ClayPeachDeep = Color(0xFFE3A982)           // Molded 3D lower peach rim

// Soft Vanilla-Cream Porcelain Clay (studio floor & airy trays)
val ClayVanillaLight = Color(0xFFFFFBF4)        // Bright cream clay top
val ClayVanilla = Color(0xFFFFF1DC)             // Warm matte vanilla clay
val ClayVanillaDeep = Color(0xFFE2C8A8)         // Molded cream bevel

// Signature 3D Molded Toybox Clay Accents (from outfit, icons, mug, cap, apple)
val ClayAquaLight = Color(0xFF7EDCD9)           // Bright sculpted aqua top
val ClayAqua = Color(0xFF4AB5B6)                // Signature turquoise-aqua clay
val ClayAquaDeep = Color(0xFF277F84)            // Deep 3D aqua molded base

val ClayCoralLight = Color(0xFFFF9889)          // Bright coral-peach top
val ClayCoral = Color(0xFFF0675A)               // Watermelon coral clay (overalls/backpack)
val ClayCoralDeep = Color(0xFFC24038)           // Deep 3D coral molded base

val ClayHoneyLight = Color(0xFFFFD56E)          // Sunlit honey-yellow top (cap/coins)
val ClayHoney = Color(0xFFF7A633)               // Warm honey-tangerine clay (mug/bear/bulb)
val ClayHoneyDeep = Color(0xFFCB7514)           // Deep 3D honey molded base

val ClaySageLight = Color(0xFF6CD6AD)           // Soft mint-emerald highlight
val ClaySage = Color(0xFF33B389)                // Online & Accept emerald clay
val ClaySageDeep = Color(0xFF1A7F5F)            // Deep emerald base

// 3D Extruded Typography & Guaranteed-Contrast Ink Tokens
val ClayTextCream = Color(0xFFFFF9F2)           // Front face of "What is a feeling?" 3D text
val ClayTextPeachExtrude = Color(0xFFEBA886)    // Warm peach 3D side extrusion for titles
val ClayTextTealCast = Color(0xFF1F5D61)        // Studio cast shadow on teal
val ClayDeepTealInk = Color(0xFF15494E)         // Ultra-crisp dark teal text on Peach/Vanilla cards
val ClayCocoaInk = Color(0xFF3A2620)            // Warm espresso ink (hair/eyes) for maximum clarity
val ClayMutedInk = Color(0xFF3D6F73)            // Muted readable teal-slate on light clay

// Dark Twilight Mint-Teal Clay Theme
val DarkBackgroundTop = Color(0xFF16373B)
val DarkBackgroundBottom = Color(0xFF1E4C50)
val DarkSurfaceClay = Color(0xFF23575C)
val DarkOnSurface = Color(0xFFFFF8F0)
val DarkOnSurfaceMuted = Color(0xFFC7E8E5)

// =============================================================================
// Semantic & Compatibility Token Mapping (keeps every file harmonious)
// =============================================================================
val VibrantOrangeTop = ClayHoneyLight
val VibrantOrange = ClayHoney
val VibrantOrangeDeep = ClayHoneyDeep

val VibrantCyanTop = ClayAquaLight
val VibrantCyan = ClayAqua
val VibrantCyanDeep = ClayAquaDeep

val VibrantVioletTop = ClayCoralLight
val VibrantViolet = ClayCoral
val VibrantVioletDeep = ClayCoralDeep

val VibrantMagentaTop = ClayCoralLight
val VibrantMagenta = ClayCoral
val VibrantMagentaDeep = ClayCoralDeep

val VibrantMintTop = ClaySageLight
val VibrantMint = ClaySage
val VibrantMintDeep = ClaySageDeep

val VibrantYellowTop = ClayHoneyLight
val VibrantYellow = ClayHoney
val VibrantYellowDeep = ClayHoneyDeep

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

val Text3DShadowDark = Color(0x66174B4F)
val Text3DExtrudeTint = ClayTextPeachExtrude

val PastelSkyPink = ClayPeach
val PastelSkyLavender = ClayTealWallMid
val PastelSkyPeach = ClayVanilla
val PastelSkyCream = ClayVanillaLight

val ClayBlushCard = ClayPeach
val ClayButterCard = ClayVanilla
val ClayButterSoft = ClayVanillaLight
val ClayLavenderCard = ClayPeach
val ClayPinkMistCard = ClayPeach
val ClaySeafoamCard = ClayAqua

val AmalfiBerry = ClayCoral
val AmalfiBerryDeep = ClayCoralDeep
val CoralRoseLight = ClayCoralLight
val CoralRose = ClayCoral
val WisteriaLight = ClayAquaLight
val WisteriaCore = ClayAqua
val WisteriaDeep = ClayAquaDeep
val ApricotLight = ClayHoneyLight
val ApricotCore = ClayHoney
val ApricotDeep = ClayHoneyDeep
val SeafoamLight = ClayAquaLight
val SeafoamCore = ClayAqua
val SeafoamDeep = ClayAquaDeep

val CoastalInk = ClayDeepTealInk
val PlumInk = ClayCocoaInk
val SoftPlumMuted = ClayMutedInk

// Bottom Dock 4 Sculpted Clay Medallions
val TabLightHome = ClayCoral            // Buzz — Watermelon Coral Clay
val TabLightCall = ClayAqua             // Ring — Molded Aqua Clay
val TabLightChat = ClayHoney            // Chatter — Honey-Tangerine Clay
val TabLightPlay = ClaySage             // Play — Mint-Emerald Clay
val TabLightUs = ClayCoral

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

val PeriwinklePop = ClayAqua
val OrchidPink = ClayCoral
val CitrusLime = ClayHoneyLight

val MintTreehouse = ClaySage
val MintTreehouseDeep = ClaySageDeep
val MintTreehouseSoft = ClaySageLight

val ElectricRocket = ClayAqua
val ElectricRocketSoft = ClayAquaLight

val BubblePink = ClayCoral
val BubblePinkSoft = ClayCoralLight

val PurpleChest = ClayHoney
val PurpleChestSoft = ClayHoneyLight

val ConstellationGold = ClayHoney
val ConstellationSoft = ClayHoneyLight

val WalkieAmber = ClayHoney
val WalkieAmberDeep = ClayCoral
val EmeraldAccept = ClaySage
val CrimsonDecline = ClayCoral

// Per-Member Dynamic Halos (harmonized to the studio clay illustration)
val MithunSkyHalo = ClayAqua
val MafithaRoseHalo = ClayCoral
val ZaynSunHalo = ClayHoney

// Light Theme Canvas & Surface Tokens
val LightBackgroundTop = ClayTealWallTop
val LightBackgroundBottom = ClayTealWallBottom
val LightSurfaceClay = ClayPeach
val LightOnSurface = ClayDeepTealInk
val LightOnSurfaceMuted = ClayMutedInk
