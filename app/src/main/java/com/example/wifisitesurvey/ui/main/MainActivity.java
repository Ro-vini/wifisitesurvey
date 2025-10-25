package com.example.wifisitesurvey.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.wifisitesurvey.R;
import com.example.wifisitesurvey.ui.glossary.GlossaryActivity;
import com.example.wifisitesurvey.ui.survey.SurveyActivity;
import com.example.wifisitesurvey.utils.EdgeToEdgeUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    private MainViewModel mainViewModel;
    public static final String EXTRA_SURVEY_ID = "com.example.wifisitesurvey.EXTRA_SURVEY_ID";
    public static final String EXTRA_SURVEY_NAME = "com.example.wifisitesurvey.EXTRA_SURVEY_NAME";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View mainLayout = findViewById(R.id.main_container);
        EdgeToEdgeUtils.setupEdgeToEdge(mainLayout);

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        // Assumindo que você criou a classe SurveyAdapter
        final SurveyAdapter adapter = new SurveyAdapter();
        recyclerView.setAdapter(adapter);

        mainViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        mainViewModel.getAllSurveys().observe(this, adapter::submitList);

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
            if (!surveyName.trim().isEmpty()) {
                long newSurveyId = mainViewModel.createNewSurvey(surveyName);
                if (newSurveyId != -1) {
                    Intent intent = new Intent(MainActivity.this, SurveyActivity.class);
                    intent.putExtra(EXTRA_SURVEY_ID, newSurveyId);
                    intent.putExtra(EXTRA_SURVEY_NAME, surveyName);
                    startActivity(intent);
                }
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }
}