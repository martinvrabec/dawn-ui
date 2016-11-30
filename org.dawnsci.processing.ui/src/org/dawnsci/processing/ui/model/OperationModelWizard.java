package org.dawnsci.processing.ui.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dawnsci.processing.ui.ServiceHolder;
import org.dawnsci.processing.ui.api.IOperationModelWizard;
import org.dawnsci.processing.ui.api.IOperationSetupWizardPage;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistenceService;
import org.eclipse.dawnsci.analysis.api.persistence.IPersistentFile;
import org.eclipse.dawnsci.analysis.api.processing.IOperation;
import org.eclipse.dawnsci.analysis.api.processing.OperationData;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.Wizard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OperationModelWizard extends Wizard implements IOperationModelWizard {

	private final List<IOperationSetupWizardPage> wizardPages;
	
	@SuppressWarnings("unused")
	final static private Logger logger = LoggerFactory.getLogger(OperationModelWizard.class);
	
	public OperationModelWizard(final IDataset initialData, final List<IOperationSetupWizardPage> wizardPages) {
		if (wizardPages == null || wizardPages.size()== 0)
			throw new IllegalArgumentException("Constructor must be passed at least one IOperationSetupWizardPage");
		this.wizardPages = wizardPages;
		this.wizardPages.get(0).setInputData(new OperationData(initialData));
		setHelpAvailable(false);
	}
	
	public OperationModelWizard(final IDataset initialData, final IOperationSetupWizardPage wizardPage) {
		if (wizardPage == null)
			throw new IllegalArgumentException("Constructor must be passed at least one IOperationSetupWizardPage");
		this.wizardPages = new ArrayList<>();
		this.wizardPages.add(wizardPage);
		this.wizardPages.get(0).setInputData(new OperationData(initialData));
		setHelpAvailable(false);
	}
	
	public OperationModelWizard(final IDataset initialData, final IOperationSetupWizardPage... wizardPages) {
		if (wizardPages == null || wizardPages.length == 0)
			throw new IllegalArgumentException("Constructor must be passed at least one IOperationSetupWizardPage");
		this.wizardPages = new ArrayList<>();
		Arrays.stream(wizardPages).forEachOrdered(wizardPage -> this.wizardPages.add(wizardPage));
		this.wizardPages.get(0).setInputData(new OperationData(initialData));
		setHelpAvailable(false);
	}
	
	@Override
	public void addPages() {
		wizardPages.stream().forEachOrdered(page -> addPage(page));
	}

	@Override
	public boolean performFinish() {
		wizardPages.stream().forEachOrdered(page -> {
			page.wizardTerminatingButtonPressed(Dialog.OK);
		});
		return true;
	}

	@Override
	public boolean performCancel() {
		wizardPages.stream().forEachOrdered(page -> {
			page.wizardTerminatingButtonPressed(Dialog.CANCEL);
		});
		return true;
	}

	@Override
	public void saveOutputFile(String filename) throws Exception {
		IPersistenceService service = ServiceHolder.getPersistenceService();
		IPersistentFile pf = service.getPersistentFile(filename);
		
		List<IOperation> operationsList = new ArrayList<>();
		
		// get all operations, for those pages that are instances of AbstractOperationModelWizardPage
		for (IOperationSetupWizardPage page : wizardPages) {
			if (page instanceof AbstractOperationModelWizardPage)
				operationsList.add(((AbstractOperationModelWizardPage) page).getOperation());
		}
		
		pf.setOperations(operationsList.toArray(new IOperation[operationsList.size()]));
		pf.close();
	}

}