package com.example.locationupdateservice

import android.Manifest
import android.app.ActivityManager
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import androidx.core.app.ActivityCompat

import android.content.pm.PackageManager

import androidx.core.content.ContextCompat

import android.location.Address
import android.os.Build
import android.os.IBinder

import android.widget.Toast
import java.io.IOException
import java.lang.StringBuilder
import android.text.method.ScrollingMovementMethod
import androidx.localbroadcastmanager.content.LocalBroadcastManager


class MainActivity : AppCompatActivity() {
    lateinit var s: StringBuilder
    var locationList: MutableList<Latlng> = mutableListOf()

    // Provides location updates for while-in-use feature.
    private var updateLocationWithFusedLocation: UpdateLocationWithFusedLocation? = null
    private var foregroundOnlyLocationServiceBound = false

    data class Latlng(
        var lat: Double,
        var long: Double
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        s = StringBuilder()
        location_display_textView.movementMethod = ScrollingMovementMethod()

        start_button.setOnClickListener {
            if (checkPermissions(false) == true) {
                locationList = mutableListOf()
                val serviceIntent = Intent(this, UpdateLocationWithFusedLocation::class.java)
                bindService(serviceIntent, foregroundOnlyServiceConnection, BIND_AUTO_CREATE)
//                startService(Intent(this, UpdateLocationWithFusedLocation::class.java))
            } else {
                Toast.makeText(this, "Already Started", Toast.LENGTH_SHORT).show()
            }
        }

        stop_button.setOnClickListener {
            s.clear()
            location_display_textView.text = s.toString()
            updateLocationWithFusedLocation?.stopLocationUpdates()
            if (foregroundOnlyLocationServiceBound) {
                unbindService(foregroundOnlyServiceConnection)
                foregroundOnlyLocationServiceBound = false
            }

//            stopService(Intent(this, UpdateLocationWithFusedLocation::class.java))
        }

        get_distance_button.setOnClickListener {
            val first = locationList.get(0)
            val curr = locationList.get(locationList.size - 1)
            val totalDistance = distance(first.lat, first.long, curr.lat, curr.long)
            s.append("Total distance", "" + totalDistance)
            location_display_textView.text = s.toString()
        }

    }

    private fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val theta = lon1 - lon2
        var dist = (Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + (Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta))))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515
        return dist
    }

    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }


    private val REQUEST_PERMISSIONS = 100
    var boolean_permission = false

    private fun checkPermissions(checkRational: Boolean): Boolean? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (checkRational && ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Toast.makeText(this, "Allow Location from Settings", Toast.LENGTH_LONG)
                null
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    REQUEST_PERMISSIONS
                )
                false
            }
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean_permission = true

                } else {
                    Toast.makeText(
                        applicationContext,
                        "Please allow the permission",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.hasExtra("Close")) {
                s.append("\n service Close")
                location_display_textView.text = s.toString()
                return
            } else if (intent.hasExtra("Start")) {
                s.append("\n service Start")
                location_display_textView.text = s.toString()
                return
            }
            val latitude = java.lang.Double.valueOf(intent.getStringExtra("latutide"))
            val longitude = java.lang.Double.valueOf(intent.getStringExtra("longitude"))
            val latt = Latlng(latitude, longitude)
            locationList.add(latt)
            val dis = java.lang.Double.valueOf(intent.getStringExtra("Distance"))
            var addresses: List<Address>? = null
            try {
//                addresses = geocoder.getFromLocation(latitude, longitude, 1)
//                val cityName: String = addresses[0].getAddressLine(0)
//                val stateName: String = addresses[0].getAddressLine(1)
//                val countryName: String = addresses[0].getAddressLine(2)
//                tv_area.setText(addresses[0].getAdminArea())
//                tv_locality.setText(stateName)
//                tv_address.setText(countryName)
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
            s.append("\n lat--" + latitude.toString() + "//Long--" + longitude.toString() + "//Dis--" + dis + "km")
            location_display_textView.text = s.toString()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(broadcastReceiver)
    }

    // Monitors connection to the while-in-use service.
    private val foregroundOnlyServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as UpdateLocationWithFusedLocation.LocalBinder
            updateLocationWithFusedLocation = binder.service
            foregroundOnlyLocationServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            updateLocationWithFusedLocation = null
            foregroundOnlyLocationServiceBound = false
        }
    }


    override fun onResume() {
        super.onResume()
        registerReceiver(
            broadcastReceiver,
            IntentFilter(
                UpdateLocationWithFusedLocation.ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST
            )
        )
    }


}