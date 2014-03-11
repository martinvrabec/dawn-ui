package org.dawnsci.spectrum.ui.processing;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.dawb.common.services.IPersistenceService;
import org.dawb.common.services.IPersistentFile;
import org.dawb.common.services.ServiceManager;
import org.dawnsci.spectrum.ui.file.IContain1DData;

import uk.ac.diamond.scisoft.analysis.dataset.AbstractDataset;
import uk.ac.diamond.scisoft.analysis.dataset.IDataset;

public class SaveProcess extends AbstractProcess {
	
	String path;
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}

	@Override
	public List<IContain1DData> process(List<IContain1DData> list) {
		File                file=null;
		IPersistentFile     pf=null;
		
		try {
    		IPersistenceService service = (IPersistenceService)ServiceManager.getService(IPersistenceService.class);
    		file = new File(this.path);
    		pf = service.createPersistentFile(file.getAbsolutePath());
		    
    		for (IDataset ds : list.get(0).getyDatasets()) {
		    	pf.setData(ds);
		    }
    		
		    pf.setAxes(Arrays.asList(new IDataset[] {list.get(0).getxDataset(), null}));
		        		    
		} catch (Throwable ne) {
			ne.printStackTrace();
		} finally {
			if (pf!=null) pf.close();
		}
		return null;
	}

	@Override
	protected AbstractDataset process(AbstractDataset x, AbstractDataset y) {
		return null;
	}

	@Override
	protected String getAppendingName() {
		//Should never be called
		return "_save";
	}

}