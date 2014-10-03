package com.wheelermarine.publicAccessSites;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewConfiguration;
import android.widget.TextView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.lang.reflect.Field;

/**
 * <p>
 * Copyright 2013 Steven Wheeler<br/>
 * Released under the GPLv3 license, see LICENSE file for details.
 * </p>
 */
public class PublicAccessDetailActivity extends Activity {

	private static final String TAG = "PublicAccesses.PublicAccessDetailActivity";

	private PublicAccess access;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.public_access_detail);

		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKey = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
			if (menuKey != null) {
				menuKey.setAccessible(true);
				menuKey.setBoolean(config, false);
			}
		} catch (Exception e) {
			Log.v(TAG, "Error updating menu button.", e);
		}

		ActionBar actionBar = getActionBar();
		if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

		long id = getIntent().getLongExtra("id", -1);
		Log.v(TAG, "Access ID: " + id);
		DatabaseHelper db = new DatabaseHelper(getApplicationContext());
		access = db.getPublicAccess(id);
		db.close();

		TextView name = (TextView) getWindow().findViewById(R.id.publicAccessName);
		name.setText(access.getName());

		TextView lake = (TextView) getWindow().findViewById(R.id.publicAccessLake);
		lake.setText(access.getLake());

		TextView county = (TextView) getWindow().findViewById(R.id.publicAccessCounty);
		county.setText(access.getCounty());

		TextView launch = (TextView) getWindow().findViewById(R.id.publicAccessLaunch);
		launch.setText((access.getRamps() > 0 ? access.getRamps() + " " : "") + (!access.getRamp().isEmpty() ? access.getRamp() + " " : "") + access.getLaunch());

		TextView docks = (TextView) getWindow().findViewById(R.id.publicAccessDocks);
		docks.setText(access.getDocks() > 0 ? "Yes (" + access.getDocks() + ")" : "No");

		TextView directions = (TextView) getWindow().findViewById(R.id.publicAccessDirections);
		directions.setText(access.getDirections());

		int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
		if (status != ConnectionResult.SUCCESS) {
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, 10);
			dialog.show();
		} else {
			// Load the map type from the user's preferences. The default is normal.
			android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			String mapTypeStr = prefs.getString("mapType", String.valueOf(GoogleMap.MAP_TYPE_NORMAL));
			if (mapTypeStr == null || !mapTypeStr.matches("[0-9]+")) mapTypeStr = String.valueOf(GoogleMap.MAP_TYPE_NORMAL);
			int mapType = Integer.parseInt(mapTypeStr);

			MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
			if (mapFragment != null) {
				GoogleMap map = mapFragment.getMap();
				map.setMyLocationEnabled(true);
				map.setMapType(mapType);
				map.moveCamera(CameraUpdateFactory.newLatLngZoom(access.getLatLon(), 13));
				map.addMarker(new MarkerOptions().title(access.getName()).snippet(access.getLake()).position(access.getLatLon()));
			}
		}

		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		if (locationManager != null) {
			// First try and get the last known GPS position.
			Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

			// If GPS wasn't found try the last known network location.
			if (location == null) location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

			if (location != null) {
				TextView distance = (TextView) getWindow().findViewById(R.id.publicAccessDistance);
				distance.setText(formatDistance(location.distanceTo(access.getLocation())) + " " + formatBearing(location.bearingTo(access.getLocation())));
			} else {
				TextView distance = (TextView) getWindow().findViewById(R.id.publicAccessDistance);
				distance.setText("-");
			}
		}
	}

	private String formatDistance(double meters) {

		double miles = meters * 0.00062137;
		if (miles <= 0.25) {
			return "< 1/4 mile";
		} else if (miles <= 0.5) {
			return "< 1/2 mile";
		} else if (miles <= 0.75) {
			return "< 3/4 mile";
		} else if (miles <= 1) {
			return "< 1 mile";
		}
		return ((int) miles) + " miles";
	}

	private String formatBearing(double degrees) {

		double d = (360 + degrees) % 360;
		if (337.5 <= d || d <= 22.5) {
			return "N";
		} else if (22.5 <= d && d <= 67.5) {
			return "NE";
		} else if (67.5 <= d && d <= 112.5) {
			return "E";
		} else if (112.5 <= d && d <= 157.5) {
			return "SE";
		} else if (157.5 <= d && d <= 202.5) {
			return "S";
		} else if (202.5 <= d && d <= 247.5) {
			return "SW";
		} else if (247.5 <= d && d <= 292.5) {
			return "W";
		} else if (292.5 <= d && d <= 337.5) {
			return "NW";
		} else {
			throw new IllegalArgumentException("This should never happen!");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.main, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		if (searchItem != null) searchItem.setVisible(false);

		MenuItem updateItem = menu.findItem(R.id.action_update);
		if (updateItem != null) updateItem.setVisible(false);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Log.v(TAG, "Selected: " + item.getItemId());
		if (item.getItemId() == R.id.action_settings) {
			startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
		} else if (item.getItemId() == R.id.action_view_map) {
			String uri = String.format("geo:%1$f,%2$f?q=%1$f,%2$f(%3$s)", access.getLatitude(), access.getLongitude(), Uri.encode(access.getName().replace("(", "").replace(")", "")));
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
		}
		return super.onOptionsItemSelected(item);
	}
}