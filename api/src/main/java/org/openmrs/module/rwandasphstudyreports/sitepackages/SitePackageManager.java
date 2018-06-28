package org.openmrs.module.rwandasphstudyreports.sitepackages;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Location;
import org.openmrs.api.context.Context;

/**
 * The SPH study categorised sites into packages, package one including all
 * their upgraded 56 sites, package 2 and 3 are managed by a global property
 *
 */
public class SitePackageManager {
	public static String GP_PACKAGE_2_SITES_FOSIDS = "rwandasphstudyreports.package2FosIDS";
	public static String GP_PACKAGE_3_SITES_FOSIDS = "rwandasphstudyreports.package3FosIDS";
	public static String GP_CURRENT_LOCATION = "mohtracportal.defaultLocationId";

	private static String getDefaultFOSID() {
		String defaultLocationId = Context.getAdministrationService().getGlobalProperty(GP_CURRENT_LOCATION);
		if (StringUtils.isNotBlank(defaultLocationId)) {
			Location defaultLocation = Context.getLocationService().getLocation(Integer.parseInt(defaultLocationId));
			if (defaultLocation != null) {
				return extractFosIdFromString(defaultLocation.getDescription());
			}
		}
		return null;
	}

	/**
	 * such string should be of format; FOSAID: 757 TYPE: MU as in the MoH
	 * instance
	 */
	private static String extractFosIdFromString(String locationFosDescription) {
		if (StringUtils.isNotBlank(locationFosDescription) && locationFosDescription.contains("FOSAID: ") && locationFosDescription.contains(" TYPE: ")) {
			return locationFosDescription.split("FOSAID: ")[1].split(" ")[0];
		}
		return null;
	}
	
	public static boolean currentSiteIsPackage2() {
		String package2FosIds = Context.getAdministrationService().getGlobalProperty(GP_PACKAGE_2_SITES_FOSIDS);
		return StringUtils.isNotBlank(package2FosIds) && Arrays.asList(package2FosIds.split(",")).contains(getDefaultFOSID());
	}
	
	public static boolean currentSiteIsPackage3() {
		String package3FosIds = Context.getAdministrationService().getGlobalProperty(GP_PACKAGE_3_SITES_FOSIDS);
		return StringUtils.isNotBlank(package3FosIds) && Arrays.asList(package3FosIds.split(",")).contains(getDefaultFOSID());
	}
}
