/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripInsertionWithSocialNetworkTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.thibautd.socnetsim.cliques.config.JointTripInsertorConfigGroup;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.population.SocialNetworkImpl;
import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.replanning.modules.ActedUponInformation;
import playground.thibautd.socnetsim.replanning.modules.JointTripInsertorAlgorithm;

/**
 * @author thibautd
 */
public class JointTripInsertionWithSocialNetworkTest {
	private static final Logger log =
		Logger.getLogger(JointTripInsertionWithSocialNetworkTest.class);

	@Test
	public void testJointTripsGeneratedOnlyAlongSocialTies() {
		final Random random = new Random( 123 );

		for ( int i=0; i < 10; i++ ) {
			final Scenario scenario = generateScenario();

			final SocialNetwork sn =
					(SocialNetwork) scenario.getScenarioElement(
							SocialNetwork.ELEMENT_NAME );
			final JointTripInsertorAlgorithm algo =
				new JointTripInsertorAlgorithm(
						random,
						sn,
						new JointTripInsertorConfigGroup(),
						new TripRouter() );

			final JointPlan jp = groupAllPlansInJointPlan( scenario.getPopulation() );


			final Set<Id> agentsToIgnore = new HashSet<Id>();
			while ( true ) {
				final ActedUponInformation actedUpon =
							algo.run( jp , agentsToIgnore );

				if (actedUpon == null) break;
				agentsToIgnore.add( actedUpon.getDriverId() );
				agentsToIgnore.add( actedUpon.getPassengerId() );

				Assert.assertTrue(
						"passenger not alter of driver!",
						sn.getAlters( actedUpon.getDriverId() ).contains( actedUpon.getPassengerId() ) );
			}

			log.info( "there were "+agentsToIgnore.size()+" agents handled" );
		}
	}

	private JointPlan groupAllPlansInJointPlan(final Population population) {
		final Map<Id, Plan> plans = new HashMap<Id, Plan>();

		for ( Person person : population.getPersons().values() ) {
			plans.put(
					person.getId(),
					person.getSelectedPlan() );
		}

		return new JointPlanFactory().createJointPlan( plans );
	}

	private Scenario generateScenario() {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );

		final Population population = sc.getPopulation();
		final PopulationFactory factory = population.getFactory();

		final Coord coordHome = new CoordImpl( 0 , 0 );
		final Id linkHome = new IdImpl( "link" );
		final int nAgents = 100;
		for ( int i = 0; i < nAgents; i++ ) {
			final Person person = factory.createPerson( new IdImpl( i ) );
			final Plan plan = factory.createPlan();

			final ActivityImpl firstAct = (ActivityImpl) factory.createActivityFromCoord( "h" , coordHome );
			firstAct.setEndTime( 10 );
			firstAct.setLinkId( linkHome );
			plan.addActivity( firstAct );

			plan.addLeg( factory.createLeg( i % 2 == 0 ? TransportMode.car : TransportMode.pt ) );

			final ActivityImpl secondAct = (ActivityImpl) factory.createActivityFromCoord( "h" , coordHome );
			secondAct.setLinkId( linkHome );
			plan.addActivity( secondAct );

			person.addPlan( plan );
			population.addPerson( person );
		}

		final SocialNetwork sn = new SocialNetworkImpl( true );
		sc.addScenarioElement( SocialNetwork.ELEMENT_NAME , sn );

		for ( int i=0; i < nAgents; i++ ) sn.addEgo( new IdImpl( i ) );
		for ( int i=0; i < nAgents - 1; i++ ) {
			sn.addBidirectionalTie( new IdImpl( i ) , new IdImpl( i + 1 ) );
		}

		return sc;
	}
}

