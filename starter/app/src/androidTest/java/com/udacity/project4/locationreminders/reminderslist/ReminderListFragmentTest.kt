package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.Mockito
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4ClassRunner::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderListFragmentTest : KoinTest {

    private lateinit var fakeDataSource: FakeAndroidDataSource

    lateinit var mockModule: Module

    @Before
    fun setup() {
        fakeDataSource = FakeAndroidDataSource()
        mockModule = module {
            single(override = true) { fakeDataSource }
        }
        loadKoinModules(mockModule)
    }

    @After
    fun tearDown() {
        unloadKoinModules(mockModule)
    }

    @Test
    fun clickAdd_navigateToSaveReminderFragment() = runBlockingTest {
        fakeDataSource.saveReminder(ReminderDTO("title1", "desc1", "location1", 1.0, 2.0))

        // GIVEN - On the reminders screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment { Navigation.setViewNavController(it.view!!, navController) }

        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))

        // WHEN - Click on the add reminder FAB
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN - Verify navigation to the save reminder screen
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }
}