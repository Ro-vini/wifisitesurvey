package com.example.wifisitesurvey.ui.main;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.wifisitesurvey.data.model.Survey;
import com.example.wifisitesurvey.data.repository.SurveyRepository;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * ViewModel para a MainActivity.
 * Fornece os dados para a UI e sobrevive a mudanças de configuração.
 */
public class MainViewModel extends AndroidViewModel {

    private final SurveyRepository repository;
    private final LiveData<List<Survey>> allSurveys;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new SurveyRepository(application);
        allSurveys = repository.getAllSurveys();
    }

    /**
     * Retorna um LiveData contendo a lista de todos os surveys.
     * A UI pode observar este LiveData para se atualizar automaticamente.
     */
    public LiveData<List<Survey>> getAllSurveys() {
        return allSurveys;
    }

    /**
     * Cria um novo survey no banco de dados.
     * @param name O nome para o novo survey.
     * @return O ID do survey recém-criado, ou -1 em caso de falha.
     */
    public long createNewSurvey(String name) {
        Survey newSurvey = new Survey(name);
        Future<Long> insertFuture = repository.insertSurvey(newSurvey);
        try {
            // Espera a conclusão da inserção para obter o ID retornado pelo DAO.
            // Isso é necessário para navegar para a tela de survey com o ID correto.
            return insertFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt(); // Restaura o status de interrupção
            return -1L; // Retorna -1 em caso de falha
        }
    }
}