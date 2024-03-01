/*
 * @test /nodynamiccopyright/
 * @bug 8004832
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:all,-missing -ref UnescapedOrUnknownEntity.out UnescapedOrUnknownEntity.java
 */


/**
 * L&F
 * Drag&Drop
 * if (a & b);
 */
public class UnescapedOrUnknownEntity { }
