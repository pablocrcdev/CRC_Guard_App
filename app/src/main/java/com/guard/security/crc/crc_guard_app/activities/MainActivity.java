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
import android.os.Environment;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.guard.security.crc.crc_guard_app.R;
import com.guard.security.crc.crc_guard_app.dao.DatabaseHandler;
import com.guard.security.crc.crc_guard_app.model.Marca;
import com.guard.security.crc.crc_guard_app.util.ErrorController;
import com.guard.security.crc.crc_guard_app.util.GPSRastreador;
import com.guard.security.crc.crc_guard_app.webview.ManagerWebClient;
import com.guard.security.crc.crc_guard_app.webview.WebInterface;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private WebView gvWebView;
    private ProgressBar gvProgressBar;
    //IP Public ALFA
    private String mURL = "http://186.96.89.66:9090/crccoding/f?p=2560:1";
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

    private int gvALL_PERMISSION = 0;

    // =================== Variables para permisos de android =================== //
    private static final int gvFILECHOOSER_RESULTCODE = 1;
    int gvPERMISSION_ALL = 1;
    String[] gvPERMISSIONS = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};
    // =============== Usadas para seleccion de archivos nativos =============== //
    private ValueCallback<Uri> gvUploadMessage;
    private Uri gvCapturedImageURI = null;
    private ValueCallback<Uri[]> gvFilePathCallback;
    private String gvCameraPhotoPath;

    //********************************************************************************************//
    // Metodos de validacion
    //********************************************************************************************//
    protected boolean validarEstadoRed() {
        ConnectivityManager vConnectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo vNetworkInfo = vConnectivityManager.getActiveNetworkInfo();
        boolean Resultado = false;
        Resultado = vNetworkInfo != null && vNetworkInfo.isConnectedOrConnecting();
        // Si encuentra que hay conexion
        // De no encontrar conexion arroja falso
        String Res = null;
        try {
            Res = new GetUrlContentTask().execute("http://186.96.89.66:9090/crccoding").get(7, TimeUnit.SECONDS);
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
        Log.i("RES2", Res);
        if (Res.equals("SI")) {

            Resultado = true;
        } else {
            Resultado = false;
        }
        Log.i("RES2", String.valueOf(Resultado));
        return Resultado;
    }


    protected boolean accesarLocalizacion() {
        int vResult = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return vResult == PackageManager.PERMISSION_GRANTED;
    }

    protected boolean accesarInfoDispositivo() {
        int vResult = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        return vResult == PackageManager.PERMISSION_GRANTED;
    }

    protected void solicitarAccesos() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_PHONE_STATE)) {
            //Codigo extra para el manejo de peticiones de permiso, en esta parte se colocan
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
    private void registrarMarca(Marca pMarca) {
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
            nuevoRegistro.put("ind_estado", "PRC");
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
        gvNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        //
        if (validarEstadoRed()) {
            Log.i("REDDS","VALIDO");
            {
                if (!accesarLocalizacion() && !accesarInfoDispositivo()) {
                    solicitarAccesos();
                }
            }
            if (!hasPermissions(MainActivity.this, gvPERMISSIONS)) {
                ActivityCompat.requestPermissions(MainActivity.this, gvPERMISSIONS, gvPERMISSION_ALL);
            }
            gvContext = this;
            // Declaracion del elemento xml en la clase para configuraciones
            gvWebView = findViewById(R.id.WebView);
            // Inicializacion de elemento Progress Bar
            gvProgressBar = findViewById(R.id.progressBar);

            if (gvNfcAdapter == null) {
                Toast.makeText(this, "El dispositivo no soporta NFC.", Toast.LENGTH_LONG).show();
                //finish();
            } else if (!gvNfcAdapter.isEnabled()) {
                Toast.makeText(this, "NFC desactivado.", Toast.LENGTH_LONG).show();
            }
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
            gvWebView.setWebChromeClient(new WebChromeClient() {
                // page loading progress, gone when fully loaded
                public void onProgressChanged(WebView view, int progress) {
                    if (progress < 100 && gvProgressBar.getVisibility() == ProgressBar.GONE) {
                        gvProgressBar.setVisibility(ProgressBar.VISIBLE);
                    }
                    gvProgressBar.setProgress(progress);
                    if (progress == 100) {
                        gvProgressBar.setVisibility(ProgressBar.GONE);
                    }
                }

                @Override
                public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                    if (gvFilePathCallback != null) {
                        gvFilePathCallback.onReceiveValue(null);
                    }
                    gvFilePathCallback = filePathCallback;
                    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                        // create the file where the photo should go
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                            takePictureIntent.putExtra("PhotoPath", gvCameraPhotoPath);
                        } catch (IOException ex) {
                            // Error occurred while creating the File
                            Log.e("UPLOADFILE", "Unable to create Image File", ex);
                        }

                        // continue only if the file was successfully created
                        if (photoFile != null) {
                            gvCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(photoFile));
                        } else {
                            takePictureIntent = null;
                        }
                    }
                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType("image/*");

                    Intent[] intentArray;
                    if (takePictureIntent != null) {
                        intentArray = new Intent[]{takePictureIntent};
                    } else {
                        intentArray = new Intent[0];
                    }

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.app_name));
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

                    startActivityForResult(chooserIntent, gvFILECHOOSER_RESULTCODE);

                    return true;
                }

                // creating image files (Lollipop only)
                private File createImageFile() throws IOException {

                    File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DirectoryNameHere");

                    if (!imageStorageDir.exists()) {
                        imageStorageDir.mkdirs();
                    }

                    // create an image file name
                    imageStorageDir = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
                    return imageStorageDir;
                }

                // openFileChooser for Android 3.0+
                public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                    gvUploadMessage = uploadMsg;

                    try {
                        File imageStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DirectoryNameHere");

                        if (!imageStorageDir.exists()) {
                            imageStorageDir.mkdirs();
                        }

                        File file = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");

                        gvCapturedImageURI = Uri.fromFile(file); // save to the private variable

                        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, gvCapturedImageURI);
                        // captureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

                        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                        i.addCategory(Intent.CATEGORY_OPENABLE);
                        i.setType("image/*");

                        Intent chooserIntent = Intent.createChooser(i, getString(R.string.app_name));
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{captureIntent});

                        startActivityForResult(chooserIntent, gvFILECHOOSER_RESULTCODE);
                    } catch (Exception e) {
                        Toast.makeText(getBaseContext(), "Camera Exception:" + e, Toast.LENGTH_LONG).show();
                    }

                }

                // openFileChooser for Android < 3.0
                public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                    openFileChooser(uploadMsg, "");
                }

                // openFileChooser for other Android versions
            /* may not work on KitKat due to lack of implementation of openFileChooser() or onShowFileChooser()
               https://code.google.com/p/android/issues/detail?id=62220
               however newer versions of KitKat fixed it on some devices */
                public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                    openFileChooser(uploadMsg, acceptType);
                }
            });
            //Abrimos la base de datos 'DBTest1' en modo escritura
            dbhelper = new DatabaseHandler(this, "RG", null, 1);
            db = dbhelper.getWritableDatabase();
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
                /*
                Intent intent = new Intent(this, LocalHomeActivity.class);
                startActivity(intent);*/

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
            buildTagViews(msgs, getTagSerial_number(getIntent().getByteArrayExtra(NfcAdapter.EXTRA_ID)));
        }
    }

    //Metodo compartido entre Main y Local Activity
    private String getTagSerial_number(byte[] tagId) {
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
        registrarMarca(marca);
        gvWebView.loadUrl("javascript:receiveData('" + marca.getImei() + "'" +
                ",'" + marca.getNfcData() + "'" +
                ",'" + marca.getHoraMarca() + "'" +
                ",'" + marca.getLat() + "'" +
                ",'" + marca.getLng() + "'" +
                ",'" + "NFC" + "');");

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

