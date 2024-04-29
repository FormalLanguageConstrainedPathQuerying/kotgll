/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.eql.analysis;

import org.elasticsearch.xpack.eql.expression.OptionalResolvedAttribute;
import org.elasticsearch.xpack.eql.expression.OptionalUnresolvedAttribute;
import org.elasticsearch.xpack.ql.expression.Attribute;
import org.elasticsearch.xpack.ql.expression.FieldAttribute;
import org.elasticsearch.xpack.ql.expression.UnresolvedAttribute;
import org.elasticsearch.xpack.ql.type.DataTypes;
import org.elasticsearch.xpack.ql.type.InvalidMappedField;
import org.elasticsearch.xpack.ql.type.UnsupportedEsField;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class AnalysisUtils {

    private AnalysisUtils() {}

    static Attribute resolveAgainstList(UnresolvedAttribute u, Collection<Attribute> attrList) {
        return resolveAgainstList(u, attrList, false);
    }

    static Attribute resolveAgainstList(UnresolvedAttribute u, Collection<Attribute> attrList, boolean allowCompound) {
        Set<Attribute> matches = new LinkedHashSet<>();

        boolean qualified = u.qualifier() != null;

        for (Attribute attribute : attrList) {
            if (attribute.synthetic() == false) {
                boolean match = qualified ? Objects.equals(u.qualifiedName(), attribute.qualifiedName()) :
                    (Objects.equals(u.name(), attribute.name())
                        || Objects.equals(u.name(), attribute.qualifiedName()));
                if (match) {
                    matches.add(attribute.withLocation(u.source()));
                }
            }
        }

        if (matches.isEmpty()) {
            return null;
        }

        if (matches.size() == 1) {
            return handleSpecialFields(u, matches.iterator().next(), allowCompound);
        }

        return u.withUnresolvedMessage(
            "Reference ["
                + u.qualifiedName()
                + "] is ambiguous (to disambiguate use quotes or qualifiers); matches any of "
                + matches.stream().map(a -> "\"" + a.qualifier() + "\".\"" + a.name() + "\"").sorted().toList()
        );
    }

    private static Attribute handleSpecialFields(UnresolvedAttribute u, Attribute named, boolean allowCompound) {
        if (named instanceof FieldAttribute fa) {

            if (fa.field() instanceof InvalidMappedField field) {
                named = u.withUnresolvedMessage("Cannot use field [" + fa.name() + "] due to ambiguities being " + field.errorMessage());
            }
            else if (DataTypes.isUnsupported(fa.dataType())) {
                UnsupportedEsField unsupportedField = (UnsupportedEsField) fa.field();
                if (unsupportedField.hasInherited()) {
                    named = u.withUnresolvedMessage(
                        "Cannot use field ["
                            + fa.name()
                            + "] with unsupported type ["
                            + unsupportedField.getOriginalType()
                            + "] "
                            + "in hierarchy (field ["
                            + unsupportedField.getInherited()
                            + "])"
                    );
                } else {
                    named = u.withUnresolvedMessage(
                        "Cannot use field [" + fa.name() + "] with unsupported type [" + unsupportedField.getOriginalType() + "]"
                    );
                }
            }
            else if (allowCompound == false && DataTypes.isPrimitive(fa.dataType()) == false && fa.dataType() != DataTypes.NESTED) {
                named = u.withUnresolvedMessage(
                    "Cannot use field [" + fa.name() + "] type [" + fa.dataType().typeName() + "] only its subfields"
                );
            }
            else if (fa.dataType() == DataTypes.NESTED) {
                named = u.withUnresolvedMessage(
                    "Cannot use field ["
                        + fa.name()
                        + "] type ["
                        + fa.dataType().typeName()
                        + "] "
                        + "due to nested fields not being supported yet"
                );
            }
            else if (fa.isNested()) {
                named = u.withUnresolvedMessage(
                    "Cannot use field ["
                        + fa.name()
                        + "] type ["
                        + fa.dataType().typeName()
                        + "] "
                        + "with unsupported nested type in hierarchy (field ["
                        + fa.nestedParent().name()
                        + "])"
                );
            }
            else if (u instanceof OptionalUnresolvedAttribute) {
                named = new OptionalResolvedAttribute(fa);
            }
        } else {
            if (u instanceof OptionalUnresolvedAttribute) {
                named = u.withUnresolvedMessage("Unsupported optional field [" + named.name() + "] type [" + named.dataType().typeName());
            }
        }
        return named;
    }
}
