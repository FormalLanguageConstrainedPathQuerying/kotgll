/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

/**
 * @test
 * @bug 8306461
 * @summary ObjectInputStream::readObject() should handle negative array sizes without throwing NegativeArraySizeExceptions
 * @run main/othervm NegativeArraySizeTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputFilter.Status;
import java.util.PriorityQueue;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;

public class NegativeArraySizeTest {

    private static byte[] buildArrayPayload() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(new String[1]);
        oos.close();
        byte[] serializedData = baos.toByteArray();

        int firstPos = 0;
        for (int i = 0; i < serializedData.length - 1; i++) {
            if (serializedData[i] == 0x78) {
                serializedData[i + 2] = (byte) 0xff;
                serializedData[i + 3] = (byte) 0xff;
                serializedData[i + 4] = (byte) 0xff;
                serializedData[i + 5] = (byte) 0xfe;

                return serializedData;
            }
        }
        throw new RuntimeException("Can't find TC_ENDBLOCKDATA in object output stream");
    }

    private static byte[] buildPriorityQueuePayload() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(new PriorityQueue<>());
        oos.close();
        byte[] serializedData = baos.toByteArray();

        int firstPos = 0;
        for (int i = 0; i < serializedData.length - 1; i++) {
            if (serializedData[i] == 0x77) {
                serializedData[i - 5] = (byte) 0xff;
                serializedData[i - 4] = (byte) 0xff;
                serializedData[i - 3] = (byte) 0xff;
                serializedData[i - 2] = (byte) 0xfd;

                return serializedData;
            }
        }
        throw new RuntimeException("Can't find TC_BLOCKDATA in object output stream");
    }

    private static class CustomFilter implements ObjectInputFilter {
        @Override
        public Status checkInput(FilterInfo filterInfo) {
            Class<?> cl = filterInfo.serialClass();
            if (cl != null && cl.isArray() && filterInfo.arrayLength() < -1) {
                throw new RuntimeException("FilterInfo.arrayLength() must be >= -1 for arrays (was " + filterInfo.arrayLength() + ")");
            }
            return Status.ALLOWED;
        }
    }

    public static void main(String[] args) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(buildArrayPayload());
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            ois.readObject();
        } catch (NegativeArraySizeException nase) {
            throw new Exception("ObjectInputStream::readObject() shouldn't throw a NegativeArraySizeException", nase);
        } catch (ObjectStreamException ose) {
            if (!"Array length is negative".equals(ose.getMessage())) {
                throw new Exception("Expected \"Array length is negative\" as exception message", ose);
            }
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(buildArrayPayload());
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            ois.setObjectInputFilter(new CustomFilter());
            ois.readObject();
        } catch (NegativeArraySizeException nase) {
            throw new Exception("ObjectInputStream::readObject() shouldn't throw a NegativeArraySizeException", nase);
        } catch (ObjectStreamException ose) {
            if (ose instanceof InvalidClassException ice && ice.getMessage().contains("filter status: REJECTED")) {
                throw new Exception("ObjectInputStream::readObject() should catch NegativeArraySizeExceptions before filtering", ice);
            }
            if (!"Array length is negative".equals(ose.getMessage())) {
                throw new Exception("Expected \"Array length is negative\" as exception message", ose);
            }
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(buildPriorityQueuePayload());
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            ois.readObject();
        } catch (NegativeArraySizeException nase) {
            throw new Exception("ObjectInputStream::readObject() shouldn't throw a NegativeArraySizeException", nase);
        } catch (ObjectStreamException ose) {
            if (!"Array length is negative".equals(ose.getMessage())) {
                throw new Exception("Expected \"Array length is negative\" as exception message", ose);
            }
        }
    }
}
