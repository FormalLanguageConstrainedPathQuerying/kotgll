/*
 * Copyright (c) 2001, 2022, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.debugger.cdbg.basic;

import java.util.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.debugger.cdbg.*;
import sun.jvm.hotspot.utilities.AddressOps;
import sun.jvm.hotspot.utilities.Assert;

public class BasicCDebugInfoDataBase implements CDebugInfoDataBase {
  private static final int INITIALIZED_STATE  = 0;
  private static final int CONSTRUCTION_STATE = 1;
  private static final int RESOLVED_STATE     = 2;
  private static final int COMPLETE_STATE     = 3;

  private int state = INITIALIZED_STATE;


  private Map<Object, Type> lazyTypeMap;

  private List<Type> types;

  private Map<String, Type> nameToTypeMap;


  private Map<Object, BlockSym> lazySymMap;

  private List<BlockSym> blocks;

  private Map<String, GlobalSym> nameToSymMap;


  private BasicLineNumberMapping lineNumbers;

  /** Supports lazy instantiation and references between types and
      symbols via insertion using arbitrary Object keys that are
      wrapped by LazyTypes. Once the database has been fully
      constructed and all types are present, one should call
      resolveTypes(), which will resolve all LazyTypes down to
      concrete types (and signal an error if some lazy types were
      unresolved). */
  public void beginConstruction() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(state == INITIALIZED_STATE, "wrong state");
    }
    state   = CONSTRUCTION_STATE;

    lazyTypeMap  = new HashMap<>();
    types        = new ArrayList<>();

    lazySymMap   = new HashMap<>();
    blocks       = new ArrayList<>();
    nameToSymMap = new HashMap<>();

    lineNumbers  = new BasicLineNumberMapping();
  }

  /** Add a type which may later in construction be referred to via a
      LazyType with this key. lazyKey may be null. */
  public void addType(Object lazyKey, Type type) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(state == CONSTRUCTION_STATE, "wrong state");
    }
    if (lazyKey != null) {
      if (lazyTypeMap.put(lazyKey, type) != null) {
        throw new RuntimeException("Type redefined for lazy key " + lazyKey);
      }
    } else {
      types.add(type);
    }
  }

  public void resolve(ResolveListener listener) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(state == CONSTRUCTION_STATE, "wrong state");
    }
    resolveLazyMap(listener);
    for (ListIterator<Type> iter = types.listIterator(); iter.hasNext(); ) {
      BasicType t = (BasicType) iter.next();
      BasicType t2 = (BasicType) t.resolveTypes(this, listener);
      if (t != t2) {
        iter.set(t2);
      }
    }
    for (Iterator iter = blocks.iterator(); iter.hasNext(); ) {
      ((BasicSym) iter.next()).resolve(this, listener);
    }
    for (Iterator iter = nameToSymMap.values().iterator(); iter.hasNext(); ) {
      ((BasicSym) iter.next()).resolve(this, listener);
    }

    blocks.sort(new Comparator<>() {
        public int compare(BlockSym b1, BlockSym b2) {
          Address a1 = b1.getAddress();
          Address a2 = b2.getAddress();
          if (AddressOps.lt(a1, a2)) { return -1; }
          if (AddressOps.gt(a1, a2)) { return 1; }
          return 0;
        }
      });

    state = RESOLVED_STATE;
  }

  public void endConstruction() {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(state == RESOLVED_STATE, "wrong state");
    }
    for (Iterator<Type> iter = lazyTypeMap.values().iterator(); iter.hasNext(); ) {
      types.add(iter.next());
    }
    nameToTypeMap = new HashMap<>();
    for (Iterator iter = types.iterator(); iter.hasNext(); ) {
      Type t = (Type) iter.next();
      if (!t.isConst() && !t.isVolatile()) {
        nameToTypeMap.put(t.getName(), t);
      }
    }
    lazyTypeMap = null;
    lazySymMap  = null;
    lineNumbers.sort();
    lineNumbers.recomputeEndPCs();

    state = COMPLETE_STATE;
  }

  public Type lookupType(String name) {
    return lookupType(name, 0);
  }

  public Type lookupType(String name, int cvAttributes) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(state == COMPLETE_STATE, "wrong state");
    }
    BasicType t = (BasicType) nameToTypeMap.get(name);
    if (t != null) {
      if (cvAttributes != 0) {
        t = (BasicType) t.getCVVariant(cvAttributes);
      }
    }
    return t;
  }

  public void iterate(TypeVisitor v) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(state == COMPLETE_STATE, "wrong state");
    }
    for (Iterator iter = types.iterator(); iter.hasNext(); ) {
      BasicType t = (BasicType) iter.next();
      t.visit(v);
    }
  }

  /** Add a BlockSym to the debug information database. The given
      BlockSym may be referred to by a LazyBlockSym wrapping the given
      Object key, which must be non-null. Any references to other
      blocks (for example, the parent scope) should be made with
      LazyBlockSyms. These references will be resolved after the
      database is built. */
  public void addBlock(Object key, BlockSym block) {
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(key != null, "key must be non-null");
    }
    lazySymMap.put(key, block);
    blocks.add(block);
  }

  public void addGlobalSym(GlobalSym sym) {
    nameToSymMap.put(sym.getName(), sym);
  }

  public BlockSym debugInfoForPC(Address pc) {
    return searchBlocks(pc, 0, blocks.size() - 1);
  }

  public GlobalSym lookupSym(String name) {
    return nameToSymMap.get(name);
  }

  public void addLineNumberInfo(BasicLineNumberInfo info) {
    lineNumbers.addLineNumberInfo(info);
  }

  public LineNumberInfo lineNumberForPC(Address pc) throws DebuggerException {
    return lineNumbers.lineNumberForPC(pc);
  }

  public void iterate(LineNumberVisitor v) {
    lineNumbers.iterate(v);
  }


  /** Intended only to be used by the BasicType implementation. */
  public Type resolveType(Type containingType, Type targetType, ResolveListener listener, String detail) {
    BasicType basicTargetType = (BasicType) targetType;
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(state == CONSTRUCTION_STATE, "wrong state");
    }
    if (basicTargetType.isLazy()) {
      BasicType resolved = (BasicType) lazyTypeMap.get(((LazyType) targetType).getKey());
      if (resolved == null) {
        listener.resolveFailed(containingType, (LazyType) targetType, detail + " because target type was not found");
        return targetType;
      }
      if (resolved.isLazy()) {
        if (resolved.isConst() || resolved.isVolatile()) {
          resolved = (BasicType) resolved.resolveTypes(this, listener);
        }
        if (resolved.isLazy()) {
          listener.resolveFailed(containingType, (LazyType) targetType,
                                 detail + " because target type (with key " +
                                 ((Integer) ((LazyType) resolved).getKey()).intValue() +
                                 (resolved.isConst() ? ", const" : ", not const") +
                                 (resolved.isVolatile() ? ", volatile" : ", not volatile") +
                                 ") was lazy");
        }
      }
      return resolved;
    }
    return targetType;
  }

  /** Intended only to be usd by the BasicSym implementation. */
  public Type resolveType(Sym containingSymbol, Type targetType, ResolveListener listener, String detail) {
    BasicType basicTargetType = (BasicType) targetType;
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(state == CONSTRUCTION_STATE, "wrong state");
    }
    if (basicTargetType.isLazy()) {
      BasicType resolved = (BasicType) lazyTypeMap.get(((LazyType) targetType).getKey());
      if (resolved == null) {
        listener.resolveFailed(containingSymbol, (LazyType) targetType, detail);
        return targetType;
      }
      if (resolved.isLazy()) {
        if (resolved.isConst() || resolved.isVolatile()) {
          resolved = (BasicType) resolved.resolveTypes(this, listener);
        }
        if (resolved.isLazy()) {
          listener.resolveFailed(containingSymbol, (LazyType) targetType, detail);
        }
      }
      return resolved;
    }
    return targetType;
  }

  /** Intended only to be usd by the BasicSym implementation. */
  public Sym resolveSym(Sym containingSymbol, Sym targetSym, ResolveListener listener, String detail) {
    if (targetSym == null) return null;
    BasicSym basicTargetSym = (BasicSym) targetSym;
    if (Assert.ASSERTS_ENABLED) {
      Assert.that(state == CONSTRUCTION_STATE, "wrong state");
    }
    if (basicTargetSym.isLazy()) {
      BasicSym resolved = (BasicSym) lazySymMap.get(((LazyBlockSym) targetSym).getKey());
      if (resolved == null) {
        listener.resolveFailed(containingSymbol, (LazyBlockSym) targetSym, detail);
        return targetSym;
      }
      if (resolved.isLazy()) {
        listener.resolveFailed(containingSymbol, (LazyBlockSym) targetSym, detail);
      }
      return resolved;
    }
    return targetSym;
  }

  private void resolveLazyMap(ResolveListener listener) {
    for (Iterator<Map.Entry<Object, Type>> iter = lazyTypeMap.entrySet().iterator(); iter.hasNext(); ) {
      Map.Entry<Object, Type> entry = iter.next();
      BasicType t = (BasicType) entry.getValue();
      BasicType t2 = (BasicType) t.resolveTypes(this, listener);
      if (t2 != t) {
        entry.setValue(t2);
      }
    }
  }

  /** Find the block whose starting address is closest to but less
      than the given address. */
  private BlockSym searchBlocks(Address addr, int lowIdx, int highIdx) {
    if (highIdx < lowIdx) return null;
    if ((lowIdx == highIdx) || (lowIdx == highIdx - 1)) {
      Address lastAddr = null;
      BlockSym ret = null;
      for (int i = highIdx; i >= 0; --i) {
        BlockSym block = blocks.get(i);
        if (AddressOps.lte(block.getAddress(), addr)) {
          if ((lastAddr == null) || (AddressOps.equal(block.getAddress(), lastAddr))) {
            lastAddr = block.getAddress();
            ret = block;
          } else {
            break;
          }
        }
      }
      return ret;
    }
    int midIdx = (lowIdx + highIdx) >> 1;
    BlockSym block = blocks.get(midIdx);
    if (AddressOps.lte(block.getAddress(), addr)) {
      return searchBlocks(addr, midIdx, highIdx);
    } else {
      return searchBlocks(addr, lowIdx, midIdx);
    }
  }
}
