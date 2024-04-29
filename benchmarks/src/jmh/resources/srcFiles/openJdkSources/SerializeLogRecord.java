/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @test
 * @bug 8072645
 * @summary tests the compatibility of LogRecord serial form between
 *          JDK 8 and JDK 9. Ideally this test should be run on both platforms.
 *          (It is designed to run on both).
 * @run main/othervm SerializeLogRecord
 * @author danielfuchs
 */
public class SerializeLogRecord {

    /**
     * Serializes a log record, encode the serialized bytes in base 64, and
     * prints pseudo java code that can be cut and pasted into this test.
     * @param record the log record to serialize, encode in base 64, and for
     *               which test data will be generated.
     * @return A string containing the generated pseudo java code.
     * @throws IOException Unexpected.
     * @throws ClassNotFoundException  Unexpected.
     */
    public static String generate(LogRecord record) throws IOException, ClassNotFoundException {

        SimpleFormatter formatter = new SimpleFormatter();
        String str = formatter.format(record);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(record);
        oos.flush();
        oos.close();

        final ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bais);
        final LogRecord record2 = (LogRecord)ois.readObject();

        String str2 = formatter.format(record2);
        if (!str.equals(str2)) throw new RuntimeException("Unexpected values in deserialized object:"
                + "\n\tExpected:  " + str
                + "\n\tRetrieved: "+str);

        final String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());

        final ByteArrayInputStream bais2 = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
        final ObjectInputStream ois2 = new ObjectInputStream(bais2);
        final LogRecord record3 = (LogRecord)ois2.readObject();

        String str3 = formatter.format(record3);
        if (!str.equals(str3)) throw new RuntimeException("Unexpected values in deserialized object:"
                + "\n\tExpected:  " + str
                + "\n\tRetrieved: "+str);

        final StringBuilder sb = new StringBuilder();
        sb.append("    /**").append('\n');
        sb.append("     * Base64 encoded string for LogRecord object.").append('\n');
        sb.append("     * Java version: ").append(System.getProperty("java.version")).append('\n');
        sb.append("     **/").append('\n');
        sb.append("    final String base64 = ").append("\n          ");
        final int last = base64.length() - 1;
        for (int i=0; i<base64.length();i++) {
            if (i%64 == 0) sb.append("\"");
            sb.append(base64.charAt(i));
            if (i%64 == 63 || i == last) {
                sb.append("\"");
                if (i == last) sb.append(";\n");
                else sb.append("\n        + ");
            }
        }
        sb.append('\n');
        sb.append("    /**").append('\n');
        sb.append("     * SimpleFormatter output for LogRecord object.").append('\n');
        sb.append("     * Java version: ").append(System.getProperty("java.version")).append('\n');
        sb.append("     **/").append('\n');
        sb.append("    final String str = ").append("\n          ");
        sb.append("\"").append(str.replace("\n", "\\n")).append("\";\n");
        return sb.toString();
    }

    /**
     * An abstract class to test that a log record previously serialized on a
     * different java version can be deserialized in the current java version.
     * (see Jdk8SerializedLog and Jdk9SerializedLog below)
     */
    public abstract static class SerializedLog {
        public abstract String getBase64();
        public abstract String getString();

        /**
         * Deserializes the Base64 encoded string returned by {@link
         * #getBase64()}, format the obtained LogRecord using a
         * SimpleFormatter, and checks that the string representation obtained
         * matches the original string representation returned by {@link
         * #getString()}.
         */
        protected void dotest() {
            try {
                final String base64 = getBase64();
                final ByteArrayInputStream bais =
                        new ByteArrayInputStream(Base64.getDecoder().decode(base64));
                final ObjectInputStream ois = new ObjectInputStream(bais);
                final LogRecord record = (LogRecord)ois.readObject();
                final SimpleFormatter formatter = new SimpleFormatter();
                String expected = getString();
                String str2 = formatter.format(record);
                check(expected, str2);
                System.out.println(str2);
                System.out.println("PASSED: "+this.getClass().getName()+"\n");
            } catch (IOException | ClassNotFoundException x) {
                throw new RuntimeException(x);
            }
        }
        /**
         * Check that the actual String representation obtained matches the
         * expected String representation.
         * @param expected Expected String representation, as returned by
         *                 {@link #getString()}.
         * @param actual   Actual String representation obtained by formatting
         *                 the LogRecord obtained by the deserialization of the
         *                 bytes encoded in {@link #getBase64()}.
         */
        protected void check(String expected, String actual) {
            if (!expected.equals(actual)) {
                throw new RuntimeException(this.getClass().getName()
                    + " - Unexpected values in deserialized object:"
                    + "\n\tExpected:  " + expected
                    + "\n\tRetrieved: "+ actual);
            }
        }
    }

    public static class Jdk8SerializedLog extends SerializedLog {


        /**
         * Base64 encoded string for LogRecord object.
         * Java version: 1.8.0_11
         **/
        final String base64 =
              "rO0ABXNyABtqYXZhLnV0aWwubG9nZ2luZy5Mb2dSZWNvcmRKjVk982lRlgMACkoA"
            + "Bm1pbGxpc0oADnNlcXVlbmNlTnVtYmVySQAIdGhyZWFkSURMAAVsZXZlbHQAGUxq"
            + "YXZhL3V0aWwvbG9nZ2luZy9MZXZlbDtMAApsb2dnZXJOYW1ldAASTGphdmEvbGFu"
            + "Zy9TdHJpbmc7TAAHbWVzc2FnZXEAfgACTAAScmVzb3VyY2VCdW5kbGVOYW1lcQB+"
            + "AAJMAA9zb3VyY2VDbGFzc05hbWVxAH4AAkwAEHNvdXJjZU1ldGhvZE5hbWVxAH4A"
            + "AkwABnRocm93bnQAFUxqYXZhL2xhbmcvVGhyb3dhYmxlO3hwAAABSjUCgo0AAAAA"
            + "AAAAAAAAAAFzcgAXamF2YS51dGlsLmxvZ2dpbmcuTGV2ZWyOiHETUXM2kgIAA0kA"
            + "BXZhbHVlTAAEbmFtZXEAfgACTAAScmVzb3VyY2VCdW5kbGVOYW1lcQB+AAJ4cAAA"
            + "AyB0AARJTkZPdAAic3VuLnV0aWwubG9nZ2luZy5yZXNvdXJjZXMubG9nZ2luZ3QA"
            + "BHRlc3R0ABFKYXZhIFZlcnNpb246IHswfXBwcHB3BgEAAAAAAXQACDEuOC4wXzEx"
            + "eA==";

        /**
         * SimpleFormatter output for LogRecord object.
         * Java version: 1.8.0_11
         **/
        final String str =
              "Dec 10, 2014 4:22:44.621000000 PM test - INFO: Java Version: 1.8.0_11";


        @Override
        public String getBase64() {
            return base64;
        }

        @Override
        public String getString() {
            return str;
        }

        public static void test() {
            new Jdk8SerializedLog().dotest();
        }
    }

    public static class Jdk9SerializedLog extends SerializedLog {


        /**
         * Base64 encoded string for LogRecord object.
         * Java version: 1.9.0-internal
         **/
        final String base64 =
              "rO0ABXNyABtqYXZhLnV0aWwubG9nZ2luZy5Mb2dSZWNvcmRKjVk982lRlgMAC0oA"
            + "Bm1pbGxpc0kADm5hbm9BZGp1c3RtZW50SgAOc2VxdWVuY2VOdW1iZXJJAAh0aHJl"
            + "YWRJREwABWxldmVsdAAZTGphdmEvdXRpbC9sb2dnaW5nL0xldmVsO0wACmxvZ2dl"
            + "ck5hbWV0ABJMamF2YS9sYW5nL1N0cmluZztMAAdtZXNzYWdlcQB+AAJMABJyZXNv"
            + "dXJjZUJ1bmRsZU5hbWVxAH4AAkwAD3NvdXJjZUNsYXNzTmFtZXEAfgACTAAQc291"
            + "cmNlTWV0aG9kTmFtZXEAfgACTAAGdGhyb3dudAAVTGphdmEvbGFuZy9UaHJvd2Fi"
            + "bGU7eHAAAAFLl3u6OAAOU/gAAAAAAAAAAAAAAAFzcgAXamF2YS51dGlsLmxvZ2dp"
            + "bmcuTGV2ZWyOiHETUXM2kgIAA0kABXZhbHVlTAAEbmFtZXEAfgACTAAScmVzb3Vy"
            + "Y2VCdW5kbGVOYW1lcQB+AAJ4cAAAAyB0AARJTkZPdAAic3VuLnV0aWwubG9nZ2lu"
            + "Zy5yZXNvdXJjZXMubG9nZ2luZ3QABHRlc3R0ABFKYXZhIFZlcnNpb246IHswfXBw"
            + "cHB3BgEAAAAAAXQADjEuOS4wLWludGVybmFseA==";

        /**
         * SimpleFormatter output for LogRecord object.
         * Java version: 1.9.0-internal
         **/
        final String str =
              "Feb 17, 2015 12:20:43.192939000 PM test - INFO: Java Version: 1.9.0-internal";


        @Override
        public String getBase64() {
            return base64;
        }

        @Override
        public String getString() {
            return str;
        }

        @Override
        protected void check(String expected, String actual) {
            if (System.getProperty("java.version").startsWith("1.8")) {
                Pattern pattern = Pattern.compile("^"
                        + "(.*\\.[0-9][0-9][0-9])" 
                        + "([0-9][0-9][0-9][0-9][0-9][0-9])" 
                        + "(.* - .*)$"); 
                Matcher matcher = pattern.matcher(expected);
                if (matcher.matches()) {
                    expected = matcher.group(1) + "000000" + matcher.group(3);
                }
            }
            super.check(expected, actual);
        }

        public static void test() {
            new Jdk9SerializedLog().dotest();
        }
    }

    public static void generate() {
        try {
            LogRecord record = new LogRecord(Level.INFO, "Java Version: {0}");
            record.setLoggerName("test");
            record.setParameters(new Object[] {System.getProperty("java.version")});
            System.out.println(generate(record));
        } catch (IOException | ClassNotFoundException x) {
            throw new RuntimeException(x);
        }
    }

    static enum TestCase { GENERATE, TESTJDK8, TESTJDK9 };

    public static void main(String[] args) {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")));

        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS.%1$tN %1$Tp %2$s - %4$s: %5$s%6$s");

        if (args == null || args.length == 0) {
            args = new String[] { "GENERATE", "TESTJDK8", "TESTJDK9" };
        }

        Stream.of(args).map(x -> TestCase.valueOf(x)).forEach((x) -> {
            switch(x) {
                case GENERATE: generate(); break;
                case TESTJDK8: Jdk8SerializedLog.test(); break;
                case TESTJDK9: Jdk9SerializedLog.test(); break;
            }
        });
    }
}
