/* *********************************************************************** *
 * project: org.matsim.*
 * EmptyStageActivityTypes.java
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
package playground.thibautd.router;

/**
 * @author thibautd
 */
public final class EmptyStageActivityTypes implements StageActivityTypes {
	public static final EmptyStageActivityTypes INSTANCE = new EmptyStageActivityTypes();

	private EmptyStageActivityTypes() {}
	@Override
	public final boolean isStageActivity(final String activityType) {
		return false;
	}
}

