package ch.ethz.matsim.basel.network;

import org.openstreetmap.osmosis.core.Osmosis;

/*
 * This class takes as input different OSM files (.osm.pbf or .osm).
 * Its output is one OSM file to be used as input for the network conversion.
 * The cut area was based on the GVMB area with a 7.5 km Buffer. 
 * In QGIS, after creating the area shapefile, conversion to .poly format is possible with plugin osmpoly_export.
 */

public class GenerateOsmInputData {
	static String path = "resources/network/";
	static String switzerlandOsmInput = path + "input/switzerland-latest.osm.pbf";
	static String franceOsmInput = path + "input/alsace-latest.osm.pbf";
	static String germanyOsmInput = path + "input/baden-wuerttemberg-latest.osm.pbf";
	static String polygonGvmbWithBuffer = path + "input/GVMB_Buffer.poly";
	
	/*
	 * input: outputFormat desired. Either pbf or osm.
	 */
	public static void run(){
		// here osmosis merges the files, applies the bounding polygon and filters for roads.
		// it is more efficient to do it all in one command
		Osmosis.run(new String[] {
				"--read-pbf", switzerlandOsmInput, "--sort", 
				"--read-pbf", franceOsmInput, "--sort",
				"--read-pbf", germanyOsmInput, "--sort",
				"--merge", "--merge", // merge twice since there are three files
				"--bounding-polygon", "file=" + polygonGvmbWithBuffer,
				"--tag-filter", "accept-ways",
				"highway=motorway,motorway_link,trunk,trunk_link,primary,primary_link,secondary,secondary_link,tertiary,tertiary_link,residential,unclassified,minor,living_street",
				"--tag-filter", "reject-relations", "--used-node", "--write-pbf",  
				path + "output/Basel_GVMB.osm.pbf", "omitmetadata=true"});
	}	
}
