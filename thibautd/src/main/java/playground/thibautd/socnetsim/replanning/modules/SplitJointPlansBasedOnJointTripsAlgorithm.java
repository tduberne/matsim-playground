/* *********************************************************************** *
 * project: org.matsim.*
 * SplitJointPlansBasedOnJointTripsAlgorithm.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.population.PassengerRoute;
import playground.thibautd.socnetsim.replanning.GroupPlansAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * Just keeps the minimal joint plans necessary given the joint trips.
 * This is thus not valid if other kinds of interactions exist (shared vehicles,
 * etc.)
 * @author thibautd
 */
public class SplitJointPlansBasedOnJointTripsAlgorithm implements GroupPlansAlgorithm {

	@Override
	public void run(final GroupPlans plans) {
		unregisterJointPlans( plans );
		splitPlans( plans );
	}

	private static void splitPlans(final GroupPlans plans) {
		final List<JointPlan> newJointPlans = new ArrayList<JointPlan>();
		final List<Plan> newIndividualPlans = new ArrayList<Plan>();
		
		for (JointPlan jp : plans.getJointPlans()) {
			final GroupPlans splitted = splitPlan( jp );
			newJointPlans.addAll( splitted.getJointPlans() );
			newIndividualPlans.addAll( splitted.getIndividualPlans() );
		}

		plans.getJointPlans().clear();
		plans.getJointPlans().addAll( newJointPlans );
		plans.getIndividualPlans().addAll( newIndividualPlans );
	}

	private static GroupPlans splitPlan(final JointPlan jp) {
		final GroupPlans groupPlans = new GroupPlans();
		final Map<Id, Plan> plansMap = new HashMap<Id, Plan>( jp.getIndividualPlans() );

		while (plansMap.size() > 0) {
			final Plan plan = plansMap.remove( plansMap.keySet().iterator().next() );
			final Map<Id, Plan> jpMap = new HashMap<Id, Plan>();
			jpMap.put( plan.getPerson().getId() , plan );

			findDependentPlans( plan , jpMap , plansMap );

			if ( jpMap.size() > 1 ) {
				groupPlans.getJointPlans().add(
						JointPlanFactory.createJointPlan( jpMap ) );
			}
			else {
				groupPlans.getIndividualPlans().add(
						jpMap.values().iterator().next() );
			}
		}

		return groupPlans;
	}

	// DFS
	private static void findDependentPlans(
			final Plan plan,
			final Map<Id, Plan> dependantPlans,
			final Map<Id, Plan> plansToLook) {
		for (PlanElement pe : plan.getPlanElements()) {
			if ( !(pe instanceof Leg) ) continue;
			final Leg leg = (Leg) pe;

			final Collection<Id> dependentIds = getDependentIds( leg );
			for (Id id : dependentIds) {
				final Plan depPlan = plansToLook.remove( id );
				if (depPlan == null) continue;

				dependantPlans.put( id , depPlan );
				findDependentPlans(
						depPlan,
						dependantPlans,
						plansToLook);
			}
		}
	}

	private static Collection<Id> getDependentIds(final Leg leg) {
		if ( JointActingTypes.DRIVER.equals( leg.getMode() ) ) {
			DriverRoute r = (DriverRoute) leg.getRoute();
			return r.getPassengersIds();
		}
		else if ( JointActingTypes.PASSENGER.equals( leg.getMode() ) ) {
			PassengerRoute r = (PassengerRoute) leg.getRoute();
			return Collections.singleton( r.getDriverId() );
		}

		return Collections.EMPTY_LIST;
	}

	private static void unregisterJointPlans(final GroupPlans plans) {
		for (JointPlan jp : plans.getJointPlans()) {
			JointPlanFactory.getPlanLinks().removeJointPlan( jp );
		}
	}
}

