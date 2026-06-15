package com.example.service

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.api.GeminiClient
import com.example.data.ActionEntity
import com.example.data.ActionRepository
import com.example.data.AppDatabase
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.SidebarSettings
import com.example.ui.getSidebarSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

class FloatingSidebarService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "FloatingSidebarService"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private lateinit var overlayLayoutParams: WindowManager.LayoutParams
    private var dummyLifecycleOwner: DummyLifecycleOwner? = null

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private lateinit var repository: ActionRepository
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var actionFlowJob: Job? = null
    private var blocklistManager: BlocklistManager? = null

    // Local lists matching Room DB custom actions
    private var availableActionsState = mutableStateListOf<ActionEntity>()

    private val isDockedOnLeftState = kotlinx.coroutines.flow.MutableStateFlow(true)
    private lateinit var settingsChangeFlow: kotlinx.coroutines.flow.MutableStateFlow<com.example.ui.SidebarSettings>
    private lateinit var prefListener: android.content.SharedPreferences.OnSharedPreferenceChangeListener

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "FloatingSidebarService onCreate")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        blocklistManager = BlocklistManager(this)

        val prefs = getSharedPreferences("arc_prefs", Context.MODE_PRIVATE)
        settingsChangeFlow = kotlinx.coroutines.flow.MutableStateFlow(getSidebarSettings(this))
        prefListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "sidebar_type" || key == "sidebar_size" || key == "sidebar_edge_width" || key == "sidebar_edge_height" || key == "edge_highlight_brightness") {
                scope.launch {
                    settingsChangeFlow.value = getSidebarSettings(this@FloatingSidebarService)
                }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        // Initialize Room Repo
        val db = AppDatabase.getDatabase(this)
        repository = ActionRepository(db.actionDao())

        // Initialize Text-To-Speech engine safely
        tts = TextToSpeech(this, this)

        // Setup custom lifecycle owners for Compose View running in background service
        dummyLifecycleOwner = DummyLifecycleOwner()

        // Sync actions layout on change
        actionFlowJob = scope.launch {
            repository.allActions.collectLatest { list ->
                availableActionsState.clear()
                availableActionsState.addAll(list)
            }
        }

        OverlayCoordinator.isOverlayServiceActive.value = true

        // Build and display our custom screen overlay
        showFloatingSidebar()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS Language is not supported on this device.")
            } else {
                ttsReady = true
                Log.i(TAG, "TTS successfully initialized.")
            }
        } else {
            Log.e(TAG, "TTS Initialization failed.")
        }
    }

    private fun showFloatingSidebar() {
        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "Draw over other apps permission missing. Stopping.")
            stopSelf()
            return
        }

        // Window parameters configuring non-focusable triggers + draw over everything
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        overlayLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 350
        }

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(dummyLifecycleOwner)
            setViewTreeViewModelStoreOwner(dummyLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(dummyLifecycleOwner)

            setContent {
                MyApplicationTheme {
                    val settings by settingsChangeFlow.collectAsState()
                    val isDockedOnLeft by isDockedOnLeftState.collectAsState()

                    FloatingSidebarOverlayContent(
                        actions = availableActionsState,
                        settings = settings,
                        isDockedOnLeft = isDockedOnLeft,
                        onDrag = { dx, dy ->
                            overlayLayoutParams.x += dx.roundToInt()
                            overlayLayoutParams.y += dy.roundToInt()
                            updateOverlayLayout()
                        },
                        onDragEnd = {
                            snapToNearestEdge()
                        },
                        onTriggerAction = { action ->
                            executeAction(action)
                        },
                        onStopSpeech = {
                            stopSpeech()
                        }
                    )
                }
            }
        }

        windowManager.addView(composeView, overlayLayoutParams)
    }

    private fun updateOverlayLayout() {
        try {
            if (composeView.isAttachedToWindow) {
                windowManager.updateViewLayout(composeView, overlayLayoutParams)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating window layout: ${e.message}")
        }
    }

    private fun snapToNearestEdge() {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        // Determine left or right screen docking orientation based on midpoints coordinate
        val center = screenWidth / 2
        overlayLayoutParams.x = if (overlayLayoutParams.x < center) 0 else screenWidth
        updateOverlayLayout()
    }

    private fun executeAction(action: ActionEntity) {
        val accessibilityService = ArcAccessibilityService.instance
        if (accessibilityService == null) {
            Toast.makeText(
                this,
                "Accessibility Service not active. Please enable in Settings.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Privacy Lock Check: Halt immediately if user has banking or auth focused
        val activePkg = OverlayCoordinator.activePackageName.value
        if (blocklistManager?.isBlocked(activePkg) == true) {
            Toast.makeText(
                this,
                "Security Shield: Screen read locked on blocklisted app ($activePkg).",
                Toast.LENGTH_LONG
            ).show()
            Log.w(TAG, "Content reading blocked on package: $activePkg")
            return
        }

        // Volatile text extraction
        val screenText = accessibilityService.extractActiveScreenText()

        OverlayCoordinator.isProcessing.value = true
        OverlayCoordinator.currentTriggeredActionTitle.value = action.title
        OverlayCoordinator.aiResponseText.value = "Scanning layout components..."

        scope.launch {
            if (action.title == "AI Read") {
                // Generate a TTS optimized audio script first
                OverlayCoordinator.aiResponseText.value = "Composing audio read summary..."
                val responseText = GeminiClient.generateContent(action.systemPrompt, screenText)
                OverlayCoordinator.aiResponseText.value = responseText
                OverlayCoordinator.isProcessing.value = false

                speakOut(responseText)
            } else {
                // Stream other outputs in real-time
                var accumulated = ""
                GeminiClient.generateContentStream(action.systemPrompt, screenText).collect { chunk ->
                    accumulated += chunk
                    OverlayCoordinator.aiResponseText.value = accumulated
                    OverlayCoordinator.isProcessing.value = false
                }
            }
        }
    }

    private fun speakOut(text: String) {
        if (ttsReady && tts != null) {
            stopSpeech()
            // Strip markdown asterisks / hashes for premium fluid speech cadence
            val cleanText = text
                .replace("*", "")
                .replace("#", "")
                .replace("-", "")
                .replace("`", "")
                .trim()
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "arc_tts_id")
        } else {
            Toast.makeText(this, "Text-To-Speech is not fully loaded.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopSpeech() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSpeech()
        tts?.shutdown()
        actionFlowJob?.cancel()

        try {
            val prefs = getSharedPreferences("arc_prefs", Context.MODE_PRIVATE)
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister prefListener")
        }

        try {
            windowManager.removeView(composeView)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove composeView on service destruction")
        }

        dummyLifecycleOwner?.handleOnDestroy()
        OverlayCoordinator.isOverlayServiceActive.value = false
        Log.i(TAG, "FloatingSidebarService destroyed.")
    }
}

// Custom lifecycle and registry class to run a local Jetpack Compose context in an background Service context
class DummyLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)

    init {
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        controller.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = store
    override val savedStateRegistry: SavedStateRegistry = controller.savedStateRegistry

    fun handleOnDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}

// Helper mapper connecting Room text keys to sleek standard Material Symbols
@Composable
fun getVectorIconForName(name: String): ImageVector {
    return when (name.lowercase()) {
        "summarize" -> Icons.Default.Summarize
        "hearing" -> Icons.Default.Hearing
        "analytics" -> Icons.Default.Analytics
        "edit" -> Icons.Default.Edit
        "fact_check" -> Icons.Default.FactCheck
        "explore" -> Icons.Default.Explore
        "brush" -> Icons.Default.Brush
        "terminal" -> Icons.Default.Terminal
        else -> Icons.Default.Bolt
    }
}

@Composable
fun FloatingSidebarOverlayContent(
    actions: List<ActionEntity>,
    settings: SidebarSettings,
    isDockedOnLeft: Boolean,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onTriggerAction: (ActionEntity) -> Unit,
    onStopSpeech: () -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    val displayResponse by OverlayCoordinator.aiResponseText.collectAsState()
    val isProcessing by OverlayCoordinator.isProcessing.collectAsState()
    val activeActionTitle by OverlayCoordinator.currentTriggeredActionTitle.collectAsState()
    val activePkgName by OverlayCoordinator.activePackageName.collectAsState()

    val blocklistManager = BlocklistManager(context)
    val isAppBlocked = blocklistManager.isBlocked(activePkgName)

    var horizontalSwipeAccumulator by remember { mutableStateOf(0f) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp

    val edgeHeightDp = (screenHeightDp * settings.edgeHeight / 100).dp
    val edgeWidthDp = settings.edgeWidth.dp
    val baseAlpha = settings.glowBrightness / 100f

    Row(
        modifier = Modifier
            .wrapContentSize()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val handleLine = @Composable {
            Box(
                modifier = Modifier
                    .width(edgeWidthDp)
                    .height(edgeHeightDp)
                    .testTag("floating_drag_handle")
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isDockedOnLeft) 0.dp else 12.dp,
                            bottomStart = if (isDockedOnLeft) 0.dp else 12.dp,
                            topEnd = if (isDockedOnLeft) 12.dp else 0.dp,
                            bottomEnd = if (isDockedOnLeft) 12.dp else 0.dp
                        )
                    )
                    .background(
                        Brush.verticalGradient(
                            colors = if (isAppBlocked) {
                                listOf(Color(0xFFEF4444).copy(alpha = baseAlpha.coerceAtLeast(0.15f)), Color(0xFF7F1D1D).copy(alpha = baseAlpha.coerceAtLeast(0.15f)))
                            } else {
                                listOf(Color(0xFF3B82F6).copy(alpha = baseAlpha.coerceAtLeast(0.15f)), Color(0xFF1D4ED8).copy(alpha = baseAlpha.coerceAtLeast(0.15f)))
                            }
                        )
                    )
                    .border(
                        androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isAppBlocked) Color(0xFFFCA5A5).copy(alpha = baseAlpha * 0.4f) else Color(0xFF93C5FD).copy(alpha = baseAlpha * 0.4f)
                        ),
                        RoundedCornerShape(
                            topStart = if (isDockedOnLeft) 0.dp else 12.dp,
                            bottomStart = if (isDockedOnLeft) 0.dp else 12.dp,
                            topEnd = if (isDockedOnLeft) 12.dp else 0.dp,
                            bottomEnd = if (isDockedOnLeft) 12.dp else 0.dp
                        )
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                horizontalSwipeAccumulator = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                horizontalSwipeAccumulator += dragAmount.x
                                onDrag(dragAmount.x, dragAmount.y)
                            },
                            onDragEnd = {
                                onDragEnd()
                                val swipeThresholdPx = 30f
                                if (isDockedOnLeft) {
                                    if (horizontalSwipeAccumulator > swipeThresholdPx) {
                                        isExpanded = true
                                    } else if (horizontalSwipeAccumulator < -swipeThresholdPx) {
                                        isExpanded = false
                                    }
                                } else {
                                    if (horizontalSwipeAccumulator < -swipeThresholdPx) {
                                        isExpanded = true
                                    } else if (horizontalSwipeAccumulator > swipeThresholdPx) {
                                        isExpanded = false
                                    }
                                }
                            }
                        )
                    }
                    .clickable {
                        isExpanded = !isExpanded
                    }
            )
        }

        if (isDockedOnLeft) {
            handleLine()
            Spacer(modifier = Modifier.width(6.dp))
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = if (isDockedOnLeft) slideInHorizontally(animationSpec = spring()) + fadeIn() else slideInHorizontally(initialOffsetX = { it }, animationSpec = spring()) + fadeIn(),
            exit = if (isDockedOnLeft) slideOutHorizontally(animationSpec = spring()) + fadeOut() else slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring()) + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .wrapContentSize(),
                verticalAlignment = Alignment.Top
            ) {
                // Sidebar actions grid card
                Card(
                    modifier = Modifier
                        .width(settings.size.dp)
                        .testTag("expanded_sidebar_menu")
                        .wrapContentHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161617)),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "ARC ACTIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFF3B82F6), // Blue title
                            modifier = Modifier
                                .padding(vertical = 6.dp, horizontal = 4.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        Divider(color = Color.White.copy(alpha = 0.08f))

                        if (isAppBlocked) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Screen Read Locked\n(Protected Field)",
                                    fontSize = 12.sp,
                                    color = Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                actions.forEach { action ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                onTriggerAction(action)
                                            }
                                            .padding(vertical = 10.dp, horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (action.icon.startsWith("text:")) {
                                            val txt = action.icon.substringAfter("text:")
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF60A5FA).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = txt.take(2),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF60A5FA),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = getVectorIconForName(action.icon),
                                                contentDescription = action.title,
                                                tint = Color(0xFF60A5FA), // Accent Blue icon info
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = action.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.08f))

                        // Stop Auditory Reads shortcut trigger
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onStopSpeech()
                                    Toast
                                        .makeText(
                                            context,
                                            "Audio reading stopped",
                                            Toast.LENGTH_SHORT
                                        )
                                        .show()
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeOff,
                                contentDescription = "Silence Read",
                                tint = Color(0xFFEF4444).copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Mute Voice Read",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8) // Slate 400
                            )
                        }
                    }
                }

                // Inline, floating results sheet overlay positioned beautifully right next to the menu
                AnimatedVisibility(
                    visible = displayResponse.isNotEmpty(),
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .width(260.dp)
                            .heightIn(max = 300.dp)
                            .testTag("ai_response_sheet"),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131314)),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        elevation = CardDefaults.cardElevation(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981)) // Animated/Active status pulse dot equivalent
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = activeActionTitle.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = Color(0xFF60A5FA),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row {
                                    val clipboardManager = LocalContext.current.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    IconButton(
                                        onClick = {
                                            val clip = ClipData.newPlainText("Arc Assistant Result", displayResponse)
                                            clipboardManager.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied text content to clipboard!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy text",
                                            tint = Color(0xFF60A5FA),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = {
                                            OverlayCoordinator.resetAIState()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Dismiss pane",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (isProcessing) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = Color(0xFF3B82F6),
                                        strokeWidth = 3.dp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    item {
                                        Text(
                                            text = displayResponse,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.SansSerif,
                                            lineHeight = 18.sp,
                                            color = Color(0xFFE2E8F0) // Slate 200
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isDockedOnLeft) {
            Spacer(modifier = Modifier.width(6.dp))
            handleLine()
        }
    }
}
