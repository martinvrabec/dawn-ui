package org.dawnsci.spectrum.ui.wizard;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.january.dataset.Dataset;
import org.eclipse.january.dataset.DatasetFactory;
import org.eclipse.january.dataset.IDataset;

import uk.ac.diamond.scisoft.analysis.io.DataSetProvider;

public class DataModel {
	
	private ArrayList<Double> xList;
	private ArrayList<Double> yList;
	private ArrayList<Double> zList;
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	private ArrayList<IDataset> outputDatArray;
	private ArrayList<IDataset> backgroundDatArray;
	private IDataset slicerBackground;
	
	public ArrayList<IDataset> getOutputDatArray() {
		return outputDatArray;
	}
	public void setOutputDatArray(ArrayList<IDataset> outputDatArray) {
		this.outputDatArray = outputDatArray;
	}
	
	public ArrayList<IDataset> getBackgroundDatArray() {
		return backgroundDatArray;
	}
	public void setBackgroundDatArray(ArrayList<IDataset> backgroundDatArray) {
		this.backgroundDatArray = backgroundDatArray;
	}
	
	public void addOutputDatArray(IDataset in){
		if (outputDatArray==null){
			outputDatArray = new ArrayList<IDataset>();
		}
		
		ArrayList<IDataset> outputDatArray1 = new ArrayList<IDataset>();
		outputDatArray1 = (ArrayList<IDataset>) outputDatArray.clone();
		outputDatArray1.add(in);
		firePropertyChange("outputDatArray", this.outputDatArray,
				this.outputDatArray= outputDatArray1);
	}
	
	public void addBackgroundDatArray(IDataset in){
		if (backgroundDatArray==null){
			backgroundDatArray = new ArrayList<IDataset>();
		}
		
		ArrayList<IDataset> backgroundDatArray1 = new ArrayList<IDataset>();
		backgroundDatArray1 = (ArrayList<IDataset>) backgroundDatArray.clone();
		backgroundDatArray1.add(in);
		firePropertyChange("backgroundDatArray", this.backgroundDatArray,
				this.backgroundDatArray= backgroundDatArray1);
	}
	
	
	
	
	public ArrayList<Double> getzList() {
		return zList;
		
	}
	public void setzList(ArrayList<Double> zList) {
		this.zList = zList;
		firePropertyChange("zList", this.zList,
				this.zList= zList);
	}
	public ArrayList<Double> getyList() {
		return yList;
	}
	public void setyList(ArrayList<Double> yList) {
		this.yList = yList;
		firePropertyChange("yList", this.yList,
				this.yList= yList);
	}
	public ArrayList<Double> getxList() {
		return xList;
		
	}
	
	public void setxList(ArrayList<Double> xList) {
		this.xList = xList;
		firePropertyChange("xList", this.xList,
				this.xList= xList);
	}
	
	public void addyList(double y){
		if (yList==null){
			yList = new ArrayList<Double>();
		}
		System.out.println("Hey, ylist got added to.");
		ArrayList<Double> yList1 = new ArrayList<Double>();
		yList1 = (ArrayList<Double>) yList.clone();
		yList1.add(y);
		firePropertyChange("yList", this.yList,
				this.yList= yList1);
		
		System.out.println("Hey, ylist length is :  " +yList.size());
	}


	public void addxList(double x){
		if (xList==null){
			xList = new ArrayList<Double>();
		}
		ArrayList<Double> xList1 = new ArrayList<Double>();
		xList1 = (ArrayList<Double>) xList.clone();
		xList1.add(x);
		firePropertyChange("xList", this.xList,
				this.xList= xList1);
	}
	

	public void addzList(double z){
		if (zList==null){
			zList = new ArrayList<Double>();
		}
		ArrayList<Double> zList1 = new ArrayList<Double>();
		zList1 = (ArrayList<Double>) zList.clone();
		zList1.add(z);
		firePropertyChange("zList", this.zList,
				this.zList= zList1);
	}
	
	public void resetX(){
		xList =null;
	}
	public void resetY(){
		yList =null;
	}
	public void resetZ(){
		zList =null;
	}
	
	public void resetAll(){
		zList =null;
		xList =null;
		yList =null;
		outputDatArray =null;
		backgroundDatArray = null;
	}

	public IDataset yIDataset(){
		if (yList==null){
			yList = new ArrayList<Double>();
		}
		IDataset yOut = DatasetFactory.createFromList(yList);
		return yOut;
	}
	
	public IDataset xIDataset(){
		if (xList==null){
			xList = new ArrayList<Double>();
		}
		IDataset xOut = DatasetFactory.createFromList(xList);
		return xOut;
	}
	
	public IDataset backupDataset(){
		
		IDataset backup = DatasetFactory.createRange(0, 200, 1, Dataset.FLOAT64);
		return backup;
	}
	
	public ArrayList<Double> backupList(){
		
		IDataset backup1 = DatasetFactory.createRange(0, 200, 1, Dataset.FLOAT64);
		ArrayList<Double> backup = new ArrayList<>();
		
		for(int i=0; i<200;i++){
			backup.add((double) backup1.getInt(i));
		}

		return backup;
	}
	
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		propertyChangeSupport.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		propertyChangeSupport.removePropertyChangeListener(propertyName,
				listener);
	}

	protected void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
		propertyChangeSupport.firePropertyChange(propertyName, oldValue,
				newValue);
	}
	public IDataset getSlicerBackground() {
		return slicerBackground;
	}
	public void setSlicerBackground(IDataset slicerBackground) {
		this.slicerBackground = slicerBackground;
	}
	
	
}
