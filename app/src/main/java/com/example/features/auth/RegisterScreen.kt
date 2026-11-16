package com.example.features.auth

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ApexSubScreenTopBar
import com.example.ui.components.MemberAvatar
import com.example.ui.components.PrimaryAthleticButton
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    isAuthenticatedOnboarding: Boolean = false,
    onRegistrationSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !isAuthenticatedOnboarding) { onNavigateBack() }
    val state by viewModel.registerState.collectAsStateWithLifecycle()
    val semantic = LocalGymSemanticColors.current
    val context = LocalContext.current

    // Zero-permission Android Photo Picker (Google Play compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> viewModel.setRegisterAvatar(uri?.toString()) }
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            ApexSubScreenTopBar(
                title = if (isAuthenticatedOnboarding) "Complete Member Profile" else "Create Account",
                subtitle = "Join GymPulse Performance Club",
                onBackClick = {
                    if (isAuthenticatedOnboarding) {
                        viewModel.signOut(context)
                    } else {
                        onNavigateBack()
                    }
                }
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                // Optional profile picture picker
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            MemberAvatar(
                                fullName = state.fullName.ifBlank { "New Member" },
                                avatarUri = state.selectedAvatarUri,
                                size = 72.dp,
                                borderColor = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("register_pick_photo_button"),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.AddAPhoto,
                                        contentDescription = "Select profile picture",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profile Picture (Optional)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (state.selectedAvatarUri == null)
                                    "Displayed on club leaderboards. Initials are used if skipped."
                                else "Photo selected! Compressed automatically on save.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (state.selectedAvatarUri == null) "Choose Photo" else "Change Photo",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.fullName,
                            onValueChange = { viewModel.updateRegisterField(fullName = it) },
                            label = { Text("Full Name") },
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_fullname_input"),
                            shape = MaterialTheme.shapes.small
                        )

                        OutlinedTextField(
                            value = state.phone,
                            onValueChange = { viewModel.updateRegisterField(phone = it) },
                            label = { Text("Phone Number (Optional)") },
                            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_phone_input"),
                            shape = MaterialTheme.shapes.small
                        )

                        if (state.email.isNotBlank()) {
                            OutlinedTextField(
                                value = state.email,
                                onValueChange = { viewModel.updateRegisterField(email = it) },
                                label = { Text("Google Account Email") },
                                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                                readOnly = isAuthenticatedOnboarding,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_email_input"),
                                shape = MaterialTheme.shapes.small
                            )
                        }

                        OutlinedTextField(
                            value = state.fitnessGoal,
                            onValueChange = { viewModel.updateRegisterField(fitnessGoal = it) },
                            label = { Text("Primary Fitness Goal") },
                            leadingIcon = { Icon(Icons.Rounded.FitnessCenter, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_goal_input"),
                            shape = MaterialTheme.shapes.small
                        )

                        OutlinedTextField(
                            value = state.referralCode,
                            onValueChange = { viewModel.updateRegisterField(referralCode = it) },
                            label = { Text("Friend's Referral Code (Optional)") },
                            placeholder = { Text("e.g. GP-REF-1042") },
                            leadingIcon = { Icon(Icons.Rounded.CardGiftcard, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_referral_input"),
                            shape = MaterialTheme.shapes.small
                        )

                        if (!state.errorMessage.isNullOrBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = semantic.alertCoralContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = state.errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = semantic.alertCoral,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        if (state.isSubmitting) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                Spacer(modifier = Modifier.size(10.dp))
                                Text("Creating your club profile...")
                            }
                        } else {
                            PrimaryAthleticButton(
                                text = if (isAuthenticatedOnboarding)
                                    "Save Member Profile" else "Continue with Google & Join",
                                testTag = "register_submit_button",
                                onClick = { viewModel.submitRegistration(context, onRegistrationSuccess) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isAuthenticatedOnboarding) {
                        TextButton(onClick = { viewModel.signOut(context) }) {
                            Text("Use a Different Google Account")
                        }
                    } else {
                        Text(
                            text = "Already a member? ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable(onClick = onNavigateBack)
                                .padding(horizontal = 4.dp)
                                .testTag("register_signin_link")
                        )
                    }
                }
            }
        }
    }
}
