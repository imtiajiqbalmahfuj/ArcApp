package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "ActionTable")
data class ActionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val systemPrompt: String,
    val icon: String, // String representation of icon name, e.g. "summarize", "audio", "extract", etc.
    val isCommunityShared: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
