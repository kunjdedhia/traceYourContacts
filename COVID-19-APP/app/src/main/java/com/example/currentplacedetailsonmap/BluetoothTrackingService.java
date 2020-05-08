package com.example.currentplacedetailsonmap;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class BluetoothTrackingService extends Service {

    boolean inProgress = false;
    private BluetoothAdapter mBLTAdapter;
    private BluetoothLeScanner mBLTScanner;
    private ScanSettings mBLTSettings;
    private List<ScanFilter> mBLTfilters;
    private HashSet<String> BLTScanResults = new HashSet<String>();
    private int notificationId = 0;
    private static Location currentLocation;
    private String curLocTime;
    private int curContactNum;
    private int score;
    private File file;
    FileOutputStream writer;

    NotificationCompat.Builder newContactBuilder = new NotificationCompat.Builder(this, "contactalert")
            .setSmallIcon(R.drawable.notificon)
            .setContentTitle("COVID-19: Try social distancing")
            .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("New Contact"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

    NotificationCompat.Builder highExposureBuilder = new NotificationCompat.Builder(this, "contactalert")
            .setSmallIcon(R.drawable.notificon)
            .setContentTitle("COVID-19: High Risk")
            .setContentText("You've been in highly crowded places today")
            .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("New Contact"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

    NotificationCompat.Builder scoreNotif = new NotificationCompat.Builder(this, "contactalert")
            .setSmallIcon(R.drawable.notificon)
            .setContentTitle("COVID-19: High Risk")
            .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("New Contact"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

    public BluetoothTrackingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        curLocTime = "";
        score = 0;
        file = null;
        writer = null;
        super.onCreate();
        Log.d("Bluetooth Service", "Bluetooth Tracking Service onCreate().");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        inProgress = false;
    }

    private final BroadcastReceiver mLocationScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra("locationData");
            Location mostRecentLocation = location;
            Log.d("Location Broadcast Rec", location.getLatitude() + " " + location.getLongitude());
            currentLocation = mostRecentLocation;
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("BLT_START_COMMAND", "Started...");
        if (intent != null) {
            String action = intent.getAction();

           if (action.equals("1")) {
               if (!inProgress) {
                   final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                   mBLTAdapter = bluetoothManager.getAdapter();
                   if (mBLTAdapter == null || !mBLTAdapter.isEnabled()) {
                       Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                       enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                       startActivity(enableBtIntent);
                   }

                   int waitTime = 20;
                   while (waitTime > 0 && (mBLTAdapter == null || !mBLTAdapter.isEnabled())) {
                       waitTime--;
                       SystemClock.sleep(1000);
                   }

                   if (mBLTAdapter == null || !mBLTAdapter.isEnabled()) {
                       inProgress = false;
                       stopBLTTrackingService();
                   }

                   createNotificationChannel();
                   registerReceiver(mLocationScanReceiver, new IntentFilter("curLoc"));

                   SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                   String fileName = sdf.toString();
                   file = getFileStreamPath(fileName + ".txt");

                   if (!file.exists()) {
                       try {
                           file.createNewFile();
                       } catch (IOException e) {
                           e.printStackTrace();
                       }
                   }

                   mBLTScanner = mBLTAdapter.getBluetoothLeScanner();
                   mBLTSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                   ScanFilter.Builder filterBuilder;
                   mBLTfilters = new ArrayList<>();
                   for (int i = 0; i < 1000; i++){
                       filterBuilder = new ScanFilter.Builder();
                       filterBuilder.setManufacturerData(i, new byte[] {});
                       mBLTfilters.add(filterBuilder.build());
                   }
                   mBLTScanner.startScan(mBLTfilters, mBLTSettings, mBLTScanCallback);
                   inProgress = true;
               }
           } else {
               inProgress = false;
            }
        }
        return START_REDELIVER_INTENT;
    }

    private void stopBLTTrackingService() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopForeground(true);
        stopSelf();
    }

    private ScanCallback mBLTScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String deviceAddress = result.getDevice().getAddress();
            if (currentLocation != null) {
                Log.d("Bluetooth Scan Results", deviceAddress + " " + currentLocation.getLatitude() + " " + currentLocation.getLongitude());
                curLocTime = DateFormat.getTimeInstance().format(new Date());

                if (!BLTScanResults.contains(deviceAddress)) {
                    BLTScanResults.add(deviceAddress);

                    String data = currentLocation.getLatitude() + " " + currentLocation.getLongitude() + " " + curLocTime;
                    try {
                        writer = openFileOutput(file.getName(), Context.MODE_PRIVATE);
                        writer.write(data.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    curContactNum = BLTScanResults.size();
                    if (BLTScanResults.size() % 2 == 0) {
                        score -= 5;
                    }
                    if (BLTScanResults.size() % 3 == 0) {
                        newContactBuilder.setContentText("Close contact with " + Integer.toString(curContactNum) + " people today!");
                        pushNotification(1);
                    }
                    if (BLTScanResults.size() % 10 == 0) {
                        pushNotification(2);
                        score -= 10;
                    }
                    if (BLTScanResults.size() % 5 == 0) {
                        scoreNotif.setContentText("Your score has dropped to " + Integer.toString(score) + ". Try Social Distancing!");
                    }
                }
            }
        }

    };

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel("contactalert", "COVID-19", importance);
            channel.setDescription("COVID-19 Social Distancing");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void pushNotification(int id) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        switch (id) {
            case 1:
                notificationManager.notify(notificationId++, newContactBuilder.build());
                break;
            case 2:
                notificationManager.notify(notificationId++, highExposureBuilder.build());
                break;
        }
    }

}
