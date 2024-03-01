/*
 * Copyright (c) 2003, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.runtime.amd64;

import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.amd64.*;
import sun.jvm.hotspot.code.*;
import sun.jvm.hotspot.interpreter.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;
import sun.jvm.hotspot.runtime.x86.*;
import sun.jvm.hotspot.types.*;
import sun.jvm.hotspot.utilities.*;

/** <P> Should be able to be used on all amd64 platforms we support
    (Linux/amd64) to implement JavaThread's
    "currentFrameGuess()" functionality. Input is an AMD64ThreadContext;
    output is SP, FP, and PC for an AMD64Frame. Instantiation of the
    AMD64Frame is left to the caller, since we may need to subclass
    AMD64Frame to support signal handler frames on Unix platforms. </P>

    <P> Algorithm is to walk up the stack within a given range (say,
    512K at most) looking for a plausible PC and SP for a Java frame,
    also considering those coming in from the context. If we find a PC
    that belongs to the VM (i.e., in generated code like the
    interpreter or CodeCache) then we try to find an associated EBP.
    We repeat this until we either find a complete frame or run out of
    stack to look at. </P> */

public class AMD64CurrentFrameGuess {
  private AMD64ThreadContext context;
  private JavaThread       thread;
  private Address          spFound;
  private Address          fpFound;
  private Address          pcFound;

  private static final boolean DEBUG = System.getProperty("sun.jvm.hotspot.runtime.amd64.AMD64Frame.DEBUG")
                                       != null;

  public AMD64CurrentFrameGuess(AMD64ThreadContext context,
                              JavaThread thread) {
    this.context = context;
    this.thread  = thread;
  }

  private boolean validateInterpreterFrame(Address sp, Address fp, Address pc) {
    VM vm = VM.getVM();
    X86Frame f = new X86Frame(sp, fp, pc);

    Method method = null;
    try {
      method = f.getInterpreterFrameMethod();
    } catch (WrongTypeException | AddressException | NullPointerException e) {
      if (DEBUG) {
        System.out.println("CurrentFrameGuess: frame->method is invalid");
      }
    }

    if (method != null && f.getInterpreterFrameBCP() != null) {
      if (method.getConstMethod().isAddressInMethod(f.getInterpreterFrameBCP())) {
        setValues(sp, fp, pc);
        return true;
      } else {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: frame->bcp is invalid");
        }
      }
    }


    Address returnAddress = context.getRegisterAsAddress(AMD64ThreadContext.RAX);
    CodeCache c = VM.getVM().getCodeCache();
    if (returnAddress == null || !c.contains(returnAddress)) {
      returnAddress = sp.getAddressAt(0);  
      if (returnAddress == null || !c.contains(returnAddress)) {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: Cannot find valid returnAddress");
        }
        setValues(sp, fp, pc);
        return false; 
      } else {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: returnAddress found on stack: " + returnAddress);
        }
      }
    } else {
      if (DEBUG) {
        System.out.println("CurrentFrameGuess: returnAddress found in RAX: " + returnAddress);
      }
    }

    Address returnAddress2 = null;
    try {
      returnAddress2 = f.getSenderPC();
    } catch (AddressException e) {
      if (DEBUG) {
        System.out.println("CurrentFrameGuess: senderPC is invalid");
      }
    }
    if (DEBUG) {
      System.out.println("CurrentFrameGuess: returnAddress2: " + returnAddress2);
    }

    if (returnAddress.equals(returnAddress2)) {
      if (DEBUG) {
        System.out.println("CurrentFrameGuess: frame pushed but not initialized.");
      }
      sp = f.getSenderSP();
      fp = f.getLink();
      setValues(sp, fp, returnAddress);
      if (vm.getInterpreter().contains(returnAddress)) {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: Interpreted: using previous frame.");
        }
        return true;
      } else {
        if (DEBUG) {
          System.out.println("CurrentFrameGuess: Not Interpreted: using previous frame.");
        }
        return false;
      }
    } else {
      setValues(sp, fp, returnAddress);
      if (DEBUG) {
        System.out.println("CurrentFrameGuess: Frame not yet pushed. Previous frame not interpreted.");
      }
      return false;
    }
  }

  /** Returns false if not able to find a frame within a reasonable range. */
  public boolean run(long regionInBytesToSearch) {
    Address sp  = context.getRegisterAsAddress(AMD64ThreadContext.RSP);
    Address pc  = context.getRegisterAsAddress(AMD64ThreadContext.RIP);
    Address fp  = context.getRegisterAsAddress(AMD64ThreadContext.RBP);
    if (sp == null) {
      return checkLastJavaSP();
    }
    Address end = sp.addOffsetTo(regionInBytesToSearch);
    VM vm       = VM.getVM();

    setValues(null, null, null); 

    if (!vm.isJavaPCDbg(pc)) {
      return checkLastJavaSP();
    } else {
      if (vm.isClientCompiler()) {

        setValues(sp, fp, pc);
        return true;
      } else {
        if (vm.getInterpreter().contains(pc)) {
          if (validateInterpreterFrame(sp, fp, pc)) {
            if (DEBUG) {
              System.out.println("CurrentFrameGuess: choosing interpreter frame: sp = " +
                                 spFound + ", fp = " + fpFound + ", pc = " + pcFound);
            }
            return true; 
          } else {
            sp = spFound;
            fp = fpFound;
            pc = pcFound;
            setValues(null, null, null);
            if (pcFound == null) {
              return false;
            }
            if (!vm.isJavaPCDbg(pc)) {
              return checkLastJavaSP();
            }
          }
        }


        if (DEBUG) {
          System.out.println("CurrentFrameGuess: sp = " + sp + ", pc = " + pc);
        }
        for (long offset = 0;
             offset < regionInBytesToSearch;
             offset += vm.getAddressSize()) {
          try {
            Address curSP = sp.addOffsetTo(offset);
            Frame frame = new X86Frame(curSP, null, pc);
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
    }
  }

  private boolean checkLastJavaSP() {

    if (DEBUG) {
      System.out.println("CurrentFrameGuess: choosing last Java frame: sp = " +
                         thread.getLastJavaSP() + ", fp = " + thread.getLastJavaFP());
    }
    if (thread.getLastJavaSP() == null) {
      return false; 
    }
    setValues(thread.getLastJavaSP(), thread.getLastJavaFP(), null);
    return true;
  }

  public Address getSP() { return spFound; }
  public Address getFP() { return fpFound; }
  /** May be null if getting values from thread-local storage; take
      care to call the correct AMD64Frame constructor to recover this if
      necessary */
  public Address getPC() { return pcFound; }

  private void setValues(Address sp, Address fp, Address pc) {
    spFound = sp;
    fpFound = fp;
    pcFound = pc;
  }
}
