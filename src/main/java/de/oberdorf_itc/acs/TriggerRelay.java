package de.oberdorf_itc.acs;

import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import io.prometheus.metrics.core.metrics.Counter;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
| `LDAP_BASEDN`               | The search base DN where the relay configuration can be found.                                  | **MANDATORY** |                                             |
| `LDAP_FILTER`               | The LDAP filter to search for the relay configuration.                                          | OPTIONAL      | `(&(objectclass=ipHost)(objectclass=oitcACSAccessPointExtension)(ipHostNumber={entrypoint_ip}))` |
 */


/**
 * Class TriggerRelay is to get the relay specific information from the LDAP server and to trigger the relay to unlock the door.
 * This service is NO standalone service. It requires an OITC ACS system to be running.
 * Copyright by Michael Oberdorf IT-Consulting 2015-2026<br>
 * Date: 2015-08-27
 * @author Michael Oberdorf
 * @version 1.0.0
 * @see "https://www.oberdorf-itc.de/"
 */
public class TriggerRelay {
    private final static Logger logger = LoggerFactory.getLogger(de.oberdorf_itc.acs.TriggerRelay.class);
    private Map<String, Object> configuration = new HashMap<String, Object>();
    private Counter accessGrantedCounter;
    private String entryPointIP;
    private LDAPConnection connection = null;
    private String relayType;
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

        // connect to LDAP server and bind with technical user
        ldapConnector();
        closeLdapConnection();
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
        logger.debug(" - User DN: {}", ldap_user_dn);
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
      public void closeLdapConnection()
        {
        logger.trace("Method: closeLdapConnection()");
        logger.debug("Disconnect from LDAP");
        connection.close();
        }


}
