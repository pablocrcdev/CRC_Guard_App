package com.guard.security.crc.crc_guard_app.webview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.webkit.JavascriptInterface;

import com.guard.security.crc.crc_guard_app.activities.MainActivity;
import com.guard.security.crc.crc_guard_app.util.GPSRastreador;

public class WebInterface {
    private Context gvContext;
    private GPSRastreador gvGPS;
    //private String idDevice;

    //Constructor de la clase que solo recibe el contexto de la apicacion
    public WebInterface(Context pContext, GPSRastreador pGps){//, String pIdDevice) {
        this.gvContext = pContext;
        this.gvGPS = pGps;
    }

    @JavascriptInterface
    public String getLatitude() {
        gvGPS = new GPSRastreador(this.gvContext);
        if (gvGPS.poderObtenerLocacion()) {
            return String.valueOf(gvGPS.obtenerLatitud());
        } else {
            gvGPS.mostrarAlertaConfiguracion();
            return "";
        }
    }

    @JavascriptInterface
    public String getLongitude() {
        gvGPS = new GPSRastreador(this.gvContext);
        if (gvGPS.poderObtenerLocacion()) {
            return String.valueOf(gvGPS.obtenerLongitud());
        } else {
            gvGPS.mostrarAlertaConfiguracion();
            return "";
        }
    }

    @JavascriptInterface
    public String getIdDevice() {
        return obtenerIdentificador();
    }

    public String obtenerIdentificador() {
        TelephonyManager telephonyManager = (TelephonyManager) gvContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(gvContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telephonyManager.getImei();
        }else{
            return telephonyManager.getDeviceId();
        }

    }





}
