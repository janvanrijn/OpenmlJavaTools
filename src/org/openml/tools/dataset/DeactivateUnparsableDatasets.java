package org.openml.tools.dataset;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.Data;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xstream.XstreamXmlMapping;

import com.thoughtworks.xstream.XStream;

import org.openml.apiconnector.xml.Data.DataSet;

public class DeactivateUnparsableDatasets {

	private static final String test_url = "https://test.openml.org/";
	private static final String live_url = "https://www.openml.org/";
	private static final String test_key_write = "8baa83ecddfe44b561fd3d92442e3319";
	private static final String live_key_read = "c1994bdb7ecb3c6f3c8f3b35f4b47f1f"; // mlr ..  sorry i borrowed it
	private static final XStream xstream = XstreamXmlMapping.getInstance();
	private static final String output_filepath = "unparsable.bout";
	
	private static final OpenmlConnector test_client_write = new OpenmlConnector(test_url, test_key_write);
	private static final OpenmlConnector live_client_read = new OpenmlConnector(live_url, live_key_read);
	
	private static final int start_did = 1;
	
	public static void main(String[] args) throws Exception {
		Map<String, String> filters = new TreeMap<String, String>();
		filters.put("status", "in_preparation");
		Data data = live_client_read.dataList(filters);
		List<Integer> unparsable = new ArrayList<>();
		
		for (DataSet current : data.getData()) {
			if (current.getDid() < start_did) { 
				continue;
			}
			DataSetDescription dsd = live_client_read.dataGet(current.getDid());
			DataSetDescription dsdNew = new DataSetDescription(dsd.getName(), dsd.getDefault_target_attribute(), dsd.getFormat(), dsd.getDefault_target_attribute());
			File dataset = live_client_read.datasetGet(dsd);
			
			try {
				test_client_write.dataUpload(dsdNew, dataset);
			} catch(ApiException api) {
				if (api.getCode() == 145) {
					unparsable.add(current.getDid());
					Conversion.log("Warning", "Upload", "Found unparsable. Current list: " + unparsable);
					
				} else {
					Conversion.log("Warning", "Upload", "API Problem uploading dataset " + current.getDid() + ": " + api.getMessage());
				}
			} catch(Exception e) {
				Conversion.log("Warning", "Upload", "Problem uploading dataset " + current.getDid() + ": " + e.getMessage());
			}
		}
		
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(output_filepath));
		oos.writeObject(unparsable);
		oos.flush();
		oos.close();
	}
}
