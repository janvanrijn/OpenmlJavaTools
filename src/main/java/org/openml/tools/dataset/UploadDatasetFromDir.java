package org.openml.tools.dataset;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.xml.Data;
import org.openml.apiconnector.xml.Data.DataSet;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.Study;
import org.openml.apiconnector.xml.TaskInputs;
import org.openml.apiconnector.xml.TaskInputs.Input;
import org.openml.webapplication.ProcessDataset;
import org.openml.weka.io.OpenmlWekaConnector;

public class UploadDatasetFromDir {
	
	private static final String SERVER = "https://test.openml.org/";
	private static final String APIKEY = "d488d8afd93b32331cf6ea9d7003d4c3";
	private static final OpenmlWekaConnector openml = new OpenmlWekaConnector(SERVER, APIKEY);
	
	private static final File DIRECTORY = new File("/home/janvanrijn/data/arff_forex");
	private static final String DESCRIPTION = "**Source**: Dukascopy Historical Data Feed https://www.dukascopy.com/swiss/english/marketwatch/historical/\n" + 
			"**Edited by**: Fabian Schut\n" + 
			" \n" + 
			"# Data Description\n" +
			"This is the historical price data of the FOREX $pair from Dukascopy.\n" + 
			"One instance (row) is one candlestick of one $interval.\n" + 
			"The whole dataset has the data range from 1-1-2018 to 13-12-2018 and does not include the weekends, since the FOREX is not traded in the weekend.\n" + 
			"The timezone of the feature Timestamp is Europe/Amsterdam.\n" + 
			"The class attribute is the direction of the mean of the $type_Bid and the $type_Ask of the following $interval,\n" + 
			"relative to the $type_Bid and $type_Ask mean of the current minute.\n" + 
			"This means the class attribute is True when the mean $type price is going up the following $interval,\n" + 
			"and the class attribute is False when the mean $type price is going down (or stays the same) the following $interval.\n" + 
			"$note" +
			"# Attributes \n" +
			"`Timestamp`: The time of the current data point (Europe/Amsterdam)\n" +
			"`Bid_Open`: The bid price at the start of this time interval\n" +
			"`Bid_High`: The highest bid price during this time interval\n" +
			"`Bid_Low`: The lowest bid price during this time interval\n" +
			"`Bid_Close`: The bid price at the end of this time interval\n" +
			"`Bid_Volume`: The number of times the Bid Price changed within this time interval\n" +
			"`Ask_Open`: The ask price at the start of this time interval\n" +
			"`Ask_High`: The highest ask price during this time interval\n" +
			"`Ask_Low`: The lowest ask price during this time interval\n" +
			"`Ask_Close`: The ask price at the end of this time interval\n" +
			"`Ask_Volume`: The number of times the Ask Price changed within this time interval\n" +
			"`Class`: Whether the average price will go up during the next interval";
	
	public static void main(String[] args) throws Exception {
		//uploadData();
		//makeTasks();
		addToStudy(219);
	}
	
	private static void uploadData() throws Exception {
		Map<String, String> filters = new HashMap<String, String>();
		filters.put("tag", "forex");
		
		Map<String, Integer> dataNameId = new HashMap<String, Integer>();
		try {
			Data data = openml.dataList(filters);
			for (DataSet ds : data.getData()) {
				dataNameId.put(ds.getName(), ds.getDid());
			}
		} catch(ApiException e) {}
		
		int counter = 0;
		for (File dataset : DIRECTORY.listFiles()) {
			// assemble some basic properties
			String nameVanilla = dataset.getName().split("\\.")[0];
			Triple<String, String, String> nameParts = splitName(nameVanilla);
			
			String note = nameParts.getRight().contentEquals("Close") ? "" : "**Note that this is a hypothetical task, meant for scientific purposes only. Realistic trade strategies can only be applied to predictions on 'Close'-attributes (also available).\n"; 
			String description = DESCRIPTION.replace("$pair", nameParts.getLeft().toUpperCase()).replace("$interval", nameParts.getMiddle()).replace("$type", nameParts.getRight()).replace("$note", note);
			
			DataSetDescription dsd = new DataSetDescription(nameVanilla, description, "arff", "Class");
			final String[] TAGS = {"finance", "forex", "forex_" + nameParts.getMiddle().toLowerCase(), "forex_" + nameParts.getRight().toLowerCase()};
			for (String tag : TAGS) {
				dsd.addTag(tag);
			}
			
			Conversion.log("OK", "Upload", "Uploading " + nameVanilla + "(" + ++counter + " / " + DIRECTORY.listFiles().length + ")");
			
			// obtain data id, skip if it was already uploaded
			int did;
			if (dataNameId.containsKey(nameVanilla)) {
				did = dataNameId.get(nameVanilla);
				Conversion.log("OK", "Skip", "Skipping data upload of " + nameVanilla + " - did = " + did);
				continue;
			} else {
				did = openml.dataUpload(dsd, dataset);
			}
			
			// automatically processes and activates dataset. catch in case the dataset is already activated
			try {
				new ProcessDataset(openml, did, null);
			} catch(ApiException e) {}
		}
	}
	
	private static void makeTasks() throws Exception {
		Map<String, String> filters = new HashMap<String, String>();
		filters.put("tag", "forex");
		Data data = openml.dataList(filters);
		
		for (DataSet ds : data.getData()) {
			Triple<String, String, String> nameParts = splitName(ds.getName());
			final String[] TAGS = {"finance", "forex", "forex_" + nameParts.getMiddle().toLowerCase(), "forex_" + nameParts.getRight().toLowerCase()};
			Input[] inputs = {
				new Input("source_data", "" + ds.getDid()),
				new Input("estimation_procedure", "28"),
				new Input("target_feature", "Class"),
			};
			TaskInputs ti = new TaskInputs(null, 1, inputs, TAGS);
			// obtain task id, catch if it was already uploaded
			try {
				openml.taskUpload(ti);
			} catch(ApiException ae) {
				int taskId = Integer.parseInt(ae.getMessage().replaceAll("[\\D]", ""));
				Conversion.log("OK", "Skip", "Skipping task creation of dataset " + ds.getDid() + "; task id = " + taskId);
				/*Task t = openml.taskGet(taskId);
				Set<String> taskTags = new TreeSet<String>(Arrays.asList(t.getTags()));
				Set<String> allTags = new TreeSet<String>(Arrays.asList(TAGS));
				if (!taskTags.equals(allTags)) {
					allTags.removeAll(taskTags);
					for (String tag : allTags) {
						try {
							openml.taskTag(taskId, tag);
						} catch(ApiException ae2) {
							
						}
					}
				}*/
			}
		}
	}
	
	private static void addToStudy(Integer studyId) throws Exception {
		Study s = openml.studyGet(studyId);
		Map<String, String> filters = new HashMap<String, String>();
		filters.put("tag", "forex");
		List<Integer> taskIds = new ArrayList<Integer>(Arrays.asList(openml.taskList(filters).getTaskIds()));
		taskIds.removeAll(Arrays.asList(s.getTasks()));
		openml.studyAttach(studyId, taskIds);
	}
	
	private static void generateStudy() throws Exception {
		Map<String, String> filters = new HashMap<String, String>();
		filters.put("tag", "forex");
		Set<Integer> taskIds = new HashSet<Integer>(Arrays.asList(openml.taskList(filters).getTaskIds()));
		System.out.println(taskIds.size());
		Integer[] taskIdsArray = taskIds.toArray(new Integer[taskIds.size()]);
		Study s = new Study("FOREX", "Forex", "Contains currency trading tasks, for various valuta pairs.", null, taskIdsArray, null);
		int studyId = openml.studyUpload(s);
		System.out.println("study id: " + studyId);
	}
	
	private static Triple<String, String, String> splitName(String nameVanilla) {
		String[] nameParts = nameVanilla.split("_")[1].split("-");
		String pair = nameParts[0].substring(0, 3) + '/' + nameParts[0].substring(3);
		
		return new ImmutableTriple<String, String, String>(pair, nameParts[1], nameParts[2]);
	}
}
