/*
 * Copyright (c) 2009-2011, IETR/INSA of Rennes
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
package net.sf.orcc.df;

import java.util.List;
import java.util.Map;

import net.sf.dftools.graph.Edge;
import net.sf.dftools.graph.Graph;
import net.sf.dftools.graph.Vertex;
import net.sf.orcc.OrccException;
import net.sf.orcc.ir.Var;
import net.sf.orcc.moc.MoC;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->This class defines a hierarchical XDF network. It
 * extends both entity and graph.<!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link net.sf.orcc.df.Network#getEntities <em>Entities</em>}</li>
 *   <li>{@link net.sf.orcc.df.Network#getFileName <em>File Name</em>}</li>
 *   <li>{@link net.sf.orcc.df.Network#getInputs <em>Inputs</em>}</li>
 *   <li>{@link net.sf.orcc.df.Network#getInstances <em>Instances</em>}</li>
 *   <li>{@link net.sf.orcc.df.Network#getMoC <em>Mo C</em>}</li>
 *   <li>{@link net.sf.orcc.df.Network#getName <em>Name</em>}</li>
 *   <li>{@link net.sf.orcc.df.Network#getOutputs <em>Outputs</em>}</li>
 *   <li>{@link net.sf.orcc.df.Network#getParameters <em>Parameters</em>}</li>
 *   <li>{@link net.sf.orcc.df.Network#getVariables <em>Variables</em>}</li>
 * </ul>
 * </p>
 *
 * @see net.sf.orcc.df.DfPackage#getNetwork()
 * @model
 * @generated
 */
public interface Network extends Graph {

	/**
	 * Adds the given port to this network. The port is added to the vertices
	 * and the inputs list.
	 * 
	 * @param port
	 *            a port
	 */
	void addInput(Port port);

	/**
	 * Adds the given port to this network. The port is added to the vertices
	 * and the outputs list.
	 * 
	 * @param port
	 *            a port
	 */
	void addOutput(Port port);

	/**
	 * Classifies this network.
	 * 
	 * @throws OrccException
	 *             if something goes wrong
	 */
	void classify() throws OrccException;

	/**
	 * Computes the source map and target maps that associate each connection to
	 * its source vertex (respectively target vertex).
	 */
	void computeTemplateMaps();

	/**
	 * Returns the list of actors referenced by or contained in this network,
	 * and its sub-networks. This is different from the list of instances of
	 * this network: There are typically more instances than there are actors,
	 * because an actor may be instantiated several times.
	 * 
	 * <p>
	 * The list is computed on the fly by adding all the actors referenced in a
	 * set.
	 * </p>
	 * 
	 * @return a list of actors
	 */
	List<Actor> getAllActors();

	/**
	 * Returns the list of networks referenced by or contained in this network,
	 * and its sub-networks. This is different from the list of instances of
	 * this network: There are typically more instances than there are networks,
	 * because a network may be instantiated several times.
	 * 
	 * <p>
	 * The list is computed on the fly by adding all the networks referenced in
	 * a set.
	 * </p>
	 * 
	 * @return a list of networks
	 */
	List<Network> getAllNetworks();

	/**
	 * Returns the list of this graph's connections. This returns the same as
	 * {@link #getEdges()} but as a list of {@link Connection}s rather than as a
	 * list of {@link Edge}s.
	 * 
	 * @return the list of this graph's connections
	 */
	EList<Connection> getConnections();

	/**
	 * Returns the value of the '<em><b>Entities</b></em>' reference list. The
	 * list contents are of type {@link net.sf.dftools.graph.Vertex}. <!--
	 * begin-user-doc --><!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Entities</em>' reference list.
	 * @see net.sf.orcc.df.DfPackage#getNetwork_Entities()
	 * @model
	 * @generated
	 */
	EList<Vertex> getEntities();

	/**
	 * Returns the file this network is defined in.
	 * 
	 * @return the file this network is defined in
	 */
	IFile getFile();

	/**
	 * Returns the value of the '<em><b>File Name</b></em>' attribute. <!--
	 * begin-user-doc --><!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>File Name</em>' attribute.
	 * @see #setFileName(String)
	 * @see net.sf.orcc.df.DfPackage#getNetwork_FileName()
	 * @model
	 * @generated
	 */
	String getFileName();

	/**
	 * Returns the input port whose name matches the given name.
	 * 
	 * @param name
	 *            the port name
	 * @return an input port whose name matches the given name
	 */
	Port getInput(String name);

	/**
	 * Returns the value of the '<em><b>Inputs</b></em>' reference list. The
	 * list contents are of type {@link net.sf.orcc.df.Port}. <!--
	 * begin-user-doc --><!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Inputs</em>' reference list.
	 * @see net.sf.orcc.df.DfPackage#getNetwork_Inputs()
	 * @model
	 * @generated
	 */
	EList<Port> getInputs();

	Instance getInstance(String id);

	/**
	 * Returns the value of the '<em><b>Instances</b></em>' reference list. The
	 * list contents are of type {@link net.sf.orcc.df.Instance}. <!--
	 * begin-user-doc --><!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Instances</em>' reference list.
	 * @see net.sf.orcc.df.DfPackage#getNetwork_Instances()
	 * @model
	 * @generated
	 */
	EList<Instance> getInstances();

	/**
	 * Returns the list of instances of the given actor in the graph.
	 * 
	 * @param actor
	 *            the actor to get the instance of
	 * 
	 * @return a list of instances
	 */
	List<Instance> getInstancesOf(Actor actor);

	/**
	 * Returns the value of the '<em><b>Mo C</b></em>' containment reference.
	 * <!-- begin-user-doc --><!-- end-user-doc -->
	 * @return the value of the '<em>Mo C</em>' containment reference.
	 * @see #setMoC(MoC)
	 * @see net.sf.orcc.df.DfPackage#getNetwork_MoC()
	 * @model containment="true"
	 * @generated
	 */
	MoC getMoC();

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute. <!--
	 * begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Name</em>' attribute isn't clear, there really
	 * should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see net.sf.orcc.df.DfPackage#getNetwork_Name()
	 * @model transient="true" volatile="true" derived="true"
	 * @generated
	 */
	String getName();

	Map<Port, List<Connection>> getOutgoingPortMap();

	/**
	 * Returns the output port whose name matches the given name.
	 * 
	 * @param name
	 *            the port name
	 * @return an output port whose name matches the given name
	 */
	Port getOutput(String name);

	/**
	 * Returns the value of the '<em><b>Outputs</b></em>' reference list. The
	 * list contents are of type {@link net.sf.orcc.df.Port}. <!--
	 * begin-user-doc --><!-- end-user-doc -->
	 * 
	 * @return the value of the '<em>Outputs</em>' reference list.
	 * @see net.sf.orcc.df.DfPackage#getNetwork_Outputs()
	 * @model
	 * @generated
	 */
	EList<Port> getOutputs();

	/**
	 * Returns the parameter with the given name.
	 * 
	 * @param name
	 *            name of a parameter
	 * @return the parameter with the given name
	 */
	Var getParameter(String name);

	/**
	 * Returns the value of the '<em><b>Parameters</b></em>' containment reference list.
	 * The list contents are of type {@link net.sf.orcc.ir.Var}.
	 * <!-- begin-user-doc -->
	 * <p>
	 * If the meaning of the '<em>Parameters</em>' containment reference list
	 * isn't clear, there really should be more of a description here...
	 * </p>
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Parameters</em>' containment reference list.
	 * @see net.sf.orcc.df.DfPackage#getNetwork_Parameters()
	 * @model containment="true"
	 * @generated
	 */
	EList<Var> getParameters();

	String getSimpleName();

	/**
	 * Returns the variable with the given name.
	 * 
	 * @param name
	 *            name of a variable
	 * @return the variable with the given name
	 */
	Var getVariable(String name);

	/**
	 * Returns the value of the '<em><b>Variables</b></em>' containment reference list.
	 * The list contents are of type {@link net.sf.orcc.ir.Var}.
	 * <!-- begin-user-doc --><!-- end-user-doc -->
	 * @return the value of the '<em>Variables</em>' containment reference list.
	 * @see net.sf.orcc.df.DfPackage#getNetwork_Variables()
	 * @model containment="true"
	 * @generated
	 */
	EList<Var> getVariables();

	/**
	 * Merges actors of this network. Note that for this transformation to work
	 * properly, actors must have been classified and normalized first.
	 * 
	 * @throws OrccException
	 *             if something goes wrong
	 */
	void mergeActors() throws OrccException;

	/**
	 * Normalizes actors of this network so they can later be merged. Note that
	 * for this transformation to work properly, actors must have been
	 * classified first.
	 * 
	 * @throws OrccException
	 *             if something goes wrong
	 */
	void normalizeActors() throws OrccException;

	/**
	 * Removes the given instance from the list of instances, and then removes
	 * it from this graph.
	 * 
	 * @param instance
	 *            an instance
	 */
	void remove(Instance instance);

	/**
	 * Removes the given entity from the list of entities, and then removes it
	 * from this graph.
	 * 
	 * @param entity
	 *            an entity
	 */
	void removeEntity(Vertex entity);

	/**
	 * Sets the value of the '{@link net.sf.orcc.df.Network#getFileName
	 * <em>File Name</em>}' attribute. <!-- begin-user-doc --><!-- end-user-doc
	 * -->
	 * 
	 * @param value
	 *            the new value of the '<em>File Name</em>' attribute.
	 * @see #getFileName()
	 * @generated
	 */
	void setFileName(String value);

	/**
	 * Sets the value of the '{@link net.sf.orcc.df.Network#getMoC <em>Mo C</em>}' containment reference.
	 * <!-- begin-user-doc --><!--
	 * end-user-doc -->
	 * @param value the new value of the '<em>Mo C</em>' containment reference.
	 * @see #getMoC()
	 * @generated
	 */
	void setMoC(MoC value);

	/**
	 * Sets the value of the '{@link net.sf.orcc.df.Network#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

}
