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

/*
 * Copyright (C) 2013-2014 IBM Corporation and Others. All Rights Reserved.
 */

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This test runs against a test XML file. It opens the fonts and attempts
 * to shape and layout glyphs.
 * Note that the test is highly environment dependent- you must have
 * the same versions of the same fonts available or the test will fail.
 *
 * It is similar to letest which is part of ICU.
 * For reference, here are some reference items:
 * ICU's test file:
 *  http:
 * ICU's readme for the similar test:
 *  http:
 *
 * @bug 8054203
 * @test
 * @summary manual test of layout engine behavior. Takes an XML control file.
 * @compile TestLayoutVsICU.java
 * @author srl
 * @run main/manual
 */
public class TestLayoutVsICU {

    public static boolean OPT_DRAW = false;
    public static boolean OPT_VERBOSE = false;
    public static boolean OPT_FAILMISSING = false;
    public static boolean OPT_NOTHROW= false; 

    public static int docs = 0; 
    public static int skipped = 0; 
    public static int total = 0; 
    public static int bad = 0; 

    public static final String XML_LAYOUT_TESTS = "layout-tests"; 
    public static final String XML_TEST_CASE = "test-case";
    public static final String XML_TEST_FONT = "test-font";
    public static final String XML_TEST_TEXT = "test-text";
    public static final String XML_RESULT_GLYPHS = "result-glyphs";
    public static final String XML_ID = "id";
    public static final String XML_SCRIPT = "script";
    public static final String XML_NAME = "name";
    public static final String XML_VERSION = "version";
    public static final String XML_CHECKSUM = "checksum";
    public static final String XML_RESULT_INDICES = "result-indices";
    public static final String XML_RESULT_POSITIONS = "result-positions";

    /**
     * @param args
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        System.out.println("Java " + System.getProperty("java.version") + " from " + System.getProperty("java.vendor"));
        TestLayoutVsICU tlvi = null;
        for(String arg : args) {
            if(arg.equals("-d")) {
                OPT_DRAW = true;
            } else if(arg.equals("-n")) {
                OPT_NOTHROW = true;
            } else if(arg.equals("-v")) {
                OPT_VERBOSE = true;
            } else if(arg.equals("-f")) {
                OPT_FAILMISSING = true;
            } else {
                if(tlvi == null) {
                    tlvi = new TestLayoutVsICU();
                }
                try {
                    tlvi.show(arg);
                } finally {
                    if(OPT_VERBOSE) {
                        System.out.println("# done with " + arg);
                    }
                }
            }
        }

        if(tlvi == null) {
            throw new IllegalArgumentException("No XML input. Usage: " + TestLayoutVsICU.class.getSimpleName() + " [-d][-v][-f] letest.xml ...");
        } else {
            System.out.println("\n\nRESULTS:\n");
            System.out.println(skipped+"\tskipped due to missing font");
            System.out.println(total+"\ttested of which:");
            System.out.println(bad+"\twere bad");

            if(bad>0) {
                throw new InternalError("One or more failure(s)");
            }
        }
    }

    String id;

    private void show(String arg) throws ParserConfigurationException, SAXException, IOException {
        id = "<none>";
        File xmlFile = new File(arg);
        if(!xmlFile.exists()) {
            throw new FileNotFoundException("Can't open input XML file " + arg);
        }
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        if(OPT_VERBOSE) {
            System.out.println("# Parsing " + xmlFile.getAbsolutePath());
        }
        Document doc = db.parse(xmlFile);
        Element e = doc.getDocumentElement();
        if(!XML_LAYOUT_TESTS.equals(e.getNodeName())) {
            throw new IllegalArgumentException("Document " + xmlFile.getAbsolutePath() + " does not have <layout-tests> as its base");
        }

        NodeList testCases = e.getElementsByTagName(XML_TEST_CASE);
        for(int caseNo=0;caseNo<testCases.getLength();caseNo++) {
            final Node testCase = testCases.item(caseNo);
            final Map<String,String> testCaseAttrs = attrs(testCase);
            id = testCaseAttrs.get(XML_ID);
            final String script = testCaseAttrs.get(XML_SCRIPT);
            String testText = null;
            Integer[] expectGlyphs = null;
            Integer[] expectIndices = null;
            Map<String,String> fontAttrs = null;
            if(OPT_VERBOSE) {
                System.out.println("#"+caseNo+" id="+id + ", script="+script);
            }
            NodeList children = testCase.getChildNodes();
            for(int sub=0;sub<children.getLength();sub++) {
                Node n = children.item(sub);
                if(n.getNodeType()!=Node.ELEMENT_NODE) continue;
                String nn = n.getNodeName();
                if(nn.equals(XML_TEST_FONT)) {
                    fontAttrs = attrs(n);
                } else if(nn.equals(XML_TEST_TEXT)) {
                    testText = n.getTextContent();
                } else if(nn.equals(XML_RESULT_GLYPHS)) {
                    String hex = n.getTextContent();
                    expectGlyphs = parseHexArray(hex);
                } else if(nn.equals(XML_RESULT_INDICES)) {
                    String hex = n.getTextContent();
                    expectIndices = parseHexArray(hex);
                } else if(OPT_VERBOSE) {
                    System.out.println("Ignoring node " + nn);
                }
            }
            if(fontAttrs == null) {
                throw new IllegalArgumentException(id + " Missing node " + XML_TEST_FONT);
            }
            if(testText == null) {
                throw new IllegalArgumentException(id + " Missing node " + XML_TEST_TEXT);
            }

            String fontName = fontAttrs.get(XML_NAME);
            Font f = getFont(fontName, fontAttrs);
            if(f==null) {
                if(OPT_FAILMISSING) {
                    throw new MissingResourceException("Missing font,  abort test", Font.class.getName(), fontName);
                }
                System.out.println("Skipping " + id + " because font is missing: " + fontName);
                skipped++;
                continue;
            }
            FontRenderContext frc = new FontRenderContext(null, true, true);
            TextLayout tl = new TextLayout(testText,f,frc);
            final List<GlyphVector> glyphs = new ArrayList<GlyphVector>();
            Graphics2D myg2 = new Graphics2D(){

                    @Override
                    public void draw(Shape s) {

                    }

                    @Override
                    public boolean drawImage(Image img, AffineTransform xform,
                                             ImageObserver obs) {
                        return false;
                    }

                    @Override
                    public void drawImage(BufferedImage img,
                                          BufferedImageOp op, int x, int y) {

                    }

                    @Override
                    public void drawRenderedImage(RenderedImage img,
                                                  AffineTransform xform) {

                    }

                    @Override
                    public void drawRenderableImage(RenderableImage img,
                                                    AffineTransform xform) {

                    }

                    @Override
                    public void drawString(String str, int x, int y) {

                    }

                    @Override
                    public void drawString(String str, float x, float y) {

                    }

                    @Override
                    public void drawString(
                                           AttributedCharacterIterator iterator, int x, int y) {

                    }

                    @Override
                    public void drawString(
                                           AttributedCharacterIterator iterator, float x,
                                           float y) {

                    }

                    @Override
                    public void drawGlyphVector(GlyphVector g, float x, float y) {
                        if(x!=0.0 || y!=0.0) {
                            throw new InternalError("x,y should be 0 but got " + x+","+y);
                        }
                        glyphs.add(g);
                    }

                    @Override
                    public void fill(Shape s) {

                    }

                    @Override
                    public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
                        return false;
                    }

                    @Override
                    public GraphicsConfiguration getDeviceConfiguration() {
                        return null;
                    }

                    @Override
                    public void setComposite(Composite comp) {

                    }

                    @Override
                    public void setPaint(Paint paint) {

                    }

                    @Override
                    public void setStroke(Stroke s) {

                    }

                    @Override
                    public void setRenderingHint(Key hintKey, Object hintValue) {

                    }

                    @Override
                    public Object getRenderingHint(Key hintKey) {
                        return null;
                    }

                    @Override
                    public void setRenderingHints(Map<?, ?> hints) {

                    }

                    @Override
                    public void addRenderingHints(Map<?, ?> hints) {

                    }

                    @Override
                    public RenderingHints getRenderingHints() {
                        return null;
                    }

                    @Override
                    public void translate(int x, int y) {

                    }

                    @Override
                    public void translate(double tx, double ty) {

                    }

                    @Override
                    public void rotate(double theta) {

                    }

                    @Override
                    public void rotate(double theta, double x, double y) {

                    }

                    @Override
                    public void scale(double sx, double sy) {

                    }

                    @Override
                    public void shear(double shx, double shy) {

                    }

                    @Override
                    public void transform(AffineTransform Tx) {

                    }

                    @Override
                    public void setTransform(AffineTransform Tx) {

                    }

                    @Override
                    public AffineTransform getTransform() {
                        return null;
                    }

                    @Override
                    public Paint getPaint() {
                        return null;
                    }

                    @Override
                    public Composite getComposite() {
                        return null;
                    }

                    @Override
                    public void setBackground(Color color) {

                    }

                    @Override
                    public Color getBackground() {
                        return null;
                    }

                    @Override
                    public Stroke getStroke() {
                        return null;
                    }

                    @Override
                    public void clip(Shape s) {

                    }

                    @Override
                    public FontRenderContext getFontRenderContext() {
                        return null;
                    }

                    @Override
                    public Graphics create() {
                        return null;
                    }

                    @Override
                    public Color getColor() {
                        return null;
                    }

                    @Override
                    public void setColor(Color c) {

                    }

                    @Override
                    public void setPaintMode() {

                    }

                    @Override
                    public void setXORMode(Color c1) {

                    }

                    @Override
                    public Font getFont() {
                        return null;
                    }

                    @Override
                    public void setFont(Font font) {

                    }

                    @Override
                    public FontMetrics getFontMetrics(Font f) {
                        return null;
                    }

                    @Override
                    public Rectangle getClipBounds() {
                        return null;
                    }

                    @Override
                    public void clipRect(int x, int y, int width, int height) {

                    }

                    @Override
                    public void setClip(int x, int y, int width, int height) {

                    }

                    @Override
                    public Shape getClip() {
                        return null;
                    }

                    @Override
                    public void setClip(Shape clip) {

                    }

                    @Override
                    public void copyArea(int x, int y, int width, int height,
                                         int dx, int dy) {

                    }

                    @Override
                    public void drawLine(int x1, int y1, int x2, int y2) {

                    }

                    @Override
                    public void fillRect(int x, int y, int width, int height) {

                    }

                    @Override
                    public void clearRect(int x, int y, int width, int height) {

                    }

                    @Override
                    public void drawRoundRect(int x, int y, int width,
                                              int height, int arcWidth, int arcHeight) {

                    }

                    @Override
                    public void fillRoundRect(int x, int y, int width,
                                              int height, int arcWidth, int arcHeight) {

                    }

                    @Override
                    public void drawOval(int x, int y, int width, int height) {

                    }

                    @Override
                    public void fillOval(int x, int y, int width, int height) {

                    }

                    @Override
                    public void drawArc(int x, int y, int width, int height,
                                        int startAngle, int arcAngle) {

                    }

                    @Override
                    public void fillArc(int x, int y, int width, int height,
                                        int startAngle, int arcAngle) {

                    }

                    @Override
                    public void drawPolyline(int[] xPoints, int[] yPoints,
                                             int nPoints) {

                    }

                    @Override
                    public void drawPolygon(int[] xPoints, int[] yPoints,
                                            int nPoints) {

                    }

                    @Override
                    public void fillPolygon(int[] xPoints, int[] yPoints,
                                            int nPoints) {

                    }

                    @Override
                    public boolean drawImage(Image img, int x, int y,
                                             ImageObserver observer) {
                        return false;
                    }

                    @Override
                    public boolean drawImage(Image img, int x, int y,
                                             int width, int height, ImageObserver observer) {
                        return false;
                    }

                    @Override
                    public boolean drawImage(Image img, int x, int y,
                                             Color bgcolor, ImageObserver observer) {
                        return false;
                    }

                    @Override
                    public boolean drawImage(Image img, int x, int y,
                                             int width, int height, Color bgcolor,
                                             ImageObserver observer) {
                        return false;
                    }

                    @Override
                    public boolean drawImage(Image img, int dx1, int dy1,
                                             int dx2, int dy2, int sx1, int sy1, int sx2,
                                             int sy2, ImageObserver observer) {
                        return false;
                    }

                    @Override
                    public boolean drawImage(Image img, int dx1, int dy1,
                                             int dx2, int dy2, int sx1, int sy1, int sx2,
                                             int sy2, Color bgcolor, ImageObserver observer) {
                        return false;
                    }

                    @Override
                    public void dispose() {

                    }

                };
            tl.draw(myg2, 0, 0);
            if(glyphs.size() != 1) {
                err("drew " + glyphs.size() + " times - expected 1");
                total++;
                bad++;
                continue;
            }
            boolean isBad = false;
            GlyphVector gv = glyphs.get(0);

            int gotGlyphs[] = gv.getGlyphCodes(0, gv.getNumGlyphs(), new int[gv.getNumGlyphs()]);

            int count = Math.min(gotGlyphs.length, expectGlyphs.length); 

            for(int i=0;i<count;i++) {
                if(gotGlyphs[i]!=expectGlyphs[i]) {
                    err("@"+i+" - got \tglyph 0x" + Integer.toHexString(gotGlyphs[i]) + " wanted 0x" + Integer.toHexString(expectGlyphs[i]));
                    isBad=true;
                    break;
                }
            }

            int gotIndices[] = gv.getGlyphCharIndices(0, gv.getNumGlyphs(), new int[gv.getNumGlyphs()]);
            for(int i=0;i<count;i++) {
                if(gotIndices[i]!=expectIndices[i]) {
                    err("@"+i+" - got \tindex 0x" + Integer.toHexString(gotGlyphs[i]) + " wanted 0x" + Integer.toHexString(expectGlyphs[i]));
                    isBad=true;
                    break;
                }
            }


            if(gotGlyphs.length != expectGlyphs.length) {
                System.out.println("Got " + gotGlyphs.length + " wanted " + expectGlyphs.length + " glyphs");
                isBad=true;
            } else {
                if(OPT_VERBOSE) {
                    System.out.println(">> OK: " + gotGlyphs.length + " glyphs");
                }
            }


            if(isBad) {
                bad++;
                System.out.println("* FAIL: " + id + "  /\t" + fontName);
            } else {
                System.out.println("* OK  : " + id + "  /\t" + fontName);
            }
            total++;
        }
    }


    private boolean verifyFont(File f, Map<String, String> fontAttrs) {
        InputStream fis = null;
        String fontName = fontAttrs.get(XML_NAME);
        int count=0;
        try {
            fis = new BufferedInputStream(new FileInputStream(f));

            int i = 0;
            int r;
            try {
                while((r=fis.read())!=-1) {
                    i+=(int)r;
                    count++;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            if(OPT_VERBOSE) {
                System.out.println("for " + f.getAbsolutePath() + " chks = 0x" + Integer.toHexString(i) + " size=" + count);
            }
            String theirStr = fontAttrs.get("rchecksum");

            String ourStr = Integer.toHexString(i).toLowerCase();

            if(theirStr!=null) {
                if(theirStr.startsWith("0x")) {
                    theirStr = theirStr.substring(2).toLowerCase();
                } else {
                    theirStr = theirStr.toLowerCase();
                }
                long theirs = Integer.parseInt(theirStr, 16);
                if(theirs != i) {
                    err("WARNING: rchecksum for " + fontName + " was " + i  + " (0x"+ourStr+") "+ " but file said " + theirs +" (0x"+theirStr+")  - perhaps a different font?");
                    return false;
                } else {
                    if(OPT_VERBOSE) {
                        System.out.println(" rchecksum for " + fontName + " OK");
                    }
                    return true;
                }
            } else {
                System.err.println("WARNING: rchecksum for " + fontName + " was " + i  + " (0x"+ourStr+") "+ " but rchecksum was MISSING. Old ICU data?");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }


    private Integer[] parseHexArray(String hex) {
        List<Integer> ret = new ArrayList<Integer>();
        String items[] = hex.split("[\\s,]");
        for(String i : items) {
            if(i.isEmpty()) continue;
            if(i.startsWith("0x")) {
                i = i.substring(2);
            }
            ret.add(Integer.parseInt(i, 16));
        }
        return ret.toArray(new Integer[0]);
    }


    private void err(String string) {
        if(OPT_NOTHROW) {
            System.out.println(id+" ERROR: " + string +" (continuing due to -n)");
        } else {
            throw new InternalError(id+ ": " + string);
        }
    }


    private Font getFont(String fontName, Map<String, String> fontAttrs) {
        Font f;
        if(false)
            try {
                f = Font.getFont(fontName);
                if(f!=null)  {
                    if(OPT_VERBOSE) {
                        System.out.println("Loaded default path to " + fontName);
                    }
                    return f;
                }
            } catch(Throwable t) {
                if(OPT_VERBOSE) {
                    t.printStackTrace();
                    System.out.println("problem loading font " + fontName + " - " + t.toString());
                }
            }

        File homeDir = new File(System.getProperty("user.home"));
        File fontDir = new File(homeDir, "fonts");
        File fontFile = new File(fontDir, fontName);
        if(fontFile.canRead()) {
            try {
                if(!verifyFont(fontFile,fontAttrs)) {
                    System.out.println("Warning: failed to verify " + fontName);
                }
                f = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                if(f!=null & OPT_VERBOSE) {
                    System.out.println("> loaded from " + fontFile.getAbsolutePath() + " - " + f.toString());
                }
                return f;
            } catch (FontFormatException e) {
                if(OPT_VERBOSE) {
                    e.printStackTrace();
                    System.out.println("problem loading font " + fontName + " - " + e.toString());
                }
            } catch (IOException e) {
                if(OPT_VERBOSE) {
                    e.printStackTrace();
                    System.out.println("problem loading font " + fontName + " - " + e.toString());
                }
            }
        }
        return null;
    }


    private static Map<String, String> attrs(Node testCase) {
        Map<String,String> rv = new TreeMap<String,String>();
        NamedNodeMap nnm = testCase.getAttributes();
        for(int i=0;i<nnm.getLength();i++) {
            Node n = nnm.item(i);
            String k = n.getNodeName();
            String v = n.getNodeValue();
            rv.put(k, v);
        }
        return rv;
    }
}
