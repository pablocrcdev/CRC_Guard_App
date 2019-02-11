package com.guard.security.crc.crc_guard_app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.guard.security.crc.crc_guard_app.R;
import com.guard.security.crc.crc_guard_app.dao.DatabaseHandler;
import com.guard.security.crc.crc_guard_app.model.Marca;
import com.guard.security.crc.crc_guard_app.util.ErrorController;
import com.guard.security.crc.crc_guard_app.util.GPSRastreador;
import com.guard.security.crc.crc_guard_app.webview.ManagerChromeClient;
import com.guard.security.crc.crc_guard_app.webview.ManagerWebClient;
import com.guard.security.crc.crc_guard_app.webview.WebInterface;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView gvWebView;
    private ProgressBar gvProgressBar;
    //private String mURL = "http://201.196.88.8:9091/crccoding/f?p=2560";
    private String mURL = "http://192.168.1.50:9090/crccoding/f?p=2560";

    private SQLiteDatabase db;
    private DatabaseHandler dbhelper;

    private GPSRastreador gvGPS;

    private NfcAdapter gvNfcAdapter;
    private PendingIntent gvPendingIntent;
    private IntentFilter gvWriteTagFilters[];
    private boolean gvWriteMode;
    private Tag gvMytag;

    private Context gvContext;

    private int gvALL_PERMISSION = 0;

    private int REQUEST_READ_PHONE_STATE = 1;
    //********************************************************************************************//
    // Metodos de inicializacion
    //********************************************************************************************//
    private void initUIComponents() {
        gvWebView = (WebView) findViewById(R.id.WebView);
        gvProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        gvNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        gvContext = this;
        if (gvNfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        } else if (!gvNfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC desactivado.", Toast.LENGTH_LONG).show();
        }
    }

    private void initWebviewComponents() {
        // Seteo de Cliente Web, para manejo de navegador interno
        gvWebView.setWebViewClient(new ManagerWebClient(this));
        // Habilitacion de Javascript en el webview
        gvWebView.getSettings().setJavaScriptEnabled(true);
        // Inicializacion de interfaz de javascript entre webview y app android
        gvWebView.addJavascriptInterface(new WebInterface(MainActivity.this, gvGPS), "Android");
        // Permite el acceso a documentos
        gvWebView.getSettings().setAllowFileAccess(true);
        // Carga de URL en el elemento Webview
        gvWebView.loadUrl(mURL);

        gvWebView.setWebChromeClient(new ManagerChromeClient(this));

    }

    private void initDb() {
        //Abrimos la base de datos 'DBTest1' en modo escritura
        dbhelper = new DatabaseHandler(this, "RG", null, 1);
        db = dbhelper.getWritableDatabase();
    }

    private void initNFCComponents(){
        readFromIntent(getIntent());

        gvPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
    }

    //********************************************************************************************//
    // Metodos de validacion
    //********************************************************************************************//
    protected boolean validarEstadoRed() {
        ConnectivityManager vConnectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo vNetworkInfo = vConnectivityManager.getActiveNetworkInfo();
        if (vNetworkInfo != null && vNetworkInfo.isConnectedOrConnecting())
            return true;  // Si encuentra que hay conexion
        else
            return false; // De no encontrar conexion arroja falso
    }

    protected boolean accesarLocalizacion() {
        int vResult = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (vResult == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }

    protected boolean accesarInfoDispositivo() {
        int vResult = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        if (vResult == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }

    protected void solicitarAccesos() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
            //Codigo extra para el manejo de peticiones de permiso, en esta parte se colocalan
            //explicaciones con respecto a los permisos. Pueden ser ventanas emergentes o Toast
        }
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE}, gvALL_PERMISSION);
    }

    public String obtenerIdentificador() {
        TelephonyManager telephonyManager = (TelephonyManager) gvContext.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE}, gvALL_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return telephonyManager.getImei();
        }else{
            return "not found";
        }
    }

    //********************************************************************************************//
    // Metodos para interactuar con la base de datos
    //********************************************************************************************//
    private void registrarMarca(Marca pMarca) {
        //Si hemos abierto correctamente la base de datos
        if (db != null) {
            //Creamos el registro a insertar como objeto ContentValues
            ContentValues nuevoRegistro = new ContentValues();
            // El ID es auto incrementable como declaramos en el DatabaseHandler
            nuevoRegistro.put("imei_device", pMarca.getImei());
            nuevoRegistro.put("nfc_data", pMarca.getNfcData());
            nuevoRegistro.put("hora_marca", pMarca.getHoraMarca().toString());
            nuevoRegistro.put("latitud", pMarca.getLat());
            nuevoRegistro.put("longitud", pMarca.getLng());
            nuevoRegistro.put("ind_estado", "PRC");


            //Insertamos el registro en la base de datos
            db.insert("marca_reloj", null, nuevoRegistro);
        }
    }
    /*
    private void obtenerMarcasDispositivo(){
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
    }

    private void actualizarRegistroPendiente(Marca pMarca){
        if (db != null) {
            ContentValues cv = new ContentValues();
            cv.put("ind_estado", "ACT"); // registro de estado (ACT)ualizado

            db.update("marca_reloj", cv, "num_marca="+pMarca.getDbId(), null);

        }

    }
    */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (validarEstadoRed()) {
            if (!accesarLocalizacion() && !accesarInfoDispositivo()) {
                solicitarAccesos();
            }
            initUIComponents();
            initWebviewComponents();
            initDb();
            initNFCComponents();
        } else {
            new ErrorController(this).showNetworkDialog();
        }
    }

    @Override
    protected void onNewIntent(Intent pIntent) {
        setIntent(pIntent);
        readFromIntent(pIntent);
        if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(pIntent.getAction())){
            gvMytag = pIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    protected void onDestroy() {
        // cerramos conexión base de datos antes de destruir el activity
        db.close();
        super.onDestroy();
    }

    //********************************************************************************************//
    // Metodos para usar el servicio de NFC
    //********************************************************************************************//
    private void readFromIntent(Intent pIntent) {
        String action = pIntent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            gvMytag = pIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Toast.makeText(this, "Etiqueta Detectada", Toast.LENGTH_SHORT ).show();
            sonarAlarma();
            Parcelable[] rawMsgs = pIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }

    private void buildTagViews(NdefMessage[] pMsgs) {
        if (pMsgs == null || pMsgs.length == 0) return;

        String text = "";
        byte[] payload = pMsgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
        // String tagId = new String(msgs[0].getRecords()[0].getType());
        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        gvGPS = new GPSRastreador(this);
        //tvNFCContent.setText("NFC Content: " + text);
        Marca marca = new Marca(obtenerIdentificador(),
                text,
                dateFormat.format(date).toString(),
                Double.toString(gvGPS.obtenerLatitud()),
                Double.toString(gvGPS.obtenerLongitud()));
        registrarMarca(marca);

        //String v = gvWebView.getUrl();
        gvWebView.loadUrl("javascript:receiveData('" + marca.getImei() + "'" +
                ",'" + marca.getNfcData() + "'" +
                ",'" + marca.getHoraMarca() + "'" +
                ",'" + marca.getLat() + "'" +
                ",'" + marca.getLng() + "'" +
                ",'" + "NFC" + "');");
        //gvWebView.reload();

        //gvWebView.loadUrl("javascript:" + "readNFCTag(" + text + ");");
        //gvWebView.loadUrl("javascript:(function(){ alert ('"+text+"')})();");
    }

    public void sonarAlarma(){
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
