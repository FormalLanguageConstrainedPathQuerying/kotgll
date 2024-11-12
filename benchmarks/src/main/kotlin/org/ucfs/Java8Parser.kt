@file:Suppress("RedundantVisibilityModifier")

package org.ucfs

import org.ucfs.JavaToken
import org.ucfs.descriptors.Descriptor
import org.ucfs.input.IInputGraph
import org.ucfs.input.ILabel
import org.ucfs.parser.GeneratedParser
import org.ucfs.rsm.symbol.Nonterminal
import org.ucfs.sppf.node.SppfNode

public class Java8Parser<VertexType, LabelType : ILabel> : GeneratedParser<VertexType, LabelType>()
    {
  public val grammar: Java8 = Java8()

  private val CompilationUnit: Nonterminal = grammar.CompilationUnit.nonterm

  private val Identifier: Nonterminal = grammar.Identifier.nonterm

  private val Literal: Nonterminal = grammar.Literal.nonterm

  private val Type: Nonterminal = grammar.Type.nonterm

  private val PrimitiveType: Nonterminal = grammar.PrimitiveType.nonterm

  private val ReferenceType: Nonterminal = grammar.ReferenceType.nonterm

  private val Annotation: Nonterminal = grammar.Annotation.nonterm

  private val NumericType: Nonterminal = grammar.NumericType.nonterm

  private val IntegralType: Nonterminal = grammar.IntegralType.nonterm

  private val FloatingPointType: Nonterminal = grammar.FloatingPointType.nonterm

  private val ClassOrInterfaceType: Nonterminal = grammar.ClassOrInterfaceType.nonterm

  private val TypeVariable: Nonterminal = grammar.TypeVariable.nonterm

  private val ArrayType: Nonterminal = grammar.ArrayType.nonterm

  private val ClassType: Nonterminal = grammar.ClassType.nonterm

  private val InterfaceType: Nonterminal = grammar.InterfaceType.nonterm

  private val TypeArguments: Nonterminal = grammar.TypeArguments.nonterm

  private val Dims: Nonterminal = grammar.Dims.nonterm

  private val TypeParameter: Nonterminal = grammar.TypeParameter.nonterm

  private val TypeParameterModifier: Nonterminal = grammar.TypeParameterModifier.nonterm

  private val TypeBound: Nonterminal = grammar.TypeBound.nonterm

  private val AdditionalBound: Nonterminal = grammar.AdditionalBound.nonterm

  private val TypeArgumentList: Nonterminal = grammar.TypeArgumentList.nonterm

  private val TypeArgument: Nonterminal = grammar.TypeArgument.nonterm

  private val Wildcard: Nonterminal = grammar.Wildcard.nonterm

  private val WildcardBounds: Nonterminal = grammar.WildcardBounds.nonterm

  private val TypeName: Nonterminal = grammar.TypeName.nonterm

  private val PackageOrTypeName: Nonterminal = grammar.PackageOrTypeName.nonterm

  private val ExpressionName: Nonterminal = grammar.ExpressionName.nonterm

  private val AmbiguousName: Nonterminal = grammar.AmbiguousName.nonterm

  private val MethodName: Nonterminal = grammar.MethodName.nonterm

  private val PackageName: Nonterminal = grammar.PackageName.nonterm

  private val Result: Nonterminal = grammar.Result.nonterm

  private val PackageDeclaration: Nonterminal = grammar.PackageDeclaration.nonterm

  private val ImportDeclaration: Nonterminal = grammar.ImportDeclaration.nonterm

  private val TypeDeclaration: Nonterminal = grammar.TypeDeclaration.nonterm

  private val PackageModifier: Nonterminal = grammar.PackageModifier.nonterm

  private val SingleTypeImportDeclaration: Nonterminal = grammar.SingleTypeImportDeclaration.nonterm

  private val TypeImportOnDemandDeclaration: Nonterminal =
      grammar.TypeImportOnDemandDeclaration.nonterm

  private val SingleStaticImportDeclaration: Nonterminal =
      grammar.SingleStaticImportDeclaration.nonterm

  private val StaticImportOnDemandDeclaration: Nonterminal =
      grammar.StaticImportOnDemandDeclaration.nonterm

  private val ClassDeclaration: Nonterminal = grammar.ClassDeclaration.nonterm

  private val InterfaceDeclaration: Nonterminal = grammar.InterfaceDeclaration.nonterm

  private val Throws: Nonterminal = grammar.Throws.nonterm

  private val NormalClassDeclaration: Nonterminal = grammar.NormalClassDeclaration.nonterm

  private val EnumDeclaration: Nonterminal = grammar.EnumDeclaration.nonterm

  private val ClassModifier: Nonterminal = grammar.ClassModifier.nonterm

  private val TypeParameters: Nonterminal = grammar.TypeParameters.nonterm

  private val Superclass: Nonterminal = grammar.Superclass.nonterm

  private val Superinterfaces: Nonterminal = grammar.Superinterfaces.nonterm

  private val ClassBody: Nonterminal = grammar.ClassBody.nonterm

  private val TypeParameterList: Nonterminal = grammar.TypeParameterList.nonterm

  private val InterfaceTypeList: Nonterminal = grammar.InterfaceTypeList.nonterm

  private val ClassBodyDeclaration: Nonterminal = grammar.ClassBodyDeclaration.nonterm

  private val ClassMemberDeclaration: Nonterminal = grammar.ClassMemberDeclaration.nonterm

  private val InstanceInitializer: Nonterminal = grammar.InstanceInitializer.nonterm

  private val StaticInitializer: Nonterminal = grammar.StaticInitializer.nonterm

  private val ConstructorDeclaration: Nonterminal = grammar.ConstructorDeclaration.nonterm

  private val FieldDeclaration: Nonterminal = grammar.FieldDeclaration.nonterm

  private val MethodDeclaration: Nonterminal = grammar.MethodDeclaration.nonterm

  private val FieldModifier: Nonterminal = grammar.FieldModifier.nonterm

  private val UnannType: Nonterminal = grammar.UnannType.nonterm

  private val VariableDeclaratorList: Nonterminal = grammar.VariableDeclaratorList.nonterm

  private val VariableDeclarator: Nonterminal = grammar.VariableDeclarator.nonterm

  private val VariableDeclaratorId: Nonterminal = grammar.VariableDeclaratorId.nonterm

  private val VariableInitializer: Nonterminal = grammar.VariableInitializer.nonterm

  private val Expression: Nonterminal = grammar.Expression.nonterm

  private val ArrayInitializer: Nonterminal = grammar.ArrayInitializer.nonterm

  private val UnannPrimitiveType: Nonterminal = grammar.UnannPrimitiveType.nonterm

  private val UnannReferenceType: Nonterminal = grammar.UnannReferenceType.nonterm

  private val UnannClassOrInterfaceType: Nonterminal = grammar.UnannClassOrInterfaceType.nonterm

  private val UnannTypeVariable: Nonterminal = grammar.UnannTypeVariable.nonterm

  private val UnannArrayType: Nonterminal = grammar.UnannArrayType.nonterm

  private val UnannClassType: Nonterminal = grammar.UnannClassType.nonterm

  private val UnannInterfaceType: Nonterminal = grammar.UnannInterfaceType.nonterm

  private val MethodModifier: Nonterminal = grammar.MethodModifier.nonterm

  private val MethodHeader: Nonterminal = grammar.MethodHeader.nonterm

  private val MethodBody: Nonterminal = grammar.MethodBody.nonterm

  private val MethodDeclarator: Nonterminal = grammar.MethodDeclarator.nonterm

  private val FormalParameterList: Nonterminal = grammar.FormalParameterList.nonterm

  private val ReceiverParameter: Nonterminal = grammar.ReceiverParameter.nonterm

  private val FormalParameters: Nonterminal = grammar.FormalParameters.nonterm

  private val LastFormalParameter: Nonterminal = grammar.LastFormalParameter.nonterm

  private val FormalParameter: Nonterminal = grammar.FormalParameter.nonterm

  private val VariableModifier: Nonterminal = grammar.VariableModifier.nonterm

  private val ExceptionTypeList: Nonterminal = grammar.ExceptionTypeList.nonterm

  private val ExceptionType: Nonterminal = grammar.ExceptionType.nonterm

  private val Block: Nonterminal = grammar.Block.nonterm

  private val ConstructorModifier: Nonterminal = grammar.ConstructorModifier.nonterm

  private val ConstructorDeclarator: Nonterminal = grammar.ConstructorDeclarator.nonterm

  private val ConstructorBody: Nonterminal = grammar.ConstructorBody.nonterm

  private val SimpleTypeName: Nonterminal = grammar.SimpleTypeName.nonterm

  private val ExplicitConstructorInvocation: Nonterminal =
      grammar.ExplicitConstructorInvocation.nonterm

  private val EnumBody: Nonterminal = grammar.EnumBody.nonterm

  private val EnumConstantList: Nonterminal = grammar.EnumConstantList.nonterm

  private val EnumConstant: Nonterminal = grammar.EnumConstant.nonterm

  private val EnumConstantModifier: Nonterminal = grammar.EnumConstantModifier.nonterm

  private val EnumBodyDeclarations: Nonterminal = grammar.EnumBodyDeclarations.nonterm

  private val BlockStatements: Nonterminal = grammar.BlockStatements.nonterm

  private val ArgumentList: Nonterminal = grammar.ArgumentList.nonterm

  private val Primary: Nonterminal = grammar.Primary.nonterm

  private val NormalInterfaceDeclaration: Nonterminal = grammar.NormalInterfaceDeclaration.nonterm

  private val InterfaceModifier: Nonterminal = grammar.InterfaceModifier.nonterm

  private val ExtendsInterfaces: Nonterminal = grammar.ExtendsInterfaces.nonterm

  private val InterfaceBody: Nonterminal = grammar.InterfaceBody.nonterm

  private val InterfaceMemberDeclaration: Nonterminal = grammar.InterfaceMemberDeclaration.nonterm

  private val ConstantDeclaration: Nonterminal = grammar.ConstantDeclaration.nonterm

  private val ConstantModifier: Nonterminal = grammar.ConstantModifier.nonterm

  private val AnnotationTypeDeclaration: Nonterminal = grammar.AnnotationTypeDeclaration.nonterm

  private val AnnotationTypeBody: Nonterminal = grammar.AnnotationTypeBody.nonterm

  private val AnnotationTypeMemberDeclaration: Nonterminal =
      grammar.AnnotationTypeMemberDeclaration.nonterm

  private val AnnotationTypeElementDeclaration: Nonterminal =
      grammar.AnnotationTypeElementDeclaration.nonterm

  private val DefaultValue: Nonterminal = grammar.DefaultValue.nonterm

  private val NormalAnnotation: Nonterminal = grammar.NormalAnnotation.nonterm

  private val ElementValuePairList: Nonterminal = grammar.ElementValuePairList.nonterm

  private val ElementValuePair: Nonterminal = grammar.ElementValuePair.nonterm

  private val ElementValue: Nonterminal = grammar.ElementValue.nonterm

  private val ElementValueArrayInitializer: Nonterminal =
      grammar.ElementValueArrayInitializer.nonterm

  private val ElementValueList: Nonterminal = grammar.ElementValueList.nonterm

  private val MarkerAnnotation: Nonterminal = grammar.MarkerAnnotation.nonterm

  private val SingleElementAnnotation: Nonterminal = grammar.SingleElementAnnotation.nonterm

  private val InterfaceMethodDeclaration: Nonterminal = grammar.InterfaceMethodDeclaration.nonterm

  private val AnnotationTypeElementModifier: Nonterminal =
      grammar.AnnotationTypeElementModifier.nonterm

  private val ConditionalExpression: Nonterminal = grammar.ConditionalExpression.nonterm

  private val VariableInitializerList: Nonterminal = grammar.VariableInitializerList.nonterm

  private val BlockStatement: Nonterminal = grammar.BlockStatement.nonterm

  private val LocalVariableDeclarationStatement: Nonterminal =
      grammar.LocalVariableDeclarationStatement.nonterm

  private val LocalVariableDeclaration: Nonterminal = grammar.LocalVariableDeclaration.nonterm

  private val Statement: Nonterminal = grammar.Statement.nonterm

  private val StatementNoShortIf: Nonterminal = grammar.StatementNoShortIf.nonterm

  private val StatementWithoutTrailingSubstatement: Nonterminal =
      grammar.StatementWithoutTrailingSubstatement.nonterm

  private val EmptyStatement: Nonterminal = grammar.EmptyStatement.nonterm

  private val LabeledStatement: Nonterminal = grammar.LabeledStatement.nonterm

  private val LabeledStatementNoShortIf: Nonterminal = grammar.LabeledStatementNoShortIf.nonterm

  private val ExpressionStatement: Nonterminal = grammar.ExpressionStatement.nonterm

  private val StatementExpression: Nonterminal = grammar.StatementExpression.nonterm

  private val IfThenStatement: Nonterminal = grammar.IfThenStatement.nonterm

  private val IfThenElseStatement: Nonterminal = grammar.IfThenElseStatement.nonterm

  private val IfThenElseStatementNoShortIf: Nonterminal =
      grammar.IfThenElseStatementNoShortIf.nonterm

  private val AssertStatement: Nonterminal = grammar.AssertStatement.nonterm

  private val SwitchStatement: Nonterminal = grammar.SwitchStatement.nonterm

  private val SwitchBlock: Nonterminal = grammar.SwitchBlock.nonterm

  private val SwitchBlockStatementGroup: Nonterminal = grammar.SwitchBlockStatementGroup.nonterm

  private val SwitchLabels: Nonterminal = grammar.SwitchLabels.nonterm

  private val SwitchLabel: Nonterminal = grammar.SwitchLabel.nonterm

  private val EnumConstantName: Nonterminal = grammar.EnumConstantName.nonterm

  private val WhileStatement: Nonterminal = grammar.WhileStatement.nonterm

  private val WhileStatementNoShortIf: Nonterminal = grammar.WhileStatementNoShortIf.nonterm

  private val DoStatement: Nonterminal = grammar.DoStatement.nonterm

  private val InterfaceMethodModifier: Nonterminal = grammar.InterfaceMethodModifier.nonterm

  private val ForStatement: Nonterminal = grammar.ForStatement.nonterm

  private val ForStatementNoShortIf: Nonterminal = grammar.ForStatementNoShortIf.nonterm

  private val BasicForStatement: Nonterminal = grammar.BasicForStatement.nonterm

  private val BasicForStatementNoShortIf: Nonterminal = grammar.BasicForStatementNoShortIf.nonterm

  private val ForInit: Nonterminal = grammar.ForInit.nonterm

  private val ForUpdate: Nonterminal = grammar.ForUpdate.nonterm

  private val StatementExpressionList: Nonterminal = grammar.StatementExpressionList.nonterm

  private val EnhancedForStatement: Nonterminal = grammar.EnhancedForStatement.nonterm

  private val EnhancedForStatementNoShortIf: Nonterminal =
      grammar.EnhancedForStatementNoShortIf.nonterm

  private val BreakStatement: Nonterminal = grammar.BreakStatement.nonterm

  private val ContinueStatement: Nonterminal = grammar.ContinueStatement.nonterm

  private val ReturnStatement: Nonterminal = grammar.ReturnStatement.nonterm

  private val ThrowStatement: Nonterminal = grammar.ThrowStatement.nonterm

  private val SynchronizedStatement: Nonterminal = grammar.SynchronizedStatement.nonterm

  private val TryStatement: Nonterminal = grammar.TryStatement.nonterm

  private val Catches: Nonterminal = grammar.Catches.nonterm

  private val CatchClause: Nonterminal = grammar.CatchClause.nonterm

  private val CatchFormalParameter: Nonterminal = grammar.CatchFormalParameter.nonterm

  private val CatchType: Nonterminal = grammar.CatchType.nonterm

  private val Finally: Nonterminal = grammar.Finally.nonterm

  private val TryWithResourcesStatement: Nonterminal = grammar.TryWithResourcesStatement.nonterm

  private val ResourceSpecification: Nonterminal = grammar.ResourceSpecification.nonterm

  private val ResourceList: Nonterminal = grammar.ResourceList.nonterm

  private val Resource: Nonterminal = grammar.Resource.nonterm

  private val PrimaryNoNewArray: Nonterminal = grammar.PrimaryNoNewArray.nonterm

  private val ClassLiteral: Nonterminal = grammar.ClassLiteral.nonterm

  private val classOrInterfaceTypeToInstantiate: Nonterminal =
      grammar.classOrInterfaceTypeToInstantiate.nonterm

  private val UnqualifiedClassInstanceCreationExpression: Nonterminal =
      grammar.UnqualifiedClassInstanceCreationExpression.nonterm

  private val ClassInstanceCreationExpression: Nonterminal =
      grammar.ClassInstanceCreationExpression.nonterm

  private val FieldAccess: Nonterminal = grammar.FieldAccess.nonterm

  private val TypeArgumentsOrDiamond: Nonterminal = grammar.TypeArgumentsOrDiamond.nonterm

  private val ArrayAccess: Nonterminal = grammar.ArrayAccess.nonterm

  private val MethodInvocation: Nonterminal = grammar.MethodInvocation.nonterm

  private val MethodReference: Nonterminal = grammar.MethodReference.nonterm

  private val ArrayCreationExpression: Nonterminal = grammar.ArrayCreationExpression.nonterm

  private val DimExprs: Nonterminal = grammar.DimExprs.nonterm

  private val DimExpr: Nonterminal = grammar.DimExpr.nonterm

  private val LambdaExpression: Nonterminal = grammar.LambdaExpression.nonterm

  private val LambdaParameters: Nonterminal = grammar.LambdaParameters.nonterm

  private val InferredFormalParameterList: Nonterminal = grammar.InferredFormalParameterList.nonterm

  private val LambdaBody: Nonterminal = grammar.LambdaBody.nonterm

  private val AssignmentExpression: Nonterminal = grammar.AssignmentExpression.nonterm

  private val Assignment: Nonterminal = grammar.Assignment.nonterm

  private val LeftHandSide: Nonterminal = grammar.LeftHandSide.nonterm

  private val AssignmentOperator: Nonterminal = grammar.AssignmentOperator.nonterm

  private val ConditionalOrExpression: Nonterminal = grammar.ConditionalOrExpression.nonterm

  private val ConditionalAndExpression: Nonterminal = grammar.ConditionalAndExpression.nonterm

  private val InclusiveOrExpression: Nonterminal = grammar.InclusiveOrExpression.nonterm

  private val ExclusiveOrExpression: Nonterminal = grammar.ExclusiveOrExpression.nonterm

  private val AndExpression: Nonterminal = grammar.AndExpression.nonterm

  private val EqualityExpression: Nonterminal = grammar.EqualityExpression.nonterm

  private val RelationalExpression: Nonterminal = grammar.RelationalExpression.nonterm

  private val ShiftExpression: Nonterminal = grammar.ShiftExpression.nonterm

  private val AdditiveExpression: Nonterminal = grammar.AdditiveExpression.nonterm

  private val MultiplicativeExpression: Nonterminal = grammar.MultiplicativeExpression.nonterm

  private val PreIncrementExpression: Nonterminal = grammar.PreIncrementExpression.nonterm

  private val PreDecrementExpression: Nonterminal = grammar.PreDecrementExpression.nonterm

  private val UnaryExpressionNotPlusMinus: Nonterminal = grammar.UnaryExpressionNotPlusMinus.nonterm

  private val UnaryExpression: Nonterminal = grammar.UnaryExpression.nonterm

  private val PostfixExpression: Nonterminal = grammar.PostfixExpression.nonterm

  private val PostIncrementExpression: Nonterminal = grammar.PostIncrementExpression.nonterm

  private val PostDecrementExpression: Nonterminal = grammar.PostDecrementExpression.nonterm

  private val CastExpression: Nonterminal = grammar.CastExpression.nonterm

  private val ConstantExpression: Nonterminal = grammar.ConstantExpression.nonterm

  override fun callNtFuncs(
    nt: Nonterminal,
    descriptor: Descriptor<VertexType>,
    curSppfNode: SppfNode<VertexType>?,
  ) {
    when(nt.name) {
      "CompilationUnit" -> parseCompilationUnit(descriptor, curSppfNode)
      "Identifier" -> parseIdentifier(descriptor, curSppfNode)
      "Literal" -> parseLiteral(descriptor, curSppfNode)
      "Type" -> parseType(descriptor, curSppfNode)
      "PrimitiveType" -> parsePrimitiveType(descriptor, curSppfNode)
      "ReferenceType" -> parseReferenceType(descriptor, curSppfNode)
      "Annotation" -> parseAnnotation(descriptor, curSppfNode)
      "NumericType" -> parseNumericType(descriptor, curSppfNode)
      "IntegralType" -> parseIntegralType(descriptor, curSppfNode)
      "FloatingPointType" -> parseFloatingPointType(descriptor, curSppfNode)
      "ClassOrInterfaceType" -> parseClassOrInterfaceType(descriptor, curSppfNode)
      "TypeVariable" -> parseTypeVariable(descriptor, curSppfNode)
      "ArrayType" -> parseArrayType(descriptor, curSppfNode)
      "ClassType" -> parseClassType(descriptor, curSppfNode)
      "InterfaceType" -> parseInterfaceType(descriptor, curSppfNode)
      "TypeArguments" -> parseTypeArguments(descriptor, curSppfNode)
      "Dims" -> parseDims(descriptor, curSppfNode)
      "TypeParameter" -> parseTypeParameter(descriptor, curSppfNode)
      "TypeParameterModifier" -> parseTypeParameterModifier(descriptor, curSppfNode)
      "TypeBound" -> parseTypeBound(descriptor, curSppfNode)
      "AdditionalBound" -> parseAdditionalBound(descriptor, curSppfNode)
      "TypeArgumentList" -> parseTypeArgumentList(descriptor, curSppfNode)
      "TypeArgument" -> parseTypeArgument(descriptor, curSppfNode)
      "Wildcard" -> parseWildcard(descriptor, curSppfNode)
      "WildcardBounds" -> parseWildcardBounds(descriptor, curSppfNode)
      "TypeName" -> parseTypeName(descriptor, curSppfNode)
      "PackageOrTypeName" -> parsePackageOrTypeName(descriptor, curSppfNode)
      "ExpressionName" -> parseExpressionName(descriptor, curSppfNode)
      "AmbiguousName" -> parseAmbiguousName(descriptor, curSppfNode)
      "MethodName" -> parseMethodName(descriptor, curSppfNode)
      "PackageName" -> parsePackageName(descriptor, curSppfNode)
      "Result" -> parseResult(descriptor, curSppfNode)
      "PackageDeclaration" -> parsePackageDeclaration(descriptor, curSppfNode)
      "ImportDeclaration" -> parseImportDeclaration(descriptor, curSppfNode)
      "TypeDeclaration" -> parseTypeDeclaration(descriptor, curSppfNode)
      "PackageModifier" -> parsePackageModifier(descriptor, curSppfNode)
      "SingleTypeImportDeclaration" -> parseSingleTypeImportDeclaration(descriptor, curSppfNode)
      "TypeImportOnDemandDeclaration" -> parseTypeImportOnDemandDeclaration(descriptor, curSppfNode)
      "SingleStaticImportDeclaration" -> parseSingleStaticImportDeclaration(descriptor, curSppfNode)
      "StaticImportOnDemandDeclaration" -> parseStaticImportOnDemandDeclaration(descriptor,
          curSppfNode)
      "ClassDeclaration" -> parseClassDeclaration(descriptor, curSppfNode)
      "InterfaceDeclaration" -> parseInterfaceDeclaration(descriptor, curSppfNode)
      "Throws" -> parseThrows(descriptor, curSppfNode)
      "NormalClassDeclaration" -> parseNormalClassDeclaration(descriptor, curSppfNode)
      "EnumDeclaration" -> parseEnumDeclaration(descriptor, curSppfNode)
      "ClassModifier" -> parseClassModifier(descriptor, curSppfNode)
      "TypeParameters" -> parseTypeParameters(descriptor, curSppfNode)
      "Superclass" -> parseSuperclass(descriptor, curSppfNode)
      "Superinterfaces" -> parseSuperinterfaces(descriptor, curSppfNode)
      "ClassBody" -> parseClassBody(descriptor, curSppfNode)
      "TypeParameterList" -> parseTypeParameterList(descriptor, curSppfNode)
      "InterfaceTypeList" -> parseInterfaceTypeList(descriptor, curSppfNode)
      "ClassBodyDeclaration" -> parseClassBodyDeclaration(descriptor, curSppfNode)
      "ClassMemberDeclaration" -> parseClassMemberDeclaration(descriptor, curSppfNode)
      "InstanceInitializer" -> parseInstanceInitializer(descriptor, curSppfNode)
      "StaticInitializer" -> parseStaticInitializer(descriptor, curSppfNode)
      "ConstructorDeclaration" -> parseConstructorDeclaration(descriptor, curSppfNode)
      "FieldDeclaration" -> parseFieldDeclaration(descriptor, curSppfNode)
      "MethodDeclaration" -> parseMethodDeclaration(descriptor, curSppfNode)
      "FieldModifier" -> parseFieldModifier(descriptor, curSppfNode)
      "UnannType" -> parseUnannType(descriptor, curSppfNode)
      "VariableDeclaratorList" -> parseVariableDeclaratorList(descriptor, curSppfNode)
      "VariableDeclarator" -> parseVariableDeclarator(descriptor, curSppfNode)
      "VariableDeclaratorId" -> parseVariableDeclaratorId(descriptor, curSppfNode)
      "VariableInitializer" -> parseVariableInitializer(descriptor, curSppfNode)
      "Expression" -> parseExpression(descriptor, curSppfNode)
      "ArrayInitializer" -> parseArrayInitializer(descriptor, curSppfNode)
      "UnannPrimitiveType" -> parseUnannPrimitiveType(descriptor, curSppfNode)
      "UnannReferenceType" -> parseUnannReferenceType(descriptor, curSppfNode)
      "UnannClassOrInterfaceType" -> parseUnannClassOrInterfaceType(descriptor, curSppfNode)
      "UnannTypeVariable" -> parseUnannTypeVariable(descriptor, curSppfNode)
      "UnannArrayType" -> parseUnannArrayType(descriptor, curSppfNode)
      "UnannClassType" -> parseUnannClassType(descriptor, curSppfNode)
      "UnannInterfaceType" -> parseUnannInterfaceType(descriptor, curSppfNode)
      "MethodModifier" -> parseMethodModifier(descriptor, curSppfNode)
      "MethodHeader" -> parseMethodHeader(descriptor, curSppfNode)
      "MethodBody" -> parseMethodBody(descriptor, curSppfNode)
      "MethodDeclarator" -> parseMethodDeclarator(descriptor, curSppfNode)
      "FormalParameterList" -> parseFormalParameterList(descriptor, curSppfNode)
      "ReceiverParameter" -> parseReceiverParameter(descriptor, curSppfNode)
      "FormalParameters" -> parseFormalParameters(descriptor, curSppfNode)
      "LastFormalParameter" -> parseLastFormalParameter(descriptor, curSppfNode)
      "FormalParameter" -> parseFormalParameter(descriptor, curSppfNode)
      "VariableModifier" -> parseVariableModifier(descriptor, curSppfNode)
      "ExceptionTypeList" -> parseExceptionTypeList(descriptor, curSppfNode)
      "ExceptionType" -> parseExceptionType(descriptor, curSppfNode)
      "Block" -> parseBlock(descriptor, curSppfNode)
      "ConstructorModifier" -> parseConstructorModifier(descriptor, curSppfNode)
      "ConstructorDeclarator" -> parseConstructorDeclarator(descriptor, curSppfNode)
      "ConstructorBody" -> parseConstructorBody(descriptor, curSppfNode)
      "SimpleTypeName" -> parseSimpleTypeName(descriptor, curSppfNode)
      "ExplicitConstructorInvocation" -> parseExplicitConstructorInvocation(descriptor, curSppfNode)
      "EnumBody" -> parseEnumBody(descriptor, curSppfNode)
      "EnumConstantList" -> parseEnumConstantList(descriptor, curSppfNode)
      "EnumConstant" -> parseEnumConstant(descriptor, curSppfNode)
      "EnumConstantModifier" -> parseEnumConstantModifier(descriptor, curSppfNode)
      "EnumBodyDeclarations" -> parseEnumBodyDeclarations(descriptor, curSppfNode)
      "BlockStatements" -> parseBlockStatements(descriptor, curSppfNode)
      "ArgumentList" -> parseArgumentList(descriptor, curSppfNode)
      "Primary" -> parsePrimary(descriptor, curSppfNode)
      "NormalInterfaceDeclaration" -> parseNormalInterfaceDeclaration(descriptor, curSppfNode)
      "InterfaceModifier" -> parseInterfaceModifier(descriptor, curSppfNode)
      "ExtendsInterfaces" -> parseExtendsInterfaces(descriptor, curSppfNode)
      "InterfaceBody" -> parseInterfaceBody(descriptor, curSppfNode)
      "InterfaceMemberDeclaration" -> parseInterfaceMemberDeclaration(descriptor, curSppfNode)
      "ConstantDeclaration" -> parseConstantDeclaration(descriptor, curSppfNode)
      "ConstantModifier" -> parseConstantModifier(descriptor, curSppfNode)
      "AnnotationTypeDeclaration" -> parseAnnotationTypeDeclaration(descriptor, curSppfNode)
      "AnnotationTypeBody" -> parseAnnotationTypeBody(descriptor, curSppfNode)
      "AnnotationTypeMemberDeclaration" -> parseAnnotationTypeMemberDeclaration(descriptor,
          curSppfNode)
      "AnnotationTypeElementDeclaration" -> parseAnnotationTypeElementDeclaration(descriptor,
          curSppfNode)
      "DefaultValue" -> parseDefaultValue(descriptor, curSppfNode)
      "NormalAnnotation" -> parseNormalAnnotation(descriptor, curSppfNode)
      "ElementValuePairList" -> parseElementValuePairList(descriptor, curSppfNode)
      "ElementValuePair" -> parseElementValuePair(descriptor, curSppfNode)
      "ElementValue" -> parseElementValue(descriptor, curSppfNode)
      "ElementValueArrayInitializer" -> parseElementValueArrayInitializer(descriptor, curSppfNode)
      "ElementValueList" -> parseElementValueList(descriptor, curSppfNode)
      "MarkerAnnotation" -> parseMarkerAnnotation(descriptor, curSppfNode)
      "SingleElementAnnotation" -> parseSingleElementAnnotation(descriptor, curSppfNode)
      "InterfaceMethodDeclaration" -> parseInterfaceMethodDeclaration(descriptor, curSppfNode)
      "AnnotationTypeElementModifier" -> parseAnnotationTypeElementModifier(descriptor, curSppfNode)
      "ConditionalExpression" -> parseConditionalExpression(descriptor, curSppfNode)
      "VariableInitializerList" -> parseVariableInitializerList(descriptor, curSppfNode)
      "BlockStatement" -> parseBlockStatement(descriptor, curSppfNode)
      "LocalVariableDeclarationStatement" -> parseLocalVariableDeclarationStatement(descriptor,
          curSppfNode)
      "LocalVariableDeclaration" -> parseLocalVariableDeclaration(descriptor, curSppfNode)
      "Statement" -> parseStatement(descriptor, curSppfNode)
      "StatementNoShortIf" -> parseStatementNoShortIf(descriptor, curSppfNode)
      "StatementWithoutTrailingSubstatement" ->
          parseStatementWithoutTrailingSubstatement(descriptor, curSppfNode)
      "EmptyStatement" -> parseEmptyStatement(descriptor, curSppfNode)
      "LabeledStatement" -> parseLabeledStatement(descriptor, curSppfNode)
      "LabeledStatementNoShortIf" -> parseLabeledStatementNoShortIf(descriptor, curSppfNode)
      "ExpressionStatement" -> parseExpressionStatement(descriptor, curSppfNode)
      "StatementExpression" -> parseStatementExpression(descriptor, curSppfNode)
      "IfThenStatement" -> parseIfThenStatement(descriptor, curSppfNode)
      "IfThenElseStatement" -> parseIfThenElseStatement(descriptor, curSppfNode)
      "IfThenElseStatementNoShortIf" -> parseIfThenElseStatementNoShortIf(descriptor, curSppfNode)
      "AssertStatement" -> parseAssertStatement(descriptor, curSppfNode)
      "SwitchStatement" -> parseSwitchStatement(descriptor, curSppfNode)
      "SwitchBlock" -> parseSwitchBlock(descriptor, curSppfNode)
      "SwitchBlockStatementGroup" -> parseSwitchBlockStatementGroup(descriptor, curSppfNode)
      "SwitchLabels" -> parseSwitchLabels(descriptor, curSppfNode)
      "SwitchLabel" -> parseSwitchLabel(descriptor, curSppfNode)
      "EnumConstantName" -> parseEnumConstantName(descriptor, curSppfNode)
      "WhileStatement" -> parseWhileStatement(descriptor, curSppfNode)
      "WhileStatementNoShortIf" -> parseWhileStatementNoShortIf(descriptor, curSppfNode)
      "DoStatement" -> parseDoStatement(descriptor, curSppfNode)
      "InterfaceMethodModifier" -> parseInterfaceMethodModifier(descriptor, curSppfNode)
      "ForStatement" -> parseForStatement(descriptor, curSppfNode)
      "ForStatementNoShortIf" -> parseForStatementNoShortIf(descriptor, curSppfNode)
      "BasicForStatement" -> parseBasicForStatement(descriptor, curSppfNode)
      "BasicForStatementNoShortIf" -> parseBasicForStatementNoShortIf(descriptor, curSppfNode)
      "ForInit" -> parseForInit(descriptor, curSppfNode)
      "ForUpdate" -> parseForUpdate(descriptor, curSppfNode)
      "StatementExpressionList" -> parseStatementExpressionList(descriptor, curSppfNode)
      "EnhancedForStatement" -> parseEnhancedForStatement(descriptor, curSppfNode)
      "EnhancedForStatementNoShortIf" -> parseEnhancedForStatementNoShortIf(descriptor, curSppfNode)
      "BreakStatement" -> parseBreakStatement(descriptor, curSppfNode)
      "ContinueStatement" -> parseContinueStatement(descriptor, curSppfNode)
      "ReturnStatement" -> parseReturnStatement(descriptor, curSppfNode)
      "ThrowStatement" -> parseThrowStatement(descriptor, curSppfNode)
      "SynchronizedStatement" -> parseSynchronizedStatement(descriptor, curSppfNode)
      "TryStatement" -> parseTryStatement(descriptor, curSppfNode)
      "Catches" -> parseCatches(descriptor, curSppfNode)
      "CatchClause" -> parseCatchClause(descriptor, curSppfNode)
      "CatchFormalParameter" -> parseCatchFormalParameter(descriptor, curSppfNode)
      "CatchType" -> parseCatchType(descriptor, curSppfNode)
      "Finally" -> parseFinally(descriptor, curSppfNode)
      "TryWithResourcesStatement" -> parseTryWithResourcesStatement(descriptor, curSppfNode)
      "ResourceSpecification" -> parseResourceSpecification(descriptor, curSppfNode)
      "ResourceList" -> parseResourceList(descriptor, curSppfNode)
      "Resource" -> parseResource(descriptor, curSppfNode)
      "PrimaryNoNewArray" -> parsePrimaryNoNewArray(descriptor, curSppfNode)
      "ClassLiteral" -> parseClassLiteral(descriptor, curSppfNode)
      "classOrInterfaceTypeToInstantiate" -> parseclassOrInterfaceTypeToInstantiate(descriptor,
          curSppfNode)
      "UnqualifiedClassInstanceCreationExpression" ->
          parseUnqualifiedClassInstanceCreationExpression(descriptor, curSppfNode)
      "ClassInstanceCreationExpression" -> parseClassInstanceCreationExpression(descriptor,
          curSppfNode)
      "FieldAccess" -> parseFieldAccess(descriptor, curSppfNode)
      "TypeArgumentsOrDiamond" -> parseTypeArgumentsOrDiamond(descriptor, curSppfNode)
      "ArrayAccess" -> parseArrayAccess(descriptor, curSppfNode)
      "MethodInvocation" -> parseMethodInvocation(descriptor, curSppfNode)
      "MethodReference" -> parseMethodReference(descriptor, curSppfNode)
      "ArrayCreationExpression" -> parseArrayCreationExpression(descriptor, curSppfNode)
      "DimExprs" -> parseDimExprs(descriptor, curSppfNode)
      "DimExpr" -> parseDimExpr(descriptor, curSppfNode)
      "LambdaExpression" -> parseLambdaExpression(descriptor, curSppfNode)
      "LambdaParameters" -> parseLambdaParameters(descriptor, curSppfNode)
      "InferredFormalParameterList" -> parseInferredFormalParameterList(descriptor, curSppfNode)
      "LambdaBody" -> parseLambdaBody(descriptor, curSppfNode)
      "AssignmentExpression" -> parseAssignmentExpression(descriptor, curSppfNode)
      "Assignment" -> parseAssignment(descriptor, curSppfNode)
      "LeftHandSide" -> parseLeftHandSide(descriptor, curSppfNode)
      "AssignmentOperator" -> parseAssignmentOperator(descriptor, curSppfNode)
      "ConditionalOrExpression" -> parseConditionalOrExpression(descriptor, curSppfNode)
      "ConditionalAndExpression" -> parseConditionalAndExpression(descriptor, curSppfNode)
      "InclusiveOrExpression" -> parseInclusiveOrExpression(descriptor, curSppfNode)
      "ExclusiveOrExpression" -> parseExclusiveOrExpression(descriptor, curSppfNode)
      "AndExpression" -> parseAndExpression(descriptor, curSppfNode)
      "EqualityExpression" -> parseEqualityExpression(descriptor, curSppfNode)
      "RelationalExpression" -> parseRelationalExpression(descriptor, curSppfNode)
      "ShiftExpression" -> parseShiftExpression(descriptor, curSppfNode)
      "AdditiveExpression" -> parseAdditiveExpression(descriptor, curSppfNode)
      "MultiplicativeExpression" -> parseMultiplicativeExpression(descriptor, curSppfNode)
      "PreIncrementExpression" -> parsePreIncrementExpression(descriptor, curSppfNode)
      "PreDecrementExpression" -> parsePreDecrementExpression(descriptor, curSppfNode)
      "UnaryExpressionNotPlusMinus" -> parseUnaryExpressionNotPlusMinus(descriptor, curSppfNode)
      "UnaryExpression" -> parseUnaryExpression(descriptor, curSppfNode)
      "PostfixExpression" -> parsePostfixExpression(descriptor, curSppfNode)
      "PostIncrementExpression" -> parsePostIncrementExpression(descriptor, curSppfNode)
      "PostDecrementExpression" -> parsePostDecrementExpression(descriptor, curSppfNode)
      "CastExpression" -> parseCastExpression(descriptor, curSppfNode)
      "ConstantExpression" -> parseConstantExpression(descriptor, curSppfNode)
    }
  }

  private fun parseCompilationUnit(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ImportDeclaration,
            state.nonterminalEdges[ImportDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, TypeDeclaration,
            state.nonterminalEdges[TypeDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, PackageDeclaration,
            state.nonterminalEdges[PackageDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ImportDeclaration,
            state.nonterminalEdges[ImportDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, TypeDeclaration,
            state.nonterminalEdges[TypeDeclaration]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeDeclaration,
            state.nonterminalEdges[TypeDeclaration]!!, curSppfNode)
      }
    }
  }

  private fun parseIdentifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ID -> 
            handleTerminal(JavaToken.ID, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
      }
    }
  }

  private fun parseLiteral(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.FLOATINGLIT -> 
            handleTerminal(JavaToken.FLOATINGLIT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.BOOLEANLIT -> 
            handleTerminal(JavaToken.BOOLEANLIT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.CHARLIT -> 
            handleTerminal(JavaToken.CHARLIT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.INTEGERLIT -> 
            handleTerminal(JavaToken.INTEGERLIT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.STRINGLIT -> 
            handleTerminal(JavaToken.STRINGLIT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.NULLLIT -> 
            handleTerminal(JavaToken.NULLLIT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
      }
    }
  }

  private fun parseType(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, PrimitiveType, state.nonterminalEdges[PrimitiveType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ReferenceType, state.nonterminalEdges[ReferenceType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsePrimitiveType(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, NumericType, state.nonterminalEdges[NumericType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseReferenceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ArrayType, state.nonterminalEdges[ArrayType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, TypeVariable, state.nonterminalEdges[TypeVariable]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ClassOrInterfaceType,
            state.nonterminalEdges[ClassOrInterfaceType]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseAnnotation(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, MarkerAnnotation,
            state.nonterminalEdges[MarkerAnnotation]!!, curSppfNode)
        handleNonterminalEdge(descriptor, SingleElementAnnotation,
            state.nonterminalEdges[SingleElementAnnotation]!!, curSppfNode)
        handleNonterminalEdge(descriptor, NormalAnnotation,
            state.nonterminalEdges[NormalAnnotation]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseNumericType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, IntegralType, state.nonterminalEdges[IntegralType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, FloatingPointType,
            state.nonterminalEdges[FloatingPointType]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseIntegralType(descriptor: Descriptor<VertexType>,
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

  private fun parseFloatingPointType(descriptor: Descriptor<VertexType>,
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

  private fun parseClassOrInterfaceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, InterfaceType, state.nonterminalEdges[InterfaceType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ClassType, state.nonterminalEdges[ClassType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseTypeVariable(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseArrayType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeVariable, state.nonterminalEdges[TypeVariable]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, PrimitiveType, state.nonterminalEdges[PrimitiveType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ClassOrInterfaceType,
            state.nonterminalEdges[ClassOrInterfaceType]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Dims, state.nonterminalEdges[Dims]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseClassType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ClassOrInterfaceType,
            state.nonterminalEdges[ClassOrInterfaceType]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeArguments, state.nonterminalEdges[TypeArguments]!!,
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

  private fun parseInterfaceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ClassType, state.nonterminalEdges[ClassType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseTypeArguments(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeArgumentList,
            state.nonterminalEdges[TypeArgumentList]!!, curSppfNode)
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

  private fun parseDims(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BRACKETLEFT -> 
            handleTerminal(JavaToken.BRACKETLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BRACKETRIGHT -> 
            handleTerminal(JavaToken.BRACKETRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BRACKETLEFT -> 
            handleTerminal(JavaToken.BRACKETLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
    }
  }

  private fun parseTypeParameter(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, TypeParameterModifier,
            state.nonterminalEdges[TypeParameterModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeBound, state.nonterminalEdges[TypeBound]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseTypeParameterModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseTypeBound(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeVariable, state.nonterminalEdges[TypeVariable]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ClassOrInterfaceType,
            state.nonterminalEdges[ClassOrInterfaceType]!!, curSppfNode)
      }
      2 -> 
       {
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AdditionalBound,
            state.nonterminalEdges[AdditionalBound]!!, curSppfNode)
      }
    }
  }

  private fun parseAdditionalBound(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ANDBIT -> 
            handleTerminal(JavaToken.ANDBIT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, InterfaceType, state.nonterminalEdges[InterfaceType]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseTypeArgumentList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeArgument, state.nonterminalEdges[TypeArgument]!!,
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

  private fun parseTypeArgument(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Wildcard, state.nonterminalEdges[Wildcard]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ReferenceType, state.nonterminalEdges[ReferenceType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseWildcard(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.QUESTIONMARK -> 
            handleTerminal(JavaToken.QUESTIONMARK, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, WildcardBounds, state.nonterminalEdges[WildcardBounds]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseWildcardBounds(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, ReferenceType, state.nonterminalEdges[ReferenceType]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseTypeName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, PackageOrTypeName,
            state.nonterminalEdges[PackageOrTypeName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parsePackageOrTypeName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, PackageOrTypeName,
            state.nonterminalEdges[PackageOrTypeName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parseExpressionName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, AmbiguousName, state.nonterminalEdges[AmbiguousName]!!,
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parseAmbiguousName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, AmbiguousName, state.nonterminalEdges[AmbiguousName]!!,
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parseMethodName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsePackageName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, PackageName, state.nonterminalEdges[PackageName]!!,
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parseResult(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
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
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsePackageDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, PackageModifier,
            state.nonterminalEdges[PackageModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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

  private fun parseImportDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeImportOnDemandDeclaration,
            state.nonterminalEdges[TypeImportOnDemandDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, SingleTypeImportDeclaration,
            state.nonterminalEdges[SingleTypeImportDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, SingleStaticImportDeclaration,
            state.nonterminalEdges[SingleStaticImportDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, StaticImportOnDemandDeclaration,
            state.nonterminalEdges[StaticImportOnDemandDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseTypeDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, ClassDeclaration,
            state.nonterminalEdges[ClassDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, InterfaceDeclaration,
            state.nonterminalEdges[InterfaceDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsePackageModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseSingleTypeImportDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeName, state.nonterminalEdges[TypeName]!!, curSppfNode)
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

  private fun parseTypeImportOnDemandDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, PackageOrTypeName,
            state.nonterminalEdges[PackageOrTypeName]!!, curSppfNode)
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
            JavaToken.STAR -> 
            handleTerminal(JavaToken.STAR, state, inputEdge, descriptor, curSppfNode)
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

  private fun parseSingleStaticImportDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeName, state.nonterminalEdges[TypeName]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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

  private fun parseStaticImportOnDemandDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeName, state.nonterminalEdges[TypeName]!!, curSppfNode)
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
            JavaToken.STAR -> 
            handleTerminal(JavaToken.STAR, state, inputEdge, descriptor, curSppfNode)
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

  private fun parseClassDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, NormalClassDeclaration,
            state.nonterminalEdges[NormalClassDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, EnumDeclaration,
            state.nonterminalEdges[EnumDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseInterfaceDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AnnotationTypeDeclaration,
            state.nonterminalEdges[AnnotationTypeDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, NormalInterfaceDeclaration,
            state.nonterminalEdges[NormalInterfaceDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseThrows(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
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
        handleNonterminalEdge(descriptor, ExceptionTypeList,
            state.nonterminalEdges[ExceptionTypeList]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseNormalClassDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, ClassModifier, state.nonterminalEdges[ClassModifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Superclass, state.nonterminalEdges[Superclass]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, TypeParameters, state.nonterminalEdges[TypeParameters]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Superinterfaces,
            state.nonterminalEdges[Superinterfaces]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ClassBody, state.nonterminalEdges[ClassBody]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Superclass, state.nonterminalEdges[Superclass]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Superinterfaces,
            state.nonterminalEdges[Superinterfaces]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ClassBody, state.nonterminalEdges[ClassBody]!!,
            curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Superinterfaces,
            state.nonterminalEdges[Superinterfaces]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ClassBody, state.nonterminalEdges[ClassBody]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ClassBody, state.nonterminalEdges[ClassBody]!!,
            curSppfNode)
      }
      6 -> 
       {
      }
    }
  }

  private fun parseEnumDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, ClassModifier, state.nonterminalEdges[ClassModifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, EnumBody, state.nonterminalEdges[EnumBody]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Superinterfaces,
            state.nonterminalEdges[Superinterfaces]!!, curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, EnumBody, state.nonterminalEdges[EnumBody]!!, curSppfNode)
      }
      4 -> 
       {
      }
    }
  }

  private fun parseClassModifier(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseTypeParameters(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeParameterList,
            state.nonterminalEdges[TypeParameterList]!!, curSppfNode)
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

  private fun parseSuperclass(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, ClassType, state.nonterminalEdges[ClassType]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseSuperinterfaces(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, InterfaceTypeList,
            state.nonterminalEdges[InterfaceTypeList]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseClassBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYLEFT -> 
            handleTerminal(JavaToken.CURLYLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ClassBodyDeclaration,
            state.nonterminalEdges[ClassBodyDeclaration]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseTypeParameterList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeParameter, state.nonterminalEdges[TypeParameter]!!,
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

  private fun parseInterfaceTypeList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, InterfaceType, state.nonterminalEdges[InterfaceType]!!,
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

  private fun parseClassBodyDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, StaticInitializer,
            state.nonterminalEdges[StaticInitializer]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ConstructorDeclaration,
            state.nonterminalEdges[ConstructorDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, InstanceInitializer,
            state.nonterminalEdges[InstanceInitializer]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ClassMemberDeclaration,
            state.nonterminalEdges[ClassMemberDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseClassMemberDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, ClassDeclaration,
            state.nonterminalEdges[ClassDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, MethodDeclaration,
            state.nonterminalEdges[MethodDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, InterfaceDeclaration,
            state.nonterminalEdges[InterfaceDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, FieldDeclaration,
            state.nonterminalEdges[FieldDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseInstanceInitializer(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseStaticInitializer(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseConstructorDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ConstructorDeclarator,
            state.nonterminalEdges[ConstructorDeclarator]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ConstructorModifier,
            state.nonterminalEdges[ConstructorModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ConstructorBody,
            state.nonterminalEdges[ConstructorBody]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Throws, state.nonterminalEdges[Throws]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ConstructorBody,
            state.nonterminalEdges[ConstructorBody]!!, curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseFieldDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, FieldModifier, state.nonterminalEdges[FieldModifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableDeclaratorList,
            state.nonterminalEdges[VariableDeclaratorList]!!, curSppfNode)
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

  private fun parseMethodDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, MethodModifier, state.nonterminalEdges[MethodModifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, MethodHeader, state.nonterminalEdges[MethodHeader]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, MethodBody, state.nonterminalEdges[MethodBody]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseFieldModifier(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseUnannType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannPrimitiveType,
            state.nonterminalEdges[UnannPrimitiveType]!!, curSppfNode)
        handleNonterminalEdge(descriptor, UnannReferenceType,
            state.nonterminalEdges[UnannReferenceType]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseVariableDeclaratorList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableDeclarator,
            state.nonterminalEdges[VariableDeclarator]!!, curSppfNode)
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

  private fun parseVariableDeclarator(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableDeclaratorId,
            state.nonterminalEdges[VariableDeclaratorId]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ASSIGN -> 
            handleTerminal(JavaToken.ASSIGN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableInitializer,
            state.nonterminalEdges[VariableInitializer]!!, curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseVariableDeclaratorId(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Dims, state.nonterminalEdges[Dims]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseVariableInitializer(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ArrayInitializer,
            state.nonterminalEdges[ArrayInitializer]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AssignmentExpression,
            state.nonterminalEdges[AssignmentExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, LambdaExpression,
            state.nonterminalEdges[LambdaExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseArrayInitializer(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYLEFT -> 
            handleTerminal(JavaToken.CURLYLEFT, state, inputEdge, descriptor, curSppfNode)
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
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableInitializerList,
            state.nonterminalEdges[VariableInitializerList]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
    }
  }

  private fun parseUnannPrimitiveType(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, NumericType, state.nonterminalEdges[NumericType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseUnannReferenceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannClassOrInterfaceType,
            state.nonterminalEdges[UnannClassOrInterfaceType]!!, curSppfNode)
        handleNonterminalEdge(descriptor, UnannArrayType, state.nonterminalEdges[UnannArrayType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, UnannTypeVariable,
            state.nonterminalEdges[UnannTypeVariable]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseUnannClassOrInterfaceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannInterfaceType,
            state.nonterminalEdges[UnannInterfaceType]!!, curSppfNode)
        handleNonterminalEdge(descriptor, UnannClassType, state.nonterminalEdges[UnannClassType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseUnannTypeVariable(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseUnannArrayType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannClassOrInterfaceType,
            state.nonterminalEdges[UnannClassOrInterfaceType]!!, curSppfNode)
        handleNonterminalEdge(descriptor, UnannPrimitiveType,
            state.nonterminalEdges[UnannPrimitiveType]!!, curSppfNode)
        handleNonterminalEdge(descriptor, UnannTypeVariable,
            state.nonterminalEdges[UnannTypeVariable]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Dims, state.nonterminalEdges[Dims]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseUnannClassType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, UnannClassOrInterfaceType,
            state.nonterminalEdges[UnannClassOrInterfaceType]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeArguments, state.nonterminalEdges[TypeArguments]!!,
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      4 -> 
       {
      }
    }
  }

  private fun parseUnannInterfaceType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannClassType, state.nonterminalEdges[UnannClassType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseMethodModifier(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseMethodHeader(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeParameters, state.nonterminalEdges[TypeParameters]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Result, state.nonterminalEdges[Result]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, MethodDeclarator,
            state.nonterminalEdges[MethodDeclarator]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Result, state.nonterminalEdges[Result]!!, curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Throws, state.nonterminalEdges[Throws]!!, curSppfNode)
      }
      4 -> 
       {
      }
    }
  }

  private fun parseMethodBody(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseMethodDeclarator(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, FormalParameterList,
            state.nonterminalEdges[FormalParameterList]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Dims, state.nonterminalEdges[Dims]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseFormalParameterList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ReceiverParameter,
            state.nonterminalEdges[ReceiverParameter]!!, curSppfNode)
        handleNonterminalEdge(descriptor, LastFormalParameter,
            state.nonterminalEdges[LastFormalParameter]!!, curSppfNode)
        handleNonterminalEdge(descriptor, FormalParameters,
            state.nonterminalEdges[FormalParameters]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, LastFormalParameter,
            state.nonterminalEdges[LastFormalParameter]!!, curSppfNode)
      }
    }
  }

  private fun parseReceiverParameter(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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

  private fun parseFormalParameters(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, FormalParameter,
            state.nonterminalEdges[FormalParameter]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ReceiverParameter,
            state.nonterminalEdges[ReceiverParameter]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, FormalParameter,
            state.nonterminalEdges[FormalParameter]!!, curSppfNode)
      }
    }
  }

  private fun parseLastFormalParameter(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, FormalParameter,
            state.nonterminalEdges[FormalParameter]!!, curSppfNode)
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, VariableModifier,
            state.nonterminalEdges[VariableModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, VariableModifier,
            state.nonterminalEdges[VariableModifier]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableDeclaratorId,
            state.nonterminalEdges[VariableDeclaratorId]!!, curSppfNode)
      }
    }
  }

  private fun parseFormalParameter(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, VariableModifier,
            state.nonterminalEdges[VariableModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableDeclaratorId,
            state.nonterminalEdges[VariableDeclaratorId]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseVariableModifier(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseExceptionTypeList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ExceptionType, state.nonterminalEdges[ExceptionType]!!,
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

  private fun parseExceptionType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeVariable, state.nonterminalEdges[TypeVariable]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ClassType, state.nonterminalEdges[ClassType]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseBlock(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYLEFT -> 
            handleTerminal(JavaToken.CURLYLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, BlockStatements,
            state.nonterminalEdges[BlockStatements]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parseConstructorModifier(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseConstructorDeclarator(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeParameters, state.nonterminalEdges[TypeParameters]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, SimpleTypeName, state.nonterminalEdges[SimpleTypeName]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, SimpleTypeName, state.nonterminalEdges[SimpleTypeName]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, FormalParameterList,
            state.nonterminalEdges[FormalParameterList]!!, curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
      }
    }
  }

  private fun parseConstructorBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYLEFT -> 
            handleTerminal(JavaToken.CURLYLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ExplicitConstructorInvocation,
            state.nonterminalEdges[ExplicitConstructorInvocation]!!, curSppfNode)
        handleNonterminalEdge(descriptor, BlockStatements,
            state.nonterminalEdges[BlockStatements]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, BlockStatements,
            state.nonterminalEdges[BlockStatements]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
    }
  }

  private fun parseSimpleTypeName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseExplicitConstructorInvocation(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeArguments, state.nonterminalEdges[TypeArguments]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Primary, state.nonterminalEdges[Primary]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ExpressionName, state.nonterminalEdges[ExpressionName]!!,
            curSppfNode)
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
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
        handleNonterminalEdge(descriptor, TypeArguments, state.nonterminalEdges[TypeArguments]!!,
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
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ArgumentList, state.nonterminalEdges[ArgumentList]!!,
            curSppfNode)
      }
      7 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
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

  private fun parseEnumBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYLEFT -> 
            handleTerminal(JavaToken.CURLYLEFT, state, inputEdge, descriptor, curSppfNode)
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
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, EnumConstantList,
            state.nonterminalEdges[EnumConstantList]!!, curSppfNode)
        handleNonterminalEdge(descriptor, EnumBodyDeclarations,
            state.nonterminalEdges[EnumBodyDeclarations]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, EnumBodyDeclarations,
            state.nonterminalEdges[EnumBodyDeclarations]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, EnumBodyDeclarations,
            state.nonterminalEdges[EnumBodyDeclarations]!!, curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
      }
    }
  }

  private fun parseEnumConstantList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, EnumConstant, state.nonterminalEdges[EnumConstant]!!,
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

  private fun parseEnumConstant(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, EnumConstantModifier,
            state.nonterminalEdges[EnumConstantModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ArgumentList, state.nonterminalEdges[ArgumentList]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ClassBody, state.nonterminalEdges[ClassBody]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseEnumConstantModifier(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseEnumBodyDeclarations(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, ClassBodyDeclaration,
            state.nonterminalEdges[ClassBodyDeclaration]!!, curSppfNode)
      }
    }
  }

  private fun parseBlockStatements(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, BlockStatement, state.nonterminalEdges[BlockStatement]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, BlockStatement, state.nonterminalEdges[BlockStatement]!!,
            curSppfNode)
      }
    }
  }

  private fun parseArgumentList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
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

  private fun parsePrimary(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, PrimaryNoNewArray,
            state.nonterminalEdges[PrimaryNoNewArray]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ArrayCreationExpression,
            state.nonterminalEdges[ArrayCreationExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseNormalInterfaceDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, InterfaceModifier,
            state.nonterminalEdges[InterfaceModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, InterfaceBody, state.nonterminalEdges[InterfaceBody]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, TypeParameters, state.nonterminalEdges[TypeParameters]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ExtendsInterfaces,
            state.nonterminalEdges[ExtendsInterfaces]!!, curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, InterfaceBody, state.nonterminalEdges[InterfaceBody]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ExtendsInterfaces,
            state.nonterminalEdges[ExtendsInterfaces]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, InterfaceBody, state.nonterminalEdges[InterfaceBody]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseInterfaceModifier(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseExtendsInterfaces(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, InterfaceTypeList,
            state.nonterminalEdges[InterfaceTypeList]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseInterfaceBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYLEFT -> 
            handleTerminal(JavaToken.CURLYLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, InterfaceMemberDeclaration,
            state.nonterminalEdges[InterfaceMemberDeclaration]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseInterfaceMemberDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, InterfaceMethodDeclaration,
            state.nonterminalEdges[InterfaceMethodDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ClassDeclaration,
            state.nonterminalEdges[ClassDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ConstantDeclaration,
            state.nonterminalEdges[ConstantDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, InterfaceDeclaration,
            state.nonterminalEdges[InterfaceDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseConstantDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ConstantModifier,
            state.nonterminalEdges[ConstantModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableDeclaratorList,
            state.nonterminalEdges[VariableDeclaratorList]!!, curSppfNode)
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

  private fun parseConstantModifier(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseAnnotationTypeDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, InterfaceModifier,
            state.nonterminalEdges[InterfaceModifier]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AnnotationTypeBody,
            state.nonterminalEdges[AnnotationTypeBody]!!, curSppfNode)
      }
      4 -> 
       {
      }
    }
  }

  private fun parseAnnotationTypeBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYLEFT -> 
            handleTerminal(JavaToken.CURLYLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AnnotationTypeMemberDeclaration,
            state.nonterminalEdges[AnnotationTypeMemberDeclaration]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseAnnotationTypeMemberDeclaration(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, AnnotationTypeElementDeclaration,
            state.nonterminalEdges[AnnotationTypeElementDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ClassDeclaration,
            state.nonterminalEdges[ClassDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ConstantDeclaration,
            state.nonterminalEdges[ConstantDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, InterfaceDeclaration,
            state.nonterminalEdges[InterfaceDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseAnnotationTypeElementDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, AnnotationTypeElementModifier,
            state.nonterminalEdges[AnnotationTypeElementModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
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
        handleNonterminalEdge(descriptor, DefaultValue, state.nonterminalEdges[DefaultValue]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Dims, state.nonterminalEdges[Dims]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, DefaultValue, state.nonterminalEdges[DefaultValue]!!,
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

  private fun parseDefaultValue(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, ElementValue, state.nonterminalEdges[ElementValue]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseNormalAnnotation(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeName, state.nonterminalEdges[TypeName]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ElementValuePairList,
            state.nonterminalEdges[ElementValuePairList]!!, curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
      }
    }
  }

  private fun parseElementValuePairList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ElementValuePair,
            state.nonterminalEdges[ElementValuePair]!!, curSppfNode)
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

  private fun parseElementValuePair(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ASSIGN -> 
            handleTerminal(JavaToken.ASSIGN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ElementValue, state.nonterminalEdges[ElementValue]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseElementValue(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ElementValueArrayInitializer,
            state.nonterminalEdges[ElementValueArrayInitializer]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ConditionalExpression,
            state.nonterminalEdges[ConditionalExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseElementValueArrayInitializer(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYLEFT -> 
            handleTerminal(JavaToken.CURLYLEFT, state, inputEdge, descriptor, curSppfNode)
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
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ElementValueList,
            state.nonterminalEdges[ElementValueList]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.COMMA -> 
            handleTerminal(JavaToken.COMMA, state, inputEdge, descriptor, curSppfNode)
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
    }
  }

  private fun parseElementValueList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ElementValue, state.nonterminalEdges[ElementValue]!!,
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

  private fun parseMarkerAnnotation(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeName, state.nonterminalEdges[TypeName]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseSingleElementAnnotation(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeName, state.nonterminalEdges[TypeName]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ElementValue, state.nonterminalEdges[ElementValue]!!,
            curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      5 -> 
       {
      }
    }
  }

  private fun parseInterfaceMethodDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, InterfaceMethodModifier,
            state.nonterminalEdges[InterfaceMethodModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, MethodHeader, state.nonterminalEdges[MethodHeader]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, MethodBody, state.nonterminalEdges[MethodBody]!!,
            curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseAnnotationTypeElementModifier(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseConditionalExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ConditionalOrExpression,
            state.nonterminalEdges[ConditionalOrExpression]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.QUESTIONMARK -> 
            handleTerminal(JavaToken.QUESTIONMARK, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
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
        handleNonterminalEdge(descriptor, ConditionalExpression,
            state.nonterminalEdges[ConditionalExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, LambdaExpression,
            state.nonterminalEdges[LambdaExpression]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseVariableInitializerList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableInitializer,
            state.nonterminalEdges[VariableInitializer]!!, curSppfNode)
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

  private fun parseBlockStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ClassDeclaration,
            state.nonterminalEdges[ClassDeclaration]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Statement, state.nonterminalEdges[Statement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, LocalVariableDeclarationStatement,
            state.nonterminalEdges[LocalVariableDeclarationStatement]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseLocalVariableDeclarationStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, LocalVariableDeclaration,
            state.nonterminalEdges[LocalVariableDeclaration]!!, curSppfNode)
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

  private fun parseLocalVariableDeclaration(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, VariableModifier,
            state.nonterminalEdges[VariableModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableDeclaratorList,
            state.nonterminalEdges[VariableDeclaratorList]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, LabeledStatement,
            state.nonterminalEdges[LabeledStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, StatementWithoutTrailingSubstatement,
            state.nonterminalEdges[StatementWithoutTrailingSubstatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, IfThenElseStatement,
            state.nonterminalEdges[IfThenElseStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, WhileStatement, state.nonterminalEdges[WhileStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ForStatement, state.nonterminalEdges[ForStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, IfThenStatement,
            state.nonterminalEdges[IfThenStatement]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseStatementNoShortIf(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, WhileStatementNoShortIf,
            state.nonterminalEdges[WhileStatementNoShortIf]!!, curSppfNode)
        handleNonterminalEdge(descriptor, StatementWithoutTrailingSubstatement,
            state.nonterminalEdges[StatementWithoutTrailingSubstatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, LabeledStatementNoShortIf,
            state.nonterminalEdges[LabeledStatementNoShortIf]!!, curSppfNode)
        handleNonterminalEdge(descriptor, IfThenElseStatementNoShortIf,
            state.nonterminalEdges[IfThenElseStatementNoShortIf]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ForStatementNoShortIf,
            state.nonterminalEdges[ForStatementNoShortIf]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseStatementWithoutTrailingSubstatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, BreakStatement, state.nonterminalEdges[BreakStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, SwitchStatement,
            state.nonterminalEdges[SwitchStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, DoStatement, state.nonterminalEdges[DoStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, TryStatement, state.nonterminalEdges[TryStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ExpressionStatement,
            state.nonterminalEdges[ExpressionStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, EmptyStatement, state.nonterminalEdges[EmptyStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, AssertStatement,
            state.nonterminalEdges[AssertStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ReturnStatement,
            state.nonterminalEdges[ReturnStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ThrowStatement, state.nonterminalEdges[ThrowStatement]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, SynchronizedStatement,
            state.nonterminalEdges[SynchronizedStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ContinueStatement,
            state.nonterminalEdges[ContinueStatement]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseEmptyStatement(descriptor: Descriptor<VertexType>,
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

  private fun parseLabeledStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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
        handleNonterminalEdge(descriptor, Statement, state.nonterminalEdges[Statement]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseLabeledStatementNoShortIf(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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
        handleNonterminalEdge(descriptor, StatementNoShortIf,
            state.nonterminalEdges[StatementNoShortIf]!!, curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseExpressionStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, StatementExpression,
            state.nonterminalEdges[StatementExpression]!!, curSppfNode)
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

  private fun parseStatementExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Assignment, state.nonterminalEdges[Assignment]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, PostIncrementExpression,
            state.nonterminalEdges[PostIncrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, PreDecrementExpression,
            state.nonterminalEdges[PreDecrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, PreIncrementExpression,
            state.nonterminalEdges[PreIncrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, PostDecrementExpression,
            state.nonterminalEdges[PostDecrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, MethodInvocation,
            state.nonterminalEdges[MethodInvocation]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ClassInstanceCreationExpression,
            state.nonterminalEdges[ClassInstanceCreationExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseIfThenStatement(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Statement, state.nonterminalEdges[Statement]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseIfThenElseStatement(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, StatementNoShortIf,
            state.nonterminalEdges[StatementNoShortIf]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, Statement, state.nonterminalEdges[Statement]!!,
            curSppfNode)
      }
      7 -> 
       {
      }
    }
  }

  private fun parseIfThenElseStatementNoShortIf(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, StatementNoShortIf,
            state.nonterminalEdges[StatementNoShortIf]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, StatementNoShortIf,
            state.nonterminalEdges[StatementNoShortIf]!!, curSppfNode)
      }
      7 -> 
       {
      }
    }
  }

  private fun parseAssertStatement(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
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
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
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

  private fun parseSwitchStatement(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, SwitchBlock, state.nonterminalEdges[SwitchBlock]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseSwitchBlock(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYLEFT -> 
            handleTerminal(JavaToken.CURLYLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, SwitchLabel, state.nonterminalEdges[SwitchLabel]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, SwitchBlockStatementGroup,
            state.nonterminalEdges[SwitchBlockStatementGroup]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.CURLYRIGHT -> 
            handleTerminal(JavaToken.CURLYRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, SwitchLabel, state.nonterminalEdges[SwitchLabel]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseSwitchBlockStatementGroup(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, SwitchLabels, state.nonterminalEdges[SwitchLabels]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, BlockStatements,
            state.nonterminalEdges[BlockStatements]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseSwitchLabels(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, SwitchLabel, state.nonterminalEdges[SwitchLabel]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, SwitchLabel, state.nonterminalEdges[SwitchLabel]!!,
            curSppfNode)
      }
    }
  }

  private fun parseSwitchLabel(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, EnumConstantName,
            state.nonterminalEdges[EnumConstantName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ConstantExpression,
            state.nonterminalEdges[ConstantExpression]!!, curSppfNode)
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

  private fun parseEnumConstantName(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseWhileStatement(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Statement, state.nonterminalEdges[Statement]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseWhileStatementNoShortIf(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, StatementNoShortIf,
            state.nonterminalEdges[StatementNoShortIf]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseDoStatement(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Statement, state.nonterminalEdges[Statement]!!,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
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

  private fun parseInterfaceMethodModifier(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseForStatement(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, BasicForStatement,
            state.nonterminalEdges[BasicForStatement]!!, curSppfNode)
        handleNonterminalEdge(descriptor, EnhancedForStatement,
            state.nonterminalEdges[EnhancedForStatement]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseForStatementNoShortIf(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, BasicForStatementNoShortIf,
            state.nonterminalEdges[BasicForStatementNoShortIf]!!, curSppfNode)
        handleNonterminalEdge(descriptor, EnhancedForStatementNoShortIf,
            state.nonterminalEdges[EnhancedForStatementNoShortIf]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseBasicForStatement(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
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
        handleNonterminalEdge(descriptor, ForInit, state.nonterminalEdges[ForInit]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ForUpdate, state.nonterminalEdges[ForUpdate]!!,
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
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      8 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Statement, state.nonterminalEdges[Statement]!!,
            curSppfNode)
      }
      9 -> 
       {
      }
    }
  }

  private fun parseBasicForStatementNoShortIf(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
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
        handleNonterminalEdge(descriptor, ForInit, state.nonterminalEdges[ForInit]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ForUpdate, state.nonterminalEdges[ForUpdate]!!,
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
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      8 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, StatementNoShortIf,
            state.nonterminalEdges[StatementNoShortIf]!!, curSppfNode)
      }
      9 -> 
       {
      }
    }
  }

  private fun parseForInit(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, StatementExpressionList,
            state.nonterminalEdges[StatementExpressionList]!!, curSppfNode)
        handleNonterminalEdge(descriptor, LocalVariableDeclaration,
            state.nonterminalEdges[LocalVariableDeclaration]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseForUpdate(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, StatementExpressionList,
            state.nonterminalEdges[StatementExpressionList]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseStatementExpressionList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, StatementExpression,
            state.nonterminalEdges[StatementExpression]!!, curSppfNode)
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

  private fun parseEnhancedForStatement(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, VariableModifier,
            state.nonterminalEdges[VariableModifier]!!, curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableDeclaratorId,
            state.nonterminalEdges[VariableDeclaratorId]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      6 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      7 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Statement, state.nonterminalEdges[Statement]!!,
            curSppfNode)
      }
      8 -> 
       {
      }
    }
  }

  private fun parseEnhancedForStatementNoShortIf(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, VariableModifier,
            state.nonterminalEdges[VariableModifier]!!, curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableDeclaratorId,
            state.nonterminalEdges[VariableDeclaratorId]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      6 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      7 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, StatementNoShortIf,
            state.nonterminalEdges[StatementNoShortIf]!!, curSppfNode)
      }
      8 -> 
       {
      }
    }
  }

  private fun parseBreakStatement(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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

  private fun parseContinueStatement(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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

  private fun parseReturnStatement(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
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

  private fun parseThrowStatement(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
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

  private fun parseSynchronizedStatement(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseTryStatement(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TryWithResourcesStatement,
            state.nonterminalEdges[TryWithResourcesStatement]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
      }
      2 -> 
       {
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Catches, state.nonterminalEdges[Catches]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Finally, state.nonterminalEdges[Finally]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Finally, state.nonterminalEdges[Finally]!!, curSppfNode)
      }
    }
  }

  private fun parseCatches(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, CatchClause, state.nonterminalEdges[CatchClause]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, CatchClause, state.nonterminalEdges[CatchClause]!!,
            curSppfNode)
      }
    }
  }

  private fun parseCatchClause(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, CatchFormalParameter,
            state.nonterminalEdges[CatchFormalParameter]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseCatchFormalParameter(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableModifier,
            state.nonterminalEdges[VariableModifier]!!, curSppfNode)
        handleNonterminalEdge(descriptor, CatchType, state.nonterminalEdges[CatchType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableDeclaratorId,
            state.nonterminalEdges[VariableDeclaratorId]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseCatchType(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannClassType, state.nonterminalEdges[UnannClassType]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ORBIT -> 
            handleTerminal(JavaToken.ORBIT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ClassType, state.nonterminalEdges[ClassType]!!,
            curSppfNode)
      }
    }
  }

  private fun parseFinally(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
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
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseTryWithResourcesStatement(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, ResourceSpecification,
            state.nonterminalEdges[ResourceSpecification]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Catches, state.nonterminalEdges[Catches]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Finally, state.nonterminalEdges[Finally]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Finally, state.nonterminalEdges[Finally]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseResourceSpecification(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ResourceList, state.nonterminalEdges[ResourceList]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
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
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
    }
  }

  private fun parseResourceList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Resource, state.nonterminalEdges[Resource]!!, curSppfNode)
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

  private fun parseResource(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnannType, state.nonterminalEdges[UnannType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, VariableModifier,
            state.nonterminalEdges[VariableModifier]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, VariableDeclaratorId,
            state.nonterminalEdges[VariableDeclaratorId]!!, curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ASSIGN -> 
            handleTerminal(JavaToken.ASSIGN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      4 -> 
       {
      }
    }
  }

  private fun parsePrimaryNoNewArray(descriptor: Descriptor<VertexType>,
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, TypeName, state.nonterminalEdges[TypeName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ArrayAccess, state.nonterminalEdges[ArrayAccess]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Literal, state.nonterminalEdges[Literal]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ClassInstanceCreationExpression,
            state.nonterminalEdges[ClassInstanceCreationExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, MethodInvocation,
            state.nonterminalEdges[MethodInvocation]!!, curSppfNode)
        handleNonterminalEdge(descriptor, MethodReference,
            state.nonterminalEdges[MethodReference]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ClassLiteral, state.nonterminalEdges[ClassLiteral]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, FieldAccess, state.nonterminalEdges[FieldAccess]!!,
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
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
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

  private fun parseClassLiteral(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, NumericType, state.nonterminalEdges[NumericType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, TypeName, state.nonterminalEdges[TypeName]!!, curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BRACKETLEFT -> 
            handleTerminal(JavaToken.BRACKETLEFT, state, inputEdge, descriptor, curSppfNode)
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
            JavaToken.BRACKETRIGHT -> 
            handleTerminal(JavaToken.BRACKETRIGHT, state, inputEdge, descriptor, curSppfNode)
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
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
        handleNonterminalEdge(descriptor, TypeArgumentsOrDiamond,
            state.nonterminalEdges[TypeArgumentsOrDiamond]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseUnqualifiedClassInstanceCreationExpression(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, classOrInterfaceTypeToInstantiate,
            state.nonterminalEdges[classOrInterfaceTypeToInstantiate]!!, curSppfNode)
        handleNonterminalEdge(descriptor, TypeArguments, state.nonterminalEdges[TypeArguments]!!,
            curSppfNode)
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
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ArgumentList, state.nonterminalEdges[ArgumentList]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      6 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ClassBody, state.nonterminalEdges[ClassBody]!!,
            curSppfNode)
      }
      7 -> 
       {
      }
    }
  }

  private fun parseClassInstanceCreationExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnqualifiedClassInstanceCreationExpression,
            state.nonterminalEdges[UnqualifiedClassInstanceCreationExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Primary, state.nonterminalEdges[Primary]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ExpressionName, state.nonterminalEdges[ExpressionName]!!,
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
        handleNonterminalEdge(descriptor, UnqualifiedClassInstanceCreationExpression,
            state.nonterminalEdges[UnqualifiedClassInstanceCreationExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseFieldAccess(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeName, state.nonterminalEdges[TypeName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Primary, state.nonterminalEdges[Primary]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseTypeArgumentsOrDiamond(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeArguments, state.nonterminalEdges[TypeArguments]!!,
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

  private fun parseArrayAccess(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, PrimaryNoNewArray,
            state.nonterminalEdges[PrimaryNoNewArray]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ExpressionName, state.nonterminalEdges[ExpressionName]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BRACKETLEFT -> 
            handleTerminal(JavaToken.BRACKETLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BRACKETRIGHT -> 
            handleTerminal(JavaToken.BRACKETRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
      }
    }
  }

  private fun parseMethodInvocation(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, TypeName, state.nonterminalEdges[TypeName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Primary, state.nonterminalEdges[Primary]!!, curSppfNode)
        handleNonterminalEdge(descriptor, MethodName, state.nonterminalEdges[MethodName]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ExpressionName, state.nonterminalEdges[ExpressionName]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, TypeArguments, state.nonterminalEdges[TypeArguments]!!,
            curSppfNode)
      }
      5 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, TypeArguments, state.nonterminalEdges[TypeArguments]!!,
            curSppfNode)
      }
      7 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ArgumentList, state.nonterminalEdges[ArgumentList]!!,
            curSppfNode)
      }
      8 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      9 -> 
       {
      }
    }
  }

  private fun parseMethodReference(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, ArrayType, state.nonterminalEdges[ArrayType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, TypeName, state.nonterminalEdges[TypeName]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Primary, state.nonterminalEdges[Primary]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ClassType, state.nonterminalEdges[ClassType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ReferenceType, state.nonterminalEdges[ReferenceType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ExpressionName, state.nonterminalEdges[ExpressionName]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOUBLECOLON -> 
            handleTerminal(JavaToken.DOUBLECOLON, state, inputEdge, descriptor, curSppfNode)
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
            JavaToken.DOUBLECOLON -> 
            handleTerminal(JavaToken.DOUBLECOLON, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      4 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.DOUBLECOLON -> 
            handleTerminal(JavaToken.DOUBLECOLON, state, inputEdge, descriptor, curSppfNode)
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
        handleNonterminalEdge(descriptor, TypeArguments, state.nonterminalEdges[TypeArguments]!!,
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
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, TypeArguments, state.nonterminalEdges[TypeArguments]!!,
            curSppfNode)
      }
      10 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
            curSppfNode)
      }
    }
  }

  private fun parseArrayCreationExpression(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, PrimitiveType, state.nonterminalEdges[PrimitiveType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ClassOrInterfaceType,
            state.nonterminalEdges[ClassOrInterfaceType]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, DimExprs, state.nonterminalEdges[DimExprs]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Dims, state.nonterminalEdges[Dims]!!, curSppfNode)
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Dims, state.nonterminalEdges[Dims]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ArrayInitializer,
            state.nonterminalEdges[ArrayInitializer]!!, curSppfNode)
      }
      5 -> 
       {
      }
    }
  }

  private fun parseDimExprs(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, DimExpr, state.nonterminalEdges[DimExpr]!!, curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, DimExpr, state.nonterminalEdges[DimExpr]!!, curSppfNode)
      }
    }
  }

  private fun parseDimExpr(descriptor: Descriptor<VertexType>, curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BRACKETLEFT -> 
            handleTerminal(JavaToken.BRACKETLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Annotation, state.nonterminalEdges[Annotation]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.BRACKETRIGHT -> 
            handleTerminal(JavaToken.BRACKETRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
      }
    }
  }

  private fun parseLambdaExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, LambdaParameters,
            state.nonterminalEdges[LambdaParameters]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, LambdaBody, state.nonterminalEdges[LambdaBody]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseLambdaParameters(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, FormalParameterList,
            state.nonterminalEdges[FormalParameterList]!!, curSppfNode)
        handleNonterminalEdge(descriptor, InferredFormalParameterList,
            state.nonterminalEdges[InferredFormalParameterList]!!, curSppfNode)
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
    }
  }

  private fun parseInferredFormalParameterList(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Identifier, state.nonterminalEdges[Identifier]!!,
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

  private fun parseLambdaBody(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, Block, state.nonterminalEdges[Block]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseAssignmentExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Assignment, state.nonterminalEdges[Assignment]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ConditionalExpression,
            state.nonterminalEdges[ConditionalExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseAssignment(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, LeftHandSide, state.nonterminalEdges[LeftHandSide]!!,
            curSppfNode)
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AssignmentOperator,
            state.nonterminalEdges[AssignmentOperator]!!, curSppfNode)
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      3 -> 
       {
      }
    }
  }

  private fun parseLeftHandSide(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ArrayAccess, state.nonterminalEdges[ArrayAccess]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, FieldAccess, state.nonterminalEdges[FieldAccess]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ExpressionName, state.nonterminalEdges[ExpressionName]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parseAssignmentOperator(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PERCENTASSIGN -> 
            handleTerminal(JavaToken.PERCENTASSIGN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.ORASSIGN -> 
            handleTerminal(JavaToken.ORASSIGN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.STARASSIGN -> 
            handleTerminal(JavaToken.STARASSIGN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.SLASHASSIGN -> 
            handleTerminal(JavaToken.SLASHASSIGN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PLUSASSIGN -> 
            handleTerminal(JavaToken.PLUSASSIGN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.USRIGHTSHIFTASSIGN -> 
            handleTerminal(JavaToken.USRIGHTSHIFTASSIGN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.MINUSASSIGN -> 
            handleTerminal(JavaToken.MINUSASSIGN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.SHIFTLEFTASSIGN -> 
            handleTerminal(JavaToken.SHIFTLEFTASSIGN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.ANDASSIGN -> 
            handleTerminal(JavaToken.ANDASSIGN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.ASSIGN -> 
            handleTerminal(JavaToken.ASSIGN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.SHIFTRIGHTASSIGN -> 
            handleTerminal(JavaToken.SHIFTRIGHTASSIGN, state, inputEdge, descriptor, curSppfNode)
            JavaToken.XORASSIGN -> 
            handleTerminal(JavaToken.XORASSIGN, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
      }
    }
  }

  private fun parseConditionalOrExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ConditionalAndExpression,
            state.nonterminalEdges[ConditionalAndExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ConditionalOrExpression,
            state.nonterminalEdges[ConditionalOrExpression]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, ConditionalAndExpression,
            state.nonterminalEdges[ConditionalAndExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseConditionalAndExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, InclusiveOrExpression,
            state.nonterminalEdges[InclusiveOrExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ConditionalAndExpression,
            state.nonterminalEdges[ConditionalAndExpression]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, InclusiveOrExpression,
            state.nonterminalEdges[InclusiveOrExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseInclusiveOrExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, InclusiveOrExpression,
            state.nonterminalEdges[InclusiveOrExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ExclusiveOrExpression,
            state.nonterminalEdges[ExclusiveOrExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ORBIT -> 
            handleTerminal(JavaToken.ORBIT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ExclusiveOrExpression,
            state.nonterminalEdges[ExclusiveOrExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseExclusiveOrExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AndExpression, state.nonterminalEdges[AndExpression]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ExclusiveOrExpression,
            state.nonterminalEdges[ExclusiveOrExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.XORBIT -> 
            handleTerminal(JavaToken.XORBIT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AndExpression, state.nonterminalEdges[AndExpression]!!,
            curSppfNode)
      }
    }
  }

  private fun parseAndExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AndExpression, state.nonterminalEdges[AndExpression]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, EqualityExpression,
            state.nonterminalEdges[EqualityExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.ANDBIT -> 
            handleTerminal(JavaToken.ANDBIT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, EqualityExpression,
            state.nonterminalEdges[EqualityExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseEqualityExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, RelationalExpression,
            state.nonterminalEdges[RelationalExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, EqualityExpression,
            state.nonterminalEdges[EqualityExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.EQ -> 
            handleTerminal(JavaToken.EQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.NOTEQ -> 
            handleTerminal(JavaToken.NOTEQ, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, RelationalExpression,
            state.nonterminalEdges[RelationalExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseRelationalExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, RelationalExpression,
            state.nonterminalEdges[RelationalExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ShiftExpression,
            state.nonterminalEdges[ShiftExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.LESSEQ -> 
            handleTerminal(JavaToken.LESSEQ, state, inputEdge, descriptor, curSppfNode)
            JavaToken.INSTANCEOF -> 
            handleTerminal(JavaToken.INSTANCEOF, state, inputEdge, descriptor, curSppfNode)
            JavaToken.GREATEQ -> 
            handleTerminal(JavaToken.GREATEQ, state, inputEdge, descriptor, curSppfNode)
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
        handleNonterminalEdge(descriptor, ShiftExpression,
            state.nonterminalEdges[ShiftExpression]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, ReferenceType, state.nonterminalEdges[ReferenceType]!!,
            curSppfNode)
      }
    }
  }

  private fun parseShiftExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AdditiveExpression,
            state.nonterminalEdges[AdditiveExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ShiftExpression,
            state.nonterminalEdges[ShiftExpression]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, AdditiveExpression,
            state.nonterminalEdges[AdditiveExpression]!!, curSppfNode)
      }
      6 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AdditiveExpression,
            state.nonterminalEdges[AdditiveExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseAdditiveExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AdditiveExpression,
            state.nonterminalEdges[AdditiveExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, MultiplicativeExpression,
            state.nonterminalEdges[MultiplicativeExpression]!!, curSppfNode)
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
        handleNonterminalEdge(descriptor, MultiplicativeExpression,
            state.nonterminalEdges[MultiplicativeExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseMultiplicativeExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnaryExpression,
            state.nonterminalEdges[UnaryExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, MultiplicativeExpression,
            state.nonterminalEdges[MultiplicativeExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.SLASH -> 
            handleTerminal(JavaToken.SLASH, state, inputEdge, descriptor, curSppfNode)
            JavaToken.PERCENT -> 
            handleTerminal(JavaToken.PERCENT, state, inputEdge, descriptor, curSppfNode)
            JavaToken.STAR -> 
            handleTerminal(JavaToken.STAR, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnaryExpression,
            state.nonterminalEdges[UnaryExpression]!!, curSppfNode)
      }
    }
  }

  private fun parsePreIncrementExpression(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, UnaryExpression,
            state.nonterminalEdges[UnaryExpression]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parsePreDecrementExpression(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, UnaryExpression,
            state.nonterminalEdges[UnaryExpression]!!, curSppfNode)
      }
      2 -> 
       {
      }
    }
  }

  private fun parseUnaryExpressionNotPlusMinus(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.EXCLAMATIONMARK -> 
            handleTerminal(JavaToken.EXCLAMATIONMARK, state, inputEdge, descriptor, curSppfNode)
            JavaToken.TILDA -> 
            handleTerminal(JavaToken.TILDA, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, PostfixExpression,
            state.nonterminalEdges[PostfixExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, CastExpression, state.nonterminalEdges[CastExpression]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnaryExpression,
            state.nonterminalEdges[UnaryExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseUnaryExpression(descriptor: Descriptor<VertexType>,
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
        handleNonterminalEdge(descriptor, PreDecrementExpression,
            state.nonterminalEdges[PreDecrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, UnaryExpressionNotPlusMinus,
            state.nonterminalEdges[UnaryExpressionNotPlusMinus]!!, curSppfNode)
        handleNonterminalEdge(descriptor, PreIncrementExpression,
            state.nonterminalEdges[PreIncrementExpression]!!, curSppfNode)
      }
      1 -> 
       {
      }
      2 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnaryExpression,
            state.nonterminalEdges[UnaryExpression]!!, curSppfNode)
      }
    }
  }

  private fun parsePostfixExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, PostIncrementExpression,
            state.nonterminalEdges[PostIncrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, Primary, state.nonterminalEdges[Primary]!!, curSppfNode)
        handleNonterminalEdge(descriptor, PostDecrementExpression,
            state.nonterminalEdges[PostDecrementExpression]!!, curSppfNode)
        handleNonterminalEdge(descriptor, ExpressionName, state.nonterminalEdges[ExpressionName]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  private fun parsePostIncrementExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, PostfixExpression,
            state.nonterminalEdges[PostfixExpression]!!, curSppfNode)
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

  private fun parsePostDecrementExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, PostfixExpression,
            state.nonterminalEdges[PostfixExpression]!!, curSppfNode)
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

  private fun parseCastExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    val pos = descriptor.inputPosition
    when(state.numId) {
      0 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHLEFT -> 
            handleTerminal(JavaToken.PARENTHLEFT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      1 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, PrimitiveType, state.nonterminalEdges[PrimitiveType]!!,
            curSppfNode)
        handleNonterminalEdge(descriptor, ReferenceType, state.nonterminalEdges[ReferenceType]!!,
            curSppfNode)
      }
      2 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
      }
      3 -> 
       {
        // handle terminal edges
        for (inputEdge in ctx.input.getEdges(pos)) {
          when(inputEdge.label.terminal) {
            JavaToken.PARENTHRIGHT -> 
            handleTerminal(JavaToken.PARENTHRIGHT, state, inputEdge, descriptor, curSppfNode)
            else -> {}
          }
        }
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, AdditionalBound,
            state.nonterminalEdges[AdditionalBound]!!, curSppfNode)
      }
      4 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnaryExpressionNotPlusMinus,
            state.nonterminalEdges[UnaryExpressionNotPlusMinus]!!, curSppfNode)
        handleNonterminalEdge(descriptor, LambdaExpression,
            state.nonterminalEdges[LambdaExpression]!!, curSppfNode)
      }
      5 -> 
       {
      }
      6 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, UnaryExpression,
            state.nonterminalEdges[UnaryExpression]!!, curSppfNode)
      }
    }
  }

  private fun parseConstantExpression(descriptor: Descriptor<VertexType>,
      curSppfNode: SppfNode<VertexType>?) {
    val state = descriptor.rsmState
    when(state.numId) {
      0 -> 
       {
        // handle nonterminal edges
        handleNonterminalEdge(descriptor, Expression, state.nonterminalEdges[Expression]!!,
            curSppfNode)
      }
      1 -> 
       {
      }
    }
  }

  override fun setInput(`value`: IInputGraph<VertexType, LabelType>) {
    ctx = org.ucfs.parser.context.Context(grammar.rsm, value)
  }
}
