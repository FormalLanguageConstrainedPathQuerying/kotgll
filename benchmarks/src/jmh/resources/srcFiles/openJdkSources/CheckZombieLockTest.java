/*
 * Copyright (c) 2014, 2022, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug     8048020
 * @author  Daniel Fuchs
 * @summary Regression on java.util.logging.FileHandler.
 *     The fix is to avoid filling up the file system with zombie lock files.
 *
 * @run  main/othervm CheckZombieLockTest WRITABLE CLOSE CLEANUP
 * @run  main/othervm CheckZombieLockTest CLEANUP
 * @run  main/othervm CheckZombieLockTest WRITABLE
 * @run  main/othervm CheckZombieLockTest CREATE_FIRST
 * @run  main/othervm CheckZombieLockTest CREATE_NEXT
 * @run  main/othervm CheckZombieLockTest CREATE_NEXT
 * @run  main/othervm CheckZombieLockTest CLEANUP
 * @run  main/othervm CheckZombieLockTest REUSE
 * @run  main/othervm CheckZombieLockTest CLEANUP
 * @key randomness
 */
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import static java.nio.file.StandardOpenOption.*;

public class CheckZombieLockTest {

    private static final String WRITABLE_DIR = "writable-lockfile-dir";
    private static volatile boolean supportsLocking = true;

    static enum TestCase {
        WRITABLE,  
        CLOSE, 
        CREATE_FIRST, 
        CREATE_NEXT, 
        REUSE, 
        CLEANUP 
    };

    public static void main(String... args) throws IOException {
        File writableDir = setup();
        System.out.println("Writable dir is: " + writableDir.getAbsolutePath());
        if (args == null || args.length == 0) {
            args = new String[] { "WRITABLE", "CLOSE", "CLEANUP" };
        }
        try {
            runTests(writableDir, args);
        } catch (RuntimeException | IOException | Error x) {
            delete(writableDir);
            throw x;
        }
    }

    /**
     * @param writableDir in which log and lock file are created
     * @throws SecurityException
     * @throws RuntimeException
     * @throws IOException
     */
    private static void runTests(File writableDir, String... args) throws SecurityException,
            RuntimeException, IOException {
        for (String arg : args) {
            switch(TestCase.valueOf(arg)) {
                case WRITABLE: checkWritable(writableDir); break;
                case CLOSE: testFileHandlerClose(writableDir); break;
                case CREATE_FIRST: testFileHandlerCreate(writableDir, true); break;
                case CREATE_NEXT: testFileHandlerCreate(writableDir, false); break;
                case REUSE: testFileHandlerReuse(writableDir); break;
                case CLEANUP: delete(writableDir); break;
                default: throw new RuntimeException("No such test case: " + arg);
            }
        }
    }

    /**
     * @param writableDir in which log and lock file are created
     * @throws SecurityException
     * @throws RuntimeException
     * @throws IOException
     */
    private static void checkWritable(File writableDir) throws SecurityException,
            RuntimeException, IOException {
        final File file = new File(writableDir, "test.txt");
        if (!createFile(file, false)) {
            throw new IOException("Can't create " + file + "\n\tUnable to run test");
        } else {
            delete(file);
        }
    }


    private static FileHandler createFileHandler(File writableDir) throws SecurityException,
            RuntimeException, IOException {
        try {
            FileHandler handler = new FileHandler("%t/" + WRITABLE_DIR + "/log.log");
            handler.publish(new LogRecord(Level.INFO, handler.toString()));
            handler.flush();
            return handler;
        } catch (IOException ex) {
            throw new RuntimeException("Test failed: should have been able"
                    + " to create FileHandler for " + "%t/" + WRITABLE_DIR
                    + "/log.log in writable directory.", ex);
        }
    }

    private static List<File> listLocks(File writableDir, boolean print)
            throws IOException {
        List<File> locks = new ArrayList<>();
        for (File f : writableDir.listFiles()) {
            if (print) {
                System.out.println("Found file: " + f.getName());
            }
            if (f.getName().endsWith(".lck")) {
                locks.add(f);
            }
        }
        return locks;
    }

    private static void testFileHandlerClose(File writableDir) throws IOException {
        File fakeLock = new File(writableDir, "log.log.lck");
        if (!createFile(fakeLock, false)) {
            throw new IOException("Can't create fake lock file: " + fakeLock);
        }
        try {
            List<File> before = listLocks(writableDir, true);
            System.out.println("before: " + before.size() + " locks found");
            FileHandler handler = createFileHandler(writableDir);
            System.out.println("handler created: " + handler);
            List<File> after = listLocks(writableDir, true);
            System.out.println("after creating handler: " + after.size() + " locks found");
            handler.close();
            System.out.println("handler closed: " + handler);
            List<File> afterClose = listLocks(writableDir, true);
            System.out.println("after closing handler: " + afterClose.size() + " locks found");
            afterClose.removeAll(before);
            if (!afterClose.isEmpty()) {
                throw new RuntimeException("Zombie lock file detected: " + afterClose);
            }
        } finally {
            if (fakeLock.canRead()) delete(fakeLock);
        }
        List<File> finalLocks = listLocks(writableDir, false);
        System.out.println("After cleanup: " + finalLocks.size() + " locks found");
    }


    private static void testFileHandlerReuse(File writableDir) throws IOException {
        List<File> before = listLocks(writableDir, true);
        System.out.println("before: " + before.size() + " locks found");
        try {
            if (!before.isEmpty()) {
                throw new RuntimeException("Expected no lock file! Found: " + before);
            }
        } finally {
            before.stream().forEach(CheckZombieLockTest::delete);
        }

        FileHandler handler1 = createFileHandler(writableDir);
        System.out.println("handler created: " + handler1);
        List<File> after = listLocks(writableDir, true);
        System.out.println("after creating handler: " + after.size() + " locks found");
        if (after.size() != 1) {
            throw new RuntimeException("Unexpected number of lock files found for "
                    + handler1 + ": " + after);
        }
        final File lock = after.get(0);
        after.clear();
        handler1.close();
        after = listLocks(writableDir, true);
        System.out.println("after closing handler: " + after.size() + " locks found");
        if (!after.isEmpty()) {
            throw new RuntimeException("Unexpected number of lock files found for "
                    + handler1 + ": " + after);
        }
        if (!createFile(lock, false)) {
            throw new IOException("Can't create fake lock file: " + lock);
        }
        try {
            before = listLocks(writableDir, true);
            System.out.println("before: " + before.size() + " locks found");
            if (before.size() != 1) {
                throw new RuntimeException("Unexpected number of lock files found: "
                        + before + " expected [" + lock + "].");
            }
            FileHandler handler2 = createFileHandler(writableDir);
            System.out.println("handler created: " + handler2);
            after = listLocks(writableDir, true);
            System.out.println("after creating handler: " + after.size() + " locks found");
            after.removeAll(before);
            if (!after.isEmpty()) {
                throw new RuntimeException("Unexpected lock file found: " + after
                        + "\n\t" + lock + " should have been reused");
            }
            handler2.close();
            System.out.println("handler closed: " + handler2);
            List<File> afterClose = listLocks(writableDir, true);
            System.out.println("after closing handler: " + afterClose.size() + " locks found");
            if (!afterClose.isEmpty()) {
                throw new RuntimeException("Zombie lock file detected: " + afterClose);
            }

            if (supportsLocking) {
                handler2 = null;
                try (FileChannel fc = FileChannel.open(lock.toPath(), CREATE_NEW, APPEND, WRITE)) {

                    if (fc.tryLock() != null) {
                        System.out.println("locked: " + lock);
                        handler2 = createFileHandler(writableDir);
                        System.out.println("handler created: " + handler2);
                        after = listLocks(writableDir, true);
                        System.out.println("after creating handler: " + after.size()
                                + " locks found");
                        after.removeAll(before);
                        if (after.size() != 1) {
                            throw new RuntimeException("Unexpected lock files found: " + after
                                + "\n\t" + lock + " should not have been reused");
                        }
                    } else {
                        throw new RuntimeException("Failed to lock: " + lock);
                    }
                } finally {
                    if (handler2 != null) handler2.close();
                    delete(lock);
                }
            }
        } finally {
            List<File> finalLocks = listLocks(writableDir, false);
            System.out.println("end: " + finalLocks.size() + " locks found");
            delete(writableDir);
        }
    }


    private static void testFileHandlerCreate(File writableDir, boolean first)
            throws IOException {
        List<File> before = listLocks(writableDir, true);
        System.out.println("before: " + before.size() + " locks found");
        try {
            if (first && !before.isEmpty()) {
                throw new RuntimeException("Expected no lock file! Found: " + before);
            } else if (!first && before.size() != 1) {
                throw new RuntimeException("Expected a single lock file! Found: " + before);
            }
        } finally {
            before.stream().forEach(CheckZombieLockTest::delete);
        }
        FileHandler handler = createFileHandler(writableDir);
        System.out.println("handler created: " + handler);
        List<File> after = listLocks(writableDir, true);
        System.out.println("after creating handler: " + after.size() + " locks found");
        if (after.size() != 1) {
            throw new RuntimeException("Unexpected number of lock files found for "
                    + handler + ": " + after);
        }
    }


    /**
     * Setup all the files and directories needed for the tests
     *
     * @return writable directory created that needs to be deleted when done
     * @throws RuntimeException
     */
    private static File setup() throws RuntimeException {
        String tmpDir = System.getProperty("java.io.tmpdir"); 
        if (tmpDir == null) {
            tmpDir = System.getProperty("user.home");
        }
        File tmpOrHomeDir = new File(tmpDir);
        File writableDir = new File(tmpOrHomeDir, WRITABLE_DIR);
        if (!createFile(writableDir, true)) {
            throw new RuntimeException("Test setup failed: unable to create"
                    + " writable working directory "
                    + writableDir.getAbsolutePath() );
        }

        final String uniqueFileName = UUID.randomUUID().toString()+".lck";
        try (FileChannel fc = FileChannel.open(Paths.get(writableDir.getAbsolutePath(),
                    uniqueFileName), CREATE_NEW, APPEND, DELETE_ON_CLOSE)) {
            try {
                fc.tryLock();
            } catch (IOException x) {
                supportsLocking = false;
            }
        } catch (IOException t) {
            System.err.println("Failed to create new file " + uniqueFileName +
                    " in " + writableDir.getAbsolutePath());
            throw new RuntimeException("Test setup failed: unable to run test", t);
        }
        return writableDir;
    }

    /**
     * @param newFile
     * @return true if file already exists or creation succeeded
     */
    private static boolean createFile(File newFile, boolean makeDirectory) {
        if (newFile.exists()) {
            return true;
        }
        if (makeDirectory) {
            return newFile.mkdir();
        } else {
            try {
                return newFile.createNewFile();
            } catch (IOException ioex) {
                ioex.printStackTrace();
                return false;
            }
        }
    }

    /*
     * Recursively delete all files starting at specified file
     */
    private static void delete(File f) {
        if (f != null && f.isDirectory()) {
            for (File c : f.listFiles())
                delete(c);
        }
        if (!f.delete())
            System.err.println(
                    "WARNING: unable to delete/cleanup writable test directory: "
                    + f );
        }
}
