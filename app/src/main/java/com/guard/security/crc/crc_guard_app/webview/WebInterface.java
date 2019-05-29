package com.guard.security.crc.crc_guard_app.webview;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

//import com.google.gson.Gson;
import com.guard.security.crc.crc_guard_app.BuildConfig;
import com.guard.security.crc.crc_guard_app.activities.MainActivity;
import com.guard.security.crc.crc_guard_app.dao.DatabaseHandler;
import com.guard.security.crc.crc_guard_app.model.Marca;
import com.guard.security.crc.crc_guard_app.util.GPSRastreador;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class WebInterface {
    private Context gvContext;
    private Activity App;
    private GPSRastreador gvGPS;
    private SQLiteDatabase db;
    private DatabaseHandler dbhelper;

    //Constructor de la clase que solo recibe el contexto de la apicacion
    public WebInterface(Context pContext, GPSRastreador pGps, Activity app){//, String pIdDevice) {
        this.gvContext = pContext;
        this.gvGPS = pGps;
        this.App = app;
    }
    @JavascriptInterface
    public String getLocInfo(){
        return getIdDevice() + "," + getLatitude() + "," + getLongitude();
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
        dbhelper = new DatabaseHandler(gvContext, "RG", null, 1);
        db = dbhelper.getWritableDatabase();
        if (db != null) {
            Cursor cursor = db.rawQuery("select count(*) from marca_reloj where ind_estado = 'PEN'", null);
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            cursor.close();
            if (count != 0)
                return true;
            return false;
        }
        Toast.makeText(gvContext, "La base de datos no existe! Informe al Administrador de la aplicación", Toast.LENGTH_LONG ).show();
        return false;
    }

    @JavascriptInterface
    public void actualizaRegistros(){

        Toast.makeText(gvContext, "Actualizando registros", Toast.LENGTH_LONG ).show();
        actualizarRegistroPendiente();
    }

    @JavascriptInterface
    public String getMarks(){
        return getJSON((ArrayList<Marca>) obtenerMarcas());
    }


    @JavascriptInterface
    public String getImei(){
        ArrayList<String> Imei = new ArrayList<>();
        Imei.add(obtenerIdentificador2(this.gvContext));
        return getJSONImei(Imei);
    }
    @JavascriptInterface
    public String getAppVersion(){
        return BuildConfig.VERSION_NAME;
    }

    public String obtenerIdentificador2(Context gvContext) {
        TelephonyManager telephonyManager = (TelephonyManager) gvContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(gvContext, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }

        ActivityCompat.requestPermissions(App, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE}, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telephonyManager.getImei();
        } else {
            return telephonyManager.getDeviceId();
        }
    }

    private String getJSONImei(ArrayList<String> list) {
        JSONObject listJSON = new JSONObject();
        JSONObject obj = null;
        JSONArray jsonArray = new JSONArray();
        for (String Imei : list) {
            obj = new JSONObject();
            try {
                obj.put("Imei",Imei);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonArray.put(obj);
        }
        try {
            listJSON.put("Imei",jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return listJSON.toString();


    }

    private String getJSON(ArrayList<Marca> list) {
        JSONObject listJSON = new JSONObject();
        JSONObject obj = null;
        JSONArray jsonArray = new JSONArray();
        for (Marca marcas : list) {
            obj = new JSONObject();
            try {
                obj.put("imei", marcas.getImei());
                obj.put("nfc", marcas.getNfcData());
                obj.put("hora", marcas.getHoraMarca());
                obj.put("lat", marcas.getLat());
                obj.put("lng", marcas.getLng());

            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonArray.put(obj);
        }
        try {
            listJSON.put("marcas",jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return listJSON.toString();
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
                String numSerial = cursor.getString(cursor.getColumnIndex("num_serial"));
                list.add(new Marca(dbId, idDevice, numSerial, nfcData, horaMarca, lat, lng, estado));
                cursor.moveToNext();
            }
        }
        return list;
    }

    private void actualizarRegistroPendiente(){
        dbhelper = new DatabaseHandler(gvContext, "RG", null, 1);
        db = dbhelper.getWritableDatabase();

        if (db != null) {
            ContentValues cv = new ContentValues();
            cv.put("ind_estado", "PRC"); // registro de estado (ACT)ualizado

            db.update("marca_reloj", cv, "ind_estado="+ "\"PEN\"", null);
        }

    }
}
