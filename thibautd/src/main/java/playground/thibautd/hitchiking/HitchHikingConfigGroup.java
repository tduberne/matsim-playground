/* *********************************************************************** *
 * project: org.matsim.*
 * HitchHikingConfigGroup.java
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
package playground.thibautd.hitchiking;

import playground.thibautd.utils.ReflectiveModule;

/**
 * @author thibautd
 */
public class HitchHikingConfigGroup extends ReflectiveModule {

	public static final String GROUP_NAME = "hitchHiking";

	private double maximumDetourFraction = 0.25;
	private String spotsFile = null;

	public HitchHikingConfigGroup() {
		super(GROUP_NAME);
	}

	public void setMaximumDetourFraction(final String value) {
		maximumDetourFraction = Double.parseDouble( value );
	}

	public double getMaximumDetourFraction() {
		return maximumDetourFraction;
	}

	public void setSpotsFile(final String value) {
		spotsFile = value;
	}

	public String getSpotsFile() {
		return spotsFile;
	}
}

