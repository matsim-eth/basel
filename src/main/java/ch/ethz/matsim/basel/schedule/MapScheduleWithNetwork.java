package ch.ethz.matsim.basel.schedule;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.pt2matsim.config.PublicTransitMappingConfigGroup;
import org.matsim.pt2matsim.run.CheckMappedSchedulePlausibility;
import org.matsim.pt2matsim.run.CreateDefaultPTMapperConfig;
import org.matsim.pt2matsim.run.PublicTransitMapper;

/*
 * This class maps the schedule and network together as well as some pre-processing to match the scenario's area.
 */
public class MapScheduleWithNetwork {
	
	static String schedulePath = "resources/schedule/";
	static String networkPath = "resources/network/";
	static String inputNetwork = "output/basel_IVT_network.xml.gz";
	static String outputMappedNetwork = "output/Mapped_basel_IVT_network.xml.gz";
	static String outputNetworkStreetOnly = "output/Mapped_basel_IVT_network_StreetOnly.xml.gz";
	static String inputSchedule1 = "output/Unmapped_Basel_IVT_Schedule.xml";
	static String inputSchedule2 = "input/hafasSchedule.xml";
	static String outputUnmappedSchedule = "output/GVMB_Unmapped_Schedule.xml";
	static String outputMappedSchedule = "output/GVMB_Mapped_Schedule.xml";
	static String stopsGVMBcsv = "input/GVMB_Stops.csv";
	

	public static void main(String args[]) {
		// 1. Pre-process by merging with hafas schedule and filtering to scenario's area 
		ScheduleFixAndMerge.run(schedulePath+inputSchedule1, schedulePath+inputSchedule2, 
				schedulePath+stopsGVMBcsv, schedulePath+outputUnmappedSchedule);
		// 2. Map the schedule onto the network
		mapScheduleToNetwork();
		// 3. Do a plausibility check
		checkPlausibility();
	}

	/**
	 * 	3. The core of the PT2MATSim-package is the mapping process of the schedule to the network.
	 *
	 * 	Here as an example, the unmapped schedule of GrandRiverTransit (previously converted from GTFS) is mapped
	 * 	to the converted OSM network of the Waterloo Area, Canada.
	 */
	
	@SuppressWarnings("deprecation")
	public static void mapScheduleToNetwork() {
		// Create a mapping config:
		CreateDefaultPTMapperConfig.main(new String[]{schedulePath + "output/MapperConfig.xml"});
		// Open the mapping config and set the parameters to the required values
		// (usually done manually by opening the config with a simple editor)
		Config mapperConfig = ConfigUtils.loadConfig(
				schedulePath + "MapperConfig.xml",
				PublicTransitMappingConfigGroup.createDefaultConfig());
		mapperConfig.getModule("PublicTransitMapping").addParam("inputNetworkFile", networkPath + inputNetwork);
		mapperConfig.getModule("PublicTransitMapping").addParam("outputNetworkFile", networkPath + outputMappedNetwork);
		mapperConfig.getModule("PublicTransitMapping").addParam("outputScheduleFile", schedulePath + outputMappedSchedule);
		mapperConfig.getModule("PublicTransitMapping").addParam("outputStreetNetworkFile", networkPath + outputNetworkStreetOnly);
		mapperConfig.getModule("PublicTransitMapping").addParam("inputScheduleFile", schedulePath + outputUnmappedSchedule);
		mapperConfig.getModule("PublicTransitMapping").addParam("scheduleFreespeedModes", "rail, light_rail");
		mapperConfig.getModule("PublicTransitMapping").addParam("candidateDistanceMultiplier", "3.2"); 
		mapperConfig.getModule("PublicTransitMapping").addParam("maxTravelCostFactor", "10.0"); 
		mapperConfig.getModule("PublicTransitMapping").addParam("nLinkThreshold", "10");
		// Save the mapping config
		// (usually done manually)
		new ConfigWriter(mapperConfig).write(schedulePath + "output/MapperConfigAdjusted.xml");

		// Map the schedule to the network using the config
		PublicTransitMapper.main(new String[]{schedulePath + "output/MapperConfigAdjusted.xml"});
	}
	
	/*
	 * 	4. The PT2MATSim package provides a plausibility checker to get quick feedback on the mapping process.
	 *
	 */
	
	public static void checkPlausibility() {
		CheckMappedSchedulePlausibility.run(
				schedulePath + outputMappedSchedule,
				schedulePath + outputMappedNetwork,
				"EPSG:2056", 
				schedulePath + "PlausibilityResultsBasel/"
		);
	}

}