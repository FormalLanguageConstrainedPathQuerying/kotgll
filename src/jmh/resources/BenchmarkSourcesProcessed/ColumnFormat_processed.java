/*
 * Copyright (c) 2004, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.tools.jstat;

/**
 * A class to represent the format for a column of data.
 *
 * @author Brian Doherty
 * @since 1.5
 */
public class ColumnFormat extends OptionFormat {
    private int number;
    private int width;
    private Alignment align = Alignment.CENTER;
    private Scale scale = Scale.RAW;
    private String format;
    private String header;
    private Expression expression;
    private boolean required = false;
    private Object previousValue;

    public ColumnFormat(int number) {
        super("Column" + number);
        this.number = number;
    }

    /*
     * method to apply various validation rules to the ColumnFormat object.
     */
    public void validate() throws ParserException {


        if (expression == null) {
            throw new ParserException("Missing data statement in column " + number);
        }
        if (header == null) {
            throw new ParserException("Missing header statement in column " + number);
        }
        if (format == null) {
            format="0";
        }

        expression.setRequired(required);
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setAlignment(Alignment align) {
        this.align = align;
    }

    public void setScale(Scale scale) {
        this.scale = scale;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }

    public String getFormat() {
        return format;
    }

    public int getWidth() {
        return width;
    }

    public Alignment getAlignment() {
        return align;
    }

    public Scale getScale() {
        return scale;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression e) {
        this.expression = e;
    }

    public void setRequired(boolean r) {
        this.required = r;
    }

    public boolean isRequired() {
        return this.required;
    }

    public void setPreviousValue(Object o) {
        this.previousValue = o;
    }

    public Object getPreviousValue() {
        return previousValue;
    }

    public void printFormat(int indentLevel) {
        String indentAmount = "  ";

        StringBuilder indent = new StringBuilder("");
        for (int j = 0; j < indentLevel; j++) {
            indent.append(indentAmount);
        }

        System.out.println(indent + name + " {");
        System.out.println(indent + indentAmount + "name=" + name
                + ";data=" + expression.toString() + ";header=" + header
                + ";format=" + format + ";width=" + width
                + ";scale=" + scale.toString() + ";align=" + align.toString()
                + ";required=" + required);

        for (OptionFormat of : children) {
            of.printFormat(indentLevel + 1);
        }

        System.out.println(indent + "}");
    }

    public String getValue() {
        return null;
    }
}
