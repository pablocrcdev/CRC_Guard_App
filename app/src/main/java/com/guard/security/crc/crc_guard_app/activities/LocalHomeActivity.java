package com.guard.security.crc.crc_guard_app.activities;
//TODO Crear metodos en JS paralelos a los que ya estan para poder trabajar con la verison vieja y la nueva y poder enviar el numero serial de los tags

import android.Manifest;
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
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.guard.security.crc.crc_guard_app.R;
import com.guard.security.crc.crc_guard_app.adapters.MarcaAdapter;
import com.guard.security.crc.crc_guard_app.dao.DatabaseHandler;
import com.guard.security.crc.crc_guard_app.model.Marca;
import com.guard.security.crc.crc_guard_app.util.GPSRastreador;
import com.guard.security.crc.crc_guard_app.util.Procesos;


import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class LocalHomeActivity extends AppCompatActivity {

    private DatabaseHandler dbHelper;
    private SQLiteDatabase db;
    private ListView listView;
    private MarcaAdapter adapter;
    private List<Marca> marcas;
    private GPSRastreador gvGPS;
    private NfcAdapter gvNfcAdapter;
    private PendingIntent gvPendingIntent;
    private Tag gvMytag;
    private IntentFilter gvWriteTagFilters[];

    private Procesos Procesar = new Procesos();

    //********************************************************************************************//
    // Inicializadores
    //********************************************************************************************//
    private void initNFCComponents() {


        gvNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (gvNfcAdapter != null) {

            readFromIntent(getIntent());

            gvPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            tagDetected.addCategory(Intent.CATEGORY_DEFAULT);

            gvWriteTagFilters = new IntentFilter[]{tagDetected};
        }
    }

    //********************************************************************************************//
    // Metodos para interactuar con la base de datos
    //********************************************************************************************//
    private List<Marca> obtenerMarcasDispositivo() {

        // Seleccionamos todos los registros de la tabla Cars
        //Cursor cursor = db.rawQuery("select * from marca_reloj where imei_device="+obtenerIdentificador(), null);
        Cursor cursor = db.rawQuery("select * from marca_reloj", null);
        List<Marca> list = new ArrayList<>();

        if (cursor.getCount() <= 0) {
            //La base de datos está vacía.
            LinearLayout LocaHome = findViewById(R.id.LLNo_data);
            LocaHome.setVisibility(View.VISIBLE);
            return list;
        }
        if (cursor.moveToFirst()) {
            LinearLayout LocaHome = findViewById(R.id.LLNo_data);
            LocaHome.setVisibility(View.GONE);
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
                list.add(new Marca(dbId, idDevice, nfcData, numSerial, horaMarca, lat, lng, estado));
                cursor.moveToNext();
            }
        }
        return list;
    }

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
            nuevoRegistro.put("num_serial", pMarca.getNum_serial());
            //Insertamos el registro en la base de datos
            db.insert("marca_reloj", null, nuevoRegistro);
        }
    }

    private void actualizarListView() {
        // borramos todos los elementos
        marcas.clear();
        // cargamos todos los elementos
        marcas.addAll(obtenerMarcasDispositivo());
        // refrescamos el adaptador
        adapter.notifyDataSetChanged();
    }

    private void create() {
        //Si hemos abierto correctamente la base de datos
        if (db != null) {
            //Creamos el registro a insertar como objeto ContentValues
            ContentValues nuevoRegistro = new ContentValues();
            // El ID es auto incrementable como declaramos en el DatabaseHandler
            nuevoRegistro.put("imei_device", obtenerIdentificador());
            nuevoRegistro.put("nfc_data", "Prueba");
            nuevoRegistro.put("hora_marca", new Date().toString());
            nuevoRegistro.put("latitud", "latitud");
            nuevoRegistro.put("longitud", "longitud");
            //Insertamos el registro en la base de datos
            db.insert("marca_reloj", null, nuevoRegistro);
        }
    }

    //********************************************************************************************//
    // Metodos para obtener datos del dispositivo
    //********************************************************************************************//
    public String obtenerIdentificador() {
        TelephonyManager telephonyManager = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
        }
        return telephonyManager.getDeviceId();
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
        ListView LocaHome = findViewById(R.id.listView);
        LocaHome.setVisibility(View.GONE);
        actualizarListView();

        final TextView txtMarca = findViewById(R.id.txtImei);
        txtMarca.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("CLICKK", "CLICK");
            }
        });

       /* final LinearLayout txtMarca = findViewById(R.id.LLR);
        txtMarca.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("CLICKK", "CLICK");
            }
        });*/
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
        //Prueba
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
        if (gvNfcAdapter != null)
            gvNfcAdapter.enableForegroundDispatch(this, gvPendingIntent, gvWriteTagFilters, null);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_home);
        Procesar.SolicitarPermisos(this, null, this);
        listView = findViewById(R.id.listView);
        marcas = new ArrayList<>();

        //Abrimos la base de datos 'DBTest1' en modo escritura
        dbHelper = new DatabaseHandler(this, "RG", null, 1);
        db = dbHelper.getWritableDatabase();
        initNFCComponents();
        adapter = new MarcaAdapter(this, marcas, R.layout.clv_row);
        listView.setAdapter(adapter);
        actualizarListView();
        dbHelper.LimpiarDB(db);

        final Button button = findViewById(R.id.btnReconectar);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(LocalHomeActivity.this, MainActivity.class
                );
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onDestroy() {
        // cerramos conexión base de datos antes de destruir el activity
        db.close();
        super.onDestroy();
    }
}
