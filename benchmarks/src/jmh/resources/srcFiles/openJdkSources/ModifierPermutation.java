/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
  test %I% %E%
  @bug 6315717
  @summary presses buttons in all permutations and verifies modifiers
  @author Andrei Dmitriev : area=awt.mouse
  @run main ModifierPermutation
 */

/*
The test will try to press-release every button present on the mouse in different order.
Here are some abbreviations:
 BUTTON1 press = P1
 BUTTON2 press = P2 etc.
 BUTTON1 release = R1
 BUTTON2 release = R2 etc.
Only sequences alike below are possible : <P1, P2, R2, R1>.
Sequences like <P1, P2, R1, R2> will not be covered by this test due to its probable complexity.
 */

import java.awt.*;
import sun.awt.SunToolkit;
import java.awt.event.*;
import java.util.Arrays;

public class ModifierPermutation {
    static boolean failed = false;
    final static int BUTTONSNUMBER = MouseInfo.getNumberOfButtons();

/*
 * Because of some problems with BUTTONx_MASK
 * (they are not ordered. Instead, their values are: 16 8 4)
 * We have to use array [1..n] and make every permutation on its
 * containment. After each permutation, make the same thing with
 * array of buttons and array of expected modifiers.
 */
    static SunToolkit st = (SunToolkit)(Toolkit.getDefaultToolkit());
    static int [] mouseButtons = new int [BUTTONSNUMBER]; 
    static int [] mouseButtonsDown = new int [BUTTONSNUMBER]; 

    static int [] affectedButtonsToPressRelease;

    static Robot robot;
    static CheckingAdapter adapterTest1;
    static Frame f;

    static {
        for (int i = 0; i < BUTTONSNUMBER; i++){
            mouseButtons[i] = InputEvent.getMaskForButton(i+1); 
            mouseButtonsDown[i] = InputEvent.getMaskForButton(i+1);
        }
/*        mouseButtons[0] = InputEvent.BUTTON1_MASK;
        mouseButtons[1] = InputEvent.BUTTON2_MASK;
        mouseButtons[2] = InputEvent.BUTTON3_MASK;
 */
    }

    public static void main(String s[]){
        init();

        try {
            robot = new Robot();
        } catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Test failed.", e);
        }
        robot.delay(500);
        robot.mouseMove(f.getLocationOnScreen().x + f.getWidth()/2, f.getLocationOnScreen().y + f.getHeight()/2);
        robot.delay(500);
        for (int k = 0; k < factorial(mouseButtons.length)-1; k++){
            for (int buttonsToPressNumber = 2; buttonsToPressNumber <= BUTTONSNUMBER; buttonsToPressNumber++ ){
                System.out.println(">>>");

                affectedButtonsToPressRelease = Arrays.copyOf(mouseButtons, buttonsToPressNumber);


                dumpArray("Affected Buttons ", affectedButtonsToPressRelease);
                pressAllButtons(affectedButtonsToPressRelease);
                releaseAllButtonsForwardOrder(affectedButtonsToPressRelease);

                System.out.println("<<<");
            }
            nextPermutation(k, mouseButtons);
            dumpArray("mouseButtons (step="+k+")", mouseButtons);
        }
    }

    private static void init(){
        adapterTest1 = new CheckingAdapter();
        f = new Frame("Robot presses mouse here");
        f.setSize(300, 300);
        f.setVisible(true);
        f.addMouseListener(adapterTest1);
    }
    public static int factorial(int t){
        if (t <=1 ) {
            return 1;
        } else {
            return t*factorial(t-1);
        }
    }

    static volatile int currentButtonIndexUnderAction;

    public static void pressAllButtons(int []array){
        for (int i = 0; i <array.length; i ++){
            if (failed) {
                throw new RuntimeException("PRESSED_EVENT is not filled with correct values. Review messaage above.");
            }
            System.out.println("Pressing button = " + array[i]);
            currentButtonIndexUnderAction = i;
            robot.mousePress(array[i]);
            System.out.println("currentButtonIndexUnderAction ="+currentButtonIndexUnderAction);
            st.realSync();
        }
    }

    public static void releaseAllButtonsForwardOrder(int []array){
        for (int i = 0; i <array.length; i ++){
            System.out.println("Releasing button = " + array[i]);
            currentButtonIndexUnderAction = i;
            robot.mouseRelease(array[i]);
            System.out.println("currentButtonIndexUnderAction ="+currentButtonIndexUnderAction);
            st.realSync();
        }
    }

    public static void checkModifiersOnPress(MouseEvent e){
        System.out.println("checkModifiers. currentButtonIndexUnderAction ="+currentButtonIndexUnderAction);
        for (int i = 0; i<= currentButtonIndexUnderAction; i++){
            if ((e.getModifiersEx() & affectedButtonsToPressRelease[i]) == 0){
                System.out.println("ERROR["+i+"]: PRESSED_EVENT is not filled with correct values. affectedButtonsToPressRelease[i]= "+affectedButtonsToPressRelease[i] +" Event = "+e);
                ModifierPermutation.failed = true;
            } else {
                System.out.println("CORRECT["+i+"]: affectedButtonsToPressRelease[i]= "+affectedButtonsToPressRelease[i]+ " Event = "+e);
            }
        }
    }

    /*======================================================================*/
    public static void dumpValues(int button, int modifiers, int modifiersStandard, int modifiersEx, int modifiersExStandard){
        System.out.println("Button = "+button + "Modifiers = "+ modifiers + " standard = "+ modifiersStandard);
        System.out.println("                   ModifiersEx = "+ modifiersEx + " standardEx = "+ modifiersExStandard);
    }

    public static void dumpArray(String id, int [] array){
        System.out.print(id);
        for (int i = 0; i < array.length; i++){
            System.out.print(array[i]+" ");
        }
        System.out.println();
    }
    public static void nextPermutation(int step, int []array){
        int i;
        int leftEl = 0;
        int rightEl = 0;

        i = array.length - 2;
        while (i>=0) {
            if (array[i] < array[i+1]){
                leftEl = i;
                break;
            }
            i--;
        }

        i = array.length - 1;
        while (i>=0) {
            if (array[i] > array[leftEl]) {
                rightEl = i;
                break;
            }
            i--;
        }
        swapElements(array, leftEl, rightEl);
        if (leftEl + 2 <  array.length){
            Arrays.sort(array, leftEl + 1 , array.length);
        }
    }

    public static void swapElements(int [] array, int leftEl, int rightEl){
        int tmp = array[leftEl];
        array[leftEl] = array[rightEl];
        array[rightEl] = tmp;
    }

    public static void checkModifiersOnRelease(MouseEvent e){
        System.out.println("CheckModifiersOnRelease. currentButtonIndexUnderAction ="+currentButtonIndexUnderAction);
        for (int i = currentButtonIndexUnderAction+1; i<affectedButtonsToPressRelease.length; i++){
            if ((e.getModifiersEx() & affectedButtonsToPressRelease[i]) == 0){
                System.out.println("ERROR["+i+"]: RELEASED_EVENT is not filled with correct values. affectedButtonsToPressRelease[i]= "+affectedButtonsToPressRelease[i] +" Event = "+e);
                ModifierPermutation.failed = true;
            } else {
                System.out.println("CORRECT["+i+"]: affectedButtonsToPressRelease[i]= "+affectedButtonsToPressRelease[i]+ " Event = "+e);
            }
        }
    }

    public static void checkModifiersOnClick(MouseEvent e){
        System.out.println("CheckModifiersOnClick. currentButtonIndexUnderAction ="+currentButtonIndexUnderAction);
        for (int i = currentButtonIndexUnderAction+1; i<affectedButtonsToPressRelease.length; i++){
            if ((e.getModifiersEx() & affectedButtonsToPressRelease[i]) == 0){
                System.out.println("ERROR["+i+"]: CLICK_EVENT is not filled with correct values. affectedButtonsToPressRelease[i]= "+affectedButtonsToPressRelease[i] +" Event = "+e);
                ModifierPermutation.failed = true;
            } else {
                System.out.println("CORRECT["+i+"]: affectedButtonsToPressRelease[i]= "+affectedButtonsToPressRelease[i]+ " Event = "+e);
            }
        }
    }
}

/* A class that invoke appropriate verification
 * routine with current modifier.
 */
class CheckingAdapter extends MouseAdapter{
    public CheckingAdapter(){}

    public void mousePressed(MouseEvent e) {
        System.out.println("PRESSED "+e);
        ModifierPermutation.checkModifiersOnPress(e);
    }
    public void mouseReleased(MouseEvent e) {
        System.out.println("RELEASED "+e);
        ModifierPermutation.checkModifiersOnRelease(e);

    }
    public void mouseClicked(MouseEvent e) {
        System.out.println("CLICKED "+e);
        ModifierPermutation.checkModifiersOnClick(e);
    }
}

/*
class PermutationGenerator{
    final static int BUTTONSNUMBER = MouseInfo.getNumberOfButtons();
    static int [] buttonIndicesToPermutate = new int [BUTTONSNUMBER];;
    public PermutationGenerator(){
        for (int i = 0; i < BUTTONSNUMBER; i++){
            buttonIndicesToPermutate[i] = i+1; 
        }
    }

    public static void nextPermutation(int step, int []array){
        if (array.length != buttonIndicesToPermutate.length) {
            throw new IllegalArgumentException("Array should have length equals to mouse buttons number.");
        }
        int i;
        int leftEl = 0;
        int rightEl = 0;

        i = array.length - 2;
        while (i>=0) {
            if (buttonIndicesToPermutate[i] < buttonIndicesToPermutate[i+1]){
                leftEl = i;
                break;
            }
            i--;
        }

        i = array.length - 1;
        while (i>=0) {
            if (buttonIndicesToPermutate[i] >buttonIndicesToPermutate[leftEl]) {
                rightEl = i;
                break;
            }
            i--;
        }
        swapElements(array, leftEl, rightEl);
        swapElements(buttonIndicesToPermutate, leftEl, rightEl);

        if (leftEl + 2 <  array.length){
            Arrays.sort(array, leftEl + 1 , array.length);
            Arrays.sort(buttonIndicesToPermutate, leftEl + 1 , buttonIndicesToPermutate.length);
        }
    }
    public static void swapElements(int [] array, int leftEl, int rightEl){
        int tmp = array[leftEl];
        array[leftEl] = array[rightEl];
        array[rightEl] = tmp;
    }
}
*/
