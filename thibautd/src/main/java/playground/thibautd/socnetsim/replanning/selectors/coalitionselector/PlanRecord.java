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

package playground.thibautd.socnetsim.replanning.selectors.coalitionselector;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.priorityqueue.HasIndex;

final class PlanRecord implements HasIndex {
	private final PointingAgent agent;
	private final double weight;
	private final Plan plan;
	private boolean isFeasible = true;
	private int index;

	public PlanRecord(
			final PointingAgent agent,
			final Plan plan,
			final double weight,
			final int index) {
		this.agent = agent;
		this.plan = plan;
		this.weight = weight;
		this.index = index;
	}

	public PointingAgent getAgent() {
		return agent;
	}

	public Plan getPlan() {
		return plan;
	}

	public double getWeight() {
		return weight;
	}

	public boolean isFeasible() {
		return isFeasible;
	}

	public void setInfeasible() {
		assert isFeasible;
		isFeasible = false;
	}

	@Override
	public int getArrayIndex() {
		return index;
	}
}
