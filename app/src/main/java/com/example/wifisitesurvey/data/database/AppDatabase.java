package com.example.wifisitesurvey.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.wifisitesurvey.data.model.DataPoint;
import com.example.wifisitesurvey.data.model.Floorplan;
import com.example.wifisitesurvey.data.model.Survey;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A classe principal do banco de dados Room para o aplicativo.
 * Esta classe é um singleton para prevenir que múltiplas instâncias do banco
 * sejam abertas ao mesmo tempo.
 */
@Database(entities = {Survey.class, DataPoint.class, Floorplan.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract SurveyDao surveyDao();
    public abstract FloorplanDao floorplanDao();

    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "wifi_survey_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}