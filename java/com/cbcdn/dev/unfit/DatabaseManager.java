package com.cbcdn.dev.unfit;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseManager extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;

    private static final String createDeviceTable = "CREATE TABLE devices ( " +
            "    mac    TEXT    PRIMARY KEY" +
            "                   NOT NULL" +
            "                   UNIQUE," +
            "    is_def BOOLEAN NOT NULL" +
            "                   DEFAULT ( 0 ) " +
            ");";

    private static final String createReadingTable = "CREATE TABLE readings ( " +
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
