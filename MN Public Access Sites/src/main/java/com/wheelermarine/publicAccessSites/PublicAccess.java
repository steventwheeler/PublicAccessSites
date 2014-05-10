package com.wheelermarine.publicAccessSites;

import android.database.Cursor;
import android.location.Location;
import com.google.android.gms.maps.model.LatLng;

/**
 * <p>
 * This class represents a public access.
 * </p>
 * <p/>
 * <p>
 * Copyright 2013 Steven Wheeler<br/>
 * Released under the GPLv3 license, see LICENSE file for details.
 * </p>
 */
public class PublicAccess {

	private final Long id;
	private final String name;
	private final String launch;
	private final String ramp;
	private final int ramps;
	private final int docks;
	private final String directions;
	private final String lake;
	private final String county;
	private final int recordNumber;
	private double latitude;
	private double longitude;

	public PublicAccess(Cursor cursor) {

		id = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_ID));
		name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_NAME));
		launch = cursor.getString(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_LAUNCH));
		ramp = cursor.getString(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_RAMP));
		ramps = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_RAMPS));
		docks = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_DOCKS));
		directions = cursor.getString(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_DIRECTIONS));
		lake = cursor.getString(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_LAKE));
		county = cursor.getString(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_COUNTY));
		latitude = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_LATITUDE));
		longitude = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_LONGITUDE));
		recordNumber = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.PublicAccessEntry.COLUMN_NAME_RECORD_NUMBER));
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return value(name);
	}

	public String getLaunch() {
		return value(launch);
	}

	public String getRamp() {
		return value(ramp);
	}

	public int getRamps() {
		return ramps;
	}

	public int getDocks() {
		return docks;
	}

	public String getDirections() {
		return value(directions);
	}

	public String getLake() {
		return value(lake);
	}

	public String getCounty() {
		return value(county);
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public int getRecordNumber() {
		return recordNumber;
	}

	public Location getLocation() {

		Location loc = new Location("DATABASE");
		loc.setLatitude(latitude);
		loc.setLongitude(longitude);
		return loc;
	}

	public LatLng getLatLon() {

		return new LatLng(getLatitude(), getLongitude());
	}

	public String toString() {

		return getName();
	}

	private String value(Object o) {

		if (o == null) return "";
		String s = String.valueOf(o).trim();
		if (s.equalsIgnoreCase("Unknown")) s = "";
		if (s.equalsIgnoreCase("Other")) s = "";
		return s;
	}
}