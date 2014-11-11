/* *********************************************************************** *
 * project: org.matsim.*
 * JointControlerUtilsTest.java
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
package playground.thibautd.socnetsim.utils;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.PassengerRoute;

/**
 * @author thibautd
 */
public class JointScenarioUtilsTest {
	@Rule
	public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testJointTripsImport() throws Exception {
		final Population dumpedPopulation = createPopulation();
		final String popFile = utils.getOutputDirectory()+"/pop.xml";

		new PopulationWriter( dumpedPopulation , null ).write( popFile );
		final Config config = JointScenarioUtils.createConfig();
		config.plans().setInputFile( popFile );
		final Scenario loadedScenario = JointScenarioUtils.loadScenario( config );

		final Population loadedPopulation = loadedScenario.getPopulation();

		assertEquals(
				"read pop is not the same size as dumped",
				dumpedPopulation.getPersons().size(),
				loadedPopulation.getPersons().size());

		for (Person person : loadedPopulation.getPersons().values()) {
			final Person dumpedPerson = dumpedPopulation.getPersons().get( person.getId() );

			final Plan dumpedPlan = dumpedPerson.getPlans().get( 0 );
			final Plan loadedPlan = person.getPlans().get( 0 );

			assertEquals(
					"incompatible plan sizes",
					dumpedPlan.getPlanElements().size(),
					loadedPlan.getPlanElements().size());

			final Leg dumpedLeg = (Leg) dumpedPlan.getPlanElements().get( 1 );
			final Leg loadedLeg = (Leg) loadedPlan.getPlanElements().get( 1 );
			
			if (dumpedLeg.getMode().equals( JointActingTypes.DRIVER )) {
				assertEquals(
						"wrong route class",
						DriverRoute.class,
						loadedLeg.getRoute().getClass());

				final Collection<Id<Person>> dumpedPassengers = ((DriverRoute) dumpedLeg.getRoute()).getPassengersIds();
				final Collection<Id<Person>> loadedPassengers = ((DriverRoute) loadedLeg.getRoute()).getPassengersIds();

				assertEquals(
						"unexpected passenger ids",
						dumpedPassengers,
						loadedPassengers);
			}
			else {
				assertEquals(
						"wrong route class",
						PassengerRoute.class,
						loadedLeg.getRoute().getClass());

				final Id<Person> dumpedDriver = ((PassengerRoute) dumpedLeg.getRoute()).getDriverId();
				final Id<Person> loadedDriver = ((PassengerRoute) loadedLeg.getRoute()).getDriverId();

				assertEquals(
						"unexpected passenger ids",
						dumpedDriver,
						loadedDriver);
			}
		}
	}

	private static Population createPopulation() {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final Population population = sc.getPopulation();

		final Person driver = population.getFactory().createPerson( Id.create( "driver" , Person.class ) );
		population.addPerson( driver );
		final Plan driverPlan = population.getFactory().createPlan();
		driverPlan.setPerson( driver );
		driver.addPlan( driverPlan );
		
		driverPlan.addActivity( population.getFactory().createActivityFromLinkId( "h" , Id.create( 1 , Link.class ) ) );
		final Leg driverLeg = population.getFactory().createLeg( JointActingTypes.DRIVER );
		final DriverRoute dRoute = new DriverRoute( Id.create( 1 , Link.class ) , Id.create( 1 , Link.class ) );
		dRoute.addPassenger( Id.create( "random_hitch_hiker" , Person.class) );
		dRoute.setDistance( 234 );
		dRoute.setTravelTime( 234 );
		driverLeg.setRoute( dRoute );
		driverPlan.addLeg( driverLeg );
		driverPlan.addActivity( population.getFactory().createActivityFromLinkId( "h" , Id.create( 1 , Link.class ) ) );

		final Person passenger = population.getFactory().createPerson( Id.create( "passenger" , Person.class) );
		population.addPerson( passenger );
		final Plan passengerPlan = population.getFactory().createPlan();
		passengerPlan.setPerson( passenger );
		passenger.addPlan( passengerPlan );
		
		passengerPlan.addActivity( population.getFactory().createActivityFromLinkId( "h" , Id.create( 1 , Link.class ) ) );
		final Leg passengerLeg = population.getFactory().createLeg( JointActingTypes.PASSENGER );
		final PassengerRoute pRoute = new PassengerRoute( Id.create( 1 , Link.class ) , Id.create( 1 , Link.class ) );
		pRoute.setDriverId( Id.create( "lorrie driver", Person.class ) );
		pRoute.setDistance( 234 );
		pRoute.setTravelTime( 234 );
		passengerLeg.setRoute( pRoute );
		passengerPlan.addLeg( passengerLeg );
		passengerPlan.addActivity( population.getFactory().createActivityFromLinkId( "h" , Id.create( 1 , Link.class) ) );

		return population;
	}
}

