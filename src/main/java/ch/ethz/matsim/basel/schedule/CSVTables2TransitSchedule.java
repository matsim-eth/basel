package ch.ethz.matsim.basel.schedule;

import java.io.IOException;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt2matsim.tools.ScheduleTools;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class CSVTables2TransitSchedule {
	
	private CSVTables2TransitSchedule() {
	}

	public static void main(String[] args) throws IOException {
		if(args.length == 4) {
			run(args[0], args[1], args[2], args[3], args[4], args[5]);
		} else {
			String path = "H:/Basel/New PT Mapping/OldFiles/";
			String linesCSV = "lines_list2.csv";
			String stopsCSV = "stops_list_Jun03_2.csv"; 
			String vehiclesCSV = "VehicleData.csv";
			String outputCoordinateSystem = "null";
			String outputScheduleFile = "reconvertedSchedule.xml"; 
			String outputVehicleFile = "reconvertedVehicles.xml";
			run(path+linesCSV, path+stopsCSV, path+vehiclesCSV, outputCoordinateSystem, path+outputScheduleFile, path+outputVehicleFile);
			//throw new IllegalArgumentException(args.length + " instead of 6 arguments given");
		}
	}

	/**
	 * Converts all input files in and writes the output schedule and vehicles to the respective
	 * files. Stop Facility coordinates are transformed to <tt>outputCoordinateSystem</tt>.
	 */
	public static void run(String linesCSV, String stopsCSV, String vehiclesCSV, String outputCoordinateSystem, 
			String outputScheduleFile, String outputVehicleFile) throws IOException {
		TransitSchedule schedule = ScheduleTools.createSchedule();
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		CoordinateTransformation transformation = !outputCoordinateSystem.equals("null") ?
				TransformationFactory.getCoordinateTransformation("WGS84", outputCoordinateSystem) : new IdentityTransformation();

		ScheduleFromCSVConverter converter = new ScheduleFromCSVConverter(schedule, vehicles, transformation);
		converter.run(linesCSV, stopsCSV, vehiclesCSV);
		
		ScheduleTools.writeTransitSchedule(schedule, outputScheduleFile);
		ScheduleTools.writeVehicles(vehicles, outputVehicleFile);
	}

}
