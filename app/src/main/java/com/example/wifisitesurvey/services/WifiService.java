package com.example.wifisitesurvey.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;

/**
 * Classe de serviço dedicada a interagir com o WifiManager do Android.
 * Encapsula a lógica para obter informações da rede Wi-Fi, como a intensidade do sinal (RSSI).
 */
public class WifiService {

    private static final String TAG = "WifiService";
    private final WifiManager wifiManager;
    private final Context context;

    /**
     * Construtor que inicializa o WifiManager a partir do contexto da aplicação.
     * @param context O contexto da aplicação.
     */
    public WifiService(Context context) {
        this.context = context.getApplicationContext(); // Armazenar context
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
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

    public WifiInfo getCurrentConnection() {
        return wifiManager.getConnectionInfo();
    }

    public List<ScanResult> scanNetworks() {
        return wifiManager.getScanResults();
    }

    public DhcpInfo getDhcpInfo() {
        return wifiManager.getDhcpInfo();
    }

    public String getMobileIpAddress() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null)
            return "N/A (CM Indisponível)";

        try {
            for (Network network : cm.getAllNetworks()) {
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    LinkProperties linkProperties = cm.getLinkProperties(network);
                    if (linkProperties != null) {
                        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                            InetAddress address = linkAddress.getAddress();

                            // Garantir que é IPv4 e não loopback
                            if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                                return address.getHostAddress();
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            // Log.e("WifiReportFormatter", "Erro ao obter IP móvel: " + e.getMessage());
            return "N/A (Erro)";
        }

        return "N/A (Não encontrado)";
    }
}