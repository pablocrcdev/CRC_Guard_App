package com.guard.security.crc.crc_guard_app.util;

import android.util.Log;

public class Procesos {

    public String Num_Pagina(String url) {
        String Resultado = "";
        try {
            String[] UrlS = url.split("=");
            for (int i = 1; i < 2; i++) {
                String[] Procesado = UrlS[i].split(":");
                for (int p = 1; p < 2; p++) {
                    Resultado = Procesado[i];
                }
            }
        } catch (Exception io) {
        }
        return Resultado;
    }

    public String Obt_sesion__URL(String Url) {
        String[] url = Url.split("=");
        String Resultado = "";
        for (int i = 1; i < url.length; i++) {
            String[] Params = url[i].split(":");
            Resultado = Params[2];
            break;
        }
        return Resultado;
    }

}
