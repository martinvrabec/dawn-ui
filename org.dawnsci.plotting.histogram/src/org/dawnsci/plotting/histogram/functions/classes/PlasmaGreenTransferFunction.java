package org.dawnsci.plotting.histogram.functions.classes;

import org.dawnsci.plotting.histogram.data.HistogramData;
import org.dawnsci.plotting.histogram.data.HistogramData.RGBChannel;

public class PlasmaGreenTransferFunction extends AbstractTransferFunction {

	@Override
	public double getPoint(double value) {
		return HistogramData.getPointFromRGBData(value, HistogramData.PLASMA, RGBChannel.GREEN);
	}

}
