package dbicudo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.boescpa.lib.tools.fileCreation.F2LConfigGroup;
import playground.boescpa.lib.tools.spatialCutting.ScheduleCutter;

public class PTScheduleFilter {
	
	Scenario scenario = null;
	static TransitSchedule schedule = null;
	String pathToFiles = "C:/Users/davig/Documents/Basel/PT Schedules/SchedulesFilter/"; //change
	
	public PTScheduleFilter() {
		final String configName = "filterConfig.xml";
		Config config = ConfigUtils.loadConfig(pathToFiles+configName, new F2LConfigGroup());
		scenario = ScenarioUtils.loadScenario(config);
		schedule = scenario.getTransitSchedule();
		reportWriter("LineName;LineID;RouteDesc;RouteID;TransportMode;StopName;StopID;StopAttr;X_Coord;Y_Coord;isBlocking\n");
	}

	public static void main(String[] args) {
		PTScheduleFilter sf = new PTScheduleFilter();
		
		// **************** Cut Schedule ****************
		Coord cutCenter = new Coord(2611900.0, 1265300.0);
		int cutRadius = 10000;
		TransitSchedule cuttedSchedule = schedule;
		new ScheduleCutter(cuttedSchedule, null, cutCenter, cutRadius).cutSchedule();
		
		sf.filter(cuttedSchedule);
	}
	
	public void filter(TransitSchedule scheduleToFilter){
		for (TransitLine line : scheduleToFilter.getTransitLines().values()) {
			for (TransitRoute transitRoute : line.getRoutes().values()) {
				for (TransitRouteStop routeStop : transitRoute.getStops()){
					String report = 
									line.getName() + ";" + 
									line.getId() + ";" +
									transitRoute.getDescription() + ";" +
									transitRoute.getId() + ";" +
									transitRoute.getTransportMode() + ";" +
									routeStop.getStopFacility().getName() + ";" +
									routeStop.getStopFacility().getId() + ";" +
									routeStop.getStopFacility().getCustomAttributes() + ";" +
									routeStop.getStopFacility().getCoord().getX() + ";" +
									routeStop.getStopFacility().getCoord().getY() + ";" +
									routeStop.getStopFacility().getIsBlockingLane() + "\n";
					try {
					    Files.write(Paths.get(pathToFiles+"report.csv"), report.getBytes(), StandardOpenOption.APPEND);
					}catch (IOException e) {
					    e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void reportWriter(String input) {
        try {
            File statText = new File(pathToFiles+"report.csv");
            FileOutputStream is = new FileOutputStream(statText);
            OutputStreamWriter osw = new OutputStreamWriter(is);    
            Writer w = new BufferedWriter(osw);
            w.write(input);
            w.close();
        } catch (IOException e) {
            System.err.println("Problem writing report.");
        }
    }

}
