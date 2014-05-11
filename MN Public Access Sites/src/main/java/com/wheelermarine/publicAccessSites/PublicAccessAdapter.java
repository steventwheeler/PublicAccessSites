package com.wheelermarine.publicAccessSites;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Copyright 2013 Steven Wheeler<br/>
 * Released under the GPLv3 license, see LICENSE file for details.
 * </p>
 */
public class PublicAccessAdapter extends ArrayAdapter<PublicAccess> implements Filterable {

	private static final String TAG = "PublicAccesses.PublicAccessAdapter";
	private final Filter filter;
	private List<PublicAccess> original = new ArrayList<PublicAccess>();
	private List<PublicAccess> fitems = new ArrayList<PublicAccess>();

	public PublicAccessAdapter(Context context, int textViewResourceID) {

		super(context, textViewResourceID, new ArrayList<PublicAccess>());
		filter = new PublicAccessFilter();
		refresh();
	}

	public void refresh() {

		final DatabaseHelper db = new DatabaseHelper(getContext());
		try {
			original.clear();
			fitems.clear();
			original = db.getAllPublicAccesses();
			fitems.addAll(original);
			super.clear();
			super.addAll(original);
			super.notifyDataSetChanged();
		} finally {
			db.close();
		}
	}

	@Override
	public long getItemId(int position) {

		PublicAccess access = getItem(position);
		return access.getId();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public Filter getFilter() {
		return filter;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.public_access_text_view, null);
		}
		if (v == null) throw new RuntimeException("Unable to find view!");

		PublicAccess access = fitems.get(position);
		if (access != null) {
			TextView name = (TextView) v.findViewById(R.id.publicAccessViewName);
			TextView county = (TextView) v.findViewById(R.id.publicAccessViewCounty);
			TextView directions = (TextView) v.findViewById(R.id.publicAccessViewDirections);
			if (access.getName().toLowerCase().contains(access.getLake().toLowerCase()) || access.getLake().equalsIgnoreCase("unknown")) {
				name.setText(access.getName());
			} else {
				name.setText(access.getName() + ", " + access.getLake());
			}
			county.setText(access.getCounty() + " County");
			directions.setText(access.getDirections());
		}
		return v;
	}

	private class PublicAccessFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence query) {

			FilterResults results = new FilterResults();
			String queryStr = normalize(query);
			Log.v(TAG, "Filtering: " + queryStr);

			List<PublicAccess> out = new ArrayList<PublicAccess>();
			if (queryStr.isEmpty()) {
				out.addAll(original);
			} else {
				for (PublicAccess access : new ArrayList<PublicAccess>(original)) {
					if (normalize(access.getLake()).contains(queryStr) || normalize(access.getName()).contains(queryStr)) {
						out.add(access);
					}
				}
			}
			results.values = out;
			results.count = out.size();
			return results;
		}

		private String normalize(CharSequence str) {

			return str == null?"":str.toString().toLowerCase().replaceAll("[^a-z0-9]", "");
		}

		@Override
		@SuppressWarnings("unchecked")
		protected void publishResults(CharSequence charSequence, FilterResults filterResults) {

			if (filterResults == null || filterResults.values == null) return;
			fitems = (List<PublicAccess>) filterResults.values;

			clear();
			for (PublicAccess access : fitems) {
				add(access);
			}
		}
	}
}