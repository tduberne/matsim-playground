/* *********************************************************************** *
 * project: org.matsim.*
 * HighestWeightSelectorTest.java
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
package playground.thibautd.socnetsim.replanning.selectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
@RunWith(Parameterized.class)
public class HighestWeightSelectorTest {
	private static final Logger log =
		Logger.getLogger(HighestWeightSelectorTest.class);

	private final Fixture fixture;

	public static class Fixture {
		final String name;
		final ReplanningGroup group;
		final GroupPlans expectedSelectedPlans;
		final GroupPlans expectedSelectedPlansWhenBlocking;
		final JointPlans jointPlans;

		public Fixture(
				final String name,
				final ReplanningGroup group,
				final GroupPlans expectedPlans,
				final GroupPlans expectedSelectedPlansWhenBlocking,
				final JointPlans jointPlans) {
			this.name = name;
			this.group = group;
			this.expectedSelectedPlans = expectedPlans;
			this.expectedSelectedPlansWhenBlocking = expectedSelectedPlansWhenBlocking;
			this.jointPlans = jointPlans;
		}
	}

	// XXX the SAME instance is used for all tests!
	// should ot be a problem, but this is contrary to the idea of "fixture"
	public HighestWeightSelectorTest(final Fixture fixture) {
		this.fixture = fixture;
		log.info( "fixture "+fixture.name );
	}

	@Parameterized.Parameters
	public static Collection<Fixture[]> fixtures() {
		return Arrays.asList(
				new Fixture[]{createIndividualPlans()},
				new Fixture[]{createFullyJointPlans()},
				new Fixture[]{createPartiallyJointPlansOneSelectedJp()},
				new Fixture[]{createPartiallyJointPlansTwoSelectedJps()},
				new Fixture[]{createPartiallyJointPlansMessOfJointPlans()},
				new Fixture[]{createPartiallyJointPlansNoSelectedJp()},
				new Fixture[]{createOneBigJointPlanDifferentNPlansPerAgent()},
				new Fixture[]{createOneBigJointPlanDifferentNPlansPerAgent2()},
				new Fixture[]{createOneBigJointPlanDifferentNPlansPerAgentWithNullScores()},
				new Fixture[]{createPlanWithDifferentSolutionIfBlocked()},
				new Fixture[]{createPlanWithNoSolutionIfBlocked()});
	}

	// /////////////////////////////////////////////////////////////////////////
	// fixtures management
	// /////////////////////////////////////////////////////////////////////////
	public static Fixture createIndividualPlans() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		List<Plan> toBeSelected = new ArrayList<Plan>();

		PersonImpl person = new PersonImpl( new IdImpl( "tintin" ) );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );
		toBeSelected.add( plan );

		person = new PersonImpl( new IdImpl( "milou" ) );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		toBeSelected.add( plan );

		person = new PersonImpl( new IdImpl( "tim" ) );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );

		person = new PersonImpl( new IdImpl( "struppy" ) );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );

		GroupPlans exp = new GroupPlans( Collections.EMPTY_LIST , toBeSelected );
		return new Fixture(
				"all individual",
				group,
				exp,
				exp,
				jointPlans);
	}

	public static Fixture createFullyJointPlans() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp3 = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp3.put( id , plan );

		id = new IdImpl( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp3.put( id , plan );

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp3.put( id , plan );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -10d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp3.put( id , plan );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		JointPlan sel = jointPlans.getFactory().createJointPlan( jp2 );
		jointPlans.addJointPlan( sel );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel ),
					Collections.EMPTY_LIST );
		return new Fixture(
				"fully joint",
				group,
				expected,
				expected,
				jointPlans);
	}

	public static Fixture createPartiallyJointPlansOneSelectedJp() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		List<Plan> toBeSelected = new ArrayList<Plan>();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp3 = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp3.put( id , plan );

		id = new IdImpl( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp3.put( id , plan );

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp3.put( id , plan );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		JointPlan sel = jointPlans.getFactory().createJointPlan( jp2 );
		jointPlans.addJointPlan( sel );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel ),
					toBeSelected );

		return new Fixture(
				"partially joint, one selected joint plan",
				group,
				expected,
				expected,
				jointPlans);

	}

	public static Fixture createPartiallyJointPlansTwoSelectedJps() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp3 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp4 = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp3.put( id , plan );

		id = new IdImpl( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp3.put( id , plan );

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp3.put( id , plan );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -15d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		JointPlan sel1 = jointPlans.getFactory().createJointPlan( jp2 );
		jointPlans.addJointPlan( sel1 );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );
		JointPlan sel2 = jointPlans.getFactory().createJointPlan( jp4 );
		jointPlans.addJointPlan( sel2 );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel1 , sel2 ),
					Collections.EMPTY_LIST );
		return new Fixture(
				"partially joint, two selected joint plans",
				group,
				expected,
				expected,
				jointPlans);
	}

	public static Fixture createPartiallyJointPlansMessOfJointPlans() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp3 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp4 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp5 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp6 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp7 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp8 = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 20d );
		jp3.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 30d );
		jp5.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 40d );
		jp7.put( id , plan );

		id = new IdImpl( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -200d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -100d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 500d );
		jp5.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 200d );
		jp8.put( id , plan );

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 100d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 11d );
		jp6.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 101d );
		jp7.put( id , plan );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 333d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 666d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 777d );
		jp6.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 444d );
		jp8.put( id , plan );

		id = new IdImpl( "haddock" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 500d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );
		jp3.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 100d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp5.put( id , plan );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp4 ) );
		JointPlan sel1 = jointPlans.getFactory().createJointPlan( jp5 );
		jointPlans.addJointPlan( sel1 );
		JointPlan sel2 = jointPlans.getFactory().createJointPlan( jp6 );
		jointPlans.addJointPlan( sel2 );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp7 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp8 ) );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel1 , sel2 ),
					Collections.EMPTY_LIST );
		return new Fixture(
				"partially joint, multiple combinations",
				group,
				expected,
				expected,
				jointPlans);
	}

	public static Fixture createOneBigJointPlanDifferentNPlansPerAgent() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		jp.put( id , plan );

		id = new IdImpl( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		jp.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp.put( id , plan );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -15d );
		jp.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -15000d );

		JointPlan sel = jointPlans.getFactory().createJointPlan( jp );
		jointPlans.addJointPlan( sel );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel ),
					Collections.EMPTY_LIST );

		return new Fixture(
				"one big joint plan",
				group,
				expected,
				null,
				jointPlans);
	}

	public static Fixture createPartiallyJointPlansNoSelectedJp() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		List<Plan> toBeSelected = new ArrayList<Plan>();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp3 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp4 = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );

		id = new IdImpl( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1000d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		jp3.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1000d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1000d );
		jp3.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );

		id = new IdImpl( "haddock" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		toBeSelected.add( plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1000d );
		jp4.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5d );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp4 ) );

		GroupPlans expected = new GroupPlans(
					Collections.EMPTY_LIST,
					toBeSelected );

		return new Fixture(
				"partially joint, no selected joint trips",
				group,
				expected,
				expected,
				jointPlans);
	}

	public static Fixture createOneBigJointPlanDifferentNPlansPerAgent2() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp = new HashMap<Id, Plan>();

		Id id = new IdImpl( "milou" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		jp.put( id , plan );

		id = new IdImpl( "tintin" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1d );
		jp.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 5000d );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10d );
		jp.put( id , plan );

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -15d );
		jp.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( -15000d );

		JointPlan sel = jointPlans.getFactory().createJointPlan( jp );
		jointPlans.addJointPlan( sel );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel ),
					Collections.EMPTY_LIST );

		return new Fixture(
				"one big joint plan order 2",
				group,
				expected,
				null,
				jointPlans);
	}

	public static Fixture createOneBigJointPlanDifferentNPlansPerAgentWithNullScores() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( -1295836d );
		jp.put( id , plan );

		id = new IdImpl( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -12348597d );
		jp.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( null );

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1043872360d );
		jp.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( null );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( -159484723d );
		jp.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -5000d );
		plan = person.createAndAddPlan( false );
		plan.setScore( null );

		JointPlan sel = jointPlans.getFactory().createJointPlan( jp );
		jointPlans.addJointPlan( sel );

		GroupPlans expected = new GroupPlans(
					Arrays.asList( sel ),
					Collections.EMPTY_LIST );

		return new Fixture(
				"one big joint plan, null scores",
				group,
				expected,
				null,
				jointPlans);
	}

	public static Fixture createPlanWithDifferentSolutionIfBlocked() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp3 = new HashMap<Id, Plan>();


		Id id = new IdImpl( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 0d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		Plan p1 = plan;
		plan = person.createAndAddPlan( false );
		plan.setScore( -1295836d );
		jp3.put( id , plan );

		id = new IdImpl( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 0d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -123445d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		Plan p2 = plan;

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		Plan p3 = plan;
		plan = person.createAndAddPlan( false );
		plan.setScore( -123454d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -1295836d );
		jp3.put( id , plan );

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 0d );
		plan = person.createAndAddPlan( false );
		plan.setScore( 1000d );
		Plan p4 = plan;
		plan = person.createAndAddPlan( false );
		plan.setScore( -1295836d );

		JointPlan sel = jointPlans.getFactory().createJointPlan( jp1 );
		jointPlans.addJointPlan( sel );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp3 ) );

		GroupPlans expected = new GroupPlans(
					Collections.EMPTY_LIST,
					Arrays.asList( p1 , p2 , p3 , p4 ) );
		GroupPlans expectedBlock = new GroupPlans(
					Arrays.asList( sel ),
					Arrays.asList( p3 , p4 ) );

		return new Fixture(
				"different plans if blocking",
				group,
				expected,
				expectedBlock,
				jointPlans);
	}

	public static Fixture createPlanWithNoSolutionIfBlocked() {
		final JointPlans jointPlans = new JointPlans();
		ReplanningGroup group = new ReplanningGroup();

		Map<Id, Plan> jp1 = new HashMap<Id, Plan>();
		Map<Id, Plan> jp2 = new HashMap<Id, Plan>();

		Id id = new IdImpl( "tintin" );
		PersonImpl person = new PersonImpl( id );
		group.addPerson( person );
		PlanImpl plan = person.createAndAddPlan( false );
		plan.setScore( 10000d );
		Plan p1 = plan;

		id = new IdImpl( "milou" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 0d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 100000d );
		Plan p2 = plan;

		id = new IdImpl( "tim" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 0d );
		jp1.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( -123454d );
		jp2.put( id , plan );
		plan = person.createAndAddPlan( false );
		plan.setScore( 10000d );
		Plan p3 = plan;

		id = new IdImpl( "struppy" );
		person = new PersonImpl( id );
		group.addPerson( person );
		plan = person.createAndAddPlan( false );
		plan.setScore( 100000d );
		Plan p4 = plan;
		plan = person.createAndAddPlan( false );
		plan.setScore( 0d );
		jp2.put( id , plan );

		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp1 ) );
		jointPlans.addJointPlan(
				jointPlans.getFactory().createJointPlan( jp2 ) );

		GroupPlans expected = new GroupPlans(
					Collections.EMPTY_LIST,
					Arrays.asList( p1 , p2 , p3 , p4 ) );
		GroupPlans expectedBlock = null;

		return new Fixture(
				"no plans if blocking",
				group,
				expected,
				expectedBlock,
				jointPlans);
	}

	@Before
	public void setupLogging() {
		//Logger.getRootLogger().setLevel( Level.TRACE );
	}

	// /////////////////////////////////////////////////////////////////////////
	// Tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testSelectedPlansNonBlockingFullExploration() throws Exception {
		testSelectedPlans( false , true );
	}

	@Test
	public void testSelectedPlansNonBlocking() throws Exception {
		testSelectedPlans( false , false );
	}

	@Test
	public void testSelectedPlansBlocking() throws Exception {
		testSelectedPlans( true , false );
	}

	/**
	 * Check that plans are not removed from the plans DB in the selection process,
	 * particularly when pruning unplausible plans.
	 */
	@Test
	public void testNoSideEffects() throws Exception {
		HighestScoreSumSelector selector = new HighestScoreSumSelector( false , false );
		final Map<Id, Integer> planCounts = new HashMap<Id, Integer>();

		final int initialGroupSize = fixture.group.getPersons().size();
		for (Person p : fixture.group.getPersons()) {
			planCounts.put( p.getId() , p.getPlans().size() );
		}

		selector.selectPlans( fixture.jointPlans , fixture.group );

		Assert.assertEquals(
				"unexpected change in group size for fixture "+fixture.name,
				initialGroupSize,
				fixture.group.getPersons().size() );

		for (Person p : fixture.group.getPersons()) {
			Assert.assertEquals(
					"unexpected change in the number of plans for agent "+p.getId()+" in fixture "+fixture.name,
					planCounts.get( p.getId() ).intValue(),
					p.getPlans().size() );
		}
	}

	private void testSelectedPlans( final boolean blocking , final boolean exploreAll ) {
		HighestScoreSumSelector selector = new HighestScoreSumSelector( blocking , exploreAll );
		GroupPlans selected = null;
		try {
			selected = selector.selectPlans( fixture.jointPlans , fixture.group );
		}
		catch (Exception e) {
			throw new RuntimeException( "exception thrown for instance <<"+fixture.name+">>", e );
		}

		Assert.assertEquals(
				"unexpected selected plan in test instance <<"+fixture.name+">> ",
				blocking ?
					fixture.expectedSelectedPlansWhenBlocking :
					fixture.expectedSelectedPlans,
				selected);
	}
}

