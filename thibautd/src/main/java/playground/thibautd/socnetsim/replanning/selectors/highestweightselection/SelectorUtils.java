/* *********************************************************************** *
 * project: org.matsim.*
 * HighestWeightSelectorUtils.java
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
package playground.thibautd.socnetsim.replanning.selectors.highestweightselection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
final class SelectorUtils {
	private SelectorUtils() {}

	public static boolean isBlocking(
			final KnownFeasibleAllocations knownFeasibleAllocations,
			final IncompatiblePlanRecords incompatibleRecords,
			final Map<Id, PersonRecord> personRecords,
			final GroupPlans groupPlan) {
		final FeasibilityChanger everybodyChanger = new FeasibilityChanger( true );
		final FeasibilityChanger infeasibility = new FeasibilityChanger();

		if ( knownFeasibleAllocations.isKownAsBlocking( groupPlan ) ) {
			return true;
		}

		if ( !knownFeasibleAllocations.blocksAllKnownAllocations( groupPlan ) ) {
			return false;
		}

		for ( PersonRecord person : personRecords.values() ) {
			for ( PlanRecord plan : person.plans ) everybodyChanger.changeIfNecessary( plan );
		}

		for ( Plan p : groupPlan.getAllIndividualPlans() ) {
			final PersonRecord person = personRecords.get( p.getPerson().getId() );
			final PlanRecord plan = person.getRecord( p );
			assert plan.isStillFeasible;
			infeasibility.changeIfNecessary( plan );
		}

		final GroupPlans relevantForbiddenPlans = new GroupPlans();
		final GroupPlans nonBlockingPlan = new GroupPlans();
		final boolean isBlocking = !searchForCombinationsWithoutForbiddenPlans(
				incompatibleRecords,
				relevantForbiddenPlans,
				nonBlockingPlan,
				new ArrayList<PersonRecord>( personRecords.values() ));
		infeasibility.resetFeasibilities();
		everybodyChanger.resetFeasibilities();

		if ( isBlocking ) {
			// only consider the plans of the group plan
			final List<Plan> ps = new ArrayList<Plan>( groupPlan.getIndividualPlans() );
			final List<JointPlan> jps = new ArrayList<JointPlan>( groupPlan.getJointPlans() );
			ps.retainAll( relevantForbiddenPlans.getIndividualPlans() );
			jps.retainAll( relevantForbiddenPlans.getJointPlans() );
			knownFeasibleAllocations.setKownBlockingCombination( new GroupPlans( jps , ps ) );
		}
		else {
			// if the plan found here remains feasible,
			// no need to re-do the search.
			assert nonBlockingPlan.getAllIndividualPlans().size() == personRecords.size() :
				"nonBlockingGroupPlanSize="+nonBlockingPlan.getAllIndividualPlans().size()+
				 " testedGroupPlanSize="+groupPlan.getAllIndividualPlans().size();
			knownFeasibleAllocations.addFeasibleAllocation( nonBlockingPlan );
		}

		return isBlocking;
	}

	private static boolean searchForCombinationsWithoutForbiddenPlans(
			final IncompatiblePlanRecords incompatibleRecords,
			final GroupPlans relevantForbiddenPlans,
			final GroupPlans constructedPlan,
			final List<PersonRecord> personsStillToAllocate) {
		//if ( !remainsFeasible( personsStillToAllocate ) ) return false;
		// do one step forward: "point" to the next person
		final List<PersonRecord> remainingPersons =
			new ArrayList<PersonRecord>( personsStillToAllocate );
		final PersonRecord currentPerson = remainingPersons.remove(0);

		final FeasibilityChanger feasibilityChanger = new FeasibilityChanger();
		final Set<Branch> exploredBranches = new HashSet<Branch>();
		// examine plans from worst to best. This increases a lot the chances
		// that the non-blocked plan found for a given leave is also non-blocked
		// for the next leave
		int i = currentPerson.plans.size() - 1;
		for (PlanRecord r = currentPerson.plans.get( i );
				r != null;
				r = i-- > 0 ? currentPerson.plans.get( i ) : null ) {
			// skip forbidden plans
			if ( !r.isStillFeasible ) {
				// remember that this plan was used for determining if blocking
				// or not
				if ( r.jointPlan == null ) {
					relevantForbiddenPlans.addIndividualPlan( r.plan );
				}
				else {
					relevantForbiddenPlans.addJointPlan( r.jointPlan );
				}
				continue;
			}

			final Set<Id> cotravs = r.jointPlan == null ?
				Collections.<Id>emptySet() :
				r.jointPlan.getIndividualPlans().keySet();

			if ( !exploredBranches.add(
						new Branch(
							cotravs,
							incompatibleRecords.getIncompatiblePlanIdentifier().identifyIncompatibilityGroups( r.plan ) ) ) ) {
				continue;
			}

			List<PersonRecord> actuallyRemainingPersons = remainingPersons;
			if (r.jointPlan != null) {
				assert containsAllIds( personsStillToAllocate , r.jointPlan.getIndividualPlans().keySet() ); 
				actuallyRemainingPersons = filter( remainingPersons , r.jointPlan );
			}

			boolean found = true;
			if ( !actuallyRemainingPersons.isEmpty() ) {
				tagIncompatiblePlansAsInfeasible(
						r,
						incompatibleRecords,
						feasibilityChanger);

				found = searchForCombinationsWithoutForbiddenPlans(
						incompatibleRecords,
						relevantForbiddenPlans,
						constructedPlan,
						actuallyRemainingPersons);

				feasibilityChanger.resetFeasibilities();
			}

			if (found) {
				if ( constructedPlan != null ) add( constructedPlan , r );
				return true;
			}
		}

		return false;
	}

	private static void add(
			final GroupPlans constructedPlan,
			final PlanRecord r) {
		if ( r.jointPlan == null ) {
			constructedPlan.addIndividualPlan( r.plan );
		}
		else {
			constructedPlan.addJointPlan( r.jointPlan );
		}
	}

	public static void tagIncompatiblePlansAsInfeasible(
			final PlanRecord r,
			final IncompatiblePlanRecords incompatibleRecords,
			final FeasibilityChanger localFeasibilityChanger) {
		for ( PlanRecord incompatible : incompatibleRecords.getIncompatiblePlans( r ) ) {
			localFeasibilityChanger.changeIfNecessary( incompatible );
		}
	}

	public static List<PersonRecord> filter(
			final List<PersonRecord> toFilter,
			final JointPlan jointPlan) {
		List<PersonRecord> newList = new ArrayList<PersonRecord>();

		for (PersonRecord r : toFilter) {
			if (!jointPlan.getIndividualPlans().containsKey( r.person.getId() )) {
				newList.add( r );
			}
		}

		return newList;
	}

	public static boolean intersects(
			final Collection<Id> ids1,
			final PlanAllocation alloc) {
		for ( PlanRecord p : alloc.getPlans() ) {
			if ( ids1.contains( p.person.person.getId() ) ) return true;
		}
		return false;
	}

	public static boolean intersectsRecords(
			final Collection<PersonRecord> records,
			final PlanAllocation alloc) {
		for ( PlanRecord p : alloc.getPlans() ) {
			if ( records.contains( p.person ) ) return true;
		}
		return false;
	}

	public static boolean containsAllIds(
			final List<PersonRecord> persons,
			final Set<Id> ids) {
		final Collection<Id> remainingIds = new HashSet<Id>( ids );

		for ( PersonRecord p : persons ) {
			remainingIds.remove( p.person.getId() );
			if ( remainingIds.isEmpty() ) return true;
		}

		return false;
	}

	public static PlanAllocation merge(
			final PlanAllocation a1,
			final PlanAllocation a2) {
		final PlanAllocation merged = new PlanAllocation();
		if ( a1 != null ) merged.addAll( a1.getPlans() );
		if ( a2 != null ) merged.addAll( a2.getPlans() );
		return merged;
	}

	public static boolean containsFeasiblePlans(final List<PlanRecord> records) {
		for ( PlanRecord r : records ) {
			if ( r.isStillFeasible ) return true;
		}
		return false;
	}

	public static boolean containsUnfeasiblePlans(final List<PlanRecord> records) {
		for ( PlanRecord r : records ) {
			if ( !r.isStillFeasible ) return true;
		}
		return false;
	}

	public static GroupPlans toGroupPlans(final PlanAllocation allocation) {
		Set<JointPlan> jointPlans = new HashSet<JointPlan>();
		List<Plan> individualPlans = new ArrayList<Plan>();

		for ( PlanRecord p : allocation.getPlans() ) {
			if ( p.jointPlan != null ) {
				jointPlans.add( p.jointPlan );
			}
			else {
				individualPlans.add( p.plan );
			}
		}

		return new GroupPlans( jointPlans , individualPlans );
	}

	public static void addToGroupPlans(
			final GroupPlans groupPlans,
			final PlanAllocation allocation) {
		final Set<JointPlan> knownJointPlans = new HashSet<JointPlan>();
		for ( PlanRecord p : allocation.getPlans() ) {
			if ( p.jointPlan != null ) {
				if ( knownJointPlans.add( p.jointPlan ) ) {
					groupPlans.addJointPlan( p.jointPlan );
				}
			}
			else {
				groupPlans.addIndividualPlan( p.plan );
			}
		}
	}

	public static PlanAllocation copy(final PlanAllocation allocation) {
		final PlanAllocation copy = new PlanAllocation();
		copy.addAll( allocation.getPlans() );
		return copy;
	}
}

