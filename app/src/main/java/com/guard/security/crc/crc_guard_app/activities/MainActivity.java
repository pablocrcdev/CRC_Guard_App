package com.guard.security.crc.crc_guard_app.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
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
import com.guard.security.crc.crc_guard_app.webview.ManagerChromeClient;
import com.guard.security.crc.crc_guard_app.webview.ManagerWebClient;
import com.guard.security.crc.crc_guard_app.webview.WebInterface;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private WebView gvWebView;
    private ProgressBar gvProgressBar;
    //private String mURL = "http://186.96.89.66:9090/crccoding/f?p=2560:1";

    //private String mURL = "http://201.196.88.8:9091/crccoding/f?p=2560";
    private String mURL = "http://192.168.1.50:9090/crccoding/f?p=2560:1";

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

    // Permisos para utilizar camara de dispositivc
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
    public void onBackPressed() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (validarEstadoRed()) {
            if (!accesarLocalizacion() && !accesarInfoDispositivo()) {
                solicitarAccesos();
            }
            if(!hasPermissions(MainActivity.this, gvPERMISSIONS)){
                ActivityCompat.requestPermissions(MainActivity.this, gvPERMISSIONS, gvPERMISSION_ALL);
            }
            /*initUIComponents();
            initWebviewComponents();
            initDb();
            initNFCComponents();*/
            gvContext = this;
            // Declaracion del elemento xml en la clase para configuraciones
            gvWebView = (WebView) findViewById(R.id.WebView);
            // Inicializacion de elemento Progress Bar
            gvProgressBar = (ProgressBar) findViewById(R.id.progressBar);

            gvNfcAdapter = NfcAdapter.getDefaultAdapter(this);

            if (gvNfcAdapter == null) {
                Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
                //finish();
            } else if (!gvNfcAdapter.isEnabled()) {
                Toast.makeText(this, "NFC desactivado.", Toast.LENGTH_LONG).show();
            }
            // Seteo de Cliente Web, para manejo de navegador interno
            gvWebView.setWebViewClient(new ManagerWebClient(this));
            // Habilitacion de Javascript en el webview
            gvWebView.getSettings().setJavaScriptEnabled(true);
            // Inicializacion de interfaz de javascript entre webview y app android
            gvWebView.addJavascriptInterface(new WebInterface(MainActivity.this, gvGPS),"Android");
            // Permite el acceso a documentos
            gvWebView.getSettings().setAllowFileAccess(true);
            // Carga de URL en el elemento Webview
            gvWebView.loadUrl(mURL);
            gvWebView.setWebChromeClient(new WebChromeClient(){
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
                    imageStorageDir  = new File(imageStorageDir + File.separator + "IMG_" + String.valueOf(System.currentTimeMillis()) + ".jpg");
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
                Intent intent = new Intent(this, LocalHomeActivity.class);
                startActivity(intent);
            }
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

        //String v = gvWebView.getOriginalUrl();

        gvWebView.loadUrl("javascript:receiveData('" + marca.getImei() + "'" +
                ",'" + marca.getNfcData() + "'" +
                ",'" + marca.getHoraMarca() + "'" +
                ",'" + marca.getLat() + "'" +
                ",'" + marca.getLng() + "'" +
                ",'" + "NFC" + "');");
        //v.replace("XXXXXXXXXX",text);
        //gvWebView.loadUrl(v);
        //Toast.makeText(this, v, Toast.LENGTH_SHORT ).show();
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

    /******************************************************************************
     **********************************Write to NFC Tag****************************
     ******************************************************************************/
    private void write(String pText, Tag pTag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(pText) };
        NdefMessage message = new NdefMessage(records);
        // Get an instance of Ndef for the tag.
        Ndef ndef = Ndef.get(pTag);
        // Enable I/O
        ndef.connect();
        // Write the message
        ndef.writeNdefMessage(message);
        // Close the connection
        ndef.close();
    }

    private NdefRecord createRecord(String pText) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = pText.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        // set status byte (see NDEF spec for actual bits)
        payload[0] = (byte) langLength;


        // copy langbytes and textbytes into payload
        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
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
    public void onPause(){
        super.onPause();
        if (gvNfcAdapter != null)
            gvNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    public void onResume(){
        super.onResume();
        if (gvNfcAdapter != null)
            gvNfcAdapter.enableForegroundDispatch(this, gvPendingIntent, gvWriteTagFilters, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // code for all versions except of Lollipop
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {

            if(requestCode==gvFILECHOOSER_RESULTCODE) {
                if (null == this.gvUploadMessage) {
                    return;
                }
                Uri result=null;
                try{
                    if (resultCode != RESULT_OK) {
                        result = null;
                    } else {
                        // retrieve from the private variable if the intent is null
                        result = data == null ? gvCapturedImageURI : data.getData();
                    }
                }
                catch(Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :"+e, Toast.LENGTH_LONG).show();
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
    /******************************************************************************
     **********************************Enable Write********************************
     ******************************************************************************/
    /* private void WriteModeOn(){
        gvWriteMode = true;
    }*/
    /******************************************************************************
     **********************************Disable Write*******************************
     ******************************************************************************/
    /*private void WriteModeOff(){
        gvWriteMode = false;

    }*/
}
