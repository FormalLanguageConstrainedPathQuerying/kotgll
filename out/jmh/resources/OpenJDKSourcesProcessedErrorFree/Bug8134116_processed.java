
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

/*
 * @test
 * @key headful
 * @bug 8134116
 * @summary JTabbedPane$Page.getBounds throws IndexOutOfBoundsException
 * @run main Bug8134116
 */
public class Bug8134116 {

    private static volatile Exception exception = null;
    private static JFrame frame;

    public static void main(String args[]) throws Exception {

        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            SwingUtilities.invokeAndWait(() -> {
                JPanel panel0 = new JPanel();
                JPanel panel2 = new JPanel();
                BadPane badPane = new BadPane();
                badPane.add("zero", panel0);
                badPane.add("one", null);  
                badPane.add("", panel2);  
                badPane.add("", null); 
                JPanel tabComponent = new JPanel();
                JLabel tabComponentLabel = new JLabel("three");
                tabComponent.add(tabComponentLabel);
                badPane.setTabComponentAt(3, tabComponent);
                frame = new JFrame();
                frame.add(badPane);
                frame.setSize(300, 300);
                frame.setVisible(true);

                try {
                    AccessibleContext ac = badPane.getAccessibleContext();
                    Accessible page0 = ac.getAccessibleChild(0);
                    if (page0 == null) {
                        throw new RuntimeException("getAccessibleChild(0) is null");
                    }
                    Accessible page1 = ac.getAccessibleChild(1);
                    if (page1 == null) {
                        throw new RuntimeException("getAccessibleChild(1) is null");
                    }
                    Accessible page2 = ac.getAccessibleChild(2);
                    Accessible page3 = ac.getAccessibleChild(3);
                    AccessibleContext pac0 = page0.getAccessibleContext();
                    AccessibleContext pac1 = page1.getAccessibleContext();
                    AccessibleContext pac2 = page2.getAccessibleContext();
                    AccessibleContext pac3 = page3.getAccessibleContext();

                    Rectangle r0 = pac0.getAccessibleComponent().getBounds();
                    Rectangle r1  = pac1.getAccessibleComponent().getBounds();
                    if (r1.equals(r0)) {
                        String msg = "Second tab should not have same bounds as first tab";
                        throw new RuntimeException(msg);
                    }

                    AccessibleStateSet accSS0 = pac0.getAccessibleStateSet();
                    if (!accSS0.contains(AccessibleState.SELECTED)) {
                        String msg = "Empty title -> AccessibleState.SELECTED not set";
                        throw new RuntimeException(msg);
                    }
                    badPane.setSelectedIndex(1);
                    AccessibleStateSet accSS1 = pac1.getAccessibleStateSet();
                    if (!accSS1.contains(AccessibleState.SELECTED)) {
                        String msg = "Second tab selected but AccessibleState.SELECTED not set";
                        throw new RuntimeException(msg);
                    }
                    badPane.setSelectedIndex(2);
                    AccessibleStateSet accSS2 = pac2.getAccessibleStateSet();
                    if (!accSS1.contains(AccessibleState.SELECTED)) {
                        String msg = "Third tab selected but AccessibleState.SELECTED not set";
                        throw new RuntimeException(msg);
                    }
                    badPane.setSelectedIndex(3);
                    AccessibleStateSet accSS3 = pac3.getAccessibleStateSet();
                    if (!accSS1.contains(AccessibleState.SELECTED)) {
                        String msg = "Fourth tab selected but AccessibleState.SELECTED not set";
                        throw new RuntimeException(msg);
                    }

                    if (pac0.getAccessibleIndexInParent() == -1) {
                        String msg = "Empty title -> negative AccessibleIndexInParent";
                        throw new RuntimeException(msg);
                    }
                    if (pac0.getAccessibleIndexInParent() != 0) {
                        String msg = "first tab is not at index 0 in parent";
                        throw new RuntimeException(msg);
                    }
                    if (pac1.getAccessibleIndexInParent() != 1) {
                        String msg = "second tab (null component) is not at index 1 in parent";
                        throw new RuntimeException(msg);
                    }
                    if (pac2.getAccessibleIndexInParent() != 2) {
                        String msg = "third tab (empty title) string is not at index 2 in parent";
                        throw new RuntimeException(msg);
                    }
                    if (pac3.getAccessibleIndexInParent() != 3) {
                        String msg = "fourth tab (empty title, null component, has tabComponent) string is not at index 3 in parent";
                        throw new RuntimeException(msg);
                    }

                    String accName = pac0.getAccessibleName();
                    if (!accName.equals("zero")) {
                        String msg = "Empty title -> empty AccessibleName";
                        throw new RuntimeException(msg);
                    }
                    accName = pac1.getAccessibleName();
                    if (!accName.equals("one")) {
                        String msg = "AccessibleName of null panel not 'one'";
                        throw new RuntimeException(msg);
                    }

                    badPane.setDisplayedMnemonicIndexAt(0, 1);

                    badPane.setMnemonicAt(0, KeyEvent.VK_Z);
                    if (badPane.getDisplayedMnemonicIndexAt(0) == -1) {
                        String msg="Empty title -> getDisplayedMnemonicIndexAt failure";
                        throw new RuntimeException(msg);
                    }
                } catch (Exception e) {
                    exception = e;
                }
            });
            if (exception != null) {
                System.out.println("Test failed: " + exception.getMessage());
                throw exception;
            } else {
                System.out.println("Test passed.");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    static class BadPane extends JTabbedPane {
        private List<String> titles;

        BadPane() {
            titles = new ArrayList<String>(1);
        }

        @Override
        public void insertTab( String title, Icon icon, Component component,
                               String tip, int index ) {
            titles.add(index, title);
            super.insertTab("", icon, component, tip, index);
        }

        @Override
        public String getTitleAt(int i) {
            return titles.get(i);
        }
    }

}
