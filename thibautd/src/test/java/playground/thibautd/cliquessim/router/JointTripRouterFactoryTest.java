/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripRouterFactoryTest.java
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
package playground.thibautd.cliquessim.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTimeCalculator;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.socnetsim.router.JointPlanRouter;
import playground.thibautd.socnetsim.router.JointTripRouterFactory;

/**
 * @author thibautd
 */
public class JointTripRouterFactoryTest {
	private static final Logger log =
		Logger.getLogger(JointTripRouterFactoryTest.class);

	private JointTripRouterFactory factory;
	private Scenario scenario;

	@Before
	public void initFixtures() {
		this.scenario = createScenario();
		this.factory = createFactory( scenario );
	}

	private static Scenario createScenario() {
		Id node1 = new IdImpl( "node1" );
		Id node2 = new IdImpl( "node2" );
		Id node3 = new IdImpl( "node3" );
		Id node4 = new IdImpl( "node4" );

		Id link1 = new IdImpl( "link1" );
		Id link2 = new IdImpl( "link2" );
		Id link3 = new IdImpl( "link3" );

		Scenario sc = ScenarioUtils.createScenario(
				ConfigUtils.createConfig() );
		NetworkImpl net = (NetworkImpl) sc.getNetwork();
		Node node1inst = net.createAndAddNode( node1 , new CoordImpl( 0 , 1 ) );
		Node node2inst = net.createAndAddNode( node2 , new CoordImpl( 0 , 2 ) );
		Node node3inst = net.createAndAddNode( node3 , new CoordImpl( 0 , 3 ) );
		Node node4inst = net.createAndAddNode( node4 , new CoordImpl( 0 , 4 ) );

		net.createAndAddLink( link1 , node1inst , node2inst , 1 , 1 , 1 , 1 );
		net.createAndAddLink( link2 , node2inst , node3inst , 1 , 1 , 1 , 1 );
		net.createAndAddLink( link3 , node3inst , node4inst , 1 , 1 , 1 , 1 );

		Population pop = sc.getPopulation();
		Id driverId = new IdImpl( "driver" );
		Id passengerId = new IdImpl( "passenger" );

		// driver
		PersonImpl pers = new PersonImpl( driverId );
		PlanImpl plan = new PlanImpl( pers );
		pers.addPlan( plan );
		pers.setSelectedPlan( plan );
		pop.addPerson( pers );

		plan.createAndAddActivity( "home" , link1 ).setEndTime( 32454 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( JointActingTypes.PICK_UP , link1 ).setMaximumDuration( 0 );
		Leg dLeg = plan.createAndAddLeg( JointActingTypes.DRIVER );
		plan.createAndAddActivity( JointActingTypes.DROP_OFF , link3 ).setMaximumDuration( 0 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( "home" , link3 );

		DriverRoute dRoute = new DriverRoute( link1 , link3 );
		dRoute.addPassenger( passengerId );
		dLeg.setRoute( dRoute );

		// passenger
		pers = new PersonImpl( passengerId );
		plan = new PlanImpl( pers );
		pers.addPlan( plan );
		pers.setSelectedPlan( plan );
		pop.addPerson( pers );

		ActivityImpl a = plan.createAndAddActivity( "home" , link1 );
		a.setEndTime( 1246534 );
		a.setCoord( new CoordImpl( 0 , 1 ) );
		plan.createAndAddLeg( TransportMode.walk );
		a = plan.createAndAddActivity( JointActingTypes.PICK_UP , link1 );
		a.setMaximumDuration( 0 );
		a.setCoord( new CoordImpl( 0 , 2 ) );
		Leg pLeg = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		a = plan.createAndAddActivity( JointActingTypes.DROP_OFF , link3 );
		a.setMaximumDuration( 0 );
		a.setCoord( new CoordImpl( 0 , 3 ) );
		plan.createAndAddLeg( TransportMode.walk );
		a = plan.createAndAddActivity( "home" , link3 );
		a.setCoord( new CoordImpl( 0 , 4 ) );

		PassengerRoute pRoute = new PassengerRoute( link1 , link3 );
		pRoute.setDriverId( driverId );
		pLeg.setRoute( pRoute );

		return sc;
	}

	private static JointTripRouterFactory createFactory( final Scenario scenario ) {
		return new JointTripRouterFactory(
				scenario,
				new TravelDisutilityFactory () {
					@Override
					public TravelDisutility createTravelDisutility(
							TravelTime timeCalculator,
							PlanCalcScoreConfigGroup cnScoringGroup) {
						return new TravelTimeAndDistanceBasedTravelDisutility( timeCalculator , cnScoringGroup );
					}
				},
				new FreeSpeedTravelTimeCalculator(),
				new DijkstraFactory(),
				null);
	}

	@Test
	public void testPassengerRoute() throws Exception {
		JointPlanRouter planRouter = new JointPlanRouter( factory.createTripRouter() );
		for (Person pers : scenario.getPopulation().getPersons().values()) {
			Plan plan = pers.getSelectedPlan();
			boolean toRoute = false;
			Id driver = null;

			for (PlanElement pe : plan.getPlanElements()) {
				if ( pe instanceof Leg && ((Leg) pe).getMode().equals(  JointActingTypes.PASSENGER ) ) {
					driver = ((PassengerRoute) ((Leg) pe).getRoute()).getDriverId();
					toRoute = true;
					break;
				}
			}

			if (toRoute) {
				log.debug( "testing passenger route on plan of "+plan.getPerson().getId() );
				planRouter.run( plan );

				for (PlanElement pe : plan.getPlanElements()) {
					if ( pe instanceof Leg && ((Leg) pe).getMode().equals(  JointActingTypes.PASSENGER ) ) {
						Id actualDriver = ((PassengerRoute) ((Leg) pe).getRoute()).getDriverId();

						Assert.assertEquals(
								"wrong driver Id",
								driver,
								actualDriver);
					}
				}

			}
		}
	}

	@Test
	public void testDriverRoute() throws Exception {
		JointPlanRouter planRouter = new JointPlanRouter( factory.createTripRouter() );
		for (Person pers : scenario.getPopulation().getPersons().values()) {
			Plan plan = pers.getSelectedPlan();
			boolean toRoute = false;
			List<Id> passengerIds = new ArrayList<Id>();

			for (PlanElement pe : plan.getPlanElements()) {
				if ( pe instanceof Leg && ((Leg) pe).getMode().equals(  JointActingTypes.DRIVER ) ) {
					passengerIds.addAll( ((DriverRoute) ((Leg) pe).getRoute()).getPassengersIds());
					toRoute = true;
					break;
				}
			}

			if (toRoute) {
				log.debug( "testing driver route on plan of "+plan.getPerson().getId() );
				planRouter.run( plan );

				for (PlanElement pe : plan.getPlanElements()) {
					if ( pe instanceof Leg && ((Leg) pe).getMode().equals(  JointActingTypes.DRIVER ) ) {
						Collection<Id> actualPassengers = ((DriverRoute) ((Leg) pe).getRoute()).getPassengersIds();

						Assert.assertEquals(
								"wrong number of passengers",
								passengerIds.size(),
								actualPassengers.size());

						Assert.assertTrue(
								"wrong passengers ids: "+actualPassengers+" is not "+passengerIds,
								passengerIds.containsAll( actualPassengers ));
					}
				}

			}
		}
	}
}

