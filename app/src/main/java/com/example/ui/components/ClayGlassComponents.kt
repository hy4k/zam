package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.ui.theme.ClayAquaLight
import com.example.ui.theme.ClayCoral
import com.example.ui.theme.ClayCoralDeep
import com.example.ui.theme.ClayCoralLight
import com.example.ui.theme.ClayDeepTealInk
import com.example.ui.theme.ClayHoney
import com.example.ui.theme.ClayHoneyDeep
import com.example.ui.theme.ClayHoneyLight
import com.example.ui.theme.ClayMutedInk
import com.example.ui.theme.ClayPeach
import com.example.ui.theme.ClayPeachDeep
import com.example.ui.theme.ClaySage
import com.example.ui.theme.ClaySageLight
import com.example.ui.theme.ClayTealShadow
import com.example.ui.theme.ClayTealWallBottom
import com.example.ui.theme.ClayTealWallMid
import com.example.ui.theme.ClayTealWallTop
import com.example.ui.theme.ClayTextCream
import com.example.ui.theme.ClayTextPeachExtrude
import com.example.ui.theme.ClayTextTealCast
import com.example.ui.theme.ClayVanillaLight
import com.example.ui.theme.DarkBackgroundBottom
import com.example.ui.theme.DarkBackgroundTop
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.VibrantViolet
import com.example.ui.theme.VibrantVioletDeep
import com.example.ui.theme.VibrantYellow
import com.example.ui.theme.clayDisc3D
import com.example.ui.theme.clayPressScale
import com.example.ui.theme.claymorphic
import com.example.ui.theme.liquidGlass

/**
 * Refined Sculpted Light-on-Dark Typography for Carved Walnut, Terracotta, Honey Oak, and Sage surfaces.
 * Clean, sharp, warm editorial depth without visual clutter.
 */
@Composable
fun Text3D(
    text: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier,
    textColor: Color = ClayTextCream,
    extrudeColor: Color = ClayTextPeachExtrude,
    castShadowColor: Color = ClayTextTealCast.copy(alpha = 0.36f),
    depthDp: Dp = 1.2.dp,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val subtleDepth = (depthDp * 0.55f).coerceAtMost(1.4.dp)
    Box(modifier = modifier) {
        if (subtleDepth > 0.dp) {
            Text(
                text = text,
                style = style.copy(shadow = null),
                color = castShadowColor,
                textAlign = textAlign,
                maxLines = maxLines,
                overflow = overflow,
                modifier = Modifier.offset(y = subtleDepth)
            )
        }
        Text(
            text = text,
            style = style.copy(
                shadow = if (subtleDepth > 0.dp) {
                    Shadow(
                        color = extrudeColor.copy(alpha = 0.28f),
                        offset = Offset(0f, 1.5f),
                        blurRadius = 2.5f
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
 * High-Contrast Warm Editorial Espresso Ink Typography for Alabaster Cream & Oat-Sand Surfaces.
 * Guaranteed razor-sharp legibility matching the studio reference image.
 */
@Composable
fun ClayTextOnLight(
    text: String,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    modifier: Modifier = Modifier,
    textColor: Color = ClayDeepTealInk,
    depthDp: Dp = 0.8.dp,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = text,
        style = style.copy(
            shadow = if (depthDp > 0.5.dp) {
                Shadow(
                    color = Color(0x1613263C),
                    offset = Offset(0f, 1.2f),
                    blurRadius = 2f
                )
            } else null
        ),
        color = textColor,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        modifier = modifier
    )
}

/**
 * VIBRANT 3D CLAY STUDIO CANVAS:
 * Bright pastel aqua-to-lavender sunlit studio backdrop with soft luminous pastel bokeh orbs
 * and tiny joyful floating clay confetti dots along the outer margins (matching the 3D reference style
 * while keeping the center ultra-clean and crisp for iPhone-grade legibility).
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
                0.55f to DarkBackgroundBottom,
                1.0f to Color(0xFF1A1E36)
            )
        )
    } else {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to ClayTealWallTop,
                0.48f to ClayTealWallMid,
                1.0f to ClayTealWallBottom
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

            // Upper-left bright sky-cyan sunlit glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (darkTheme) ClayAqua.copy(alpha = 0.14f) else ClayAquaLight.copy(alpha = 0.24f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.08f),
                    radius = size.width * 0.72f
                ),
                center = Offset(size.width * 0.12f, size.height * 0.08f),
                radius = size.width * 0.72f
            )

            // Upper-right warm peach-mango studio glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (darkTheme) ClayHoney.copy(alpha = 0.12f) else ClayHoneyLight.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.90f, size.height * 0.16f),
                    radius = size.width * 0.66f
                ),
                center = Offset(size.width * 0.90f, size.height * 0.16f),
                radius = size.width * 0.66f
            )

            // Lower-right playful periwinkle lavender ambient aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (darkTheme) VibrantViolet.copy(alpha = 0.16f) else VibrantViolet.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.86f, size.height * 0.88f),
                    radius = size.width * 0.78f
                ),
                center = Offset(size.width * 0.86f, size.height * 0.88f),
                radius = size.width * 0.78f
            )

            // Lower-left soft mint seafoam bokeh glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        if (darkTheme) ClaySage.copy(alpha = 0.12f) else ClaySageLight.copy(alpha = 0.20f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.10f, size.height * 0.78f),
                    radius = size.width * 0.60f
                ),
                center = Offset(size.width * 0.10f, size.height * 0.78f),
                radius = size.width * 0.60f
            )

            // Cheerful subtle 3D clay confetti dots in the outer margins (inspired by Reference Image 2)
            val dotAlpha = if (darkTheme) 0.24f else 0.36f
            val confetti = listOf(
                Triple(Offset(size.width * 0.07f, size.height * 0.14f), 4.5.dp.toPx(), VibrantYellow),
                Triple(Offset(size.width * 0.93f, size.height * 0.11f), 5.dp.toPx(), ClayCoralLight),
                Triple(Offset(size.width * 0.95f, size.height * 0.34f), 4.dp.toPx(), ClayAqua),
                Triple(Offset(size.width * 0.05f, size.height * 0.44f), 4.5.dp.toPx(), ClayCoralLight),
                Triple(Offset(size.width * 0.94f, size.height * 0.64f), 5.dp.toPx(), VibrantViolet),
                Triple(Offset(size.width * 0.06f, size.height * 0.74f), 4.dp.toPx(), ClayHoneyLight),
                Triple(Offset(size.width * 0.91f, size.height * 0.84f), 4.5.dp.toPx(), ClaySage)
            )
            confetti.forEach { (pos, radius, tint) ->
                drawCircle(
                    color = tint.copy(alpha = dotAlpha),
                    radius = radius,
                    center = pos
                )
                drawCircle(
                    color = Color.White.copy(alpha = dotAlpha * 0.85f),
                    radius = radius * 0.36f,
                    center = Offset(pos.x - radius * 0.26f, pos.y - radius * 0.26f)
                )
            }
        }
        content()
    }
}

/**
 * Refined Sculpted Circular Studio Button (matte oat-sand, honey oak, or carved walnut disc).
 */
@Composable
fun ClayCircleButton(
    icon: ImageVector,
    contentDescription: String,
    clayColor: Color = ClayPeach,
    iconTint: Color = ClayAqua,
    size: Dp = 50.dp,
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
                elevation = 8.dp,
                shadowTint = ClayTealShadow,
                ledgeDepth = 3.dp
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

@Composable
fun PlushCircleButton(
    icon: ImageVector,
    contentDescription: String,
    plushColor: Color,
    iconTint: Color = ClayTextCream,
    size: Dp = 50.dp,
    furLength: Dp = 3.dp,
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
    iconTint: Color = ClayTextCream,
    size: Dp = 48.dp,
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
 * Refined Editorial Pill Action Button (soft sand, carved walnut, terracotta, or sage).
 */
@Composable
fun ClayActionButton(
    text: String,
    containerColor: Color,
    contentColor: Color = ClayTextCream,
    icon: ImageVector? = null,
    testTag: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 52.dp,
    onClick: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    val isLightSurface = containerColor.luminance() > 0.68f
    val resolvedTextColor = if (isLightSurface) ClayDeepTealInk else contentColor

    Box(
        modifier = modifier
            .height(minHeight)
            .clayPressScale(reducedMotion = a11y.reducedMotion)
            .claymorphic(
                cornerRadius = minHeight / 2,
                surfaceColor = containerColor,
                elevation = 8.dp,
                shadowTint = ClayTealShadow,
                ledgeDepth = 3.5.dp
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp)
            .padding(bottom = 2.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = resolvedTextColor,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (isLightSurface) {
                ClayTextOnLight(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textColor = resolvedTextColor,
                    depthDp = 0.6.dp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text3D(
                    text = text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textColor = resolvedTextColor,
                    extrudeColor = Color(0x38102840),
                    depthDp = 1.dp,
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
    minHeight: Dp = 52.dp,
    furLength: Dp = 3.dp,
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
                        .size(width = 16.dp, height = 3.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                ClayTextOnLight(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    textColor = ClayMutedInk,
                    depthDp = 0.dp
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            ClayTextOnLight(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textColor = ClayDeepTealInk,
                depthDp = 0.6.dp
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
        isOnline != null -> if (isOnline) EmeraldAccept else Color(0xFF9FB0C2)
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
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = ClayTealShadow.copy(alpha = 0.24f),
                    spotColor = ClayTealShadow.copy(alpha = 0.24f)
                )
                .clip(CircleShape)
                .background(ClayVanillaLight)
                .border(2.2.dp, ClayVanillaLight, CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
                .background(halo.copy(alpha = 0.16f))
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
            val dotSize = (size * 0.24f).coerceIn(10.dp, 15.dp)
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
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.35f)
                    .border(1.4.dp, ClayPeachDeep, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = badge.emoji,
                        fontSize = (size.value * 0.18f).sp
                    )
                }
            }
        }
    }
}

/**
 * VIBRANT iOS-SMOOTH FLOATING FROSTED GLASS BOTTOM DOCK:
 * Crisp porcelain frosted glass pill with 4 juicy 3D clay medallions
 * (Sky Cyan, Joyful Coral, Tangerine Orange, Periwinkle Lavender),
 * spring lift physics, animated color transitions, active capsule indicator, and tactile haptics.
 */
@Composable
fun KinNestToyBottomDock(
    selectedTab: WorkspaceTab,
    unreadCount: Int,
    onSelectTab: (WorkspaceTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val a11y = LocalKinNestAccessibility.current
    val haptic = LocalHapticFeedback.current

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
                    shape = RoundedCornerShape(30.dp),
                    surfaceAlpha = 0.94f,
                    tintColor = ClayVanillaLight,
                    cornerRadius = 30.dp
                )
                .padding(horizontal = 10.dp, vertical = 7.dp)
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
                        targetValue = if (isSelected && !a11y.reducedMotion) 1.09f else 0.95f,
                        animationSpec = spring(dampingRatio = 0.58f, stiffness = 460f),
                        label = "clay_orb_scale_${tab.name}"
                    )
                    val orbLift by animateFloatAsState(
                        targetValue = if (isSelected && !a11y.reducedMotion) -3.0f else 0f,
                        animationSpec = spring(dampingRatio = 0.62f, stiffness = 460f),
                        label = "clay_orb_lift_${tab.name}"
                    )
                    val discColor by animateColorAsState(
                        targetValue = if (isSelected) style.activeClay else ClayPeach,
                        animationSpec = tween(durationMillis = if (a11y.reducedMotion) 0 else 220),
                        label = "clay_orb_color_${tab.name}"
                    )
                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) ClayTextCream else style.activeLabelInk.copy(alpha = 0.85f),
                        animationSpec = tween(durationMillis = if (a11y.reducedMotion) 0 else 220),
                        label = "clay_orb_icon_${tab.name}"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    onSelectTab(tab)
                                }
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
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clayDisc3D(
                                        surfaceColor = discColor,
                                        elevation = if (isSelected) 10.dp else 4.dp,
                                        shadowTint = if (isSelected) style.activeClay else ClayTealShadow,
                                        ledgeDepth = if (isSelected) 3.2.dp else 2.dp
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = style.icon,
                                    contentDescription = tab.accessibleTitle,
                                    tint = iconColor,
                                    modifier = Modifier.size(21.dp)
                                )
                            }

                            if (tab == WorkspaceTab.CHAT && unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 3.dp, y = (-2).dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(ClayCoral)
                                        .border(1.6.dp, ClayVanillaLight, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text3D(
                                        text = unreadCount.coerceAtMost(9).toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        depthDp = 0.6.dp
                                    )
                                }
                            }
                        }

                        ClayTextOnLight(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            ),
                            textColor = if (isSelected) style.activeLabelInk else ClayMutedInk,
                            depthDp = 0.dp,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )

                        Box(
                            modifier = Modifier
                                .size(width = if (isSelected) 16.dp else 0.dp, height = 3.dp)
                                .clip(CircleShape)
                                .background(style.activeClay)
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
        containerColor = ClayVanillaLight.copy(alpha = 0.96f)
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
                        fontWeight = FontWeight.Bold
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
        activeClay = ClayAqua,
        activeLabelInk = ClayAquaDeep
    )
    WorkspaceTab.CALL -> TabClayStyle(
        icon = Icons.Rounded.Call,
        activeClay = ClayCoral,
        activeLabelInk = ClayCoralDeep
    )
    WorkspaceTab.CHAT -> TabClayStyle(
        icon = Icons.Rounded.Forum,
        activeClay = ClayHoney,
        activeLabelInk = ClayHoneyDeep
    )
    WorkspaceTab.PLAY -> TabClayStyle(
        icon = Icons.Rounded.GridView,
        activeClay = VibrantViolet,
        activeLabelInk = VibrantVioletDeep
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
                        cornerRadius = 18.dp,
                        surfaceColor = if (isWarn) ClayHoney else ClayVanillaLight,
                        elevation = 6.dp,
                        ledgeDepth = 2.5.dp
                    )
                    .clickable(onClick = onRetrySync)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
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
                            depthDp = 0.8.dp
                        )
                        Text3D(
                            text = if (syncState.failedQueueCount > 0) "Tap to try again" else syncState.lastSyncedLabel,
                            style = MaterialTheme.typography.bodySmall,
                            depthDp = 0.6.dp
                        )
                    } else {
                        ClayTextOnLight(
                            text = "Sending family updates…",
                            style = MaterialTheme.typography.labelLarge,
                            depthDp = 0.4.dp
                        )
                        ClayTextOnLight(
                            text = syncState.lastSyncedLabel,
                            style = MaterialTheme.typography.bodySmall,
                            textColor = ClayMutedInk,
                            depthDp = 0.dp
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
                        shape = RoundedCornerShape(18.dp),
                        surfaceAlpha = 0.92f,
                        tintColor = ClayVanillaLight,
                        cornerRadius = 18.dp
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("quiet_hours_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bedtime,
                    contentDescription = "Quiet Hours active",
                    tint = ClayAqua
                )
                Spacer(modifier = Modifier.width(8.dp))
                ClayTextOnLight(
                    text = "Quiet Hours On — Soft chimes only",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    textColor = ClayDeepTealInk,
                    depthDp = 0.dp
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
