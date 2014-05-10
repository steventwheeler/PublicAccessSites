package com.wheelermarine.publicAccessSites.dbase;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;

/**
 * <p>
 * This class represents a single record in the database.  Individual field
 * values can be retrieved using <code>getValue(String)</code>.
 * </p>
 * <p/>
 * <p>
 * Copyright 2013 Steven Wheeler<br/>
 * Released under the GPLv3 license, see LICENSE file for details.
 * </p>
 */
public class Record {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	private final LinkedHashMap<String, Object> values;

	/**
	 * Load the next record from the data source.
	 *
	 * @param header the database Header.
	 * @param in     the data source.
	 * @throws IOException  if there is an error reading the record.
	 * @throws EOFException if there are no more records.
	 */
	public Record(Header header, DataInput in) throws IOException {

		values = new LinkedHashMap<String, Object>(header.getFields().size());

		boolean isDeleted = false;
		do {
			if (isDeleted) in.skipBytes(header.getRecordLength() - 1);
			byte b = in.readByte();
			// Check to see if the end of the file has been reached.
			if (b == 0x1A) throw new EOFException();

			// Check to see if the current record has been deleted.
			isDeleted = b == 0x2a;
		} while (isDeleted);

		// Read the record data.
		for (Field field : header.getFields()) {
			byte[] bytes = new byte[field.getFieldLength()];
			in.readFully(bytes);
			String value = new String(bytes, "ASCII").trim();
			switch (field.getType()) {
				case CHARACTER:
					values.put(field.getFieldName(), value);
					break;
				case DATE:
					try {
						if (!value.isEmpty()) {
							values.put(field.getFieldName(), dateFormat.parse(value));
						}
					} catch (ParseException e) {
						throw new IOException(e);
					}
					break;
				case FLOAT:
				case NUMERIC:
					if (!value.isEmpty() && !value.contains("?")) {
						values.put(field.getFieldName(), new Double(value));
					}
					break;
				case LOGICAL:
					values.put(field.getFieldName(), value.matches("(Y|y|T|t)"));
					break;
				default:
					throw new IllegalArgumentException("Unknown type: " + field.getType());
			}
		}
	}

	public Object getValue(String name) {

		return values.get(name);
	}

	@Override
	public String toString() {

		return "Record: " + values;
	}

	/**
	 * Read the next Record from the data source.  If no record is available
	 * then <code>null</code> is returned.
	 *
	 * @param header the database Header.
	 * @param in     the data source
	 * @return the next available record or <code>null</code> if no records are
	 *         available.
	 * @throws IOException if there is an error reading the next record.
	 */
	public static Record read(Header header, DataInput in) throws IOException {

		try {
			return new Record(header, in);
		} catch (EOFException e) {
			return null;
		}
	}
}