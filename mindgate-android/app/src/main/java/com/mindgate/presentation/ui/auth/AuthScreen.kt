package com.mindgate.presentation.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.Image
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.mindgate.R

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    initialRole: String = "student",
    onSuccess: (Long, String, Boolean) -> Unit,
    onSettingsClick: () -> Unit = {},
    onTeacherClick: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.mindgate.R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }
    
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { idToken ->
                viewModel.signInWithGoogle(idToken) { id, role, needs -> onSuccess(id, role, needs) }
            }
        } catch (e: ApiException) {
            // Error handled by state in ViewModel
        }
    }

    // Form Tab state: 0 for Login, 1 for Register
    var selectedFormTab by remember { mutableIntStateOf(0) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var yearInput by remember { mutableIntStateOf(1) }
    var genderInput by remember { mutableStateOf("Male") }
    
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    fun validateStudentForm(): Boolean {
        var isValid = true
        if (selectedFormTab == 1 && nameInput.isBlank()) {
            nameError = "Full Name is required"
            isValid = false
        } else nameError = null

        val email = emailInput.trim()
        if (email.isBlank()) {
            emailError = "Email is required"
            isValid = false
        } else if (!email.contains("@")) {
            emailError = "Invalid email format"
            isValid = false
        } else emailError = null

        if (passwordInput.isBlank()) {
            passwordError = "Password is required"
            isValid = false
        } else if (selectedFormTab == 1 && passwordInput.length < 6) {
            passwordError = "At least 6 characters"
            isValid = false
        } else passwordError = null

        return isValid
    }

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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(16.dp))
            
            // App Logo / Identity
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
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = "MindGate",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                ),
                color = TextPrimary
            )
            
            Text(
                text = "Digital Wellbeing for ${initialRole.replaceFirstChar { it.uppercase() }}s",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )
            
            Spacer(Modifier.height(40.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.medium,
                colors = CardDefaults.elevatedCardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    // Modern Tab Row
                    TabRow(
                        selectedTabIndex = selectedFormTab,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = { tabPositions ->
                            if (selectedFormTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedFormTab]),
                                    color = Primary
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedFormTab == 0,
                            onClick = { selectedFormTab = 0 },
                            selectedContentColor = Primary,
                            unselectedContentColor = TextSecondary
                        ) {
                            Text("Login", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold)
                        }
                        Tab(
                            selected = selectedFormTab == 1,
                            onClick = { selectedFormTab = 1 },
                            selectedContentColor = Primary,
                            unselectedContentColor = TextSecondary
                        ) {
                            Text("Register", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Form Fields with AnimatedContent for smooth transitions
                    AnimatedContent(
                        targetState = selectedFormTab,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "auth_form"
                    ) { tab ->
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (tab == 1) {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it; nameError = null },
                                    label = { Text("Full Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Primary) },
                                    isError = nameError != null,
                                    supportingText = { nameError?.let { Text(it) } },
                                    shape = Shapes.small,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        focusedLabelColor = Primary
                                    )
                                )
                            }

                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = { emailInput = it; emailError = null },
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Email, null, tint = Primary) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                isError = emailError != null,
                                supportingText = { emailError?.let { Text(it) } },
                                shape = Shapes.small,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    focusedLabelColor = Primary
                                )
                            )

                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it; passwordError = null },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Primary) },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, null, tint = TextSecondary)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                isError = passwordError != null,
                                supportingText = { passwordError?.let { Text(it) } },
                                shape = Shapes.small,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    focusedLabelColor = Primary
                                )
                            )

                            if (tab == 1 && initialRole == "student") {
                                Column {
                                    Text("Year of Study", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        (1..4).forEach { y ->
                                            FilterChip(
                                                selected = yearInput == y,
                                                onClick = { yearInput = y },
                                                label = { Text(y.toString()) },
                                                modifier = Modifier.weight(1f),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PrimaryContainer,
                                                    selectedLabelColor = Primary
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = yearInput == y,
                                                    borderColor = Outline,
                                                    selectedBorderColor = Primary
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (state.error != null) {
                        Surface(
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                            color = Danger.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = state.error!!,
                                color = Danger,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    val isLoading = state.isLoading
                    var lastSubmitTime by remember { mutableLongStateOf(0L) }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MindGateButton(
                            text = if (selectedFormTab == 0) "Login" else "Create Account",
                            onClick = {
                                val now = System.currentTimeMillis()
                                if (now - lastSubmitTime < 500) return@MindGateButton
                                lastSubmitTime = now

                                if (validateStudentForm()) {
                                    if (selectedFormTab == 0) viewModel.login(emailInput, passwordInput) { id, role, needs -> onSuccess(id, role, needs) }
                                    else viewModel.register(nameInput, emailInput, passwordInput, yearInput, genderInput, initialRole) { id, role, needs -> onSuccess(id, role, needs) }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            style = ButtonStyle.Brand,
                            isLoading = isLoading
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(Modifier.weight(1f), color = Outline)
                            Text(" OR ", style = MaterialTheme.typography.labelSmall, color = ActionDisabledText, modifier = Modifier.padding(horizontal = 8.dp))
                            HorizontalDivider(Modifier.weight(1f), color = Outline)
                        }

                        MindGateButton(
                            text = "Sign in with Google",
                            onClick = { googleLauncher.launch(googleSignInClient.signInIntent) },
                            modifier = Modifier.fillMaxWidth(),
                            style = ButtonStyle.Neutral,
                            isSecondary = true,
                            enabled = !isLoading,
                            iconPainter = painterResource(id = com.mindgate.R.drawable.ic_google_logo)
                        )

                        if (initialRole != "teacher") {
                            MindGateButton(
                                text = "Teacher Portal",
                                onClick = onTeacherClick,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                style = ButtonStyle.Neutral,
                                isSecondary = true,
                                icon = Icons.Default.AdminPanelSettings
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (state.showRoleSelection) {
        Dialog(onDismissRequest = { viewModel.cancelRoleSelection() }) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = Shapes.large,
                colors = CardDefaults.elevatedCardColors(containerColor = SurfaceCard)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Welcome to MindGate",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "To personalize your experience, please tell us who you are.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(32.dp))
                    
                    MindGateButton(
                        text = "I am a Student",
                        onClick = { viewModel.completeGoogleAuth("student") { id, role, needs -> onSuccess(id, role, needs) } },
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Brand,
                        icon = AppIcons.School
                    )
                    Spacer(Modifier.height(12.dp))
                    MindGateButton(
                        text = "I am a Parent",
                        onClick = { viewModel.completeGoogleAuth("parent") { id, role, needs -> onSuccess(id, role, needs) } },
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Brand,
                        icon = AppIcons.Family
                    )
                }
            }
        }
    }
}
