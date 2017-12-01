package ch.ethz.matsim.basel.scenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.Facility;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

public class RunCreateFacilitiesFromFactual {

	private final static Logger log = Logger.getLogger(RunCreateFacilitiesFromFactual.class);
	
	private static Scenario scenario;
	private static Network network;
	
	private final static String factualFacilitiesFile = new File("TestResources/input/cdata.csv").getAbsolutePath();
	private final static String outputFacilities = new File("TestResources/output/FactualFacilities2.xml.gz").getAbsolutePath();
	private final static CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus");
	private final static String networkFile = new File("TestResources/input/FinalNetwork2.xml.gz").getAbsolutePath();
	
	public static void main(String[] args) {
		RunCreateFacilitiesFromFactual facilitiesCreator = new RunCreateFacilitiesFromFactual();
		facilitiesCreator.init();
		facilitiesCreator.run();
		new FacilitiesWriter(scenario.getActivityFacilities()).write(outputFacilities);
		Config config = ConfigUtils.createConfig();
		Scenario newScenario = ScenarioUtils.createScenario(config);
		new FacilitiesReaderMatsimV1(newScenario).readFile(outputFacilities);
//		new FacilitiesWriter(scenario.getActivityFacilities()).write("output_facilities.xml.gz");
		log.info("Creation finished #################################");
	}
	
	private void init() {
		/*
		 * Create the scenario
		 */
		Config config = ConfigUtils.createConfig();
		RunCreateFacilitiesFromFactual.scenario = ScenarioUtils.createScenario(config);
		network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);
	}
	
	private void run() {
		/*
		 * Read the factual.com facilities extracted data
		 */
		this.readFactualData();
//		this.addLinkIds();
	
	}

	private void readFactualData () {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(factualFacilitiesFile));
			bufferedReader.readLine(); //skip header
			
			// indexes
			int index_UID = 2;
			int index_xCoord = 7;
			int index_yCoord = 8;
			int index_factualId = 9;
			int index_categories = 11;
			int index_activityTypes[] = {19, 20, 21};
			int index_capacities[] = {22, 23, 24};
			int index_workPlaces[] = {25, 26, 27};
			int index_openingTimes[] = {28, 29, 30, 31, 32, 33, 34};
			ArrayList<String> activityTypes = new ArrayList<String>();
			ArrayList<String> capacities = new ArrayList<String>();
			ArrayList<String> workPlaces = new ArrayList<String>();
			ArrayList<String> openingTimes = new ArrayList<String>();
			String desc = "";
			
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split(";");
				
				// get opening times for the facility, starts looking from monday and loops up to sunday to find the needed data.
				for (int index : index_openingTimes) openingTimes.add(parts[index]);
				ArrayList<OpeningTime> oTimes = new ArrayList<OpeningTime>(); 
				for(String weekDay : openingTimes){
					if(!weekDay.equals("NA") && oTimes.isEmpty()){
						oTimes = createOpeningTimes(weekDay);
					}
				}
				
				if(!oTimes.isEmpty()){
					// get coord.
					Coord coord = transformation.transform(new Coord(Double.parseDouble(parts[index_xCoord]), Double.parseDouble(parts[index_yCoord])));
					// create and add facility
					ActivityFacilityImpl facility = (ActivityFacilityImpl)scenario.getActivityFacilities().getFactory().createActivityFacility(Id.create(String.format("%06d", Integer.parseInt(parts[index_UID])), ActivityFacility.class), coord);
					scenario.getActivityFacilities().addActivityFacility(facility);
					// Lists for future reference
					for (int index : index_activityTypes) activityTypes.add(parts[index]);
					for (int index : index_capacities) capacities.add(parts[index]);
					for (int index : index_workPlaces) workPlaces.add(parts[index]);
					
					// creates activity options and adds to facility
					for(int i = 0; i < activityTypes.size(); i++) {
						if(!activityTypes.get(i).equals("NA")){
							if(Collections.frequency(activityTypes, activityTypes.get(i)) == 1){
								ActivityOptionImpl actOption = new ActivityOptionImpl(activityTypes.get(i));
								actOption.setCapacity(Double.parseDouble(capacities.get(i)));
								desc += "wp: " + workPlaces.get(i) + " (" + activityTypes.get(i) + "). / ";
								actOption.setOpeningTimes(oTimes);
								facility.addActivityOption(actOption);
							}else{
								// Default minimal capacity and work places is defined as 1 to avoid problematic facilities later on.
								Double totalCapacity = 0.0;
								int totalWorkPlaces = 1;
								for(int j = 0; j < activityTypes.size(); j++){
									if(activityTypes.get(j).equals(activityTypes.get(i))){
										totalCapacity += Double.parseDouble(capacities.get(j));
										try { // in some cases, where the number of facilities is greater than the workplaces in the zone, the facility has more than one activity type but just one work place.
											totalWorkPlaces += Integer.parseInt(workPlaces.get(j));
										} catch (NumberFormatException e) {}
									}
								}
								ActivityOptionImpl actOption = new ActivityOptionImpl(activityTypes.get(i));
								actOption.setCapacity(totalCapacity);
								desc += "wp: " + totalWorkPlaces + " (" + activityTypes.get(i) + "). / ";
								actOption.setOpeningTimes(oTimes);
								try {
									facility.addActivityOption(actOption);
								} catch (Exception e) {}
							}
						}
					}
					
					// Set facility description
					desc += parts[index_categories].replace("[", "").replace("]", "").replace("u'", "").replace("u\"", "").replace("'", "").replace("\"", "") + " (" + parts[index_factualId] + ").";
					facility.setDesc(desc);
					
//					System.out.println(facility.getId().toString());
					
					activityTypes.clear();
					capacities.clear();
					workPlaces.clear();
					openingTimes.clear();
					desc = "";
				}
			}
			bufferedReader.close();
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<OpeningTime> createOpeningTimes(String time) {
		ArrayList<OpeningTime> oTimes = new ArrayList<OpeningTime>();
		time = time.replace("[[u'", "").replace("']]", "");
		String times[] = time.split("'\\], \\[u'");
		for(String t : times){
			String startTime = t.split("', u'")[0];
			String endTime = t.split("', u'")[1];
			Integer sHour = Integer.parseInt(startTime.split(":")[0]);
			Integer sMinute = Integer.parseInt(startTime.split(":")[1]);
			Integer eHour = Integer.parseInt(endTime.split(":")[0]);
			Integer eMinute = Integer.parseInt(endTime.split(":")[1]);
			if(eHour*3600 + eMinute*60 > sHour*3600 + sMinute*60) oTimes.add(new OpeningTimeImpl(sHour*3600 + sMinute*60, eHour*3600 + eMinute*60));
		}
		return oTimes;
	}
	
//	private void addLinkIds() {
//		new FacilitiesReaderMatsimV1(scenario).readFile(outputFacilities);
//		ActivityFacilities facilities = scenario.getActivityFacilities();
//		
//		
//		
//		Map<Id<ActivityFacility>, ? extends ActivityFacility> facilitiesMap = new HashMap<Id<ActivityFacility>, ActivityFacility>();  
//		facilitiesMap = facilities.getFacilities();
//		
//		for (Id<ActivityFacility> key : facilitiesMap.keySet()){
//			Facility fac = facilitiesMap.get(key);
//			
//			
//
//        }
//	}
//		
//	public void write(String facilitiesOutputPath) {
//		new FacilitiesWriter(RunCreateFacilitiesFromFactual.scenario.getActivityFacilities()).write(facilitiesOutputPath);
//	}

}
