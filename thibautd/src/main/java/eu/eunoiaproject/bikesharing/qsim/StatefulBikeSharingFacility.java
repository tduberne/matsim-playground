/* *********************************************************************** *
 * project: org.matsim.*
 * StatefulBikeSharingFacility.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package eu.eunoiaproject.bikesharing.qsim;

import eu.eunoiaproject.bikesharing.scenario.BikeSharingFacility;

/**
 * @author thibautd
 */
public interface StatefulBikeSharingFacility extends BikeSharingFacility {
	public boolean hasBikes();
	public int getNumberOfBikes();
}
