/* *********************************************************************** *
 * project: org.matsim.*
 * GroupWhoIsTheBossSelectExpBetaFactory.java
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
package playground.thibautd.socnetsim.replanning.strategies;

import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.selectors.LogitWeight;
import playground.thibautd.socnetsim.replanning.selectors.whoisthebossselector.WhoIsTheBossSelector;

/**
 * @author thibautd
 */
public class GroupWhoIsTheBossSelectExpBetaFactory implements GroupPlanStrategyFactory {

	@Override
	public GroupPlanStrategy createStrategy(final ControllerRegistry registry) {
		return new GroupPlanStrategy(
				 new WhoIsTheBossSelector(
					 MatsimRandom.getLocalInstance(),
					 registry.getIncompatiblePlansIdentifierFactory() ,
					 new LogitWeight(
						MatsimRandom.getLocalInstance(),
						registry.getScenario().getConfig().planCalcScore().getBrainExpBeta()) ) );
	}
}
