/*
 * Copyright (c) 2009-2010, IETR/INSA of Rennes
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the IETR/INSA of Rennes nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package net.sf.orcc.backends.transformations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import net.sf.orcc.OrccRuntimeException;
import net.sf.orcc.ir.CFGNode;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.LocalVariable;
import net.sf.orcc.ir.Location;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Variable;
import net.sf.orcc.ir.expr.BinaryExpr;
import net.sf.orcc.ir.expr.BinaryOp;
import net.sf.orcc.ir.expr.BoolExpr;
import net.sf.orcc.ir.expr.ExpressionInterpreter;
import net.sf.orcc.ir.expr.IntExpr;
import net.sf.orcc.ir.expr.ListExpr;
import net.sf.orcc.ir.expr.StringExpr;
import net.sf.orcc.ir.expr.UnaryExpr;
import net.sf.orcc.ir.expr.VarExpr;
import net.sf.orcc.ir.instructions.Assign;
import net.sf.orcc.ir.instructions.Call;
import net.sf.orcc.ir.instructions.Load;
import net.sf.orcc.ir.instructions.Return;
import net.sf.orcc.ir.instructions.Store;
import net.sf.orcc.ir.nodes.AbstractNode;
import net.sf.orcc.ir.nodes.BlockNode;
import net.sf.orcc.ir.nodes.IfNode;
import net.sf.orcc.ir.nodes.WhileNode;
import net.sf.orcc.ir.transforms.AbstractActorTransformation;

/**
 * Split expression and effective node.
 * 
 * @author J�r�me GORIN
 * @author Matthieu Wipliez
 * 
 */
public class ThreeAddressCodeTransformation extends AbstractActorTransformation {

	private class ExpressionSplitter implements ExpressionInterpreter {

		private ListIterator<Instruction> it;

		/**
		 * type of the target variable
		 */
		private Type type;

		/**
		 * Creates a new expression splitter with the given list iterator. The
		 * iterator must be placed immediately before the expression to be
		 * translated is used.
		 * 
		 * @param it
		 *            iterator on a list of instructions
		 */
		public ExpressionSplitter(ListIterator<Instruction> it, Type type) {
			this.it = it;
			this.type = type;
		}

		@Override
		public Object interpret(BinaryExpr expr, Object... args) {
			// Get expr information
			Type previousType = type;
			BinaryOp op = expr.getOp();

			Location location = expr.getLocation();
			Expression e1 = expr.getE1();
			Expression e2 = expr.getE2();

			// Correct binaryExpr type if expr is a comparison
			if (op.isComparison()) {
				type = e1.getType();
			}

			// Transform e1 and e2
			e1 = (Expression) e1.accept(this, args);
			e2 = (Expression) e2.accept(this, args);

			type = previousType;

			// Assign the correct type to the current expression
			if (op.isComparison()) {
				previousType = IrFactory.eINSTANCE.createTypeBool();
			}

			// Make the final assignment
			LocalVariable target = newVariable(type);
			target.setType(type);
			Assign assign = new Assign(location, target, new BinaryExpr(
					location, e1, op, e2, previousType));
			assign.setBlock(block);
			it.add(assign);

			return new VarExpr(location, new Use(target));
		}

		@Override
		public Object interpret(BoolExpr expr, Object... args) {
			return expr;
		}

		@Override
		public Object interpret(IntExpr expr, Object... args) {
			return expr;
		}

		@Override
		public Object interpret(ListExpr expr, Object... args) {
			throw new OrccRuntimeException("list expression not supported");
		}

		@Override
		public Object interpret(StringExpr expr, Object... args) {
			return expr;
		}

		@Override
		public Object interpret(UnaryExpr expr, Object... args) {
			Expression constExpr;
			Expression binary;
			Expression exprE1 = expr.getExpr();
			Location loc = expr.getLocation();
			Type type = expr.getType();

			switch (expr.getOp()) {
			case MINUS:
				constExpr = new IntExpr(0);
				binary = new BinaryExpr(loc, constExpr, BinaryOp.MINUS, exprE1,
						type);
				return binary.accept(this, args);
			case LOGIC_NOT:
				constExpr = new BoolExpr(new Location(), false);
				binary = new BinaryExpr(loc, exprE1, BinaryOp.EQ, constExpr,
						type);
				return binary.accept(this, args);
			case BITNOT:
				binary = new BinaryExpr(loc, exprE1, BinaryOp.BITXOR, exprE1,
						type);
				return binary.accept(this, args);
			default:
				throw new OrccRuntimeException("unsupported operator");
			}
		}

		@Override
		public Object interpret(VarExpr expr, Object... args) {
			if (!expr.getType().equals(type)) {
				if (expr.getType().isList() && type.isList()) {
					// compare type of two arrays
					TypeList exprtype = (TypeList) expr.getType();
					TypeList refType = (TypeList) type;
					if (exprtype.getType().equals(refType.getType())) {
						return expr;
					}
				}

				// Make the final assignment
				LocalVariable target = newVariable(type);
				Use use = new Use(target);
				Location location = expr.getLocation();
				target.setType(type);
				if (expr.getType().isBool()) {

					Assign assign = new Assign(location, target,
							new BinaryExpr(location, expr, BinaryOp.LOGIC_OR,
									new BoolExpr(false), expr.getType()));
					assign.setBlock(block);
					it.add(assign);
				} else {
					Assign assign = new Assign(location, target,
							new BinaryExpr(location, expr, BinaryOp.PLUS,
									new IntExpr(0), expr.getType()));
					assign.setBlock(block);
					it.add(assign);
				}
				return new VarExpr(use);
			}
			return expr;
		}

		/**
		 * Creates a new local variable with type
		 * 
		 * @return a new local variable with type
		 */
		private LocalVariable newVariable(Type type) {
			String procName = procedure.getName();
			LocalVariable local = new LocalVariable(true, tempVarCount++,
					new Location(), procName + "_" + "expr", type);
			procedure.getLocals().put(local.getName(), local);
			return local;
		}

	}

	private BlockNode block;

	private int tempVarCount;

	/**
	 * Returns an iterator over the last instruction of the previous block. A
	 * new block is created if there is no previous one.
	 * 
	 * @param it
	 * @return
	 */
	private ListIterator<Instruction> getItr(ListIterator<CFGNode> it) {
		it.previous();
		if (it.hasPrevious()) {
			// get previous and restore iterator's position
			CFGNode previous = it.previous();
			it.next();

			if (previous instanceof BlockNode) {
				block = ((BlockNode) previous);
			} else if (previous instanceof IfNode) {
				block = ((IfNode) previous).getJoinNode();
			} else {
				block = ((WhileNode) previous).getJoinNode();
			}
		} else {
			// no previous block, create and add a new one
			block = new BlockNode(procedure);
			it.add(block);
		}
		it.next();

		return block.lastListIterator();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visit(Assign assign, Object... args) {
		Type type;
		block = assign.getBlock();
		ListIterator<Instruction> it = (ListIterator<Instruction>) args[0];
		if (assign.getTarget().getType().isBool()) {
			type = IrFactory.eINSTANCE.createTypeBool();
		} else {
			type = IrFactory.eINSTANCE.createTypeInt(32);
		}
		it.previous();
		assign.setValue(visitExpression(assign.getValue(), it, type));
		it.next();

		// 3AC does not support direct assignment
		if (!(assign.getValue() instanceof BinaryExpr)) {
			Expression expr = assign.getValue();
			Location location = expr.getLocation();
			if (expr.getType().isBool()) {
				BinaryOp op = BinaryOp.LOGIC_OR;
				assign.setValue(new BinaryExpr(location, expr, op,
						new BoolExpr(false), expr.getType()));
			} else {
				BinaryOp op = BinaryOp.PLUS;
				assign.setValue(new BinaryExpr(location, expr, op, new IntExpr(
						0), expr.getType()));
			}
		}
	}

	@Override
	public void visit(Call call, Object... args) {
		block = call.getBlock();
		List<Variable> params = call.getProcedure().getParameters().getList();
		List<Type> types = new ArrayList<Type>(params.size());
		for (Variable variable : params) {
			types.add(variable.getType());
		}
		visitExpressions(call.getParameters(), args[0], types);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visit(IfNode ifNode, Object... args) {
		ListIterator<CFGNode> it = (ListIterator<CFGNode>) args[0];
		ifNode.setValue(visitExpression(ifNode.getValue(), getItr(it),
				IrFactory.eINSTANCE.createTypeBool()));
		super.visit(ifNode, args);
	}

	@Override
	public void visit(Load load, Object... args) {
		block = load.getBlock();
		List<Type> types = new ArrayList<Type>(load.getIndexes().size());
		for (int i = 0; i < load.getIndexes().size(); i++) {
			types.add(IrFactory.eINSTANCE.createTypeInt(32));
		}
		visitExpressions(load.getIndexes(), args[0], types);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void visit(Return returnInstr, Object... args) {
		block = returnInstr.getBlock();
		if (returnInstr.getValue() != null) {
			ListIterator<Instruction> it = (ListIterator<Instruction>) args[0];
			it.previous();
			returnInstr.setValue(visitExpression(returnInstr.getValue(), it,
					procedure.getReturnType()));
			it.next();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(Store store, Object... args) {
		block = store.getBlock();
		ListIterator<Instruction> it = (ListIterator<Instruction>) args[0];
		Expression value = store.getValue();
		Variable target = store.getTarget();
		Type targetType = target.getType();

		// Check indexes
		List<Type> types = new ArrayList<Type>(store.getIndexes().size());
		for (int i = 0; i < store.getIndexes().size(); i++) {
			types.add(IrFactory.eINSTANCE.createTypeInt(32));
		}
		visitExpressions(store.getIndexes(), it, types);
		it.previous();

		// Check store value
		if (targetType.isBool()) {
			store.setValue(visitExpression(value, it,
					IrFactory.eINSTANCE.createTypeBool()));
		} else if (targetType.isList()) {
			TypeList exprtype = (TypeList) targetType;
			if (exprtype.getElementType().isBool()) {
				store.setValue(visitExpression(value, it,
						IrFactory.eINSTANCE.createTypeBool()));
			} else {
				store.setValue(visitExpression(value, it,
						IrFactory.eINSTANCE.createTypeInt(32)));

			}
		} else {
			store.setValue(visitExpression(value, it,
					IrFactory.eINSTANCE.createTypeInt(32)));
		}

		it.next();
	}

	@Override
	public void visit(WhileNode whileNode, Object... args) {
		ListIterator<Instruction> it = whileNode.getJoinNode().listIterator();

		// Go to the end of joinNode
		while (it.hasNext()) {
			it.next();
		}
		whileNode.setValue(visitExpression(whileNode.getValue(), it,
				IrFactory.eINSTANCE.createTypeBool()));

		super.visit(whileNode, args);
	}

	private Expression visitExpression(Expression value,
			ListIterator<Instruction> it, Type type) {
		return (Expression) value.accept(new ExpressionSplitter(it, type));
	}

	@SuppressWarnings("unchecked")
	private void visitExpressions(List<Expression> expressions, Object arg,
			List<Type> types) {
		ListIterator<Instruction> it = (ListIterator<Instruction>) arg;
		it.previous();
		ListIterator<Expression> pit = expressions.listIterator();
		Iterator<Type> itt = types.iterator();
		while (pit.hasNext()) {
			Expression value = pit.next();
			if (itt.hasNext()) {
				Expression expr = (Expression) value
						.accept(new ExpressionSplitter(it, itt.next()));
				pit.set(expr);
			}
		}
		it.next();
	}

	@Override
	public void visitProcedure(Procedure procedure) {
		tempVarCount = 1;

		// set the label counter to prevent new nodes from having the same label
		// as existing nodes
		List<CFGNode> nodes = procedure.getNodes();
		if (nodes.size() > 0) {
			CFGNode lastNode = nodes.get(nodes.size() - 1);
			AbstractNode.setLabelCount(lastNode.getLabel() + 1);
		}
		super.visitProcedure(procedure);
	}

}
