/*
 * @test /nodynamiccopyright/
 * @bug 8004832
 * @summary Add new doclint package
 * @library ..
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 * @build DocLintTester
 * @run main DocLintTester -Xmsgs:all,-missing -ref InvalidEntity.out InvalidEntity.java
 */



/**
 * &#01;
 * &#x01;
 * &splodge;
 *
 */
public class InvalidEntity { }
