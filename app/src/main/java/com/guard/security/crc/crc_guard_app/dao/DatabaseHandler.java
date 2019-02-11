package com.guard.security.crc.crc_guard_app.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHandler extends SQLiteOpenHelper {
    public DatabaseHandler (Context pContext, String pName, SQLiteDatabase.CursorFactory pFactory, int pVersion){
        super(pContext, pName, pFactory, pVersion);
    }
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table marca_reloj (" +
                "num_marca integer primary key autoincrement," +
                "imei_device text," +
                "nfc_data text," +
                "hora_marca datetime," +
                "latitud text," +
                "longitud text," +
                "ind_estado text default \"PEN\")");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int prevVersion, int newVersion) {
        //Se elimina la versión anterior de la tabla
        db.execSQL("DROP TABLE IF EXISTS marca_reloj");

        //Se crea la nueva versión de la tabla
        db.execSQL("create table marca_reloj (" +
                "num_marca integer primary key autoincrement," +
                "imei_device text," +
                "nfc_data text," +
                "hora_marca datetime," +
                "latitud text," +
                "longitud text," +
                "ind_estado text default \"PEN\")");

    }

}
