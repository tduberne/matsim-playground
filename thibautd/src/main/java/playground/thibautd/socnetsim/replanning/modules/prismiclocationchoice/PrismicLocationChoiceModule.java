/* *********************************************************************** *
 * project: org.matsim.*
 * PrismicLocationChoiceModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.replanning.modules.prismiclocationchoice;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.ReplanningContext;

import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;

/**
 * @author thibautd
 */
public class PrismicLocationChoiceModule  extends AbstractMultithreadedGenericStrategyModule<GroupPlans> {
	private final Scenario scenario;

	public PrismicLocationChoiceModule(final Scenario sc) {
		super( sc.getConfig().global() );
		this.scenario = sc;
	}

	@Override
	public GenericPlanAlgorithm<GroupPlans> createAlgorithm(final ReplanningContext replanningContext) {
		return new PrismicLocationChoiceAlgorithm(
				(PrismicLocationChoiceConfigGroup) scenario.getConfig().getModule( PrismicLocationChoiceConfigGroup.GROUP_NAME ),
				scenario.getActivityFacilities(),
				(SocialNetwork) scenario.getScenarioElement( SocialNetwork.ELEMENT_NAME ),
				replanningContext.getTripRouter().getStageActivityTypes() );
	}
}

