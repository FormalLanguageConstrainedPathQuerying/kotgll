/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4459133
 * @summary Forward reference legal or illegal?
 * @author gafter
 *
 * @compile UseBeforeDeclaration.java
 */

class UseBeforeDeclaration {
    static {
        x = 100; 
        int v = ((x)) = 3; 
        int z = UseBeforeDeclaration.x * 2; 
        Object o = new Object(){
                void foo(){x++;} 
                {x++;} 
            };
    }
    {
        j = 200; 
        int n = j = 300; 
        int l = this.j * 3; 
        Object o = new Object(){
                void foo(){j++;} 
                { j = j + 1;} 
            };
    }
    int w = x= 3; 
    int p = x; 
    static int u = (new Object(){int bar(){return x;}}).bar(); 
    static int x;
    int m = j = 4; 
    int o = (new Object(){int bar(){return j;}}).bar(); 
    int j;
}
