package com.auction.app.ui.screens.verify

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.auction.app.LocalAppContainer
import com.auction.app.ui.navigation.Routes
import com.auction.app.ui.theme.AuctionAppTheme

@Composable
fun VerifyEmailScreen(navController: NavController, email: String) {
    val container = LocalAppContainer.current
    val viewModel: VerifyEmailViewModel = viewModel(factory = VerifyEmailViewModel.factory(container.repository, email))
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.verificationSuccess) {
        if (uiState.verificationSuccess) {
            viewModel.onVerificationHandled()
            navController.navigate(Routes.LOGIN) {
                popUpTo(Routes.LOGIN) { inclusive = true }
            }
        }
    }

    VerifyEmailScreenContent(
        uiState = uiState,
        email = email,
        onTokenChange = viewModel::onTokenChange,
        onVerifyClick = viewModel::verify,
        onNavigateToLogin = { navController.navigate(Routes.LOGIN) { popUpTo(0) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyEmailScreenContent(
    uiState: VerifyEmailUiState,
    email: String,
    onTokenChange: (String) -> Unit,
    onVerifyClick: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Scaffold() { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Xác nhận địa chỉ email",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val maskedEmail = remember(email) {
                if (email.contains("@")) {
                    val parts = email.split("@")
                    val username = parts[0]
                    val domain = parts[1]
                    if (username.isNotEmpty()) {
                        "${username[0]}******@$domain"
                    } else {
                        email
                    }
                } else {
                    email
                }
            }

            Text(
                text = "Một mã xác nhận đã được gửi đến email\n$maskedEmail.\nVui lòng kiểm tra và nhập mã vào bên dưới.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = uiState.token,
                onValueChange = onTokenChange,
                label = { Text("Mã xác nhận (6 ký tự)") },
                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            if (uiState.success != null) {
                Text(
                    text = uiState.success!!,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Button(
                onClick = onVerifyClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Xác thực")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateToLogin) {
                Text("Quay lại đăng nhập")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun VerifyEmailScreenPreview() {
    AuctionAppTheme {
        VerifyEmailScreenContent(
            uiState = VerifyEmailUiState(),
            email = "test@example.com",
            onTokenChange = {},
            onVerifyClick = {},
            onNavigateToLogin = {}
        )
    }
}
