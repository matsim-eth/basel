package ch.matsim.basel.schedule;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt2matsim.hafas.HafasConverter;
import org.matsim.pt2matsim.hafas.lib.BitfeldAnalyzer;
import org.matsim.pt2matsim.hafas.lib.FPLANReader;
import org.matsim.pt2matsim.hafas.lib.FPLANRoute;
import org.matsim.pt2matsim.hafas.lib.OperatorReader;
import org.matsim.pt2matsim.hafas.lib.StopReader;
import org.matsim.pt2matsim.tools.debug.ScheduleCleaner;
import org.matsim.vehicles.Vehicles;

import com.opencsv.CSVReader;

public class ScheduleFromCSV {
	
	private TransitSchedule schedule = null;
	private CoordinateTransformation transformation = null; 
	private Vehicles vehicles = null;
	private String prefixString = null;
	private Map<Coord, String> usedCoordinates = new HashMap<>();
	private TransitScheduleFactory scheduleBuilder;
	
	public ScheduleFromCSV(TransitSchedule schedule, Vehicles vehicles, CoordinateTransformation transformation, String prefix) {
		this.schedule = schedule;
		this.vehicles = vehicles;
		this.transformation = transformation;
		this.prefixString = prefix;
		this.scheduleBuilder = this.schedule.getFactory();
	}
	
	public ScheduleFromCSV(TransitSchedule schedule, Vehicles vehicles) {
		this.schedule = schedule;
		this.vehicles = vehicles;
		this.scheduleBuilder = this.schedule.getFactory();
	}
	
	protected static Logger log = Logger.getLogger(ScheduleFromCSV.class);

	public void run(String linesCSV, String stopsCSV) throws IOException {
		log.info("Creating the schedule based on CSV tables...");

		// 1. Read and create stop facilities
		log.info("  Read transit stops...");
		stopReader(stopsCSV);
		log.info("  Read transit stops... done.");

		// 4. Create all lines from HAFAS-Schedule
		log.info("  Read transit lines...");
		List<FPLANRoute> routes = FPLANReader.parseFPLAN(bitfeldNummern, operators, hafasFolder + "FPLAN");
		log.info("  Read transit lines... done.");

		log.info("  Creating transit routes...");
		createTransitRoutesFromFPLAN(routes, schedule, vehicles);
		log.info("  Creating transit routes... done.");

		// 5. Clean schedule
		ScheduleCleaner.removeNotUsedStopFacilities(schedule);
		ScheduleCleaner.combineIdenticalTransitRoutes(schedule);
		ScheduleCleaner.cleanDepartures(schedule);
		ScheduleCleaner.cleanVehicles(schedule, vehicles);

		log.info("Creating the schedule based on HAFAS... done.");
	}

	private void stopReader(String stopsCSV) {
		CSVReader reader = null;
		try {
            reader = new CSVReader(new FileReader(stopsCSV));
            String[] newLine;
            while ((newLine = reader.readNext()) != null) {
            	Id<TransitStopFacility> stopId = Id.create(this.prefixString+newLine[0], TransitStopFacility.class);
				double xCoord = Double.parseDouble(newLine[3]);
				double yCoord = Double.parseDouble(newLine[4]);
				Coord coord = new Coord(xCoord, yCoord);
				if (this.transformation != null) {
					coord = this.transformation.transform(coord);
				}
				String stopName = newLine[2];
				createStop(stopId, coord, stopName);
			}
		log.info("  Read transit stops... done.");
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	private void createStop(Id<TransitStopFacility> stopId, Coord coord, String stopName) {

		//check if coordinates are already used by another facility
		String check = usedCoordinates.put(coord, stopName);
		if(check != null && !check.equals(stopName)) {
			if(check.contains(stopName) || stopName.contains(check)) {
				log.info("Two stop facilities at " + coord + " \"" + check + "\" & \"" + stopName + "\"");
			} else {
				log.warn("Two stop facilities at " + coord + " \"" + check + "\" & \"" + stopName + "\"");
			}
		}

		TransitStopFacility stopFacility = this.scheduleBuilder.createTransitStopFacility(stopId, coord, false);
		stopFacility.setName(stopName);
		this.schedule.addStopFacility(stopFacility);
	}
}
