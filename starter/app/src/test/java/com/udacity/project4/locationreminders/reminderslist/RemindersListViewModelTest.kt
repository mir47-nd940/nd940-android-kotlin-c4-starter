package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    // Use a fake repository to be injected into the viewmodel
    private lateinit var reminderDataSource: FakeDataSource

    // Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel() {
        // We initialise the reminders to 3
        reminderDataSource = FakeDataSource(
            mutableListOf(
                ReminderDTO("title1", "desc1", "location1", 1.0, 2.0),
                ReminderDTO("title2", "desc2", "location2", 3.0, 4.0),
                ReminderDTO("title3", "desc3", "location3", 5.0, 6.0)
            )
        )
        remindersListViewModel = RemindersListViewModel(reminderDataSource)
    }

    @Test
    fun loadReminders_success() {
        // When reminders are loaded
        remindersListViewModel.loadReminders()

        // Then the list has 3 items
        assertThat(remindersListViewModel.remindersList.getOrAwaitValue().size, `is`(3))
    }

    @Test
    fun loadReminders_error() {
        // Make the data source return error.
        reminderDataSource.setReturnError(true)

        // When reminders are loaded
        remindersListViewModel.loadReminders()

        // Then an error message is triggered.
        assertThat(remindersListViewModel.showErrorMessage.getOrAwaitValue(), `is`("Test error"))
    }

    @Test
    fun loadReminders_loading() {
        // Pause coroutines actions
        mainCoroutineRule.pauseDispatcher()

        // When reminders are loaded
        remindersListViewModel.loadReminders()

        // Then progress indicator is shown
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        // Then progress indicator is hidden
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }
}