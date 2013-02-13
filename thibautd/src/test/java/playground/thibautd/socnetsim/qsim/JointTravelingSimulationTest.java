/* *********************************************************************** *
 * project: org.matsim.*
 * JointTravelingSimulationTest.java
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

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

import org.apache.log4j.Logger;

import static org.junit.Assert.*;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 * @author thibautd
 */
public class JointTravelingSimulationTest {
	private static final Logger log =
		Logger.getLogger(JointTravelingSimulationTest.class);

	private static final Id originLink = new IdImpl( "origin" );
	private static final Id toPuLink = new IdImpl( "toPu" );
	private static final Id puLink = new IdImpl( "pu" );
	private static final Id travelLink1 = new IdImpl( 1 );
	private static final Id travelLink2 = new IdImpl( 2 );
	private static final Id doLink = new IdImpl( "do" );
	private static final Id toDestinationLink = new IdImpl( "to_destination" );
	private static final Id destinationLink = new IdImpl( "destination" );
	
	@Test
	public void testAgentsArriveTogetherWithoutDummies() throws Exception {
		testAgentsArriveTogether( false );
	}

	@Test
	public void testAgentsArriveTogetherWithDummies() throws Exception {
		testAgentsArriveTogether( true );
	}

	public void testAgentsArriveTogether(final boolean insertDummies) throws Exception {
		final Random random = new Random( 1234 );

		for (int i=0; i < 50; i++) {
			log.info( "random test scenario "+i );
			final Scenario sc = createTestScenario( insertDummies , random );
			final EventsManager events = EventsUtils.createEventsManager();

			final AtomicInteger arrCount = new AtomicInteger( 0 );
			final AtomicInteger atDestCount = new AtomicInteger( 0 );
			events.addHandler( new AgentArrivalEventHandler() {
				private double departure = -100;
				@Override
				public void reset(final int iteration) {}

				@Override
				public void handleEvent(final AgentArrivalEvent event) {
					final String mode = event.getLegMode();
					log.info( mode+" arrival at "+event.getTime() );
					if ( mode.equals( JointActingTypes.DRIVER ) ||
							mode.equals( JointActingTypes.PASSENGER ) ) {
						if ( departure < 0 ) {
							departure = event.getTime();
							assert departure > 0;
						}

						arrCount.incrementAndGet();
						assertEquals(
							"unexpected joint arrival time",
							departure,
							event.getTime(),
							MatsimTestUtils.EPSILON);

						assertEquals(
								"unexpected arrival location for mode "+mode,
								doLink,
								event.getLinkId() );
					}

					if ( event.getLinkId().equals( destinationLink ) ) {
						atDestCount.incrementAndGet();
					}
				}
			});

			final JointQSimFactory factory = new JointQSimFactory( );
			factory.createMobsim( sc , events ).run();

			assertEquals(
					"unexpected number of joint arrivals",
					3,
					arrCount.get());

			assertEquals(
					"unexpected number of agents arriving at destination",
					sc.getPopulation().getPersons().size(),
					atDestCount.get());
		}
	}

	@Test
	public void testNumberOfEnterLeaveVehicle() throws Exception {
		final Random random = new Random( 1234 );

		for (int i=0; i < 50; i++) {
			log.info( "random test scenario "+i );
			final Scenario sc = createTestScenario( false , random );
			final EventsManager events = EventsUtils.createEventsManager();

			final AtomicInteger enterCount = new AtomicInteger( 0 );
			final AtomicInteger leaveCount = new AtomicInteger( 0 );
			events.addHandler( new PersonEntersVehicleEventHandler() {
				@Override
				public void reset(int iteration) {}

				@Override
				public void handleEvent(final PersonEntersVehicleEvent event) {
					enterCount.incrementAndGet();
				}
			});

			events.addHandler( new PersonLeavesVehicleEventHandler() {
				@Override
				public void reset(int iteration) {}

				@Override
				public void handleEvent(final PersonLeavesVehicleEvent event) {
					leaveCount.incrementAndGet();
				}
			});

			final JointQSimFactory factory = new JointQSimFactory( );
			factory.createMobsim( sc , events ).run();

			assertEquals(
					"not as many leave events as enter events",
					enterCount.get(),
					leaveCount.get());
		}
	}

	private Scenario createTestScenario(
			final boolean insertDummyActivities,
			final Random random) {
		final Config config = ConfigUtils.createConfig();
		final QSimConfigGroup qsimConf = new QSimConfigGroup();
		qsimConf.setEndTime( 30 * 3600 );
		config.addQSimConfigGroup( qsimConf );
		final Scenario sc = ScenarioUtils.createScenario( config );

		createNetwork( sc.getNetwork() );

		final Id driverId = new IdImpl( "driver" );
		final Id passengerId1 = new IdImpl( "passenger_1" );
		final Id passengerId2 = new IdImpl( "passenger_2" );

		final PopulationFactory factory = sc.getPopulation().getFactory();

		// TODO: complicate:
		// - same OD served several times by one driver for different passengers
		// - different drivers serve the same OD

		// driver
		final Person driver = factory.createPerson( driverId );
		final Plan driverPlan = factory.createPlan();
		driverPlan.setPerson( driver );
		driver.addPlan( driverPlan );
		sc.getPopulation().addPerson( driver );

		Activity act = factory.createActivityFromLinkId( "h" , originLink );
		act.setEndTime( random.nextDouble() * 12 * 3600 );
		driverPlan.addActivity( act );

		Leg l = factory.createLeg( TransportMode.car );
		l.setRoute( new LinkNetworkRouteImpl( originLink , Arrays.asList( toPuLink ) , puLink ) );
		driverPlan.addLeg( l );

		if (insertDummyActivities) {
			act = factory.createActivityFromLinkId(
						JointActingTypes.PICK_UP,
						puLink );
			driverPlan.addActivity( act );
			act.setMaximumDuration( 0 );
		}

		l = factory.createLeg( JointActingTypes.DRIVER );
		DriverRoute dRoute =
			new DriverRoute(
					puLink,
					doLink );
		dRoute.setLinkIds(
				puLink , 
				Arrays.asList( travelLink1 , travelLink2 ),
				doLink);
		dRoute.addPassenger( passengerId1 );
		dRoute.addPassenger( passengerId2 );
		l.setRoute( dRoute );
		driverPlan.addLeg( l );

		if (insertDummyActivities) {
			act = factory.createActivityFromLinkId(
						JointActingTypes.DROP_OFF,
						doLink );
			driverPlan.addActivity( act );
			act.setMaximumDuration( 0 );
		}

		l = factory.createLeg( TransportMode.car );
		l.setRoute( new LinkNetworkRouteImpl( doLink , Arrays.asList( toDestinationLink ) , destinationLink ) );
		driverPlan.addLeg( l );

		act = factory.createActivityFromLinkId( "h" , destinationLink );
		driverPlan.addActivity( act );

		// passengers 
		for (Id passengerId : new Id[]{ passengerId1 , passengerId2 }) {
			final Person p1 = factory.createPerson( passengerId );
			final Plan p1Plan = factory.createPlan();
			p1Plan.setPerson( driver );
			p1.addPlan( p1Plan );
			sc.getPopulation().addPerson( p1 );

			act = factory.createActivityFromLinkId( "h" , originLink );
			act.setEndTime( random.nextDouble() * 12 * 3600 );
			p1Plan.addActivity( act );

			l = factory.createLeg( TransportMode.walk );
			double tt = random.nextDouble() * 1234;
			Route walkRoute = new GenericRouteImpl( originLink , puLink );
			walkRoute.setTravelTime( tt );
			l.setTravelTime( tt );
			l.setRoute( walkRoute );
			p1Plan.addLeg( l );

			if (insertDummyActivities) {
				act = factory.createActivityFromLinkId(
							JointActingTypes.PICK_UP,
							puLink );
				p1Plan.addActivity( act );
				act.setMaximumDuration( 0 );
			}

			l = factory.createLeg( JointActingTypes.PASSENGER );
			PassengerRoute pRoute =
				new PassengerRoute(
						puLink,
						doLink );
			pRoute.setDriverId( driverId );
			l.setRoute( pRoute );
			p1Plan.addLeg( l );

			if (insertDummyActivities) {
				act = factory.createActivityFromLinkId(
							JointActingTypes.DROP_OFF,
							doLink );
				p1Plan.addActivity( act );
				act.setMaximumDuration( 0 );
			}

			l = factory.createLeg( TransportMode.walk );
			tt = random.nextDouble() * 1234;
			walkRoute = new GenericRouteImpl( doLink , destinationLink );
			walkRoute.setTravelTime( tt );
			l.setTravelTime( tt );
			l.setRoute( walkRoute );
			p1Plan.addLeg( l );

			act = factory.createActivityFromLinkId( "h" , destinationLink );
			p1Plan.addActivity( act );
		}

		return sc;
	}

	private void createNetwork(final Network network) {
		int c = 0;
		int d = 0;

		Node node1 = network.getFactory().createNode( new IdImpl( c++ ) , new CoordImpl( 0 , d++ ) );
		Node node2 = network.getFactory().createNode( new IdImpl( c++ ) , new CoordImpl( 0 , d++ ) );

		network.addNode( node1 );
		network.addNode( node2 );
		network.addLink( network.getFactory().createLink( originLink , node1 , node2 ) );

		for (Id linkId : new Id[]{ toPuLink , puLink , travelLink1 , travelLink2 , doLink , toDestinationLink , destinationLink }) {
			node1 = node2;
			node2 = network.getFactory().createNode( new IdImpl( c++ ) , new CoordImpl( 0 , d++ ) );
			network.addNode( node2 );
			network.addLink( network.getFactory().createLink( linkId , node1 , node2 ) );
		}
	}
}

