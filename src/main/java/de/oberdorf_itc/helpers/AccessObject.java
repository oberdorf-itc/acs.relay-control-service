package de.oberdorf_itc.helpers;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Class AccessObject represents an mqtt message that contains door access information.
 * Copyright by Michael Oberdorf IT-Consulting 2015-2026<br>
 * Date: 2026-04-08
 * @author Michael Oberdorf
 * @version 1.0.0
 * @see "https://www.oberdorf-itc.de/"
 * @see "https://github.com/oberdorf-itc/acs.documentation/blob/main/docs/mqtt-message-reference.md"
 */
public class AccessObject {
    private final static Logger logger = LoggerFactory.getLogger(de.oberdorf_itc.helpers.AccessObject.class);
    private String timestamp;
    private String entrypoint_ip;
    private String entrypoint_location;
    private String transponder_uid;
    private String user_id;
    private String user_dn;
    private String user_display_name;
    private String status;
    private Boolean notification;

    /**
     * Get the timestamp field of the AccessObject instance. The method returns the value of the timestamp field, which
     * is a String representing the time when the access event occurred. The method also includes a logging statement to trace the execution flow.
     * @param None
     * @return the timestamp value
     */
    public String getTimestamp() {
        logger.trace("Method: getTimestamp()");
        return timestamp;
    }

    /**
     * Get the timestamp as an Instant object. The method attempts to parse the timestamp string into an Instant object.
     * If the parsing is successful, it returns the Instant object. If the parsing fails due to an invalid format, it logs a warning and returns null.
     * @param timestamp
     * @return the timestamp as an Instant object, or null if the format is invalid
     */
    public Instant getTimestampInstance() {
        logger.trace("Method: getTimestampInstance()");
        try {
            Instant instant = Instant.parse(this.timestamp);
            return instant;
        } catch (Exception e) {
            logger.warn("Invalid timestamp format: {}", this.timestamp);
            return null;
        }
    }

    /**
     * Set the timestamp field of the AccessObject instance. The method takes a String parameter
     * and assigns it to the timestamp field. It also includes a logging statement to trace the execution flow and log the value being set.
     * @param timestamp
     * @return None
     */
    private void setTimestamp(String timestamp) {
        logger.trace("Method: setTimestamp(String {})", timestamp);
        this.timestamp = timestamp;
    }

    /**
     * Get the entrypoint_ip field of the AccessObject instance. The method returns the value of the entrypoint_ip field, which is a
     * String representing the IP address of the access point where the access event occurred. The method also includes a logging
     * statement to trace the execution flow.
     * @param None
     * @return the entrypoint_ip value
     */
    public String getEntrypoint_ip() {
        logger.trace("Method: getEntrypoint_ip()");
        return entrypoint_ip;
    }

    /**
     * Set the entrypoint_ip field of the AccessObject instance. The method takes a String parameter and assigns it to the entrypoint_ip field.
     * It also includes a logging statement to trace the execution flow and log the value being set.
     * @param entrypoint_ip
     * @return None
     */
    private void setEntrypoint_ip(String entrypoint_ip) {
        logger.trace("Method: setEntrypoint_ip(String {})", entrypoint_ip);
        this.entrypoint_ip = entrypoint_ip;
    }

    /**
     * Get the entrypoint_location field of the AccessObject instance. The method returns the value of the entrypoint_location field,
     * which is a String representing the location of the access point where the access event occurred. The method also includes a
     * logging statement to trace the execution flow.
     * @param None
     * @return the entrypoint_location value
     */
    public String getEntrypoint_location() {
        logger.trace("Method: getEntrypoint_location()");
        return entrypoint_location;
    }

    /**
     * Set the entrypoint_location field of the AccessObject instance. The method takes a String parameter and assigns it
     * to the entrypoint_location field. It also includes a logging statement to trace the execution flow and log the value being set.
     * @param entrypoint_location
     * @return None
     */
    private void setEntrypoint_location(String entrypoint_location) {
        logger.trace("Method: setEntrypoint_location(String {})", entrypoint_location);
        this.entrypoint_location = entrypoint_location;
    }

    /**
     * Get the transponder_uid field of the AccessObject instance. The method returns the value of the transponder_uid field,
     * which is a String representing the unique identifier of the transponder used for access. The method also includes a
     * logging statement to trace the execution flow.
     * @param None
     * @return the transponder_uid value
     */
    public String getTransponder_uid() {
        logger.trace("Method: getTransponder_uid()");
        return transponder_uid;
    }

    /**
     * Set the transponder_uid field of the AccessObject instance. The method takes a String parameter and assigns it to
     * the transponder_uid field. It also includes a logging statement to trace the execution flow and log the value being set.
     * @param transponder_uid
     * @return None
     */
    private void setTransponder_uid(String transponder_uid) {
        logger.trace("Method: setTransponder_uid(String {})", transponder_uid);
        this.transponder_uid = transponder_uid;
    }

    /**
     * Get the user_id field of the AccessObject instance. The method returns the value of the user_id field, which is
     * a String representing the unique identifier of the user associated with the access event. The method also includes
     * a logging statement to trace the execution flow.
     * @param None
     * @return the user_id value
     */
    public String getUser_id() {
        logger.trace("Method: getUser_id()");
        return user_id;
    }

    /**
     * Set the user_id field of the AccessObject instance. The method takes a String parameter and assigns it to
     * the user_id field. It also includes a logging statement to trace the execution flow and log the value being set.
     * @param user_id
     * @return None
     */
    private void setUser_id(String user_id) {
        logger.trace("Method: setUser_id(String {})", user_id);
        this.user_id = user_id;
    }

    /**
     * Get the user_dn field of the AccessObject instance. The method returns the value of the user_dn field, which is a String
     * representing the distinguished name of the user associated with the access event. The method also includes a logging statement to trace the execution flow.
     * @param None
     * @return the user_dn value
     */
    public String getUser_dn() {
        logger.trace("Method: getUser_dn()");
        return user_dn;
    }

    /**
     * Set the user_dn field of the AccessObject instance. The method takes a String parameter and assigns it to the user_dn field.
     * It also includes a logging statement to trace the execution flow and log the value being set.
     * @param user_dn
     * @return None
     */
    private void setUser_dn(String user_dn) {
        logger.trace("Method: setUser_dn(String {})", user_dn);
        this.user_dn = user_dn;
    }

    /**
     * Get the user_display_name field of the AccessObject instance. The method returns the value of the user_display_name field,
     * which is a String representing the display name of the user associated with the access event. The method also includes a
     * logging statement to trace the execution flow.
     * @param None
     * @return the user_display_name value
     */
    public String getUser_display_name() {
        logger.trace("Method: getUser_display_name()");
        return user_display_name;
    }

    /**
     * Set the user_display_name field of the AccessObject instance. The method takes a String parameter and assigns it to
     * the user_display_name field. It also includes a logging statement to trace the execution flow and log the value being set.
     * @param user_display_name
     * @return None
     */
    private void setUser_display_name(String user_display_name) {
        logger.trace("Method: setUser_display_name(String {})", user_display_name);
        this.user_display_name = user_display_name;
    }

    /**
     * Get the status field of the AccessObject instance. The method returns the value of the status field, which is a String indicating
     * whether access was granted or denied. The method also includes a logging statement to trace the execution flow.
     * @param None
     * @return the status value
     */
    public String getStatus() {
        logger.trace("Method: getStatus()");
        return status;
    }

    /**
     * Set the status field of the AccessObject instance. The method takes a String parameter and assigns it to the status field.
     * It also includes a logging statement to trace the execution flow and log the value being set
     * @param status
     * @return None
     */
    private void setStatus(String status) {
        logger.trace("Method: setStatus(String {})", status);
        if (!status.equals("granted") && !status.equals("denied")) {
            logger.warn("Invalid status value: {}", status);
            return;
        }
        this.status = status;
    }

    /**
     * Get the notification field of the AccessObject instance. The method returns the value of the notification field,
     * which is a Boolean indicating whether a notification should be sent for this access event. The method also includes
     * a logging statement to trace the execution flow.
     * @param None
     * @return the notification value
     */
    public Boolean getNotification() {
        logger.trace("Method: getNotification()");
        return notification;
    }

    /**
     * Set the notification field of the AccessObject instance. The method takes a Boolean parameter and assigns it to the
     * notification field. It also includes a logging statement to trace the execution flow and log the value being set.
     * @param notification
     * @return None
     */
    private void setNotification(Boolean notification) {
        logger.trace("Method: setNotification(Boolean {})", notification);
        this.notification = notification;
    }

    /**
     * Parse a JSON string and populate the AccessObject fields with the corresponding values from the JSON.
     * The method uses the Gson library to parse the JSON string and create an AccessObject instance. It then
     * sets the fields of the current AccessObject instance with the values from the parsed AccessObject instance.
     * The method also includes logging statements to trace the execution flow and log any warnings if the status
     * value is invalid.
     * @param json
     * @return None
     */
    public void fromJson(String json) {
        logger.trace("Method: parseJson(String {})", json);
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        AccessObject accessObject = gson.fromJson(json, AccessObject.class);
        this.setTimestamp(accessObject.getTimestamp());
        this.setEntrypoint_ip(accessObject.getEntrypoint_ip());
        this.setEntrypoint_location(accessObject.getEntrypoint_location());
        this.setTransponder_uid(accessObject.getTransponder_uid());
        this.setUser_id(accessObject.getUser_id());
        this.setUser_dn(accessObject.getUser_dn());
        this.setUser_display_name(accessObject.getUser_display_name());
        this.setStatus(accessObject.getStatus());
        this.setNotification(accessObject.getNotification());
    }
}
