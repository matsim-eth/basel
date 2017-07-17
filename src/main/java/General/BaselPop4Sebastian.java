package General;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class BaselPop4Sebastian extends BaselPopGenerator{
	final static String BASEL_POP = new File("Basel/input/BaselFull.csv").getAbsolutePath();
	final static String OUTPUT_PLANS = new File("Basel/output/plans4Sebastian.xml").getAbsolutePath();
	final static String OUTPUT_ATTRIBUTES = new File("Basel/output/personAtrributes4Sebastian.xml").getAbsolutePath();
	
	final static Coord nullCoord = new Coord(0, 0);
	static int naTripDur = 0;
	static int naBeeline = 0;

	public static void main(String[] args) {
		BaselPop4Sebastian popGen = new BaselPop4Sebastian();
		popGen.popCreate();
		log.warn("Total amount of dumped persons due to NA TripDuration: " + naTripDur);
		log.warn("Total amount of dumped persons due to NA Beeline: " + naBeeline);
		new PopulationWriter(getScenario().getPopulation(), getScenario().getNetwork()).write(OUTPUT_PLANS);
		new ObjectAttributesXmlWriter(getScenario().getPopulation().getPersonAttributes()).writeFile(OUTPUT_ATTRIBUTES);
	}
	
	private void popCreate(){
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(BASEL_POP));
			String line = bufferedReader.readLine(); // skip header
			
			int index_PersonID = 3;
			int index_TripChain = 14;
			int index_start_time = 15;
			int index_duration = 16;
			int index_mode = 17;
			int index_beeline = 19;
			int index_tripDuration = 20;

			while ((line = bufferedReader.readLine()) != null) {
				String parts[] = line.split(",");
				
				Person person = getPopulationFactory().createPerson(Id.create(parts[index_PersonID], Person.class));
				getPopulation().addPerson(person);
				
				Plan plan = getPopulationFactory().createPlan();
				person.addPlan(plan);
				person.setSelectedPlan(plan);
				
				createAndAddActivities(person, parts[index_TripChain], parts[index_start_time], parts[index_duration], 
						parts[index_mode], parts[index_beeline], parts[index_tripDuration]);
			}
			bufferedReader.close();
		} // end try
		catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	private void createAndAddActivities(Person person, String tripChain, String startTimes, String durations, String modes, 
			String beelines, String trip_Durations) {
		String tChain[] = tripChain.split("-");
		String sTimes[] = startTimes.split("-");
		String dur[] = durations.split("-");
		String mode[] = modes.split("-");
		String beeline[] = beelines.split("-");
		String tDuration[] = trip_Durations.split("-");
		
		Plan plan = person.getSelectedPlan();
		for (int i = 0; i < tChain.length; i++){
			if (tChain[i].equals("H")) {
				Activity home = getPopulationFactory().createActivityFromCoord("h", nullCoord);
				home.setStartTime(Integer.parseInt(sTimes[i]));
				if(i < tChain.length -1) home.setMaximumDuration(Integer.parseInt(dur[i])); // doesnt adds duration to last home activity
				plan.addActivity(home);
			}
			else if (tChain[i].equals("W")) {
				Activity work = getPopulationFactory().createActivityFromCoord("w", nullCoord);
				work.setStartTime(Integer.parseInt(sTimes[i]));
				work.setMaximumDuration(Integer.parseInt(dur[i]));
				plan.addActivity(work);
			}else if (tChain[i].equals("E")) {
				Activity education = getPopulationFactory().createActivityFromCoord("e", nullCoord);
				education.setStartTime(Integer.parseInt(sTimes[i]));
				education.setMaximumDuration(Integer.parseInt(dur[i]));
				plan.addActivity(education);
			}
			else if (tChain[i].equals("L")) {
				Activity leisure = getPopulationFactory().createActivityFromCoord("l", nullCoord);
				leisure.setStartTime(Integer.parseInt(sTimes[i]));
				leisure.setMaximumDuration(Integer.parseInt(dur[i]));
				plan.addActivity(leisure);
			}
			else { // shop or sonstiges (S, T)
				Activity shop = getPopulationFactory().createActivityFromCoord("s", nullCoord);
				shop.setStartTime(Integer.parseInt(sTimes[i]));
				shop.setMaximumDuration(Integer.parseInt(dur[i]));
				plan.addActivity(shop);
			}
			
			// Add leg if it's not the last activity
			if(i < tChain.length -1){
				if (tDuration[i].equals("na")) {
					naTripDur++;
					getPopulation().removePerson(person.getId());
				} else if(beeline[i].equals("na")) {
					naBeeline++;
					getPopulation().removePerson(person.getId());
				} else {
					Leg leg = getPopulationFactory().createLeg(mode[i]);
					leg.setTravelTime(60*Double.parseDouble(tDuration[i]));
					leg.setDepartureTime(Double.parseDouble(beeline[i]));
					getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), "beeline" + i, beeline[i]);
					plan.addLeg(leg);
				}
			}
		}
	}	
}
