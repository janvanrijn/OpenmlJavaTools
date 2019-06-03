package org.openml.tools.dataset;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
	
	private static final File DIRECTORY = new File("/Users/janvanrijn/data/arff_forex");
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
		uploadDataMakeTasks();
		generateStudy();
	}
	
	private static void uploadDataMakeTasks() throws Exception {
		Map<String, String> filters = new HashMap<String, String>();
		filters.put("tag", "forex");
		Set<String> dataNames = new HashSet<String>(Arrays.asList(openml.dataList(filters).getNames()));
		
		for (File dataset : DIRECTORY.listFiles()) {
			String nameVanilla = dataset.getName().split("\\.")[0];
			if (dataNames.contains(nameVanilla)) {
				continue;
			}
			
			String[] nameParts = nameVanilla.split("_")[1].split("-");
			String pair = nameParts[0].substring(0, 3) + '/' + nameParts[0].substring(3);
			String note = nameParts[2].contentEquals("Close") ? "" : "**Note that this is a hypothetical task, meant for scientific purposes only. Realistic trade strategies can only be applied to predictions on 'Close'-attributes (also available).\n"; 
			String description = DESCRIPTION.replace("$pair", pair.toUpperCase()).replace("$interval", nameParts[1]).replace("$type", nameParts[2]).replace("$note", note);
			
			DataSetDescription dsd = new DataSetDescription(nameVanilla, description, "arff", "Class");
			final String[] TAGS = {"finance", "forex", "forex_" + nameParts[1].toLowerCase(), "forex_" + nameParts[2].toLowerCase()};
			for (String tag : TAGS) {
				dsd.addTag(tag);
			}
			int did = openml.dataUpload(dsd, dataset);
			
			// automatically processes and activates dataset
			new ProcessDataset(openml, did, null);
			Input[] inputs = {
				new Input("source_data", "" + did),
				new Input("estimation_procedure", "28"),
				new Input("target_feature", dsd.getDefault_target_attribute()),
			};
			TaskInputs ti = new TaskInputs(null, 1, inputs, TAGS);
			openml.taskUpload(ti);
		}
	}
	
	private static void generateStudy() throws Exception {

		Map<String, String> filters = new HashMap<String, String>();
		filters.put("tag", "forex");
		Set<Integer> taskIds = new HashSet<Integer>(Arrays.asList(openml.taskList(filters).getTaskIds()));
		
		Integer[] taskIdsArray = taskIds.toArray(new Integer[taskIds.size()]);
		Study s = new Study("FOREX", "Forex", "Contains currency trading tasks, for various valuta pairs.", null, taskIdsArray, null);
		int studyId = openml.studyUpload(s);
		System.out.println("study id: " + studyId);
	}
}
