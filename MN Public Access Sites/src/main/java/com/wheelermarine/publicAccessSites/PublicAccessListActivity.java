package com.wheelermarine.publicAccessSites;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;

/**
 * <p>
 * Copyright 2013 Steven Wheeler<br/>
 * Released under the GPLv3 license, see LICENSE file for details.
 * </p>
 */
public class PublicAccessListActivity extends ListActivity {

	private static final String TAG = "PublicAccesses.PublicAccessListActivity";

	private PublicAccessAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.public_access_list);

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

		try {
			ListView listView = getListView();

			adapter = new PublicAccessAdapter(this, R.layout.public_access_text_view);
			listView.setAdapter(adapter);

			listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

					final PublicAccess item = (PublicAccess) adapterView.getItemAtPosition(position);
					if (item == null) {
						Log.v(TAG, "No item found at position: " + position);
						return;
					}
					Log.v(TAG, item.getName());
					Intent intent = new Intent(getApplicationContext(), PublicAccessDetailActivity.class);
					Log.v(TAG, "Selected ID: " + item.getId());
					intent.putExtra("id", item.getId());
					startActivity(intent);
				}
			});

			registerForContextMenu(listView);
		} catch (Exception e) {
			Log.e(TAG, "Error initializing view!", e);
		}

		try {
			DatabaseHelper db = new DatabaseHelper(this);
			if (db.getPublicAccessesCount() == 0) upgrade();
			db.close();
		} catch (Exception e) {
			Log.e(TAG, "Error checking database size!", e);
		}
	}

	private void upgrade() {

		try {
			Updater updater = new Updater(this, this, adapter);
			Log.v(TAG, "Starting updater...");
			android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			updater.execute(new URL(prefs.getString("updateServer", getString(R.string.updateURL))));
		} catch (IOException e) {
			Log.e(TAG, "Error updating database.", e);
			Toast.makeText(this, "Error updating database: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

		try {
			super.onCreateContextMenu(menu, v, menuInfo);
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.context, menu);
		} catch (Exception e) {
			Log.e(TAG, "Error creating context menu!", e);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {

		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		if (item.getItemId() == R.id.context_view_on_map && info != null) {
			PublicAccess access = adapter.getItem(info.position);
			String uri = String.format("geo:%1$f,%2$f?q=%1$f,%2$f(%3$s)", access.getLatitude(), access.getLongitude(), Uri.encode(access.getName().replace("(", "").replace(")", "")));
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
			return true;
		} else {
			return super.onContextItemSelected(item);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.main, menu);
		MenuItem viewItem = menu.findItem(R.id.action_view_map);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		SearchView searchView = searchItem == null ? null : (SearchView) searchItem.getActionView();

		if (viewItem != null) viewItem.setVisible(false);
		if (searchView != null) {
			searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
				@Override
				public boolean onQueryTextSubmit(String s) {
					Log.v(TAG, "Searching: " + s);
					adapter.getFilter().filter(s);
					return true;
				}

				@Override
				public boolean onQueryTextChange(String s) {
					Log.v(TAG, "Searching: " + s);
					adapter.getFilter().filter(s);
					return true;
				}
			});
			searchView.setOnCloseListener(new SearchView.OnCloseListener() {
				@Override
				public boolean onClose() {
					adapter.getFilter().filter("");
					return true;
				}
			});
			searchView.setSubmitButtonEnabled(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		Log.v(TAG, "Selected: " + item.getItemId());
		if (item.getItemId() == R.id.action_settings) {
			startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
		} else if (item.getItemId() == R.id.action_update) {
			upgrade();
		}
		return super.onOptionsItemSelected(item);
	}
}