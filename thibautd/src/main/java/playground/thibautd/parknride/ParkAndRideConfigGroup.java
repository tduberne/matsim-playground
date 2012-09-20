/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideConfigGroup.java
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
package playground.thibautd.parknride;

import org.matsim.api.core.v01.TransportMode;

import playground.thibautd.utils.ReflectiveModule;

/**
 * @author thibautd
 */
public class ParkAndRideConfigGroup extends ReflectiveModule {
	public static final String GROUP_NAME = "parkAndRide";

	private String facilities = null;
	private String[] availableModes = new String[]{
		TransportMode.car, TransportMode.pt, TransportMode.bike,
			TransportMode.walk, ParkAndRideConstants.PARK_N_RIDE_LINK_MODE};
	private String[] chainBasedModes = new String[]{ TransportMode.car , TransportMode.bike };
	private double facilityChangeProbability = 0.9;
	private double localSearchRadius = 5000;
	private double priceOfDistance = 1;

	public ParkAndRideConfigGroup() {
		super( GROUP_NAME );
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters/setters
	// /////////////////////////////////////////////////////////////////////////
	public void setFacilities(final String fileName) {
		facilities = fileName;
	}

	public String getFacilities() {
		return facilities;
	}

	public void setAvailableModes(final String value) {
		availableModes = value.split(",");
		
		for (int i=0; i<availableModes.length; i++) {
			availableModes[ i ] = availableModes[ i ].trim();
		}
	}

	public String[] getAvailableModes() {
		return availableModes;
	}

	public String getAvailableModesAsString() {
		return arrayToString( availableModes );
	}

	private String arrayToString(final String[] a) {
		StringBuffer b = new StringBuffer();

		if (a.length > 0) {
			b.append( a[0] );
			for (int i=1; i < a.length; i++) {
				b.append( "," );
				b.append( a[ i ] );
			}
		}

		return b.toString();
	}

	public void setChainBasedModes(final String value) {
		chainBasedModes = value.split(",");
		
		for (int i=0; i<availableModes.length; i++) {
			chainBasedModes[ i ] = chainBasedModes[ i ].trim();
		}
	}

	public String[] getChainBasedModes() {
		return chainBasedModes;
	}

	public String getChainBasedModesAsString() {
		return arrayToString( chainBasedModes );
	}

	public void setFacilityChangeProbability(final String value) {
		this.facilityChangeProbability = Double.parseDouble( value );
	}

	public double getFacilityChangeProbability() {
		return facilityChangeProbability;
	}

	public void setLocalSearchRadius(final String value) {
		this.localSearchRadius = Double.parseDouble( value );
	}

	public double getLocalSearchRadius() {
		return localSearchRadius;
	}

	public void setPriceOfDistance(final String value) {
		this.priceOfDistance = Double.parseDouble( value );
	}

	public double getPriceOfDistance() {
		return priceOfDistance;
	}
}

