package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment.Companion.ACTION_GEOFENCE_EVENT
import com.udacity.project4.utils.errorMessage
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_GEOFENCE_EVENT) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            if (geofencingEvent.hasError()) {
                val errorMessage = errorMessage(context, geofencingEvent.errorCode)
                Log.e(TAG, errorMessage)
                return
            }

            // Get the transition type.
            val geofenceTransition = geofencingEvent.geofenceTransition

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                Log.v(TAG, context.getString(R.string.geofence_entered))
                val fenceId = when {
                    geofencingEvent.triggeringGeofences.isNotEmpty() ->
                        geofencingEvent.triggeringGeofences[0].requestId
                    else -> {
                        Log.e(TAG, "No Geofence Trigger Found!")
                        return
                    }
                }
                // Send notification and log the transition details.
                notify(context, fenceId)
                Log.i(TAG, "Geofence triggered: $fenceId")
            } else {
                Log.e(
                    TAG,
                    context.getString(R.string.geofence_transition_invalid_type, geofenceTransition)
                )
            }
        }
    }

    private fun notify(context: Context, id: String) {
        val dataSource: ReminderDataSource =
            RemindersLocalRepository(LocalDB.createRemindersDao(context))

        GlobalScope.launch {
            val itemResult = dataSource.getReminder(id)
            if (itemResult is Result.Success) {
                val reminderDto = itemResult.data
                val reminderDataItem = ReminderDataItem(
                    id = reminderDto.id,
                    title = reminderDto.title,
                    description = reminderDto.description,
                    location = reminderDto.location,
                    latitude = reminderDto.latitude,
                    longitude = reminderDto.longitude
                )
                sendNotification(context, reminderDataItem)
            }
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}