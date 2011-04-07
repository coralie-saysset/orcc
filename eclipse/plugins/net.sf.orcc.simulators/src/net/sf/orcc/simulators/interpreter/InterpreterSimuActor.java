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
package net.sf.orcc.simulators.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.orcc.OrccRuntimeException;
import net.sf.orcc.debug.model.OrccProcess;
import net.sf.orcc.interpreter.ActorInterpreter;
import net.sf.orcc.interpreter.ListAllocator;
import net.sf.orcc.ir.Action;
import net.sf.orcc.ir.Actor;
import net.sf.orcc.ir.ExprBool;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.Node;
import net.sf.orcc.ir.NodeBlock;
import net.sf.orcc.ir.NodeIf;
import net.sf.orcc.ir.NodeWhile;
import net.sf.orcc.ir.Port;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.ExpressionEvaluator;
import net.sf.orcc.plugins.simulators.Simulator.DebugStackFrame;
import net.sf.orcc.runtime.Fifo;
import net.sf.orcc.simulators.SimuActor;

public class InterpreterSimuActor extends AbstractInterpreterSimuActor
		implements SimuActor {

	/**
	 * Debugger utils
	 */
	protected class Breakpoint {
		public Action action;

		public int lineNb;

		public Breakpoint(Action action, Integer lineNb) {
			this.action = action;
			this.lineNb = lineNb;
		}
	}

	private class NodeInfo {
		public Expression condition;
		public NodeBlock joinNode;
		public int nbSubNodes;
		public int subNodeIdx;
		public List<Node> subNodes;

		public NodeInfo(int subNodeIdx, int nbSubNodes, List<Node> subNodes,
				NodeBlock joinNode, Expression condition) {
			this.subNodeIdx = subNodeIdx;
			this.nbSubNodes = nbSubNodes;
			this.subNodes = subNodes;
			this.joinNode = joinNode;
			this.condition = condition;
		}
	}

	/**
	 * Interpretation and evaluation tools
	 */
	protected ActorInterpreter actorInterpreter;

	protected Actor actorIR;

	protected Action breakAction = null;

	protected List<Breakpoint> breakpoints;

	private Action currentAction = null;

	protected ExpressionEvaluator exprEvaluator;

	protected Map<String, Fifo> fifos;

	protected String instanceId;

	private List<Instruction> instrStack;

	protected boolean isStepping = false;

	private ListAllocator listAllocator;

	private List<NodeInfo> nodeStack;

	private int nodeStackLevel;

	protected OrccProcess process;

	/**
	 * Constructor
	 * 
	 * @param instanceId
	 *            : name of the instance of the Actor model
	 * @param actorParameters
	 *            : map of actor's parameters values to be set at init
	 * @param actorIR
	 *            : duplicated intermediate representation of the Actor model to
	 *            be directly used/modified for the interpretation
	 * @param tracesFolder
	 *            : the defined folder for tracing simulation information into
	 *            output files
	 * @param process
	 *            : the master OrccProcess (can be used for write to output
	 *            console
	 */
	public InterpreterSimuActor(String instanceId,
			Map<String, Expression> actorParameters, Actor actorIR,
			OrccProcess process) {
		this.process = process;

		this.instanceId = instanceId;
		this.actorIR = actorIR;

		this.fifos = new HashMap<String, Fifo>();

		// Create lists for nodes and instructions in case of debug mode
		this.nodeStack = new ArrayList<NodeInfo>();
		this.instrStack = new ArrayList<Instruction>();
		this.breakpoints = new ArrayList<Breakpoint>();
		this.nbOfFirings = 0;

		this.listAllocator = new ListAllocator();

		// Create an Actor interpreter that is able to interpret simulated
		// actor's instance according to a visitor design pattern
		this.actorInterpreter = new ActorInterpreter(actorParameters, actorIR,
				process);
		// Create the expression evaluator
		this.exprEvaluator = new ExpressionEvaluator();
	}

	@Override
	public void clearBreakpoint(int breakpoint) {
		Breakpoint rm_bkpt = null;
		// Remove breakpoint from the list
		for (Breakpoint bkpt : breakpoints) {
			if ((breakpoint == bkpt.lineNb)) {
				rm_bkpt = bkpt;
				break;
			}
		}
		if (rm_bkpt != null)
			breakpoints.remove(rm_bkpt);
	}

	@Override
	public void close() {
	}

	@Override
	public void connect() {
		actorInterpreter.setFifos(fifos);
	}

	@Override
	public String getActorName() {
		return actorIR.getName();
	}

	@Override
	public String getFileName() {
		return actorIR.getFile();
	}

	@Override
	public String getInstanceId() {
		return instanceId;
	}

	@Override
	public DebugStackFrame getStackFrame() {
		DebugStackFrame stackFrame = new DebugStackFrame();
		stackFrame.actorFilename = actorIR.getFile();
		stackFrame.codeLine = lastVisitedLocation.getStartLine();
		stackFrame.nbOfFirings = nbOfFirings;
		stackFrame.stateVars.clear();
		for (Var stateVar : actorIR.getStateVars()) {
			stackFrame.stateVars.put(stateVar.getName(), stateVar.getValue());
		}
		stackFrame.currentAction = lastVisitedAction;
		stackFrame.fsmState = actorInterpreter.getFsmState();
		return stackFrame;
	}

	@Override
	public int goToBreakpoint() {
		int bkptLine = 0;
		// Then interpret the actor until breakpoint is reached
		while (bkptLine == 0) {
			int actorStatus = step(false);
			if (actorStatus <= 1) {
				process.write("! Breakpoint location forbidden !");
			} else {
				for (Breakpoint bkpt : breakpoints) {
					if ((bkpt.lineNb == lastVisitedLocation.getStartLine())) {
						bkptLine = bkpt.lineNb;
					}
				}
			}
		}
		return bkptLine;
	}

	@Override
	public void initialize() {
		actorInterpreter.initialize();
	}

	@Override
	public boolean isStepping() {
		return isStepping;
	}

	/**
	 * Manages the stack of nodes to be interpreted by the debugger
	 */
	private boolean popNodeStack() {
		boolean exeStmt = false;
		NodeInfo node = nodeStack.get(nodeStackLevel - 1);

		if (node.nbSubNodes > 0) {
			if (node.subNodeIdx == node.nbSubNodes) {
				if ((node.condition != null)
						&& ((ExprBool) node.condition.accept(exprEvaluator))
								.isValue()) {
					node.subNodeIdx = 0;
					exeStmt = true;
				} else {
					nodeStack.remove(nodeStackLevel - 1);
					nodeStackLevel--;
					if (node.joinNode != null) {
						Iterator<Instruction> it = node.joinNode.iterator();
						while (it.hasNext()) {
							instrStack.add(it.next());
						}
						nodeStack.add(new NodeInfo(0, 0, null, null, null));
						nodeStackLevel++;
					}
				}
			} else {
				Node subNode = node.subNodes.get(node.subNodeIdx++);
				if (subNode instanceof NodeIf) {
					Object condition = ((NodeIf) subNode).getCondition()
							.accept(exprEvaluator);
					if (((ExprBool) condition).isValue()) {
						nodeStack.add(new NodeInfo(0, ((NodeIf) subNode)
								.getThenNodes().size(), ((NodeIf) subNode)
								.getThenNodes(), ((NodeIf) subNode)
								.getJoinNode(), null));
					} else {
						nodeStack.add(new NodeInfo(0, ((NodeIf) subNode)
								.getElseNodes().size(), ((NodeIf) subNode)
								.getElseNodes(), ((NodeIf) subNode)
								.getJoinNode(), null));
					}
					nodeStackLevel++;
					exeStmt = true;
				} else if (subNode instanceof NodeWhile) {
					Expression condition = ((NodeWhile) subNode).getCondition();
					if (((ExprBool) condition.accept(exprEvaluator)).isValue()) {
						nodeStack.add(new NodeInfo(0, ((NodeWhile) subNode)
								.getNodes().size(), ((NodeWhile) subNode)
								.getNodes(), null, condition));
						nodeStackLevel++;
					}
					exeStmt = true;
				} else /* NodeBlock => add instructions to stack */{
					Iterator<Instruction> it = ((NodeBlock) subNode).iterator();
					while (it.hasNext()) {
						instrStack.add(it.next());
					}
					nodeStack.add(new NodeInfo(0, 0, null, null, null));
					nodeStackLevel++;
				}
			}
		} else {
			// Instructions
			if (instrStack.size() > 0) {
				Instruction instr = instrStack.remove(0);

				System.out.println("TODO");

				if ((instr.getLocation().getStartLine() != lastVisitedLocation
						.getStartLine())
						&& (instr.getLocation().getStartLine() != 0)) {
					lastVisitedLocation = instr.getLocation();
					exeStmt = true;
				}
			}
			if (instrStack.size() == 0) {
				nodeStack.remove(nodeStackLevel - 1);
				nodeStackLevel--;
			}
		}
		return exeStmt;
	}

	@Override
	public int runAllSchedulableAction() {
		try {
			// Skip execution if currently stepping
			if (isStepping) {
				return 0;
			}
			// "Round-robbin-like" scheduling policy : schedule only all
			// schedulable
			// action of an
			// actor before returning
			int nbOfFiredActions = 0;
			Action action;
			while ((action = actorInterpreter.getNextAction()) != null) {
				for (Breakpoint bkpt : breakpoints) {
					if (action == bkpt.action) {
						breakAction = action;
						return -2;
					}
				}

				actorInterpreter.execute(action);

				nbOfFiredActions++;
				nbOfFirings += nbOfFiredActions;
				return nbOfFiredActions;
			}
			return 0;
		} catch (OrccRuntimeException ex) {
			throw new OrccRuntimeException("Runtime exception thrown by actor "
					+ actorIR.getName(), ex);
		}
	}

	@Override
	public int runNextSchedulableAction() {
		try {
			// Skip execution if currently stepping
			if (isStepping) {
				return 0;
			}
			// "Synchronous-like" scheduling policy : schedule only 1 action per
			// actor at each "schedule" (network logical cycle) call
			Action action = actorInterpreter.getNextAction();
			if (action != null) {
				for (Breakpoint bkpt : breakpoints) {
					if (action == bkpt.action) {
						breakAction = action;
						return -2;
					}
				}
				actorInterpreter.execute(action);
				return 1;
			} else {
				return 0;
			}
		} catch (OrccRuntimeException ex) {
			throw new OrccRuntimeException("Runtime exception thrown by actor "
					+ actorIR.getName() + " :\n" + ex.getMessage());
		}
	}

	@Override
	public void setBreakpoint(int breakpoint) {
		Breakpoint bkpt = new Breakpoint(actorIR.getActions().get(0),
				breakpoint);
		for (Action action : actorIR.getActions()) {
			if (action.getLocation().getStartLine() <= breakpoint) {
				if (action.getLocation().getStartLine() > bkpt.action
						.getLocation().getStartLine()) {
					bkpt.action = action;
				}
			}
		}
		// Add breakpoint to the list
		breakpoints.add(bkpt);
	}

	@Override
	public void setFifo(Port port, Fifo fifo) {
		fifos.put(port.getName(), fifo);
	}

	@Override
	public int step(boolean stepInto) {
		try {
			if (currentAction == null) {
				if ((isStepping == false) && (breakAction != null)) {
					currentAction = breakAction;
					isStepping = true;
				} else {
					currentAction = actorInterpreter.getNextAction();
				}
				if (currentAction != null) {
					nbOfFirings++;
					lastVisitedAction = currentAction.getName();
					// Allocate local List variables
					for (Var local : currentAction.getBody().getLocals()) {
						Type type = local.getType();
						if (type.isList()) {
							local.setValue((Expression) type
									.accept(listAllocator));
						}
					}
					// Initialize stack frame
					nodeStack.add(new NodeInfo(0, currentAction.getBody()
							.getNodes().size(), currentAction.getBody()
							.getNodes(), null, null));
					nodeStackLevel = 1;
				}
			}
			if (currentAction != null) {
				while ((nodeStackLevel > 0) && (!popNodeStack()))
					;
				if (nodeStackLevel > 0) {
					return 2;
				} else {
					currentAction = null;
					isStepping = false;
					return 1;
				}
			} else {
				isStepping = false;
				return 0;
			}
		} catch (OrccRuntimeException ex) {
			throw new OrccRuntimeException("Runtime exception thrown by actor "
					+ actorIR.getName() + " :\n" + ex.getMessage());
		}
	}

}
