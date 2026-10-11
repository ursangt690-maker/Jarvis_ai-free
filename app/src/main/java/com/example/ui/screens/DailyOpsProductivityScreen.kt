package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.HabitEntity
import com.example.data.local.NoteEntity
import com.example.data.local.ReminderEntity
import com.example.data.local.TaskEntity
import com.example.ui.components.HudFramedCard
import com.example.ui.theme.JarvisOnlineGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyOpsProductivityScreen(
    notes: List<NoteEntity>,
    tasks: List<TaskEntity>,
    reminders: List<ReminderEntity>,
    habits: List<HabitEntity>,
    onAddNote: (title: String, content: String, category: String) -> Unit,
    onDeleteNote: (Int) -> Unit,
    onAddTask: (title: String, details: String, category: String, priority: String) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onAddReminder: (title: String, message: String, minutes: Int, type: String) -> Unit,
    onDeleteReminder: (Int) -> Unit,
    onIncrementHabit: (HabitEntity) -> Unit,
    onAddHabit: (name: String, target: Int) -> Unit,
    onSetSystemAlarm: (hour: Int, minute: Int, label: String) -> Unit,
    onSetSystemTimer: (seconds: Int, label: String) -> Unit,
    onCreateCalendarEvent: (title: String, desc: String) -> Unit,
    onTriggerBriefing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTaskCategory by rememberSaveable { mutableStateOf("ALL") }
    var newTaskTitle by rememberSaveable { mutableStateOf("") }
    var newTaskCat by rememberSaveable { mutableStateOf("TASK") }

    var newNoteTitle by rememberSaveable { mutableStateOf("") }
    var newNoteContent by rememberSaveable { mutableStateOf("") }
    var noteSearchQuery by rememberSaveable { mutableStateOf("") }

    var reminderTitle by rememberSaveable { mutableStateOf("") }
    var reminderMinutes by rememberSaveable { mutableStateOf("15") }
    var reminderType by rememberSaveable { mutableStateOf("REMINDER") }

    var alarmHour by rememberSaveable { mutableStateOf("07") }
    var alarmMinute by rememberSaveable { mutableStateOf("00") }
    var timerSeconds by rememberSaveable { mutableStateOf("120") }

    var newHabitName by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "DAILY LIFE, ROUTINES & PRODUCTIVITY",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // 1. Morning / Evening Briefing & Routines
        HudFramedCard(
            title = "AUTOMATED BRIEFINGS & ROUTINES",
            subtitle = "OFFLINE + AI SYNC"
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onTriggerBriefing("Deliver my complete morning briefing including battery, network, pending tasks, and habits.") },
                    modifier = Modifier.testTag("morning_briefing_button")
                ) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("MORNING BRIEFING")
                }

                OutlinedButton(
                    onClick = { onTriggerBriefing("Give me an evening summary of completed tasks, remaining reminders, and tomorrow's preparation.") }
                ) {
                    Text("EVENING SUMMARY")
                }

                OutlinedButton(
                    onClick = { onCreateCalendarEvent("JARVIS Strategic Review", "Scheduled via JARVIS Daily Ops") }
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ADD CALENDAR EVENT")
                }
            }
        }

        // 2. Alarms, Timers & Scheduled Reminders (Meetings / Birthdays)
        HudFramedCard(
            title = "ALARMS, TIMERS & SCHEDULED REMINDERS",
            subtitle = "${reminders.size} SCHEDULED"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = alarmHour,
                    onValueChange = { alarmHour = it },
                    label = { Text("HH") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = alarmMinute,
                    onValueChange = { alarmMinute = it },
                    label = { Text("MM") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val h = alarmHour.toIntOrNull() ?: 7
                        val m = alarmMinute.toIntOrNull() ?: 0
                        onSetSystemAlarm(h, m, "JARVIS Alarm")
                    },
                    modifier = Modifier.testTag("set_alarm_button")
                ) {
                    Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ALARM")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = timerSeconds,
                    onValueChange = { timerSeconds = it },
                    label = { Text("Timer Seconds") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val s = timerSeconds.toIntOrNull() ?: 60
                        onSetSystemTimer(s, "JARVIS Timer")
                    },
                    modifier = Modifier.testTag("set_timer_button")
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("START TIMER")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add Local Reminder (with Meeting / Birthday tag)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("REMINDER", "MEETING", "BIRTHDAY").forEach { type ->
                    FilterChip(
                        selected = reminderType == type,
                        onClick = { reminderType = type },
                        label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = reminderTitle,
                    onValueChange = { reminderTitle = it },
                    label = { Text("Reminder / Meeting / Birthday Title") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(2f)
                        .testTag("reminder_title_input")
                )
                OutlinedTextField(
                    value = reminderMinutes,
                    onValueChange = { reminderMinutes = it },
                    label = { Text("In Min") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (reminderTitle.isNotBlank()) {
                            onAddReminder(
                                reminderTitle,
                                "$reminderType alert scheduled via JARVIS",
                                reminderMinutes.toIntOrNull() ?: 15,
                                reminderType
                            )
                            reminderTitle = ""
                        }
                    },
                    modifier = Modifier.testTag("add_reminder_button")
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = "Schedule Reminder")
                }
            }

            if (reminders.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.US)
                reminders.forEach { rem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "[${rem.type}] ${rem.title}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Trigger: ${fmt.format(Date(rem.triggerTimeMillis))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onDeleteReminder(rem.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Reminder")
                        }
                    }
                }
            }
        }

        // 3. Tasks, To-Do & Shopping List Manager
        HudFramedCard(
            title = "TASKS, TO-DO & SHOPPING LIST",
            subtitle = "${tasks.count { !it.isCompleted }} ACTIVE"
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("ALL", "TASK", "SHOPPING", "ROUTINE").forEach { cat ->
                    FilterChip(
                        selected = selectedTaskCategory == cat,
                        onClick = { selectedTaskCategory = cat },
                        label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newTaskTitle,
                    onValueChange = { newTaskTitle = it },
                    label = { Text("Add Task or Shopping Item") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_task_input")
                )
                AssistChip(
                    onClick = {
                        newTaskCat = if (newTaskCat == "TASK") "SHOPPING" else "TASK"
                    },
                    label = { Text(newTaskCat, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        if (newTaskCat == "SHOPPING") {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                )
                Button(
                    onClick = {
                        if (newTaskTitle.isNotBlank()) {
                            onAddTask(newTaskTitle, "", newTaskCat, "HIGH")
                            newTaskTitle = ""
                        }
                    },
                    modifier = Modifier.testTag("add_task_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val displayedTasks = tasks.filter {
                selectedTaskCategory == "ALL" || it.category == selectedTaskCategory
            }

            displayedTasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggleTask(task) }) {
                            Icon(
                                imageVector = if (task.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "Toggle Task",
                                tint = if (task.isCompleted) JarvisOnlineGreen else MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                            Text(
                                text = "${task.category} • ${task.priority}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onDeleteTask(task.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Task")
                    }
                }
            }
        }

        // 4. Habit Tracker
        HudFramedCard(
            title = "HABIT & PERFORMANCE TRACKING",
            subtitle = "${habits.size} PROTOCOLS"
        ) {
            habits.forEach { habit ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(habit.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Today: ${habit.currentCount}/${habit.targetPerDay} • Streak: ${habit.streakDays} days",
                            style = MaterialTheme.typography.labelSmall,
                            color = JarvisOnlineGreen
                        )
                    }
                    OutlinedButton(onClick = { onIncrementHabit(habit) }) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+1 LOG")
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newHabitName,
                    onValueChange = { newHabitName = it },
                    label = { Text("New Habit Protocol") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (newHabitName.isNotBlank()) {
                            onAddHabit(newHabitName, 1)
                            newHabitName = ""
                        }
                    }
                ) {
                    Text("ADD")
                }
            }
        }

        // 5. Offline Notes Vault (Create, Save, Search, Read)
        HudFramedCard(
            title = "ENCRYPTED OFFLINE NOTES VAULT",
            subtitle = "${notes.size} SAVED"
        ) {
            OutlinedTextField(
                value = noteSearchQuery,
                onValueChange = { noteSearchQuery = it },
                label = { Text("Search Saved Notes") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newNoteTitle,
                onValueChange = { newNoteTitle = it },
                label = { Text("Note Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = newNoteContent,
                onValueChange = { newNoteContent = it },
                label = { Text("Note Content") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_content_input")
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = {
                    if (newNoteTitle.isNotBlank() || newNoteContent.isNotBlank()) {
                        onAddNote(newNoteTitle, newNoteContent, "GENERAL")
                        newNoteTitle = ""
                        newNoteContent = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_note_button")
            ) {
                Text("SAVE NOTE TO LOCAL STORAGE")
            }

            Spacer(modifier = Modifier.height(8.dp))

            val filteredNotes = notes.filter {
                noteSearchQuery.isBlank() ||
                    it.title.contains(noteSearchQuery, ignoreCase = true) ||
                    it.content.contains(noteSearchQuery, ignoreCase = true)
            }

            filteredNotes.forEach { note ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { onDeleteNote(note.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Note")
                        }
                    }
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
