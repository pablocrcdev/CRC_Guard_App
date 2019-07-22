package com.guard.security.crc.crc_guard_app.util;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;

import com.guard.security.crc.crc_guard_app.activities.LocalHomeActivity;
import com.guard.security.crc.crc_guard_app.activities.MainActivity;

/**
 * Clase para procesos compartidos entre activities u otras clases
 * Tambien contiene codigo de procesos que se necesitaron pero no se usaron más
 */
public class Procesos {
    /**
     * Solicita permisos al dispositivo, puede ser solicitado desde MainActivity(on-line) o LocalHomeActivity(off-line)
     * @param pContext Contexto de la app
     * @param Main Clase MainActivity (Puede ser null si viene desde LocalHomeActivity)
     * @param Local Clase LocalHome (Puede ser null si viene desde MainActivity
     */
    public void SolicitarPermisos(Context pContext, MainActivity Main, LocalHomeActivity Local) {
        Activity App;
        if (Main == null) {
            App = Local;
        } else {
            App = Main;
        }
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int check = ActivityCompat.checkSelfPermission(pContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (check == PackageManager.PERMISSION_GRANTED) {
                //Tengo permisos, con solo revisar un permiso ya sé que tengo los demás
            } else {
                ActivityCompat.requestPermissions(App, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE,
                        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE,
                        Manifest.permission.CAMERA}, 1);
            }
        }
    }

    /**
     * Proceso que obtiene el numero de pagina de una URL de apex
     * Sirve para ejecutar un proceso de javascript en una pagina en especifico.
     * Así no se ejecuta ese proceso en cualquier pagina provocando llamadas innecesarias a JS.
     *
     * @param url Url de la pagina a la que se accesa.
     * @return Numero de pagina destino (segun formato de link en apex.
     */
    public String Num_Pagina(String url) {
        String Resultado = "";
        try {
            String[] UrlS = url.split("=");
            for (int i = 1; i < 2; i++) {
                String[] Procesado = UrlS[i].split(":");
                for (int p = 1; p < 2; p++) {
                    Resultado = Procesado[i];
                    break;
                }
            }
        } catch (Exception io) {
        }
        return Resultado;
    }

    /**
     * Proceso que obtiene el numero se sesión asignada por APEX, no se puede usar antes de hacer login
     * Se recomienda usarlo al entrar en la pagina 1 (por ejemplo) ya que en ese punto si tengo una sesión.
     *
     * @param Url Url de la pagina destino en APEX.
     * @return La sesión que me asignó apex despues de login.
     */
    public String Obt_sesion__URL(String Url) {
        String[] url = Url.split("=");
        String Resultado = "";
        for (int i = 1; i < url.length; i++) {
            Resultado = url[i].split(":")[2];
            break;
        }
        return Resultado;
    }

    /**
     * Proceso que obtiene el numero serial del tag que se está leyendo.
     *
     * @param tagId Información del tag.
     * @return Serial del tag (valor unico)
     */
    public String getTagSerial_number(byte[] tagId) {
        String hexdump = null;
        for (int i = 0; i < tagId.length; i++) {
            String x = Integer.toHexString(((int) tagId[i] & 0xff));
            if (x.length() == 1) {
                x = '0' + x;
            }
            if (hexdump == null) {
                hexdump = x;
            } else {
                hexdump += ':' + x;
            }
        }
        return hexdump;
    }

}
