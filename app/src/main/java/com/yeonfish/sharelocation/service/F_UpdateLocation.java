package com.yeonfish.sharelocation.service;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.LocationRequest;
import com.yeonfish.sharelocation.MainActivity;
import com.yeonfish.sharelocation.R;
import com.yeonfish.sharelocation.user.GoogleAuth;
import com.yeonfish.sharelocation.user.GoogleUser;
import com.yeonfish.sharelocation.util.HttpUtil;
import com.yeonfish.sharelocation.util.SpeedUtil;

import java.net.URLEncoder;

public class F_UpdateLocation extends Service {

    public static final String CHANNEL_ID = "com.yeonfish.sharelocation.ForegroundServiceChannel";

    private GoogleAuth googleAuth;
    private GoogleUser googleUser;
    private String group;

    private LocationManager client;
    private LocationListener locationUpdates;
    private Location curPos;
    private double speed;

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

        client.removeUpdates(locationUpdates);

        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    void syncLocation() {
        locationUpdates = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (curPos == null)
                                curPos = location;
                            speed = SpeedUtil.calculateSpeed(curPos.getTime(), curPos.getLatitude(), curPos.getLongitude(), location.getTime(), location.getLatitude(), location.getLongitude());
                            curPos = location;
                            String query = "id=" + googleUser.getId() + "&group=" + group + "&name=" + URLEncoder.encode(googleUser.getDisplayName(), "utf-8") + "&loc_lat=" + location.getLatitude() + "&loc_lng=" + location.getLongitude() + "&heading=" + location.getBearing() + "&speed=" + location.getSpeed();
                            String result = HttpUtil.getInstance().post("https://lyj.kr/Sync", query, null);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {}
        client = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        client.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, locationUpdates);
    }
}
