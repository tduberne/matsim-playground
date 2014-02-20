/* *********************************************************************** *
 * project: org.matsim.*
 * IgnoranceBehaviorTest.java
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
package playground.thibautd.socnetsim.replanning.modules;

import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.MainModeIdentifierImpl;
import org.matsim.core.router.TripRouter;

import playground.thibautd.socnetsim.cliques.config.JointTripInsertorConfigGroup;
import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.socnetsim.replanning.modules.JointTripInsertorAlgorithm;
import playground.thibautd.socnetsim.replanning.modules.JointTripInsertorAndRemoverAlgorithm;
import playground.thibautd.socnetsim.replanning.modules.JointTripRemoverAlgorithm;
import playground.thibautd.socnetsim.utils.JointScenarioUtils;

/**
 * @author thibautd
 */
public class InsertionRemovalIgnoranceBehaviorTest {
	private Config config;
	private TripRouter tripRouter;
	private Random random;

	@Before
	public void configureLogging() {
		Logger.getLogger( JointTripInsertorAndRemoverAlgorithm.class ).setLevel( Level.TRACE );
	}

	@Before
	public void init() {
		config = JointScenarioUtils.createConfig();
		tripRouter = new  TripRouter();
		random = new Random( 1234 );
	}

	@Test
	public void testRemoverIgnorance() throws Exception {
		final JointTripRemoverAlgorithm algo = new JointTripRemoverAlgorithm( random , EmptyStageActivityTypes.INSTANCE , new MainModeIdentifierImpl() );
		
		JointPlan jointPlan = createPlanWithJointTrips();

		assertNull(
				"unexpected removed trips",
				algo.run( jointPlan , jointPlan.getIndividualPlans().keySet() ) );

	}

	@Test
	public void testInsertorIgnorance() throws Exception {
		final JointTripInsertorAlgorithm algo =
			new JointTripInsertorAlgorithm(
					random,
					null,
					(JointTripInsertorConfigGroup) config.getModule( JointTripInsertorConfigGroup.GROUP_NAME ),
					tripRouter );
		
		JointPlan jointPlan = createPlanWithoutJointTrips();

		assertNull(
				"unexpected removed trips",
				algo.run( jointPlan , jointPlan.getIndividualPlans().keySet() ) );

	}

	private JointPlan createPlanWithoutJointTrips() {
		final Map<Id, Plan> individualPlans = new HashMap<Id, Plan>();

		for (int i=0; i < 100; i++) {
			Id driverId = new IdImpl( "driver"+i );
			Person person = new PersonImpl( driverId );
			PlanImpl plan = new PlanImpl( person );
			individualPlans.put( driverId , plan );
			plan.createAndAddActivity( "first_act_d"+i , new IdImpl( "some_link" ) ).setEndTime( 10 );
			plan.createAndAddLeg( TransportMode.car );
			plan.createAndAddActivity( "second_act_d"+i , new IdImpl( "nowhere" ) );

			Id passengerId = new IdImpl( "passenger"+i );
			person = new PersonImpl( passengerId );
			plan = new PlanImpl( person );
			individualPlans.put( passengerId , plan );
			plan.createAndAddActivity( "first_act_p"+i , new IdImpl( "earth" ) ).setEndTime( 10 );
			plan.createAndAddLeg( TransportMode.walk );
			plan.createAndAddActivity( "second_act_p"+i , new IdImpl( "space" ) );
		}

		return new JointPlanFactory().createJointPlan( individualPlans );
	}

	private JointPlan createPlanWithJointTrips() {
		final Map<Id, Plan> individualPlans = new HashMap<Id, Plan>();

		Id puLink = new IdImpl( "pu" );
		Id doLink = new IdImpl( "do" );

		for (int i=0; i < 100; i++) {
			Id driverId = new IdImpl( "driver"+i );
			Person person = new PersonImpl( driverId );
			PlanImpl plan = new PlanImpl( person );
			individualPlans.put( driverId , plan );
			plan.createAndAddActivity( "first_act_d"+i , new IdImpl( "some_link" ) ).setEndTime( 10 );
			plan.createAndAddLeg( TransportMode.car );
			plan.createAndAddActivity( JointActingTypes.INTERACTION , puLink ).setMaximumDuration( 0 );
			Leg driverLeg1 = plan.createAndAddLeg( JointActingTypes.DRIVER );
			plan.createAndAddActivity( JointActingTypes.INTERACTION , doLink ).setMaximumDuration( 0 );
			plan.createAndAddLeg( TransportMode.car );
			plan.createAndAddActivity( "second_act_d"+i , new IdImpl( "nowhere" ) );

			Id passengerId = new IdImpl( "passenger"+i );
			person = new PersonImpl( passengerId );
			plan = new PlanImpl( person );
			individualPlans.put( passengerId , plan );
			plan.createAndAddActivity( "first_act_p"+i , new IdImpl( "earth" ) ).setEndTime( 10 );
			plan.createAndAddLeg( TransportMode.walk );
			plan.createAndAddActivity( JointActingTypes.INTERACTION , puLink ).setMaximumDuration( 0 );
			Leg passengerLeg1 = plan.createAndAddLeg( JointActingTypes.PASSENGER );
			plan.createAndAddActivity( JointActingTypes.INTERACTION , doLink ).setMaximumDuration( 0 );
			plan.createAndAddLeg( TransportMode.walk );
			plan.createAndAddActivity( "second_act_p"+i , new IdImpl( "space" ) );

			DriverRoute driverRoute = new DriverRoute( puLink , doLink );
			driverRoute.addPassenger( passengerId );
			driverLeg1.setRoute( driverRoute );

			PassengerRoute passengerRoute = new PassengerRoute( puLink , doLink );
			passengerRoute.setDriverId( driverId );
			passengerLeg1.setRoute( passengerRoute );
		}

		return new JointPlanFactory().createJointPlan( individualPlans );
	}
}

