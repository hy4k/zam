package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.CoralSalmon
import com.example.ui.theme.CoralSalmonDeep
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.OchreGold
import com.example.ui.theme.OchreGoldBright
import com.example.ui.theme.PowderSky
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.StudioTealDeep

private val LuxuryCabinetTop = Color(0xFF0E1D2B)
private val LuxuryCabinetBottom = Color(0xFF07111A)
private val LuxuryTileBg = Color(0xFF132638)
private val LuxuryTileWinBg = Color(0xFF1F384D)
private val ChampagneGold = Color(0xFFF3D250)
private val NeonXPrimary = Color(0xFFFF7A59)
private val NeonXGlow = Color(0xFFEB5E28)
private val NeonOPrimary = Color(0xFF67E8F9)
private val NeonOGlow = Color(0xFF38BDF8)

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
    var pickingSlot by remember { mutableStateOf<XoxoMark?>(null) }

    val activeAccent by animateColorAsState(
        targetValue = when {
            xoxoState.winnerMark == XoxoMark.X -> NeonXPrimary
            xoxoState.winnerMark == XoxoMark.O -> NeonOPrimary
            xoxoState.isDraw -> ChampagneGold
            xoxoState.currentTurn == XoxoMark.X -> NeonXPrimary
            else -> NeonOPrimary
        },
        animationSpec = tween(350),
        label = "xoxo_active_accent"
    )

    val isLiveMode = xoxoState.playMode == XoxoPlayMode.LIVE_TWO_PHONES
    val isSpectator = isLiveMode && selfMember != xoxoState.playerX && selfMember != xoxoState.playerO
    val isMyTurnNow = !xoxoState.isGameOver && (!isLiveMode || xoxoState.currentTurnMember == selfMember)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Luxury XOXO Header + Mode Switcher
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "FAMILY LOUNGE • ROUND ${xoxoState.roundNumber}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = StudioTealDeep
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "XOXO",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = OchreGoldBright.copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "2 OF 3",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color(0xFF785100),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Segmented Live 2-Phone vs Same Phone Switcher
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = LuxuryCabinetTop,
                    shadowElevation = 6.dp,
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = ChampagneGold.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(18.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        ModePillButton(
                            label = "Live",
                            icon = Icons.Rounded.Wifi,
                            selected = isLiveMode,
                            testTag = "xoxo_mode_live",
                            onClick = { onChangePlayMode(XoxoPlayMode.LIVE_TWO_PHONES) }
                        )
                        ModePillButton(
                            label = "1 Phone",
                            icon = Icons.Rounded.PhoneAndroid,
                            selected = !isLiveMode,
                            testTag = "xoxo_mode_local",
                            onClick = { onChangePlayMode(XoxoPlayMode.PASS_AND_PLAY) }
                        )
                    }
                }
            }
        }

        // 2. Luxury Scoreboard & 2-of-3 Player Arena Selector
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = activeAccent.copy(alpha = 0.28f),
                        spotColor = activeAccent.copy(alpha = 0.32f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(listOf(LuxuryCabinetTop, LuxuryCabinetBottom))
                    )
                    .border(
                        width = 1.4.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                ChampagneGold.copy(alpha = 0.5f),
                                Color.White.copy(alpha = 0.12f),
                                activeAccent.copy(alpha = 0.45f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Player X vs Player O Faceoff Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        XoxoPlayerCard(
                            mark = XoxoMark.X,
                            member = xoxoState.playerX,
                            profile = profiles.firstOrNull { it.memberId == xoxoState.playerX.name },
                            score = xoxoState.xScore,
                            isOnline = memberPresence[xoxoState.playerX]?.isOnline == true,
                            isActiveTurn = !xoxoState.isGameOver && xoxoState.currentTurn == XoxoMark.X,
                            isWinner = xoxoState.winnerMark == XoxoMark.X,
                            isSelecting = pickingSlot == XoxoMark.X,
                            accentColor = NeonXPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pickingSlot = if (pickingSlot == XoxoMark.X) null else XoxoMark.X
                            }
                        )

                        // Center Swap & Draws Column
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSwapSides()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, ChampagneGold.copy(alpha = 0.35f), CircleShape)
                                    .testTag("xoxo_swap_sides_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SwapHoriz,
                                    contentDescription = "Swap X and O players",
                                    tint = ChampagneGold,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Text(
                                text = "TIE ${xoxoState.drawScore}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = Color(0xFF9FB6C5)
                            )
                        }

                        XoxoPlayerCard(
                            mark = XoxoMark.O,
                            member = xoxoState.playerO,
                            profile = profiles.firstOrNull { it.memberId == xoxoState.playerO.name },
                            score = xoxoState.oScore,
                            isOnline = memberPresence[xoxoState.playerO]?.isOnline == true,
                            isActiveTurn = !xoxoState.isGameOver && xoxoState.currentTurn == XoxoMark.O,
                            isWinner = xoxoState.winnerMark == XoxoMark.O,
                            isSelecting = pickingSlot == XoxoMark.O,
                            accentColor = NeonOPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pickingSlot = if (pickingSlot == XoxoMark.O) null else XoxoMark.O
                            }
                        )
                    }

                    // 1-Tap "Pick Any 2 Players" Roster Bar (all 3 family members always visible)
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
                                XoxoMark.X -> NeonXPrimary.copy(alpha = 0.22f)
                                XoxoMark.O -> NeonOPrimary.copy(alpha = 0.22f)
                                null -> Color.White.copy(alpha = 0.06f)
                            }
                            val borderColor = when (assignedMark) {
                                XoxoMark.X -> NeonXPrimary
                                XoxoMark.O -> NeonOPrimary
                                null -> Color.White.copy(alpha = 0.16f)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(pillColor)
                                    .border(1.2.dp, borderColor, RoundedCornerShape(14.dp))
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
                                                // Instantly swap the benched 3rd member into the match as Player O
                                                // (preserving self if self is currently playing)
                                                if (selfMember == xoxoState.playerO) {
                                                    onSelectPlayers(member, selfMember)
                                                } else {
                                                    onSelectPlayers(xoxoState.playerX, member)
                                                }
                                            }
                                            else -> {
                                                // Tap an active player to pick who sits in that slot
                                                pickingSlot = assignedMark
                                            }
                                        }
                                    }
                                    .padding(horizontal = 8.dp)
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
                                            .background(if (online) EmeraldAccept else Color(0xFF64748B))
                                    )
                                    Text(
                                        text = member.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (assignedMark != null) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (assignedMark == XoxoMark.X) NeonXPrimary else NeonOPrimary
                                        ) {
                                            Text(
                                                text = assignedMark.name,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp
                                                ),
                                                color = Color(0xFF0A141E),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (pickingSlot != null) {
                        Text(
                            text = "Tap any family member above to assign as Player ${pickingSlot!!.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneGold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 3. Turn / Victory Status Pill
        item {
            val statusText = when {
                xoxoState.winnerMember != null -> "${xoxoState.winnerMember!!.displayName} Wins Round ${xoxoState.roundNumber}!"
                xoxoState.isDraw -> "It's a Friendly Draw!"
                isSpectator -> "Watching Live • ${xoxoState.currentTurnMember.displayName}'s Turn (${xoxoState.currentTurn.name})"
                isLiveMode && isMyTurnNow -> "Your Turn! Place ${xoxoState.currentTurn.name}"
                isLiveMode -> "Waiting for ${xoxoState.currentTurnMember.displayName} (${xoxoState.currentTurn.name})…"
                else -> "${xoxoState.currentTurnMember.displayName}'s Turn (${xoxoState.currentTurn.name})"
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (xoxoState.isGameOver) {
                    activeAccent.copy(alpha = 0.22f)
                } else {
                    LuxuryCabinetTop.copy(alpha = 0.92f)
                },
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .border(
                        width = 1.5.dp,
                        color = activeAccent.copy(alpha = if (xoxoState.isGameOver) 0.9f else 0.55f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .testTag("xoxo_status_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (xoxoState.winnerMember != null) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Winner",
                            tint = ChampagneGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(activeAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (xoxoState.isGameOver) MaterialTheme.colorScheme.onBackground else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 4. Sculpted Luxury 3x3 XOXO Game Board
        item {
            LuxuryXoxoBoard(
                state = xoxoState,
                activeAccent = activeAccent,
                reducedMotion = a11y.reducedMotion,
                onTapCell = { idx ->
                    if (!xoxoState.isGameOver && xoxoState.board[idx].isEmpty()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    onMakeMove(idx)
                },
                modifier = Modifier
                    .widthIn(max = 372.dp)
                    .fillMaxWidth()
            )
        }

        // 5. Game Controls Row (Next Round / Reset)
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ClayActionButton(
                    text = if (xoxoState.isGameOver) "Play Next Round" else "Restart Round",
                    containerColor = if (xoxoState.isGameOver) EmeraldAccept else StudioTealDeep,
                    icon = Icons.Rounded.Refresh,
                    testTag = "xoxo_next_round_button",
                    minHeight = 56.dp,
                    modifier = Modifier.weight(1.4f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNextRound()
                    }
                )
                ClayActionButton(
                    text = "Reset",
                    containerColor = Color(0xFF1F3444),
                    contentColor = Color(0xFFE2EEF5),
                    icon = Icons.Rounded.RestartAlt,
                    testTag = "xoxo_reset_scores_button",
                    minHeight = 56.dp,
                    modifier = Modifier.weight(0.85f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onResetScores()
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }
    }
}

@Composable
private fun ModePillButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    Brush.horizontalGradient(listOf(StudioTeal, PowderSky))
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) Color(0xFF0A1620) else Color(0xFF9EBAC9),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                ),
                color = if (selected) Color(0xFF0A1620) else Color(0xFFB5CBD4)
            )
        }
    }
}

@Composable
private fun XoxoPlayerCard(
    mark: XoxoMark,
    member: FamilyMemberId,
    profile: UserProfileEntity?,
    score: Int,
    isOnline: Boolean,
    isActiveTurn: Boolean,
    isWinner: Boolean,
    isSelecting: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardScale by animateFloatAsState(
        targetValue = if (isActiveTurn || isWinner) 1.02f else 0.98f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 360f),
        label = "player_card_scale_${mark.name}"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isActiveTurn || isWinner) {
                    accentColor.copy(alpha = 0.18f)
                } else {
                    Color.White.copy(alpha = 0.05f)
                }
            )
            .border(
                width = if (isSelecting || isActiveTurn || isWinner) 2.dp else 1.dp,
                color = when {
                    isSelecting -> ChampagneGold
                    isWinner -> ChampagneGold
                    isActiveTurn -> accentColor
                    else -> Color.White.copy(alpha = 0.14f)
                },
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("xoxo_slot_${mark.name.lowercase()}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FamilyMemberAvatar(
                member = member,
                customPhotoUri = profile?.customPhotoUri,
                size = 42.dp,
                showBadge = false,
                isOnline = isOnline
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = mark.name,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = accentColor
                    )
                    Text(
                        text = member.title,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "$score ${if (score == 1) "win" else "wins"}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = ChampagneGold
                )
            }
        }
    }
}

@Composable
private fun LuxuryXoxoBoard(
    state: XoxoGameState,
    activeAccent: Color,
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
            .shadow(
                elevation = 24.dp,
                shape = RoundedCornerShape(34.dp),
                ambientColor = activeAccent.copy(alpha = 0.4f),
                spotColor = activeAccent.copy(alpha = 0.45f)
            )
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF152D40), LuxuryCabinetTop, LuxuryCabinetBottom)
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        ChampagneGold.copy(alpha = 0.72f),
                        Color.White.copy(alpha = 0.18f),
                        activeAccent.copy(alpha = 0.65f)
                    )
                ),
                shape = RoundedCornerShape(34.dp)
            )
            .padding(14.dp)
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

                        LuxuryXoxoCell(
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

        // Radiant Animated Winning Strike-Through Beam across the 3 winning tiles
        if (state.winningLine.size == 3 && winLineProgress > 0.01f) {
            val startIdx = state.winningLine.first()
            val endIdx = state.winningLine.last()
            val beamColor = if (state.winnerMark == XoxoMark.X) NeonXPrimary else NeonOPrimary

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

                // Outer luminous glow
                drawLine(
                    color = beamColor.copy(alpha = 0.45f),
                    start = p1,
                    end = currentEnd,
                    strokeWidth = 18.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Champagne gold mid-beam
                drawLine(
                    color = ChampagneGold.copy(alpha = 0.92f),
                    start = p1,
                    end = currentEnd,
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // Crisp incandescent white core
                drawLine(
                    color = Color.White,
                    start = p1,
                    end = currentEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun LuxuryXoxoCell(
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
        targetValue = if (isWinningCell && !reducedMotion) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
        label = "xoxo_cell_scale_$index"
    )

    val markGlowColor = when (mark) {
        "X" -> NeonXGlow
        "O" -> NeonOGlow
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = tileScale
                scaleY = tileScale
                alpha = if (isDimmed) 0.42f else 1f
            }
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (isWinningCell) {
                        listOf(LuxuryTileWinBg, Color(0xFF13293A))
                    } else {
                        listOf(LuxuryTileBg, Color(0xFF0C1A27))
                    }
                )
            )
            .border(
                width = if (isWinningCell) 2.dp else 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = if (isWinningCell) {
                        listOf(ChampagneGold, markGlowColor.copy(alpha = 0.8f))
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.16f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    }
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(enabled = mark.isEmpty(), onClick = onClick)
            .testTag("xoxo_cell_$index"),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = mark.isNotEmpty(),
            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.78f, animationSpec = spring(dampingRatio = 0.6f))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                val strokeMain = 9.dp.toPx()
                val strokeGlow = 17.dp.toPx()
                val strokeCore = 3.dp.toPx()

                if (mark == "X") {
                    val p1 = (drawProgress * 2f).coerceIn(0f, 1f)
                    val p2 = ((drawProgress - 0.45f) / 0.55f).coerceIn(0f, 1f)

                    // First diagonal (\)
                    if (p1 > 0f) {
                        val s1 = Offset(0f, 0f)
                        val e1 = Offset(size.width * p1, size.height * p1)
                        drawLine(NeonXGlow.copy(alpha = 0.35f), s1, e1, strokeWidth = strokeGlow, cap = StrokeCap.Round)
                        drawLine(NeonXPrimary, s1, e1, strokeWidth = strokeMain, cap = StrokeCap.Round)
                        drawLine(Color(0xFFFFEDE6), s1, e1, strokeWidth = strokeCore, cap = StrokeCap.Round)
                    }
                    // Second diagonal (/)
                    if (p2 > 0f) {
                        val s2 = Offset(size.width, 0f)
                        val e2 = Offset(size.width * (1f - p2), size.height * p2)
                        drawLine(NeonXGlow.copy(alpha = 0.35f), s2, e2, strokeWidth = strokeGlow, cap = StrokeCap.Round)
                        drawLine(NeonXPrimary, s2, e2, strokeWidth = strokeMain, cap = StrokeCap.Round)
                        drawLine(Color(0xFFFFEDE6), s2, e2, strokeWidth = strokeCore, cap = StrokeCap.Round)
                    }
                } else if (mark == "O") {
                    val sweep = 360f * drawProgress
                    val inset = strokeMain / 2f
                    val arcSize = Size(size.width - strokeMain, size.height - strokeMain)
                    val topLeft = Offset(inset, inset)
                    drawArc(
                        color = NeonOGlow.copy(alpha = 0.35f),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeGlow, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = NeonOPrimary,
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeMain, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFFE0FCFF),
                        startAngle = -90f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeCore, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
