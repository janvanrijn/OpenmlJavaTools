package org.openml.tools.tasks;

import java.util.HashSet;
import java.util.Set;

import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.settings.Config;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.TaskInputs;
import org.openml.apiconnector.xml.TaskInputs.Input;
import org.openml.webapplication.ProcessDataset;
import org.openml.weka.io.OpenmlWekaConnector;

public class DidToTaskId {

	private static Set<Integer> taskIds = new HashSet<Integer>();
	
	public static void main(String[] args) throws Exception {
		Config c = new Config();
		OpenmlWekaConnector openmlConnector = new OpenmlWekaConnector( "https://test.openml.org/", "d488d8afd93b32331cf6ea9d7003d4c3" );
		
		// Study s = openmlConnector.studyGet(14, "data");
		//Integer[] dids = {44327};
		
		//System.out.println("searching for: " + s.getDataset().length);
		for (int did = 1; did <= 100; ++ did) {
			DataSetDescription dsd = openmlConnector.dataGet(did);
			try {
				openmlConnector.dataQualities(dsd.getId(), 1);
				openmlConnector.dataFeatures(dsd.getId());
			} catch(ApiException e) {
				try {
					new ProcessDataset(openmlConnector, did, null);
				} catch(Exception ee) {
					continue;
				}
			}
			
			Input estimation_procedure = new Input("estimation_procedure", "28");
			Input data_set = new Input("source_data", did + "");
			Input target_feature = new Input("target_feature", dsd.getDefault_target_attribute());
			//Input evaluation_measure = new Input("evaluation_measures", "predictive_accuracy");
			Input[] inputs = {estimation_procedure, data_set, target_feature};
			
			TaskInputs task = new TaskInputs(null, 1, inputs, null); 
			
			try {
				int taskId = openmlConnector.taskUpload(task);
				taskIds.add(taskId);
			} catch(ApiException e) {
				System.out.println(e.getMessage());
				String id = e.getMessage().substring(e.getMessage().indexOf('[') + 1, e.getMessage().indexOf(']'));
				taskIds.add(Integer.parseInt(id));	
			}
		}
		System.out.println(taskIds.size() + ": " + taskIds);
	}
}
