package com.example.wifisitesurvey.data.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.wifisitesurvey.data.model.DataPoint;
import com.example.wifisitesurvey.data.model.Survey;

/**
 * A classe principal do banco de dados Room para o aplicativo.
 * Esta classe é um singleton para prevenir que múltiplas instâncias do banco
 * sejam abertas ao mesmo tempo.
 */
@Database(entities = {Survey.class, DataPoint.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract SurveyDao surveyDao();

    private static volatile AppDatabase INSTANCE;

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