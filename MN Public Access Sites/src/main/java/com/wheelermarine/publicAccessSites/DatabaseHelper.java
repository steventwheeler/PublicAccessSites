package com.wheelermarine.publicAccessSites;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * This class simplifies interfacing with the SQLite database where public
 * accesses are stored.
 * </p>
 * <p/>
 * <p>
 * Copyright 2013 Steven Wheeler<br/>
 * Released under the GPLv3 license, see LICENSE file for details.
 * </p>
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "PublicAccesses.DatabaseHelper";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE =
            "CREATE TABLE " + PublicAccessEntry.TABLE_NAME + " (" +
                    PublicAccessEntry.COLUMN_NAME_ID + INT_TYPE + " PRIMARY KEY AUTOINCREMENT," +
                    PublicAccessEntry.COLUMN_NAME_NAME + TEXT_TYPE + COMMA_SEP +
                    PublicAccessEntry.COLUMN_NAME_LAUNCH + TEXT_TYPE + COMMA_SEP +
                    PublicAccessEntry.COLUMN_NAME_RAMP + TEXT_TYPE + COMMA_SEP +
                    PublicAccessEntry.COLUMN_NAME_RAMPS + INT_TYPE + COMMA_SEP +
                    PublicAccessEntry.COLUMN_NAME_DOCKS + INT_TYPE + COMMA_SEP +
                    PublicAccessEntry.COLUMN_NAME_DIRECTIONS + TEXT_TYPE + COMMA_SEP +
                    PublicAccessEntry.COLUMN_NAME_LAKE + TEXT_TYPE + COMMA_SEP +
                    PublicAccessEntry.COLUMN_NAME_COUNTY + TEXT_TYPE + COMMA_SEP +
                    PublicAccessEntry.COLUMN_NAME_LATITUDE + REAL_TYPE + COMMA_SEP +
                    PublicAccessEntry.COLUMN_NAME_LONGITUDE + REAL_TYPE + COMMA_SEP +
                    PublicAccessEntry.COLUMN_NAME_RECORD_NUMBER + INT_TYPE +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + PublicAccessEntry.TABLE_NAME;

    /**
     * Create a new DatabaseHelper.
     *
     * @param context to use to open or create the database.
     */
    public DatabaseHelper(Context context) {

        super(context, PublicAccessEntry.DATABASE_NAME, null, PublicAccessEntry.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Add a new public access to the database.
     *
     * @param publicAccess the public access to add.
     * @return the new record's ID.
     */
    public long addPublicAccess(PublicAccess publicAccess) {

        SQLiteDatabase db = getWritableDatabase();
        if (db == null) throw new RuntimeException("Unable to find writable database.");

        ContentValues values = new ContentValues();
        values.put(PublicAccessEntry.COLUMN_NAME_NAME, publicAccess.getName());
        values.put(PublicAccessEntry.COLUMN_NAME_LAUNCH, publicAccess.getLaunch());
        values.put(PublicAccessEntry.COLUMN_NAME_RAMP, publicAccess.getRamp());
        values.put(PublicAccessEntry.COLUMN_NAME_RAMPS, publicAccess.getRamps());
        values.put(PublicAccessEntry.COLUMN_NAME_DOCKS, publicAccess.getDocks());
        values.put(PublicAccessEntry.COLUMN_NAME_DIRECTIONS, publicAccess.getDirections());
        values.put(PublicAccessEntry.COLUMN_NAME_LAKE, publicAccess.getLake());
        values.put(PublicAccessEntry.COLUMN_NAME_COUNTY, publicAccess.getCounty());
        values.put(PublicAccessEntry.COLUMN_NAME_LATITUDE, publicAccess.getLatitude());
        values.put(PublicAccessEntry.COLUMN_NAME_LONGITUDE, publicAccess.getLongitude());
        values.put(PublicAccessEntry.COLUMN_NAME_RECORD_NUMBER, publicAccess.getRecordNumber());
        Log.v(TAG, "Created new PublicAccess(name=" + publicAccess.getName() + ")");
        return db.insert(PublicAccessEntry.TABLE_NAME, null, values);
    }

    /**
     * Get a specific public access from the database based on its ID.
     *
     * @param id the ID of the public access.
     * @return the public access if it exists or <code>null</code> if it doesn't.
     */
    public PublicAccess getPublicAccess(long id) {

        SQLiteDatabase db = this.getReadableDatabase();
        if (db == null) throw new RuntimeException("Unable to find readable database.");

        String whereSql = PublicAccessEntry.COLUMN_NAME_ID + "=?";
        String[] args = {String.valueOf(id)};
        Cursor cursor = db.query(PublicAccessEntry.TABLE_NAME, PublicAccessEntry.COLUMN_NAMES, whereSql, args, null, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                Log.v(TAG, "Loaded PublicAccess: " + id);
                return new PublicAccess(cursor);
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    public PublicAccess getPublicAccessByRecordNumber(int recordNumber) {

        SQLiteDatabase db = this.getReadableDatabase();
        if (db == null) throw new RuntimeException("Unable to find readable database.");

        String whereSql = PublicAccessEntry.COLUMN_NAME_RECORD_NUMBER + "=?";
        String[] args = {String.valueOf(recordNumber)};
        Cursor cursor = db.query(PublicAccessEntry.TABLE_NAME, PublicAccessEntry.COLUMN_NAMES, whereSql, args, null, null, null, null);

        try {
            if (cursor.moveToFirst()) {
                Log.v(TAG, "Loaded PublicAccess: " + recordNumber);
                return new PublicAccess(cursor);
            } else {
                return null;
            }
        } finally {
            cursor.close();
        }
    }

    /**
     * Load all the public accesses from the database.
     *
     * @return all of the public accesses.
     */
    public List<PublicAccess> getAllPublicAccesses() {

        List<PublicAccess> list = new ArrayList<PublicAccess>();
        SQLiteDatabase db = this.getReadableDatabase();
        if (db == null) throw new RuntimeException("Unable to find readable database.");
        Cursor cursor = db.query(PublicAccessEntry.TABLE_NAME, PublicAccessEntry.COLUMN_NAMES, null, null, null, null, "name, lake", null);
        try {
            while (cursor.moveToNext()) {
                list.add(new PublicAccess(cursor));
            }
        } finally {
            cursor.close();
        }
        Log.v(TAG, "Loaded " + list.size() + " public accesses.");
        return list;
    }

    /**
     * Get the number of public accesses in the database.
     *
     * @return the number of public accesses.
     */
    public int getPublicAccessesCount() {

        return getAllPublicAccesses().size();
    }

    /**
     * Update a public access with new information.
     *
     * @param publicAccess the public access to update.
     * @return the number of rows affected.
     */
    public int updatePublicAccess(PublicAccess publicAccess) {

        SQLiteDatabase db = this.getWritableDatabase();
        if (db == null) throw new RuntimeException("Unable to find writable database.");

        ContentValues values = new ContentValues();
        values.put(PublicAccessEntry.COLUMN_NAME_ID, publicAccess.getId());
        values.put(PublicAccessEntry.COLUMN_NAME_NAME, publicAccess.getName());
        values.put(PublicAccessEntry.COLUMN_NAME_LAUNCH, publicAccess.getLaunch());
        values.put(PublicAccessEntry.COLUMN_NAME_RAMP, publicAccess.getRamp());
        values.put(PublicAccessEntry.COLUMN_NAME_RAMPS, publicAccess.getRamps());
        values.put(PublicAccessEntry.COLUMN_NAME_DOCKS, publicAccess.getDocks());
        values.put(PublicAccessEntry.COLUMN_NAME_DIRECTIONS, publicAccess.getDirections());
        values.put(PublicAccessEntry.COLUMN_NAME_LAKE, publicAccess.getLake());
        values.put(PublicAccessEntry.COLUMN_NAME_COUNTY, publicAccess.getCounty());
        values.put(PublicAccessEntry.COLUMN_NAME_LATITUDE, publicAccess.getLatitude());
        values.put(PublicAccessEntry.COLUMN_NAME_LONGITUDE, publicAccess.getLongitude());
        values.put(PublicAccessEntry.COLUMN_NAME_RECORD_NUMBER, publicAccess.getRecordNumber());

        String whereSql = PublicAccessEntry.COLUMN_NAME_ID + "=?";
        String[] args = {String.valueOf(publicAccess.getId())};

        return db.update(PublicAccessEntry.TABLE_NAME, values, whereSql, args);
    }

    /**
     * Delete all the public accesses from the database.
     *
     * @return the number of rows affected.
     */
    public int deleteAllPublicAccesses() {

        SQLiteDatabase db = this.getWritableDatabase();
        if (db == null) throw new RuntimeException("Unable to find writable database.");
        return db.delete(PublicAccessEntry.TABLE_NAME, null, null);
    }

    /**
     * Delete a specific public access from the database.
     *
     * @param publicAccess the public access to delete.
     * @return the number of rows affected.
     */
    public int deletePublicAccess(PublicAccess publicAccess) {

        SQLiteDatabase db = this.getWritableDatabase();
        String whereSql = PublicAccessEntry.COLUMN_NAME_ID + "=?";
        String[] args = {String.valueOf(publicAccess.getId())};
        if (db == null) throw new RuntimeException("Unable to find writable database.");
        return db.delete(PublicAccessEntry.TABLE_NAME, whereSql, args);
    }

    public static abstract class PublicAccessEntry implements BaseColumns {

        public static final int DATABASE_VERSION = 2;
        public static final String DATABASE_NAME = "wheelermarine";
        public static final String TABLE_NAME = "public_access";
        public static final String COLUMN_NAME_ID = "_id";
        public static final String COLUMN_NAME_NAME = "name";
        public static final String COLUMN_NAME_LAUNCH = "launch";
        public static final String COLUMN_NAME_RAMP = "ramp";
        public static final String COLUMN_NAME_RAMPS = "ramps";
        public static final String COLUMN_NAME_DOCKS = "docks";
        public static final String COLUMN_NAME_DIRECTIONS = "directions";
        public static final String COLUMN_NAME_LAKE = "lake";
        public static final String COLUMN_NAME_COUNTY = "county";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_RECORD_NUMBER = "record_number";

        public static final String[] COLUMN_NAMES = {
                PublicAccessEntry.COLUMN_NAME_ID, PublicAccessEntry.COLUMN_NAME_NAME,
                PublicAccessEntry.COLUMN_NAME_LAUNCH, PublicAccessEntry.COLUMN_NAME_RAMP,
                PublicAccessEntry.COLUMN_NAME_RAMPS, PublicAccessEntry.COLUMN_NAME_DOCKS,
                PublicAccessEntry.COLUMN_NAME_DIRECTIONS, PublicAccessEntry.COLUMN_NAME_LAKE,
                PublicAccessEntry.COLUMN_NAME_COUNTY, PublicAccessEntry.COLUMN_NAME_LATITUDE,
                PublicAccessEntry.COLUMN_NAME_LONGITUDE, COLUMN_NAME_RECORD_NUMBER
        };
    }
}