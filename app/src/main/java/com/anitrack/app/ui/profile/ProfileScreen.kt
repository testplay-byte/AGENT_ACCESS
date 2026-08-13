package com.anitrack.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.anitrack.app.ui.profile.ProfileViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.anitrack.app.R
import com.anitrack.app.ui.components.AniTrackBottomNavigation
import com.anitrack.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory)
) {
    val uiState = viewModel.uiState.collectAsState().value
    
    Scaffold(
        bottomBar = {
            AniTrackBottomNavigation(
                currentRoute = "profile",
                onNavigate = { }
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Header
            ProfileHeader()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Settings Section
            SettingsSection(
                uiState = uiState,
                viewModel = viewModel
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // About Section
            AboutSection(appVersion = uiState.appVersion)
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileHeader() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            SkyBlue600,
                            SkyBlue400,
                            Aqua300,
                            Aqua500
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(1f, 1f)
                    )
                )
                .padding(vertical = 36.dp, horizontal = 24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar - Premium Style with ring effect
                Box {
                    // Outer glow ring
                    Surface(
                        modifier = Modifier.size(110.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f)
                    ) {}
                    
                    // Main avatar container
                    Surface(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.Center),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 3.dp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Anime Fan",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = stringResource(R.string.app_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stats Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    StatBadge(label = "Watched", value = "127")
                    StatBadge(label = "Planning", value = "45")
                    StatBadge(label = "Completed", value = "89")
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.2f)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun SettingsSection(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel
) {
    Column {
        // Section Header with accent bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = SkyBlue500,
                modifier = Modifier.size(4.dp, 20.dp)
            ) {}
            Text(
                text = stringResource(R.string.profile_settings),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp
        ) {
            Column {
                // Remote Control Toggle
                SettingsToggleItem(
                    icon = Icons.Default.SettingsRemote,
                    iconColor = SkyBlue500,
                    title = stringResource(R.string.profile_remote_control),
                    description = stringResource(R.string.profile_remote_control_description),
                    isToggled = uiState.isRemoteControlEnabled,
                    onToggleChanged = { viewModel.toggleRemoteControl(it) },
                    showDivider = true
                )
                
                // Dark Mode Toggle
                SettingsToggleItem(
                    icon = Icons.Default.DarkMode,
                    iconColor = Aqua500,
                    title = stringResource(R.string.profile_dark_mode),
                    description = "Enable dark theme",
                    isToggled = uiState.isDarkModeEnabled,
                    onToggleChanged = { viewModel.toggleDarkMode(it) },
                    showDivider = true
                )
                
                // Notifications Toggle
                SettingsToggleItem(
                    icon = Icons.Default.Notifications,
                    iconColor = OrangeAccent,
                    title = stringResource(R.string.profile_notifications),
                    description = "Enable push notifications",
                    isToggled = uiState.isNotificationsEnabled,
                    onToggleChanged = { viewModel.toggleNotifications(it) },
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun AboutSection(appVersion: String) {
    Column {
        // Section Header with accent bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Aqua500,
                modifier = Modifier.size(4.dp, 20.dp)
            ) {}
            Text(
                text = stringResource(R.string.profile_about),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 3.dp
        ) {
            Column {
                SettingsClickableItem(
                    icon = Icons.Default.Info,
                    iconColor = SkyBlue500,
                    title = "Version",
                    trailingText = stringResource(R.string.profile_version, appVersion),
                    onClick = { },
                    showDivider = true
                )
                
                SettingsClickableItem(
                    icon = Icons.Default.Star,
                    iconColor = GoldAccent,
                    title = stringResource(R.string.profile_rate_app),
                    trailingText = "",
                    onClick = { },
                    showDivider = true
                )
                
                SettingsClickableItem(
                    icon = Icons.Default.Share,
                    iconColor = Aqua500,
                    title = stringResource(R.string.profile_share_app),
                    trailingText = "",
                    onClick = { },
                    showDivider = true
                )
                
                SettingsClickableItem(
                    icon = Icons.Default.PrivacyTip,
                    iconColor = SkyBlue600,
                    title = stringResource(R.string.profile_privacy_policy),
                    trailingText = "",
                    onClick = { },
                    showDivider = true
                )
                
                SettingsClickableItem(
                    icon = Icons.Default.Description,
                    iconColor = Aqua600,
                    title = stringResource(R.string.profile_terms_of_service),
                    trailingText = "",
                    onClick = { },
                    showDivider = false
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconColor: Color = SkyBlue500,
    title: String,
    description: String,
    isToggled: Boolean,
    onToggleChanged: (Boolean) -> Unit,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleChanged(!isToggled) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = isToggled,
                onCheckedChange = onToggleChanged,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SkyBlue500,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
        if (showDivider) {
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun SettingsClickableItem(
    icon: ImageVector,
    iconColor: Color = SkyBlue500,
    title: String,
    trailingText: String,
    onClick: () -> Unit,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.1f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            if (trailingText.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showDivider) {
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}
