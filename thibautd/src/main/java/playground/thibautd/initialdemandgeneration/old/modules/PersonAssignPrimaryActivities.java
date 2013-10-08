/* *********************************************************************** *
 * project: org.matsim.*
 * PersonLicenseModel.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.thibautd.initialdemandgeneration.old.modules;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.facilities.ActivityOption;
import org.matsim.core.facilities.ActivityOptionImpl;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.knowledges.KnowledgeImpl;
import org.matsim.knowledges.Knowledges;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * This class DOES NOT assign primary activities, but the name is kept to allow
 * easy linkage to the initial balmermi class.
 *
 * What it does is:
 *
 * <ul>
 * <li> it sets the primary flag to true to <u>all</u> of the activity options
 * initially in agent's knowledges
 * <li> it adds activity options corresponding to the agent's plan: for all non-primary activities,
 * the options of the facility are added to the agent's knowledge
 * </ul>
 *
 * @author thibautd
 */
public class PersonAssignPrimaryActivities extends AbstractPersonAlgorithm implements PlanAlgorithm {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final static Logger log = Logger.getLogger(PersonAssignPrimaryActivities.class);
	private final Knowledges knowledges;
	private final ActivityFacilities facilities;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PersonAssignPrimaryActivities(
			final Knowledges knowledges,
			final ActivityFacilities facilities) {
		log.info("    init " + this.getClass().getName() + " module...");
		this.knowledges = knowledges;
		this.facilities = facilities;
		log.info("    done.");
	}

	//////////////////////////////////////////////////////////////////////
	// run methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		this.run(person.getSelectedPlan());
	}

	@Override
	public void run(final Plan plan) {
		KnowledgeImpl k = this.knowledges.getKnowledgesByPersonId().get(plan.getPerson().getId());
		if (k == null) {
			throw new RuntimeException("pid="+plan.getPerson().getId()+": no knowledge defined!");
		}
		if (!k.setPrimaryFlag(true)) {
			throw new RuntimeException("pid="+plan.getPerson().getId()+": no activities defined!");
		}

		ArrayList<ActivityOptionImpl> prim_acts = k.getActivities(true);
		for (int i=0; i<plan.getPlanElements().size(); i=i+2) {
			ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(i);
			String curr_type = act.getType();
			ActivityOption a = this.facilities.getFacilities().get( act.getFacilityId() ).getActivityOptions().get( curr_type );
			if (a == null) {
				throw new RuntimeException("pid="+plan.getPerson().getId()+": Inconsistency with f_id="+act.getFacilityId()+"!");
			}
			if (!prim_acts.contains(a)) {
				k.addActivityOption(a, false);
			}
		}
	}
}
