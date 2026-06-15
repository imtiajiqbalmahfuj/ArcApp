package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActionEntity
import com.example.data.ActionRepository
import com.example.data.AppDatabase
import com.example.service.BlocklistManager
import com.example.service.OverlayCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ActionRepository(db.actionDao())
    private val blocklistManager = BlocklistManager(application)
    private val prefs = application.getSharedPreferences("arc_prefs", Context.MODE_PRIVATE)

    // Tracks onboarding completion state
    val isOnboardingCompleted = MutableStateFlow(
        prefs.getBoolean("onboarding_completed", false)
    )

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        isOnboardingCompleted.value = true
    }

    // Reactive actions list loaded in real time from Room Database SQLite table
    val actionsFlow: StateFlow<List<ActionEntity>> = repository.allActions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Exposed lists of privacy blocklisted packages
    val blocklistedPackages = MutableStateFlow<List<String>>(emptyList())

    // Tracks if permissions statuses have changed
    val isAccessibilityEnabled = OverlayCoordinator.isAccessibilityActive
    val isSidebarActive = OverlayCoordinator.isOverlayServiceActive

    init {
        refreshBlocklist()
    }

    fun refreshBlocklist() {
        blocklistedPackages.value = blocklistManager.getBlockedPackages().toList().sorted()
    }

    fun addBlocklistPackage(packageName: String) {
        if (packageName.isNotBlank()) {
            val added = blocklistManager.addPackage(packageName.trim())
            if (added) {
                refreshBlocklist()
            }
        }
    }

    fun removeBlocklistPackage(packageName: String) {
        val removed = blocklistManager.removePackage(packageName)
        if (removed) {
            refreshBlocklist()
        }
    }

    fun addNewAction(title: String, systemPrompt: String, icon: String) {
        viewModelScope.launch {
            val action = ActionEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                systemPrompt = systemPrompt.trim(),
                icon = icon,
                isCommunityShared = false
            )
            repository.insertAction(action)
        }
    }

    fun deleteAction(id: String) {
        viewModelScope.launch {
            repository.deleteActionById(id)
        }
    }

    fun updateAction(action: ActionEntity) {
        viewModelScope.launch {
            repository.updateAction(action)
        }
    }
}
