package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ActionEntity
import com.example.service.getVectorIconForName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditActionScreen(
    actionToEdit: ActionEntity?,
    onCancel: () -> Unit,
    onSave: (title: String, systemPrompt: String, icon: String) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = actionToEdit != null

    var title by remember { mutableStateOf(actionToEdit?.title ?: "") }
    var systemPrompt by remember { mutableStateOf(actionToEdit?.systemPrompt ?: "") }
    
    // We can support predefined icons or simple text-based initials icon (e.g. Emoji or 2 Letters text prefix)
    var selectedIconType by remember { 
        mutableStateOf(
            if (actionToEdit != null && actionToEdit.icon.startsWith("text:")) "text" else "predefined"
        ) 
    }
    
    var selectedIconName by remember { 
        mutableStateOf(
            if (isEditMode && !actionToEdit!!.icon.startsWith("text:")) actionToEdit.icon else "summarize" 
        ) 
    }
    
    var customTextIcon by remember { 
        mutableStateOf(
            if (isEditMode && actionToEdit!!.icon.startsWith("text:")) {
                actionToEdit.icon.substringAfter("text:")
            } else "AI"
        ) 
    }

    // Validation State
    var titleError by remember { mutableStateOf<String?>(null) }
    var promptError by remember { mutableStateOf<String?>(null) }
    var textIconError by remember { mutableStateOf<String?>(null) }

    val predefinedIcons = listOf(
        "summarize", "hearing", "analytics", "edit",
        "fact_check", "explore", "brush", "terminal"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "EDIT CUSTOM WORKFLOW" else "CREATE NEW WORKFLOW",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel, modifier = Modifier.testTag("action_cancel_back_button")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate Back",
                            tint = Color(0xFF3B82F6)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0A0A0A)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0A0A0A), Color(0xFF151515))
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Screen Title Section explaining purpose
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.IntegrationInstructions,
                            contentDescription = "Instructions",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "WORKFLOW BUILDER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = Color(0xFF60A5FA)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Customize the cognitive engine. Define what instructions are fed into the LLM when this workflow is executed under the floating overlay menu.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Input: Workflow Name
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "WORKFLOW NAME *",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (title.isNotBlank()) titleError = null
                    },
                    placeholder = { Text("e.g., Code Explainer, Email Polish", color = Color.Gray, fontSize = 13.sp) },
                    isError = titleError != null,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        errorBorderColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_prompt_title")
                )
                
                if (titleError != null) {
                    Text(
                        text = titleError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Input: LLM Instructions System Prompt
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "GEMINI SYSTEM INSTRUCTIONS *",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )
                
                OutlinedTextField(
                    value = systemPrompt,
                    onValueChange = {
                        systemPrompt = it
                        if (systemPrompt.isNotBlank()) promptError = null
                    },
                    placeholder = { 
                        Text(
                            text = "Instruct Gemini how to interpret, parse, and analyze the on-screen components. e.g., 'Summarize in bullet points...', 'Translate this conversation to French...'", 
                            color = Color.Gray, 
                            fontSize = 13.sp
                        ) 
                    },
                    isError = promptError != null,
                    minLines = 4,
                    maxLines = 10,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp, lineHeight = 18.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1C1C1E),
                        unfocusedContainerColor = Color(0xFF1C1C1E),
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        errorBorderColor = Color(0xFFEF4444)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_prompt_system")
                )
                
                if (promptError != null) {
                    Text(
                        text = promptError!!,
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Icon Customizer Selector
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "CHOOSE VISUAL COGNITIVE ICON",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White
                )

                // Tab style selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1C1C1E))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedIconType == "predefined") Color(0xFF3B82F6) else Color.Transparent)
                            .clickable { selectedIconType = "predefined" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Predefined Symbol",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedIconType == "predefined") Color.White else Color(0xFF94A3B8)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedIconType == "text") Color(0xFF3B82F6) else Color.Transparent)
                            .clickable { selectedIconType = "text" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Text Initials / Emoji",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedIconType == "text") Color.White else Color(0xFF94A3B8)
                        )
                    }
                }

                if (selectedIconType == "predefined") {
                    // Predefined Icon Grid in card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            predefinedIcons.forEach { iconName ->
                                val isSelected = selectedIconName == iconName
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) Color(0xFF3B82F6) else Color(0xFF0A0A0A)
                                        )
                                        .border(
                                            androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                if (isSelected) Color.Transparent else Color.White.copy(alpha = 0.08f)
                                            ),
                                            CircleShape
                                        )
                                        .clickable { selectedIconName = iconName }
                                        .testTag("icon_option_$iconName"),
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
                } else {
                    // Custom Text/Emoji Input
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // High aesthetic live text preview
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
                                    .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3B82F6)), CircleShape),
                                    contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (customTextIcon.isNotBlank()) customTextIcon.take(3) else "?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF60A5FA),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = customTextIcon,
                                    onValueChange = {
                                        customTextIcon = it
                                        if (customTextIcon.isNotBlank()) textIconError = null
                                    },
                                    placeholder = { Text("AI, CL, ⚡, 📖", color = Color.Gray, fontSize = 13.sp) },
                                    isError = textIconError != null,
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0A0A0A),
                                        unfocusedContainerColor = Color(0xFF0A0A0A),
                                        focusedBorderColor = Color(0xFF3B82F6),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("input_custom_text_icon")
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Enter up to 3 initials or an emoji representing your smart action.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                if (textIconError != null) {
                                    Text(
                                        text = textIconError!!,
                                        color = Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Save & Cancel
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("cancel_prompt_button")
                ) {
                    Text(text = "CANCEL", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }

                Button(
                    onClick = {
                        var isValid = true
                        if (title.isBlank()) {
                            titleError = "Title is required and cannot be empty."
                            isValid = false
                        }
                        if (systemPrompt.isBlank()) {
                            promptError = "Instructions are required to route Gemini."
                            isValid = false
                        }
                        if (selectedIconType == "text" && customTextIcon.isBlank()) {
                            textIconError = "Please specify a text shortcut or emoji"
                            isValid = false
                        }

                        if (isValid) {
                            val finalIcon = if (selectedIconType == "text") {
                                "text:$customTextIcon"
                            } else {
                                selectedIconName
                            }
                            onSave(title, systemPrompt, finalIcon)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("save_prompt_button")
                ) {
                    Text(
                        text = if (isEditMode) "UPDATE ACTION" else "CREATE ACTION",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
