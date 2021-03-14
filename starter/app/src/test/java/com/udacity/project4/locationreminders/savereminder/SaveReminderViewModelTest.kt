package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.udacity.project4.R
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SaveReminderViewModelTest {

    // Use a fake repository to be injected into the viewmodel
    private lateinit var reminderDataSource: FakeDataSource

    // Subject under test
    private lateinit var saveReminderViewModel: SaveReminderViewModel

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
        saveReminderViewModel = SaveReminderViewModel(reminderDataSource)
    }

    @Test
    fun saveValidReminder_setsNavigationEvent() {
        // When adding a new reminder
        saveReminderViewModel.validateAndSaveReminder(
            ReminderDataItem("title4", "desc4", "location4", 7.0, 8.0)
        )
        // Then the navigation event is triggered
        assertEquals(saveReminderViewModel.navigationCommand.getOrAwaitValue(), NavigationCommand.Back)
    }

    @Test
    fun inputInvalidTitleReminder_setsSnackbarEvent() {
        // When adding a new reminder
        saveReminderViewModel.validateAndSaveReminder(
            ReminderDataItem("", "desc4", "location4", 7.0, 8.0)
        )
        // Then the snackbar event is triggered
        assertEquals(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), R.string.err_enter_title)
    }

    @Test
    fun inputInvalidLocationReminder_setsSnackbarEvent() {
        // When adding a new reminder
        saveReminderViewModel.validateAndSaveReminder(
            ReminderDataItem("title4", "desc4", "", 7.0, 8.0)
        )
        // Then the snackbar event is triggered
        assertEquals(saveReminderViewModel.showSnackBarInt.getOrAwaitValue(), R.string.err_select_location)
    }
}