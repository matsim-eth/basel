package ch.ethz.matsim.basel.osm;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.commons.io.FilenameUtils;
import org.matsim.api.core.v01.network.Network;
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
//	public static void run(String configFile) {
//		Config configAll = ConfigUtils.loadConfig(configFile, new OsmExtendedConverterConfigGroup());
//		OsmExtendedConverterConfigGroup config = ConfigUtils.addOrGetModule(configAll, OsmExtendedConverterConfigGroup.GROUP_NAME, OsmExtendedConverterConfigGroup.class );
//
//		run(configFile);
//	}
//
//	/**
//	 * Converts an osm file with default conversion parameters.
//	 * @param osmFile the osm file
//	 * @param outputNetworkFile the path to the output network file
//	 * @param outputCoordinateSystem output coordinate system (no transformation is applied if <tt>null</tt>)
//	 */
//	public static void run(String osmFile, String outputNetworkFile, String outputCoordinateSystem) {
//		OsmExtendedConverterConfigGroup config = OsmExtendedConverterConfigGroup.createDefaultConfig();
//		config.setOsmFile(osmFile);
//		config.setOutputNetworkFile(outputNetworkFile);
//		config.setOutputCoordinateSystem(outputCoordinateSystem);
//
//		run(config);
//	}

	public static void run(String configFile) {
		Config configAll = ConfigUtils.loadConfig(configFile, new OsmExtendedConverterConfigGroup());
		OsmExtendedConverterConfigGroup config = ConfigUtils.addOrGetModule(configAll, OsmExtendedConverterConfigGroup.GROUP_NAME, OsmExtendedConverterConfigGroup.class );

		OsmData osmData = new OsmDataImpl(config.getBasicWayFilter());
		String osmFile = config.getOsmFile();
		String osmFileType = config.getOsmFileType();
		String osmPolygonFilter = config.getOsmPolygonFilterFile();
		String osmFilterRoads = config.getOsmFilterRoads();
		OsmNetworkConverter converter_cut = null; // in case of a network cut is to be performed 
		
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
	        String osmFile2 = osmFile;
	        osmFile = tempOsmFile2.getPath();
	        //osmFile = osmFile2;
			if (osmFilterRoads != null && !osmFilterRoads.isEmpty()){
				Config configAll2 = ConfigUtils.loadConfig(configFile, new OsmExtendedConverterConfigGroup());
				OsmExtendedConverterConfigGroup config2 = ConfigUtils.addOrGetModule(configAll2, OsmExtendedConverterConfigGroup.GROUP_NAME, OsmExtendedConverterConfigGroup.class );

				String[] filterRoads = Arrays.stream(osmFilterRoads.split(",")).map(String::trim).toArray(String[]::new);
				config2.filterBasicWayFilter(new HashSet<String>(Arrays.asList(filterRoads)));

				OsmData osmData2 = new OsmDataImpl(config2.getBasicWayFilter());
				new OsmFileReader(osmData2).readFile(osmFile2);

				converter_cut = new OsmNetworkConverter(osmData2);
				converter_cut.convert(config2);
			} else {
				osmFile = tempOsmFile2.getPath();
			}
		}
			
		new OsmFileReader(osmData).readFile(osmFile);

		OsmNetworkConverter converter = new OsmNetworkConverter(osmData);
		converter.convert(config);
		Network finalNetwork = converter.getNetwork();
		
		if (converter_cut != null) {
			NetworkTools.mergeNetworks(finalNetwork, Collections.singleton(converter_cut.getNetwork()));
			}
		
		NetworkTools.writeNetwork(finalNetwork, config.getOutputNetworkFile());
		
	}
}
