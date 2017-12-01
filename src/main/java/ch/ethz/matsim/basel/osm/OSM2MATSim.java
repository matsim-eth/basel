package ch.ethz.matsim.basel.osm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt2matsim.config.OsmConverterConfigGroup;
import org.matsim.pt2matsim.osm.lib.OsmData;
import org.matsim.pt2matsim.osm.lib.OsmDataImpl;
import org.matsim.pt2matsim.osm.lib.OsmFileReader;
import org.matsim.pt2matsim.tools.NetworkTools;
import org.openstreetmap.osmosis.core.Osmosis;

/**
 * Run this class to create a multimodal MATSim network from OSM.
 *
 * @author polettif
 */
public final class OSM2MATSim {

	private OSM2MATSim() {
	}

	/**
	 * Converts an osm file to a MATSim network. The input and output file as well
	 * as conversion parameters are defined in this file. Run {@link CreateDefaultOsmConfig}
	 * to create a default config.
	 *
	 * @param args [0] the config.xml file<br/>
	 */
	public static void main(String[] args) {
		if(args.length == 1) {
			run(args[0]);
		} else {
			throw new IllegalArgumentException("Wrong number of arguments");
		}
	}

	/**
	 * Converts an osm file to a MATSim network. The input and output file as well
	 * as conversion parameters are defined in the config file. Run {@link CreateDefaultOsmConfig}
	 * to create a default config.
	 *
	 * @param configFile the config.xml file
	 */
	public static void run(String configFile) {
		Config configAll = ConfigUtils.loadConfig(configFile, new OsmExtendedConverterConfigGroup());
		OsmExtendedConverterConfigGroup config = ConfigUtils.addOrGetModule(configAll, OsmExtendedConverterConfigGroup.GROUP_NAME, OsmExtendedConverterConfigGroup.class );

		run(config);
	}

	/**
	 * Converts an osm file with default conversion parameters.
	 * @param osmFile the osm file
	 * @param outputNetworkFile the path to the output network file
	 * @param outputCoordinateSystem output coordinate system (no transformation is applied if <tt>null</tt>)
	 */
	public static void run(String osmFile, String outputNetworkFile, String outputCoordinateSystem) {
		OsmExtendedConverterConfigGroup config = OsmExtendedConverterConfigGroup.createDefaultConfig();
		config.setOsmFile(osmFile);
		config.setOutputNetworkFile(outputNetworkFile);
		config.setOutputCoordinateSystem(outputCoordinateSystem);

		run(config);
	}

	public static void run(OsmExtendedConverterConfigGroup config) {
		OsmData osmData = new OsmDataImpl(config.getBasicWayFilter());
		String osmFile = config.getOsmFile();
		String osmFileType = config.getOsmFileType();
		String osmPolygonFilter = config.getOsmPolygonFilterFile();
		String osmFilterRoads = config.getOsmFilterRoads();
		
		if (osmFileType.equals("pbf")){ // rough filetype check
			File tempOsmFile = null;
			try {
				tempOsmFile = File.createTempFile("osmInput", ".osm");
				tempOsmFile.deleteOnExit();
			} catch (IOException e) {e.printStackTrace();}
			// Read the PBF and write to XML.
	        Osmosis.run(new String[] {"--read-pbf",osmFile,"--write-xml",tempOsmFile.getPath()});
	        osmFile = tempOsmFile.getPath();
		}
		
		if (osmPolygonFilter != null && !osmPolygonFilter.isEmpty()){
			File tempOsmFile2 = null;
			try {
				tempOsmFile2 = File.createTempFile("osmInput", ".osm");
				tempOsmFile2.deleteOnExit();
			} catch (IOException e) {e.printStackTrace();}
			Osmosis.run(new String[] {"--read-xml", osmFile, "--bounding-polygon", 
					osmPolygonFilter, "--write-xml",  tempOsmFile2.getPath()});
	        String osmFile2 = tempOsmFile2.getPath();
			if (osmFilterRoads == null || osmFilterRoads.isEmpty()){
				osmFile = osmFile2;
			} else {
				String[] filterRoads = Arrays.stream(osmFilterRoads.split(",")).map(String::trim).toArray(String[]::new);
				OsmExtendedConverterConfigGroup config2 = config;
//				TODO: set new highway filter for config 2 -> convert to MATSim and then merge.
//				A better option would be to filter highway with Osmosis, then it's possible to save the exact OSM.
			}
		}
			
		new OsmFileReader(osmData).readFile(osmFile);

		OsmNetworkConverter converter = new OsmNetworkConverter(osmData);
		converter.convert(config);

		NetworkTools.writeNetwork(converter.getNetwork(), config.getOutputNetworkFile());
	}
}
