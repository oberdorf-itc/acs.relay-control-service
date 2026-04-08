package de.oberdorf_itc.acs;

// Import Java Libraries
import de.oberdorf_itc.helpers.Manifest_helper;
import de.oberdorf_itc.helpers.PropertiesFromEnvironment;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class RelayControlService is the main class of the OITC Access Control System: service that controls the door relays.
 * This service is part of the Michael Oberdorf IT-Consulting (OITC) Access Control System (ACS).
 * On accessing entry points, this information is available in the internal bus system. This service listens on these events
 * and triggers the relay to unlock the associated door.
 * This service is NO standalone service. It requires an OITC ACS system to be running.
 * Copyright by Michael Oberdorf IT-Consulting 2015-2026<br>
 * Date: 2015-08-27
 * @author Michael Oberdorf
 * @version 1.0.0
 * @see "https://www.oberdorf-itc.de/"
 */
public class RelayControlService {
    private final static Logger logger = LoggerFactory.getLogger(de.oberdorf_itc.acs.RelayControlService.class);
    private static Map<String, Object> configuration = new HashMap<String, Object>();

    public static void main(String[] args) throws IOException, InterruptedException {
        // read value of Implementation-Version from META-INF/MANIFEST.MF of this class
        Manifest_helper MF = new Manifest_helper();
        logger.info("OITC Access Control System: Relay Control Service v{} started", MF.getImplementationVersion());
        logger.info("(C) Copyright by Michael Oberdorf IT-Consulting 2015-2026, https://www.oberdorf-itc.de/)");

        // read environment variables and store them in the configuration map
        PropertiesFromEnvironment.readEnvironment();
        PropertiesFromEnvironment.readFiles();
        configuration = PropertiesFromEnvironment.getConfiguration();
        logger.debug("Configuration: {}", configuration);
    }

}
