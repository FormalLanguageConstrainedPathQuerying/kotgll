/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package test.rowset;

import java.util.Locale;
import java.sql.SQLException;
import javax.sql.rowset.RowSetProvider;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8294989
 * @summary Check that the resource bundle can be accessed
 * @throws SQLException if an unexpected error occurs
 * @run testng/othervm
 */
public class ValidateResourceBundleAccess{
    private static final String INVALIDSTATE = "Invalid state";
    private static final String RSREADERERROR = "Internal Error in RowSetReader: no connection or command";

    @BeforeClass
    public void setEnglishEnvironment() {
        Locale.setDefault(Locale.US);
    }

    @Test
    public void testResourceBundleAccess() throws SQLException {
        var rsr = RowSetProvider.newFactory();
        var crs =rsr.createCachedRowSet();
        var jrs = rsr.createJdbcRowSet();
        try {
            jrs.getMetaData();
            throw new RuntimeException("$$$ Expected SQLException was not thrown!");
        } catch (SQLException sqe) {
            assertTrue(sqe.getMessage().equals(INVALIDSTATE));
        }
        try {
            crs.execute();
            throw new RuntimeException("$$$ Expected SQLException was not thrown!");
        } catch (SQLException e) {
            assertTrue(e.getMessage().equals(RSREADERERROR));
        }
    }
}
