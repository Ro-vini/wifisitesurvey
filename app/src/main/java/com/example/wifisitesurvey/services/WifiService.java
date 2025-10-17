package com.example.wifisitesurvey.services;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 * Classe de serviço dedicada a interagir com o WifiManager do Android.
 * Encapsula a lógica para obter informações da rede Wi-Fi, como a intensidade do sinal (RSSI).
 */
public class WifiService {

    private static final String TAG = "WifiService";
    private final WifiManager wifiManager;

    /**
     * Construtor que inicializa o WifiManager a partir do contexto da aplicação.
     * @param context O contexto da aplicação.
     */
    public WifiService(Context context) {
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    /**
     * Obtém a intensidade do sinal (RSSI) da rede Wi-Fi atualmente conectada.
     * <p>
     * <b>Importante:</b> A partir do Android 9 (API 28), o aplicativo precisa ter a
     * permissão {@link android.Manifest.permission#ACCESS_FINE_LOCATION} concedida
     * para que este método retorne um valor válido.
     * </p>
     * @return O valor do RSSI em dBm, ou -100 se o Wi-Fi estiver desabilitado,
     * não conectado, ou se ocorrer um erro.
     */
    public int getCurrentRssi() {
        if (!wifiManager.isWifiEnabled()) {
            Log.w(TAG, "Wi-Fi is disabled. Cannot get RSSI.");
            return -100;
        }

        try {
            // A informação da conexão contém o valor do RSSI
            return wifiManager.getConnectionInfo().getRssi();
        } catch (Exception e) {
            Log.e(TAG, "Could not get RSSI value.", e);
            // Retorna um valor baixo em caso de qualquer exceção
            return -100;
        }
    }
}