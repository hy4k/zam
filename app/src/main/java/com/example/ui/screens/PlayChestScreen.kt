package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserProfileEntity
import com.example.data.model.FamilyMemberId
import com.example.data.model.MemberPresenceStatus
import com.example.data.model.XoxoGameState
import com.example.data.model.XoxoMark
import com.example.data.model.XoxoPlayMode
import com.example.ui.components.ClayActionButton
import com.example.ui.components.ClayCircleButton
import com.example.ui.components.ClayTextOnLight
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.components.Text3D
import com.example.ui.game.ArcadeAudioEngine
import com.example.ui.game.SkyHopArcadeGame
import com.example.ui.theme.ClayAqua
import com.example.ui.theme.ClayAquaDeep
import com.example.ui.theme.ClayCoral
import com.example.ui.theme.ClayCoralDeep
import com.example.ui.theme.ClayDeepTealInk
import com.example.ui.theme.ClayHoney
import com.example.ui.theme.ClayHoneyLight
import com.example.ui.theme.ClayMutedInk
import com.example.ui.theme.ClayPeach
import com.example.ui.theme.ClaySage
import com.example.ui.theme.ClayTealShadow
import com.example.ui.theme.ClayTextCream
import com.example.ui.theme.ClayVanilla
import com.example.ui.theme.ClayVanillaLight
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.claymorphic
import com.example.ui.theme.liquidGlass

private enum class PlayTabGame { SKY_CATCH, XOXO }

/**
 * Page 4 — Family Play Hub (NO top page title!).
 * Hosts the full-viewport 60fps interactive clay arcade game ("Sky Catch")
 * alongside the live-synced 2-of-3 Family XOXO Arena with procedural audio!
 */
@Composable
fun PlayChestScreen(
    xoxoState: XoxoGameState,
    selfMember: FamilyMemberId,
    profiles: List<UserProfileEntity>,
    memberPresence: Map<FamilyMemberId, MemberPresenceStatus>,
    onSelectPlayers: (playerX: FamilyMemberId, playerO: FamilyMemberId) -> Unit,
    onSwapSides: () -> Unit,
    onChangePlayMode: (XoxoPlayMode) -> Unit,
    onMakeMove: (Int) -> Unit,
    onNextRound: () -> Unit,
    onResetScores: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    val haptic = LocalHapticFeedback.current
    var activeGame by rememberSaveable { mutableStateOf(PlayTabGame.SKY_CATCH) }
    var pickingSlot by remember { mutableStateOf<XoxoMark?>(null) }

    val isLiveMode = xoxoState.playMode == XoxoPlayMode.LIVE_TWO_PHONES
    val isSpectator = isLiveMode && selfMember != xoxoState.playerX && selfMember != xoxoState.playerO
    val isMyTurnNow = !xoxoState.isGameOver && (!isLiveMode || xoxoState.currentTurnMember == selfMember)

    LaunchedEffect(xoxoState.winnerMember, xoxoState.roundNumber) {
        if (activeGame == PlayTabGame.XOXO && xoxoState.winnerMember != null) {
            ArcadeAudioEngine.playVictoryFanfare()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Compact 2-Game Switcher Clay-Glass Pill (NO top page name!)
        Row(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(24.dp),
                    surfaceAlpha = 0.88f,
                    tintColor = ClayVanillaLight,
                    cornerRadius = 24.dp
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PlayGameSwitcherPill(
                label = "Sky Catch ★",
                icon = Icons.Rounded.SportsEsports,
                selected = activeGame == PlayTabGame.SKY_CATCH,
                activeColor = ClayCoral,
                testTag = "play_switch_sky_catch",
                modifier = Modifier.weight(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    activeGame = PlayTabGame.SKY_CATCH
                }
            )
            PlayGameSwitcherPill(
                label = "XOXO 2-of-3",
                icon = Icons.Rounded.GridView,
                selected = activeGame == PlayTabGame.XOXO,
                activeColor = ClayAqua,
                testTag = "play_switch_xoxo",
                modifier = Modifier.weight(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    activeGame = PlayTabGame.XOXO
                }
            )
        }

        if (activeGame == PlayTabGame.SKY_CATCH) {
            // Full-Viewport 60fps Interactive Clay Arcade Game Shell
            SkyHopArcadeGame(
                playerName = selfMember.displayName,
                reducedMotion = a11y.reducedMotion,
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // 1. Minimal Top Bar (NO page name!) — Round pill + Sculpted Live / 1 Phone Switcher
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .claymorphic(
                            cornerRadius = 20.dp,
                            surfaceColor = ClayVanillaLight,
                            elevation = 8.dp,
                            ledgeDepth = 3.5.dp
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    ClayTextOnLight(
                        text = "ROUND ${xoxoState.roundNumber}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        textColor = ClayDeepTealInk,
                        depthDp = 0.8.dp
                    )
                    Surface(
                        shape = CircleShape,
                        color = ClayHoney
                    ) {
                        Text3D(
                            text = "2 OF 3",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            extrudeColor = Color(0x558B4F08),
                            depthDp = 0.8.dp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                // Sculpted Frosted Clay-Glass Live vs 1 Phone Switcher
                Row(
                    modifier = Modifier
                        .liquidGlass(
                            shape = RoundedCornerShape(22.dp),
                            surfaceAlpha = 0.86f,
                            tintColor = ClayVanillaLight,
                            cornerRadius = 22.dp
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ClayModePill(
                        label = "Live",
                        icon = Icons.Rounded.Wifi,
                        selected = isLiveMode,
                        activeColor = ClayAqua,
                        testTag = "xoxo_mode_live",
                        onClick = { onChangePlayMode(XoxoPlayMode.LIVE_TWO_PHONES) }
                    )
                    ClayModePill(
                        label = "1 Phone",
                        icon = Icons.Rounded.PhoneAndroid,
                        selected = !isLiveMode,
                        activeColor = ClayCoral,
                        testTag = "xoxo_mode_local",
                        onClick = { onChangePlayMode(XoxoPlayMode.PASS_AND_PLAY) }
                    )
                }
            }
        }

        // 2. Warm Vanilla-Cream 3D Clay Scoreboard & Any-2-of-3 Picker
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 30.dp,
                        surfaceColor = ClayVanilla,
                        elevation = 14.dp,
                        shadowTint = ClayTealShadow,
                        ledgeDepth = 6.dp
                    )
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioClayXoxoPlayerTile(
                            mark = XoxoMark.X,
                            member = xoxoState.playerX,
                            profile = profiles.firstOrNull { it.memberId == xoxoState.playerX.name },
                            score = xoxoState.xScore,
                            isOnline = memberPresence[xoxoState.playerX]?.isOnline == true,
                            isActiveTurn = !xoxoState.isGameOver && xoxoState.currentTurn == XoxoMark.X,
                            isWinner = xoxoState.winnerMark == XoxoMark.X,
                            isSelecting = pickingSlot == XoxoMark.X,
                            clayColor = ClayCoral,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pickingSlot = if (pickingSlot == XoxoMark.X) null else XoxoMark.X
                            }
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ClayCircleButton(
                                icon = Icons.Rounded.SwapHoriz,
                                contentDescription = "Swap X and O players",
                                clayColor = ClayHoney,
                                iconTint = ClayTextCream,
                                size = 44.dp,
                                testTag = "xoxo_swap_sides_button",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSwapSides()
                                }
                            )
                            ClayTextOnLight(
                                text = "TIE ${xoxoState.drawScore}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.5.sp
                                ),
                                textColor = ClayDeepTealInk,
                                depthDp = 0.7.dp
                            )
                        }

                        StudioClayXoxoPlayerTile(
                            mark = XoxoMark.O,
                            member = xoxoState.playerO,
                            profile = profiles.firstOrNull { it.memberId == xoxoState.playerO.name },
                            score = xoxoState.oScore,
                            isOnline = memberPresence[xoxoState.playerO]?.isOnline == true,
                            isActiveTurn = !xoxoState.isGameOver && xoxoState.currentTurn == XoxoMark.O,
                            isWinner = xoxoState.winnerMark == XoxoMark.O,
                            isSelecting = pickingSlot == XoxoMark.O,
                            clayColor = ClayAqua,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pickingSlot = if (pickingSlot == XoxoMark.O) null else XoxoMark.O
                            }
                        )
                    }

                    // 1-Tap "Pick Any 2 Players" Sculpted Clay Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FamilyMemberId.entries.forEach { member ->
                            val assignedMark = when (member) {
                                xoxoState.playerX -> XoxoMark.X
                                xoxoState.playerO -> XoxoMark.O
                                else -> null
                            }
                            val online = memberPresence[member]?.isOnline == true
                            val pillColor = when (assignedMark) {
                                XoxoMark.X -> ClayPeach
                                XoxoMark.O -> Color(0xFFE7DDD0)
                                null -> ClayVanillaLight
                            }
                            val badgeColor = when (assignedMark) {
                                XoxoMark.X -> ClayCoral
                                XoxoMark.O -> ClayAqua
                                null -> Color.Transparent
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .claymorphic(
                                        cornerRadius = 18.dp,
                                        surfaceColor = pillColor,
                                        elevation = if (assignedMark != null) 7.dp else 4.dp,
                                        ledgeDepth = 3.dp
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        when {
                                            pickingSlot == XoxoMark.X -> {
                                                val other = if (member == xoxoState.playerO) xoxoState.playerX else xoxoState.playerO
                                                onSelectPlayers(member, other)
                                                pickingSlot = null
                                            }
                                            pickingSlot == XoxoMark.O -> {
                                                val other = if (member == xoxoState.playerX) xoxoState.playerO else xoxoState.playerX
                                                onSelectPlayers(other, member)
                                                pickingSlot = null
                                            }
                                            assignedMark == null -> {
                                                if (selfMember == xoxoState.playerO) {
                                                    onSelectPlayers(member, selfMember)
                                                } else {
                                                    onSelectPlayers(xoxoState.playerX, member)
                                                }
                                            }
                                            else -> {
                                                pickingSlot = assignedMark
                                            }
                                        }
                                    }
                                    .padding(horizontal = 7.dp)
                                    .testTag("xoxo_pick_${member.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (online) EmeraldAccept else Color(0xFFA89989))
                                    )
                                    ClayTextOnLight(
                                        text = member.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        textColor = ClayDeepTealInk,
                                        depthDp = 0.7.dp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (assignedMark != null) {
                                        Surface(
                                            shape = CircleShape,
                                            color = badgeColor
                                        ) {
                                            Text3D(
                                                text = assignedMark.name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 10.sp
                                                ),
                                                extrudeColor = Color(0x452A170C),
                                                depthDp = 0.8.dp,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (pickingSlot != null) {
                        ClayTextOnLight(
                            text = "Tap a family member above to play as ${pickingSlot!!.name}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            textColor = ClayCoralDeep,
                            depthDp = 0.7.dp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 3. Sculpted 3D Clay Turn / Victory Banner (Aqua or Honey-Gold with Crisp 3D Cream-White Text)
        item {
            val statusText = when {
                xoxoState.winnerMember != null -> "${xoxoState.winnerMember!!.displayName} Wins Round ${xoxoState.roundNumber}!"
                xoxoState.isDraw -> "Friendly Draw! Play Again!"
                isSpectator -> "Watching Live • ${xoxoState.currentTurnMember.displayName}'s Turn"
                isLiveMode && isMyTurnNow -> "Your Turn! Place ${xoxoState.currentTurn.name}"
                isLiveMode -> "Waiting for ${xoxoState.currentTurnMember.displayName} (${xoxoState.currentTurn.name})…"
                else -> "${xoxoState.currentTurnMember.displayName}'s Turn (${xoxoState.currentTurn.name})"
            }
            val bannerClay = when {
                xoxoState.isGameOver -> ClayHoney
                xoxoState.currentTurn == XoxoMark.X -> ClayCoral
                else -> ClayAqua
            }

            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 24.dp,
                        surfaceColor = bannerClay,
                        elevation = 10.dp,
                        shadowTint = ClayTealShadow,
                        ledgeDepth = 4.5.dp
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 11.dp, bottom = 14.dp)
                    .testTag("xoxo_status_banner"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (xoxoState.winnerMember != null) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = "Winner",
                        tint = ClayTextCream,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text3D(
                    text = statusText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    extrudeColor = Color(0x552A170C),
                    depthDp = 1.6.dp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 4. Sculpted Warm Peach-Cream 3D Clay XOXO Board!
        item {
            StudioClayXoxoBoard(
                state = xoxoState,
                reducedMotion = a11y.reducedMotion,
                onTapCell = { idx ->
                    if (!xoxoState.isGameOver && xoxoState.board[idx].isEmpty()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        ArcadeAudioEngine.playBoardTap(isPlayerX = xoxoState.currentTurn == XoxoMark.X)
                    }
                    onMakeMove(idx)
                },
                modifier = Modifier
                    .widthIn(max = 356.dp)
                    .fillMaxWidth()
            )
        }

        // 5. Game Controls Row (Sage/Coral Next Round + Aqua Reset)
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ClayActionButton(
                    text = if (xoxoState.isGameOver) "Play Next Round" else "Restart Round",
                    containerColor = if (xoxoState.isGameOver) ClaySage else ClayCoral,
                    icon = Icons.Rounded.Refresh,
                    testTag = "xoxo_next_round_button",
                    minHeight = 52.dp,
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNextRound()
                    }
                )
                ClayActionButton(
                    text = "Reset",
                    containerColor = ClayAqua,
                    icon = Icons.Rounded.RestartAlt,
                    testTag = "xoxo_reset_scores_button",
                    minHeight = 52.dp,
                    modifier = Modifier.weight(0.85f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onResetScores()
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }
        }
    }
}

@Composable
private fun PlayGameSwitcherPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .then(
                if (selected) {
                    Modifier.claymorphic(
                        cornerRadius = 19.dp,
                        surfaceColor = activeColor,
                        elevation = 8.dp,
                        shadowTint = ClayTealShadow,
                        ledgeDepth = 3.dp
                    )
                } else {
                    Modifier.clip(CircleShape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) ClayTextCream else ClayDeepTealInk,
                modifier = Modifier.size(18.dp)
            )
            if (selected) {
                Text3D(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                    extrudeColor = Color(0x502A170C),
                    depthDp = 1.dp,
                    maxLines = 1
                )
            } else {
                ClayTextOnLight(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    textColor = ClayDeepTealInk,
                    depthDp = 0.6.dp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ClayModePill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(if (selected) activeColor else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) ClayTextCream else ClayMutedInk,
                modifier = Modifier.size(15.dp)
            )
            if (selected) {
                Text3D(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    extrudeColor = Color(0x452A170C),
                    depthDp = 0.8.dp
                )
            } else {
                ClayTextOnLight(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    textColor = ClayMutedInk,
                    depthDp = 0.5.dp
                )
            }
        }
    }
}

@Composable
private fun StudioClayXoxoPlayerTile(
    mark: XoxoMark,
    member: FamilyMemberId,
    profile: UserProfileEntity?,
    score: Int,
    isOnline: Boolean,
    isActiveTurn: Boolean,
    isWinner: Boolean,
    isSelecting: Boolean,
    clayColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardScale by animateFloatAsState(
        targetValue = if (isActiveTurn || isWinner) 1.03f else 0.98f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "player_tile_scale_${mark.name}"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .claymorphic(
                cornerRadius = 22.dp,
                surfaceColor = clayColor,
                elevation = if (isActiveTurn || isWinner) 11.dp else 6.dp,
                shadowTint = ClayTealShadow,
                ledgeDepth = 4.dp
            )
            .then(
                if (isSelecting || isActiveTurn || isWinner) {
                    Modifier.border(2.2.dp, ClayHoneyLight, RoundedCornerShape(22.dp))
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 12.dp)
            .testTag("xoxo_slot_${mark.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FamilyMemberAvatar(
                member = member,
                customPhotoUri = profile?.customPhotoUri,
                size = 38.dp,
                showBadge = false,
                isOnline = isOnline
            )
            Column(modifier = Modifier.weight(1f)) {
                Text3D(
                    text = "${mark.name} • ${member.title}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.5.sp
                    ),
                    extrudeColor = Color(0x552A170C),
                    depthDp = 1.dp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text3D(
                    text = "$score ${if (score == 1) "win" else "wins"}",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp
                    ),
                    textColor = ClayVanillaLight,
                    extrudeColor = Color(0x552A170C),
                    depthDp = 1.1.dp
                )
            }
        }
    }
}

@Composable
private fun StudioClayXoxoBoard(
    state: XoxoGameState,
    reducedMotion: Boolean,
    onTapCell: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val winLineProgress by animateFloatAsState(
        targetValue = if (state.winningLine.size == 3) 1f else 0f,
        animationSpec = if (reducedMotion) tween(60) else tween(420, easing = FastOutSlowInEasing),
        label = "xoxo_win_line_progress"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .claymorphic(
                cornerRadius = 34.dp,
                surfaceColor = ClayPeach,
                elevation = 18.dp,
                shadowTint = ClayTealShadow,
                ledgeDepth = 8.dp
            )
            .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 20.dp)
            .testTag("xoxo_game_board")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (row in 0..2) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (col in 0..2) {
                        val idx = row * 3 + col
                        val markStr = state.board[idx]
                        val isWinningCell = idx in state.winningLine
                        val dimCell = state.winningLine.isNotEmpty() && !isWinningCell

                        StudioClayXoxoCell(
                            index = idx,
                            mark = markStr,
                            isWinningCell = isWinningCell,
                            isDimmed = dimCell,
                            reducedMotion = reducedMotion,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            onClick = { onTapCell(idx) }
                        )
                    }
                }
            }
        }

        // Glowing Warm Honey-Cream Winning Beam
        if (state.winningLine.size == 3 && winLineProgress > 0.01f) {
            val startIdx = state.winningLine.first()
            val endIdx = state.winningLine.last()

            Canvas(modifier = Modifier.fillMaxSize()) {
                fun centerOf(index: Int): Offset {
                    val r = index / 3
                    val c = index % 3
                    val cellW = size.width / 3f
                    val cellH = size.height / 3f
                    return Offset(c * cellW + cellW / 2f, r * cellH + cellH / 2f)
                }

                val p1 = centerOf(startIdx)
                val p2 = centerOf(endIdx)
                val currentEnd = Offset(
                    x = p1.x + (p2.x - p1.x) * winLineProgress,
                    y = p1.y + (p2.y - p1.y) * winLineProgress
                )

                drawLine(
                    color = ClayHoney.copy(alpha = 0.62f),
                    start = p1,
                    end = currentEnd,
                    strokeWidth = 18.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = ClayTextCream,
                    start = p1,
                    end = currentEnd,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun StudioClayXoxoCell(
    index: Int,
    mark: String,
    isWinningCell: Boolean,
    isDimmed: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var revealed by remember(mark) { mutableStateOf(false) }
    LaunchedEffect(mark) {
        revealed = mark.isNotEmpty()
    }

    val drawProgress by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = if (reducedMotion) tween(50) else tween(260, easing = FastOutSlowInEasing),
        label = "xoxo_cell_draw_$index"
    )
    val tileScale by animateFloatAsState(
        targetValue = if (isWinningCell && !reducedMotion) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 380f),
        label = "xoxo_cell_scale_$index"
    )

    val tileClayColor = when {
        isWinningCell -> ClayHoney
        mark == "X" -> ClayCoral
        mark == "O" -> ClayAqua
        else -> ClayVanillaLight
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = tileScale
                scaleY = tileScale
                alpha = if (isDimmed) 0.56f else 1f
            }
            .claymorphic(
                cornerRadius = 22.dp,
                surfaceColor = tileClayColor,
                elevation = if (isWinningCell) 12.dp else 7.dp,
                shadowTint = ClayTealShadow,
                ledgeDepth = 4.5.dp
            )
            .clickable(enabled = mark.isEmpty(), onClick = onClick)
            .testTag("xoxo_cell_$index"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = mark.isNotEmpty(),
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.74f, animationSpec = spring(dampingRatio = 0.56f))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                val strokeMain = 11.dp.toPx()
                val strokeShadow = 13.dp.toPx()
                val shadowOffset = 2.6.dp.toPx()

                if (mark == "X") {
                    val p1 = (drawProgress * 2f).coerceIn(0f, 1f)
                    val p2 = ((drawProgress - 0.45f) / 0.55f).coerceIn(0f, 1f)

                    if (p1 > 0f) {
                        val s1 = Offset(0f, 0f)
                        val e1 = Offset(size.width * p1, size.height * p1)
                        drawLine(Color(0x452B190E), s1 + Offset(0f, shadowOffset), e1 + Offset(0f, shadowOffset), strokeWidth = strokeShadow, cap = StrokeCap.Round)
                        drawLine(ClayTextCream, s1, e1, strokeWidth = strokeMain, cap = StrokeCap.Round)
                    }
                    if (p2 > 0f) {
                        val s2 = Offset(size.width, 0f)
                        val e2 = Offset(size.width * (1f - p2), size.height * p2)
                        drawLine(Color(0x452B190E), s2 + Offset(0f, shadowOffset), e2 + Offset(0f, shadowOffset), strokeWidth = strokeShadow, cap = StrokeCap.Round)
                        drawLine(ClayTextCream, s2, e2, strokeWidth = strokeMain, cap = StrokeCap.Round)
                    }
                } else if (mark == "O") {
                    val sweep = 360f * drawProgress
                    val inset = strokeMain / 2f
                    val arcSize = Size(size.width - strokeMain, size.height - strokeMain)
                    val topLeft = Offset(inset, inset)
                    drawArc(
                        color = Color(0x452B190E),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft + Offset(0f, shadowOffset),
                        size = arcSize,
                        style = Stroke(width = strokeShadow, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = ClayTextCream,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeMain, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
