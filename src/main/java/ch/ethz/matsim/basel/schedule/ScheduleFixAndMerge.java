package ch.ethz.matsim.basel.schedule;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt2matsim.tools.ScheduleTools;
import org.matsim.pt2matsim.tools.debug.ScheduleCleaner;

import com.opencsv.CSVReader;

public class ScheduleFixAndMerge {
	static Set<Id<TransitStopFacility>> stopsGVMB = new HashSet<Id<TransitStopFacility>>();
	static CSVReader reader = null;
	
	public static void run(String path2InputSchedule1, 
			String path2InputSchedule2, String path2StopsFilterCsv, String path2OutputSchedule) {
		
		// populate set with GVMB stops ids
		try {
            reader = new CSVReader(new FileReader(path2StopsFilterCsv));
            String[] line;
            while ((line = reader.readNext()) != null) {
            	stopsGVMB.add(Id.create(line[2], TransitStopFacility.class));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		TransitSchedule schedule1 = ScheduleTools.readTransitSchedule(path2InputSchedule1);
		TransitSchedule schedule2 = ScheduleTools.readTransitSchedule(path2InputSchedule2);
		ScheduleTools.mergeSchedules(schedule1, schedule2);
		ScheduleCleaner.removeMapping(schedule1);
		ScheduleCleaner.cutSchedule(schedule1, stopsGVMB);
		ScheduleCleaner.removeNotUsedStopFacilities(schedule1);
		
		ScheduleTools.writeTransitSchedule(schedule1, path2OutputSchedule);
    }

}
