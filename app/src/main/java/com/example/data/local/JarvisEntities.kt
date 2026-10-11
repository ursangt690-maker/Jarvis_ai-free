package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String = "GENERAL", // GENERAL, SHOPPING, CODE, BRIEFING
    val isPinned: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val details: String = "",
    val category: String = "TASK", // TASK, SHOPPING, ROUTINE, MEETING, BIRTHDAY
    val priority: String = "HIGH", // HIGH, NORMAL, LOW
    val isCompleted: Boolean = false,
    val dueTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val triggerTimeMillis: Long,
    val type: String = "REMINDER", // REMINDER, MEETING, BIRTHDAY, BRIEFING
    val speakAloud: Boolean = true,
    val isTriggered: Boolean = false
)

@Serializable
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetPerDay: Int = 1,
    val currentCount: Int = 0,
    val streakDays: Int = 1,
    val lastUpdatedDay: Long = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
)

@Serializable
@Entity(tableName = "memories")
data class MemoryFactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val keyFact: String,
    val category: String = "PREFERENCE", // PREFERENCE, USER_FACT, CONTEXT, ROUTINE
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "conversation_turns")
data class ConversationTurnEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "user" or "model"
    val message: String,
    val languageCode: String = "en-US",
    val actionTaken: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
