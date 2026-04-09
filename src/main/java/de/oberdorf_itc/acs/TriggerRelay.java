package de.oberdorf_itc.acs;

import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import de.oberdorf_itc.mcp2200.MCP2200lib;
import io.prometheus.metrics.core.metrics.Counter;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class TriggerRelay is to get the relay specific information from the LDAP server and to trigger the relay to unlock the door.
 * This service is NO standalone service. It requires an OITC ACS system to be running.
 * Copyright by Michael Oberdorf IT-Consulting 2015-2026<br>
 * Date: 2016-04-08
 * @author Michael Oberdorf
 * @version 1.0.0
 * @see "https://www.oberdorf-itc.de/"
 */
public class TriggerRelay implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(de.oberdorf_itc.acs.TriggerRelay.class);
    private final static byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);
    private JSONObject relayTypes;
    private Map<String, Object> configuration = new HashMap<String, Object>();
    private Counter accessGrantedCounter;
    private String entryPointIP;
    private LDAPConnection connection = null;
    private byte[] relayType;
    private String relayConfig;


    /**
     * Constructor for TriggerRelay class. It initializes the entryPointIP variable with the provided value.
     * @param entryPointIP - The IP address of the entry point for which the relay should be triggered. This IP address is used to identify the specific entry point in the LDAP server and to retrieve the corresponding relay configuration.
     * @param configuration - The configuration object
     * @param accessGrantedCounter - Counter to track the number of times access has been granted for a specific entry point IP address. This counter is used to monitor and analyze access patterns, and it can be incremented each time access is granted for the corresponding entry point IP.
     * @return None
     */
    public TriggerRelay(String entryPointIP, Map<String, Object> configuration, Counter accessGrantedCounter) {
        logger.trace("Constructor: TriggerRelay(String entryPointIP={})", entryPointIP);
        this.entryPointIP = entryPointIP;
        this.configuration = configuration;
        this.accessGrantedCounter = accessGrantedCounter;

        // get the file path of the relayTypes.json file and load it into JSONObject
        try {
            URL rtURI = de.oberdorf_itc.acs.TriggerRelay.class.getResource("/relayTypes.json");
            Path rtPath = Path.of(new File(rtURI.toURI()).getAbsolutePath());
            String rtContent = Files.readString(rtPath, Charset.defaultCharset());
            this.relayTypes = new JSONObject(rtContent);
        } catch (URISyntaxException e) {
            logger.error("Failed to get the file path of relayTypes.json: {}", e.toString());
        } catch (Exception e) {
            logger.error("Failed to read the content of relayTypes.json: {}", e.toString());
        }
    }

    /**
     * overwrites the run method to start the tread in background to get the configuration and trigger the relay to open the door.
     * @params None
     * @return None
     */
    @Override
    public void run() {
        triggerRelay();
    }

    /**
     * Triggers the relay to unlock the door. The method performs the following steps:
     * 1. Connects to the LDAP server and binds with a technical user.
     * 2. Searches the LDAP server for the entry point using the provided IP address and retrieves the relay configuration.
     * 3. Closes the LDAP connection.
     * 4. Triggers the relay based on the retrieved configuration.
     * @param None
     * @return None
     */
    private void triggerRelay() {
        logger.trace("Method: triggerRelay()");

        // connect to LDAP server and bind with technical user, search in ldap for entry point and get relay configuration, close ldap connection
        logger.debug("Get relay configuration for entry point IP: {}", this.entryPointIP);
        ldapConnector();
        getRelayConfiguration();
        closeLdapConnection();

        logger.debug("Trigger relay of type {} with config {}", bytesToHex(this.relayType), this.relayConfig);
        // get additional configuration from relayTypes configuration
        JSONObject relayInformation = this.relayTypes.optJSONObject(bytesToHex(this.relayType));
        String relayVendor = null;
        String relayProduct = null;
        String relayProductURL = null;
        String relayType = null;
        JSONObject relayDefaultConfiguration = null;

        if (relayInformation != null) {
            logger.debug("Relay information:");
            relayVendor = relayInformation.optString("vendor");
            logger.debug(" - Vendor: {}", relayVendor);
            relayProduct = relayInformation.optString("product");
            logger.debug(" - Product: {}", relayProduct);
            relayProductURL = relayInformation.optString("url");
            logger.debug(" - Product URL: {}", relayProductURL);
            relayType = relayInformation.optString("type");
            logger.debug(" - Type: {}", relayType);
            relayDefaultConfiguration = relayInformation.optJSONObject("defaults");
            logger.debug(" - Default relay configuration: {}", relayDefaultConfiguration.toString());
        }

        switch(bytesToHex(this.relayType)) {
            case "00":
                logger.info("This is a dummy relay type. Nothing to trigger.");
                break;
            case "01":
                logger.info("Trigger {} - {}", relayVendor, relayProduct);
                // get the configuration from LDAP
                JSONObject relayConfigLDAP = new JSONObject(this.relayConfig);
                int relayNumber;
                try {
                    relayNumber = relayConfigLDAP.getInt("relayNumber");
                } catch (JSONException e) {
                    logger.warn("No relay number in relay configuration in LDAP, using default value.");
                    relayNumber = relayDefaultConfiguration.optInt("relayNumber", 0);
                }
                int relayTriggerTime;
                try {
                    relayTriggerTime = relayConfigLDAP.getInt("relayTriggerTime");
                } catch (JSONException e) {
                    logger.warn("No relay trigger time in relay configuration in LDAP, using default value.");
                    relayTriggerTime = relayDefaultConfiguration.optInt("relayTriggerTime", 500);
                }
                triggerMCP2200((int) relayNumber, (int) relayTriggerTime);
                accessGrantedCounter.labelValues(this.entryPointIP).inc();
                break;
            default:
                logger.error("Relay type {} unknown!", this.relayType);
        }
    }

    /**
     * Open connection to LDAP server and bind with technical user
     * @params None
     * @return None
     */
    private void ldapConnector() {
        logger.trace("Method: ldapConnector()");

        logger.debug("Get LDAP Server connection parameters.");
        String ldap_server     = configuration.get("LDAP_SERVER").toString();
        logger.debug(" - Server: {}", ldap_server);
        int ldap_port          = (int) configuration.get("LDAP_PORT");
        logger.debug(" - TCP Port: {}", ldap_port);
        boolean ldap_tls        = (boolean) configuration.get("LDAP_TLS");
        logger.debug(" - Using TLS: {}", ldap_tls);
        String ldap_user_dn     = configuration.get("LDAP_USERDN") != null ? configuration.get("LDAP_USERDN").toString() : null;
        if (ldap_user_dn != null) {
            logger.debug(" - User DN: {}", ldap_user_dn);
        } else {
            logger.debug(" - User DN: not set");
        }
        String ldap_password    = configuration.get("LDAP_PASSWORD") != null ? configuration.get("LDAP_PASSWORD").toString() : null;
        logger.debug(" - User password: {}", ldap_password != null ? "set" : "not set");

        logger.debug("Connect to LDAP Server: {}:{}", ldap_server, ldap_port);
        if (ldap_tls == false) {
            connection = new LDAPConnection();
            try {
                connection.connect(ldap_server, ldap_port);
            } catch (LDAPException e) {
                logger.error("Failed to connect to LDAP server: {}", e.toString());
            }
        } else {
            SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
            try {
                SSLSocketFactory socketFactory = sslUtil.createSSLSocketFactory();
                connection = new LDAPConnection(socketFactory, ldap_server, ldap_port);
            } catch (LDAPException | GeneralSecurityException e) {
                logger.error("Failed to connect to LDAP server: {}", e.toString());
            }
        }

        // Authentication
        if (ldap_user_dn != null && ldap_password != null) {
            logger.debug("Bind to LDAP with DN: {} ", ldap_user_dn);
            BindResult result = null;
            try {
                result = connection.bind(ldap_user_dn, ldap_password);
            } catch (LDAPException e) {
                logger.error(e.toString());
            }

            logger.trace(">>>> ldap result code: {}", result.getResultCode().intValue());
            if (result.getResultCode().intValue() != 0) {
                logger.error("LDAP error: {} (RC {})", result.getDiagnosticMessage(), result.getResultCode().intValue());
                connection.close();
                System.exit(1);
            }
        }
    }

    /**
     * closeLdapConnection - close the LDAP connection from server
     * @param None
     * @return None
     */
    private void closeLdapConnection() {
        logger.trace("Method: closeLdapConnection()");
        logger.debug("Disconnect from LDAP");
        connection.close();
    }

    /**
     * Search in the Directory Server for the Entry point to retrieve the relay configuration.
     * The entry point is identified by the IP address that is passed as a parameter to the method.
     * @param None
     * @return None
     */
    private void getRelayConfiguration() {
	    logger.trace("Method: getRelayConfiguration()");

        String searchBaseDN = configuration.get("LDAP_BASEDN").toString();
        logger.debug("Search for entry point in LDAP with base DN: {}", searchBaseDN);
        String searchFilter = configuration.get("LDAP_FILTER").toString().replace("{entrypoint_ip}", entryPointIP);
        logger.debug("Search filter: {}", searchFilter);

        // search for resource access point and get ruleset
        SearchResult searchResult = null;
	    try {
	        searchResult = connection.search(searchBaseDN, SearchScope.SUB, searchFilter, "oitcACSRelayType", "oitcACSRelayConfiguration");
	    } catch (LDAPSearchException e) {
	        logger.error("Failed to search LDAP: {}", e.toString());
            return;
        }
	    if (searchResult.getResultCode().intValue() != 0 || searchResult.getEntryCount() != 1) {
	        logger.error("Access Point can't be identified in Database. (LDAP Result: " + searchResult.getResultCode().intValue() + ").");
	        return;
	    }

	    for (SearchResultEntry e : searchResult.getSearchEntries()) {
	        this.relayType = e.getAttributeValueBytes("oitcACSRelayType");
            this.relayConfig = e.getAttributeValue("oitcACSRelayConfiguration");
	        break;
	    }

	    logger.trace("Relay Type: {}", bytesToHex(this.relayType));
	    logger.trace("Relay Config: {}", this.relayConfig);
	}

    /**
     * Helper method to convert byte array to hex string. This is used to convert the relayType byte array to a readable string format for logging and debugging purposes.
     * @param bytes - The byte array that represents the relay type retrieved from the LDAP server. This byte array is converted to a hexadecimal string representation for easier readability and logging.
     * @return String - The hexadecimal representation of the input byte array. Each byte is converted to its corresponding two-character hexadecimal string, and the resulting string is a concatenation of all the hexadecimal values.
     */
    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    /**
     * Helper method to convert a hexadecimal string to a byte array.
     * @param s - The hexadecimal string to convert.
     * @return byte[] - The resulting byte array.
     */
    public static byte[] hexToBytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                             + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * triggerMCP2200 - Trigger a relay from the Denkovi USB4v2 board
     * @param relayID (integer, relay number [1..4])
     * @param relayTriggerTime (integer, trigger time in milliseconds)
     * @return None
     */
    private static void triggerMCP2200(int relayID, int relayTriggerTime) {
        logger.trace("Method triggerMCP2200(int relayID={}, int relayTriggerTime={})", relayID, relayTriggerTime);
        MCP2200lib MCPDevice = new MCP2200lib();
        // trigger relay
        logger.trace("Try to connect to first Denkovi USB4v2 device");
        if (MCPDevice.connectToFirstDevice() != 0) {
            logger.error("Can't connect to Denkovi USB4v2 device. Check if the Device is connected to USB port!");
            return;
        }

        logger.trace("Connection established!");
        logger.trace("Trigger relay no. {} for {} milliseconds.", relayID, relayTriggerTime);
        MCPDevice.setSingleRelayFlipFlop(relayID, true, relayTriggerTime);

        // close connection to USB device
        logger.trace("Closing connection to denkovi USB4v2 device.");
        MCPDevice.close();
    }
}
