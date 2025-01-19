@file:Suppress("RedundantVisibilityModifier")

package org.ucfs

import org.ucfs.descriptors.Descriptor
import org.ucfs.input.IInputGraph
import org.ucfs.input.ILabel
import org.ucfs.parser.GeneratedParser
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.sppf.node.SppfNode

public class Java8ParserRecovery<VertexType, LabelType : ILabel> :
    GeneratedParser<VertexType, LabelType>() {
  public val grammar: Java8 = Java8()

  private val compilationUnit: Nonterminal = grammar.compilationUnit.nonterm

  private val identifier: Nonterminal = grammar.identifier.nonterm

  private val literal: Nonterminal = grammar.literal.nonterm

  private val type: Nonterminal = grammar.type.nonterm

  private val primitiveType: Nonterminal = grammar.primitiveType.nonterm

  private val referenceType: Nonterminal = grammar.referenceType.nonterm

  private val `annotation`: Nonterminal = grammar.annotation.nonterm

  private val numericType: Nonterminal = grammar.numericType.nonterm

  private val integralType: Nonterminal = grammar.integralType.nonterm

  private val floatingPointType: Nonterminal = grammar.floatingPointType.nonterm

  private val classOrInterfaceType: Nonterminal = grammar.classOrInterfaceType.nonterm

  private val typeVariable: Nonterminal = grammar.typeVariable.nonterm

  private val arrayType: Nonterminal = grammar.arrayType.nonterm

  private val classType: Nonterminal = grammar.classType.nonterm

  private val interfaceType: Nonterminal = grammar.interfaceType.nonterm

  private val typeArguments: Nonterminal = grammar.typeArguments.nonterm

  private val dims: Nonterminal = grammar.dims.nonterm

  private val typeParameter: Nonterminal = grammar.typeParameter.nonterm

  private val typeParameterModifier: Nonterminal = grammar.typeParameterModifier.nonterm

  private val typeBound: Nonterminal = grammar.typeBound.nonterm

  private val additionalBound: Nonterminal = grammar.additionalBound.nonterm

  private val typeArgumentList: Nonterminal = grammar.typeArgumentList.nonterm

  private val typeArgument: Nonterminal = grammar.typeArgument.nonterm

  private val wildcard: Nonterminal = grammar.wildcard.nonterm

  private val wildcardBounds: Nonterminal = grammar.wildcardBounds.nonterm

  private val typeName: Nonterminal = grammar.typeName.nonterm

  private val packageOrTypeName: Nonterminal = grammar.packageOrTypeName.nonterm

  private val expressionName: Nonterminal = grammar.expressionName.nonterm

  private val ambiguousName: Nonterminal = grammar.ambiguousName.nonterm

  private val methodName: Nonterminal = grammar.methodName.nonterm

  private val packageName: Nonterminal = grammar.packageName.nonterm

  private val result: Nonterminal = grammar.result.nonterm

  private val packageDeclaration: Nonterminal = grammar.packageDeclaration.nonterm

  private val importDeclaration: Nonterminal = grammar.importDeclaration.nonterm

  private val typeDeclaration: Nonterminal = grammar.typeDeclaration.nonterm

  private val packageModifier: Nonterminal = grammar.packageModifier.nonterm

  private val singleTypeImportDeclaration: Nonterminal = grammar.singleTypeImportDeclaration.nonterm

  private val typeImportOnDemandDeclaration: Nonterminal =
      grammar.typeImportOnDemandDeclaration.nonterm

  private val singleStaticImportDeclaration: Nonterminal =
      grammar.singleStaticImportDeclaration.nonterm

  private val staticImportOnDemandDeclaration: Nonterminal =
      grammar.staticImportOnDemandDeclaration.nonterm

  private val classDeclaration: Nonterminal = grammar.classDeclaration.nonterm

  private val interfaceDeclaration: Nonterminal = grammar.interfaceDeclaration.nonterm

  private val throws: Nonterminal = grammar.throws.nonterm

  private val normalClassDeclaration: Nonterminal = grammar.normalClassDeclaration.nonterm

  private val enumDeclaration: Nonterminal = grammar.enumDeclaration.nonterm

  private val classModifier: Nonterminal = grammar.classModifier.nonterm

  private val typeParameters: Nonterminal = grammar.typeParameters.nonterm

  private val superclass: Nonterminal = grammar.superclass.nonterm

  private val superinterfaces: Nonterminal = grammar.superinterfaces.nonterm

  private val classBody: Nonterminal = grammar.classBody.nonterm

  private val typeParameterList: Nonterminal = grammar.typeParameterList.nonterm

  private val interfaceTypeList: Nonterminal = grammar.interfaceTypeList.nonterm

  private val classBodyDeclaration: Nonterminal = grammar.classBodyDeclaration.nonterm

  private val classMemberDeclaration: Nonterminal = grammar.classMemberDeclaration.nonterm

  private val instanceInitializer: Nonterminal = grammar.instanceInitializer.nonterm

  private val staticInitializer: Nonterminal = grammar.staticInitializer.nonterm

  private val constructorDeclaration: Nonterminal = grammar.constructorDeclaration.nonterm

  private val fieldDeclaration: Nonterminal = grammar.fieldDeclaration.nonterm

  private val methodDeclaration: Nonterminal = grammar.methodDeclaration.nonterm

  private val fieldModifier: Nonterminal = grammar.fieldModifier.nonterm

  private val unannType: Nonterminal = grammar.unannType.nonterm

  private val variableDeclaratorList: Nonterminal = grammar.variableDeclaratorList.nonterm

  private val variableDeclarator: Nonterminal = grammar.variableDeclarator.nonterm

  private val variableDeclaratorId: Nonterminal = grammar.variableDeclaratorId.nonterm

  private val variableInitializer: Nonterminal = grammar.variableInitializer.nonterm

  private val expression: Nonterminal = grammar.expression.nonterm

  private val arrayInitializer: Nonterminal = grammar.arrayInitializer.nonterm

  private val unannPrimitiveType: Nonterminal = grammar.unannPrimitiveType.nonterm

  private val unannReferenceType: Nonterminal = grammar.unannReferenceType.nonterm

  private val unannClassOrInterfaceType: Nonterminal = grammar.unannClassOrInterfaceType.nonterm

  private val unannTypeVariable: Nonterminal = grammar.unannTypeVariable.nonterm

  private val unannArrayType: Nonterminal = grammar.unannArrayType.nonterm

  private val unannClassType: Nonterminal = grammar.unannClassType.nonterm

  private val unannInterfaceType: Nonterminal = grammar.unannInterfaceType.nonterm

  private val methodModifier: Nonterminal = grammar.methodModifier.nonterm

  private val methodHeader: Nonterminal = grammar.methodHeader.nonterm

  private val methodBody: Nonterminal = grammar.methodBody.nonterm

  private val methodDeclarator: Nonterminal = grammar.methodDeclarator.nonterm

  private val formalParameterList: Nonterminal = grammar.formalParameterList.nonterm

  private val receiverParameter: Nonterminal = grammar.receiverParameter.nonterm

  private val formalParameters: Nonterminal = grammar.formalParameters.nonterm

  private val lastFormalParameter: Nonterminal = grammar.lastFormalParameter.nonterm

  private val formalParameter: Nonterminal = grammar.formalParameter.nonterm

  private val variableModifier: Nonterminal = grammar.variableModifier.nonterm

  private val exceptionTypeList: Nonterminal = grammar.exceptionTypeList.nonterm

  private val exceptionType: Nonterminal = grammar.exceptionType.nonterm

  private val block: Nonterminal = grammar.block.nonterm

  private val constructorModifier: Nonterminal = grammar.constructorModifier.nonterm

  private val constructorDeclarator: Nonterminal = grammar.constructorDeclarator.nonterm

  private val constructorBody: Nonterminal = grammar.constructorBody.nonterm

  private val simpleTypeName: Nonterminal = grammar.simpleTypeName.nonterm

  private val explicitConstructorInvocation: Nonterminal =
      grammar.explicitConstructorInvocation.nonterm

  private val enumBody: Nonterminal = grammar.enumBody.nonterm

  private val enumConstantList: Nonterminal = grammar.enumConstantList.nonterm

  private val enumConstant: Nonterminal = grammar.enumConstant.nonterm

  private val enumConstantModifier: Nonterminal = grammar.enumConstantModifier.nonterm

  private val enumBodyDeclarations: Nonterminal = grammar.enumBodyDeclarations.nonterm

  private val blockStatements: Nonterminal = grammar.blockStatements.nonterm

  private val argumentList: Nonterminal = grammar.argumentList.nonterm

  private val primary: Nonterminal = grammar.primary.nonterm

  private val normalInterfaceDeclaration: Nonterminal = grammar.normalInterfaceDeclaration.nonterm

  private val interfaceModifier: Nonterminal = grammar.interfaceModifier.nonterm

  private val extendsInterfaces: Nonterminal = grammar.extendsInterfaces.nonterm

  private val interfaceBody: Nonterminal = grammar.interfaceBody.nonterm

  private val interfaceMemberDeclaration: Nonterminal = grammar.interfaceMemberDeclaration.nonterm

  private val constantDeclaration: Nonterminal = grammar.constantDeclaration.nonterm

  private val constantModifier: Nonterminal = grammar.constantModifier.nonterm

  private val annotationTypeDeclaration: Nonterminal = grammar.annotationTypeDeclaration.nonterm

  private val annotationTypeBody: Nonterminal = grammar.annotationTypeBody.nonterm

  private val annotationTypeMemberDeclaration: Nonterminal =
      grammar.annotationTypeMemberDeclaration.nonterm

  private val annotationTypeElementDeclaration: Nonterminal =
      grammar.annotationTypeElementDeclaration.nonterm

  private val defaultValue: Nonterminal = grammar.defaultValue.nonterm

  private val normalAnnotation: Nonterminal = grammar.normalAnnotation.nonterm

  private val elementValuePairList: Nonterminal = grammar.elementValuePairList.nonterm

  private val elementValuePair: Nonterminal = grammar.elementValuePair.nonterm

  private val elementValue: Nonterminal = grammar.elementValue.nonterm

  private val elementValueArrayInitializer: Nonterminal =
      grammar.elementValueArrayInitializer.nonterm

  private val elementValueList: Nonterminal = grammar.elementValueList.nonterm

  private val markerAnnotation: Nonterminal = grammar.markerAnnotation.nonterm

  private val singleElementAnnotation: Nonterminal = grammar.singleElementAnnotation.nonterm

  private val interfaceMethodDeclaration: Nonterminal = grammar.interfaceMethodDeclaration.nonterm

  private val annotationTypeElementModifier: Nonterminal =
      grammar.annotationTypeElementModifier.nonterm

  private val conditionalExpression: Nonterminal = grammar.conditionalExpression.nonterm

  private val variableInitializerList: Nonterminal = grammar.variableInitializerList.nonterm

  private val blockStatement: Nonterminal = grammar.blockStatement.nonterm

  private val localVariableDeclarationStatement: Nonterminal =
      grammar.localVariableDeclarationStatement.nonterm

  private val localVariableDeclaration: Nonterminal = grammar.localVariableDeclaration.nonterm

  private val statement: Nonterminal = grammar.statement.nonterm

  private val statementNoShortIf: Nonterminal = grammar.statementNoShortIf.nonterm

  private val statementWithoutTrailingSubstatement: Nonterminal =
      grammar.statementWithoutTrailingSubstatement.nonterm

  private val emptyStatement: Nonterminal = grammar.emptyStatement.nonterm

  private val labeledStatement: Nonterminal = grammar.labeledStatement.nonterm

  private val labeledStatementNoShortIf: Nonterminal = grammar.labeledStatementNoShortIf.nonterm

  private val expressionStatement: Nonterminal = grammar.expressionStatement.nonterm

  private val statementExpression: Nonterminal = grammar.statementExpression.nonterm

  private val ifThenStatement: Nonterminal = grammar.ifThenStatement.nonterm

  private val ifThenElseStatement: Nonterminal = grammar.ifThenElseStatement.nonterm

  private val ifThenElseStatementNoShortIf: Nonterminal =
      grammar.ifThenElseStatementNoShortIf.nonterm

  private val assertStatement: Nonterminal = grammar.assertStatement.nonterm

  private val switchStatement: Nonterminal = grammar.switchStatement.nonterm

  private val switchBlock: Nonterminal = grammar.switchBlock.nonterm

  private val switchBlockStatementGroup: Nonterminal = grammar.switchBlockStatementGroup.nonterm

  private val switchLabels: Nonterminal = grammar.switchLabels.nonterm

  private val switchLabel: Nonterminal = grammar.switchLabel.nonterm

  private val enumConstantName: Nonterminal = grammar.enumConstantName.nonterm

  private val whileStatement: Nonterminal = grammar.whileStatement.nonterm

  private val whileStatementNoShortIf: Nonterminal = grammar.whileStatementNoShortIf.nonterm

  private val doStatement: Nonterminal = grammar.doStatement.nonterm

  private val interfaceMethodModifier: Nonterminal = grammar.interfaceMethodModifier.nonterm

  private val forStatement: Nonterminal = grammar.forStatement.nonterm

  private val forStatementNoShortIf: Nonterminal = grammar.forStatementNoShortIf.nonterm

  private val basicForStatement: Nonterminal = grammar.basicForStatement.nonterm

  private val basicForStatementNoShortIf: Nonterminal = grammar.basicForStatementNoShortIf.nonterm

  private val forInit: Nonterminal = grammar.forInit.nonterm

  private val forUpdate: Nonterminal = grammar.forUpdate.nonterm

  private val statementExpressionList: Nonterminal = grammar.statementExpressionList.nonterm

  private val enhancedForStatement: Nonterminal = grammar.enhancedForStatement.nonterm

  private val enhancedForStatementNoShortIf: Nonterminal =
      grammar.enhancedForStatementNoShortIf.nonterm

  private val breakStatement: Nonterminal = grammar.breakStatement.nonterm

  private val continueStatement: Nonterminal = grammar.continueStatement.nonterm

  private val returnStatement: Nonterminal = grammar.returnStatement.nonterm

  private val throwStatement: Nonterminal = grammar.throwStatement.nonterm

  private val synchronizedStatement: Nonterminal = grammar.synchronizedStatement.nonterm

  private val tryStatement: Nonterminal = grammar.tryStatement.nonterm

  private val catches: Nonterminal = grammar.catches.nonterm

  private val catchClause: Nonterminal = grammar.catchClause.nonterm

  private val catchFormalParameter: Nonterminal = grammar.catchFormalParameter.nonterm

  private val catchType: Nonterminal = grammar.catchType.nonterm

  private val `finally`: Nonterminal = grammar.finally.nonterm

  private val tryWithResourcesStatement: Nonterminal = grammar.tryWithResourcesStatement.nonterm

  private val resourceSpecification: Nonterminal = grammar.resourceSpecification.nonterm

  private val resourceList: Nonterminal = grammar.resourceList.nonterm

  private val resource: Nonterminal = grammar.resource.nonterm

  private val primaryNoNewArray: Nonterminal = grammar.primaryNoNewArray.nonterm

  private val classLiteral: Nonterminal = grammar.classLiteral.nonterm

  private val classOrInterfaceTypeToInstantiate: Nonterminal =
      grammar.classOrInterfaceTypeToInstantiate.nonterm

  private val unqualifiedClassInstanceCreationExpression: Nonterminal =
      grammar.unqualifiedClassInstanceCreationExpression.nonterm

  private val classInstanceCreationExpression: Nonterminal =
      grammar.classInstanceCreationExpression.nonterm

  private val fieldAccess: Nonterminal = grammar.fieldAccess.nonterm

  private val typeArgumentsOrDiamond: Nonterminal = grammar.typeArgumentsOrDiamond.nonterm

  private val arrayAccess: Nonterminal = grammar.arrayAccess.nonterm

  private val methodInvocation: Nonterminal = grammar.methodInvocation.nonterm

  private val methodReference: Nonterminal = grammar.methodReference.nonterm

  private val arrayCreationExpression: Nonterminal = grammar.arrayCreationExpression.nonterm

  private val dimExprs: Nonterminal = grammar.dimExprs.nonterm

  private val dimExpr: Nonterminal = grammar.dimExpr.nonterm

  private val lambdaExpression: Nonterminal = grammar.lambdaExpression.nonterm

  private val lambdaParameters: Nonterminal = grammar.lambdaParameters.nonterm

  private val inferredFormalParameterList: Nonterminal = grammar.inferredFormalParameterList.nonterm

  private val lambdaBody: Nonterminal = grammar.lambdaBody.nonterm

  private val assignmentExpression: Nonterminal = grammar.assignmentExpression.nonterm

  private val assignment: Nonterminal = grammar.assignment.nonterm

  private val leftHandSide: Nonterminal = grammar.leftHandSide.nonterm

  private val assignmentOperator: Nonterminal = grammar.assignmentOperator.nonterm

  private val conditionalOrExpression: Nonterminal = grammar.conditionalOrExpression.nonterm

  private val conditionalAndExpression: Nonterminal = grammar.conditionalAndExpression.nonterm

  private val inclusiveOrExpression: Nonterminal = grammar.inclusiveOrExpression.nonterm

  private val exclusiveOrExpression: Nonterminal = grammar.exclusiveOrExpression.nonterm

  private val andExpression: Nonterminal = grammar.andExpression.nonterm

  private val equalityExpression: Nonterminal = grammar.equalityExpression.nonterm

  private val relationalExpression: Nonterminal = grammar.relationalExpression.nonterm

  private val shiftExpression: Nonterminal = grammar.shiftExpression.nonterm

  private val additiveExpression: Nonterminal = grammar.additiveExpression.nonterm

  private val multiplicativeExpression: Nonterminal = grammar.multiplicativeExpression.nonterm

  private val preIncrementExpression: Nonterminal = grammar.preIncrementExpression.nonterm

  private val preDecrementExpression: Nonterminal = grammar.preDecrementExpression.nonterm

  private val unaryExpressionNotPlusMinus: Nonterminal = grammar.unaryExpressionNotPlusMinus.nonterm

  private val unaryExpression: Nonterminal = grammar.unaryExpression.nonterm

  private val postfixExpression: Nonterminal = grammar.postfixExpression.nonterm

  private val postIncrementExpression: Nonterminal = grammar.postIncrementExpression.nonterm

  private val postDecrementExpression: Nonterminal = grammar.postDecrementExpression.nonterm

  private val castExpression: Nonterminal = grammar.castExpression.nonterm

  private val constantExpression: Nonterminal = grammar.constantExpression.nonterm

  override fun callNtFuncs(
    nt: Nonterminal,
    descriptor: Descriptor<VertexType>,
    curSppfNode: SppfNode<VertexType>?,
  ) {
    when(nt.name) {
      "compilationUnit" -> parsecompilationUnit(descriptor, curSppfNode)
      "identifier" -> parseidentifier(descriptor, curSppfNode)
      "literal" -> parseliteral(descriptor, curSppfNode)
      "type" -> parsetype(descriptor, curSppfNode)
      "primitiveType" -> parseprimitiveType(descriptor, curSppfNode)
      "referenceType" -> parsereferenceType(descriptor, curSppfNode)
      "annotation" -> parseannotation(descriptor, curSppfNode)
      "numericType" -> parsenumericType(descriptor, curSppfNode)
      "integralType" -> parseintegralType(descriptor, curSppfNode)
      "floatingPointType" -> parsefloatingPointType(descriptor, curSppfNode)
      "classOrInterfaceType" -> parseclassOrInterfaceType(descriptor, curSppfNode)
      "typeVariable" -> parsetypeVariable(descriptor, curSppfNode)
      "arrayType" -> parsearrayType(descriptor, curSppfNode)
      "classType" -> parseclassType(descriptor, curSppfNode)
      "interfaceType" -> parseinterfaceType(descriptor, curSppfNode)
      "typeArguments" -> parsetypeArguments(descriptor, curSppfNode)
      "dims" -> parsedims(descriptor, curSppfNode)
      "typeParameter" -> parsetypeParameter(descriptor, curSppfNode)
      "typeParameterModifier" -> parsetypeParameterModifier(descriptor, curSppfNode)
      "typeBound" -> parsetypeBound(descriptor, curSppfNode)
      "additionalBound" -> parseadditionalBound(descriptor, curSppfNode)
      "typeArgumentList" -> parsetypeArgumentList(descriptor, curSppfNode)
      "typeArgument" -> parsetypeArgument(descriptor, curSppfNode)
      "wildcard" -> parsewildcard(descriptor, curSppfNode)
      "wildcardBounds" -> parsewildcardBounds(descriptor, curSppfNode)
      "typeName" -> parsetypeName(descriptor, curSppfNode)
      "packageOrTypeName" -> parsepackageOrTypeName(descriptor, curSppfNode)
      "expressionName" -> parseexpressionName(descriptor, curSppfNode)
      "ambiguousName" -> parseambiguousName(descriptor, curSppfNode)
      "methodName" -> parsemethodName(descriptor, curSppfNode)
      "packageName" -> parsepackageName(descriptor, curSppfNode)
      "result" -> parseresult(descriptor, curSppfNode)
      "packageDeclaration" -> parsepackageDeclaration(descriptor, curSppfNode)
      "importDeclaration" -> parseimportDeclaration(descriptor, curSppfNode)
      "typeDeclaration" -> parsetypeDeclaration(descriptor, curSppfNode)
      "packageModifier" -> parsepackageModifier(descriptor, curSppfNode)
      "singleTypeImportDeclaration" -> parsesingleTypeImportDeclaration(descriptor, curSppfNode)
      "typeImportOnDemandDeclaration" -> parsetypeImportOnDemandDeclaration(descriptor, curSppfNode)
      "singleStaticImportDeclaration" -> parsesingleStaticImportDeclaration(descriptor, curSppfNode)
      "staticImportOnDemandDeclaration" -> parsestaticImportOnDemandDeclaration(descriptor,
          curSppfNode)
      "classDeclaration" -> parseclassDeclaration(descriptor, curSppfNode)
      "interfaceDeclaration" -> parseinterfaceDeclaration(descriptor, curSppfNode)
      "throws" -> parsethrows(descriptor, curSppfNode)
      "normalClassDeclaration" -> parsenormalClassDeclaration(descriptor, curSppfNode)
      "enumDeclaration" -> parseenumDeclaration(descriptor, curSppfNode)
      "classModifier" -> parseclassModifier(descriptor, curSppfNode)
      "typeParameters" -> parsetypeParameters(descriptor, curSppfNode)
      "superclass" -> parsesuperclass(descriptor, curSppfNode)
      "superinterfaces" -> parsesuperinterfaces(descriptor, curSppfNode)
      "classBody" -> parseclassBody(descriptor, curSppfNode)
      "typeParameterList" -> parsetypeParameterList(descriptor, curSppfNode)
      "interfaceTypeList" -> parseinterfaceTypeList(descriptor, curSppfNode)
      "classBodyDeclaration" -> parseclassBodyDeclaration(descriptor, curSppfNode)
      "classMemberDeclaration" -> parseclassMemberDeclaration(descriptor, curSppfNode)
      "instanceInitializer" -> parseinstanceInitializer(descriptor, curSppfNode)
      "staticInitializer" -> parsestaticInitializer(descriptor, curSppfNode)
      "constructorDeclaration" -> parseconstructorDeclaration(descriptor, curSppfNode)
      "fieldDeclaration" -> parsefieldDeclaration(descriptor, curSppfNode)
      "methodDeclaration" -> parsemethodDeclaration(descriptor, curSppfNode)
      "fieldModifier" -> parsefieldModifier(descriptor, curSppfNode)
      "unannType" -> parseunannType(descriptor, curSppfNode)
      "variableDeclaratorList" -> parsevariableDeclaratorList(descriptor, curSppfNode)
      "variableDeclarator" -> parsevariableDeclarator(descriptor, curSppfNode)
      "variableDeclaratorId" -> parsevariableDeclaratorId(descriptor, curSppfNode)
      "variableInitializer" -> parsevariableInitializer(descriptor, curSppfNode)
      "expression" -> parseexpression(descriptor, curSppfNode)
      "arrayInitializer" -> parsearrayInitializer(descriptor, curSppfNode)
      "unannPrimitiveType" -> parseunannPrimitiveType(descriptor, curSppfNode)
      "unannReferenceType" -> parseunannReferenceType(descriptor, curSppfNode)
      "unannClassOrInterfaceType" -> parseunannClassOrInterfaceType(descriptor, curSppfNode)
      "unannTypeVariable" -> parseunannTypeVariable(descriptor, curSppfNode)
      "unannArrayType" -> parseunannArrayType(descriptor, curSppfNode)
      "unannClassType" -> parseunannClassType(descriptor, curSppfNode)
      "unannInterfaceType" -> parseunannInterfaceType(descriptor, curSppfNode)
      "methodModifier" -> parsemethodModifier(descriptor, curSppfNode)
      "methodHeader" -> parsemethodHeader(descriptor, curSppfNode)
      "methodBody" -> parsemethodBody(descriptor, curSppfNode)
      "methodDeclarator" -> parsemethodDeclarator(descriptor, curSppfNode)
      "formalParameterList" -> parseformalParameterList(descriptor, curSppfNode)
      "receiverParameter" -> parsereceiverParameter(descriptor, curSppfNode)
      "formalParameters" -> parseformalParameters(descriptor, curSppfNode)
      "lastFormalParameter" -> parselastFormalParameter(descriptor, curSppfNode)
      "formalParameter" -> parseformalParameter(descriptor, curSppfNode)
      "variableModifier" -> parsevariableModifier(descriptor, curSppfNode)
      "exceptionTypeList" -> parseexceptionTypeList(descriptor, curSppfNode)
      "exceptionType" -> parseexceptionType(descriptor, curSppfNode)
      "block" -> parseblock(descriptor, curSppfNode)
      "constructorModifier" -> parseconstructorModifier(descriptor, curSppfNode)
      "constructorDeclarator" -> parseconstructorDeclarator(descriptor, curSppfNode)
      "constructorBody" -> parseconstructorBody(descriptor, curSppfNode)
      "simpleTypeName" -> parsesimpleTypeName(descriptor, curSppfNode)
      "explicitConstructorInvocation" -> parseexplicitConstructorInvocation(descriptor, curSppfNode)
      "enumBody" -> parseenumBody(descriptor, curSppfNode)
      "enumConstantList" -> parseenumConstantList(descriptor, curSppfNode)
      "enumConstant" -> parseenumConstant(descriptor, curSppfNode)
      "enumConstantModifier" -> parseenumConstantModifier(descriptor, curSppfNode)
      "enumBodyDeclarations" -> parseenumBodyDeclarations(descriptor, curSppfNode)
      "blockStatements" -> parseblockStatements(descriptor, curSppfNode)
      "argumentList" -> parseargumentList(descriptor, curSppfNode)
      "primary" -> parseprimary(descriptor, curSppfNode)
      "normalInterfaceDeclaration" -> parsenormalInterfaceDeclaration(descriptor, curSppfNode)
      "interfaceModifier" -> parseinterfaceModifier(descriptor, curSppfNode)
      "extendsInterfaces" -> parseextendsInterfaces(descriptor, curSppfNode)
      "interfaceBody" -> parseinterfaceBody(descriptor, curSppfNode)
      "interfaceMemberDeclaration" -> parseinterfaceMemberDeclaration(descriptor, curSppfNode)
      "constantDeclaration" -> parseconstantDeclaration(descriptor, curSppfNode)
      "constantModifier" -> parseconstantModifier(descriptor, curSppfNode)
      "annotationTypeDeclaration" -> parseannotationTypeDeclaration(descriptor, curSppfNode)
      "annotationTypeBody" -> parseannotationTypeBody(descriptor, curSppfNode)
      "annotationTypeMemberDeclaration" -> parseannotationTypeMemberDeclaration(descriptor,
          curSppfNode)
      "annotationTypeElementDeclaration" -> parseannotationTypeElementDeclaration(descriptor,
          curSppfNode)
      "defaultValue" -> parsedefaultValue(descriptor, curSppfNode)
      "normalAnnotation" -> parsenormalAnnotation(descriptor, curSppfNode)
      "elementValuePairList" -> parseelementValuePairList(descriptor, curSppfNode)
      "elementValuePair" -> parseelementValuePair(descriptor, curSppfNode)
      "elementValue" -> parseelementValue(descriptor, curSppfNode)
      "elementValueArrayInitializer" -> parseelementValueArrayInitializer(descriptor, curSppfNode)
      "elementValueList" -> parseelementValueList(descriptor, curSppfNode)
      "markerAnnotation" -> parsemarkerAnnotation(descriptor, curSppfNode)
      "singleElementAnnotation" -> parsesingleElementAnnotation(descriptor, curSppfNode)
      "interfaceMethodDeclaration" -> parseinterfaceMethodDeclaration(descriptor, curSppfNode)
      "annotationTypeElementModifier" -> parseannotationTypeElementModifier(descriptor, curSppfNode)
      "conditionalExpression" -> parseconditionalExpression(descriptor, curSppfNode)
      "variableInitializerList" -> parsevariableInitializerList(descriptor, curSppfNode)
      "blockStatement" -> parseblockStatement(descriptor, curSppfNode)
      "localVariableDeclarationStatement" -> parselocalVariableDeclarationStatement(descriptor,
          curSppfNode)
      "localVariableDeclaration" -> parselocalVariableDeclaration(descriptor, curSppfNode)
      "statement" -> parsestatement(descriptor, curSppfNode)
      "statementNoShortIf" -> parsestatementNoShortIf(descriptor, curSppfNode)
      "statementWithoutTrailingSubstatement" ->
          parsestatementWithoutTrailingSubstatement(descriptor, curSppfNode)
      "emptyStatement" -> parseemptyStatement(descriptor, curSppfNode)
      "labeledStatement" -> parselabeledStatement(descriptor, curSppfNode)
      "labeledStatementNoShortIf" -> parselabeledStatementNoShortIf(descriptor, curSppfNode)
      "expressionStatement" -> parseexpressionStatement(descriptor, curSppfNode)
      "statementExpression" -> parsestatementExpression(descriptor, curSppfNode)
      "ifThenStatement" -> parseifThenStatement(descriptor, curSppfNode)
      "ifThenElseStatement" -> parseifThenElseStatement(descriptor, curSppfNode)
      "ifThenElseStatementNoShortIf" -> parseifThenElseStatementNoShortIf(descriptor, curSppfNode)
      "assertStatement" -> parseassertStatement(descriptor, curSppfNode)
      "switchStatement" -> parseswitchStatement(descriptor, curSppfNode)
      "switchBlock" -> parseswitchBlock(descriptor, curSppfNode)
      "switchBlockStatementGroup" -> parseswitchBlockStatementGroup(descriptor, curSppfNode)
      "switchLabels" -> parseswitchLabels(descriptor, curSppfNode)
      "switchLabel" -> parseswitchLabel(descriptor, curSppfNode)
      "enumConstantName" -> parseenumConstantName(descriptor, curSppfNode)
      "whileStatement" -> parsewhileStatement(descriptor, curSppfNode)
      "whileStatementNoShortIf" -> parsewhileStatementNoShortIf(descriptor, curSppfNode)
      "doStatement" -> parsedoStatement(descriptor, curSppfNode)
      "interfaceMethodModifier" -> parseinterfaceMethodModifier(descriptor, curSppfNode)
      "forStatement" -> parseforStatement(descriptor, curSppfNode)
      "forStatementNoShortIf" -> parseforStatementNoShortIf(descriptor, curSppfNode)
      "basicForStatement" -> parsebasicForStatement(descriptor, curSppfNode)
      "basicForStatementNoShortIf" -> parsebasicForStatementNoShortIf(descriptor, curSppfNode)
      "forInit" -> parseforInit(descriptor, curSppfNode)
      "forUpdate" -> parseforUpdate(descriptor, curSppfNode)
      "statementExpressionList" -> parsestatementExpressionList(descriptor, curSppfNode)
      "enhancedForStatement" -> parseenhancedForStatement(descriptor, curSppfNode)
      "enhancedForStatementNoShortIf" -> parseenhancedForStatementNoShortIf(descriptor, curSppfNode)
      "breakStatement" -> parsebreakStatement(descriptor, curSppfNode)
      "continueStatement" -> parsecontinueStatement(descriptor, curSppfNode)
      "returnStatement" -> parsereturnStatement(descriptor, curSppfNode)
      "throwStatement" -> parsethrowStatement(descriptor, curSppfNode)
      "synchronizedStatement" -> parsesynchronizedStatement(descriptor, curSppfNode)
      "tryStatement" -> parsetryStatement(descriptor, curSppfNode)
      "catches" -> parsecatches(descriptor, curSppfNode)
      "catchClause" -> parsecatchClause(descriptor, curSppfNode)
      "catchFormalParameter" -> parsecatchFormalParameter(descriptor, curSppfNode)
      "catchType" -> parsecatchType(descriptor, curSppfNode)
      "finally" -> parsefinally(descriptor, curSppfNode)
      "tryWithResourcesStatement" -> parsetryWithResourcesStatement(descriptor, curSppfNode)
      "resourceSpecification" -> parseresourceSpecification(descriptor, curSppfNode)
      "resourceList" -> parseresourceList(descriptor, curSppfNode)
      "resource" -> parseresource(descriptor, curSppfNode)
      "primaryNoNewArray" -> parseprimaryNoNewArray(descriptor, curSppfNode)
      "classLiteral" -> parseclassLiteral(descriptor, curSppfNode)
      "classOrInterfaceTypeToInstantiate" -> parseclassOrInterfaceTypeToInstantiate(descriptor,
          curSppfNode)
      "unqualifiedClassInstanceCreationExpression" ->
          parseunqualifiedClassInstanceCreationExpression(descriptor, curSppfNode)
      "classInstanceCreationExpression" -> parseclassInstanceCreationExpression(descriptor,
          curSppfNode)
      "fieldAccess" -> parsefieldAccess(descriptor, curSppfNode)
      "typeArgumentsOrDiamond" -> parsetypeArgumentsOrDiamond(descriptor, curSppfNode)
      "arrayAccess" -> parsearrayAccess(descriptor, curSppfNode)
      "methodInvocation" -> parsemethodInvocation(descriptor, curSppfNode)
      "methodReference" -> parsemethodReference(descriptor, curSppfNode)
      "arrayCreationExpression" -> parsearrayCreationExpression(descriptor, curSppfNode)
      "dimExprs" -> parsedimExprs(descriptor, curSppfNode)
      "dimExpr" -> parsedimExpr(descriptor, curSppfNode)
      "lambdaExpression" -> parselambdaExpression(descriptor, curSppfNode)
      "lambdaParameters" -> parselambdaParameters(descriptor, curSppfNode)
      "inferredFormalParameterList" -> parseinferredFormalParameterList(descriptor, curSppfNode)
      "lambdaBody" -> parselambdaBody(descriptor, curSppfNode)
      "assignmentExpression" -> parseassignmentExpression(descriptor, curSppfNode)
      "assignment" -> parseassignment(descriptor, curSppfNode)
      "leftHandSide" -> parseleftHandSide(descriptor, curSppfNode)
      "assignmentOperator" -> parseassignmentOperator(descriptor, curSppfNode)
      "conditionalOrExpression" -> parseconditionalOrExpression(descriptor, curSppfNode)
      "conditionalAndExpression" -> parseconditionalAndExpression(descriptor, curSppfNode)
      "inclusiveOrExpression" -> parseinclusiveOrExpression(descriptor, curSppfNode)
      "exclusiveOrExpression" -> parseexclusiveOrExpression(descriptor, curSppfNode)
      "andExpression" -> parseandExpression(descriptor, curSppfNode)
      "equalityExpression" -> parseequalityExpression(descriptor, curSppfNode)
      "relationalExpression" -> parserelationalExpression(descriptor, curSppfNode)
      "shiftExpression" -> parseshiftExpression(descriptor, curSppfNode)
      "additiveExpression" -> parseadditiveExpression(descriptor, curSppfNode)
      "multiplicativeExpression" -> parsemultiplicativeExpression(descriptor, curSppfNode)
      "preIncrementExpression" -> parsepreIncrementExpression(descriptor, curSppfNode)
      "preDecrementExpression" -> parsepreDecrementExpression(descriptor, curSppfNode)
      "unaryExpressionNotPlusMinus" -> parseunaryExpressionNotPlusMinus(descriptor, curSppfNode)
      "unaryExpression" -> parseunaryExpression(descriptor, curSppfNode)
      "postfixExpression" -> parsepostfixExpression(descriptor, curSppfNode)
      "postIncrementExpression" -> parsepostIncrementExpression(descriptor, curSppfNode)
      "postDecrementExpression" -> parsepostDecrementExpression(descriptor, curSppfNode)
      "castExpression" -> parsecastExpression(descriptor, curSppfNode)
      "constantExpression" -> parseconstantExpression(descriptor, curSppfNode)
    }
  }

  private fun parsecompilationUnit(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, packageDeclaration,
            state.nonterminalEdges[packageDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, typeDeclaration,
            state.nonterminalEdges[typeDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, importDeclaration,
            state.nonterminalEdges[importDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeDeclaration,
            state.nonterminalEdges[typeDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, importDeclaration,
            state.nonterminalEdges[importDeclaration]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeDeclaration,
            state.nonterminalEdges[typeDeclaration]!!, curSppfNode)
      }
    }
  }

  private fun parseidentifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.IDENTIFIER -> 
            handleTerminal(JavaToken.IDENTIFIER, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
      }
    }
  }

  private fun parseliteral(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.FLOATING_POINT_LITERAL -> 
            handleTerminal(JavaToken.FLOATING_POINT_LITERAL, state, inputEdge, descriptor,
                curSppfNode)
            JavaToken.BOOLEAN_LITERAL -> 
            handleTerminal(JavaToken.BOOLEAN_LITERAL, state, inputEdge, descriptor, curSppfNode)
            JavaToken.CHARACTER_LITERAL -> 
            handleTerminal(JavaToken.CHARACTER_LITERAL, state, inputEdge, descriptor, curSppfNode)
            JavaToken.INTEGER_LITERAL -> 
            handleTerminal(JavaToken.INTEGER_LITERAL, state, inputEdge, descriptor, curSppfNode)
            JavaToken.STRING_LITERAL -> 
            handleTerminal(JavaToken.STRING_LITERAL, state, inputEdge, descriptor, curSppfNode)
            JavaToken.NULL_LITERAL -> 
            handleTerminal(JavaToken.NULL_LITERAL, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
      }
    }
  }

  private fun parsetype(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, referenceType, state.nonterminalEdges[referenceType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, primitiveType, state.nonterminalEdges[primitiveType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseprimitiveType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BOOLEAN -> 
            handleTerminal(JavaToken.BOOLEAN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, numericType, state.nonterminalEdges[numericType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsereferenceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeVariable, state.nonterminalEdges[typeVariable]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, arrayType, state.nonterminalEdges[arrayType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, classOrInterfaceType,
            state.nonterminalEdges[classOrInterfaceType]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseannotation(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, singleElementAnnotation,
            state.nonterminalEdges[singleElementAnnotation]!!, curSppfNode)
        handleNonterminalEdge(descriptor, normalAnnotation,
            state.nonterminalEdges[normalAnnotation]!!, curSppfNode)
        handleNonterminalEdge(descriptor, markerAnnotation,
            state.nonterminalEdges[markerAnnotation]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsenumericType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, floatingPointType,
            state.nonterminalEdges[floatingPointType]!!, curSppfNode)
        handleNonterminalEdge(descriptor, integralType, state.nonterminalEdges[integralType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseintegralType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CHAR -> 
            handleTerminal(JavaToken.CHAR, state, inputEdge, descriptor, curSppfNode)
            JavaToken.BYTE -> 
            handleTerminal(JavaToken.BYTE, state, inputEdge, descriptor, curSppfNode)
            JavaToken.INT -> 
            handleTerminal(JavaToken.INT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.LONG -> 
            handleTerminal(JavaToken.LONG, state, inputEdge, descriptor, curSppfNode)
            JavaToken.SHORT -> 
            handleTerminal(JavaToken.SHORT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
      }
    }
  }

  private fun parsefloatingPointType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOUBLE -> 
            handleTerminal(JavaToken.DOUBLE, state, inputEdge, descriptor, curSppfNode)
            JavaToken.FLOAT -> 
            handleTerminal(JavaToken.FLOAT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
      }
    }
  }

  private fun parseclassOrInterfaceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceType, state.nonterminalEdges[interfaceType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, classType, state.nonterminalEdges[classType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsetypeVariable(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsearrayType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeVariable, state.nonterminalEdges[typeVariable]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, classOrInterfaceType,
            state.nonterminalEdges[classOrInterfaceType]!!, curSppfNode)
        handleNonterminalEdge(descriptor, primitiveType, state.nonterminalEdges[primitiveType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, dims, state.nonterminalEdges[dims]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseclassType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, classOrInterfaceType,
            state.nonterminalEdges[classOrInterfaceType]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArguments, state.nonterminalEdges[typeArguments]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
    }
  }

  private fun parseinterfaceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classType, state.nonterminalEdges[classType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsetypeArguments(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LT -> 
            handleTerminal(JavaToken.LT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArgumentList,
            state.nonterminalEdges[typeArgumentList]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.GT -> 
            handleTerminal(JavaToken.GT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parsedims(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACK -> 
            handleTerminal(JavaToken.LBRACK, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACK -> 
            handleTerminal(JavaToken.RBRACK, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACK -> 
            handleTerminal(JavaToken.LBRACK, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
    }
  }

  private fun parsetypeParameter(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, typeParameterModifier,
            state.nonterminalEdges[typeParameterModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeBound, state.nonterminalEdges[typeBound]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsetypeParameterModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsetypeBound(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.EXTENDS -> 
            handleTerminal(JavaToken.EXTENDS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeVariable, state.nonterminalEdges[typeVariable]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, classOrInterfaceType,
            state.nonterminalEdges[classOrInterfaceType]!!, curSppfNode)
      }
      2 -> 
       {
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, additionalBound,
            state.nonterminalEdges[additionalBound]!!, curSppfNode)
      }
    }
  }

  private fun parseadditionalBound(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.AND -> 
            handleTerminal(JavaToken.AND, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceType, state.nonterminalEdges[interfaceType]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsetypeArgumentList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArgument, state.nonterminalEdges[typeArgument]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parsetypeArgument(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, referenceType, state.nonterminalEdges[referenceType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, wildcard, state.nonterminalEdges[wildcard]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsewildcard(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.QUESTION -> 
            handleTerminal(JavaToken.QUESTION, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, wildcardBounds, state.nonterminalEdges[wildcardBounds]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsewildcardBounds(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SUPER -> 
            handleTerminal(JavaToken.SUPER, state, inputEdge, descriptor, curSppfNode)
            JavaToken.EXTENDS -> 
            handleTerminal(JavaToken.EXTENDS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, referenceType, state.nonterminalEdges[referenceType]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsetypeName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, packageOrTypeName,
            state.nonterminalEdges[packageOrTypeName]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parsepackageOrTypeName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, packageOrTypeName,
            state.nonterminalEdges[packageOrTypeName]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parseexpressionName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ambiguousName, state.nonterminalEdges[ambiguousName]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parseambiguousName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ambiguousName, state.nonterminalEdges[ambiguousName]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parsemethodName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsepackageName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, packageName, state.nonterminalEdges[packageName]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parseresult(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.VOID -> 
            handleTerminal(JavaToken.VOID, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsepackageDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PACKAGE -> 
            handleTerminal(JavaToken.PACKAGE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, packageModifier,
            state.nonterminalEdges[packageModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parseimportDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, singleTypeImportDeclaration,
            state.nonterminalEdges[singleTypeImportDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, staticImportOnDemandDeclaration,
            state.nonterminalEdges[staticImportOnDemandDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, singleStaticImportDeclaration,
            state.nonterminalEdges[singleStaticImportDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, typeImportOnDemandDeclaration,
            state.nonterminalEdges[typeImportOnDemandDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsetypeDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceDeclaration,
            state.nonterminalEdges[interfaceDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, classDeclaration,
            state.nonterminalEdges[classDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsepackageModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsesingleTypeImportDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.IMPORT -> 
            handleTerminal(JavaToken.IMPORT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeName, state.nonterminalEdges[typeName]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parsetypeImportOnDemandDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.IMPORT -> 
            handleTerminal(JavaToken.IMPORT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, packageOrTypeName,
            state.nonterminalEdges[packageOrTypeName]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.MULT -> 
            handleTerminal(JavaToken.MULT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
      }
    }
  }

  private fun parsesingleStaticImportDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.IMPORT -> 
            handleTerminal(JavaToken.IMPORT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.STATIC -> 
            handleTerminal(JavaToken.STATIC, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeName, state.nonterminalEdges[typeName]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      6 -> 
       {
      }
    }
  }

  private fun parsestaticImportOnDemandDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.IMPORT -> 
            handleTerminal(JavaToken.IMPORT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.STATIC -> 
            handleTerminal(JavaToken.STATIC, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeName, state.nonterminalEdges[typeName]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.MULT -> 
            handleTerminal(JavaToken.MULT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      6 -> 
       {
      }
    }
  }

  private fun parseclassDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, enumDeclaration,
            state.nonterminalEdges[enumDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, normalClassDeclaration,
            state.nonterminalEdges[normalClassDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseinterfaceDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotationTypeDeclaration,
            state.nonterminalEdges[annotationTypeDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, normalInterfaceDeclaration,
            state.nonterminalEdges[normalInterfaceDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsethrows(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.THROWS -> 
            handleTerminal(JavaToken.THROWS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, exceptionTypeList,
            state.nonterminalEdges[exceptionTypeList]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsenormalClassDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CLASS -> 
            handleTerminal(JavaToken.CLASS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classModifier, state.nonterminalEdges[classModifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, superinterfaces,
            state.nonterminalEdges[superinterfaces]!!, curSppfNode)
        handleNonterminalEdge(descriptor, superclass, state.nonterminalEdges[superclass]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, classBody, state.nonterminalEdges[classBody]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, typeParameters, state.nonterminalEdges[typeParameters]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, superinterfaces,
            state.nonterminalEdges[superinterfaces]!!, curSppfNode)
        handleNonterminalEdge(descriptor, superclass, state.nonterminalEdges[superclass]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, classBody, state.nonterminalEdges[classBody]!!,
            curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, superinterfaces,
            state.nonterminalEdges[superinterfaces]!!, curSppfNode)
        handleNonterminalEdge(descriptor, classBody, state.nonterminalEdges[classBody]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classBody, state.nonterminalEdges[classBody]!!,
            curSppfNode)
      }
      6 -> 
       {
      }
    }
  }

  private fun parseenumDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ENUM -> 
            handleTerminal(JavaToken.ENUM, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classModifier, state.nonterminalEdges[classModifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, superinterfaces,
            state.nonterminalEdges[superinterfaces]!!, curSppfNode)
        handleNonterminalEdge(descriptor, enumBody, state.nonterminalEdges[enumBody]!!, curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, enumBody, state.nonterminalEdges[enumBody]!!, curSppfNode)
      }
      4 -> 
       {
      }
    }
  }

  private fun parseclassModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.STATIC -> 
            handleTerminal(JavaToken.STATIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PUBLIC -> 
            handleTerminal(JavaToken.PUBLIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.ABSTRACT -> 
            handleTerminal(JavaToken.ABSTRACT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.FINAL -> 
            handleTerminal(JavaToken.FINAL, state, inputEdge, descriptor, curSppfNode)
            JavaToken.STRICTFP -> 
            handleTerminal(JavaToken.STRICTFP, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PROTECTED -> 
            handleTerminal(JavaToken.PROTECTED, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PRIVATE -> 
            handleTerminal(JavaToken.PRIVATE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsetypeParameters(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LT -> 
            handleTerminal(JavaToken.LT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeParameterList,
            state.nonterminalEdges[typeParameterList]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.GT -> 
            handleTerminal(JavaToken.GT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parsesuperclass(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.EXTENDS -> 
            handleTerminal(JavaToken.EXTENDS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classType, state.nonterminalEdges[classType]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsesuperinterfaces(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.IMPLEMENTS -> 
            handleTerminal(JavaToken.IMPLEMENTS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceTypeList,
            state.nonterminalEdges[interfaceTypeList]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseclassBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACE -> 
            handleTerminal(JavaToken.LBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classBodyDeclaration,
            state.nonterminalEdges[classBodyDeclaration]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsetypeParameterList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeParameter, state.nonterminalEdges[typeParameter]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseinterfaceTypeList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceType, state.nonterminalEdges[interfaceType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseclassBodyDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classMemberDeclaration,
            state.nonterminalEdges[classMemberDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, staticInitializer,
            state.nonterminalEdges[staticInitializer]!!, curSppfNode)
        handleNonterminalEdge(descriptor, instanceInitializer,
            state.nonterminalEdges[instanceInitializer]!!, curSppfNode)
        handleNonterminalEdge(descriptor, constructorDeclaration,
            state.nonterminalEdges[constructorDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseclassMemberDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, methodDeclaration,
            state.nonterminalEdges[methodDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, interfaceDeclaration,
            state.nonterminalEdges[interfaceDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, fieldDeclaration,
            state.nonterminalEdges[fieldDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, classDeclaration,
            state.nonterminalEdges[classDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseinstanceInitializer(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, block, state.nonterminalEdges[block]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsestaticInitializer(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.STATIC -> 
            handleTerminal(JavaToken.STATIC, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, block, state.nonterminalEdges[block]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseconstructorDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, constructorDeclarator,
            state.nonterminalEdges[constructorDeclarator]!!, curSppfNode)
        handleNonterminalEdge(descriptor, constructorModifier,
            state.nonterminalEdges[constructorModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, throws, state.nonterminalEdges[throws]!!, curSppfNode)
        handleNonterminalEdge(descriptor, constructorBody,
            state.nonterminalEdges[constructorBody]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, constructorBody,
            state.nonterminalEdges[constructorBody]!!, curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parsefieldDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, fieldModifier, state.nonterminalEdges[fieldModifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableDeclaratorList,
            state.nonterminalEdges[variableDeclaratorList]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parsemethodDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, methodHeader, state.nonterminalEdges[methodHeader]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, methodModifier, state.nonterminalEdges[methodModifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, methodBody, state.nonterminalEdges[methodBody]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsefieldModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.TRANSIENT -> 
            handleTerminal(JavaToken.TRANSIENT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.VOLATILE -> 
            handleTerminal(JavaToken.VOLATILE, state, inputEdge, descriptor, curSppfNode)
            JavaToken.STATIC -> 
            handleTerminal(JavaToken.STATIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PUBLIC -> 
            handleTerminal(JavaToken.PUBLIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.FINAL -> 
            handleTerminal(JavaToken.FINAL, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PROTECTED -> 
            handleTerminal(JavaToken.PROTECTED, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PRIVATE -> 
            handleTerminal(JavaToken.PRIVATE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseunannType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unannReferenceType,
            state.nonterminalEdges[unannReferenceType]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannPrimitiveType,
            state.nonterminalEdges[unannPrimitiveType]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsevariableDeclaratorList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableDeclarator,
            state.nonterminalEdges[variableDeclarator]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parsevariableDeclarator(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableDeclaratorId,
            state.nonterminalEdges[variableDeclaratorId]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.EQ -> 
            handleTerminal(JavaToken.EQ, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableInitializer,
            state.nonterminalEdges[variableInitializer]!!, curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parsevariableDeclaratorId(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, dims, state.nonterminalEdges[dims]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsevariableInitializer(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, arrayInitializer,
            state.nonterminalEdges[arrayInitializer]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseexpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, lambdaExpression,
            state.nonterminalEdges[lambdaExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, assignmentExpression,
            state.nonterminalEdges[assignmentExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsearrayInitializer(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACE -> 
            handleTerminal(JavaToken.LBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableInitializerList,
            state.nonterminalEdges[variableInitializerList]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
    }
  }

  private fun parseunannPrimitiveType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BOOLEAN -> 
            handleTerminal(JavaToken.BOOLEAN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, numericType, state.nonterminalEdges[numericType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseunannReferenceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unannTypeVariable,
            state.nonterminalEdges[unannTypeVariable]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannArrayType, state.nonterminalEdges[unannArrayType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, unannClassOrInterfaceType,
            state.nonterminalEdges[unannClassOrInterfaceType]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseunannClassOrInterfaceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unannClassType, state.nonterminalEdges[unannClassType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, unannInterfaceType,
            state.nonterminalEdges[unannInterfaceType]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseunannTypeVariable(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseunannArrayType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unannTypeVariable,
            state.nonterminalEdges[unannTypeVariable]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannPrimitiveType,
            state.nonterminalEdges[unannPrimitiveType]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannClassOrInterfaceType,
            state.nonterminalEdges[unannClassOrInterfaceType]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, dims, state.nonterminalEdges[dims]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseunannClassType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, unannClassOrInterfaceType,
            state.nonterminalEdges[unannClassOrInterfaceType]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArguments, state.nonterminalEdges[typeArguments]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      4 -> 
       {
      }
    }
  }

  private fun parseunannInterfaceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unannClassType, state.nonterminalEdges[unannClassType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsemethodModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.NATIVE -> 
            handleTerminal(JavaToken.NATIVE, state, inputEdge, descriptor, curSppfNode)
            JavaToken.STATIC -> 
            handleTerminal(JavaToken.STATIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PUBLIC -> 
            handleTerminal(JavaToken.PUBLIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.ABSTRACT -> 
            handleTerminal(JavaToken.ABSTRACT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.FINAL -> 
            handleTerminal(JavaToken.FINAL, state, inputEdge, descriptor, curSppfNode)
            JavaToken.SYNCHRONIZED -> 
            handleTerminal(JavaToken.SYNCHRONIZED, state, inputEdge, descriptor, curSppfNode)
            JavaToken.STRICTFP -> 
            handleTerminal(JavaToken.STRICTFP, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PROTECTED -> 
            handleTerminal(JavaToken.PROTECTED, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PRIVATE -> 
            handleTerminal(JavaToken.PRIVATE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsemethodHeader(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, result, state.nonterminalEdges[result]!!, curSppfNode)
        handleNonterminalEdge(descriptor, typeParameters, state.nonterminalEdges[typeParameters]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, methodDeclarator,
            state.nonterminalEdges[methodDeclarator]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, result, state.nonterminalEdges[result]!!, curSppfNode)
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, throws, state.nonterminalEdges[throws]!!, curSppfNode)
      }
      4 -> 
       {
      }
    }
  }

  private fun parsemethodBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, block, state.nonterminalEdges[block]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsemethodDeclarator(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, formalParameterList,
            state.nonterminalEdges[formalParameterList]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, dims, state.nonterminalEdges[dims]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseformalParameterList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, receiverParameter,
            state.nonterminalEdges[receiverParameter]!!, curSppfNode)
        handleNonterminalEdge(descriptor, formalParameters,
            state.nonterminalEdges[formalParameters]!!, curSppfNode)
        handleNonterminalEdge(descriptor, lastFormalParameter,
            state.nonterminalEdges[lastFormalParameter]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, lastFormalParameter,
            state.nonterminalEdges[lastFormalParameter]!!, curSppfNode)
      }
    }
  }

  private fun parsereceiverParameter(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.THIS -> 
            handleTerminal(JavaToken.THIS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.THIS -> 
            handleTerminal(JavaToken.THIS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseformalParameters(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, receiverParameter,
            state.nonterminalEdges[receiverParameter]!!, curSppfNode)
        handleNonterminalEdge(descriptor, formalParameter,
            state.nonterminalEdges[formalParameter]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, formalParameter,
            state.nonterminalEdges[formalParameter]!!, curSppfNode)
      }
    }
  }

  private fun parselastFormalParameter(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableModifier,
            state.nonterminalEdges[variableModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, formalParameter,
            state.nonterminalEdges[formalParameter]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableModifier,
            state.nonterminalEdges[variableModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ELLIPSIS -> 
            handleTerminal(JavaToken.ELLIPSIS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableDeclaratorId,
            state.nonterminalEdges[variableDeclaratorId]!!, curSppfNode)
      }
    }
  }

  private fun parseformalParameter(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableModifier,
            state.nonterminalEdges[variableModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableDeclaratorId,
            state.nonterminalEdges[variableDeclaratorId]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsevariableModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.FINAL -> 
            handleTerminal(JavaToken.FINAL, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseexceptionTypeList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, exceptionType, state.nonterminalEdges[exceptionType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseexceptionType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeVariable, state.nonterminalEdges[typeVariable]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, classType, state.nonterminalEdges[classType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseblock(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACE -> 
            handleTerminal(JavaToken.LBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, blockStatements,
            state.nonterminalEdges[blockStatements]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parseconstructorModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PUBLIC -> 
            handleTerminal(JavaToken.PUBLIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PROTECTED -> 
            handleTerminal(JavaToken.PROTECTED, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PRIVATE -> 
            handleTerminal(JavaToken.PRIVATE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseconstructorDeclarator(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, simpleTypeName, state.nonterminalEdges[simpleTypeName]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, typeParameters, state.nonterminalEdges[typeParameters]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, simpleTypeName, state.nonterminalEdges[simpleTypeName]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, formalParameterList,
            state.nonterminalEdges[formalParameterList]!!, curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
      }
    }
  }

  private fun parseconstructorBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACE -> 
            handleTerminal(JavaToken.LBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, blockStatements,
            state.nonterminalEdges[blockStatements]!!, curSppfNode)
        handleNonterminalEdge(descriptor, explicitConstructorInvocation,
            state.nonterminalEdges[explicitConstructorInvocation]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, blockStatements,
            state.nonterminalEdges[blockStatements]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
    }
  }

  private fun parsesimpleTypeName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseexplicitConstructorInvocation(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.THIS -> 
            handleTerminal(JavaToken.THIS, state, inputEdge, descriptor, curSppfNode)
            JavaToken.SUPER -> 
            handleTerminal(JavaToken.SUPER, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArguments, state.nonterminalEdges[typeArguments]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, expressionName, state.nonterminalEdges[expressionName]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, primary, state.nonterminalEdges[primary]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.THIS -> 
            handleTerminal(JavaToken.THIS, state, inputEdge, descriptor, curSppfNode)
            JavaToken.SUPER -> 
            handleTerminal(JavaToken.SUPER, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SUPER -> 
            handleTerminal(JavaToken.SUPER, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArguments, state.nonterminalEdges[typeArguments]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SUPER -> 
            handleTerminal(JavaToken.SUPER, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      6 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, argumentList, state.nonterminalEdges[argumentList]!!,
            curSppfNode)
      }
      7 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      8 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      9 -> 
       {
      }
    }
  }

  private fun parseenumBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACE -> 
            handleTerminal(JavaToken.LBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, enumConstantList,
            state.nonterminalEdges[enumConstantList]!!, curSppfNode)
        handleNonterminalEdge(descriptor, enumBodyDeclarations,
            state.nonterminalEdges[enumBodyDeclarations]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, enumBodyDeclarations,
            state.nonterminalEdges[enumBodyDeclarations]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, enumBodyDeclarations,
            state.nonterminalEdges[enumBodyDeclarations]!!, curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
      }
    }
  }

  private fun parseenumConstantList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, enumConstant, state.nonterminalEdges[enumConstant]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseenumConstant(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, enumConstantModifier,
            state.nonterminalEdges[enumConstantModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, argumentList, state.nonterminalEdges[argumentList]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classBody, state.nonterminalEdges[classBody]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseenumConstantModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseenumBodyDeclarations(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classBodyDeclaration,
            state.nonterminalEdges[classBodyDeclaration]!!, curSppfNode)
      }
    }
  }

  private fun parseblockStatements(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, blockStatement, state.nonterminalEdges[blockStatement]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, blockStatement, state.nonterminalEdges[blockStatement]!!,
            curSppfNode)
      }
    }
  }

  private fun parseargumentList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseprimary(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, primaryNoNewArray,
            state.nonterminalEdges[primaryNoNewArray]!!, curSppfNode)
        handleNonterminalEdge(descriptor, arrayCreationExpression,
            state.nonterminalEdges[arrayCreationExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsenormalInterfaceDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.INTERFACE -> 
            handleTerminal(JavaToken.INTERFACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceModifier,
            state.nonterminalEdges[interfaceModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceBody, state.nonterminalEdges[interfaceBody]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, extendsInterfaces,
            state.nonterminalEdges[extendsInterfaces]!!, curSppfNode)
        handleNonterminalEdge(descriptor, typeParameters, state.nonterminalEdges[typeParameters]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceBody, state.nonterminalEdges[interfaceBody]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, extendsInterfaces,
            state.nonterminalEdges[extendsInterfaces]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceBody, state.nonterminalEdges[interfaceBody]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseinterfaceModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.STATIC -> 
            handleTerminal(JavaToken.STATIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PUBLIC -> 
            handleTerminal(JavaToken.PUBLIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.ABSTRACT -> 
            handleTerminal(JavaToken.ABSTRACT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.STRICTFP -> 
            handleTerminal(JavaToken.STRICTFP, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PROTECTED -> 
            handleTerminal(JavaToken.PROTECTED, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PRIVATE -> 
            handleTerminal(JavaToken.PRIVATE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseextendsInterfaces(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.EXTENDS -> 
            handleTerminal(JavaToken.EXTENDS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceTypeList,
            state.nonterminalEdges[interfaceTypeList]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseinterfaceBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACE -> 
            handleTerminal(JavaToken.LBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceMemberDeclaration,
            state.nonterminalEdges[interfaceMemberDeclaration]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseinterfaceMemberDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceDeclaration,
            state.nonterminalEdges[interfaceDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, constantDeclaration,
            state.nonterminalEdges[constantDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, interfaceMethodDeclaration,
            state.nonterminalEdges[interfaceMethodDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, classDeclaration,
            state.nonterminalEdges[classDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseconstantDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, constantModifier,
            state.nonterminalEdges[constantModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableDeclaratorList,
            state.nonterminalEdges[variableDeclaratorList]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parseconstantModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.STATIC -> 
            handleTerminal(JavaToken.STATIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PUBLIC -> 
            handleTerminal(JavaToken.PUBLIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.FINAL -> 
            handleTerminal(JavaToken.FINAL, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseannotationTypeDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.AT -> 
            handleTerminal(JavaToken.AT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceModifier,
            state.nonterminalEdges[interfaceModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.INTERFACE -> 
            handleTerminal(JavaToken.INTERFACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotationTypeBody,
            state.nonterminalEdges[annotationTypeBody]!!, curSppfNode)
      }
      4 -> 
       {
      }
    }
  }

  private fun parseannotationTypeBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACE -> 
            handleTerminal(JavaToken.LBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotationTypeMemberDeclaration,
            state.nonterminalEdges[annotationTypeMemberDeclaration]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseannotationTypeMemberDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, interfaceDeclaration,
            state.nonterminalEdges[interfaceDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, annotationTypeElementDeclaration,
            state.nonterminalEdges[annotationTypeElementDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, constantDeclaration,
            state.nonterminalEdges[constantDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, classDeclaration,
            state.nonterminalEdges[classDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseannotationTypeElementDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotationTypeElementModifier,
            state.nonterminalEdges[annotationTypeElementModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, dims, state.nonterminalEdges[dims]!!, curSppfNode)
        handleNonterminalEdge(descriptor, defaultValue, state.nonterminalEdges[defaultValue]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, defaultValue, state.nonterminalEdges[defaultValue]!!,
            curSppfNode)
      }
      6 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      7 -> 
       {
      }
    }
  }

  private fun parsedefaultValue(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DEFAULT -> 
            handleTerminal(JavaToken.DEFAULT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, elementValue, state.nonterminalEdges[elementValue]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsenormalAnnotation(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.AT -> 
            handleTerminal(JavaToken.AT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeName, state.nonterminalEdges[typeName]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, elementValuePairList,
            state.nonterminalEdges[elementValuePairList]!!, curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
      }
    }
  }

  private fun parseelementValuePairList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, elementValuePair,
            state.nonterminalEdges[elementValuePair]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseelementValuePair(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.EQ -> 
            handleTerminal(JavaToken.EQ, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, elementValue, state.nonterminalEdges[elementValue]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseelementValue(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, elementValueArrayInitializer,
            state.nonterminalEdges[elementValueArrayInitializer]!!, curSppfNode)
        handleNonterminalEdge(descriptor, conditionalExpression,
            state.nonterminalEdges[conditionalExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseelementValueArrayInitializer(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACE -> 
            handleTerminal(JavaToken.LBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, elementValueList,
            state.nonterminalEdges[elementValueList]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
    }
  }

  private fun parseelementValueList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, elementValue, state.nonterminalEdges[elementValue]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parsemarkerAnnotation(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.AT -> 
            handleTerminal(JavaToken.AT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeName, state.nonterminalEdges[typeName]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsesingleElementAnnotation(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.AT -> 
            handleTerminal(JavaToken.AT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeName, state.nonterminalEdges[typeName]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, elementValue, state.nonterminalEdges[elementValue]!!,
            curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
      }
    }
  }

  private fun parseinterfaceMethodDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, methodHeader, state.nonterminalEdges[methodHeader]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, interfaceMethodModifier,
            state.nonterminalEdges[interfaceMethodModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, methodBody, state.nonterminalEdges[methodBody]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseannotationTypeElementModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PUBLIC -> 
            handleTerminal(JavaToken.PUBLIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.ABSTRACT -> 
            handleTerminal(JavaToken.ABSTRACT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseconditionalExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, conditionalOrExpression,
            state.nonterminalEdges[conditionalOrExpression]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.QUESTION -> 
            handleTerminal(JavaToken.QUESTION, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COLON -> 
            handleTerminal(JavaToken.COLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, lambdaExpression,
            state.nonterminalEdges[lambdaExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, conditionalExpression,
            state.nonterminalEdges[conditionalExpression]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parsevariableInitializerList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableInitializer,
            state.nonterminalEdges[variableInitializer]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseblockStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, localVariableDeclarationStatement,
            state.nonterminalEdges[localVariableDeclarationStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, statement, state.nonterminalEdges[statement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, classDeclaration,
            state.nonterminalEdges[classDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parselocalVariableDeclarationStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, localVariableDeclaration,
            state.nonterminalEdges[localVariableDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
      }
    }
  }

  private fun parselocalVariableDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableModifier,
            state.nonterminalEdges[variableModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableDeclaratorList,
            state.nonterminalEdges[variableDeclaratorList]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsestatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, whileStatement, state.nonterminalEdges[whileStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, labeledStatement,
            state.nonterminalEdges[labeledStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, forStatement, state.nonterminalEdges[forStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, statementWithoutTrailingSubstatement,
            state.nonterminalEdges[statementWithoutTrailingSubstatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ifThenElseStatement,
            state.nonterminalEdges[ifThenElseStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ifThenStatement,
            state.nonterminalEdges[ifThenStatement]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsestatementNoShortIf(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, whileStatementNoShortIf,
            state.nonterminalEdges[whileStatementNoShortIf]!!, curSppfNode)
        handleNonterminalEdge(descriptor, labeledStatementNoShortIf,
            state.nonterminalEdges[labeledStatementNoShortIf]!!, curSppfNode)
        handleNonterminalEdge(descriptor, forStatementNoShortIf,
            state.nonterminalEdges[forStatementNoShortIf]!!, curSppfNode)
        handleNonterminalEdge(descriptor, statementWithoutTrailingSubstatement,
            state.nonterminalEdges[statementWithoutTrailingSubstatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ifThenElseStatementNoShortIf,
            state.nonterminalEdges[ifThenElseStatementNoShortIf]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsestatementWithoutTrailingSubstatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, emptyStatement, state.nonterminalEdges[emptyStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, tryStatement, state.nonterminalEdges[tryStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, doStatement, state.nonterminalEdges[doStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, throwStatement, state.nonterminalEdges[throwStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, assertStatement,
            state.nonterminalEdges[assertStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, breakStatement, state.nonterminalEdges[breakStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, block, state.nonterminalEdges[block]!!, curSppfNode)
        handleNonterminalEdge(descriptor, expressionStatement,
            state.nonterminalEdges[expressionStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, continueStatement,
            state.nonterminalEdges[continueStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, returnStatement,
            state.nonterminalEdges[returnStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, switchStatement,
            state.nonterminalEdges[switchStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, synchronizedStatement,
            state.nonterminalEdges[synchronizedStatement]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseemptyStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
      }
    }
  }

  private fun parselabeledStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COLON -> 
            handleTerminal(JavaToken.COLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statement, state.nonterminalEdges[statement]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parselabeledStatementNoShortIf(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COLON -> 
            handleTerminal(JavaToken.COLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statementNoShortIf,
            state.nonterminalEdges[statementNoShortIf]!!, curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseexpressionStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statementExpression,
            state.nonterminalEdges[statementExpression]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
      }
    }
  }

  private fun parsestatementExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classInstanceCreationExpression,
            state.nonterminalEdges[classInstanceCreationExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, assignment, state.nonterminalEdges[assignment]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, methodInvocation,
            state.nonterminalEdges[methodInvocation]!!, curSppfNode)
        handleNonterminalEdge(descriptor, preDecrementExpression,
            state.nonterminalEdges[preDecrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, preIncrementExpression,
            state.nonterminalEdges[preIncrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, postIncrementExpression,
            state.nonterminalEdges[postIncrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, postDecrementExpression,
            state.nonterminalEdges[postDecrementExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseifThenStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.IF -> 
            handleTerminal(JavaToken.IF, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statement, state.nonterminalEdges[statement]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseifThenElseStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.IF -> 
            handleTerminal(JavaToken.IF, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statementNoShortIf,
            state.nonterminalEdges[statementNoShortIf]!!, curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ELSE -> 
            handleTerminal(JavaToken.ELSE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      6 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statement, state.nonterminalEdges[statement]!!,
            curSppfNode)
      }
      7 -> 
       {
      }
    }
  }

  private fun parseifThenElseStatementNoShortIf(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.IF -> 
            handleTerminal(JavaToken.IF, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statementNoShortIf,
            state.nonterminalEdges[statementNoShortIf]!!, curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ELSE -> 
            handleTerminal(JavaToken.ELSE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      6 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statementNoShortIf,
            state.nonterminalEdges[statementNoShortIf]!!, curSppfNode)
      }
      7 -> 
       {
      }
    }
  }

  private fun parseassertStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ASSERT -> 
            handleTerminal(JavaToken.ASSERT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            JavaToken.COLON -> 
            handleTerminal(JavaToken.COLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseswitchStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SWITCH -> 
            handleTerminal(JavaToken.SWITCH, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, switchBlock, state.nonterminalEdges[switchBlock]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseswitchBlock(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACE -> 
            handleTerminal(JavaToken.LBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, switchBlockStatementGroup,
            state.nonterminalEdges[switchBlockStatementGroup]!!, curSppfNode)
        handleNonterminalEdge(descriptor, switchLabel, state.nonterminalEdges[switchLabel]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACE -> 
            handleTerminal(JavaToken.RBRACE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, switchLabel, state.nonterminalEdges[switchLabel]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseswitchBlockStatementGroup(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, switchLabels, state.nonterminalEdges[switchLabels]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, blockStatements,
            state.nonterminalEdges[blockStatements]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseswitchLabels(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, switchLabel, state.nonterminalEdges[switchLabel]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, switchLabel, state.nonterminalEdges[switchLabel]!!,
            curSppfNode)
      }
    }
  }

  private fun parseswitchLabel(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CASE -> 
            handleTerminal(JavaToken.CASE, state, inputEdge, descriptor, curSppfNode)
            JavaToken.DEFAULT -> 
            handleTerminal(JavaToken.DEFAULT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, enumConstantName,
            state.nonterminalEdges[enumConstantName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, constantExpression,
            state.nonterminalEdges[constantExpression]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COLON -> 
            handleTerminal(JavaToken.COLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parseenumConstantName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsewhileStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.WHILE -> 
            handleTerminal(JavaToken.WHILE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statement, state.nonterminalEdges[statement]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parsewhileStatementNoShortIf(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.WHILE -> 
            handleTerminal(JavaToken.WHILE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statementNoShortIf,
            state.nonterminalEdges[statementNoShortIf]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parsedoStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DO -> 
            handleTerminal(JavaToken.DO, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statement, state.nonterminalEdges[statement]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.WHILE -> 
            handleTerminal(JavaToken.WHILE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      6 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      7 -> 
       {
      }
    }
  }

  private fun parseinterfaceMethodModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DEFAULT -> 
            handleTerminal(JavaToken.DEFAULT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.STATIC -> 
            handleTerminal(JavaToken.STATIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PUBLIC -> 
            handleTerminal(JavaToken.PUBLIC, state, inputEdge, descriptor, curSppfNode)
            JavaToken.ABSTRACT -> 
            handleTerminal(JavaToken.ABSTRACT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.STRICTFP -> 
            handleTerminal(JavaToken.STRICTFP, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseforStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, enhancedForStatement,
            state.nonterminalEdges[enhancedForStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, basicForStatement,
            state.nonterminalEdges[basicForStatement]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseforStatementNoShortIf(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, basicForStatementNoShortIf,
            state.nonterminalEdges[basicForStatementNoShortIf]!!, curSppfNode)
        handleNonterminalEdge(descriptor, enhancedForStatementNoShortIf,
            state.nonterminalEdges[enhancedForStatementNoShortIf]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsebasicForStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.FOR -> 
            handleTerminal(JavaToken.FOR, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, forInit, state.nonterminalEdges[forInit]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, forUpdate, state.nonterminalEdges[forUpdate]!!,
            curSppfNode)
      }
      6 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      7 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      8 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statement, state.nonterminalEdges[statement]!!,
            curSppfNode)
      }
      9 -> 
       {
      }
    }
  }

  private fun parsebasicForStatementNoShortIf(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.FOR -> 
            handleTerminal(JavaToken.FOR, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, forInit, state.nonterminalEdges[forInit]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, forUpdate, state.nonterminalEdges[forUpdate]!!,
            curSppfNode)
      }
      6 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      7 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      8 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statementNoShortIf,
            state.nonterminalEdges[statementNoShortIf]!!, curSppfNode)
      }
      9 -> 
       {
      }
    }
  }

  private fun parseforInit(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, localVariableDeclaration,
            state.nonterminalEdges[localVariableDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, statementExpressionList,
            state.nonterminalEdges[statementExpressionList]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseforUpdate(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statementExpressionList,
            state.nonterminalEdges[statementExpressionList]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsestatementExpressionList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statementExpression,
            state.nonterminalEdges[statementExpression]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseenhancedForStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.FOR -> 
            handleTerminal(JavaToken.FOR, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableModifier,
            state.nonterminalEdges[variableModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableDeclaratorId,
            state.nonterminalEdges[variableDeclaratorId]!!, curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COLON -> 
            handleTerminal(JavaToken.COLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      6 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      7 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statement, state.nonterminalEdges[statement]!!,
            curSppfNode)
      }
      8 -> 
       {
      }
    }
  }

  private fun parseenhancedForStatementNoShortIf(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.FOR -> 
            handleTerminal(JavaToken.FOR, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableModifier,
            state.nonterminalEdges[variableModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableDeclaratorId,
            state.nonterminalEdges[variableDeclaratorId]!!, curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COLON -> 
            handleTerminal(JavaToken.COLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      6 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      7 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, statementNoShortIf,
            state.nonterminalEdges[statementNoShortIf]!!, curSppfNode)
      }
      8 -> 
       {
      }
    }
  }

  private fun parsebreakStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BREAK -> 
            handleTerminal(JavaToken.BREAK, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parsecontinueStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CONTINUE -> 
            handleTerminal(JavaToken.CONTINUE, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parsereturnStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RETURN -> 
            handleTerminal(JavaToken.RETURN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parsethrowStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.THROW -> 
            handleTerminal(JavaToken.THROW, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parsesynchronizedStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SYNCHRONIZED -> 
            handleTerminal(JavaToken.SYNCHRONIZED, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, block, state.nonterminalEdges[block]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parsetryStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.TRY -> 
            handleTerminal(JavaToken.TRY, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, tryWithResourcesStatement,
            state.nonterminalEdges[tryWithResourcesStatement]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, block, state.nonterminalEdges[block]!!, curSppfNode)
      }
      2 -> 
       {
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, finally, state.nonterminalEdges[finally]!!, curSppfNode)
        handleNonterminalEdge(descriptor, catches, state.nonterminalEdges[catches]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, finally, state.nonterminalEdges[finally]!!, curSppfNode)
      }
    }
  }

  private fun parsecatches(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, catchClause, state.nonterminalEdges[catchClause]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, catchClause, state.nonterminalEdges[catchClause]!!,
            curSppfNode)
      }
    }
  }

  private fun parsecatchClause(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CATCH -> 
            handleTerminal(JavaToken.CATCH, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, catchFormalParameter,
            state.nonterminalEdges[catchFormalParameter]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, block, state.nonterminalEdges[block]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parsecatchFormalParameter(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableModifier,
            state.nonterminalEdges[variableModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, catchType, state.nonterminalEdges[catchType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableDeclaratorId,
            state.nonterminalEdges[variableDeclaratorId]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsecatchType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unannClassType, state.nonterminalEdges[unannClassType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.OR -> 
            handleTerminal(JavaToken.OR, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classType, state.nonterminalEdges[classType]!!,
            curSppfNode)
      }
    }
  }

  private fun parsefinally(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.FINALLY -> 
            handleTerminal(JavaToken.FINALLY, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, block, state.nonterminalEdges[block]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsetryWithResourcesStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.TRY -> 
            handleTerminal(JavaToken.TRY, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, resourceSpecification,
            state.nonterminalEdges[resourceSpecification]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, block, state.nonterminalEdges[block]!!, curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, finally, state.nonterminalEdges[finally]!!, curSppfNode)
        handleNonterminalEdge(descriptor, catches, state.nonterminalEdges[catches]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, finally, state.nonterminalEdges[finally]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseresourceSpecification(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, resourceList, state.nonterminalEdges[resourceList]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.SEMICOLON -> 
            handleTerminal(JavaToken.SEMICOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
    }
  }

  private fun parseresourceList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, resource, state.nonterminalEdges[resource]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseresource(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableModifier,
            state.nonterminalEdges[variableModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unannType, state.nonterminalEdges[unannType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, variableDeclaratorId,
            state.nonterminalEdges[variableDeclaratorId]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.EQ -> 
            handleTerminal(JavaToken.EQ, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      4 -> 
       {
      }
    }
  }

  private fun parseprimaryNoNewArray(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.THIS -> 
            handleTerminal(JavaToken.THIS, state, inputEdge, descriptor, curSppfNode)
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classInstanceCreationExpression,
            state.nonterminalEdges[classInstanceCreationExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, methodInvocation,
            state.nonterminalEdges[methodInvocation]!!, curSppfNode)
        handleNonterminalEdge(descriptor, fieldAccess, state.nonterminalEdges[fieldAccess]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, methodReference,
            state.nonterminalEdges[methodReference]!!, curSppfNode)
        handleNonterminalEdge(descriptor, classLiteral, state.nonterminalEdges[classLiteral]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, typeName, state.nonterminalEdges[typeName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, arrayAccess, state.nonterminalEdges[arrayAccess]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, literal, state.nonterminalEdges[literal]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.THIS -> 
            handleTerminal(JavaToken.THIS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseclassLiteral(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BOOLEAN -> 
            handleTerminal(JavaToken.BOOLEAN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.VOID -> 
            handleTerminal(JavaToken.VOID, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeName, state.nonterminalEdges[typeName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, numericType, state.nonterminalEdges[numericType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACK -> 
            handleTerminal(JavaToken.LBRACK, state, inputEdge, descriptor, curSppfNode)
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CLASS -> 
            handleTerminal(JavaToken.CLASS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACK -> 
            handleTerminal(JavaToken.RBRACK, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseclassOrInterfaceTypeToInstantiate(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArgumentsOrDiamond,
            state.nonterminalEdges[typeArgumentsOrDiamond]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseunqualifiedClassInstanceCreationExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.NEW -> 
            handleTerminal(JavaToken.NEW, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArguments, state.nonterminalEdges[typeArguments]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, classOrInterfaceTypeToInstantiate,
            state.nonterminalEdges[classOrInterfaceTypeToInstantiate]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classOrInterfaceTypeToInstantiate,
            state.nonterminalEdges[classOrInterfaceTypeToInstantiate]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, argumentList, state.nonterminalEdges[argumentList]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      6 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classBody, state.nonterminalEdges[classBody]!!,
            curSppfNode)
      }
      7 -> 
       {
      }
    }
  }

  private fun parseclassInstanceCreationExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expressionName, state.nonterminalEdges[expressionName]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, unqualifiedClassInstanceCreationExpression,
            state.nonterminalEdges[unqualifiedClassInstanceCreationExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, primary, state.nonterminalEdges[primary]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unqualifiedClassInstanceCreationExpression,
            state.nonterminalEdges[unqualifiedClassInstanceCreationExpression]!!, curSppfNode)
      }
    }
  }

  private fun parsefieldAccess(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SUPER -> 
            handleTerminal(JavaToken.SUPER, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeName, state.nonterminalEdges[typeName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, primary, state.nonterminalEdges[primary]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SUPER -> 
            handleTerminal(JavaToken.SUPER, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parsetypeArgumentsOrDiamond(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LT -> 
            handleTerminal(JavaToken.LT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArguments, state.nonterminalEdges[typeArguments]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.GT -> 
            handleTerminal(JavaToken.GT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parsearrayAccess(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expressionName, state.nonterminalEdges[expressionName]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, primaryNoNewArray,
            state.nonterminalEdges[primaryNoNewArray]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACK -> 
            handleTerminal(JavaToken.LBRACK, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACK -> 
            handleTerminal(JavaToken.RBRACK, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
    }
  }

  private fun parsemethodInvocation(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SUPER -> 
            handleTerminal(JavaToken.SUPER, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expressionName, state.nonterminalEdges[expressionName]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, typeName, state.nonterminalEdges[typeName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, methodName, state.nonterminalEdges[methodName]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, primary, state.nonterminalEdges[primary]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArguments, state.nonterminalEdges[typeArguments]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      6 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SUPER -> 
            handleTerminal(JavaToken.SUPER, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArguments, state.nonterminalEdges[typeArguments]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      7 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, argumentList, state.nonterminalEdges[argumentList]!!,
            curSppfNode)
      }
      8 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      9 -> 
       {
      }
    }
  }

  private fun parsemethodReference(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SUPER -> 
            handleTerminal(JavaToken.SUPER, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, arrayType, state.nonterminalEdges[arrayType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, expressionName, state.nonterminalEdges[expressionName]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, typeName, state.nonterminalEdges[typeName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, referenceType, state.nonterminalEdges[referenceType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, classType, state.nonterminalEdges[classType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, primary, state.nonterminalEdges[primary]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COLONCOLON -> 
            handleTerminal(JavaToken.COLONCOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOT -> 
            handleTerminal(JavaToken.DOT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COLONCOLON -> 
            handleTerminal(JavaToken.COLONCOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COLONCOLON -> 
            handleTerminal(JavaToken.COLONCOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.NEW -> 
            handleTerminal(JavaToken.NEW, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      6 -> 
       {
      }
      7 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.NEW -> 
            handleTerminal(JavaToken.NEW, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArguments, state.nonterminalEdges[typeArguments]!!,
            curSppfNode)
      }
      8 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SUPER -> 
            handleTerminal(JavaToken.SUPER, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      9 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, typeArguments, state.nonterminalEdges[typeArguments]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      10 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parsearrayCreationExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.NEW -> 
            handleTerminal(JavaToken.NEW, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, classOrInterfaceType,
            state.nonterminalEdges[classOrInterfaceType]!!, curSppfNode)
        handleNonterminalEdge(descriptor, primitiveType, state.nonterminalEdges[primitiveType]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, dims, state.nonterminalEdges[dims]!!, curSppfNode)
        handleNonterminalEdge(descriptor, dimExprs, state.nonterminalEdges[dimExprs]!!, curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, dims, state.nonterminalEdges[dims]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, arrayInitializer,
            state.nonterminalEdges[arrayInitializer]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parsedimExprs(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, dimExpr, state.nonterminalEdges[dimExpr]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, dimExpr, state.nonterminalEdges[dimExpr]!!, curSppfNode)
      }
    }
  }

  private fun parsedimExpr(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LBRACK -> 
            handleTerminal(JavaToken.LBRACK, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, annotation, state.nonterminalEdges[annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RBRACK -> 
            handleTerminal(JavaToken.RBRACK, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parselambdaExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, lambdaParameters,
            state.nonterminalEdges[lambdaParameters]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ARROW -> 
            handleTerminal(JavaToken.ARROW, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, lambdaBody, state.nonterminalEdges[lambdaBody]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parselambdaParameters(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, inferredFormalParameterList,
            state.nonterminalEdges[inferredFormalParameterList]!!, curSppfNode)
        handleNonterminalEdge(descriptor, formalParameterList,
            state.nonterminalEdges[formalParameterList]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseinferredFormalParameterList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, identifier, state.nonterminalEdges[identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parselambdaBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, block, state.nonterminalEdges[block]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseassignmentExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, conditionalExpression,
            state.nonterminalEdges[conditionalExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, assignment, state.nonterminalEdges[assignment]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseassignment(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, leftHandSide, state.nonterminalEdges[leftHandSide]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, assignmentOperator,
            state.nonterminalEdges[assignmentOperator]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseleftHandSide(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expressionName, state.nonterminalEdges[expressionName]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, fieldAccess, state.nonterminalEdges[fieldAccess]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, arrayAccess, state.nonterminalEdges[arrayAccess]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseassignmentOperator(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.MODEQ -> 
            handleTerminal(JavaToken.MODEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.OREQ -> 
            handleTerminal(JavaToken.OREQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.MULTEQ -> 
            handleTerminal(JavaToken.MULTEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.DIVEQ -> 
            handleTerminal(JavaToken.DIVEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PLUSEQ -> 
            handleTerminal(JavaToken.PLUSEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.URSHIFTEQ -> 
            handleTerminal(JavaToken.URSHIFTEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.MINUSEQ -> 
            handleTerminal(JavaToken.MINUSEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.LSHIFTEQ -> 
            handleTerminal(JavaToken.LSHIFTEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.ANDEQ -> 
            handleTerminal(JavaToken.ANDEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.EQ -> 
            handleTerminal(JavaToken.EQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.RSHIFTEQ -> 
            handleTerminal(JavaToken.RSHIFTEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.XOREQ -> 
            handleTerminal(JavaToken.XOREQ, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
      }
    }
  }

  private fun parseconditionalOrExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, conditionalOrExpression,
            state.nonterminalEdges[conditionalOrExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, conditionalAndExpression,
            state.nonterminalEdges[conditionalAndExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.OROR -> 
            handleTerminal(JavaToken.OROR, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, conditionalAndExpression,
            state.nonterminalEdges[conditionalAndExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseconditionalAndExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, inclusiveOrExpression,
            state.nonterminalEdges[inclusiveOrExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, conditionalAndExpression,
            state.nonterminalEdges[conditionalAndExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ANDAND -> 
            handleTerminal(JavaToken.ANDAND, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, inclusiveOrExpression,
            state.nonterminalEdges[inclusiveOrExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseinclusiveOrExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, exclusiveOrExpression,
            state.nonterminalEdges[exclusiveOrExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, inclusiveOrExpression,
            state.nonterminalEdges[inclusiveOrExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.OR -> 
            handleTerminal(JavaToken.OR, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, exclusiveOrExpression,
            state.nonterminalEdges[exclusiveOrExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseexclusiveOrExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, andExpression, state.nonterminalEdges[andExpression]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, exclusiveOrExpression,
            state.nonterminalEdges[exclusiveOrExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.XOR -> 
            handleTerminal(JavaToken.XOR, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, andExpression, state.nonterminalEdges[andExpression]!!,
            curSppfNode)
      }
    }
  }

  private fun parseandExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, andExpression, state.nonterminalEdges[andExpression]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, equalityExpression,
            state.nonterminalEdges[equalityExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.AND -> 
            handleTerminal(JavaToken.AND, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, equalityExpression,
            state.nonterminalEdges[equalityExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseequalityExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, relationalExpression,
            state.nonterminalEdges[relationalExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, equalityExpression,
            state.nonterminalEdges[equalityExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.EQEQ -> 
            handleTerminal(JavaToken.EQEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.NOTEQ -> 
            handleTerminal(JavaToken.NOTEQ, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, relationalExpression,
            state.nonterminalEdges[relationalExpression]!!, curSppfNode)
      }
    }
  }

  private fun parserelationalExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, shiftExpression,
            state.nonterminalEdges[shiftExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, relationalExpression,
            state.nonterminalEdges[relationalExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LTEQ -> 
            handleTerminal(JavaToken.LTEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.INSTANCEOF -> 
            handleTerminal(JavaToken.INSTANCEOF, state, inputEdge, descriptor, curSppfNode)
            JavaToken.GTEQ -> 
            handleTerminal(JavaToken.GTEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.GT -> 
            handleTerminal(JavaToken.GT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.LT -> 
            handleTerminal(JavaToken.LT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, shiftExpression,
            state.nonterminalEdges[shiftExpression]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, referenceType, state.nonterminalEdges[referenceType]!!,
            curSppfNode)
      }
    }
  }

  private fun parseshiftExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, shiftExpression,
            state.nonterminalEdges[shiftExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, additiveExpression,
            state.nonterminalEdges[additiveExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.GT -> 
            handleTerminal(JavaToken.GT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.LT -> 
            handleTerminal(JavaToken.LT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LT -> 
            handleTerminal(JavaToken.LT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.GT -> 
            handleTerminal(JavaToken.GT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.GT -> 
            handleTerminal(JavaToken.GT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, additiveExpression,
            state.nonterminalEdges[additiveExpression]!!, curSppfNode)
      }
      6 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, additiveExpression,
            state.nonterminalEdges[additiveExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseadditiveExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, additiveExpression,
            state.nonterminalEdges[additiveExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, multiplicativeExpression,
            state.nonterminalEdges[multiplicativeExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PLUS -> 
            handleTerminal(JavaToken.PLUS, state, inputEdge, descriptor, curSppfNode)
            JavaToken.MINUS -> 
            handleTerminal(JavaToken.MINUS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, multiplicativeExpression,
            state.nonterminalEdges[multiplicativeExpression]!!, curSppfNode)
      }
    }
  }

  private fun parsemultiplicativeExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, multiplicativeExpression,
            state.nonterminalEdges[multiplicativeExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, unaryExpression,
            state.nonterminalEdges[unaryExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DIV -> 
            handleTerminal(JavaToken.DIV, state, inputEdge, descriptor, curSppfNode)
            JavaToken.MOD -> 
            handleTerminal(JavaToken.MOD, state, inputEdge, descriptor, curSppfNode)
            JavaToken.MULT -> 
            handleTerminal(JavaToken.MULT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unaryExpression,
            state.nonterminalEdges[unaryExpression]!!, curSppfNode)
      }
    }
  }

  private fun parsepreIncrementExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PLUSPLUS -> 
            handleTerminal(JavaToken.PLUSPLUS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unaryExpression,
            state.nonterminalEdges[unaryExpression]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsepreDecrementExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.MINUSMINUS -> 
            handleTerminal(JavaToken.MINUSMINUS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unaryExpression,
            state.nonterminalEdges[unaryExpression]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseunaryExpressionNotPlusMinus(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.NOT -> 
            handleTerminal(JavaToken.NOT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.COMP -> 
            handleTerminal(JavaToken.COMP, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, castExpression, state.nonterminalEdges[castExpression]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, postfixExpression,
            state.nonterminalEdges[postfixExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unaryExpression,
            state.nonterminalEdges[unaryExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseunaryExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PLUS -> 
            handleTerminal(JavaToken.PLUS, state, inputEdge, descriptor, curSppfNode)
            JavaToken.MINUS -> 
            handleTerminal(JavaToken.MINUS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unaryExpressionNotPlusMinus,
            state.nonterminalEdges[unaryExpressionNotPlusMinus]!!, curSppfNode)
        handleNonterminalEdge(descriptor, preDecrementExpression,
            state.nonterminalEdges[preDecrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, preIncrementExpression,
            state.nonterminalEdges[preIncrementExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unaryExpression,
            state.nonterminalEdges[unaryExpression]!!, curSppfNode)
      }
    }
  }

  private fun parsepostfixExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expressionName, state.nonterminalEdges[expressionName]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, postIncrementExpression,
            state.nonterminalEdges[postIncrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, postDecrementExpression,
            state.nonterminalEdges[postDecrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, primary, state.nonterminalEdges[primary]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsepostIncrementExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, postfixExpression,
            state.nonterminalEdges[postfixExpression]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PLUSPLUS -> 
            handleTerminal(JavaToken.PLUSPLUS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
      }
    }
  }

  private fun parsepostDecrementExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, postfixExpression,
            state.nonterminalEdges[postfixExpression]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.MINUSMINUS -> 
            handleTerminal(JavaToken.MINUSMINUS, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
      }
    }
  }

  private fun parsecastExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LPAREN -> 
            handleTerminal(JavaToken.LPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, referenceType, state.nonterminalEdges[referenceType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, primitiveType, state.nonterminalEdges[primitiveType]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.RPAREN -> 
            handleTerminal(JavaToken.RPAREN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, additionalBound,
            state.nonterminalEdges[additionalBound]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unaryExpressionNotPlusMinus,
            state.nonterminalEdges[unaryExpressionNotPlusMinus]!!, curSppfNode)
        handleNonterminalEdge(descriptor, lambdaExpression,
            state.nonterminalEdges[lambdaExpression]!!, curSppfNode)
      }
      5 -> 
       {
      }
      6 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, unaryExpression,
            state.nonterminalEdges[unaryExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseconstantExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, expression, state.nonterminalEdges[expression]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  override fun handleDescriptor(descriptor: Descriptor<VertexType>) {
    super.handleDescriptor(descriptor)
    org.ucfs.intersection.RecoveryIntersection.handleRecoveryEdges(this, descriptor)
  }

  override fun setInput(`value`: IInputGraph<VertexType, LabelType>) {
    ctx = org.ucfs.parser.context.RecoveryContext(grammar.rsm, value)
  }
}
