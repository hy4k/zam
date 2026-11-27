package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.EncryptedMessageEntity
import com.example.data.local.UserProfileEntity
import com.example.data.model.BadgeElement
import com.example.data.model.FamilyMemberId
import com.example.data.model.MessageType
import com.example.data.model.UserSessionState
import com.example.data.model.haloColor
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.BubblePink
import com.example.ui.theme.MintTreehouseDeep
import com.example.ui.theme.claymorphic
import com.example.ui.theme.liquidGlass

private data class ChatThreadOption(val id: String, val label: String)

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
        ChatThreadOption("family_group", "3-Way Family"),
        ChatThreadOption("mithun_mafitha", "Mithun & Mafitha"),
        ChatThreadOption("mithun_zayn", "Vapi & Zayn"),
        ChatThreadOption("mafitha_zayn", "Ummi & Zayn")
    )
    val threadMessages = allMessages
        .filter { it.threadId == session.selectedThreadId }
        .sortedBy { it.timestampMs }

    var draftText by remember { mutableStateOf("") }

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
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Thread selector + E2EE indicator
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Private family chat",
                    tint = MintTreehouseDeep,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Private Family Chat • Just Mithun, Mafitha & Zayn",
                    style = MaterialTheme.typography.labelMedium,
                    color = MintTreehouseDeep
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                threads.forEach { thread ->
                    FilterChip(
                        selected = session.selectedThreadId == thread.id,
                        onClick = { onSelectThread(thread.id) },
                        label = { Text(thread.label) }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(threadMessages, key = { it.id }) { msg ->
                val sender = runCatching { FamilyMemberId.valueOf(msg.senderId) }
                    .getOrDefault(FamilyMemberId.MITHUN)
                val senderProf = profiles.firstOrNull { it.memberId == sender.name }
                val senderBadge = runCatching {
                    BadgeElement.valueOf(senderProf?.badgeElement ?: BadgeElement.STARLIGHT.name)
                }.getOrDefault(BadgeElement.STARLIGHT)
                val isSelf = sender == session.activeMember
                val halo = sender.haloColor()

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

                    Column(
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .claymorphic(
                                cornerRadius = 24.dp,
                                surfaceColor = if (isSelf) {
                                    halo.copy(alpha = 0.24f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                elevation = 8.dp
                            )
                            .border(2.dp, halo.copy(alpha = 0.65f), RoundedCornerShape(24.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${sender.displayName} ${senderBadge.emoji}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (msg.deliveryStatus == "QUEUED_OFFLINE") {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = "Queued offline",
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable(onClick = onRetrySync)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.DoneAll,
                                    contentDescription = msg.deliveryStatus,
                                    tint = MintTreehouseDeep,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (!msg.mediaLocalPath.isNullOrBlank() && msg.messageType == MessageType.PHOTO.name) {
                            AsyncImage(
                                model = msg.mediaLocalPath,
                                contentDescription = "Shared family photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }

                        if (msg.messageType == MessageType.VOICE_AAC.name) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(halo.copy(alpha = 0.22f))
                                    .clickable { onPlayVoiceClip(msg.mediaLocalPath) }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play voice note",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = msg.decryptedPreview,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Text(
                                text = msg.decryptedPreview,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Quick E2EE reaction strip
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("💖", "😂", "🚀").forEach { emoji ->
                                Text(
                                    text = emoji,
                                    fontSize = 18.sp,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (msg.reactionEmoji == emoji) halo.copy(alpha = 0.3f)
                                            else Color.Transparent
                                        )
                                        .clickable { onReactMessage(msg.id, emoji) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (msg.deliveryStatus == "QUEUED_OFFLINE") {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFEF3C7),
                                    modifier = Modifier.clickable(onClick = onRetrySync)
                                ) {
                                    Text(
                                        text = "Sending… Tap to retry",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Liquid Glass Message Composer
        Row(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .liquidGlass(
                    shape = RoundedCornerShape(28.dp),
                    surfaceAlpha = 0.85f,
                    tintColor = MaterialTheme.colorScheme.surface
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_photo_picker_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoLibrary,
                    contentDescription = "Attach photo",
                    tint = BubblePink
                )
            }
            IconButton(
                onClick = {
                    videoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("chat_video_picker_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = "Attach video snippet",
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
            OutlinedTextField(
                value = draftText,
                onValueChange = { draftText = it },
                placeholder = { Text("Say hi to the family…") },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = {
                    if (draftText.isNotBlank()) {
                        onSendText(draftText.trim())
                        draftText = ""
                    }
                },
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(BubblePink)
                    .testTag("chat_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send message",
                    tint = Color.White
                )
            }
        }
    }
}
