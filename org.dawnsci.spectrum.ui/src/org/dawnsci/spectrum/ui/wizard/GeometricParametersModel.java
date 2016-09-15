package org.dawnsci.spectrum.ui.wizard;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.swt.widgets.Text;

public class GeometricParametersModel {
	
	private Boolean beamCorrection = false;
	private Double beamInPlane = 0.3;
	private Double beamOutPlane = 0.3;
	private Double covar = 1.0;
	private Double detectorSlits = 10.0;
	private Double inPlaneSlits = 0.2;
	private Double inplanePolarisation = 0.0;
	private Double outPlaneSlits = 0.2;
	private Double outplanePolarisation =1.0;
	private Double reflectivityA = 1.0;
	private Double sampleSize = 10.0;
	private Double normalisationFactor = 10.0;
	private Boolean specular = false;
	private String imageName = "file_image";
	private String xName ="l";
	private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
	private Double ScalingFactor = 10.0;
	
	public Double getNormalisationFactor() {
		return normalisationFactor;
	}
	public void setNormalisationFactor(Double normalisationfactor) {
		this.normalisationFactor = normalisationfactor;
	}
	
	public Boolean getBeamCorrection() {
		return beamCorrection;
	}
	public void setBeamCorrection(Boolean beamCorrection) {
		this.beamCorrection = beamCorrection;
	}
	public Double getBeamInPlane() {
		return beamInPlane;
	}
	public void setBeamInPlane(Double beamInPlane) {
		this.beamInPlane = beamInPlane;
	}
	public Double getBeamOutPlane() {
		return beamOutPlane;
	}
	public void setBeamOutPlane(Double beamOutPlane) {
		this.beamOutPlane = beamOutPlane;
	}
	public Double getCovar() {
		return covar;
	}
	public void setCovar(Double covar) {
		this.covar = covar;
	}
	public Double getDetectorSlits() {
		return detectorSlits;
	}
	public void setDetectorSlits(Double detectorSlits) {
		this.detectorSlits = detectorSlits;
	}
	public Double getInPlaneSlits() {
		return inPlaneSlits;
	}
	public void setInPlaneSlits(Double inPlaneSlits) {
		this.inPlaneSlits = inPlaneSlits;
	}
	public Double getOutPlaneSlits() {
		return outPlaneSlits;
	}
	public void setOutPlaneSlits(Double outPlaneSlits) {
		this.outPlaneSlits = outPlaneSlits;
	}
	public Double getReflectivityA() {
		return reflectivityA;
	}
	public void setReflectivityA(Double reflectivityA) {
		this.reflectivityA = reflectivityA;
	}
	public Double getSampleSize() {
		return sampleSize;
	}
	public void setSampleSize(Double sampleSize) {
		this.sampleSize = sampleSize;
	}
	public Boolean getSpecular() {
		return specular;
	}
	public void setSpecular(Boolean specular) {
		this.specular = specular;
	}
	public Double getInplanePolarisation() {
		return inplanePolarisation;
	}
	public void setInplanePolarisation(Double inplanePolarisation) {
		this.inplanePolarisation = inplanePolarisation;
	}
	public Double getOutplanePolarisation() {
		return outplanePolarisation;
	}
	public void setOutplanePolarisation(Double outplanePolaristaion) {
		this.outplanePolarisation = outplanePolaristaion;
	}
	public String getImageName() {
		return imageName;
	}
	public void setImageName(String imageName) {
		this.imageName = imageName;
	}
	public String getxName() {
		return xName;
	}
	public void setxName(String xName) {
		this.xName = xName;
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
	public Double getScalingFactor() {
		return ScalingFactor;
	}
	public void setScalingFactor(Double scalingFactor) {
		ScalingFactor = scalingFactor;
	}
	
}