package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BadgeElement
import com.example.data.model.EmojiBurstEvent
import com.example.data.model.FamilyMemberId
import com.example.data.model.OfflineSyncState
import com.example.data.model.PresenceAvailability
import com.example.data.model.defaultAvatarRes
import com.example.data.model.haloColor
import com.example.data.model.statusColor
import com.example.ui.WorkspaceTab
import com.example.ui.theme.ClayAqua
import com.example.ui.theme.ClayAquaDeep
import com.example.ui.theme.ClayCocoaInk
import com.example.ui.theme.ClayCoral
import com.example.ui.theme.ClayCoralDeep
import com.example.ui.theme.ClayDeepTealInk
import com.example.ui.theme.ClayHoney
import com.example.ui.theme.ClayHoneyDeep
import com.example.ui.theme.ClayHoneyLight
import com.example.ui.theme.ClayMutedInk
import com.example.ui.theme.ClayPeach
import com.example.ui.theme.ClayPeachLight
import com.example.ui.theme.ClaySage
import com.example.ui.theme.ClaySageDeep
import com.example.ui.theme.ClayTealShadow
import com.example.ui.theme.ClayTealWallBottom
import com.example.ui.theme.ClayTealWallMid
import com.example.ui.theme.ClayTealWallTop
import com.example.ui.theme.ClayTextCream
import com.example.ui.theme.ClayTextPeachExtrude
import com.example.ui.theme.ClayTextTealCast
import com.example.ui.theme.ClayVanilla
import com.example.ui.theme.ClayVanillaLight
import com.example.ui.theme.DarkBackgroundBottom
import com.example.ui.theme.DarkBackgroundTop
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.clayDisc3D
import com.example.ui.theme.clayPressScale
import com.example.ui.theme.claymorphic
import com.example.ui.theme.liquidGlass

/**
 * SIGNATURE 3D EXTRUDED CLAY TYPOGRAPHY ("What is a feeling?" style from the attached image!):
 * Multi-layered molded 3D letters with:
 *  - Soft cast drop shadow down-left/down
 *  - Thick peach/warm clay extruded side thickness (stepped 3D body)
 *  - Crisp pure Cream-White front face (use on Teal, Coral, Aqua, Honey surfaces!)
 */
@Composable
fun Text3D(
    text: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier,
    textColor: Color = ClayTextCream,
    extrudeColor: Color = ClayTextPeachExtrude,
    castShadowColor: Color = ClayTextTealCast.copy(alpha = 0.56f),
    depthDp: Dp = 2.2.dp,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Box(modifier = modifier) {
        if (depthDp > 0.dp) {
            // Layer 1: Soft directional studio cast shadow (floating off the clay wall)
            Text(
                text = text,
                style = style.copy(shadow = null),
                color = castShadowColor,
                textAlign = textAlign,
                maxLines = maxLines,
                overflow = overflow,
                modifier = Modifier.offset(x = -depthDp * 0.45f, y = depthDp * 1.45f)
            )
            // Layer 2: Outer 3D molded clay thickness edge (warm peach depth)
            Text(
                text = text,
                style = style.copy(shadow = null),
                color = extrudeColor,
                textAlign = textAlign,
                maxLines = maxLines,
                overflow = overflow,
                modifier = Modifier.offset(x = depthDp * 0.25f, y = depthDp * 0.92f)
            )
            // Layer 3: Mid 3D extruded clay step (smooth solid 3D lettering extrusion!)
            Text(
                text = text,
                style = style.copy(shadow = null),
                color = extrudeColor.copy(alpha = 0.88f),
                textAlign = textAlign,
                maxLines = maxLines,
                overflow = overflow,
                modifier = Modifier.offset(x = depthDp * 0.12f, y = depthDp * 0.46f)
            )
        }
        // Layer 4: Sunlit Crisp Front Face
        Text(
            text = text,
            style = style.copy(
                shadow = if (depthDp > 0.dp) {
                    Shadow(
                        color = castShadowColor.copy(alpha = 0.32f),
                        offset = Offset(0f, 2f),
                        blurRadius = 3f
                    )
                } else null
            ),
            color = textColor,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}

/**
 * High-Contrast Molded 3D Clay Text for Light Peach-Cream & Vanilla Clay Surfaces:
 * Guarantees 100% visibility on light peach/cream cards with a crisp top highlight
 * and deep ocean-teal/cocoa sculpted 3D depth!
 */
@Composable
fun ClayTextOnLight(
    text: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier,
    textColor: Color = ClayDeepTealInk,
    depthDp: Dp = 1.6.dp,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Box(modifier = modifier) {
        // Subtle sunlit white inner emboss rim
        Text(
            text = text,
            style = style.copy(shadow = null),
            color = Color.White.copy(alpha = 0.72f),
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.offset(y = (-depthDp * 0.5f))
        )
        // Sculpted 3D drop depth
        Text(
            text = text,
            style = style.copy(shadow = null),
            color = Color(0x2E15494E),
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow,
            modifier = Modifier.offset(x = depthDp * 0.35f, y = depthDp)
        )
        // Razor-sharp Deep Teal / Cocoa Front Face
        Text(
            text = text,
            style = style.copy(shadow = null),
            color = textColor,
            textAlign = textAlign,
            maxLines = maxLines,
            overflow = overflow
        )
    }
}

/**
 * SIGNATURE STUDIO MINT-TEAL CLAY BACKDROP (exact match for the attached image!):
 * Smooth matte Mint-Teal gradient wall with subtle warm cream floor glow at the bottom
 * and scattered 3D honey-gold clay floating dots/coins.
 */
@Composable
fun KinNestAmbientCanvas(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val wallBrush = if (darkTheme) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to DarkBackgroundTop,
                0.65f to DarkBackgroundBottom,
                1.0f to Color(0xFF132F33)
            )
        )
    } else {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to ClayTealWallTop,
                0.38f to ClayTealWallMid,
                0.86f to ClayTealWallBottom,
                1.0f to Color(0xFF9AD9CE)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(wallBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (size.width <= 0f || size.height <= 0f) return@Canvas

            // Soft studio spotlight bloom from upper right (matching the reference lighting)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (darkTheme) ClayAqua.copy(alpha = 0.16f) else Color(0xFFA6E5DE).copy(alpha = 0.55f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.78f, size.height * 0.14f),
                    radius = size.width * 0.72f
                ),
                center = Offset(size.width * 0.78f, size.height * 0.14f),
                radius = size.width * 0.72f
            )

            // Warm vanilla-cream floor glow at the very bottom (matching reference studio shelf)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        if (darkTheme) Color(0x22F7A633) else ClayVanilla.copy(alpha = 0.55f)
                    ),
                    startY = size.height * 0.86f,
                    endY = size.height
                )
            )

            // Subtle floating 3D honey-gold clay coins/dots scattered gently like the reference!
            val dots = listOf(
                Triple(0.10f, 0.16f, 6.dp.toPx()),
                Triple(0.89f, 0.13f, 8.dp.toPx()),
                Triple(0.08f, 0.42f, 9.dp.toPx()),
                Triple(0.92f, 0.48f, 7.dp.toPx()),
                Triple(0.13f, 0.70f, 8.dp.toPx()),
                Triple(0.87f, 0.74f, 6.dp.toPx()),
                Triple(0.24f, 0.25f, 4.dp.toPx()),
                Triple(0.76f, 0.30f, 4.5.dp.toPx())
            )
            val coinAlpha = if (darkTheme) 0.35f else 0.72f
            dots.forEach { (fx, fy, r) ->
                val cx = size.width * fx
                val cy = size.height * fy
                drawCircle(
                    color = ClayTealShadow.copy(alpha = 0.22f * coinAlpha),
                    radius = r,
                    center = Offset(cx - 1.5.dp.toPx(), cy + 2.5.dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ClayHoneyLight.copy(alpha = coinAlpha),
                            ClayHoney.copy(alpha = coinAlpha)
                        ),
                        center = Offset(cx - r * 0.3f, cy - r * 0.3f),
                        radius = r * 1.4f
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
        }
        content()
    }
}

/**
 * SIGNATURE 3D MATTE CLAY CIRCULAR BUTTON (modeled directly on the floating clay discs
 * with molded 3D raised icons in the attached studio image!).
 */
@Composable
fun ClayCircleButton(
    icon: ImageVector,
    contentDescription: String,
    clayColor: Color = ClayPeach,
    iconTint: Color = ClayAqua,
    size: Dp = 52.dp,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    Box(
        modifier = modifier
            .size(size)
            .clayPressScale(reducedMotion = a11y.reducedMotion)
            .clayDisc3D(
                surfaceColor = clayColor,
                elevation = 12.dp,
                shadowTint = ClayTealShadow,
                ledgeDepth = 4.dp
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Raised 3D molded icon shadow + crisp front icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0x3D1B4D50),
            modifier = Modifier
                .offset(x = (-0.8).dp, y = 2.dp)
                .size(size * 0.48f)
        )
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier
                .offset(y = (-1).dp)
                .size(size * 0.48f)
        )
    }
}

/**
 * Compatibility delegate for PlushCircleButton -> sleek Pixar 3D Matte Clay Circle Button.
 */
@Composable
fun PlushCircleButton(
    icon: ImageVector,
    contentDescription: String,
    plushColor: Color,
    iconTint: Color = Color.White,
    size: Dp = 52.dp,
    furLength: Dp = 4.dp,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ClayCircleButton(
        icon = icon,
        contentDescription = contentDescription,
        clayColor = plushColor,
        iconTint = iconTint,
        size = size,
        testTag = testTag,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
fun PastelClayCircleButton(
    icon: ImageVector,
    contentDescription: String,
    topColor: Color,
    bottomColor: Color,
    iconTint: Color = Color.White,
    size: Dp = 50.dp,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ClayCircleButton(
        icon = icon,
        contentDescription = contentDescription,
        clayColor = bottomColor,
        iconTint = iconTint,
        size = size,
        testTag = testTag,
        modifier = modifier,
        onClick = onClick
    )
}

/**
 * SIGNATURE 3D MATTE CLAY PILL ACTION BUTTON:
 * Smooth velvety clay button with molded 3D bottom lip, tactile spring press,
 * and guaranteed-legible 3D extruded typography!
 */
@Composable
fun ClayActionButton(
    text: String,
    containerColor: Color,
    contentColor: Color = ClayTextCream,
    icon: ImageVector? = null,
    testTag: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 54.dp,
    onClick: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    val isLightSurface = containerColor == ClayPeach ||
        containerColor == ClayPeachLight ||
        containerColor == ClayVanilla ||
        containerColor == ClayVanillaLight
    val resolvedTextColor = if (isLightSurface) ClayDeepTealInk else contentColor

    Box(
        modifier = modifier
            .height(minHeight)
            .clayPressScale(reducedMotion = a11y.reducedMotion)
            .claymorphic(
                cornerRadius = minHeight / 2,
                surfaceColor = containerColor,
                elevation = 12.dp,
                shadowTint = ClayTealShadow,
                ledgeDepth = 5.dp
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp)
            .padding(bottom = 3.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Box {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0x3816484C),
                        modifier = Modifier
                            .offset(y = 1.5.dp)
                            .size(20.dp)
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = resolvedTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (isLightSurface) {
                ClayTextOnLight(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textColor = resolvedTextColor,
                    depthDp = 1.3.dp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text3D(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    textColor = resolvedTextColor,
                    extrudeColor = Color(0x665E241C),
                    depthDp = 1.6.dp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PlushActionButton(
    text: String,
    containerColor: Color,
    contentColor: Color = ClayTextCream,
    icon: ImageVector? = null,
    testTag: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 54.dp,
    furLength: Dp = 4.dp,
    onClick: () -> Unit
) {
    ClayActionButton(
        text = text,
        containerColor = containerColor,
        contentColor = contentColor,
        icon = icon,
        testTag = testTag,
        modifier = modifier,
        minHeight = minHeight,
        onClick = onClick
    )
}

@Composable
fun EditorialSectionHeader(
    eyebrow: String,
    title: String,
    accentColor: Color = ClayHoney,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 18.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text3D(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    depthDp = 1.2.dp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text3D(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                depthDp = 2.2.dp
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun FamilyMemberAvatar(
    member: FamilyMemberId,
    customPhotoUri: String? = null,
    badge: BadgeElement = BadgeElement.STARLIGHT,
    size: Dp = 64.dp,
    showBadge: Boolean = true,
    isOnline: Boolean? = null,
    availability: PresenceAvailability? = null,
    modifier: Modifier = Modifier
) {
    val halo = member.haloColor()
    val resolvedDotColor: Color? = when {
        availability != null -> availability.statusColor()
        isOnline != null -> if (isOnline) EmeraldAccept else Color(0xFFAABFC1)
        else -> null
    }
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = ClayTealShadow.copy(alpha = 0.42f),
                    spotColor = ClayTealShadow.copy(alpha = 0.42f)
                )
                .clip(CircleShape)
                .background(ClayVanillaLight)
                .border(2.8.dp, ClayVanillaLight, CircleShape)
                .padding(2.5.dp)
                .clip(CircleShape)
                .background(halo.copy(alpha = 0.24f))
        ) {
            if (!customPhotoUri.isNullOrBlank()) {
                AsyncImage(
                    model = customPhotoUri,
                    contentDescription = "${member.displayName} avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Image(
                    painter = painterResource(id = member.defaultAvatarRes()),
                    contentDescription = "${member.displayName} avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (resolvedDotColor != null) {
            val dotSize = (size * 0.25f).coerceIn(10.dp, 16.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(resolvedDotColor)
                    .border(2.dp, ClayVanillaLight, CircleShape)
            )
        }
        if (showBadge) {
            Surface(
                shape = CircleShape,
                color = ClayVanillaLight,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.36f)
                    .border(1.8.dp, halo, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = badge.emoji,
                        fontSize = (size.value * 0.19f).sp
                    )
                }
            }
        }
    }
}

/**
 * SIGNATURE STUDIO CLAY-GLASS BOTTOM DOCK:
 * Smooth frosted vanilla-cream clay-glass pill tray housing 4 sculpted 3D Matte Clay
 * circular disc buttons (Peach-Cream resting discs with molded colored icons, popping into
 * rich 3D Coral / Aqua / Honey / Sage clay spheres when active!).
 */
@Composable
fun KinNestToyBottomDock(
    selectedTab: WorkspaceTab,
    unreadCount: Int,
    onSelectTab: (WorkspaceTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val a11y = LocalKinNestAccessibility.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(
                    shape = RoundedCornerShape(34.dp),
                    surfaceAlpha = 0.86f,
                    tintColor = ClayVanillaLight,
                    cornerRadius = 34.dp
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkspaceTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val style = tabClayStyle(tab)
                    val orbScale by animateFloatAsState(
                        targetValue = if (isSelected && !a11y.reducedMotion) 1.12f else 0.96f,
                        animationSpec = spring(dampingRatio = 0.56f, stiffness = 420f),
                        label = "clay_orb_scale_${tab.name}"
                    )
                    val orbLift by animateFloatAsState(
                        targetValue = if (isSelected && !a11y.reducedMotion) -3.5f else 0f,
                        animationSpec = spring(dampingRatio = 0.6f, stiffness = 420f),
                        label = "clay_orb_lift_${tab.name}"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelectTab(tab) }
                            )
                            .padding(vertical = 2.dp)
                            .testTag("tab_${tab.name.lowercase()}"),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = orbScale
                                    scaleY = orbScale
                                    translationY = orbLift
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Sculpted 3D Clay Disc (Warm Peach-Cream with Aqua/Coral/Honey icon when resting;
                            // solid sculpted Coral/Aqua/Honey/Sage clay sphere with Cream icon when active!)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clayDisc3D(
                                        surfaceColor = if (isSelected) style.activeClay else ClayPeach,
                                        elevation = if (isSelected) 12.dp else 6.dp,
                                        shadowTint = ClayTealShadow,
                                        ledgeDepth = if (isSelected) 4.dp else 3.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = null,
                                    tint = Color(0x30174B4F),
                                    modifier = Modifier
                                        .offset(y = 1.5.dp)
                                        .size(23.dp)
                                )
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = tab.accessibleTitle,
                                    tint = if (isSelected) ClayTextCream else style.activeClay,
                                    modifier = Modifier
                                        .offset(y = (-0.8).dp)
                                        .size(23.dp)
                                )
                            }

                            if (tab == WorkspaceTab.CHAT && unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 3.dp, y = (-2).dp)
                                        .size(19.dp)
                                        .clip(CircleShape)
                                        .background(ClayCoral)
                                        .border(1.8.dp, ClayVanillaLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text3D(
                                        text = unreadCount.coerceAtMost(9).toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        depthDp = 0.8.dp
                                    )
                                }
                            }
                        }

                        // Always 100% legible Deep Teal / Active Clay label on the cream-glass dock
                        ClayTextOnLight(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            textColor = if (isSelected) style.activeLabelInk else ClayMutedInk,
                            depthDp = if (isSelected) 1.2.dp else 0.7.dp,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KinNestNavigationRail(
    selectedTab: WorkspaceTab,
    onSelectTab: (WorkspaceTab) -> Unit
) {
    NavigationRail(
        containerColor = ClayVanilla.copy(alpha = 0.94f)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        WorkspaceTab.entries.forEach { tab ->
            val style = tabClayStyle(tab)
            NavigationRailItem(
                selected = tab == selectedTab,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = tab.accessibleTitle,
                        tint = style.activeClay
                    )
                },
                label = {
                    Text(
                        text = tab.label,
                        color = ClayDeepTealInk,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
            )
        }
    }
}

private data class TabClayStyle(
    val icon: ImageVector,
    val activeClay: Color,
    val activeLabelInk: Color
)

private fun tabClayStyle(tab: WorkspaceTab): TabClayStyle = when (tab) {
    WorkspaceTab.HOME -> TabClayStyle(
        icon = Icons.Rounded.CellTower,
        activeClay = ClayCoral,
        activeLabelInk = ClayCoralDeep
    )
    WorkspaceTab.CALL -> TabClayStyle(
        icon = Icons.Rounded.Call,
        activeClay = ClayAqua,
        activeLabelInk = ClayAquaDeep
    )
    WorkspaceTab.CHAT -> TabClayStyle(
        icon = Icons.Rounded.Forum,
        activeClay = ClayHoney,
        activeLabelInk = ClayHoneyDeep
    )
    WorkspaceTab.PLAY -> TabClayStyle(
        icon = Icons.Rounded.GridView,
        activeClay = ClaySage,
        activeLabelInk = ClaySageDeep
    )
}

@Composable
fun OfflineAndQuietStatusBanners(
    syncState: OfflineSyncState,
    quietHoursActive: Boolean,
    onRetrySync: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(visible = !syncState.isOnline || syncState.failedQueueCount > 0 || syncState.pendingQueueCount > 0) {
            val isWarn = !syncState.isOnline || syncState.failedQueueCount > 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 22.dp,
                        surfaceColor = if (isWarn) ClayHoney else ClayVanilla,
                        elevation = 8.dp,
                        ledgeDepth = 4.dp
                    )
                    .clickable(onClick = onRetrySync)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("offline_sync_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (!syncState.isOnline) Icons.Rounded.CloudOff else Icons.Rounded.Sync,
                    contentDescription = "Connection status",
                    tint = if (isWarn) ClayTextCream else ClayDeepTealInk
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (isWarn) {
                        Text3D(
                            text = if (!syncState.isOnline) "No internet right now — saved for later" else "Some messages are waiting to send",
                            style = MaterialTheme.typography.labelLarge,
                            depthDp = 1.2.dp
                        )
                        Text3D(
                            text = if (syncState.failedQueueCount > 0) "Tap to try again" else syncState.lastSyncedLabel,
                            style = MaterialTheme.typography.bodySmall,
                            depthDp = 0.9.dp
                        )
                    } else {
                        ClayTextOnLight(
                            text = "Sending family updates…",
                            style = MaterialTheme.typography.labelLarge,
                            depthDp = 1.dp
                        )
                        ClayTextOnLight(
                            text = syncState.lastSyncedLabel,
                            style = MaterialTheme.typography.bodySmall,
                            textColor = ClayMutedInk,
                            depthDp = 0.7.dp
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = quietHoursActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(22.dp),
                        surfaceAlpha = 0.86f,
                        tintColor = ClayVanillaLight,
                        cornerRadius = 22.dp
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("quiet_hours_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bedtime,
                    contentDescription = "Quiet Hours active",
                    tint = ClayAquaDeep
                )
                Spacer(modifier = Modifier.width(8.dp))
                ClayTextOnLight(
                    text = "Quiet Hours On — Soft chimes only",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    textColor = ClayDeepTealInk,
                    depthDp = 1.dp
                )
            }
        }
    }
}

@Composable
fun EmojiPhysicsBurstOverlay(
    bursts: List<EmojiBurstEvent>,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier
) {
    if (bursts.isEmpty()) return
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        bursts.forEach { burst ->
            var launched by remember(burst.id) { mutableStateOf(false) }
            LaunchedEffect(burst.id) { launched = true }
            val progress by animateFloatAsState(
                targetValue = if (launched) 1f else 0f,
                animationSpec = tween(
                    durationMillis = if (reducedMotion) 250 else 1600,
                    easing = LinearEasing
                ),
                label = "emoji_flight_${burst.id}"
            )
            val yOffset = h * (0.78f - progress * 0.65f)
            val xOffset = w * burst.startXFraction
            Text(
                text = burst.emoji,
                fontSize = (34 + (1f - progress) * 14).sp,
                modifier = Modifier
                    .offset(x = xOffset, y = yOffset)
            )
        }
    }
}
