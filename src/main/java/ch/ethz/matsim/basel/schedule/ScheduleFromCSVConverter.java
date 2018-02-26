package ch.ethz.matsim.basel.schedule;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.misc.Counter;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt2matsim.hafas.HafasConverter;
import org.matsim.pt2matsim.hafas.HafasDefaults;
import org.matsim.pt2matsim.hafas.lib.BitfeldAnalyzer;
import org.matsim.pt2matsim.hafas.lib.FPLANReader;
import org.matsim.pt2matsim.hafas.lib.FPLANRoute;
import org.matsim.pt2matsim.hafas.lib.OperatorReader;
import org.matsim.pt2matsim.hafas.lib.StopReader;
import org.matsim.pt2matsim.tools.debug.ScheduleCleaner;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import com.opencsv.CSVReader;

public class ScheduleFromCSVConverter {
	
	private TransitSchedule schedule = null;
	private CoordinateTransformation transformation = null; 
	private Vehicles vehicles = null;
	private Map<Coord, String> usedCoordinates = new HashMap<>();
	private TransitScheduleFactory scheduleBuilder;
	
	public ScheduleFromCSVConverter(TransitSchedule schedule, Vehicles vehicles, CoordinateTransformation transformation) {
		this.schedule = schedule;
		this.vehicles = vehicles;
		this.transformation = transformation;
		this.scheduleBuilder = this.schedule.getFactory();
	}
	
	public ScheduleFromCSVConverter(TransitSchedule schedule, Vehicles vehicles) {
		this.schedule = schedule;
		this.vehicles = vehicles;
		this.scheduleBuilder = this.schedule.getFactory();
	}
	
	protected static Logger log = Logger.getLogger(ScheduleFromCSVConverter.class);

	public void run(String linesCSV, String stopsCSV, String vehiclesCSV) throws IOException {
		log.info("Creating the schedule based on CSV tables...");

		// 1. Read and create stop facilities
		log.info("  Read transit stops...");
		stopReader(stopsCSV);
		log.info("  Read transit stops... done.");

		// 2. Read and create vehicle types
		log.info("  Read vehicle types...");
		vehiclesTypesReader(vehiclesCSV);
		log.info("  Read vehicle types... done.");

		// 3. Create all lines from CSV lines table
		log.info("  Creating transit routes...");
		createTransitLinesFromCSV(linesCSV);
		log.info("  Creating transit routes... done.");

		// 4. Clean schedule
		ScheduleCleaner.removeNotUsedStopFacilities(schedule);
		ScheduleCleaner.combineIdenticalTransitRoutes(schedule);
		ScheduleCleaner.cleanDepartures(schedule);
		ScheduleCleaner.cleanVehicles(schedule, vehicles);

		log.info("Creating the schedule based on CSV tables... done.");
	}

	private void stopReader(String stopsCSV) {
		CSVReader reader = null;
		try {
            reader = new CSVReader(new FileReader(stopsCSV));
            reader.readNext(); // skip header
            String[] newLine;
            while ((newLine = reader.readNext()) != null) {
            	Id<TransitStopFacility> stopId = Id.create(newLine[0], TransitStopFacility.class);
				double xCoord = Double.parseDouble(newLine[3]);
				double yCoord = Double.parseDouble(newLine[4]);
				Coord coord = new Coord(xCoord, yCoord);
				if (this.transformation != null) {
					coord = this.transformation.transform(coord);
				}
				String stopName = newLine[2];
				createStop(stopId, coord, stopName);
			}
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	private void createStop(Id<TransitStopFacility> stopId, Coord coord, String stopName) {

//		//check if coordinates are already used by another facility
//		String check = usedCoordinates.put(coord, stopName);
//		if(check != null && !check.equals(stopName)) {
//			if(check.contains(stopName) || stopName.contains(check)) {
//				log.info("Two stop facilities at " + coord + " \"" + check + "\" & \"" + stopName + "\"");
//			} else {
//				log.warn("Two stop facilities at " + coord + " \"" + check + "\" & \"" + stopName + "\"");
//			}
//		}

		TransitStopFacility stopFacility = this.scheduleBuilder.createTransitStopFacility(stopId, coord, false);
		stopFacility.setName(stopName);
		this.schedule.addStopFacility(stopFacility);
		Map<Id<TransitStopFacility>, TransitStopFacility> stopFacilities = this.schedule.getFacilities();
		stopFacilities.values();
		stopFacilities.get(stopId);
		//stopFacilities.get(stopId.toString());
	}

	private void vehiclesTypesReader(String vehiclesCSV) {
		VehiclesFactory vehicleFactory = vehicles.getFactory();
		CSVReader reader = null;
		try {
            reader = new CSVReader(new FileReader(vehiclesCSV));
            reader.readNext(); // skip header
            String[] newLine;
            while ((newLine = reader.readNext()) != null) {
        		String vehicleTypeId = newLine[0];
        		VehicleType vehicleType = vehicleFactory.createVehicleType(Id.create(vehicleTypeId.toString(), VehicleType.class));

				vehicleType.setLength(Double.parseDouble(newLine[1]));
				vehicleType.setWidth(Double.parseDouble(newLine[2]));
				vehicleType.setAccessTime(Double.parseDouble(newLine[3]));
				vehicleType.setEgressTime(Double.parseDouble(newLine[4]));
				vehicleType.setDoorOperationMode(VehicleType.DoorOperationMode.serial);
				vehicleType.setPcuEquivalents(Double.parseDouble(newLine[8]));
				vehicleType.setDescription(newLine[9]); 

				VehicleCapacity vehicleCapacity = vehicleFactory.createVehicleCapacity();
				vehicleCapacity.setSeats(Integer.parseInt(newLine[6]));
				vehicleCapacity.setStandingRoom(Integer.parseInt(newLine[7]));
				vehicleType.setCapacity(vehicleCapacity);

				vehicles.addVehicleType(vehicleType);
			}
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	private void createTransitLinesFromCSV(String linesCSV) {
		TransitScheduleFactory scheduleFactory = schedule.getFactory();
		VehiclesFactory vehicleFactory = vehicles.getFactory();

		Counter lineCounter = new Counter(" TransitLine # ");
		
		CSVReader reader = null;
		Id<TransitLine> lineId = null;
		TransitLine transitLine = null;
		String[] stopIds = null;
		String transportMode = null;
		String vehicleTypeId = null;
		int routeCount = 0;
		try {
            reader = new CSVReader(new FileReader(linesCSV));
            String[] newLine;
            while ((newLine = reader.readNext()) != null) {
            	if (newLine[0].length() >= 4 && newLine[0].substring(0, 4).equals("Line")){
            		routeCount = 0;
            		// create or get TransitLine
            		lineId = Id.create(newLine[0], TransitLine.class);
            		
					if(!schedule.getTransitLines().containsKey(lineId)) {
						transitLine = scheduleFactory.createTransitLine(lineId);
						schedule.addTransitLine(transitLine);
						lineCounter.incCounter();
					} else {
						transitLine = schedule.getTransitLines().get(lineId);
					}

					// move to mode line
					newLine = reader.readNext();
					vehicleTypeId = newLine[0];
					transportMode = HafasDefaults.Vehicles.valueOf(vehicleTypeId).transportMode.modeName;
					
					// move to stop IDs line
					newLine = reader.readNext();
					stopIds = newLine;
					
					// move to stop names line
					newLine = reader.readNext();
					
            	}else{
            		routeCount++;
            		List<TransitRouteStop> transitRouteStops = new ArrayList<>();
            		String[] stopSequence = newLine;
            		Double firstStopDepartureTime = null;
            		Double currentStopDepartureTime = null;
            		int stopTime = 0;
            		for (int i = 0; i<stopSequence.length; i++){
            			if (!stopSequence[i].equals("-") && !stopSequence[i].equals("")){
            				try {
            					stopTime = Integer.parseInt(newLine[i].split("h")[0])*3600 + Integer.parseInt(newLine[i].split("h")[1])*60;
							} catch (Exception e) {
								if (e instanceof IndexOutOfBoundsException) {
									stopTime = Integer.parseInt(newLine[i])*3600;
								} else {
									throw e;
								}
							}
            				if (firstStopDepartureTime == null) {
            					firstStopDepartureTime = (double) stopTime;
            				} 
            				currentStopDepartureTime = (double) stopTime;
           					Double arrivalDelay = currentStopDepartureTime - firstStopDepartureTime;
            				TransitStopFacility stopFacility = schedule.getFacilities().get(Id.create(stopIds[i], TransitStopFacility.class));
            				TransitRouteStop routeStop = scheduleFactory.createTransitRouteStop(stopFacility, arrivalDelay, arrivalDelay + 30.0);
            				routeStop.setAwaitDepartureTime(true); // Only *T-Lines (currently not implemented) would have this as false...
            				transitRouteStops.add(routeStop);
            			}
            		}

            		// create actual TransitRoute 
					Id<TransitRoute> routeId = Id.create(lineId.toString()+"_"+routeCount, TransitRoute.class);
					TransitRoute transitRoute = scheduleFactory.createTransitRoute(routeId, null, transitRouteStops, transportMode);
					// Departure Id
					Id<Departure> departureId = Id.create(routeCount, Departure.class);
					// Departure vehicle
					Id<Vehicle> vehicleId = Id.create(vehicleTypeId + "_" + routeId.toString(), Vehicle.class);
					// create and add departure
					Departure departure = scheduleFactory.createDeparture(departureId, firstStopDepartureTime);
					departure.setVehicleId(vehicleId);
					transitRoute.addDeparture(departure);
					try {
						vehicles.addVehicle(vehicleFactory.createVehicle(departure.getVehicleId(), vehicles.getVehicleTypes().get(Id.create(vehicleTypeId, VehicleType.class))));
					} catch (Exception e) {
						e.printStackTrace();
					}
					transitRoute.setTransportMode(transportMode);
					transitLine.addRoute(transitRoute);
            	}
			}
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
