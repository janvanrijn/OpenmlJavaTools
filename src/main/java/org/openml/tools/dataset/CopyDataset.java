package org.openml.tools.dataset;

import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.DataSetDescription;

public class CopyDataset {
	protected static final String url_test = "https://test.openml.org/";
	protected static final String url_live = "https://www.openml.org/";
	protected static final OpenmlConnector client_write_test = new OpenmlConnector(url_test, "8baa83ecddfe44b561fd3d92442e3319");
	protected static final OpenmlConnector client_read_live = new OpenmlConnector(url_live, "c1994bdb7ecb3c6f3c8f3b35f4b47f1f"); 
	
	public static void main(String[] args) throws Exception {
		int[] idsToCopy = {61,61,61,61};
		for (int id : idsToCopy) {
			DataSetDescription dsd = client_read_live.dataGet(id);
			DataSetDescription dsdNew = new DataSetDescription(null, dsd.getName(), null, "Copied from main server", null, null, dsd.getFormat(), null, null, null, dsd.getUrl(), dsd.getRow_id_attribute(), dsd.getDefault_target_attribute(), null, null, null);
			
			client_write_test.dataUpload(dsdNew, null);
		}
	}
	
}
