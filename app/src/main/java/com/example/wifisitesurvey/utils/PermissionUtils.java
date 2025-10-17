package com.example.wifisitesurvey.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Classe auxiliar para simplificar a verificação e solicitação de permissões em tempo de execução.
 */
public final class PermissionUtils {

    // Previne a instanciação desta classe de utilidades.
    private PermissionUtils() {}

    /**
     * Verifica se a permissão de localização fina (GPS) já foi concedida ao aplicativo.
     *
     * @param context O contexto para verificar a permissão.
     * @return true se a permissão foi concedida, false caso contrário.
     */
    public static boolean isLocationPermissionGranted(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * @param activity A Activity que está solicitando a permissão.
     * @return true se uma justificativa deve ser mostrada, false caso contrário.
     */
    public static boolean shouldShowLocationPermissionRationale(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    /**
     * @param context O contexto para exibir o diálogo.
     * @param positiveClickListener O listener a ser executado quando o usuário clica em "OK".
     * Normalmente, isso acionará a solicitação de permissão real.
     */
    public static void showLocationRationaleDialog(Context context, DialogInterface.OnClickListener positiveClickListener) {
        new AlertDialog.Builder(context)
                .setTitle("Permissão de Localização Necessária")
                .setMessage("Este aplicativo precisa da sua localização para mapear a intensidade do sinal Wi-Fi em pontos geográficos precisos. Por favor, conceda a permissão para continuar.")
                .setPositiveButton("OK", positiveClickListener)
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}