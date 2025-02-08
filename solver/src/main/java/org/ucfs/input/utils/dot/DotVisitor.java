// Generated from Dot.g4 by ANTLR 4.13.2
package org.ucfs.input.utils.dot;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link DotParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface DotVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link DotParser#graph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGraph(DotParser.GraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#stmt_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt_list(DotParser.Stmt_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt(DotParser.StmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#attr_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttr_stmt(DotParser.Attr_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#attr_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttr_list(DotParser.Attr_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#attr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttr(DotParser.AttrContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#edge_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEdge_stmt(DotParser.Edge_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#edgeRHS}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEdgeRHS(DotParser.EdgeRHSContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#edgeop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEdgeop(DotParser.EdgeopContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#node_stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNode_stmt(DotParser.Node_stmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#node_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNode_id(DotParser.Node_idContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#port}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPort(DotParser.PortContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#subgraph}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubgraph(DotParser.SubgraphContext ctx);
	/**
	 * Visit a parse tree produced by {@link DotParser#id_}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId_(DotParser.Id_Context ctx);
}