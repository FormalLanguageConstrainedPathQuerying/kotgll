/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.rowset;

import java.sql.*;
import javax.sql.*;
import java.io.*;
import java.math.*;
import java.util.*;
import java.text.*;

import org.xml.sax.*;

import javax.sql.rowset.*;
import javax.sql.rowset.spi.*;

import com.sun.rowset.providers.*;
import com.sun.rowset.internal.*;

/**
 * The standard implementation of the <code>WebRowSet</code> interface. See the interface
 * definition for full behavior and implementation requirements.
 *
 * @author Jonathan Bruce, Amit Handa
 */
public class WebRowSetImpl extends CachedRowSetImpl implements WebRowSet {

    /**
     * The <code>WebRowSetXmlReader</code> object that this
     * <code>WebRowSet</code> object will call when the method
     * <code>WebRowSet.readXml</code> is invoked.
     */
    private WebRowSetXmlReader xmlReader;

    /**
     * The <code>WebRowSetXmlWriter</code> object that this
     * <code>WebRowSet</code> object will call when the method
     * <code>WebRowSet.writeXml</code> is invoked.
     */
    private WebRowSetXmlWriter xmlWriter;

    /* This stores the cursor position prior to calling the writeXML.
     * This variable is used after the write to restore the position
     * to the point where the writeXml was called.
     */
    private int curPosBfrWrite;

    @SuppressWarnings("serial")
    private SyncProvider provider;

    /**
     * Constructs a new <code>WebRowSet</code> object initialized with the
     * default values for a <code>CachedRowSet</code> object instance. This
     * provides the <code>RIOptimistic</code> provider to deliver
     * synchronization capabilities to relational datastores and a default
     * <code>WebRowSetXmlReader</code> object and a default
     * <code>WebRowSetXmlWriter</code> object to enable XML output
     * capabilities.
     *
     * @throws SQLException if an error occurs in configuring the default
     * synchronization providers for relational and XML providers.
     */
    public WebRowSetImpl() throws SQLException {
        super();

        xmlReader = new WebRowSetXmlReader();
        xmlWriter = new WebRowSetXmlWriter();
    }

    /**
     * Constructs a new <code>WebRowSet</code> object initialized with the
     * synchronization SPI provider properties as specified in the <code>Hashtable</code>. If
     * this hashtable is empty or is <code>null</code> the default constructor is invoked.
     *
     * @throws SQLException if an error occurs in configuring the specified
     * synchronization providers for the relational and XML providers; or
     * if the Hashtanle is null
     */
    @SuppressWarnings("rawtypes")
    public WebRowSetImpl(Hashtable env) throws SQLException {

        try {
           resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }

        if ( env == null) {
            throw new SQLException(resBundle.handleGetObject("webrowsetimpl.nullhash").toString());
        }

        String providerName =
            (String)env.get(javax.sql.rowset.spi.SyncFactory.ROWSET_SYNC_PROVIDER);

        provider = SyncFactory.getInstance(providerName);

    }

   /**
    * Populates this <code>WebRowSet</code> object with the
    * data in the given <code>ResultSet</code> object and writes itself
    * to the given <code>java.io.Writer</code> object in XML format.
    * This includes the rowset's data,  properties, and metadata.
    *
    * @throws SQLException if an error occurs writing out the rowset
    *          contents to XML
    */
    public void writeXml(ResultSet rs, java.io.Writer writer)
        throws SQLException {
            this.populate(rs);

            curPosBfrWrite = this.getRow();

            this.writeXml(writer);
    }

    /**
     * Writes this <code>WebRowSet</code> object to the given
     * <code>java.io.Writer</code> object in XML format. This
     * includes the rowset's data,  properties, and metadata.
     *
     * @throws SQLException if an error occurs writing out the rowset
     *          contents to XML
     */
    public void writeXml(java.io.Writer writer) throws SQLException {
        if (xmlWriter != null) {

            curPosBfrWrite = this.getRow();

            xmlWriter.writeXML(this, writer);
        } else {
            throw new SQLException(resBundle.handleGetObject("webrowsetimpl.invalidwr").toString());
        }
    }

    /**
     * Reads this <code>WebRowSet</code> object in its XML format.
     *
     * @throws SQLException if a database access error occurs
     */
    public void readXml(java.io.Reader reader) throws SQLException {
        try {
             if (reader != null) {
                xmlReader.readXML(this, reader);

                if(curPosBfrWrite == 0) {
                   this.beforeFirst();
                }

                else {
                   this.absolute(curPosBfrWrite);
                }

            } else {
                throw new SQLException(resBundle.handleGetObject("webrowsetimpl.invalidrd").toString());
            }
        } catch (Exception e) {
            throw new SQLException(e.getMessage());
        }
    }

    /**
     * Reads a stream based XML input to populate this <code>WebRowSet</code>
     * object.
     *
     * @throws SQLException if a data source access error occurs
     * @throws IOException if a IO exception occurs
     */
    public void readXml(java.io.InputStream iStream) throws SQLException, IOException {
        if (iStream != null) {
            xmlReader.readXML(this, iStream);

                if(curPosBfrWrite == 0) {
                   this.beforeFirst();
                }

                else {
                   this.absolute(curPosBfrWrite);
                }

        } else {
            throw new SQLException(resBundle.handleGetObject("webrowsetimpl.invalidrd").toString());
        }
    }

    /**
     * Writes this <code>WebRowSet</code> object to the given <code> OutputStream</code>
     * object in XML format.
     * Creates an output stream of the internal state and contents of a
     * <code>WebRowSet</code> for XML proceessing
     *
     * @throws SQLException if a datasource access error occurs
     * @throws IOException if an IO exception occurs
     */
    public void writeXml(java.io.OutputStream oStream) throws SQLException, IOException {
        if (xmlWriter != null) {

            curPosBfrWrite = this.getRow();

            xmlWriter.writeXML(this, oStream);
        } else {
            throw new SQLException(resBundle.handleGetObject("webrowsetimpl.invalidwr").toString());
        }

    }

    /**
     * Populates this <code>WebRowSet</code> object with the
     * data in the given <code>ResultSet</code> object and writes itself
     * to the given <code>java.io.OutputStream</code> object in XML format.
     * This includes the rowset's data,  properties, and metadata.
     *
     * @throws SQLException if a datasource access error occurs
     * @throws IOException if an IO exception occurs
     */
    public void writeXml(ResultSet rs, java.io.OutputStream oStream) throws SQLException, IOException {
            this.populate(rs);

            curPosBfrWrite = this.getRow();

            this.writeXml(oStream);
    }

    /**
     * This method re populates the resBundle
     * during the deserialization process
     *
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        try {
           resBundle = JdbcRowSetResourceBundle.getJdbcRowSetResourceBundle();
        } catch(IOException ioe) {
            throw new RuntimeException(ioe);
        }

    }

    static final long serialVersionUID = -8771775154092422943L;
}
