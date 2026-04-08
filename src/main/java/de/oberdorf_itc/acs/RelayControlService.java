package de.oberdorf_itc.acs;

// Import Java Libraries
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
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
    private static Map<String, String> configuration = new HashMap<String, String>();

    public static void main(String[] args) throws IOException, InterruptedException {
        // read value of Implementation-Version from META-INF/MANIFEST.MF of this class
        Manifest_helper MF = new Manifest_helper();
        logger.info("OITC Access Control System: Relay Control Service v{} started", MF.getImplementationVersion());
        logger.info("(C) Copyright by Michael Oberdorf IT-Consulting 2015-2026, https://www.oberdorf-itc.de/)");

        getPropertiesFromEnvironment();
    }




    /**
     * This method reads the configuration from environment variables and stores them in the configuration map. The list of known environment variables is defined in the method. If an environment variable is found, its value is stored in the configuration map after removing any surrounding quotes.
     * @param None
     * @return None
     * @throws IOException if an I/O error occurs while reading environment variables
     */
    private static void getPropertiesFromEnvironment() {
        logger.trace("Method: getPropertiesFromEnvironment()");

        // The list of known environment variables
        List<String> attributes = Arrays.asList(
            // MQTT environment
            "MQTT_SERVER",
            "MQTT_PORT",
            "MQTT_PROTOCOL_VERSION",
            "MQTT_TLS",
            "MQTT_CACERT_FILE",
            "MQTT_TLS_INSECURE",
            "MQTT_CLIENT_ID",
            "MQTT_USERNAME",
            "MQTT_PASSWORD",
            "MQTT_PASSWORD_FILE",
            "MQTT_TOPIC_DOOR_ACCESS",
            // LDAP environment
            "LDAP_SERVER",
            "LDAP_PORT",
            "LDAP_TLS",
            "LDAP_USERDN",
            "LDAP_PASSWORD",
            "LDAP_PASSWORD_FILE",
            "LDAP_BASEDN",
            "LDAP_FILTER",
            // Prometheus configuration
            "PROMETHEUS_LISTENER_ADDR",
            "PROMETHEUS_LISTENER_PORT",
            // Timezone configuration
            "TZ"
        );

        for (String attribute : attributes) {
            logger.trace("Try to get environment variable {}", attribute);
            String value = System.getenv(attribute);
            if (value != null) {
                logger.trace("Store environment variable {}={}", attribute, value);
                configuration.put(attribute, value.replaceAll("^\"|\"$", ""));
            }
        }
    }
}
