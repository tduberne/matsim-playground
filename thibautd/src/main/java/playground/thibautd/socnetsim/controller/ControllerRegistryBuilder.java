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
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactoryInternal;
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
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.XY2Links;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.router.TransitRouterImplFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import playground.thibautd.router.PlanRoutingAlgorithmFactory;
import playground.thibautd.socnetsim.qsim.JointQSimFactory;
import playground.thibautd.socnetsim.replanning.DefaultPlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.GenericStrategyModule;
import playground.thibautd.socnetsim.replanning.grouping.GroupIdentifier;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.selectors.EmptyIncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;
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
	private final LinkedList<GenericStrategyModule<ReplanningGroup>> prepareForSimModules =
			new LinkedList<GenericStrategyModule<ReplanningGroup>>();

	// configurable elements
	// if still null at build time, defaults will be created
	// lazily. This allows to used other fields at construction
	private TravelTimeCalculator travelTime = null;
	private TravelDisutilityFactory travelDisutilityFactory = null;
	private ScoringFunctionFactory scoringFunctionFactory = null;
	private MobsimFactory mobsimFactory = null;
	private TripRouterFactoryInternal tripRouterFactory = null;
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory = null;
	private PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory = null;
	private GroupIdentifier groupIdentifier = null;
	private PlanLinkIdentifier planLinkIdentifier = null;
	private IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory = null;

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
		if ( this.travelTime != null ) throw new IllegalStateException( "object already set" );
		this.travelTime = travelTime2;
		return this;
	}

	public ControllerRegistryBuilder withTravelDisutilityFactory(
			final TravelDisutilityFactory travelDisutilityFactory2) {
		if ( this.travelDisutilityFactory != null ) throw new IllegalStateException( "object already set" );
		this.travelDisutilityFactory = travelDisutilityFactory2;
		return this;
	}

	public ControllerRegistryBuilder withScoringFunctionFactory(
			final ScoringFunctionFactory scoringFunctionFactory2) {
		if ( this.scoringFunctionFactory != null ) throw new IllegalStateException( "object already set" );
		this.scoringFunctionFactory = scoringFunctionFactory2;
		return this;
	}

	public ControllerRegistryBuilder withMobsimFactory(
			final MobsimFactory mobsimFactory2) {
		if ( this.mobsimFactory != null ) throw new IllegalStateException( "object already set" );
		this.mobsimFactory = mobsimFactory2;
		return this;
	}

	public ControllerRegistryBuilder withTripRouterFactory(
			final TripRouterFactoryInternal tripRouterFactory2) {
		if ( this.tripRouterFactory != null ) throw new IllegalStateException( "object already set" );
		this.tripRouterFactory = tripRouterFactory2;
		return this;
	}

	public ControllerRegistryBuilder withLeastCostPathCalculatorFactory(
			final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory2) {
		if ( this.leastCostPathCalculatorFactory != null ) throw new IllegalStateException( "object already set" );
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory2;
		return this;
	}

	public ControllerRegistryBuilder withPlanRoutingAlgorithmFactory(
			final PlanRoutingAlgorithmFactory planRoutingAlgorithmFactory2) {
		if ( this.planRoutingAlgorithmFactory != null ) throw new IllegalStateException( "object already set" );
		this.planRoutingAlgorithmFactory = planRoutingAlgorithmFactory2;
		return this;
	}

	public ControllerRegistryBuilder withGroupIdentifier(
			final GroupIdentifier groupIdentifier2) {
		if ( this.groupIdentifier != null ) throw new IllegalStateException( "object already set" );
		this.groupIdentifier = groupIdentifier2;
		return this;
	}

	public ControllerRegistryBuilder withAdditionalPrepareForSimModule(
			final GenericStrategyModule<ReplanningGroup> algo ) {
		this.prepareForSimModules.add( algo );
		return this;
	}

	public ControllerRegistryBuilder withPlanLinkIdentifier(
			final PlanLinkIdentifier identifier) {
		if ( this.planLinkIdentifier != null ) throw new IllegalStateException( "object already set" );
		this.planLinkIdentifier = identifier;
		return this;
	}

	public ControllerRegistryBuilder withIncompatiblePlansIdentifierFactory(
			final IncompatiblePlansIdentifierFactory factory) {
		if ( this.incompatiblePlansIdentifierFactory != null ) throw new IllegalStateException( "object already set" );
		this.incompatiblePlansIdentifierFactory = factory;
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
			getTravelTime(),
			getTravelDisutilityFactory(),
			getScoringFunctionFactory(),
			legTimes,
			getMobsimFactory(),
			getTripRouterFactory(),
			getLeastCostPathCalculatorFactory(),
			getPlanRoutingAlgorithmFactory(),
			getGroupIdentifier(),
			prepareForSimModules,
			getPlanLinkIdentifier(),
			getIncompatiblePlansIdentifierFactory());
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters, which initialize to defaults if nothing. SHould be renamed.
	public GroupIdentifier getGroupIdentifier() {
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
		return groupIdentifier;
	}

	public ScoringFunctionFactory getScoringFunctionFactory() {
		if ( scoringFunctionFactory == null ) {
			this.scoringFunctionFactory =
					new CharyparNagelScoringFunctionFactory(
						scenario.getConfig().planCalcScore(),
						scenario.getNetwork());
		}
		return scoringFunctionFactory;
	}

	public PlanRoutingAlgorithmFactory getPlanRoutingAlgorithmFactory() {
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
		return planRoutingAlgorithmFactory;
	}

	public MobsimFactory getMobsimFactory() {
		if ( mobsimFactory == null ) {
			this.mobsimFactory = new JointQSimFactory();
		}
		return mobsimFactory;
	}

	public PlanLinkIdentifier getPlanLinkIdentifier() {
		if ( planLinkIdentifier == null ) {
			this.planLinkIdentifier = new DefaultPlanLinkIdentifier();
		}
		return planLinkIdentifier;
	}

	public TravelTimeCalculator getTravelTime() {
		if ( travelTime == null ) {
			this.travelTime =
				new TravelTimeCalculatorFactoryImpl().createTravelTimeCalculator(
						scenario.getNetwork(),
						scenario.getConfig().travelTimeCalculator());
			this.events.addHandler(travelTime);	
		}
		return travelTime;
	}

	public TravelDisutilityFactory getTravelDisutilityFactory() {
		if ( travelDisutilityFactory == null ) {
			this.travelDisutilityFactory = new TravelCostCalculatorFactoryImpl();
		}
		return travelDisutilityFactory;
	}

	public LeastCostPathCalculatorFactory getLeastCostPathCalculatorFactory() {
		if ( leastCostPathCalculatorFactory == null ) {
			switch (scenario.getConfig().controler().getRoutingAlgorithmType()) {
				case AStarLandmarks:
					this.leastCostPathCalculatorFactory =
							new AStarLandmarksFactory(
										scenario.getNetwork(),
										getTravelDisutilityFactory().createTravelDisutility(
											getTravelTime().getLinkTravelTimes(),
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
										getTravelDisutilityFactory().createTravelDisutility(
											getTravelTime().getLinkTravelTimes(),
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
		return leastCostPathCalculatorFactory;
	}

	public TripRouterFactoryInternal getTripRouterFactory() {
		if ( tripRouterFactory == null ) {
			final TransitSchedule schedule = scenario.getTransitSchedule();
			final TransitRouterFactory transitRouterFactory =
				schedule != null ?
					new TransitRouterImplFactory(
							schedule,
							new TransitRouterConfig(
								scenario.getConfig() ) ) :
					null;

			this.tripRouterFactory = new JointTripRouterFactory(
					scenario,
					getTravelDisutilityFactory(),
					getTravelTime().getLinkTravelTimes(),
					getLeastCostPathCalculatorFactory(),
					transitRouterFactory);
		}
		return tripRouterFactory;
	}

	public IncompatiblePlansIdentifierFactory getIncompatiblePlansIdentifierFactory() {
		if ( incompatiblePlansIdentifierFactory == null ) {
			incompatiblePlansIdentifierFactory = new EmptyIncompatiblePlansIdentifierFactory();
		}
		return incompatiblePlansIdentifierFactory;
	}

	private void setDefaults() {
		// we do this here, as we need configurable objects
		// but we want it to be executed at the start!
		this.prepareForSimModules.addFirst(
				new AbstractMultithreadedGenericStrategyModule<ReplanningGroup>(
					scenario.getConfig().global()) {
					@Override
					public GenericPlanAlgorithm<ReplanningGroup> createAlgorithm() {
						final PlanAlgorithm routingAlgorithm =
									getPlanRoutingAlgorithmFactory().createPlanRoutingAlgorithm(
										getTripRouterFactory().instantiateAndConfigureTripRouter() );
						final PlanAlgorithm checkJointRoutes =
							new ImportedJointRoutesChecker( getTripRouterFactory().instantiateAndConfigureTripRouter() );
						final PlanAlgorithm xy2link = new XY2Links( scenario.getNetwork() );
						return new GenericPlanAlgorithm<ReplanningGroup>() {
							@Override
							public void run(final ReplanningGroup group) {
								for ( Person person : group.getPersons() ) {
									for ( Plan plan : person.getPlans() ) {
										xy2link.run( plan );
										checkJointRoutes.run( plan );
										if ( hasLegsWithoutRoutes( plan ) ) {
											routingAlgorithm.run( plan );
										}
									}
								}
							}

							private boolean hasLegsWithoutRoutes(final Plan plan) {
								for ( PlanElement pe : plan.getPlanElements() ) {
									if ( pe instanceof Leg && ((Leg) pe).getRoute() == null ) {
										return true;
									}
								}
								return false;
							}
						};
					}

					@Override
					protected String getName() {
						return "PrepareRoutesForSim";
					}
				});
	}
}

