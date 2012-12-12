package org.dawb.workbench.plotting.system.swtxy;

import org.csstudio.swt.xygraph.dataprovider.IDataProvider;
import org.csstudio.swt.xygraph.figures.Axis;
import org.csstudio.swt.xygraph.figures.Trace;
import org.dawb.common.ui.plot.trace.ITrace;
import org.dawb.common.ui.plot.trace.ITraceContainer;

/**
 * Trace with drawPolyline(...) for faster rendering.
 * 
 * @author fcp94556
 *
 */
public class LineTrace extends Trace implements ITraceContainer {
	
	protected String internalName; 
	
	public LineTrace(String name, Axis xAxis, Axis yAxis, IDataProvider dataProvider) {
		super(name, xAxis, yAxis, dataProvider);
	}
	

	public void dispose() {
		removeAll();
		getHotSampleList().clear();
		name=null;
		internalName=null;
		traceDataProvider=null;
		xAxis=null;	
		yAxis=null;	
		traceColor=null;
		traceType=null;
		baseLine=null;
		pointStyle=null;
		yErrorBarType=null;
		xErrorBarType=null;
		errorBarColor=null;
		xyGraph=null;
	}

	public boolean isDisposed() {
		return xyGraph==null;
	}


	public String getInternalName() {
		if (internalName!=null) return internalName;
		return getName();
	}


	public void setInternalName(String internalName) {
		this.internalName = internalName;
	}

	private ITrace trace;

	@Override
	public ITrace getTrace() {
		return trace;
	}


	@Override
	public void setTrace(ITrace trace) {
		this.trace = trace;
	}

}
