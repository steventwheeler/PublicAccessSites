package com.wheelermarine.publicAccessSites.dbase;

import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;

/**
 * <p>
 * This class represents a database Field.  It contains the name of the
 * Field as well as the location of the Field in each record.
 * </p>
 * <p/>
 * <p>
 * Copyright 2013 Steven Wheeler<br/>
 * Released under the GPLv3 license, see LICENSE file for details.
 * </p>
 */
public class Field {

	/**
	 * This enum describes the type of data stored in a Field.
	 */
	public static enum FieldType {
		CHARACTER("C"),
		LOGICAL("L"),
		NUMERIC("N"),
		FLOAT("F"),
		DATE("D"),
		MEMO("M");

		private final String flag;

		private FieldType(String flag) {

			this.flag = flag;
		}

		public String getFlag() {

			return flag;
		}

		public static FieldType getType(String flag) {

			for (FieldType type : values()) {
				if (type.getFlag().equals(flag)) {
					return type;
				}
			}
			return null;
		}
	}

	/**
	 * Field name in ASCII (zero-filled).
	 * 11 bytes, offset: 0
	 */
	private final String fieldName;

	/**
	 * Field type in ASCII (B, C, D, F, G, L, M, or N).
	 * 1 byte, offset: 11
	 */
	private final FieldType type;

	/**
	 * Reserved.
	 * 4 bytes, offset: 12
	 */
	private final int reserved1;

	/**
	 * Field length in binary.
	 * 1 byte, offset: 16
	 */
	private final int fieldLength;

	/**
	 * Field decimal count in binary.
	 * 1 byte, offset: 17
	 */
	private final byte decimalCount;

	/**
	 * Reserved.
	 * 2 bytes, offset: 18
	 */
	private final int reserved2;

	/**
	 * Work area ID.
	 * 1 byte, offset 20
	 */
	private final byte workAreaID;

	/**
	 * Reserved.
	 * 10 bytes, offset: 21
	 */
	private final byte[] reserved3;

	/**
	 * Production .MDX field flag; 01h if field has an index tag in the
	 * production .MDX file; 00h if the field is not indexed.
	 * 1 byte, offset: 31
	 */
	private final byte indexFieldFlag; /* 31 */

	/**
	 * Load a Field from the database.
	 *
	 * @param in the source of the database.
	 * @throws IOException  if there is an error reading the Field.
	 * @throws EOFException if the end of the Field array has been reached.
	 */
	public Field(DataInput in) throws IOException {

		// Check to see if this is the end of the header data.
		byte b = in.readByte();
		if (b == 0x0d) throw new EOFException();

		byte[] nameBytes = new byte[11];
		nameBytes[0] = b;
		in.readFully(nameBytes, 1, 10);
		fieldName = new String(nameBytes, "ASCII").trim();

		byte[] typeBytes = new byte[1];
		in.readFully(typeBytes);
		type = FieldType.getType(new String(typeBytes, "ASCII"));

		reserved1 = Header.readLittleEndian(in, 4);
		fieldLength = in.readUnsignedByte();
		decimalCount = in.readByte();
		reserved2 = Header.readLittleEndian(in, 2);
		workAreaID = in.readByte();
		in.readFully(reserved3 = new byte[10]);
		indexFieldFlag = in.readByte();
	}

	public String getFieldName() {

		return fieldName;
	}

	public FieldType getType() {

		return type;
	}

	public int getReserved1() {

		return reserved1;
	}

	public int getFieldLength() {

		return fieldLength;
	}

	public byte getDecimalCount() {

		return decimalCount;
	}

	public int getReserved2() {

		return reserved2;
	}

	public byte getWorkAreaID() {

		return workAreaID;
	}

	public byte[] getReserved3() {

		return reserved3;
	}

	public byte getIndexFieldFlag() {

		return indexFieldFlag;
	}

	public static Field read(DataInput in) throws IOException {

		try {
			return new Field(in);
		} catch (EOFException e) {
			return null;
		}
	}
}