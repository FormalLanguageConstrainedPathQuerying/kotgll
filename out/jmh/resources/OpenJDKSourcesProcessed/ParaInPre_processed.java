/*
 * @test /nodynamiccopyright/
 * @bug 8004832
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:all,-missing -ref ParaInPre.out ParaInPre.java
 */


/**
 * <pre>
 *     text
 *     <p>
 *     more text
 * </pre>
 */
public class ParaInPre { }
