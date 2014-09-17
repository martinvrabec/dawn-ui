package org.dawnsci.plotting.tools.profile;

import java.util.Arrays;

import org.dawb.common.ui.util.GridUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IActionBars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.Dataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.roi.IROI;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;

public class SpotTool extends ZoomTool {
	
	private static Logger logger = LoggerFactory.getLogger(SpotTool.class);

	private IPlottingSystem topSystem;
	private IPlottingSystem rightSystem;
	
	public SpotTool() {
		
		super();
		
		try {
			topSystem = PlottingFactory.createPlottingSystem();
			rightSystem = PlottingFactory.createPlottingSystem();
		} catch (Exception e) {
			logger.error("Cannot create plotting systems!", e);
		}
	}

	private Composite content;
	
	public void createControl(Composite parent, IActionBars actionbars) {
		
		this.content = new Composite(parent, SWT.NONE);
		content.setLayout(new GridLayout(1, false));
		GridUtils.removeMargins(content);
		content.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		// Make some sashes
		final SashForm horiz  = new SashForm(content, SWT.VERTICAL);
		horiz.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		horiz.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));		
		final SashForm top    = new SashForm(horiz, SWT.HORIZONTAL);
		final SashForm bottom = new SashForm(horiz, SWT.HORIZONTAL);
		
		// Fill the sashes
		topSystem.createPlotPart(top, "Integration", null, PlotType.XY, getPart());
		Label label = new Label(top, SWT.NONE);
		label.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

		super.createControl(bottom, actionbars);
		rightSystem.createPlotPart(bottom, "Integration", null, PlotType.XY, getPart());		
		
		horiz.setWeights(new int[]{30,70});
		top.setWeights(new int[]{70,30});
		bottom.setWeights(new int[]{70,30});

		topSystem.setShowLegend(false);
		rightSystem.setShowLegend(false);
		profilePlottingSystem.setShowIntensity(false);
	}
	
	public Control getControl() {
		return content;
	}

	@Override
	protected String getRegionName() {
		return "Spot";
	}
	
	public void dispose() {
		super.dispose();
		topSystem.dispose();
		rightSystem.dispose();
	}

	@Override
	protected void createProfile(final IImageTrace  image, 
					            IRegion      region,
					            IROI         rbs, 
					            boolean      tryUpdate, 
					            boolean      isDrag,
					            IProgressMonitor monitor) {
		
		if (monitor.isCanceled()) return;
		if (image==null) return;
		
		if ((region.getRegionType()!=RegionType.BOX)&&(region.getRegionType()!=RegionType.PERIMETERBOX)) return;

		Dataset slice = createZoom(image, region, rbs, tryUpdate, isDrag, monitor);
		
        Dataset yData = slice.sum(0);
        Dataset xData = slice.sum(1);
		
		final RectangularROI bounds = (RectangularROI) (rbs==null ? region.getROI() : rbs);
 		final Dataset y_indices   = AbstractDataset.arange(bounds.getPoint()[0], bounds.getPoint()[0]+bounds.getLength(0), 1, Dataset.FLOAT);
		y_indices.setName("Y Location");
		
		yData.setName("Intensity");
		topSystem.updatePlot1D(y_indices, Arrays.asList(new IDataset[]{yData}), monitor);
		topSystem.repaint();

		final Dataset x_indices   = AbstractDataset.arange(bounds.getPoint()[1]+bounds.getLength(1), bounds.getPoint()[1], -1, Dataset.FLOAT);
		x_indices.setName("Y Location");
	
		x_indices.setName("Indices in Y");
		rightSystem.updatePlot1D(xData, Arrays.asList(new IDataset[]{x_indices}), monitor);
		rightSystem.repaint();		

		Display.getDefault().syncExec(new Runnable() {
			
			@Override
			public void run() {
				topSystem.setTitle("");
				rightSystem.setTitle("");
			}
		});
	}

}
