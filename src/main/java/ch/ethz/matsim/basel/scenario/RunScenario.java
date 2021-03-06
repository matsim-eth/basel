package ch.ethz.matsim.basel.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.baseline_scenario.BaselineModule;

public class RunScenario {
	private final static String simulationPath = "resources/simulation/";
	static public void main(String[] args) {
		Config config = ConfigUtils.loadConfig(simulationPath + "input/sim_config.xml");
		
		config.global().setNumberOfThreads(1);
		config.qsim().setNumberOfThreads(1);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new BaselineModule());
		
		controler.run();
	}
}