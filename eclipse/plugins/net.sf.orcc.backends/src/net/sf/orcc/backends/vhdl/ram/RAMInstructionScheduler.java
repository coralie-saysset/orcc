/*
 * Copyright (c) 2010-2011, IETR/INSA of Rennes
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
package net.sf.orcc.backends.vhdl.ram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.orcc.backends.instructions.InstRamRead;
import net.sf.orcc.backends.instructions.InstRamSetAddress;
import net.sf.orcc.backends.instructions.InstRamWrite;
import net.sf.orcc.backends.instructions.InstSplit;
import net.sf.orcc.backends.instructions.InstructionsFactory;
import net.sf.orcc.backends.vhdl.VHDLTemplateData;
import net.sf.orcc.ir.Action;
import net.sf.orcc.ir.Actor;
import net.sf.orcc.ir.Def;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.InstLoad;
import net.sf.orcc.ir.InstStore;
import net.sf.orcc.ir.Instruction;
import net.sf.orcc.ir.NodeBlock;
import net.sf.orcc.ir.Pattern;
import net.sf.orcc.ir.Predicate;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.TypeList;
import net.sf.orcc.ir.Use;
import net.sf.orcc.ir.Var;
import net.sf.orcc.ir.util.AbstractActorVisitor;
import net.sf.orcc.ir.util.IrUtil;
import net.sf.orcc.util.EcoreHelper;

import org.eclipse.emf.ecore.util.EcoreUtil;

/**
 * This class defines a visitor that transforms loads and stores to RAM
 * operations.
 * 
 * @author Matthieu Wipliez
 * 
 */
public class RAMInstructionScheduler extends AbstractActorVisitor<Object> {

	/**
	 * current list of instructions
	 */
	private List<Instruction> instructions;

	private final int numPortsRam;

	/**
	 * instructions that depend on a pending InstRamRead
	 */
	private List<Instruction> pendingInstructions;

	private Map<RAM, List<InstRamRead>> pendingReads;

	/**
	 * store instructions that write to variables of output pattern. Must be
	 * done as late as possible so we don't accidentally lose prefetched data if
	 * we can't fire an action.
	 */
	private List<InstStore> pendingStores;

	private Map<Var, RAM> ramMap;

	public RAMInstructionScheduler(int numPortsRam) {
		this.numPortsRam = numPortsRam;
	}

	/**
	 * Adds a RamSetAddress instruction before the previous SplitInstruction.
	 * 
	 * @param ram
	 *            RAM
	 * @param var
	 *            variable
	 * @param indexes
	 *            list of expressions
	 */
	private void addEarlySetAddress(RAM ram, Var var, List<Expression> indexes) {
		// insert the RSA before the previous split instruction
		for (int i = indexInst - 1; i > 0; i--) {
			Instruction instruction = instructions.get(i);
			if (instruction instanceof InstSplit) {
				int port = ram.getLastPortUsed() % numPortsRam + 1;
				InstRamSetAddress rsa = InstructionsFactory.eINSTANCE
						.createInstRamSetAddress(port, var, indexes);
				rsa.setPredicate(IrUtil.copy(ram.getPredicate()));
				instructions.add(i, rsa);
				indexInst++;
				break;
			} else {
				// check if the instruction defines a variable that is used by
				// the indexes
				boolean keepGoing = true;
				for (Expression expr : indexes) {
					for (Use use : EcoreHelper.getObjects(expr, Use.class)) {
						for (Def def : EcoreHelper.getObjects(instruction,
								Def.class)) {
							if (def.getVariable() == use.getVariable()) {
								keepGoing = false;
							}
						}
					}
				}

				// if that is the case, we cannot set the address early
				// so reset the port used (so that split instructions will be
				// added appropriately)
				if (!keepGoing) {
					ram.setLastPortUsed(0);
					addSetAddress(ram, var, indexes);
					break;
				}
			}
		}
	}

	/**
	 * Adds a RamRead to the list of pending reads on the given RAM from an
	 * InstLoad.
	 * 
	 * @param ram
	 *            a RAM
	 * @param load
	 *            the InstLoad used to create the InstRamRead
	 */
	private void addPendingRead(RAM ram, InstLoad load) {
		int port = ram.getLastPortUsed() % numPortsRam + 1;
		InstRamRead read = InstructionsFactory.eINSTANCE.createInstRamRead(
				port, load.getSource().getVariable(), load.getTarget()
						.getVariable());
		read.setPredicate(IrUtil.copy(ram.getPredicate()));

		// add pending read to the list
		pendingReads.get(ram).add(read);
		pendingInstructions.add(read);
	}

	/**
	 * Adds a RamSetAddress instruction at the current index.
	 * 
	 * @param ram
	 *            RAM
	 * @param var
	 *            variable
	 * @param indexes
	 *            list of expressions
	 */
	private void addSetAddress(RAM ram, Var var, List<Expression> indexes) {
		int port = ram.getLastPortUsed() % numPortsRam + 1;
		InstRamSetAddress rsa = InstructionsFactory.eINSTANCE
				.createInstRamSetAddress(port, var, indexes);
		rsa.setPredicate(IrUtil.copy(ram.getPredicate()));
		instructions.add(indexInst++, rsa);
	}

	/**
	 * Adds a split instruction at the current index in instructions.
	 * 
	 * @param ram
	 *            a RAM
	 */
	private void addSplitInstruction(RAM ram) {
		InstSplit instSplit = InstructionsFactory.eINSTANCE.createInstSplit();
		instSplit.setPredicate(IrUtil.copy(ram.getPredicate()));
		instructions.add(indexInst++, instSplit);
	}

	/**
	 * Adds a RamWrite instruction at the current index.
	 * 
	 * @param predicate
	 *            a predicate
	 * @param ram
	 *            a RAM
	 * @param store
	 *            the InstStore used to create the InstRamWrite
	 */
	private void addWrite(RAM ram, InstStore store) {
		int port = ram.getLastPortUsed() % numPortsRam + 1;
		InstRamWrite write = InstructionsFactory.eINSTANCE.createInstRamWrite(
				port, store.getTarget().getVariable(), store.getValue());
		write.setPredicate(IrUtil.copy(ram.getPredicate()));
		instructions.add(indexInst++, write);
	}

	@Override
	public Object caseActor(Actor actor) {
		pendingReads = new HashMap<RAM, List<InstRamRead>>();

		// creates RAM map and reference it in the template data
		ramMap = new HashMap<Var, RAM>();
		((VHDLTemplateData) actor.getTemplateData()).setRamMap(ramMap);

		// fill RAM map
		for (Var variable : actor.getStateVars()) {
			if (isVarTransformableToRam(variable)) {
				RAM ram = new RAM(numPortsRam);
				ramMap.put(variable, ram);

				// initialize the list of pending reads for this RAM
				pendingReads.put(ram, new ArrayList<InstRamRead>(numPortsRam));
			}
		}

		for (Action action : actor.getActions()) {
			doSwitch(action.getBody());
		}

		return null;
	}

	@Override
	public Object caseInstLoad(InstLoad load) {
		if (isVarTransformableToRam(load.getSource().getVariable())) {
			convertLoad(load);
			IrUtil.delete(load);
			indexInst--;
		}
		return null;
	}

	@Override
	public Object caseInstruction(Instruction instruction) {
		// do not consider deleted instructions
		if (instruction.eContainer() == null) {
			return null;
		}

		// check if this instruction should be moved to the pending list:
		// happens when at least one instruction that defines one of the
		// variables used by this instruction is in the pending list
		boolean addToPending = false;
		for (Use use : EcoreHelper.getObjects(instruction, Use.class)) {
			Var var = use.getVariable();
			if (var.isLocal() && !var.getType().isList()) {
				for (Def def : var.getDefs()) {
					Instruction defInst = EcoreHelper.getContainerOfType(def,
							Instruction.class);
					if (pendingInstructions.contains(defInst)) {
						addToPending = true;
						break;
					}
				}
			}
		}

		// if necessary, move instruction to pending list
		if (addToPending) {
			// removes this instruction from the instruction
			// list and adds it to the pending list
			EcoreUtil.remove(instruction);
			pendingInstructions.add(instruction);

			// updates the index so we don't skip the next
			// instruction
			indexInst--;
		}

		return null;
	}

	@Override
	public Object caseInstStore(InstStore store) {
		Var var = store.getTarget().getVariable();
		if (isVarTransformableToRam(var)) {
			convertStore(store);
			IrUtil.delete(store);
			indexInst--;
		} else if (var.eContainer() instanceof Pattern) {
			// removes this instruction from the instruction
			// list and adds it to the pending list
			EcoreUtil.remove(store);
			pendingStores.add(store);

			// updates the index so we don't skip the next
			// instruction
			indexInst--;
		}

		return null;
	}

	@Override
	public Object caseProcedure(Procedure procedure) {
		pendingInstructions = new ArrayList<Instruction>();
		pendingStores = new ArrayList<InstStore>();

		instructions = null; // just in case
		super.caseProcedure(procedure);

		NodeBlock block = procedure.getLast();
		for (RAM ram : ramMap.values()) {
			// set the RAM as "never accessed"
			ram.reset();

			boolean hasMorePendingReads;
			instructions = block.getInstructions();
			indexInst = instructions.size() - 1; // before the return
			do {
				hasMorePendingReads = executeAllPendingReads(ram);
			} while (hasMorePendingReads);
		}

		// add all pending stores
		instructions = block.getInstructions();
		indexInst = instructions.size() - 1; // before the return
		instructions.addAll(indexInst, pendingStores);

		return null;
	}

	/**
	 * Converts the given Load to RAM instructions.
	 * 
	 * @param load
	 *            a Load
	 */
	private void convertLoad(InstLoad load) {
		instructions = EcoreHelper.getContainingList(load);
		List<Expression> indexes = load.getIndexes();
		Var var = load.getSource().getVariable();
		Predicate predicate = load.getPredicate();

		RAM ram = ramMap.get(var);
		if (ram.isLastAccessRead()
				&& !ram.getPredicate().isMutuallyExclusive(predicate)) {
			int port = ram.getLastPortUsed();
			if ((port + 1) % numPortsRam == 0) {
				// all ports have been used
				executeAllPendingReads(ram);
			}

			// increase the number of the port used
			port++;
			ram.setLastPortUsed(port);
			ram.setPredicate(predicate);

			if (port < numPortsRam) {
				addSetAddress(ram, var, indexes);
			} else {
				addEarlySetAddress(ram, var, indexes);
			}
			addPendingRead(ram, load);
		} else {
			if (ram.isLastAccessWrite()) {
				// read after write... let's wait a cycle
				addSplitInstruction(ram);
			}

			// udpate state of RAM
			ram.setLastAccessRead();
			ram.setLastPortUsed(0);
			ram.setPredicate(predicate);

			// set address and add pending read
			addSetAddress(ram, var, indexes);
			addPendingRead(ram, load);
		}
	}

	/**
	 * Converts the given Store to RAM instructions.
	 * 
	 * @param store
	 *            a Store
	 */
	private void convertStore(InstStore store) {
		instructions = EcoreHelper.getContainingList(store);
		List<Expression> indexes = store.getIndexes();
		Var var = store.getTarget().getVariable();
		Predicate predicate = store.getPredicate();

		RAM ram = ramMap.get(var);
		int port;
		boolean addPendingWrites = false;
		if (ram.isLastAccessWrite()
				&& !ram.getPredicate().isMutuallyExclusive(predicate)) {
			port = ram.getLastPortUsed() + 1;
			ram.setPredicate(predicate);
			if (port > 0 && port % numPortsRam == 0) {
				// port == numPortsRam, 2 * numPortsRam, 3 * numPortsRam...
				addSplitInstruction(ram);
			}
		} else {
			// need to schedule the Writes after the pending reads
			addPendingWrites = ram.isLastAccessRead();

			port = 0;
		}

		// update state of RAM
		ram.setLastAccessWrite();
		ram.setLastPortUsed(port);
		ram.setPredicate(predicate);

		// set address and write
		addSetAddress(ram, var, indexes);
		addWrite(ram, store);

		// move the two instructions set address and write to pending list
		if (addPendingWrites) {
			indexInst -= 2;
			for (int i = 0; i < 2; i++) {
				pendingInstructions.add(instructions.remove(indexInst));
			}
		}
	}

	/**
	 * Executes at most two pending reads. This method inserts split
	 * instructions as necessary before the reads.
	 * 
	 * @param instructions
	 *            a list of instructions that this method should add
	 *            instructions to
	 * @param predicate
	 *            predicate of instructions
	 * @param ram
	 *            a RAM
	 * @return <code>true</code> if there are still pending reads
	 */
	private boolean executeAllPendingReads(RAM ram) {
		List<InstRamRead> reads = pendingReads.get(ram);
		boolean moreReadsPending;
		if (reads.isEmpty()) {
			moreReadsPending = false;
		} else {
			if (ram.getLastPortUsed() < numPortsRam) {
				addSplitInstruction(ram);
			}

			addSplitInstruction(ram);

			Iterator<InstRamRead> it = reads.iterator();
			for (int i = 0; it.hasNext() && i < numPortsRam; i++) {
				InstRamRead read = it.next();
				instructions.add(indexInst++, read);
				pendingInstructions.remove(read);
				it.remove();
			}

			moreReadsPending = it.hasNext();
		}

		// add all pending instructions up to the first RamRead
		Iterator<Instruction> itPending = pendingInstructions.iterator();
		while (itPending.hasNext()) {
			Instruction pending = itPending.next();
			if (pending instanceof InstRamRead) {
				break;
			}
			instructions.add(indexInst++, pending);
			itPending.remove();
		}

		// returns true if there are more reads pending
		return moreReadsPending;
	}

	/**
	 * Returns <code>true</code> if the given variable can be transformed as a
	 * RAM.
	 * 
	 * @param var
	 *            a variable
	 * @return <code>true</code> if the given variable can be transformed as a
	 *         RAM
	 */
	private boolean isVarTransformableToRam(Var var) {
		boolean isCandidate = var.isAssignable() && var.isGlobal()
				&& var.getType().isList();
		if (isCandidate) {
			// only put RAM for memories larger than 1024 bits
			TypeList typeList = (TypeList) var.getType();
			int size = typeList.getSizeInBits();
			return (size > 1024);
		}

		return false;
	}

}
