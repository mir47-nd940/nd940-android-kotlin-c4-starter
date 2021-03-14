package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

// Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeAndroidDataSource(
    var reminders: MutableList<ReminderDTO>? = mutableListOf()
) : ReminderDataSource {

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return reminders?.let {
            Result.Success(it)
        } ?: run {
            Result.Error("Reminders not found")
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders?.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return reminders?.find { it.id == id }?.let {
            Result.Success(it)
        } ?: run {
            Result.Error("Reminder not found")
        }
    }

    override suspend fun deleteAllReminders() {
        reminders?.clear()
    }
}