/*
 * Copyright (c) 2003, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.awt.X11;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.peer.*;
import java.io.*;
import java.util.Locale;
import java.util.Arrays;
import java.security.AccessController;
import java.security.PrivilegedAction;

import sun.awt.AWTAccessor.ComponentAccessor;
import sun.util.logging.PlatformLogger;
import sun.awt.AWTAccessor;

class XFileDialogPeer extends XDialogPeer
        implements FileDialogPeer, ActionListener, ItemListener,
                   KeyEventDispatcher, XChoicePeerListener {

    private static final PlatformLogger log =
            PlatformLogger.getLogger("sun.awt.X11.XFileDialogPeer");

    FileDialog  target;

    String      file;

    String      dir;

    String      title;
    int         mode;
    FilenameFilter  filter;

    private static final int PATH_CHOICE_WIDTH = 20;

    String      savedFile;

    String      savedDir;
    String      userDir;

    Dialog      fileDialog;

    GridBagLayout       gbl;
    GridBagLayout       gblButtons;
    GridBagConstraints  gbc;


    TextField   filterField;

    TextField   selectionField;

    List        directoryList;

    List        fileList;

    Panel       buttons;
    Button      openButton;
    Button      filterButton;
    Button      cancelButton;
    Choice      pathChoice;
    TextField   pathField;
    Panel       pathPanel;

    String cancelButtonText = null;
    String enterFileNameLabelText = null;
    String filesLabelText= null;
    String foldersLabelText= null;
    String pathLabelText= null;
    String filterLabelText= null;
    String openButtonText= null;
    String saveButtonText= null;
    String actionButtonText= null;


    void installStrings() {
        Locale l = target.getLocale();
        UIDefaults uid = XToolkit.getUIDefaults();
        cancelButtonText = uid.getString("FileChooser.cancelButtonText",l);
        enterFileNameLabelText = uid.getString("FileChooser.enterFileNameLabelText",l);
        filesLabelText = uid.getString("FileChooser.filesLabelText",l);
        foldersLabelText = uid.getString("FileChooser.foldersLabelText",l);
        pathLabelText = uid.getString("FileChooser.pathLabelText",l);
        filterLabelText = uid.getString("FileChooser.filterLabelText",l);
        openButtonText = uid.getString("FileChooser.openButtonText",l);
        saveButtonText  = uid.getString("FileChooser.saveButtonText",l);

    }

    XFileDialogPeer(FileDialog target) {
        super((Dialog)target);
        this.target = target;
    }

    @SuppressWarnings({"removal","deprecation"})
    private void init(FileDialog target) {
        fileDialog = target; 
        this.title = target.getTitle();
        this.mode = target.getMode();
        this.target = target;
        this.filter = target.getFilenameFilter();

        savedFile = target.getFile();
        savedDir = target.getDirectory();
        userDir = AccessController.doPrivileged(
            new PrivilegedAction<String>() {
                public String run() {
                    return System.getProperty("user.dir");
                }
            });

        installStrings();
        gbl = new GridBagLayout();
        gblButtons = new GridBagLayout();
        gbc = new GridBagConstraints();
        fileDialog.setLayout(gbl);

        buttons = new Panel();
        buttons.setLayout(gblButtons);
        actionButtonText = (target.getMode() == FileDialog.SAVE) ? saveButtonText : openButtonText;
        openButton = new Button(actionButtonText);

        filterButton = new Button(filterLabelText);
        cancelButton = new Button(cancelButtonText);
        directoryList = new List();
        fileList = new List();
        filterField = new TextField();
        selectionField = new TextField();

        boolean isMultipleMode =
            AWTAccessor.getFileDialogAccessor().isMultipleMode(target);
        fileList.setMultipleMode(isMultipleMode);

        Insets noInset = new Insets(0, 0, 0, 0);
        Insets textFieldInset = new Insets(0, 8, 0, 8);
        Insets leftListInset = new Insets(0, 8, 0, 4);
        Insets rightListInset = new Insets(0, 4, 0, 8);
        Insets separatorInset = new Insets(8, 0, 0, 0);
        Insets labelInset = new Insets(0, 8, 0, 0);
        Insets buttonsInset = new Insets(10, 8, 10, 8);


        Font f = new Font(Font.DIALOG, Font.PLAIN, 12);

        Label label = new Label(pathLabelText);
        label.setFont(f);
        addComponent(label, gbl, gbc, 0, 0, 1,
                     GridBagConstraints.WEST, (Container)fileDialog,
                     1, 0, GridBagConstraints.NONE, labelInset);

        pathField = new TextField(savedDir != null ? savedDir : userDir);
        @SuppressWarnings("serial") 
        Choice tmp = new Choice() {
                public Dimension getPreferredSize() {
                    return new Dimension(PATH_CHOICE_WIDTH, pathField.getPreferredSize().height);
                }
            };
        pathChoice = tmp;
        pathPanel = new Panel();
        pathPanel.setLayout(new BorderLayout());

        pathPanel.add(pathField,BorderLayout.CENTER);
        pathPanel.add(pathChoice,BorderLayout.EAST);
        addComponent(pathPanel, gbl, gbc, 0, 1, 2,
                    GridBagConstraints.WEST, (Container)fileDialog,
                   1, 0, GridBagConstraints.HORIZONTAL, textFieldInset);



        label = new Label(filterLabelText);

        label.setFont(f);
        addComponent(label, gbl, gbc, 0, 2, 1,
                     GridBagConstraints.WEST, (Container)fileDialog,
                     1, 0, GridBagConstraints.NONE, labelInset);
        addComponent(filterField, gbl, gbc, 0, 3, 2,
                     GridBagConstraints.WEST, (Container)fileDialog,
                     1, 0, GridBagConstraints.HORIZONTAL, textFieldInset);

        label = new Label(foldersLabelText);

        label.setFont(f);
        addComponent(label, gbl, gbc, 0, 4, 1,
                     GridBagConstraints.WEST, (Container)fileDialog,
                     1, 0, GridBagConstraints.NONE, labelInset);

        label = new Label(filesLabelText);

        label.setFont(f);
        addComponent(label, gbl, gbc, 1, 4, 1,
                     GridBagConstraints.WEST, (Container)fileDialog,
                     1, 0, GridBagConstraints.NONE, labelInset);
        addComponent(directoryList, gbl, gbc, 0, 5, 1,
                     GridBagConstraints.WEST, (Container)fileDialog,
                     1, 1, GridBagConstraints.BOTH, leftListInset);
        addComponent(fileList, gbl, gbc, 1, 5, 1,
                     GridBagConstraints.WEST, (Container)fileDialog,
                     1, 1, GridBagConstraints.BOTH, rightListInset);

        label = new Label(enterFileNameLabelText);

        label.setFont(f);
        addComponent(label, gbl, gbc, 0, 6, 1,
                     GridBagConstraints.WEST, (Container)fileDialog,
                     1, 0, GridBagConstraints.NONE, labelInset);
        addComponent(selectionField, gbl, gbc, 0, 7, 2,
                     GridBagConstraints.WEST, (Container)fileDialog,
                     1, 0, GridBagConstraints.HORIZONTAL, textFieldInset);
        addComponent(new Separator(fileDialog.size().width, 2, Separator.HORIZONTAL), gbl, gbc, 0, 8, 15,
                     GridBagConstraints.WEST, (Container)fileDialog,
                     1, 0, GridBagConstraints.HORIZONTAL, separatorInset);

        addComponent(openButton, gblButtons, gbc, 0, 0, 1,
                     GridBagConstraints.WEST, (Container)buttons,
                     1, 0, GridBagConstraints.NONE, noInset);
        addComponent(filterButton, gblButtons, gbc, 1, 0, 1,
                     GridBagConstraints.CENTER, (Container)buttons,
                     1, 0, GridBagConstraints.NONE, noInset);
        addComponent(cancelButton, gblButtons, gbc, 2, 0, 1,
                     GridBagConstraints.EAST, (Container)buttons,
                     1, 0, GridBagConstraints.NONE, noInset);

        addComponent(buttons, gbl, gbc, 0, 9, 2,
                     GridBagConstraints.WEST, (Container)fileDialog,
                     1, 0, GridBagConstraints.HORIZONTAL, buttonsInset);

        fileDialog.setSize(400, 400);

        XChoicePeer choicePeer = AWTAccessor.getComponentAccessor()
                                            .getPeer(pathChoice);
        choicePeer.setDrawSelectedItem(false);
        choicePeer.setAlignUnder(pathField);

        filterField.addActionListener(this);
        selectionField.addActionListener(this);
        directoryList.addActionListener(this);
        directoryList.addItemListener(this);
        fileList.addItemListener(this);
        fileList.addActionListener(this);
        openButton.addActionListener(this);
        filterButton.addActionListener(this);
        cancelButton.addActionListener(this);
        pathChoice.addItemListener(this);
        pathField.addActionListener(this);

        target.addWindowListener(
            new WindowAdapter(){
                public void windowClosing(WindowEvent e){
                    handleCancel();
                }
            }
        );

        pathChoice.addItemListener(this);

    }

    public void updateMinimumSize() {
    }

    public void updateIconImages() {
        if (winAttr.icons == null){
            winAttr.iconsInherited = false;
            winAttr.icons = getDefaultIconInfo();
            setIconHints(winAttr.icons);
        }
    }

    /**
     * add Component comp to the container cont.
     * add the component to the correct GridBagLayout
     */
    void addComponent(Component comp, GridBagLayout gb, GridBagConstraints c, int gridx,
                      int gridy, int gridwidth, int anchor, Container cont, int weightx, int weighty,
                      int fill, Insets in) {
        c.gridx = gridx;
        c.gridy = gridy;
        c.gridwidth = gridwidth;
        c.anchor = anchor;
        c.weightx = weightx;
        c.weighty = weighty;
        c.fill = fill;
        c.insets = in;
        gb.setConstraints(comp, c);
        cont.add(comp);
    }

    /**
     * get fileName
     */
    String getFileName(String str) {
        if (str == null) {
            return "";
        }

        int index = str.lastIndexOf('/');

        if (index == -1) {
            return str;
        } else {
            return str.substring(index + 1);
        }
    }

    /** handleFilter
     *
     */
    void handleFilter(String f) {

        if (f == null) {
            return;
        }
        setFilterEntry(dir,f);

        directoryList.select(0);
        if (fileList.getItemCount() != 0) {
            fileList.requestFocus();
        } else {
            directoryList.requestFocus();
        }
    }

    /**
     * handle the selection event
     */
    void handleSelection(String file) {

        int index = file.lastIndexOf(java.io.File.separatorChar);

        if (index == -1) {
            savedDir = this.dir;
            savedFile = file;
        } else {
            savedDir = file.substring(0, index+1);
            savedFile = file.substring(index+1);
        }

        String[] fileNames = fileList.getSelectedItems();
        int filesNumber = (fileNames != null) ? fileNames.length : 0;
        File[] files = new File[filesNumber];
        for (int i = 0; i < filesNumber; i++) {
            files[i] = new File(savedDir, fileNames[i]);
        }

        AWTAccessor.FileDialogAccessor fileDialogAccessor = AWTAccessor.getFileDialogAccessor();

        fileDialogAccessor.setDirectory(target, savedDir);
        fileDialogAccessor.setFile(target, savedFile);
        fileDialogAccessor.setFiles(target, files);
    }

    /**
     * handle the cancel event
     */
    @SuppressWarnings("deprecation")
    void handleCancel() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .removeKeyEventDispatcher(this);

        setSelectionField(null);
        setFilterField(null);
        directoryList.clear();
        fileList.clear();

        AWTAccessor.FileDialogAccessor fileDialogAccessor = AWTAccessor.getFileDialogAccessor();

        fileDialogAccessor.setDirectory(target, null);
        fileDialogAccessor.setFile(target, null);
        fileDialogAccessor.setFiles(target, null);

        handleQuitButton();
    }

    /**
     * handle the quit event
     */
    @SuppressWarnings("deprecation")
    void handleQuitButton() {
        dir = null;
        file = null;
        target.hide();
    }

    /**
     * set the entry of the new dir with f
     */
    @SuppressWarnings("deprecation")
    void setFilterEntry(String d, String f) {
        File fe = new File(d);

        if (fe.isDirectory() && fe.canRead()) {
            setSelectionField(target.getFile());

            if (f.isEmpty()) {
                f = "*";
                setFilterField(f);
            } else {
                setFilterField(f);
            }
            String[] l;

            if (f.equals("*")) {
                l = fe.list();
            } else {
                FileDialogFilter ff = new FileDialogFilter(f);
                l = fe.list(ff);
            }
            if (l == null) {
                this.dir = getParentDirectory();
                return;
            }
            directoryList.clear();
            fileList.clear();
            directoryList.setVisible(false);
            fileList.setVisible(false);

            directoryList.addItem("..");
            Arrays.sort(l);
            for (int i = 0 ; i < l.length ; i++) {
                File file = new File(d + l[i]);
                if (file.isDirectory()) {
                    directoryList.addItem(l[i] + "/");
                } else {
                    if (filter != null) {
                        if (filter.accept(new File(l[i]),l[i]))  fileList.addItem(l[i]);
                    }
                    else fileList.addItem(l[i]);
                }
            }
            this.dir = d;

            pathField.setText(dir);


            target.setDirectory(this.dir);
            directoryList.setVisible(true);
            fileList.setVisible(true);
        }
    }


    String[] getDirList(String dir) {
        if (!dir.endsWith("/"))
            dir = dir + "/";
        char[] charr = dir.toCharArray();
        int numSlashes = 0;
        for (int i=0;i<charr.length;i++) {
           if (charr[i] == '/')
               numSlashes++;
        }
        String[] starr =  new String[numSlashes];
        int j=0;
        for (int i=charr.length-1;i>=0;i--) {
            if (charr[i] == '/')
            {
                starr[j++] = new String(charr,0,i+1);
            }
        }
        return starr;
    }

    /**
     * set the text in the selectionField
     */
    void setSelectionField(String str) {
        selectionField.setText(str);
    }

    /**
     * set the text in the filterField
     */
    void setFilterField(String str) {
        filterField.setText(str);
    }

    /**
     *
     * @see java.awt.event.ItemEvent
     * ItemEvent.ITEM_STATE_CHANGED
     */
    public void itemStateChanged(ItemEvent itemEvent){
        if (itemEvent.getID() != ItemEvent.ITEM_STATE_CHANGED ||
            itemEvent.getStateChange() == ItemEvent.DESELECTED) {
            return;
        }

        Object source = itemEvent.getSource();

        if (source == pathChoice) {
            /*
             * Update the selection ('folder name' text field) after
             * the current item changing in the unfurled choice by the arrow keys.
             * See 6259434, 6240074 for more information
             */
            String dir = pathChoice.getSelectedItem();
            pathField.setText(dir);
        } else if (directoryList == source) {
            setFilterField(getFileName(filterField.getText()));
        } else if (fileList == source) {
            String file = fileList.getItem((Integer)itemEvent.getItem());
            setSelectionField(file);
        }
    }

    /*
     * Updates the current directory only if directoryList-specific
     * action occurred. Returns false if the forward directory is inaccessible
     */
    boolean updateDirectoryByUserAction(String str) {

        String dir;
        if (str.equals("..")) {
            dir = getParentDirectory();
        }
        else {
            dir = this.dir + str;
        }

        File fe = new File(dir);
        if (fe.canRead()) {
            this.dir = dir;
            return true;
        }else {
            return false;
        }
    }

    String getParentDirectory(){
        String parent = this.dir;
        if (!this.dir.equals("/"))   
        {
            if (dir.endsWith("/"))
                parent = parent.substring(0,parent.lastIndexOf("/"));

            parent = parent.substring(0,parent.lastIndexOf("/")+1);
        }
        return parent;
    }

    public void actionPerformed( ActionEvent actionEvent ) {
        String actionCommand = actionEvent.getActionCommand();
        Object source = actionEvent.getSource();

        if (actionCommand.equals(actionButtonText)) {
            handleSelection( selectionField.getText() );
            handleQuitButton();
        } else if (actionCommand.equals(filterLabelText)) {
            handleFilter( filterField.getText() );
        } else if (actionCommand.equals(cancelButtonText)) {
            handleCancel();
        } else if ( source instanceof TextField ) {
            if ( selectionField == ((TextField)source) ) {
                handleSelection(selectionField.getText());
                handleQuitButton();
            } else if (filterField == ((TextField)source)) {
                handleFilter(filterField.getText());
            } else if (pathField == ((TextField)source)) {
                target.setDirectory(pathField.getText());
            }
        } else if (source instanceof List) {
            if (directoryList == ((List)source)) {
                if (updateDirectoryByUserAction(actionCommand)){
                    handleFilter( getFileName( filterField.getText() ) );
                }
            } else if (fileList == ((List)source)) {
                handleSelection( actionCommand );
                handleQuitButton();
            }
        }
    }

    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        int id = keyEvent.getID();
        int keyCode = keyEvent.getKeyCode();

        if (id == KeyEvent.KEY_PRESSED && keyCode == KeyEvent.VK_ESCAPE) {
            synchronized (target.getTreeLock()) {
                Component comp = (Component) keyEvent.getSource();
                while (comp != null) {
                    ComponentAccessor acc = AWTAccessor.getComponentAccessor();
                    if (comp == pathChoice) {
                        XChoicePeer choicePeer = acc.getPeer(pathChoice);
                        if (choicePeer.isUnfurled()){
                            return false;
                        }
                    }
                    Object peer = acc.getPeer(comp);
                    if (peer == this) {
                        handleCancel();
                        return true;
                    }
                    comp = comp.getParent();
                }
            }
        }

        return false;
    }


    /**
     * set the file
     */
    public void setFile(String file) {

        if (file == null) {
            this.file = null;
            return;
        }

        if (this.dir == null) {
            String d = "./";
            File f = new File(d, file);

            if (f.isFile()) {
                this.file = file;
                setDirectory(d);
            }
        } else {
            File f = new File(this.dir, file);
            if (f.isFile()) {
                this.file = file;
            }
        }

        setSelectionField(file);
    }

    /**
     * set the directory
     * FIXME: we should update 'savedDir' after programmatically 'setDirectory'
     * Otherwise, SavedDir will be not null before second showing
     * So the current directory of the file dialog will be incorrect after second showing
     * since 'setDirectory' will be ignored
     * We can't update savedDir here now since it used very often
     */
    public void setDirectory(String dir) {

        if (dir == null) {
            this.dir = null;
            return;
        }

        if (dir.equals(this.dir)) {
            return;
        }

        int i;
        if ((i=dir.indexOf("~")) != -1) {

            dir = dir.substring(0,i) + System.getProperty("user.home") + dir.substring(i+1);
        }

        File fe = new File(dir).getAbsoluteFile();
        if (log.isLoggable(PlatformLogger.Level.FINE)) {
            log.fine("Current directory : " + fe);
        }

        if (!fe.isDirectory()) {
            dir = "./";
            fe = new File(dir).getAbsoluteFile();

            if (!fe.isDirectory()) {
                return;
            }
        }
        try {
            dir = this.dir = fe.getCanonicalPath();
        } catch (java.io.IOException ie) {
            dir = this.dir = fe.getAbsolutePath();
        }
        pathField.setText(this.dir);


        if (dir.endsWith("/")) {
            this.dir = dir;
            handleFilter("");
        } else {
            this.dir = dir + "/";
            handleFilter("");
        }

    }

    /**
     * set filenameFilter
     *
     */
    public void setFilenameFilter(FilenameFilter filter) {
        this.filter = filter;
    }


    public void dispose() {
        FileDialog fd = (FileDialog)fileDialog;
        if (fd != null) {
            fd.removeAll();
        }
        super.dispose();
    }

    @SuppressWarnings("deprecation")
    public void setVisible(boolean b){
        if (fileDialog == null) {
            init(target);
        }

        if (savedDir != null || userDir != null) {
            setDirectory(savedDir != null ? savedDir : userDir);
        }

        if (savedFile != null) {
            setFile(savedFile);
        }

        super.setVisible(b);
        XChoicePeer choicePeer = AWTAccessor.getComponentAccessor()
                                            .getPeer(pathChoice);
        if (b == true){
            choicePeer.addXChoicePeerListener(this);
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                                .addKeyEventDispatcher(this);
        }else{
            choicePeer.removeXChoicePeerListener();
            KeyboardFocusManager.getCurrentKeyboardFocusManager()
                                .removeKeyEventDispatcher(this);
        }

        selectionField.requestFocusInWindow();
    }

    /*
     * Adding items to the path choice based on the text string
     * See 6240074 for more information
     */
    public void addItemsToPathChoice(String text){
        String[] dirList = getDirList(text);
        for (int i = 0; i < dirList.length; i++) pathChoice.addItem(dirList[i]);
    }

    /*
     * Refresh the unfurled choice at the time of the opening choice according to the text of the path field
     * See 6240074 for more information
     */
    public void unfurledChoiceOpening(ListHelper choiceHelper){

        if (choiceHelper.getItemCount() == 0){
            addItemsToPathChoice(pathField.getText());
            return;
        }

        if (pathChoice.getItem(0).equals(pathField.getText()))
            return;

        pathChoice.removeAll();
        addItemsToPathChoice(pathField.getText());
    }

    /*
     * Refresh the file dialog at the time of the closing choice according to the selected item of the choice
     * See 6240074 for more information
     */
    public void unfurledChoiceClosing(){
          String dir = pathChoice.getSelectedItem();
          target.setDirectory(dir);
    }
}

@SuppressWarnings("serial") 
class Separator extends Canvas {
    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    int orientation;

    @SuppressWarnings("deprecation")
    public Separator(int length, int thickness, int orient) {
        super();
        orientation = orient;
        if (orient == HORIZONTAL) {
            resize(length, thickness);
        } else {
            resize(thickness, length);
        }
    }

    @SuppressWarnings("deprecation")
    public void paint(Graphics g) {
        int x1, y1, x2, y2;
        Rectangle bbox = bounds();
        Color c = getBackground();
        Color brighter = c.brighter();
        Color darker = c.darker();

        if (orientation == HORIZONTAL) {
            x1 = 0;
            x2 = bbox.width - 1;
            y1 = y2 = bbox.height/2 - 1;

        } else {
            x1 = x2 = bbox.width/2 - 1;
            y1 = 0;
            y2 = bbox.height - 1;
        }
        g.setColor(darker);
        g.drawLine(x1, y2, x2, y2);
        g.setColor(brighter);
        if (orientation == HORIZONTAL)
            g.drawLine(x1, y2+1, x2, y2+1);
        else
            g.drawLine(x1+1, y2, x2+1, y2);
    }
}

/*
 * Motif file dialogs let the user specify a filter that controls the files that
 * are displayed in the dialog. This filter is generally specified as a regular
 * expression. The class is used to implement Motif-like filtering.
 */
class FileDialogFilter implements FilenameFilter {

    String filter;

    public FileDialogFilter(String f) {
        filter = f;
    }

    /*
     * Tells whether or not the specified file should be included in a file list
     */
    public boolean accept(File dir, String fileName) {

        File f = new File(dir, fileName);

        if (f.isDirectory()) {
            return true;
        } else {
            return matches(fileName, filter);
        }
    }

    /*
     * Tells whether or not the input string matches the given filter
     */
    private boolean matches(String input, String filter) {
        String regex = convert(filter);
        return input.matches(regex);
    }

    /*
     * Converts the filter into the form which is acceptable by Java's regexps
     */
    private String convert(String filter) {
        String regex = "^" + filter + "$";
        regex = regex.replaceAll("\\.", "\\\\.");
        regex = regex.replaceAll("\\?", ".");
        regex = regex.replaceAll("\\*", ".*");
        return regex;
    }
}
