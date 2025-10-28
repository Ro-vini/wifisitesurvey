package com.example.wifisitesurvey.ui.main;

import android.app.Application;
import android.database.sqlite.SQLiteConstraintException;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.wifisitesurvey.data.model.Survey;
import com.example.wifisitesurvey.data.repository.SurveyRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * ViewModel para a MainActivity.
 * Fornece os dados para a UI e sobrevive a mudanças de configuração.
 */
public class MainViewModel extends AndroidViewModel {

    private final SurveyRepository repository;
    private final MediatorLiveData<List<Survey>> surveysForSsid = new MediatorLiveData<>();
    private final MutableLiveData<String> currentSsid = new MutableLiveData<>();
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>();
    private LiveData<List<Survey>> currentSource;
    private boolean wifiWarningShown = false;

    public static final long SURVEY_EXISTS = -2;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new SurveyRepository(application);
        surveysForSsid.addSource(currentSsid, ssid -> {
            if (currentSource != null) {
                surveysForSsid.removeSource(currentSource);
            }
            if (ssid == null || ssid.isEmpty() || "<unknown ssid>".equals(ssid)) {
                statusMessage.setValue("Por favor, conecte-se a uma rede");
                surveysForSsid.setValue(new ArrayList<>());
            } else {
                statusMessage.setValue(null);
                wifiWarningShown = false; // Reset flag when connected
                currentSource = repository.getSurveysBySsid(ssid);
                surveysForSsid.addSource(currentSource, surveys -> surveysForSsid.setValue(surveys));
            }
        });
    }

    public LiveData<List<Survey>> getSurveysForSsid() {
        return surveysForSsid;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public boolean isWifiWarningShown() {
        return wifiWarningShown;
    }

    public void setWifiWarningShown(boolean shown) {
        this.wifiWarningShown = shown;
    }

    public void setSsid(String ssid) {
        currentSsid.setValue(ssid);
    }

    /**
     * Cria um novo survey no banco de dados.
     * @param name O nome para o novo survey.
     * @param ssid A rede conectada para atrelar o survey
     * @return O ID do survey recém-criado, ou -1 em caso de falha.
     */
    public long createNewSurvey(String name, String ssid) {
        Survey newSurvey = new Survey(name, ssid);
        Future<Long> insertFuture = repository.insertSurvey(newSurvey);
        try {
            // Espera a conclusão da inserção para obter o ID retornado pelo DAO.
            // Isso é necessário para navegar para a tela de survey com o ID correto.
            return insertFuture.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof SQLiteConstraintException) {
                return SURVEY_EXISTS;
            }
            e.printStackTrace();
            return -1L; // Retorna -1 em caso de falha
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restaura o status de interrupção
            e.printStackTrace();
            return -1L; // Retorna -1 em caso de falha
        }
    }

    public void update(Survey survey) {
        repository.updateSurvey(survey);
    }

    public void delete(Survey survey) {
        repository.deleteSurvey(survey);
    }
}