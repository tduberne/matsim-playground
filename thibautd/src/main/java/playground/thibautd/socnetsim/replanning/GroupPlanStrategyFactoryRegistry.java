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

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.trafficmonitoring.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.utils.objectattributes.ObjectAttributes;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.router.replanning.BlackListedTimeAllocationMutator;
import playground.thibautd.socnetsim.cliques.replanning.modules.jointtimemodechooser.JointTimeModeChooserAlgorithm;
import playground.thibautd.socnetsim.cliques.replanning.modules.jointtripinsertor.JointTripInsertorAndRemoverAlgorithm;
import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.JointPlanMergingModule;
import playground.thibautd.socnetsim.replanning.modules.MutateActivityLocationsToLocationsOfOthersModule;
import playground.thibautd.socnetsim.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.LogitSumSelector;
import playground.thibautd.socnetsim.replanning.selectors.LogitWeight;
import playground.thibautd.socnetsim.replanning.selectors.LossWeight;
import playground.thibautd.socnetsim.replanning.selectors.LowestScoreOfJointPlanWeight;
import playground.thibautd.socnetsim.replanning.selectors.RandomGroupLevelSelector;
import playground.thibautd.socnetsim.replanning.selectors.WeightedWeight;
import playground.thibautd.socnetsim.replanning.selectors.whoisthebossselector.WhoIsTheBossSelector;
import playground.thibautd.socnetsim.sharedvehicles.replanning.AllocateVehicleToPlansInGroupPlanModule;
import playground.thibautd.socnetsim.sharedvehicles.replanning.AllocateVehicleToSubtourModule;
import playground.thibautd.socnetsim.sharedvehicles.replanning.OptimizeVehicleAllocationAtTourLevelModule;
import playground.thibautd.socnetsim.sharedvehicles.SharedVehicleUtils;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;

/**
 * Provides factory methods to create standard strategies.
 * @author thibautd
 */
public class GroupPlanStrategyFactoryRegistry {
	private static final Logger log =
		Logger.getLogger(GroupPlanStrategyFactoryRegistry.class);

	private GroupPlanStrategyFactoryRegistry() {}

	// /////////////////////////////////////////////////////////////////////////
	// strategies
	// /////////////////////////////////////////////////////////////////////////
	public static GroupPlanStrategy createReRoute(
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy =
				GroupPlanStrategyFactoryUtils.createRandomSelectingStrategy(
					registry.getIncompatiblePlansIdentifierFactory());
	
		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					registry.getScenario().getConfig(),
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier()));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					registry.getScenario().getConfig(),
					registry.getTripRouterFactory() ) );

		return strategy;
	}

	public static GroupPlanStrategy createTimeAllocationMutator(
			final ControllerRegistry registry) {
		return createTimeAllocationMutator( 24 , registry );
	}

	public static GroupPlanStrategy createTimeAllocationMutator(
			final double maxTemp,
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy =
				GroupPlanStrategyFactoryUtils.createRandomSelectingStrategy(
					registry.getIncompatiblePlansIdentifierFactory());
		final Config config = registry.getScenario().getConfig();
		final TripRouterFactoryInternal tripRouterFactory = registry.getTripRouterFactory();
		final PlanRoutingAlgorithmFactory planRouterFactory = registry.getPlanRoutingAlgorithmFactory();

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
					new AbstractMultithreadedModule( config.global().getNumberOfThreads() ) {
						@Override
						public PlanAlgorithm getPlanAlgoInstance() {
							final CompositeStageActivityTypes blackList = new CompositeStageActivityTypes();
							blackList.addActivityTypes( tripRouterFactory.instantiateAndConfigureTripRouter().getStageActivityTypes() );
							blackList.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );

							final int iteration = getReplanningContext().getIteration();
							final int firstIteration = config.controler().getFirstIteration();
							final double nIters = config.controler().getLastIteration() - firstIteration;
							final double minTemp = 1;
							final double startMin = (2 / 3.) * nIters;
							final double progress = (iteration - firstIteration) / startMin;
							final double temp = minTemp + Math.max(1 - progress , 0) * (maxTemp - minTemp);
							log.debug( "temperature in iteration "+iteration+": "+temp );
							final BlackListedTimeAllocationMutator algo =
									new BlackListedTimeAllocationMutator(
										blackList,
										config.timeAllocationMutator().getMutationRange() * temp,
										MatsimRandom.getLocalInstance() );
							return algo;
						}
					}));

		strategy.addStrategyModule( GroupPlanStrategyFactoryUtils.createReRouteModule( config , planRouterFactory , tripRouterFactory ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					config,
					tripRouterFactory) );

		return strategy;
	}

	public static GroupPlanStrategy createCliqueJointTripMutator(
			final ControllerRegistry registry) {
		return createCliqueJointTripMutator( registry , true );
	}

	/**
	 * for tests only!!!
	 */
	public static GroupPlanStrategy createNonOptimizingCliqueJointTripMutator(
			final ControllerRegistry registry) {
		return createCliqueJointTripMutator( registry , false );
	}

	private static GroupPlanStrategy createCliqueJointTripMutator(
			final ControllerRegistry registry,
			final boolean optimize) {
		final GroupPlanStrategy strategy =
				GroupPlanStrategyFactoryUtils.createRandomSelectingStrategy(
					registry.getIncompatiblePlansIdentifierFactory());
		
		final Config config = registry.getScenario().getConfig();

		strategy.addStrategyModule(
				new JointPlanMergingModule(
					registry.getJointPlans().getFactory(),
					config.global().getNumberOfThreads(),
					// merge everything
					1.0 ) );

		strategy.addStrategyModule(
			new JointPlanBasedGroupStrategyModule(
					new AbstractMultithreadedGenericStrategyModule<JointPlan>( config.global() ) {
						@Override
						public GenericPlanAlgorithm<JointPlan> createAlgorithm() {
							return new JointTripInsertorAndRemoverAlgorithm(
								config,
								registry.getTripRouterFactory().instantiateAndConfigureTripRouter(),
								MatsimRandom.getLocalInstance(),
								true); // "iterative"
						}
					}));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					config,
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		if (optimize) {
			final DepartureDelayAverageCalculator delay =
				new DepartureDelayAverageCalculator(
					registry.getScenario().getNetwork(),
					registry.getScenario().getConfig().travelTimeCalculator().getTraveltimeBinSize());

			// split immediately after insertion/removal,
			// to make optimisation easier.
			strategy.addStrategyModule(
					GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
						config,
						registry.getJointPlans().getFactory(),
						// XXX use the default, to allow stupid simulations
						// without joint plans. This is rather ugly.
						new DefaultPlanLinkIdentifier() ) );

			strategy.addStrategyModule(
					new JointPlanBasedGroupStrategyModule(
						new AbstractMultithreadedGenericStrategyModule<JointPlan>(
								registry.getScenario().getConfig().global() ) {
							@Override
							public GenericPlanAlgorithm<JointPlan> createAlgorithm() {
								return new JointTimeModeChooserAlgorithm(
									MatsimRandom.getLocalInstance(),
									null,
									delay,
									registry.getScenario(),
									registry.getScoringFunctionFactory(),
									registry.getTravelTime().getLinkTravelTimes(),
									registry.getLeastCostPathCalculatorFactory(),
									registry.getTripRouterFactory() );
							}
						}));
		}
		else {
			strategy.addStrategyModule(
					GroupPlanStrategyFactoryUtils.createSynchronizerModule(
						config,
						registry.getTripRouterFactory()) );
		}

		final VehicleRessources vehicles = 
					registry.getScenario().getScenarioElement(
						VehicleRessources.class );

		if ( vehicles != null ) {
			strategy.addStrategyModule(
					new AllocateVehicleToPlansInGroupPlanModule(
						registry.getScenario().getConfig().global().getNumberOfThreads(),
						vehicles,
						SharedVehicleUtils.DEFAULT_VEHICULAR_MODES,
						false,
						true)); // preserve allocation (ie just reallocate for modified plans)
		}

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					config,
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier() ) );

		return strategy;
	}

	public static GroupPlanStrategy createSelectExpBeta(
			final IncompatiblePlansIdentifierFactory incompFact,
			final Config config) {
		return new GroupPlanStrategy(
				new LogitSumSelector(
					MatsimRandom.getLocalInstance(),
					incompFact,
					config.planCalcScore().getBrainExpBeta()) );
	}

	public static GroupPlanStrategy createWeightedSelectExpBeta(
			final String weightAttributeName,
			final ObjectAttributes personAttributes,
			final IncompatiblePlansIdentifierFactory incompFact,
			final Config config) {
		return new GroupPlanStrategy(
				 new HighestWeightSelector(
					 incompFact ,
					 new WeightedWeight(
						 new LogitWeight(
							MatsimRandom.getLocalInstance(),
							config.planCalcScore().getBrainExpBeta()),
						 weightAttributeName,
						 personAttributes ) ) );
	}

	public static GroupPlanStrategy createWhoIsTheBossSelectExpBeta(
			final IncompatiblePlansIdentifierFactory incompFact,
			final Config config) {
		return new GroupPlanStrategy(
				 new WhoIsTheBossSelector(
					 MatsimRandom.getLocalInstance(),
					 incompFact ,
					 new LogitWeight(
						MatsimRandom.getLocalInstance(),
						config.planCalcScore().getBrainExpBeta()) ) );
	}

	public static GroupPlanStrategy createMinSelectExpBeta(
			final JointPlans jointPlans,
			final IncompatiblePlansIdentifierFactory incompFact,
			final Config config) {
		return new GroupPlanStrategy(
				 new HighestWeightSelector(
					 incompFact ,
					 new LogitWeight(
						new LowestScoreOfJointPlanWeight( jointPlans ),
						MatsimRandom.getLocalInstance(),
						config.planCalcScore().getBrainExpBeta()) ));
	}

	public static GroupPlanStrategy createMinLossSelectExpBeta(
			final JointPlans jointPlans,
			final IncompatiblePlansIdentifierFactory incompFact,
			final Config config) {
		return new GroupPlanStrategy(
				 new HighestWeightSelector(
					 incompFact ,
					 new LogitWeight(
						new LowestScoreOfJointPlanWeight(
							new LossWeight(),
							jointPlans ),
						MatsimRandom.getLocalInstance(),
						config.planCalcScore().getBrainExpBeta()) ));
	}

	public static GroupPlanStrategy createSubtourModeChoice(
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy =
				GroupPlanStrategyFactoryUtils.createRandomSelectingStrategy(
					registry.getIncompatiblePlansIdentifierFactory());

		// Why the hell did I put this here???
		//strategy.addStrategyModule(
		//		createReRouteModule(
		//			registry.getScenario().getConfig(),
		//			registry.getPlanRoutingAlgorithmFactory(),
		//			registry.getTripRouterFactory() ) );

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
					new SubtourModeChoice(
						registry.getScenario().getConfig() ) ) );

		// TODO: add an option to enable or disable this part?
		final VehicleRessources vehicles =
				registry.getScenario().getScenarioElement(
					VehicleRessources.class );
		if ( vehicles != null ) {
			strategy.addStrategyModule(
				new AllocateVehicleToPlansInGroupPlanModule(
						registry.getScenario().getConfig().global().getNumberOfThreads(),
						registry.getScenario().getScenarioElement(
							VehicleRessources.class ),
						SharedVehicleUtils.DEFAULT_VEHICULAR_MODES,
						true,
						true));
		}

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					registry.getScenario().getConfig(),
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier()));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createSynchronizerModule(
					registry.getScenario().getConfig(),
					registry.getTripRouterFactory()) );

		return strategy;
	}

	public static GroupPlanStrategy createTourVehicleAllocation(
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy =
				GroupPlanStrategyFactoryUtils.createRandomSelectingStrategy(
					registry.getIncompatiblePlansIdentifierFactory());

		strategy.addStrategyModule(
				new IndividualBasedGroupStrategyModule(
					new AllocateVehicleToSubtourModule(
						registry.getScenario().getConfig().global().getNumberOfThreads(),
						TransportMode.car,
						registry.getScenario().getScenarioElement(
							VehicleRessources.class ) ) ) );

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier()));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					registry.getScenario().getConfig(),
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		return strategy;
	}

	public static GroupPlanStrategy createGroupPlanVehicleAllocation(
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy =
				GroupPlanStrategyFactoryUtils.createRandomSelectingStrategy(
					registry.getIncompatiblePlansIdentifierFactory());

		strategy.addStrategyModule(
				new AllocateVehicleToPlansInGroupPlanModule(
					registry.getScenario().getConfig().global().getNumberOfThreads(),
					registry.getScenario().getScenarioElement(
						VehicleRessources.class ),
					SharedVehicleUtils.DEFAULT_VEHICULAR_MODES,
					false,
					false));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier()));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					registry.getScenario().getConfig(),
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		return strategy;
	}

	public static GroupPlanStrategy createOptimizingTourVehicleAllocation(
			final ControllerRegistry registry) {
		final GroupPlanStrategy strategy =
				GroupPlanStrategyFactoryUtils.createRandomSelectingStrategy(
					registry.getIncompatiblePlansIdentifierFactory());

		final CompositeStageActivityTypes stageActs = new CompositeStageActivityTypes();
		stageActs.addActivityTypes( registry.getTripRouterFactory().instantiateAndConfigureTripRouter().getStageActivityTypes() );
		stageActs.addActivityTypes( JointActingTypes.JOINT_STAGE_ACTS );
		strategy.addStrategyModule(
				//new AllocateVehicleToPlansInGroupPlanModule(
				new OptimizeVehicleAllocationAtTourLevelModule(
						registry.getScenario().getConfig().global().getNumberOfThreads(),
						stageActs,
						registry.getScenario().getScenarioElement(
							VehicleRessources.class ),
						SharedVehicleUtils.DEFAULT_VEHICULAR_MODES,
						true));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier()));

		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createReRouteModule(
					registry.getScenario().getConfig(),
					registry.getPlanRoutingAlgorithmFactory(),
					registry.getTripRouterFactory() ) );

		return strategy;
	}

	public static GroupPlanStrategy createRandomJointPlansRecomposer(
			final ControllerRegistry registry) {
		// Note that this breaks incompatibility constraints, but not
		// joint plans constraints. Thus, it is not such a "recomposition"
		// as a grouping of joint plans.
		final GroupPlanStrategy strategy = new GroupPlanStrategy(
				new RandomGroupLevelSelector(
					MatsimRandom.getLocalInstance(),
					new EmptyIncompatiblePlansIdentifierFactory() ) );

		// recompose
		strategy.addStrategyModule(
				GroupPlanStrategyFactoryUtils.createRecomposeJointPlansModule(
					registry.getScenario().getConfig(),
					registry.getJointPlans().getFactory(),
					registry.getPlanLinkIdentifier()));

		return strategy;
	}

	public static GroupPlanStrategy createActivityInGroupLocationChoice(
			final String type,
			final ControllerRegistry registry) {
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
