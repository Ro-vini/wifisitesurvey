package com.example.wifisitesurvey.ui.survey;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.wifisitesurvey.R;
import com.example.wifisitesurvey.data.model.DataPoint;
import com.example.wifisitesurvey.services.WifiService;
import com.example.wifisitesurvey.ui.main.MainActivity;
import com.example.wifisitesurvey.utils.EdgeToEdgeUtils;
import com.example.wifisitesurvey.utils.PermissionUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import android.graphics.Color;
import com.google.maps.android.SphericalUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.provider.MediaStore;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

// Adicione esta importação no topo (necessária para os novos métodos)
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
    private Uri floorplanImageUri;
    private List<Circle> realTimeCircles = new ArrayList<>();
    private FusedLocationProviderClient fusedLocationClient; // << ADICIONE ESTA LINHA
    private WifiService wifiService;
    private CountDownTimer buttonCountdown;

    // Views do Layout
    private Button startStopButton, generateHeatmapButton, btnConfirmPoint;
    private Button btnPlaceFloorplan, btnEditFloorplan, btnDoneEditing;
    private ImageView crosshair;
    private LinearLayout layoutControls, layoutEditFloorplan;
    private SeekBar seekbarSize, seekbarRotation;
    private TextView textViewRssi;
    private TextView textViewSpeed;

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
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        this.floorplanImageUri = uri; // Guardar a URI

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

        View mainLayout = findViewById(R.id.survey_container);
        EdgeToEdgeUtils.setupEdgeToEdge(mainLayout);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        wifiService = new WifiService(this);

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
        textViewRssi = findViewById(R.id.textRssi);
        textViewSpeed = findViewById(R.id.textSpeed);

        // Views do Modo de Edição
        layoutEditFloorplan = findViewById(R.id.layout_edit_floorplan);
        seekbarSize = findViewById(R.id.seekbar_size);
        seekbarRotation = findViewById(R.id.seekbar_rotation);
        btnEditFloorplan = findViewById(R.id.btn_edit_floorplan);
        btnDoneEditing = findViewById(R.id.btn_done_editing);

        btnEditFloorplan.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        startUnlockButtonCountdown(10000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Cancela o contador se o utilizador sair da atividade
        if (buttonCountdown != null) {
            buttonCountdown.cancel();
            buttonCountdown = null;
        }
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

        drawHeatmap();

        loadSavedFloorplan();
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

    /**
     * Inicia uma contagem decrescente de 5 segundos no botão Iniciar/Parar,
     * mostrando o tempo restante.
     */
    private void startUnlockButtonCountdown(int millisPause) {
        // Cancela qualquer contador anterior, se estiver a decorrer
        if (buttonCountdown != null) {
            buttonCountdown.cancel();
        }

        startStopButton.setEnabled(false); // Desativa o botão

        buttonCountdown = new CountDownTimer(millisPause, 1000) { // 5000ms total, 1000ms de intervalo
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                if (startStopButton != null) {
                    // Arredonda para cima para mostrar "5" em vez de "4" no início
                    long seconds = (long) Math.round(millisUntilFinished / 1000.0);
                    startStopButton.setText("Aguarde (" + seconds + "s)");
                }
            }

            @Override
            public void onFinish() {
                if (startStopButton != null) {
                    buttonCountdown = null; // Limpa a referência

                    // Restaura o texto original (Iniciar ou Parar)
                    Boolean isTracking = viewModel.getIsTracking().getValue();
                    startStopButton.setText((isTracking != null && isTracking) ? "Parar" : "Iniciar");

                    startStopButton.setEnabled(true); // Reativa o botão
                }
            }
        };

        buttonCountdown.start(); // Inicia o contador
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
            textViewRssi.setVisibility(isTracking ? View.VISIBLE : View.GONE);
            textViewSpeed.setVisibility(isTracking ? View.VISIBLE : View.GONE);

            if (isTracking) {
                // Se está fazendo tracking, esconda sempre os botões de planta
                btnPlaceFloorplan.setVisibility(View.GONE);
                btnEditFloorplan.setVisibility(View.GONE);
            } else {
                btnPlaceFloorplan.setVisibility(View.VISIBLE);
                // Se NÃO está fazendo tracking, mostre o botão APENAS se a planta existir
                btnEditFloorplan.setVisibility(floorplanOverlay != null ? View.VISIBLE : View.GONE);

                // Se o tracking parou, limpa os círculos de tempo real
                if (realTimeCircles != null && !realTimeCircles.isEmpty()) {
                    for (Circle circle : realTimeCircles) {
                        circle.remove();
                    }
                    realTimeCircles.clear();
                }
            }
        });

        viewModel.getLiveLocation().observe(this, location -> {
            if (viewModel.getIsTracking().getValue() != null && viewModel.getIsTracking().getValue()) {
                viewModel.recordDataPoint(location);

                int rssi = -100; // Valor padrão
                WifiInfo wifiInfo = wifiService.getCurrentConnection();
                if (wifiInfo != null) {
                    rssi = wifiInfo.getRssi();
                    int linkSpeed = wifiInfo.getLinkSpeed();
                    textViewRssi.setText(String.format("RSSI\n %d dBm", rssi));
                    textViewSpeed.setText(String.format("Speed\n %d Mbps", linkSpeed));
                }

                // Esta é a forma leve de dar feedback em tempo real.
                // Não chame drawHeatmap() aqui!
                if (googleMap != null) {
                    LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());

                    // Reutiliza sua função de cor para o círculo
                    int pointColor = getRssiColor(rssi);

                    CircleOptions circleOptions = new CircleOptions()
                            .center(newPoint)
                            .radius(0.3) // Raio pequeno (ex: 30cm)
                            .strokeWidth(0) // Sem borda
                            .fillColor(pointColor); // Usa a cor do RSSI

                    // Adiciona o círculo ao mapa e à nossa lista
                    realTimeCircles.add(googleMap.addCircle(circleOptions));
                }
            }
        });
    }

    private void setupClickListeners() {
        startStopButton.setOnClickListener(v -> {
            if (PermissionUtils.isLocationPermissionGranted(this)) {
                viewModel.toggleTracking();
                startStopButton.setEnabled(false); // desativa imediatamente

                startUnlockButtonCountdown(5000);
            } else {
                Toast.makeText(this, "Conceda a permissão de localização para iniciar.", Toast.LENGTH_SHORT).show();
                checkLocationPermission();
            }
        });

        generateHeatmapButton.setOnClickListener(v -> drawHeatmap());

        btnPlaceFloorplan.setOnClickListener(v -> {
            clearHeatmap(); // Limpa o heatmap anterior

            currentState = GeoreferenceState.NONE;
            if (floorplanOverlay != null) {
                floorplanOverlay.remove();
                floorplanOverlay = null;
                btnEditFloorplan.setVisibility(View.GONE);
            }

            viewModel.clearFloorplanData();
            this.floorplanImageUri = null;
            this.floorplanBitmap = null;

            selectImageLauncher.launch("image/*");
        });

        btnConfirmPoint.setOnClickListener(v -> handleConfirmPoint());
        btnEditFloorplan.setOnClickListener(v -> enterEditMode());
        btnDoneEditing.setOnClickListener(v -> exitEditMode());
    }

    /**
     * Guarda o estado atual da planta (posição, tamanho, rotação e URI da imagem)
     * através do ViewModel.
     */
    private void saveCurrentFloorplanState() {
        if (floorplanOverlay == null || floorplanImageUri == null) {
            // Não há nada para guardar
            return;
        }

        LatLng center = floorplanOverlay.getPosition();
        float width = floorplanOverlay.getWidth();
        float bearing = floorplanOverlay.getBearing();
        String uriString = floorplanImageUri.toString();

        // Informa o ViewModel para guardar estes dados na base de dados
        viewModel.saveFloorplanData(uriString, center, width, bearing);
    }

    /**
     * Tenta carregar os dados da planta guardada (associada a este surveyId)
     * a partir do ViewModel e recriá-la no mapa.
     */
    private void loadSavedFloorplan() {
        // Observa o ViewModel. O ViewModel deve ser responsável por ir buscar
        // os dados da planta à base de dados com base no currentSurveyId.
        // (Terá de implementar getFloorplanForSurvey() no seu ViewModel)
        viewModel.getFloorplanForSurvey().observe(this, floorplanData -> {
            // Verifica se os dados existem e se o mapa está pronto
            if (floorplanData == null || floorplanData.imageUri == null || googleMap == null) {
                return;
            }

            try {
                // 1. Obter a URI e re-adquirir permissão de leitura
                Uri uri = Uri.parse(floorplanData.imageUri);
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                getContentResolver().takePersistableUriPermission(uri, takeFlags);

                // 2. Carregar o Bitmap
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);

                // 3. Recriar as opções do GroundOverlay
                GroundOverlayOptions options = new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                        .position(new LatLng(floorplanData.latitude, floorplanData.longitude), floorplanData.width)
                        .bearing(floorplanData.bearing)
                        .transparency(0.4f);

                // 4. Adicionar ao mapa e guardar referências locais
                floorplanOverlay = googleMap.addGroundOverlay(options);
                floorplanImageUri = uri;
                floorplanBitmap = bitmap;

                // 5. Atualizar a UI
                // Verifica se não estamos a fazer tracking antes de mostrar o botão
                Boolean isTracking = viewModel.getIsTracking().getValue();
                if (isTracking == null || !isTracking) {
                    btnEditFloorplan.setVisibility(View.VISIBLE);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Falha ao recarregar a planta guardada.", Toast.LENGTH_SHORT).show();
                // Se falhar, limpa os dados para evitar loops
                viewModel.clearFloorplanData();
            }

            // Remove o observer. Isto é uma operação de "load" que só acontece uma vez.
            viewModel.getFloorplanForSurvey().removeObservers(this);
        });
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

        Boolean isTracking = viewModel.getIsTracking().getValue();
        if (isTracking == null || !isTracking) {
            btnEditFloorplan.setVisibility(View.VISIBLE);
        }

        saveCurrentFloorplanState(); // Guarda o estado inicial
        drawHeatmap(); // Gera o heatmap pela primeira vez com a planta
        Toast.makeText(this, "Planta posicionada. Pode ajustar em 'Editar'.", Toast.LENGTH_LONG).show();
    }

    private void enterEditMode() {
        if (floorplanOverlay == null) {
            Toast.makeText(this, "Posicione a planta primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }

        isEditingFloorplan = true;

        viewModel.clearFloorplanData();
        clearHeatmap(); // Limpa o heatmap para ver a planta claramente

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

        saveCurrentFloorplanState(); // Guarda o estado final
        drawHeatmap(); // Redesenha o heatmap com as novas dimensões/posição
        Toast.makeText(this, "Ajustes da planta guardados.", Toast.LENGTH_SHORT).show();
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

    /**
     * Remove a sobreposição do mapa de calor, se existir.
     */
    private void clearHeatmap() {
        if (heatmapOverlay != null) {
            heatmapOverlay.remove();
            heatmapOverlay = null;
        }
    }

    private void drawHeatmap() {
        clearHeatmap(); // Garante que qualquer heatmap antigo seja removido primeiro

        viewModel.getDataPointsForSurvey().observe(this, dataPoints -> {
            if (dataPoints == null || dataPoints.isEmpty()) {
                // Toast.makeText(this, "Não há dados para gerar o mapa de calor.", Toast.LENGTH_SHORT).show();
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

        // --- CORREÇÃO PRINCIPAL AQUI ---
        // Nosso raio de influência no IDW é de 50 metros.
        // Vamos adicionar um padding de 60 metros (aprox 0.00055 graus)
        // para garantir que a "caixa" tenha uma borda transparente.
        double padding = 0.00055;

        return bounds.including(new LatLng(bounds.northeast.latitude + padding, bounds.northeast.longitude + padding))
                .including(new LatLng(bounds.southwest.latitude - padding, bounds.southwest.longitude - padding));
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
                    if (distance < 2) { // ALTERAR AQUI TAMANHO DAS MEDIÇÕES HEATMAP (BOLOTAS)
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
        // Se o valor for "sem dados" (NaN), retorna transparente
        if (Double.isNaN(rssi)) {
            return Color.TRANSPARENT; // 0x00000000
        }

        int baseAlpha = 170; // 0xAA (Nossa opacidade padrão)

        // --- 1. ZONA DE COR SÓLIDA ---
        // Se o sinal for -80dBm ou mais forte, retorna a cor sólida.
        if (rssi >= -35) return Color.argb(baseAlpha, 0x00, 0xFF, 0x00); // Verde Forte
        if (rssi >= -40) return Color.argb(baseAlpha, 0x40, 0xFF, 0x00);
        if (rssi >= -50) return Color.argb(baseAlpha, 0x80, 0xFF, 0x00);
        if (rssi >= -55) return Color.argb(baseAlpha, 0xFF, 0xFF, 0x00); // Amarelo
        if (rssi >= -60) return Color.argb(baseAlpha, 0xFF, 0xBF, 0x00);
        if (rssi >= -65) return Color.argb(baseAlpha, 0xFF, 0x80, 0x00);
        if (rssi >= -70) return Color.argb(baseAlpha, 0xFF, 0x40, 0x00);
        if (rssi >= -80) return Color.argb(baseAlpha, 0xFF, 0x00, 0x00); // Vermelho


        // --- 2. ZONA DE DESBOTAMENTO (FADE-OUT) ---
        // Se chegamos aqui, o RSSI está abaixo de -80 dBm.
        // Vamos desbotar do "Vermelho Escuro" para "Transparente"
        // na faixa de -80 dBm até -90 dBm.

        double fadeStartRssi = -80.0;
        double fadeEndRssi = -90.0; // Ponto onde fica 100% transparente

        if (rssi <= fadeEndRssi) {
            return Color.TRANSPARENT; // Abaixo de -90, totalmente transparente
        }

        // Estamos na zona de desbotamento (entre -80 e -90)
        // Mapeia o RSSI para uma porcentagem de 0.0 (em -90) a 1.0 (em -80)
        // Ex: RSSI de -85 -> ((-85) - (-90)) / ((-80) - (-90)) = 5 / 10 = 0.5 (50%)
        double percentage = (rssi - fadeEndRssi) / (fadeStartRssi - fadeEndRssi);

        // O novo alfa será uma porcentagem do nosso alfa base
        int newAlpha = (int) (baseAlpha * percentage);

        // A cor será a nossa cor mais fraca (Vermelho Escuro)
        return Color.argb(newAlpha, 0x80, 0x00, 0x00);
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

            // AJUSTAR ESSE VALOR PARA MEXER NO BLUR DOS HEATMAP GERADO
            blurScript.setRadius(10f);

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