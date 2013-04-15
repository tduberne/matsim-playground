/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsStats.java
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
package playground.thibautd.socnetsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import playground.thibautd.socnetsim.population.JointActingTypes;

/**
 * @author thibautd
 */
public class JointTripsStats extends AbstractPlanAnalyzerPerGroup {

	public JointTripsStats(
			final int graphWriteInterval,
			final OutputDirectoryHierarchy controlerIO,
			final Scenario scenario,
			final GroupIdentifier groupIdentifier) {
		super(graphWriteInterval, controlerIO, scenario, groupIdentifier);
	}

	@Override
	protected double calcStat(final Plan plan) {
		int count = 0;
		for (PlanElement pe : plan.getPlanElements()) {
			if (pe instanceof Activity &&
					((Activity) pe).getType().equals(
						JointActingTypes.PICK_UP )) {
				count++;
			}
		}

		return count;
	}

	@Override
	protected String getStatName() {
		return "Number of Joint Trips";
	}
}

