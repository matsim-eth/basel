package ch.ethz.matsim.basel.scenario;

import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import ch.ethz.matsim.baseline_scenario.utils.AdaptConfig;
import ch.ethz.matsim.baseline_scenario.utils.FixFacilityActivityTypes;
import ch.ethz.matsim.baseline_scenario.utils.FixLinkIds;
import ch.ethz.matsim.baseline_scenario.utils.FixShopActivities;
import ch.ethz.matsim.baseline_scenario.utils.RemoveInvalidPlans;
import ch.ethz.matsim.baseline_scenario.utils.TypicalDurationForActivityTypes;
import ch.ethz.matsim.baseline_scenario.utils.UnselectedPlanRemoval;
import ch.ethz.matsim.baseline_scenario.utils.routing.BestResponseCarRouting;

public class MakeScenario {

	private final static String simulationPath = "resources/simulation/";
	private final static String networkPath = "resources/network/";
	private final static String populationPath = "resources/population/";
	private final static String facilitiesPath = "resources/facilities/";
	
	private final static String network = networkPath + "output/Mapped_basel_IVT_network.xml.gz";
	private final static String facilities = facilitiesPath + "output/facilities.xml.gz";
	private final static String population = populationPath + "output/population.xml.gz";
	private final static String populationAttributes = populationPath + "output/population_attributes.xml.gz";
	
	static public void main(String args[]) throws Exception {
		// adjust if necessary
		double scenarioScale = 0.01;
		int numberOfThreads = 1;
		
		Config config = new AdaptConfig().run(scenarioScale, "bsl");
		new ConfigWriter(config).write(simulationPath + "input/sim_config.xml");

		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		
		new PopulationReader(scenario).readFile(population);
		new ObjectAttributesXmlReader(scenario.getPopulation().getPersonAttributes())
				.readFile(populationAttributes);
		
		new MatsimFacilitiesReader(scenario).readFile(facilities);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(network);

		// GENERAL PREPARATION AND FIXING

		// Clean network
		Set<Id<Link>> remove = scenario.getNetwork().getLinks().values().stream()
				.filter(l -> !l.getAllowedModes().contains("car")).map(l -> l.getId()).collect(Collectors.toSet());
		remove.forEach(id -> scenario.getNetwork().removeLink(id));

		new NetworkCleaner().run(scenario.getNetwork());

		for (Link link : scenario.getNetwork().getLinks().values()) {
			link.setLength(Math.max(1.0, link.getLength()));
		}

		// Set link ids for activities and facilities
		new FixLinkIds(scenario.getNetwork()).run(scenario.getActivityFacilities(), scenario.getPopulation());

		// Add missing activity types to facilities (escort, ...) and remove opening
		// times from "home"
		new FixFacilityActivityTypes().run(scenario.getActivityFacilities());

		// Some shop activities are named "shopping" ... change that!
		new FixShopActivities().apply(scenario.getPopulation());

		// Remove invalid plans (not starting or ending with "home", zero durations)
		new RemoveInvalidPlans().apply(scenario.getPopulation());

		// DEPATURE TIMES

		// Dilute departure times
//		new ShiftTimes(1800.0, random, false).apply(scenario.getPopulation());

		// LOCATION CHOICE

		// SCORING

		// Adjust activities for typical durations
		new TypicalDurationForActivityTypes().run(scenario.getPopulation(), scenario.getActivityFacilities());

		// PREPARE FOR RUNNING

		// Do best response routing with free-flow travel times
		new BestResponseCarRouting(numberOfThreads, scenario.getNetwork()).run(scenario.getPopulation());
		
		// Select plans to fit counts	
//		new TrafficCountPlanSelector(scenario.getNetwork(), countItems, scenarioScale, 0.01, numberOfThreads, "counts_locchoice.txt", 20).run(scenario.getPopulation());
		new UnselectedPlanRemoval().run(scenario.getPopulation());

		// Here we get some nice pre-initialized routes for free, because
		// the TrafficCountPlanSelector already estimates them using BPR
		
		// OUTPUT

		new PopulationWriter(scenario.getPopulation()).write(simulationPath + "input/sim_population.xml.gz");
		new ObjectAttributesXmlWriter(scenario.getPopulation().getPersonAttributes())
				.writeFile(simulationPath + "input/sim_population_attributes.xml.gz");
		new FacilitiesWriter(scenario.getActivityFacilities()).write(simulationPath + "input/sim_facilities.xml.gz");
		new NetworkWriter(scenario.getNetwork()).write(simulationPath + "input/sim_network.xml.gz");
	}
}
