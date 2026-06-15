package com.example.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class ActionRepository(private val actionDao: ActionDao) {

    val allActions: Flow<List<ActionEntity>> = actionDao.getAllActionsFlow()
        .onStart {
            seedInitialActionsIfNeeded()
        }

    suspend fun insertAction(action: ActionEntity) = withContext(Dispatchers.IO) {
        actionDao.insertAction(action)
    }

    suspend fun deleteActionById(id: String) = withContext(Dispatchers.IO) {
        actionDao.deleteActionById(id)
    }

    suspend fun updateAction(action: ActionEntity) = withContext(Dispatchers.IO) {
        actionDao.updateAction(action)
    }

    private suspend fun seedInitialActionsIfNeeded() = withContext(Dispatchers.IO) {
        val existing = actionDao.getAllActions()
        if (existing.isEmpty()) {
            val defaults = listOf(
                ActionEntity(
                    id = "default_summary",
                    title = "AI Summary",
                    systemPrompt = "Summarize the following on-screen content clearly, highlighting key takeaways and main arguments.",
                    icon = "summarize",
                    isCommunityShared = true
                ),
                ActionEntity(
                    id = "default_read",
                    title = "AI Read",
                    systemPrompt = "Create a highly concise, natural-sounding audio script summary of this content, optimized for text-to-speech reading.",
                    icon = "hearing",
                    isCommunityShared = true
                ),
                ActionEntity(
                    id = "default_extract",
                    title = "Smart Extract",
                    systemPrompt = "Identify and extract all actionable data fields (names, phone numbers, email addresses, physical locations, URLs, or dates) from the text. Format them into a structured JSON block.",
                    icon = "analytics",
                    isCommunityShared = true
                ),
                ActionEntity(
                    id = "default_writer",
                    title = "AI Writer",
                    systemPrompt = "You are a professional editor. Rewrite on-screen text to make it more polished, engaging, and clear. Maintain a helpful and polite tone.",
                    icon = "edit",
                    isCommunityShared = false
                ),
                ActionEntity(
                    id = "default_fact_check",
                    title = "Fact Check",
                    systemPrompt = "Analyze the text and check for any potential factual claims or statements. Summarize what is true, what is questionable, and why.",
                    icon = "fact_check",
                    isCommunityShared = false
                )
            )
            actionDao.insertActions(defaults)
        }
    }
}
