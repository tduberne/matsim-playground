/* *********************************************************************** *
 * project: org.matsim.*
 * OptimizeVehicleAllocationAtTourLevelTest.java
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
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;

/**
 * @author thibautd
 */
public class OptimizeVehicleAllocationAtTourLevelTest {
	private static StageActivityTypes stages = EmptyStageActivityTypes.INSTANCE;
	private static String MODE = "the_vehicular_mode";

	private GroupPlans createTestPlan(final Random random) {
		// attempt to get a high diversity of joint structures.
		final int nMembers = random.nextInt( 100 );
		//final int nJointPlans = random.nextInt( nMembers );
		//final double pJoin = 0.1;
		//final JointPlansFactory jointPlansFact = new JointPlans().getFactory();

		final List<Plan> individualPlans = new ArrayList<Plan>();
		final List<JointPlan> jointPlans = new ArrayList<JointPlan>();

		// create plans
		int currentId = 0;
		for (int j=0; j < nMembers; j++) {
			final Id id = new IdImpl( currentId++ );
			final PersonImpl person = new PersonImpl( id );

			final Plan plan = new PlanImpl( person );
			fillPlan( plan , random );
			person.addPlan( plan );

			individualPlans.add( plan );
		}

		// TODO: join some plans in joint plans

		return new GroupPlans( jointPlans , individualPlans );
	}

	private void fillPlan(
			final Plan plan,
			final Random random) {
		final PopulationFactory popFact = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory();
		final Id homeLinkId = new IdImpl( "versailles" );

		final Activity firstAct = popFact.createActivityFromLinkId( "h" , homeLinkId );
		firstAct.setEndTime( random.nextDouble() * 24 * 3600d );
		plan.addActivity( firstAct );

		final Leg l = popFact.createLeg( MODE );
		l.setRoute( new LinkNetworkRouteImpl( new IdImpl( 1 ) , new IdImpl( 12 ) ) );
		l.setTravelTime( random.nextDouble() * 36000 );
		plan.addLeg( l );

		plan.addActivity( popFact.createActivityFromLinkId( "h" , homeLinkId ) );
	}

	@Test
	@Ignore( "TODO" )
	public void testVehiclesAreAllocatedAtTheTourLevel() throws Exception {
		throw new UnsupportedOperationException( "TODO" );
	}

	@Test
	public void testCannotFindBetterAllocationRandomly() throws Exception {
		for ( int i = 0; i < 5; i++ ) {
			final GroupPlans optimized = createTestPlan( new Random( i ) );

			final VehicleRessources vehs = createRessources( optimized );

			final OptimizeVehicleAllocationAtTourLevelAlgorithm algo =
				new OptimizeVehicleAllocationAtTourLevelAlgorithm(
						stages,
						new Random( 1234 ),
						vehs,
						MODE,
						false );
			algo.run( optimized );
			final double optimizedOverlap = algo.calcOverlap( optimized );
			final Counter counter = new Counter( "test plan # "+(i+1)+", test # " );
			for ( int j = 0; j < 5000; j++ ) {
				counter.incCounter();
				final GroupPlans randomized = createTestPlan( new Random( i ) );
				 new AllocateVehicleToPlansInGroupPlanAlgorithm(
						new Random( j ),
						vehs,
						MODE,
						false).run( randomized );
				 final double randomizedOverlap = algo.calcOverlap( randomized );
				 Assert.assertTrue(
						 "["+i+","+j+"] found better solution than optimized one: "+randomizedOverlap+" < "+optimizedOverlap,
						 optimizedOverlap <= randomizedOverlap );
			}
			counter.printCounter();
		}
	}

	private VehicleRessources createRessources(final GroupPlans optimized) {
		final Set<Id> ids = new HashSet<Id>();
		final int nVehicles = optimized.getAllIndividualPlans().size() / 2;

		int i = 0;
		for ( Plan p : optimized.getAllIndividualPlans() ) {
			if ( i++ == nVehicles ) break;
			ids.add( p.getPerson().getId() );
		}

		return new VehicleRessources() {
			@Override
			public Set<Id> identifyVehiclesUsableForAgent(final Id person) {
				return ids;
			}
		};
	}
}

