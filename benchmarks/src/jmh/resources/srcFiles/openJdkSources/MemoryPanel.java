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

package sun.jvm.hotspot.ui;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import sun.jvm.hotspot.debugger.*;
import sun.jvm.hotspot.utilities.PointerFinder;
import sun.jvm.hotspot.utilities.PointerLocation;

public class MemoryPanel extends JPanel {
  private boolean is64Bit;
  private boolean isAnnotated;
  private Debugger debugger;
  private int addressSize;
  private String unmappedAddrString;
  private HighPrecisionJScrollBar scrollBar;
  private AbstractTableModel model;
  private JTable table;
  private BigInteger startVal;
  private int numVisibleRows;
  private int numUsableRows;
  private boolean haveAnchor;
  private int     rowAnchorIndex;
  private int     colAnchorIndex;
  private boolean haveLead;
  private int     rowLeadIndex;
  private int     colLeadIndex;

  abstract class ActionWrapper extends AbstractAction {
    private Action parent;
    ActionWrapper() {
    }

    void setParent(Action parent) {
      this.parent = parent;
    }

    Action getParent() {
      return parent;
    }

    public void actionPerformed(ActionEvent e) {
      if (getParent() != null) {
        getParent().actionPerformed(e);
      }
    }
  }

  public MemoryPanel(final Debugger debugger, boolean isAnnotated, boolean is64Bit) {
    super();
    this.debugger = debugger;
    this.is64Bit = is64Bit;
    this.isAnnotated = isAnnotated;
    if (is64Bit) {
      addressSize = 8;
      unmappedAddrString = "??????????????????";
    } else {
      addressSize = 4;
      unmappedAddrString = "??????????";
    }
    setLayout(new BorderLayout());
    setupScrollBar();
    add(scrollBar, BorderLayout.EAST);

    model = new AbstractTableModel() {
        public int getRowCount() {
          return numVisibleRows;
        }
        public int getColumnCount() {
          return isAnnotated ? 1 : 2;
        }
        public Object getValueAt(int row, int column) {
          if (!isAnnotated) {
            switch (column) {
            case 0:  return bigIntToHexString(startVal.add(new BigInteger(Integer.toString((row * addressSize)))));
            case 1: {
              try {
                Address addr = bigIntToAddress(startVal.add(new BigInteger(Integer.toString((row * addressSize)))));
                if (addr != null) {
                  return addressToString(addr.getAddressAt(0));
                }
                return unmappedAddrString;
              } catch (UnmappedAddressException e) {
                return unmappedAddrString;
              }
            }
            default: throw new RuntimeException("Column " + column + " out of bounds");
            }
          } else {
            switch (column) {
            case 0: {
              BigInteger bigaddr = startVal.add(new BigInteger(Integer.toString((row * addressSize))));
              Address addr = bigIntToAddress(bigaddr);

              String col1 = bigIntToHexString(bigaddr);
              String col2 = unmappedAddrString;
              String col3 = "";

              if (addr != null) {
                try {
                  col2 = addressToString(addr.getAddressAt(0));
                  PointerLocation loc = PointerFinder.find(addr.getAddressAt(0));
                  if (!loc.isUnknown()) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    PrintStream tty = new PrintStream(bos);
                    loc.printOn(tty, false, false);
                    col3 = bos.toString();
                  }
                } catch (UnmappedAddressException e) {
                }
              }

              return col1 + ": " + col2 + " " + col3;
            }
            default: throw new RuntimeException("Column " + column + " out of bounds");
            }
          }
        }
        public boolean isCellEditable(int row, int col) {
          return false;
        }
      };

    table = new JTable(model);
    table.setTableHeader(null);
    table.setShowGrid(false);
    table.setIntercellSpacing(new Dimension(0, 0));
    table.setCellSelectionEnabled(true);
    table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    table.setDragEnabled(true);
    Font font = GraphicsUtilities.getMonospacedFont();
    if (font == null) {
      throw new RuntimeException("Error looking up monospace font Courier");
    }
    table.setFont(font);

    table.setTransferHandler(new TransferHandler() {
        protected Transferable createTransferable(JComponent c) {
          JTable table = (JTable)c;
          if (haveSelection()) {
            StringBuilder buf = new StringBuilder();
            int iDir = (getRowAnchor() < getRowLead() ? 1 : -1);
            int jDir = (getColAnchor() < getColLead() ? 1 : -1);

            for (int i = getRowAnchor(); i != getRowLead() + iDir; i += iDir) {
              for (int j = getColAnchor(); j != getColLead() + jDir; j += jDir) {
                Object val = model.getValueAt(i, j);
                buf.append(val == null ? "" : val.toString());
                if (j != getColLead()) {
                  buf.append("\t");
                }
              }
              if (i != getRowLead()) {
                buf.append("\n");
              }
            }

            return new StringTransferable(buf.toString());
          }
          return null;
        }

        public int getSourceActions(JComponent c) {
          return COPY;
        }

        public boolean importData(JComponent c, Transferable t) {
          if (canImport(c, t.getTransferDataFlavors())) {
            try {
              String str = (String)t.getTransferData(DataFlavor.stringFlavor);
              handleImport(c, str);
              return true;
            } catch (UnsupportedFlavorException | IOException e) {
            }
          }

          return false;
        }

        public boolean canImport(JComponent c, DataFlavor[] flavors) {
          for (int i = 0; i < flavors.length; i++) {
            if (DataFlavor.stringFlavor.equals(flavors[i])) {
              return true;
            }
          }
          return false;
        }

        private void handleImport(JComponent c, String str) {
          try {
            int secondAddrStartIndex = str.indexOf("0x", 2);
            if (secondAddrStartIndex != -1) {
              str = str.substring(secondAddrStartIndex);
              str = str.split("\\s")[0];
            }
            makeVisible(debugger.parseAddress(str));
            clearSelection();
            table.clearSelection();
          } catch (NumberFormatException e) {
            System.err.println("Unable to parse address \"" + str + "\"");
          }
        }
      });



    ActionMap map = table.getActionMap();

    installActionWrapper(map, "selectPreviousRow", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          clearSelection();
          if (table.getSelectedRow() == 0) {
            scrollBar.scrollUpOrLeft();
            table.setRowSelectionInterval(0, 0);
          } else {
            super.actionPerformed(e);
          }
          maybeGrabSelection();
          endUpdate();
        }
      });
    installActionWrapper(map, "selectNextRow", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          clearSelection();
          int row = table.getSelectedRow();
          if (row >= numUsableRows) {
            scrollBar.scrollDownOrRight();
            table.setRowSelectionInterval(row, row);
          } else {
            super.actionPerformed(e);
          }
          maybeGrabSelection();
          endUpdate();
        }
      });
    installActionWrapper(map, "scrollUpChangeSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          clearSelection();
          int row = table.getSelectedRow();
          scrollBar.pageUpOrLeft();
          if (row >= 0) {
            table.setRowSelectionInterval(row, row);
          }
          maybeGrabSelection();
          endUpdate();
        }
      });
    installActionWrapper(map, "scrollDownChangeSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          clearSelection();
          int row = table.getSelectedRow();
          scrollBar.pageDownOrRight();
          if (row >= 0) {
            table.setRowSelectionInterval(row, row);
          }
          maybeGrabSelection();
          endUpdate();
        }
      });
    installActionWrapper(map, "selectPreviousRowExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
          }
          int newLead = getRowLead() - 1;
          int newAnchor = getRowAnchor();
          if (newLead < 0) {
            scrollBar.scrollUpOrLeft();
            ++newLead;
            ++newAnchor;
          }
          setSelection(newAnchor, newLead, getColAnchor(), getColLead());
          endUpdate();
        }
      });
    installActionWrapper(map, "selectPreviousColumnExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
          }
          int newLead = Math.max(0, getColLead() - 1);
          setSelection(getRowAnchor(), getRowLead(), getColAnchor(), newLead);
          endUpdate();
        }
      });
    installActionWrapper(map, "selectNextRowExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
          }
          int newLead = getRowLead() + 1;
          int newAnchor = getRowAnchor();
          if (newLead > numUsableRows) {
            scrollBar.scrollDownOrRight();
            --newLead;
            --newAnchor;
          }
          setSelection(newAnchor, newLead, getColAnchor(), getColLead());
          endUpdate();
        }
      });
    installActionWrapper(map, "selectNextColumnExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
          }
          int newLead = Math.min(model.getColumnCount() - 1, getColLead() + 1);
          setSelection(getRowAnchor(), getRowLead(), getColAnchor(), newLead);
          endUpdate();
        }
      });
    installActionWrapper(map, "scrollUpExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
          }
          int newLead = getRowLead() - numUsableRows;
          int newAnchor = getRowAnchor();
          if (newLead < 0) {
            scrollBar.pageUpOrLeft();
            newLead += numUsableRows;
            newAnchor += numUsableRows;
          }
          setSelection(newAnchor, newLead, getColAnchor(), getColLead());
          endUpdate();
        }
      });
    installActionWrapper(map, "scrollDownExtendSelection", new ActionWrapper() {
        public void actionPerformed(ActionEvent e) {
          beginUpdate();
          if (!haveAnchor()) {
            setAnchorFromTable();
            setLeadFromTable();
          }
          int newLead = getRowLead() + numUsableRows;
          int newAnchor = getRowAnchor();
          if (newLead > numUsableRows) {
            scrollBar.pageDownOrRight();
            newLead -= numUsableRows;
            newAnchor -= numUsableRows;
          }
          setSelection(newAnchor, newLead, getColAnchor(), getColLead());
          endUpdate();
        }
      });

    table.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          if (shouldIgnore(e)) {
            return;
          }
          if (e.isShiftDown()) {
            maybeGrabSelection();
            return;
          }
          clearSelection();
        }
      });

    table.addMouseMotionListener(new MouseMotionAdapter() {
        public void mouseDragged(MouseEvent e) {
          if (shouldIgnore(e)) {
            return;
          }

          Point p = e.getPoint();
          if (table.rowAtPoint(p) == -1) {
            Rectangle rect = new Rectangle();
            getBounds(rect);
            beginUpdate();
            if (p.y < rect.y) {
              scrollBar.scrollUpOrLeft();
              setSelection(getRowAnchor(), 0, getColAnchor(), getColLead());
            } else {
              scrollBar.scrollDownOrRight();
              setSelection(getRowAnchor(), numUsableRows, getColAnchor(), getColLead());
            }
            endUpdate();
          } else {
            maybeGrabSelection();
          }
        }
      });


    add(table, BorderLayout.CENTER);

    addComponentListener(new ComponentAdapter() {
        public void componentResized(ComponentEvent e) {
          recomputeNumVisibleRows();
          constrain();
        }
      });
    addHierarchyListener(new HierarchyListener() {
        public void hierarchyChanged(HierarchyEvent e) {
          recomputeNumVisibleRows();
          constrain();
        }
      });
    updateFromScrollBar();
  }

  /** Makes the given address visible somewhere in the window */
  public void makeVisible(Address addr) {
    BigInteger bi = addressToBigInt(addr);
    scrollBar.setValueHP(bi);
  }


  private void setupScrollBar() {
    if (is64Bit) {
      scrollBar =
        new HighPrecisionJScrollBar(
          Scrollbar.VERTICAL,
          new BigInteger(1, new byte[] {
            (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
          new BigInteger(1, new byte[] {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
          new BigInteger(1, new byte[] {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFC}));
      scrollBar.setUnitIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08}));
      scrollBar.setBlockIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40}));
    } else {
      scrollBar=
        new HighPrecisionJScrollBar(
          Scrollbar.VERTICAL,
          new BigInteger(1, new byte[] {
            (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
          new BigInteger(1, new byte[] {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00}),
          new BigInteger(1, new byte[] {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFC}));
      scrollBar.setUnitIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04}));
      scrollBar.setBlockIncrementHP(new BigInteger(1, new byte[] {
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x20}));
    }
    scrollBar.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          updateFromScrollBar();
        }
      });
  }

  private void updateFromScrollBar() {
    beginUpdate();
    BigInteger oldStartVal = startVal;
    startVal = scrollBar.getValueHP();
    constrain();
    model.fireTableDataChanged();
    if (oldStartVal != null) {
      modifySelection(oldStartVal.subtract(startVal).intValue() / addressSize);
    }
    endUpdate();
  }

  private void constrain() {
    BigInteger offset = new BigInteger(Integer.toString(addressSize * (numUsableRows)));
    BigInteger endVal = startVal.add(offset);
    if (endVal.compareTo(scrollBar.getMaximumHP()) > 0) {
      startVal = scrollBar.getMaximumHP().subtract(offset);
      endVal   = scrollBar.getMaximumHP();
      scrollBar.setValueHP(startVal);
      model.fireTableDataChanged();
    }
  }

  private void recomputeNumVisibleRows() {
    Rectangle rect = new Rectangle();
    getBounds(rect);
    int h = table.getRowHeight();
    numVisibleRows = (rect.height + (h - 1)) / h;
    numUsableRows  = numVisibleRows - 2;
    scrollBar.setBlockIncrementHP(new BigInteger(Integer.toString(addressSize * (numUsableRows))));
    model.fireTableDataChanged();
  }

  private String bigIntToHexString(BigInteger bi) {
    StringBuilder buf = new StringBuilder();
    buf.append("0x");
    String val = bi.toString(16);
    for (int i = 0; i < ((2 * addressSize) - val.length()); i++) {
      buf.append('0');
    }
    buf.append(val);
    return buf.toString();
  }

  private Address bigIntToAddress(BigInteger i) {
    String s = bigIntToHexString(i);
    return debugger.parseAddress(s);
  }

  private BigInteger addressToBigInt(Address a) {
    String s = addressToString(a);
    if (!s.startsWith("0x")) {
      throw new NumberFormatException(s);
    }
    return new BigInteger(s.substring(2), 16);
  }

  private String addressToString(Address a) {
    if (a == null) {
      if (is64Bit) {
        return "0x0000000000000000";
      } else {
        return "0x00000000";
      }
    }
    return a.toString();
  }

  private static void installActionWrapper(ActionMap map,
                                           String actionName,
                                           ActionWrapper wrapper) {
    wrapper.setParent(map.get(actionName));
    map.put(actionName, wrapper);
  }

  private boolean shouldIgnore(MouseEvent e) {
    return e.isConsumed() || (!(SwingUtilities.isLeftMouseButton(e) && table.isEnabled()));
  }

  private void clearSelection() {
    haveAnchor = false;
    haveLead = false;
  }

  private int updateLevel;
  private boolean updating()         { return updateLevel > 0; }
  private void    beginUpdate()      { ++updateLevel;          }
  private void    endUpdate()        { --updateLevel;          }

  private boolean haveAnchor()       { return haveAnchor;  }
  private boolean haveLead()         { return haveLead;    }
  private boolean haveSelection()    { return haveAnchor() && haveLead(); }
  private int     getRowAnchor()     { return rowAnchorIndex; }
  private int     getColAnchor()     { return colAnchorIndex; }
  private int     getRowLead()       { return rowLeadIndex;   }
  private int     getColLead()       { return colLeadIndex;   }

  private void setAnchorFromTable() {
    setAnchor(table.getSelectionModel().getAnchorSelectionIndex(),
              table.getColumnModel().getSelectionModel().getAnchorSelectionIndex());
  }
  private void setLeadFromTable() {
    setLead(table.getSelectionModel().getAnchorSelectionIndex(),
            table.getColumnModel().getSelectionModel().getAnchorSelectionIndex());
  }
  private void setAnchor(int row, int col) {
    rowAnchorIndex = row;
    colAnchorIndex = col;
    haveAnchor = true;
  }
  private void setLead(int row, int col) {
    rowLeadIndex = row;
    colLeadIndex = col;
    haveLead = true;
  }
  private int clamp(int val, int min, int max) {
    return Math.max(Math.min(val, max), min);
  }
  private void maybeGrabSelection() {
    if (table.getSelectedRow() != -1) {
      ListSelectionModel rowSel = table.getSelectionModel();
      ListSelectionModel colSel = table.getColumnModel().getSelectionModel();
      if (!haveAnchor()) {
        setSelection(rowSel.getAnchorSelectionIndex(), rowSel.getLeadSelectionIndex(),
                     colSel.getAnchorSelectionIndex(), colSel.getLeadSelectionIndex());
      } else {
        setSelection(getRowAnchor(), rowSel.getLeadSelectionIndex(),
                     getColAnchor(), colSel.getLeadSelectionIndex());
      }
    }
  }
  private void setSelection(int rowAnchor, int rowLead, int colAnchor, int colLead) {
    setAnchor(rowAnchor, colAnchor);
    setLead(rowLead, colLead);
    table.setRowSelectionInterval(clamp(rowAnchor, 0, numUsableRows),
                                  clamp(rowLead, 0, numUsableRows));
    table.setColumnSelectionInterval(colAnchor, colLead);
  }
  private void modifySelection(int amount) {
    if (haveSelection()) {
      setSelection(getRowAnchor() + amount, getRowLead() + amount,
                   getColAnchor(), getColLead());
    }
  }
  private void printSelection() {
    System.err.println("Selection updated to (" +
                       model.getValueAt(getRowAnchor(), getColAnchor()) +
                       ", " +
                       model.getValueAt(getRowLead(), getColLead()) + ") [(" +
                       getRowAnchor() + ", " + getColAnchor() + "), (" +
                       getRowLead() + ", " + getColLead() + ")]");
  }
}
