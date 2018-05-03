package ch.ethz.matsim.basel.schedule;

import java.io.IOException;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt2matsim.tools.ScheduleTools;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class CsvTables2TransitSchedule {
	
	private CsvTables2TransitSchedule() {
	}

	public static void main(String[] args) throws IOException {
		if(args.length == 4) {
			run(args[0], args[1], args[2], args[3], args[4], args[5]);
		} else {
			try {
				String path = "resources/schedule/";
				String linesCSV = "input/lines_list2016.csv";
				String stopsCSV = "input/stops_list2016.csv"; 
				String vehiclesCSV = "input/VehicleData.csv";
				String outputCoordinateSystem = "null"; // set to null if no conversion is needed
				String outputScheduleFile = "output/Unmapped_Basel_IVT_Schedule.xml"; 
				String outputVehicleFile = "output/Unmapped_Basel_IVT_Vehicles.xml";
				run(path+linesCSV, path+stopsCSV, path+vehiclesCSV, outputCoordinateSystem, path+outputScheduleFile, path+outputVehicleFile);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Wrong number of arguments or default files not found.");
			}
		}
	}

	/*
	 * Converts all input files in and writes the output schedule and vehicles to the respective
	 * files. 
	 */
	public static void run(String linesCSV, String stopsCSV, String vehiclesCSV, String outputCoordinateSystem, 
			String outputScheduleFile, String outputVehicleFile) throws IOException {
		TransitSchedule schedule = ScheduleTools.createSchedule();
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		CoordinateTransformation transformation = !outputCoordinateSystem.equals("null") ?
				TransformationFactory.getCoordinateTransformation("WGS84", outputCoordinateSystem) : new IdentityTransformation();

		ScheduleFromCsvConverter converter = new ScheduleFromCsvConverter(schedule, vehicles, transformation);
		converter.run(linesCSV, stopsCSV, vehiclesCSV);
		
		ScheduleTools.writeTransitSchedule(schedule, outputScheduleFile);
		ScheduleTools.writeVehicles(vehicles, outputVehicleFile);
	}

}
