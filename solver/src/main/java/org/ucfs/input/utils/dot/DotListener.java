// Generated from Dot.g4 by ANTLR 4.13.2
package org.ucfs.input.utils.dot;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link DotParser}.
 */
public interface DotListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link DotParser#graph}.
	 * @param ctx the parse tree
	 */
	void enterGraph(DotParser.GraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#graph}.
	 * @param ctx the parse tree
	 */
	void exitGraph(DotParser.GraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#stmt_list}.
	 * @param ctx the parse tree
	 */
	void enterStmt_list(DotParser.Stmt_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#stmt_list}.
	 * @param ctx the parse tree
	 */
	void exitStmt_list(DotParser.Stmt_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#stmt}.
	 * @param ctx the parse tree
	 */
	void enterStmt(DotParser.StmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#stmt}.
	 * @param ctx the parse tree
	 */
	void exitStmt(DotParser.StmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#attr_stmt}.
	 * @param ctx the parse tree
	 */
	void enterAttr_stmt(DotParser.Attr_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#attr_stmt}.
	 * @param ctx the parse tree
	 */
	void exitAttr_stmt(DotParser.Attr_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#attr_list}.
	 * @param ctx the parse tree
	 */
	void enterAttr_list(DotParser.Attr_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#attr_list}.
	 * @param ctx the parse tree
	 */
	void exitAttr_list(DotParser.Attr_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#attr}.
	 * @param ctx the parse tree
	 */
	void enterAttr(DotParser.AttrContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#attr}.
	 * @param ctx the parse tree
	 */
	void exitAttr(DotParser.AttrContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#edge_stmt}.
	 * @param ctx the parse tree
	 */
	void enterEdge_stmt(DotParser.Edge_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#edge_stmt}.
	 * @param ctx the parse tree
	 */
	void exitEdge_stmt(DotParser.Edge_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#edgeRHS}.
	 * @param ctx the parse tree
	 */
	void enterEdgeRHS(DotParser.EdgeRHSContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#edgeRHS}.
	 * @param ctx the parse tree
	 */
	void exitEdgeRHS(DotParser.EdgeRHSContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#edgeop}.
	 * @param ctx the parse tree
	 */
	void enterEdgeop(DotParser.EdgeopContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#edgeop}.
	 * @param ctx the parse tree
	 */
	void exitEdgeop(DotParser.EdgeopContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#node_stmt}.
	 * @param ctx the parse tree
	 */
	void enterNode_stmt(DotParser.Node_stmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#node_stmt}.
	 * @param ctx the parse tree
	 */
	void exitNode_stmt(DotParser.Node_stmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#node_id}.
	 * @param ctx the parse tree
	 */
	void enterNode_id(DotParser.Node_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#node_id}.
	 * @param ctx the parse tree
	 */
	void exitNode_id(DotParser.Node_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#port}.
	 * @param ctx the parse tree
	 */
	void enterPort(DotParser.PortContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#port}.
	 * @param ctx the parse tree
	 */
	void exitPort(DotParser.PortContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#subgraph}.
	 * @param ctx the parse tree
	 */
	void enterSubgraph(DotParser.SubgraphContext ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#subgraph}.
	 * @param ctx the parse tree
	 */
	void exitSubgraph(DotParser.SubgraphContext ctx);
	/**
	 * Enter a parse tree produced by {@link DotParser#id_}.
	 * @param ctx the parse tree
	 */
	void enterId_(DotParser.Id_Context ctx);
	/**
	 * Exit a parse tree produced by {@link DotParser#id_}.
	 * @param ctx the parse tree
	 */
	void exitId_(DotParser.Id_Context ctx);
}