package com.example.locationupdateservice

import android.annotation.SuppressLint
import android.app.Service
import android.location.LocationListener
import android.location.LocationManager
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.Nullable
import com.google.android.gms.location.*
import java.util.*

class GoogleService : Service(), LocationListener {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    var isGPSEnable = false
    var isNetworkEnable = false
    var latitude = 0.0
    var longitude = 0.0
    var locationManager: LocationManager? = null
    var location: Location? = null
    private val mHandler = Handler()
    private var mTimer: Timer? = null
    var oneMinuteInterval = 60000
    var notify_interval: Long = 60000
    var intent: Intent? = null
    var locationsList: MutableList<Location> = mutableListOf<Location>()

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        intent!!.putExtra("Start", "service start")
        sendBroadcast(intent)
        return null
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mTimer = Timer()
        mTimer!!.schedule(TimerTaskToGetLocation(), 5, notify_interval)
        intent = Intent(str_receiver)
        //        fn_getlocation();
    }

    override fun onLocationChanged(location: Location) {}
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    @SuppressLint("MissingPermission")
    private fun fn_getlocation() {
        locationManager = applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
        isGPSEnable = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        isNetworkEnable = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!isGPSEnable && !isNetworkEnable) {
        } else {
            if (isGPSEnable) {
                location = null
                locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    0f,
                    this
                )
                if (locationManager != null) {
                    location = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (location != null) {
                        Log.e("latitude", location!!.latitude.toString() + "")
                        Log.e("longitude", location!!.longitude.toString() + "")
                        latitude = location!!.latitude
                        longitude = location!!.longitude
                        fn_update(location!!)
                    }
                }
            } else  if (isNetworkEnable) {
                location = null
                locationManager!!.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000,
                    0f,
                    this
                )
                if (locationManager != null) {
                    location =
                        locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    if (location != null) {
                        Log.e("latitude", location!!.latitude.toString() + "")
                        Log.e("longitude", location!!.longitude.toString() + "")
                        latitude = location!!.latitude
                        longitude = location!!.longitude
                        fn_update(location!!)
                    }
                }
            }
        }
    }
    @SuppressLint("MissingPermission")

    private inner class TimerTaskToGetLocation : TimerTask() {
        override fun run() {
            mHandler.post { fn_getlocation() }
        }
    }

    private fun fn_update(location: Location) {
        var dis = 0.0
        if (locationsList.isNotEmpty()) {
            dis = distance(locationsList[locationsList.size-1], location)
            if(dis<2.0){
                sendBrodcast(location,dis = dis)
            }
        }else{
            sendBrodcast(location,dis)
        }

    }

    fun sendBrodcast(location: Location,dis:Double){
        locationsList.add(location)
        intent!!.putExtra("latutide", location.latitude.toString().substring(0,5) + "")
        intent!!.putExtra("longitude", location.longitude.toString().substring(0,5) + "")
        intent!!.putExtra("Distance", dis.toString().substring(0,2))
        sendBroadcast(intent)
    }

    private fun distance(loc1: Location, loc2: Location): Double {
        val lat1 = loc1.latitude
        val lon1 = loc1.longitude
        val lat2 = loc2.latitude
        val lon2 = loc2.longitude
        val theta = lon1 - lon2
        var dist = (Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + (Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta))))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515
        val km =dist / 0.62137;
        return km
    }


    companion object {
        var str_receiver = "servicetutorial.service.receiver"
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        mTimer?.cancel()
        intent!!.putExtra("Close", "service closed")
        sendBroadcast(intent)
        super.onDestroy()

    }


    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }
}