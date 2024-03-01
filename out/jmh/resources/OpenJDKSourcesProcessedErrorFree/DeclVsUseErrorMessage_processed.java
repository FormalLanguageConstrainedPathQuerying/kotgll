/*
 * @test /nodynamiccopyright/
 * @bug 8073534
 * @summary Check for correct type annotation error messages.
 * @compile/fail/ref=DeclVsUseErrorMessage.out -XDrawDiagnostics DeclVsUseErrorMessage.java
 */

import java.lang.annotation.*;
import java.util.ArrayList;

class DeclVsUseErrorMessage {

    @Target(ElementType.METHOD)
    @interface A {}

    @A int i;

    {
        new ArrayList<@A String>();
    }
}
