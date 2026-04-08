package de.oberdorf_itc.helpers;

/**
 * Class Manifest_helper - to parse Data from local MANIFEST.MF<br>
 * Copyright by Michael Oberdorf IT-Consulting 2015-2026,<br>
 * Date: 2015-09-18
 * @author Michael Oberdorf
 * @version 1.0.0
 * @see "http://www.oberdorf-itc.de/"
 */
public class Manifest_helper {
	/**
	 * getImplementationVendorId - method to get value of Implementation-Vendor-Id
	 * @return String (value of Implementation-Vendor-Id)
	 */
	public String getImplementationVendorId() {
		return getClass().getPackage().getName();
    }

	/**
	 * getImplementationTitle - method to get value of Implementation-Title
	 * @return String (value of Implementation-Title)
	 */
	public String getImplementationTitle() {
		return getClass().getPackage().getImplementationTitle();
    }

	/**
	 * getImplementationVersion - method to get value of Implementation-Version
	 * @return String (value of Implementation-Version)
	 */
	public String getImplementationVersion() {
		return getClass().getPackage().getImplementationVersion();
    }
}
