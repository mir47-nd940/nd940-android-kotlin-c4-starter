package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDB() {
        // Using an in-memory database so that the information stored here
        // disappears when the process is killed.
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        // GIVEN - Insert a reminder.
        val rem = ReminderDTO(
            title = "title",
            description = "desc",
            location = "location",
            latitude = 0.0,
            longitude = 0.0
        )
        database.reminderDao().saveReminder(rem)

        // WHEN - Get the reminder by id from the database.
        val loaded = database.reminderDao().getReminderById(rem.id)

        // THEN - The loaded data contains the expected values.
        assertThat<ReminderDTO>(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(rem.id))
        assertThat(loaded.title, `is`(rem.title))
        assertThat(loaded.description, `is`(rem.description))
        assertThat(loaded.location, `is`(rem.location))
        assertThat(loaded.latitude, `is`(rem.latitude))
        assertThat(loaded.longitude, `is`(rem.longitude))
    }

    @Test
    fun deleteReminder() = runBlockingTest {
        // GIVEN - Insert a reminder.
        val rem = ReminderDTO(
            title = "title",
            description = "desc",
            location = "location",
            latitude = 0.0,
            longitude = 0.0
        )
        database.reminderDao().saveReminder(rem)

        // When the database is cleared
        database.reminderDao().deleteAllReminders()

        // THEN - The loaded data contains the expected values
        val loaded = database.reminderDao().getReminders()
        assertTrue(loaded.isEmpty())
    }
}