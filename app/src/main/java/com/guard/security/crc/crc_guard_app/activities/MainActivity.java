package com.guard.security.crc.crc_guard_app.activities;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.guard.security.crc.crc_guard_app.R;
import com.guard.security.crc.crc_guard_app.dao.DatabaseHandler;
import com.guard.security.crc.crc_guard_app.model.Marca;
import com.guard.security.crc.crc_guard_app.util.ErrorController;
import com.guard.security.crc.crc_guard_app.util.GPSRastreador;
import com.guard.security.crc.crc_guard_app.util.Procesos;
import com.guard.security.crc.crc_guard_app.webview.ManagerChromeClient;
import com.guard.security.crc.crc_guard_app.webview.ManagerWebClient;
import com.guard.security.crc.crc_guard_app.webview.WebInterface;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import android.app.AlarmManager;

public class MainActivity extends AppCompatActivity {

    private WebView gvWebView;
    private ProgressBar gvProgressBar;
    //IP Public ALFA
    //private String mURL = "http://186.96.89.66:9090/crccoding/f?p=2560:1";
    private String mURL = "http://10.1.1.12:9090/crccoding/f?p=2560:LOGIN_DESKTOP";
    //private String mURL =  "https://androidfilehost.com/?fid=3556969557455276147";
    //Desa Externo
    //private String mURL = "http://201.196.88.8:9090/crccoding/f?p=2560:1";
    //IP Desa
    //private String mURL = "http://192.168.1.50:9090/crccoding/f?p=2560:1";

    private SQLiteDatabase db;
    private DatabaseHandler dbhelper;
    private GPSRastreador gvGPS;
    private NfcAdapter gvNfcAdapter;
    private PendingIntent gvPendingIntent;
    private IntentFilter gvWriteTagFilters[];
    private boolean gvWriteMode;
    private Tag gvMytag;
    private Context gvContext;
    private SwipeRefreshLayout mySwipeRefreshLayout;
    private ViewTreeObserver.OnScrollChangedListener mOnScrollChangedListener;
    private int gvALL_PERMISSION = 0;
    // =================== Variables para permisos de android =================== //
    private static final int gvFILECHOOSER_RESULTCODE = 1;
    // =============== Usadas para seleccion de archivos nativos =============== //
    public ValueCallback<Uri> gvUploadMessage;
    public Uri gvCapturedImageURI = null;
    public ValueCallback<Uri[]> gvFilePathCallback;
    public String gvCameraPhotoPath;
    private AlarmManager planificarAlarma;
    private Procesos Procesar = new Procesos();

    //********************************************************************************************//
    // Metodos de validacion
    //********************************************************************************************//
    protected boolean validarEstadoRed() {

        ConnectivityManager vConnectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo vNetworkInfo = vConnectivityManager.getActiveNetworkInfo();
        boolean Resultado;
        Resultado = vNetworkInfo != null && vNetworkInfo.isConnectedOrConnecting();
        // Si encuentra que hay conexion
        // De no encontrar conexion arroja falso
        String Res = null;
        try {
            Res = new GetUrlContentTask().execute(mURL).get(7, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                // yourMethod();
            }
        }, 7000);
        if (Res.equals("SI")) {
            Resultado = true;
        } else {
            Resultado = false;
        }
        return Resultado;
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
        } else {
            return telephonyManager.getDeviceId();
        }
    }

    // Permisos para utilizar camara de dispositivc y album de imagenes
    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    //********************************************************************************************//
    // Metodos para interactuar con la base de datos
    //********************************************************************************************//
    private void registrarMarca(Marca pMarca, String Estado) {
        //Si hemos abierto correctamente la base de datos
        if (db != null) {
            //Creamos el registro a insertar como objeto ContentValues
            ContentValues nuevoRegistro = new ContentValues();
            // El ID es auto incrementable como declaramos en el DatabaseHandler
            nuevoRegistro.put("imei_device", pMarca.getImei());
            nuevoRegistro.put("nfc_data", pMarca.getNfcData());
            nuevoRegistro.put("hora_marca", pMarca.getHoraMarca());
            nuevoRegistro.put("latitud", pMarca.getLat());
            nuevoRegistro.put("longitud", pMarca.getLng());
            nuevoRegistro.put("ind_estado", Estado);
            nuevoRegistro.put("num_serial", pMarca.getNum_serial());


            //Insertamos el registro en la base de datos
            db.insert("marca_reloj", null, nuevoRegistro);
        }
    }

    // se reescribe el metodo para quitarle la funcionalidad del back button en el webview
    @Override
    public void onBackPressed() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Procesar.SolicitarPermisos(this, this, null);
        gvContext = this;

        String deviceId = obtenerIdentificador();
        gvNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        planificarAlarma = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intentt = new Intent(getApplicationContext(), Sender.class);
        PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, intentt, 0);
        planificarAlarma.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 600000, 600000, pi);
        //
        if (validarEstadoRed()) {


            // Declaracion del elemento xml en la clase para configuraciones
            gvWebView = findViewById(R.id.WebView);
            // Inicializacion de elemento Progress Bar
            gvProgressBar = findViewById(R.id.progressBar);

            if (gvNfcAdapter == null) {
                Toast.makeText(this, "El dispositivo no soporta NFC.", Toast.LENGTH_LONG).show();
            } else if (!gvNfcAdapter.isEnabled()) {
                Toast.makeText(this, "NFC desactivado.", Toast.LENGTH_LONG).show();
            }

            dbhelper = new DatabaseHandler(this, "RG", null, 1);
            db = dbhelper.getWritableDatabase();
            // Seteo de Cliente Web, para manejo de navegador interno
            gvWebView.setWebViewClient(new ManagerWebClient(this, this, gvWebView, this, dbhelper, db));
            // Habilitacion de Javascript en el webview
            gvWebView.getSettings().setJavaScriptEnabled(true);
            gvWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            // Inicializacion de interfaz de javascript entre webview y app android
            gvWebView.addJavascriptInterface(new WebInterface(MainActivity.this, gvGPS, this), "Android");
            // Permite el acceso a documentos
            gvWebView.getSettings().setAllowFileAccess(true);
            // Carga de URL en el elemento Webview
            gvWebView.loadUrl(mURL);


            mySwipeRefreshLayout = this.findViewById(R.id.Swipe);
            mySwipeRefreshLayout.getViewTreeObserver().addOnScrollChangedListener(mOnScrollChangedListener =
                    new ViewTreeObserver.OnScrollChangedListener() {
                        @Override
                        public void onScrollChanged() {
                            if (gvWebView.getScrollY() == 0)
                                mySwipeRefreshLayout.setEnabled(true);
                            else
                                mySwipeRefreshLayout.setEnabled(false);

                        }
                    });
            mySwipeRefreshLayout.setOnRefreshListener(
                    new SwipeRefreshLayout.OnRefreshListener() {
                        @Override
                        public void onRefresh() {
                            gvWebView.reload();
                            mySwipeRefreshLayout.setRefreshing(false);
                        }
                    }
            );
            gvWebView.setWebChromeClient(new ManagerChromeClient(gvProgressBar, this));
            //Se limpia la db de tags pasados y procesados(a partir de 1 mes anterior al actual)
            dbhelper.LimpiarDB(db);
            if (gvNfcAdapter != null) {
                readFromIntent(getIntent());

                gvPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
                IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
                tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
                gvWriteTagFilters = new IntentFilter[]{tagDetected};
            }

        } else {
            if (gvNfcAdapter == null)
                new ErrorController(this).showNetworkDialog();
            else {
                Toast.makeText(this, "No hay conexión a internet.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, LocalHomeActivity.class);
                startActivity(intent);
            }
        }
    }


    @Override
    protected void onDestroy() {
        // cerramos conexión base de datos antes de destruir el activity
        if (db != null)
            db.close();
        super.onDestroy();
    }

    //********************************************************************************************//
    // Metodos para usar el servicio de NFC
    //********************************************************************************************//
    //Metodo compartido entre Main y Local Activity
    private void readFromIntent(Intent pIntent) {
        String action = pIntent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            gvMytag = pIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Toast.makeText(this, "Etiqueta Detectada", Toast.LENGTH_SHORT).show();
            sonarAlarma();
            Parcelable[] rawMsgs = pIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs, Procesar.getTagSerial_number(getIntent().getByteArrayExtra(NfcAdapter.EXTRA_ID)));
        }
    }
    //Metodo compartido , poner en Procesos
    private void buildTagViews(NdefMessage[] pMsgs, String pNumSerial) {
        if (pMsgs == null || pMsgs.length == 0) return;

        String text = "";
        byte[] payload = pMsgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063; // Get the Language Code, e.g. "en"
        try {
            // Get the Text
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        gvGPS = new GPSRastreador(this);
        Marca marca = new Marca(obtenerIdentificador(),
                text,
                pNumSerial,
                dateFormat.format(date),
                Double.toString(gvGPS.obtenerLatitud()),
                Double.toString(gvGPS.obtenerLongitud()));
        if (Procesar.Num_Pagina(gvWebView.getUrl()).equals("2")) {
            gvWebView.loadUrl("javascript:receiveData_up('" + marca.getImei() + "'" +
                    ",'" + marca.getNfcData() + "'" +
                    ",'" + marca.getHoraMarca() + "'" +
                    ",'" + marca.getLat() + "'" +
                    ",'" + marca.getLng() + "'" +
                    ",'" + "NFC" + "'" +
                    ",'"+ marca.getNum_serial() + "');");
            registrarMarca(marca, "PRC");
        } else {
            registrarMarca(marca, "PEN");
        }


    }

    public void sonarAlarma() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onNewIntent(Intent pIntent) {
        setIntent(pIntent);
        if (gvNfcAdapter != null) {
            readFromIntent(pIntent);
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(pIntent.getAction())) {
                gvMytag = pIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (gvNfcAdapter != null)
            gvNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        gvNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (gvNfcAdapter != null) {
            gvPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
            gvWriteTagFilters = new IntentFilter[]{tagDetected};
            gvNfcAdapter.enableForegroundDispatch(this, gvPendingIntent, gvWriteTagFilters, null);
        }
    }

    //Resultado de cuando se toma una foto con el ManagerChromeClient
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // code for all versions except of Lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            if (requestCode == gvFILECHOOSER_RESULTCODE) {
                if (null == this.gvUploadMessage) {
                    return;
                }
                Uri result = null;
                try {
                    if (resultCode != RESULT_OK) {
                        result = null;
                    } else {
                        // retrieve from the private variable if the intent is null
                        result = data == null ? gvCapturedImageURI : data.getData();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :" + e, Toast.LENGTH_LONG).show();
                }
                gvUploadMessage.onReceiveValue(result);
                gvUploadMessage = null;
            }

        } // end of code for all versions except of Lollipop

        // start of code for Lollipop only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != gvFILECHOOSER_RESULTCODE || gvFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;
            // check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (data == null || data.getData() == null) {
                    // if there is not data, then we may have taken a photo
                    if (gvCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(gvCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            gvFilePathCallback.onReceiveValue(results);
            gvFilePathCallback = null;
        } // end of code for Lollipop only
    }

    //Parametros AsyncTask
    //1 = Parametros
    //2 = Progress
    //3 = Result
    private class GetUrlContentTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... urls) {
            String Resultado;
            try {

                URL url;
                url = new URL(urls[0]);
                HttpURLConnection connection;
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();
                Resultado = "SI";
            } catch (
                    Exception ex) {
                Resultado = "NO";
            }
            return Resultado;
        }

        protected void onPostExecute(String result) {

        }

    }
}

