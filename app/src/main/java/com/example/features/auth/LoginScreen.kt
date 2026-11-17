package com.example.features.auth

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.models.UserRole
import com.example.ui.components.ApexPulseLogo
import com.example.ui.components.PrimaryAthleticButton
import com.example.ui.components.SecondaryAthleticButton
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.loginState.collectAsStateWithLifecycle()
    val semantic = LocalGymSemanticColors.current
    val context = LocalContext.current
    var showAccountHelpDialog by remember { mutableStateOf(false) }

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
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            ApexPulseLogo(size = 54.dp)

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Sign in securely with Google to access entrance QR check-in, nutrition tracking, and live club leaderboards.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Role Selector
            Text(
                text = "ACCESS ROLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                UserRole.entries.forEach { role ->
                    val isSelected = state.selectedRole == role
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("role_chip_${role.name.lowercase()}"),
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        onClick = { viewModel.selectLoginRole(role) }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = role.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (role == UserRole.MEMBER) "Member Portal" else "Management",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (state.selectedRole == UserRole.MEMBER)
                                Icons.Rounded.VerifiedUser else Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = if (state.selectedRole == UserRole.MEMBER)
                                    "Member Sign-In" else "Staff & Admin Sign-In",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (state.selectedRole == UserRole.MEMBER)
                                    "Sign in with your Google account to access your gym profile"
                                else "Authorized gym staff sign in with their registered Google account",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

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

                    if (!state.infoMessage.isNullOrBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = semantic.infoCyanContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = state.infoMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.infoCyan,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    if (state.isSigningIn) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Spacer(modifier = Modifier.size(10.dp))
                            Text(
                                text = "Connecting to Google Identity...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        PrimaryAthleticButton(
                            text = "Sign in with Google",
                            icon = Icons.AutoMirrored.Rounded.Login,
                            testTag = "google_signin_button",
                            onClick = { viewModel.signInWithGoogle(context) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showAccountHelpDialog = true },
                            modifier = Modifier.testTag("forgot_password_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.HelpOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = "Account Recovery Info",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New to Gymimpulse? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onNavigateToRegister)
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("create_account_link")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            SecondaryAthleticButton(
                text = "Create Member Account",
                testTag = "login_create_account_button",
                onClick = onNavigateToRegister
            )
        }
    }

    if (showAccountHelpDialog) {
        AlertDialog(
            onDismissRequest = { showAccountHelpDialog = false },
            title = { Text("Account Access & Recovery") },
            text = {
                Text(
                    text = "Gymimpulse uses Google Sign-In so you never have to manage a separate gym password. If you cannot access your Google account, recover it via accounts.google.com and sign back in with one tap."
                )
            },
            confirmButton = {
                TextButton(onClick = { showAccountHelpDialog = false }) { Text("Got It") }
            }
        )
    }
}
