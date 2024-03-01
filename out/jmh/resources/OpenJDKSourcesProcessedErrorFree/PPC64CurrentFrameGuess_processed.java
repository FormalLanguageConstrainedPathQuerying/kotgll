/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot.runtime.ppc64;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.ppc64.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.runtime.ppc64.*;

/** <P> Should be able to be used on all ppc64 platforms we support
    (Linux/ppc64) to implement JavaThread's "currentFrameGuess()"
    functionality. Input is a PPC64ThreadContext; output is SP, FP,
    and PC for an PPC64Frame. Instantiation of the PPC64Frame is left
    to the caller, since we may need to subclass PPC64Frame to support
    signal handler frames on Unix platforms. </P>
 */

public class PPC64CurrentFrameGuess {
  private PPC64ThreadContext context;
  private JavaThread       thread;
  private Address          spFound;
  private Address          fpFound;
  private Address          pcFound;

  private static final boolean DEBUG;
  static {
    DEBUG = System.getProperty("sun.jvm.hotspot.runtime.ppc64.PPC64Frame.DEBUG") != null;
  }

  public PPC64CurrentFrameGuess(PPC64ThreadContext context,
                              JavaThread thread) {
    this.context = context;
    this.thread  = thread;
  }

  /** Returns false if not able to find a frame within a reasonable range. */
  public boolean run(long regionInBytesToSearch) {
    Address sp = context.getRegisterAsAddress(PPC64ThreadContext.SP);
    Address pc = context.getRegisterAsAddress(PPC64ThreadContext.PC);
    if (sp == null) {
      if (thread.getLastJavaSP() != null) {
        Address javaSP = thread.getLastJavaSP();
        Address javaFP = javaSP.getAddressAt(0);
        setValues(javaSP, javaFP, null);
        return true;
      }
      return false;
    }
    /* There is no frame pointer per se for the ppc64 architecture.  To mirror
     * the behavior of the VM frame manager, we set fp to be the caller's (i.e., "sender's")
     * stack pointer, which is the back chain value contained in our sp.
     */
    Address fp = sp.getAddressAt(0);
    setValues(null, null, null); 

    VM vm = VM.getVM();
    if (vm.isJavaPCDbg(pc)) {
      if (vm.isClientCompiler()) {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: choosing compiler frame: sp = " +
                             sp + ", fp = " + fp + ", pc = " + pc);
        }
        setValues(sp, fp, pc);
        return true;
      } else {
        if (vm.getInterpreter().contains(pc)) {
          if (DEBUG) {
            System.out.println("CurrentFrameGuess: choosing interpreter frame: sp = " +
                               sp + ", fp = " + fp + ", pc = " + pc);
          }
          setValues(sp, fp, pc);
          return true;
        }

        for (long offset = 0;
             offset < regionInBytesToSearch;
             offset += vm.getAddressSize()) {
          try {
            Address curSP = sp.addOffsetTo(offset);
            fp = curSP.getAddressAt(0);
            Frame frame = new PPC64Frame(curSP, fp, pc);
            RegisterMap map = thread.newRegisterMap(false);
            while (frame != null) {
              if (frame.isEntryFrame() && frame.entryFrameIsFirst()) {
                if (DEBUG) {
                  System.out.println("CurrentFrameGuess: Choosing sp = " + curSP + ", pc = " + pc);
                }
                setValues(curSP, fp, pc);
                return true;
              }
              frame = frame.sender(map);
            }
          } catch (Exception e) {
            if (DEBUG) {
              System.out.println("CurrentFrameGuess: Exception " + e + " at offset " + offset);
            }
          }
        }

        return false;

      }
    } else {

      if (thread.getLastJavaSP() == null) {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: last java sp is null");
        }
        return false; 
      }

      Address javaSP = thread.getLastJavaSP();
      Address javaFP = javaSP.getAddressAt(0);
      Address javaPC = thread.getLastJavaPC();
      if (DEBUG) {
        System.out.println("CurrentFrameGuess: choosing last Java frame: sp = " +
                           javaSP + ", fp = " + javaFP + ", pc = " + javaPC);
      }
      setValues(javaSP, javaFP, javaPC);
      return true;
    }
  }

  public Address getSP() { return spFound; }
  public Address getFP() { return fpFound; }
  /** May be null if getting values from thread-local storage; take
      care to call the correct PPC64Frame constructor to recover this if
      necessary */
  public Address getPC() { return pcFound; }

  private void setValues(Address sp, Address fp, Address pc) {
    spFound = sp;
    fpFound = fp;
    pcFound = pc;
  }
}
