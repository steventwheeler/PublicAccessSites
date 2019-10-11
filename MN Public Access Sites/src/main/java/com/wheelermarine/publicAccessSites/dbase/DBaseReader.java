package com.wheelermarine.publicAccessSites.dbase;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * <p>
 * Based on the file format documented at
 * <a href="http://ulisse.elettra.trieste.it/services/doc/dbase/DBFstruct.htm#C4">http://ulisse.elettra.trieste.it/services/doc/dbase/DBFstruct.htm#C4</a>
 * This class simplifies reading data from DBase files.  Example:
 * </p>
 * <p/>
 * <pre>
 *     FileInputStream in = new FileInputStream("myfile.dbf");
 *     try {
 *         DBaseReader reader = new DBaseReader(in);
 *         for (Record record: reader) {
 *             // Process record...
 *         }
 *     } finally {
 *         in.close();
 *     }
 * </pre>
 * <p/>
 * <p>
 * Copyright 2013 Steven Wheeler<br/>
 * Released under the GPLv3 license, see LICENSE file for details.
 * </p>
 */
public class DBaseReader implements Iterable<Record>, Iterator<Record> {

    private final DataInput in;
    private final Header header;
    private Record next;

    /**
     * Create a new DBaseReader which will read the database records from the
     * provided InputStream.
     *
     * @param in the InputStream to read the records from.
     * @throws IOException if there is an error reading the records.
     */
    public DBaseReader(InputStream in) throws IOException {

        this.in = new DataInputStream(in);
        header = new Header(this.in);
        next = Record.read(header, this.in);
    }

    /**
     * Get the total number of records in the database.
     *
     * @return the number of records.
     */
    public int size() {

        return header.getNumberOfRecords();
    }

    /**
     * Get the Field at the provided index.
     *
     * @param index the index of the Field.
     * @return the Field at <code>index</code>.
     */
    public Field getField(int index) {

        return header.getFields().get(index);
    }

    /**
     * Get the total number of Fields in the database.
     *
     * @return the number of Fields.
     */
    public int getFieldCount() {

        return header.getFields().size();
    }

    /**
     * Get the Header which contains metadata regarding the database.
     *
     * @return the database metadata.
     */
    public Header getHeader() {

        return header;
    }

    @Override
    public boolean hasNext() {

        return next != null;
    }

    @Override
    public Record next() {

        Record r = next;
        try {
            next = Record.read(header, in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return r;
    }

    @Override
    public void remove() {

        throw new UnsupportedOperationException("Not implemented.");
    }

    @Override
    public Iterator<Record> iterator() {

        return this;
    }
}