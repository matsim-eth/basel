package ch.ethz.matsim.basel.facilities;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

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
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.FacilitiesReaderMatsimV1;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.OpeningTime;
import org.matsim.facilities.OpeningTimeImpl;

public class CreateFactFacilities {

	private final static Logger log = Logger.getLogger(CreateFactFacilities.class);
	
	private static Scenario scenario;
	private static Network network;
	
	private final static String facilitiesPath = "resources/facilities/";
	private final static String networkPath = "resources/network/";
	private final static String outputFacilities = facilitiesPath + "output/facilities.xml.gz";
	private final static String factFacilitiesFile = facilitiesPath + "input/cdata.csv";
	private final static CoordinateTransformation transformation = 
			TransformationFactory.getCoordinateTransformation("WGS84", "CH1903_LV03_Plus");
	private final static String networkFile = networkPath + "output/Mapped_basel_IVT_network.xml.gz";
	
	public static void main(String[] args) {
		CreateFactFacilities facilitiesCreator = new CreateFactFacilities();
		facilitiesCreator.init();
		facilitiesCreator.run();
		new FacilitiesWriter(scenario.getActivityFacilities()).write(outputFacilities);
		// to test if the output is loadable
		Config config = ConfigUtils.createConfig();
		Scenario newScenario = ScenarioUtils.createScenario(config);
		new FacilitiesReaderMatsimV1(newScenario).readFile(outputFacilities);
		log.info("Creation finished #################################");
	}
	
	private void init() {
		/*
		 * Create the scenario
		 */
		Config config = ConfigUtils.createConfig();
		CreateFactFacilities.scenario = ScenarioUtils.createScenario(config);
		network = scenario.getNetwork();
		new MatsimNetworkReader(network).readFile(networkFile);
	}
	
	private void run() {
		/*
		 * Read the fact facilities extracted data
		 */
		this.readfactData();
	
	}

	private void readfactData () {
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(factFacilitiesFile));
			bufferedReader.readLine(); //skip header
			
			// indexes
			int index_UID = 2;
			int index_xCoord = 7;
			int index_yCoord = 8;
			int index_factId = 9;
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
					desc += parts[index_categories].replace("[", "").replace("]", "").replace("u'", "").replace("u\"", "").replace("'", "").replace("\"", "") + " (" + parts[index_factId] + ").";
					facility.setDesc(desc);
					
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
}