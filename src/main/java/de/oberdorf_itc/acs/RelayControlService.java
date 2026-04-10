package de.oberdorf_itc.acs;

// Import Java Libraries
import de.oberdorf_itc.helpers.Manifest_helper;
import de.oberdorf_itc.helpers.PropertiesFromEnvironment;
import de.oberdorf_itc.helpers.AccessObject;
import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import io.prometheus.metrics.core.metrics.Info;
import io.prometheus.metrics.core.metrics.Counter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.HashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
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
    private static Map<String, Counter> metrics = new HashMap<String, Counter>();
    private static Info service_info;

    public static void main(String[] args) throws IOException,InterruptedException {
        // read value of Implementation-Version from META-INF/MANIFEST.MF of this class
        Manifest_helper MF = new Manifest_helper();
        logger.info("OITC Access Control System: Relay Control Service v{} started", MF.getImplementationVersion());
        logger.info("(C) Copyright by Michael Oberdorf IT-Consulting 2015-2026, https://www.oberdorf-itc.de/)");

        // read environment variables and store them in the configuration map
        PropertiesFromEnvironment.readEnvironment();
        PropertiesFromEnvironment.readFiles();
        configuration = PropertiesFromEnvironment.getConfiguration();

        // initialize Prometheus exporter
        try {
            initializePrometheusExporter();
        } catch (IOException e) {
            logger.error("Error initializing Prometheus exporter: {}", e.getMessage());
            System.exit(1);
        }
        service_info.addLabelValues(MF.getImplementationVersion(), "Michael Oberdorf <info@oberdorf-itc.de>", "production", (String) configuration.get("TZ"));

        try {
            initializeMqttClient();
        } catch (MqttException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("Error initializing MQTT client: {}", e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Initialize prometheus exporter to expose metrics on the configured address and port. Register JVM metrics to monitor the performance of the service.
     * @param None
     * @return None
     * @throws IOException if an I/O error occurs while initializing the Prometheus exporter
     */
    private static void initializePrometheusExporter() throws IOException {
        logger.trace("Method: initializePrometheusExporter()");

        JvmMetrics.builder().register();
        @SuppressWarnings("unused")
        HTTPServer server = HTTPServer.builder()
            .port((int) configuration.get("PROMETHEUS_LISTENER_PORT"))
            .inetAddress(InetAddress.getByName((String) configuration.get("PROMETHEUS_LISTENER_ADDR")))
            .buildAndStart();
        logger.info("Prometheus metrics listener started on http://{}:{}/metrics", configuration.get("PROMETHEUS_LISTENER_ADDR"), configuration.get("PROMETHEUS_LISTENER_PORT"));
        // Thread.currentThread().join();


        service_info = Info.builder()
            .name("service_info")
            .help("Information about the service")
            .labelNames("version", "author", "status", "timezone")
            .register();

        metrics.put("mqtt_messages", Counter.builder()
            .name("mqtt_messages")
            .help("Count all received MQTT messages")
            .register()
        );
        metrics.put("mqtt_messages_refused", Counter.builder()
            .name("mqtt_messages_refused")
            .help("Count all refused MQTT messages due to timestamp drift.")
            .register()
        );
        metrics.put("acs_access_granted", Counter.builder()
            .name("acs_access_granted")
            .help("Count all access granted events received by transponders")
            .labelNames("entrypoint_ip")
            .register()
        );
        metrics.put("acs_access_denied", Counter.builder()
            .name("acs_access_denied")
            .help("Count all access denied events received by transponders")
            .labelNames("entrypoint_ip")
            .register()
        );
        metrics.put("acs_relays_triggered", Counter.builder()
            .name("acs_relays_triggered")
            .help("Count all relay triggering events")
            .labelNames("entrypoint_ip")
            .register()
        );
    }


    /**
     * Initialize MQTT client and connect to the MQTT broker. Subscribe to the topic for door access events and set up a callback
     * to handle incoming messages. The callback should check if the message indicates that a door should be unlocked, and if so,
     * trigger the relay to unlock the door.
     * @param None
     * @return None
     * @throws IOException if an I/O error occurs while initializing the MQTT client or connecting to the MQTT broker
     * @throws InterruptedException if the thread is interrupted while waiting for messages
     * @throws MqttException if the MQTT Client could not be initialized
     * @throws CertificateException if an error occurs while processing the certificate for TLS connection
     * @throws KeyStoreException if an error occurs while initializing the KeyStore for TLS connection
     * @throws NoSuchAlgorithmException if the TrustManagers Factory can't get the default algorithm
     * @throws KeyManagementException if an error occurs while managing the keys for TLS connection
     */
    private static void initializeMqttClient() throws IOException, InterruptedException, MqttException, CertificateException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        logger.trace("Method: initializeMqttClient()");

        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setAutomaticReconnect(true);
        connOpts.setConnectionTimeout(10);
        connOpts.setKeepAliveInterval(60);

        String mqttBroker = (String) configuration.get("MQTT_SERVER") + ":" + configuration.get("MQTT_PORT");
        if ((boolean) configuration.get("MQTT_TLS")) {
            mqttBroker = "ssl://" + mqttBroker;

            if ((boolean) configuration.get("MQTT_TLS_INSECURE")) {
                System.setProperty("com.ibm.ssl.disableHostnameVerification", "true");
            }
            if (configuration.get("MQTT_CACERT_FILE") != null) {
                System.setProperty("javax.net.ssl.trustStore", (String) configuration.get("MQTT_CACERT_FILE"));
                InputStream certInput = new FileInputStream((String) configuration.get("MQTT_CACERT_FILE"));
                CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
                Collection<? extends Certificate> certs = certFactory.generateCertificates(certInput);
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);
                int index = 0;
                for (Certificate cert : certs) {
                    String alias = "serverr_ca_" + index++;
                    trustStore.setCertificateEntry(alias, cert);
                }
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());
                SSLContext.setDefault(sslContext);
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                connOpts.setSocketFactory(sslSocketFactory);
            }
        } else {
            mqttBroker = "tcp://" + mqttBroker;
        }
        logger.debug("MQTT Broker: {}", mqttBroker);
        String mqttClientId = (String) configuration.get("MQTT_CLIENT_ID");
        if (mqttClientId == null) {
            mqttClientId = MqttClient.generateClientId();
        }
        logger.debug("MQTT Client ID: {}", mqttClientId);

        if (configuration.get("MQTT_USERNAME") != null && configuration.get("MQTT_PASSWORD") != null) {
            logger.debug("MQTT Username: {}", configuration.get("MQTT_USERNAME"));
            logger.debug("MQTT Password: set");
            connOpts.setUserName((String) configuration.get("MQTT_USERNAME"));
            String password = (String) configuration.get("MQTT_PASSWORD");
            connOpts.setPassword(password.toCharArray());
        } else {
            logger.debug("MQTT Username: not set");
        }

        // Specify directory for storing messages
        logger.debug("Define MQTT persistence directory to: {}", configuration.get("java.io.tmpdir"));
        MqttDefaultFilePersistence persistence = new MqttDefaultFilePersistence((String) configuration.get("java.io.tmpdir"));

        @SuppressWarnings("resource")
        MqttClient mqttClient = new MqttClient(mqttBroker, mqttClientId, persistence);
        try {
            mqttClient.connect(connOpts);
        } catch (MqttException mqttException) {
            logger.error("Error connecting to MQTT server: {}", mqttException.getMessage());
            mqttException.printStackTrace();
            System.exit(1);
        }
        logger.debug("Successfully connected to MQTT server.");
        logger.debug("Register MQTT callbacks.");

        mqttClient.setCallback(new MqttCallback() {
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                logger.debug("Received MQTT message");
                logger.debug("  topic  : {}", topic);
                logger.debug("  qos    : {}", message.getQos());
                String payload = new String(message.getPayload());
                logger.debug("  payload: {}", payload);
                // decode json message and check if it indicates that a door should be unlocked
                // if so, trigger the relay to unlock the door
                metrics.get("mqtt_messages").inc();

                AccessObject accessObject = new AccessObject();
                accessObject.fromJson(payload);

                logger.trace("Current timestamp: {}", Instant.now().atZone(ZoneId.of(configuration.get("TZ").toString())).toString());
                logger.trace("Access timestamp: {}", accessObject.getTimestampInstance().atZone(ZoneId.of(configuration.get("TZ").toString())).toString());

                if (Math.abs(Instant.now().getEpochSecond() - accessObject.getTimestampInstance().getEpochSecond()) > 10) {
                    logger.warn("Received MQTT message with timestamp drift: {}", accessObject.getTimestampInstance().atZone(ZoneId.of(configuration.get("TZ").toString())).toString());
                    metrics.get("mqtt_messages_refused").inc();
                    return;
                }

                if (accessObject.getStatus().equals("denied")) {
                    logger.info("Access denied for entry point {}. No relay will be triggered.", accessObject.getEntrypoint_ip());
                    metrics.get("acs_access_denied").labelValues(accessObject.getEntrypoint_ip()).inc();
                    return;
                }

                logger.info("Access granted for entry point {}. Trigger the relay to unlock the door.", accessObject.getEntrypoint_ip());
                metrics.get("acs_access_granted").labelValues(accessObject.getEntrypoint_ip()).inc();

                // add a new thread to get the relay configuration from LDAP related to entrypoint_ip, after that trigger the relay to open the door
                TriggerRelay triggerRelay = new TriggerRelay(accessObject.getEntrypoint_ip(), configuration, metrics.get("acs_relays_triggered"));
                Thread thread = new Thread(triggerRelay);
                thread.start();
            }
            public void connectionLost(Throwable cause) {
                logger.error("connectionLost: " + cause.getMessage());
            }
            public void deliveryComplete(IMqttDeliveryToken token) {
                logger.debug("deliveryComplete: " + token.isComplete());
            }
        });
        mqttClient.subscribe((String) configuration.get("MQTT_TOPIC_DOOR_ACCESS"), 1);

        // keep the main thread alive to continue receiving messages
        while (true) {
            Thread.sleep(1000);
        }
        // mqttClient.close();
    }

}
