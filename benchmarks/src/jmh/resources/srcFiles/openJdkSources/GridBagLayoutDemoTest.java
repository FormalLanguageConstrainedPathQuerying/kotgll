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
import org.jtregext.GuiTestListener;
import com.sun.swingset3.demos.gridbaglayout.GridBagLayoutDemo;
import static com.sun.swingset3.demos.gridbaglayout.GridBagLayoutDemo.*;
import static com.sun.swingset3.demos.gridbaglayout.Calculator.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import javax.swing.JButton;
import javax.swing.UIManager;
import org.testng.annotations.Test;
import org.netbeans.jemmy.ClassReference;
import org.netbeans.jemmy.operators.JFrameOperator;
import org.netbeans.jemmy.operators.JButtonOperator;
import org.netbeans.jemmy.operators.JTextFieldOperator;
import org.testng.annotations.Listeners;
import static org.jemmy2ext.JemmyExt.EXACT_STRING_COMPARATOR;
import static org.jemmy2ext.JemmyExt.getUIValue;
/*
 * @test
 * @key headful
 * @summary Verifies SwingSet3 GridBagLayoutDemo by checking the relative
 *  location of all the components before and after resizing the frame,
 *  interacting with all the controls and checking this interaction on the
 *  text field display.
 *
 * @library /sanity/client/lib/jemmy/src
 * @library /sanity/client/lib/Extensions/src
 * @library /sanity/client/lib/SwingSet3/src
 * @modules java.desktop
 *          java.logging
 * @build org.jemmy2ext.JemmyExt
 * @build com.sun.swingset3.demos.gridbaglayout.GridBagLayoutDemo
 * @run testng/timeout=600 GridBagLayoutDemoTest
 */

@Listeners(GuiTestListener.class)
public class GridBagLayoutDemoTest {

    private JTextFieldOperator tfScreen;
    private JButtonOperator buttonZero;
    private JButtonOperator buttonOne;
    private JButtonOperator buttonTwo;
    private JButtonOperator buttonThree;
    private JButtonOperator buttonFour;
    private JButtonOperator buttonFive;
    private JButtonOperator buttonSix;
    private JButtonOperator buttonSeven;
    private JButtonOperator buttonEight;
    private JButtonOperator buttonNine;
    private JButtonOperator buttonPlus;
    private JButtonOperator buttonMinus;
    private JButtonOperator buttonMultiply;
    private JButtonOperator buttonDivide;
    private JButtonOperator buttonComma;
    private JButtonOperator buttonSqrt;
    private JButtonOperator buttonReciprocal;
    private JButtonOperator buttonToggleSign;
    private JButtonOperator buttonEquals;
    private JButtonOperator backspaceButton;
    private JButtonOperator resetButton;
    private JFrameOperator mainFrame;

    @Test(dataProvider = "availableLookAndFeels", dataProviderClass = TestHelpers.class)
    public void test(String lookAndFeel) throws Exception {
        initializeUIComponents(lookAndFeel);
        checkRelativeLocations();
        checkInteractionOnDisplay();
        checkChangeLocation();
        checkChangeSize();
    }

    private double x(Component component) {
        return component.getLocation().getX();
    }

    private double y(Component component) {
        return component.getLocation().getY();
    }

    private void checkRight(JButtonOperator currentButton, JButtonOperator rightButton) {
        currentButton.waitStateOnQueue(button -> x(button) + button.getWidth() < x(rightButton.getSource()));
        currentButton.waitStateOnQueue(button -> y(button) == y(rightButton.getSource()));
        currentButton.waitStateOnQueue(button -> button.getHeight() == rightButton.getHeight());
    }

    private void checkBelow(JButtonOperator currentButton, JButtonOperator buttonBelow) {
        currentButton.waitStateOnQueue(button -> y(button) + button.getHeight() < y(buttonBelow.getSource()));
        currentButton.waitStateOnQueue(button -> x(button) == x(buttonBelow.getSource()));
        currentButton.waitStateOnQueue(button -> button.getWidth() == buttonBelow.getWidth());
    }

    private void checkRelativeLocations() {
        checkRight(buttonSeven, buttonEight);
        checkBelow(buttonSeven, buttonFour);

        checkRight(buttonEight, buttonNine);
        checkBelow(buttonEight, buttonFive);

        checkRight(buttonNine, buttonDivide);
        checkBelow(buttonNine, buttonSix);

        checkRight(buttonDivide, buttonReciprocal);
        checkBelow(buttonDivide, buttonMultiply);

        checkBelow(buttonReciprocal, buttonSqrt);

        checkRight(buttonFour, buttonFive);
        checkBelow(buttonFour, buttonOne);

        checkRight(buttonFive, buttonSix);
        checkBelow(buttonFive, buttonTwo);

        checkRight(buttonSix, buttonMultiply);
        checkBelow(buttonSix, buttonThree);

        checkRight(buttonMultiply, buttonSqrt);
        checkBelow(buttonMultiply, buttonMinus);

        checkRight(buttonOne, buttonTwo);
        checkBelow(buttonOne, buttonZero);

        checkRight(buttonTwo, buttonThree);
        checkBelow(buttonTwo, buttonToggleSign);

        checkRight(buttonThree, buttonMinus);
        checkBelow(buttonThree, buttonComma);

        checkBelow(buttonMinus, buttonPlus);

        checkRight(buttonZero, buttonToggleSign);

        checkRight(buttonToggleSign, buttonComma);

        checkRight(buttonComma, buttonPlus);

        checkRight(buttonPlus, buttonEquals);

        Point parentLocation = getUIValue(backspaceButton, (JButton button) -> button.getParent().getLocation());
        tfScreen.waitStateOnQueue(screen -> y(screen) + screen.getHeight() < parentLocation.getY());
        buttonSeven.waitStateOnQueue(button -> parentLocation.getY() < y(button));
        buttonReciprocal.waitStateOnQueue(button -> parentLocation.getY() < y(button));
        tfScreen.waitStateOnQueue(screen -> x(screen) == parentLocation.getX());
        buttonSeven.waitStateOnQueue(button -> x(button) == parentLocation.getX());

        backspaceButton.waitStateOnQueue(button -> x(button) + button.getWidth() < x(resetButton.getSource()));
        backspaceButton.waitStateOnQueue(button -> button.getHeight() == resetButton.getHeight());
        backspaceButton.waitStateOnQueue(
                button -> parentLocation.getY() + button.getParent().getHeight() < y(buttonSeven.getSource()));

        resetButton.waitStateOnQueue(button -> x(backspaceButton.getSource()) + backspaceButton.getWidth() < x(button));

        resetButton.waitStateOnQueue(button -> backspaceButton.getHeight() == button.getHeight());

        resetButton.waitStateOnQueue(
                button -> parentLocation.getY() + button.getParent().getHeight() < y(buttonDivide.getSource()));

        tfScreen.waitStateOnQueue(screen -> y(screen) + screen.getHeight() < parentLocation.getY());
    }

    private void checkInteractionOnDisplay() {
        buttonOne.push();
        tfScreen.waitText("1");
        buttonPlus.push();
        tfScreen.waitText("1");
        buttonTwo.push();
        tfScreen.waitText("2");
        buttonEquals.push();
        tfScreen.waitText("3");
        resetButton.push();
        tfScreen.waitText("0");

        buttonFour.push();
        tfScreen.waitText("4");
        buttonMinus.push();
        tfScreen.waitText("4");
        buttonThree.push();
        tfScreen.waitText("3");
        buttonEquals.push();
        tfScreen.waitText("1");
        reset();

        buttonFive.push();
        tfScreen.waitText("5");
        buttonMultiply.push();
        tfScreen.waitText("5");
        buttonSix.push();
        tfScreen.waitText("6");
        buttonEquals.push();
        tfScreen.waitText("30");
        reset();

        buttonNine.push();
        buttonNine.push();
        tfScreen.waitText("99");
        buttonDivide.push();
        tfScreen.waitText("99");
        buttonEight.push();
        tfScreen.waitText("8");
        buttonEquals.push();
        tfScreen.waitText("12.375");
        reset();

        buttonSeven.push();
        tfScreen.waitText("7");
        buttonZero.push();
        tfScreen.waitText("70");
        buttonToggleSign.push();
        tfScreen.waitText("-70");
        buttonToggleSign.push();
        tfScreen.waitText("70");
        backspaceButton.push();
        tfScreen.waitText("7");
        reset();

        buttonFour.push();
        buttonNine.push();
        tfScreen.waitText("49");
        buttonSqrt.push();
        tfScreen.waitText("7");
        reset();

        buttonFour.push();
        tfScreen.waitText("4");
        buttonReciprocal.push();
        tfScreen.waitText("0.25");
        reset();

        buttonFour.push();
        buttonComma.push();
        tfScreen.waitText("4,");
    }

    private void reset() {
        resetButton.push();
        tfScreen.waitText("0");
    }

    private void initializeUIComponents(String lookAndFeel) throws Exception {
        UIManager.setLookAndFeel(lookAndFeel);
        new ClassReference(GridBagLayoutDemo.class.getCanonicalName()).startApplication();
        mainFrame = new JFrameOperator(GRID_BAG_LAYOUT_DEMO_TITLE);
        mainFrame.setComparator(EXACT_STRING_COMPARATOR);
        buttonZero = new JButtonOperator(mainFrame, ZERO_BUTTON_TITLE);
        buttonOne = new JButtonOperator(mainFrame, ONE_BUTTON_TITLE);
        buttonTwo = new JButtonOperator(mainFrame, TWO_BUTTON_TITLE);
        buttonThree = new JButtonOperator(mainFrame, THREE_BUTTON_TITLE);
        buttonFour = new JButtonOperator(mainFrame, FOUR_BUTTON_TITLE);
        buttonFive = new JButtonOperator(mainFrame, FIVE_BUTTON_TITLE);
        buttonSix = new JButtonOperator(mainFrame, SIX_BUTTON_TITLE);
        buttonSeven = new JButtonOperator(mainFrame, SEVEN_BUTTON_TITLE);
        buttonEight = new JButtonOperator(mainFrame, EIGHT_BUTTON_TITLE);
        buttonNine = new JButtonOperator(mainFrame, NINE_BUTTON_TITLE);
        buttonPlus = new JButtonOperator(mainFrame, PLUS_BUTTON_TITLE);
        buttonMinus = new JButtonOperator(mainFrame, MINUS_BUTTON_TITLE);
        buttonMultiply = new JButtonOperator(mainFrame, MULTIPLY_BUTTON_TITLE);
        buttonDivide = new JButtonOperator(mainFrame, DIVIDE_BUTTON_TITLE);
        buttonComma = new JButtonOperator(mainFrame, ",");
        buttonSqrt = new JButtonOperator(mainFrame, SQRT_BUTTON_TITLE);
        buttonReciprocal = new JButtonOperator(mainFrame, INVERSE_BUTTON_TITLE);
        buttonToggleSign = new JButtonOperator(mainFrame, SWAPSIGN_BUTTON_TITLE);
        buttonEquals = new JButtonOperator(mainFrame, EQUALS_BUTTON_TITLE);
        resetButton = new JButtonOperator(mainFrame, C_BUTTON_TITLE);
        backspaceButton = new JButtonOperator(mainFrame, BACKSPACE_BUTTON_TITLE);
        tfScreen = new JTextFieldOperator(mainFrame, 0);
    }

    private void checkChangeLocation() {
        Point startingPoint = new Point(100, 100);
        mainFrame.setLocation(startingPoint);
        mainFrame.waitComponentLocation(startingPoint);
        checkRelativeLocations();
    }

    private void checkChangeSize() {
        Dimension newSize = new Dimension((int) mainFrame.getToolkit().getScreenSize().getWidth() / 2,
                (int) mainFrame.getToolkit().getScreenSize().getHeight() / 2);
        mainFrame.setSize(newSize);
        mainFrame.waitComponentSize(newSize);
        checkRelativeLocations();
    }
}
