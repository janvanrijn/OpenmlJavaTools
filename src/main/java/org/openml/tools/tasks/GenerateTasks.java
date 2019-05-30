package org.openml.tools.tasks;

import java.util.ArrayList;
import java.util.List;

import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.TaskInputs;
import org.openml.apiconnector.xml.TaskInputs.Input;

public class GenerateTasks {
	
	public static void main(String[] args) throws Exception {
		OpenmlConnector connector = new OpenmlConnector("https://www.openml.org/", "48830dd663e41d5cb689016a072e6ec1");
		connector.setVerboseLevel(1);
		List<Integer> dataset_ids = new ArrayList<Integer>();
		dataset_ids.add(41496);

		Integer[] tt1_ep = {1, 2, 3, 4, 5, 6, 26};
		generateTaskType(1, dataset_ids, tt1_ep, connector);
	}
	
	public static void generateTaskType(Integer ttid, List<Integer> dataset_ids, 
			Integer[] estimation_procedures, 
			OpenmlConnector connector) throws Exception {
		
		for (int did : dataset_ids) {
			DataSetDescription dsd = connector.dataGet(did);
			for (int ep : estimation_procedures) {
				Input source_data = new Input("source_data", "" + did);
				Input estimation_procedure = new Input("estimation_procedure", "" + ep);
				Input target_feature = new Input("target_feature", dsd.getDefault_target_attribute());
				Input[] inputs = {source_data, estimation_procedure, target_feature};
				
				TaskInputs task = new TaskInputs(null, ttid, inputs, null);
				try {
					connector.taskUpload(task);
				} catch(ApiException e) {
					if (e.getCode() != 533) {
						throw e;
					}
				}
			}
		}
	}
}
