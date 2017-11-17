package General;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

import ch.ethz.matsim.baseline_scenario.BaselineModule;
import ch.ethz.matsim.baseline_scenario.scoring.BaselineScoringFunctionFactory;

public class RunScenario {
	final static String INPUT = new File("TestResources/input").getAbsolutePath();
	final static String OUTPUT = new File("TestResources/output").getAbsolutePath();
	static public void main(String[] args) {
		Config config = ConfigUtils.loadConfig(OUTPUT+"/output_config.xml");//args[0]);
		
		config.global().setNumberOfThreads(1);//Integer.parseInt(args[1]));
		config.qsim().setNumberOfThreads(1);//Integer.parseInt(args[2]));
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new BaselineModule());
		
		controler.run();
	}
}
