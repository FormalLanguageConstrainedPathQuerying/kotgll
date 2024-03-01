/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.sysdict.share;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nsk.share.ClassUnloader;
import nsk.share.TestFailure;
import nsk.share.gc.ThreadedGCTest;
import nsk.share.gc.gp.GarbageUtils;
import nsk.share.test.ExecutionController;
import nsk.share.test.LocalRandom;

/**
 * This is the base class for btree & chain tests. It is a standard GCThreaded Test.
 */
public abstract class SysDictTest extends ThreadedGCTest {

    static String PACKAGE_PREFIX = "nsk.sysdict.share.";
    private boolean isHeapStressed = false;
    private boolean useSingleLoader = true;
    boolean useFats = false;
    URL[] jars;

    protected void parseArgs(String args[]) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-stressHeap")) {
                isHeapStressed = true;
            }
            if (args[i].equals("-useFatClass")) {
                useFats = true;
            }
            if (args[i].equals("-useSingleLoader")) {
                this.useSingleLoader = false;
            }
            if (args[i].equals("-jarpath")) {
                String[] files = args[i + 1].split(File.pathSeparator);
                jars = new URL[files.length];
                for (int j = 0; j < files.length; j++) {
                    try {
                        jars[j] = new File(files[j]).toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new TestFailure(e);
                    }
                }
            }
        }
    }

    protected ClassLoader createJarLoader() {
        return new URLClassLoader(jars);
    }

    abstract ClassLoader[] createLoaders();

    abstract String[] getClassNames();

    ClassLoader[] createClassLoadersInternal() {
        if (!useSingleLoader) {
            return createLoaders();
        } else {
            ClassLoader[] single = new ClassLoader[1];
            single[0] = createJarLoader();
            return single;
        }
    }
    volatile ClassLoader[] currentClassLoaders;

    class Worker implements Runnable {

        private ClassLoader loader;
        private String[] names;
        private ExecutionController stresser;
        int index;
        public String tmp;

        public Worker(int number, String[] classnames) {

            this.index = number;
            this.names = new String[classnames.length];
            List<String> listNames = new ArrayList<String>(classnames.length);
            listNames.addAll(Arrays.asList(classnames));
            for (int i = 0; i < classnames.length; i++) {
                int idx1 = LocalRandom.nextInt(listNames.size());
                this.names[i] = listNames.remove(idx1);
            }
        }

        @Override
        public void run() {
            if (stresser == null) {
                stresser = getExecutionController();
            }

            if (index == 0) {
                try {
                    currentClassLoaders = createClassLoadersInternal();
                } catch (OutOfMemoryError oome) {
                    Thread.yield();
                    return;
                }
            }
            for (int i = 0; i < names.length; i++) {
                try {
                    String name = names[i];
                    if (!stresser.continueExecution()) {
                        return;
                    }
                    loader = currentClassLoaders[index];
                    Class clz = Class.forName(name, true, loader);
                    tmp = clz.getName();
                } catch (OutOfMemoryError | ClassNotFoundException | NoClassDefFoundError e) {
                } catch (StackOverflowError soe) {
                }
            }
            if (isHeapStressed) {
                GarbageUtils.eatMemory(stresser, 50, 1024, 0);
            }
        }
    }


    @Override
    protected Runnable createRunnable(int i) {
        currentClassLoaders = createClassLoadersInternal();
        return new Worker(i % currentClassLoaders.length, getClassNames());
    }
}
