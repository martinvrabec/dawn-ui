/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.dawnsci.spectrum.ui.processing;

import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.Maths;

public class RollingBallBaselineProcess extends AbstractProcess {

	int width = 1;
	
	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	@Override
	protected Dataset process(Dataset x, Dataset y) {
		return rollingBallBaselineCorrection(y, width);
	}

	@Override
	protected String getAppendingName() {

		return "_rolling_baseline_"+width;
	}
	
	private  Dataset rollingBallBaselineCorrection(Dataset y, int width) {

		Dataset t1 = DatasetFactory.zeros(y);
		Dataset t2 = DatasetFactory.zeros(y);

		for (int i = 0 ; i < y.getSize()-1; i++) {
			int start = (i-width) < 0 ? 0 : (i - width);
			int end = (i+width) > (y.getSize()-1) ? (y.getSize()-1) : (i+width);
			double val = y.getSlice(new int[]{start}, new int[]{end}, null).min().doubleValue();
			t1.set(val, i);
		}

		for (int i = 0 ; i < y.getSize()-1; i++) {
			int start = (i-width) < 0 ? 0 : (i - width);
			int end = (i+width) > (y.getSize()-1) ? (y.getSize()-1) : (i+width);
			double val = t1.getSlice(new int[]{start}, new int[]{end}, null).max().doubleValue();
			t2.set(val, i);
		}

		for (int i = 0 ; i < y.getSize()-1; i++) {
			int start = (i-width) < 0 ? 0 : (i - width);
			int end = (i+width) > (y.getSize()-1) ? (y.getSize()-1) : (i+width);
			double val = (Double)t2.getSlice(new int[]{start}, new int[]{end}, null).mean();
			t1.set(val, i);
		}

		return Maths.subtract(y, t1);
	}

}
