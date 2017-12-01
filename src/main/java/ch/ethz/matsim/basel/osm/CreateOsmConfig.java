package ch.ethz.matsim.basel.osm;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates a default osmConverter config file for Basel.
 */
public final class CreateOsmConfig {

	/**
	 * Creates a default publicTransitMapping config file.
	 * @param args [0] default config filename
	 */
	public static void main(final String[] args) {
		Config config = ConfigUtils.createConfig();

		config.addModule(OsmExtendedConverterConfigGroup.createDefaultConfig());

		Set<String> toRemove = config.getModules().keySet().stream().filter(module -> !module.equals(OsmExtendedConverterConfigGroup.GROUP_NAME)).collect(Collectors.toSet());
		toRemove.forEach(config::removeModule);

		new ConfigWriter(config).write(args[0]);
	}
}
