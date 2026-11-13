package com.mindgate.presentation.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindgate.R
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import com.mindgate.presentation.viewmodel.RoleGateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleGateScreen(
    onRoleSelected: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: RoleGateViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(AppIcons.Settings, contentDescription = "Connection Settings", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_mindgate_logo),
                    contentDescription = "MindGate Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "Account Setup",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Would you like to manage a child's device or use this device for yourself?",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TextSecondary,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(56.dp))
            
            MindGateButton(
                text = "I am a Parent",
                onClick = { viewModel.selectRole("parent") { onRoleSelected("parent") } },
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Primary,
                isLoading = isLoading,
                icon = AppIcons.Family
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            MindGateButton(
                text = "I am a Student",
                onClick = { viewModel.selectRole("student") { onRoleSelected("student") } },
                modifier = Modifier.fillMaxWidth(),
                style = ButtonStyle.Neutral,
                isSecondary = true,
                enabled = !isLoading,
                icon = AppIcons.School
            )

            if (error != null) {
                Surface(
                    modifier = Modifier.padding(top = 24.dp),
                    color = Danger.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = error!!,
                        color = Danger,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
