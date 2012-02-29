/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutator.java
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
package playground.thibautd.router.replanning;

import java.util.Random;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.misc.Time;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.StageActivityTypes;

/**
 * A time allocation mutator to use with multi-leg routing.
 * The whole code is taken from the transit version of the time allocation
 * mutator, except that it uses a {@link StageActivityTypes} instance to 
 * check which activities it must mutate.
 *
 * @author thibautd
 */
public class TimeAllocationMutator implements PlanAlgorithm {
	private final int mutationRange;
	private final StageActivityTypes blackList;
	private final Random random;
	private boolean useActivityDurations = true;

	public TimeAllocationMutator(
			final StageActivityTypes blackList,
			final int mutationRange,
			final Random random) {
		this.blackList = blackList;
		this.mutationRange = mutationRange;
		this.random = random;
	}

	@Override
	public void run(final Plan plan) {
		double now = 0;
		boolean isFirst = true;
		ActivityImpl lastAct = (ActivityImpl) plan.getPlanElements().listIterator(plan.getPlanElements().size()).previous();

		// apply mutation to all activities except the last home activity
		for (PlanElement pe : plan.getPlanElements()) {

			if (pe instanceof ActivityImpl) {
				ActivityImpl act = (ActivityImpl)pe;

				// handle first activity
				if (isFirst) {
					isFirst = false;
					// set start to midnight
					act.setStartTime(now);
					// mutate the end time of the first activity
					act.setEndTime(mutateTime(act.getEndTime()));
					// calculate resulting duration
					act.setMaximumDuration(act.getEndTime() - act.getStartTime());
					// move now pointer
					now += act.getEndTime();

				// handle middle activities
				}
				else if (act != lastAct) {
					// assume that there will be no delay between arrival time and activity start time
					act.setStartTime(now);
					if (!blackList.isStageActivity( act.getType() )) {
						if (this.useActivityDurations) {
							if (act.getMaximumDuration() != Time.UNDEFINED_TIME) {
								// mutate the durations of all 'middle' activities
								act.setMaximumDuration(mutateTime(act.getMaximumDuration()));
								now += act.getMaximumDuration();
								// set end time accordingly
								act.setEndTime(now);
							}
							else {
								double newEndTime = mutateTime(act.getEndTime());
								if (newEndTime < now) {
									newEndTime = now;
								}
								act.setEndTime(newEndTime);
								now = newEndTime;
							}
						}
						else {
							if (act.getEndTime() == Time.UNDEFINED_TIME) {
								throw new IllegalStateException("Can not mutate activity end time because it is not set for Person: " + plan.getPerson().getId());
							}
							double newEndTime = mutateTime(act.getEndTime());
							if (newEndTime < now) {
								newEndTime = now;
							}
							act.setEndTime(newEndTime);
							now = newEndTime;
						}
					}
				// handle last activity
				}
				else {
					// assume that there will be no delay between arrival time and activity start time
					act.setStartTime(now);
					// invalidate duration and end time because the plan will be interpreted 24 hour wrap-around
					act.setMaximumDuration(Time.UNDEFINED_TIME);
					act.setEndTime(Time.UNDEFINED_TIME);
				}

			}
			else {
				LegImpl leg = (LegImpl) pe;

				// assume that there will be no delay between end time of previous activity and departure time
				leg.setDepartureTime(now);
				// let duration untouched. if defined add it to now
				if (leg.getTravelTime() != Time.UNDEFINED_TIME) {
					now += leg.getTravelTime();
				}
				// set planned arrival time accordingly
				leg.setArrivalTime(now);
			}
		}
	}

	private double mutateTime(final double time) {
		double t = time;
		if (t != Time.UNDEFINED_TIME) {
			t = t + (int)((this.random.nextDouble() * 2.0 - 1.0) * this.mutationRange);
			if (t < 0) t = 0;
			if (t > 24*3600) t = 24*3600;
		}
		else {
			t = this.random.nextInt(24*3600);
		}
		return t;
	}

	public void setUseActivityDurations(final boolean useActivityDurations) {
		this.useActivityDurations = useActivityDurations;
	}

}

