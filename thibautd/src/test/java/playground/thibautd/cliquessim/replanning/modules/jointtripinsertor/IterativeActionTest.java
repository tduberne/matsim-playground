/* *********************************************************************** *
 * project: org.matsim.*
 * IterativeActionTest.java
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
package playground.thibautd.cliquessim.replanning.modules.jointtripinsertor;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.thibautd.cliquessim.utils.JointControlerUtils;
import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 * @author thibautd
 */
public class IterativeActionTest {
	private static int N_COUPLES = 100;
	private Config config;
	private TripRouter tripRouter;
	private Random random;

	@Before
	public void configureLogging() {
		Logger.getLogger( JointTripInsertorAndRemoverAlgorithm.class ).setLevel( Level.TRACE );
	}

	@Before
	public void init() {
		config = JointControlerUtils.createConfig( null );
		tripRouter = new  TripRouter();
		random = new Random( 1234 );
	}

	@Test
	public void testNonIterativeRemoval() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					config,
					tripRouter,
					random,
					false);
		JointPlan jointPlan = createPlanWithJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (PlanElement pe : jointPlan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg l = (Leg) pe;
			final String mode = l.getMode();
			if ( JointActingTypes.DRIVER.equals( mode ) ) d++;
			if ( JointActingTypes.PASSENGER.equals( mode ) ) p++;
		}

		final String finalPlanDescr = jointPlan.toString();

		assertEquals(
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				N_COUPLES - 1,
				d);

		assertEquals(
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				N_COUPLES - 1,
				p);
	}

	@Test
	public void testIterativeRemoval() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					config,
					tripRouter,
					random,
					true);
		JointPlan jointPlan = createPlanWithJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (PlanElement pe : jointPlan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg l = (Leg) pe;
			final String mode = l.getMode();
			if ( JointActingTypes.DRIVER.equals( mode ) ) d++;
			if ( JointActingTypes.PASSENGER.equals( mode ) ) p++;
		}

		final String finalPlanDescr = jointPlan.toString();

		assertEquals(
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				0,
				d);

		assertEquals(
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				0,
				p);
	}

	@Test
	public void testNonIterativeInsertion() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					config,
					tripRouter,
					random,
					false);
		JointPlan jointPlan = createPlanWithoutJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (PlanElement pe : jointPlan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg l = (Leg) pe;
			final String mode = l.getMode();
			if ( JointActingTypes.DRIVER.equals( mode ) ) d++;
			if ( JointActingTypes.PASSENGER.equals( mode ) ) p++;
		}

		final String finalPlanDescr = jointPlan.toString();

		assertEquals(
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				1,
				d);

		assertEquals(
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				1,
				p);
	}

	@Test
	public void testIterativeInsertion() throws Exception {
		JointTripInsertorAndRemoverAlgorithm algo =
			new JointTripInsertorAndRemoverAlgorithm(
					config,
					tripRouter,
					random,
					true);
		JointPlan jointPlan = createPlanWithoutJointTrips();
		algo.run( jointPlan );

		final String initialPlanDescr = jointPlan.toString();

		int d = 0;
		int p = 0;
		for (PlanElement pe : jointPlan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg l = (Leg) pe;
			final String mode = l.getMode();
			if ( JointActingTypes.DRIVER.equals( mode ) ) d++;
			if ( JointActingTypes.PASSENGER.equals( mode ) ) p++;
		}

		final String finalPlanDescr = jointPlan.toString();

		assertEquals(
				"unexpected number of driver trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				N_COUPLES,
				d);

		assertEquals(
				"unexpected number of passenger trips when passing from plan "
				+initialPlanDescr+" to plan "
				+finalPlanDescr,
				N_COUPLES,
				p);
	}


	private JointPlan createPlanWithJointTrips() {
		final Map<Id, Plan> individualPlans = new HashMap<Id, Plan>();

		Id puLink = new IdImpl( "pu" );
		Id doLink = new IdImpl( "do" );

		for (int i=0; i < N_COUPLES; i++) {
			Id driverId = new IdImpl( "driver"+i );
			Person person = new PersonImpl( driverId );
			PlanImpl plan = new PlanImpl( person );
			individualPlans.put( driverId , plan );
			plan.createAndAddActivity( "first_act_d"+i , new IdImpl( "some_link" ) ).setEndTime( 10 );
			plan.createAndAddLeg( TransportMode.car );
			plan.createAndAddActivity( JointActingTypes.PICK_UP , puLink ).setMaximumDuration( 0 );
			Leg driverLeg1 = plan.createAndAddLeg( JointActingTypes.DRIVER );
			plan.createAndAddActivity( JointActingTypes.DROP_OFF , doLink ).setMaximumDuration( 0 );
			plan.createAndAddLeg( TransportMode.car );
			plan.createAndAddActivity( "second_act_d"+i , new IdImpl( "nowhere" ) );

			Id passengerId = new IdImpl( "passenger"+i );
			person = new PersonImpl( passengerId );
			plan = new PlanImpl( person );
			individualPlans.put( passengerId , plan );
			plan.createAndAddActivity( "first_act_p"+i , new IdImpl( "earth" ) ).setEndTime( 10 );
			plan.createAndAddLeg( TransportMode.walk );
			plan.createAndAddActivity( JointActingTypes.PICK_UP , puLink ).setMaximumDuration( 0 );
			Leg passengerLeg1 = plan.createAndAddLeg( JointActingTypes.PASSENGER );
			plan.createAndAddActivity( JointActingTypes.DROP_OFF , doLink ).setMaximumDuration( 0 );
			plan.createAndAddLeg( TransportMode.walk );
			plan.createAndAddActivity( "second_act_p"+i , new IdImpl( "space" ) );

			DriverRoute driverRoute = new DriverRoute( puLink , doLink );
			driverRoute.addPassenger( passengerId );
			driverLeg1.setRoute( driverRoute );

			PassengerRoute passengerRoute = new PassengerRoute( puLink , doLink );
			passengerRoute.setDriverId( driverId );
			passengerLeg1.setRoute( passengerRoute );
		}

		return JointPlanFactory.createJointPlan( individualPlans );
	}

	private JointPlan createPlanWithoutJointTrips() {
		final Map<Id, Plan> individualPlans = new HashMap<Id, Plan>();

		Coord coord1 = new CoordImpl( 0 , 0 );
		Coord coord2 = new CoordImpl( 3600 , 21122012 );

		for (int i=0; i < N_COUPLES; i++) {
			Id driverId1 = new IdImpl( "driver"+i );
			Person person = new PersonImpl( driverId1 );
			PlanImpl plan = new PlanImpl( person );
			individualPlans.put( driverId1 , plan );
			ActivityImpl act = plan.createAndAddActivity( "first_act_d"+i , new IdImpl( "some_link" ) );
			act.setEndTime( 10 );
			act.setCoord( coord1 );
			plan.createAndAddLeg( TransportMode.car );
			act = plan.createAndAddActivity( "second_act_d"+i , new IdImpl( "nowhere" ) );
			act.setCoord( coord2 );

			Id passengerId1 = new IdImpl( "passenger"+i );
			person = new PersonImpl( passengerId1 );
			plan = new PlanImpl( person );
			individualPlans.put( passengerId1 , plan );
			act = plan.createAndAddActivity( "first_act_p"+i , new IdImpl( "earth" ) );
			act.setEndTime( 10 );
			act.setCoord( coord1 );
			plan.createAndAddLeg( TransportMode.walk );
			act = plan.createAndAddActivity( "second_act_p"+i , new IdImpl( "space" ) );
			act.setCoord( coord2 );
		}

		return JointPlanFactory.createJointPlan( individualPlans );
	}
}

