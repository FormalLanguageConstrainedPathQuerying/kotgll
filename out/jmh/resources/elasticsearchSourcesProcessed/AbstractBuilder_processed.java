/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.esql.parser;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.elasticsearch.xpack.ql.parser.ParserUtils;
import org.elasticsearch.xpack.ql.tree.Source;

abstract class AbstractBuilder extends EsqlBaseParserBaseVisitor<Object> {

    @Override
    public Object visit(ParseTree tree) {
        return ParserUtils.visit(super::visit, tree);
    }

    @Override
    public Object visitTerminal(TerminalNode node) {
        return ParserUtils.source(node);
    }

    static String unquoteString(Source source) {
        String text = source.text();
        if (text == null) {
            return null;
        }

        if (text.startsWith("\"\"\"")) {
            return text.substring(3, text.length() - 3);
        }

        text = text.substring(1, text.length() - 1);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < text.length();) {
            if (text.charAt(i) == '\\') {
                switch (text.charAt(++i)) {
                    case 't' -> sb.append('\t');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('\"');
                    case '\\' -> sb.append('\\');

                    default ->
                        sb.append('\\').append(text.charAt(i));
                }
                i++;
            } else {
                sb.append(text.charAt(i++));
            }
        }
        return sb.toString();
    }
}
