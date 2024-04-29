/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8249719
 * @summary ResolvedMethodTable hash function should take method class into account
 * @run main/othervm/manual -Xmx256m -XX:MaxMetaspaceSize=256m ResolvedMethodTableHash 200000
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ResolvedMethodTableHash extends ClassLoader {

    private MethodHandle generate(String className) throws ReflectiveOperationException {
        byte[] buf = new byte[100];
        int size = writeClass(buf, className);
        Class<?> cls = defineClass(null, buf, 0, size);
        return MethodHandles.publicLookup().findStatic(cls, "m", MethodType.methodType(void.class));
    }

    private MethodHandle generateWithSameName() throws ReflectiveOperationException {
        byte[] buf = new byte[100];
        int size = writeClass(buf, "MH$$");
        Class<?> cls = new ResolvedMethodTableHash().defineClass(null, buf, 0, size);
        return MethodHandles.publicLookup().findStatic(cls, "m", MethodType.methodType(void.class));
    }

    private int writeClass(byte[] buf, String className) {
        return ByteBuffer.wrap(buf)
                .putInt(0xCAFEBABE)       
                .putInt(50)               
                .putShort((short) 7)      
                .put((byte) 7).putShort((short) 2)
                .put((byte) 1).putShort((short) className.length()).put(className.getBytes())
                .put((byte) 7).putShort((short) 4)
                .put((byte) 1).putShort((short) 16).put("java/lang/Object".getBytes())
                .put((byte) 1).putShort((short) 1).put("m".getBytes())
                .put((byte) 1).putShort((short) 3).put("()V".getBytes())
                .putShort((short) 0x21)   
                .putShort((short) 1)      
                .putShort((short) 3)      
                .putShort((short) 0)      
                .putShort((short) 0)      
                .putShort((short) 1)      
                .putShort((short) 0x109)  
                .putShort((short) 5)      
                .putShort((short) 6)      
                .putShort((short) 0)      
                .putShort((short) 0)      
                .position();
    }

    public static void main(String[] args) throws Exception {
        ResolvedMethodTableHash generator = new ResolvedMethodTableHash();
        List<MethodHandle> handles = new ArrayList<>();

        int count = args.length > 0 ? Integer.parseInt(args[0]) : 200000;

        for (int i = 0; i < count; i++) {
            if (i % 20 != 0) {
                handles.add(generator.generate("MH$" + i));
            } else {
                handles.add(generator.generateWithSameName());
            }
            if (i % 1000 == 0) {
                System.out.println("Generated " + i + " handles");
            }
        }

        System.out.println("Test passed");
    }
}
