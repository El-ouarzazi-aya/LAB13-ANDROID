package com.example.mapapplication;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GoogleMapActivity extends AppCompatActivity {

    // ── UI ────────────────────────────────────────────────────────────────────
    private MapView  osmMapSurface;
    private TextView txtMarkerCount, txtSelectedPin;
    private CardView btnCenter;

    // ── Data ──────────────────────────────────────────────────────────────────
    private final List<GeoPoint> allPoints = new ArrayList<>();

    // ── Network ───────────────────────────────────────────────────────────────
    private RequestQueue httpQueue;
    private static final String FETCH_PINS_ENDPOINT =
            "http://10.0.2.2/map_project/getPosition.php";

    // ── Defaults ──────────────────────────────────────────────────────────────
    private static final double DEFAULT_LAT  = 33.9716;
    private static final double DEFAULT_LON  = -6.8498;
    private static final double DEFAULT_ZOOM = 13.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initOsmdroid();
        setContentView(R.layout.activity_google_map);

        bindViews();
        applyColors();
        bootMapSurface();
        setupCenterButton();

        httpQueue = Volley.newRequestQueue(getApplicationContext());
        fetchAndPlotPins();
    }

    // ── OSMDroid ──────────────────────────────────────────────────────────────
    private void initOsmdroid() {
        Context ctx = getApplicationContext();
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidBasePath(
                new File(getCacheDir(), "osmdroid"));
        Configuration.getInstance().setOsmdroidTileCache(
                new File(getCacheDir(), "osmdroid/tiles"));
        Configuration.getInstance().setTileDownloadThreads((short) 4);
        Configuration.getInstance().setTileFileSystemCacheMaxBytes(
                50L * 1024 * 1024);
        Configuration.getInstance().load(
                ctx, getSharedPreferences("osm_prefs", MODE_PRIVATE));
    }

    // ── Binding ───────────────────────────────────────────────────────────────
    private void bindViews() {
        osmMapSurface  = findViewById(R.id.map);
        txtMarkerCount = findViewById(R.id.txtMarkerCount);
        txtSelectedPin = findViewById(R.id.txtSelectedPin);
        btnCenter      = findViewById(R.id.btnCenter);
    }

    // ── Couleurs programmatiques ──────────────────────────────────────────────
    private void applyColors() {
        ImageView iconHeader = findViewById(R.id.iconHeader);
        ImageView iconCenter = findViewById(R.id.iconCenter);

        if (iconHeader != null)
            iconHeader.setColorFilter(0xFF00D4FF,
                    android.graphics.PorterDuff.Mode.SRC_IN);
        if (iconCenter != null)
            iconCenter.setColorFilter(0xFF00D4FF,
                    android.graphics.PorterDuff.Mode.SRC_IN);
    }

    // ── Map ───────────────────────────────────────────────────────────────────
    private void bootMapSurface() {
        XYTileSource tileSource = new XYTileSource(
                "OSM", 0, 19, 256, ".png",
                new String[]{
                        "https://a.tile.openstreetmap.org/",
                        "https://b.tile.openstreetmap.org/",
                        "https://c.tile.openstreetmap.org/"
                }
        );
        osmMapSurface.setTileSource(tileSource);
        osmMapSurface.setBuiltInZoomControls(false);
        osmMapSurface.setMultiTouchControls(true);
        osmMapSurface.getController().setZoom(DEFAULT_ZOOM);
        osmMapSurface.getController().setCenter(
                new GeoPoint(DEFAULT_LAT, DEFAULT_LON));
    }

    private void setupCenterButton() {
        btnCenter.setOnClickListener(v -> {
            if (!allPoints.isEmpty()) {
                GeoPoint last = allPoints.get(allPoints.size() - 1);
                osmMapSurface.getController().animateTo(last);
                osmMapSurface.getController().setZoom(15.0);
            } else {
                osmMapSurface.getController().animateTo(
                        new GeoPoint(DEFAULT_LAT, DEFAULT_LON));
            }
        });
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override protected void onResume()  { super.onResume();  osmMapSurface.onResume(); }
    @Override protected void onPause()   { super.onPause();   osmMapSurface.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); osmMapSurface.onDetach(); }

    // ── Network ───────────────────────────────────────────────────────────────
    private void fetchAndPlotPins() {
        txtSelectedPin.setText("Chargement des positions...");

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST,
                FETCH_PINS_ENDPOINT,
                null,
                this::handlePinResponse,
                error -> {
                    txtSelectedPin.setText("Erreur de connexion au serveur");
                    error.printStackTrace();
                }
        );
        httpQueue.add(req);
    }

    private void handlePinResponse(JSONObject serverData) {
        try {
            JSONArray pinArray = serverData.getJSONArray("positions");
            int count = pinArray.length();

            for (int i = 0; i < count; i++) {
                JSONObject entry = pinArray.getJSONObject(i);
                double lat  = entry.getDouble("latitude");
                double lon  = entry.getDouble("longitude");
                String date = entry.optString("date", "");

                allPoints.add(new GeoPoint(lat, lon));
                dropMarkerAt(lat, lon, i + 1, date);
            }

            osmMapSurface.invalidate();
            txtMarkerCount.setText(count + " pts");
            txtSelectedPin.setText(count == 0
                    ? "Aucune position enregistrée"
                    : count + " position(s) — appuyez sur un marqueur");

            if (!allPoints.isEmpty()) {
                osmMapSurface.getController().animateTo(
                        allPoints.get(allPoints.size() - 1));
            }

        } catch (JSONException e) {
            e.printStackTrace();
            txtSelectedPin.setText("Erreur lecture des données");
        }
    }

    // ── Markers ───────────────────────────────────────────────────────────────
    private void dropMarkerAt(double lat, double lon, int index, String date) {
        Marker pin = new Marker(osmMapSurface);
        pin.setPosition(new GeoPoint(lat, lon));
        pin.setTitle("Position #" + index);
        pin.setSnippet(date);

        Drawable icon = getResources().getDrawable(
                android.R.drawable.ic_menu_mylocation);
        pin.setIcon(icon);
        pin.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        pin.setOnMarkerClickListener((marker, mapView) -> {
            txtSelectedPin.setText(
                    "Position #" + index
                            + "\nLat: " + String.format(Locale.US, "%.5f", lat)
                            + "  Lon: " + String.format(Locale.US, "%.5f", lon)
                            + (date.isEmpty() ? "" : "\n" + date));
            return true;
        });

        osmMapSurface.getOverlays().add(pin);
    }
}