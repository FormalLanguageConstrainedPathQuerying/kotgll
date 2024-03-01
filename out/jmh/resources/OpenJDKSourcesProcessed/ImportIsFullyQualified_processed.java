/*
 * @test /nodynamiccopyright/
 * @bug 4335264
 * @summary Verify that import-on-demand must be fully qualified.
 * @author maddox
 *
 * @compile/fail/ref=ImportIsFullyQualified.out -XDrawDiagnostics  ImportIsFullyQualified.java
 */

import java.awt.*;
import JobAttributes.*;  

public class ImportIsFullyQualified {
    JobAttributes.DefaultSelectionType x;
}
