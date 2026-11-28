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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.UserProfileEntity
import com.example.data.model.FamilyMemberId
import com.example.ui.LoginUiState

// Exact sampled palette from the 3D claymorphic reference scene
private val CardIvoryTop = Color(0xFFF6EFEF)
private val CardIvoryMid = Color(0xFFEDE3D9)
private val CardIvoryBottom = Color(0xFFEAE0DD)
private val TrayIvoryTop = Color(0xFFF7F2F0)
private val TrayIvoryMid = Color(0xFFF0E9E7)
private val TrayIvoryBottom = Color(0xFFE7DFDC)
private val RecessedWellTop = Color(0xFFE2D9D6)
private val RecessedWellMid = Color(0xFFEDE6E3)
private val RecessedWellBottom = Color(0xFFF5F0EE)
private val RoyalBrandBlue = Color(0xFF1553D6)
private val RoyalBlueLight = Color(0xFF428BFF)
private val RoyalBlueCore = Color(0xFF1D62EC)
private val RoyalBlueDeep = Color(0xFF1043BE)
private val SubtitleNavy = Color(0xFF365592)
private val InputTextNavy = Color(0xFF1A3263)
private val InputPlaceholderSlate = Color(0xFF6B7792)
private val EyeIconNavy = Color(0xFF1A3A70)
private val DividerLineTint = Color(0xFFBDC2D0)
private val ClayAmbientBlueShadow = Color(0xFF0F2D7A)
private val ClaySoftInnerShadow = Color(0xFF5A4E60)

private data class QuickMemberOption(
    val member: FamilyMemberId,
    val username: String,
    val badgeInitial: String,
    val accentStart: Color,
    val accentEnd: Color
)

/**
 * Tactile 3D Claymorphic Login Screen for Holy Moly !
 * Mirrors the cozy cobalt studio backdrop, sculpted matte-ivory clay card, 3D crown,
 * boy + puppy artwork, recessed neumorphic pill inputs with 3D cobalt icon badges,
 * glossy royal-blue Login pill button, and quick-pick member medallions.
 */
@Composable
fun AvatarLoginScreen(
    profiles: List<UserProfileEntity>,
    loginUiState: LoginUiState,
    onSubmitLogin: (username: String, password: String) -> Unit,
    onClearError: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val passwordFocusRequester = remember { FocusRequester() }

    val quickMembers = remember {
        listOf(
            QuickMemberOption(
                member = FamilyMemberId.MITHUN,
                username = "mithun",
                badgeInitial = "M",
                accentStart = Color(0xFF3E87FF),
                accentEnd = Color(0xFF1349C6)
            ),
            QuickMemberOption(
                member = FamilyMemberId.MAFITHA,
                username = "mafitha",
                badgeInitial = "F",
                accentStart = Color(0xFFFB7185),
                accentEnd = Color(0xFFD93860)
            ),
            QuickMemberOption(
                member = FamilyMemberId.ZAYN,
                username = "Zzayn",
                badgeInitial = "Z",
                accentStart = Color(0xFFFBBF24),
                accentEnd = Color(0xFFE07A16)
            )
        )
    }

    val outerCardShape = RoundedCornerShape(34.dp)
    val trayShape = RoundedCornerShape(30.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Full-Bleed 3D Cozy Cobalt Studio Room Backdrop (from user's attached scene)
        Image(
            painter = painterResource(id = R.drawable.bg_login_room),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Subtle vertical depth vignette so status/navigation chrome blends effortlessly
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0x2A0B2466),
                            0.18f to Color.Transparent,
                            0.84f to Color.Transparent,
                            1.0f to Color(0x360A215C)
                        )
                    )
                )
        )

        // 2. Centered Scrollable Sculpted 3D Claymorphic Card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .widthIn(max = 368.dp)
                    .fillMaxWidth()
                    .shadow(
                        elevation = 26.dp,
                        shape = outerCardShape,
                        ambientColor = ClayAmbientBlueShadow,
                        spotColor = ClayAmbientBlueShadow
                    )
                    .clip(outerCardShape)
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to CardIvoryTop,
                                0.24f to Color(0xFFF2E8E6),
                                0.50f to CardIvoryMid,
                                1.0f to CardIvoryBottom
                            )
                        )
                    )
                    .border(
                        width = 1.8.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.94f),
                                Color(0xFFFDF8F6).copy(alpha = 0.62f),
                                Color(0xFFC9B7B3).copy(alpha = 0.45f)
                            )
                        ),
                        shape = outerCardShape
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(14.dp))

                    // 3D Gold Crown + Blue/Gold Sparkles
                    Image(
                        painter = painterResource(id = R.drawable.img_login_crown),
                        contentDescription = "Holy Moly Crown",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .width(106.dp)
                            .height(41.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Brand Title ("Holy Moly !") in exact sculpted royal cobalt style
                    Text(
                        text = "Holy Moly !",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 31.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            shadow = Shadow(
                                color = Color(0x2E103B9E),
                                offset = Offset(0f, 3f),
                                blurRadius = 6f
                            )
                        ),
                        color = RoyalBrandBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .testTag("login_brand_title")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Login to continue your journey",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.1.sp
                        ),
                        color = SubtitleNavy,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Enhanced Comic-Illustrated Photo of Boy + Towel Buddy resting on the tray
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(960f / 685f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_login_zayn_towel),
                            contentDescription = "Enhanced comic illustration of boy smiling at his folded towel buddy",
                            contentScale = ContentScale.Crop,
                            alignment = Alignment.TopCenter,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Soft top & side edge feathering so artwork melts seamlessly into the clay card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            CardIvoryMid,
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight()
                                .align(Alignment.CenterStart)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            CardIvoryMid.copy(alpha = 0.85f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight()
                                .align(Alignment.CenterEnd)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            CardIvoryMid.copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                    }

                    // 3. Raised 3D Claymorphic Form Tray overlapping right below the arms & paws
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-10).dp)
                            .shadow(
                                elevation = 18.dp,
                                shape = trayShape,
                                ambientColor = ClayAmbientBlueShadow.copy(alpha = 0.34f),
                                spotColor = ClayAmbientBlueShadow.copy(alpha = 0.38f)
                            )
                            .clip(trayShape)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        TrayIvoryTop,
                                        TrayIvoryMid,
                                        TrayIvoryBottom
                                    )
                                )
                            )
                            .border(
                                width = 1.6.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.96f),
                                        Color(0xFFF6EFEF).copy(alpha = 0.52f),
                                        Color(0xFFCABBB8).copy(alpha = 0.42f)
                                    )
                                ),
                                shape = trayShape
                            )
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Username Recessed Neumorphic Pill
                            NeumorphicInputPill(
                                value = username,
                                onValueChange = {
                                    username = it
                                    onClearError()
                                },
                                placeholder = "Username",
                                leadingIcon = Icons.Rounded.Person,
                                leadingIconDescription = "Username icon",
                                isPassword = false,
                                passwordVisible = true,
                                onTogglePasswordVisibility = null,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    autoCorrectEnabled = false,
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                enabled = !loginUiState.isLoading,
                                fieldTestTag = "login_username_input"
                            )

                            // Password Recessed Neumorphic Pill
                            NeumorphicInputPill(
                                value = password,
                                onValueChange = {
                                    password = it
                                    onClearError()
                                },
                                placeholder = "Password",
                                leadingIcon = Icons.Rounded.Lock,
                                leadingIconDescription = "Password icon",
                                isPassword = true,
                                passwordVisible = passwordVisible,
                                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.None,
                                    autoCorrectEnabled = false,
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        focusManager.clearFocus()
                                        onSubmitLogin(username, password)
                                    }
                                ),
                                enabled = !loginUiState.isLoading,
                                fieldTestTag = "login_password_input",
                                modifier = Modifier.focusRequester(passwordFocusRequester)
                            )

                            // Inline Validation / Supabase Error Pill
                            AnimatedVisibility(
                                visible = !loginUiState.errorMessage.isNullOrBlank(),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFFE4E6))
                                        .border(
                                            width = 1.dp,
                                            color = Color(0xFFFDA4AF),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = "Login error",
                                        tint = Color(0xFFBE123C),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = loginUiState.errorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        ),
                                        color = Color(0xFF9F1239),
                                        modifier = Modifier.testTag("login_error_text")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            // Glossy 3D Royal-Blue Pill "Login ->" Button
                            ClayRoyalLoginButton(
                                isLoading = loginUiState.isLoading,
                                onClick = {
                                    focusManager.clearFocus()
                                    onSubmitLogin(username, password)
                                }
                            )

                            // Soft Divider Matching Reference ("or select member")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(DividerLineTint.copy(alpha = 0.72f))
                                )
                                Text(
                                    text = "or quick fill user",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = SubtitleNavy.copy(alpha = 0.86f)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(DividerLineTint.copy(alpha = 0.72f))
                                )
                            }

                            // 3 Raised 3D Claymorphic Circular Member Medallions (pre-fill username)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                quickMembers.forEach { option ->
                                    val isSelected = username.trim().equals(option.username, ignoreCase = true)
                                    val profile = profiles.firstOrNull { it.memberId == option.member.name }
                                    QuickMemberClayCircle(
                                        option = option,
                                        displayLabel = profile?.roleLabel ?: option.member.title,
                                        isSelected = isSelected,
                                        onClick = {
                                            username = option.username
                                            onClearError()
                                            runCatching { passwordFocusRequester.requestFocus() }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Recessed Neumorphic Input Pill with inner top-left depth groove, white bottom-right catchlight,
 * and leading glossy 3D royal-blue circular icon badge matching the attached reference image.
 */
@Composable
private fun NeumorphicInputPill(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    leadingIconDescription: String,
    isPassword: Boolean,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: (() -> Unit)?,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    enabled: Boolean,
    fieldTestTag: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Recessed Neumorphic Pill Well
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 6.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            RecessedWellTop,
                            RecessedWellMid,
                            RecessedWellBottom
                        )
                    )
                )
                .drawWithContent {
                    drawContent()
                    // Recessed top inner shadow groove
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to ClaySoftInnerShadow.copy(alpha = 0.22f),
                            0.26f to ClaySoftInnerShadow.copy(alpha = 0.05f),
                            0.42f to Color.Transparent
                        )
                    )
                    // Recessed left inner shadow groove
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0.0f to ClaySoftInnerShadow.copy(alpha = 0.16f),
                            0.12f to Color.Transparent
                        )
                    )
                }
                .border(
                    width = if (isFocused) 1.6.dp else 1.2.dp,
                    brush = if (isFocused) {
                        Brush.linearGradient(
                            colors = listOf(
                                RoyalBlueLight.copy(alpha = 0.85f),
                                RoyalBlueCore.copy(alpha = 0.65f)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                ClaySoftInnerShadow.copy(alpha = 0.24f),
                                Color.White.copy(alpha = 0.35f),
                                Color.White.copy(alpha = 0.96f)
                            )
                        )
                    },
                    shape = CircleShape
                )
                .padding(start = 52.dp, end = if (isPassword) 4.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = InputPlaceholderSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = InputTextNavy,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    cursorBrush = SolidColor(RoyalBlueCore),
                    visualTransformation = if (isPassword && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    modifier = modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                        .testTag(fieldTestTag)
                )
            }

            if (isPassword && onTogglePasswordVisibility != null) {
                IconButton(
                    onClick = onTogglePasswordVisibility,
                    enabled = enabled,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("login_password_toggle")
                ) {
                    Icon(
                        imageVector = if (passwordVisible) {
                            Icons.Rounded.VisibilityOff
                        } else {
                            Icons.Rounded.Visibility
                        },
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = EyeIconNavy,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }

        // Leading Glossy 3D Royal-Blue Circular Icon Medallion (sculpted sphere feel)
        Box(
            modifier = Modifier
                .size(46.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = RoyalBlueDeep,
                    spotColor = RoyalBlueDeep
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            RoyalBlueLight,
                            RoyalBlueCore,
                            RoyalBlueDeep
                        ),
                        center = Offset(16f, 14f),
                        radius = 85f
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.72f),
                            Color.White.copy(alpha = 0.14f),
                            Color(0xFF0A2E8A).copy(alpha = 0.55f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = leadingIconDescription,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Glossy 3D Royal-Blue Claymorphic Pill Button ("Login ->") with top bevel highlight and spring press.
 */
@Composable
private fun ClayRoyalLoginButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 460f),
        label = "login_btn_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed) 6.dp else 14.dp,
                shape = CircleShape,
                ambientColor = RoyalBlueDeep,
                spotColor = RoyalBlueDeep
            )
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        RoyalBlueLight,
                        RoyalBlueCore,
                        RoyalBlueDeep
                    )
                )
            )
            .border(
                width = 1.6.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.62f),
                        Color.White.copy(alpha = 0.16f),
                        Color(0xFF0A2C82).copy(alpha = 0.65f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !isLoading,
                onClick = onClick
            )
            .testTag("login_submit_button"),
        contentAlignment = Alignment.Center
    ) {
        // Subtle top glossy claymorphic reflection band
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 3.dp, start = 22.dp, end = 22.dp)
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.6.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.2.sp
                    ),
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "Submit login",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Raised White 3D Claymorphic Circular Quick-Pick Member Medallion matching the bottom row
 * of 3 sculptural white spheres in the attached reference image.
 */
@Composable
private fun QuickMemberClayCircle(
    option: QuickMemberOption,
    displayLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .shadow(
                    elevation = if (isSelected) 12.dp else 8.dp,
                    shape = CircleShape,
                    ambientColor = if (isSelected) RoyalBlueCore else ClayAmbientBlueShadow.copy(alpha = 0.28f),
                    spotColor = if (isSelected) RoyalBlueCore else ClayAmbientBlueShadow.copy(alpha = 0.32f)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFF6F2F0),
                            Color(0xFFE7E0DD)
                        )
                    )
                )
                .border(
                    width = if (isSelected) 2.2.dp else 1.4.dp,
                    brush = if (isSelected) {
                        Brush.linearGradient(listOf(RoyalBlueLight, RoyalBlueDeep))
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.White,
                                Color(0xFFD6CBC8).copy(alpha = 0.65f)
                            )
                        )
                    },
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
                .testTag("quick_pick_${option.member.name.lowercase()}"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(option.accentStart, option.accentEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.badgeInitial,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = Color.White
                )
            }
        }

        Text(
            text = option.username,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
            ),
            color = if (isSelected) RoyalBrandBlue else SubtitleNavy,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
