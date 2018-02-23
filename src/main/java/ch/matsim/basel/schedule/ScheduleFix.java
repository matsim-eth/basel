package ch.matsim.basel.schedule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.opencsv.CSVReader;

import java.io.FileReader;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt2matsim.tools.debug.ScheduleCleaner;
import org.matsim.pt2matsim.tools.ScheduleTools;

public class ScheduleFix {
	static String path = "H:/Basel/New PT Mapping/";
	static String inputSchedule = "Basel_Schedules_Final/FinalSchedule.xml.gz";
	static String outputSchedule = "Basel_Schedules_Final/FinalSchedule_railGVMB.xml";
//	static String inputSchedule = "S-Bahn_DE/hafasSchedule.xml";
//	static String outputSchedule = "S-Bahn_DE/hafasSchedule_railGVMB.xml";
	static String stopsGVMBcsv = "GVMB_Stops.csv";
	static Set<Id<TransitStopFacility>> stopsGVMB = new HashSet<Id<TransitStopFacility>>();
	static CSVReader reader = null;
	
	public static void main(String[] args) {
		
		// populate set with GVMB stops ids
		try {
            reader = new CSVReader(new FileReader(path+stopsGVMBcsv));
            String[] line;
            while ((line = reader.readNext()) != null) {
            	stopsGVMB.add(Id.create(line[0], TransitStopFacility.class));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		TransitSchedule schedule = ScheduleTools.readTransitSchedule(path+inputSchedule);
		ScheduleCleaner.removeTransitRouteByMode(schedule, new HashSet<>(Arrays.asList("bus", "tram", "ferry")));
		ScheduleCleaner.removeMapping(schedule);
		ScheduleCleaner.cutSchedule(schedule, stopsGVMB);
//		ScheduleCleaner.cutSchedule(schedule, new Coord(2612771.061763661,1271489.005109281), 25000.0);
		ScheduleCleaner.removeNotUsedStopFacilities(schedule);
		ScheduleTools.writeTransitSchedule(schedule, path+outputSchedule);
 
    }

}
