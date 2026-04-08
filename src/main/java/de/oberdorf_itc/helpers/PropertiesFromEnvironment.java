package de.oberdorf_itc.helpers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import de.oberdorf_itc.helpers.ConfigObject;

public class PropertiesFromEnvironment {
    private static Map<String, Object> configuration = new HashMap<String, Object>();
    private static java.util.List<ConfigObject> configObjects = new java.util.ArrayList<>();
    private final static Logger logger = LoggerFactory.getLogger(de.oberdorf_itc.helpers.PropertiesFromEnvironment.class);

    /**
     * Populate the configObjects list with the known environment variables, their type and default value. The type can be "string",#
     * "int" or "boolean". The default value is used if the environment variable is not found or has an invalid value.
     */
    static {
        // MQTT environment
        configObjects.add(new ConfigObject("MQTT_SERVER", "string", "localhost"));
        configObjects.add(new ConfigObject("MQTT_PORT", "int", "1883"));
        configObjects.add(new ConfigObject("MQTT_PROTOCOL_VERSION", "string", "3"));
        configObjects.add(new ConfigObject("MQTT_TLS", "boolean", "false"));
        configObjects.add(new ConfigObject("MQTT_CACERT_FILE", "string", "/etc/ssl/certs/ca-certificates.crt"));
        configObjects.add(new ConfigObject("MQTT_TLS_INSECURE", "boolean", "false"));
        configObjects.add(new ConfigObject("MQTT_CLIENT_ID", "string", null));
        configObjects.add(new ConfigObject("MQTT_USERNAME", "string", null));
        configObjects.add(new ConfigObject("MQTT_PASSWORD", "string", null));
        configObjects.add(new ConfigObject("MQTT_PASSWORD_FILE", "string", null));
        configObjects.add(new ConfigObject("MQTT_TOPIC_DOOR_ACCESS", "string", null));
        // LDAP environment
        configObjects.add(new ConfigObject("LDAP_SERVER", "string", "localhost"));
        configObjects.add(new ConfigObject("LDAP_PORT", "int", "389"));
        configObjects.add(new ConfigObject("LDAP_TLS", "boolean", "false"));
        configObjects.add(new ConfigObject("LDAP_USERDN", "string", null));
        configObjects.add(new ConfigObject("LDAP_PASSWORD", "string", null));
        configObjects.add(new ConfigObject("LDAP_PASSWORD_FILE", "string", null));
        configObjects.add(new ConfigObject("LDAP_BASEDN", "string", null));
        configObjects.add(new ConfigObject("LDAP_FILTER", "string", "(&(objectclass=ipHost)(objectclass=oitcACSAccessPointExtension)(ipHostNumber={entrypoint_ip}))"));
        // Prometheus configuration
        configObjects.add(new ConfigObject("PROMETHEUS_LISTENER_ADDR", "string", "0.0.0.0"));
        configObjects.add(new ConfigObject("PROMETHEUS_LISTENER_PORT", "int", "8080"));
        // Timezone configuration
        configObjects.add(new ConfigObject("TZ", "string", "UTC"));
    }

    /**
     * This method reads the configuration from environment variables and stores them in the configuration map. The list of known environment variables is defined in the method. If an environment variable is found, its value is stored in the configuration map after removing any surrounding quotes.
     * @param None
     * @return None
     * @throws IOException if an I/O error occurs while reading environment variables
     */
    public static void readEnvironment() {
        logger.trace("Method: readEnvironment()");

        for (ConfigObject configObject : configObjects) {
            logger.trace("Try to get environment variable {}", configObject.getAttribute());
            String value = System.getenv(configObject.getAttribute());
            if (value == null && configObject.getDefaultValue() != null) {
                logger.trace("Environment variable {} not found, use the default value {}", configObject.getAttribute(), configObject.getDefaultValue());
                value = configObject.getDefaultValue();
            } else if (value == null && configObject.getDefaultValue() == null) {
                logger.warn("Environment variable {} not found and no default value defined", configObject.getAttribute());
                continue;
            }
            logger.trace("Store environment variable {}={} as {}", configObject.getAttribute(), value, configObject.getType());

            // validate value according to type
            if (
                configObject.getType().equals("int")) {
                try {
                    configuration.put(configObject.getAttribute(), Integer.parseInt(value));
                    continue;
                } catch (NumberFormatException e) {
                    logger.warn("Environment variable {} has invalid value: {}", configObject.getAttribute(), value);
                    continue;
                }
            } else if (configObject.getType().equals("boolean")) {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) {
                    logger.warn("Environment variable {} has invalid value: {}", configObject.getAttribute(), value);
                    continue;
                } else {
                    if (value.equalsIgnoreCase("true")) {
                        configuration.put(configObject.getAttribute(), Boolean.TRUE);
                    } else {
                        configuration.put(configObject.getAttribute(), Boolean.FALSE);
                    }
                    continue;
                }
            } else {
                // remove surrounding quotes if present
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                configuration.put(configObject.getAttribute(), value);
            }
        }
    }

    /**
     * Read file content and replace configuration value with file content if the environment variable name ends with "_FILE". The method iterates through the configuration map and checks if any key ends with "_FILE". If it finds such a key, it reads the content of the file specified by the value of that key and replaces the value in the configuration map with the content of the file. If an error occurs while reading the file, it logs a warning message and continues with the next key.
     * @param None
     * @return None
     * @throws IOException if an I/O error occurs while reading the file
     */
    public static void readFiles() {
        logger.trace("Method: readFiles()");

        // Collect keys ending with "_FILE" to avoid ConcurrentModificationException
        java.util.List<String> fileKeys = new java.util.ArrayList<>();
        for (String key : configuration.keySet()) {
            if (key.endsWith("_FILE") && key != "MQTT_CACERT_FILE") {
                fileKeys.add(key);
            }
        }

        for (String key : fileKeys) {
            String filePath = (String) configuration.get(key);
            // check if file exists and is readable
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            if (!java.nio.file.Files.exists(path)) {
                logger.warn("File {} for configuration key {} does not exist", filePath, key);
                continue;
            }
            if (!java.nio.file.Files.isReadable(path)) {
                logger.warn("File {} for configuration key {} is not readable", filePath, key);
                continue;
            }
            try {
                String fileContent = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(filePath)));
                String configKey = key.substring(0, key.length() - 5); // remove "_FILE" suffix
                configuration.put(configKey, fileContent.trim());
                logger.trace("Read file content from {} and stored it in configuration key {}", filePath, configKey);
            } catch (IOException e) {
                logger.warn("Failed to read file {} for configuration key {}: {}", filePath, key, e.getMessage());
            }
        }
    }

    /**
     * This method returns the configuration map that contains the environment variables and their values.
     * @param None
     * @return Map<String, Object> - the configuration map
     */
    public static Map<String, Object> getConfiguration() {
        logger.trace("Method: getConfiguration()");
        return configuration;
    }
}
