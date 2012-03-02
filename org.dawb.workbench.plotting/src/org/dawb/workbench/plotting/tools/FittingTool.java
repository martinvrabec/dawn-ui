package org.dawb.workbench.plotting.tools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.dawb.common.ui.image.IconUtils;
import org.dawb.common.ui.menu.CheckableActionGroup;
import org.dawb.common.ui.menu.MenuAction;
import org.dawb.common.ui.plot.annotation.IAnnotation;
import org.dawb.common.ui.plot.region.IRegion;
import org.dawb.common.ui.plot.region.IRegion.RegionType;
import org.dawb.common.ui.plot.region.IRegionListener;
import org.dawb.common.ui.plot.region.RegionBounds;
import org.dawb.common.ui.plot.region.RegionEvent;
import org.dawb.common.ui.plot.tool.AbstractToolPage;
import org.dawb.common.ui.plot.trace.ILineTrace;
import org.dawb.common.ui.plot.trace.ITrace;
import org.dawb.common.ui.plot.trace.ITraceListener;
import org.dawb.common.ui.plot.trace.TraceEvent;
import org.dawb.workbench.plotting.Activator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.fitting.functions.IPeak;
import uk.ac.diamond.scisoft.analysis.rcp.plotting.IGuiInfoManager;
import uk.ac.diamond.scisoft.analysis.rcp.views.PlotServerConnection;

public class FittingTool extends AbstractToolPage implements IRegionListener {

	private static final Logger logger = LoggerFactory.getLogger(FittingTool.class);
	
	private Composite     composite;
	private TableViewer   viewer;
	private IRegion       fitRegion;
	private Job           fittingJob;
	private FittedPeaks bean;
	
	private ISelectionChangedListener viewUpdateListener;
	private MenuAction tracesMenu;
	private ITraceListener traceListener;
	protected ILineTrace selectedTrace;

	public FittingTool() {
		super();
		this.fittingJob = createFittingJob();
		
		this.traceListener = new ITraceListener.Stub() {
			
			@Override
			public void tracesPlotted(TraceEvent evt) {
				
				updateTracesChoice();
			   
			}
			
			@Override
			public void tracesCleared(TraceEvent evet) {
				tracesMenu.clear();
				getSite().getActionBars().updateActionBars();
			}
		};

	}

	@Override
	public void createControl(Composite parent) {
		
		this.composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new FillLayout());

		viewer = new TableViewer(composite, SWT.FULL_SELECTION | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        createColumns(viewer);
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);
		
		createActions();
				
		getSite().setSelectionProvider(viewer);
		
		this.viewUpdateListener = new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				final StructuredSelection sel = (StructuredSelection)event.getSelection();
				if (bean!=null && sel!=null && sel.getFirstElement()!=null) {
					bean.setSelectedPeak((Integer)sel.getFirstElement());
					viewer.refresh();
				}
			}
		};
		viewer.addSelectionChangedListener(viewUpdateListener);
		
		activate();
	}

	private void createColumns(final TableViewer viewer) {
		
		ColumnViewerToolTipSupport.enableFor(viewer,ToolTip.NO_RECREATE);

        TableViewerColumn var   = new TableViewerColumn(viewer, SWT.LEFT, 0);
		var.getColumn().setText("Name");
		var.getColumn().setWidth(80);
		var.setLabelProvider(new FittingLabelProvider(0));
		
        var   = new TableViewerColumn(viewer, SWT.CENTER, 1);
		var.getColumn().setText("Position");
		var.getColumn().setWidth(100);
		var.setLabelProvider(new FittingLabelProvider(1));

        var   = new TableViewerColumn(viewer, SWT.CENTER, 2);
		var.getColumn().setText("FWHM");
		var.getColumn().setWidth(100);
		var.setLabelProvider(new FittingLabelProvider(2));
		
        var   = new TableViewerColumn(viewer, SWT.CENTER, 3);
		var.getColumn().setText("Area");
		var.getColumn().setWidth(100);
		var.setLabelProvider(new FittingLabelProvider(3));

        var   = new TableViewerColumn(viewer, SWT.CENTER, 4);
		var.getColumn().setText("Type");
		var.getColumn().setWidth(100);
		var.setLabelProvider(new FittingLabelProvider(4));
		
        var   = new TableViewerColumn(viewer, SWT.CENTER, 5);
		var.getColumn().setText("Algorithm");
		var.getColumn().setWidth(100);
		var.setLabelProvider(new FittingLabelProvider(5));

	}
	
	private IContentProvider createActorContentProvider(final int numerOfPeaks) {
		return new IStructuredContentProvider() {
			@Override
			public void dispose() {
			}
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

			@Override
			public Object[] getElements(Object inputElement) {

				if (numerOfPeaks<0) return new Integer[]{0};
				
				List<Integer> indices = new ArrayList<Integer>(numerOfPeaks);
                for (int ipeak = 0; ipeak < numerOfPeaks; ipeak++) {
                	indices.add(ipeak); // autoboxing
				}
				return indices.toArray(new Integer[indices.size()]);
			}
		};
	}
	

	@Override
	public void activate() {
		super.activate();
		if (viewer!=null && viewer.getControl().isDisposed()) return;
		
		if (viewUpdateListener!=null) viewer.addSelectionChangedListener(viewUpdateListener);
		if (this.traceListener!=null) getPlottingSystem().addTraceListener(traceListener);
		updateTracesChoice();
		
		try {
			if (bean!=null) bean.activate();
			getPlottingSystem().addRegionListener(this);
			this.fitRegion = getPlottingSystem().createRegion("Fit selection", IRegion.RegionType.XAXIS);
			fitRegion.setRegionColor(ColorConstants.green);
			
			if (viewer!=null) {
				viewer.refresh();
			}
			
		} catch (Exception e) {
			logger.error("Cannot put the selection into fitting region mode!", e);
		}		
	}
	@Override
	public void deactivate() {
		super.deactivate();
		if (viewer!=null && viewer.getControl().isDisposed()) return;
		
		if (viewUpdateListener!=null) viewer.removeSelectionChangedListener(viewUpdateListener);
		if (this.traceListener!=null) getPlottingSystem().removeTraceListener(traceListener);

		try {
			getPlottingSystem().removeRegionListener(this);
			if (bean!=null) bean.deactivate();
			
		} catch (Exception e) {
			logger.error("Cannot put the selection into fitting region mode!", e);
		}		
	}

	@Override
	public void setFocus() {
        if (viewer!=null && !viewer.getControl().isDisposed()) viewer.getControl().setFocus();
	}
	
	public void dispose() {
		if (getPlottingSystem()!=null) {
			getPlottingSystem().removeRegionListener(this);
		}
		if (viewUpdateListener!=null) viewer.removeSelectionChangedListener(viewUpdateListener);
		if (this.traceListener!=null) getPlottingSystem().removeTraceListener(traceListener);
		viewUpdateListener = null;
		
        if (viewer!=null) viewer.getControl().dispose();
       
        // Using clear and setting to null helps the garbage collector.
        if (bean!=null) bean.dispose();
        bean = null;
        
		super.dispose();
	}


	@Override
	public Control getControl() {
		return composite;
	}

	@Override
	public void regionCreated(RegionEvent evt) {
		
		
	}

	@Override
	public void regionAdded(RegionEvent evt) {
		if (evt==null || evt.getRegion()==null) {
			getPlottingSystem().clearRegions();
			return;
		}
		if (evt.getRegion()==fitRegion) {
			fittingJob.schedule();
		}
	}

	@Override
	public void regionRemoved(RegionEvent evt) {
		
		
	}

	public Job createFittingJob() {
		
		final Job fit = new Job("Fit peaks") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				if (composite==null)        return Status.CANCEL_STATUS;
				if (composite.isDisposed()) return Status.CANCEL_STATUS;
				
				
				final RegionBounds bounds = fitRegion.getRegionBounds();
				if (fitRegion==null || bounds==null) return Status.CANCEL_STATUS;
				
				getPlottingSystem().removeRegionListener(FittingTool.this);
				
				composite.getDisplay().syncExec(new Runnable() {
					public void run() {
						getPlottingSystem().removeRegion(fitRegion);
						if (bean!=null) bean.removeSelections(getPlottingSystem());
				    }
				});
				
				
				// We chop x and y by the region bounds. We assume the
				// plot is an XAXIS selection therefore the indices in
				// y = indices chosen in x.
				final double[] p1 = bounds.getP1();
				final double[] p2 = bounds.getP2();
				
				// We peak fit only the first of the data sets plotted for now.
				AbstractDataset x  = selectedTrace.getXData();
				AbstractDataset y  = selectedTrace.getYData();
				
				AbstractDataset[] a= FittingUtils.xintersection(x,y,p1[0],p2[0]);
				x = a[0]; y=a[1];
				
				final FittedPeaks bean = FittingUtils.getFittedPeaks(x, y, monitor);
				createFittedPeaks(bean);
				
				return Status.OK_STATUS;
			}
		};
		
		fit.setSystem(true);
		fit.setUser(true);
		fit.setPriority(Job.INTERACTIVE);
		return fit;
	}

	/**
	 * Thread safe
	 * @param peaks
	 */
	protected synchronized void createFittedPeaks(final FittedPeaks newBean) {
		
		if (newBean==null) {
			bean = null;
			logger.error("Cannot find peaks in the given selection.");
			return;
		}
		composite.getDisplay().syncExec(new Runnable() {
			
		    public void run() {
		    	try {
		    		
		    		boolean requireFWHMSelections = Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_FWHM_SELECTIONS);
		    		boolean requirePeakSelections = Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_PEAK_SELECTIONS);
					int ipeak = 1;
					// Draw the regions
					for (RegionBounds rb : newBean.getPeakBounds()) {
						
						final IRegion area = getPlottingSystem().createRegion("Peak Area "+ipeak, RegionType.XAXIS);
						area.setRegionColor(ColorConstants.orange);
						area.setRegionBounds(rb);
						area.setMotile(false);
						getPlottingSystem().addRegion(area);
						newBean.addAreaRegion(area);
						if (!requireFWHMSelections) area.setVisible(false);
						
						final IRegion line = getPlottingSystem().createRegion("Peak Line "+ipeak, RegionType.XAXIS_LINE);
						line.setRegionBounds(new RegionBounds(rb.getCentre(), rb.getCentre()));
						line.setRegionColor(ColorConstants.black);
						line.setMotile(false);
						line.setAlpha(150);
						getPlottingSystem().addRegion(line);
						newBean.addLineRegion(line);
						if (!requirePeakSelections) area.setVisible(false);

					    ++ipeak;
					}

					ipeak = 1;
		    		boolean requireTrace = Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_FITTING_TRACE);
					// Create some traces for the fitted function
					for (AbstractDataset[] pair : newBean.getFunctionData()) {
						
						final ILineTrace trace = getPlottingSystem().createLineTrace("Peak "+ipeak);
						trace.setData(pair[0], pair[1]);
						trace.setLineWidth(1);
						trace.setTraceColor(ColorConstants.black);
						getPlottingSystem().addTrace(trace);
						newBean.addTrace(trace);
						if (!requireTrace) trace.setVisible(false);
						
					    ++ipeak;
  				    }
					
		    		boolean requireAnnot = Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_ANNOTATION_AT_PEAK);
                    final List<? extends IPeak> peaks = newBean.getPeaks();
                    ipeak = 1;
                    for (IPeak peak : peaks) {
					
                    	final IAnnotation ann = getPlottingSystem().createAnnotation("Peak "+ipeak);
                    	ann.setLocation(peak.getPosition(), peak.val(peak.getPosition()));
                    	
                    	getPlottingSystem().addAnnotation(ann);
                    	
                    	newBean.addAnnotation(ann);
                    	if (!requireAnnot) ann.setVisible(false);
                    	
                    	++ipeak;
					}
		    		
					
					FittingTool.this.bean = newBean;
					viewer.setContentProvider(createActorContentProvider(newBean.size()));
					viewer.setInput(newBean);
                    viewer.refresh();
                    
                    updatePlotServerConnection(newBean);
                    
		    	} catch (Exception ne) {
		    		logger.error("Cannot create fitted peaks!", ne);
		    	}
		    } 
		});
	}

	private IGuiInfoManager plotServerConnection;
	
	protected void updatePlotServerConnection(FittedPeaks bean) {
		
		if (bean==null) return;
		
		if (plotServerConnection==null && getPart() instanceof IEditorPart) {
			this.plotServerConnection = new PlotServerConnection(((IEditorPart)getPart()).getEditorInput().getName());
		}
		
		if (plotServerConnection!=null) {
			final Serializable peaks = (Serializable)bean.getPeaks();
			if (peaks!=null && !bean.isEmpty()) {
				
				// For some reason this causes npes if you create more than one for a given file.
				
				//plotServerConnection.putGUIInfo(GuiParameters.FITTEDPEAKS, peaks);
			}
		}
	}
	

	protected void updateTracesChoice() {
		
		if (tracesMenu==null) return;
		
		tracesMenu.clear();
		
		final Collection<ITrace> traces = getPlottingSystem().getTraces();
		if (traces==null || traces.size()<0) return;
		if (bean!=null) traces.removeAll(bean.getPeakTraces());
		
		final CheckableActionGroup group = new CheckableActionGroup();
		FittingTool.this.selectedTrace = null;
		for (final ITrace iTrace : traces) {
			
			if (FittingTool.this.selectedTrace==null && iTrace instanceof ILineTrace) {
				FittingTool.this.selectedTrace = (ILineTrace)iTrace;
			}
			
			final Action action = new Action(iTrace.getName(), IAction.AS_CHECK_BOX) {
				public void run() {
					if (iTrace instanceof ILineTrace) FittingTool.this.selectedTrace = (ILineTrace)iTrace;
					tracesMenu.setSelectedAction(this);
					if (fittingJob!=null&&isActive()) fittingJob.schedule();
					setChecked(true);
				}
			};
			tracesMenu.add(action);
			group.add(action);
		}
		
		tracesMenu.setSelectedAction(0);
		tracesMenu.getAction(0).setChecked(true);
				
		getSite().getActionBars().updateActionBars();
	}

	
	/**
	 * We use the old actions here for simplicity of configuration.
	 * 
	 * TODO consider moving to commands.
	 */
	private void createActions() {
		
		
		final Action showAnns = new Action("Show annotations at the peak position.", IAction.AS_CHECK_BOX) {
			public void run() {
				final boolean isChecked = isChecked();
				Activator.getDefault().getPreferenceStore().setValue(FittingConstants.SHOW_ANNOTATION_AT_PEAK, isChecked);
				if (bean!=null) bean.setAnnotationsVisible(isChecked);
			}
		};
		showAnns.setImageDescriptor(Activator.getImageDescriptor("icons/plot-tool-peak-fit-showAnnotation.png"));
		getSite().getActionBars().getToolBarManager().add(showAnns);
		
		showAnns.setChecked(Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_ANNOTATION_AT_PEAK));

		final Action showTrace = new Action("Show fitting traces.", IAction.AS_CHECK_BOX) {
			public void run() {
				final boolean isChecked = isChecked();
				Activator.getDefault().getPreferenceStore().setValue(FittingConstants.SHOW_FITTING_TRACE, isChecked);
				if (bean!=null) bean.setTracesVisible(isChecked);
			}
		};
		showTrace.setImageDescriptor(Activator.getImageDescriptor("icons/plot-tool-peak-fit-showFittingTrace.png"));
		getSite().getActionBars().getToolBarManager().add(showTrace);

		showTrace.setChecked(Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_FITTING_TRACE));

		
		final Action showPeak = new Action("Show peak lines.", IAction.AS_CHECK_BOX) {
			public void run() {
				final boolean isChecked = isChecked();
				Activator.getDefault().getPreferenceStore().setValue(FittingConstants.SHOW_PEAK_SELECTIONS, isChecked);
				if (bean!=null) bean.setPeaksVisible(isChecked);
			}
		};
		showPeak.setImageDescriptor(Activator.getImageDescriptor("icons/plot-tool-peak-fit-showPeakLine.png"));
		getSite().getActionBars().getToolBarManager().add(showPeak);
		
		showPeak.setChecked(Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_PEAK_SELECTIONS));

		final Action showFWHM = new Action("Show selection regions for full width, half max.", IAction.AS_CHECK_BOX) {
			public void run() {
				final boolean isChecked = isChecked();
				Activator.getDefault().getPreferenceStore().setValue(FittingConstants.SHOW_FWHM_SELECTIONS, isChecked);
				if (bean!=null) bean.setAreasVisible(isChecked);
			}
		};
		showFWHM.setImageDescriptor(Activator.getImageDescriptor("icons/plot-tool-peak-fit-showFWHM.png"));
		getSite().getActionBars().getToolBarManager().add(showFWHM);
		
		showFWHM.setChecked(Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_FWHM_SELECTIONS));
		
		final Separator sep = new Separator(getClass().getName()+".separator1");	
		getSite().getActionBars().getToolBarManager().add(sep);
		

		final MenuAction  peakType = new MenuAction("Peak type to fit");
		peakType.setToolTipText("Peak type to fit");
		peakType.setImageDescriptor(Activator.getImageDescriptor("icons/plot-tool-peak-fit-peak-type.png"));
		
		CheckableActionGroup group = new CheckableActionGroup();
		
		Action selectedPeakAction = null;
		for (final IPeak peak : FittingUtils.getPeakOptions().values()) {
			
			final Action action = new Action(peak.getClass().getSimpleName(), IAction.AS_CHECK_BOX) {
				public void run() {
					Activator.getDefault().getPreferenceStore().setValue(FittingConstants.PEAK_TYPE, peak.getClass().getName());
					setChecked(true);
					if (fittingJob!=null&&isActive()) fittingJob.schedule();
					peakType.setSelectedAction(this);
				}
			};
			peakType.add(action);
			group.add(action);
			if (peak.getClass().getName().equals(Activator.getDefault().getPreferenceStore().getString(FittingConstants.PEAK_TYPE))) {
				selectedPeakAction = action;
			}
		}
		
		if (selectedPeakAction!=null) {
			peakType.setSelectedAction(selectedPeakAction);
			selectedPeakAction.setChecked(true);
		}
		getSite().getActionBars().getToolBarManager().add(peakType);
		getSite().getActionBars().getMenuManager().add(peakType);

		
		final Separator sep2 = new Separator(getClass().getName()+".separator2");	
		getSite().getActionBars().getToolBarManager().add(sep2);

		this.tracesMenu = new MenuAction("Traces");
		tracesMenu.setToolTipText("Choice of trace to fit on.");
		tracesMenu.setImageDescriptor(Activator.getImageDescriptor("icons/plot-tool-trace-choice.png"));
		
		getSite().getActionBars().getToolBarManager().add(tracesMenu);
		getSite().getActionBars().getMenuManager().add(tracesMenu);
				
		final MenuAction numberPeaks = new MenuAction("Number peaks to fit");
		numberPeaks.setToolTipText("Number peaks to fit");
				
		group = new CheckableActionGroup();
		
		final int npeak = Activator.getDefault().getPreferenceStore().getDefaultInt(FittingConstants.PEAK_NUMBER_CHOICES);
		for (int ipeak = 1; ipeak <= npeak; ipeak++) {
			
			final int peak = ipeak;
			final Action action = new Action("Fit "+String.valueOf(ipeak)+" Peaks", IAction.AS_CHECK_BOX) {
				public void run() {
					Activator.getDefault().getPreferenceStore().setValue(FittingConstants.PEAK_NUMBER, peak);
					numberPeaks.setSelectedAction(this);
					setChecked(true);
					if (isActive()) fittingJob.schedule();
				}
			};
			
			action.setImageDescriptor(IconUtils.createIconDescriptor(String.valueOf(ipeak)));
			numberPeaks.add(action);
			group.add(action);
			action.setChecked(false);
			action.setToolTipText("Fit "+ipeak+" peak(s)");
			
		}

		final int ipeak = Activator.getDefault().getPreferenceStore().getInt(FittingConstants.PEAK_NUMBER);
		numberPeaks.setSelectedAction(ipeak-1);
		numberPeaks.setCheckedAction(ipeak-1, true);
		
		getSite().getActionBars().getToolBarManager().add(numberPeaks);
		getSite().getActionBars().getMenuManager().add(numberPeaks);
		
		
		final Action clear = new Action("Clear", Activator.getImageDescriptor("icons/plot-tool-peak-fit-clear.png")) {
			public void run() {
				if (!isActive()) return;
				if (bean!=null) bean.removeSelections(getPlottingSystem());
				bean.dispose();
				bean = null;
				viewer.setContentProvider(createActorContentProvider(0));
				viewer.setInput(null);
			}
		};
		clear.setToolTipText("Clear all regions found in the fitting");
		
		getSite().getActionBars().getToolBarManager().add(clear);
		getSite().getActionBars().getMenuManager().add(clear);
		
		createRightClickMenu();
	}
	
	private void createRightClickMenu() {	
	    final MenuManager menuManager = new MenuManager();
	    for (IContributionItem item : getSite().getActionBars().getMenuManager().getItems()) menuManager.add(item);
	    viewer.getControl().setMenu(menuManager.createContextMenu(viewer.getControl()));
	}

}
