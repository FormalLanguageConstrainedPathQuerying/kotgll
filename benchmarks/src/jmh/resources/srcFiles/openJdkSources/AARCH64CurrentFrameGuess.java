/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2015, 2019, Red Hat Inc.
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

package sun.jvm.hotspot.runtime.aarch64;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.aarch64.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.runtime.aarch64.*;

/** <P> Should be able to be used on all aarch64 platforms we support
    (Linux/aarch64) to implement JavaThread's "currentFrameGuess()"
    functionality. Input is an AARCH64ThreadContext; output is SP, FP,
    and PC for an AARCH64Frame. Instantiation of the AARCH64Frame is
    left to the caller, since we may need to subclass AARCH64Frame to
    support signal handler frames on Unix platforms. </P>

    <P> Algorithm is to walk up the stack within a given range (say,
    512K at most) looking for a plausible PC and SP for a Java frame,
    also considering those coming in from the context. If we find a PC
    that belongs to the VM (i.e., in generated code like the
    interpreter or CodeCache) then we try to find an associated FP.
    We repeat this until we either find a complete frame or run out of
    stack to look at. </P> */

public class AARCH64CurrentFrameGuess {
  private AARCH64ThreadContext context;
  private JavaThread       thread;
  private Address          spFound;
  private Address          fpFound;
  private Address          pcFound;

  private static final boolean DEBUG = System.getProperty("sun.jvm.hotspot.runtime.aarch64.AARCH64Frame.DEBUG")
                                       != null;

  public AARCH64CurrentFrameGuess(AARCH64ThreadContext context,
                              JavaThread thread) {
    this.context = context;
    this.thread  = thread;
  }

  /** Returns false if not able to find a frame within a reasonable range. */
  public boolean run(long regionInBytesToSearch) {
    Address sp  = context.getRegisterAsAddress(AARCH64ThreadContext.SP);
    Address pc  = context.getRegisterAsAddress(AARCH64ThreadContext.PC);
    Address fp  = context.getRegisterAsAddress(AARCH64ThreadContext.FP);
    if (sp == null) {
      if (thread.getLastJavaSP() != null) {
        setValues(thread.getLastJavaSP(), thread.getLastJavaFP(), null);
        return true;
      }
      return false;
    }
    Address end = sp.addOffsetTo(regionInBytesToSearch);
    VM vm       = VM.getVM();

    setValues(null, null, null); 

    if (vm.isJavaPCDbg(pc)) {
      if (vm.isClientCompiler()) {

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
            Frame frame = new AARCH64Frame(curSP, null, pc);
            RegisterMap map = thread.newRegisterMap(false);
            while (frame != null) {
              if (frame.isEntryFrame() && frame.entryFrameIsFirst()) {
                if (DEBUG) {
                  System.out.println("CurrentFrameGuess: Choosing sp = " + curSP + ", pc = " + pc);
                }
                setValues(curSP, null, pc);
                return true;
              }
              Frame oldFrame = frame;
              frame = frame.sender(map);
              if (frame.getSP().lessThanOrEqual(oldFrame.getSP())) {
                if (DEBUG) {
                  System.out.println("CurrentFrameGuess: frame <= oldFrame: " + frame);
                }
                break;
              }
            }
          } catch (Exception e) {
            if (DEBUG) {
              System.out.println("CurrentFrameGuess: Exception " + e + " at offset " + offset);
            }
          }
        }

        return false;

        /*


        CodeCache cc = vm.getCodeCache();
        if (cc.contains(pc)) {
          CodeBlob cb = cc.findBlob(pc);

          Address saved_fp = null;
          int llink_offset = cb.getLinkOffset();
          if (llink_offset >= 0) {
            Address fp_addr = sp.addOffsetTo(VM.getVM().getAddressSize() * llink_offset);
            saved_fp = fp_addr.getAddressAt(0);
          }

          setValues(sp, saved_fp, pc);
          return true;
        }
        */
      }
    } else {

      if (DEBUG) {
        System.out.println("CurrentFrameGuess: choosing last Java frame: sp = " +
                           thread.getLastJavaSP() + ", fp = " + thread.getLastJavaFP());
      }
      if (thread.getLastJavaSP() == null) {
        return false; 
      }

      pc = thread.getLastJavaPC();
      fp = thread.getLastJavaFP();
      sp = thread.getLastJavaSP();

      if (fp == null) {
        CodeCache cc = vm.getCodeCache();
        if (cc.contains(pc)) {
          CodeBlob cb = cc.findBlob(pc);
          if (DEBUG) {
            System.out.println("FP is null.  Found blob frame size " + cb.getFrameSize());
          }
          long link_offset = cb.getFrameSize() - 2 * VM.getVM().getAddressSize();
          if (link_offset >= 0) {
            fp = sp.addOffsetTo(link_offset);
          }
        }
      }

      if (vm.isJavaPCDbg(pc)) {
        setValues(sp, fp, pc);
      } else {
        setValues(sp, fp, null);
      }

      return true;
    }
  }

  public Address getSP() { return spFound; }
  public Address getFP() { return fpFound; }
  /** May be null if getting values from thread-local storage; take
      care to call the correct AARCH64Frame constructor to recover this if
      necessary */
  public Address getPC() { return pcFound; }

  private void setValues(Address sp, Address fp, Address pc) {
    spFound = sp;
    fpFound = fp;
    pcFound = pc;
  }
}
