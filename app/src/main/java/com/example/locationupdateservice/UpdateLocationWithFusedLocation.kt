package com.example.locationupdateservice

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.provider.ContactsContract.Directory.PACKAGE_NAME
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil


class UpdateLocationWithFusedLocation : Service() {
    var TAG = UpdateLocationWithFusedLocation.javaClass.name
    private var fusedLocationClient: FusedLocationProviderClient? = null
    var intent: Intent? = null
    var locationsList: MutableList<Location> = mutableListOf<Location>()
    private lateinit var locationCallback: LocationCallback
    var LOCATION_UPDATE_INTERVAL: Long = 5000
    var LOCATION_FASTEST_INTERVAL: Long = 5000
    var totalDistance: Float = 0F
    private lateinit var notificationManager: NotificationManager
    private val localBinder = LocalBinder()
    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return localBinder
    }
    inner class LocalBinder : Binder() {
        internal val service: UpdateLocationWithFusedLocation
            get() = this@UpdateLocationWithFusedLocation
    }

    companion object {
        private const val NOTIFICATION_ID = 123456780
        var isServiceStarted = false
        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_04"
        internal const val ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST =
            "$PACKAGE_NAME.action.FOREGROUND_ONLY_LOCATION_BROADCAST"
    }

    override fun onCreate() {
        super.onCreate()
        isServiceStarted = true
        intent = Intent(ACTION_FOREGROUND_ONLY_LOCATION_BROADCAST)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    if (location != null) {

                        fn_update(location)
                    }
                }
            }
        }
        setLocationListner()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        return START_STICKY
    }

    fun stopLocationUpdates() {
        fusedLocationClient?.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    private fun setLocationListner() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // for getting the current location update after every 2 seconds with high accuracy
        val locationRequest = LocationRequest.create().apply {
            interval = LOCATION_UPDATE_INTERVAL
            fastestInterval = LOCATION_FASTEST_INTERVAL
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.myLooper()!!
        )
    }

    var dis = 0.0F
    private fun fn_update(location: Location) {

        if (locationsList.isNotEmpty()) {
            val loc1 = locationsList[locationsList.size - 1]
            val loc2 = location
//            val dis1 = distanceBetween(
//                LatLng(loc1.latitude, loc1.longitude),
//                LatLng(loc2.latitude, loc2.longitude)
//            )
            val newdist = location.distanceTo(loc1) / 1000
            Log.e("lat new dist ", "" + newdist)

//            dis = distance(locationsList[locationsList.size - 1], location)


            if (newdist < 2.0) {
                dis += newdist
                Log.e(
                    "lat--",
                    "" + location.latitude.toString() + "//Long--" + location.longitude.toString() + "//Dis--" + dis + "km"
                )
                sendBrodcast(location, dis = dis.toDouble())
            }
        } else {
            sendBrodcast(location, dis.toDouble())
        }

    }

    fun sendBrodcast(location: Location, dis: Double) {
        locationsList.add(location)
        intent!!.putExtra("latutide", location.latitude.toString().substring(0, 7) + "")
        intent!!.putExtra("longitude", location.longitude.toString().substring(0, 7) + "")
        intent!!.putExtra("Distance", dis.toString())
        sendBroadcast(intent)
    }


    private fun distance(loc1: Location, loc2: Location): Double {
        val lat1 = loc1.latitude.toString().substring(0, 6).toDouble()
        val lon1 = loc1.longitude.toString().substring(0, 6).toDouble()
        val lat2 = loc2.latitude.toString().substring(0, 6).toDouble()
        val lon2 = loc2.longitude.toString().substring(0, 6).toDouble()
        val theta = lon1 - lon2
        var dist = (Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + (Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta))))
        dist = Math.acos(dist)
        dist = rad2deg(dist)
        dist = dist * 60 * 1.1515
        val km = dist / 0.62137;
//        val distance = Math.sqrt((lat2 - lat1) * (lat2 - lat1) + (lon2 - lon1) * (lon2 - lon1));
//        Log.e("lat two points is:" ,""+ distance);
        return km
    }

    fun distanceBetween(point1: LatLng?, point2: LatLng?): Double? {
        return if (point1 == null || point2 == null) {
            null
        } else SphericalUtil.computeDistanceBetween(point1, point2)
    }

    override fun onRebind(intent: Intent?) {
        Log.d(TAG, "onRebind()")
        stopForeground(true)
        super.onRebind(intent)
    }
    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind()")
        val notification = generateNotification(locationsList.get(locationsList.size - 1))
        startForeground(NOTIFICATION_ID, notification)
        return true
    }

    private fun generateNotification(location: Location?): Notification {
        Log.d(TAG, "generateNotification()")

        // Main steps for building a BIG_TEXT_STYLE notification:
        //      0. Get data
        //      1. Create Notification Channel for O+
        //      2. Build the BIG_TEXT_STYLE
        //      3. Set up Intent / Pending Intent for notification
        //      4. Build and issue the notification

        // 0. Get data
        val mainNotificationText = location?.toString() ?: getString(R.string.no_location_text)
        val titleText = getString(R.string.app_name)

        // 1. Create Notification Channel for O+ and beyond devices (26+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT
            )

            // Adds NotificationChannel to system. Attempting to create an
            // existing notification channel with its original values performs
            // no operation, so it's safe to perform the below sequence.
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, MainActivity::class.java)

//        val cancelIntent = Intent(this, UpdateLocationWithFusedLocation::class.java)
//        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, true)

//        val servicePendingIntent = PendingIntent.getService(
//            this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT
//        )

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, 0
        )

        // 4. Build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        return notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
//            .addAction(
//                R.drawable.ic_launch, getString(R.string.launch_activity),
//                activityPendingIntent
//            )
//            .addAction(
//                R.drawable.ic_cancel,
//                getString(R.string.stop_location_updates_button_text),
//                servicePendingIntent
//            )
            .build()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")

        intent!!.putExtra("Close", "service closed")
        sendBroadcast(intent)
        isServiceStarted = false
    }


    private fun deg2rad(deg: Double): Double {
        return deg * Math.PI / 180.0
    }

    private fun rad2deg(rad: Double): Double {
        return rad * 180.0 / Math.PI
    }
}