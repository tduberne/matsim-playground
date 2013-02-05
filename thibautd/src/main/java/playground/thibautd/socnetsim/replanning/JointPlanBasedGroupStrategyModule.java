/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanBasedGroupStrategyModule.java
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
package playground.thibautd.socnetsim.replanning;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * Delegates to a {@link PlanStrategyModule} which handles JointPlans.
 * @author thibautd
 */
public class JointPlanBasedGroupStrategyModule implements GroupStrategyModule {
	private final boolean wrapIndividualPlansAndActOnThem;
	private final GenericStrategyModule<JointPlan> delegate;
	private final JointPlanFactory jointPlanFactory = new JointPlanFactory();

	public JointPlanBasedGroupStrategyModule(
			final GenericStrategyModule<JointPlan> module) {
		this( true , module );
	}

	public JointPlanBasedGroupStrategyModule(
			final boolean wrapIndividualPlansAndActOnThem,
			final GenericStrategyModule<JointPlan> module) {
		this.wrapIndividualPlansAndActOnThem = wrapIndividualPlansAndActOnThem;
		this.delegate = module;
	}

	@Override
	public void handlePlans(
			final Collection<GroupPlans> groupPlans) {
		final List<JointPlan> jointPlans = new ArrayList<JointPlan>();
		
		for (GroupPlans groupPlan : groupPlans) {
			jointPlans.addAll( groupPlan.getJointPlans() );
	
			if (!wrapIndividualPlansAndActOnThem) return;
	
			for (Plan p : groupPlan.getIndividualPlans()) {
				Map<Id, Plan> fakeJointPlanMap = new HashMap<Id, Plan>();
				fakeJointPlanMap.put( p.getPerson().getId() , p );
				JointPlan jp = jointPlanFactory.createJointPlan( fakeJointPlanMap , false );
				jointPlans.add( jp );
			}
		}
		
		delegate.handlePlans( jointPlans );
	}

	@Override
	public String toString() {
		String delegateName = delegate.getClass().getSimpleName();
		if (delegateName.length() == 0 && delegate instanceof AbstractMultithreadedModule) {
			// anonymous class
			delegateName = ((AbstractMultithreadedModule) delegate).getPlanAlgoInstance().getClass().getSimpleName();
		}

		return "["+getClass().getSimpleName()+": "+delegateName+"]";
	}
}
