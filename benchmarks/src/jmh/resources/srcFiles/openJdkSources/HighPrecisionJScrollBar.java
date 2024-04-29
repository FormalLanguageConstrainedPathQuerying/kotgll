/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.ui;

import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.math.*;
import java.util.*;

/** A JScrollBar which uses BigIntegers as the representation for the
    minimum, maximum, unit increment, etc. Interaction with the
    buttons and track is accurate to unit and block increments;
    however, if the scale of the scrollbar (defined by
    getMaximumHP().subtract(getMinimumHP())) is very large, each
    interaction with the thumb will necessarily cause extremely large
    motion of the value. */

public class HighPrecisionJScrollBar extends JScrollBar {
  private BigInteger valueHP;
  private BigInteger visibleHP;
  private BigInteger minimumHP;
  private BigInteger maximumHP;
  private BigInteger unitIncrementHP;
  private BigInteger blockIncrementHP;
  private BigDecimal scaleFactor;
  private BigInteger rangeHP;
  private static final int BIG_RANGE = 10000;
  private boolean    down;
  private java.util.List<ChangeListener> changeListeners = new ArrayList<>();
  private static final int SCALE = 20;


  private static final int UNIT_INCREMENT  = 1;
  private static final int BLOCK_INCREMENT = 2;
  private static final int MINIMUM = 0;
  private static final int MAXIMUM = 65536;
  private boolean updating = false;
  private int lastValueSeen = -1;

  public HighPrecisionJScrollBar() {
    super();
    initialize();
    installListener();
  }

  public HighPrecisionJScrollBar(int orientation) {
    super(orientation);
    initialize();
    installListener();
  }

  /** value, minimum and maximum should be positive */
  public HighPrecisionJScrollBar(int orientation, BigInteger value, BigInteger minimum, BigInteger maximum) {
    super(orientation);
    initialize(value, minimum, maximum);
    installListener();
  }

  public BigInteger getValueHP() {
    return valueHP;
  }


  /** NOTE: the real value will always be set to be (value mod
      unitIncrement) == 0, subtracting off the mod of the passed value
      if necessary. */

  public void setValueHP(BigInteger value) {
    if (value.compareTo(getMaximumHP()) > 0) {
      value = getMaximumHP();
    } else if (value.compareTo(getMinimumHP()) < 0) {
      value = getMinimumHP();
    }
    valueHP = value.subtract(value.mod(unitIncrementHP));
    int lpValue = toUnderlyingRange(this.valueHP);
    if (getValueHP().add(getVisibleAmountHP()).compareTo(getMaximumHP()) >= 0 ) {
      lpValue = BIG_RANGE - getVisibleAmount();
    }
    lastValueSeen = lpValue;
    setValue(lpValue);
    fireStateChanged();
  }
  public BigInteger getMinimumHP() {
    return minimumHP;
  }

  public void setMinimumHP(BigInteger minimum) {
    setRange(minimum, maximumHP);
    updateScrollBarValues();
  }

  public BigInteger getMaximumHP() {
    return maximumHP;
  }

  public void setMaximumHP(BigInteger maximum) {
    setRange(minimumHP, maximum);
    updateScrollBarValues();
  }

  public BigInteger getVisibleAmountHP() {
    return visibleHP;
  }

  public void setVisibleAmountHP(BigInteger visibleAmount) {
    this.visibleHP = visibleAmount;
    int lpVisAmt;
    if (visibleAmount.compareTo(rangeHP) < 0) {
      lpVisAmt = scaleToUnderlying(visibleAmount);
      if (lpVisAmt == 0) {
        lpVisAmt = 1;
      }
      setVisible(true);
    } else {
      lpVisAmt = BIG_RANGE;
      setVisible(false);
    }
    setVisibleAmount(lpVisAmt);
  }

  public BigInteger getBlockIncrementHP() {
    return blockIncrementHP;
  }

  public void setBlockIncrementHP(BigInteger blockIncrement) {
    this.blockIncrementHP = blockIncrement;
  }

  public BigInteger getUnitIncrementHP() {
    return unitIncrementHP;
  }

  public void setUnitIncrementHP(BigInteger unitIncrement) {
    this.unitIncrementHP = unitIncrement;
  }


  public void addChangeListener(ChangeListener l) {
    changeListeners.add(l);
  }

  public void removeChangeListener(ChangeListener l) {
    changeListeners.remove(l);
  }


  public void scrollUpOrLeft() {
    if (updating) return;
    beginUpdate();
    setValueHP(getValueHP().subtract(getUnitIncrementHP()));
    endUpdate();
  }

  public void scrollDownOrRight() {
    if (updating) return;
    beginUpdate();
    setValueHP(getValueHP().add(getUnitIncrementHP()));
    endUpdate();
  }

  public void pageUpOrLeft() {
    if (updating) return;
    beginUpdate();
    setValueHP(getValueHP().subtract(getBlockIncrementHP()));
    endUpdate();
  }

  public void pageDownOrRight() {
    if (updating) return;
    beginUpdate();
    setValueHP(getValueHP().add(getBlockIncrementHP()));
    endUpdate();
  }


  private void beginUpdate() {
    updating = true;
  }

  private void endUpdate() {
    updating = false;
  }

  private void initialize(BigInteger value, BigInteger minimum, BigInteger maximum) {
    setMinimum(0);
    setMaximum(BIG_RANGE - 1);
    setValue(0);
    setVisibleAmount(1);
    setUnitIncrement(UNIT_INCREMENT);
    setBlockIncrement(BLOCK_INCREMENT);

    setUnitIncrementHP(new BigInteger(Integer.toString(getUnitIncrement())));
    setBlockIncrementHP(new BigInteger(Integer.toString(getBlockIncrement())));

    setRange(minimum, maximum);

    setVisibleAmountHP(new BigInteger(Integer.toString(getVisibleAmount())));
    setValueHP(value);
  }

  private void initialize() {
    BigInteger min = new BigInteger(Integer.toString(getMinimum()));
    BigInteger max = new BigInteger(Integer.toString(getMaximum()));
    initialize(min, min, max);
  }

  private void setRange(BigInteger minimum, BigInteger maximum) {
    if (minimum.compareTo(maximum) > 0 ) {
      throw new RuntimeException("Bad scrollbar range " + minimum + " > " + maximum);
    }
    minimumHP = minimum;
    maximumHP = maximum;
    rangeHP = maximum.subtract(minimum).add(BigInteger.ONE);
    BigInteger range2 = new BigInteger(Integer.toString(BIG_RANGE));
    if (rangeHP.compareTo(range2) >= 0 ) {
      down = true;
      scaleFactor = new BigDecimal(rangeHP, SCALE).divide(new BigDecimal(range2, SCALE), RoundingMode.DOWN).max(new BigDecimal(BigInteger.ONE));
    } else {
      down = false;
      scaleFactor = new BigDecimal(range2, SCALE).divide(new BigDecimal(rangeHP, SCALE), RoundingMode.DOWN).max(new BigDecimal(BigInteger.ONE));
    }
  }

  private void updateScrollBarValues() {
    setValueHP(getValueHP());
    setVisibleAmountHP(getVisibleAmountHP());
    setBlockIncrementHP(getBlockIncrementHP());
    setUnitIncrementHP(getUnitIncrementHP());
  }

  private BigDecimal getScaleFactor() {
    return scaleFactor;
  }


  private BigInteger scaleToHP(int i) {
    BigDecimal ib = new BigDecimal(Integer.toString(i));
    if (down) return ib.multiply(getScaleFactor()).toBigInteger();
    else return ib.divide(getScaleFactor(), RoundingMode.DOWN).toBigInteger();
  }

  private int scaleToUnderlying(BigInteger i) {
    BigDecimal d = new BigDecimal(i);
    if (down) return d.divide(getScaleFactor(), RoundingMode.DOWN).intValue();
    else return d.multiply(getScaleFactor()).intValue();
  }

  private BigInteger toHPRange(int i) {
    return scaleToHP(i).add(minimumHP);
  }

  private int toUnderlyingRange(BigInteger i) {
    return scaleToUnderlying(i.subtract(minimumHP));
  }

  private void installListener() {
    super.addAdjustmentListener(new AdjustmentListener() {
        public void adjustmentValueChanged(AdjustmentEvent e) {
          if (updating) {
            return;
          }
          beginUpdate();
          switch (e.getAdjustmentType()) {
          case AdjustmentEvent.TRACK:
            int val = e.getValue();
            int diff = val - lastValueSeen;
            int absDiff = Math.abs(diff);
            if (absDiff == UNIT_INCREMENT) {
              if (diff > 0) {
                setValueHP(getValueHP().add(getUnitIncrementHP()));
              } else {
                setValueHP(getValueHP().subtract(getUnitIncrementHP()));
              }
            } else if (absDiff == BLOCK_INCREMENT) {
              if (diff > 0) {
                setValueHP(getValueHP().add(getBlockIncrementHP()));
              } else {
                setValueHP(getValueHP().subtract(getBlockIncrementHP()));
              }
            } else {
              if (absDiff != 0) {
                BigInteger i = null;
                if (e.getValue() == getMinimum()) {
                  i = getMinimumHP();
                } else if (e.getValue() >= getMaximum() - 1) {
                  i = getMaximumHP();
                } else {
                  i = toHPRange(e.getValue());
                }
                setValueHP(i);
              }
            }
            break;
          default:
            break;
          }
          endUpdate();
        }
      });
  }

  private void fireStateChanged() {
    ChangeEvent e = null;
    for (Iterator iter = changeListeners.iterator(); iter.hasNext(); ) {
      ChangeListener l = (ChangeListener) iter.next();
      if (e == null) {
        e = new ChangeEvent(this);
      }
      l.stateChanged(e);
    }
  }

  public static void main(String[] args) {
    JFrame frame = new JFrame();
    frame.setSize(300, 300);
    /*
    HighPrecisionJScrollBar hpsb =
      new HighPrecisionJScrollBar(
        JScrollBar.VERTICAL,
        new BigInteger(1, new byte[] {
          (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
        new BigInteger(1, new byte[] {
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
        new BigInteger(1, new byte[] {
          (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}));
    hpsb.setUnitIncrementHP(new BigInteger(1, new byte[] {
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01}));
    hpsb.setBlockIncrementHP(new BigInteger(1, new byte[] {
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10}));
    */

    HighPrecisionJScrollBar hpsb =
      new HighPrecisionJScrollBar(
        JScrollBar.VERTICAL,
        new BigInteger(1, new byte[] {
          (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00,
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
        new BigInteger(1, new byte[] {
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
          (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
        new BigInteger(1, new byte[] {
          (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
          (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}));
    hpsb.setUnitIncrementHP(new BigInteger(1, new byte[] {
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01}));
    hpsb.setBlockIncrementHP(new BigInteger(1, new byte[] {
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
      (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10}));
    hpsb.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          HighPrecisionJScrollBar h = (HighPrecisionJScrollBar) e.getSource();
          System.out.println("New value = 0x" + h.getValueHP().toString(16));
        }
      });
    frame.getContentPane().add(hpsb);
    frame.setVisible(true);
  }

}
