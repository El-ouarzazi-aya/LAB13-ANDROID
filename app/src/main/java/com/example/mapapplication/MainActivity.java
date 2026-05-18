package com.example.mapapplication;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // ── UI ────────────────────────────────────────────────────────────────────
    private Button   btnMap;
    private TextView txtStatus, txtLat, txtLon, txtAlt, txtAccuracy, txtSentCount;
    private View     statusDot;
    private ImageView iconGps;

    // ── Location ──────────────────────────────────────────────────────────────
    private LocationManager locationManager;
    private double currentLat, currentLon, currentAlt;
    private float  currentAccuracy;

    // ── Network ───────────────────────────────────────────────────────────────
    private RequestQueue requestQueue;
    private static final String INSERT_URL =
            "http://10.0.2.2/map_project/createPosition.php";

    // ── State ─────────────────────────────────────────────────────────────────
    private int sentCount = 0;
    private static final int PERMISSION_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        applyColors();
        setupButton();

        requestQueue    = Volley.newRequestQueue(getApplicationContext());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        checkAndRequestPermissions();
    }

    // ── View binding ──────────────────────────────────────────────────────────
    private void bindViews() {
        btnMap       = findViewById(R.id.btnMap);
        txtStatus    = findViewById(R.id.txtStatus);
        txtLat       = findViewById(R.id.txtLat);
        txtLon       = findViewById(R.id.txtLon);
        txtAlt       = findViewById(R.id.txtAlt);
        txtAccuracy  = findViewById(R.id.txtAccuracy);
        txtSentCount = findViewById(R.id.txtSentCount);
        statusDot    = findViewById(R.id.statusDot);
        iconGps      = findViewById(R.id.iconGps);
    }

    // ── Appliquer les couleurs programmatiquement ─────────────────────────────
    private void applyColors() {
        iconGps.setColorFilter(0xFF00D4FF,
                android.graphics.PorterDuff.Mode.SRC_IN);
    }

    private void setupButton() {
        btnMap.setOnClickListener(v -> {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 1f, 0.95f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 1f, 0.95f, 1f);
            scaleX.setDuration(150);
            scaleY.setDuration(150);
            scaleX.start();
            scaleY.start();
            startActivity(new Intent(this, GoogleMapActivity.class));
        });
    }

    // ── Permissions ───────────────────────────────────────────────────────────
    private void checkAndRequestPermissions() {
        boolean hasLocation = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!hasLocation) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    }, PERMISSION_CODE);
        } else {
            startLocationTracking();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationTracking();
        } else {
            setStatusWaiting("Permission refusée");
        }
    }

    // ── Location ──────────────────────────────────────────────────────────────
    private void startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        setStatusWaiting("Recherche du signal GPS...");

        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                30000, 50, locationListener);
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currentLat      = location.getLatitude();
            currentLon      = location.getLongitude();
            currentAlt      = location.getAltitude();
            currentAccuracy = location.getAccuracy();
            updateUI();
            sendPosition(currentLat, currentLon);
        }

        @Override public void onProviderEnabled(@NonNull String p)  { setStatusActive(); }
        @Override public void onProviderDisabled(@NonNull String p) { setStatusWaiting("GPS désactivé"); }
        @Override public void onStatusChanged(String p, int s, Bundle e) {}
    };

    // ── UI updates ────────────────────────────────────────────────────────────
    private void updateUI() {
        txtLat.setText(String.format(Locale.US, "%.5f", currentLat));
        txtLon.setText(String.format(Locale.US, "%.5f", currentLon));
        txtAlt.setText(String.format(Locale.US, "%.1f m", currentAlt));
        txtAccuracy.setText(String.format(Locale.US, "±%.0fm", currentAccuracy));
        setStatusActive();
        animatePulse(txtLat);
        animatePulse(txtLon);
    }

    private void setStatusActive() {
        txtStatus.setText("Signal GPS actif");
        txtStatus.setTextColor(0xFF00D4FF);
    }

    private void setStatusWaiting(String message) {
        txtStatus.setText(message);
        txtStatus.setTextColor(0xFF6B7FA3);
    }

    private void animatePulse(View view) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 0.3f, 1f);
        anim.setDuration(400);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.start();
    }

    // ── Network ───────────────────────────────────────────────────────────────
    private void sendPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(
                Request.Method.POST, INSERT_URL,
                response -> {
                    sentCount++;
                    txtSentCount.setText(sentCount + " position(s) envoyée(s)");
                },
                error -> Toast.makeText(this,
                        "Erreur envoi", Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                SimpleDateFormat sdf =
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                params.put("latitude",  String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date",      sdf.format(new Date()));
                params.put("imei",      Settings.Secure.getString(
                        getContentResolver(), Settings.Secure.ANDROID_ID));
                return params;
            }
        };
        requestQueue.add(request);
    }
}