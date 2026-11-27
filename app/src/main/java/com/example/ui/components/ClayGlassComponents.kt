package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.RocketLaunch
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.BadgeElement
import com.example.data.model.EmojiBurstEvent
import com.example.data.model.FamilyMemberId
import com.example.data.model.OfflineSyncState
import com.example.data.model.defaultAvatarRes
import com.example.data.model.haloColor
import com.example.ui.WorkspaceTab
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.CitrusLime
import com.example.ui.theme.CoralSalmon
import com.example.ui.theme.CrimsonDecline
import com.example.ui.theme.DarkBackgroundBottom
import com.example.ui.theme.DarkBackgroundTop
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LightBackgroundBottom
import com.example.ui.theme.LightBackgroundTop
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.OchreGold
import com.example.ui.theme.OchreGoldBright
import com.example.ui.theme.OchreGoldSoft
import com.example.ui.theme.PowderSky
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealDeep
import com.example.ui.theme.TabLightCall
import com.example.ui.theme.TabLightChat
import com.example.ui.theme.TabLightHome
import com.example.ui.theme.TabLightPlay
import com.example.ui.theme.clayPressScale
import com.example.ui.theme.claymorphic
import com.example.ui.theme.liquidGlass

/**
 * Atmospheric Retro-Modern Studio Canvas:
 * Subtle architectural grid + soft Studio Teal, Coral Salmon, Powder Sky, and Ochre Gold
 * ambient light orbs behind Glassmorphic surfaces.
 */
@Composable
fun KinNestAmbientCanvas(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val brush = if (darkTheme) {
        Brush.verticalGradient(listOf(DarkBackgroundTop, DarkBackgroundBottom))
    } else {
        Brush.verticalGradient(listOf(LightBackgroundTop, LightBackgroundBottom))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Soft atmospheric color orbs for Glassmorphism refraction
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        StudioTeal.copy(alpha = if (darkTheme) 0.28f else 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.15f, size.height * 0.12f),
                    radius = size.width * 0.65f
                ),
                center = Offset(size.width * 0.15f, size.height * 0.12f),
                radius = size.width * 0.65f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CoralSalmon.copy(alpha = if (darkTheme) 0.24f else 0.19f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.34f),
                    radius = size.width * 0.6f
                ),
                center = Offset(size.width * 0.88f, size.height * 0.34f),
                radius = size.width * 0.6f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        OchreGoldBright.copy(alpha = if (darkTheme) 0.18f else 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.24f, size.height * 0.78f),
                    radius = size.width * 0.58f
                ),
                center = Offset(size.width * 0.24f, size.height * 0.78f),
                radius = size.width * 0.58f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        PowderSky.copy(alpha = if (darkTheme) 0.12f else 0.30f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.88f),
                    radius = size.width * 0.55f
                ),
                center = Offset(size.width * 0.8f, size.height * 0.88f),
                radius = size.width * 0.55f
            )

            // Crisp subtle editorial studio grid lines inspired by the reference boards
            val step = 36.dp.toPx()
            val gridColor = if (darkTheme) {
                Color.White.copy(alpha = 0.035f)
            } else {
                StudioTealDeep.copy(alpha = 0.045f)
            }
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                y += step
            }
        }
        content()
    }
}

/**
 * Elegant Comic-World Storyboard Hero Showcase:
 * Combines Franco-Belgian ligne-claire ink framing, subtle Canvas halftone dot accents,
 * collector-issue pills, and a frosted Liquid Glass caption bar.
 */
@Composable
fun ComicStoryboardBanner(
    issueBadge: String,
    secondaryBadge: String,
    headline: String,
    caption: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    height: Dp = 216.dp,
    onClick: (() -> Unit)? = null
) {
    val a11y = LocalKinNestAccessibility.current
    val interactionModifier = if (onClick != null) {
        Modifier
            .clayPressScale(reducedMotion = a11y.reducedMotion)
            .clickable(onClick = onClick)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(interactionModifier)
            .claymorphic(
                cornerRadius = 30.dp,
                surfaceColor = Color(0xFFFFFBF2),
                elevation = 14.dp
            )
            .border(1.5.dp, Color(0xFF15181C), RoundedCornerShape(30.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .border(1.5.dp, Color(0xFF15181C), RoundedCornerShape(22.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_comic_universe_hero),
                contentDescription = headline,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Elegant gradient scrim + refined comic halftone dot matrix overlay in top corners
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x26092D30),
                                Color.Transparent,
                                Color(0xCC0A2229)
                            )
                        )
                    )
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val dotSpacing = 11.dp.toPx()
                val cols = 9
                val rows = 5
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        val fade = (1f - (c.toFloat() / cols)) * (1f - (r.toFloat() / rows))
                        if (fade > 0.08f) {
                            drawCircle(
                                color = Color(0xFFFFFBF2).copy(alpha = 0.28f * fade),
                                radius = (2.1.dp.toPx()) * (0.45f + 0.55f * fade),
                                center = Offset(
                                    x = 14.dp.toPx() + c * dotSpacing,
                                    y = 14.dp.toPx() + r * dotSpacing
                                )
                            )
                        }
                    }
                }
            }

            // Collector Issue Badge (Top-Left)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CitrusLime,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .border(1.2.dp, Color(0xFF15181C), RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = issueBadge.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF15181C),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            // Edition Pill (Top-Right)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = OchreGoldBright,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .border(1.2.dp, Color(0xFF15181C), RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = secondaryBadge.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF15181C),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            // Bottom Frosted Liquid Glass Storyboard Plate
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(10.dp)
                    .liquidGlass(
                        shape = RoundedCornerShape(18.dp),
                        surfaceAlpha = 0.42f,
                        tintColor = Color(0xFF0E2A30),
                        reducedMotion = a11y.reducedMotion,
                        cornerRadius = 18.dp
                    )
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CitrusLime)
                        .border(1.2.dp, Color(0xFF15181C), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = Color(0xFF15181C),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Structured Editorial Section Header with tracked uppercase eyebrow & crisp display title.
 */
@Composable
fun EditorialSectionHeader(
    eyebrow: String,
    title: String,
    accentColor: Color = StudioTealDeep,
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
                Text(
                    text = eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
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
    modifier: Modifier = Modifier
) {
    val halo = member.haloColor()
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(halo.copy(alpha = 0.18f))
                .border(3.dp, halo, CircleShape)
                .padding(3.5.dp)
                .clip(CircleShape)
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
        if (isOnline != null) {
            val dotSize = (size * 0.24f).coerceIn(11.dp, 18.dp)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (isOnline) EmeraldAccept else Color(0xFF94A3B8))
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
        if (showBadge) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.36f)
                    .border(1.5.dp, halo.copy(alpha = 0.6f), CircleShape)
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
 * 56dp+ tactile Claymorphic Toy Button with refined Outfit typography.
 */
@Composable
fun ClayActionButton(
    text: String,
    containerColor: Color,
    contentColor: Color = Color.White,
    icon: ImageVector? = null,
    testTag: String,
    modifier: Modifier = Modifier,
    minHeight: Dp = 58.dp,
    onClick: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    Box(
        modifier = modifier
            .height(minHeight)
            .clayPressScale(reducedMotion = a11y.reducedMotion)
            .claymorphic(cornerRadius = 26.dp, surfaceColor = containerColor, elevation = 10.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp)
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
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Sculpted Liquid Glass Light-Up Bottom Menu Dock:
 * Inspired by the attached translucent Liquid Glass light switch + ocean/coral/butter palette.
 * Tapping any menu item smoothly "switches ON" a glowing liquid glass orb & luminous under-glow
 * in that tab's signature color.
 */
@Composable
fun KinNestToyBottomDock(
    selectedTab: WorkspaceTab,
    unreadCount: Int,
    onSelectTab: (WorkspaceTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val a11y = LocalKinNestAccessibility.current
    val (_, activeGlowColor) = tabVisuals(selectedTab)
    val animatedBarGlow by animateColorAsState(
        targetValue = activeGlowColor,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 260f),
        label = "dock_bar_glow"
    )
    val isDarkSurface = MaterialTheme.colorScheme.background.red < 0.25f
    val glassBaseTop = if (isDarkSurface) Color(0xCC1B3447) else Color(0xD8ECECEC)
    val glassBaseBottom = if (isDarkSurface) Color(0xB80E202E) else Color(0xC2DFE6EC)
    val inactiveTint = if (isDarkSurface) Color(0xFFB5CBD4) else Color(0xFF475569)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Outer sculpted Liquid Glass capsule with subtle ambient glow from the active "switched-on" tab
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(36.dp),
                    clip = false,
                    ambientColor = animatedBarGlow.copy(alpha = 0.42f),
                    spotColor = animatedBarGlow.copy(alpha = 0.48f)
                )
                .clip(RoundedCornerShape(36.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(glassBaseTop, glassBaseBottom)
                    )
                )
                .drawBehind {
                    val cornerPx = 36.dp.toPx()
                    // Soft inner ambient light spill from the active switched-on tab
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                animatedBarGlow.copy(alpha = 0.16f),
                                Color.Transparent
                            ),
                            center = Offset(
                                x = size.width * ((selectedTab.index + 0.5f) / WorkspaceTab.entries.size),
                                y = size.height * 0.6f
                            ),
                            radius = size.width * 0.34f
                        ),
                        cornerRadius = CornerRadius(cornerPx, cornerPx)
                    )
                    // Specular liquid glass bevel rim (bright white top highlight + crisp reflective bottom)
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.88f),
                                Color.White.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.62f)
                            )
                        ),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
                .padding(horizontal = 6.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WorkspaceTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val (icon, tabLightColor) = tabVisuals(tab)
                    val lightIntensity by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0f,
                        animationSpec = if (a11y.reducedMotion) tween(100) else spring(
                            dampingRatio = 0.62f,
                            stiffness = 380f
                        ),
                        label = "tab_light_${tab.name}"
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected && !a11y.reducedMotion) 1.12f else 1f,
                        animationSpec = spring(dampingRatio = 0.52f, stiffness = 420f),
                        label = "tab_scale_${tab.name}"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .drawBehind {
                                if (lightIntensity > 0.02f) {
                                    // Luminous under-glow spilling beneath the switched-on glass pill (Image 1 effect)
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.72f * lightIntensity),
                                                tabLightColor.copy(alpha = 0.55f * lightIntensity),
                                                Color.Transparent
                                            ),
                                            center = Offset(size.width * 0.5f, size.height * 0.78f),
                                            radius = size.maxDimension * 0.68f
                                        ),
                                        center = Offset(size.width * 0.5f, size.height * 0.78f),
                                        radius = size.maxDimension * 0.68f
                                    )
                                }
                            }
                            .clip(RoundedCornerShape(28.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelectTab(tab) }
                            )
                            .drawBehind {
                                if (lightIntensity > 0.02f) {
                                    val r = 28.dp.toPx()
                                    // Sculpted translucent Liquid Glass lens pill tinted with the tab's illuminated color
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                tabLightColor.copy(alpha = 0.88f * lightIntensity),
                                                tabLightColor.copy(alpha = 0.68f * lightIntensity)
                                            )
                                        ),
                                        cornerRadius = CornerRadius(r, r)
                                    )
                                    // Glossy top-dome liquid glass reflection highlight
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.56f * lightIntensity),
                                                Color.White.copy(alpha = 0.08f * lightIntensity),
                                                Color.Transparent
                                            ),
                                            startY = 0f,
                                            endY = size.height * 0.58f
                                        ),
                                        cornerRadius = CornerRadius(r, r)
                                    )
                                    // Crisp liquid glass specular rim
                                    drawRoundRect(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.95f * lightIntensity),
                                                Color.White.copy(alpha = 0.35f * lightIntensity),
                                                Color.White.copy(alpha = 0.75f * lightIntensity)
                                            )
                                        ),
                                        cornerRadius = CornerRadius(r, r),
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                }
                            }
                            .testTag("tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                // Illuminated bulb halo directly behind the icon when switched ON
                                if (lightIntensity > 0.05f) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .drawBehind {
                                                drawCircle(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(
                                                            Color.White.copy(alpha = 0.75f * lightIntensity),
                                                            Color.White.copy(alpha = 0.18f * lightIntensity),
                                                            Color.Transparent
                                                        )
                                                    )
                                                )
                                            }
                                    )
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = tab.accessibleTitle,
                                    tint = if (isSelected) {
                                        if (tab == WorkspaceTab.PLAY) Color(0xFF0D1B2A) else Color.White
                                    } else {
                                        inactiveTint
                                    },
                                    modifier = Modifier
                                        .size(23.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                                if (tab == WorkspaceTab.CHAT && unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 8.dp, y = (-4).dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(CrimsonDecline)
                                            .border(1.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = unreadCount.coerceAtMost(9).toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    letterSpacing = 0.6.sp
                                ),
                                color = if (isSelected) {
                                    if (tab == WorkspaceTab.PLAY) Color(0xFF0D1B2A) else Color.White
                                } else {
                                    inactiveTint
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }
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
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        WorkspaceTab.entries.forEach { tab ->
            val (icon, tabColor) = tabVisuals(tab)
            NavigationRailItem(
                selected = tab == selectedTab,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.accessibleTitle,
                        tint = tabColor
                    )
                },
                label = { Text(tab.label) },
                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
            )
        }
    }
}

private fun tabVisuals(tab: WorkspaceTab): Pair<ImageVector, Color> = when (tab) {
    WorkspaceTab.HOME -> Icons.Rounded.Cottage to TabLightHome
    WorkspaceTab.CALL -> Icons.Rounded.RocketLaunch to TabLightCall
    WorkspaceTab.CHAT -> Icons.Rounded.Forum to TabLightChat
    WorkspaceTab.PLAY -> Icons.Rounded.GridView to TabLightPlay
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 20.dp,
                        surfaceColor = if (!syncState.isOnline || syncState.failedQueueCount > 0) {
                            OchreGoldSoft
                        } else {
                            PowderSky
                        },
                        elevation = 6.dp
                    )
                    .clickable(onClick = onRetrySync)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("offline_sync_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (!syncState.isOnline) Icons.Rounded.CloudOff else Icons.Rounded.Sync,
                    contentDescription = "Connection status",
                    tint = Color(0xFF78350F)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when {
                            !syncState.isOnline -> "No internet right now — saved for later"
                            syncState.failedQueueCount > 0 -> "Some messages are waiting to send"
                            else -> "Sending family updates…"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF78350F)
                    )
                    Text(
                        text = if (syncState.failedQueueCount > 0) "Tap to try again" else syncState.lastSyncedLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }

        AnimatedVisibility(visible = quietHoursActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        shape = RoundedCornerShape(20.dp),
                        surfaceAlpha = 0.72f,
                        tintColor = PowderSky,
                        cornerRadius = 20.dp
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("quiet_hours_banner"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bedtime,
                    contentDescription = "Quiet Hours active",
                    tint = StudioTealDeep
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quiet Hours On — Soft chimes only",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioTealDeep,
                    fontWeight = FontWeight.SemiBold
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
