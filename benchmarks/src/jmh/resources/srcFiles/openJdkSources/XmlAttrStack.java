/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sun.org.apache.xml.internal.security.c14n.implementations;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Attr;

/**
 * An XmlAttrStack that is shared between the Canonical XML 1.0 and 1.1 implementations.
 */
class XmlAttrStack {

    private static final com.sun.org.slf4j.internal.Logger LOG =
        com.sun.org.slf4j.internal.LoggerFactory.getLogger(XmlAttrStack.class);

    private static class XmlsStackElement {
        int level;
        boolean rendered = false;
        final List<Attr> nodes = new ArrayList<>();
    }

    private int currentLevel = 0;
    private int lastlevel = 0;
    private XmlsStackElement cur;

    private final List<XmlsStackElement> levels = new ArrayList<>();
    private final boolean c14n11;

    public XmlAttrStack(boolean c14n11) {
        this.c14n11 = c14n11;
    }

    void push(int level) {
        currentLevel = level;
        if (currentLevel == -1) {
            return;
        }
        cur = null;
        while (lastlevel >= currentLevel) {
            levels.remove(levels.size() - 1);
            int newSize = levels.size();
            if (newSize == 0) {
                lastlevel = 0;
                return;
            }
            lastlevel = levels.get(newSize - 1).level;
        }
    }

    void addXmlnsAttr(Attr n) {
        if (cur == null) {
            cur = new XmlsStackElement();
            cur.level = currentLevel;
            levels.add(cur);
            lastlevel = currentLevel;
        }
        cur.nodes.add(n);
    }

    void getXmlnsAttr(Collection<Attr> col) {
        int size = levels.size() - 1;
        if (cur == null) {
            cur = new XmlsStackElement();
            cur.level = currentLevel;
            lastlevel = currentLevel;
            levels.add(cur);
        }
        boolean parentRendered = false;
        XmlsStackElement e = null;
        if (size == -1) {
            parentRendered = true;
        } else {
            e = levels.get(size);
            if (e.rendered && e.level + 1 == currentLevel) {
                parentRendered = true;
            }
        }
        if (parentRendered) {
            col.addAll(cur.nodes);
            cur.rendered = true;
            return;
        }

        Map<String, Attr> loa = new HashMap<>();
        if (c14n11) {
            List<Attr> baseAttrs = new ArrayList<>();
            boolean successiveOmitted = true;
            for (; size >= 0; size--) {
                e = levels.get(size);
                if (e.rendered) {
                    successiveOmitted = false;
                }
                Iterator<Attr> it = e.nodes.iterator();
                while (it.hasNext() && successiveOmitted) {
                    Attr n = it.next();
                    if ("base".equals(n.getLocalName()) && !e.rendered) {
                        baseAttrs.add(n);
                    } else if (!loa.containsKey(n.getName())) {
                        loa.put(n.getName(), n);
                    }
                }
            }
            if (!baseAttrs.isEmpty()) {
                String base = null;
                Attr baseAttr = null;
                for (Attr n : col) {
                    if ("base".equals(n.getLocalName())) {
                        base = n.getValue();
                        baseAttr = n;
                        break;
                    }
                }
                for (Attr n : baseAttrs) {
                    if (base == null) {
                        base = n.getValue();
                        baseAttr = n;
                    } else {
                        try {
                            base = joinURI(n.getValue(), base);
                        } catch (URISyntaxException ue) {
                            LOG.debug(ue.getMessage(), ue);
                        }
                    }
                }
                if (base != null && base.length() != 0) {
                    baseAttr.setValue(base);
                    col.add(baseAttr);
                }
            }
        } else {
            for (; size >= 0; size--) {
                e = levels.get(size);
                for (Attr n : e.nodes) {
                    if (!loa.containsKey(n.getName())) {
                        loa.put(n.getName(), n);
                    }
                }
            }
        }

        cur.rendered = true;
        col.addAll(loa.values());
    }

    private static String joinURI(String baseURI, String relativeURI) throws URISyntaxException {
        String bscheme = null;
        String bauthority = null;
        String bpath = "";
        String bquery = null;

        if (baseURI != null) {
            if (baseURI.endsWith("..")) {
                baseURI = baseURI + "/";
            }
            URI base = new URI(baseURI);
            bscheme = base.getScheme();
            bauthority = base.getAuthority();
            bpath = base.getPath();
            bquery = base.getQuery();
        }

        URI r = new URI(relativeURI);
        String rscheme = r.getScheme();
        String rauthority = r.getAuthority();
        String rpath = r.getPath();
        String rquery = r.getQuery();

        String tscheme, tauthority, tpath, tquery;
        if (rscheme != null && rscheme.equals(bscheme)) {
            rscheme = null;
        }
        if (rscheme != null) {
            tscheme = rscheme;
            tauthority = rauthority;
            tpath = removeDotSegments(rpath);
            tquery = rquery;
        } else {
            if (rauthority != null) {
                tauthority = rauthority;
                tpath = removeDotSegments(rpath);
                tquery = rquery;
            } else {
                if (rpath.length() == 0) {
                    tpath = bpath;
                    if (rquery != null) {
                        tquery = rquery;
                    } else {
                        tquery = bquery;
                    }
                } else {
                    if (rpath.charAt(0) == '/') {
                        tpath = removeDotSegments(rpath);
                    } else {
                        if (bauthority != null && bpath.length() == 0) {
                            tpath = "/" + rpath;
                        } else {
                            int last = bpath.lastIndexOf('/');
                            if (last == -1) {
                                tpath = rpath;
                            } else {
                                tpath = bpath.substring(0, last+1) + rpath;
                            }
                        }
                        tpath = removeDotSegments(tpath);
                    }
                    tquery = rquery;
                }
                tauthority = bauthority;
            }
            tscheme = bscheme;
        }
        return new URI(tscheme, tauthority, tpath, tquery, null).toString();
    }

    private static String removeDotSegments(String path) {
        LOG.debug("STEP OUTPUT BUFFER\t\tINPUT BUFFER");

        String input = path;
        while (input.indexOf("
            input = input.replaceAll("
        }

        StringBuilder output = new StringBuilder();

        if (input.charAt(0) == '/') {
            output.append('/');
            input = input.substring(1);
        }

        printStep("1 ", output.toString(), input);

        while (input.length() != 0) {
            if (input.startsWith("./")) {
                input = input.substring(2);
                printStep("2A", output.toString(), input);
            } else if (input.startsWith("../")) {
                input = input.substring(3);
                if (!"/".equals(output.toString())) {
                    output.append("../");
                }
                printStep("2A", output.toString(), input);
            } else if (input.startsWith("/./")) {
                input = input.substring(2);
                printStep("2B", output.toString(), input);
            } else if ("/.".equals(input)) {
                input = input.replaceFirst("/.", "/");
                printStep("2B", output.toString(), input);
            } else if (input.startsWith("/../")) {
                input = input.substring(3);
                if (output.length() == 0) {
                    output.append('/');
                } else if (output.toString().endsWith("../")) {
                    output.append("..");
                } else if (output.toString().endsWith("..")) {
                    output.append("/..");
                } else {
                    int index = output.lastIndexOf("/");
                    if (index == -1) {
                        output = new StringBuilder();
                        if (input.charAt(0) == '/') {
                            input = input.substring(1);
                        }
                    } else {
                        output = output.delete(index, output.length());
                    }
                }
                printStep("2C", output.toString(), input);
            } else if ("/..".equals(input)) {
                input = input.replaceFirst("/..", "/");
                if (output.length() == 0) {
                    output.append('/');
                } else if (output.toString().endsWith("../")) {
                    output.append("..");
                } else if (output.toString().endsWith("..")) {
                    output.append("/..");
                } else {
                    int index = output.lastIndexOf("/");
                    if (index == -1) {
                        output = new StringBuilder();
                        if (input.charAt(0) == '/') {
                            input = input.substring(1);
                        }
                    } else {
                        output = output.delete(index, output.length());
                    }
                }
                printStep("2C", output.toString(), input);
            } else if (".".equals(input)) {
                input = "";
                printStep("2D", output.toString(), input);
            } else if ("..".equals(input)) {
                if (!"/".equals(output.toString())) {
                    output.append("..");
                }
                input = "";
                printStep("2D", output.toString(), input);
            } else {
                int end = -1;
                int begin = input.indexOf('/');
                if (begin == 0) {
                    end = input.indexOf('/', 1);
                } else {
                    end = begin;
                    begin = 0;
                }
                String segment;
                if (end == -1) {
                    segment = input.substring(begin);
                    input = "";
                } else {
                    segment = input.substring(begin, end);
                    input = input.substring(end);
                }
                output.append(segment);
                printStep("2E", output.toString(), input);
            }
        }

        if (output.toString().endsWith("..")) {
            output.append('/');
            printStep("3 ", output.toString(), input);
        }

        return output.toString();
    }

    private static void printStep(String step, String output, String input) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(" " + step + ":   " + output);
            if (output.length() == 0) {
                LOG.debug("\t\t\t\t" + input);
            } else {
                LOG.debug("\t\t\t" + input);
            }
        }
    }
}
