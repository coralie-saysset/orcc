/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package net.sf.orcc.ir.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.orcc.ir.AbstractActorVisitor;
import net.sf.orcc.ir.CFG;
import net.sf.orcc.ir.Expression;
import net.sf.orcc.ir.GlobalVariable;
import net.sf.orcc.ir.IrPackage;
import net.sf.orcc.ir.LocalVariable;
import net.sf.orcc.ir.Location;
import net.sf.orcc.ir.Node;
import net.sf.orcc.ir.NodeBlock;
import net.sf.orcc.ir.Procedure;
import net.sf.orcc.ir.Type;
import net.sf.orcc.ir.Variable;
import net.sf.orcc.ir.instructions.Load;
import net.sf.orcc.ir.instructions.Store;
import net.sf.orcc.util.OrderedMap;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;

/**
 * <!-- begin-user-doc --> An implementation of the model object '
 * <em><b>Procedure</b></em>'. <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 * <li>{@link net.sf.orcc.ir.impl.ProcedureImpl#getLocation <em>Location</em>}</li>
 * <li>{@link net.sf.orcc.ir.impl.ProcedureImpl#getName <em>Name</em>}</li>
 * <li>{@link net.sf.orcc.ir.impl.ProcedureImpl#getNodes <em>Nodes</em>}</li>
 * <li>{@link net.sf.orcc.ir.impl.ProcedureImpl#getReturnType <em>Return Type
 * </em>}</li>
 * <li>{@link net.sf.orcc.ir.impl.ProcedureImpl#isNative <em>Native</em>}</li>
 * </ul>
 * </p>
 * 
 * @generated
 */
public class ProcedureImpl extends EObjectImpl implements Procedure {
	/**
	 * This class visits the procedure to find the state variables used.
	 * 
	 * @author Matthieu Wipliez
	 * 
	 */
	private class ProcVisitor extends AbstractActorVisitor {

		private Set<GlobalVariable> loadedVariables;

		private Set<GlobalVariable> storedVariables;

		public ProcVisitor() {
			storedVariables = new HashSet<GlobalVariable>();
			loadedVariables = new HashSet<GlobalVariable>();
		}

		public List<GlobalVariable> getLoadedVariables() {
			return new ArrayList<GlobalVariable>(loadedVariables);
		}

		public List<GlobalVariable> getStoredVariables() {
			return new ArrayList<GlobalVariable>(storedVariables);
		}

		@Override
		public void visit(Load node) {
			Variable var = node.getSource().getVariable();
			if (!var.getType().isList()) {
				loadedVariables.add((GlobalVariable) var);
			}
		}

		@Override
		public void visit(Store store) {
			Variable var = store.getTarget();
			if (!var.getType().isList()) {
				storedVariables.add((GlobalVariable) var);
			}
		}

	}

	/**
	 * The default value of the '{@link #getLocation() <em>Location</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getLocation()
	 * @generated
	 * @ordered
	 */
	protected static final Location LOCATION_EDEFAULT = null;

	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The default value of the '{@link #isNative() <em>Native</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isNative()
	 * @generated
	 * @ordered
	 */
	protected static final boolean NATIVE_EDEFAULT = false;

	private CFG graph;

	/**
	 * ordered map of local variables
	 */
	private OrderedMap<String, LocalVariable> locals;

	/**
	 * The cached value of the '{@link #getLocation() <em>Location</em>}'
	 * attribute. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getLocation()
	 * @generated
	 * @ordered
	 */
	protected Location location = LOCATION_EDEFAULT;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The cached value of the '{@link #isNative() <em>Native</em>}' attribute.
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #isNative()
	 * @generated
	 * @ordered
	 */
	protected boolean native_ = NATIVE_EDEFAULT;

	/**
	 * The cached value of the '{@link #getNodes() <em>Nodes</em>}' reference
	 * list. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getNodes()
	 * @generated
	 * @ordered
	 */
	protected EList<Node> nodes;

	/**
	 * ordered map of parameters
	 */
	private OrderedMap<String, LocalVariable> parameters;

	private Expression result;

	/**
	 * The cached value of the '{@link #getReturnType() <em>Return Type</em>}'
	 * reference. <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @see #getReturnType()
	 * @generated
	 * @ordered
	 */
	protected Type returnType;

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	protected ProcedureImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public Type basicGetReturnType() {
		return returnType;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
		case IrPackage.PROCEDURE__LOCATION:
			return getLocation();
		case IrPackage.PROCEDURE__NAME:
			return getName();
		case IrPackage.PROCEDURE__NODES:
			return getNodes();
		case IrPackage.PROCEDURE__RETURN_TYPE:
			if (resolve)
				return getReturnType();
			return basicGetReturnType();
		case IrPackage.PROCEDURE__NATIVE:
			return isNative();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
		case IrPackage.PROCEDURE__LOCATION:
			return LOCATION_EDEFAULT == null ? location != null
					: !LOCATION_EDEFAULT.equals(location);
		case IrPackage.PROCEDURE__NAME:
			return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT
					.equals(name);
		case IrPackage.PROCEDURE__NODES:
			return nodes != null && !nodes.isEmpty();
		case IrPackage.PROCEDURE__RETURN_TYPE:
			return returnType != null;
		case IrPackage.PROCEDURE__NATIVE:
			return native_ != NATIVE_EDEFAULT;
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
		case IrPackage.PROCEDURE__LOCATION:
			setLocation((Location) newValue);
			return;
		case IrPackage.PROCEDURE__NAME:
			setName((String) newValue);
			return;
		case IrPackage.PROCEDURE__NODES:
			getNodes().clear();
			getNodes().addAll((Collection<? extends Node>) newValue);
			return;
		case IrPackage.PROCEDURE__RETURN_TYPE:
			setReturnType((Type) newValue);
			return;
		case IrPackage.PROCEDURE__NATIVE:
			setNative((Boolean) newValue);
			return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return IrPackage.Literals.PROCEDURE;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
		case IrPackage.PROCEDURE__LOCATION:
			setLocation(LOCATION_EDEFAULT);
			return;
		case IrPackage.PROCEDURE__NAME:
			setName(NAME_EDEFAULT);
			return;
		case IrPackage.PROCEDURE__NODES:
			getNodes().clear();
			return;
		case IrPackage.PROCEDURE__RETURN_TYPE:
			setReturnType((Type) null);
			return;
		case IrPackage.PROCEDURE__NATIVE:
			setNative(NATIVE_EDEFAULT);
			return;
		}
		super.eUnset(featureID);
	}

	/**
	 * Returns the CFG of this procedure. The CFG must be set by calling
	 * {@link #setGraph(CFG)}.
	 * 
	 * @return the CFG of this procedure
	 */
	public CFG getCFG() {
		return graph;
	}

	/**
	 * Returns the first block in the list of nodes of the given procedure. A
	 * new block is created if there is no block in the given node list.
	 * 
	 * @param procedure
	 *            a procedure
	 * @return a block
	 */
	public NodeBlock getFirst() {
		return getFirst(getNodes());
	}

	/**
	 * Returns the first block in the given list of nodes. A new block is
	 * created if there is no block in the given node list.
	 * 
	 * @param procedure
	 *            a procedure
	 * @param nodes
	 *            a list of nodes of the given procedure
	 * @return a block
	 */
	public NodeBlock getFirst(List<Node> nodes) {
		NodeBlock block;
		if (nodes.isEmpty()) {
			block = IrFactoryImpl.eINSTANCE.createNodeBlock();
			nodes.add(block);
		} else {
			Node node = nodes.get(0);
			if (node.isBlockNode()) {
				block = (NodeBlock) node;
			} else {
				block = IrFactoryImpl.eINSTANCE.createNodeBlock();
				nodes.add(0, block);
			}
		}

		return block;
	}

	/**
	 * Returns the last block in the list of nodes of the given procedure. A new
	 * block is created if there is no block in the given node list.
	 * 
	 * @param procedure
	 *            a procedure
	 * @return a block
	 */
	public NodeBlock getLast() {
		return getLast(getNodes());
	}

	/**
	 * Returns the last block in the given list of nodes. A new block is created
	 * if there is no block in the given node list.
	 * 
	 * @param procedure
	 *            a procedure
	 * @param nodes
	 *            a list of nodes that are a subset of the given procedure's
	 *            nodes
	 * @return a block
	 */
	public NodeBlock getLast(List<Node> nodes) {
		NodeBlock block;
		if (nodes.isEmpty()) {
			block = IrFactoryImpl.eINSTANCE.createNodeBlock();
			nodes.add(block);
		} else {
			Node node = nodes.get(nodes.size() - 1);
			if (node.isBlockNode()) {
				block = (NodeBlock) node;
			} else {
				block = IrFactoryImpl.eINSTANCE.createNodeBlock();
				nodes.add(block);
			}
		}

		return block;
	}

	/**
	 * Computes and returns the list of scalar variables loaded by this
	 * procedure.
	 * 
	 * @return the list of scalar variables loaded by this procedure
	 */
	public List<GlobalVariable> getLoadedVariables() {
		ProcVisitor visitor = new ProcVisitor();
		visitor.visit(nodes);
		return visitor.getLoadedVariables();
	}

	/**
	 * Returns the local variables of this procedure as an ordered map.
	 * 
	 * @return the local variables of this procedure as an ordered map
	 */
	public OrderedMap<String, LocalVariable> getLocals() {
		return locals;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public EList<Node> getNodes() {
		if (nodes == null) {
			nodes = new EObjectResolvingEList<Node>(Node.class, this,
					IrPackage.PROCEDURE__NODES);
		}
		return nodes;
	}

	/**
	 * Returns the parameters of this procedure as an ordered map.
	 * 
	 * @return the parameters of this procedure as an ordered map
	 */
	public OrderedMap<String, LocalVariable> getParameters() {
		return parameters;
	}

	public Expression getResult() {
		return result;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public Type getReturnType() {
		if (returnType != null && returnType.eIsProxy()) {
			InternalEObject oldReturnType = (InternalEObject) returnType;
			returnType = (Type) eResolveProxy(oldReturnType);
			if (returnType != oldReturnType) {
				if (eNotificationRequired())
					eNotify(new ENotificationImpl(this, Notification.RESOLVE,
							IrPackage.PROCEDURE__RETURN_TYPE, oldReturnType,
							returnType));
			}
		}
		return returnType;
	}

	/**
	 * Computes and returns the list of scalar variables stored by this
	 * procedure.
	 * 
	 * @return the list of scalar variables stored by this procedure
	 */
	public List<GlobalVariable> getStoredVariables() {
		ProcVisitor visitor = new ProcVisitor();
		visitor.visit(nodes);
		return visitor.getStoredVariables();
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public boolean isNative() {
		return native_;
	}

	/**
	 * Creates a new local variable that can be used to hold intermediate
	 * results. The variable is added to {@link #procedure}'s locals.
	 * 
	 * @param file
	 *            the file in which this procedure resides
	 * @param type
	 *            type of the variable
	 * @param name
	 *            hint for the variable name
	 * @return a new local variable
	 */
	public LocalVariable newTempLocalVariable(String file, Type type,
			String hint) {
		String name = hint;
		LocalVariable variable = locals.get(name);
		int i = 0;
		while (variable != null) {
			name = hint + i;
			variable = locals.get(name);
			i++;
		}

		variable = new LocalVariable(true, 0, new Location(), name, type);
		locals.put(file, variable.getLocation(), variable.getName(), variable);
		return (LocalVariable) variable;
	}

	/**
	 * Set the CFG of this procedure.
	 * 
	 * @param the
	 *            CFG of this procedure
	 */
	public void setGraph(CFG graph) {
		this.graph = graph;
	}

	@Override
	public void setLocals(OrderedMap<String, LocalVariable> locals) {
		this.locals = locals;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void setLocation(Location newLocation) {
		Location oldLocation = location;
		location = newLocation;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					IrPackage.PROCEDURE__LOCATION, oldLocation, location));
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					IrPackage.PROCEDURE__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void setNative(boolean newNative) {
		boolean oldNative = native_;
		native_ = newNative;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					IrPackage.PROCEDURE__NATIVE, oldNative, native_));
	}

	@Override
	public void setParameters(OrderedMap<String, LocalVariable> parameters) {
		this.parameters = parameters;
	}

	public void setResult(Expression result) {
		this.result = result;
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	public void setReturnType(Type newReturnType) {
		Type oldReturnType = returnType;
		returnType = newReturnType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET,
					IrPackage.PROCEDURE__RETURN_TYPE, oldReturnType, returnType));
	}

	/**
	 * <!-- begin-user-doc --> <!-- end-user-doc -->
	 * 
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy())
			return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (location: ");
		result.append(location);
		result.append(", name: ");
		result.append(name);
		result.append(", native: ");
		result.append(native_);
		result.append(')');
		return result.toString();
	}

} // ProcedureImpl
