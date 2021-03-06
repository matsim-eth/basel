package ch.ethz.matsim.basel.network;

import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.pt2matsim.osm.lib.AllowedTagsFilter;
import org.matsim.pt2matsim.osm.lib.Osm;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Config group for osm conversion {@link org.matsim.pt2matsim.osm.OsmMultimodalNetworkConverter}
 *
 * @author polettif
 */
public class OsmExtendedConverterConfigGroup extends ReflectiveConfigGroup {

	// constant values used in converter
	public static final String LINK_ATTRIBUTE_WAY_ID = "osm:way:id";
	public static final String LINK_ATTRIBUTE_WAY_PREFIX = "osm:way:";
	public static final String LINK_ATTRIBUTE_RELATION_ROUTE = "osm:relation:route";
	public static final String LINK_ATTRIBUTE_RELATION_ROUTE_MASTER = "osm:relation:route_master";

	// actual config values
	public static final String GROUP_NAME = "OsmConverter";

	private static final String OSM_FILE ="osmFile";
	private static final String OSM_FILE_TYPE ="osmFileType";
	private static final String OSM_POLYGON_FILTER_FILE ="osmPolygonFilterFile";
	private static final String OSM_FILTER_ROADS ="osmFilterRoads";
	private static final String OUTPUT_NETWORK_FILE ="outputNetworkFile";
	private static final String OUTPUT_COORDINATE_SYSTEM ="outputCoordinateSystem";
	private static final String KEEP_PATHS ="keepPaths";
	private static final String MAX_LINK_LENGTH = "maxLinkLength";
	private static final String SCALE_MAX_SPEED = "scaleMaxSpeed";
	private static final String GUESS_FREE_SPEED = "guessFreeSpeed";
	private static final String KEEP_TAGS_AS_ATTRIBUTES = "keepTagsAsAttributes";

	private String osmFile;
	private String osmFileType;
	private String osmPolygonFilterFile;
	private String osmFilterRoads;
	private String outputNetworkFile;
	private String outputCoordinateSystem;
	private CoordinateTransformation coordinateTransformation = new IdentityTransformation();

	private double maxLinkLength = 500.0;
	private boolean keepPaths = false;
	private boolean scaleMaxSpeed = false;
	private boolean guessFreeSpeed = false;
	private boolean keepTagsAsAttributes = true;

	public OsmExtendedConverterConfigGroup() {
		super(GROUP_NAME);
	}

	/**
	 * @return A new default OsmConverter config
	 */
	public static OsmExtendedConverterConfigGroup createDefaultConfig() {
		Set<String> carSingleton = Collections.singleton("car");
		Set<String> railSingleton = Collections.singleton("rail");

		OsmExtendedConverterConfigGroup defaultConfig = new OsmExtendedConverterConfigGroup();
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.MOTORWAY, 2, 120.0 / 3.6, 1.0, 2000, true, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.MOTORWAY_LINK, 1, 80.0 / 3.6, 1.0, 1500, true, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.TRUNK, 1, 80.0 / 3.6, 1.0, 2000, false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.TRUNK_LINK, 1, 50.0 / 3.6, 1.0, 1500, false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.PRIMARY, 1, 80.0 / 3.6, 1.0, 1500, false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.PRIMARY_LINK, 1, 60.0 / 3.6, 1.0, 1500, false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.SECONDARY, 1, 30.0 / 3.6, 1.0, 1000, false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.SECONDARY_LINK, 1, 30.0 / 3.6, 1.0, 1000, false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.TERTIARY, 1, 25.0 / 3.6, 1.0, 600, false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.TERTIARY_LINK, 1, 25.0 / 3.6, 1.0, 600, false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.MINOR, 1, 40.0 / 3.6, 1.0, 600, false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.UNCLASSIFIED, 1, 15.0 / 3.6, 1.0, 600, false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.RESIDENTIAL, 1, 15.0 / 3.6, 1.0, 600, false, carSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.HIGHWAY, Osm.Value.LIVING_STREET, 1, 10. / 3.6, 1.0, 300, false, carSingleton));

		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.RAILWAY, Osm.Value.RAIL, 1, 160.0 / 3.6, 1.0, 9999, false, railSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.RAILWAY, Osm.Value.TRAM, 1, 40.0 / 3.6, 1.0, 9999, true, railSingleton));
		defaultConfig.addParameterSet(new OsmWayParams(Osm.Key.RAILWAY, Osm.Value.LIGHT_RAIL, 1, 80.0 / 3.6, 1.0, 9999, false, railSingleton));

		return defaultConfig;
	}

	@StringGetter(OSM_FILE)
	public String getOsmFile() {
		return osmFile;
	}

	@StringSetter(OSM_FILE)
	public void setOsmFile(String osmFile) {
		this.osmFile = osmFile;
	}
	@StringGetter(OSM_FILE_TYPE)
	public String getOsmFileType() {
		return osmFileType;
	}

	@StringSetter(OSM_FILE_TYPE)
	public void setOsmFileType(String osmFileType) {
		this.osmFileType = osmFileType;
	}
	
	@StringGetter(OSM_POLYGON_FILTER_FILE)
	public String getOsmPolygonFilterFile() {
		return osmPolygonFilterFile;
	}

	@StringSetter(OSM_POLYGON_FILTER_FILE)
	public void setOsmPolygonFilterFile(String osmPolygonFilterFile) {
		this.osmPolygonFilterFile = osmPolygonFilterFile;
	}
	@StringGetter(OSM_FILTER_ROADS)
	public String getOsmFilterRoads() {
		return osmFilterRoads;
	}

	@StringSetter(OSM_FILTER_ROADS)
	public void setOsmFilterRoads(String osmFilterRoads) {
		this.osmFilterRoads = osmFilterRoads;
	}
	
	@StringGetter(KEEP_PATHS)
	public boolean getKeepPaths() {
		return keepPaths;
	}

	@StringSetter(KEEP_PATHS)
	public void setKeepPaths(boolean keepPaths) {
		this.keepPaths = keepPaths;
	}

	@StringGetter(GUESS_FREE_SPEED)
	public boolean getGuessFreeSpeed() {
		return guessFreeSpeed;
	}

	@StringSetter(GUESS_FREE_SPEED)
	public void setGuessFreeSpeed(boolean guessFreeSpeed) {
		this.guessFreeSpeed = guessFreeSpeed;
	}

	@StringGetter(MAX_LINK_LENGTH)
	public double getMaxLinkLength() {
		return maxLinkLength;
	}

	@StringSetter(MAX_LINK_LENGTH)
	public void setMaxLinkLength(double maxLinkLength) {
		this.maxLinkLength = maxLinkLength;
	}

	@StringGetter(SCALE_MAX_SPEED)
	public boolean getScaleMaxSpeed() {
		return scaleMaxSpeed;
	}

	@StringSetter(SCALE_MAX_SPEED)
	public void setScaleMaxSpeed(boolean scaleMaxSpeed) {
		this.scaleMaxSpeed = scaleMaxSpeed;
	}

	@StringGetter(KEEP_TAGS_AS_ATTRIBUTES)
	public boolean getKeepTagsAsAttributes() {
		return keepTagsAsAttributes;
	}

	@StringSetter(KEEP_TAGS_AS_ATTRIBUTES)
	public void setKeepTagsAsAttributes(boolean v) {
		this.keepTagsAsAttributes = v;
	}

	@StringGetter(OUTPUT_NETWORK_FILE)
	public String getOutputNetworkFile() {
		return outputNetworkFile;
	}

	@StringSetter(OUTPUT_NETWORK_FILE)
	public void setOutputNetworkFile(String outputNetworkFile) {
		this.outputNetworkFile = outputNetworkFile;
	}

	@StringGetter(OUTPUT_COORDINATE_SYSTEM)
	public String getOutputCoordinateSystem() {
		return outputCoordinateSystem;
	}

	@StringSetter(OUTPUT_COORDINATE_SYSTEM)
	public void setOutputCoordinateSystem(String outputCoordinateSystem) {
		if(outputCoordinateSystem != null) {
			this.outputCoordinateSystem = outputCoordinateSystem;
			this.coordinateTransformation = TransformationFactory.getCoordinateTransformation("WGS84", outputCoordinateSystem);
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(OSM_FILE,
				"The path to the osm file.");
		map.put(OSM_FILE_TYPE,
				"Format of the OSM file. Either pbf or xml.");
		map.put(OSM_POLYGON_FILTER_FILE,
				"Special file to apply a filter to the OSM file. More info: http://wiki.openstreetmap.org/wiki/Osmosis/Polygon_Filter_File_Format");
		map.put(OSM_FILTER_ROADS,
				"Comma separated list of OSM road types to be kept outside the filter area. Ignored if no filter is supplied.");
		map.put(KEEP_PATHS,
				"Sets whether the detailed geometry of the roads should be retained in the conversion or not.\n" +
				"\t\tKeeping the detailed paths results in a much higher number of nodes and links in the resulting MATSim network.\n" +
				"\t\tNot keeping the detailed paths removes all nodes where only one road passes through, thus only real intersections\n" +
				"\t\tor branchings are kept as nodes. This reduces the number of nodes and links in the network, but can in some rare\n" +
				"\t\tcases generate extremely long links (e.g. for motorways with only a few ramps every few kilometers).\n" +
				"\t\tDefaults to <code>false</code>.");
		map.put(GUESS_FREE_SPEED,
				"If true: The numeric characters from the maxspeed:motorcar tag are used if it cannot be directly parsed (e.g. removes trailling kph or similar).");
		map.put(KEEP_TAGS_AS_ATTRIBUTES,
				"If true: The osm tags for ways and containing relations are saved as link attributes in the network. Increases filesize. Default: true.");
		map.put(SCALE_MAX_SPEED,
				"In case the speed limit allowed does not represent the speed a vehicle can actually realize, e.g. by constrains of\n" +
				"\t\ttraffic lights not explicitly modeled, a kind of \"average simulated speed\" can be used.\n" +
				"\t\tDefaults to false. Set true to scale the speed limit down by the value specified by the wayValues)");
		return map;
	}

	public CoordinateTransformation getCoordinateTransformation() {
		return this.coordinateTransformation;
	}

	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch(type) {
			case OsmWayParams.SET_NAME :
				return new OsmWayParams();
			default:
				throw new IllegalArgumentException("Unknown parameterset name!");
		}
	}

	public AllowedTagsFilter getBasicWayFilter() {
		AllowedTagsFilter filter = new AllowedTagsFilter();
		for(ConfigGroup e : this.getParameterSets(OsmExtendedConverterConfigGroup.OsmWayParams.SET_NAME)) {
			OsmExtendedConverterConfigGroup.OsmWayParams w = (OsmExtendedConverterConfigGroup.OsmWayParams) e;
			filter.add(Osm.ElementType.WAY, w.getOsmKey(), w.getOsmValue());
		}
		return filter;
	}
	
	public void filterBasicWayFilter(Set<String> waysToRemove) {
		Set<ConfigGroup> valuesToRemove = new HashSet<ConfigGroup>();
		for(ConfigGroup e : this.getParameterSets(OsmExtendedConverterConfigGroup.OsmWayParams.SET_NAME)) {
			OsmExtendedConverterConfigGroup.OsmWayParams w = (OsmExtendedConverterConfigGroup.OsmWayParams) e;
			if (!waysToRemove.contains(w.getOsmValue())) valuesToRemove.add(e);
		}
		for(ConfigGroup e : valuesToRemove) this.removeParameterSet(e);
	}

	/**
	 * Defines link attributes for converting OSM highway paths
	 * into MATSim links.
	 *
	 */
	public static class OsmWayParams extends ReflectiveConfigGroup implements MatsimParameters {

		public final static String SET_NAME = "wayParams";

		private String osmKey;
		private String osmValue;

		/** number of lanes on that road type **/
		private double lanes;
		/** free speed vehicles can drive on that road type [meters/second] **/
		private double freespeed;
		/** factor the freespeed is scaled **/
		private double freespeedFactor;
		/** capacity per lane [veh/h] **/
		private double laneCapacity;
		/** true to say that this road is a oneway road  **/
		private boolean oneway;
		/** defines the allowed transport modes for the link  **/
		private Set<String> allowedTransportModes;

		/**
		 * Constructors
		 */
		public OsmWayParams() {
			super(SET_NAME);
		}

		public OsmWayParams(String osmKey, String osmValue, double lanes, double freespeed, double freespeedFactor, double laneCapacity, boolean oneway, Set<String> allowedTransportModes) {
			super(SET_NAME);
			this.osmKey = osmKey;
			this.osmValue = osmValue;
			this.lanes = lanes;
			this.freespeed = freespeed;
			this.freespeedFactor = freespeedFactor;
			this.laneCapacity = laneCapacity;
			this.oneway = oneway;
			this.allowedTransportModes = allowedTransportModes;
		}

		@StringGetter("osmValue")
		public String getOsmValue() {
			return osmValue;
		}

		@StringSetter("osmValue")
		public void setOsmValue(String osmValue) {
			this.osmValue = osmValue;
		}

		@StringGetter("osmKey")
		public String getOsmKey() {
			return osmKey;
		}

		@StringSetter("osmKey")
		public void setOsmKey(String osmKey) {
			this.osmKey = osmKey;
		}


		@StringGetter("lanes")
		public double getLanes() {
			return lanes;
		}

		@StringSetter("lanes")
		public void setLanes(double lanes) {
			this.lanes = lanes;
		}

		@StringGetter("freespeed")
		public double getFreespeed() {
			return freespeed;
		}

		@StringSetter("freespeed")
		public void setFreespeed(double freespeed) {
			this.freespeed = freespeed;
		}

		@StringGetter("freespeedFactor")
		public double getFreespeedFactor() {
			return freespeedFactor;
		}

		@StringSetter("freespeedFactor")
		public void setFreespeedFactor(double freespeedFactor) {
			this.freespeedFactor = freespeedFactor;
		}

		@StringGetter("laneCapacity")
		public double getLaneCapacity() {
			return laneCapacity;
		}

		@StringSetter("laneCapacity")
		public void setLaneCapacity(double laneCapacity) {
			this.laneCapacity = laneCapacity;
		}

		@StringGetter("oneway")
		public boolean getOneway() {
			return oneway;
		}

		@StringSetter("oneway")
		public void setOneway(boolean oneway) {
			this.oneway = oneway;
		}


		public Set<String> getAllowedTransportModes() {
			return this.allowedTransportModes;
		}

		public void setAllowedTransportModes(Set<String> allowedTransportModes) {
			this.allowedTransportModes = allowedTransportModes;
		}

		@StringGetter("allowedTransportModes")
		private String getAllowedTransportModesString() {
			return CollectionUtils.setToString(allowedTransportModes);
		}

		@StringSetter("allowedTransportModes")
		private void setAllowedTransportModesString(String allowedTransportModesString) {
			this.allowedTransportModes = CollectionUtils.stringToSet(allowedTransportModesString);
		}
	}
}
