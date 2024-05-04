package com.yeonfish.sharelocation.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.yeonfish.sharelocation.MainActivity;
import com.yeonfish.sharelocation.R;
import com.yeonfish.sharelocation.databinding.ActivityMainBinding;
import com.yeonfish.sharelocation.user.GoogleAuth;
import com.yeonfish.sharelocation.user.GoogleUser;
import com.yeonfish.sharelocation.util.HttpUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import kotlinx.coroutines.GlobalScope;


public class F_UpdateLocation extends Service {

    public static final String CHANNEL_ID = "com.yeonfish.sharelocation.ForegroundServiceChannel";

    private GoogleAuth googleAuth;
    private GoogleUser googleUser;
    private String group;

    private FusedLocationProviderClient client;
    private LocationCallback locationUpdates;
    private LocationRequest request;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) { //여기 시작
        Log.d("Foreground Service", "start");

        if (intent.getAction() != null && intent.getAction().equals("stop")) {
            stopForeground(true);
            stopSelf();
        }

        //안드로이드 O버전 이상에서는 알림창을 띄워야 포그라운드 사용 가능
        Intent notificationIntent = new Intent(this, MainActivity.class).setAction("stop");
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        NotificationManager manager = getSystemService(NotificationManager.class);
        assert manager != null;
        manager.createNotificationChannel(serviceChannel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentTitle("위치 공유중...")
                .setContentText("종료하려면 여기를 클릭")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);

        SharedPreferences sp = getSharedPreferences("sharelocation_user", MODE_PRIVATE);
        if (sp.getString("id", null) == null) stopSelf();
        googleUser = new GoogleUser();
        googleUser.setId(sp.getString("id", null));
        googleUser.setEmail(sp.getString("email", null));
        googleUser.setDisplayName(sp.getString("name", null));
        googleUser.setProfilePicture(sp.getString("profilePicture", null));
        group = intent.getStringExtra("group");
        syncLocation();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        client.removeLocationUpdates(locationUpdates);

        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void syncLocation() {
        LocationRequest.Builder requestBuilder = new LocationRequest.Builder(new LocationRequest());
        requestBuilder.setIntervalMillis(1000);
        requestBuilder.setMinUpdateDistanceMeters(1f);
        requestBuilder.setWaitForAccurateLocation(true);
        request = requestBuilder.build();
        locationUpdates = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Location curPos = locationResult.getLastLocation();
                            String query = "id=" + googleUser.getId() + "&group=" + group + "&name=" + URLEncoder.encode(googleUser.getDisplayName(), "utf-8") + "&loc_lat=" + curPos.getLatitude() + "&loc_lng=" + curPos.getLongitude() + "&heading=" + curPos.getBearing() + "&speed=" + curPos.getSpeed();
                            String result = HttpUtil.getInstance().post("https://lyj.kr/Sync", query, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {}
        client = LocationServices.getFusedLocationProviderClient(this);
        client.requestLocationUpdates(request, locationUpdates, Looper.getMainLooper());
    }
}
