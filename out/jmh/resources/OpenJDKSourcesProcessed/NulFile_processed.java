/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8003992 8027155
 * @summary Test a file whose path name is embedded with NUL character, and
 *          ensure it is handled correctly.
 * @author Dan Xu
 */

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.InvalidPathException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;

public class NulFile {

    private static final char CHAR_NUL = '\u0000';

    private static final String ExceptionMsg = "Invalid file path";

    public static void main(String[] args) {
        testFile();
        testFileInUnix();
        testFileInWindows();
        testTempFile();
    }

    private static void testFile() {
        test(new File(new StringBuilder().append(CHAR_NUL).toString()));
        test(new File(
                new StringBuilder().append("").append(CHAR_NUL).toString()));
        test(new File(
                new StringBuilder().append(CHAR_NUL).append("").toString()));
    }

    private static void testFileInUnix() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows"))
            return;

        String unixFile = "/";
        test(unixFile);

        unixFile = "
        test(unixFile);

        unixFile = "data/info";
        test(unixFile);

        unixFile = "/data/info";
        test(unixFile);

        unixFile = "
        test(unixFile);
    }

    private static void testFileInWindows() {
        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Windows"))
            return;

        String windowsFile = "\\";
        test(windowsFile);

        windowsFile = "\\\\";
        test(windowsFile);

        windowsFile = "/";
        test(windowsFile);

        windowsFile = "
        test(windowsFile);

        windowsFile = "/\\";
        test(windowsFile);

        windowsFile = "\\/";
        test(windowsFile);

        windowsFile = "data\\info";
        test(windowsFile);

        windowsFile = "\\data\\info";
        test(windowsFile);

        windowsFile = "\\\\server\\data\\info";
        test(windowsFile);

        windowsFile = "z:data\\info";
        test(windowsFile);

        windowsFile = "z:\\data\\info";
        test(windowsFile);
    }

    private static void test(final String name) {
        int length = name.length();

        for (int i = 0; i <= length; i++) {
            StringBuilder sbName = new StringBuilder(name);
            sbName.insert(i, CHAR_NUL);
            String curName = sbName.toString();

            File testFile = new File(curName, "child");
            test(testFile);
            testFile = new File("parent", curName);
            test(testFile);

            testFile = new File(curName);
            test(testFile);

            testFile = new File(new File(curName), "child");
            test(testFile);
            testFile = new File(new File("parent"), curName);
            test(testFile);

            testFileInputStream(curName);

            testFileOutputStream(curName);

            testRandomAccessFile(curName);
        }
    }

    private static void testFileInputStream(final String str) {
        boolean exceptionThrown = false;
        FileInputStream is = null;
        try {
            is = new FileInputStream(str);
        } catch (FileNotFoundException ex) {
            if (ExceptionMsg.equals(ex.getMessage()))
                exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("FileInputStream constructor"
                    + " should throw FileNotFoundException");
        }
        if (is != null) {
            throw new RuntimeException("FileInputStream constructor"
                    + " should fail");
        }

        exceptionThrown = false;
        is = null;
        try {
            is = new FileInputStream(new File(str));
        } catch (FileNotFoundException ex) {
            if (ExceptionMsg.equals(ex.getMessage()))
                exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("FileInputStream constructor"
                    + " should throw FileNotFoundException");
        }
        if (is != null) {
            throw new RuntimeException("FileInputStream constructor"
                    + " should fail");
        }
    }

    private static void testFileOutputStream(final String str) {
        boolean exceptionThrown = false;
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(str);
        } catch (FileNotFoundException ex) {
            if (ExceptionMsg.equals(ex.getMessage()))
                exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("FileOutputStream constructor"
                    + " should throw FileNotFoundException");
        }
        if (os != null) {
            throw new RuntimeException("FileOutputStream constructor"
                    + " should fail");
        }

        exceptionThrown = false;
        os = null;
        try {
            os = new FileOutputStream(new File(str));
        } catch (FileNotFoundException ex) {
            if (ExceptionMsg.equals(ex.getMessage()))
                exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("FileOutputStream constructor"
                    + " should throw FileNotFoundException");
        }
        if (os != null) {
            throw new RuntimeException("FileOutputStream constructor"
                    + " should fail");
        }
    }

    private static void testRandomAccessFile(final String str) {
        boolean exceptionThrown = false;
        RandomAccessFile raf = null;
        String[] modes = {"r", "rw", "rws", "rwd"};

        for (String mode : modes) {
            try {
                raf = new RandomAccessFile(str, mode);
            } catch (FileNotFoundException ex) {
                if (ExceptionMsg.equals(ex.getMessage()))
                    exceptionThrown = true;
            }
            if (!exceptionThrown) {
                throw new RuntimeException("RandomAccessFile constructor"
                        + " should throw FileNotFoundException");
            }
            if (raf != null) {
                throw new RuntimeException("RandomAccessFile constructor"
                        + " should fail");
            }

            exceptionThrown = false;
            raf = null;
            try {
                raf = new RandomAccessFile(new File(str), mode);
            } catch (FileNotFoundException ex) {
                if (ExceptionMsg.equals(ex.getMessage()))
                    exceptionThrown = true;
            }
            if (!exceptionThrown) {
                throw new RuntimeException("RandomAccessFile constructor"
                        + " should throw FileNotFoundException");
            }
            if (raf != null) {
                throw new RuntimeException("RandomAccessFile constructor"
                        + " should fail");
            }
        }
    }

    private static void test(File testFile) {
        test(testFile, false);
        testSerialization(testFile);
    }

    @SuppressWarnings("deprecation")
    private static void test(File testFile, boolean derived) {
        boolean exceptionThrown = false;

        if (testFile == null) {
            throw new RuntimeException("test file should not be null.");
        }

        if (testFile.getPath().indexOf(CHAR_NUL) < 0) {
            throw new RuntimeException(
                    "File path should contain Nul character");
        }
        if (testFile.getAbsolutePath().indexOf(CHAR_NUL) < 0) {
            throw new RuntimeException(
                    "File absolute path should contain Nul character");
        }
        File derivedAbsFile = testFile.getAbsoluteFile();
        if (derived) {
            if (derivedAbsFile.getPath().indexOf(CHAR_NUL) < 0) {
                throw new RuntimeException(
                        "Derived file path should also contain Nul character");
            }
        } else {
            test(derivedAbsFile, true);
        }
        try {
            exceptionThrown = false;
            testFile.getCanonicalPath();
        } catch (IOException ex) {
            if (ExceptionMsg.equals(ex.getMessage()))
                exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException(
                    "getCanonicalPath() should throw IOException with"
                        + " message \"" + ExceptionMsg + "\"");
        }
        try {
            exceptionThrown = false;
            testFile.getCanonicalFile();
        } catch (IOException ex) {
            if (ExceptionMsg.equals(ex.getMessage()))
                exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException(
                    "getCanonicalFile() should throw IOException with"
                        + " message \"" + ExceptionMsg + "\"");
        }
        try {
            exceptionThrown = false;
            testFile.toURL();
        } catch (MalformedURLException ex) {
            if (ExceptionMsg.equals(ex.getMessage()))
                exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("toURL() should throw IOException with"
                + " message \"" + ExceptionMsg + "\"");
        }
        if (testFile.canRead())
            throw new RuntimeException("File should not be readable");
        if (testFile.canWrite())
            throw new RuntimeException("File should not be writable");
        if (testFile.exists())
            throw new RuntimeException("File should not be existed");
        if (testFile.isDirectory())
            throw new RuntimeException("File should not be a directory");
        if (testFile.isFile())
            throw new RuntimeException("File should not be a file");
        if (testFile.isHidden())
            throw new RuntimeException("File should not be hidden");
        if (testFile.lastModified() != 0L)
            throw new RuntimeException("File last modified time should be 0L");
        if (testFile.length() != 0L)
            throw new RuntimeException("File length should be 0L");
        try {
            exceptionThrown = false;
            testFile.createNewFile();
        } catch (IOException ex) {
            if (ExceptionMsg.equals(ex.getMessage()))
                exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException(
                    "createNewFile() should throw IOException with"
                        + " message \"" + ExceptionMsg + "\"");
        }
        if (testFile.delete())
            throw new RuntimeException("Delete operation should fail");
        if (testFile.list() != null)
            throw new RuntimeException("File list() should return null");
        FilenameFilter fnFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return false;
            }
        };
        if (testFile.list(fnFilter) != null) {
            throw new RuntimeException("File list(FilenameFilter) should"
                + " return null");
        }
        if (testFile.listFiles() != null)
            throw new RuntimeException("File listFiles() should return null");
        if (testFile.listFiles(fnFilter) != null) {
            throw new RuntimeException("File listFiles(FilenameFilter)"
                + " should return null");
        }
        FileFilter fFilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                return false;
            }
        };
        if (testFile.listFiles(fFilter) != null) {
            throw new RuntimeException("File listFiles(FileFilter)"
                + " should return null");
        }
        if (testFile.mkdir()) {
            throw new RuntimeException("File should not be able to"
                + " create directory");
        }
        if (testFile.mkdirs()) {
            throw new RuntimeException("File should not be able to"
                + " create directories");
        }
        if (testFile.renameTo(new File("dest")))
            throw new RuntimeException("File rename should fail");
        if (new File("dest").renameTo(testFile))
            throw new RuntimeException("File rename should fail");
        try {
            exceptionThrown = false;
            testFile.renameTo(null);
        } catch (NullPointerException ex) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("File rename should thrown NPE");
        }
        if (testFile.setLastModified(0L)) {
            throw new RuntimeException("File should fail to set"
                + " last modified time");
        }
        try {
            exceptionThrown = false;
            testFile.setLastModified(-1);
        } catch (IllegalArgumentException ex) {
            if ("Negative time".equals(ex.getMessage()))
                exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("File should fail to set"
                + " last modified time with message \"Negative time\"");
        }
        if (testFile.setReadOnly())
            throw new RuntimeException("File should fail to set read-only");
        if (testFile.setWritable(true, true))
            throw new RuntimeException("File should fail to set writable");
        if (testFile.setWritable(true, false))
            throw new RuntimeException("File should fail to set writable");
        if (testFile.setWritable(false, true))
            throw new RuntimeException("File should fail to set writable");
        if (testFile.setWritable(false, false))
            throw new RuntimeException("File should fail to set writable");
        if (testFile.setWritable(false))
            throw new RuntimeException("File should fail to set writable");
        if (testFile.setWritable(true))
            throw new RuntimeException("File should fail to set writable");
        if (testFile.setReadable(true, true))
            throw new RuntimeException("File should fail to set readable");
        if (testFile.setReadable(true, false))
            throw new RuntimeException("File should fail to set readable");
        if (testFile.setReadable(false, true))
            throw new RuntimeException("File should fail to set readable");
        if (testFile.setReadable(false, false))
            throw new RuntimeException("File should fail to set readable");
        if (testFile.setReadable(false))
            throw new RuntimeException("File should fail to set readable");
        if (testFile.setReadable(true))
            throw new RuntimeException("File should fail to set readable");
        if (testFile.setExecutable(true, true))
            throw new RuntimeException("File should fail to set executable");
        if (testFile.setExecutable(true, false))
            throw new RuntimeException("File should fail to set executable");
        if (testFile.setExecutable(false, true))
            throw new RuntimeException("File should fail to set executable");
        if (testFile.setExecutable(false, false))
            throw new RuntimeException("File should fail to set executable");
        if (testFile.setExecutable(false))
            throw new RuntimeException("File should fail to set executable");
        if (testFile.setExecutable(true))
            throw new RuntimeException("File should fail to set executable");
        if (testFile.canExecute())
            throw new RuntimeException("File should not be executable");
        if (testFile.getTotalSpace() != 0L)
            throw new RuntimeException("The total space should be 0L");
        if (testFile.getFreeSpace() != 0L)
            throw new RuntimeException("The free space should be 0L");
        if (testFile.getUsableSpace() != 0L)
            throw new RuntimeException("The usable space should be 0L");
        try {
            exceptionThrown = false;
            testFile.compareTo(null);
        } catch (NullPointerException ex) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("compareTo(null) should throw NPE");
        }
        if (testFile.toString().indexOf(CHAR_NUL) < 0) {
            throw new RuntimeException(
                    "File path should contain Nul character");
        }
        try {
            exceptionThrown = false;
            testFile.toPath();
        } catch (InvalidPathException ex) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("toPath() should throw"
                + " InvalidPathException");
        }
    }

    private static void testSerialization(File testFile) {
        String path = testFile.getPath();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(testFile);
            oos.close();
            byte[] bytes = baos.toByteArray();
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            ObjectInputStream ois = new ObjectInputStream(is);
            File newFile = (File) ois.readObject();
            String newPath = newFile.getPath();
            if (!path.equals(newPath)) {
                throw new RuntimeException(
                        "Serialization should not change file path");
            }
            test(newFile, false);
        } catch (IOException | ClassNotFoundException ex) {
            System.err.println("Exception happens in testSerialization");
            System.err.println(ex.getMessage());
        }
    }

    private static void testTempFile() {
        final String[] names = {"x", "xx", "xxx", "xxxx"};
        final String shortPrefix = "sp";
        final String prefix = "prefix";
        final String suffix = "suffix";
        File tmpDir = new File("tmpDir");

        for (String name : names) {
            int length = name.length();
            for (int i = 0; i <= length; i++) {
                StringBuilder sbName = new StringBuilder(name);
                sbName.insert(i, CHAR_NUL);
                String curName = sbName.toString();

                testCreateTempFile(curName, suffix, tmpDir);
                testCreateTempFile(shortPrefix, curName, tmpDir);
                testCreateTempFile(prefix, curName, tmpDir);
                testCreateTempFile(shortPrefix, suffix, new File(curName));
                testCreateTempFile(prefix, suffix, new File(curName));
            }
        }
    }

    private static void testCreateTempFile(String prefix, String suffix,
                                           File directory) {
        boolean exceptionThrown = false;
        boolean shortPrefix = (prefix.length() < 3);
        if (shortPrefix) {
            try {
                File.createTempFile(prefix, suffix, directory);
            } catch (IllegalArgumentException ex) {
                String actual = ex.getMessage();
                String expected = "Prefix string \"" + prefix +
                    "\" too short: length must be at least 3";
                if (actual != null && actual.equals(expected))
                    exceptionThrown = true;
            } catch (IOException ioe) {
                System.err.println("IOException happens in testCreateTempFile");
                System.err.println(ioe.getMessage());
            }
        } else {
            try {
                File.createTempFile(prefix, suffix, directory);
            } catch (IOException ex) {
                String err = "Unable to create temporary file";
                if (ex.getMessage() != null && ex.getMessage().startsWith(err))
                    exceptionThrown = true;
                else {
                    throw new RuntimeException("Get IOException with message, "
                            + ex.getMessage() + ", expect message, "+ err);
                }
            }
        }
        if (!exceptionThrown) {
            throw new RuntimeException("createTempFile() should throw"
                    + (shortPrefix ? " IllegalArgumentException"
                                   : " IOException"));
        }
    }
}
