package org.dawb.workbench.plotting.system.swtxy;

import org.csstudio.swt.xygraph.figures.Axis;
import org.csstudio.swt.xygraph.figures.XYGraph;
import org.csstudio.swt.xygraph.linearscale.Range;
import org.csstudio.swt.xygraph.linearscale.AbstractScale.LabelSide;
import org.dawb.common.util.text.NumberUtils;
import org.dawb.workbench.plotting.Activator;
import org.dawb.workbench.plotting.preference.PlottingConstants;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.widgets.Display;

/**
 * An axis which can 
 * @author fcp94556
 *
 */
public class AspectAxis extends Axis {

	private AspectAxis relativeTo;
    private boolean    keepAspect; // This is so that the user may have images with and without aspect in the same application.
	public AspectAxis(String title, boolean yAxis) {
		super(title, yAxis);
		keepAspect = Activator.getDefault().getPreferenceStore().getBoolean(PlottingConstants.ASPECT);
	}

	public void setKeepAspectWith(final AspectAxis axis) {
		this.relativeTo = axis;
	}
	
	public void checkBounds() {
		
		Rectangle calcBounds = getBounds().getCopy();
		if (relativeTo == null) return;
		if (!keepAspect)        return;
		
		// We keep aspect if the other axis has a larger range than this axis.
		final double  thisRange     = getInterval(getRange());
		final double  relRange      = getInterval(relativeTo.getRange());
		final boolean equal         = NumberUtils.equalsPercent(thisRange, relRange, 0.001);
		final boolean isOtherReallyLonger = isLonger(calcBounds, getGraph().getPlotArea().getBounds());
		final boolean isRelative    = equal && !isOtherReallyLonger; // The parent layouts ys second so x is the right size.
		final boolean isOtherLarger = relRange>thisRange;
		
		if (isRelative || isOtherLarger) {
			setRelativeAxisBounds(calcBounds, thisRange, relRange);
			setBounds(calcBounds);
		}		
		
		// y correction for companion axis
		if (!isHorizontal() && getTickLablesSide() == LabelSide.Primary) { 
			
			// We have to ensure that our own ticks have been layed out
			// because we use their size to set the location of the
			// relative to field.
			super.layoutTicks();
			
			// Make relativeTo appear near to bottom y axis
			final Rectangle clientArea = getParent().getBounds();
			Dimension yAxisSize = ((IFigure)getChildren().get(1)).getSize();
			final Rectangle relBounds = relativeTo.getBounds().getCopy();
			relBounds.y = clientArea.y + yAxisSize.height - 10;
			relativeTo.setBounds(relBounds);
		}

	}

	private XYGraph getGraph() {
		return (XYGraph)getParent();
	}

	/**
	 * true if with is longer in its direction in pixels than this axis. 
	 * @param aspectAxis
	 * @param relativeTo2
	 * @return
	 */
	private boolean isLonger(Rectangle compare, Rectangle otherBounds) {
		final int len1 = isYAxis() ? compare.height : compare.width;
		final int len2 = relativeTo.isYAxis() ? otherBounds.height : otherBounds.width;
		if (len1==len2) return true;
		return len2>=len1;
	}

	private void setRelativeAxisBounds (final Rectangle origBounds, 
										final double    thisRange, 
										final double    relRange) {
		
		final Rectangle relBounds = relativeTo.getBounds();
		int      realPixels = relativeTo.isYAxis() ? relBounds.height : relBounds.width;
		realPixels-= 2*relativeTo.getMargin();
		
		final double    pixRatio  = realPixels/relRange;   // pix / unit
		int       range     = (int)Math.round(thisRange*pixRatio);    // span for thisRange of them
		range+=2*getMargin();
		
		if (isYAxis()) origBounds.height = range; 
		else           origBounds.width  = range;
		
	}

	/**
	 * Should be a method on Range really but 
	 * @param range
	 * @return
	 */
	private double getInterval(Range range) {
		return Math.max(range.getLower(), range.getUpper()) - Math.min(range.getLower(), range.getUpper());
	}

	public boolean isKeepAspect() {
		return keepAspect;
	}

	public void setKeepAspect(boolean keepAspect) {
		this.keepAspect = keepAspect;
	}
}
