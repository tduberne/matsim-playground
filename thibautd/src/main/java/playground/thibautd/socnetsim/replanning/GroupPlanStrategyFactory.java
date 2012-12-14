/* *********************************************************************** *
 * project: org.matsim.*
 * GroupPlanStrategyFactory.java
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
package playground.thibautd.socnetsim.replanning;

import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.TripsToLegsAlgorithm;

import playground.thibautd.cliquessim.replanning.modules.jointtripinsertor.JointTripInsertorAndRemoverAlgorithm;
import playground.thibautd.socnetsim.replanning.modules.JointPlanMergingModule;
import playground.thibautd.socnetsim.replanning.modules.SplitJointPlansBasedOnJointTripsModule;
import playground.thibautd.socnetsim.replanning.selectors.LogitSumSelector;
import playground.thibautd.socnetsim.replanning.selectors.RandomGroupLevelSelector;

/**
 * Provides factory methods to create standard strategies.
 * @author thibautd
 */
public class GroupPlanStrategyFactory {
	private GroupPlanStrategyFactory() {}

	// /////////////////////////////////////////////////////////////////////////
	// strategies
	// /////////////////////////////////////////////////////////////////////////
	public static GroupPlanStrategy createReRoute(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();
	
		strategy.addStrategyModule( createReRouteModule( config , tripRouterFactory ) );

		return strategy;
	}

	public static GroupPlanStrategy createTimeAllocationMutator(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();

		strategy.addStrategyModule( createTripsToLegsModule( config , tripRouterFactory ) ) ;

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
							new TimeAllocationMutator( config )));

		strategy.addStrategyModule( createReRouteModule( config , tripRouterFactory ) );

		return strategy;
	}

	public static GroupPlanStrategy createCliqueJointTripMutator(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();

		strategy.addStrategyModule(
				new JointPlanMergingModule(
					config.global().getNumberOfThreads(),
					// XXX: merge everything: is it the best idea?
					// on the one hand, it assures we actually have
					// potential drivers and passengers in the same group.
					// On the other hand, only one joint trip will be created
					// or deleted in the whole group, while with partial sub-groups,
					// each one will be handled separately...
					1.0 ) );

		strategy.addStrategyModule(
				new JointPlanBasedGroupStrategyModule(
					new AbstractMultithreadedModule( config.global() ) {
						@Override
						public PlanAlgorithm getPlanAlgoInstance() {
							return new JointTripInsertorAndRemoverAlgorithm(
								config,
								tripRouterFactory.createTripRouter(),
								MatsimRandom.getLocalInstance());
						}
					}));

		// split immediately after insertion/removal,
		// to make optimisation easier.
		strategy.addStrategyModule(
				new SplitJointPlansBasedOnJointTripsModule(
					config.global().getNumberOfThreads() ) );

		strategy.addStrategyModule( createReRouteModule( config , tripRouterFactory ) );

		if (true) throw new RuntimeException( "TODO: jointimemodechooser" );

		return strategy;
	}

	public static GroupPlanStrategy createSelectExpBeta(final Config config) {
		return new GroupPlanStrategy(
				new LogitSumSelector(
					MatsimRandom.getLocalInstance(),
					config.planCalcScore().getBrainExpBeta()) );
	}

	public static GroupPlanStrategy createSubtourModeChoice(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		final GroupPlanStrategy strategy = createRandomSelectingStrategy();

		strategy.addStrategyModule( createReRouteModule( config , tripRouterFactory ) );

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
					new SubtourModeChoice( config ) ) );

		strategy.addStrategyModule( createReRouteModule( config , tripRouterFactory ) );

		return strategy;
	}

	// /////////////////////////////////////////////////////////////////////////
	// standard modules
	// /////////////////////////////////////////////////////////////////////////
	public static GroupStrategyModule createTripsToLegsModule(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		return new IndividualBasedGroupStrategyModule(
				new AbstractMultithreadedModule( config.global() ) {
					@Override
					public PlanAlgorithm getPlanAlgoInstance() {
						return new TripsToLegsAlgorithm( tripRouterFactory.createTripRouter() );
					}
				});
	}

	public static GroupStrategyModule createReRouteModule(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		return new IndividualBasedGroupStrategyModule(
				new AbstractMultithreadedModule( config.global() ) {
					@Override
					public PlanAlgorithm getPlanAlgoInstance() {
						return new PlanRouter( tripRouterFactory.createTripRouter() );
					}
				});
	}


	private static GroupPlanStrategy createRandomSelectingStrategy() {
		return new GroupPlanStrategy(
				new RandomGroupLevelSelector(
					MatsimRandom.getLocalInstance() ) );
	}
}
