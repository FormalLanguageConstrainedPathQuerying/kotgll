/*
 * @test /nodynamiccopyright/
 * @bug 8004832
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:all,-missing -ref RepeatedAttr.out RepeatedAttr.java
 */


/**
 * <img src="image.gif" alt alt="summary">
 */
public class RepeatedAttr { }
