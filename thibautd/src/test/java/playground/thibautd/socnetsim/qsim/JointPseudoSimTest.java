/* *********************************************************************** *
 * project: org.matsim.*
 * JointPseudoSimTest.java
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
package playground.thibautd.socnetsim.qsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.TripRouterFactoryInternal;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.mobsim.CompareEventsUtils;
import playground.thibautd.scripts.scenariohandling.CreateGridNetworkWithDimensions;
import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.socnetsim.router.JointPlanRouter;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;
import playground.thibautd.socnetsim.utils.JointScenarioUtils;

/**
 * @author thibautd
 */
public class JointPseudoSimTest {
	// to help tracking test failures.
	// should never be true in a commited version
	private static final boolean TRACE = false;
	private static final boolean DUMP_EVENTS = false;

	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testPSimEventsSimilarToQsim() {
		if (TRACE) Logger.getLogger( "playground.thibautd" ).setLevel( Level.TRACE );
		utils.getOutputDirectory(); // intended side effect: delete content
		final Scenario scenario = createTestScenario();

		final TravelTimeCalculator travelTime =
			new TravelTimeCalculator(
					scenario.getNetwork(),
					scenario.getConfig().travelTimeCalculator());

		final TripRouterFactory defFact =
				new TripRouterFactoryBuilderWithDefaults().build(
							scenario );
		CompareEventsUtils.testEventsSimilarToQsim(
				createTestScenario(),
				new JointPlanRouter(
					new JointTripRouterFactory(
						new TripRouterFactoryInternal() {
							@Override
							public TripRouter instantiateAndConfigureTripRouter() {
								return defFact.instantiateAndConfigureTripRouter(
										new RoutingContextImpl(
											new TravelTimeAndDistanceBasedTravelDisutility(
												travelTime.getLinkTravelTimes(),
												scenario.getConfig().planCalcScore() ), 
											travelTime.getLinkTravelTimes() ) );
							}
						},
						scenario.getPopulation().getFactory()
						).instantiateAndConfigureTripRouter(),
					null ),
				new JointQSimFactory(),
				DUMP_EVENTS ? utils.getOutputDirectory()+"/qSimEvent.xml" : null,
				new JointPseudoSimFactory(
					travelTime ),
				DUMP_EVENTS ? utils.getOutputDirectory()+"/pSimEvent.xml" : null,
				travelTime,
				false );
	}

	@Test @Ignore( "test is too restrictive on what is a correct output..." )
	public void testTSimEventsSimilarToQsim() {
		if (TRACE) Logger.getLogger( "playground.thibautd" ).setLevel( Level.TRACE );
		utils.getOutputDirectory(); // intended side effect: delete content
		final Scenario scenario = createTestScenario();

		final TravelTimeCalculator travelTime =
			new TravelTimeCalculator(
					scenario.getNetwork(),
					scenario.getConfig().travelTimeCalculator());

		final TripRouterFactory defFact =
				new TripRouterFactoryBuilderWithDefaults().build(
							scenario );
		CompareEventsUtils.testEventsSimilarToQsim(
				createTestScenario(),
				new JointPlanRouter(
					new JointTripRouterFactory(
						new TripRouterFactoryInternal() {
							@Override
							public TripRouter instantiateAndConfigureTripRouter() {
								return defFact.instantiateAndConfigureTripRouter(
										new RoutingContextImpl(
											new TravelTimeAndDistanceBasedTravelDisutility(
												travelTime.getLinkTravelTimes(),
												scenario.getConfig().planCalcScore() ), 
											travelTime.getLinkTravelTimes() ) );
							}
						},
						scenario.getPopulation().getFactory()
						).instantiateAndConfigureTripRouter(),
					null ),
				new JointQSimFactory(),
				DUMP_EVENTS ? utils.getOutputDirectory()+"/qSimEvent.xml" : null,
				new JointTeleportationSimFactory(),
				DUMP_EVENTS ? utils.getOutputDirectory()+"/tSimEvent.xml" : null,
				travelTime,
				true );
	}

	private Scenario createTestScenario() {
		final Scenario sc = JointScenarioUtils.createScenario( ConfigUtils.createConfig() );
		CreateGridNetworkWithDimensions.createNetwork(
				sc.getNetwork(),
				75 / 3.6,
				1000,
				100,
				2,
				10 );

		final Random random = new Random( 1234 );
		final List<Id> linkIds = new ArrayList<Id>( sc.getNetwork().getLinks().keySet() );
		for ( int i = 0; i < 20; i++ ) {
			final Id puLinkId = linkIds.get(
						random.nextInt(
							linkIds.size() ) );
			final Id doLinkId = linkIds.get(
						random.nextInt(
							linkIds.size() ) );

			final Id driverId = new IdImpl( "driver-"+i );
			final Id passengerId = new IdImpl( "passenger-"+i );
			/* driver plan */ {
				final Person driver = sc.getPopulation().getFactory().createPerson( driverId );
				sc.getPopulation().addPerson( driver );

				final Plan plan = sc.getPopulation().getFactory().createPlan();
				driver.addPlan( plan );

				final Activity firstActivity = 
						sc.getPopulation().getFactory().createActivityFromLinkId(
							"h",
							linkIds.get(
								random.nextInt(
									linkIds.size() ) ) );

				firstActivity.setEndTime( random.nextDouble() * 36000 );
				plan.addActivity( firstActivity );

				plan.addLeg( sc.getPopulation().getFactory().createLeg( TransportMode.car ) );

				final Activity pu = 
						sc.getPopulation().getFactory().createActivityFromLinkId(
							JointActingTypes.INTERACTION,
							puLinkId );
				pu.setMaximumDuration( 0 );
				plan.addActivity( pu );

				final Leg leg = sc.getPopulation().getFactory().createLeg( JointActingTypes.DRIVER );
				final DriverRoute route = new DriverRoute( puLinkId , doLinkId );
				route.addPassenger( passengerId );
				leg.setRoute( route );
				plan.addLeg( leg );

				final Activity dro = 
						sc.getPopulation().getFactory().createActivityFromLinkId(
							JointActingTypes.INTERACTION,
							doLinkId );
				dro.setMaximumDuration( 0 );
				plan.addActivity( dro );


				plan.addLeg( sc.getPopulation().getFactory().createLeg( TransportMode.car ) );

				plan.addActivity(
						sc.getPopulation().getFactory().createActivityFromLinkId(
							"h",
							linkIds.get(
								random.nextInt(
									linkIds.size() ) ) ) );
			}

			/* passenger plan */ {
				final Person passenger = sc.getPopulation().getFactory().createPerson( passengerId );
				sc.getPopulation().addPerson( passenger );

				final Plan plan = sc.getPopulation().getFactory().createPlan();
				passenger.addPlan( plan );

				final Activity firstActivity = 
						sc.getPopulation().getFactory().createActivityFromLinkId(
							"h",
							puLinkId );

				firstActivity.setEndTime( random.nextDouble() * 36000 );
				plan.addActivity( firstActivity );

				final Leg leg = sc.getPopulation().getFactory().createLeg( JointActingTypes.PASSENGER );
				final PassengerRoute route = new PassengerRoute( puLinkId , doLinkId );
				route.setDriverId( driverId );
				leg.setRoute( route );
				plan.addLeg( leg );

				plan.addActivity(
						sc.getPopulation().getFactory().createActivityFromLinkId(
							"h",
							doLinkId ) );
			}
		}
		return sc;
	}


}

