/*
 * Copyright (c) 2012 European Synchrotron Radiation Facility,
 *                    Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.dawb.passerelle.actors.dawn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.direct.CMAESOptimizer;
import org.dawb.passerelle.common.actors.AbstractDataMessageTransformer;
import org.dawb.passerelle.common.message.DataMessageComponent;
import org.dawb.passerelle.common.message.DataMessageException;
import org.dawb.passerelle.common.message.MessageUtils;

import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import uk.ac.diamond.scisoft.analysis.SDAPlotter;
import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.DatasetUtils;
import uk.ac.diamond.scisoft.analysis.dataset.DoubleDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IndexIterator;
import uk.ac.diamond.scisoft.analysis.dataset.Maths;
import uk.ac.diamond.scisoft.analysis.dataset.Slice;
import uk.ac.diamond.scisoft.analysis.fitting.functions.AFunction;
import uk.ac.diamond.scisoft.analysis.fitting.functions.FermiGauss;
import uk.ac.diamond.scisoft.analysis.fitting.functions.IParameter;

public class FitGaussianConvolutedFermiActor extends
		AbstractDataMessageTransformer {

	private static final long serialVersionUID = 813882139346261410L;
	public StringParameter datasetName;
	public StringParameter functionName;
	public StringParameter xAxisName;
	public StringParameter fitDirection;
	public StringParameter fitConvolution;
	public StringParameter updatePlotName;
	public StringParameter quickConvolutionWidth;

	public FitGaussianConvolutedFermiActor(CompositeEntity container,
			String name) throws NameDuplicationException,
			IllegalActionException {
		super(container, name);

		datasetName = new StringParameter(this, "datasetName");
		registerConfigurableParameter(datasetName);
		functionName = new StringParameter(this, "functionName");
		registerConfigurableParameter(functionName);
		xAxisName = new StringParameter(this, "xAxisName");
		registerConfigurableParameter(xAxisName);
		fitDirection = new StringParameter(this, "fitDirection");
		registerConfigurableParameter(fitDirection);
		fitConvolution = new StringParameter(this, "fitConvolution");
		fitConvolution.addChoice("Off");
		fitConvolution.addChoice("Quick");
		fitConvolution.addChoice("Full");
		registerConfigurableParameter(fitConvolution);
		updatePlotName = new StringParameter(this, "updatePlotName");
		registerConfigurableParameter(updatePlotName);
		quickConvolutionWidth = new StringParameter(this,
				"quickConvolutionWidth");
		registerConfigurableParameter(quickConvolutionWidth);
	}

	private AFunction FitGaussianConvFermi(final AbstractDataset xAxis,
			final AbstractDataset values, final AFunction fitFunction) {

		if (!(fitFunction instanceof FermiGauss)) {
			throw new IllegalArgumentException(
					"Input function must be of type FermiGauss");
		}

		final double kT = fitFunction.getParameterValue(1);

		// first fit using a simple fermi edge to get the initial parameters
		MultivariateFunction f = new MultivariateFunction() {

			@Override
			public double value(double[] p) {

				double p0 = deNormalizeParameter(p[0],
						fitFunction.getParameter(0));
				double p1 = deNormalizeParameter(p[1],
						fitFunction.getParameter(2));
				double p2 = deNormalizeParameter(p[2],
						fitFunction.getParameter(3));
				double p3 = deNormalizeParameter(p[3],
						fitFunction.getParameter(4));

				FermiGauss fg = new FermiGauss(p0, kT, p1, p2, p3, 0.0);

				AbstractDataset fermiDS = fg.getFermiDS(xAxis);

				AbstractDataset residual = Maths.subtract(fermiDS, values);
				residual.ipower(2);

				return (Double) residual.sum();
			}
		};

		// preform the optimisation
		double[] start = new double[] {
				normalizeParameter(fitFunction.getParameterValue(0),
						fitFunction.getParameter(0)),
				normalizeParameter(fitFunction.getParameterValue(2),
						fitFunction.getParameter(2)),
				normalizeParameter(fitFunction.getParameterValue(3),
						fitFunction.getParameter(3)),
				normalizeParameter(fitFunction.getParameterValue(4),
						fitFunction.getParameter(4)) };

		CMAESOptimizer cOpt = new CMAESOptimizer(3);
		PointValuePair result = cOpt
				.optimize(1000, f, GoalType.MINIMIZE, start);

		// set the input functions parameters to be the result before finishing.
		double[] r = result.getPoint();

		double r0 = deNormalizeParameter(r[0], fitFunction.getParameter(0));
		double r1 = deNormalizeParameter(r[1], fitFunction.getParameter(2));
		double r2 = deNormalizeParameter(r[2], fitFunction.getParameter(3));
		double r3 = deNormalizeParameter(r[3], fitFunction.getParameter(4));
		double r4 = 0.0;

		// Now fit the system properly with the Full function
		if (fitConvolution.getExpression().contains("Full")) {

			// provide the fitting function which wrappers all the normal
			// fitting functionality
			MultivariateFunction f1 = new MultivariateFunction() {

				@Override
				public double value(double[] p) {
					double p0 = deNormalizeParameter(p[0],
							fitFunction.getParameter(0));
					double p1 = deNormalizeParameter(p[1],
							fitFunction.getParameter(2));
					double p2 = deNormalizeParameter(p[2],
							fitFunction.getParameter(3));
					double p3 = deNormalizeParameter(p[3],
							fitFunction.getParameter(4));
					double p4 = deNormalizeParameter(p[4],
							fitFunction.getParameter(5));

					FermiGauss fg = new FermiGauss(p0, kT, p1, p2, p3, p4);

					AbstractDataset fermiDS = fg.makeDataset(xAxis);

					AbstractDataset residual = Maths.subtract(fermiDS, values);
					residual.ipower(2);

					return (Double) residual.sum();
				}
			};

			// preform the optimisation
			start = new double[] {
					r[0],
					r[1],
					r[2],
					r[3],
					normalizeParameter(fitFunction.getParameterValue(5),
							fitFunction.getParameter(5)) };

			result = cOpt.optimize(1000, f1, GoalType.MINIMIZE, start);

			// set the input functions parameters to be the result before
			// finishing.
			r = result.getPoint();

			r0 = deNormalizeParameter(r[0], fitFunction.getParameter(0));
			r1 = deNormalizeParameter(r[1], fitFunction.getParameter(2));
			r2 = deNormalizeParameter(r[2], fitFunction.getParameter(3));
			r3 = deNormalizeParameter(r[3], fitFunction.getParameter(4));
			r4 = deNormalizeParameter(r[4], fitFunction.getParameter(5));
		}

		// Now fit the system properly with the Full function
		if (fitConvolution.getExpression().contains("Quick")) {

			final double t0 = deNormalizeParameter(r[0],
					fitFunction.getParameter(0));
			final double t1 = deNormalizeParameter(r[1],
					fitFunction.getParameter(2));
			final double t2 = deNormalizeParameter(r[2],
					fitFunction.getParameter(3));
			final double t3 = deNormalizeParameter(r[3],
					fitFunction.getParameter(4));

			// trim the x_axis to just around the fermi edge
			double width = 0.025;
			try {
				width = Double.parseDouble(quickConvolutionWidth
						.getExpression());
			} catch (Exception e) {
				// Not an issue if this fails as we have a default.
			}
			Double afterCrossing = DatasetUtils.crossings(xAxis, t0 + width)
					.get(0);
			Double beforeCrossing = DatasetUtils.crossings(xAxis, t0 - width)
					.get(0);
			Integer sliceStop = (int) Math.floor(afterCrossing);
			Integer sliceStart = (int) Math.floor(beforeCrossing);
			final AbstractDataset trimmedXAxis = xAxis.getSlice(new Slice(
					sliceStart, sliceStop));
			final AbstractDataset trimmedValues = values.getSlice(new Slice(
					sliceStart, sliceStop));

			// provide the fitting function which wrappers all the normal
			// fitting functionality
			MultivariateFunction f1 = new MultivariateFunction() {

				@Override
				public double value(double[] p) {

					double p0 = deNormalizeParameter(p[0],
							fitFunction.getParameter(5));

					FermiGauss fg = new FermiGauss(t0, kT, t1, t2, t3, p0);

					AbstractDataset fermiDS = fg.makeDataset(trimmedXAxis);

					AbstractDataset residual = Maths.subtract(fermiDS,
							trimmedValues);
					residual.ipower(2);

					return (Double) residual.sum();
				}
			};

			// preform the optimisation
			start = new double[] { normalizeParameter(
					fitFunction.getParameterValue(5),
					fitFunction.getParameter(5)) };

			result = cOpt.optimize(1000, f1, GoalType.MINIMIZE, start);

			// set the input functions parameters to be the result before
			// finishing.
			r = result.getPoint();

			r4 = deNormalizeParameter(Math.abs(r[0]),
					fitFunction.getParameter(5));
		}
		;

		fitFunction.setParameterValues(r0, kT, r1, r2, r3, r4);

		String plotName = updatePlotName.getExpression();
		if (!plotName.isEmpty()) {
			try {
				AbstractDataset fermiDS = fitFunction.makeDataset(xAxis);
				SDAPlotter.plot(plotName, xAxis, new IDataset[] { fermiDS,
						values });
			} catch (Exception e) {
				// Not an important issue, as its just for display, and doesn't
				// affect the result.
			}
		}

		return fitFunction;
	}

	@Override
	protected DataMessageComponent getTransformedMessage(
			List<DataMessageComponent> cache) throws DataMessageException {

		// get the data out of the message, name of the item should be specified
		final Map<String, Serializable> data = MessageUtils.getList(cache);

		// prepare the output message
		DataMessageComponent result = new DataMessageComponent();

		// put all the datasets in for reprocessing
		for (String key : data.keySet()) {
			result.addList(key, (AbstractDataset) data.get(key));
		}

		Map<String, AFunction> functions = null;
		try {
			functions = MessageUtils.getFunctions(cache);
		} catch (Exception e1) {
			throw createDataMessageException(
					"Failed to get the list of functions from the incomming message",
					e1);
		}

		// get the required datasets
		String dataset = datasetName.getExpression();
		String function = functionName.getExpression();
		String xAxis = xAxisName.getExpression();
		Integer fitDim = Integer.parseInt(fitDirection.getExpression());

		AbstractDataset dataDS = ((AbstractDataset) data.get(dataset)).clone();
		AFunction fitFunction = functions.get(function);
		AbstractDataset xAxisDS = null;
		if (data.containsKey(xAxis)) {
			xAxisDS = ((AbstractDataset) data.get(xAxis)).clone();
		} else {
			xAxisDS = DoubleDataset.arange(dataDS.getShape()[fitDim], 0, -1);
		}

		ArrayList<Slice> slices = new ArrayList<Slice>();
		for (int i = 0; i < dataDS.getShape().length; i++) {
			if (i == fitDim) {
				slices.add(new Slice(0, dataDS.getShape()[i], 1));
			} else {
				slices.add(new Slice(0, 1, 1));
			}
		}

		ArrayList<AbstractDataset> parametersDS = new ArrayList<AbstractDataset>(
				fitFunction.getNoOfParameters());
		for (int i = 0; i < fitFunction.getNoOfParameters(); i++) {
			int[] shape = dataDS.getShape().clone();
			shape[fitDim] = 1;
			DoubleDataset parameterDS = new DoubleDataset(shape);
			parameterDS.squeeze();
			parametersDS.add(parameterDS);
		}

		AbstractDataset functionsDS = new DoubleDataset(dataDS.getShape());

		int[] starts = dataDS.getShape().clone();
		starts[fitDim] = 1;
		DoubleDataset ind = DoubleDataset.ones(starts);
		IndexIterator iter = ind.getIterator();

		boolean first = true;

		while (iter.hasNext()) {
			System.out.println(iter.index);
			System.out.println(Arrays.toString(ind.getNDPosition(iter.index)));
			int[] start = ind.getNDPosition(iter.index).clone();
			int[] stop = start.clone();
			for (int i = 0; i < stop.length; i++) {
				stop[i] = stop[i] + 1;
			}
			stop[fitDim] = dataDS.getShape()[fitDim];
			AbstractDataset slice = dataDS.getSlice(start, stop, null);
			slice.squeeze();
			try {
				AFunction fitResult = null;
				if (first) {
					fitResult = FitGaussianConvFermi(xAxisDS, slice,
							fitFunction);
					first = false;
				} else {
					fitResult = FitGaussianConvFermi(xAxisDS, slice,
							fitFunction);
				}
				int[] position = new int[dataDS.getShape().length - 1];
				int count = 0;
				for (int i = 0; i < dataDS.getShape().length; i++) {
					if (i != fitDim) {
						position[count] = start[i];
						count++;
					}
				}
				for (int p = 0; p < fitResult.getNoOfParameters(); p++) {
					parametersDS.get(p).set(
							fitResult.getParameter(p).getValue(), position);
				}

				DoubleDataset resultFunctionDS = fitResult.makeDataset(xAxisDS);
				functionsDS.setSlice(resultFunctionDS, start, stop, null);

			} catch (Exception e) {
				throw createDataMessageException("Failed to fit row "
						+ iter.index + " of the data", e);
			}
		}

		result.addList("fit_image", functionsDS);
		for (int i = 0; i < fitFunction.getNoOfParameters(); i++) {
			result.addList("fit_parameter_" + i, parametersDS.get(i));
		}

		return result;
	}

	@Override
	protected String getOperationName() {
		return "Fit 1D data in 2D image";
	}

	/**
	 * Takes in a real value and returns a value between 0 and 1; this value can
	 * be reverted using deNormalizeParameter
	 * 
	 * @param value
	 * @param iParameter
	 * @return
	 */
	public static double normalizeParameter(double value, IParameter iParameter) {
		double max = iParameter.getUpperLimit();
		double min = iParameter.getLowerLimit();
		double range = max - min;
		return (value - min) / range;
	}

	/**
	 * 
	 * @param value
	 * @param iParameter
	 * @return
	 */
	public static double deNormalizeParameter(double value,
			IParameter iParameter) {
		double max = iParameter.getUpperLimit();
		double min = iParameter.getLowerLimit();
		double range = max - min;
		return (value * range) + min;
	}

}
