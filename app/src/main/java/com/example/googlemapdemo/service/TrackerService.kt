package com.example.googlemapdemo.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.googlemapdemo.util.Constants.ACTION_SERVICE_START
import com.example.googlemapdemo.util.Constants.ACTION_SERVICE_STOP
import com.example.googlemapdemo.util.Constants.LOCATION_FASTEST_UPDATE_INTERVAL
import com.example.googlemapdemo.util.Constants.LOCATION_UPDATE_INTERVAL
import com.example.googlemapdemo.util.Constants.NOTIFICATION_CHANEL_ID
import com.example.googlemapdemo.util.Constants.NOTIFICATION_CHANEL_NAME
import com.example.googlemapdemo.util.Constants.NOTIFICATION_ID
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackerService:LifecycleService() {
    @Inject
    lateinit var notification: NotificationCompat.Builder
    @Inject
    lateinit var notificationManager: NotificationManager

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object{
        val started = MutableLiveData<Boolean>()

        val locationList = MutableLiveData<MutableList<LatLng>>()
    }

    private val locationCallback = object : LocationCallback(){
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result?.locations?.let{ locations ->
                for(location in locations){
                    updateLocationList(location)
                }

            }
        }
    }
    private fun setInitialValues(){
        started.postValue(false)
        locationList.postValue(mutableListOf())
    }
    private fun updateLocationList(location: Location){
        val newLatLng = LatLng(location.latitude, location.longitude)
        locationList.value?.apply {
            add(newLatLng)
            locationList.postValue(this)
        }
    }
    override fun onCreate() {
        setInitialValues()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when(it.action){
                ACTION_SERVICE_START ->{
                    started.postValue(true)
                }
                ACTION_SERVICE_STOP ->{
                    started.postValue(false)
                }
                else -> {}
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService(){
        createNotificationChanel()
        startForeground(
            NOTIFICATION_ID,notification.build()
        )
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(){
        val locationRequest = LocationRequest().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_FASTEST_UPDATE_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
    private fun createNotificationChanel(){
        var channel = NotificationChannel(NOTIFICATION_CHANEL_ID, NOTIFICATION_CHANEL_NAME, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }
}