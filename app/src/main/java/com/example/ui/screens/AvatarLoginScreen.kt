package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.UserProfileEntity
import com.example.data.model.FamilyMemberId
import com.example.ui.LoginUiState
import com.example.ui.components.FamilyMemberAvatar
import com.example.ui.theme.AtelierDeskDarkBottom
import com.example.ui.theme.AtelierWoodFrameDeep
import com.example.ui.theme.ClayAqua
import com.example.ui.theme.ClayCoral
import com.example.ui.theme.ClayDeepTealInk
import com.example.ui.theme.ClayHoney
import com.example.ui.theme.ClayHoneyLight
import com.example.ui.theme.ClayMutedInk
import com.example.ui.theme.ClayPeachDeep
import com.example.ui.theme.ClayTextCream
import com.example.ui.theme.ClayVanilla
import com.example.ui.theme.ClayVanillaLight
import com.example.ui.theme.PlayfairDisplayFontFamily

private val LoginCreamTop = Color(0xFFF8F1E7)
private val LoginCreamBottom = Color(0xFFECE0CE)
private val LoginSandTop = Color(0xFFEBE0D0)
private val LoginSandBottom = Color(0xFFE0D1BC)
private val LoginWalnutTop = Color(0xFF6E4B30)
private val LoginWalnutMid = Color(0xFF4F3420)
private val LoginWalnutDeep = Color(0xFF362113)

private data class FamilyLoginAccount(
    val member: FamilyMemberId,
    val username: String,
    val displayLabel: String,
    val accentColor: Color
)

/**
 * Tap-Your-Name + 4-Digit PIN Popup Login Screen using the user's attached doorway artwork
 * (`img_adam_door_login`). No username typing — family members simply tap their name and enter
 * their 4-digit PIN in the warm cream & walnut studio popup.
 */
@Composable
fun AvatarLoginScreen(
    profiles: List<UserProfileEntity>,
    loginUiState: LoginUiState,
    onSubmitLogin: (username: String, password: String) -> Unit,
    onClearError: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedAccountName by rememberSaveable { mutableStateOf<String?>(null) }

    val accounts = remember {
        listOf(
            FamilyLoginAccount(
                member = FamilyMemberId.MITHUN,
                username = "mithun",
                displayLabel = "Mithun",
                accentColor = ClayAqua
            ),
            FamilyLoginAccount(
                member = FamilyMemberId.MAFITHA,
                username = "mafitha",
                displayLabel = "Mafitha",
                accentColor = ClayCoral
            ),
            FamilyLoginAccount(
                member = FamilyMemberId.ZAYN,
                username = "Zzayn",
                displayLabel = "Zayn Adam",
                accentColor = ClayHoney
            )
        )
    }

    val selectedAccount = accounts.firstOrNull { it.member.name == selectedAccountName }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AtelierDeskDarkBottom)
    ) {
        // 1. Full-screen attached Adam Welcome Doorway artwork
        Image(
            painter = painterResource(id = R.drawable.img_adam_door_login),
            contentDescription = "Adam welcoming family at the wooden doorway",
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle warm walnut vignette at the very bottom desk edge so the name buttons stay crisp
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.38f to Color(0x9924160D),
                            1.0f to Color(0xEB1A0F08)
                        )
                    )
                )
        )

        // 2. Clean Bottom Family Name Selector Dock (NO username typing!)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color.Black,
                        spotColor = Color.Black
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                LoginCreamTop.copy(alpha = 0.94f),
                                LoginCreamBottom.copy(alpha = 0.96f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        color = ClayVanillaLight.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Tap your name to enter",
                    style = TextStyle(
                        fontFamily = PlayfairDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    ),
                    color = ClayDeepTealInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("login_brand_title")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accounts.forEach { account ->
                        FamilyNameButton(
                            label = account.displayLabel,
                            accentColor = account.accentColor,
                            testTag = "quick_pick_${account.member.name.lowercase()}",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClearError()
                                selectedAccountName = account.member.name
                            }
                        )
                    }
                }
            }
        }
    }

    // 3. Strictly 4-Digit PIN Popup Dialog when any name is tapped
    if (selectedAccount != null) {
        val profile = profiles.firstOrNull { it.memberId == selectedAccount.member.name }
        FourDigitPinLoginDialog(
            account = selectedAccount,
            customPhotoUri = profile?.customPhotoUri,
            isLoading = loginUiState.isLoading,
            errorMessage = loginUiState.errorMessage,
            onDismiss = {
                if (!loginUiState.isLoading) {
                    onClearError()
                    selectedAccountName = null
                }
            },
            onClearError = onClearError,
            onCompletePin = { fourDigitPin ->
                onSubmitLogin(selectedAccount.username, fourDigitPin)
            }
        )
    }
}

@Composable
private fun FamilyNameButton(
    label: String,
    accentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "name_btn_scale"
    )

    Box(
        modifier = modifier
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (pressed) 2.dp else 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = AtelierWoodFrameDeep.copy(alpha = 0.35f),
                spotColor = AtelierWoodFrameDeep.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(LoginWalnutTop, LoginWalnutMid, LoginWalnutDeep)
                )
            )
            .border(
                width = 1.dp,
                color = ClayHoneyLight.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                ),
                color = ClayTextCream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FourDigitPinLoginDialog(
    account: FamilyLoginAccount,
    customPhotoUri: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    onCompletePin: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var pin by remember(account.member) { mutableStateOf("") }

    // Clear the entered 4 digits automatically when login fails so the user can retry instantly
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            pin = ""
        }
    }

    fun appendDigit(digit: String) {
        if (isLoading || pin.length >= 4) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClearError()
        val next = pin + digit
        pin = next
        if (next.length == 4) {
            onCompletePin(next)
        }
    }

    fun deleteDigit() {
        if (isLoading || pin.isEmpty()) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClearError()
        pin = pin.dropLast(1)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = !isLoading,
            dismissOnClickOutside = !isLoading,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 22.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 28.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(LoginCreamTop, LoginCreamBottom)
                    )
                )
                .border(
                    width = 1.4.dp,
                    color = ClayVanillaLight,
                    shape = RoundedCornerShape(32.dp)
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .testTag("login_pin_dialog")
        ) {
            // Close button in top-right
            IconButton(
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .testTag("login_pin_close_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close PIN dialog",
                    tint = ClayMutedInk,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FamilyMemberAvatar(
                    member = account.member,
                    customPhotoUri = customPhotoUri,
                    size = 64.dp,
                    showBadge = false
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = account.displayLabel,
                        style = TextStyle(
                            fontFamily = PlayfairDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = ClayDeepTealInk
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = ClayMutedInk,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Enter 4-digit PIN",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = ClayMutedInk
                        )
                    }
                }

                // 4 Sculpted Sand PIN Boxes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .testTag("login_password_input")
                ) {
                    for (index in 0 until 4) {
                        val isFilled = index < pin.length
                        val isActive = index == pin.length && !isLoading
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isFilled) {
                                        Brush.verticalGradient(listOf(LoginWalnutTop, LoginWalnutMid))
                                    } else {
                                        Brush.verticalGradient(listOf(LoginSandBottom, LoginSandTop))
                                    }
                                )
                                .border(
                                    width = if (isActive) 2.dp else 1.dp,
                                    color = when {
                                        isFilled -> LoginWalnutDeep
                                        isActive -> account.accentColor
                                        else -> ClayPeachDeep
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFilled) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(ClayTextCream)
                                )
                            }
                        }
                    }
                }

                // Loading Indicator or Inline Error Message
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = LoginWalnutMid,
                            strokeWidth = 2.4.dp
                        )
                        Text(
                            text = "Opening door…",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = ClayDeepTealInk
                        )
                    }
                } else {
                    AnimatedVisibility(
                        visible = !errorMessage.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF8E1DA))
                                .border(
                                    width = 1.dp,
                                    color = ClayCoral.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = "Login error",
                                tint = ClayCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFF7A2F1D),
                                modifier = Modifier.testTag("login_error_text")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Tactile 0–9 Numeric Keypad (easy for 6-year-old Zayn & parents!)
                val keypadRows = remember {
                    listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("CLR", "0", "DEL")
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    keypadRows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { key ->
                                when (key) {
                                    "CLR" -> PinActionKey(
                                        label = "Clear",
                                        enabled = !isLoading && pin.isNotEmpty(),
                                        testTag = "pin_key_clear",
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            pin = ""
                                            onClearError()
                                        }
                                    )
                                    "DEL" -> PinBackspaceKey(
                                        enabled = !isLoading && pin.isNotEmpty(),
                                        testTag = "pin_key_backspace",
                                        modifier = Modifier.weight(1f),
                                        onClick = { deleteDigit() }
                                    )
                                    else -> PinDigitKey(
                                        digit = key,
                                        enabled = !isLoading && pin.length < 4,
                                        testTag = "pin_key_$key",
                                        modifier = Modifier.weight(1f),
                                        onClick = { appendDigit(key) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDigitKey(
    digit: String,
    enabled: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 550f),
        label = "pin_digit_scale_$digit"
    )

    Box(
        modifier = modifier
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (pressed) 1.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = AtelierWoodFrameDeep.copy(alpha = 0.22f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(ClayVanillaLight, LoginSandTop)
                )
            )
            .border(
                width = 1.dp,
                color = ClayPeachDeep.copy(alpha = 0.75f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = ClayDeepTealInk
        )
    }
}

@Composable
private fun PinActionKey(
    label: String,
    enabled: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ClayVanilla.copy(alpha = if (enabled) 0.85f else 0.45f))
            .border(
                width = 1.dp,
                color = ClayPeachDeep.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = if (enabled) ClayDeepTealInk else ClayMutedInk.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun PinBackspaceKey(
    enabled: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ClayVanilla.copy(alpha = if (enabled) 0.85f else 0.45f))
            .border(
                width = 1.dp,
                color = ClayPeachDeep.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.Backspace,
            contentDescription = "Delete digit",
            tint = if (enabled) ClayDeepTealInk else ClayMutedInk.copy(alpha = 0.5f),
            modifier = Modifier.size(22.dp)
        )
    }
}
