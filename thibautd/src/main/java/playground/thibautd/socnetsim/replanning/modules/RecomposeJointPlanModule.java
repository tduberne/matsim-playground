/* *********************************************************************** *
 * project: org.matsim.*
 * RecomposeJointPlanModule.java
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
package playground.thibautd.socnetsim.replanning.modules;

import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;

/**
 * @author thibautd
 */
public class RecomposeJointPlanModule extends AbstractMultithreadedGenericStrategyModule<GroupPlans> {
	private final JointPlanFactory factory;
	private final PlanLinkIdentifier linkIdentifier;

	public RecomposeJointPlanModule(
			final int nThreads,
			final JointPlanFactory factory,
			final PlanLinkIdentifier linkIdentifier) {
		super( nThreads );
		this.factory = factory;
		this.linkIdentifier = linkIdentifier;
	}

	@Override
	public GenericPlanAlgorithm<GroupPlans> createAlgorithm() {
		return new RecomposeJointPlanAlgorithm(
				factory,
				linkIdentifier);
	}
}

