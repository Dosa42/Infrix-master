package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.ui.theme.AdminPrimary
import com.example.ui.theme.DarkAppBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.KlantPrimary
import com.example.ui.theme.PrimaryBlueGlow
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WerkerPrimary
import com.example.ui.viewmodel.AuthViewModel

@Composable
fun SimpleLoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: (UserEntity) -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkAppBackground)
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Inloggen",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Text(
            text = "Veilig Toegangs- & Beheerportaal",
            fontSize = 13.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 1. Keuze uit 3 types: Admin, Werker, Klant
                Text(
                    text = "Gebruikerstype:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    // Admin Keuze
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { authViewModel.onRoleSelected(UserRole.ADMIN) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.selectedRole == UserRole.ADMIN,
                            onClick = { authViewModel.onRoleSelected(UserRole.ADMIN) },
                            colors = RadioButtonDefaults.colors(selectedColor = AdminPrimary, unselectedColor = TextMuted),
                            modifier = Modifier.testTag("role_select_admin")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin (Hoogste authority)",
                            fontSize = 15.sp,
                            fontWeight = if (uiState.selectedRole == UserRole.ADMIN) FontWeight.Bold else FontWeight.Medium,
                            color = if (uiState.selectedRole == UserRole.ADMIN) AdminPrimary else TextSecondary
                        )
                    }

                    // Werker Keuze
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { authViewModel.onRoleSelected(UserRole.WERKER) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.selectedRole == UserRole.WERKER,
                            onClick = { authViewModel.onRoleSelected(UserRole.WERKER) },
                            colors = RadioButtonDefaults.colors(selectedColor = WerkerPrimary, unselectedColor = TextMuted),
                            modifier = Modifier.testTag("role_select_werker")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Werker",
                            fontSize = 15.sp,
                            fontWeight = if (uiState.selectedRole == UserRole.WERKER) FontWeight.Bold else FontWeight.Medium,
                            color = if (uiState.selectedRole == UserRole.WERKER) WerkerPrimary else TextSecondary
                        )
                    }

                    // Klant Keuze
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { authViewModel.onRoleSelected(UserRole.KLANT) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.selectedRole == UserRole.KLANT,
                            onClick = { authViewModel.onRoleSelected(UserRole.KLANT) },
                            colors = RadioButtonDefaults.colors(selectedColor = KlantPrimary, unselectedColor = TextMuted),
                            modifier = Modifier.testTag("role_select_klant")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Klant",
                            fontSize = 15.sp,
                            fontWeight = if (uiState.selectedRole == UserRole.KLANT) FontWeight.Bold else FontWeight.Medium,
                            color = if (uiState.selectedRole == UserRole.KLANT) KlantPrimary else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkCardBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // 2. Typeveld voor gebruiker-naam
                Text(
                    text = "Gebruikersnaam:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = uiState.usernameInput,
                    onValueChange = { authViewModel.onUsernameChanged(it) },
                    placeholder = { Text("Voer uw gebruikersnaam in", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlueGlow,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkAppBackground,
                        unfocusedContainerColor = DarkAppBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_username_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Typeveld voor passwoord
                Text(
                    text = "Wachtwoord:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = uiState.passwordInput,
                    onValueChange = { authViewModel.onPasswordChanged(it) },
                    placeholder = { Text("Voer uw wachtwoord in", color = TextMuted) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryBlueGlow,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedContainerColor = DarkAppBackground,
                        unfocusedContainerColor = DarkAppBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_password_input")
                )

                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = StatusDanger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { authViewModel.login(onLoginSuccess) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("login_button"),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (uiState.selectedRole) {
                            UserRole.ADMIN -> Color(0xFF7C3AED)
                            UserRole.WERKER -> Color(0xFF0284C7)
                            UserRole.KLANT -> Color(0xFF0D9488)
                        }
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = "Inloggen",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
