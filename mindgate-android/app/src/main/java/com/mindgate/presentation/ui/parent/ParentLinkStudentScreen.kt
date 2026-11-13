package com.mindgate.presentation.ui.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.viewmodel.ParentConnectionViewModel
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentLinkStudentScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: ParentConnectionViewModel = hiltViewModel()
) {
    var manualCode by remember { mutableStateOf("") }
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Link Child Account", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.Back, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.mindgate.presentation.ui.theme.Surface)
            )
        },
        containerColor = Surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = RoundedCornerShape(32.dp),
                color = PrimaryContainer,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        AppIcons.Add,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Primary
                    )
                }
            }
            
            Spacer(Modifier.height(40.dp))
            
            Text(
                "Link Your Child",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = TextPrimary
            )
            
            Text(
                "Connect to your child's account to enable remote monitoring and time extensions.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp, bottom = 32.dp),
                color = TextSecondary,
                lineHeight = 24.sp
            )

            Surface(
                color = PrimaryContainer.copy(alpha = 0.3f),
                shape = Shapes.medium,
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.HelpOutline, null, tint = Primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "On your child's device, go to Profile → Parent Connection to find the linking code.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            OutlinedTextField(
                value = manualCode,
                onValueChange = { manualCode = it.uppercase() },
                label = { Text("6-Digit Code") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("MG-XXXXXX") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = Shapes.small,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    focusedLabelColor = Primary
                )
            )
            
            if (error != null) {
                Surface(
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                    color = Danger.copy(alpha = 0.1f),
                    shape = Shapes.small
                ) {
                    Text(
                        text = error.toString(),
                        color = Danger,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(Modifier.height(40.dp))
            
            MindGateButton(
                text = "Verify & Link",
                onClick = { 
                    val normalized = manualCode.uppercase()
                        .replace("MG-", "")
                        .replace("-", "")
                        .trim()
                    viewModel.acceptCode("MG-$normalized", onSuccess) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = manualCode.isNotBlank() && !isLoading,
                style = ButtonStyle.Brand,
                isLoading = isLoading,
                icon = AppIcons.Approved
            )
        }
    }
}
