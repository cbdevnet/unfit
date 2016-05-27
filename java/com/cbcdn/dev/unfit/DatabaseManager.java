package com.cbcdn.dev.unfit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseManager extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    private static final String createDeviceTable = "CREATE TABLE devices ( " +
            "    device     INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL UNIQUE," +
            "    mac    TEXT    NOT NULL" +
            "                   UNIQUE" +
            ");";

    private static final String createReadingTable = "CREATE TABLE readings ( " +
            "    device    INTEGER NOT NULL REFERENCES devices (id) " +
            "                      ON UPDATE CASCADE ON DELETE CASCADE," +
            "    uuid      TEXT    NOT NULL," +
            "    timestamp INTEGER NOT NULL" +
            "                      DEFAULT ( strftime( '%s', 'now' )  )," +
            "    reading   TEXT " +
            ");";

    public DatabaseManager(Context context) {
        super(context, "unfit", null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(createDeviceTable);
        db.execSQL(createReadingTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
