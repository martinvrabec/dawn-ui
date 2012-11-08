package org.dawb.workbench.plotting.tools.diffraction;

import java.util.Map;
import java.util.TreeMap;

import javax.measure.quantity.Angle;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Length;
import javax.measure.quantity.Quantity;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;
import javax.swing.tree.TreeNode;
import javax.vecmath.Vector3d;

import org.dawb.common.services.IImageService;
import org.dawb.common.services.ServiceManager;
import org.dawb.common.ui.plot.trace.IImageTrace;
import org.eclipse.jface.viewers.TreeViewer;
import org.jscience.physics.amount.Amount;

import uk.ac.diamond.scisoft.analysis.diffraction.DetectorProperties;
import uk.ac.diamond.scisoft.analysis.diffraction.DetectorPropertyEvent;
import uk.ac.diamond.scisoft.analysis.diffraction.DiffractionCrystalEnvironment;
import uk.ac.diamond.scisoft.analysis.diffraction.IDetectorPropertyListener;
import uk.ac.diamond.scisoft.analysis.io.IDiffractionMetadata;
import uk.ac.diamond.scisoft.analysis.io.IMetaData;

/**
 * Holds data for the Diffraction model.
 * 
 * Use getNode(String labelPath)  to get a node for use in calculation actions.
 * 
 * The label path is the path to the value in label strings. It is not case
 * sensitive. For instance '/experimental information/beam center/X'  or
 * '/Experimental Information/Beam Center/X'
 * 
 * 
 * 
 * @author fcp94556
 *
 */
public class DiffractionTreeModel {

	private LabelNode   root;
    private TreeViewer  viewer;
    
	private Map<String, TreeNode> nodeMap;
	private boolean isDisposed;
	
	private Unit<Length>               xpixel, ypixel;
	private NumericNode<Dimensionless> max,min,mean;
	private NumericNode<Length>        beamX, beamY;
	private final IMetaData metaData;
	
	
	public DiffractionTreeModel(IMetaData metaData) throws Exception {
		this.metaData = metaData;
		this.root     = new LabelNode();
		createDiffractionModel(metaData);
		nodeMap = new TreeMap<String, TreeNode>();
	}

	private void createDiffractionModel(IMetaData metaData) throws Exception {

		final DiffractionCrystalEnvironment dce = getCrystalEnvironment();						
	    final DetectorProperties        detprop = getDetectorProperties();
		
	    LabelNode experimentalInfo = createExperimentalInfo(dce, detprop);
        
        createBeamCenter(detprop, experimentalInfo);
        createIntensity();      
        createDetector(dce, detprop);
        createRaw(metaData);
        
        createUnitsListeners(detprop);
        
        // TODO listen to other things, for instance refine when it
        // is available may change other values.
        createBeamCenterListener(detprop);
	}
	
	private DetectorProperties getDetectorProperties() {
		return (metaData instanceof IDiffractionMetadata)
				? ((IDiffractionMetadata)metaData).getDetector2DProperties()
						: null;
	}

	private DiffractionCrystalEnvironment getCrystalEnvironment() {
		return (metaData instanceof IDiffractionMetadata)
				? ((IDiffractionMetadata)metaData).getDiffractionCrystalEnvironment()
						: null;
	}

	private void createUnitsListeners(final DetectorProperties detprop) {
        if (detprop!=null) {
        	beamX.setDefault(getBeamX(detprop.getOriginal()));
        	beamX.setValue(getBeamX(detprop));
        	beamX.addAmountListener(new AmountListener<Length>() {		
    			@Override
    			public void amountChanged(AmountEvent<Length> evt) {
    				setBeamX(detprop, evt.getAmount());
    			}
    		});
        }
        beamX.setIncrement(1);
		beamX.setFormat("#0");
		beamX.setLowerBound(0);
		beamX.setUpperBound(100000);
        beamX.addUnitListener(createPixelFormatListener(beamX));
        
        if (detprop!=null) {
        	beamY.setDefault(getBeamY(detprop.getOriginal()));
        	beamY.setValue(getBeamY(detprop));
        	beamY.addAmountListener(new AmountListener<Length>() {		
    			@Override
    			public void amountChanged(AmountEvent<Length> evt) {
    				setBeamY(detprop, evt.getAmount());
    			}
    		});
        }
        beamY.setIncrement(1);
        beamY.setFormat("#0");
		beamY.setLowerBound(0);
		beamY.setUpperBound(100000);
        beamY.addUnitListener(createPixelFormatListener(beamY));		
	}

	private void createRaw(IMetaData metaData) throws Exception {
		
        if (metaData!=null && metaData.getMetaNames()!=null && metaData.getMetaNames().size()>0) {
            final LabelNode rawMeta = new LabelNode("Raw Meta", root);
	        registerNode(rawMeta);
        	for (String name : metaData.getMetaNames()) {
        		ObjectNode on = new ObjectNode(name, metaData.getMetaValue(name), rawMeta);
		        registerNode(on);
			}
        }		
	}

	private void createDetector(final DiffractionCrystalEnvironment dce, final DetectorProperties detprop) {
       
		// Detector Meta
        final LabelNode detectorMeta = new LabelNode("Detector", root);
        registerNode(detectorMeta);
        detectorMeta.setDefaultExpanded(true);
        
        final NumericNode<Duration> exposure   = new NumericNode<Duration>("Exposure Time", detectorMeta, SI.SECOND);
        registerNode(exposure);
        if (dce!=null) {
           	exposure.setDefault(dce.getOriginal().getExposureTime(), SI.SECOND);
           	exposure.setValue(dce.getExposureTime(), SI.SECOND);
        }
        
        final LabelNode size = new LabelNode("Size", detectorMeta);
        registerNode(size);
        NumericNode<Length> x  = new NumericNode<Length>("x", size, SI.MILLIMETER);
        registerNode(x);
        if (detprop!=null) {
        	x.setDefault(detprop.getOriginal().getDetectorSizeH(), SI.MILLIMETER);
        	x.setValue(detprop.getDetectorSizeH(), SI.MILLIMETER);
        }
        NumericNode<Length> y  = new NumericNode<Length>("y", size, SI.MILLIMETER);
        registerNode(y);
        if (detprop!=null) {
        	y.setDefault(detprop.getOriginal().getDetectorSizeV(), SI.MILLIMETER);
        	y.setValue(detprop.getDetectorSizeV(), SI.MILLIMETER);
        }

        final LabelNode pixel = new LabelNode("Pixel", detectorMeta);
        registerNode(pixel);
        
        final NumericNode<Length> xPixelSize  = new NumericNode<Length>("x-size", pixel, SI.MILLIMETER);
        registerNode(xPixelSize);
        if (detprop!=null) {
           	xPixelSize.setDefault(detprop.getOriginal().getHPxSize(), SI.MILLIMETER);
           	xPixelSize.setValue(detprop.getHPxSize(), SI.MILLIMETER);
        	xPixelSize.addAmountListener(new AmountListener<Length>() {				
				@Override
				public void amountChanged(AmountEvent<Length> evt) {
					detprop.setHPxSize(evt.getAmount().doubleValue(SI.MILLIMETER));
				}
			});
        }
        xPixelSize.setEditable(true);
        xPixelSize.setIncrement(0.01);
        xPixelSize.setFormat("#0.###");
        xPixelSize.setLowerBound(0.001);
        xPixelSize.setUpperBound(1000);

        final NumericNode<Length> yPixelSize  = new NumericNode<Length>("y-size", pixel, SI.MILLIMETER);
        registerNode(yPixelSize);
        if (detprop!=null) {
        	yPixelSize.setDefault(detprop.getOriginal().getVPxSize(), SI.MILLIMETER);
        	yPixelSize.setValue(detprop.getVPxSize(), SI.MILLIMETER);
        	yPixelSize.addAmountListener(new AmountListener<Length>() {				
				@Override
				public void amountChanged(AmountEvent<Length> evt) {
					detprop.setVPxSize(evt.getAmount().doubleValue(SI.MILLIMETER));
				}
			});
        }
        yPixelSize.setEditable(true);
        yPixelSize.setIncrement(0.01);
        yPixelSize.setFormat("#0.###");
        yPixelSize.setLowerBound(0.001);
        yPixelSize.setUpperBound(1000);
        
        // Listeners
        xpixel = setBeamCenterUnit(xPixelSize, beamX, "pixel");
        xPixelSize.addAmountListener(new AmountListener() {		
			@Override
			public void amountChanged(AmountEvent evt) {
		        xpixel = setBeamCenterUnit(xPixelSize, beamX, "pixel");
			}
		});
        
        ypixel = setBeamCenterUnit(yPixelSize, beamY, "pixel");
        yPixelSize.addAmountListener(new AmountListener() {		
			@Override
			public void amountChanged(AmountEvent evt) {
				ypixel = setBeamCenterUnit(yPixelSize, beamY, "pixel");
			}
		});		
	}

	private void createBeamCenter(DetectorProperties detprop,
			                      LabelNode experimentalInfo) {
	    // Beam Center
        final LabelNode beamCen = new LabelNode("Beam Center", experimentalInfo);
        registerNode(beamCen);
        beamCen.setDefaultExpanded(true);
      
        this.beamX = new NumericNode<Length>("X", beamCen, SI.MILLIMETER);
        registerNode(beamX);
        beamX.setEditable(true);
       
        this.beamY = new NumericNode<Length>("Y", beamCen, SI.MILLIMETER);
        registerNode(beamY);
        beamY.setEditable(true);
		
	}

	private void createIntensity() {
	       // Pixel Info
        final LabelNode pixelValue = new LabelNode("Intensity", root);
        registerNode(pixelValue);
        pixelValue.setDefaultExpanded(true);
				                 
        this.max  = new NumericNode<Dimensionless>("Visible Maximum", pixelValue, Dimensionless.UNIT);
        registerNode(max);
        this.min  = new NumericNode<Dimensionless>("Visible Minimum", pixelValue, Dimensionless.UNIT);
        registerNode(min);
        this.mean = new NumericNode<Dimensionless>("Mean", pixelValue, Dimensionless.UNIT);
        registerNode(mean);
 		
	}

	private LabelNode createExperimentalInfo(final DiffractionCrystalEnvironment dce,
			                                 final DetectorProperties detprop) {
	    // Experimental Info
        final LabelNode experimentalInfo = new LabelNode("Experimental Information", root);
        registerNode(experimentalInfo);
        experimentalInfo.setDefaultExpanded(true);
       
        NumericNode<Length> lambda = new NumericNode<Length>("Wavelength", experimentalInfo, NonSI.ANGSTROM);
        registerNode(lambda);
        if (dce!=null) {
           	lambda.setDefault(dce.getOriginal().getWavelength(), NonSI.ANGSTROM);
           	lambda.setValue(dce.getWavelength(), NonSI.ANGSTROM);
        	lambda.addAmountListener(new AmountListener<Length>() {		
				@Override
				public void amountChanged(AmountEvent<Length> evt) {
					dce.setWavelength(evt.getAmount().doubleValue(NonSI.ANGSTROM));
				}
			});
        }
        lambda.setEditable(true);
        lambda.setIncrement(0.01);
        lambda.setFormat("#0.##");
        lambda.setLowerBound(0);
        lambda.setUpperBound(1000);
        lambda.setUnits(NonSI.ANGSTROM, NonSI.ELECTRON_VOLT, SI.KILO(NonSI.ELECTRON_VOLT), SI.GIGA(NonSI.ELECTRON_VOLT));
        
        final NumericNode<Length> dist   = new NumericNode<Length>("Distance", experimentalInfo, SI.MILLIMETER);
        registerNode(dist);
        if (detprop!=null) {
        	dist.setDefault(detprop.getOriginal().getOrigin().z, SI.MILLIMETER);
        	dist.setValue(detprop.getOrigin().z, SI.MILLIMETER);
            dist.addAmountListener(new AmountListener<Length>() {		
    			@Override
    			public void amountChanged(AmountEvent<Length> evt) {
    				Vector3d origin = detprop.getOrigin();
    				origin.z = evt.getAmount().doubleValue(SI.MILLIMETER);
    				detprop.setOrigin(origin);
    			}
    		});
        }
        dist.setEditable(true);

        dist.setIncrement(1);
        dist.setFormat("#0.##");
        dist.setLowerBound(0);
        dist.setUpperBound(1000000);
        final Unit<Length> micron = SI.MICRO(SI.METER);
        dist.setUnits(SI.MILLIMETER, micron, NonSI.INCH);
     
        NumericNode<Angle> start = new NumericNode<Angle>("Oscillation Start", experimentalInfo, NonSI.DEGREE_ANGLE);
        registerNode(start);
        if (dce!=null)  {
        	start.setDefault(dce.getOriginal().getPhiStart(), NonSI.DEGREE_ANGLE);
        	start.setValue(dce.getPhiStart(), NonSI.DEGREE_ANGLE);
        }
       
        NumericNode<Angle> stop = new NumericNode<Angle>("Oscillation Stop", experimentalInfo, NonSI.DEGREE_ANGLE);
        registerNode(stop);
        if (dce!=null)  {
        	stop.setDefault(dce.getOriginal().getPhiStart()+dce.getOriginal().getPhiRange(), NonSI.DEGREE_ANGLE);
        	stop.setValue(dce.getPhiStart()+dce.getPhiRange(), NonSI.DEGREE_ANGLE);
        }

        NumericNode<Angle> osci = new NumericNode<Angle>("Oscillation Range", experimentalInfo, NonSI.DEGREE_ANGLE);
        registerNode(osci);
        if (dce!=null)  {
        	osci.setDefault(dce.getOriginal().getPhiRange(), NonSI.DEGREE_ANGLE);
        	osci.setValue(dce.getPhiRange(), NonSI.DEGREE_ANGLE);
        }

	    return experimentalInfo;
	}

	private Amount<Length> getBeamX(DetectorProperties dce) {
		final double[] beamCen = dce.getBeamLocation();
		return Amount.valueOf(beamCen[0], xpixel);
	}
	
	private Amount<Length> getBeamY(DetectorProperties dce) {
		final double[] beamCen = dce.getBeamLocation();
		return Amount.valueOf(beamCen[1], ypixel);
	}

	private void setBeamX(DetectorProperties dce, Amount<Length> beamX) {
		final double[] beamCen = dce.getBeamLocation();
		beamCen[0] = beamX.doubleValue(xpixel);
		try {
			beamCenterActive = false;
		    dce.setBeamLocation(beamCen);
		} finally {
			beamCenterActive = true;
		}
	}
	private void setBeamY(DetectorProperties dce, Amount<Length> beamY) {
		final double[] beamCen = dce.getBeamLocation();
		beamCen[1] = beamY.doubleValue(ypixel);
		try {
			beamCenterActive = false;
			dce.setBeamLocation(beamCen);
		} finally {
			beamCenterActive = true;
		}
	}
	
	private IDetectorPropertyListener beamCenterListener;
	private boolean                   beamCenterActive=true;
	private void createBeamCenterListener(final DetectorProperties detprop) {
		if (beamCenterListener==null) this.beamCenterListener = new IDetectorPropertyListener() {
			
			@Override
			public void detectorPropertiesChanged(DetectorPropertyEvent evt) {
				if (!beamCenterActive) return;
				if (evt.hasBeamCentreChanged()) {
					double[]     cen = detprop.getBeamLocation();
					Amount<Length> x = Amount.valueOf(cen[0], xpixel);
					beamX.setValue(x.to(beamX.getValue().getUnit()));
					if (viewer!=null) viewer.refresh(beamX); // Cancels cell editing.
					
					Amount<Length> y = Amount.valueOf(cen[1], ypixel);
					beamY.setValue(y.to(beamY.getValue().getUnit()));
					if (viewer!=null) viewer.refresh(beamY);  // Cancels cell editing.
				}
			}
		};
		detprop.addDetectorPropertyListener(beamCenterListener);
	}

	
	private void registerNode(LabelNode node) {
		final String labelPath = node.getPath();
		// System.out.println(labelPath);
		if (labelPath!=null && nodeMap!=null) {
			this.nodeMap.put(labelPath, node);
		}
	}
	
	/**
	 * Get any node from the tree. Useful when running algorithms with the model.
	 * @param labelPath
	 * @return
	 */
	public TreeNode getNode(final String labelPath) {
		return nodeMap.get(labelPath.toLowerCase());
	}

	private UnitListener createPixelFormatListener(final NumericNode node) {
		return new UnitListener() {			
			@Override
			public void unitChanged(UnitEvent<? extends Quantity> evt) {
				if (evt.getUnit().toString().equals("pixel")) {
					node.setIncrement(1);
					node.setFormat("#0");
					node.setLowerBound(0);
					node.setUpperBound(100000);
				} else {
					node.setIncrement(0.01);
					node.setFormat("#0.##");
					node.setLowerBound(0);
					node.setUpperBound(1000);
				}
			}
		};	
	}

	protected Unit<Length> setBeamCenterUnit(NumericNode<Length> size,
			                                 NumericNode<Length> coord,
			                                 String unitName) {
		
        Unit<Length> unit = SI.MILLIMETER.times(size.getValue(SI.MILLIMETER));
        UnitFormat.getInstance().label(unit, unitName);
        coord.setUnits(SI.MILLIMETER, unit);
        if (viewer!=null) viewer.update(coord, new String[]{"Value","Unit"});
        return unit;
	}

	public void setIntensityValues(IImageTrace image) throws Exception {
		
		if (image==null)  return;
		if (isDisposed)   return;
		max.setDefault(image.getImageServiceBean().getMax().doubleValue(), Dimensionless.UNIT);
		min.setDefault(image.getImageServiceBean().getMin().doubleValue(), Dimensionless.UNIT);

		IImageService service = (IImageService)ServiceManager.getService(IImageService.class);
		float[] fa = service.getFastStatistics(image.getImageServiceBean());
		mean.setDefault(fa[2], Dimensionless.UNIT);
        mean.setLabel(image.getImageServiceBean().getHistogramType().getLabel());
	}

	public LabelNode getRoot() {
		return root;
	}

	public void dispose() {
		if (nodeMap!=null) for (TreeNode node : nodeMap.values()) {
			if (node instanceof LabelNode) {
				((LabelNode)node).dispose();
			}
		}
		
	    final DetectorProperties detprop = getDetectorProperties();
	    		
		if (detprop!=null && beamCenterListener!=null) {
			detprop.removeDetectorPropertyListener(beamCenterListener);
		}
		nodeMap.clear();
		nodeMap = null;
		isDisposed = true;
		root   = null;
		viewer = null;
	}

	public void reset() {
		reset(root);
	}

	private void reset(TreeNode node) {
		if (node instanceof NumericNode) {
			((NumericNode)node).reset();
		}
		for (int i = 0; i < node.getChildCount(); i++) {
			reset(node.getChildAt(i));
		}
	}

	public TreeViewer getViewer() {
		return viewer;
	}

	public void setViewer(TreeViewer viewer) {
		this.viewer = viewer;
	}
}
