/* *********************************************************************** *
 * project: org.matsim.*
 * IncompatiblePlansIdentifierImpl.java
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
package playground.thibautd.socnetsim.replanning.selectors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;

/**
 * Default implementation returning information from a map.
 * It doesn't check the consistency of the data it is fed.
 * @author thibautd
 */
public final class IncompatiblePlansIdentifierImpl extends IncompatiblePlansIdentifier {
	private final Map<Plan, Set<Id>> incompatiblePlansPerPlans =
		new HashMap<Plan, Set<Id>>();

	@Override
	public Set<Id> identifyIncompatibilityGroups(final Plan plan) {
		final Set<Id> plans = incompatiblePlansPerPlans.get( plan );
		return plans != null ? plans : Collections.<Id>emptySet();
	}

	public void put( final Plan plan , final Set<Id> incompatiblePlans ) {
		incompatiblePlansPerPlans.put( plan , incompatiblePlans );
	}
	
	public void putAll( final Map<Plan, Set<Id>> incompatiblePlans ) {
		incompatiblePlansPerPlans.putAll( incompatiblePlans );
	}
}

