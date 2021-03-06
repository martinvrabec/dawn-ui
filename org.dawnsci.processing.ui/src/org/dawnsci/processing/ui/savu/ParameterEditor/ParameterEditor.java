package org.dawnsci.processing.ui.savu.ParameterEditor;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

public class ParameterEditor extends Composite {

	private TableViewer tableViewer;
	public String pluginName=null;
	public String pluginPath;
	public Integer pluginRank;
	public Integer width = 1000;
	public Integer height = 600;
	
	public Integer getWidth() {
		return width;
	}


	public void setWidth(Integer width) {
		this.width = width;
	}


	public Integer getHeight() {
		return height;
	}


	public void setHeight(Integer height) {
		this.height = height;
	}


	public String getPluginName() {
		return pluginName;
	}


	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}


	public String getPluginPath() {
		return pluginPath;
	}


	public void setPluginPath(String pluginPath) {
		this.pluginPath = pluginPath;
	}


	public Integer getPluginRank() {
		return pluginRank;
	}


	public void setPluginRank(Integer pluginRank) {
		this.pluginRank = pluginRank;
	}



	public ParameterEditor(Composite parent, int style) {
		super(parent,style);
		setLayout(new GridLayout(1, true));

	}


	public void initialiseTable(ParameterEditorTableViewModel viewModel) {
		buildAndLayoutTable( viewModel);
		update(viewModel);
	}
	
	
	public void setpluginName(String pluginName) {
		this.pluginName = pluginName;
	}

	public String getpluginName() {
		return this.pluginName;
	}
	
	private void buildAndLayoutTable(ParameterEditorTableViewModel viewModel) {

		final Table table = new Table(this, SWT.FULL_SELECTION | SWT.SINGLE | SWT.V_SCROLL | SWT.BORDER);
		
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setSize(getWidth(), getHeight());
		table.setLayoutData(layoutData);
		
		
		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		TableViewerColumn viewerColumn = new TableViewerColumn(tableViewer, SWT.NONE, 0);
		viewerColumn.getColumn().setText("Name");
		viewerColumn.getColumn().setWidth(100);
		viewerColumn.setLabelProvider(new ParameterEditorLabelProvider(viewModel, 0));
		ParameterEditingSupport regionEditor = new ParameterEditingSupport(viewModel, tableViewer, 0);
		viewerColumn.setEditingSupport(regionEditor);
		
		TableViewerColumn viewerColumn1 = new TableViewerColumn(tableViewer, SWT.NONE, 1);
		viewerColumn1.getColumn().setText("Value");
		viewerColumn1.getColumn().setWidth(400);
		viewerColumn1.setLabelProvider(new ParameterEditorLabelProvider(viewModel, 1));
		ParameterEditingSupport regionEditor1 = new ParameterEditingSupport(viewModel,tableViewer, 1);
		viewerColumn1.setEditingSupport(regionEditor1);

		TableViewerColumn viewerColumn2 = new TableViewerColumn(tableViewer, SWT.NONE, 2);
		viewerColumn2.getColumn().setText("Description");
		viewerColumn2.getColumn().setWidth(400);
		viewerColumn2.setLabelProvider(new ParameterEditorLabelProvider(viewModel, 2));
		ParameterEditingSupport regionEditor2 = new ParameterEditingSupport(viewModel,tableViewer, 2);
		viewerColumn2.setEditingSupport(regionEditor2);	

	}

	public void update(ParameterEditorTableViewModel model) {
		tableViewer.setInput(null);
		tableViewer.setInput(model.getValues());
		tableViewer.getTable().pack();
	}

	@Override
	protected void checkSubclass() {
		// Disable the check that prevents subclassing of SWT components
	}

}
