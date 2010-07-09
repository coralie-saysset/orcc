/*
 * Copyright (c) 2010, IETR/INSA of Rennes
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
package net.sf.orcc.tools.merger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.orcc.OrccException;
import net.sf.orcc.ir.Action;
import net.sf.orcc.ir.ActionScheduler;
import net.sf.orcc.ir.Actor;
import net.sf.orcc.ir.CFGNode;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.LocalVariable;
import net.sf.orcc.ir.Location;
import net.sf.orcc.ir.Pattern;
import net.sf.orcc.ir.Port;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.StateVariable;
import net.sf.orcc.ir.Tag;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Variable;
import net.sf.orcc.ir.expr.BinaryExpr;
import net.sf.orcc.ir.expr.BinaryOp;
import net.sf.orcc.ir.expr.BoolExpr;
import net.sf.orcc.ir.expr.IntExpr;
import net.sf.orcc.ir.expr.VarExpr;
import net.sf.orcc.ir.instructions.Assign;
import net.sf.orcc.ir.instructions.Call;
import net.sf.orcc.ir.instructions.HasTokens;
import net.sf.orcc.ir.instructions.Read;
import net.sf.orcc.ir.instructions.Return;
import net.sf.orcc.ir.instructions.Write;
import net.sf.orcc.ir.nodes.BlockNode;
import net.sf.orcc.ir.nodes.WhileNode;
import net.sf.orcc.network.Connection;
import net.sf.orcc.network.Instance;
import net.sf.orcc.network.Network;
import net.sf.orcc.network.Vertex;
import net.sf.orcc.network.transforms.INetworkTransformation;
import net.sf.orcc.tools.transforms.RemoveReadWrites;
import net.sf.orcc.util.ActionList;
import net.sf.orcc.util.OrderedMap;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DirectedSubgraph;

/**
 * This class defines a network transformation that merges actors.
 * 
 * @author Matthieu Wipliez
 * @author Ghislain Roquier
 * 
 */
public class ActorMerger implements INetworkTransformation {

	private static final String ACTION_NAME = "schedule_loop";

	private static final String SCHEDULER_NAME = "isSchedulable_" + ACTION_NAME;

	private int clusterIdx = 0;

	private DirectedGraph<Vertex, Connection> graph;

	private IScheduler scheduler;

	private OrderedMap<String, Variable> loopVariables;

	private OrderedMap<String, Variable> variables;

	private List<LocalVariable> indexes;

	private int index = 0;

	private Map<Connection, Port> outputsMap;

	private Map<Connection, Port> inputsMap;

	private Map<Connection, Variable> buffersMap;

	private Actor actor;

	private void addLocalVars() {
		for (Variable var : buffersMap.values()) {
			if (loopVariables.get(var.getName()) == null) {
				loopVariables.put(var.getName(), var);
			}
		}
	}

	private List<Expression> addParameters(LocalVariable loopVar,
			Action action, Vertex vertex) {
		List<Expression> params = new ArrayList<Expression>();

		for (Map.Entry<Port, Integer> entry : action.getInputPattern()
				.entrySet()) {
			Variable var = null;
			for (Connection connection : graph.incomingEdgesOf(vertex)) {
				Port tgt = connection.getTarget();
				if (tgt.equals(entry.getKey())) {
					var = buffersMap.get(connection);
				}
			}

			Expression expr = new BinaryExpr(new IntExpr(entry.getValue()),
					BinaryOp.TIMES, new VarExpr(new Use(loopVar)),
					IrFactory.eINSTANCE.createTypeInt(32));

			Expression param = new BinaryExpr(new VarExpr(new Use(var)),
					BinaryOp.PLUS, expr, IrFactory.eINSTANCE.createTypeInt(32));

			params.add(param);
		}

		for (Map.Entry<Port, Integer> entry : action.getOutputPattern()
				.entrySet()) {
			Variable var = null;
			for (Connection connection : graph.outgoingEdgesOf(vertex)) {
				Port src = connection.getSource();
				if (src.equals(entry.getKey())) {
					var = buffersMap.get(connection);
				}
			}

			Expression expr = new BinaryExpr(new IntExpr(entry.getValue()),
					BinaryOp.TIMES, new VarExpr(new Use(loopVar)),
					IrFactory.eINSTANCE.createTypeInt(32));

			Expression param = new BinaryExpr(new VarExpr(new Use(var)),
					BinaryOp.PLUS, expr, IrFactory.eINSTANCE.createTypeInt(32));

			params.add(param);
		}

		return params;
	}

	/**
	 * Converts a given action into a procedure
	 * 
	 * @param action
	 */
	private Procedure convertAction(Action action) {
		OrderedMap<String, Variable> parameters = new OrderedMap<String, Variable>();
		OrderedMap<String, Variable> locals = action.getBody().getLocals();
		List<CFGNode> nodes = action.getBody().getNodes();

		Pattern inputPattern = action.getInputPattern();

		for (Map.Entry<Port, Integer> entry : inputPattern.entrySet()) {
			Port port = entry.getKey();
			if (!parameters.contains(port.getName())) {
				Type type = IrFactory.eINSTANCE.createTypeList(
						entry.getValue(), port.getType());
				LocalVariable param = new LocalVariable(false, 0,
						new Location(), port.getName(), type);
				parameters.put(param.getName(), param);
			}
		}

		Pattern outputPattern = action.getOutputPattern();

		for (Map.Entry<Port, Integer> entry : outputPattern.entrySet()) {
			Port port = entry.getKey();
			Type type = IrFactory.eINSTANCE.createTypeList(entry.getValue(),
					port.getType());
			LocalVariable param = new LocalVariable(false, 0, new Location(),
					port.getName(), type);
			parameters.put(param.getName(), param);
		}

		return new Procedure(action.getName(), false, new Location(),
				IrFactory.eINSTANCE.createTypeVoid(), parameters, locals, nodes);
	}

	/**
	 * Creates the static action for this actor.
	 * 
	 * @return a static action
	 * @throws OrccException
	 */
	private Action createAction() throws OrccException {
		Pattern inputPattern = new Pattern();
		Pattern outputPattern = new Pattern();

		for (Port port : inputsMap.values()) {
			inputPattern.put(port, port.getNumTokensConsumed());
		}

		for (Port port : outputsMap.values()) {
			outputPattern.put(port, port.getNumTokensProduced());
		}

		Procedure scheduler = createScheduler();

		Procedure body = createBody();

		return new Action(new Location(), new Tag(), inputPattern,
				outputPattern, scheduler, body);
	}

	private Actor createActor(Set<Vertex> vertices) throws OrccException {
		OrderedMap<String, Port> inputs = new OrderedMap<String, Port>();
		OrderedMap<String, Port> outputs = new OrderedMap<String, Port>();
		OrderedMap<String, StateVariable> stateVars = new OrderedMap<String, StateVariable>();
		OrderedMap<String, Procedure> procs = new OrderedMap<String, Procedure>();

		OrderedMap<String, Variable> parameters = new OrderedMap<String, Variable>();
		ActionList actions = new ActionList();
		ActionList initializes = new ActionList();
		ActionScheduler scheduler = new ActionScheduler(
				actions.getAllActions(), null);

		actor = new Actor("cluster_" + clusterIdx++, "", parameters, inputs,
				outputs, stateVars, procs, actions.getAllActions(),
				initializes.getAllActions(), scheduler);

		getInputs(vertices);
		getOutputs(vertices);

		Action action = createAction();
		actor.getActions().add(action);

		return actor;
	}

	/**
	 * Creates the body of the static action.
	 * 
	 * @return the body of the static action
	 * @throws OrccException
	 */
	private Procedure createBody() throws OrccException {
		List<CFGNode> nodes = new ArrayList<CFGNode>();
		indexes = new ArrayList<LocalVariable>();
		loopVariables = new OrderedMap<String, Variable>();

		Procedure procedure = new Procedure(ACTION_NAME, false, new Location(),
				IrFactory.eINSTANCE.createTypeVoid(),
				new OrderedMap<String, Variable>(), loopVariables, nodes);

		createInternalBuffers();
		addLocalVars();
		createReads(procedure);
		createLoopedSchedule(procedure, scheduler.getSchedule(), nodes);
		createWrites(procedure);
		return procedure;
	}

	/**
	 * Creates a return instruction that uses the results of the hasTokens tests
	 * previously created.
	 * 
	 * @param block
	 *            block to which return is to be added
	 */
	private void createInputCondition(BlockNode block) {
		Expression value;
		Iterator<Variable> it = variables.iterator();
		if (it.hasNext()) {
			LocalVariable previous = (LocalVariable) it.next();
			value = new VarExpr(new Use(previous, block));

			while (it.hasNext()) {
				LocalVariable thisOne = (LocalVariable) it.next();
				value = new BinaryExpr(value, BinaryOp.LOGIC_AND, new VarExpr(
						new Use(thisOne, block)),
						IrFactory.eINSTANCE.createTypeBool());
				previous = thisOne;
			}
		} else {
			value = new BoolExpr(true);
		}

		Return returnInstr = new Return(value);
		block.add(returnInstr);
	}

	/**
	 * Creates hasTokens tests for the input pattern of the static class.
	 * 
	 * @param block
	 *            the block to which hasTokens instructions are added
	 */
	private void createInputTests(BlockNode block) {
		int i = 0;
		for (Port port : inputsMap.values()) {
			Location location = new Location();
			int numTokens = port.getNumTokensConsumed();
			LocalVariable varDef = new LocalVariable(true, i, new Location(),
					"pattern", IrFactory.eINSTANCE.createTypeBool());
			i++;
			variables.put(varDef.getName(), varDef);
			HasTokens hasTokens = new HasTokens(location, port, numTokens,
					varDef);
			varDef.setInstruction(hasTokens);
			block.add(hasTokens);
		}
	}

	/**
	 * turns FIFOs between static actors into buffers
	 * 
	 */
	private void createInternalBuffers() {
		int index = 0;

		Map<Connection, Integer> bufferCapacities = scheduler
				.getBufferCapacities();
		for (Map.Entry<Connection, Integer> entry : bufferCapacities.entrySet()) {
			Connection connection = entry.getKey();
			int size = entry.getValue();
			String name = "buf_" + index;
			Type type = IrFactory.eINSTANCE.createTypeList(size, connection
					.getSource().getType());
			Variable buf = new LocalVariable(true, 0, new Location(), name,
					type);
			buffersMap.put(connection, buf);
			index++;
		}
	}

	/**
	 * Creates the schedule loop of the
	 * 
	 */
	private void createLoopedSchedule(Procedure procedure, Schedule schedule,
			List<CFGNode> nodes) throws OrccException {
		OrderedMap<String, Procedure> procs = actor.getProcs();
		OrderedMap<String, StateVariable> vars = actor.getStateVars();

		for (Iterand iterand : schedule.getIterands()) {
			if (iterand.isVertex()) {
				Vertex vertex = iterand.getVertex();

				Actor actor = vertex.getInstance().getActor();

				for (Procedure proc : actor.getProcs()) {
					if (procedure.getStateVarsUsed().isEmpty()) {
						if (!procs.contains(proc.getName())) {
							String name = actor.getName() + "_"
									+ proc.getName();
							proc.setName(name);
							procs.put(proc.getName(), proc);
						}
					} else {
						// TODO manage procedure with side effects
					}
				}

				String id = vertex.getInstance().getId();

				for (StateVariable var : actor.getStateVars()) {
					String name = id + "_" + var.getName();
					var.setName(name);
					vars.put(name, var);
				}

				List<Action> actions = actor.getActions();
				if (actions.size() == 1) {
					Procedure proc = convertAction(actions.get(0));

					proc.setName(id + "_" + proc.getName());
					BlockNode blkNode = new BlockNode(procedure);
					LocalVariable counter = indexes.get(index - 1);

					List<Expression> parameters = addParameters(counter,
							actions.get(0), vertex);

					Expression binopExpr = new BinaryExpr(new VarExpr(new Use(
							counter)), BinaryOp.PLUS, new IntExpr(1),
							IrFactory.eINSTANCE.createTypeInt(32));
					blkNode.add(new Call(new Location(), null, proc, parameters));
					blkNode.add(new Assign(counter, binopExpr));
					nodes.add(blkNode);
					procs.put(proc.getName(), proc);
				} else {
					throw new OrccException(
							"SDF actor with multiple actions is not yet supported!");
				}

			} else {

				LocalVariable loopVar = new LocalVariable(true, 0,
						new Location(), "idx_" + index,
						IrFactory.eINSTANCE.createTypeInt(32));

				if (indexes.size() <= index) {
					indexes.add(loopVar);
					loopVariables.put(loopVar.getName(), loopVar);
				}

				Schedule sched = iterand.getSchedule();

				BlockNode blkNode = new BlockNode(procedure);
				List<CFGNode> statements = new ArrayList<CFGNode>();

				int interationCount = sched.getIterationCount();

				blkNode.add(new Assign(loopVar, new IntExpr(0)));
				nodes.add(blkNode);

				WhileNode whileNode = new WhileNode(procedure, null,
						statements, new BlockNode(procedure));

				Expression condition = new BinaryExpr(new VarExpr(new Use(
						loopVar)), BinaryOp.LT, new IntExpr(interationCount),
						IrFactory.eINSTANCE.createTypeBool());
				whileNode.setValue(condition);

				nodes.add(whileNode);

				index++;

				createLoopedSchedule(procedure, sched, statements);

				index--;
			}
		}
	}

	/**
	 * Creates the read instructions of the static action
	 */
	private void createReads(Procedure procedure) {
		BlockNode block = BlockNode.getLast(procedure);
		for (Port port : inputsMap.values()) {
			Variable local = loopVariables.get(port.getName());
			int numTokens = port.getNumTokensConsumed();
			Read read = new Read(port, numTokens, local);
			block.add(read);
		}
	}

	/**
	 * Creates the scheduler of the static action.
	 * 
	 * @return the scheduler of the static action
	 * @throws OrccException
	 */

	private Procedure createScheduler() {
		Location location = new Location();
		variables = new OrderedMap<String, Variable>();
		List<CFGNode> nodes = new ArrayList<CFGNode>();
		Procedure procedure = new Procedure(SCHEDULER_NAME, false, location,
				IrFactory.eINSTANCE.createTypeBool(),
				new OrderedMap<String, Variable>(), variables, nodes);
		BlockNode block = new BlockNode(procedure);
		nodes.add(block);
		createInputTests(block);
		createInputCondition(block);
		return procedure;
	}

	/**
	 * Creates the write instructions of the static action
	 */
	private void createWrites(Procedure procedure) {
		BlockNode block = BlockNode.getLast(procedure);
		for (Port port : outputsMap.values()) {
			Variable local = loopVariables.get(port.getName());
			int numTokens = port.getNumTokensProduced();
			Write read = new Write(port, numTokens, local);
			block.add(read);
		}
	}

	private void getInputs(Set<Vertex> vertices) {
		inputsMap = new HashMap<Connection, Port>();
		buffersMap = new HashMap<Connection, Variable>();

		int index = 0;
		for (Connection connection : graph.edgeSet()) {
			Vertex src = graph.getEdgeSource(connection);
			Vertex tgt = graph.getEdgeTarget(connection);

			if (!vertices.contains(src) && vertices.contains(tgt)) {
				Port tgtPort = connection.getTarget();
				Port port = new Port(tgtPort);
				port.setName("input_" + index);
				port.increaseTokenConsumption(tgtPort.getNumTokensConsumed());
				inputsMap.put(connection, port);
				actor.getInputs().put(port.getName(), port);

				int size = port.getNumTokensConsumed();
				Type type = IrFactory.eINSTANCE.createTypeList(size,
						port.getType());
				Variable var = new LocalVariable(true, 0, new Location(),
						port.getName(), type);

				buffersMap.put(connection, var);
				index++;
			}
		}
	}

	private void getOutputs(Set<Vertex> vertices) {
		int index = 0;
		outputsMap = new HashMap<Connection, Port>();

		for (Connection connection : graph.edgeSet()) {
			Vertex src = graph.getEdgeSource(connection);
			Vertex tgt = graph.getEdgeTarget(connection);

			if (vertices.contains(src) && !vertices.contains(tgt)) {
				Port srcPort = connection.getSource();
				Port port = new Port(srcPort);
				port.setName("output_" + index);
				port.increaseTokenProduction(srcPort.getNumTokensProduced());
				outputsMap.put(connection, port);
				actor.getOutputs().put(port.getName(), port);

				int size = port.getNumTokensProduced();
				Type type = IrFactory.eINSTANCE.createTypeList(size,
						port.getType());
				Variable var = new LocalVariable(true, 0, new Location(),
						port.getName(), type);

				buffersMap.put(connection, var);
				index++;
			}
		}
	}

	/**
	 * Tries to merge actors.
	 * 
	 * @return <code>true</code> if actors were merged, <code>false</code>
	 *         otherwise
	 * @throws OrccException
	 */
	private void mergeActors(Set<Vertex> vertices) throws OrccException {
		createActor(vertices);
		Vertex mergeVertex = new Vertex(new Instance(actor.getName(), actor));
		graph.addVertex(mergeVertex);
		updateConnection(mergeVertex, vertices);
		graph.removeAllVertices(vertices);
	}

	@Override
	public void transform(Network network) throws OrccException {
		graph = network.getGraph();

		Set<Set<Vertex>> sets = new StaticSubsetDetector(network)
				.staticRegionSets();
		for (Set<Vertex> vertices : sets) {
			DirectedGraph<Vertex, Connection> subgraph = new DirectedSubgraph<Vertex, Connection>(
					graph, vertices, null);

			scheduler = new FlatSASScheduler(subgraph);

			for (Vertex vertex : vertices) {
				Actor actor = vertex.getInstance().getActor();
				new RemoveReadWrites().transform(actor);
			}
			mergeActors(vertices);
		}
	}

	private void updateConnection(Vertex merge, Set<Vertex> vertices) {
		Set<Connection> connections = new HashSet<Connection>(graph.edgeSet());
		for (Connection connection : connections) {
			Vertex src = graph.getEdgeSource(connection);
			Vertex tgt = graph.getEdgeTarget(connection);
			if (!vertices.contains(src) && vertices.contains(tgt)) {
				Connection newConn = new Connection(connection.getSource(),
						inputsMap.get(connection), null);
				graph.addEdge(graph.getEdgeSource(connection), merge, newConn);
			}
			if (vertices.contains(src) && !vertices.contains(tgt)) {
				Connection newConn = new Connection(outputsMap.get(connection),
						connection.getTarget(), null);
				graph.addEdge(merge, graph.getEdgeTarget(connection), newConn);
			}
		}
	}
}
