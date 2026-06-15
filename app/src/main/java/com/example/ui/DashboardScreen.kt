package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.getVectorIconForName
import com.example.data.ActionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val actions by viewModel.actionsFlow.collectAsState()
    val blocklist by viewModel.blocklistedPackages.collectAsState()
    val isAccessibilityActive by viewModel.isAccessibilityEnabled.collectAsState()
    val isSidebarActive by viewModel.isSidebarActive.collectAsState()

    var isNavigatedToForm by remember { mutableStateOf(false) }
    var editingAction by remember { mutableStateOf<ActionEntity?>(null) }
    var isNavigatedToSettings by remember { mutableStateOf(false) }

    if (isNavigatedToSettings) {
        SidebarSettingsScreen(
            onBack = {
                isNavigatedToSettings = false
            }
        )
        return
    }

    if (isNavigatedToForm) {
        CreateEditActionScreen(
            actionToEdit = editingAction,
            onCancel = {
                isNavigatedToForm = false
                editingAction = null
            },
            onSave = { title, systemPrompt, iconName ->
                if (editingAction == null) {
                    viewModel.addNewAction(title, systemPrompt, iconName)
                    Toast.makeText(context, "Added custom action workflow!", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.updateAction(
                        editingAction!!.copy(
                            title = title,
                            systemPrompt = systemPrompt,
                            icon = iconName
                        )
                    )
                    Toast.makeText(context, "Updated custom action workflow!", Toast.LENGTH_SHORT).show()
                }
                isNavigatedToForm = false
                editingAction = null
            }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Arc Sparkle",
                            tint = Color(0xFF3B82F6), // Sophisticated Blue
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ARC CLONE CONTROL",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { isNavigatedToSettings = true },
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Sidebar Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0A0A0A)
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A0A0A),
                            Color(0xFF151515)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Card explaining setup in visual harmony with HTML
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1C1C1E)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth().testTag("welcome_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "TECHNOLOGY TODAY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Color(0xFF60A5FA) // Ice blue accent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "The Future of On-Device Intelligence",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 28.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "A proactive overlay system that traverses view hierarchies dynamically, streaming structured layout summaries to Gemini 3.5-Flash to execute tasks inside any application in volatile memory.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFF94A3B8) // Slate 400
                        )
                    }
                }
            }

            // Technical system health status (Permissions Panel)
            item {
                Text(
                    text = "SYSTEM HEALTH & CONFIGURATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF94A3B8) // Slate 400
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Accessibility Permission card
                    PermissionCard(
                        title = "Accessibility Screen Parser",
                        description = "Required to traverse the active app visual state dynamically without saving or caching data.",
                        isActive = isAccessibilityActive,
                        onToggle = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                                Toast.makeText(context, "Locate 'Arc Clone' in downloaded services and turn it ON.", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open settings: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        tag = "accessibility_card"
                    )

                    // Overlay Drawer Permission card
                    PermissionCard(
                        title = "Draw Over Other Apps",
                        description = "Needed to render the floating circular handle and streamed response sheets globally.",
                        isActive = Settings.canDrawOverlays(context),
                        onToggle = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not launch overlays panel.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        tag = "overlay_card"
                    )

                    // Control Board to Launch Service manually if conditions satisfied
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Floating Handle Service",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isSidebarActive) "Running over background grids" else "Inactive backdrop",
                                        fontSize = 12.sp,
                                        color = if (isSidebarActive) Color(0xFF10B981) else Color(0xFF94A3B8)
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (!Settings.canDrawOverlays(context)) {
                                            Toast.makeText(context, "Please allow overlay drawing first.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }

                                        val intent = Intent(context, com.example.service.FloatingSidebarService::class.java)
                                        if (isSidebarActive) {
                                            context.stopService(intent)
                                            Toast.makeText(context, "Arc floating handle closed.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            context.startService(intent)
                                            Toast.makeText(context, "Arc floating handle activated!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSidebarActive) Color(0xFFEF4444) else Color(0xFF3B82F6)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("toggle_service_button")
                                ) {
                                    Text(
                                        text = if (isSidebarActive) "Close Overlay" else "Launch Overlay",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Prompts Editor Board
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DYNAMIC CUSTOM PROMPTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF94A3B8) // Slate 400
                    )

                    IconButton(
                        onClick = {
                            editingAction = null
                            isNavigatedToForm = true
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C1C1E))
                            .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)), CircleShape)
                            .testTag("add_prompt_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New prompt",
                            tint = Color(0xFF3B82F6), // Sophisticated Blue
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Display of actions
            if (actions.isEmpty()) {
                item {
                    Text(
                        text = "Loading initial trigger prompts...",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        color = Color.Gray
                    )
                }
            } else {
                items(actions) { action ->
                    PromptActionRow(
                        action = action,
                        onEdit = {
                            editingAction = action
                            isNavigatedToForm = true
                        },
                        onDelete = {
                            viewModel.deleteAction(action.id)
                            Toast.makeText(context, "Deleted prompt action.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Security Shield & Blocklist Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "PRIVACY SECURITY SHIELD (APP BLOCKLIST)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF94A3B8) // Slate 400
                )
            }

            item {
                BlocklistPanel(
                    blockedPackages = blocklist,
                    onAdd = { pkg ->
                        viewModel.addBlocklistPackage(pkg)
                        Toast.makeText(context, "Added $pkg to secure blocklist.", Toast.LENGTH_SHORT).show()
                    },
                    onRemove = { pkg ->
                        viewModel.removeBlocklistPackage(pkg)
                        Toast.makeText(context, "Removed $pkg from secure blocklist.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isActive: Boolean,
    onToggle: () -> Unit,
    tag: String
) {
    val indicatorColor by animateColorAsState(
        targetValue = if (isActive) Color(0xFF10B981) else Color(0xFFEF4444)
    )

    Card(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8), // Slate 400
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onToggle,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isActive) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFF3B82F6).copy(alpha = 0.5f)),
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = if (isActive) "Active" else "Enable",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color(0xFF10B981) else Color(0xFF3B82F6)
                )
            }
        }
    }
}

@Composable
fun PromptActionRow(
    action: ActionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("action_entity_${action.id}"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1C1C1E)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2C2E)),
                    contentAlignment = Alignment.Center
                ) {
                    if (action.icon.startsWith("text:")) {
                        val txt = action.icon.substringAfter("text:")
                        Text(
                            text = txt.take(3),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Icon(
                            imageVector = getVectorIconForName(action.icon),
                            contentDescription = "Action prompt icon",
                            tint = Color(0xFF60A5FA), // Light blue tint
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = action.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        if (action.id.startsWith("default_")) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF3B82F6).copy(alpha = 0.15f)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "System",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF60A5FA),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = action.systemPrompt,
                        maxLines = 2,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8), // Slate 400
                        lineHeight = 14.sp
                    )
                }
            }

            // Only permit deletion/edit of user custom added prompt actions
            if (!action.id.startsWith("default_")) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_prompt_button_${action.id}")) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit prompt action",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_prompt_button_${action.id}")) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove prompt action",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlocklistPanel(
    blockedPackages: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var textValue by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("blocklist_panel"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Add to Blocklist (e.g., com.example.mybank)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    placeholder = { Text("package.name", fontSize = 12.sp, color = Color.Gray) },
                    modifier = Modifier.weight(1f).testTag("blocklist_input"),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF0A0A0A),
                        unfocusedContainerColor = Color(0xFF0A0A0A),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (textValue.isNotBlank()) {
                            onAdd(textValue)
                            textValue = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("blocklist_add_button")
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Currently Blocked Apps (${blockedPackages.size})",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8), // Slate 400
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                blockedPackages.forEach { pkg ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2C1C1C)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { onRemove(pkg) },
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = pkg,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFCA5A5)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove block",
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// A simple implementation of FlowRow for chips layout when Android compose layout flowrow is not loaded
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = verticalArrangement) {
        Row(horizontalArrangement = horizontalArrangement, modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePromptDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("summarize") }

    val iconOptions = listOf("summarize", "hearing", "analytics", "edit", "fact_check")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && prompt.isNotBlank()) {
                        onSave(title, prompt, selectedIcon)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("save_prompt_button")
            ) {
                Text("Save Workflow", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        title = {
            Text(
                "Design Custom Prompt Workflow",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Workflow Name (e.g., Code Explainer)", color = Color(0xFF94A3B8)) },
                    modifier = Modifier.fillMaxWidth().testTag("input_prompt_title"),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                    )
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Gemini System Instructions", color = Color(0xFF94A3B8)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("input_prompt_system"),
                    maxLines = 4,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                    )
                )

                Text(
                    text = "Pick Workflow Symbol",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    iconOptions.forEach { iconName ->
                        val isSelected = selectedIcon == iconName
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(0xFF3B82F6) else Color(0xFF1C1C1E)
                                )
                                .border(androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.08f)), CircleShape)
                                .clickable { selectedIcon = iconName },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getVectorIconForName(iconName),
                                contentDescription = iconName,
                                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}
