package com.example.wifisitesurvey.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.example.wifisitesurvey.data.model.Floorplan;

@Dao
public interface FloorplanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(Floorplan floorplan);

    @Query("SELECT * FROM floorplans WHERE surveyId = :surveyId LIMIT 1")
    LiveData<Floorplan> getFloorplanForSurvey(long surveyId);

    @Query("DELETE FROM floorplans WHERE surveyId = :surveyId")
    void deleteBySurveyId(long surveyId);
}