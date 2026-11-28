package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.EncryptedMessageEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.BadgeElement
import com.example.data.model.FamilyMemberId
import com.example.data.model.MessageType
import com.example.data.model.UserSessionState
import com.example.ui.components.FamilyMemberAvatar

// Exact Login Page 3D Claymorphism & Royal Cobalt Palette (sampled directly from AvatarLoginScreen)
private val ChatCardIvoryTop = Color(0xFFF6EFEF)
private val ChatCardIvoryMid = Color(0xFFEDE3D9)
private val ChatCardIvoryBottom = Color(0xFFEAE0DD)
private val ChatTrayIvoryTop = Color(0xFFF7F2F0)
private val ChatTrayIvoryMid = Color(0xFFF0E9E7)
private val ChatTrayIvoryBottom = Color(0xFFE7DFDC)
private val ChatRecessedWellTop = Color(0xFFE2D9D6)
private val ChatRecessedWellMid = Color(0xFFEDE6E3)
private val ChatRecessedWellBottom = Color(0xFFF5F0EE)
private val ChatRoyalBrandBlue = Color(0xFF1553D6)
private val ChatRoyalBlueLight = Color(0xFF428BFF)
private val ChatRoyalBlueCore = Color(0xFF1D62EC)
private val ChatRoyalBlueDeep = Color(0xFF1043BE)
private val ChatSubtitleNavy = Color(0xFF365592)
private val ChatInputTextNavy = Color(0xFF1A3263)
private val ChatPlaceholderSlate = Color(0xFF6B7792)
private val ChatAmbientBlueShadow = Color(0xFF0F2D7A)
private val ChatSoftInnerShadow = Color(0xFF5A4E60)
private val ChatGoldAccent = Color(0xFFF5B82E)

private data class ChatThreadOption(val id: String, val label: String)

/**
 * Page 3 — Family Chat (NO top page title!).
 * Matches the Login Page's exact Cozy Royal-Blue 3D Studio theme:
 * sculpted matte-ivory clay cards, glossy 3D royal-blue pills & circular icon medallions,
 * recessed neumorphic input pill well, and crisp royal-blue / navy typography.
 */
@Composable
fun ChatBubbleScreen(
    session: UserSessionState,
    profiles: List<UserProfileEntity>,
    allMessages: List<EncryptedMessageEntity>,
    onSelectThread: (String) -> Unit,
    onSendText: (String) -> Unit,
    onSendMediaUri: (String, MessageType) -> Unit,
    onPlayVoiceClip: (String?) -> Unit,
    onReactMessage: (String, String) -> Unit,
    onRetrySync: () -> Unit
) {
    val threads = listOf(
        ChatThreadOption("family_group", "All 3 Family"),
        ChatThreadOption("mithun_mafitha", "Vapi & Ummi"),
        ChatThreadOption("mithun_zayn", "Vapi & Zayn"),
        ChatThreadOption("mafitha_zayn", "Ummi & Zayn")
    )
    val threadMessages = allMessages
        .filter { it.threadId == session.selectedThreadId }
        .sortedBy { it.timestampMs }

    var draftText by remember { mutableStateOf("") }
    var inputFocused by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onSendMediaUri(it.toString(), MessageType.PHOTO) }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onSendMediaUri(it.toString(), MessageType.VIDEO_MP4) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Minimal Top Thread Selector Strip (NO page name!) — Login Page Glossy Blue & Ivory Pills
        Row(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                threads.forEach { thread ->
                    val selected = session.selectedThreadId == thread.id
                    ChatLoginStyleThreadPill(
                        label = thread.label,
                        selected = selected,
                        testTag = "chat_thread_${thread.id}",
                        onClick = { onSelectThread(thread.id) }
                    )
                }
            }

            // Glossy 3D Royal-Blue Lock Medallion (matches Login password lock medallion)
            ChatGlossyBlueMedallion(
                icon = Icons.Rounded.Lock,
                contentDescription = "Private 3-member family chat",
                size = 38.dp,
                iconSize = 18.dp,
                onClick = null
            )
        }

        // Sculpted Matte-Ivory & Glossy Royal-Blue Message Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            items(threadMessages, key = { it.id }) { msg ->
                val sender = runCatching { FamilyMemberId.valueOf(msg.senderId) }
                    .getOrDefault(FamilyMemberId.MITHUN)
                val senderProf = profiles.firstOrNull { it.memberId == sender.name }
                val senderBadge = runCatching {
                    BadgeElement.valueOf(senderProf?.badgeElement ?: BadgeElement.STARLIGHT.name)
                }.getOrDefault(BadgeElement.STARLIGHT)
                val isSelf = sender == session.activeMember

                val bubbleShape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = if (isSelf) 24.dp else 10.dp,
                    bottomEnd = if (isSelf) 10.dp else 24.dp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (!isSelf) {
                        FamilyMemberAvatar(
                            member = sender,
                            customPhotoUri = senderProf?.customPhotoUri,
                            badge = senderBadge,
                            size = 42.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    // Bubble Card: Outgoing = Glossy 3D Royal Blue; Incoming = Sculpted Matte Ivory Clay
                    Box(
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .shadow(
                                elevation = if (isSelf) 14.dp else 16.dp,
                                shape = bubbleShape,
                                ambientColor = ChatAmbientBlueShadow.copy(alpha = 0.42f),
                                spotColor = ChatAmbientBlueShadow.copy(alpha = 0.42f)
                            )
                            .clip(bubbleShape)
                            .background(
                                if (isSelf) {
                                    Brush.verticalGradient(
                                        listOf(
                                            ChatRoyalBlueLight,
                                            ChatRoyalBlueCore,
                                            ChatRoyalBlueDeep
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        colorStops = arrayOf(
                                            0.0f to ChatCardIvoryTop,
                                            0.5f to ChatCardIvoryMid,
                                            1.0f to ChatCardIvoryBottom
                                        )
                                    )
                                }
                            )
                            .border(
                                width = 1.6.dp,
                                brush = if (isSelf) {
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.62f),
                                            Color.White.copy(alpha = 0.16f),
                                            Color(0xFF0A2C82).copy(alpha = 0.62f)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.96f),
                                            Color(0xFFFDF8F6).copy(alpha = 0.60f),
                                            Color(0xFFC9B7B3).copy(alpha = 0.46f)
                                        )
                                    )
                                },
                                shape = bubbleShape
                            )
                    ) {
                        // Subtle top specular clay highlight band on outgoing glossy blue bubble
                        if (isSelf) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 3.dp, start = 18.dp, end = 18.dp)
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.White.copy(alpha = 0.24f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }

                        Column(
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            // Sender Name + Delivery Status
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${sender.displayName} ${senderBadge.emoji}",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.1).sp,
                                        shadow = if (isSelf) {
                                            Shadow(Color(0x330A2A78), Offset(0f, 1.5f), 3f)
                                        } else {
                                            Shadow(Color(0x22103B9E), Offset(0f, 1.5f), 3f)
                                        }
                                    ),
                                    color = if (isSelf) Color.White else ChatRoyalBrandBlue
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                if (msg.deliveryStatus == "QUEUED_OFFLINE") {
                                    Icon(
                                        imageVector = Icons.Rounded.Schedule,
                                        contentDescription = "Queued offline",
                                        tint = if (isSelf) ChatGoldAccent else ChatRoyalBrandBlue,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable(onClick = onRetrySync)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.DoneAll,
                                        contentDescription = msg.deliveryStatus,
                                        tint = if (isSelf) Color.White.copy(alpha = 0.9f) else ChatRoyalBlueCore,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            val resolvedMediaSource = msg.mediaLocalPath?.takeIf { it.isNotBlank() }
                                ?: msg.mediaRemoteUrl?.takeIf { it.isNotBlank() }

                            if (!resolvedMediaSource.isNullOrBlank() && msg.messageType == MessageType.PHOTO.name) {
                                AsyncImage(
                                    model = resolvedMediaSource,
                                    contentDescription = "Shared family photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(152.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .border(
                                            width = 2.dp,
                                            color = Color.White.copy(alpha = 0.88f),
                                            shape = RoundedCornerShape(18.dp)
                                        )
                                )
                            }

                            if (msg.messageType == MessageType.VOICE_AAC.name) {
                                // Voice Clip Pill styled like the Login Recessed Input Pill + Glossy Blue Medallion!
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelf) {
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.22f),
                                                        Color.White.copy(alpha = 0.14f)
                                                    )
                                                )
                                            } else {
                                                Brush.verticalGradient(
                                                    listOf(
                                                        ChatRecessedWellTop,
                                                        ChatRecessedWellMid,
                                                        ChatRecessedWellBottom
                                                    )
                                                )
                                            }
                                        )
                                        .border(
                                            width = 1.2.dp,
                                            color = if (isSelf) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.85f),
                                            shape = CircleShape
                                        )
                                        .clickable { onPlayVoiceClip(resolvedMediaSource) }
                                        .padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .shadow(6.dp, CircleShape, ambientColor = ChatRoyalBlueDeep, spotColor = ChatRoyalBlueDeep)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelf) {
                                                    Brush.verticalGradient(listOf(Color.White, Color(0xFFE7E0DD)))
                                                } else {
                                                    Brush.radialGradient(
                                                        listOf(ChatRoyalBlueLight, ChatRoyalBlueCore, ChatRoyalBlueDeep),
                                                        center = Offset(10f, 10f),
                                                        radius = 58f
                                                    )
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Play voice note",
                                            tint = if (isSelf) ChatRoyalBrandBlue else Color.White,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg.decryptedPreview,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = if (isSelf) Color.White else ChatInputTextNavy,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = msg.decryptedPreview,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = if (isSelf) Color.White else ChatInputTextNavy
                                )
                            }

                            // Reaction Spheres (matching Login page's raised sculptural white spheres!)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("💖", "😂", "🚀").forEach { emoji ->
                                    val isPicked = msg.reactionEmoji == emoji
                                    Box(
                                        modifier = Modifier
                                            .shadow(
                                                elevation = if (isPicked) 7.dp else 4.dp,
                                                shape = CircleShape,
                                                ambientColor = ChatAmbientBlueShadow.copy(alpha = 0.25f),
                                                spotColor = ChatAmbientBlueShadow.copy(alpha = 0.25f)
                                            )
                                            .clip(CircleShape)
                                            .background(
                                                if (isPicked) {
                                                    Brush.verticalGradient(listOf(Color.White, Color(0xFFE3EEFF)))
                                                } else {
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            Color.White.copy(alpha = if (isSelf) 0.26f else 0.96f),
                                                            Color(0xFFE7E0DD).copy(alpha = if (isSelf) 0.18f else 0.92f)
                                                        )
                                                    )
                                                }
                                            )
                                            .border(
                                                width = if (isPicked) 1.6.dp else 1.dp,
                                                color = if (isPicked) ChatRoyalBlueLight else Color.White.copy(alpha = 0.65f),
                                                shape = CircleShape
                                            )
                                            .clickable { onReactMessage(msg.id, emoji) }
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 16.sp)
                                    }
                                }

                                if (msg.deliveryStatus == "QUEUED_OFFLINE") {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(ChatGoldAccent)
                                            .clickable(onClick = onRetrySync)
                                            .padding(horizontal = 9.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "Retry",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.ExtraBold
                                            ),
                                            color = ChatInputTextNavy
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Raised 3D Matte-Ivory Composer Tray (exact match for Login Page's raised form tray!)
        val trayShape = RoundedCornerShape(30.dp)
        Row(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = trayShape,
                    ambientColor = ChatAmbientBlueShadow.copy(alpha = 0.36f),
                    spotColor = ChatAmbientBlueShadow.copy(alpha = 0.40f)
                )
                .clip(trayShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ChatTrayIvoryTop,
                            ChatTrayIvoryMid,
                            ChatTrayIvoryBottom
                        )
                    )
                )
                .border(
                    width = 1.6.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.96f),
                            Color(0xFFF6EFEF).copy(alpha = 0.54f),
                            Color(0xFFCABBB8).copy(alpha = 0.42f)
                        )
                    ),
                    shape = trayShape
                )
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Photo Attachment Glossy 3D Royal-Blue Medallion
            ChatGlossyBlueMedallion(
                icon = Icons.Rounded.PhotoLibrary,
                contentDescription = "Attach photo",
                size = 42.dp,
                iconSize = 20.dp,
                testTag = "chat_photo_picker_button",
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            // Video Attachment Sculpted White-Ivory Clay Medallion with Royal-Blue Icon
            ChatWhiteClayMedallion(
                icon = Icons.Rounded.Videocam,
                contentDescription = "Attach video snippet",
                size = 42.dp,
                iconSize = 20.dp,
                testTag = "chat_video_picker_button",
                onClick = {
                    videoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )

            // Recessed Neumorphic Pill Input Well (exact match for Login Username/Password wells!)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ChatRecessedWellTop,
                                ChatRecessedWellMid,
                                ChatRecessedWellBottom
                            )
                        )
                    )
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to ChatSoftInnerShadow.copy(alpha = 0.22f),
                                0.26f to ChatSoftInnerShadow.copy(alpha = 0.05f),
                                0.42f to Color.Transparent
                            )
                        )
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.0f to ChatSoftInnerShadow.copy(alpha = 0.16f),
                                0.12f to Color.Transparent
                            )
                        )
                    }
                    .border(
                        width = if (inputFocused) 1.6.dp else 1.2.dp,
                        brush = if (inputFocused) {
                            Brush.linearGradient(
                                listOf(
                                    ChatRoyalBlueLight.copy(alpha = 0.85f),
                                    ChatRoyalBlueCore.copy(alpha = 0.65f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(
                                    ChatSoftInnerShadow.copy(alpha = 0.24f),
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.96f)
                                )
                            )
                        },
                        shape = CircleShape
                    )
                    .padding(horizontal = 15.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (draftText.isEmpty()) {
                    Text(
                        text = "Say hi to family…",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = ChatPlaceholderSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = ChatInputTextNavy,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = MaterialTheme.typography.bodyMedium.fontFamily
                    ),
                    cursorBrush = SolidColor(ChatRoyalBlueCore),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { inputFocused = it.isFocused }
                        .testTag("chat_message_input")
                )
            }

            // Glossy 3D Royal-Blue Circular Send Medallion
            ChatGlossyBlueMedallion(
                icon = Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Send message",
                size = 46.dp,
                iconSize = 21.dp,
                testTag = "chat_send_button",
                onClick = {
                    if (draftText.isNotBlank()) {
                        onSendText(draftText.trim())
                        draftText = ""
                    }
                }
            )
        }
    }
}

/**
 * Thread selector pill matching Login page's Glossy 3D Royal-Blue pill (selected)
 * and Raised Sculpted Matte-Ivory Clay pill (unselected).
 */
@Composable
private fun ChatLoginStyleThreadPill(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 460f),
        label = "thread_pill_scale"
    )

    Box(
        modifier = Modifier
            .height(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (selected) 12.dp else 8.dp,
                shape = CircleShape,
                ambientColor = if (selected) ChatRoyalBlueDeep else ChatAmbientBlueShadow.copy(alpha = 0.28f),
                spotColor = if (selected) ChatRoyalBlueDeep else ChatAmbientBlueShadow.copy(alpha = 0.32f)
            )
            .clip(CircleShape)
            .background(
                if (selected) {
                    Brush.verticalGradient(
                        listOf(ChatRoyalBlueLight, ChatRoyalBlueCore, ChatRoyalBlueDeep)
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(Color.White, ChatTrayIvoryMid, ChatTrayIvoryBottom)
                    )
                }
            )
            .border(
                width = 1.5.dp,
                brush = if (selected) {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.16f),
                            Color(0xFF0A2C82).copy(alpha = 0.65f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.96f),
                            Color(0xFFD6CBC8).copy(alpha = 0.62f)
                        )
                    )
                },
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 15.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 2.dp, start = 8.dp, end = 8.dp)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
                        )
                    )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.1.sp
            ),
            color = if (selected) Color.White else ChatSubtitleNavy,
            maxLines = 1
        )
    }
}

/**
 * Glossy 3D Royal-Blue Circular Icon Medallion (exact copy of Login input icon spheres!).
 */
@Composable
private fun ChatGlossyBlueMedallion(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    iconSize: Dp,
    testTag: String? = null,
    onClick: (() -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 460f),
        label = "blue_medallion_scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed) 5.dp else 10.dp,
                shape = CircleShape,
                ambientColor = ChatRoyalBlueDeep,
                spotColor = ChatRoyalBlueDeep
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        ChatRoyalBlueLight,
                        ChatRoyalBlueCore,
                        ChatRoyalBlueDeep
                    ),
                    center = Offset(16f, 14f),
                    radius = 85f
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.72f),
                        Color.White.copy(alpha = 0.14f),
                        Color(0xFF0A2E8A).copy(alpha = 0.55f)
                    )
                ),
                shape = CircleShape
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Raised White-Ivory 3D Claymorphic Circular Medallion (matches Login page's white bottom spheres!).
 */
@Composable
private fun ChatWhiteClayMedallion(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    iconSize: Dp,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 460f),
        label = "white_medallion_scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed) 4.dp else 9.dp,
                shape = CircleShape,
                ambientColor = ChatAmbientBlueShadow.copy(alpha = 0.28f),
                spotColor = ChatAmbientBlueShadow.copy(alpha = 0.32f)
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White, Color(0xFFF6F2F0), Color(0xFFE7E0DD))
                )
            )
            .border(
                width = 1.4.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White, Color(0xFFD6CBC8).copy(alpha = 0.65f))
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ChatRoyalBrandBlue,
            modifier = Modifier.size(iconSize)
        )
    }
}
