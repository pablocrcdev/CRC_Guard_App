package com.guard.security.crc.crc_guard_app.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.guard.security.crc.crc_guard_app.model.Marca;
import com.guard.security.crc.crc_guard_app.util.GPSRastreador;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DatabaseHandler extends SQLiteOpenHelper {
    public DatabaseHandler(Context pContext, String pName, SQLiteDatabase.CursorFactory pFactory, int pVersion) {
        super(pContext, pName, pFactory, pVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //Se crea la nueva versión de la tabla
        create_DB(sqLiteDatabase);
    }

    private void create_DB(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table marca_reloj (" +
                "num_marca integer primary key autoincrement," +
                "imei_device text," +
                "nfc_data text," +
                "hora_marca datetime," +
                "latitud text," +
                "longitud text," +
                "num_serial text,"+
                "ind_estado text default \"PEN\")");
        //
        sqLiteDatabase.execSQL("create table tbl_url (" + "Url text," + "fecha text" + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int prevVersion, int newVersion) {
        //Se elimina la versión anterior de la tabla
        db.execSQL("DROP TABLE IF EXISTS marca_reloj");
        db.execSQL("DROP TABLE IF EXISTS tbl_url");
        //Se crea la nueva versión de la tabla
        create_DB(db);

    }
    public void LimpiarDB(SQLiteDatabase db){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Date date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String Fecha = dateFormat.format(date);

        db.execSQL("DELETE FROM marca_reloj WHERE ind_estado = 'PRC'  AND hora_marca <= '"+Fecha+"'");
    }

}
