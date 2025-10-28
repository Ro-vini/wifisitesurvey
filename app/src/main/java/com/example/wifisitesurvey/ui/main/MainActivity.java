package com.example.wifisitesurvey.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wifisitesurvey.R;
import com.example.wifisitesurvey.data.model.Survey;
import com.example.wifisitesurvey.ui.glossary.GlossaryActivity;
import com.example.wifisitesurvey.ui.survey.SurveyActivity;
import com.example.wifisitesurvey.utils.EdgeToEdgeUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private MainViewModel mainViewModel;
    private WifiStateReceiver wifiStateReceiver;
    private TextView textViewStatus;
    private RecyclerView recyclerView;
    public static final String EXTRA_SURVEY_ID = "com.example.wifisitesurvey.EXTRA_SURVEY_ID";
    public static final String EXTRA_SURVEY_NAME = "com.example.wifisitesurvey.EXTRA_SURVEY_NAME";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View mainLayout = findViewById(R.id.main_container);
        EdgeToEdgeUtils.setupEdgeToEdge(mainLayout);

        textViewStatus = findViewById(R.id.text_view_status);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        final SurveyAdapter adapter = new SurveyAdapter();
        recyclerView.setAdapter(adapter);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mainViewModel.getSurveysForSsid().observe(this, adapter::submitList);
        mainViewModel.getStatusMessage().observe(this, status -> {
            if (status != null) {
                textViewStatus.setText(status);
                textViewStatus.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                if (!mainViewModel.isWifiWarningShown()) {
                    showWifiWarningDialog();
                    mainViewModel.setWifiWarningShown(true);
                }
            } else {
                textViewStatus.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });

        wifiStateReceiver = new WifiStateReceiver();

        FloatingActionButton fab = findViewById(R.id.fab_add_survey);
        fab.setOnClickListener(view -> showCreateSurveyDialog());

        ImageButton btnGlossary = findViewById(R.id.btn_glossary);
        btnGlossary.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GlossaryActivity.class);
            startActivity(intent);
        });

        adapter.setOnItemClickListener(survey -> {
            Intent intent = new Intent(MainActivity.this, SurveyActivity.class);
            intent.putExtra(EXTRA_SURVEY_ID, survey.id);
            intent.putExtra(EXTRA_SURVEY_NAME, survey.name);
            startActivity(intent);
        });

        adapter.setOnEditClickListener(this::showUpdateSurveyDialog);
        adapter.setOnDeleteClickListener(this::showDeleteConfirmationDialog);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiStateReceiver, intentFilter);
        updateCurrentSsid();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wifiStateReceiver);
    }

    private void updateCurrentSsid() {
        mainViewModel.setSsid(getCurrentSsid());
    }

    private String getCurrentSsid() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        if (ssid != null && !ssid.equals("<unknown ssid>")) {
            return ssid.replace("\"", "");
        }
        return null;
    }

    private void showWifiWarningDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Sem Conexão Wi-Fi")
                .setMessage("Por favor, conecte-se a uma rede Wi-Fi para visualizar e criar surveys.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showCreateSurveyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Novo Survey");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Ex: Escritório 3º Andar");
        builder.setView(input);

        builder.setPositiveButton("Criar e Iniciar", (dialog, which) -> {
            String surveyName = input.getText().toString();
            String ssid = getCurrentSsid();

            if (ssid == null) {
                Toast.makeText(this, "Não conectado a uma rede Wi-Fi.", Toast.LENGTH_LONG).show();
                return;
            }

            if (!surveyName.trim().isEmpty()) {
                long newSurveyId = mainViewModel.createNewSurvey(surveyName, ssid);

                if (newSurveyId >= 0) {
                    Intent intent = new Intent(MainActivity.this, SurveyActivity.class);
                    intent.putExtra(EXTRA_SURVEY_ID, newSurveyId);
                    intent.putExtra(EXTRA_SURVEY_NAME, surveyName);
                    startActivity(intent);
                } else if (newSurveyId == MainViewModel.SURVEY_EXISTS) {
                    Toast.makeText(this, "Já existe um survey com este nome para o SSID atual.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Erro ao criar o survey.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showUpdateSurveyDialog(Survey survey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Survey");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(survey.name);
        builder.setView(input);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String surveyName = input.getText().toString();
            if (!surveyName.trim().isEmpty()) {
                Survey updatedSurvey = new Survey();
                updatedSurvey.id = survey.id;
                updatedSurvey.name = surveyName;
                updatedSurvey.ssid = survey.ssid;
                updatedSurvey.creationTimestamp = survey.creationTimestamp;
                mainViewModel.update(updatedSurvey);
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showDeleteConfirmationDialog(Survey survey) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Survey")
                .setMessage("Tem certeza que deseja excluir o survey \"" + survey.name + "\"?")
                .setPositiveButton("Excluir", (dialog, which) -> mainViewModel.delete(survey))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private class WifiStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                updateCurrentSsid();
            }
        }
    }
}