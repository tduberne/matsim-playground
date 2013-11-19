/* *********************************************************************** *
 * project: org.matsim.*
 * ActivitySequenceMutatorAlgorithmTest.java
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
package playground.thibautd.replanning;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author thibautd
 */
public class ActivitySequenceMutatorAlgorithmTest {
	@Test
	public void testTwoActivities() throws Exception {
		final Plan plan = new PlanImpl( new PersonImpl( new IdImpl( "somebody" ) ) );
		
		plan.addActivity( new ActivityImpl( "h" , new IdImpl( "h" ) ) );
		plan.addLeg( new LegImpl( "mode" ) );
		plan.addActivity( new ActivityImpl( "w" , new IdImpl( "w" ) ) );
		plan.addLeg( new LegImpl( "mode" ) );
		plan.addActivity( new ActivityImpl( "l" , new IdImpl( "l" ) ) );
		plan.addLeg( new LegImpl( "mode" ) );
		plan.addActivity( new ActivityImpl( "h" , new IdImpl( "h" ) ) );

		final PlanAlgorithm testee =
			new ActivitySequenceMutatorAlgorithm(
					new Random( 890 ),
					EmptyStageActivityTypes.INSTANCE );
		testee.run( plan );

		Assert.assertEquals(
				"unexpected size of plan "+plan.getPlanElements(),
				plan.getPlanElements().size(),
				7 );
		Assert.assertEquals(
				"unexpected type of first in-plan activity",
				((Activity) plan.getPlanElements().get( 2 )).getType(),
				"l" );
		Assert.assertEquals(
				"unexpected type of second in-plan activity",
				((Activity) plan.getPlanElements().get( 4 )).getType(),
				"w" );
	}

	@Test
	public void testOneActivities() throws Exception {
		final Plan plan = new PlanImpl( new PersonImpl( new IdImpl( "somebody" ) ) );
		
		plan.addActivity( new ActivityImpl( "h" , new IdImpl( "h" ) ) );
		plan.addLeg( new LegImpl( "mode" ) );
		plan.addActivity( new ActivityImpl( "w" , new IdImpl( "w" ) ) );
		plan.addLeg( new LegImpl( "mode" ) );
		plan.addActivity( new ActivityImpl( "h" , new IdImpl( "h" ) ) );

		final PlanAlgorithm testee =
			new ActivitySequenceMutatorAlgorithm(
					new Random( 890 ),
					EmptyStageActivityTypes.INSTANCE );
		testee.run( plan );

		Assert.assertEquals(
				"unexpected size of plan "+plan.getPlanElements(),
				plan.getPlanElements().size(),
				5 );
		Assert.assertEquals(
				"unexpected type of first in-plan activity",
				((Activity) plan.getPlanElements().get( 2 )).getType(),
				"w" );
	}

	@Test
	public void testZeroActivities() throws Exception {
		final Plan plan = new PlanImpl( new PersonImpl( new IdImpl( "somebody" ) ) );
		
		plan.addActivity( new ActivityImpl( "h" , new IdImpl( "h" ) ) );
		plan.addLeg( new LegImpl( "mode" ) );
		plan.addActivity( new ActivityImpl( "h" , new IdImpl( "h" ) ) );

		final PlanAlgorithm testee =
			new ActivitySequenceMutatorAlgorithm(
					new Random( 890 ),
					EmptyStageActivityTypes.INSTANCE );
		testee.run( plan );

		Assert.assertEquals(
				"unexpected size of plan "+plan.getPlanElements(),
				plan.getPlanElements().size(),
				3 );
	}

	@Test
	public void testStage() throws Exception {
		final Plan plan = new PlanImpl( new PersonImpl( new IdImpl( "somebody" ) ) );
		
		plan.addActivity( new ActivityImpl( "h" , new IdImpl( "h" ) ) );
		plan.addLeg( new LegImpl( "mode" ) );
		plan.addActivity( new ActivityImpl( "stage" , new IdImpl( "s" ) ) );
		plan.addLeg( new LegImpl( "mode" ) );
		plan.addActivity( new ActivityImpl( "w" , new IdImpl( "w" ) ) );
		plan.addLeg( new LegImpl( "mode" ) );
		plan.addActivity( new ActivityImpl( "l" , new IdImpl( "l" ) ) );
		plan.addLeg( new LegImpl( "mode" ) );
		plan.addActivity( new ActivityImpl( "h" , new IdImpl( "h" ) ) );

		final PlanAlgorithm testee =
			new ActivitySequenceMutatorAlgorithm(
					new Random( 890 ),
					new StageActivityTypesImpl( "stage" ) );
		testee.run( plan );

		Assert.assertEquals(
				"unexpected size of plan "+plan.getPlanElements(),
				plan.getPlanElements().size(),
				9 );
		Assert.assertEquals(
				"unexpected type of first in-plan activity",
				((Activity) plan.getPlanElements().get( 2 )).getType(),
				"stage" );
		Assert.assertEquals(
				"unexpected type of second in-plan activity",
				((Activity) plan.getPlanElements().get( 4 )).getType(),
				"l" );
		Assert.assertEquals(
				"unexpected type of third in-plan activity",
				((Activity) plan.getPlanElements().get( 6 )).getType(),
				"w" );
	}
}

