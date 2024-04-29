/*
 * Copyright (C) 2005 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.base;

import static com.google.common.base.StandardSystemProperty.JAVA_CLASS_PATH;
import static com.google.common.base.StandardSystemProperty.JAVA_SPECIFICATION_VERSION;
import static com.google.common.base.StandardSystemProperty.PATH_SEPARATOR;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.GcFinalization;
import java.io.Closeable;
import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import junit.framework.TestCase;

/**
 * Tests that the {@code ClassLoader} of {@link FinalizableReferenceQueue} can be unloaded. These
 * tests are separate from {@link FinalizableReferenceQueueTest} so that they can be excluded from
 * coverage runs, as the coverage system interferes with them.
 *
 * @author Eamonn McManus
 */


public class FinalizableReferenceQueueClassLoaderUnloadingTest extends TestCase {

  /*
   * The following tests check that the use of FinalizableReferenceQueue does not prevent the
   * ClassLoader that loaded that class from later being garbage-collected. If anything continues
   * to reference the FinalizableReferenceQueue class then its ClassLoader cannot be
   * garbage-collected, even if there are no more instances of FinalizableReferenceQueue itself.
   * The code in FinalizableReferenceQueue goes to considerable trouble to ensure that there are
   * no such references and the tests here check that that trouble has not been in vain.
   *
   * When we reference FinalizableReferenceQueue in this test, we are referencing a class that is
   * loaded by this test and that will obviously remain loaded for as long as the test is running.
   * So in order to check ClassLoader garbage collection we need to create a new ClassLoader and
   * make it load its own version of FinalizableReferenceQueue. Then we need to interact with that
   * parallel version through reflection in order to exercise the parallel
   * FinalizableReferenceQueue, and then check that the parallel ClassLoader can be
   * garbage-collected after that.
   */

  public static class MyFinalizableWeakReference extends FinalizableWeakReference<Object> {
    public MyFinalizableWeakReference(Object x, FinalizableReferenceQueue queue) {
      super(x, queue);
    }

    @Override
    public void finalizeReferent() {}
  }

  private static class PermissivePolicy extends Policy {
    @Override
    public boolean implies(ProtectionDomain pd, Permission perm) {
      return true;
    }
  }

  private WeakReference<ClassLoader> useFrqInSeparateLoader() throws Exception {
    final ClassLoader myLoader = getClass().getClassLoader();
    URLClassLoader sepLoader = new URLClassLoader(getClassPathUrls(), myLoader.getParent());

    Class<?> frqC = FinalizableReferenceQueue.class;
    Class<?> sepFrqC = sepLoader.loadClass(frqC.getName());
    assertNotSame(frqC, sepFrqC);

    Class<?> sepFrqSystemLoaderC =
        sepLoader.loadClass(FinalizableReferenceQueue.SystemLoader.class.getName());
    Field disabled = sepFrqSystemLoaderC.getDeclaredField("disabled");
    disabled.setAccessible(true);
    disabled.set(null, true);

    AtomicReference<Object> sepFrqA =
        new AtomicReference<Object>(sepFrqC.getDeclaredConstructor().newInstance());
    Class<?> sepFwrC = sepLoader.loadClass(MyFinalizableWeakReference.class.getName());
    Constructor<?> sepFwrCons = sepFwrC.getConstructor(Object.class, sepFrqC);
    Class<?> sepStopwatchC = sepLoader.loadClass(Stopwatch.class.getName());
    assertSame(sepLoader, sepStopwatchC.getClassLoader());
    AtomicReference<Object> sepStopwatchA =
        new AtomicReference<Object>(sepStopwatchC.getMethod("createUnstarted").invoke(null));
    AtomicReference<WeakReference<?>> sepStopwatchRef =
        new AtomicReference<WeakReference<?>>(
            (WeakReference<?>) sepFwrCons.newInstance(sepStopwatchA.get(), sepFrqA.get()));
    assertNotNull(sepStopwatchA.get());
    sepStopwatchA.set(null);
    GcFinalization.awaitClear(sepStopwatchRef.get());
    return new WeakReference<ClassLoader>(sepLoader);
  }

  private void doTestUnloadable() throws Exception {
    WeakReference<ClassLoader> loaderRef = useFrqInSeparateLoader();
    GcFinalization.awaitClear(loaderRef);
  }

  /**
   * Tests that the use of a {@link FinalizableReferenceQueue} does not subsequently prevent the
   * loader of that class from being garbage-collected.
   */
  public void testUnloadableWithoutSecurityManager() throws Exception {
    if (isJdk9OrHigher()) {
      return;
    }
    SecurityManager oldSecurityManager = System.getSecurityManager();
    try {
      System.setSecurityManager(null);
      doTestUnloadable();
    } finally {
      System.setSecurityManager(oldSecurityManager);
    }
  }

  /**
   * Tests that the use of a {@link FinalizableReferenceQueue} does not subsequently prevent the
   * loader of that class from being garbage-collected even if there is a {@link SecurityManager}.
   * The {@link SecurityManager} environment makes such leaks more likely because when you create a
   * {@link URLClassLoader} with a {@link SecurityManager}, the creating code's {@link
   * java.security.AccessControlContext} is captured, and that references the creating code's {@link
   * ClassLoader}.
   */
  public void testUnloadableWithSecurityManager() throws Exception {
    if (isJdk9OrHigher()) {
      return;
    }
    Policy oldPolicy = Policy.getPolicy();
    SecurityManager oldSecurityManager = System.getSecurityManager();
    try {
      Policy.setPolicy(new PermissivePolicy());
      System.setSecurityManager(new SecurityManager());
      doTestUnloadable();
    } finally {
      System.setSecurityManager(oldSecurityManager);
      Policy.setPolicy(oldPolicy);
    }
  }

  public static class FrqUser implements Callable<WeakReference<Object>> {
    public static FinalizableReferenceQueue frq = new FinalizableReferenceQueue();
    public static final Semaphore finalized = new Semaphore(0);

    @Override
    public WeakReference<Object> call() {
      WeakReference<Object> wr =
          new FinalizableWeakReference<Object>(new Integer(23), frq) {
            @Override
            public void finalizeReferent() {
              finalized.release();
            }
          };
      return wr;
    }
  }

  public void testUnloadableInStaticFieldIfClosed() throws Exception {
    if (isJdk9OrHigher()) {
      return;
    }
    Policy oldPolicy = Policy.getPolicy();
    SecurityManager oldSecurityManager = System.getSecurityManager();
    try {
      Policy.setPolicy(new PermissivePolicy());
      System.setSecurityManager(new SecurityManager());
      WeakReference<ClassLoader> loaderRef = doTestUnloadableInStaticFieldIfClosed();
      GcFinalization.awaitClear(loaderRef);
    } finally {
      System.setSecurityManager(oldSecurityManager);
      Policy.setPolicy(oldPolicy);
    }
  }

  private WeakReference<ClassLoader> doTestUnloadableInStaticFieldIfClosed() throws Exception {
    final ClassLoader myLoader = getClass().getClassLoader();
    URLClassLoader sepLoader = new URLClassLoader(getClassPathUrls(), myLoader.getParent());

    Class<?> frqC = FinalizableReferenceQueue.class;
    Class<?> sepFrqC = sepLoader.loadClass(frqC.getName());
    assertNotSame(frqC, sepFrqC);

    Class<?> sepFrqSystemLoaderC =
        sepLoader.loadClass(FinalizableReferenceQueue.SystemLoader.class.getName());
    Field disabled = sepFrqSystemLoaderC.getDeclaredField("disabled");
    disabled.setAccessible(true);
    disabled.set(null, true);

    Class<?> frqUserC = FrqUser.class;
    Class<?> sepFrqUserC = sepLoader.loadClass(frqUserC.getName());
    assertNotSame(frqUserC, sepFrqUserC);
    assertSame(sepLoader, sepFrqUserC.getClassLoader());

    Callable<?> sepFrqUser = (Callable<?>) sepFrqUserC.getDeclaredConstructor().newInstance();
    WeakReference<?> finalizableWeakReference = (WeakReference<?>) sepFrqUser.call();

    GcFinalization.awaitClear(finalizableWeakReference);

    Field sepFrqUserFinalizedF = sepFrqUserC.getField("finalized");
    Semaphore finalizeCount = (Semaphore) sepFrqUserFinalizedF.get(null);
    boolean finalized = finalizeCount.tryAcquire(5, TimeUnit.SECONDS);
    assertTrue(finalized);

    Field sepFrqUserFrqF = sepFrqUserC.getField("frq");
    Closeable frq = (Closeable) sepFrqUserFrqF.get(null);
    frq.close();

    return new WeakReference<ClassLoader>(sepLoader);
  }

  private URL[] getClassPathUrls() {
    ClassLoader classLoader = getClass().getClassLoader();
    return classLoader instanceof URLClassLoader
        ? ((URLClassLoader) classLoader).getURLs()
        : parseJavaClassPath().toArray(new URL[0]);
  }

  /**
   * Returns the URLs in the class path specified by the {@code java.class.path} {@linkplain
   * System#getProperty system property}.
   */
  private static ImmutableList<URL> parseJavaClassPath() {
    ImmutableList.Builder<URL> urls = ImmutableList.builder();
    for (String entry : Splitter.on(PATH_SEPARATOR.value()).split(JAVA_CLASS_PATH.value())) {
      try {
        try {
          urls.add(new File(entry).toURI().toURL());
        } catch (SecurityException e) { 
          urls.add(new URL("file", null, new File(entry).getAbsolutePath()));
        }
      } catch (MalformedURLException e) {
        AssertionError error = new AssertionError("malformed class path entry: " + entry);
        error.initCause(e);
        throw error;
      }
    }
    return urls.build();
  }

  /**
   * These tests fail in JDK 9 and JDK 10 for an unknown reason. It might be the test; it might be
   * the underlying functionality. Fixing this is not a high priority; if you need it to be fixed,
   * please comment on <a href="https:
   */
  private static boolean isJdk9OrHigher() {
    return JAVA_SPECIFICATION_VERSION.value().startsWith("9")
        || JAVA_SPECIFICATION_VERSION.value().startsWith("10");
  }
}
