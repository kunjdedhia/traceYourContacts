package com.example.currentplacedetailsonmap;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;


import java.text.DateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class LocationTrackingService extends Service implements  GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    protected GoogleApiClient googleApiClient;
    protected LocationRequest locationRequest;
    protected Location prevLocation, updLocation, curLocation;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onCreate() {
        super.onCreate();
        prevLocation = new Location("Point A");
        updLocation = new Location("Point B");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(LocationTrackingService.this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = intent.getAction();
            Log.d("Location Service", "Location in");
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            locationRequest = new LocationRequest();
            locationRequest.setInterval(45000);
            locationRequest.setFastestInterval(45000);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            googleApiClient.connect();
                if (googleApiClient.isConnected()) {
                    reqLocUpdates();
                }
//        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("LocationTrackingService", location.getLatitude() + " " + location.getLongitude());
        curLocation = location;
        transmitBroadCast();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        reqLocUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("Location Service", "Connection Failed");
    }

    protected void reqLocUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.myLooper());
        } catch (SecurityException ex) {
            Log.d("Location Service", "Location update failed");
        }
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            curLocation = locationResult.getLastLocation();
            Log.d("LocationTrackingService", curLocation.getLatitude() + " " + curLocation.getLongitude());
            transmitBroadCast();
        }
    };

    private void transmitBroadCast() {
        if (null != curLocation) {
            Intent locationIntent = new Intent("curLoc");
            locationIntent.putExtra("locationData", curLocation);
            sendBroadcast(locationIntent);
            Log.d("Location Service", "broadcast sent");
        } else {
            Log.d("Location Service", "Location Unavailable");
        }
    }
}
