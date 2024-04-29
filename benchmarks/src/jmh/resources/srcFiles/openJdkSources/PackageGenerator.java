/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package sampleapi.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCPackageDecl;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sampleapi.util.SimpleMultiplier;

public class PackageGenerator {

    public String packageName;
    String packageDirName;
    public String id;

    ArrayList<JCCompilationUnit> topLevels;
    Map<String, Integer> nameIndex;
    public Map<String, JCClassDecl> idBases;
    Map<String, JCAnnotation> idAnnos;

    TreeMaker make;
    Names names;
    Symtab syms;
    DocumentBuilderFactory factory;
    Documentifier documentifier;
    boolean fx;

    public PackageGenerator() {
        JavacTool jt = JavacTool.create();
        JavacTask task = jt.getTask(null, null, null, null, null, null);
        Context ctx = ((JavacTaskImpl)task).getContext();

        make = TreeMaker.instance(ctx);
        names = Names.instance(ctx);
        syms = Symtab.instance(ctx);
        factory = DocumentBuilderFactory.newInstance();

        documentifier = Documentifier.instance(ctx);
    }

    boolean isDataSetProcessed = false;

    public static PackageGenerator processDataSet(Element rootElement) {
        PackageGenerator result = new PackageGenerator();
        result.isDataSetProcessed = true;
        result.topLevels = new ArrayList<>();
        result.nameIndex = new HashMap<>();
        result.idBases = new HashMap<>();
        result.idAnnos = new HashMap<>();
        result.fx = false;

        if (!rootElement.getTagName().equals("package")) {
            throw new IllegalStateException("Unexpected tag name: "
                    + rootElement.getTagName());
        }
        result.packageName = rootElement.getAttribute("name");
        result.id = rootElement.getAttribute("id");
        result.fx = "fx".equals(rootElement.getAttribute("style"));
        result.packageDirName = result.packageName.replace('.', '/');

        NodeList nodeList = rootElement.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (!(node instanceof Element)) {
                continue;
            }
            result.processTopLevel((Element) node);
        }
        return result;
    }

    public void generate(Path outDir) {
        if (!isDataSetProcessed)
            throw new RuntimeException("No Data Set processed");

        try {
            File pkgDir = new File(outDir.toFile(), packageDirName);
            pkgDir.mkdirs();

            for (JCCompilationUnit decl : topLevels) {
                JCClassDecl classDecl = (JCClassDecl) decl.getTypeDecls().get(0);
                File outFile
                        = new File(pkgDir, classDecl.getSimpleName().toString() + ".java");
                FileWriter writer = new FileWriter(outFile);
                writer.write(decl.toString());
                writer.flush();
                writer.close();
            }

            File outFile = new File(pkgDir, "package-info.java");
            FileWriter writer = new FileWriter(outFile);
            writer.write("/**\n");
            writer.write(documentifier.getDocGenerator().getPackageComment());
            writer.write("*/\n");
            writer.write("package " + packageName + ";\n");
            writer.flush();
            writer.close();

            outFile = new File(pkgDir, "overview.html");
            writer = new FileWriter(outFile);
            writer.write("<html>\n");
            writer.write("<head>\n<title>" + packageName + "</title>\n</head>\n");
            writer.write("<body>\n");
            writer.write("<p>Package " + packageName + " overview.\n");
            writer.write("</body>\n");
            writer.write("</html>\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Error writing output");
        }
    }


    void processTopLevel(Element tlTag) {
        String kind = tlTag.getTagName();

        if (kind.equals("annodecl")) {
            String declId = tlTag.getAttribute("id");
            if (!declId.startsWith("@"))
                declId = "@" + declId;
            idAnnos.put(declId, processAnnoDecl(tlTag));
            return;
        }

        ListBuffer<JCTree>[] bases = processBases(tlTag, null);

        for (JCTree base : bases[0]) { 
            JCPackageDecl pkg = make.PackageDecl(
                                    List.<JCAnnotation>nil(),
                                    make.QualIdent(
                                        new Symbol.PackageSymbol(
                                            names.fromString(packageName),
                                            null)));
            ListBuffer<JCTree> topLevelParts = new ListBuffer<>();
            topLevelParts.append(pkg);
            topLevelParts.appendList(bases[1]); 
            topLevelParts.append(base);

            JCCompilationUnit topLevel = make.TopLevel(topLevelParts.toList());
            documentifier.documentify(topLevel, fx);
            topLevels.add(topLevel);
        }
    }

    ListBuffer<JCTree>[] processBases(Element baseTag, HashMap<String, Integer> scope) {
        String kind = baseTag.getTagName();
        String baseName = baseTag.getAttribute("basename");
        String typeParam = baseTag.getAttribute("tparam");
        String baseId = baseTag.getAttribute("id");
        System.out.println("Found class id: " + baseId);

        long kindFlag = 0;
        switch (kind) {
            case "class":
                break;
            case "interface":
                kindFlag |= Flags.INTERFACE;
                break;
            case "enum":
                kindFlag |= Flags.ENUM;
                break;
            case "annotation":
                kindFlag |= Flags.ANNOTATION | Flags.INTERFACE;
                break;
        }

        NodeList nodes = baseTag.getChildNodes();
        ListBuffer<JCTree> bases = new ListBuffer<>();
        ListBuffer<JCTree> members = new ListBuffer<>();
        ListBuffer<JCTree> imports = new ListBuffer<>();
        JCExpression extType = null;
        ListBuffer<JCExpression> implTypes = new ListBuffer<>();
        SimpleMultiplier multiply = new SimpleMultiplier();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (!(node instanceof Element))
                continue;
            Element element = (Element)node;
            switch (element.getTagName()) {
                case "modifier":
                    multiply.addAxis(element.getTextContent());
                    break;
                case "anno":
                    multiply.addAxis(element.getTextContent());
                    break;
                case "member":
                    members.appendList(processMembers(element, baseName, kind));
                    break;
                case "extend":
                    String classId = element.getAttribute("id");   
                    String classRef = element.getAttribute("ref"); 
                    if (classId.length() !=0 &&
                        idBases.containsKey(classId)) {
                        JCClassDecl baseDecl = idBases.get(classId);
                        extType = make.Type(
                                      getTypeByName(
                                          baseDecl.getSimpleName().toString()));
                        members.appendList(processMethods(baseDecl.getMembers(), false));
                    } else if (classRef.length() !=0) {
                        extType = make.Type(getTypeByName(classRef));
                    }
                    break;
                case "implement":
                    String interfaceId = element.getAttribute("id");
                    String interfaceRef = element.getAttribute("ref");
                    if (interfaceId.length() != 0 &&
                        idBases.containsKey(interfaceId)) {
                        JCClassDecl baseDecl = idBases.get(interfaceId);
                        implTypes.add(
                            make.Type(
                                getTypeByName(
                                    baseDecl.getSimpleName().toString())));
                        members.appendList(processMethods(baseDecl.getMembers(), true));
                    } else if (interfaceRef.length() != 0) {
                        implTypes.add(make.Type(getTypeByName(interfaceRef)));
                    }
                    break;
                case "import":
                    String[] idents = element.getTextContent().split("\\.");
                    if (idents.length < 2)
                        throw new IllegalStateException("Invalid import: " + element.getTextContent());
                    JCFieldAccess select = make.Select(
                        make.Ident(names.fromString(idents[0])), names.fromString(idents[1]));
                    for (int j = 2; j < idents.length; j++)
                        select = make.Select(select, names.fromString(idents[j]));
                    imports.append(make.Import(select, false));
                    break;
            }
        }

        multiply.initIterator();
        while (multiply.hasNext()) {
            ArrayList<String> tuple = multiply.getNext();

            long declFlags = kindFlag;
            ListBuffer<JCAnnotation> annos = new ListBuffer<>();
            for (String modifier : tuple) {
                if (modifier.startsWith("@") && idAnnos.containsKey(modifier))
                    annos.add(idAnnos.get(modifier)); 
                else
                    declFlags |= getFlagByName(modifier); 
            }

            String declName = (scope == null)
                                  ? getUniqName(baseName)
                                  : baseName + getUniqIndex(scope, baseName);
            JCClassDecl baseDecl = make.ClassDef(
                                       make.Modifiers(declFlags, annos.toList()),
                                       names.fromString(declName),
                                       processTypeParams(typeParam), 
                                       extType,                      
                                       implTypes.toList(),           
                                       members.toList());            

            fixConstructorNames(baseDecl);

            bases.append(baseDecl);

            if (baseId.length() != 0) {
                idBases.put(baseId, baseDecl);
                baseId = "";
            }
        }

        return new ListBuffer[] { bases, imports };
    }

    List<JCTypeParameter> processTypeParams(String typeParams) {

        if (typeParams == null || typeParams.length() == 0)
            return List.<JCTypeParameter>nil(); 

        String[] typeVarsArr = typeParams.split(",");
        ListBuffer<JCTypeParameter> typeParamsDecls = new ListBuffer<>();

        for (String typeVar : typeVarsArr) {
            typeParamsDecls.add(
                make.TypeParameter(names.fromString(typeVar),
                                    List.<JCExpression>nil()));
        }

        return typeParamsDecls.toList();
    }

    ListBuffer<JCTree> processMembers(Element memberTag, String name, String kind) {
        ListBuffer<JCTree> members = new ListBuffer<>();
        NodeList nodes = memberTag.getChildNodes();
        HashMap<String, Integer> scope = new HashMap<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (!(node instanceof Element))
                continue;

            switch (((Element)node).getTagName()) {
                case "field":
                    members.appendList(processFields((Element)node, scope));
                    break;
                case "serialfield":
                    members.append(processSerialFields((Element)node));
                    break;
                case "constant":
                    members.appendList(processConstants((Element)node, scope));
                    break;
                case "constructor":
                    members.appendList(processMethods((Element)node, scope, true, true));
                    break;
                case "method":
                    boolean needBody = kind.equals("class") || kind.equals("enum");
                    members.appendList(processMethods((Element)node, scope, needBody, false));
                    break;
                case "class":
                case "interface":
                case "enum":
                case "annotation":
                    members.appendList(processBases((Element)node, scope)[0]);
                    break;
            }
        }

        return members;
    }

    ListBuffer<JCTree> processFields(Element fieldsNode, HashMap<String, Integer> scope) {
        String kind = fieldsNode.getTagName();
        String baseName = fieldsNode.getAttribute("basename");

        ListBuffer<JCTree> fields = new ListBuffer<>();
        NodeList nodes = fieldsNode.getChildNodes();
        SimpleMultiplier multiply = new SimpleMultiplier(); 
        String[] types = new String[] {};
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (!(node instanceof Element))
                continue;

            switch (((Element)node).getTagName()) {
                case "modifier":
                    multiply.addAxis(((Element)node).getTextContent());
                    break;
                case "anno":
                    multiply.addAxis(((Element)node).getTextContent());
                case "type":
                    types = ((Element)node).getTextContent().split("\\|");
                    break;
            }
        }

        multiply.initIterator();
        while (multiply.hasNext()) {
            ArrayList<String> tuple = multiply.getNext();

            long declFlags = 0;
            ListBuffer<JCAnnotation> annos = new ListBuffer<>();
            for (String modifier : tuple) {
                if (modifier.startsWith("@") && idAnnos.containsKey(modifier))
                    annos.add(idAnnos.get(modifier)); 
                else
                    declFlags |= getFlagByName(modifier); 
            }


            for (String type : types) {
                String declName = baseName + getUniqIndex(scope, baseName);

                Type initType = getTypeByName(type);
                JCExpression initExpr = null;
                if ((declFlags & Flags.STATIC) != 0) 
                    initExpr = make.Literal(initType.isPrimitive() ?
                                             initType.getTag() :
                                             TypeTag.BOT,
                                             "String".equals(type)
                                                 ? new String("blah-blah-blah")
                                                 : Integer.valueOf(0));

                JCVariableDecl fieldDecl = make.VarDef(
                                               make.Modifiers(declFlags, annos.toList()),
                                               names.fromString(declName),
                                               make.Type(getTypeByName(type)),
                                               initExpr);

                fields.append(fieldDecl);
            }
        }

        return fields;
    }

    JCTree processSerialFields(Element sfNode) {
        String baseName = sfNode.getAttribute("basename");
        String[] fieldTypes = sfNode.getTextContent().split(",");

        ListBuffer<JCExpression> serialFields = new ListBuffer<>();
        HashMap<String, Integer> scope = new HashMap<>();

        for (String fType : fieldTypes) {
            String fieldName = baseName + getUniqIndex(scope, baseName);
            serialFields.add(
                make.NewClass(
                    null,
                    null,
                    make.Type(getTypeByName("ObjectStreamField")),
                    List.from(
                        new JCTree.JCExpression[] {
                            make.Literal(fieldName),
                            make.Ident(names.fromString(fType + ".class"))
                        }),
                    null));
        }

        JCTree sfDecl = make.VarDef(
                            make.Modifiers(
                                Flags.PRIVATE | Flags.STATIC | Flags.FINAL),
                            names.fromString("serialPersistentFields"),
                            make.TypeArray(
                                make.Type(getTypeByName("ObjectStreamField"))),
                            make.NewArray(
                                null,
                                List.<JCExpression>nil(),
                                serialFields.toList()));

        return sfDecl;
    }

    ListBuffer<JCTree> processConstants(Element constNode, HashMap<String, Integer> scope) {
        String baseName = constNode.getAttribute("basename");
        int count = 1;
        try {
            count = Integer.parseInt(constNode.getAttribute("count"));
        } catch (Exception e) {} 

        long declFlags = Flags.PUBLIC | Flags.STATIC | Flags.FINAL | Flags.ENUM;
        ListBuffer<JCTree> fields = new ListBuffer<>();

        for (int i = 0; i < count; i++) {
            String declName = baseName +
                              ((count == 1) ? "" : getUniqIndex(scope, baseName));

            JCVariableDecl constDecl = make.VarDef(
                                           make.Modifiers(declFlags),
                                           names.fromString(declName),
                                           null,  
                                           null); 

            fields.append(constDecl);
        }
        return fields;
    }

    ListBuffer<JCTree> processMethods(Element methodsNode, HashMap<String, Integer> scope, boolean needBody, boolean isConstructor) {
        String kind = methodsNode.getTagName();
        String baseName = methodsNode.getAttribute("basename");
        String name = methodsNode.getAttribute("name");
        String methodTypeParam = methodsNode.getAttribute("tparam");

        ListBuffer<JCTree> methods = new ListBuffer<>();
        NodeList nodes = methodsNode.getChildNodes();
        SimpleMultiplier multiply = new SimpleMultiplier(); 
        String[] types = new String[0];
        String[] params = new String[] { "none" }; 
        ListBuffer<Type> throwTypes = new ListBuffer<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (!(node instanceof Element))
                continue;

            switch (((Element)node).getTagName()) {
                case "modifier":
                    multiply.addAxis(((Element)node).getTextContent());
                    break;
                case "anno":
                    multiply.addAxis(((Element)node).getTextContent());
                    break;
                case "type":
                    types = ((Element)node).getTextContent().split("\\|");
                    break;
                case "param":
                    params = ((Element)node).getTextContent().split("\\|");
                    break;
                case "throw":
                    throwTypes.add(
                        getTypeByName(((Element)node).getTextContent()));
                    break;

            }
        }

        if (isConstructor) {
            baseName = "constructor";
            types = new String[] { "" };
        }

        boolean isDirectName = false;
        if (name.length() > 0) {
            baseName = name;
            isDirectName = true;
        }

        multiply.initIterator();
        while (multiply.hasNext()) {
            ArrayList<String> tuple = multiply.getNext();

            long declFlags = 0;
            ListBuffer<JCAnnotation> annos = new ListBuffer<>();
            for (String modifier : tuple) {
                if (modifier.startsWith("@") && idAnnos.containsKey(modifier))
                    annos.add(idAnnos.get(modifier)); 
                else
                    declFlags |= getFlagByName(modifier); 
            }

            for (String type : types) {
                String declName = baseName
                                  + ((isConstructor || isDirectName)
                                     ? "" : getUniqIndex(scope, baseName));

                JCBlock body = null;
                if (needBody && (declFlags & Flags.ABSTRACT) == 0) { 
                    List<JCStatement> bodyStatements = List.<JCStatement>nil();
                    if (!type.equals("") && !type.equals("void")) { 
                        Type retType = getTypeByName(type);
                        bodyStatements = List.<JCStatement>of(
                                             make.Return(
                                                 make.Literal(
                                                     retType.isPrimitive() ?
                                                         retType.getTag() :
                                                         TypeTag.BOT,
                                                     Integer.valueOf(0))));
                    }
                    body = make.Block(0, bodyStatements);
                }

                for (String param : params) {

                    JCMethodDecl methodDecl =
                        make.MethodDef(
                            make.Modifiers(declFlags, annos.toList()),
                            names.fromString(declName),
                            isConstructor ? null : make.Type(getTypeByName(type)),
                            processTypeParams(methodTypeParam), 
                            null,                               
                            processParams(param),               
                            make.Types(throwTypes.toList()),   
                            body,
                            null);                              

                    methods.append(methodDecl);
                }
            }
        }

        return methods;
    }

    JCAnnotation processAnnoDecl(Element annoDeclNode) {
        String annoId = annoDeclNode.getAttribute("id");

        ListBuffer<JCExpression> args = new ListBuffer<>();
        String className = "";

        NodeList nodes = annoDeclNode.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);

            if (!(node instanceof Element))
                continue;

            switch (((Element)node).getTagName()) {
                case "class":
                    className = ((Element)node).getTextContent();
                    break;
                case "arg":
                    String argName = ((Element)node).getAttribute("name");
                    String argValue = ((Element)node).getAttribute("value");

                    JCExpression arg;
                    if (argName.length() == 0)
                        arg = make.Ident(names.fromString(argValue));
                    else
                        arg = make.Assign(
                                  make.Ident(names.fromString(argName)),
                                  make.Ident(names.fromString(argValue)));

                    args.add(arg);
                    break;
            }
        }

        return make.Annotation(
                   make.Ident(names.fromString(className)),
                   args.toList());
    }

    ListBuffer<JCTree> processMethods(List<JCTree> tree, boolean needBody) {
        ListBuffer<JCTree> methods = new ListBuffer<>();
        for (JCTree memberDecl : tree) {
            if (memberDecl instanceof JCMethodDecl) {
                JCMethodDecl methodDecl = (JCMethodDecl)memberDecl;
                JCTree retTypeTree = methodDecl.getReturnType();

                if (retTypeTree == null)
                    continue;

                if (needBody) {
                    Type retType = retTypeTree.type;

                    List<JCStatement> bodyStatements = List.<JCStatement>nil();
                    if (retType.getTag() != TypeTag.VOID)
                        bodyStatements = List.<JCStatement>of(
                                             make.Return(
                                                 make.Literal(
                                                     retType.isPrimitive() ?
                                                         retType.getTag() :
                                                         TypeTag.BOT,
                                                     Integer.valueOf(0))));

                    JCBlock body = make.Block(0, bodyStatements);

                    methodDecl = make.MethodDef(
                                     methodDecl.getModifiers(),
                                     methodDecl.getName(),
                                     (JCExpression)methodDecl.getReturnType(),
                                     methodDecl.getTypeParameters(),
                                     methodDecl.getReceiverParameter(),
                                     methodDecl.getParameters(),
                                     methodDecl.getThrows(),
                                     body,
                                     (JCExpression)methodDecl.getDefaultValue());
                }

                methods.add(methodDecl);
            }
        }
        return methods;
    }

    void fixConstructorNames(JCClassDecl baseDecl) {
        ListBuffer<JCTree> newMembers = new ListBuffer<>();
        List<JCTree> members = baseDecl.getMembers();
        Name name = baseDecl.getSimpleName();

        for (JCTree memberDecl : members) {
            JCTree newDecl = memberDecl;

            if (memberDecl instanceof JCMethodDecl) {
                JCMethodDecl methodDecl = (JCMethodDecl)memberDecl;
                JCTree retTypeTree = methodDecl.getReturnType();

                if (retTypeTree == null)
                    newDecl = make.MethodDef(
                                  methodDecl.getModifiers(),
                                  name,
                                  (JCExpression)methodDecl.getReturnType(),
                                  methodDecl.getTypeParameters(),
                                  methodDecl.getReceiverParameter(),
                                  methodDecl.getParameters(),
                                  methodDecl.getThrows(),
                                  methodDecl.getBody(),
                                  (JCExpression)methodDecl.getDefaultValue());
            }

            newMembers.add(newDecl);
        }

        baseDecl.defs = newMembers.toList();
    }

    List<JCVariableDecl> processParams(String paramTypes) {

        if ("none".equals(paramTypes))
            return List.<JCVariableDecl>nil(); 

        String[] typesArr = paramTypes.split(",(?!(\\w+,)*\\w+>)");
        ListBuffer<JCVariableDecl> paramsDecls = new ListBuffer<>();

        int i = 0;
        for (String typeName : typesArr) {
            String paramName = "param"
                               + (typesArr.length == 1 ? "" : String.valueOf(i));
            paramsDecls.add(
                make.VarDef(make.Modifiers(0),
                             names.fromString(paramName),
                             make.Type(getTypeByName(typeName)),
                             null));
            i++;
        }

        return paramsDecls.toList();
    }


    String getUniqName(String name) {
        if (!nameIndex.containsKey(name))
            nameIndex.put(name, 0);
        Integer index = nameIndex.get(name);
        String uniqName = name + index;
        nameIndex.put(name, index + 1);
        return uniqName;
    }

    int getUniqIndex(HashMap<String, Integer> scope, String name) {
        if (!scope.containsKey(name))
            scope.put(name, 0);
        Integer index = scope.get(name);
        scope.put(name, index + 1);
        return index;
    }

    long getFlagByName(String modifierName) {
        switch (modifierName) {
            case "public":
                return Flags.PUBLIC;
            case "private":
                return Flags.PRIVATE;
            case "protected":
                return Flags.PROTECTED;
            case "static":
                return Flags.STATIC;
            case "final":
                return Flags.FINAL;
            case "abstract":
                return Flags.ABSTRACT;
            case "strictfp":
                return Flags.STRICTFP;
            default:
                return 0;
        }
    }

    Type getTypeByName(String typeName) {
        switch (typeName) {
            case "void":
                return syms.voidType;
            case "boolean":
                return syms.booleanType;
            case "byte":
                return syms.byteType;
            case "char":
                return syms.charType;
            case "double":
                return syms.doubleType;
            case "float":
                return syms.floatType;
            case "int":
                return syms.intType;
            case "long":
                return syms.longType;
            default:
                return getTypeByName(typeName, List.<Type>nil());
        }
    }

    Type getTypeByName(String typeName, List<Type> tparams) {
        return new Type.ClassType(
                   Type.noType,
                   tparams,
                   new Symbol.ClassSymbol(0, names.fromString(typeName), null));
    }
}
