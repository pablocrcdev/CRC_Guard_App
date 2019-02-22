package com.guard.security.crc.crc_guard_app.webview;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.webkit.JavascriptInterface;

import com.google.gson.Gson;
import com.guard.security.crc.crc_guard_app.activities.MainActivity;
import com.guard.security.crc.crc_guard_app.dao.DatabaseHandler;
import com.guard.security.crc.crc_guard_app.model.Marca;
import com.guard.security.crc.crc_guard_app.util.GPSRastreador;

import java.util.ArrayList;
import java.util.List;

public class WebInterface {
    private Context gvContext;
    private GPSRastreador gvGPS;
    //private String idDevice;
    private SQLiteDatabase db;
    private DatabaseHandler dbhelper;

    //Constructor de la clase que solo recibe el contexto de la apicacion
    public WebInterface(Context pContext, GPSRastreador pGps){//, String pIdDevice) {
        this.gvContext = pContext;
        this.gvGPS = pGps;

        dbhelper = new DatabaseHandler(pContext, "RG", null, 1);
        db = dbhelper.getWritableDatabase();
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

    @JavascriptInterface
    public boolean validarPendientes(){
        Cursor cursor = db.rawQuery("select count(*) from marca_reloj where ind_estado = 'PEN'", null);
        cursor.moveToFirst();
        int count= cursor.getInt(0);
        cursor.close();
        if(count > 0)
            return true;
        return false;
    }

    @JavascriptInterface
    public String getMarks(){
        return getJSON((ArrayList<Marca>) obtenerMarcas());
    }

    private String getJSON(ArrayList<Marca> list) {
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        for(Marca d : list) {
            sb.append(gson.toJson(d));
        }
        return sb.toString();
    }

    private String obtenerIdentificador() {
        TelephonyManager telephonyManager = (TelephonyManager) gvContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(gvContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telephonyManager.getImei();
        }else{
            return telephonyManager.getDeviceId();
        }

    }

    private List<Marca> obtenerMarcas(){
        // Seleccionamos todos los registros de la tabla Cars
        Cursor cursor = db.rawQuery("select * from marca_reloj where ind_estado = 'PEN'", null);
        List<Marca> list = new ArrayList<Marca>();
        if (cursor.moveToFirst()) {
            // iteramos sobre el cursor de resultados,
            // y vamos rellenando el array que posteriormente devolveremos
            while (cursor.isAfterLast() == false) {
                int dbId = cursor.getInt(cursor.getColumnIndex("num_marca"));
                String idDevice = cursor.getString(cursor.getColumnIndex("imei_device"));
                String nfcData = cursor.getString(cursor.getColumnIndex("nfc_data"));
                String horaMarca = cursor.getString(cursor.getColumnIndex("hora_marca"));
                String lat = cursor.getString(cursor.getColumnIndex("latitud"));
                String lng = cursor.getString(cursor.getColumnIndex("longitud"));
                String estado = cursor.getString(cursor.getColumnIndex("ind_estado"));
                list.add(new Marca(dbId, idDevice, nfcData, horaMarca, lat, lng, estado));
                cursor.moveToNext();
            }
        }
        return list;
    }


}
