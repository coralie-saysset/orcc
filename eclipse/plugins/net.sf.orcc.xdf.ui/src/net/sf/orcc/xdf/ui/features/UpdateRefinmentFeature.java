/*
 * Copyright (c) 2013, IETR/INSA of Rennes
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
package net.sf.orcc.xdf.ui.features;

import net.sf.orcc.df.Instance;
import net.sf.orcc.df.Port;
import net.sf.orcc.xdf.ui.dialogs.EntitySelectionDialog;
import net.sf.orcc.xdf.ui.patterns.InstancePattern;
import net.sf.orcc.xdf.ui.util.XdfUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IContext;
import org.eclipse.graphiti.features.context.ICustomContext;
import org.eclipse.graphiti.features.custom.AbstractCustomFeature;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.pattern.IFeatureProviderWithPatterns;
import org.eclipse.graphiti.pattern.IPattern;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IPeService;
import org.eclipse.jface.dialogs.Dialog;

/**
 * This class implements the dialog shown to user when he want to set or modify
 * the entity (Actor or Network) contained by an instance.
 * 
 * @author Antoine Lorence
 * 
 */
public class UpdateRefinmentFeature extends AbstractCustomFeature {

	private final IPeService peService;
	private boolean hasDoneChanges = false;

	public UpdateRefinmentFeature(final IFeatureProvider fp) {
		super(fp);
		peService = Graphiti.getPeService();
	}

	@Override
	public String getName() {
		return "Set/Update refinment";
	}

	@Override
	public String getDescription() {
		return "Assign an existing Actor or Network to this Instance";
	}

	@Override
	public boolean isAvailable(IContext context) {
		// TODO Check cases for isAvailable/canExecute when ports
		// will be available in the diagram
		return super.isAvailable(context);
	}

	@Override
	public boolean canExecute(final ICustomContext context) {

		PictogramElement[] pes = context.getPictogramElements();
		if (pes.length != 1) {
			return false;
		}

		PictogramElement pe = pes[0];
		if (getBusinessObjectForPictogramElement(pe) instanceof Instance) {
			return true;
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void execute(final ICustomContext context) {

		Instance instance = (Instance) getBusinessObjectForPictogramElement(context
				.getPictogramElements()[0]);

		String path = instance.eResource().getURI().toPlatformString(true);
		IProject project = ResourcesPlugin.getWorkspace().getRoot()
				.getFile(new Path(path)).getProject();

		EntitySelectionDialog entityDialog;

		try {
			entityDialog = new EntitySelectionDialog(XdfUtil.getDefaultShell(),
					project);
		} catch (CoreException e) {
			e.printStackTrace();
			return;
		}

		entityDialog.setTitle("Select an entity");
		entityDialog.setMessage("Choose the entity you want to use "
				+ "in this Instance. You can filter "
				+ "results with string patterns.");

		// FIXME: Find why this is mandatory.
		// Allow blank pattern corresponding to '*'
		entityDialog.setInitialPattern("**");

		int returnCode = entityDialog.open();

		if (returnCode == Dialog.CANCEL) {
			hasDoneChanges = false;
			return;
		}

		// Update the current instance with entity selected by user
		instance.setEntity((EObject) entityDialog.getFirstResult());
		// Inform editor that changes has been done in the model
		hasDoneChanges = true;

		// Now, we want to display ports of the selected entity in the instance
		// shape (graphical representation of the Instance)

		ContainerShape instanceShape;
		{
			final PictogramElement clickedPe = context.getInnerPictogramElement();
			// We want to get the instance container shape
			if (clickedPe.isActive()) {
				// This this the shape we want to use
				instanceShape = (ContainerShape) clickedPe;
			} else {
				// Get the active container shape
				instanceShape = (ContainerShape) peService.getActiveContainerPe(clickedPe);
			}
		}

		// We get the corresponding Pattern. It must be an InstancePattern
		IPattern ipattern = ((IFeatureProviderWithPatterns) getFeatureProvider())
				.getPatternForPictogramElement(instanceShape);
		InstancePattern pattern = (InstancePattern) ipattern;

		// Remove ports for this instance
		pattern.cleanInputsPorts(instanceShape);
		pattern.cleanOutputsPorts(instanceShape);

		EList<Port> inputs, outputs;
		if (instance.isActor()) {
			inputs = instance.getActor().getInputs();
			outputs = instance.getActor().getOutputs();
		} else if (instance.isNetwork()) {
			inputs = instance.getNetwork().getInputs();
			outputs = instance.getNetwork().getOutputs();
		} else {
			// Error message ?
			return;
		}
		// Add new ports to this instance
		pattern.addInputsPorts(instanceShape, inputs);
		pattern.addOutputsPorts(instanceShape, outputs);

		updatePictogramElement(instanceShape);
	}

	@Override
	public boolean hasDoneChanges() {
		return hasDoneChanges;
	}
}
