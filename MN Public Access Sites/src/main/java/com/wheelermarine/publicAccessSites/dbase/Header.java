package com.wheelermarine.publicAccessSites.dbase;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * <p>
 * This class represents the database header, it contains various metadata
 * about the contents of a database.
 * </p>
 * <p/>
 * <p>
 * Copyright 2013 Steven Wheeler<br/>
 * Released under the GPLv3 license, see LICENSE file for details.
 * </p>
 */
public class Header {

    /**
     * Valid dBASE IV file; bits 0-2 indicate version number, bit 3 the presence
     * of a dBASE IV memo file, bits 4-6 the presence of an SQL table, bit 7 the
     * presence of any memo file (either dBASE III PLUS or dBASE IV).
     * 1 byte, offset: 0
     */
    private final byte signature;

    /**
     * Date of last update; formatted as YYMMDD.
     * 3 bytes, offset: 1
     */
    private final Date lastUpdate;

    /**
     * Number of records in the file.
     * 4 bytes, offset: 4
     */
    private final int numberOfRecords;

    /**
     * Number of bytes in the header.
     * 2 bytes, offset: 8
     */
    private final int headerLength;

    /**
     * Number of bytes in the record.
     * 2 bytes, offset: 10
     */
    private final int recordLength;

    /**
     * Reserved; fill with 0.
     * 2 bytes, offset 12
     */
    private final int reserved1;

    /**
     * Flag indicating incomplete transaction.
     * 1 byte, offset: 14
     */
    private final byte incompleteTransaction;

    /**
     * Encryption flag.
     * 1 byte, offset: 15
     */
    private final byte encryptionFlag;

    /**
     * Reserved for dBASE IV in a multi-user environment.
     * 12 bytes, offset: 16
     */
    private final byte[] reserved2;

    /**
     * Production MDX flag; 01h stored in this byte if a production .MDX file
     * exists for this table; 00h if no .MDX file exists.
     * 1 byte, offset: 28
     */
    private final byte mdxFlag;

    /**
     * Language driver ID.
     * 1 byte, offset: 29
     */
    private final byte languageDriver;

    /**
     * Reserved; filled with zeros.
     * 2 bytes, offset: 30
     */
    private final int reserved3;

    /**
     * Field descriptor array.
     * 32 bytes each
     */
    private final List<Field> fields;

    /**
     * 0Dh stored as the field terminator.
     */
    private final byte terminator;

    /**
     * Parse the header data from the data source.
     *
     * @param in the source to read the database from.
     * @throws IOException if there is an error reading the data.
     */
    public Header(DataInput in) throws IOException {

        signature = in.readByte();
        lastUpdate = new GregorianCalendar(in.readByte() + 1900, in.readByte(), in.readByte()).getTime();
        numberOfRecords = readLittleEndian(in, 4);
        headerLength = readLittleEndian(in, 2);
        recordLength = readLittleEndian(in, 2);
        reserved1 = readLittleEndian(in, 2);
        incompleteTransaction = in.readByte();
        encryptionFlag = in.readByte();
        in.readFully(reserved2 = new byte[12]);
        mdxFlag = in.readByte();
        languageDriver = in.readByte();
        reserved3 = readLittleEndian(in, 2);

        List<Field> fields = new ArrayList<>();
        Field field;
        while ((field = Field.read(in)) != null) {
            fields.add(field);
        }
        this.fields = Collections.unmodifiableList(fields);
        // The actual terminator bit is lost when parsing the fields.
        terminator = 0x0d;
    }

    public byte getSignature() {

        return signature;
    }

    public Date getLastUpdate() {

        return lastUpdate;
    }

    public int getNumberOfRecords() {

        return numberOfRecords;
    }

    public int getHeaderLength() {

        return headerLength;
    }

    public int getRecordLength() {

        return recordLength;
    }

    public int getReserved1() {

        return reserved1;
    }

    public byte getIncompleteTransaction() {

        return incompleteTransaction;
    }

    public byte getEncryptionFlag() {

        return encryptionFlag;
    }

    public byte[] getReserved2() {

        return reserved2;
    }

    public byte getMdxFlag() {

        return mdxFlag;
    }

    public byte getLanguageDriver() {

        return languageDriver;
    }

    public int getReserved3() {

        return reserved3;
    }

    public List<Field> getFields() {

        return fields;
    }

    public byte getTerminator() {

        return terminator;
    }

    public static int readLittleEndian(DataInput in, int bytes) throws IOException {

        if (bytes <= 0)
            throw new IllegalArgumentException("Number of bytes must be a positive integer.");
        if (bytes > 4)
            throw new IllegalArgumentException("Number of bytes cannot be greater than 4.");
        int bigEndian = 0;
        for (int shift = 0; shift < 8 * bytes; shift += 8) {
            bigEndian |= (in.readUnsignedByte() & 0xff) << shift;
        }
        return bigEndian;
    }
}