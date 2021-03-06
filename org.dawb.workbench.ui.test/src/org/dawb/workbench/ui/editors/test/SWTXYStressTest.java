/*
 * Copyright (c) 2012 Diamond Light Source Ltd.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */ 
package org.dawb.workbench.ui.editors.test;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.dawb.common.ui.util.EclipseUtils;
import org.dawb.workbench.ui.editors.AsciiEditor;
import org.dawb.workbench.ui.editors.ImageEditor;
import org.dawb.workbench.ui.editors.PlotDataEditor;
import org.dawnsci.plotting.AbstractPlottingSystem;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.RegionUtils;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace.DownsampleType;
import org.eclipse.dawnsci.plotting.api.trace.ILineTrace;
import org.eclipse.dawnsci.plotting.api.trace.ITrace;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.DoubleDataset;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.IntegerDataset;
import org.eclipse.january.dataset.Random;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.junit.Test;
import org.osgi.framework.Bundle;

import uk.ac.diamond.scisoft.analysis.io.LoaderFactory;

/**
 * 
 * Testing scalability of plotting.
 * 
 * @author gerring
 *
 */
public class SWTXYStressTest {
	
	
	@Test
	public void testRandomNumbers() throws Throwable {
		
		createTest(SWTXYTestUtils.createTestArraysRandom(10, 1000), 3000);
	}
	
	@Test
	public void testCoherantNumbers1() throws Throwable {
		
		createTest(SWTXYTestUtils.createTestArraysCoherent(10, 1000, null), 3000);
	}
	
	@Test
	public void testCoherantNumbers2() throws Throwable {
		
		createTest(SWTXYTestUtils.createTestArraysCoherent(100, 10000, null), 3000);
	}

	private void createTest(final List<IDataset> ys, long expectedTime) throws Throwable {
		
		final Bundle bun  = Platform.getBundle("org.dawb.workbench.ui.test");

		String path = (bun.getLocation()+"src/org/dawb/workbench/ui/editors/test/ascii.dat");
		path = path.substring("reference:file:".length());
		if (path.startsWith("/C:")) path = path.substring(1);
		
		final IWorkbenchPage page      = EclipseUtils.getPage();		
		final IFileStore  externalFile = EFS.getLocalFileSystem().fromLocalFile(new File(path));
		final IEditorPart part         = page.openEditor(new FileStoreEditorInput(externalFile), AsciiEditor.ID);
		
		final AsciiEditor editor       = (AsciiEditor)part;
		final PlotDataEditor plotter   = (PlotDataEditor)editor.getActiveEditor();
		final IPlottingSystem<Composite> sys = plotter.getPlottingSystem();
		
		//if (!(sys instanceof PlottingSystemImpl)) throw new Exception("This test is designed for "+PlottingSystemImpl.class.getName());
		page.setPartState(EclipseUtils.getPage().getActivePartReference(), IWorkbenchPage.STATE_MAXIMIZED);
				
		final long start = System.currentTimeMillis();
		
		sys.createPlot1D(DatasetFactory.createRange(0, ys.get(0).getSize(), 1, Dataset.INT32), ys, null);
		EclipseUtils.delay(10);
		
		final long end  = System.currentTimeMillis();
		final long time = end-start-10;
		
		System.out.println("It took "+time+"ms to plot the sets.");
		if (time>expectedTime) throw new Exception("It took too long to plot the data sets! It took "+time+"ms to plot the sets, it should be "+expectedTime+" or less.");
			
 		EclipseUtils.delay(2000);
		
		EclipseUtils.getPage().closeEditor(editor, false);
		System.out.println("Closed: "+path);
	}

	
	@Test
	public void testIfMemoryLeak1D() throws Throwable {
		
		final Bundle bun  = Platform.getBundle("org.dawb.workbench.ui.test");

		String path = (bun.getLocation()+"src/org/dawb/workbench/ui/editors/test/ascii.dat");
		path = path.substring("reference:file:".length());
		if (path.startsWith("/C:")) path = path.substring(1);
		
		final IWorkbenchPage page      = EclipseUtils.getPage();		
		final IFileStore  externalFile = EFS.getLocalFileSystem().fromLocalFile(new File(path));
		final IEditorPart part         = page.openEditor(new FileStoreEditorInput(externalFile), AsciiEditor.ID);
		
		final AsciiEditor editor       = (AsciiEditor)part;
		final PlotDataEditor plotter   = (PlotDataEditor)editor.getActiveEditor();
		final IPlottingSystem<Composite> sys = plotter.getPlottingSystem();
		
    	Dataset data = Random.rand(new int[]{2048});
    	
    	final IRegion region = sys.createRegion(RegionUtils.getUniqueName("Y Profile", sys), IRegion.RegionType.XAXIS_LINE);
		region.setTrackMouse(true);
		region.setRegionColor(ColorConstants.red);
		region.setUserRegion(false); // They cannot see preferences or change it!
		sys.addRegion(region);
		
		final ILineTrace trace = sys.createLineTrace("Test line plot");
		trace.setData(DatasetFactory.createRange(IntegerDataset.class, 2048), data);
		sys.addTrace(trace);

		System.gc();
		long sizeStart = Runtime.getRuntime().freeMemory();
				
        for (int i = 0; i < 1000; i++) { // TODO should be larger
			
        	data = Random.rand(new int[]{2048});
        	
        	Display.getDefault().syncExec(new Runnable() {
        		public void run() {
        			Dataset data = Random.rand(new int[]{2048});
         			trace.setData(DatasetFactory.createRange(IntegerDataset.class, 2048), data);
        			sys.repaint();
          		}
        	});
        	if (i%1000==0) System.out.println(i);
    		EclipseUtils.delay(1);
		}
	
		
 		LoaderFactory.clear();
		System.gc();
		EclipseUtils.delay(5000);
		
		long sizeEnd = Runtime.getRuntime().freeMemory();
        if ((sizeStart-sizeEnd)>10000) throw new Exception("Unexpected memory leak - "+(sizeStart-sizeEnd));
	} 

	@Test
	public void testIfMemoryLeak2D() throws Throwable {
		
		final Bundle bun  = Platform.getBundle("org.dawb.workbench.ui.test");

		String path = (bun.getLocation()+"src/org/dawb/workbench/ui/editors/test/billeA.edf");
		path = path.substring("reference:file:".length());
		if (path.startsWith("/C:")) path = path.substring(1);

		final IWorkbenchPage page      = EclipseUtils.getPage();		
		final IFileStore  externalFile = EFS.getLocalFileSystem().fromLocalFile(new File(path));
		final IEditorPart part         = page.openEditor(new FileStoreEditorInput(externalFile), ImageEditor.ID);
		page.setPartState(EclipseUtils.getPage().getActivePartReference(), IWorkbenchPage.STATE_MAXIMIZED);

		EclipseUtils.delay(2000);
		
		final AbstractPlottingSystem sys = (AbstractPlottingSystem)PlottingFactory.getPlottingSystem(part.getTitle());

		final Collection<ITrace>   traces= sys.getTraces(IImageTrace.class);
		final IImageTrace          imt = (IImageTrace)traces.iterator().next();

		System.gc();
		long sizeStart = Runtime.getRuntime().freeMemory();

        for (int i = 0; i < 1000; i++) {
			
        	final Dataset data = Random.rand(new int[]{2048, 2048});
        	
        	Display.getDefault().syncExec(new Runnable() {
        		public void run() {
                	imt.setData(data, null, false);
        		}
        	});
        	
        	if (i%1000==0) System.out.println(i);
    		EclipseUtils.delay(1);
      	
		}
	
 		LoaderFactory.clear();
		System.gc();
		EclipseUtils.delay(5000);
		
		long sizeEnd = Runtime.getRuntime().freeMemory();
		if ((sizeStart-sizeEnd)>10000) throw new Exception("Unexpected memory leak - "+(sizeStart-sizeEnd));
		
	}


	@Test
	public void testMaximumSpeed2DUpdate() throws Throwable {
		
		final Bundle bun  = Platform.getBundle("org.dawb.workbench.ui.test");

		String path = (bun.getLocation()+"src/org/dawb/workbench/ui/editors/test/billeA.edf");
		path = path.substring("reference:file:".length());
		if (path.startsWith("/C:")) path = path.substring(1);

		final IWorkbenchPage page      = EclipseUtils.getPage();		
		final IFileStore  externalFile = EFS.getLocalFileSystem().fromLocalFile(new File(path));
		final IEditorPart part         = page.openEditor(new FileStoreEditorInput(externalFile), ImageEditor.ID);
		page.setPartState(EclipseUtils.getPage().getActivePartReference(), IWorkbenchPage.STATE_MAXIMIZED);

		EclipseUtils.delay(2000);
		
		final IPlottingSystem<Composite> sys = (IPlottingSystem<Composite>)part.getAdapter(IPlottingSystem.class);

		final Collection<ITrace>   traces= sys.getTraces(IImageTrace.class);
		final IImageTrace          imt = (IImageTrace)traces.iterator().next();

		// 10 random images
    	final DoubleDataset[] data = new DoubleDataset[10];
    	for (int i = 0; i < data.length; i++) {
    		data[i] = Random.rand(new int[]{2048, 2048});
		}
 
    	// Plot something then fix histogram
    	Display.getDefault().syncExec(new Runnable() {
    		public void run() {
            	imt.setData(data[0], null, false);
    		}
    	});

    	imt.setDownsampleType(DownsampleType.POINT); // Fast!
    	imt.setRescaleHistogram(false); // Fast!
    	
    	
		long sizeStart = Runtime.getRuntime().freeMemory();

		Display.getDefault().syncExec(new Runnable() {
    		public void run() {
    			// Long loop printing how long to plot.
    			long start = System.currentTimeMillis();

    			double average = Double.NaN;
    			for (int i = 0; i < 100; i++) {

    				for (int j = 0; j < data.length; j++) {

    					imt.setData(data[j], null, false);

    					long end  = System.currentTimeMillis();
    					long diff = end-start;
    					System.out.println(diff);
    					average = Double.isNaN(average) 
    							? diff
    						    : (average+diff)/2d;   // cumulative moving average I think http://en.wikipedia.org/wiki/Moving_average
    					start = end;
    					
    					EclipseUtils.delay(1);
    				}

    			}

    			System.out.println("Average time to plot a 2k image = "+average+" ms");
    		}
    	});
    	
    	LoaderFactory.clear();
		System.gc();
		EclipseUtils.delay(1000);
		
		long sizeEnd = Runtime.getRuntime().freeMemory();
		if ((sizeStart-sizeEnd)>10000) throw new Exception("Unexpected memory leak - "+(sizeStart-sizeEnd));
		
	}

}
