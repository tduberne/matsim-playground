/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripInsertorConfigGroup.java
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
package playground.thibautd.cliquessim.config;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.TransportMode;

import playground.thibautd.utils.ReflectiveModule;

/**
 * @author thibautd
 */
public class JointTripInsertorConfigGroup extends ReflectiveModule {

	public static final String GROUP_NAME = "jointTripInsertor";

	private List<String> chainBasedModes = Arrays.asList( TransportMode.car , TransportMode.bike );
	private double betaDetour = 2;
	private double scale = 1;

	public JointTripInsertorConfigGroup() {
		super( GROUP_NAME );
	}

	public List<String> getChainBasedModes() {
		return this.chainBasedModes;
	}

	public double getBetaDetour() {
		return this.betaDetour;
	}

	public void setBetaDetour(final String betaDetour) {
		this.betaDetour = Double.parseDouble( betaDetour );
	}

	public double getScale() {
		return this.scale;
	}

	public void setScale(final String scale) {
		this.scale = Double.parseDouble( scale );
	}

	public void setChainBasedModes(final String chainBasedModes) {
		this.chainBasedModes = Arrays.asList( chainBasedModes.split(",") );
	}
}

