/* *********************************************************************** *
 * project: org.matsim.*
 * RecomposeJointPlanAlgorithm.java
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
package playground.thibautd.socnetsim.replanning.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
public class RecomposeJointPlanAlgorithm implements GenericPlanAlgorithm<GroupPlans> {
	private final PlanLinkIdentifier linkIdentifier;
	private final JointPlanFactory factory;

	public RecomposeJointPlanAlgorithm(
			final JointPlanFactory jointPlanFactory,
			final PlanLinkIdentifier linkIdentifier) {
		this.factory = jointPlanFactory;
		this.linkIdentifier = linkIdentifier;
	}

	@Override
	public void run(final GroupPlans groupPlans) {
		final Map<Id, Plan> plansMap = getPlansMap( groupPlans );

		groupPlans.clear();
		while (plansMap.size() > 0) {
			final Plan plan = plansMap.remove( plansMap.keySet().iterator().next() );
			final Map<Id, Plan> jpMap = new HashMap<Id, Plan>();
			jpMap.put( plan.getPerson().getId() , plan );

			findDependentPlans( plan , jpMap , plansMap );

			if ( jpMap.size() > 1 ) {
				groupPlans.addJointPlan(
						factory.createJointPlan( jpMap ) );
			}
			else {
				groupPlans.addIndividualPlan( plan );
			}
		}
	}

	private Map<Id, Plan> getPlansMap(final GroupPlans groupPlans) {
		final Map<Id, Plan> map = new HashMap<Id, Plan>();

		for (Plan p : groupPlans.getIndividualPlans()) {
			map.put( p.getPerson().getId() , p );
		}
		for (JointPlan jp : groupPlans.getJointPlans()) {
			map.putAll( jp.getIndividualPlans() );
		}

		return map;
	}

	// DFS
	private void findDependentPlans(
			final Plan plan,
			final Map<Id, Plan> dependantPlans,
			final Map<Id, Plan> plansToLook) {
		final List<Plan> dependentPlansList = new ArrayList<Plan>();

		final Iterator<Plan> toLookIt = plansToLook.values().iterator();
		while ( toLookIt.hasNext() ) {
			final Plan toLook = toLookIt.next();
			if ( linkIdentifier.areLinked( plan , toLook ) ) {
				dependentPlansList.add( toLook );
				toLookIt.remove();
			}
		}

		for (Plan depPlan : dependentPlansList) {
			dependantPlans.put( depPlan.getPerson().getId() , depPlan );
			findDependentPlans(
					depPlan,
					dependantPlans,
					plansToLook);
		}
	}
}

