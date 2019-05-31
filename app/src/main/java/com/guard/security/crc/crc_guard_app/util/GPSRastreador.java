package com.guard.security.crc.crc_guard_app.util;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class GPSRastreador extends Service implements LocationListener {
    //==================================Variables Globales========================================//
    private final Context gvContext;
    private boolean gvEstaActivoGPS = false;
    private boolean gvEstaActivaRed = false;
    private boolean gvPuedeObtenerLocalizacion = false;
    private Location gvLocalizacion;
    private double gvLatitud;
    private double gvLongitud;
    //--------------------------------------------------------------------------------------------//
    private static final long MIN_CAMBIO_DISTANCIA_AL_ACTUALIZAR = 10;
    private static final long MIN_TIEMPO_BW_ACTUALIZACIONES = 1000 * 60 * 1;
    protected LocationManager managerLocalizacion;
    //==================================Variables Globales========================================//

    public GPSRastreador(Context context) {
        this.gvContext = context;
        ObtenerLocalizacion();
    }

    private Location ObtenerLocalizacion() {
        try {
            managerLocalizacion = (LocationManager) gvContext.getSystemService(Context.LOCATION_SERVICE);
            gvEstaActivoGPS = managerLocalizacion.isProviderEnabled(LocationManager.GPS_PROVIDER);
            gvEstaActivaRed = managerLocalizacion.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!gvEstaActivoGPS && !gvEstaActivaRed) {

            } else {
                this.gvPuedeObtenerLocalizacion = true;
                if (gvEstaActivaRed) {
                    if (ActivityCompat.checkSelfPermission(gvContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(gvContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    }
                    managerLocalizacion.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            MIN_TIEMPO_BW_ACTUALIZACIONES,
                            MIN_CAMBIO_DISTANCIA_AL_ACTUALIZAR,
                            this);
                    Log.i("Red", "Hay conexion");
                    if (managerLocalizacion != null) {
                        gvLocalizacion = managerLocalizacion.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (gvLocalizacion != null) {
                            gvLatitud = gvLocalizacion.getLatitude();
                            gvLongitud = gvLocalizacion.getLongitude();
                        }
                    }
                }
                if (gvEstaActivoGPS) {
                    if (gvLocalizacion == null) {
                        managerLocalizacion.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                                MIN_TIEMPO_BW_ACTUALIZACIONES,
                                MIN_CAMBIO_DISTANCIA_AL_ACTUALIZAR,
                                this);
                        Log.d("GPS", "GPS Activado");
                        if (managerLocalizacion != null) {
                            gvLocalizacion = managerLocalizacion.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (gvLocalizacion != null) {
                                gvLatitud = gvLocalizacion.getLatitude();
                                gvLongitud = gvLocalizacion.getLongitude();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gvLocalizacion;
    }



    //----------------------------------Permisos de Acceso----------------------------------------//
    public boolean poderObtenerLocacion() {
        return this.gvPuedeObtenerLocalizacion;
    }
    //------------------------------Funciones de Configuracion------------------------------------//
    public void mostrarAlertaConfiguracion() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(gvContext);
        alertDialog.setTitle("GPS configurado");
        alertDialog.setMessage("GPS no disponible. Quieres ir al menu de opciones?");
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                gvContext.startActivity(intent);
            }
        });
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        alertDialog.show();
    }

    public void detenerGPS() {
        if (managerLocalizacion != null) {
           /* if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            }*/
            managerLocalizacion.removeUpdates(GPSRastreador.this);
        }
    }
    //-----------------------------Funciones de Obtener lat-lng-----------------------------------//
    public double obtenerLongitud(){
        if(gvLocalizacion != null)
            gvLongitud = gvLocalizacion.getLongitude();
        return gvLongitud;
    }

    public double obtenerLatitud() {
        if (gvLocalizacion != null)
            gvLatitud = gvLocalizacion.getLatitude();
        return gvLatitud;
    }
    //==================================Metodos requeridos========================================//
    @Override
    public void onLocationChanged(Location location) {

    }
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }
    @Override
    public void onProviderEnabled(String s) {

    }
    @Override
    public void onProviderDisabled(String s) {

    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
