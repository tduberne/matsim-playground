/* *********************************************************************** *
 * project: org.matsim.*
 * ControllerRegistryBuilder.java
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
package playground.thibautd.socnetsim.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import org.matsim.analysis.CalcLegTimes;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.population.algorithms.PersonAlgorithm;
import org.matsim.population.algorithms.PersonPrepareForSim;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.qsim.JointQSimFactory;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;
import playground.thibautd.socnetsim.utils.ImportedJointRoutesChecker;

/**
 * Allows to build a ControllerRegistry with certain default values
 * @author thibautd
 */
public class ControllerRegistryBuilder {
	private final Scenario scenario;
	private final EventsManager events;
	private final CalcLegTimes legTimes;
	private final LinkedList<GenericPlanAlgorithm<ReplanningGroup>> prepareForSimAlgorithms =
			new LinkedList<GenericPlanAlgorithm<ReplanningGroup>>();

	// configurable elements
	// if still null at build time, defaults will be created
	// lazily. This allows to used other fields at construction
	private TravelTimeCalculator travelTime = null;
	private TravelDisutilityFactory travelDisutilityFactory = null;
	private ScoringFunctionFactory scoringFunctionFactory = null;
	private MobsimFactory mobsimFactory = null;
	private TripRouterFactory tripRouterFactory = null;
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = null;
	private PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory = null;
	private GroupIdentifier groupIdentifier = null;

	// /////////////////////////////////////////////////////////////////////////
	// contrs
	/**
	 * Initializes a builder bith default fields
	 * @param scenario must contain a JointPlans element
	 */
	public ControllerRegistryBuilder( final Scenario scenario ) {
		this.scenario = scenario;
		this.events = EventsUtils.createEventsManager( scenario.getConfig() );

		// some analysis utils
		// should probably be moved somewhere else
		this.events.addHandler(
				new VolumesAnalyzer(
					3600, 24 * 3600 - 1,
					scenario.getNetwork()));

		this.legTimes = new CalcLegTimes();
		this.events.addHandler( legTimes );
	}

	// /////////////////////////////////////////////////////////////////////////
	// "with" methods
	public ControllerRegistryBuilder withTravelTimeCalculator(
			final TravelTimeCalculator travelTime2) {
		this.travelTime = travelTime2;
		return this;
	}

	public ControllerRegistryBuilder withTravelDisutilityFactory(
			final TravelDisutilityFactory travelDisutilityFactory2) {
		this.travelDisutilityFactory = travelDisutilityFactory2;
		return this;
	}

	public ControllerRegistryBuilder withScoringFunctionFactory(
			final ScoringFunctionFactory scoringFunctionFactory2) {
		this.scoringFunctionFactory = scoringFunctionFactory2;
		return this;
	}

	public ControllerRegistryBuilder withMobsimFactory(
			final MobsimFactory mobsimFactory2) {
		this.mobsimFactory = mobsimFactory2;
		return this;
	}

	public ControllerRegistryBuilder withTripRouterFactory(
			final TripRouterFactory tripRouterFactory2) {
		this.tripRouterFactory = tripRouterFactory2;
		return this;
	}

	public ControllerRegistryBuilder withLeastCostPathCalculatorFactory(
			final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory2) {
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory2;
		return this;
	}

	public ControllerRegistryBuilder withPlanRoutingAlgorithmFactory(
			final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory2) {
		this.planRoutingAlgorithmFactory = planRoutingAlgorithmFactory2;
		return this;
	}

	public ControllerRegistryBuilder withGroupIdentifier(
			final GroupIdentifier groupIdentifier2) {
		this.groupIdentifier = groupIdentifier2;
		return this;
	}

	public ControllerRegistryBuilder withAdditionalPrepareForSimAlgorithms(
			final GenericPlanAlgorithm<ReplanningGroup> algo ) {
		this.prepareForSimAlgorithms.add( algo );
		return this;
	}

	// /////////////////////////////////////////////////////////////////////////
	// build
	private boolean wasBuild = false;
	public ControllerRegistry build() {
		if ( wasBuild ) {
			throw new IllegalStateException( "building several instances is unsafe!" );
		}
		wasBuild = true;

		// set defaults at the last time, in case we must access user-defined elements
		setDefaults();
		return new ControllerRegistry(
			scenario,
			events,
			travelTime,
			travelDisutilityFactory,
			scoringFunctionFactory,
			legTimes,
			mobsimFactory,
			tripRouterFactory,
			leastCostPathCalculatorFactory,
			planRoutingAlgorithmFactory,
			groupIdentifier,
			prepareForSimAlgorithms);
	}

	private final void setDefaults() {
		// by default, no groups (results in individual replanning)
		if ( groupIdentifier == null ) {
			this.groupIdentifier = new GroupIdentifier() {
				@Override
				public Collection<ReplanningGroup> identifyGroups(
						final Population population) {
					return Collections.<ReplanningGroup>emptyList();
				}
			};
		}

		if ( scoringFunctionFactory == null ) {
			this.scoringFunctionFactory =
					new CharyparNagelScoringFunctionFactory(
						scenario.getConfig().planCalcScore(),
						scenario.getNetwork());
		}

		if ( planRoutingAlgorithmFactory == null ) {
			// by default: do not care about joint trips, vehicles or what not
			this.planRoutingAlgorithmFactory = new PlanRoutingAlgorithmFactory() {
				@Override
				public PlanAlgorithm createPlanRoutingAlgorithm(
						final TripRouter tripRouter) {
					return new PlanRouter( tripRouter );
				}
			};
		}

		if ( mobsimFactory == null ) {
			this.mobsimFactory = new JointQSimFactory();
		}

		if ( travelTime == null ) {
			this.travelTime =
				new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(
						scenario.getNetwork(),
						scenario.getConfig().travelTimeCalculator());
			this.events.addHandler(travelTime);	
		}

		if ( travelDisutilityFactory == null ) {
			this.travelDisutilityFactory = new TravelCostCalculatorFactoryImpl();
		}

		if ( leastCostPathCalculatorFactory == null ) {
			switch (scenario.getConfig().controler().getRoutingAlgorithmType()) {
				case AStarLandmarks:
					this.leastCostPathCalculatorFactory =
							new AStarLandmarksFactory(
										scenario.getNetwork(),
										travelDisutilityFactory.createTravelDisutility(
											travelTime.getLinkTravelTimes(),
											scenario.getConfig().planCalcScore()));
					break;
				case Dijkstra:
					PreProcessDijkstra ppd = new PreProcessDijkstra();
					ppd.run( scenario.getNetwork() );
					this.leastCostPathCalculatorFactory = new DijkstraFactory( ppd );
					break;
				case FastAStarLandmarks:
					this.leastCostPathCalculatorFactory =
							new FastAStarLandmarksFactory(
										scenario.getNetwork(),
										travelDisutilityFactory.createTravelDisutility(
											travelTime.getLinkTravelTimes(),
											scenario.getConfig().planCalcScore()));
					break;
				case FastDijkstra:
					PreProcessDijkstra ppfd = new PreProcessDijkstra();
					ppfd.run( scenario.getNetwork() );
					this.leastCostPathCalculatorFactory = new FastDijkstraFactory( ppfd );
					break;
				default:
					throw new IllegalArgumentException( "unkown algorithm "+scenario.getConfig().controler().getRoutingAlgorithmType() );
			}
		}

		if ( tripRouterFactory == null ) {
			this.tripRouterFactory = new JointTripRouterFactory(
					scenario,
					travelDisutilityFactory,
					travelTime.getLinkTravelTimes(),
					leastCostPathCalculatorFactory,
					null); // last arg: transit router factory.
		}

		// we do this here, as we need configurable objects
		final PersonAlgorithm prepareForSim =
			new PersonPrepareForSim(
					planRoutingAlgorithmFactory.createPlanRoutingAlgorithm(
						tripRouterFactory.createTripRouter() ),
					scenario);
		final PersonAlgorithm checkJointRoutes =
			new ImportedJointRoutesChecker( tripRouterFactory.createTripRouter() );
		// but we want it to be executed at the start!
		this.prepareForSimAlgorithms.addFirst(
				new GenericPlanAlgorithm<ReplanningGroup>() {
					@Override
					public void run(final ReplanningGroup group) {
						for ( Person person : group.getPersons() ) {
							checkJointRoutes.run( person );
							prepareForSim.run( person );
						}
					}
				});
	}
}

