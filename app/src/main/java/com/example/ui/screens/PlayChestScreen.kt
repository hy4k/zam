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
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.FamilyActivityEntity
import com.example.ui.components.ClayActionButton
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.theme.BubblePink
import com.example.ui.theme.ConstellationGold
import com.example.ui.theme.ElectricRocket
import com.example.ui.theme.EmeraldAccept
import com.example.ui.theme.LocalKinNestAccessibility
import com.example.ui.theme.MintTreehouseDeep
import com.example.ui.theme.PurpleChest
import com.example.ui.theme.StudioTeal
import com.example.ui.theme.WalkieAmber
import com.example.ui.theme.clayPressScale
import com.example.ui.theme.claymorphic

@Composable
fun PlayChestScreen(
    activities: List<FamilyActivityEntity>,
    onOpenLiveDrawing: () -> Unit,
    onOpenStoryTogether: () -> Unit,
    onOpenAdventureMission: () -> Unit,
    onToggleActivityDone: (String, Boolean) -> Unit,
    onStartPartyGameCall: () -> Unit
) {
    val a11y = LocalKinNestAccessibility.current
    val missions = activities.filter { it.kind == "MISSION" }
    val studyBoards = activities.filter { it.kind == "STUDY" }
    var partyTapScore by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Big 3 Play Chest Hero Cards
        item {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EditorialSectionHeader(
                    eyebrow = "Playroom & Co-Creation",
                    title = "Zayn's Treasure Chest",
                    accentColor = PurpleChest
                )

                PlayPortalCard(
                    title = "Adventure Missions",
                    subtitle = "Indoor AR Camera clue hunts & 5–7 min family quests",
                    color = PurpleChest,
                    icon = Icons.Rounded.CameraAlt,
                    testTag = "open_adventure_mission",
                    reducedMotion = a11y.reducedMotion,
                    onClick = onOpenAdventureMission
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayPortalCard(
                        title = "Live Drawing",
                        subtitle = "Multiplayer sketchpad",
                        color = BubblePink,
                        icon = Icons.Rounded.Brush,
                        testTag = "open_live_drawing",
                        reducedMotion = a11y.reducedMotion,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenLiveDrawing
                    )
                    PlayPortalCard(
                        title = "Story Together",
                        subtitle = "Shared narration",
                        color = MintTreehouseDeep,
                        icon = Icons.Rounded.AutoStories,
                        testTag = "open_story_together",
                        reducedMotion = a11y.reducedMotion,
                        modifier = Modifier.weight(1f),
                        onClick = onOpenStoryTogether
                    )
                }
            }
        }

        // Spontaneous 3-Way Gaming Mode Mini-Challenge
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .claymorphic(
                        cornerRadius = 28.dp,
                        surfaceColor = ElectricRocket,
                        elevation = 12.dp
                    )
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SportsEsports,
                            contentDescription = "3-Way Gaming Mode",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "3-Way Rocket Tap Party",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Tap the rocket booster fast! Score: $partyTapScore 🚀",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ClayActionButton(
                            text = "Boost! ($partyTapScore)",
                            containerColor = WalkieAmber,
                            testTag = "party_game_tap_button",
                            modifier = Modifier.weight(1f),
                            onClick = { partyTapScore++ }
                        )
                        ClayActionButton(
                            text = "Play on Video",
                            containerColor = EmeraldAccept,
                            testTag = "party_game_video_button",
                            modifier = Modifier.weight(1f),
                            onClick = onStartPartyGameCall
                        )
                    }
                }
            }
        }

        // Adventure Missions Checklist
        item {
            EditorialSectionHeader(
                eyebrow = "Mini-Quests",
                title = "Family Challenges (5–7 min)",
                accentColor = StudioTeal,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            )
        }

        items(missions, key = { it.id }) { mission ->
            ActivityChecklistCard(
                item = mission,
                accentColor = PurpleChest,
                icon = Icons.Rounded.Explore,
                onToggle = { onToggleActivityDone(mission.id, !mission.isCompleted) }
            )
        }

        // Study Sessions Board
        item {
            EditorialSectionHeader(
                eyebrow = "Learning Studio",
                title = "Study Sessions & Reading",
                accentColor = WalkieAmber,
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
            )
        }

        items(studyBoards, key = { it.id }) { study ->
            ActivityChecklistCard(
                item = study,
                accentColor = ConstellationGold,
                icon = Icons.Rounded.School,
                onToggle = { onToggleActivityDone(study.id, !study.isCompleted) }
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PlayPortalCard(
    title: String,
    subtitle: String,
    color: Color,
    icon: ImageVector,
    testTag: String,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clayPressScale(reducedMotion = reducedMotion)
            .claymorphic(cornerRadius = 28.dp, surfaceColor = color, elevation = 12.dp)
            .clickable(onClick = onClick)
            .padding(18.dp)
            .testTag(testTag)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.24f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun ActivityChecklistCard(
    item: FamilyActivityEntity,
    accentColor: Color,
    icon: ImageVector,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .claymorphic(
                cornerRadius = 24.dp,
                surfaceColor = MaterialTheme.colorScheme.surface,
                elevation = 8.dp
            )
            .clickable(onClick = onToggle)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (item.isCompleted) Icons.Rounded.CheckCircle else icon,
                    contentDescription = item.title,
                    tint = if (item.isCompleted) EmeraldAccept else accentColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item.costEstimate?.let { tag ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
