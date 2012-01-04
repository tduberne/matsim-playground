/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsMutatorAlgorithm.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.replanning.modules.jointtripsmutator;

import java.util.Map;
import java.util.Random;

import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.jointtrips.config.JointTripsMutatorConfigGroup;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.jointtrips.population.jointtrippossibilities.JointTripPossibilitiesUtils;
import playground.thibautd.jointtrips.population.jointtrippossibilities.JointTripPossibility;
import playground.thibautd.jointtrips.replanning.JointPlanAlgorithm;

/**
 * changes randomly joint trips participation.
 * No routing is done, no consistency check/enforcement neither
 * (nor for mode nor time).
 * Thus, this algorithm is meant to be used before a consistency aware algorithm
 * (such as the JointPlanOptimizer) only.
 *
 * @author thibautd
 */
public class JointTripsMutatorAlgorithm extends JointPlanAlgorithm {
	private final JointTripsMutatorConfigGroup params;
	private final Random random;

	public JointTripsMutatorAlgorithm(
			final JointTripsMutatorConfigGroup configGroup) {
		params = configGroup;
		random = MatsimRandom.getLocalInstance();
	}

	@Override
	public void run(final JointPlan plan) {
		Map<JointTripPossibility, Boolean> participation =
			JointTripPossibilitiesUtils.getPerformedJointTrips( plan );

		double prob = params.getMutationProbability();
		for (Map.Entry<JointTripPossibility, Boolean> entry :
				participation.entrySet() ) {
			if (random.nextDouble() < prob) {
				boolean oldValue = entry.getValue();
				entry.setValue( !oldValue );
			}
		}

		// include changes in plan
		JointTripPossibilitiesUtils.includeJointTrips( participation , plan );

	}

}

