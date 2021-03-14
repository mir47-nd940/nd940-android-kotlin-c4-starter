package com.udacity.project4.locationreminders.reminderslist

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.authentication.LoginViewModel
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentRemindersBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.utils.GeofencingConstants
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.setTitle
import com.udacity.project4.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel

class ReminderListFragment : BaseFragment() {
    //use Koin to retrieve the ViewModel instance
    override val _viewModel: RemindersListViewModel by viewModel()

    private val loginViewModel by viewModels<LoginViewModel>()

    private lateinit var binding: FragmentRemindersBinding

    private lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_reminders, container, false
            )
        binding.viewModel = _viewModel

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(false)
        setTitle(getString(R.string.app_name))

        binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAuthenticationState()
        binding.lifecycleOwner = this
        setupRecyclerView()
        binding.addReminderFAB.setOnClickListener {
            navigateToAddReminder()
        }

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        _viewModel.remindersList.observe(viewLifecycleOwner) {
            checkDeviceLocationSettingsAndStartGeofence(it)
        }
    }

    override fun onResume() {
        super.onResume()
        //load the reminders list on the ui
        _viewModel.loadReminders()
    }

    private fun navigateToAddReminder() {
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(
                ReminderListFragmentDirections.toSaveReminder()
            )
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter {
        }

//        setup the recycler view using the extension function
        binding.reminderssRecyclerView.setup(adapter)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                AuthUI.getInstance().signOut(requireContext())
            }
        }
        return super.onOptionsItemSelected(item)

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
//        display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }

    private fun observeAuthenticationState() {
        loginViewModel.authenticationState.observe(viewLifecycleOwner, Observer<LoginViewModel.AuthenticationState> { authenticationState ->
            when (authenticationState) {
                LoginViewModel.AuthenticationState.AUTHENTICATED -> { }
                else -> {
                    startActivity(Intent(requireActivity(), AuthenticationActivity::class.java))
                    requireActivity().finish()
                }
            }
        })
    }

    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndStartGeofence(
        items: List<ReminderDataItem>,
        resolve: Boolean = true
    ) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireContext())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence(items)
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofences(items)
            }
        }
    }

    /*
     * Adds Geofences. This method should be called after location permission is granted.
     */
    @SuppressLint("MissingPermission")
    private fun addGeofences(items: List<ReminderDataItem>) {
        val geoList = mutableListOf<Geofence>()
        items.forEach { item ->
            val geofence = Geofence.Builder()
                .setRequestId(item.id)
                .setCircularRegion(
                    item.latitude ?: 0.0,
                    item.longitude ?: 0.0,
                    GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
                )
                .setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
            geoList.add(geofence)
        }

        if (geoList.isNotEmpty()) {
            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geoList)

            geofencingClient.addGeofences(geofencingRequest.build(), geofencePendingIntent)?.run {
                addOnSuccessListener {
                    Toast.makeText(requireActivity(), R.string.geofences_added, Toast.LENGTH_SHORT)
                        .show()
                    Log.d(TAG, "Geofences added")
                }
                addOnFailureListener {
                    Toast.makeText(
                        requireActivity(),
                        R.string.geofences_not_added,
                        Toast.LENGTH_SHORT
                    ).show()
                    it.message?.let { msg -> Log.e(TAG, msg) }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ReminderListFragment"
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29

        internal const val ACTION_GEOFENCE_EVENT = "SaveReminderFragment.reminder.action.ACTION_GEOFENCE_EVENT"
    }
}
