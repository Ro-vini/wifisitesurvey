package com.example.wifisitesurvey.utils;

/**
 * Classe de utilidades para armazenar constantes globais usadas em todo o aplicativo..
 */
public final class Constants {

    // Previne a instanciação desta classe de utilidades.
    private Constants() {}

    /**
     * Chave usada para passar o ID de um Survey via Intent para a SurveyActivity.
     */
    public static final String EXTRA_SURVEY_ID = "com.example.wifisitesurvey.EXTRA_SURVEY_ID";

    /**
     * Chave usada para passar o NOME de um Survey via Intent para a SurveyActivity.
     */
    public static final String EXTRA_SURVEY_NAME = "com.example.wifisitesurvey.EXTRA_SURVEY_NAME";

}