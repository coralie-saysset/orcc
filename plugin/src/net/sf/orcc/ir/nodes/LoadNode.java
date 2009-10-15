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
package net.sf.orcc.ir.nodes;

import java.util.List;

import net.sf.orcc.common.Location;
import net.sf.orcc.ir.VarDef;
import net.sf.orcc.ir.actor.VarUse;
import net.sf.orcc.ir.expr.IExpr;

/**
 * @author Matthieu Wipliez
 * 
 */
public class LoadNode extends AbstractNode {

	private List<IExpr> indexes;

	private VarUse source;

	private VarDef target;

	public LoadNode(int id, Location location, VarDef target, VarUse source,
			List<IExpr> indexes) {
		super(id, location);
		this.indexes = indexes;
		this.source = source;
		this.target = target;
	}

	@Override
	public void accept(NodeVisitor visitor, Object... args) {
		visitor.visit(this, args);
	}

	public List<IExpr> getIndexes() {
		return indexes;
	}

	public VarUse getSource() {
		return source;
	}

	public VarDef getTarget() {
		return target;
	}

	public void setIndexes(List<IExpr> indexes) {
		this.indexes = indexes;
	}

	public void setSource(VarUse source) {
		this.source = source;
	}

	public void setTarget(VarDef target) {
		this.target = target;
	}

	@Override
	public String toString() {
		return target + " = " + source + indexes;
	}

}
