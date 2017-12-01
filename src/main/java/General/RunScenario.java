package General;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import ch.ethz.matsim.baseline_scenario.BaselineModule;

public class RunScenario {
	static public void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		
		int threadsGlobal = args.length > 1 ? Integer.parseInt(args[1]) : 1;
		int threadsQSIM = args.length > 2 ? Integer.parseInt(args[2]) : 1;
		
		config.global().setNumberOfThreads(threadsGlobal);
		config.qsim().setNumberOfThreads(threadsQSIM);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Controler controler = new Controler(scenario);
		
		controler.addOverridingModule(new BaselineModule());
		
		controler.run();
	}
}
