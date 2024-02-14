package com.yeonfish.sharelocation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.kakao.sdk.common.KakaoSdk;
import com.yeonfish.sharelocation.databinding.ActivityMainBinding;
import com.yeonfish.sharelocation.util.HttpUtil;
import com.yeonfish.sharelocation.util.TimeUtil;
import com.yeonfish.sharelocation.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ActivityMainBinding binding;

    // google map
    private boolean isGranted;
    private String group;
    private GoogleMap mMap;
    private Marker mMarker;
    private Map<String, Marker> markers = new HashMap<>();

    // location
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationUpdates;
    private boolean track = false;
    private Location curPos;
    private Timer locationUpdate;

    // google auth
    private static final int REQ_ONE_TAP = 7395;  // Can be any integer unique to the Activity.
    private boolean showOneTapUI = true;
    private GoogleSignInAccount user;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        KakaoSdk.init(this, "7c91ec8d182523adfa1e6f0df190eff3");

        googleSignInClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder()
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build());
        if (GoogleSignIn.getLastSignedInAccount(this) == null){
            googleSignInClient.signOut();
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, REQ_ONE_TAP);
        }else {
            user = GoogleSignIn.getLastSignedInAccount(this);
            binding.accountTextView.setText("계정: "+user.getEmail()+" (로그아웃)");
        }

        // get group
        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null)
            group = data.getQueryParameter("group");
        if (group == null)
            group = UUID.generate();
        binding.groupTextView.setText("그룹: " + group);

        binding.buttonShare.setOnClickListener(clickListener);
        binding.buttonCLocation.setOnClickListener(clickListener);
        binding.accountTextView.setOnClickListener(clickListener);

        isGranted = (
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
//                        ActivityCompat.checkSelfPermission(this, android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED
        );
        if (!isGranted) {
            String[] permissions = {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, 6040);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location == null) return;

                curPos = location;

                LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
                mMarker = mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(user != null ? user.getDisplayName() : "Me!")
                        .snippet(curPos.getSpeed()+" km/h"));
                mMarker.setIcon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(MainActivity.this, R.drawable.arrow_maps_icon_217969)));
                mMarker.setRotation(location.getBearing());
                mMarker.showInfoWindow();

                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(pos, 17.0F, mMap.getCameraPosition().tilt, mMap.getCameraPosition().bearing)), 1000, null);

                mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                    @Override
                    public void onCameraMove() {
                        mMarker.setRotation(curPos.getBearing()-mMap.getCameraPosition().bearing);
                    }
                });

                locationUpdate = startSyncLocation();
            }
        });

        startRequestLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isRestartRequired = false;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                isRestartRequired = true;

//                Toast.makeText(this, "위치에 항상 접근할 수 있도록 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 1);
//                Toast.makeText(this, "권한을 허용한 후, 앱을 재시작 해 주세요", Toast.LENGTH_LONG).show();
            }else {
                Toast.makeText(this, "권한이 허용되었습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_ONE_TAP:
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    Log.d("Google Login", "GoogleAuth:" + account.getId());
                    user = GoogleSignIn.getLastSignedInAccount(this);
                    binding.accountTextView.setText("계정: "+user.getEmail()+" (로그아웃)");
                    mMarker.setTitle(user.getDisplayName());
                } catch (ApiException e) {
                    // Google Sign In failed, update UI appropriately
                    Log.w("Google Login", "Google sign in failed", e);
                }
                break;
        }
    }

    @Override // startLocationUpdate when re-open app
    public void onResume() {
        super.onResume();
        if (fusedLocationClient != null) {
            startRequestLocation();
        }
    }

    @Override // stopLocationUpdate when close app
    public void onPause() {
        super.onPause();
        if (locationUpdates != null) {
            fusedLocationClient.removeLocationUpdates(locationUpdates);
        }
    }

    private final View.OnClickListener clickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.buttonShare) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "https://lyj.kr?group="+group);
                sendIntent.setType("text/url");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            }
            if (v.getId() == R.id.buttonCLocation) {
                if (track) {
                    track = false;
                    binding.buttonCLocation.setBackgroundColor(getColor(R.color.disable));
                }else {
                    track = true;
                    if (curPos != null)
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(new LatLng(curPos.getLatitude(), curPos.getLongitude()), 15f+(5/(curPos.getSpeed() == 0.0f ? 2f : curPos.getSpeed())), mMap.getCameraPosition().tilt, curPos.getBearing())), 1000, null);
                    binding.buttonCLocation.setBackgroundColor(getColor(R.color.enable));
                }
            }
            if (v.getId() == R.id.accountTextView) {
                if (user != null) {
                    googleSignInClient.signOut();
                    TimeUtil.setTimeout(new Runnable() {
                        @Override
                        public void run() {
                            restart();
                        }
                    }, 200);
                }else {
                    googleSignInClient.signOut();
                    Intent signInIntent = googleSignInClient.getSignInIntent();
                    startActivityForResult(signInIntent, REQ_ONE_TAP);
                }
            }
        }
    };

    private void restart() {
        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
        intent.setData(Uri.parse("https://lyj.kr/?group="+group));
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        startActivity(mainIntent);
        System.exit(0);
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private void startRequestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        LocationRequest.Builder requestBuilder = new LocationRequest.Builder(new LocationRequest());
        requestBuilder.setMinUpdateDistanceMeters(1f);
        requestBuilder.setIntervalMillis(1000);
        requestBuilder.setWaitForAccurateLocation(true);

        locationUpdates = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                curPos = locationResult.getLastLocation();

                LatLng pos = new LatLng(curPos.getLatitude(), curPos.getLongitude());
                if (mMarker != null) {
                    mMarker.setPosition(pos);
                    mMarker.setRotation(curPos.getBearing());
                    mMarker.setSnippet(curPos.getSpeed()+" km/h");
                }

                binding.speedTextView.setText(String.valueOf(new BigDecimal(curPos.getSpeed()).setScale(2, RoundingMode.HALF_DOWN))+" km/h");

                if (track && mMap != null) // set zoom by speed
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(pos, 15f+(5/(curPos.getSpeed() == 0.0f ? 1f : curPos.getSpeed())), mMap.getCameraPosition().tilt, mMap.getCameraPosition().bearing)), 1000, null);
            }
        };
        fusedLocationClient.requestLocationUpdates(requestBuilder.build(), locationUpdates, Looper.myLooper());
    }

    private Timer startSyncLocation() {
        Log.d("Timer", "start");
        return TimeUtil.setInterval(new TimerTask() {
            @Override
            public void run() {
                if (curPos != null) {
                    String result;

                    if (user == null)
                        result = HttpUtil.getInstance().post("https://lyj.kr/Sync", "group="+group, null);
                    else {
                        String query;
                        try {
                            query = "id="+user.getId()+"&group="+group+"&name="+ URLEncoder.encode(user.getDisplayName(), "utf-8")+"&loc_lat="+curPos.getLatitude()+"&loc_lng="+curPos.getLongitude()+"&heading="+curPos.getBearing()+"&speed="+curPos.getSpeed();
                        } catch (UnsupportedEncodingException e) {
                            throw new RuntimeException(e);
                        }
                        result = HttpUtil.getInstance().post("https://lyj.kr/Sync", query, null);
                    }

                    try {
                        JSONArray parsedResult = new JSONArray(result);
                        Map<String, Marker> newList = new HashMap<>();
                        for (int i=0;i<parsedResult.length();i++) {
                            JSONObject tmp = parsedResult.getJSONObject(i);

                            if (user != null && user.getId().equals(tmp.getString("id"))) continue;

                            if (markers.containsKey(tmp.getString("id"))) {
                                Marker mkrTmp = markers.get(tmp.getString("id"));
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            mkrTmp.setSnippet(tmp.getString("speed")+" km/h");
                                            mkrTmp.setRotation(Float.parseFloat(tmp.getString("heading"))-mMap.getCameraPosition().bearing);
                                            mkrTmp.setPosition(new LatLng(Double.parseDouble(tmp.getString("loc_lat")), Double.parseDouble(tmp.getString("loc_lng"))));
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });

                                newList.put(tmp.getString("id"), mkrTmp);
                            }else {
                                MarkerOptions mkrOptTmp = new MarkerOptions();
                                mkrOptTmp.icon(BitmapDescriptorFactory.fromBitmap(getBitmapFromVectorDrawable(MainActivity.this, R.drawable.arrow_maps_icon_217970)));
                                mkrOptTmp.title(tmp.getString("name"));
                                mkrOptTmp.snippet(tmp.getString("speed")+" km/h");
                                mkrOptTmp.rotation(Float.parseFloat(tmp.getString("heading"))-mMap.getCameraPosition().bearing);
                                mkrOptTmp.position(new LatLng(Double.parseDouble(tmp.getString("loc_lat")), Double.parseDouble(tmp.getString("loc_lng"))));

                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Marker mkrTmp = mMap.addMarker(mkrOptTmp);
                                            mkrTmp.showInfoWindow();
                                            newList.put(tmp.getString("id"), mkrTmp);
                                        } catch (JSONException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                });
                            }
                        }

                        markers.forEach((k,v)->v.remove());
                        markers.clear();
                        markers = newList;
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }, 1000);
    }
}