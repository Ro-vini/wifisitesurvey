package com.example.wifisitesurvey.ui.survey;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.wifisitesurvey.R;
import com.example.wifisitesurvey.data.model.DataPoint;
import com.example.wifisitesurvey.ui.main.MainActivity;
import com.example.wifisitesurvey.utils.PermissionUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import android.graphics.Color;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.provider.MediaStore;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

// Adicione esta importação no topo (necessária para os novos métodos)
import android.graphics.drawable.BitmapDrawable;
import com.google.android.gms.maps.model.LatLngBounds;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.content.Context;

public class SurveyActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveListener {

    // Views e Componentes Principais
    private GoogleMap googleMap;
    private SurveyViewModel viewModel;
    //private TileOverlay heatmapOverlay;
    private GroundOverlay heatmapOverlay;
    private GroundOverlay floorplanOverlay;
    private FusedLocationProviderClient fusedLocationClient; // << ADICIONE ESTA LINHA

    // Views do Layout
    private Button startStopButton, generateHeatmapButton, btnConfirmPoint;
    private Button btnPlaceFloorplan, btnEditFloorplan, btnDoneEditing;
    private ImageView crosshair;
    private LinearLayout layoutControls, layoutEditFloorplan;
    private SeekBar seekbarSize, seekbarRotation;

    // Estado e Lógica
    private boolean isEditingFloorplan = false;
    private boolean isInitialCameraMoveDone = false;
    private float initialOverlayWidth;

    // Georreferenciamento
    private enum GeoreferenceState {
        NONE,
        AWAITING_FIRST_MAP_POINT, AWAITING_FIRST_IMAGE_POINT,
        AWAITING_SECOND_MAP_POINT, AWAITING_SECOND_IMAGE_POINT,
        COMPLETE
    }
    private GeoreferenceState currentState = GeoreferenceState.NONE;
    private Bitmap floorplanBitmap;
    private LatLng firstMapPoint, secondMapPoint;
    private PointF firstImagePoint, secondImagePoint;

    // --- Constantes ---
    private static final float MAX_ZOOM_LEVEL = 21.0f; // Nível de zoom máximo (ruas=15, prédios=20, satélite=21/22)

    // Launchers de Atividade
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    enableLocationFeatures();
                } else {
                    Toast.makeText(this, "Permissão de localização é essencial.", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String> selectImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        floorplanBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                        currentState = GeoreferenceState.AWAITING_FIRST_MAP_POINT;
                        updateUiForState();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Não foi possível carregar a imagem.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configuração inicial do ViewModel e dados do Survey
        viewModel = new ViewModelProvider(this).get(SurveyViewModel.class);
        long surveyId = getIntent().getLongExtra(MainActivity.EXTRA_SURVEY_ID, -1L);
        String surveyName = getIntent().getStringExtra(MainActivity.EXTRA_SURVEY_NAME);
        setTitle(surveyName);

        if (surveyId == -1L) {
            Toast.makeText(this, "Erro: ID do Survey inválido.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        viewModel.setCurrentSurveyId(surveyId);

        initializeViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        setupObservers();
        setupClickListeners();
    }

    private void initializeViews() {
        startStopButton = findViewById(R.id.btn_start_stop);
        generateHeatmapButton = findViewById(R.id.btn_generate_heatmap);
        btnConfirmPoint = findViewById(R.id.btn_confirm_point);
        btnPlaceFloorplan = findViewById(R.id.btn_place_floorplan);
        crosshair = findViewById(R.id.image_view_crosshair);
        layoutControls = findViewById(R.id.layout_controls);
        // Views do Modo de Edição
        layoutEditFloorplan = findViewById(R.id.layout_edit_floorplan);
        seekbarSize = findViewById(R.id.seekbar_size);
        seekbarRotation = findViewById(R.id.seekbar_rotation);
        btnEditFloorplan = findViewById(R.id.btn_edit_floorplan);
        btnDoneEditing = findViewById(R.id.btn_done_editing);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        googleMap.setOnCameraMoveListener(this);

        // Zoom fixo
        googleMap.getUiSettings().setZoomGesturesEnabled(false);
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.setMinZoomPreference(MAX_ZOOM_LEVEL);
        googleMap.setMaxZoomPreference(MAX_ZOOM_LEVEL);

        checkLocationPermission();
    }

    @Override
    public void onCameraMove() {
        if (isEditingFloorplan && floorplanOverlay != null) {
            floorplanOverlay.setPosition(googleMap.getCameraPosition().target);
        }
    }

    private void checkLocationPermission() {
        if (PermissionUtils.isLocationPermissionGranted(this)) {
            enableLocationFeatures();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("MissingPermission")
    private void enableLocationFeatures() {
        if (googleMap != null) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);

            // Centralizar mapa na ultima localização
            if (!isInitialCameraMoveDone) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null && !isInitialCameraMoveDone) {
                        // Move a câmera para a última localização conhecida
                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(location.getLatitude(), location.getLongitude()), MAX_ZOOM_LEVEL
                        ));
                        isInitialCameraMoveDone = true;
                    } else {
                        Toast.makeText(this, "Não foi possível obter a localização inicial.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void setupObservers() {
        viewModel.getIsTracking().observe(this, isTracking -> {
            startStopButton.setText(isTracking ? "Parar" : "Iniciar");
            generateHeatmapButton.setVisibility(isTracking ? View.GONE : View.VISIBLE);
            btnPlaceFloorplan.setVisibility(isTracking ? View.GONE : View.VISIBLE);
            btnEditFloorplan.setVisibility(isTracking ? View.GONE : View.VISIBLE);
        });

        viewModel.getLiveLocation().observe(this, location -> {
            //if (location != null && !isInitialCameraMoveDone) {
            //    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
            //           new LatLng(location.getLatitude(), location.getLongitude()), 19f
            //    ));
            //    isInitialCameraMoveDone = true;
            //}

            if (viewModel.getIsTracking().getValue() != null && viewModel.getIsTracking().getValue()) {
                viewModel.recordDataPoint(location);
            }
        });
    }

    private void setupClickListeners() {
        startStopButton.setOnClickListener(v -> {
            if (PermissionUtils.isLocationPermissionGranted(this)) {
                viewModel.toggleTracking();
            } else {
                Toast.makeText(this, "Conceda a permissão de localização para iniciar.", Toast.LENGTH_SHORT).show();
                checkLocationPermission();
            }
        });

        generateHeatmapButton.setOnClickListener(v -> drawHeatmap());

        btnPlaceFloorplan.setOnClickListener(v -> {
            currentState = GeoreferenceState.NONE;
            if (floorplanOverlay != null) floorplanOverlay.remove();
            selectImageLauncher.launch("image/*");
        });

        btnConfirmPoint.setOnClickListener(v -> handleConfirmPoint());
        btnEditFloorplan.setOnClickListener(v -> enterEditMode());
        btnDoneEditing.setOnClickListener(v -> exitEditMode());
    }

    private void handleConfirmPoint() {
        if (googleMap == null) return;
        LatLng centerPoint = googleMap.getCameraPosition().target;

        if (currentState == GeoreferenceState.AWAITING_FIRST_MAP_POINT) {
            firstMapPoint = centerPoint;
            currentState = GeoreferenceState.AWAITING_FIRST_IMAGE_POINT;
            showFloorplanPointDialog();
        } else if (currentState == GeoreferenceState.AWAITING_SECOND_MAP_POINT) {
            secondMapPoint = centerPoint;
            currentState = GeoreferenceState.AWAITING_SECOND_IMAGE_POINT;
            showFloorplanPointDialog();
        }
    }

    private void showFloorplanPointDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_image_point, null);
        ImageView imageView = dialogView.findViewById(R.id.image_view_floorplan_dialog);
        imageView.setImageBitmap(floorplanBitmap);

        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        imageView.setOnTouchListener((v, event) -> {
            if (currentState == GeoreferenceState.AWAITING_FIRST_IMAGE_POINT) {
                firstImagePoint = new PointF(event.getX(), event.getY());
                currentState = GeoreferenceState.AWAITING_SECOND_MAP_POINT;
            } else if (currentState == GeoreferenceState.AWAITING_SECOND_IMAGE_POINT) {
                secondImagePoint = new PointF(event.getX(), event.getY());
                calculateAndPlaceOverlay();
                currentState = GeoreferenceState.COMPLETE;
            }
            dialog.dismiss();
            updateUiForState();
            return true;
        });

        dialog.show();
    }

    private void updateUiForState() {
        runOnUiThread(() -> {
            crosshair.setVisibility(View.GONE);
            btnConfirmPoint.setVisibility(View.GONE);
            layoutControls.setVisibility(View.VISIBLE);

            if (currentState == GeoreferenceState.AWAITING_FIRST_MAP_POINT ||
                    currentState == GeoreferenceState.AWAITING_SECOND_MAP_POINT) {
                crosshair.setVisibility(View.VISIBLE);
                btnConfirmPoint.setVisibility(View.VISIBLE);
                layoutControls.setVisibility(View.GONE);
                Toast.makeText(this, "Posicione a mira no ponto de referência do mapa", Toast.LENGTH_LONG).show();
            } else if (currentState == GeoreferenceState.COMPLETE) {
                Toast.makeText(this, "Planta posicionada com sucesso!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateAndPlaceOverlay() {
        double mapDistance = SphericalUtil.computeDistanceBetween(firstMapPoint, secondMapPoint);
        double imageDistance = Math.sqrt(Math.pow(secondImagePoint.x - firstImagePoint.x, 2) + Math.pow(secondImagePoint.y - firstImagePoint.y, 2));
        double scale = mapDistance / imageDistance;
        float overlayWidth = (float) (floorplanBitmap.getWidth() * scale);

        double mapBearing = SphericalUtil.computeHeading(firstMapPoint, secondMapPoint);
        double imageBearing = Math.toDegrees(Math.atan2(secondImagePoint.x - firstImagePoint.x, firstImagePoint.y - secondImagePoint.y));
        float overlayBearing = (float) (mapBearing - imageBearing);

        PointF vectorToCenter = new PointF(floorplanBitmap.getWidth() / 2f - firstImagePoint.x, floorplanBitmap.getHeight() / 2f - firstImagePoint.y);
        double distToCenterMeters = Math.sqrt(Math.pow(vectorToCenter.x, 2) + Math.pow(vectorToCenter.y, 2)) * scale;
        double angleToCenterDegrees = Math.toDegrees(Math.atan2(vectorToCenter.x, -vectorToCenter.y));
        double centerBearing = mapBearing + (angleToCenterDegrees - imageBearing);
        LatLng overlayCenter = SphericalUtil.computeOffset(firstMapPoint, distToCenterMeters, centerBearing);

        GroundOverlayOptions options = new GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(floorplanBitmap))
                .position(overlayCenter, overlayWidth)
                .bearing(overlayBearing)
                .transparency(0.4f);

        floorplanOverlay = googleMap.addGroundOverlay(options);
        btnEditFloorplan.setVisibility(View.VISIBLE);
    }

    private void enterEditMode() {
        if (floorplanOverlay == null) {
            Toast.makeText(this, "Posicione a planta primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        isEditingFloorplan = true;
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(floorplanOverlay.getPosition()));

        layoutEditFloorplan.setVisibility(View.VISIBLE);
        layoutControls.setVisibility(View.GONE);
        crosshair.setVisibility(View.VISIBLE); // A mira agora serve para arrastar

        initialOverlayWidth = floorplanOverlay.getWidth();
        seekbarSize.setProgress(100);
        seekbarRotation.setProgress((int) floorplanOverlay.getBearing());

        setupFloorplanManipulationControls();
    }

    private void exitEditMode() {
        isEditingFloorplan = false;
        layoutEditFloorplan.setVisibility(View.GONE);
        layoutControls.setVisibility(View.VISIBLE);
        crosshair.setVisibility(View.GONE);
    }

    private void setupFloorplanManipulationControls() {
        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && floorplanOverlay != null) {
                    if (seekBar.getId() == R.id.seekbar_size) {
                        float scale = progress / 100.0f;
                        floorplanOverlay.setDimensions(initialOverlayWidth * scale);
                    } else if (seekBar.getId() == R.id.seekbar_rotation) {
                        floorplanOverlay.setBearing(progress);
                    }
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
        seekbarSize.setOnSeekBarChangeListener(listener);
        seekbarRotation.setOnSeekBarChangeListener(listener);
    }

    // SUBSTITUA SEU drawHeatmap POR ESTE:
    private void drawHeatmap() {
        if (heatmapOverlay != null) {
            heatmapOverlay.remove();
        }

        viewModel.getDataPointsForSurvey().observe(this, dataPoints -> {
            if (dataPoints == null || dataPoints.isEmpty()) {
                Toast.makeText(this, "Não há dados para gerar o mapa de calor.", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Gerando heatmap... Isso pode levar um momento.", Toast.LENGTH_SHORT).show();

            // 1. Definir os limites (com ou sem planta)
            final LatLngBounds bounds;
            final float width, height, bearing;

            if (floorplanOverlay != null) {
                try {
                    bounds = floorplanOverlay.getBounds();
                    width = floorplanOverlay.getWidth();
                    height = floorplanOverlay.getHeight();
                    bearing = floorplanOverlay.getBearing();
                } catch (Exception e) {
                    Toast.makeText(SurveyActivity.this, "Erro ao ler dados da planta.", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                bounds = getBoundsFromData(dataPoints);
                LatLng center = bounds.getCenter();
                width = (float) SphericalUtil.computeDistanceBetween(
                        new LatLng(center.latitude, bounds.southwest.longitude),
                        new LatLng(center.latitude, bounds.northeast.longitude));
                height = (float) SphericalUtil.computeDistanceBetween(
                        new LatLng(bounds.southwest.latitude, center.longitude),
                        new LatLng(bounds.northeast.latitude, center.longitude));
                bearing = 0f;
            }

            // 2. Roda a interpolação e o blur em uma nova thread
            new Thread(() -> {
                try {
                    if (width <= 0 || height <= 0) {
                        runOnUiThread(() -> Toast.makeText(SurveyActivity.this, "Erro: Área de heatmap inválida.", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    int resolutionX, resolutionY;
                    int baseResolution = 150;
                    if (width > height) {
                        resolutionX = baseResolution;
                        resolutionY = (int) ((height / width) * baseResolution);
                    } else {
                        resolutionY = baseResolution;
                        resolutionX = (int) ((width / height) * baseResolution);
                    }
                    if (resolutionX < 2) resolutionX = 2;
                    if (resolutionY < 2) resolutionY = 2;

                    // 3. Interpola os dados (Método B)
                    final double[][] interpolatedGrid = interpolateIdw(dataPoints, bounds, resolutionX, resolutionY);

                    // 4. Cria o bitmap "pixelado" (Método B)
                    final Bitmap heatmapBitmap = createHeatmapBitmap(interpolatedGrid);

                    // 5. APLICA O BLUR (A NOVA ETAPA)
                    final Bitmap blurredBitmap = blurBitmap(SurveyActivity.this, heatmapBitmap, 15.0f);

                    // 6. Adiciona o bitmap final ao mapa
                    runOnUiThread(() -> {
                        GroundOverlayOptions options = new GroundOverlayOptions()
                                .image(BitmapDescriptorFactory.fromBitmap(blurredBitmap)) // Usa o bitmap com blur
                                .positionFromBounds(bounds)
                                .bearing(bearing)
                                .transparency(0.3f);

                        heatmapOverlay = googleMap.addGroundOverlay(options);
                        Toast.makeText(SurveyActivity.this, "Mapa de calor gerado com " + dataPoints.size() + " pontos.", Toast.LENGTH_SHORT).show();
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(SurveyActivity.this, "Falha ao gerar heatmap: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }

            }).start();

            viewModel.getDataPointsForSurvey().removeObservers(this);
        });
    }


// --- COLE TODOS OS 5 MÉTODOS ABAIXO NO SEU ARQUIVO ---

    /**
     * MÉTODO FALTANDO 1:
     * Calcula o LatLngBounds (a caixa delimitadora) que contém todos os pontos de dados coletados.
     * Adiciona um pequeno "padding" para garantir que o heatmap não seja cortado nas bordas.
     */
    private LatLngBounds getBoundsFromData(List<DataPoint> dataPoints) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (DataPoint dp : dataPoints) {
            builder.include(new LatLng(dp.latitude, dp.longitude));
        }
        LatLngBounds bounds = builder.build();

        // Adiciona um pequeno padding (aprox. 10 metros)
        return bounds.including(new LatLng(bounds.northeast.latitude + 0.0001, bounds.northeast.longitude + 0.0001))
                .including(new LatLng(bounds.southwest.latitude - 0.0001, bounds.southwest.longitude - 0.0001));
    }


    /**
     * MÉTODO FALTANDO 2:
     * Interpola os dados de RSSI em uma grade usando Ponderação Inversa da Distância (IDW).
     */
    private double[][] interpolateIdw(List<DataPoint> dataPoints, LatLngBounds bounds, int gridWidth, int gridHeight) {
        double[][] grid = new double[gridHeight][gridWidth];
        double latStep = (bounds.northeast.latitude - bounds.southwest.latitude) / (gridHeight - 1);
        double lngStep = (bounds.northeast.longitude - bounds.southwest.longitude) / (gridWidth - 1);
        double power = 2.0;

        for (int y = 0; y < gridHeight; y++) {
            for (int x = 0; x < gridWidth; x++) {
                double currentLat = bounds.southwest.latitude + y * latStep;
                double currentLng = bounds.southwest.longitude + x * lngStep;
                LatLng currentPoint = new LatLng(currentLat, currentLng);

                double numerator = 0;
                double denominator = 0;
                boolean pointFoundNearby = false;

                for (DataPoint dp : dataPoints) {
                    LatLng dataPointLocation = new LatLng(dp.latitude, dp.longitude);
                    double distance = SphericalUtil.computeDistanceBetween(currentPoint, dataPointLocation);

                    if (distance == 0) {
                        numerator = dp.rssi;
                        denominator = 1;
                        pointFoundNearby = true;
                        break;
                    }
                    if (distance < 50) { // Raio de influência de 50m
                        double weight = 1.0 / Math.pow(distance, power);
                        numerator += weight * dp.rssi;
                        denominator += weight;
                        pointFoundNearby = true;
                    }
                }

                if (pointFoundNearby && denominator > 0) {
                    grid[y][x] = numerator / denominator;
                } else {
                    grid[y][x] = Double.NaN; // "Sem dados"
                }
            }
        }
        return grid;
    }

    /**
     * MÉTODO FALTANDO 3:
     * Cria um Bitmap a partir da grade interpolada, pintando cada pixel com a cor correta.
     */
    private Bitmap createHeatmapBitmap(double[][] grid) {
        int height = grid.length;
        int width = grid[0].length;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Invertemos Y
                int pixelIndex = (height - 1 - y) * width + x;
                double rssi = grid[y][x];
                pixels[pixelIndex] = getRssiColor(rssi);
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }


    /**
     * MÉTODO FALTANDO 4:
     * Retorna a cor ARGB exata para um determinado valor de RSSI.
     */
    private int getRssiColor(double rssi) {
        if (Double.isNaN(rssi)) {
            return Color.TRANSPARENT; // 0x00000000
        }

        int alpha = 170; // 0xAA

        if (rssi >= -35) return Color.argb(alpha, 0x00, 0xFF, 0x00); // Verde Forte
        if (rssi >= -40) return Color.argb(alpha, 0x40, 0xFF, 0x00);
        if (rssi >= -50) return Color.argb(alpha, 0x80, 0xFF, 0x00);
        if (rssi >= -55) return Color.argb(alpha, 0xFF, 0xFF, 0x00); // Amarelo
        if (rssi >= -60) return Color.argb(alpha, 0xFF, 0xBF, 0x00);
        if (rssi >= -65) return Color.argb(alpha, 0xFF, 0x80, 0x00);
        if (rssi >= -70) return Color.argb(alpha, 0xFF, 0x40, 0x00);
        if (rssi >= -80) return Color.argb(alpha, 0xFF, 0x00, 0x00); // Vermelho

        return Color.argb(alpha, 0x80, 0x00, 0x00); // Vermelho Escuro
    }

    /**
     * MÉTODO FALTANDO 5:
     * Aplica um efeito de "blur" (desfoque Gaussiano) em um Bitmap.
     */
    private Bitmap blurBitmap(Context context, Bitmap inputBitmap, float radius) {
        if (inputBitmap == null) return null;
        try {
            Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);
            RenderScript rs = RenderScript.create(context); // Erro 5 (Cannot apply) será corrigido pela importação do Context
            ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

            Allocation inAllocation = Allocation.createFromBitmap(rs, inputBitmap);
            Allocation outAllocation = Allocation.createFromBitmap(rs, outputBitmap);

            // Ajuste este valor (ex: 10f, 15f, 20f) para o visual desejado
            blurScript.setRadius(15.0f);

            blurScript.setInput(inAllocation);
            blurScript.forEach(outAllocation);
            outAllocation.copyTo(outputBitmap);

            rs.destroy();
            inAllocation.destroy();
            outAllocation.destroy();
            blurScript.destroy();

            return outputBitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return inputBitmap;
        }
    }
}