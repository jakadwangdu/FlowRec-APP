package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentRed
import com.example.ui.viewmodel.Screen

@Composable
fun FlowRecBottomNav(
    currentTab: Screen,
    onTabSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Rounded Capsule Dock Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color.Black.copy(alpha = 0.45f))
                .clip(RoundedCornerShape(26.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(26.dp))
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. HOME
                BottomNavItem(
                    label = "Home",
                    selected = currentTab == Screen.HOME,
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    onClick = { onTabSelected(Screen.HOME) },
                    testTag = "nav_tab_home"
                )

                // 2. RECORD
                BottomNavItem(
                    label = "Record",
                    selected = currentTab == Screen.RECORD || currentTab == Screen.NEW_RECORDING,
                    selectedIcon = Icons.Filled.Videocam,
                    unselectedIcon = Icons.Outlined.Videocam,
                    isRecordTab = true,
                    onClick = { onTabSelected(Screen.RECORD) },
                    testTag = "nav_tab_record"
                )

                // 3. PROJECTS
                BottomNavItem(
                    label = "Projects",
                    selected = currentTab == Screen.PROJECTS || currentTab == Screen.LIBRARY,
                    selectedIcon = Icons.Filled.VideoLibrary,
                    unselectedIcon = Icons.Outlined.VideoLibrary,
                    onClick = { onTabSelected(Screen.PROJECTS) },
                    testTag = "nav_tab_projects"
                )

                // 4. EDITOR
                BottomNavItem(
                    label = "Editor",
                    selected = currentTab == Screen.EDITOR,
                    selectedIcon = Icons.Filled.Edit,
                    unselectedIcon = Icons.Outlined.Edit,
                    onClick = { onTabSelected(Screen.EDITOR) },
                    testTag = "nav_tab_editor"
                )

                // 5. SETTINGS
                BottomNavItem(
                    label = "Settings",
                    selected = currentTab == Screen.SETTINGS,
                    selectedIcon = Icons.Filled.Settings,
                    unselectedIcon = Icons.Outlined.Settings,
                    onClick = { onTabSelected(Screen.SETTINGS) },
                    testTag = "nav_tab_settings"
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    selected: Boolean,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isRecordTab: Boolean = false,
    onClick: () -> Unit,
    testTag: String
) {
    val interactionSource = remember { MutableInteractionSource() }

    val activeColor = if (isRecordTab) AccentRed else MaterialTheme.colorScheme.primary
    val tint by animateColorAsState(
        targetValue = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "tab_tint"
    )

    val pillBg by animateColorAsState(
        targetValue = if (selected) activeColor.copy(alpha = 0.12f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "pill_bg"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(pillBg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            if (isRecordTab && !selected) {
                // Subtle red recording dot to signify the core function
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(AccentRed)
                        .align(Alignment.TopEnd)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = tint,
            maxLines = 1
        )
    }
}
