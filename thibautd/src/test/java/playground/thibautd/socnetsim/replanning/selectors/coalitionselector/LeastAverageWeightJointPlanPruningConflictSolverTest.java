/* *********************************************************************** *
 * project: org.matsim.*
 * LeastAverageWeightJointPlanPruningConflictSolverTest.java
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
package playground.thibautd.socnetsim.replanning.selectors.coalitionselector;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;

import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.coalitionselector.CoalitionSelector.ConflictSolver;

/**
 * @author thibautd
 */
public class LeastAverageWeightJointPlanPruningConflictSolverTest {

	@Test
	public void testPruneBiggestPlanWithHigherSum() {
		final JointPlans jointPlans = new JointPlans();

		// two joint plans, biggest has a higher total weight,
		// but a lower average
		final Map<Id, Plan> smallJp = new HashMap<Id, Plan>();
		final Map<Id, Plan> bigJp = new HashMap<Id, Plan>();

		final ReplanningGroup group = new ReplanningGroup();

		Id id = new IdImpl( 1 );
		{
			final Person person = new PersonImpl( id );
			group.addPerson( person );
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				plan.setScore( 1d );
				person.addPlan( plan );
				bigJp.put( id , plan );
			}
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				plan.setScore( 1.2d );
				person.addPlan( plan );
				smallJp.put( id , plan );
			}
		}

		id = new IdImpl( 2 );
		{
			final Person person = new PersonImpl( id );
			group.addPerson( person );
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				plan.setScore( 1.1d );
				person.addPlan( plan );
				bigJp.put( id , plan );
			}
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				plan.setScore( 1.0d );
				person.addPlan( plan );
				smallJp.put( id , plan );
			}
		}

		id = new IdImpl( 3 );
		{
			final Person person = new PersonImpl( id );
			group.addPerson( person );
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				plan.setScore( 1d );
				person.addPlan( plan );
				bigJp.put( id , plan );
			}
			{
				final Plan plan = jointPlans.getFactory().createIndividualPlan( person );
				// lowest score than all. Should not be removed though
				plan.setScore( 0d );
				person.addPlan( plan );
			}
		}

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan(
					bigJp ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan(
					smallJp ) );

		test( new ConflictSolverTestsFixture(
					jointPlans,
					group,
					bigJp.values() ) );
	}

	private static void test(final ConflictSolverTestsFixture fixture) {
		final ConflictSolver testee = new LeastAverageWeightJointPlanPruningConflictSolver();

		testee.attemptToSolveConflicts( fixture.recordsPerJointPlan );

		for ( PlanRecord r : fixture.allRecords ) {
			if ( fixture.expectedUnfeasiblePlans.contains( r.getPlan() ) ) {
				Assert.assertFalse(
						"plan "+r.getPlan()+" unexpectedly feasible",
						r.isFeasible() );
			}
			else {
				Assert.assertTrue(
						"plan "+r.getPlan()+" unexpectedly unfeasible",
						r.isFeasible() );
			}
		}
	}

}

