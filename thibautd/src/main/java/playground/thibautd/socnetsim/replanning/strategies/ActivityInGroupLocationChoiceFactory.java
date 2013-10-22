/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityInGroupLocationChoiceFactory.java
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

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactoryUtils;
import playground.thibautd.socnetsim.replanning.modules.MutateActivityLocationsToLocationsOfOthersModule;

/**
 * @author thibautd
 */
public class ActivityInGroupLocationChoiceFactory implements GroupPlanStrategyFactory {
	private final String type;

	public ActivityInGroupLocationChoiceFactory(final String activityType) {
		this.type = activityType;
	}

	@Override
	public GroupPlanStrategy createStrategy( final ControllerRegistry registry ) {
		final GroupPlanStrategy strategy =
				GroupPlanStrategyFactoryUtils.createRandomSelectingStrategy(
					registry.getIncompatiblePlansIdentifierFactory());

		strategy.addStrategyModule(
				new MutateActivityLocationsToLocationsOfOthersModule(
					registry.getScenario().getConfig().global().getNumberOfThreads(),
					registry.getScenario().getPopulation(),
					type ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					registry.getScenario().getConfig(),
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		return strategy;

	}
}
