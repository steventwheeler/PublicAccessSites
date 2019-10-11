package com.wheelermarine.publicAccessSites;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.wheelermarine.publicAccessSites.dbase.DBaseReader;
import com.wheelermarine.publicAccessSites.dbase.Record;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.nocrala.tools.gis.data.esri.shapefile.ShapeFileReader;
import org.nocrala.tools.gis.data.esri.shapefile.exception.InvalidShapeFileException;
import org.nocrala.tools.gis.data.esri.shapefile.header.ShapeFileHeader;
import org.nocrala.tools.gis.data.esri.shapefile.shape.AbstractShape;
import org.nocrala.tools.gis.data.esri.shapefile.shape.ShapeType;
import org.nocrala.tools.gis.data.esri.shapefile.shape.shapes.PointShape;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * <p>
 * This class downloads updates from the MN DNR's data deli web site.  It
 * first requests the quick download page which contains a link to the ZIP
 * archive on the FTP server.  It then downloads and uncompresses the ZIP
 * archive while simultaneously loading the public access data from the
 * DBase database file and loading it into the SQLite database.
 * </p>
 * <p>
 * Copyright 2013 Steven Wheeler<br/>
 * Released under the GPLv3 license, see LICENSE file for details.
 * </p>
 */
public class Updater extends AsyncTask<URL, Integer, Integer> {

    private static final String TAG = "PublicAccesses.Updater";
    private static final int timeout = 60;
    private static final String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.57 Safari/537.17";
    private static final String entryName = "shor_waspt3.dbf";
    private static final String shapeEntryName = "shor_waspt3.shp";

    private Activity activity;
    private Context context;
    private PublicAccessAdapter adapter;
    private ProgressDialog progress;
    private Exception error;

    public Updater(Activity activity, Context context, PublicAccessAdapter adapter) {
        this.activity = activity;
        this.context = context;
        this.adapter = adapter;
    }

    /**
     * Convert a UTM location to a latitude and longitude location.
     *
     * @param north the northing value.
     * @param east  the easting value.
     * @param zone  the UTM zone.
     * @return a Location containing the latitude and longitude.
     */
    public static Location fromUTM(double north, double east, double zone) {

        double d = 0.99960000000000004;
        double d1 = 6378137;
        double d2 = 0.0066943799999999998;
        double d4 = (1 - Math.sqrt(1 - d2)) / (1 + Math.sqrt(1 - d2));
        double d3 = d2 / (1 - d2);
        double d12 = (north / d) / (d1 * (1 - d2 / 4 - (3 * d2 * d2) / 64 - (5 * Math.pow(d2, 3)) / 256));
        double d14 = d12 + ((3 * d4) / 2 - (27 * Math.pow(d4, 3)) / 32) * Math.sin(2 * d12) + ((21 * d4 * d4) / 16 - (55 * Math.pow(d4, 4)) / 32) * Math.sin(4 * d12) + ((151 * Math.pow(d4, 3)) / 96) * Math.sin(6 * d12);
        double d5 = d1 / Math.sqrt(1 - d2 * Math.sin(d14) * Math.sin(d14));
        double d6 = Math.tan(d14) * Math.tan(d14);
        double d7 = d3 * Math.cos(d14) * Math.cos(d14);
        double d8 = (d1 * (1 - d2)) / Math.pow(1 - d2 * Math.sin(d14) * Math.sin(d14), 1.5);
        double d9 = (east - 500000) / (d5 * d);
        double lat = (d14 - ((d5 * Math.tan(d14)) / d8) * (((d9 * d9) / 2 - (((5 + 3 * d6 + 10 * d7) - 4 * d7 * d7 - 9 * d3) * Math.pow(d9, 4)) / 24) + (((61 + 90 * d6 + 298 * d7 + 45 * d6 * d6) - 252 * d3 - 3 * d7 * d7) * Math.pow(d9, 6)) / 720)) * 180 / Math.PI;
        double lon = (((zone - 1) * 6 - 180) + 3) + (((d9 - ((1 + 2 * d6 + d7) * Math.pow(d9, 3)) / 6) + (((((5 - 2 * d7) + 28 * d6) - 3 * d7 * d7) + 8 * d3 + 24 * d6 * d6) * Math.pow(d9, 5)) / 120) / Math.cos(d14)) * 180 / Math.PI;
        Location loc = new Location("MNDNR");
        loc.setLatitude(lat);
        loc.setLongitude(lon);
        return loc;
    }

    @Override
    protected void onPreExecute() {

        // Setup the progress dialog box.
        progress = new ProgressDialog(context);
        progress.setTitle("Updating public accesses...");
        progress.setMessage("Please wait.");
        progress.setCancelable(false);
        progress.setIndeterminate(true);
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.show();
    }

    @Override
    protected Integer doInBackground(URL... urls) {

        try {
            final DatabaseHelper db = new DatabaseHelper(context);

            SQLiteDatabase database = db.getWritableDatabase();
            if (database == null)
                throw new IllegalStateException("Unable to open database!");

            database.beginTransaction();
            try {
                // Clear out the old data.
                database.delete(DatabaseHelper.PublicAccessEntry.TABLE_NAME, null, null);

                // Make sure the download URL was fund.
                final URL url;
                if (urls == null || urls.length == 0) {
                    throw new IllegalArgumentException("No URL was provided.");
                } else if (urls.length > 1) {
                    throw new IllegalArgumentException("Too many URLs were provided.");
                } else {
                    url = urls[0];
                }

                // Connect to the FTP server and download the update.
                Log.v(TAG, "Downloading update: " + url);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setMessage("Downloading update...");
                        progress.setIndeterminate(true);
                    }
                });

                final FTPClient client = new FTPClient();
                final int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
                client.connect(url.getHost(), port);
                client.login("anonymous", "");
                client.enterLocalPassiveMode();
                Log.v(TAG, "Reply Code: " + client.getReplyCode());
                try {
                    Log.v(TAG, "Connected to " + url.getHost() + ":" + port);
                    client.setFileType(FTP.BINARY_FILE_TYPE);

                    // Download the ZIP archive.
                    Log.v(TAG, "Downloading: " + url.getFile());
                    try (InputStream in = client.retrieveFileStream(url.getFile())) {
                        Log.v(TAG, "Reply Code: " + client.getReplyCode());
                        if (in == null)
                            throw new FileNotFoundException(url.getFile() + " was not found!");

                        Map<Integer, Location> locations = null;

                        try (ZipInputStream zin = new ZipInputStream(in)) {
                            // Locate the .dbf entry in the ZIP archive.
                            ZipEntry entry;
                            while ((entry = zin.getNextEntry()) != null) {
                                if (entry.getName().endsWith(entryName)) {
                                    readDBaseFile(zin, database);
                                } else if (entry.getName().endsWith(shapeEntryName)) {
                                    locations = readShapeFile(zin);
                                }
                            }
                        }

                        if (locations != null) {
                            final int recordCount = locations.size();
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progress.setIndeterminate(false);
                                    progress.setMessage("Updating locations...");
                                    progress.setMax(recordCount);
                                }
                            });

                            int progress = 0;
                            for (int recordNumber : locations.keySet()) {
                                PublicAccess access = db.getPublicAccessByRecordNumber(recordNumber);
                                Location loc = locations.get(recordNumber);
                                access.setLatitude(loc.getLatitude());
                                access.setLongitude(loc.getLongitude());
                                db.updatePublicAccess(access);
                                publishProgress(++progress);
                            }
                        }
                    }
                } finally {
                    client.disconnect();
                }

                database.setTransactionSuccessful();
                return db.getPublicAccessesCount();
            } finally {
                database.endTransaction();
            }
        } catch (Exception e) {
            error = e;
            Log.e(TAG, "Error loading data: " + e.getLocalizedMessage(), e);
            return -1;
        }
    }

    private Map<Integer, Location> readShapeFile(ZipInputStream zin) throws IOException, InvalidShapeFileException {

        ShapeFileReader reader = new ShapeFileReader(zin);
        ShapeFileHeader header = reader.getHeader();

        if (header.getShapeType() != ShapeType.POINT)
            throw new InvalidShapeFileException("Unable to read " + header.getShapeType() + " shape files.");

        Map<Integer, Location> locations = new HashMap<>();
        AbstractShape s;
        while ((s = reader.next()) != null) {
            if (s.getShapeType() == ShapeType.POINT) {
                PointShape point = (PointShape) s;
                int recordNumber = point.getHeader().getRecordNumber();
                Location location = fromUTM(point.getY(), point.getX(), 15);
                locations.put(recordNumber, location);
            } else {
                Log.d(TAG, "Unknown shape type: " + s.getShapeType());
            }
        }
        return locations;
    }

    private void readDBaseFile(ZipInputStream zin, SQLiteDatabase database) throws IOException {

        // Begin parsing the DBase data.
        DBaseReader reader = new DBaseReader(zin);
        final int recordCount = reader.size();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progress.setIndeterminate(false);
                progress.setMax(recordCount);
            }
        });
        Log.v(TAG, "DBase version: " + reader.getHeader().getSignature());
        Log.v(TAG, "Last Update: " + reader.getHeader().getLastUpdate());
        Log.v(TAG, "Record Count: " + reader.size());

        // Insert the records into the local database.
        int progress = 0;
        for (Record access : reader) {
            String lake = (String) access.getValue("LAKENAME");
            if (lake == null || lake.isEmpty())
                lake = (String) access.getValue("LAKE_NAME");
            if (lake == null || lake.isEmpty())
                lake = (String) access.getValue("ALT_NAME");
            if (lake == null || lake.isEmpty()) lake = String.valueOf(progress);

            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_NAME, (String) access.getValue("FAC_NAME"));
            values.put(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_LAUNCH, (String) access.getValue("LAUNCHTYPE"));
            values.put(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_RAMP, (String) access.getValue("RAMPTYPE"));
            values.put(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_RAMPS, (Double) access.getValue("NUMRAMPS"));
            values.put(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_DOCKS, (Double) access.getValue("NUMDOCKS"));
            values.put(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_DIRECTIONS, (String) access.getValue("DIRECTIONS"));
            values.put(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_LAKE, lake);
            values.put(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_COUNTY, (String) access.getValue("COUNTYNAME"));
            values.put(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_RECORD_NUMBER, progress + 1);
            database.insert(DatabaseHelper.PublicAccessEntry.TABLE_NAME, null, values);
            publishProgress(++progress);
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {

        // Update the progress bar.
        progress.setProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Integer integer) {

        // Must dismiss the progress dialog or it will cause an exception later.
        try {
            progress.dismiss();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error dismissing progress dialog.", e);
        }
        // Update list view.
        adapter.refresh();
        String message;
        if (error != null) {
            message = "Error loading data: " + error.getLocalizedMessage();
        } else {
            message = "Loaded " + integer + " public accesses.";
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }
}