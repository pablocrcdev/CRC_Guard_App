package com.guard.security.crc.crc_guard_app.dao;

import android.content.Context;
import android.database.Cursor;
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
                "num_serial text," +
                "ind_estado text default \"PEN\")");
        //
        sqLiteDatabase.execSQL("create table tbl_url (" + "SEC integer," + "Url text" + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int prevVersion, int newVersion) {
        //Se elimina la versión anterior de la tabla
        db.execSQL("DROP TABLE IF EXISTS marca_reloj");
        db.execSQL("DROP TABLE IF EXISTS tbl_url");
        //Se crea la nueva versión de la tabla
        create_DB(db);

    }

    public void LimpiarDB(SQLiteDatabase db) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Date date = calendar.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        String Fecha = dateFormat.format(date);

        db.execSQL("DELETE FROM marca_reloj WHERE ind_estado = 'PRC'  AND hora_marca <= '" + Fecha + "'");
    }
    public void Insertar_Act_Url(SQLiteDatabase db, String url){
       /* String query="SELECT * from tbl_url";
        Cursor cursor= db.rawQuery(query,null);
        //Sec = 1 porque solo se usará una unica URL
        if (cursor.getCount() > 0){
            db.execSQL("UPDATE tbl_url set Url'"+url+"' where SEC='1'");
        }else{
            db.execSQL("INSERT INTO tbl_url ");
        }*/
       String Query = "SELECT OR REPLACE tbl_url (SEC,URL) VALUES(1,'"+url+"'";
    }

}
