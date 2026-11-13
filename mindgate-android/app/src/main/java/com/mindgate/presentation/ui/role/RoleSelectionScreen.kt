package com.mindgate.presentation.ui.role

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindgate.R

import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*

@Composable
fun RoleSelectionScreen(
    onStudentClick: () -> Unit,
    onParentClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Surface
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = AppIcons.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Identity Area
                Box(
                    modifier = Modifier.size(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_mindgate_logo),
                        contentDescription = "MindGate Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(Modifier.height(32.dp))
                
                Text(
                    text = "MindGate",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = TextPrimary
                )
                
                Text(
                    text = "Personalized Digital Wellbeing",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(56.dp))

                // Modern Portal Cards
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    PortalCard(
                        title = "Student",
                        description = "Take control of your focus with smart limits and mindful challenges.",
                        icon = AppIcons.School,
                        buttonText = "Enter Student Portal",
                        onClick = onStudentClick,
                        style = ButtonStyle.Brand
                    )

                    PortalCard(
                        title = "Parent",
                        description = "Stay informed about your child's digital habits and risk levels.",
                        icon = AppIcons.Family,
                        buttonText = "Enter Parent Portal",
                        onClick = onParentClick,
                        style = ButtonStyle.Brand
                    )
                }
            }
        }
    }
}

@Composable
private fun PortalCard(
    title: String,
    description: String,
    icon: ImageVector,
    buttonText: String,
    onClick: () -> Unit,
    style: ButtonStyle,
    isSecondary: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = SurfaceCard,
        shape = Shapes.medium,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = Shapes.small,
                    color = PrimaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, modifier = Modifier.size(24.dp), tint = Primary)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(20.dp))
            
            MindGateButton(
                text = buttonText,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                style = style,
                isSecondary = isSecondary
            )
        }
    }
}
