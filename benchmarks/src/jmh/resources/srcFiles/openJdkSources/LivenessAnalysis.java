/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.utilities;

import java.io.*;
import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.gc.shared.*;
import sun.jvm.hotspot.memory.*;
import sun.jvm.hotspot.oops.*;
import sun.jvm.hotspot.runtime.*;

/** Finds all paths from roots to the specified set of objects. NOTE:
    currently only a subset of the roots known to the VM is exposed to
    the SA: objects on the stack, static fields in classes, and JNI
    handles. These should be most of the user-level roots keeping
    objects alive. */

public class LivenessAnalysis {
  private static final boolean DEBUG = false;

  private LivenessAnalysis() {}

  public static LivenessPathList computeAllLivenessPaths(Oop target) {
    LivenessPathList list = computeAllLivenessPaths(target, true);
    if ((list == null) || (list.size() == 0)) {
      return null;
    }
    return list;
  }


  private static LivenessPathList computeAllLivenessPaths(Oop target, boolean trimPathsThroughPopularObjects) {
    ReversePtrs rev = VM.getVM().getRevPtrs();
    if (rev == null) {
      throw new RuntimeException("LivenessAnalysis requires ReversePtrs to have been computed");
    }

    if (rev.get(target) == null) {
      return null;
    }

    Set<Oop> visitedOops = new HashSet<>();

    Map<LivenessPathElement, LivenessPathElement> visitedRoots =
      new IdentityHashMap<>();

    visitedOops.add(target);

    LivenessPathList list = new LivenessPathList();
    {
      LivenessPath path = new LivenessPath();
      path.push(new LivenessPathElement(target, null));
      list.add(path);
    }

    while (true) {
      LivenessPath path = null;

      for (int i = list.size() - 1; i >= 0; i--) {
        LivenessPath tmp = list.get(i);
        if (!tmp.isComplete()) {
          path = tmp;
          break;
        }
      }

      if (path == null) {
        return list;
      }


      list.remove(path);

      try {
        ArrayList/*<LivenessPathElement>*/ nextPtrs =
          rev.get(path.peek().getObj());

        if (nextPtrs != null) {
          for (Iterator iter = nextPtrs.iterator(); iter.hasNext(); ) {
            LivenessPathElement nextElement = (LivenessPathElement) iter.next();
            if ((nextElement.isRoot() && (visitedRoots.get(nextElement) == null)) ||
                (!nextElement.isRoot() && !visitedOops.contains(nextElement.getObj()))) {
              if (nextElement.isRoot()) {
                visitedRoots.put(nextElement, nextElement);
              } else {
                visitedOops.add(nextElement.getObj());
              }

              LivenessPath nextPath = path.copy();
              nextPath.push(nextElement);

              list.add(path);
              list.add(nextPath);

              if (trimPathsThroughPopularObjects && nextElement.isRoot()) {
                for (int i = 1; i < nextPath.size() - 1; i++) {
                  LivenessPathElement el = nextPath.get(i);
                  int j = 0;
                  while (j < list.size()) {
                    LivenessPath curPath = list.get(j);
                    if (curPath.peek() == el) {
                      list.remove(curPath);
                    } else {
                      j++;
                    }
                  }
                }
              }

              break;
            }
          }
        }
      } catch (Exception e) {
        System.err.println("LivenessAnalysis: WARNING: " + e +
                           " during traversal");
      }
    }
  }
}
