package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class ArcAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "ArcAccessibility"
        @Volatile
        var instance: ArcAccessibilityService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        OverlayCoordinator.isAccessibilityActive.value = true
        Log.i(TAG, "ArcAccessibilityService created.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        OverlayCoordinator.isAccessibilityActive.value = true
        Log.i(TAG, "ArcAccessibilityService connected and ready to parse screens.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Monitor package changes to enforce deep user privacy blocklisting
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = event.packageName?.toString() ?: ""
            if (pkgName.isNotBlank() && pkgName != "com.android.systemui") {
                OverlayCoordinator.activePackageName.value = pkgName
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "ArcAccessibilityService interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        OverlayCoordinator.isAccessibilityActive.value = false
        Log.i(TAG, "ArcAccessibilityService destroyed.")
    }

    /**
     * Traverses the active application's window node hierarchy on-demand
     * and extracts formatted Markdown matching the active view.
     */
    fun extractActiveScreenText(): String {
        val rootNode = rootInActiveWindow
            ?: return "No on-screen text could be acquired. Please verify that overlay and access permissions are configured."

        val builder = StringBuilder()
        builder.append("### Screen Context Summary\n\n")

        val traversedCount = mutableMapOf<String, Int>()
        extractTextFromNode(rootNode, builder, traversedCount)
        rootNode.recycle()

        val results = builder.toString().trim()
        return if (results == "### Screen Context Summary") {
            "Empty Screen State: No readable alphanumeric strings found in standard layout nodes."
        } else {
            results
        }
    }

    private fun extractTextFromNode(
        node: AccessibilityNodeInfo?,
        builder: StringBuilder,
        traversedCount: MutableMap<String, Int>,
        depth: Int = 0
    ) {
        if (node == null) return
        if (!node.isVisibleToUser) return

        val pkgName = node.packageName?.toString() ?: ""
        // Skip status bars, navigation bars, system keyboards
        if (pkgName.contains("com.android.systemui") || pkgName.contains("inputmethod")) {
            return
        }

        val text = node.text?.toString()?.trim()
        val contentDesc = node.contentDescription?.toString()?.trim()
        val className = node.className?.toString() ?: ""
        val widgetType = className.substringAfterLast('.')

        // Filter and compile text safely into visual Markdown containers
        if (!text.isNullOrBlank()) {
            // Deduplicate repeating identical structural elements gracefully
            val identifier = "$packageName|$text"
            val count = traversedCount[identifier] ?: 0
            if (count < 2) {
                traversedCount[identifier] = count + 1

                when {
                    widgetType.contains("Button", ignoreCase = true) -> {
                        builder.append(" [Button: $text] ")
                    }
                    widgetType.contains("EditText", ignoreCase = true) -> {
                        builder.append(" [Input: (user edits values list)] ")
                    }
                    else -> {
                        builder.append(text).append("\n")
                    }
                }
            }
        } else if (!contentDesc.isNullOrBlank()) {
            val identifier = "$packageName|$contentDesc"
            val count = traversedCount[identifier] ?: 0
            if (count < 1) {
                traversedCount[identifier] = count + 1
                // Add icons with visual references
                builder.append(" [icon: $contentDesc] ")
            }
        }

        // Depth restriction to prevent deep recursive cycles or memory exhaustions
        if (depth < 50) {
            val childCount = node.childCount
            for (i in 0 until childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    extractTextFromNode(child, builder, traversedCount, depth + 1)
                    child.recycle()
                }
            }
        }
    }
}
