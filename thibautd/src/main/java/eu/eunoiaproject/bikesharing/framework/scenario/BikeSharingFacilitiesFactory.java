/* *********************************************************************** *
 * project: org.matsim.*
 * BikeSharingFacilitiesFactory.java
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
package eu.eunoiaproject.bikesharing.framework.scenario;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.network.Link;

/**
 * Associated to a {@link BikeSharingFacilities} container,
 * allows to create facilities in a standard way.
 * @author thibautd
 */
public interface BikeSharingFacilitiesFactory extends MatsimFactory {
	public BikeSharingFacility createBikeSharingFacility(
			Id<BikeSharingFacility> id,
			Coord coord,
			Id<Link> linkId,
			int capacity,
			int initialNumberOfBikes );
}

