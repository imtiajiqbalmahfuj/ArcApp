package com.example.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("arc_prefs", Context.MODE_PRIVATE) }

    // Read stored preferences with default values matching the system
    var sidebarType by remember { mutableStateOf(prefs.getString("sidebar_type", "arc") ?: "arc") }
    var sidebarSize by remember { mutableStateOf(prefs.getInt("sidebar_size", 180)) } // 100 to 180 dp
    var sidebarEdgeWidth by remember { mutableStateOf(prefs.getInt("sidebar_edge_width", 8)) } // 6 to 32 dp
    var edgeHeightPercent by remember { mutableStateOf(prefs.getInt("sidebar_edge_height", 25)) } // 10 to 100 %
    var edgeHighlightBrightness by remember { mutableStateOf(prefs.getInt("edge_highlight_brightness", 60)) } // 0 to 100 %

    // Track state locally and persist instantly on changes or on back
    fun persistSettings() {
        prefs.edit().apply {
            putString("sidebar_type", sidebarType)
            putInt("sidebar_size", sidebarSize)
            putInt("sidebar_edge_width", sidebarEdgeWidth)
            putInt("sidebar_edge_height", edgeHeightPercent)
            putInt("edge_highlight_brightness", edgeHighlightBrightness)
            apply()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Sidebar Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            persistSettings()
                            onBack()
                        },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0F0F10)
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
                        colors = listOf(Color(0xFF0A0A0B), Color(0xFF141416))
                    )
                )
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Choice cards
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Option 1: Arc Sidebar Overlay
                    val isArcSelected = sidebarType == "arc"
                    val arcBorderColor = if (isArcSelected) Color(0xFF3B82F6) else Color.Transparent
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E20)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, if (isArcSelected) Color(0xFF3280F0) else Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sidebarType = "arc"
                                persistSettings()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Arc Overlay",
                                    tint = Color(0xFF3B82F6)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Arc Sidebar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Use Arc's floating sidebar overlay that appears on all screens",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 16.sp
                                )
                            }
                            if (isArcSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF3280F0),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Option 2: System Sidebar
                    val isSystemSelected = sidebarType == "system"
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E20)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, if (isSystemSelected) Color(0xFF3280F0) else Color.White.copy(alpha = 0.06f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sidebarType = "system"
                                persistSettings()
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF94A3B8).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhonelinkSetup,
                                    contentDescription = "System Sidebar Integration",
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "System Sidebar",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Add Arc to your phone's native sidebar (Samsung Edge, OnePlus Shelf, etc.)",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    lineHeight = 16.sp
                                )
                            }
                            if (isSystemSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF3280F0),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Card 2: Sidebar Size
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sidebar Size",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF53A1FD)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sidebar Size",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${sidebarSize} dp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF53A1FD)
                        )
                    }

                    Text(
                        text = "Adjust the size of the expanded sidebar. Icons and text will scale accordingly.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 15.sp
                    )

                    Slider(
                        value = sidebarSize.toFloat(),
                        onValueChange = {
                            sidebarSize = it.toInt()
                            persistSettings()
                        },
                        valueRange = 100f..180f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF3B82F6),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                            thumbColor = Color(0xFF3B82F6)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("slider_sidebar_size")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Compact (100 dp)", fontSize = 10.sp, color = Color.Gray)
                        Text("Large (180 dp)", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            // Card 3: Collapsed Sidebar Edge Settings
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Collapsed Sidebar Edge",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF53A1FD)
                    )

                    // Property 1: Sidebar Edge Width
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sidebar Edge Width",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${sidebarEdgeWidth} dp",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF53A1FD)
                            )
                        }

                        Text(
                            text = "Width of the visible edge when sidebar is minimized. Larger values are easier to see and tap.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 15.sp
                        )

                        Slider(
                            value = sidebarEdgeWidth.toFloat(),
                            onValueChange = {
                                sidebarEdgeWidth = it.toInt()
                                persistSettings()
                            },
                            valueRange = 6f..32f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF3B82F6),
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                                thumbColor = Color(0xFF3B82F6)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_edge_width")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Compact (6 dp)", fontSize = 10.sp, color = Color.Gray)
                            Text("Extra Large (32 dp)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    // Property 2: Edge Height
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Edge Height",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${edgeHeightPercent}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF53A1FD)
                            )
                        }

                        Text(
                            text = "Height of the visible edge as a percentage of screen height. Larger values are easier to tap.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 15.sp
                        )

                        Slider(
                            value = edgeHeightPercent.toFloat(),
                            onValueChange = {
                                edgeHeightPercent = it.toInt()
                                persistSettings()
                            },
                            valueRange = 10f..100f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF3B82F6),
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                                thumbColor = Color(0xFF3B82F6)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_edge_height")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Compact (10%)", fontSize = 10.sp, color = Color.Gray)
                            Text("Full Height (100%)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    // Property 3: Edge highlight brightness
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Edge highlight brightness",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${edgeHighlightBrightness}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF53A1FD)
                            )
                        }

                        Text(
                            text = "Outer edge glow against dark wallpapers. Lower is subtler; higher is easier to spot.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 15.sp
                        )

                        Slider(
                            value = edgeHighlightBrightness.toFloat(),
                            onValueChange = {
                                edgeHighlightBrightness = it.toInt()
                                persistSettings()
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF3B82F6),
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                                thumbColor = Color(0xFF3B82F6)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("slider_glow_brightness")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtle (0%)", fontSize = 10.sp, color = Color.Gray)
                            Text("Bright (100%)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

data class SidebarSettings(
    val type: String = "arc",
    val size: Int = 180,
    val edgeWidth: Int = 8,
    val edgeHeight: Int = 25,
    val glowBrightness: Int = 60
)

fun getSidebarSettings(context: Context): SidebarSettings {
    val prefs = context.getSharedPreferences("arc_prefs", Context.MODE_PRIVATE)
    return SidebarSettings(
        type = prefs.getString("sidebar_type", "arc") ?: "arc",
        size = prefs.getInt("sidebar_size", 180),
        edgeWidth = prefs.getInt("sidebar_edge_width", 8),
        edgeHeight = prefs.getInt("sidebar_edge_height", 25),
        glowBrightness = prefs.getInt("edge_highlight_brightness", 60)
    )
}

