package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.FamilyActivityEntity
import com.example.data.local.MemoryCapsuleEntity
import com.example.data.model.FamilyMemberId
import com.example.ui.components.ClayActionButton
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.theme.BubblePink
import com.example.ui.theme.ConstellationGold
import com.example.ui.theme.ElectricRocket
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.MintTreehouseDeep
import com.example.ui.theme.OchreGold
import com.example.ui.theme.PurpleChest
import com.example.ui.theme.claymorphic
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UsConstellationScreen(
    capsules: List<MemoryCapsuleEntity>,
    activities: List<FamilyActivityEntity>,
    onAddCapsule: (title: String, caption: String, unlockDays: Int?) -> Unit,
    onToggleVaultPin: (String, Boolean) -> Unit,
    onVoteTravelPin: (String) -> Unit,
    onAddTravelPin: (title: String, subtitle: String, cost: String, countdown: String) -> Unit,
    onOpenParentalSettings: () -> Unit
) {
    val travelPins = activities.filter { it.kind == "TRAVEL_PIN" }
    val calendarItems = activities.filter { it.kind == "CALENDAR" }
    val rewards = activities.filter { it.kind == "REWARD" }

    var showNewCapsuleDialog by remember { mutableStateOf(false) }
    var showNewTravelDialog by remember { mutableStateOf(false) }
    val nowMs = System.currentTimeMillis()
    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Parent Corner Action
        item {
            ClayActionButton(
                text = "Vapi & Ummi's Corner",
                containerColor = PurpleChest,
                icon = Icons.Rounded.AdminPanelSettings,
                testTag = "open_parental_settings_button",
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                onClick = onOpenParentalSettings
            )
        }

        // Family Memory Capsules
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorialSectionHeader(
                    eyebrow = "Family Keepsakes",
                    title = "Memory Capsules",
                    accentColor = OchreGold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showNewCapsuleDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ConstellationGold)
                        .testTag("add_capsule_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "New Memory Capsule",
                        tint = Color.White
                    )
                }
            }
        }

        items(capsules, key = { it.id }) { capsule ->
            val isTimeLocked = capsule.unlockAtMs != null && capsule.unlockAtMs > nowMs
            val author = runCatching { FamilyMemberId.valueOf(capsule.authorId) }
                .getOrDefault(FamilyMemberId.MAFITHA)

            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 26.dp,
                        surfaceColor = MaterialTheme.colorScheme.surface,
                        elevation = 10.dp
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isTimeLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                            contentDescription = null,
                            tint = if (isTimeLocked) ConstellationGold else EmeraldAccept
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = capsule.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                onToggleVaultPin(capsule.id, !capsule.isVaultOfflinePinned)
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDone,
                                contentDescription = "Save memory on this phone",
                                tint = if (capsule.isVaultOfflinePinned) MintTreehouseDeep else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    Text(
                        text = if (isTimeLocked) {
                            "Time-locked until ${dateFmt.format(Date(capsule.unlockAtMs!!))} • Special surprise inside!"
                        } else {
                            capsule.caption
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Saved by ${author.displayName} • ${if (capsule.isVaultOfflinePinned) "Saved on this phone" else "Online"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Collaborative Travel Planner & Wishlist
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorialSectionHeader(
                    eyebrow = "Shared Adventures",
                    title = "Travel Wishlist & Countdown",
                    accentColor = ElectricRocket,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showNewTravelDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ElectricRocket)
                        .testTag("add_travel_pin_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Travel Idea",
                        tint = Color.White
                    )
                }
            }
        }

        items(travelPins, key = { it.id }) { pin ->
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 24.dp,
                        surfaceColor = MaterialTheme.colorScheme.surface,
                        elevation = 8.dp
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricRocket.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlightTakeoff,
                            contentDescription = null,
                            tint = ElectricRocket
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = pin.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = pin.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pin.costEstimate?.let {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MintTreehouseDeep.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            pin.targetDateLabel?.let {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = ConstellationGold.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onVoteTravelPin(pin.id) }
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ThumbUp,
                            contentDescription = "Vote for trip",
                            tint = BubblePink
                        )
                        Text(text = "${pin.votesCount}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // Family Calendar & Rewards
        item {
            EditorialSectionHeader(
                eyebrow = "Family Milestones",
                title = "Family Calendar & Rewards",
                accentColor = BubblePink,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            )
        }

        items(calendarItems + rewards, key = { it.id }) { item ->
            val isReward = item.kind == "REWARD"
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 22.dp,
                        surfaceColor = MaterialTheme.colorScheme.surface,
                        elevation = 6.dp
                    )
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isReward) Icons.Rounded.EmojiEvents else Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = if (isReward) ConstellationGold else BubblePink,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    item.targetDateLabel?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showNewCapsuleDialog) {
        var title by remember { mutableStateOf("") }
        var caption by remember { mutableStateOf("") }
        var lockDays by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewCapsuleDialog = false },
            title = { Text("New Memory Capsule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Moment Title") }
                    )
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        label = { Text("Quote or Memory") }
                    )
                    OutlinedTextField(
                        value = lockDays,
                        onValueChange = { lockDays = it.filter(Char::isDigit) },
                        label = { Text("Future Unlock (days, optional)") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) {
                        onAddCapsule(title.trim(), caption.trim(), lockDays.toIntOrNull())
                        showNewCapsuleDialog = false
                    }
                }) { Text("Save Capsule") }
            },
            dismissButton = {
                TextButton(onClick = { showNewCapsuleDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showNewTravelDialog) {
        var dest by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        var cost by remember { mutableStateOf("") }
        var whenLabel by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewTravelDialog = false },
            title = { Text("Pin Family Trip Idea") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dest,
                        onValueChange = { dest = it },
                        label = { Text("Resort or Trip Name") }
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Pool / Activity Highlight") }
                    )
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = { Text("Estimated Budget (e.g. ₹12,000)") }
                    )
                    OutlinedTextField(
                        value = whenLabel,
                        onValueChange = { whenLabel = it },
                        label = { Text("Countdown (e.g. In 25 days)") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dest.isNotBlank()) {
                        onAddTravelPin(
                            dest.trim(),
                            note.ifBlank { "Family adventure wishlist" },
                            cost.ifBlank { "TBD" },
                            whenLabel.ifBlank { "Soon" }
                        )
                        showNewTravelDialog = false
                    }
                }) { Text("Pin Trip") }
            },
            dismissButton = {
                TextButton(onClick = { showNewTravelDialog = false }) { Text("Cancel") }
            }
        )
    }
}
