/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.population;

import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.routes.GenericRoute;
import org.matsim.core.utils.misc.Time;

/**
 * A route for passenger trips.
 * @author thibautd
 */
public class PassengerRoute implements GenericRoute {
	private double distance = Double.NaN;
	private double travelTime = Time.UNDEFINED_TIME;
	private Id startLink = null;
	private Id endLink = null;
	private Id driver = null;

	private PassengerRoute() {}

	public PassengerRoute(
			final Id startLink,
			final Id endLink) {
		this.startLink = startLink;
		this.endLink = endLink;
	}

	public Id getDriverId() {
		return driver;
	}

	public void setDriverId(final Id d) {
		driver = d;
	}

	@Override
	public double getDistance() {
		return distance;
	}

	@Override
	public void setDistance(final double distance) {
		this.distance = distance;
	}

	@Override
	public double getTravelTime() {
		return travelTime;
	}

	@Override
	public void setTravelTime(final double travelTime) {
		this.travelTime = travelTime;
	}

	@Override
	public Id getStartLinkId() {
		return startLink;
	}

	@Override
	public Id getEndLinkId() {
		return endLink;
	}

	@Override
	public void setStartLinkId(final Id linkId) {
		startLink = linkId;
	}

	@Override
	public void setEndLinkId(final Id linkId) {
		endLink = linkId;
	}

	@Override
	public void setRouteDescription(
			final Id startLinkId,
			final String routeDescription,
			final Id endLinkId) {
		startLink = startLinkId;
		endLink = endLinkId;
		driver = new IdImpl( routeDescription.trim() );
	}

	@Override
	public String getRouteDescription() {
		return driver != null ? driver.toString() : "";
	}

	@Override
	public String getRouteType() {
		return "passenger";
	}

	@Override
	public PassengerRoute clone() {
		PassengerRoute c = new PassengerRoute();
		c.distance = distance;
		c.travelTime= travelTime;
		c.startLink = startLink;
		c.endLink = endLink;
		c.driver = driver;
		return c;
	}
}

