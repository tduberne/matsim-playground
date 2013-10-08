/* *********************************************************************** *
 * project: org.matsim.*
 * AllocateVehicleToPlansInGroupPlanAlgorithmTest.java
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
package playground.thibautd.socnetsim.sharedvehicles.replanning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;

/**
 * @author thibautd
 */
public class AllocateVehicleToPlansInGroupPlanAlgorithmTest {
	private static final Logger log =
		Logger.getLogger(AllocateVehicleToPlansInGroupPlanAlgorithmTest.class);

	private static String MODE = "the_vehicular_mode";

	// uncomment to get more information in case of failure
	//@Before
	//public void setupLog() {
	//	Logger.getLogger( AllocateVehicleToPlansInGroupPlanAlgorithm.class ).setLevel( Level.TRACE );
	//}

	private GroupPlans createTestPlan() {
		final List<Plan> indivPlans = new ArrayList<Plan>();
		final List<JointPlan> jointPlans = new ArrayList<JointPlan>();

		final PopulationFactory popFact = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory();
		for ( int i = 0; i < 5; i++ ) {
			final Person person = popFact.createPerson( new IdImpl( "indiv-"+i ) );
			final Plan plan = popFact.createPlan();
			person.addPlan( plan );
			plan.setPerson( person );
			indivPlans.add( plan );

			plan.addActivity( popFact.createActivityFromCoord( "h" , new CoordImpl( 12, 34 ) ) );
			if ( i % 2 == 0 ) {
				plan.addLeg( popFact.createLeg( "some_non_vehicular_mode" ) );
			}
			else {
				final Leg l = popFact.createLeg( MODE );
				l.setRoute( new LinkNetworkRouteImpl( new IdImpl( 1 ) , new IdImpl( 12 ) ) );
				plan.addLeg( l );
			}
			plan.addActivity( popFact.createActivityFromLinkId( "h" , new IdImpl( 42 ) ) );
		}

		final JointPlanFactory jpFact = new JointPlanFactory();
		for ( int i = 1; i < 5; i++ ) {
			final Map<Id, Plan> plans = new HashMap<Id, Plan>();
			for ( int j = 0; j < i; j++ ) {
				final Person person = popFact.createPerson( new IdImpl( "joint-"+i+"-"+j ) );
				final Plan plan = popFact.createPlan();
				person.addPlan( plan );
				plan.setPerson( person );
				plans.put( person.getId() , plan );

				plan.addActivity( popFact.createActivityFromCoord( "h" , new CoordImpl( 12, 34 ) ) );
				if ( (j+1) % 2 == 0 ) {
					plan.addLeg( popFact.createLeg( "some_non_vehicular_mode" ) );
				}
				else {
					final Leg l = popFact.createLeg( MODE );
					l.setRoute( new LinkNetworkRouteImpl( new IdImpl( 1 ) , new IdImpl( 12 ) ) );
					plan.addLeg( l );
				}
				plan.addActivity( popFact.createActivityFromLinkId( "h" , new IdImpl( 42 ) ) );
			}
			jointPlans.add( jpFact.createJointPlan( plans ) );
		}

		return new GroupPlans( jointPlans , indivPlans );
	}

	@Test
	public void testEnoughVehiclesForEverybody() {
		// tests that one vehicle is allocated to each one if possible
		final Random random = new Random( 1234 );

		for ( int i = 0; i < 10 ; i++ ) {
			final GroupPlans testPlan = createTestPlan();
			final VehicleRessources vehs = createEnoughVehicles( testPlan );

			final AllocateVehicleToPlansInGroupPlanAlgorithm algo =
				new AllocateVehicleToPlansInGroupPlanAlgorithm(
						random,
						vehs,
						Collections.singleton( MODE ),
						false,
						false);
			algo.run( testPlan );

			final Set<Id> allocated = new HashSet<Id>();
			for ( Plan p : testPlan.getAllIndividualPlans() ) {
				final Id v = assertSingleVehicleAndGetVehicleId( p );
				Assert.assertTrue(
						"vehicle "+v+" already allocated",
						v == null || allocated.add( v ) );
			}
		}
	}

	@Test
	public void testOneVehiclePerTwoPersons() {
		// tests that the allocation minimizes overlaps
		final Random random = new Random( 1234 );

		for ( int i = 0; i < 10 ; i++ ) {
			final GroupPlans testPlan = createTestPlan();
			final VehicleRessources vehs = createHalfVehicles( testPlan );

			final AllocateVehicleToPlansInGroupPlanAlgorithm algo =
				new AllocateVehicleToPlansInGroupPlanAlgorithm(
						random,
						vehs,
						Collections.singleton( MODE ),
						false,
						false);
			algo.run( testPlan );

			final Map<Id, Integer> counts = new LinkedHashMap<Id, Integer>();
			for ( Plan p : testPlan.getAllIndividualPlans() ) {
				final Id v = assertSingleVehicleAndGetVehicleId( p );
				// non-vehicular plan?
				if ( v == null ) continue;
				Integer c = counts.get( v );
				counts.put( v , c == null ? 1 : c + 1 );
			}

			final int max = Collections.max( counts.values() );
			Assert.assertTrue(
					"one vehicle was allocated "+max+" times while maximum expected was 2 in "+counts,
					max <= 2 );
		}
	}

	@Test
	public void testRandomness() {
		final Random random = new Random( 1234 );

		final Map<Id, Id> allocations = new HashMap<Id, Id>();
		final Set<Id> agentsWithSeveralVehicles = new HashSet<Id>();
		for ( int i = 0; i < 50 ; i++ ) {
			final GroupPlans testPlan = createTestPlan();
			final VehicleRessources vehs = createHalfVehicles( testPlan );

			final AllocateVehicleToPlansInGroupPlanAlgorithm algo =
				new AllocateVehicleToPlansInGroupPlanAlgorithm(
						random,
						vehs,
						Collections.singleton( MODE ),
						false,
						false);
			algo.run( testPlan );

			for ( Plan p : testPlan.getAllIndividualPlans() ) {
				final Id v = assertSingleVehicleAndGetVehicleId( p );
				// non-vehicular plan?
				if ( v == null ) continue;
				final Id person = p.getPerson().getId();
				final Id oldV = allocations.get( person );
				
				if ( oldV == null ) {
					allocations.put( person , v );
				}
				else if ( !oldV.equals( v ) ) {
					agentsWithSeveralVehicles.add( person );
				}
			}
		}

		Assert.assertEquals(
				"unexpected number of agents having got several vehicles",
				allocations.size(),
				agentsWithSeveralVehicles.size() );
	}

	@Test
	public void testDeterminism() {
		final Map<Id, Id> allocations = new HashMap<Id, Id>();
		final Set<Id> agentsWithSeveralVehicles = new HashSet<Id>();
		for ( int i = 0; i < 50 ; i++ ) {
			final GroupPlans testPlan = createTestPlan();
			final VehicleRessources vehs = createHalfVehicles( testPlan );

			final AllocateVehicleToPlansInGroupPlanAlgorithm algo =
				new AllocateVehicleToPlansInGroupPlanAlgorithm(
						new Random( 1432 ),
						vehs,
						Collections.singleton( MODE ),
						false,
						false);
			algo.run( testPlan );

			for ( Plan p : testPlan.getAllIndividualPlans() ) {
				final Id v = assertSingleVehicleAndGetVehicleId( p );
				// non-vehicular plan?
				if ( v == null ) continue;
				final Id person = p.getPerson().getId();
				final Id oldV = allocations.get( person );
				
				if ( oldV == null ) {
					allocations.put( person , v );
				}
				else if ( !oldV.equals( v ) ) {
					agentsWithSeveralVehicles.add( person );
				}
			}
		}

		Assert.assertEquals(
				"unexpected number of agents having got several vehicles",
				0,
				agentsWithSeveralVehicles.size() );
	}

	private static Id assertSingleVehicleAndGetVehicleId(final Plan p) {
		Id v = null;

		for ( PlanElement pe : p.getPlanElements() ) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg leg = (Leg) pe;

			if ( !MODE.equals( leg.getMode() ) ) continue;
			final NetworkRoute r = (NetworkRoute) leg.getRoute();
			
			Assert.assertNotNull(
					"null vehicle id in route",
					r.getVehicleId() );

			Assert.assertTrue(
					"vehicle "+r.getVehicleId()+" not the same as "+v,
					v == null || r.getVehicleId().equals( v ) );

			v = r.getVehicleId();
		}

		return v;
	}

	private static VehicleRessources createEnoughVehicles( final GroupPlans plans ) {
		final Set<Id> vehs = new HashSet<Id>();

		for ( int i = 0; i < plans.getAllIndividualPlans().size() ; i++ ) {
			vehs.add( new IdImpl( i ) );
		}

		log.info( "created "+vehs.size()+" vehicles" );

		return new VehicleRessources() {
				@Override
				public Set<Id> identifyVehiclesUsableForAgent(final Id person) {
					return vehs;
				}
			};
	}

	private static VehicleRessources createHalfVehicles( final GroupPlans plans ) {
		final Set<Id> vehs = new HashSet<Id>();

		// half the agents have no vehicular route: divide by 4
		for ( int i = 0; i < plans.getAllIndividualPlans().size() / 4. ; i++ ) {
			vehs.add( new IdImpl( i ) );
		}

		log.info( "created "+vehs.size()+" vehicles" );

		return new VehicleRessources() {
				@Override
				public Set<Id> identifyVehiclesUsableForAgent(final Id person) {
					return vehs;
				}
			};
	}
}

