package com.dumbopanda.ledger.feature.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.HairlineStrong
import com.dumbopanda.ledger.core.designsystem.InactiveOnDark
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.Mint
import com.dumbopanda.ledger.core.designsystem.MintInk
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.PlaceholderInk
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.TrackBorderOnDark
import com.dumbopanda.ledger.core.designsystem.TrackOnDark
import com.dumbopanda.ledger.core.designsystem.White
import com.dumbopanda.ledger.core.designsystem.component.DisplayHeadline
import com.dumbopanda.ledger.core.designsystem.component.FirstRunField
import com.dumbopanda.ledger.core.designsystem.component.LedgerBanner
import com.dumbopanda.ledger.core.designsystem.component.LedgerBannerTone
import com.dumbopanda.ledger.core.designsystem.component.LedgerBrandMark
import com.dumbopanda.ledger.core.designsystem.component.LedgerCta
import com.dumbopanda.ledger.core.designsystem.component.LedgerHeroSheet
import com.dumbopanda.ledger.core.designsystem.icon.LedgerLineIcons
import com.dumbopanda.ledger.core.designsystem.webText

internal const val PRIVACY_POLICY_URL = "https://dumbopanda.github.io/Ledger-Pages/privacy/"
internal const val TERMS_URL = "https://dumbopanda.github.io/Ledger-Pages/terms/"

/** Sign in and Sign up run the same email-code flow (the first verified code creates the
 *  account); Sign up also asks for a name. Declaration order is the toggle's left-to-right. */
enum class AuthMode { SignUp, SignIn }

@Composable
internal fun AuthScreen(
    mode: AuthMode,
    googleState: SignInUiState,
    emailState: EmailSignInUiState,
    sessionExpiredNote: Boolean,
    onModeChange: (AuthMode) -> Unit,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onSendCode: () -> Unit,
    onCodeChanged: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
    onContinueWithGoogle: () -> Unit,
    onOpenAddAccountSettings: () -> Unit,
) {
    if (emailState is EmailSignInUiState.EnteringCode) {
        CodeStep(emailState, onCodeChanged, onVerify, onResend, onChangeEmail)
    } else {
        EmailStep(
            mode, googleState, emailState, sessionExpiredNote,
            onModeChange, onNameChanged, onEmailChanged, onSendCode, onContinueWithGoogle, onOpenAddAccountSettings,
        )
    }
}

// ---- Step 1: email (sign in or sign up) ---------------------------------------------------------

@Composable
private fun EmailStep(
    mode: AuthMode,
    googleState: SignInUiState,
    emailState: EmailSignInUiState,
    sessionExpiredNote: Boolean,
    onModeChange: (AuthMode) -> Unit,
    onNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onSendCode: () -> Unit,
    onContinueWithGoogle: () -> Unit,
    onOpenAddAccountSettings: () -> Unit,
) {
    val entering = emailState as? EmailSignInUiState.EnteringEmail
    val sendingState = emailState as? EmailSignInUiState.SendingCode
    val email = entering?.email ?: sendingState?.email ?: ""
    val name = entering?.name ?: sendingState?.name ?: ""
    val emailError = entering?.error
    val nameError = entering?.nameError
    val sending = sendingState != null
    val signUp = mode == AuthMode.SignUp
    val focusManager = LocalFocusManager.current

    LedgerHeroSheet(
        heroAlignment = Alignment.CenterHorizontally,
        hero = {
            Column(
                Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ModeToggle(mode, onModeChange)
                Spacer(Modifier.height(36.dp))
                // The glow is painted around the mark, not laid out -- it spills past the mark's
                // 64 without pushing the headline down.
                LedgerBrandMark(
                    64.dp,
                    Modifier.drawBehind {
                        drawCircle(
                            Brush.radialGradient(listOf(Lime.copy(alpha = 0.25f), Color.Transparent), radius = 100.dp.toPx()),
                            radius = 100.dp.toPx(),
                        )
                    },
                )
                Spacer(Modifier.height(22.dp))
                if (signUp) {
                    DisplayHeadline(
                        "Start your", "Ledger.", size = 32f, breakBeforeAccent = false, textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(8.dp))
                    HeroBody("Your name, your email, one code.\nThat's the whole sign-up.")
                } else {
                    DisplayHeadline(
                        "Welcome", "back.", size = 32f, breakBeforeAccent = false, textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(8.dp))
                    HeroBody("Your ledger is right where you left it.")
                }
            }
        },
        sheetTop = {
            if (sessionExpiredNote) {
                LedgerBanner("Signed out. Your entries are safe on this device.", LedgerBannerTone.Warning)
                Spacer(Modifier.height(16.dp))
            }
            if (signUp) {
                FieldLabel("What should we call you?")
                Spacer(Modifier.height(8.dp))
                FirstRunField(
                    value = name,
                    onValueChange = onNameChanged,
                    placeholder = "Your first name",
                    icon = null,
                    isError = nameError != null,
                    enabled = !sending,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    autofill = ContentType.PersonFirstName,
                    description = "Your name",
                )
                if (nameError != null) FieldError(nameError)
                Spacer(Modifier.height(16.dp))
            }
            FieldLabel(if (signUp) "Your email" else "Email")
            Spacer(Modifier.height(8.dp))
            FirstRunField(
                value = email,
                onValueChange = onEmailChanged,
                placeholder = "you@example.com",
                icon = LedgerLineIcons.Mail,
                isError = emailError != null,
                enabled = !sending,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onSend = { onSendCode() }),
                autofill = ContentType.EmailAddress,
                description = "Email address",
            )
            if (emailError != null) FieldError(emailError)
            Spacer(Modifier.height(20.dp))
            LedgerCta(
                text = when {
                    sending -> "Sending your code…"
                    signUp -> "Create my Ledger"
                    else -> "Email me a sign-in code"
                },
                onClick = onSendCode,
                enabled = !sending,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "We'll email you a 6-digit code. No password to remember, ever.",
                style = webText(13f, FontWeight.Normal, TextMuted, lineHeight = 18f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(22.dp))
            OrDivider()
            Spacer(Modifier.height(22.dp))
            GoogleSection(googleState, signUp, onContinueWithGoogle, onOpenAddAccountSettings)
        },
        sheetBottom = { LegalLine(signUp) },
    )
}

@Composable
private fun HeroBody(text: String) {
    Text(text, style = webText(15f, FontWeight.Normal, MutedOnDark, lineHeight = 22f), textAlign = TextAlign.Center)
}

/** The compact Sign up / Sign in pill -- same weight as the entry sheet's Open/Close toggle, not
 *  a full-width tab bar. 48 tall overall, so each half is a full-size touch target. */
@Composable
private fun ModeToggle(mode: AuthMode, onModeChange: (AuthMode) -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .background(TrackOnDark)
            .border(1.dp, TrackBorderOnDark, CircleShape)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        AuthMode.entries.forEach { option ->
            val selected = option == mode
            val fill by animateColorAsState(if (selected) Lime else Color.Transparent, label = "segFill")
            val ink by animateColorAsState(if (selected) CanvasDark else InactiveOnDark, label = "segInk")
            Box(
                Modifier
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onModeChange(option) })
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (option == AuthMode.SignIn) "Sign in" else "Sign up",
                    style = webText(13f, if (selected) FontWeight.ExtraBold else FontWeight.Bold, ink),
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = webText(13f, FontWeight.SemiBold, TextMuted))
}

@Composable
private fun FieldError(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(text, style = webText(13f, FontWeight.SemiBold, Coral))
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(HairlineStrong))
        Text("OR", style = webText(12f, FontWeight.Bold, TextMuted))
        Box(Modifier.weight(1f).height(1.dp).background(HairlineStrong))
    }
}

/** Google stays exactly as it worked before -- same Credential Manager flow -- just quieter:
 *  a white outline button under the email path, with its errors shown right above it. */
@Composable
private fun GoogleSection(state: SignInUiState, signUp: Boolean, onContinue: () -> Unit, onOpenAddAccountSettings: () -> Unit) {
    when (state) {
        SignInUiState.NoGoogleAccountOnDevice -> {
            LedgerBanner("There's no Google account on this phone yet.", LedgerBannerTone.Warning)
            Spacer(Modifier.height(12.dp))
        }
        is SignInUiState.NetworkFailure -> {
            LedgerBanner(state.message, LedgerBannerTone.Error)
            Spacer(Modifier.height(12.dp))
        }
        SignInUiState.TokenRejected -> {
            LedgerBanner("Google sign-in didn't go through. Try again.", LedgerBannerTone.Error)
            Spacer(Modifier.height(12.dp))
        }
        else -> Unit
    }
    val inProgress = state is SignInUiState.InProgress
    val noAccount = state is SignInUiState.NoGoogleAccountOnDevice
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CircleShape)
            .background(White)
            .border(1.dp, HairlineStrong, CircleShape)
            .clickable(enabled = !inProgress, role = Role.Button, onClick = if (noAccount) onOpenAddAccountSettings else onContinue),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (inProgress) {
            CircularProgressIndicator(Modifier.size(18.dp), color = Forest, strokeWidth = 2.dp)
        } else {
            Image(GoogleMark, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        Text(
            when {
                noAccount -> "Add a Google account"
                signUp -> "Sign up with Google"
                else -> "Continue with Google"
            },
            style = webText(15f, FontWeight.Bold, TextDark),
        )
    }
}

@Composable
private fun LegalLine(signUp: Boolean) {
    val linkStyle = TextLinkStyles(SpanStyle(color = TextDark, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline))
    Text(
        buildAnnotatedString {
            append(if (signUp) "By creating an account you agree to the " else "By continuing you agree to the ")
            withLink(LinkAnnotation.Url(PRIVACY_POLICY_URL, linkStyle)) { append("Privacy Policy") }
            append(" and ")
            withLink(LinkAnnotation.Url(TERMS_URL, linkStyle)) { append("Terms") }
            append(".")
        },
        style = webText(12f, FontWeight.Normal, TextMuted, lineHeight = 17f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---- Step 2: the code -------------------------------------------------------------------------

@Composable
private fun CodeStep(
    state: EmailSignInUiState.EnteringCode,
    onCodeChanged: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onChangeEmail: () -> Unit,
) {
    LedgerHeroSheet(
        hero = {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp)) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TrackOnDark)
                        .border(1.dp, TrackBorderOnDark, CircleShape)
                        .clickable(role = Role.Button, onClick = onChangeEmail)
                        .semantics { contentDescription = "Back to email" },
                    contentAlignment = Alignment.Center,
                ) { Icon(LedgerLineIcons.ChevronLeft, contentDescription = null, tint = White, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.height(26.dp))
                DisplayHeadline("Check your", "inbox.", size = 32f, breakBeforeAccent = false, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(10.dp))
                Text("Your 6-digit code is on its way to", style = webText(15f, FontWeight.Normal, MutedOnDark, lineHeight = 22f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(state.email, style = webText(15f, FontWeight.Bold, White), modifier = Modifier.weight(1f, fill = false))
                    Box(
                        Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onChangeEmail).padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Change", style = webText(14f, FontWeight.Bold, Lime)) }
                }
            }
        },
        sheetTop = {
            FieldLabel("Enter code")
            Spacer(Modifier.height(10.dp))
            CodeBoxes(state.code, onCodeChanged, onVerify, isError = state.error != null, enabled = !state.verifying)
            Spacer(Modifier.height(12.dp))
            Text(
                state.error ?: "Copied it? Paste it in and we'll fill all six boxes.",
                style = webText(13f, if (state.error != null) FontWeight.SemiBold else FontWeight.Normal, if (state.error != null) Coral else TextMuted, lineHeight = 18f),
            )
            Spacer(Modifier.height(22.dp))
            LedgerCta(
                text = if (state.verifying) "Checking your code…" else "Verify and continue",
                onClick = onVerify,
                enabled = state.code.length == 6 && !state.verifying,
            )
            Spacer(Modifier.height(6.dp))
            ResendRow(state, onResend)
        },
        sheetBottom = {
            Row(
                Modifier.fillMaxWidth().background(Mint, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(LedgerLineIcons.Clock, contentDescription = null, tint = MintInk, modifier = Modifier.size(20.dp))
                Text(
                    "The code works once and expires in 5 minutes. Five wrong tries locks it, so just ask for a new one.",
                    style = webText(13f, FontWeight.Medium, MintInk, lineHeight = 18f),
                )
            }
        },
    )
}

@Composable
private fun ResendRow(state: EmailSignInUiState.EnteringCode, onResend: () -> Unit) {
    val canResend = state.resendInS <= 0 && !state.resending
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .clickable(enabled = canResend, role = Role.Button, onClick = onResend),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Didn't get it?", style = webText(14f, FontWeight.Normal, TextMuted))
        Text(
            when {
                state.resending -> "Sending a new one…"
                state.resendInS > 0 -> "Resend in %d:%02d".format(state.resendInS / 60, state.resendInS % 60)
                else -> "Resend code"
            },
            style = webText(14f, FontWeight.Bold, if (canResend) Forest else PlaceholderInk),
        )
    }
}

/**
 * Six boxes over one real text field: typing, deleting and long-press → Paste all go to the
 * field, which is drawn invisibly across the boxes. The sixth digit submits by itself
 * (EmailSignInViewModel.onCodeChanged); Done on the keyboard does the same.
 */
@Composable
private fun CodeBoxes(code: String, onChange: (String) -> Unit, onDone: () -> Unit, isError: Boolean, enabled: Boolean) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focus = remember { FocusRequester() }
    // Focus on arrival, and again after each check: the field is disabled while a code is being
    // verified, which drops focus -- without this a wrong code leaves no box active and the
    // keyboard gone (seen live on the emulator).
    LaunchedEffect(enabled) { if (enabled) focus.requestFocus() }
    val caret by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(530), RepeatMode.Reverse),
        label = "caretAlpha",
    )

    BasicTextField(
        value = code,
        onValueChange = onChange,
        enabled = enabled,
        singleLine = true,
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focus)
            .semantics {
                contentType = ContentType.SmsOtpCode
                contentDescription = "6-digit code"
            },
        decorationBox = { inner ->
            Box {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(6) { index ->
                        val active = focused && index == code.length.coerceAtMost(5)
                        val border = when {
                            isError -> Coral
                            active -> Forest
                            else -> HairlineStrong
                        }
                        Box(
                            Modifier
                                .width(48.dp)
                                .height(60.dp)
                                .then(if (active) Modifier.background(Forest.copy(alpha = 0.12f), RoundedCornerShape(18.dp)).padding(3.dp) else Modifier.padding(3.dp))
                                .background(White, RoundedCornerShape(14.dp))
                                .border(if (active) 2.dp else 1.dp, border, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            val digit = code.getOrNull(index)
                            when {
                                digit != null -> Text(digit.toString(), style = webText(24f, FontWeight.ExtraBold, TextDark))
                                active -> Box(Modifier.width(2.dp).height(24.dp).alpha(caret).background(Forest))
                            }
                        }
                    }
                }
                Box(Modifier.matchParentSize().alpha(0f)) { inner() }
            }
        },
    )
}

// ---- Previews -----------------------------------------------------------------------------------

@Preview(heightDp = 900)
@Composable
private fun SignInPreview() {
    LedgerTheme {
        AuthScreen(
            AuthMode.SignIn, SignInUiState.Idle, EmailSignInUiState.EnteringEmail(), false,
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun SignUpPreview() {
    LedgerTheme {
        AuthScreen(
            AuthMode.SignUp, SignInUiState.Idle, EmailSignInUiState.EnteringEmail(), false,
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}

@Preview(heightDp = 900)
@Composable
private fun CodePreview() {
    LedgerTheme {
        AuthScreen(
            AuthMode.SignUp, SignInUiState.Idle,
            EmailSignInUiState.EnteringCode(email = "you@example.com", code = "482", resendInS = 42), false,
            {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
        )
    }
}
