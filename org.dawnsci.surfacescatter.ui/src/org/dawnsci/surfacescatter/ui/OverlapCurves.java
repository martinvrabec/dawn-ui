package org.dawnsci.surfacescatter.ui;
import java.util.ArrayList;

import org.apache.commons.lang.time.DateFormatUtils;
import org.dawb.common.ui.widgets.ActionBarWrapper;
import org.dawnsci.surfacescatter.CurveStitchDataPackage;
import org.dawnsci.surfacescatter.IntensityDisplayEnum;
import org.dawnsci.surfacescatter.IntensityDisplayEnum.IntensityDisplaySetting;
import org.dawnsci.surfacescatter.OverlapUIModel;
import org.eclipse.dawnsci.analysis.api.roi.IROI;
import org.eclipse.dawnsci.analysis.api.roi.IRectangularROI;
import org.eclipse.dawnsci.analysis.dataset.roi.RectangularROI;
import org.eclipse.dawnsci.plotting.api.IPlottingSystem;
import org.eclipse.dawnsci.plotting.api.PlotType;
import org.eclipse.dawnsci.plotting.api.PlottingFactory;
import org.eclipse.dawnsci.plotting.api.region.IROIListener;
import org.eclipse.dawnsci.plotting.api.region.IRegion;
import org.eclipse.dawnsci.plotting.api.region.IRegion.RegionType;
import org.eclipse.dawnsci.plotting.api.region.ROIEvent;
import org.eclipse.dawnsci.plotting.api.trace.ILineTrace;
import org.eclipse.january.dataset.IDataset;
import org.eclipse.january.dataset.SliceND;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class OverlapCurves extends Composite {

    private IPlottingSystem<Composite> plotSystem;
    private IRegion[] regionArray;
    private int DEBUG =0;
    private Group controls;
    private Button errors;
    private ArrayList<ILineTrace> ltList;
	private Combo intensitySelect;
	private GeneralOverlapHandlerView gohv;
	private IRegion imageNo;
	private SurfaceScatterPresenter ssp;
	private Combo activeCurveCombo;
	private Button disregardPoint; 
	private Text yValueText;
	private Text frameNoText;
	
     
    public OverlapCurves(Composite parent, 
    					int style, 
    					ArrayList<IDataset> arrayILDy, 
    					ArrayList<IDataset> arrayILDx, 
    					String title, 
    					OverlapUIModel model,
    					GeneralOverlapHandlerView gohv,
    					SurfaceScatterPresenter ssp) {
    	
        super(parent, style);
        
        new Label(this, SWT.NONE).setText(title);
        
        this.gohv = gohv;
        this.ssp =ssp;
        
        regionArray = new IRegion[arrayILDy.size()-1];
        
        try {
			plotSystem = PlottingFactory.createPlottingSystem();
		} catch (Exception e2) {
			e2.printStackTrace();
		}
        
        this.createContents(arrayILDy, arrayILDx,  model);       
    }
     
    
    public void createContents(ArrayList<IDataset> arrayILDy, 
    						   ArrayList<IDataset> arrayILDx, 
    						   OverlapUIModel model) {
    	
    	model.getROIList().clear();
    	
    	final GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 1;
        setLayout(gridLayout);
        
        Composite setupComposite = new Composite(this, SWT.FILL);
		setupComposite.setLayout(new GridLayout());
		setupComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
        
    	SashForm form = new SashForm(setupComposite, SWT.FILL);
		form.setLayout(new GridLayout());
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		form.setOrientation(SWT.VERTICAL);
		
        controls = new Group(form, SWT.NULL);
        GridLayout controlsSelectionLayout  = new GridLayout(2,true);
		GridData controlsSelectionData = new GridData(SWT.FILL, SWT.NULL, true, false);
        controls.setLayoutData(controlsSelectionData);
        controls.setLayout(controlsSelectionLayout);

		errors = new Button(controls, SWT.PUSH);
		errors.setText("Errors");
		errors.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		InputTileGenerator displayValue = new InputTileGenerator("Display Value: ", controls);
		
		intensitySelect = displayValue.getCombo();
		
		for(IntensityDisplaySetting  t: IntensityDisplayEnum.IntensityDisplaySetting.values()){
			intensitySelect.add(IntensityDisplaySetting.toString(t));
		}
	
		intensitySelect.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		intensitySelect.select(0);
		
		
		InputTileGenerator activeCurve = new InputTileGenerator("activeCurve: ", controls);
		
		for(String datName : ssp.getDatFilepaths()){
			activeCurve.getCombo().add(datName);
		}
		
		activeCurveCombo = activeCurve.getCombo();
		activeCurveCombo.select(0);
		
		Group includeControls = new Group(controls, SWT.NULL);
        GridLayout includeControlsLayout  = new GridLayout(2,true);
		GridData includeControlsData = new GridData(SWT.FILL, SWT.NULL, true, false);
		includeControls.setLayoutData(includeControlsData);
        includeControls.setLayout(includeControlsLayout);
		
		
		disregardPoint = new Button(includeControls, SWT.PUSH);
		disregardPoint.setText("Disregard Point");
		disregardPoint.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		disregardPoint.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				int xPos = ssp.xPositionFinder(OverlapCurves.this.imageNo.getROI().getPointX());
				
				if(ssp.isGoodPoint(xPos)){
					ssp.setGoodPoint(xPos, false);
				}
				else{
					ssp.setGoodPoint(xPos, true);
				}
				
				if(ssp.isGoodPoint(xPos)){
					disregardPoint.setText("Disregard Point");
				}
				else{
					disregardPoint.setText("Include Point");
				}
				
				
				gohv.getStitchedCurves().resetAll(false);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		Button includeAll = new Button(includeControls, SWT.PUSH);
		includeAll.setText("Include All");
		includeAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		includeAll.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				ssp.allGoodPoints();
				gohv.getStitchedCurves().resetAll(false);
				disregardPoint.setText("Disregard Point");
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
		
		
		InputTileGenerator scannedPoint = new InputTileGenerator("Image Number", "0" ,controls);
		frameNoText = scannedPoint.getText();
		
		
		InputTileGenerator yPoint = new InputTileGenerator("Y Value", "0" ,controls);
		yValueText = yPoint.getText();
		
		Group unstitchedCurves = new Group(form, SWT.FILL | SWT.FILL);
        GridLayout stitchedCurvesLayout = new GridLayout(1, true);
	    GridData stitchedCurvesData = new GridData(GridData.FILL_HORIZONTAL);
	    unstitchedCurves.setLayout(stitchedCurvesLayout);
	    unstitchedCurves.setLayoutData(stitchedCurvesData);
	    unstitchedCurves.setText("Unstitched Curves");
		
        ActionBarWrapper actionBarComposite = ActionBarWrapper.createActionBars(unstitchedCurves, null);;
        
        final GridData gdSecondField = new GridData(SWT.FILL, SWT.FILL, true, true);
        gdSecondField.grabExcessVerticalSpace = true;
        gdSecondField.grabExcessVerticalSpace = true;
        
        plotSystem.createPlotPart(unstitchedCurves, "ExamplePlot", actionBarComposite, PlotType.IMAGE, null);
        
        SliceND slice = new SliceND(arrayILDy.get(0).getShape());
        
		IDataset i = null;
		IDataset j = null;
		
		ltList = new ArrayList<ILineTrace>();
		
		int r=0;
	
		for (r =0; r < arrayILDy.size(); r++){
			
			ArrayList<IDataset> arrayIDy =new ArrayList<>();
			ArrayList<IDataset> arrayIDx =new ArrayList<>();
			
			slice = new SliceND(arrayILDy.get(r).getShape());
			
			try {
				i = arrayILDy.get(r);
				arrayIDy.add(i);
				j = arrayILDx.get(r).getSlice(slice);
				arrayIDx.add(j);
			} 
			
			catch (Exception e1) {
				e1.printStackTrace();
			}
			
			ILineTrace lt = plotSystem.createLineTrace(Double.toString(arrayIDx.get(0).getDouble(0)) +"-" +Double.toString(arrayIDx.get(0).getDouble(arrayIDx.size()-1)));
			lt.setData(j, i);
			
			ltList.add(lt);
			
			plotSystem.addTrace(lt);
			plotSystem.repaint();
			
		}

       plotSystem.getPlotComposite().setLayoutData(gdSecondField);

       ArrayList<IRectangularROI> roiList = new ArrayList<IRectangularROI>();
       IRectangularROI nullROI = null;
        
        
        for(int yr =0; yr<regionArray.length;yr++){
        	roiList.add(nullROI);
        }

        model.setROIList(roiList);
        
        String root = "RegionNo:";
        int k=0;
        
        double[][] overlap = OverlapFinder.overlapFinderOperation(arrayILDx);
        
        for (k=0;k<(model.getROIList().size());k++){
        	
        	if(overlap[k][1]<999999){
        		
        		if(DEBUG ==1 ){
        			System.out.println("k in overlapCurves: " + k);
        			System.out.println("overlap[k][1]: " + overlap[k][1]);
        			System.out.println("overlap[k][0]: " + overlap[k][0]);
        			System.out.println("roiList.size() : " + roiList.size());
        		}
        		
        		roiList.set(k,  new RectangularROI(overlap[k][1],0.1,overlap[k][0]-overlap[k][1],0.1,0));

	        	String regionName = root +  Integer.toString(k);
	        
		        try {
					regionArray[k] =plotSystem.createRegion(regionName, RegionType.XAXIS);
					regionArray[k].setROI(roiList.get(k));
					plotSystem.addRegion(regionArray[k]);
					
		        }
		        catch (Exception e1) {
					e1.printStackTrace();
				}
		       
		        model.setROIList(roiList);
		        
		        int ktemp =k;
		        ((IRegion) regionArray[k]).addROIListener(new IROIListener() {
		
					@Override
					public void roiDragged(ROIEvent evt) {
						model.setROIListElementEst(regionArray[ktemp].getROI().getBounds(), ktemp);
					}
		
					@Override
					public void roiChanged(ROIEvent evt) {
						model.setROIListElementEst(regionArray[ktemp].getROI().getBounds(), ktemp);
						
					}
		
					@Override
					public void roiSelected(ROIEvent evt) {
						model.setROIListElementEst(regionArray[ktemp].getROI().getBounds(), ktemp);
					}
				
				});
        	}
        }

		try {
			imageNo = plotSystem.createRegion("Point Selector", RegionType.XAXIS_LINE);
		} catch (Exception e1) {
			
		}
		imageNo.setShowPosition(true);
		plotSystem.addRegion(imageNo);
		
		imageNoAddListener();
        
    	Group resetGroup = new Group(form, SWT.FILL | SWT.FILL);
        GridLayout resetGroupLayout = new GridLayout(1, true);
	    GridData resetGroupData = new GridData(GridData.FILL_HORIZONTAL);
	    resetGroup.setLayout(resetGroupLayout);
	    resetGroup.setLayoutData(resetGroupData);
	   
	    Button resetButton = new Button(resetGroup, SWT.PUSH);
	    resetButton.setLayoutData (new GridData(GridData.FILL_HORIZONTAL));
	    resetButton.setText("Reset Overlaps");
		
		//////////////////////////////////////////////////////////////////////////////////
		
	    resetButton.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				buildAndSetOverlapRegions(model,
						                  arrayILDx,
						                  arrayILDy);
				
				gohv.getStitchedCurves().resetAll();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				
			}
		});
		
	    form.setWeights(new int[] {15,80,5});
    }
		
   public Composite getComposite(){
   	
	   return this;
   }
   
   public IPlottingSystem<Composite> getPlotSystem(){
	   return plotSystem;
   }
   
	public Button getErrorsButton(){
		return errors;
	}
   
	public ArrayList<ILineTrace> getILineTraceList(){
		return ltList;
	}
	
	public Combo getIntensity(){
		return intensitySelect;
	}
	
	private void imageNoAddListener(){
		imageNo.addROIListener(new IROIListener() {
			
			@Override
			public void roiSelected(ROIEvent evt) {
			}
			
			@Override
			public void roiDragged(ROIEvent evt) {
			}
			
			@Override
			public void roiChanged(ROIEvent evt) {
				
				int xPos =ssp.xPositionFinder(imageNo.getROI().getPointX());
				
				int datNo = activeCurveCombo.getSelectionIndex();
				
				int xPosInDat = ssp.xPositionFinderInDat(activeCurveCombo.getSelectionIndex() ,imageNo.getROI().getPointX());
		
				frameNoText.setText(String.valueOf(xPos));
				
				double yValue = 0;
				
				int k = intensitySelect.getSelectionIndex();
				
				switch(IntensityDisplaySetting.toMethod(k)){
					case Fhkl:
						yValue = ssp.getUnsplicedFhklIntensityFromFm(activeCurveCombo.getSelectionIndex(),
								 									 xPosInDat);
					
						break;
					case Corrected_Intensity:
						yValue = ssp.getUnsplicedCorrectedIntensityFromFm(activeCurveCombo.getSelectionIndex(),
																		  xPosInDat);
						break;
					case Raw_Intensity:
						yValue = ssp.getUnsplicedRawIntensityFromFm(activeCurveCombo.getSelectionIndex(),
																	xPosInDat);
						break;
					default:
						//defensive only
				}
				
				yValueText.setText(String.valueOf(yValue));
				
				if(ssp.isGoodPoint(xPos)){
					disregardPoint.setText("Disregard Point");
				}
				else{
					disregardPoint.setText("Include Point");
				}
			}
		});
		
		
	}
	
	
	private void buildAndSetOverlapRegions(OverlapUIModel model,
										   ArrayList<IDataset> arrayILDx,
										   ArrayList<IDataset> arrayILDy){
		
		IROI imN = imageNo.getROI().copy();
		
		plotSystem.clearRegions();
		
		try {
			imageNo = plotSystem.createRegion("Point Selector", RegionType.XAXIS_LINE);
		} catch (Exception e1) {
			
		}
		
		imageNo.setROI(imN);
		
		imageNo.setShowPosition(true);
		plotSystem.addRegion(imageNo);
		
		imageNo.toFront();
		
		imageNoAddListener();
		
		ArrayList<IRectangularROI> roiList = new ArrayList<IRectangularROI>();
	    IRectangularROI nullROI = null;
	    regionArray = new IRegion[arrayILDy.size()-1];    
	        
	        for(int yr =0; yr<regionArray.length;yr++){
	        	roiList.add(nullROI);
	        }

	        model.setROIList(roiList);
	        
	        String root = "RegionNo:";
	        int k=0;
	        
	     
		 
		 double[][] overlap = OverlapFinder.overlapFinderOperation(arrayILDx);
	        
	        for (k=0;k<(model.getROIList().size());k++){
	        	
	        	if(overlap[k][1]<999999){
	        		
	        		if(DEBUG ==1 ){
	        			System.out.println("k in overlapCurves: " + k);
	        			System.out.println("overlap[k][1]: " + overlap[k][1]);
	        			System.out.println("overlap[k][0]: " + overlap[k][0]);
	        			System.out.println("roiList.size() : " + roiList.size());
	        		}
	        		
	        		
	        		roiList.set(k,  new RectangularROI(overlap[k][1],0.1,overlap[k][0]-overlap[k][1],0.1,0));

		        	String regionName = root +  Integer.toString(k);
		        
			        try {
						regionArray[k] =plotSystem.createRegion(regionName, RegionType.XAXIS);
						regionArray[k].setROI(roiList.get(k));
						plotSystem.addRegion(regionArray[k]);
						
			        }
			        catch (Exception e1) {
						e1.printStackTrace();
					}
			       
			        model.setROIList(roiList);
			        
			        int ktemp =k;
			        ((IRegion) regionArray[k]).addROIListener(new IROIListener() {
			
						@Override
						public void roiDragged(ROIEvent evt) {
							model.setROIListElementEst(regionArray[ktemp].getROI().getBounds(), ktemp);
						}
			
						@Override
						public void roiChanged(ROIEvent evt) {
							model.setROIListElementEst(regionArray[ktemp].getROI().getBounds(), ktemp);
							
						}
			
						@Override
						public void roiSelected(ROIEvent evt) {
							model.setROIListElementEst(regionArray[ktemp].getROI().getBounds(), ktemp);
						}
					
					});
	        	}
	        }

	        model.setROIListElementEst(regionArray[1].getROI().getBounds(), 1);
	}
	
	
	public void changeCurves(int selector,
							 CurveStitchDataPackage csdp){

		Display display = Display.getCurrent();
		
		Color blue = display.getSystemColor(SWT.COLOR_BLUE);
		Color green = display.getSystemColor(SWT.COLOR_GREEN);
		Color black = display.getSystemColor(SWT.COLOR_BLACK);
		
		switch(selector){
			case 0:
				
				for(int i =0; i<ltList.size();i++){
					
					ILineTrace lt = getILineTraceList().get(i);
					
					lt.setName("Corrected Curve " + i);
				
					lt.setData(csdp.getxIDataset()[i],
							   csdp.getyIDataset()[i]);
					
				
					
					lt.setTraceColor(blue);
				}
				break;
		
			case 1:
				
				for(int i =0; i<ltList.size();i++){
					
					ILineTrace lt = getILineTraceList().get(i);
					
					lt.setName("Fhkl Curve " + i);
				
					lt.setData(csdp.getxIDataset()[i],
							   csdp.getyIDatasetFhkl()[i]);
					
					
					lt.setTraceColor(green);
				}
				break;
		
			case 2:
				
				for(int i =0; i<ltList.size();i++){
					
					ILineTrace lt = getILineTraceList().get(i);
					
					lt.setName("Raw Intensity Curve " + i);
				
					lt.setData(csdp.getxIDataset()[i],
							   csdp.getyRawIDataset()[i]);
					
				
					lt.setTraceColor(black);
				}
					
				break;
				
			default:
				// Purely defensive
				break;
		}
		
	}


	public IRegion getImageNo() {
		return imageNo;
	}


	public void setImageNo(IRegion imageNo) {
		this.imageNo = imageNo;
	}
	
	public void addImageNoRegion(double j){

		RectangularROI r = new RectangularROI(j ,0.1,0,0.1,0);

		if(plotSystem.getRegion("Image")== null){
			
		try{
			imageNo = plotSystem.createRegion("Image", RegionType.XAXIS_LINE);
		}
		catch(Exception x){
			
		}
		
		
		imageNo.setShowPosition(true);
		imageNo.setROI(r);
		
		plotSystem.addRegion(imageNo);
		imageNo.setShowPosition(true);
		}
		
		else{
			moveImageNoRegion(j);
		}
	}
	
	public void moveImageNoRegion(double j){
		
		RectangularROI r = new RectangularROI(j ,0.1,0,0.1,0);
		imageNo.setROI(r);
	}
}

