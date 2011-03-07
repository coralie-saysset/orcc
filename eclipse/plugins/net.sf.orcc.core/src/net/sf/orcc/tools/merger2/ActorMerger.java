/*
 * Copyright (c) 2009, IETR/INSA of Rennes
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
package net.sf.orcc.tools.merger2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.orcc.ir.AbstractActorVisitor;
import net.sf.orcc.ir.Action;
import net.sf.orcc.ir.ActionScheduler;
import net.sf.orcc.ir.Actor;
import net.sf.orcc.ir.GlobalVariable;
import net.sf.orcc.ir.IrFactory;
import net.sf.orcc.ir.Location;
import net.sf.orcc.ir.Port;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Variable;
import net.sf.orcc.ir.serialize.IRCloner;
import net.sf.orcc.ir.transformations.RenameTransformation;
import net.sf.orcc.moc.CSDFMoC;
import net.sf.orcc.moc.MoC;
import net.sf.orcc.util.MultiMap;
import net.sf.orcc.util.OrderedMap;

/**
 * This class defines a transformation that merge multiple actors into a single
 * actor.
 * 
 * @author Jerome Gorin
 * 
 */
public class ActorMerger {

	/**
	 * This class defines a transformation that remove all possible conflicts
	 * between the composite actor and the candidate to merge with. Conflicts
	 * are resolved by calling the RenameTransformation on names detected as
	 * conflicted.
	 * 
	 * @author Jerome Gorin
	 * 
	 */
	public class ConflictSolver extends AbstractActorVisitor {
		private OrderedMap<String, Procedure> refProcs;
		private Map<String, String> replacementMap;

		public ConflictSolver() {
			this.refProcs = new OrderedMap<String, Procedure>();
			this.replacementMap = new HashMap<String, String>();

			// Store all procedure names of the composite actor
			refProcs.putAll(procs);
			getProcedures(actions);
			getProcedures(initializes);
		}

		private void compareVariables(
				OrderedMap<String, GlobalVariable> refVariables,
				OrderedMap<String, GlobalVariable> variables) {
			for (Variable variable : variables) {
				String name = variable.getName();

				// Check if Global variable name is already used in the
				// reference actor
				if (refVariables.contains(name)) {
					// Name is used set a unique index to the name
					int index = 0;

					while (refVariables.contains(name + "_" + index)) {
						index++;
					}

					// Store result in replacement map
					replacementMap.put(name, name + "_" + index);
				}
			}
		}

		private void getProcedures(List<Action> actions) {
			for (Action action : actions) {
				// Get all procedures contain in an action
				Procedure scheduler = action.getScheduler();
				Procedure body = action.getBody();
				refProcs.put(scheduler.getName(), scheduler);
				refProcs.put(body.getName(), body);
			}
		}

		public void resolve(Actor actor) {
			// Check conflicts on global variables
			compareVariables(parameters, actor.getParameters());
			compareVariables(stateVars, actor.getStateVars());

			// Check all procedures
			visit(actor);

			if (!replacementMap.isEmpty()) {
				// Rename all conflicts founds
				new RenameTransformation(replacementMap).visit(actor);
			}
		}

		@Override
		public void visit(Procedure procedure) {
			String name = procedure.getName();

			// Check if procedure name is already used in the reference actor
			if (refProcs.contains(name)) {
				// Name is used set a unique index to the name
				int index = 0;

				while (refProcs.contains(name + "_" + index)) {
					index++;
				}

				// Store result in replacement map
				replacementMap.put(name, name + "_" + index);
			}
		}

	}

	private List<Action> actions;
	private Actor candidate;
	private MultiMap<Actor, Port> extInputs;
	private MultiMap<Actor, Port> extOutputs;
	private List<Action> initializes;
	private OrderedMap<String, Port> inputs;
	private MultiMap<Port, Port> IntPorts;
	private Map<Port, GlobalVariable> intVars;
	private MoC moc;
	private String name;
	private OrderedMap<String, Port> outputs;
	private OrderedMap<String, GlobalVariable> parameters;
	private OrderedMap<String, Procedure> procs;
	private int rate;
	private ActionScheduler scheduler;
	private OrderedMap<String, GlobalVariable> stateVars;
	private OrderedMap<String, Port> toKeep;

	/**
	 * Creates a new actor merger with the given name, inputs and outputs,
	 * internal ports are converted into state variable.
	 * 
	 * @param name
	 *            the composite actor name
	 * 
	 * @param inputs
	 *            the inputs of the composite
	 * 
	 * @param outputs
	 *            the outputs of the composite
	 * 
	 * @param internalPorts
	 *            the ports to convert into state variables
	 */
	public ActorMerger(String name, MultiMap<Actor, Port> inputs,
			MultiMap<Actor, Port> outputs, MultiMap<Port, Port> internalPorts) {
		this.extInputs = inputs;
		this.extOutputs = outputs;
		this.IntPorts = internalPorts;
		this.name = name;
		this.intVars = new HashMap<Port, GlobalVariable>();
		this.parameters = new OrderedMap<String, GlobalVariable>();
		this.stateVars = new OrderedMap<String, GlobalVariable>();
		this.inputs = new OrderedMap<String, Port>();
		this.outputs = new OrderedMap<String, Port>();
		this.procs = new OrderedMap<String, Procedure>();
		this.initializes = new ArrayList<Action>();
		this.actions = new ArrayList<Action>();
		this.scheduler = new ActionScheduler(actions, null);
		this.moc = new CSDFMoC();
	}

	/**
	 * Add a new candidate to the composite actor with the given rate.
	 * 
	 * @param candidate
	 *            the candidate actor
	 * 
	 * @param rate
	 *            the rate of the candidate
	 */
	public void add(Actor candidate, int rate) {
		this.rate = rate;
		this.candidate = candidate;

		// Clone the actor so as to isolate transformations on other
		// instance of this actor
		Actor clone = new IRCloner(candidate).clone();

		// Resolve potential conflict in names
		new ConflictSolver().resolve(clone);

		// Mark ports to keep from candidate and internal states to add in
		// composite
		toKeep = new OrderedMap<String, Port>();
		transformInputs(clone);
		transformOutputs(clone);

		// Add all properties
		parameters.putAll(clone.getParameters());
		stateVars.putAll(clone.getStateVars());
		procs.putAll(clone.getProcs());
		actions.addAll(clone.getActions());
		initializes.addAll(clone.getInitializes());

		// Merge MoCs
		MoC cloneMoC = clone.getMoC();
		moc = (MoC) cloneMoC.accept(new MoCMerger(moc, rate, toKeep));

		// Create corresponding action scheduler
		scheduler = (ActionScheduler) moc.accept(new SchedulerMerger());
	}

	/**
	 * Return the corresponding composite actor.
	 * 
	 * @return the corresponding composite actor
	 */
	public Actor getComposite() {
		return new Actor(name, "", parameters, inputs, outputs, false,
				stateVars, procs, actions, initializes, scheduler, moc);
	}

	private GlobalVariable getInternalStateVar(Port port) {
		// Check if a state variable has been assigned to the connection
		for (Port target : IntPorts.get(port)) {
			if (intVars.containsKey(target)) {
				// Target has already been define to a state variable
				return intVars.get(target);
			}
		}

		// No state variable represents this internal connection
		return null;
	}

	private GlobalVariable internalizePort(Port port, GlobalVariable portVar,
			Actor actor) {
		// Port has never been internalize, create a new one
		if (portVar == null) {
			// Get MoC of the current candidate to define the size of the state
			// variable required
			// TODO: extend to QSDF
			CSDFMoC moc = (CSDFMoC) actor.getMoC();
			int size = rate * moc.getNumTokensProduced(port);

			// Set new port var of size tokenSize
			TypeList typeList = IrFactory.eINSTANCE.createTypeList(size,
					port.getType());
			portVar = new GlobalVariable(new Location(), typeList,
					port.getName(), true);

			stateVars.put(portVar.getName(), portVar);
		} else {
			// Increase size of the stateVar by rate
			TypeList typeList = (TypeList) portVar.getType();

			int size = typeList.getSize() * rate;
			typeList.setSize(size);

			// Update state variable name with current port
			String name = portVar.getName() + "_" + port.getName();
			portVar.setName(name);
		}

		// Change fifo access to stateVar access
		new InternalizeFifoAccess(port, portVar).visit(actor);

		return portVar;
	}

	private void transformInputs(Actor actor) {
		// Get list of ports of the current candidate set as external
		Collection<Port> candidateIns = extInputs.get(candidate);

		for (Port port : actor.getInputs()) {
			// Get equivalent port in candidate
			Port candidatePort = candidate.getPort(port.getName());

			if (candidateIns.contains(candidatePort)) {
				// Port is external
				inputs.put(port.getName(), port);
				toKeep.put(port.getName(), port);
			} else {
				// Port is internal, merge to state variable
				GlobalVariable portVar = getInternalStateVar(candidatePort);
				GlobalVariable internalVar = internalizePort(port, portVar,
						actor);
				intVars.put(candidatePort, internalVar);
			}

		}
	}

	private void transformOutputs(Actor actor) {
		// Get list of ports of the current candidate set as external
		Collection<Port> candidateOuts = extOutputs.get(candidate);

		for (Port port : actor.getOutputs()) {
			// Get equivalent port in candidate
			Port candidatePort = candidate.getPort(port.getName());

			if (candidateOuts.contains(candidatePort)) {
				// Port is external
				outputs.put(port.getName(), port);
				toKeep.put(port.getName(), port);
			} else {
				// Port is internal, merge to state variable
				GlobalVariable portVar = getInternalStateVar(candidatePort);
				GlobalVariable internalVar = internalizePort(port, portVar,
						actor);
				intVars.put(candidatePort, internalVar);
			}

		}
	}

}