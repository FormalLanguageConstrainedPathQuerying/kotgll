/*
* Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.swingset3.demos.frame;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.DemoUtilities;

/**
* Demo for Swing's JFrame toplevel component.
*
* @author aim
*/
@DemoProperties(
value = "JFrame Demo",
category = "Toplevel Containers",
description = "Demonstrates JFrame, Swing's top-level primary window container.",
sourceFiles = {
"com/sun/swingset3/demos/frame/BusyGlass.java",
"com/sun/swingset3/demos/frame/FrameDemo.java",
"com/sun/swingset3/demos/DemoUtilities.java",
"com/sun/swingset3/demos/frame/resources/FrameDemo.html",
"com/sun/swingset3/demos/frame/resources/images/FrameDemo.gif"
}
)
public class FrameDemo extends JPanel {

public static final String DEMO_TITLE = FrameDemo.class.getAnnotation(DemoProperties.class).value();
public static final String INTERNAL_FRAME = "Demo JFrame";
public static final String MENU = "File";
public static final String MENU_ITEM1 = "Open";
public static final String MENU_ITEM2 = "Save";
public static final String TOOLBAR = "Toolbar";
public static final String TOOLBAR_BUTTON = "Toolbar Button";
public static final String CONTENT_LABEL = "I'm content but a little blue.";
public static final String STATUS_LABEL = "I show status.";
public static final String SHOW_BUTTON = "Show JFrame...";
public static final String BUSY_CHECKBOX = "Frame busy";
public static final Dimension CONTENT_LABEL_SIZE = new Dimension(300, 160);
public static final Color CONTENT_LABEL_COLOR = new Color(197, 216, 236);
public static final int STATUS_LABEL_HOR_ALIGNMENT = JLabel.LEADING;
public static final EmptyBorder STATUS_LABEL_BORDER = new EmptyBorder(4, 4, 4, 4);

static {
if (System.getProperty("os.name").equals("Mac OS X")) {
System.setProperty("apple.laf.useScreenMenuBar", "true");
}
}

private JFrame frame;

private JComponent frameSpaceholder;

public FrameDemo() {
initComponents();
}

protected void initComponents() {
frame = createFrame();

setLayout(new BorderLayout());
add(createControlPanel(), BorderLayout.WEST);
frameSpaceholder = createFrameSpaceholder(frame);
add(frameSpaceholder, BorderLayout.CENTER);
}

protected JComponent createControlPanel() {
Box controlPanel = Box.createVerticalBox();
controlPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

JButton showButton = new JButton(SHOW_BUTTON);
showButton.addActionListener(new ShowActionListener());
controlPanel.add(showButton);

JCheckBox busyCheckBox = new JCheckBox(BUSY_CHECKBOX);
busyCheckBox.setSelected(false);
busyCheckBox.addChangeListener(new BusyChangeListener());
controlPanel.add(busyCheckBox);

return controlPanel;
}

private static JComponent createFrameSpaceholder(JFrame frame) {
JPanel framePlaceholder = new JPanel();
Dimension prefSize = frame.getPreferredSize();
prefSize.width += 12;
prefSize.height += 12;
framePlaceholder.setPreferredSize(prefSize);

return framePlaceholder;
}

private static JFrame createFrame() {

JFrame frame = new JFrame(INTERNAL_FRAME);
frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

Image iconImage = null;
try {
URL imageURL = FrameDemo.class.getResource("resources/images/swingingduke.gif");
iconImage = ImageIO.read(imageURL);
} catch (Exception e) {
}
frame.setIconImage(iconImage);

frame.setGlassPane(new BusyGlass());

JMenuBar menubar = new JMenuBar();
frame.setJMenuBar(menubar);
JMenu menu = new JMenu(MENU);
menubar.add(menu);
menu.add(MENU_ITEM1);
menu.add(MENU_ITEM2);

JToolBar toolbar = new JToolBar(TOOLBAR);
frame.add(toolbar, BorderLayout.NORTH);
toolbar.add(new JButton(TOOLBAR_BUTTON));

JLabel label = new JLabel(CONTENT_LABEL);
label.setHorizontalAlignment(JLabel.CENTER);
label.setPreferredSize(CONTENT_LABEL_SIZE);
label.setBackground(CONTENT_LABEL_COLOR);
label.setOpaque(true); 
frame.add(label);

JLabel statusLabel = new JLabel(STATUS_LABEL);
statusLabel.setBorder(STATUS_LABEL_BORDER);
statusLabel.setHorizontalAlignment(STATUS_LABEL_HOR_ALIGNMENT);
frame.add(statusLabel, BorderLayout.SOUTH);

frame.pack();

return frame;
}

public void start() {
DemoUtilities.setToplevelLocation(frame, frameSpaceholder, SwingConstants.CENTER);
showFrame();
}

public void stop() {
frame.setVisible(false);
}

public void showFrame() {
if (frame.isShowing()) {
frame.toFront();
} else {
frame.setVisible(true);
}
}

public void setFrameBusy(boolean busy) {
frame.getGlassPane().setVisible(busy);
frame.getJMenuBar().setEnabled(!busy);
}

public boolean isFrameBusy() {
return frame.getGlassPane().isVisible();
}


private class ShowActionListener implements ActionListener {
public void actionPerformed(ActionEvent actionEvent) {
showFrame();
}
}

private class BusyChangeListener implements ChangeListener {
public void stateChanged(ChangeEvent changeEvent) {
JCheckBox busyCheckBox = (JCheckBox) changeEvent.getSource();
setFrameBusy(busyCheckBox.isSelected());
showFrame(); 
}
}

public static void main(String args[]) {
EventQueue.invokeLater(new Runnable() {
public void run() {
JFrame frame = new JFrame(DEMO_TITLE);
FrameDemo demo = new FrameDemo();
frame.add(demo);
frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
frame.pack();
frame.setLocationRelativeTo(null);
frame.setVisible(true);
demo.start();
}
});
}
}
