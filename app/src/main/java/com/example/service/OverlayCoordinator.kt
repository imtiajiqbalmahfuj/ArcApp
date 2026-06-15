package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow

object OverlayCoordinator {
    // Shared live states to synchronize overlays, services, and dashboards
    val activePackageName = MutableStateFlow<String>("")
    val isAccessibilityActive = MutableStateFlow<Boolean>(false)
    val isOverlayServiceActive = MutableStateFlow<Boolean>(false)

    // Streaming AI answers to display inside the floating screen overlay card
    val aiResponseText = MutableStateFlow<String>("")
    val isProcessing = MutableStateFlow<Boolean>(false)
    val currentTriggeredActionTitle = MutableStateFlow<String>("")

    fun resetAIState() {
        aiResponseText.value = ""
        isProcessing.value = false
        currentTriggeredActionTitle.value = ""
    }
}
