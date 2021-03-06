package org.dawnsci.mapping.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.dawnsci.mapping.ui.datamodel.AssociatedImage;
import org.dawnsci.mapping.ui.datamodel.MapObject;
import org.dawnsci.mapping.ui.datamodel.MappedDataArea;
import org.dawnsci.mapping.ui.datamodel.MappedDataFile;
import org.dawnsci.mapping.ui.datamodel.PlottableMapObject;
import org.dawnsci.mapping.ui.datamodel.VectorMapData;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dawnsci.analysis.dataset.roi.PointROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.axis.IAxis;
import org.eclipse.dawnsci.plotting.api.region.IROIListener;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.dawnsci.plotting.api.region.ROIEvent;
import org.eclipse.dawnsci.plotting.api.region.RegionUtils;
import org.eclipse.dawnsci.plotting.api.trace.IImageTrace;
import org.eclipse.dawnsci.plotting.api.trace.ILineTrace;
import org.eclipse.dawnsci.plotting.api.trace.ITrace;
import org.eclipse.dawnsci.plotting.api.trace.IVectorTrace;
import org.eclipse.dawnsci.plotting.api.trace.IVectorTrace.ArrowConfiguration;
import org.eclipse.dawnsci.plotting.api.trace.MetadataPlotUtils;
import org.eclipse.january.DatasetException;
import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.DatasetUtils;
import org.eclipse.january.dataset.FloatDataset;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.ILazyDataset;
import org.eclipse.january.dataset.ShapeUtils;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.january.metadata.AxesMetadata;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapPlotManager {
	
	private IPlottingSystem<Composite> map;
	private IPlottingSystem<Composite> data;
	private MappedDataArea area;
	private ConcurrentLinkedDeque<MapTrace> layers;
	private PlotJob job;
	private PlotJob mapJob;
	private volatile Dataset merge;
	private AtomicInteger atomicPosition;
	private int layerCounter = 0;
	private boolean firstHold = true;
	private Runnable mapRunnable;

	
	private static long MIN_REFRESH_TIME = 2000;
	
	private final static Logger logger = LoggerFactory.getLogger(MapPlotManager.class);
	
	public MapPlotManager(IPlottingSystem<Composite> map, IPlottingSystem<Composite> data, MappedDataArea area) {
		this.map = map;
		this.data = data;
		this.area = area;
		atomicPosition = new AtomicInteger(0);
		layers = new ConcurrentLinkedDeque<MapTrace>();
		job = new PlotJob("Plot point...",false);
		job.setPriority(Job.INTERACTIVE);
		mapJob = new PlotJob("Map update...",true);
		mapJob.setPriority(Job.INTERACTIVE);
		mapRunnable = new Runnable() {
			
			@Override
			public void run(){
				try {

					for (MapTrace t : layers) {
						if (t.getMap().isLive()) {
							t.getMap().update();
							t.switchMap(t.getMap());
						}
					}
					plotLayers();
					
				} catch (RuntimeException ne) { 
					// Unchecked exceptions here show a dialog to the user
					logger.error("Cannot update map!", ne);
				}
			}
		};
		
		
		if (data.getAxes() != null) {
			List<IAxis> axes = data.getAxes();
			for (IAxis axis : axes) axis.setAxisAutoscaleTight(true);
		}
		
	}
	
	public void updatePlot(){	
		mapJob.setRunnable(mapRunnable);
		mapJob.schedule();
		
	}
	
	public void plotData(final double x, final double y) {
		final PlottableMapObject topMap = getTopMap(x,y);
		if (topMap == null)  {
			data.clear();
			return;
		}
		merge = null;
		firstHold = true;
		atomicPosition.set(0);
		Runnable r = new Runnable() {
			
			@Override
			public void run() {
				IDataset s = topMap.getSpectrum(x,y);
				if (s == null) {
					data.clear();
					return;
				}
				
				if (s.getSize() == 1) return;
				
				if (s != null) MetadataPlotUtils.plotDataWithMetadata(s, data);
				
				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						Collection<IRegion> regions = map.getRegions();
						Iterator<IRegion> it = regions.iterator();
						while (it.hasNext()) {
							IRegion r = it.next();
							if (r.getUserObject() ==  MapPlotManager.this){
								map.removeRegion(r);
							}
						}
						map.repaint(false);
					}
					
				});
				
			}
		};
		
		job.setRunnable(r);
		
		job.schedule();
	}
	
	public void plotDataWithHold(final double x, final double y) {
		final PlottableMapObject topMap = getTopMap(x,y);
		if (topMap == null) return;
		final ILazyDataset lz = topMap.getSpectrum(x,y);
		
		Runnable r = null;
		
		if (ShapeUtils.squeezeShape(lz.getShape(),false).length > 1) {
			job.cancel();
			
			r = new Runnable() {
				
				@Override
				public void run() {
					if (firstHold) data.clear();
					firstHold = false;
					IDataset s = null;
					try {
						s = lz.getSlice().squeeze();
					} catch (DatasetException e) {
						logger.error("Could not get data from lazy dataset", e);
						return;
					}
					if (s != null) {
						Dataset mergedDataset = getMergedDataset(s);
						int pos = atomicPosition.getAndIncrement() % 4;
						int[] mShape = mergedDataset.getShape();
						SliceND slice = new SliceND(mShape);
						if (pos == 0){
							slice.setSlice(0,0, mShape[0]/2, 1);
							slice.setSlice(1,0, mShape[1]/2, 1);
							mergedDataset.setSlice(s,slice);

						} else if (pos == 1) {

								slice.setSlice(0,0, mShape[0]/2, 1);
								slice.setSlice(1,mShape[1]/2, mShape[1], 1);
								mergedDataset.setSlice(s,slice);

						} else if (pos == 2) {

							slice.setSlice(0,mShape[0]/2, mShape[0], 1);
							slice.setSlice(1,0,mShape[1]/2, 1);
							mergedDataset.setSlice(s,slice);

					} else if (pos == 3) {

						slice.setSlice(0,mShape[0]/2, mShape[0], 1);
						slice.setSlice(1,mShape[1]/2, mShape[1], 1);
						mergedDataset.setSlice(s,slice);

					}
						MetadataPlotUtils.plotDataWithMetadata(mergedDataset, data);
								
						
					}
				}
			};
			
			
			
		} else {
			r = new Runnable() {
				
				@Override
				public void run() {
					IDataset s = null;
					try {
						s = lz.getSlice();
					} catch (DatasetException e) {
						logger.error("Could not get data from lazy dataset", e);
						return;
					}
					if (s != null) {
						final ILineTrace l = MetadataPlotUtils.buildLineTrace(s, data);
						
						Display.getDefault().syncExec(new Runnable() {
							
							@Override
							public void run() {
								if (firstHold) data.clear();
								firstHold = false;
								data.addTrace(l);
								try {
									String uniqueName = RegionUtils.getUniqueName("Click", map);
									final IRegion re = map.createRegion(uniqueName, RegionType.POINT);
									re.setROI(new PointROI(x, y));
									re.setAlpha(255);
									re.setRegionColor(l.getTraceColor());
									re.setUserObject(MapPlotManager.this);
									re.addROIListener(new IROIListener.Stub() {
										@Override
										public void roiSelected(ROIEvent evt) {
											Collection<ITrace> ts = data.getTraces();
											for (ITrace t : ts) if (t instanceof ILineTrace) ((ILineTrace)t).setLineWidth(1);
											l.setLineWidth(2);

										}
									});
//									((AbstractSelectionRegion)re).setHighlighted(true);
									map.addRegion(re);
									re.setMobile(false);
									re.setUserRegion(false);
									re.setUserObject(MapPlotManager.this);
									map.clearRegionTool();
								} catch (Exception e) {
									logger.error("Error plotting line trace",e);
								}
								
								
							}
						});
					}
					
				}
			};
		}
		
		job.setRunnable(r);
		
		job.schedule();
	}
	
	public void unplotFile(MappedDataFile file){
		
		Object[] c = file.getChildren();
		List<Object> list = Arrays.asList(c);
		
		Iterator<MapTrace> it = layers.iterator();
		
		while (it.hasNext()) {
			MapTrace m = it.next();
			if (list.contains(m.getMap())) {
				it.remove();
				if (m.getTrace() != null) this.map.removeTrace(m.getTrace());
			}
		}

		plotLayers();
		
	}
	
	public void updateLayers(PlottableMapObject map) {
		
		Iterator<MapTrace> it = layers.iterator();
		
		while (it.hasNext()) {
			MapTrace m = it.next();
			if (m.getMap() == map) {
				it.remove();
				if (m.getTrace() != null) this.map.removeTrace(m.getTrace());
				plotLayers();
				return;
			}
		}
		
		if (map == null) {
			for (int i = 0; i < area.count();i++) {
				
				AssociatedImage associatedImage = area.getDataFile(i).getAssociatedImage();
				if (associatedImage != null) {
					addImage(associatedImage);
				}
				
				map = area.getDataFile(i).getMap();
				if (map == null) {
					logger.debug("Map is null");
					continue;
				}
				addMap(map);
			}
 			
		} else {
			addMap(map);
		}
		
		plotLayers();
	}
	
	public void plotMap(PlottableMapObject map) {
		addMap(map);
		plotLayers();
	}
	
	public void addImage(AssociatedImage image) {
		Iterator<MapTrace> it = layers.iterator();
		
		while (it.hasNext()) {
			MapTrace m = it.next();
			if (m.getMap() == image) {
				it.remove();
				this.map.removeTrace(m.getTrace());
				plotLayers();
				return;
			}
		}	

		layers.addLast(new MapTrace(image, null));
		map.clearTraces();
		for (MapTrace l : layers) l.rebuildTrace();
		
		plotLayers();
	}
	
	public PlottableMapObject getTopMap(double x, double y){
		
		Iterator<MapTrace> iterator = layers.iterator();
		
		while (iterator.hasNext()) {
			MapObject l = iterator.next().getMap();
			double[] range = l.getRange();
			
			if (l instanceof PlottableMapObject && x >= range[0] && x <= range[1] && y >= range[2] && y <= range[3]) return (PlottableMapObject)l;
			
		}
		
		return null;
	}
	
	public PlottableMapObject getTopMap(){
		
		Iterator<MapTrace> iterator = layers.iterator();
		
		while (iterator.hasNext()) {
			MapObject l = iterator.next().getMap();
			if (l instanceof PlottableMapObject) return (PlottableMapObject)l;
		}
		
		return null;
	}
	
	
	public void clearAll(){
		map.clearTraces();
		data.clear();
		layers.clear();
	}
	
	private void addMap(PlottableMapObject map) {

		MapTrace sameMap = null;
		
		Iterator<MapTrace> iterator = layers.iterator();
		
		while (iterator.hasNext()) {
			MapTrace l = iterator.next();
			if (l.getMap() instanceof PlottableMapObject && isTheSameMap((PlottableMapObject)l.getMap(), map)) {
				sameMap = l;
				break;
			}
		}
		
		
		if (sameMap != null) {
			if (map instanceof VectorMapData) {
				IVectorTrace vectorTrace = createVectorTrace(map);
				layers.push(new MapTrace(map, vectorTrace));
			}
			else{
				sameMap.switchMap(map); //test if switches images only
			}
		}
		else if (map instanceof VectorMapData) {
			// Initialise a vector trace
			IVectorTrace vectorTrace = createVectorTrace(map);
			layers.push(new MapTrace(map, vectorTrace));
		} 
		else {
			IImageTrace t = createImageTrace(map);
			layers.push(new MapTrace(map, t));
		}
	}
	
	
	private IImageTrace createImageTrace(MapObject ob) {
		
		String longName = "Layer " + layerCounter++;
		IDataset map = null;
		
		if (ob instanceof PlottableMapObject) {
			PlottableMapObject amd = (PlottableMapObject)ob;
			map = amd.getMap();
		}
		
		if (map == null) return null;
		IImageTrace t = null;
		try {
			t = MetadataPlotUtils.buildTrace(longName, map, this.map);
			//TODO something better here:
			t.setGlobalRange(sanizeRange(ob.getRange(), map.getShape()));
			if (ob instanceof PlottableMapObject)  t.setAlpha(((PlottableMapObject)ob).getTransparency());
		} catch (Exception e) {
			logger.error("Error creating image trace", e);
		}
	
		return t;
	}
	
	
	private IVectorTrace createVectorTrace (MapObject ob) {

		String longName = "Layer " + layerCounter++;
		IDataset map = null;
		
		if (ob instanceof PlottableMapObject) {
			PlottableMapObject amd = (PlottableMapObject)ob;
			map = amd.getMap();
		}
		
		if (map == null) return null;
		
		IVectorTrace vectorTrace = null;
		
		try {
			// Create the vector trace, the long way round
			vectorTrace = this.map.createVectorTrace(longName);
			// Get the axes
			AxesMetadata axesMetadata = map.getFirstMetadata(AxesMetadata.class);
			IDataset yAxis = (IDataset) DatasetUtils.sliceAndConvertLazyDataset(axesMetadata.getAxis(0)[0]).squeeze();
			IDataset xAxis = (IDataset) DatasetUtils.sliceAndConvertLazyDataset(axesMetadata.getAxis(1)[0]).squeeze();
			// Set the map datapoints and axes
			vectorTrace.setData(map, Arrays.asList(xAxis, yAxis));
			// And whilst setting up the plot, also define some plot specific options
			vectorTrace.setArrowColor(new int[] {200, 0, 0});
			vectorTrace.setCircleColor(new int[] {0, 200, 0});
			vectorTrace.setArrowConfiguration(ArrowConfiguration.THROUGH_CENTER);
		}
		catch (Exception e) {
		logger.error("Error creating image trace", e);
		}
			
		// Return the trace
		return vectorTrace;
	}
	
	
	public void plotLayers(){
		
		if (Display.getCurrent() == null) {
			PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
				
				@Override
				public void run() {
					plotLayers();
				}
			});
			return;
		}
		
		if (!map.is2D()) map.setPlotType(PlotType.IMAGE);
		
		updatePlottedRange();
		
		try {
			
			if (layers.isEmpty()) {
				map.clearTraces();
				layerCounter = 0;
				return;
			} else {
				
			}
			
			Iterator<MapTrace> it = layers.descendingIterator();
			
			Collection<ITrace> traces = map.getTraces(IImageTrace.class);
			
			while (it.hasNext()) {
				MapTrace m = it.next();
				if (!traces.contains(m.getTrace())) {
					if (m.getTrace() != null) map.addTrace(m.getTrace());
				}
				
			}
			this.map.repaint();
		} catch (Exception e) {
			logger.error("Error plotting mapped data", e);
		}
	}
	
	private boolean isTheSameMap(PlottableMapObject omap, PlottableMapObject map) {
		
		if (omap.getLongName().equals(map.getLongName())) return true;
		
		if (omap.getMap() == null ||  map.getMap() == null) return false;
		
		if (!Arrays.equals(omap.getMap().getShape(), map.getMap().getShape())) return false;
		
		AxesMetadata oax = omap.getMap().getFirstMetadata(AxesMetadata.class);
		AxesMetadata ax = map.getMap().getFirstMetadata(AxesMetadata.class);
		
		if (oax == null || ax == null) return false; // should never be the case
		
		ILazyDataset[] oaxes = oax.getAxes();
		ILazyDataset[] axes = ax.getAxes();
		
		if (oaxes.length != axes.length) return false;
		
		for (int i = 0 ; i < oaxes.length; i++) {
			if (oaxes[i] == null) return false;
			if (axes[i] == null) return false;
		
			IDataset oa;
			try {
				oa = oaxes[i].getSlice();
			} catch (DatasetException e) {
				logger.warn("Could not get data from lazy dataset", e);
				return false;
			}
			IDataset a;
			try {
				a = axes[i].getSlice();
			} catch (DatasetException e) {
				logger.warn("Could not get data from lazy dataset", e);
				return false;
			}
			if (!oa.equals(a)) return false;
		}
		
		return true;
		
	}
	
	public void updatePlottedRange(){
		
		double[] range = null;
		
		Iterator<MapTrace> iterator = layers.iterator();
		
		while (iterator.hasNext()) {
			PlottableMapObject object = iterator.next().getMap();
			double[] r = object.getRange();
			if (r == null) continue;
			
			if (range == null) {
				range = r;
				continue;
			}
			
			range[0]  = r[0] < range[0] ? r[0] : range[0];
			range[1]  = r[1] > range[1] ? r[1] : range[1];
			range[2]  = r[2] < range[2] ? r[2] : range[2];
			range[3]  = r[3] > range[3] ? r[3] : range[3];
		}
		
		if (range == null) return;
		
		iterator = layers.iterator();
		
		while (iterator.hasNext()) {
			ITrace trace = iterator.next().getTrace();
			if (trace instanceof IImageTrace) {
				((IImageTrace)trace).setGlobalRange(range);
			}

		}
		
	}
	
	public static void updateRange(MapObject object, double[] range) {
		if (object == null) return;
		double[] r = object.getRange();
		if (r == null) return;
		
		range[0]  = r[0] < range[0] ? r[0] : range[0];
		range[1]  = r[1] > range[1] ? r[1] : range[1];
		range[2]  = r[2] < range[2] ? r[2] : range[2];
		range[3]  = r[3] > range[3] ? r[3] : range[3];
		
	}
	
	
	public void setTransparency(PlottableMapObject m) {
		
		
		Iterator<MapTrace> iterator = layers.iterator();

		while (iterator.hasNext()) {
			MapTrace l = iterator.next();
			if (l.getMap() == m && l.getTrace() instanceof IImageTrace) ((IImageTrace)l.getTrace()).setAlpha(m.getTransparency());
		}
		
		map.repaint(false);
	}
	
	public boolean isPlotted(MapObject object) {
		
		for (MapTrace t : layers) {
			if (t.getMap().equals(object)) return true;
		}
		
		return false;
	}
	
	private Dataset getMergedDataset(IDataset input) {

		Dataset m = merge;

		if (m == null) {
			synchronized(this) {
				m = merge;
				if (m == null) {
					int[] newShape = ShapeUtils.squeezeShape(input.getShape(), false);
					for (int i = 0; i<newShape.length;i++) newShape[i]*=2;
					FloatDataset f = DatasetFactory.zeros(FloatDataset.class, newShape);
					Arrays.fill(f.getData(), Float.NaN);
					m = merge = f;

				}		
			}
		}
		return m;
	}
	
	private class PlotJob extends Job {

		private final AtomicReference<Runnable> task =new AtomicReference<Runnable>();
		private boolean delayed = false;
		
		public PlotJob(String name, boolean delayed) {
			super(name);
			this.delayed = delayed;
		}
		
		public void setRunnable(Runnable runnable) {
			this.task.set(runnable);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Runnable local = task.getAndSet(null);
			if (local == null) return Status.OK_STATUS;
			local.run();
			
			if (delayed) {
				try {
					Thread.sleep(MIN_REFRESH_TIME);
				} catch (InterruptedException e) {
					return Status.OK_STATUS;
				}
			}
			
			return Status.OK_STATUS;
		}
		
	}
	
	private double[] sanizeRange(double[] range, int[] shape) {
		if(range[0] == range[1] && range[2] == range[3]) return range;
		
		double[] r = range.clone();
		
		if (range[0] == range[1]) {
			r[1] = range[0] + (range[3] - range[2])/shape[1];
			return r;
		}
		
		if (range[2] == range[3]) {
			r[3] = range[2] + (range[1] - range[0])/shape[0];
		}
		return r;
		
	}
	
	private class MapTrace {
		
		private PlottableMapObject map;
		private ITrace trace;

		public MapTrace(PlottableMapObject map, ITrace trace) {
			this.map = map;
			this.trace = trace;
		}

		public PlottableMapObject getMap() {
			return map;
		}

		public ITrace getTrace() {
			return trace;
		}
		
		public void switchMap(final PlottableMapObject ob) {
			try {
				final IDataset d = ob.getMap();
				switchMap(ob.getLongName(),d);
				map = ob;
			} catch (Exception e) {
				logger.error("Error updating live!",e);
			}

		}

		public void rebuildTrace(){
			trace = createImageTrace(map);
		}

		private void switchMap(final String name, final IDataset d) {
			if (Display.getCurrent() == null) {
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {

					@Override
					public void run() {
						switchMap(name, d);
					}
				});
				return;
			}
			
			if (d == null) return;

			if (d.getRank() > 2) {
				d.setShape(new int[]{d.getShape()[0],d.getShape()[1]});
			}

			if (trace == null) {
				trace = createImageTrace(map);
			} else {
				if (trace instanceof IVectorTrace) {
					// TODO Passing this over for now, must sort
				}
				else {
					MetadataPlotUtils.switchData(name,d, (IImageTrace) trace);
				}
			}
		}
	}
}
