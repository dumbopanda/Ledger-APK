package com.dumbopanda.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dumbopanda.ledger.core.designsystem.CanvasDark
import com.dumbopanda.ledger.core.designsystem.Coral
import com.dumbopanda.ledger.core.designsystem.Forest
import com.dumbopanda.ledger.core.designsystem.LedgerTheme
import com.dumbopanda.ledger.core.designsystem.Lime
import com.dumbopanda.ledger.core.designsystem.MutedOnDark
import com.dumbopanda.ledger.core.designsystem.SystemChrome
import com.dumbopanda.ledger.core.designsystem.SystemChromeEffect
import com.dumbopanda.ledger.core.designsystem.TextDark
import com.dumbopanda.ledger.core.designsystem.TextMuted
import com.dumbopanda.ledger.core.designsystem.component.DisplayHeadline
import com.dumbopanda.ledger.core.designsystem.component.FirstRunChip
import com.dumbopanda.ledger.core.designsystem.component.FirstRunField
import com.dumbopanda.ledger.core.designsystem.component.LedgerCta
import com.dumbopanda.ledger.core.designsystem.component.LedgerHeroSheet
import com.dumbopanda.ledger.core.designsystem.webText
import com.dumbopanda.ledger.core.model.ProfileOccupations

@Composable
fun ProfileSetupRoute(onDone: () -> Unit, viewModel: ProfileSetupViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    SystemChromeEffect(SystemChrome.DarkOverCream)
    ProfileSetupScreen(
        uiState = uiState,
        occupationOptions = viewModel.occupationOptions,
        onNameChanged = viewModel::onNameChanged,
        onAgeChanged = viewModel::onAgeChanged,
        onLocationChanged = viewModel::onLocationChanged,
        onOccupationSelected = viewModel::onOccupationSelected,
        onConfirm = viewModel::confirm,
        onSkip = viewModel::skip,
        onDone = onDone,
    )
}

/**
 * C14: runs once, between Restoring and the app tour, only for an account with no profile
 * details saved on the server yet. The name normally arrives already known (Google, or the
 * Sign up form) and the hero greets by it; it's only asked here when neither supplied one.
 */
@Composable
internal fun ProfileSetupScreen(
    uiState: ProfileSetupUiState,
    occupationOptions: List<String>,
    onNameChanged: (String) -> Unit,
    onAgeChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onOccupationSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
    onDone: () -> Unit,
) {
    if (uiState is ProfileSetupUiState.AlreadyHasProfile || uiState is ProfileSetupUiState.Done) {
        LaunchedEffect(uiState) { onDone() }
    }
    when (uiState) {
        is ProfileSetupUiState.Ready -> ProfileForm(
            uiState, occupationOptions, onNameChanged, onAgeChanged, onLocationChanged, onOccupationSelected, onConfirm, onSkip,
        )
        else -> Box(Modifier.fillMaxSize().background(CanvasDark), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Lime)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileForm(
    state: ProfileSetupUiState.Ready,
    occupationOptions: List<String>,
    onNameChanged: (String) -> Unit,
    onAgeChanged: (String) -> Unit,
    onLocationChanged: (String) -> Unit,
    onOccupationSelected: (String) -> Unit,
    onConfirm: () -> Unit,
    onSkip: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val firstName = state.name.trim().substringBefore(' ')

    LedgerHeroSheet(
        hero = {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 32.dp)) {
                Text("ABOUT YOU", style = webText(12f, FontWeight.ExtraBold, Lime, letterSpacing = 1.4f))
                Spacer(Modifier.height(12.dp))
                DisplayHeadline(
                    lead = "Good to have you,",
                    accent = if (!state.askName && firstName.isNotEmpty()) "$firstName." else "friend.",
                    size = 32f,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Three quick questions, all optional. Your answers tell us who Ledger is for, " +
                        "so what we build next fits you. Never shown to anyone. Never sold.",
                    style = webText(15f, FontWeight.Normal, MutedOnDark, lineHeight = 22f),
                )
            }
        },
        sheetTop = {
            if (state.askName) {
                Question("What should we call you?")
                FirstRunField(
                    value = state.name,
                    onValueChange = onNameChanged,
                    placeholder = "Your first name",
                    isError = state.nameError != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    autofill = ContentType.PersonFirstName,
                    description = "Your name",
                )
                FieldError(state.nameError)
                Spacer(Modifier.height(22.dp))
            }

            Question("How old are you?")
            FirstRunField(
                value = state.age,
                onValueChange = onAgeChanged,
                placeholder = "e.g. 27",
                isError = state.ageError != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                description = "Age",
            )
            FieldError(state.ageError)
            Spacer(Modifier.height(22.dp))

            Question("Which city do you live in?")
            FirstRunField(
                value = state.location,
                onValueChange = onLocationChanged,
                placeholder = "e.g. Pune",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                autofill = ContentType.AddressLocality,
                description = "City",
            )
            Spacer(Modifier.height(22.dp))

            Question("What do you do?")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                occupationOptions.forEach { option ->
                    FirstRunChip(
                        text = option,
                        selected = option == state.occupation,
                        onClick = {
                            focusManager.clearFocus()
                            onOccupationSelected(option)
                        },
                    )
                }
            }
        },
        sheetBottom = {
            LedgerCta(text = if (state.saving) "Saving…" else "Continue", onClick = onConfirm, enabled = !state.saving)
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(enabled = !state.saving, role = Role.Button, onClick = onSkip),
                contentAlignment = Alignment.Center,
            ) {
                Text("Skip for now", style = webText(14f, FontWeight.Bold, Forest), textAlign = TextAlign.Center)
            }
        },
    )
}

@Composable
private fun Question(text: String) {
    Text(text, style = webText(15f, FontWeight.Bold, TextDark))
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun FieldError(text: String?) {
    if (text == null) return
    Spacer(Modifier.height(8.dp))
    Text(text, style = webText(13f, FontWeight.SemiBold, Coral))
}

@Preview(heightDp = 1100)
@Composable
private fun ProfileSetupScreenPreview() {
    LedgerTheme {
        ProfileSetupScreen(
            uiState = ProfileSetupUiState.Ready(name = "Bibhuti", occupation = "Student"),
            occupationOptions = ProfileOccupations.OPTIONS,
            onNameChanged = {},
            onAgeChanged = {},
            onLocationChanged = {},
            onOccupationSelected = {},
            onConfirm = {},
            onSkip = {},
            onDone = {},
        )
    }
}
