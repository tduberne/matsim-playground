/* *********************************************************************** *
 * project: org.matsim.*
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
import java.util.List;

import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;

final class ForbidenCombinations {
	private final List<GroupPlans> forbidden = new ArrayList<GroupPlans>();

	public void forbid(final GroupPlans plans) {
		forbidden.add( plans );
	}

	public boolean isForbidden(final GroupPlans groupPlans) {
		return forbidden.contains( groupPlans );
	}

	public boolean partialAllocationCanLeadToForbidden(final PlanAllocation alloc) {
		return isForbidden( alloc );
	}

	public boolean isForbidden(final PlanAllocation alloc) {
		for (GroupPlans gp : forbidden) {
			if ( forbids( gp , alloc ) ) return true;
		}
		return false;
	}

	private static boolean forbids(final GroupPlans gp, final PlanAllocation alloc) {
		for ( PlanRecord p : alloc.getPlans() ) {
			if ( !gp.getAllIndividualPlans().contains( p.plan ) ) return false;
		}
		return true;
	}
}
