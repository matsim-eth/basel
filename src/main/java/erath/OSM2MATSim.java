package erath;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.pt2matsim.config.OsmConverterConfigGroup;
import org.matsim.pt2matsim.osm.OsmMultimodalNetworkConverter;
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
		Config configAll = ConfigUtils.loadConfig(configFile, new OsmConverterConfigGroup());
		OsmConverterConfigGroup config = ConfigUtils.addOrGetModule(configAll, OsmConverterConfigGroup.GROUP_NAME, OsmConverterConfigGroup.class );

		run(config);
	}

	/**
	 * Converts an osm file with default conversion parameters.
	 * @param osmFile the osm file
	 * @param outputNetworkFile the path to the output network file
	 * @param outputCoordinateSystem output coordinate system (no transformation is applied if <tt>null</tt>)
	 */
	public static void run(String osmFile, String outputNetworkFile, String outputCoordinateSystem) {
		OsmConverterConfigGroup config = OsmConverterConfigGroup.createDefaultConfig();
		config.setOsmFile(osmFile);
		config.setOutputNetworkFile(outputNetworkFile);
		config.setOutputCoordinateSystem(outputCoordinateSystem);

		run(config);
	}

	public static void run(OsmConverterConfigGroup config) {
		OsmData osmData = new OsmDataImpl(config.getBasicWayFilter());
		String osmFile = config.getOsmFile();
		if (FilenameUtils.getExtension(osmFile).equals("pbf")){ // rough filetype check
			File tempOsmFile = null;
			try {
				tempOsmFile = File.createTempFile("osmInput", ".xml");
				tempOsmFile.deleteOnExit();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// Read the PBF and write to XML.
	        Osmosis.run(new String[] {"-q","--read-pbf-0.6",osmFile,"--write-xml-0.6",tempOsmFile.getPath()});
	        osmFile = tempOsmFile.getPath();
		}
		new OsmFileReader(osmData).readFile(osmFile);

		OsmMultimodalNetworkConverter converter = new OsmMultimodalNetworkConverter(osmData);
		converter.convert(config);

		NetworkTools.writeNetwork(converter.getNetwork(), config.getOutputNetworkFile());
	}
}
