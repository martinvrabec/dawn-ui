package org.dawnsci.plotting.tools.fitting;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import org.dawnsci.plotting.Activator;
import org.dawnsci.plotting.api.IPlottingSystem;
import org.dawnsci.plotting.api.annotation.AnnotationUtils;
import org.dawnsci.plotting.api.annotation.IAnnotation;
import org.dawnsci.plotting.api.region.IRegion;
import org.dawnsci.plotting.api.region.RegionUtils;
import org.dawnsci.plotting.api.trace.ITrace;
import org.dawnsci.plotting.api.trace.TraceUtils;
import org.dawnsci.plotting.preference.FittingConstants;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.fitting.functions.CompositeFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.IPeak;
import uk.ac.diamond.scisoft.analysis.roi.ROIBase;
import uk.ac.diamond.scisoft.analysis.roi.RectangularROI;

import javax.vecmath.Vector3d;

public class FittedFunction  {

	private RectangularROI    roi;
	private CompositeFunction function;
	private boolean           saved=false;
	private String            peakName;
	private AbstractDataset   x,y;
	private AbstractDataset[] peakFunctions;
	
	private IRegion           fwhm;
	private IRegion           center;
	private ITrace            dataTrace;
	private ITrace            trace;
	private IAnnotation       annotation;
	
	private Vector3d q;

	public AbstractDataset getX() {
		return x;
	}

	public void setX(AbstractDataset x) {
		this.x = x;
	}

	public void setQ(Vector3d q) {
		this.q=q;
	}
	
	public Vector3d getQ() {
		return this.q;
	}

	public boolean isSaved() {
		return saved;
	}

	public void setSaved(IPlottingSystem sys, boolean saved, String... usedNames) throws Exception {
		this.saved = saved;
		
		
		sys.renameRegion(fwhm, RegionUtils.getUniqueName("Saved Area", sys, usedNames));
		sys.renameRegion(center, RegionUtils.getUniqueName("Saved Line", sys, usedNames));	
	    sys.renameTrace(trace, TraceUtils.getUniqueTrace("Saved Peak", sys, usedNames));
	    sys.renameAnnotation(annotation, AnnotationUtils.getUniqueAnnotation("Saved Peak", sys, usedNames));
	    peakName = trace.getName();
	}

	public FittedFunction(CompositeFunction peak, RectangularROI bounds, AbstractDataset[] pf) {
		this.function = peak;
		this.roi  = bounds;
		this.peakFunctions = pf;
	}

	public AbstractDataset[] getPeakFunctions() {
		return peakFunctions;
	}

	public void setPeakFunctions(AbstractDataset[] peakFunctions) {
		this.peakFunctions = peakFunctions;
	}

	public void dispose() {
		roi    = null;
		function   = null;
		fwhm   = null;
		center = null;
		trace  = null;
		annotation = null;		
	}
	
	public FittedFunction clone() {
		FittedFunction ret = new FittedFunction(function, roi, peakFunctions);
		ret.roi        = roi;
		ret.function   = function;
		ret.fwhm       = fwhm;
		ret.center     = center;
		ret.trace      = trace;
		ret.dataTrace  = dataTrace;
		ret.annotation = annotation;
		ret.peakName   = peakName;
		ret.saved      = saved;
		ret.y          = y;
		return ret;
	}

	public AbstractDataset getY() {
		return y;
	}

	public void setY(AbstractDataset y) {
		this.y = y;
	}

	public void activate() {
		fwhm.setVisible(Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_FWHM_SELECTIONS));
		center.setVisible(Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_PEAK_SELECTIONS));
		trace.setVisible(Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_FITTING_TRACE));
		annotation.setVisible(Activator.getDefault().getPreferenceStore().getBoolean(FittingConstants.SHOW_ANNOTATION_AT_PEAK));
	}
	
	public void deactivate() {
		fwhm.setVisible(false);
		center.setVisible(false);
		trace.setVisible(false);
		annotation.setVisible(false);
	}
	
	
	public String getPeakName() {
		try {
		    return peakName;
		} catch (IndexOutOfBoundsException ne) {
			return null;
		}
	}

	public String getPositionName() {
		return getPeakName().replace(' ', '_')+"_position";
	}

	public double getPosition() {
		try {
			return getPeak().getPosition();
		} catch (IndexOutOfBoundsException ne) {
			return Double.NaN;
		}
	}
	public double getPeakValue() {
		return function.val(getPosition());
	}
	
	public double getFWHM() {
		try {
			return getPeak().getFWHM();
		} catch (IndexOutOfBoundsException ne) {
			return Double.NaN;
		}
	}
	
	public double getArea() {
		try {
			return getPeak().getArea();
		} catch (IndexOutOfBoundsException ne) {
			return Double.NaN;
		}
	}	
	
	public String getPeakType() {
		try {
		    return getPeak().getClass().getSimpleName();
		} catch (IndexOutOfBoundsException ne) {
			return null;
		}
	}

	public RectangularROI getRoi() {
		return roi;
	}
	public void setRoi(RectangularROI roi) {
		this.roi = roi;
	}
	/**
	 * Might throw cast exceptions if function
	 * is not IPeak.
	 * @return
	 */
	public IPeak getPeak() {
		return function.getPeak(0);
	}
	public IRegion getFwhm() {
		return fwhm;
	}
	public CompositeFunction getFunction() {
		return function;
	}
	public void setFwhm(IRegion fwhm) {
		this.fwhm = fwhm;
	}
	public IRegion getCenter() {
		return center;
	}
	public void setCenter(IRegion center) {
		this.center = center;
	}
	public ITrace getTrace() {
		return trace;
	}
	public void setTrace(ITrace trace) {
		this.peakName= trace.getName();
		this.trace = trace;
	}
	public IAnnotation getAnnotation() {
		return annotation;
	}
	public void setAnnotation(IAnnotation annotation) {
		this.annotation = annotation;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((annotation == null) ? 0 : annotation.hashCode());
		result = prime * result + ((center == null) ? 0 : center.hashCode());
		result = prime * result
				+ ((function == null) ? 0 : function.hashCode());
		result = prime * result + ((fwhm == null) ? 0 : fwhm.hashCode());
		result = prime * result + Arrays.hashCode(peakFunctions);
		result = prime * result
				+ ((peakName == null) ? 0 : peakName.hashCode());
		result = prime * result + ((roi == null) ? 0 : roi.hashCode());
		result = prime * result + (saved ? 1231 : 1237);
		result = prime * result + ((trace == null) ? 0 : trace.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FittedFunction other = (FittedFunction) obj;
		if (annotation == null) {
			if (other.annotation != null)
				return false;
		} else if (!annotation.equals(other.annotation))
			return false;
		if (center == null) {
			if (other.center != null)
				return false;
		} else if (!center.equals(other.center))
			return false;
		if (function == null) {
			if (other.function != null)
				return false;
		} else if (!function.equals(other.function))
			return false;
		if (fwhm == null) {
			if (other.fwhm != null)
				return false;
		} else if (!fwhm.equals(other.fwhm))
			return false;
		if (!Arrays.equals(peakFunctions, other.peakFunctions))
			return false;
		if (peakName == null) {
			if (other.peakName != null)
				return false;
		} else if (!peakName.equals(other.peakName))
			return false;
		if (roi == null) {
			if (other.roi != null)
				return false;
		} else if (!roi.equals(other.roi))
			return false;
		if (saved != other.saved)
			return false;
		if (trace == null) {
			if (other.trace != null)
				return false;
		} else if (!trace.equals(other.trace))
			return false;
		return true;
	}

	public void setFWHMVisible(boolean isVis) {
		fwhm.setVisible(isVis);
	}
	public void setCenterVisible(boolean isVis) {
		center.setVisible(isVis);
	}
	public void setTraceVisible(boolean isVis) {
		trace.setVisible(isVis);
	}
	public void setAnnotationVisible(boolean isVis) {
		annotation.setVisible(isVis);
	}

	public void delete(IPlottingSystem sys) {
		sys.removeRegion(fwhm);
		sys.removeTrace(trace);
		sys.removeRegion(center);
		sys.removeAnnotation(annotation);		
	}

	public void getUsedNames(List<String> names) {
		names.add(fwhm.getName());
		names.add(center.getName());
		names.add(trace.getName());
		names.add(annotation.getName());
	}

	public String getTabString() {
		
		DecimalFormat format = new DecimalFormat("##0.#####E0");
		final StringBuilder buf = new StringBuilder();
		//buf.append(getPeakName().replace(' ', '_'));
		//buf.append("\t");
		buf.append(format.format(getPosition()));
		buf.append("\t");
		buf.append(format.format(getPeakValue()));
		buf.append("\t");
		buf.append(format.format(getFWHM()));
		buf.append("\t");
		buf.append(format.format(getArea()));
		//buf.append("\t");
		//buf.append(getPeakType().replace(' ', '_'));
		return buf.toString();
	}

	public static String getCVSTitle() {
		final StringBuilder buf = new StringBuilder();
		//buf.append("Peak_Name\t");
		buf.append("# Position\t");
		buf.append("Fit\t");
		buf.append("FWHM\t");
		buf.append("Area\t");
		//buf.append("Peak_Type\t");
		return buf.toString();
	}

	public double getDataValue() {
		if (x==null || y==null) return Double.NaN;
		try {
		    final double xValue = getPosition();
		    List<Double>  cross = DatasetUtils.crossings(x, xValue);
		    final int index     = (int)Math.round(cross.get(0));
		    return y.getDouble(index);
		} catch (Throwable ne) {
			return Double.NaN;
		}
	}

	public ITrace getDataTrace() {
		return dataTrace;
	}

	public void setDataTrace(ITrace dataTrace) {
		this.dataTrace = dataTrace;
	}

	public List<ROIBase> getRegions() {
		if (fwhm!=null && center!=null) {
			return Arrays.asList(fwhm.getROI(), center.getROI());
		}
		return null;
	}
	
}
