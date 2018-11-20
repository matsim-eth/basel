package ch.ethz.matsim.basel.schedule;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt2matsim.tools.ScheduleTools;

import com.opencsv.CSVWriter;

public class ScheduleToCsvConverter {

	TransitSchedule schedule;
	
	public ScheduleToCsvConverter(String inputSchedulePath) {
		this.schedule = ScheduleTools.readTransitSchedule(inputSchedulePath);
	}

	public static void main(String[] args) {
		ScheduleToCsvConverter converter = new ScheduleToCsvConverter(args[0]);
		converter.stopsWriter(args[1]);
		converter.scheduleWriter(args[2]);
	}
	
	public void stopsWriter(String stopsOutput) {
		CSVWriter writer = null;
		try {
			writer = new CSVWriter(new BufferedWriter(new FileWriter(stopsOutput)), ';');
			
			// header
			writer.writeNext(new String[] { "id", "stop_name", "coordX", "coordY" });
			
			// stops
			String[] newLine = new String[4];
			for (TransitStopFacility stop : schedule.getFacilities().values()) {
				newLine[0] = stop.getId().toString();
				newLine[1] = stop.getName();
				newLine[2] = String.valueOf(stop.getCoord().getX());
				newLine[3] = String.valueOf(stop.getCoord().getY());
				writer.writeNext(newLine);
			}
        } catch (IOException e) { e.printStackTrace(); } 
		finally {
        	try { writer.close(); } 
        	catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	public void scheduleWriter(String linesOutput) {
		CSVWriter writer = null;
		try {
			writer = new CSVWriter(new BufferedWriter(new FileWriter(linesOutput)), ';');
			
			for (TransitLine line : schedule.getTransitLines().values()) {
				// line name
				String name = line.getId().toString();
				if (!name.startsWith("Line_")) { name = "Line_" + name; }
				writer.writeNext(new String[] { name });
				
				// mode (assumes all routes use same transport mode)
				String mode = line.getRoutes().values().iterator().next().getTransportMode();
				writer.writeNext(new String[] { mode });
				
				// routes
				for (TransitRoute route : line.getRoutes().values()) {
					// get ids and names. Prepare offsets for timetable.
					String[] stopIds = new String[route.getStops().size()];
					String[] stopNames = new String[route.getStops().size()];
					int[] arrivalOffsets = new int[route.getStops().size()];
					for (int i = 0; i < route.getStops().size(); i++) {
						TransitRouteStop routeStop = route.getStops().get(i);
						stopIds[i] = routeStop.getStopFacility().getId().toString();
						stopNames[i] = routeStop.getStopFacility().getName();
						arrivalOffsets[i] = (int) routeStop.getArrivalOffset();
					}
					writer.writeNext(stopIds);
					writer.writeNext(stopNames);
					
					// timetable
					String[][] timetable = new String[route.getDepartures().size()][];
					Iterator<Departure> departures = route.getDepartures().values().iterator();
					for (int i = 0; i < route.getDepartures().size(); i++) {
						Departure departure = departures.next();
						String[] profile = new String[route.getStops().size()];
						for (int j = 0; j < arrivalOffsets.length; j++) {
							int seconds = (int) departure.getDepartureTime() + arrivalOffsets[j];
							int hours = seconds / 3600;
							int minutes = (seconds % 3600) / 60;
							profile[j] = String.format("%02dh%02d", hours, minutes); 
						}
						timetable[i] = profile;
					}
					
					for (String[] newLine : timetable) {
						writer.writeNext(newLine);
					}
				}
			}
		} catch (IOException e) { e.printStackTrace(); } 
		finally {
        	try { writer.close(); } 
        	catch (IOException e) { e.printStackTrace(); }
		}
	}

}
