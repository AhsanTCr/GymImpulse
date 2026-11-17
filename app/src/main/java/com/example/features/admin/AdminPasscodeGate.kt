package com.example.features.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.ApexPulseLogo
import com.example.ui.theme.LocalGymSemanticColors

@Composable
fun AdminPasscodeGate(
    isBackendAdminAuthorized: Boolean,
    adminEmail: String,
    passcodeError: String?,
    onUnlockWithPasscode: (String) -> Unit,
    onSwitchToMemberView: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onSwitchToMemberView() }
    val semantic = LocalGymSemanticColors.current
    var passcodeInput by remember { mutableStateOf("") }
    var revealPasscode by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ApexPulseLogo(size = 48.dp)

                if (!isBackendAdminAuthorized) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = semantic.alertCoralContainer,
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = "Admin Access Restricted",
                                tint = semantic.alertCoral,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                    Text(
                        text = "Admin Access Restricted",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Your account ($adminEmail) does not hold verified club administrator privileges on the backend.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onSwitchToMemberView,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_denied_return_member_button")
                    ) {
                        Text("Return to Member Dashboard")
                    }
                    TextButton(onClick = onLogout) {
                        Text("Sign Out", color = semantic.alertCoral)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = semantic.warningAmberContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.AdminPanelSettings,
                                contentDescription = "Admin Security Lock",
                                tint = semantic.warningAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Text(
                        text = "Staff & Admin Access",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Signed in as: $adminEmail\nEnter the gym operations passcode to access management tools.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = passcodeInput,
                        onValueChange = { passcodeInput = it },
                        label = { Text("Security Passcode") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { revealPasscode = !revealPasscode }) {
                                Icon(
                                    imageVector = if (revealPasscode) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (revealPasscode) "Hide passcode" else "Show passcode"
                                )
                            }
                        },
                        visualTransformation = if (revealPasscode) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onUnlockWithPasscode(passcodeInput) }),
                        isError = !passcodeError.isNullOrBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_passcode_input")
                    )

                    if (!passcodeError.isNullOrBlank()) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = semantic.alertCoralContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = passcodeError,
                                style = MaterialTheme.typography.bodySmall,
                                color = semantic.alertCoral,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { onUnlockWithPasscode(passcodeInput) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_passcode_unlock_button")
                    ) {
                        Text(
                            text = "Unlock Management Console",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    OutlinedButton(
                        onClick = onSwitchToMemberView,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_switch_member_button")
                    ) {
                        Text("Switch to Member App")
                    }

                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("admin_gate_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = semantic.alertCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Sign Out", color = semantic.alertCoral)
                    }
                }
            }
        }
    }
}
