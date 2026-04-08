package de.oberdorf_itc.helpers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class ConfigObject represents a configuration object with an attribute, type, and default value.
 * Copyright by Michael Oberdorf IT-Consulting 2015-2026<br>
 * Date: 2016-04-08
 * @author Michael Oberdorf
 * @version 1.0.0
 * @see "https://www.oberdorf-itc.de/"
 */
public class ConfigObject {
    private final static Logger logger = LoggerFactory.getLogger(de.oberdorf_itc.helpers.ConfigObject.class);
    String attribute;
    String type;
    String defaultValue;

    public String getAttribute() {
        logger.trace("Method: getAttribute()");
        return attribute;
    }
    public void setAttribute(String attribute) {
        logger.trace("Method: setAttribute(String {})", attribute);
        this.attribute = attribute;
    }
    public String getType() {
        logger.trace("Method: getType()");
        return type;
    }
    public void setType(String type) {
        logger.trace("Method: setType(String {})", type);
        this.type = type;
    }
    public String getDefaultValue() {
        logger.trace("Method: getDefaultValue()");
        return defaultValue;
    }
    public void setDefaultValue(String defaultValue) {
        logger.trace("Method: setDefaultValue(String {})", defaultValue);
        this.defaultValue = defaultValue;
    }

    ConfigObject(String attribute, String type, String defaultValue) {
        logger.trace("Method: ConfigObject(String {}, String {}, String {})", attribute, type, defaultValue);
        this.attribute = attribute;
        this.type = type;
        this.defaultValue = defaultValue;
    }

}
