package com.saksham.driverdrowsy

import android.app.Service
import android.content.Intent
import android.location.Geocoder
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.location.Address
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.view.ContentInfoCompat.Flags
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.util.*

class LocationService : Service() {

    val BROADCAST_ACTION = "com.saksham.driverdrowsy.user_location"
    private lateinit var mLocationRequest: LocationRequest
    private var mGeocoder: Geocoder? = null
    var intent: Intent? = null

    override fun onCreate() {
        super.onCreate()
        intent = Intent(BROADCAST_ACTION)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mGeocoder = Geocoder(this, Locale.getDefault())
        if (ActivityCompat.checkSelfPermission(this,
                ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return startId
        } else {
            if (!this::mLocationRequest.isInitialized) {
                mLocationRequest = LocationRequest.create()
                mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                mLocationRequest.interval = 10000
                mLocationRequest.numUpdates = 1
            }

            Looper.myLooper()?.let {
                LocationServices.getFusedLocationProviderClient(this)
                    .requestLocationUpdates(mLocationRequest, object : LocationCallback() {
                        override fun onLocationResult(p0: LocationResult) {
                            val loc = p0.lastLocation
                            loc.latitude
                            loc.longitude
                            var areaList: List<Address>? = null

                            try {
                                areaList = loc.latitude.let {
                                    mGeocoder?.getFromLocation(it, loc.longitude, 1)
                                }
                                val locality = areaList?.get(0)
                                sendBroadCast(loc.latitude, loc.longitude, locality?.getAddressLine(locality.maxAddressLineIndex))
                            }
                            catch (_: Exception){
                            }
                        }

                    }, it)
            }
        }
        return startId

    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun sendBroadCast(lat: Double?, long: Double?, address: String?) {
        intent?.putExtra("Latitude", lat)
        intent?.putExtra("Longitude", long)
        intent?.putExtra("Address", address)
        sendBroadcast(intent)
    }
}