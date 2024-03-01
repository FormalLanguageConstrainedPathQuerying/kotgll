/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4109894 4239646 4785453
 * @summary Verify that class modifiers bits written into class
 * file are correct, including those within InnerClasses attributes.
 * @author John Rose (jrose). Entered as a regression test by Bill Maddox (maddox).
 *
 * @compile/ref=ClassModifiers.out --debug=dumpmodifiers=ci ClassModifiers.java
 *
 */

class T {


 class iC{}
 static class iSC{}
 private class iVC{}
 static private class iSVC{}

 final class iFC{}
 static final class iSFC{}
 final private class iFVC{}
 static final private class iSFVC{}

 abstract class iAC{}
 static abstract class iSAC{}
 abstract private class iAVC{}
 static abstract private class iSAVC{}

 protected class iRC{}
 static protected class iSRC{}
 public class iUC{}
 static public class iSUC{}

 final protected class iFRC{}
 static final protected class iSFRC{}
 final public class iFUC{}
 static final public class iSFUC{}

 abstract protected class iARC{}
 static abstract protected class iSARC{}
 abstract public class iAUC{}
 static abstract public class iSAUC{}

 interface iI{}
 static interface iSI{}
 abstract interface iAI{}
 static abstract interface iSAI{}

 protected interface iRI{}
 static protected interface iSRI{}
 abstract protected interface iARI{}
 static abstract protected interface iSARI{}

 private interface iVI{}
 static private interface iSVI{}
 abstract private interface iAVI{}
 static abstract private interface iSAVI{}

 public interface iUI{}
 static public interface iSUI{}
 abstract public interface iAUI{}
 static abstract public interface iSAUI{}
}

interface U {

 class jC{}
 static class jSC{}
 public class jUC{}
 static public class jSUC{}

 final class jFC{}
 static final class jSFC{}
 final public class jFUC{}
 static final public class jSFUC{}

 abstract class jAC{}
 static abstract class jSAC{}
 abstract public class jAUC{}
 static abstract public class jSAUC{}

 interface jI{}
 static interface jSI{}
 abstract interface jAI{}
 static abstract interface jSAI{}
 public interface jUI{}
 static public interface jSUI{}
 abstract public interface jAUI{}
 static abstract public interface jSAUI{}
}
