package com.mindgate.presentation.ui.screens.apppicker

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mindgate.presentation.ui.components.ButtonStyle
import com.mindgate.presentation.ui.components.MindGateButton
import com.mindgate.presentation.ui.theme.*
import com.mindgate.util.AppCategory
import com.mindgate.util.AppInfo

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    onNavigateToLimitSetup: (List<String>) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: AppPickerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Apps",
                        style = MaterialTheme.typography.headlineLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)
                    )
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Icon(AppIcons.Profile, "Profile", tint = Primary)
                    }
                }
                
                SearchBar(
                    query = state.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                CategoryFilters(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = viewModel::onCategoryChange,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = SurfaceCard
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    MindGateButton(
                        text = "Restrict ${state.selectedPackages.size} apps →",
                        onClick = { onNavigateToLimitSetup(state.selectedPackages.toList()) },
                        enabled = state.selectedPackages.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        style = ButtonStyle.Primary
                    )
                }
            }
        },
        containerColor = Surface
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else {
            val groupedApps = remember(state.apps) {
                state.apps.groupBy { it.category }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                groupedApps.forEach { (category, categoryApps) ->
                    item(key = "header_${category.name}") {
                        Surface(
                            color = PrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = category.name.lowercase().capitalize(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    items(categoryApps, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            usedMinutes = state.usageMap[app.packageName] ?: 0,
                            isSelected = state.selectedPackages.contains(app.packageName),
                            onToggle = { viewModel.toggleSelection(app.packageName) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp)),
        placeholder = { Text("Search apps...", color = TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Outline.copy(alpha = 0.3f),
            unfocusedContainerColor = Outline.copy(alpha = 0.3f),
            disabledContainerColor = Outline.copy(alpha = 0.3f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        singleLine = true,
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
fun CategoryFilters(
    selectedCategory: AppCategory,
    onCategorySelected: (AppCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(AppCategory.values()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.name.lowercase().capitalize()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Primary,
                    selectedLabelColor = SurfaceCard,
                    labelColor = TextSecondary,
                    containerColor = SurfaceCard
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCategory == category,
                    borderColor = Outline,
                    selectedBorderColor = Primary
                ),
                shape = CircleShape
            )
        }
    }
}

@Composable
fun AppRow(
    app: AppInfo,
    usedMinutes: Int,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .background(if (isSelected) PrimaryContainer.copy(alpha = 0.5f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            AppIcon(
                drawable = app.icon,
                modifier = Modifier
                    .size(40.dp)
                    .clip(Shapes.small)
            )
            
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Success.copy(alpha = 0.4f), Shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        AppIcons.Approved,
                        contentDescription = null,
                        tint = SurfaceCard,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = if (usedMinutes > 0) "$usedMinutes min used today" else app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = if (usedMinutes > 0) Primary else TextSecondary
            )
        }
        
        if (isSelected) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AppIcon(drawable: Drawable, modifier: Modifier = Modifier) {
    val bitmap = remember(drawable) {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp.asImageBitmap()
    }
    Image(bitmap = bitmap, contentDescription = null, modifier = modifier)
}

// Helper extension to capitalize string
private fun String.capitalize(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
